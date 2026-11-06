package com.example.redstonetransportation.test;

import com.example.analogalu.worldgen.OilDeposits;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

/** How often oil deposits turn up, counted over a large patch of regions for a few seeds. */
public final class OilGameTests {
    private static final int SPAN = 100;

    /** 3.75 % of regions hold a deposit (5 % of the old 75 %), and 90 % of those show a lake at the surface. */
    @GameTest(structure = TestWorld.ARENA, maxTicks = 20)
    public void depositsAreRareButMostlyShowAtTheSurface(GameTestHelper helper) {
        int regions = 0;
        int deposits = 0;
        int lakes = 0;
        for (long seed : new long[] {12345L, -987654321L, 42L, 2026L}) {
            for (int rx = -SPAN / 2; rx < SPAN / 2; rx++) {
                for (int rz = -SPAN / 2; rz < SPAN / 2; rz++) {
                    regions++;
                    OilDeposits.Deposit deposit = OilDeposits.deposit(seed, rx, rz);
                    if (deposit == null) continue;
                    deposits++;
                    if (deposit.seep()) lakes++;
                }
            }
        }
        double share = deposits / (double) regions;
        double lakeShare = lakes / (double) deposits;
        helper.assertTrue(share > 0.0345D && share < 0.0405D, "deposits in " + (share * 100.0D) + " % of regions");
        helper.assertTrue(lakeShare > 0.87D && lakeShare < 0.93D, "lakes over " + (lakeShare * 100.0D) + " % of deposits");
        helper.succeed();
    }

    /** The same deposits the seed oil map works out for seed 12345, so the map and the game agree. */
    @GameTest(structure = TestWorld.ARENA, maxTicks = 20)
    public void theOilMapAndTheGameAgree(GameTestHelper helper) {
        int[][] expected = {
                // region x, region z, x, y, z, radius x, radius y, radius z
                {-20, 9, -4123, 35, 2025, 22, 7, 29},
                {-19, -19, -3797, -2, -3856, 21, 6, 20},
                {-19, -16, -3816, 9, -3243, 21, 6, 25},
                {-19, 6, -3777, -9, 1385, 24, 7, 25},
        };
        for (int[] e : expected) {
            OilDeposits.Deposit d = OilDeposits.deposit(12345L, e[0], e[1]);
            helper.assertTrue(d != null, "no deposit in region " + e[0] + "," + e[1]);
            helper.assertTrue(d.x() == e[2] && d.y() == e[3] && d.z() == e[4] && d.radiusX() == e[5]
                    && d.radiusY() == e[6] && d.radiusZ() == e[7] && d.seep(), "region " + e[0] + "," + e[1] + " gave " + d);
        }
        int count = 0;
        for (int rx = -20; rx <= 20; rx++) {
            for (int rz = -20; rz <= 20; rz++) {
                if (OilDeposits.deposit(12345L, rx, rz) != null) count++;
            }
        }
        helper.assertValueEqual(count, 58, "deposits in 41 x 41 regions");
        helper.succeed();
    }

    @GameTest(structure = TestWorld.ARENA, maxTicks = 20)
    public void aDepositFollowsFromTheSeedAlone(GameTestHelper helper) {
        int checked = 0;
        for (int rx = -30; rx < 30; rx++) {
            for (int rz = -30; rz < 30; rz++) {
                OilDeposits.Deposit first = OilDeposits.deposit(777L, rx, rz);
                OilDeposits.Deposit again = OilDeposits.deposit(777L, rx, rz);
                helper.assertTrue(first == null ? again == null : first.equals(again), "region " + rx + "," + rz + " changed");
                if (first == null) continue;
                checked++;
                int span = OilDeposits.REGION_CHUNKS * 16;
                helper.assertTrue(Math.floorDiv(first.x(), span) == rx && Math.floorDiv(first.z(), span) == rz,
                        "deposit of region " + rx + "," + rz + " lies outside it");
            }
        }
        helper.assertTrue(checked > 50, "only " + checked + " deposits to check");
        helper.succeed();
    }
}
