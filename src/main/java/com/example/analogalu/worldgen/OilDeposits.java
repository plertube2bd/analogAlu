package com.example.analogalu.worldgen;

import net.minecraft.util.RandomSource;

/**
 * Where oil deposits are. The world is cut into regions of REGION_CHUNKS x REGION_CHUNKS chunks, and
 * each region may hold one very large deposit whose place and size follow from the world seed alone,
 * so every chunk can fill in its part of a deposit that spans many chunks.
 */
public final class OilDeposits {
    public static final int MIN_OIL_Y = -19;
    public static final int MAX_CENTER_Y = 35;
    public static final int REGION_CHUNKS = 13;
    /** Chance a region has a deposit, in hundredths of a percent: 375 is 3.75 %, 5 % of the old 75 %. */
    public static final int DEPOSIT_CHANCE_BASIS_POINTS = 375;
    /** Share of deposits that show at the surface as an oil lake over a seep. */
    public static final int LAKE_CHANCE_PERCENT = 90;

    public record Deposit(int x, int y, int z, int radiusX, int radiusY, int radiusZ, long noiseSeed,
            boolean seep, int lakeX, int lakeZ, int lakeRadiusX, int lakeRadiusZ) {}

    private OilDeposits() {}

    /** The deposit in this region, or null for the regions with none. */
    public static Deposit deposit(long worldSeed, int regionX, int regionZ) {
        RandomSource random = RandomSource.create(worldSeed ^ (regionX * 0x9E3779B97F4A7C15L)
                ^ (regionZ * 0xC2B2AE3D27D4EB4FL) ^ 0x6F696CL);
        if (random.nextInt(10000) >= DEPOSIT_CHANCE_BASIS_POINTS) return null;
        int span = REGION_CHUNKS * 16;
        int radiusX = 18 + random.nextInt(13);
        int radiusZ = 18 + random.nextInt(13);
        int radiusY = 6 + random.nextInt(5);
        // Keep clear of the region's edges so a deposit does not stray far into the next one.
        int margin = 32;
        int x = regionX * span + margin + random.nextInt(span - margin * 2);
        int z = regionZ * span + margin + random.nextInt(span - margin * 2);
        int minCenterY = MIN_OIL_Y + radiusY + 2;
        int y = minCenterY + random.nextInt(MAX_CENTER_Y - minCenterY + 1);
        long noiseSeed = random.nextLong();
        boolean lake = random.nextInt(100) < LAKE_CHANCE_PERCENT;
        // The lake sits right over the deposit, a little off its centre.
        int lakeX = x + random.nextInt(11) - 5;
        int lakeZ = z + random.nextInt(11) - 5;
        int lakeRadiusX = 9 + random.nextInt(8);
        int lakeRadiusZ = 9 + random.nextInt(8);
        return new Deposit(x, y, z, radiusX, radiusY, radiusZ, noiseSeed, lake, lakeX, lakeZ, lakeRadiusX, lakeRadiusZ);
    }
}
