package com.example.redstonetransportation.rail;

import com.example.redstonetransportation.block.AluTrainControllerBlock;
import com.example.redstonetransportation.content.ModContent;
import com.example.redstonetransportation.integration.analogalu.ElectricPower;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/** One decoded drive command, shared by rail motors and drive rails. */
public record DriveInput(int speed, boolean reverse, boolean brake) {
    /** For what moves carts, which runs on electricity once ALU has it. */
    public static DriveInput read(Level level, BlockPos pos) {
        return read(level, pos, true);
    }

    /** For controls that only ask whether they are being signalled, which stay on redstone. */
    public static DriveInput readSignals(Level level, BlockPos pos) {
        return read(level, pos, false);
    }

    private static DriveInput read(Level level, BlockPos pos, boolean electric) {
        // Prefer the dedicated ALU bridge if one is pointing its output face at this block.
        for (Direction direction : Direction.values()) {
            BlockState neighborState = level.getBlockState(pos.relative(direction));
            if (neighborState.getBlock() instanceof AluTrainControllerBlock
                    && neighborState.getValue(AluTrainControllerBlock.FACING) == direction.getOpposite()) {
                return new DriveInput(
                        neighborState.getValue(AluTrainControllerBlock.POWER),
                        neighborState.getValue(AluTrainControllerBlock.REVERSE),
                        neighborState.getValue(AluTrainControllerBlock.BRAKING)
                );
            }
        }
        // Direct ALU outputs, Copper Wire, live cables, and vanilla redstone where it still counts.
        return new DriveInput(readControlSignal(level, pos, electric), false, false);
    }

    private static int readControlSignal(Level level, BlockPos pos, boolean electric) {
        int best = electric ? ElectricPower.cableLevel(level, pos) : 0;
        for (Direction direction : Direction.values()) {
            BlockPos neighborPos = pos.relative(direction);
            BlockState neighbor = level.getBlockState(neighborPos);
            if (neighbor.is(ModContent.POWER_CELL) || electric && ElectricPower.ignores(neighbor)) continue;
            best = Math.max(best, level.getSignal(neighborPos, direction));
        }
        return Math.clamp(best, 0, 15);
    }
}
