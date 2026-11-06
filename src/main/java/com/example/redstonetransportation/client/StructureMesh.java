package com.example.redstonetransportation.client;

import com.example.redstonetransportation.structure.StructureBlocks;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.UVPair;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.BlockQuadOutput;
import net.minecraft.client.renderer.block.BlockStateModelSet;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.core.BlockPos;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.level.CardinalLighting;
import net.minecraft.world.level.ColorResolver;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.lighting.LevelLightEngine;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.storage.TagValueInput;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector3fc;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * A structure's quads, worked out once with the same code that builds world chunks, so hidden faces
 * are skipped and shading matches the world. Only the light is filled in as it is drawn.
 */
final class StructureMesh {
    private record Quad(float x, float y, float z, BakedQuad quad, int color0, int color1, int color2, int color3) {
        int color(int vertex) {
            return switch (vertex) {
                case 0 -> color0;
                case 1 -> color1;
                case 2 -> color2;
                default -> color3;
            };
        }
    }

    /** Shows a block its neighbours in the structure, and the world's biome colours for where it was built. */
    private record Blocks3D(StructureBlocks blocks, ClientLevel level, BlockPos anchor) implements BlockAndTintGetter {
        @Override
        public CardinalLighting cardinalLighting() {
            return level.cardinalLighting();
        }

        @Override
        public LevelLightEngine getLightEngine() {
            return LevelLightEngine.EMPTY;
        }

        @Override
        public int getBlockTint(BlockPos pos, ColorResolver color) {
            return level.getBlockTint(anchor.offset(pos), color);
        }

        @Override
        public BlockEntity getBlockEntity(BlockPos pos) {
            return null;
        }

        @Override
        public BlockState getBlockState(BlockPos pos) {
            BlockState state = blocks.at(pos.getX(), pos.getY(), pos.getZ());
            return state != null ? state : Blocks.AIR.defaultBlockState();
        }

        @Override
        public FluidState getFluidState(BlockPos pos) {
            return getBlockState(pos).getFluidState();
        }

        @Override
        public int getHeight() {
            return level.getHeight();
        }

        @Override
        public int getMinY() {
            return level.getMinY();
        }
    }

    private final Map<ChunkSectionLayer, List<Quad>> layers = new EnumMap<>(ChunkSectionLayer.class);
    private final List<BlockEntity> blockEntities = new ArrayList<>();
    private final BlockStateModelSet models;
    private final StructureBlocks blocks;
    private final Map<Integer, Optional<Model.Simple>> waterPatches = new HashMap<>();

    StructureMesh(StructureBlocks blocks, ClientLevel level, BlockPos anchor) {
        this.blocks = blocks;
        Minecraft minecraft = Minecraft.getInstance();
        models = minecraft.getModelManager().getBlockStateModelSet();
        boolean cutoutLeaves = minecraft.options.cutoutLeaves().get();
        ModelBlockRenderer renderer = new ModelBlockRenderer(minecraft.options.ambientOcclusion().get(), true, minecraft.getBlockColors());
        Blocks3D view = new Blocks3D(blocks, level, anchor);

        for (StructureBlocks.Cell cell : blocks.cells()) {
            BlockState state = cell.state();
            BlockPos pos = cell.pos();
            if (state.getRenderShape() == RenderShape.MODEL) {
                boolean opaque = ModelBlockRenderer.forceOpaque(cutoutLeaves, state);
                BlockQuadOutput output = (x, y, z, quad, instance) -> layers
                        .computeIfAbsent(opaque ? ChunkSectionLayer.SOLID : quad.materialInfo().layer(), key -> new ArrayList<>())
                        .add(new Quad(x, y, z, quad, instance.getColor(0), instance.getColor(1), instance.getColor(2), instance.getColor(3)));
                renderer.tesselateBlock(output, pos.getX(), pos.getY(), pos.getZ(), view, pos, state, models.get(state), state.getSeed(pos));
            }
            cell.clientData().ifPresent(tag -> {
                if (!(state.getBlock() instanceof EntityBlock block)) return;
                BlockEntity blockEntity = block.newBlockEntity(pos, state);
                if (blockEntity == null) return;
                blockEntity.setLevel(level);
                blockEntity.loadWithComponents(TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), tag));
                blockEntities.add(blockEntity);
            });
        }
    }

    /** False once the game's models have been reloaded, since the quads belong to the old set. */
    boolean isCurrent() {
        return models == Minecraft.getInstance().getModelManager().getBlockStateModelSet();
    }

    /** The patch that hides the water surface on this layer of the hull, as a boat does, or empty if the hull has none there. */
    Optional<Model.Simple> waterPatch(int layer) {
        return waterPatches.computeIfAbsent(layer, key -> {
            List<BlockPos> spaces = blocks.hullLayer(key);
            if (spaces.isEmpty()) return Optional.empty();
            CubeListBuilder cubes = CubeListBuilder.create();
            for (BlockPos space : spaces) cubes.addBox(space.getX() * 16.0F, key * 16.0F, space.getZ() * 16.0F, 16.0F, 0.0F, 16.0F);
            MeshDefinition mesh = new MeshDefinition();
            mesh.getRoot().addOrReplaceChild("patch", cubes, PartPose.ZERO);
            return Optional.of(new Model.Simple(LayerDefinition.create(mesh, 16, 16).bakeRoot(), texture -> RenderTypes.waterMask()));
        });
    }

    Iterable<ChunkSectionLayer> layers() {
        return layers.keySet();
    }

    List<BlockEntity> blockEntities() {
        return blockEntities;
    }

    void put(ChunkSectionLayer layer, PoseStack.Pose pose, VertexConsumer output, int light) {
        Matrix4f matrix = pose.pose();
        Vector3f position = new Vector3f();
        Vector3f normal = new Vector3f();
        for (Quad quad : layers.get(layer)) {
            BakedQuad baked = quad.quad();
            pose.transformNormal(baked.direction().getUnitVec3f(), normal);
            int quadLight = LightCoordsUtil.lightCoordsWithEmission(light, baked.materialInfo().lightEmission());
            for (int vertex = 0; vertex < 4; vertex++) {
                Vector3fc corner = baked.position(vertex);
                matrix.transformPosition(corner.x() + quad.x(), corner.y() + quad.y(), corner.z() + quad.z(), position);
                long uv = baked.packedUV(vertex);
                output.addVertex(position.x(), position.y(), position.z(), quad.color(vertex), UVPair.unpackU(uv), UVPair.unpackV(uv),
                        OverlayTexture.NO_OVERLAY, quadLight, normal.x(), normal.y(), normal.z());
            }
        }
    }
}
