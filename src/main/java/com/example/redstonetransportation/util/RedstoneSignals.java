package com.example.redstonetransportation.util;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;

/** Common redstone helpers. Analog ALU copper wire exposes vanilla's redstone signal API. */
public final class RedstoneSignals {
    private RedstoneSignals() {}

    public static int bestNeighborSignal(Level level, BlockPos pos) {
        return Math.clamp(level.getBestNeighborSignal(pos), 0, 15);
    }

    /** Reads every neighbor except the block's directional output face. */
    public static int readInputExceptOutput(Level level, BlockPos pos, Direction outputFace) {
        int best = 0;
        for (Direction direction : Direction.values()) {
            if (direction == outputFace) continue;
            int signal = level.getSignal(pos.relative(direction), direction);
            best = Math.max(best, signal);
        }
        return Math.clamp(best, 0, 15);
    }

    public static void notifyAllNeighbors(Level level, BlockPos pos, net.minecraft.world.level.block.Block source) {
        for (Direction direction : Direction.values()) {
            BlockPos neighborPos = pos.relative(direction);
            level.neighborChanged(neighborPos, source, null);
            level.updateNeighborsAt(neighborPos, source);
        }
    }
}
