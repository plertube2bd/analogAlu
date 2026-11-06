package com.example.redstonetransportation.machine;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * What the circuit last put across each factory controller, and how many machines it is feeding.
 * Every machine but the tree cutter is a 220 V, 200 ohm load, and the controller stands in for all of
 * the ones linked to it at once: they are in parallel, so it is 200 / n ohms between its two cables.
 */
public final class ControllerPower {
    public static final double RATED_VOLTS = 220.0D;
    public static final double MACHINE_OHMS = 200.0D;
    /** With no machine to feed the controller barely draws anything, though it still counts as wired. */
    public static final double IDLE_OHMS = 1.0E6D;
    /** A reading this old means the circuit has stopped being solved: the controller is cut off. */
    private static final long STALE_TICKS = 40L;

    private record Reading(double volts, double amps, long time) {}

    private static final Map<Level, Map<Long, Reading>> READINGS = new WeakHashMap<>();
    private static final Map<Level, Map<Long, Integer>> LOADS = new WeakHashMap<>();
    private static final Map<Level, Map<Long, Double>> TEST_FEEDS = new WeakHashMap<>();

    private ControllerPower() {}

    static void record(Level level, BlockPos pos, double volts, double amps) {
        READINGS.computeIfAbsent(level, ignored -> new HashMap<>()).put(pos.asLong(), new Reading(volts, amps, level.getGameTime()));
    }

    /** The volts across the controller now, or 0 when its circuit is dead or it is not wired. */
    public static double volts(Level level, BlockPos pos) {
        Double fed = TEST_FEEDS.getOrDefault(level, Map.of()).get(pos.asLong());
        if (fed != null) return fed;
        Reading reading = READINGS.getOrDefault(level, Map.of()).get(pos.asLong());
        if (reading == null || level.getGameTime() - reading.time() > STALE_TICKS) return 0.0D;
        return reading.volts();
    }

    public static double amps(Level level, BlockPos pos) {
        Double fed = TEST_FEEDS.getOrDefault(level, Map.of()).get(pos.asLong());
        if (fed != null) return fed / resistance(level, pos);
        Reading reading = READINGS.getOrDefault(level, Map.of()).get(pos.asLong());
        if (reading == null || level.getGameTime() - reading.time() > STALE_TICKS) return 0.0D;
        return reading.amps();
    }

    /** Machines that draw power through this controller, as last counted. */
    public static int loads(Level level, BlockPos pos) {
        return LOADS.getOrDefault(level, Map.of()).getOrDefault(pos.asLong(), 0);
    }

    static void countLoads(Level level, BlockPos pos, int machines) {
        LOADS.computeIfAbsent(level, ignored -> new HashMap<>()).put(pos.asLong(), machines);
    }

    public static double resistance(Level level, BlockPos pos) {
        int machines = loads(level, pos);
        return machines > 0 ? MACHINE_OHMS / machines : IDLE_OHMS;
    }

    /** Machine speed, 0 to 15, for these volts: 15 at the rated 220 V, in proportion below it. */
    public static int signal(double volts) {
        return (int) Math.round(Math.clamp(volts / RATED_VOLTS, 0.0D, 1.0D) * 15.0D);
    }

    /** Stands in for a whole circuit in the game tests: the controller reads these volts until told otherwise. */
    public static void feedForTest(Level level, BlockPos pos, double volts) {
        TEST_FEEDS.computeIfAbsent(level, ignored -> new HashMap<>()).put(pos.asLong(), volts);
    }
}
