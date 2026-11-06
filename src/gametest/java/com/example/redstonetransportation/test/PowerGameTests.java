package com.example.redstonetransportation.test;

import com.example.analogalu.ModBlocks;
import com.example.analogalu.block.CircuitDeviceBlock;
import com.example.analogalu.block.CircuitDeviceBlockEntity;
import com.example.analogalu.block.CopperCableBlockEntity;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

/**
 * Power sources in series and in parallel. Batteries face east, so the cable east of each one is its +.
 * A 9 volt battery has 0.15 ohm inside; the load is 10 ohm.
 */
public final class PowerGameTests {
    private static final int SOLVE_TICKS = 40;

    private static void cable(GameTestHelper helper, int x, int z) {
        helper.setBlock(new BlockPos(x, 1, z), ModBlocks.COPPER_CABLE);
    }

    private static void device(GameTestHelper helper, Block block, int x, int z) {
        helper.setBlock(new BlockPos(x, 1, z), block.defaultBlockState().setValue(CircuitDeviceBlock.FACING, Direction.EAST));
    }

    private static double volts(GameTestHelper helper, int x, int z) {
        return helper.getBlockEntity(new BlockPos(x, 1, z), CopperCableBlockEntity.class).getVoltage();
    }

    private static CircuitDeviceBlockEntity battery(GameTestHelper helper, int x, int z) {
        return helper.getBlockEntity(new BlockPos(x, 1, z), CircuitDeviceBlockEntity.class);
    }

    /**
     * Two batteries end to end, + of the first on - of the second, through one load:
     * <pre>
     *  z=2  C  C  C  R  C
     *  z=1  C           C
     *  z=0  C  B> C  B> C
     *      x=2 3  4  5  6
     * </pre>
     */
    @GameTest(structure = TestWorld.ARENA, maxTicks = 80)
    public void batteriesInSeriesAddUp(GameTestHelper helper) {
        TestWorld.floor(helper, Blocks.STONE);
        cable(helper, 2, 0);
        device(helper, ModBlocks.BATTERY, 3, 0);
        cable(helper, 4, 0);
        device(helper, ModBlocks.BATTERY, 5, 0);
        cable(helper, 6, 0);
        cable(helper, 6, 1);
        cable(helper, 6, 2);
        device(helper, ModBlocks.RESISTOR_LOAD, 5, 2);
        for (int x = 2; x <= 4; x++) cable(helper, x, 2);
        cable(helper, 2, 1);
        helper.runAtTickTime(SOLVE_TICKS, () -> {
            double across = volts(helper, 6, 1) - volts(helper, 2, 1);
            helper.assertTrue(across > 17.0 && across < 18.0, "two 9 V batteries in series gave " + across + " V across the load");
            double middle = volts(helper, 4, 0) - volts(helper, 2, 0);
            helper.assertTrue(middle > 8.4 && middle < 9.0, "the joint between them sits at " + middle + " V");
            helper.assertTrue(volts(helper, 2, 0) < 0.05, "the bottom of the stack reads " + volts(helper, 2, 0) + " V");
            helper.assertTrue(battery(helper, 3, 0).getSourceTerminalVoltage() > 8.5, "the first battery is not supplying");
            helper.assertTrue(battery(helper, 5, 0).getSourceTerminalVoltage() > 8.5, "the second battery is not supplying");
            helper.succeed();
        });
    }

    /**
     * Two batteries side by side across the same pair of rails, with the load between them:
     * <pre>
     *  z=2  C  B> C
     *  z=1  C  R  C
     *  z=0  C  B> C
     *      x=2 3  4
     * </pre>
     */
    @GameTest(structure = TestWorld.ARENA, maxTicks = 80)
    public void batteriesInParallelShareTheLoad(GameTestHelper helper) {
        TestWorld.floor(helper, Blocks.STONE);
        for (int z = 0; z <= 2; z++) {
            cable(helper, 2, z);
            cable(helper, 4, z);
        }
        device(helper, ModBlocks.BATTERY, 3, 0);
        device(helper, ModBlocks.RESISTOR_LOAD, 3, 1);
        device(helper, ModBlocks.BATTERY, 3, 2);
        helper.runAtTickTime(SOLVE_TICKS, () -> {
            double across = volts(helper, 4, 1) - volts(helper, 2, 1);
            helper.assertTrue(across > 8.8 && across < 9.0, "two 9 V batteries in parallel gave " + across + " V");
            double first = battery(helper, 3, 0).getSourceTerminalVoltage();
            double second = battery(helper, 3, 2).getSourceTerminalVoltage();
            helper.assertTrue(first > 8.8 && second > 8.8, "both batteries should be supplying: " + first + ", " + second);
            helper.succeed();
        });
    }

    /** In parallel each battery carries half the current, so each one runs down at half the rate. */
    @GameTest(structure = TestWorld.ARENA, maxTicks = 260)
    public void parallelBatteriesLastTwiceAsLong(GameTestHelper helper) {
        TestWorld.floor(helper, Blocks.STONE);
        // A lone battery on its own load, well away from the pair.
        for (int z = 0; z <= 2; z++) {
            cable(helper, 12, z);
            cable(helper, 14, z);
        }
        device(helper, ModBlocks.BATTERY, 13, 0);
        device(helper, ModBlocks.RESISTOR_LOAD, 13, 2);
        for (int z = 0; z <= 2; z++) {
            cable(helper, 2, z);
            cable(helper, 4, z);
        }
        device(helper, ModBlocks.BATTERY, 3, 0);
        device(helper, ModBlocks.RESISTOR_LOAD, 3, 1);
        device(helper, ModBlocks.BATTERY, 3, 2);
        double[] start = new double[3];
        helper.runAtTickTime(SOLVE_TICKS, () -> {
            start[0] = battery(helper, 13, 0).getStoredEnergyJoules();
            start[1] = battery(helper, 3, 0).getStoredEnergyJoules();
            start[2] = battery(helper, 3, 2).getStoredEnergyJoules();
        });
        helper.runAtTickTime(SOLVE_TICKS + 200, () -> {
            double lone = start[0] - battery(helper, 13, 0).getStoredEnergyJoules();
            double first = start[1] - battery(helper, 3, 0).getStoredEnergyJoules();
            double second = start[2] - battery(helper, 3, 2).getStoredEnergyJoules();
            helper.assertTrue(lone > 0.0, "the lone battery did not run down");
            helper.assertTrue(Math.abs(first - lone / 2.0) < lone * 0.1, "first of the pair used " + first + " J against " + lone + " J alone");
            helper.assertTrue(Math.abs(second - lone / 2.0) < lone * 0.1, "second of the pair used " + second + " J against " + lone + " J alone");
            helper.succeed();
        });
    }

    /** A single battery reads as it always did: 9 V less the drop inside it, with its - side at 0 V. */
    @GameTest(structure = TestWorld.ARENA, maxTicks = 80)
    public void oneLoneBatteryStillReadsNineVolts(GameTestHelper helper) {
        TestWorld.floor(helper, Blocks.STONE);
        for (int z = 0; z <= 2; z++) {
            cable(helper, 2, z);
            cable(helper, 4, z);
        }
        device(helper, ModBlocks.BATTERY, 3, 0);
        device(helper, ModBlocks.RESISTOR_LOAD, 3, 2);
        helper.runAtTickTime(SOLVE_TICKS, () -> {
            double across = volts(helper, 4, 1) - volts(helper, 2, 1);
            helper.assertTrue(across > 8.7 && across < 8.95, "a single battery gave " + across + " V");
            helper.assertTrue(Math.abs(volts(helper, 2, 0)) < 1.0e-6, "the - terminal is not 0 V");
            helper.assertTrue(volts(helper, 2, 1) < 0.05, "the - side reads " + volts(helper, 2, 1) + " V");
            helper.succeed();
        });
    }
    /** With no load to close the loop nothing flows, so the cables stay dead, as they always have. */
    @GameTest(structure = TestWorld.ARENA, maxTicks = 80)
    public void anOpenSeriesStackStaysDead(GameTestHelper helper) {
        TestWorld.floor(helper, Blocks.STONE);
        cable(helper, 2, 0);
        device(helper, ModBlocks.BATTERY, 3, 0);
        cable(helper, 4, 0);
        device(helper, ModBlocks.BATTERY, 5, 0);
        cable(helper, 6, 0);
        helper.runAtTickTime(SOLVE_TICKS, () -> {
            for (int x = 2; x <= 6; x += 2) helper.assertValueEqual(volts(helper, x, 0), 0.0, "cable at x=" + x);
            helper.succeed();
        });
    }
}
