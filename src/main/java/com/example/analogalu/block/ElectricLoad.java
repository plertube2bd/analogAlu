package com.example.analogalu.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/**
 * A block outside this mod's own devices that a circuit can run through, as a plain resistance
 * between two cables. The circuit solver finds it next to a cable, stamps its resistance, and tells it
 * what it got.
 */
public interface ElectricLoad {
    /** The two cables this load is wired between, or null when it is not wired on both sides. */
    @Nullable BlockPos[] loadTerminals(Level level, BlockPos pos);

    /** Ohms between the two terminals at the moment. */
    double loadResistance(Level level, BlockPos pos);

    /** What the last solve put across this load, or zero and zero once its circuit has gone dead. */
    void powered(Level level, BlockPos pos, double volts, double amps);
}
