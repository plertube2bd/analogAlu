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
    private static final int REGION_CHUNKS = 18;
    private static final int DEPOSIT_CHANCE_PERCENT = 60;
    /** 지표에서 이만큼 아래까지만 석유를 채운다(분출 통로 제외). */
    private static final int SURFACE_MARGIN = 5;

    private record Deposit(int x, int y, int z, int radiusX, int radiusY, int radiusZ, long noiseSeed,
            boolean seep) {}

    @Inject(method = "applyBiomeDecoration", at = @At("TAIL"))
    private void analog_alu$generateOil(WorldGenLevel level, ChunkAccess chunk, StructureManager structures,
            CallbackInfo callbackInfo) {
        ChunkPos chunkPos = chunk.getPos();
        int regionX = Math.floorDiv(chunkPos.x(), REGION_CHUNKS);
        int regionZ = Math.floorDiv(chunkPos.z(), REGION_CHUNKS);
        for (int rx = regionX - 1; rx <= regionX + 1; rx++) {
            for (int rz = regionZ - 1; rz <= regionZ + 1; rz++) {
                Deposit deposit = deposit(level.getSeed(), rx, rz);
                if (deposit != null) fillChunk(level, chunkPos, deposit);
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
        return new Deposit(x, y, z, radiusX, radiusY, radiusZ, random.nextLong(), random.nextBoolean());
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

        // 매장지 중심 기둥이 있는 청크만 지표로 이어지는 분출 통로를 만든다(매장지의 절반).
        if (placed == 0 || !deposit.seep || deposit.x < minX || deposit.x >= minX + 16
                || deposit.z < minZ || deposit.z >= minZ + 16) return;
        int surfaceY = level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, deposit.x, deposit.z);
        for (int y = deposit.y; y < surfaceY; y++) {
            cursor.set(deposit.x, y, deposit.z);
            BlockState pathState = level.getBlockState(cursor);
            if (pathState.is(BlockTags.BASE_STONE_OVERWORLD) || pathState.is(BlockTags.DIRT)) {
                level.setBlock(cursor, oil, 2);
            }
        }
        RandomSource random = RandomSource.create(deposit.noiseSeed);
        for (int offsetX = -1; offsetX <= 1; offsetX++) {
            for (int offsetZ = -1; offsetZ <= 1; offsetZ++) {
                if (random.nextBoolean()) continue;
                int seepX = deposit.x + offsetX;
                int seepZ = deposit.z + offsetZ;
                int seepY = level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, seepX, seepZ);
                cursor.set(seepX, seepY, seepZ);
                if (level.ensureCanWrite(cursor) && level.getBlockState(cursor).isAir()) {
                    level.setBlock(cursor, oil, 2);
                }
            }
        }
    }
}
