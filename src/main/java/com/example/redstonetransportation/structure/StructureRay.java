package com.example.redstonetransportation.structure;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/** Finds which block of a structure a line of sight lands on. */
public final class StructureRay {
    /**
     * @param cell     the block, in local coordinates
     * @param face     the side it was hit on, in local coordinates
     * @param distance how far along the line it was hit
     */
    public record Hit(BlockPos cell, Direction face, double distance) {}

    private StructureRay() {}

    public static @Nullable Hit cast(StructureBlocks blocks, StructurePose pose, Vec3 from, Vec3 direction, double range) {
        double reach = (blocks.reach() + 1.0D) * Mth.SQRT_OF_TWO + range;
        if (from.horizontal().distanceToSqr(pose.position().horizontal()) > reach * reach) return null;

        Vec3 start = toGrid(pose, from);
        Vec3 end = toGrid(pose, from.add(direction.scale(range)));
        return BlockGetter.traverseBlocks(start, end, null, (context, pos) -> {
            BlockState state = blocks.at(pos.getX(), pos.getY(), pos.getZ());
            if (state == null) return null;
            BlockHitResult hit = state.getShape(EmptyBlockGetter.INSTANCE, BlockPos.ZERO).clip(start, end, pos);
            return hit == null ? null : new Hit(pos.immutable(), hit.getDirection(), hit.getLocation().distanceTo(start));
        }, context -> null);
    }

    /** Local coordinates shifted so that a cell fills whole numbers, the way world blocks do. */
    private static Vec3 toGrid(StructurePose pose, Vec3 world) {
        return new Vec3(pose.localX(world.x, world.z) + 0.5D, world.y - pose.position().y, pose.localZ(world.x, world.z) + 0.5D);
    }
}
