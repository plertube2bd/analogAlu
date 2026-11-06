package com.example.redstonetransportation.machine;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.IntSummaryStatistics;
import java.util.List;

/** The 9x9 square directly in front of a machine, which every area machine works on. */
public final class WorkArea {
    public static final int RADIUS = 4;

    private WorkArea() {}

    public static BlockPos center(MachineBlockEntity machine) {
        BlockPos shared = machine.sharedAreaCenter();
        return shared != null ? shared : machine.getBlockPos().relative(machine.facing(), RADIUS + 1);
    }

    /**
     * The one area a row of machines works on together. Side by side, each machine's own area
     * would sit a block further along than its neighbour's, so a planter could plant where the
     * cutter beside it never reaches. Centred across the row, in front of its frontmost machine.
     */
    public static BlockPos sharedCenter(List<MachineBlockEntity> machines, Direction facing) {
        Direction.Axis across = facing.getClockWise().getAxis();
        int step = facing.getAxisDirection().getStep();
        IntSummaryStatistics row = new IntSummaryStatistics();
        int front = Integer.MIN_VALUE;
        BlockPos sample = null;
        for (MachineBlockEntity machine : machines) {
            if (machine.facing() != facing) continue;
            sample = machine.getBlockPos();
            row.accept(sample.get(across));
            front = Math.max(front, sample.get(facing.getAxis()) * step);
        }
        if (sample == null) return null;

        int acrossCenter = Math.floorDiv(row.getMin() + row.getMax(), 2);
        int forwardCenter = (front + RADIUS + 1) * step;
        return facing.getAxis() == Direction.Axis.X
                ? new BlockPos(forwardCenter, sample.getY(), acrossCenter)
                : new BlockPos(acrossCenter, sample.getY(), forwardCenter);
    }

    /** Every column of the area at height {@code y}. */
    public static List<BlockPos> layer(MachineBlockEntity machine, int y) {
        BlockPos center = center(machine);
        List<BlockPos> positions = new ArrayList<>((RADIUS * 2 + 1) * (RADIUS * 2 + 1));
        for (int dx = -RADIUS; dx <= RADIUS; dx++) {
            for (int dz = -RADIUS; dz <= RADIUS; dz++) {
                positions.add(new BlockPos(center.getX() + dx, y, center.getZ() + dz));
            }
        }
        return positions;
    }

    /** The area from one block below the machine up to {@code height} blocks above it. */
    public static AABB box(MachineBlockEntity machine, int height) {
        BlockPos center = center(machine);
        return new AABB(
                center.getX() - RADIUS, center.getY() - 1, center.getZ() - RADIUS,
                center.getX() + RADIUS + 1, center.getY() + height, center.getZ() + RADIUS + 1
        );
    }
}
