package com.example.redstonetransportation.integration.analogalu;

import com.example.redstonetransportation.RedstoneTransportation;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;

/**
 * Analog ALU's electric circuits, where the installed version has them. A block touching a live cable
 * runs on that cable's voltage, and plain redstone stops doing anything to it; signals from this mod's
 * blocks and from ALU's logic still count, since those are controls rather than power. With no circuits
 * to read, redstone carries on as it always has. The optional mod is reached through the registry and
 * reflection, so none of its classes are needed.
 */
public final class ElectricPower {
    /** How often a block with no neighbour update to wake it looks at its cables; ALU re-solves about this often. */
    public static final int CHECK_TICKS = 20;
    private static final Logger LOGGER = LoggerFactory.getLogger(RedstoneTransportation.MOD_ID);
    private static final Identifier CABLE = Identifier.fromNamespaceAndPath("analog_alu", "copper_cable");
    private static final String VANILLA = "minecraft";
    /** The most any of ALU's generators puts out, so the voltage that counts as a full signal. */
    private static final double FULL_VOLTAGE = 12.0D;
    /** Below this a cable is the return side of a circuit, or dead. */
    private static final double LIVE_VOLTAGE = 0.5D;

    private static Boolean available;
    private static Block cable;
    private static Method voltage;

    private ElectricPower() {}

    public static boolean isAvailable() {
        if (available == null) {
            available = BuiltInRegistries.BLOCK.containsKey(CABLE);
            if (available) cable = BuiltInRegistries.BLOCK.getValue(CABLE);
        }
        return available;
    }

    /** What a live cable touching this block amounts to as a signal, or 0 with none. */
    public static int cableLevel(Level level, BlockPos pos) {
        if (!isAvailable()) return 0;
        double best = 0.0D;
        for (Direction side : Direction.values()) {
            best = Math.max(best, voltageAt(level, pos.relative(side)));
        }
        return best < LIVE_VOLTAGE ? 0 : Math.clamp(Math.round(best / FULL_VOLTAGE * 15.0D), 1, 15);
    }

    /** What drives a block from any side. */
    public static int input(Level level, BlockPos pos) {
        return inputExcept(level, pos, null);
    }

    /** What drives a block, leaving out the face it sends its own output from. */
    public static int inputExcept(Level level, BlockPos pos, Direction outputFace) {
        int best = cableLevel(level, pos);
        for (Direction side : Direction.values()) {
            if (side == outputFace) continue;
            BlockPos neighbor = pos.relative(side);
            if (!ignores(level.getBlockState(neighbor))) best = Math.max(best, level.getSignal(neighbor, side));
        }
        return Math.clamp(best, 0, 15);
    }

    /** True for a signal source that no longer drives anything once there are circuits to use instead. */
    public static boolean ignores(BlockState state) {
        return isAvailable() && VANILLA.equals(BuiltInRegistries.BLOCK.getKey(state.getBlock()).getNamespace());
    }

    private static double voltageAt(Level level, BlockPos pos) {
        if (!level.getBlockState(pos).is(cable)) return 0.0D;
        BlockEntity entity = level.getBlockEntity(pos);
        if (entity == null) return 0.0D;
        try {
            if (voltage == null) voltage = entity.getClass().getMethod("getVoltage");
            return Math.abs((double) voltage.invoke(entity));
        } catch (ReflectiveOperationException | ClassCastException failure) {
            // A version that hides the voltage must not leave every machine dead: back to redstone.
            LOGGER.warn("Could not read Analog ALU's cable voltage, staying on redstone", failure);
            available = false;
            return 0.0D;
        }
    }
}
