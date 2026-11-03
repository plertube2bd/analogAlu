package com.example.analogalu.mixin;

import com.example.analogalu.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 석유 매장지 생성.
 * 세계를 REGION_CHUNKS x REGION_CHUNKS 청크 구역으로 나누고, 구역마다 확률적으로 아주 큰 매장지 하나의
 * 위치와 크기를 시드로 정한다. 각 청크는 장식 단계에서 주변 매장지 중 자기 청크 안에 들어오는 부분만 채우므로
 * 여러 청크에 걸친 큰 매장지도 잘리지 않는다.
 */
@Mixin(ChunkGenerator.class)
public abstract class OilWorldgenMixin {
    private static final int MIN_OIL_Y = -19;
    private static final int MAX_CENTER_Y = 35;
    private static final int REGION_CHUNKS = 13;
    private static final int DEPOSIT_CHANCE_PERCENT = 75;
    /** 지표에 석유 호수가 드러나는 매장지 비율. */
    private static final int LAKE_CHANCE_PERCENT = 80;
    /** 호수 수면보다 이만큼 이상 높은 기둥(언덕)은 깎지 않고 호숫가로 남긴다. */
    private static final int MAX_BANK_CUT = 5;
    /** 지표에서 이만큼 아래까지만 석유를 채운다(분출 통로 제외). */
    private static final int SURFACE_MARGIN = 5;

    private record Deposit(int x, int y, int z, int radiusX, int radiusY, int radiusZ, long noiseSeed,
            boolean seep, int lakeX, int lakeZ, int lakeRadiusX, int lakeRadiusZ) {}

    @Inject(method = "applyBiomeDecoration", at = @At("TAIL"))
    private void analog_alu$generateOil(WorldGenLevel level, ChunkAccess chunk, StructureManager structures,
            CallbackInfo callbackInfo) {
        ChunkPos chunkPos = chunk.getPos();
        int regionX = Math.floorDiv(chunkPos.x(), REGION_CHUNKS);
        int regionZ = Math.floorDiv(chunkPos.z(), REGION_CHUNKS);
        for (int rx = regionX - 1; rx <= regionX + 1; rx++) {
            for (int rz = regionZ - 1; rz <= regionZ + 1; rz++) {
                Deposit deposit = deposit(level.getSeed(), rx, rz);
                if (deposit == null) continue;
                fillChunk(level, chunkPos, deposit);
                if (deposit.seep) fillLake(level, chunkPos, deposit);
            }
        }
    }

    private static Deposit deposit(long worldSeed, int regionX, int regionZ) {
        RandomSource random = RandomSource.create(worldSeed ^ (regionX * 0x9E3779B97F4A7C15L)
                ^ (regionZ * 0xC2B2AE3D27D4EB4FL) ^ 0x6F696CL);
        if (random.nextInt(100) >= DEPOSIT_CHANCE_PERCENT) return null;
        int span = REGION_CHUNKS * 16;
        int radiusX = 18 + random.nextInt(13);
        int radiusZ = 18 + random.nextInt(13);
        int radiusY = 6 + random.nextInt(5);
        // 구역 경계에서 너무 벗어나지 않도록 가장자리를 비워 둔다.
        int margin = 32;
        int x = regionX * span + margin + random.nextInt(span - margin * 2);
        int z = regionZ * span + margin + random.nextInt(span - margin * 2);
        int minCenterY = MIN_OIL_Y + radiusY + 2;
        int y = minCenterY + random.nextInt(MAX_CENTER_Y - minCenterY + 1);
        long noiseSeed = random.nextLong();
        boolean lake = random.nextInt(100) < LAKE_CHANCE_PERCENT;
        // 호수는 매장지 바로 위, 중심에서 조금 비켜난 곳에 생긴다.
        int lakeX = x + random.nextInt(11) - 5;
        int lakeZ = z + random.nextInt(11) - 5;
        int lakeRadiusX = 9 + random.nextInt(8);
        int lakeRadiusZ = 9 + random.nextInt(8);
        return new Deposit(x, y, z, radiusX, radiusY, radiusZ, noiseSeed, lake, lakeX, lakeZ, lakeRadiusX,
                lakeRadiusZ);
    }

    private static void fillChunk(WorldGenLevel level, ChunkPos chunkPos, Deposit deposit) {
        int minX = chunkPos.getMinBlockX();
        int minZ = chunkPos.getMinBlockZ();
        if (minX + 15 < deposit.x - deposit.radiusX * 1.25 || minX > deposit.x + deposit.radiusX * 1.25
                || minZ + 15 < deposit.z - deposit.radiusZ * 1.25 || minZ > deposit.z + deposit.radiusZ * 1.25) {
            return;
        }

        BlockState oil = ModBlocks.OIL.defaultBlockState();
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        int placed = 0;
        for (int x = minX; x < minX + 16; x++) {
            for (int z = minZ; z < minZ + 16; z++) {
                int surfaceY = level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z);
                double nx = (x - deposit.x) / (double) deposit.radiusX;
                double nz = (z - deposit.z) / (double) deposit.radiusZ;
                double horizontal = nx * nx + nz * nz;
                // 가장자리를 울퉁불퉁하게: 방향에 따라 반지름이 ±15% 정도 달라진다.
                double angle = Math.atan2(nz, nx);
                double wobble = 1.0 + 0.15 * Math.sin(angle * 3 + (deposit.noiseSeed & 0xFF))
                        + 0.08 * Math.sin(angle * 7 + ((deposit.noiseSeed >> 8) & 0xFF));
                double limit = wobble * wobble;
                if (horizontal > limit) continue;
                for (int y = deposit.y - deposit.radiusY; y <= deposit.y + deposit.radiusY; y++) {
                    if (y <= MIN_OIL_Y || y >= surfaceY - SURFACE_MARGIN) continue;
                    double ny = (y - deposit.y) / (double) deposit.radiusY;
                    if (horizontal + ny * ny > limit) continue;
                    cursor.set(x, y, z);
                    if (level.getBlockState(cursor).is(BlockTags.BASE_STONE_OVERWORLD)) {
                        level.setBlock(cursor, oil, 2);
                        placed++;
                    }
                }
            }
        }

        // 매장지 중심 기둥이 있는 청크만 호수 바닥까지 이어지는 분출 통로를 만든다.
        if (placed == 0 || !deposit.seep || deposit.x < minX || deposit.x >= minX + 16
                || deposit.z < minZ || deposit.z >= minZ + 16) return;
        int surfaceY = level.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, deposit.x, deposit.z);
        for (int y = deposit.y; y < surfaceY; y++) {
            cursor.set(deposit.x, y, deposit.z);
            BlockState pathState = level.getBlockState(cursor);
            if (pathState.is(BlockTags.BASE_STONE_OVERWORLD) || pathState.is(BlockTags.DIRT)) {
                level.setBlock(cursor, oil, 2);
            }
        }
    }

    /**
     * 매장지 위 지표에 크고 얕은 석유 호수를 판다. 수면 높이는 청크 생성기의 기본 지형 높이로 정하므로
     * 이웃 청크가 아직 없어도 모든 청크가 같은 수면을 쓴다. 물 아래(바다, 강)에는 만들지 않는다.
     */
    private void fillLake(WorldGenLevel level, ChunkPos chunkPos, Deposit deposit) {
        int minX = chunkPos.getMinBlockX();
        int minZ = chunkPos.getMinBlockZ();
        if (minX + 15 < deposit.lakeX - deposit.lakeRadiusX * 1.25
                || minX > deposit.lakeX + deposit.lakeRadiusX * 1.25
                || minZ + 15 < deposit.lakeZ - deposit.lakeRadiusZ * 1.25
                || minZ > deposit.lakeZ + deposit.lakeRadiusZ * 1.25) return;

        ChunkGenerator generator = (ChunkGenerator) (Object) this;
        RandomState randomState = level.getLevel().getChunkSource().randomState();
        int land = generator.getBaseHeight(deposit.lakeX, deposit.lakeZ, Heightmap.Types.OCEAN_FLOOR_WG, level,
                randomState);
        int surface = generator.getBaseHeight(deposit.lakeX, deposit.lakeZ, Heightmap.Types.WORLD_SURFACE_WG,
                level, randomState);
        if (surface != land || land <= level.getSeaLevel()) return; // 물속이거나 해수면 이하
        int lakeLevel = land - 1;

        BlockState oil = ModBlocks.OIL.defaultBlockState();
        BlockState air = net.minecraft.world.level.block.Blocks.AIR.defaultBlockState();
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int x = minX; x < minX + 16; x++) {
            for (int z = minZ; z < minZ + 16; z++) {
                double nx = (x - deposit.lakeX) / (double) deposit.lakeRadiusX;
                double nz = (z - deposit.lakeZ) / (double) deposit.lakeRadiusZ;
                double angle = Math.atan2(nz, nx);
                double wobble = 1.0 + 0.15 * Math.sin(angle * 4 + ((deposit.noiseSeed >> 16) & 0xFF))
                        + 0.08 * Math.sin(angle * 9 + ((deposit.noiseSeed >> 24) & 0xFF));
                double r2 = (nx * nx + nz * nz) / (wobble * wobble);
                if (r2 > 1.0) continue;

                int columnTop = level.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, x, z); // 첫 공기/물 칸
                if (columnTop - 1 > lakeLevel + MAX_BANK_CUT) continue; // 높은 언덕은 호숫가로 둔다
                if (level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z) != columnTop) continue; // 물
                int top = Math.min(lakeLevel, columnTop - 1);
                // 가운데가 깊은 그릇 모양: 가장자리 1칸, 가운데 최대 5칸.
                int depth = 1 + (int) Math.round(4.0 * (1.0 - r2));
                for (int y = top - depth + 1; y <= top; y++) {
                    cursor.set(x, y, z);
                    BlockState here = level.getBlockState(cursor);
                    if (here.is(net.minecraft.world.level.block.Blocks.BEDROCK) || here.hasBlockEntity()) continue;
                    level.setBlock(cursor, oil, 2);
                }
                // 수면 위의 흙, 풀, 꽃, 나무 밑동 등을 걷어 내 호수가 트이게 한다.
                for (int y = top + 1; y <= lakeLevel + MAX_BANK_CUT + 6; y++) {
                    cursor.set(x, y, z);
                    BlockState here = level.getBlockState(cursor);
                    if (here.isAir() || here.hasBlockEntity()) continue;
                    if (!here.getFluidState().isEmpty()) continue;
                    level.setBlock(cursor, air, 2);
                }
            }
        }
    }
}
