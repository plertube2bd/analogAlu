package com.example.redstonetransportation.structure;

import com.example.redstonetransportation.RedstoneTransportation;
import com.example.redstonetransportation.block.ElevatorCallBlock;
import com.example.redstonetransportation.block.StructureCoreBlock;
import com.example.redstonetransportation.content.ModAdvancements;
import com.example.redstonetransportation.content.ModContent;
import com.example.redstonetransportation.vehicle.ModEntityTypes;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.storage.TagValueInput;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Lifts a built structure out of the world, and sets it back down again. */
public final class StructureAssembler {
    public static final int MAX_BLOCKS = 1024;
    /** One balloon lifts this many blocks, itself included. */
    public static final int BLOCKS_PER_BALLOON = 16;
    /** Ground the search stops at, so a structure built on a hillside takes none of the hill with it. */
    private static final TagKey<Block> TERRAIN = TagKey.create(Registries.BLOCK,
            Identifier.fromNamespaceAndPath(RedstoneTransportation.MOD_ID, "structure_terrain"));
    private static final int FLOOR_OFFSET_RANGE = 4;
    private static final int GIANT_BLOCKS = 500;

    /** Nothing here may reach the rest of the world: the blocks go and return as one, and only after that is it told. */
    private static final int QUIET = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE | Block.UPDATE_SUPPRESS_DROPS
            | Block.UPDATE_SKIP_BLOCK_ENTITY_SIDEEFFECTS;

    public record Result(@Nullable MovingStructureEntity structure, Component message) {}

    private StructureAssembler() {}

    /**
     * Takes every block joined to the core, and leaves the world untouched if it cannot.
     *
     * @param limit when set, the player's chosen area: all of it counts, even blocks that look like ground
     */
    public static Result assemble(ServerLevel level, BlockPos core, @Nullable BoundingBox limit) {
        BlockState coreState = level.getBlockState(core);
        if (!(coreState.getBlock() instanceof StructureCoreBlock coreBlock)) return failed("structure_no_core");
        if (limit != null && !limit.isInside(core)) return failed("structure_core_outside");

        List<BlockPos> found = new ArrayList<>();
        LongOpenHashSet seen = new LongOpenHashSet();
        ArrayDeque<BlockPos> open = new ArrayDeque<>();
        open.add(core);
        seen.add(core.asLong());
        while (!open.isEmpty()) {
            BlockPos pos = open.poll();
            found.add(pos);
            if (found.size() > MAX_BLOCKS) {
                return new Result(null, Component.translatable("message.redstone_transportation.structure_too_large", MAX_BLOCKS));
            }
            for (Direction side : Direction.values()) {
                BlockPos next = pos.relative(side);
                if (!seen.add(next.asLong()) || !belongs(level, next, limit)) continue;
                if (level.getBlockState(next).getBlock() instanceof StructureCoreBlock) {
                    return new Result(null, Component.translatable("message.redstone_transportation.structure_two_cores",
                            next.getX(), next.getY(), next.getZ()));
                }
                open.add(next);
            }
        }

        StructureKind kind = coreBlock.kind();
        Component refusal = refusal(level, kind, found);
        if (refusal != null) return new Result(null, refusal);

        List<StructureBlocks.Cell> cells = new ArrayList<>(found.size());
        for (BlockPos pos : found) cells.add(capture(level, pos, core));

        MovingStructureEntity structure = ModEntityTypes.MOVING_STRUCTURE.create(level, EntitySpawnReason.TRIGGERED);
        if (structure == null) return failed("structure_no_core");
        Direction facing = coreState.getValue(StructureCoreBlock.FACING);
        structure.setup(kind, new StructureBlocks(cells), core, facing, kind == StructureKind.ELEVATOR ? floorOffset(level, core) : 0);

        for (BlockPos pos : found) level.setBlock(pos, Blocks.AIR.defaultBlockState(), QUIET);
        for (BlockPos pos : found) level.updateNeighborsAt(pos, Blocks.AIR);
        level.addFreshEntity(structure);
        return new Result(structure, Component.translatable("message.redstone_transportation.structure_assembled", cells.size()));
    }

    public static void awardAssembly(Player player, MovingStructureEntity structure) {
        ModAdvancements.award(player, ModAdvancements.STRUCTURE);
        ModAdvancements.award(player, switch (structure.kind()) {
            case ELEVATOR -> ModAdvancements.ELEVATOR;
            case SHIP -> ModAdvancements.SHIP;
            case AIRSHIP -> ModAdvancements.AIRSHIP;
        });
        if (structure.blocks().size() >= GIANT_BLOCKS) ModAdvancements.award(player, ModAdvancements.GIANT);
    }

    private static boolean belongs(ServerLevel level, BlockPos pos, @Nullable BoundingBox limit) {
        if (limit != null && !limit.isInside(pos)) return false;
        BlockState state = level.getBlockState(pos);
        if (state.isAir() || state.getBlock() instanceof LiquidBlock || state.canBeReplaced()) return false;
        if (state.getDestroySpeed(level, pos) < 0.0F) return false;
        if (limit != null) return true;
        // Leaves a player put on the deck belong to the ship; a tree it was moored by does not.
        boolean wildLeaves = state.getBlock() instanceof LeavesBlock && !state.getValue(LeavesBlock.PERSISTENT);
        return !wildLeaves && !state.is(TERRAIN);
    }

    private static @Nullable Component refusal(ServerLevel level, StructureKind kind, List<BlockPos> blocks) {
        return switch (kind) {
            case ELEVATOR -> null;
            case SHIP -> isOnWater(level, blocks) ? null : Component.translatable("message.redstone_transportation.structure_no_water");
            case AIRSHIP -> {
                long balloons = blocks.stream().filter(pos -> level.getBlockState(pos).is(ModContent.LIFT_BALLOON)).count();
                long needed = Mth.positiveCeilDiv(blocks.size(), BLOCKS_PER_BALLOON);
                yield balloons >= needed ? null
                        : Component.translatable("message.redstone_transportation.structure_no_lift", needed - balloons, BLOCKS_PER_BALLOON);
            }
        };
    }

    private static boolean isOnWater(ServerLevel level, List<BlockPos> blocks) {
        int bottom = blocks.stream().mapToInt(BlockPos::getY).min().orElse(0);
        List<BlockPos> hull = blocks.stream().filter(pos -> pos.getY() == bottom).toList();
        long wet = hull.stream().filter(pos -> level.getFluidState(pos.below()).is(FluidTags.WATER)).count();
        return wet * 2 >= hull.size();
    }

    private static StructureBlocks.Cell capture(ServerLevel level, BlockPos pos, BlockPos core) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        Optional<CompoundTag> saved = Optional.empty();
        Optional<CompoundTag> clientData = Optional.empty();
        if (blockEntity != null) {
            saved = Optional.of(blockEntity.saveWithFullMetadata(level.registryAccess()));
            clientData = Optional.of(blockEntity.getUpdateTag(level.registryAccess()));
        }
        return new StructureBlocks.Cell(pos.subtract(core), level.getBlockState(pos), saved, clientData);
    }

    /** A call block near the core sets how high above the floor every call block of this elevator sits. */
    private static int floorOffset(ServerLevel level, BlockPos core) {
        int radius = MovingStructureEntity.FLOOR_RADIUS;
        int best = 0;
        boolean found = false;
        for (BlockPos pos : BlockPos.betweenClosed(core.offset(-radius, -FLOOR_OFFSET_RANGE, -radius),
                core.offset(radius, FLOOR_OFFSET_RANGE, radius))) {
            if (!(level.getBlockState(pos).getBlock() instanceof ElevatorCallBlock)) continue;
            int offset = pos.getY() - core.getY();
            if (!found || Math.abs(offset) < Math.abs(best)) best = offset;
            found = true;
        }
        return best;
    }

    /** Sets the blocks down on the nearest whole position and returns what happened. */
    public static Component restore(ServerLevel level, MovingStructureEntity structure) {
        structure.storeBlockEntities();
        Rotation rotation = structure.pose().rotation();
        BlockPos origin = BlockPos.containing(structure.getX(), structure.getY() + 0.5D, structure.getZ());
        StructureBlocks blocks = structure.blocks();

        Map<BlockPos, StructureBlocks.Cell> targets = new HashMap<>();
        for (StructureBlocks.Cell cell : blocks.cells()) {
            BlockPos target = origin.offset(cell.pos().rotate(rotation));
            if (!level.isLoaded(target) || !level.isInWorldBounds(target) || !level.getBlockState(target).canBeReplaced()) {
                return Component.translatable("message.redstone_transportation.structure_blocked", target.getX(), target.getY(), target.getZ());
            }
            targets.put(target, cell);
        }

        structure.ejectPassengers();
        for (Map.Entry<BlockPos, StructureBlocks.Cell> entry : targets.entrySet()) {
            level.setBlock(entry.getKey(), entry.getValue().state().rotate(rotation), QUIET);
        }
        for (Map.Entry<BlockPos, StructureBlocks.Cell> entry : targets.entrySet()) {
            entry.getValue().blockEntity().ifPresent(tag -> {
                BlockEntity blockEntity = level.getBlockEntity(entry.getKey());
                if (blockEntity != null) {
                    blockEntity.loadWithComponents(TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), tag));
                    blockEntity.setChanged();
                }
            });
        }
        if (structure.kind() == StructureKind.SHIP) drainHull(level, targets);
        for (BlockPos pos : targets.keySet()) level.updateNeighborsAt(pos, level.getBlockState(pos).getBlock());

        structure.discard();
        return Component.translatable("message.redstone_transportation.structure_restored", targets.size());
    }

    /** Water that flowed in under the deck while the ship sailed would otherwise stay in its hold. */
    private static void drainHull(ServerLevel level, Map<BlockPos, StructureBlocks.Cell> blocks) {
        Map<Long, int[]> columns = new HashMap<>();
        for (BlockPos pos : blocks.keySet()) {
            int[] range = columns.computeIfAbsent(BlockPos.asLong(pos.getX(), 0, pos.getZ()), key -> new int[]{pos.getY(), pos.getY()});
            range[0] = Math.min(range[0], pos.getY());
            range[1] = Math.max(range[1], pos.getY());
        }
        for (Map.Entry<Long, int[]> column : columns.entrySet()) {
            int x = BlockPos.getX(column.getKey());
            int z = BlockPos.getZ(column.getKey());
            for (int y = column.getValue()[0]; y <= column.getValue()[1]; y++) {
                BlockPos pos = new BlockPos(x, y, z);
                if (!blocks.containsKey(pos) && level.getBlockState(pos).getBlock() instanceof LiquidBlock) {
                    level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
                }
            }
        }
    }

    private static Result failed(String key) {
        return new Result(null, Component.translatable("message.redstone_transportation." + key));
    }
}
