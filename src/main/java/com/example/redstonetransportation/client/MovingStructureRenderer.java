package com.example.redstonetransportation.client;

import com.example.redstonetransportation.structure.MovingStructureEntity;
import com.example.redstonetransportation.structure.StructureBlocks;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.util.Unit;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/** Draws a moving structure's blocks, turned about its core. */
public final class MovingStructureRenderer extends EntityRenderer<MovingStructureEntity, MovingStructureRenderer.State> {
    public static final class State extends EntityRenderState {
        StructureMesh mesh;
        float turned;
        final List<BlockEntityRenderState> blockEntities = new ArrayList<>();
        final List<BlockPos> blockEntityPositions = new ArrayList<>();
        BlockPos target;
        VoxelShape targetShape;
        /** Height of the water surface above the core, or NaN with no water there. */
        float waterLevel;
    }

    private static final int OUTLINE_COLOR = ARGB.colorFromFloat(0.4F, 0.0F, 0.0F, 0.0F);
    private static final float OUTLINE_WIDTH = 2.5F;
    /** Just above the surface, or the water would fight with the patch for the same depth. */
    private static final float PATCH_LIFT = 0.01F;

    private final Map<StructureBlocks, StructureMesh> meshes = new WeakHashMap<>();

    public MovingStructureRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    /** Most of a structure lies outside the core-sized hitbox the culling check looks at. */
    @Override
    protected boolean affectedByCulling(MovingStructureEntity entity) {
        return false;
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(MovingStructureEntity entity, State state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.mesh = meshFor(entity);
        state.turned = entity.turned(Mth.rotLerp(partialTicks, entity.yRotO, entity.getYRot()));

        state.blockEntities.clear();
        state.blockEntityPositions.clear();
        if (state.mesh != null) extractBlockEntities(state, partialTicks, entity.position());

        state.target = StructureTargeting.targetOn(entity);
        BlockState target = state.target == null ? null : entity.blocks().at(state.target.getX(), state.target.getY(), state.target.getZ());
        state.targetShape = target == null ? null : target.getShape(EmptyBlockGetter.INSTANCE, BlockPos.ZERO);
        state.waterLevel = waterLevel(entity);
    }

    private static float waterLevel(MovingStructureEntity entity) {
        BlockPos.MutableBlockPos probe = entity.blockPosition().mutable();
        int core = probe.getY();
        for (int y = entity.blocks().maxY(); y >= entity.blocks().minY(); y--) {
            probe.setY(core + y);
            FluidState fluid = entity.level().getFluidState(probe);
            if (fluid.is(FluidTags.WATER)) return (float) (probe.getY() + fluid.getHeight(entity.level(), probe) - entity.getY());
        }
        return Float.NaN;
    }

    private StructureMesh meshFor(MovingStructureEntity entity) {
        StructureBlocks blocks = entity.blocks();
        if (blocks.isEmpty() || !(entity.level() instanceof ClientLevel level)) return null;
        StructureMesh mesh = meshes.get(blocks);
        if (mesh == null || !mesh.isCurrent()) {
            mesh = new StructureMesh(blocks, level, entity.blockPosition());
            meshes.put(blocks, mesh);
        }
        return mesh;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void extractBlockEntities(State state, float partialTicks, Vec3 position) {
        BlockEntityRenderDispatcher dispatcher = Minecraft.getInstance().getBlockEntityRenderDispatcher();
        for (BlockEntity blockEntity : state.mesh.blockEntities()) {
            BlockEntityRenderer renderer = dispatcher.getRenderer(blockEntity);
            if (renderer == null) continue;
            BlockEntityRenderState blockState = renderer.createRenderState();
            renderer.extractRenderState(blockEntity, blockState, partialTicks, position, null);
            blockState.lightCoords = state.lightCoords;
            state.blockEntities.add(blockState);
            state.blockEntityPositions.add(blockEntity.getBlockPos());
        }
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        if (state.mesh == null) return;
        StructureMesh mesh = state.mesh;
        int light = state.lightCoords;

        poseStack.pushPose();
        poseStack.rotateDegrees(Axis.YP, -state.turned);
        poseStack.translate(-0.5F, 0.0F, -0.5F);
        for (ChunkSectionLayer layer : mesh.layers()) {
            collector.submitCustomGeometry(poseStack, renderType(layer), (pose, output) -> mesh.put(layer, pose, output, light));
        }
        if (!Float.isNaN(state.waterLevel)) {
            int layer = Mth.floor(state.waterLevel);
            mesh.waterPatch(layer).ifPresent(patch -> {
                poseStack.pushPose();
                poseStack.translate(0.0F, state.waterLevel - layer + PATCH_LIFT, 0.0F);
                collector.submitModel(patch, Unit.INSTANCE, poseStack, TextureAtlas.LOCATION_BLOCKS, light, OverlayTexture.NO_OVERLAY, 0);
                poseStack.popPose();
            });
        }
        if (state.targetShape != null) {
            poseStack.pushPose();
            poseStack.translate(state.target.getX(), state.target.getY(), state.target.getZ());
            collector.submitShapeOutline(poseStack, state.targetShape, RenderTypes.linesTranslucent(), OUTLINE_COLOR, OUTLINE_WIDTH, false);
            poseStack.popPose();
        }

        BlockEntityRenderDispatcher dispatcher = Minecraft.getInstance().getBlockEntityRenderDispatcher();
        for (int i = 0; i < state.blockEntities.size(); i++) {
            BlockPos pos = state.blockEntityPositions.get(i);
            poseStack.pushPose();
            poseStack.translate(pos.getX(), pos.getY(), pos.getZ());
            dispatcher.submit(state.blockEntities.get(i), poseStack, collector, camera);
            poseStack.popPose();
        }
        poseStack.popPose();
        super.submit(state, poseStack, collector, camera);
    }

    private static RenderType renderType(ChunkSectionLayer layer) {
        return switch (layer) {
            case SOLID -> RenderTypes.solidMovingBlock();
            case CUTOUT -> RenderTypes.cutoutMovingBlock();
            case TRANSLUCENT -> RenderTypes.translucentMovingBlock();
        };
    }
}
