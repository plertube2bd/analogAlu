package com.example.redstonetransportation.integration.analogalu;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Optional bridge to Analog Redstone ALU.
 * The ALU exposes Copper Wire and logic outputs through Minecraft's 0..15 redstone API.
 * Registry-id detection deliberately avoids loading classes from the optional mod.
 */
public final class AnalogAluSignalAdapter {
    private static final String ALU_NAMESPACE = "analog_alu";

    private AnalogAluSignalAdapter() {}

    /** The strongest thing driving this block: electricity where ALU has it, otherwise any redstone. */
    public static int readBestNeighborSignal(Level level, BlockPos receiverPos) {
        if (ElectricPower.isAvailable()) return ElectricPower.input(level, receiverPos);
        return Math.clamp(level.getBestNeighborSignal(receiverPos), 0, 15);
    }

    /** Uses the same directional redstone query contract as Analog ALU's own logic blocks. */
    public static int readInput(Level level, BlockPos receiverPos, Direction inputDirection) {
        BlockPos sourcePos = receiverPos.relative(inputDirection);
        return Math.clamp(level.getSignal(sourcePos, inputDirection), 0, 15);
    }

    /** True for any Analog ALU block that can provide a 4-bit signal, so new ALU parts work unchanged. */
    public static boolean isAnalogAluComponent(BlockState state) {
        return state.isSignalSource()
                && ALU_NAMESPACE.equals(BuiltInRegistries.BLOCK.getKey(state.getBlock()).getNamespace());
    }
}
