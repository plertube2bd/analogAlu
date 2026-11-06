package com.example.analogalu.mixin;

import com.example.analogalu.ModBlocks;
import com.example.analogalu.worldgen.OilDeposits;
import com.example.analogalu.worldgen.OilDeposits.Deposit;
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
 * 석유 매장지 생성. 매장지의 위치와 크기는 {@link OilDeposits}가 시드로 정한다(구역의 3.75 %, 그중 90 %는
 * 지표에 석유 호수가 드러난다). 각 청크는 장식 단계에서 주변 매장지 중 자기 청크 안에 들어오는 부분만 채우므로
 * 여러 청크에 걸친 큰 매장지도 잘리지 않는다.
 */
@Mixin(ChunkGenerator.class)
public abstract class OilWorldgenMixin {
    private static final int MIN_OIL_Y = OilDeposits.MIN_OIL_Y;
    private static final int REGION_CHUNKS = OilDeposits.REGION_CHUNKS;
    /** 호수 수면보다 이만큼 이상 높은 기둥(언덕)은 깎지 않고 호숫가로 남긴다. */
    private static final int MAX_BANK_CUT = 5;
    /** 지표에서 이만큼 아래까지만 석유를 채운다(분출 통로 제외). */
    private static final int SURFACE_MARGIN = 5;

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
                if (deposit.seep()) fillLake(level, chunkPos, deposit);
            }
        }
    }

    private static Deposit deposit(long worldSeed, int regionX, int regionZ) {
        return OilDeposits.deposit(worldSeed, regionX, regionZ);
    }

    private static void fillChunk(WorldGenLevel level, ChunkPos chunkPos, Deposit deposit) {
        int minX = chunkPos.getMinBlockX();
        int minZ = chunkPos.getMinBlockZ();
        if (minX + 15 < deposit.x() - deposit.radiusX() * 1.25 || minX > deposit.x() + deposit.radiusX() * 1.25
                || minZ + 15 < deposit.z() - deposit.radiusZ() * 1.25 || minZ > deposit.z() + deposit.radiusZ() * 1.25) {
            return;
        }

        BlockState oil = ModBlocks.OIL.defaultBlockState();
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        int placed = 0;
        for (int x = minX; x < minX + 16; x++) {
            for (int z = minZ; z < minZ + 16; z++) {
                int surfaceY = level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z);
                double nx = (x - deposit.x()) / (double) deposit.radiusX();
                double nz = (z - deposit.z()) / (double) deposit.radiusZ();
                double horizontal = nx * nx + nz * nz;
                // 가장자리를 울퉁불퉁하게: 방향에 따라 반지름이 ±15% 정도 달라진다.
                double angle = Math.atan2(nz, nx);
                double wobble = 1.0 + 0.15 * Math.sin(angle * 3 + (deposit.noiseSeed() & 0xFF))
                        + 0.08 * Math.sin(angle * 7 + ((deposit.noiseSeed() >> 8) & 0xFF));
                double limit = wobble * wobble;
                if (horizontal > limit) continue;
                for (int y = deposit.y() - deposit.radiusY(); y <= deposit.y() + deposit.radiusY(); y++) {
                    if (y <= MIN_OIL_Y || y >= surfaceY - SURFACE_MARGIN) continue;
                    double ny = (y - deposit.y()) / (double) deposit.radiusY();
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
        if (placed == 0 || !deposit.seep() || deposit.x() < minX || deposit.x() >= minX + 16
                || deposit.z() < minZ || deposit.z() >= minZ + 16) return;
        int surfaceY = level.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, deposit.x(), deposit.z());
        for (int y = deposit.y(); y < surfaceY; y++) {
            cursor.set(deposit.x(), y, deposit.z());
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
        if (minX + 15 < deposit.lakeX() - deposit.lakeRadiusX() * 1.25
                || minX > deposit.lakeX() + deposit.lakeRadiusX() * 1.25
                || minZ + 15 < deposit.lakeZ() - deposit.lakeRadiusZ() * 1.25
                || minZ > deposit.lakeZ() + deposit.lakeRadiusZ() * 1.25) return;

        ChunkGenerator generator = (ChunkGenerator) (Object) this;
        RandomState randomState = level.getLevel().getChunkSource().randomState();
        int land = generator.getBaseHeight(deposit.lakeX(), deposit.lakeZ(), Heightmap.Types.OCEAN_FLOOR_WG, level,
                randomState);
        int surface = generator.getBaseHeight(deposit.lakeX(), deposit.lakeZ(), Heightmap.Types.WORLD_SURFACE_WG,
                level, randomState);
        if (surface != land || land <= level.getSeaLevel()) return; // 물속이거나 해수면 이하
        int lakeLevel = land - 1;

        BlockState oil = ModBlocks.OIL.defaultBlockState();
        BlockState air = net.minecraft.world.level.block.Blocks.AIR.defaultBlockState();
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int x = minX; x < minX + 16; x++) {
            for (int z = minZ; z < minZ + 16; z++) {
                double nx = (x - deposit.lakeX()) / (double) deposit.lakeRadiusX();
                double nz = (z - deposit.lakeZ()) / (double) deposit.lakeRadiusZ();
                double angle = Math.atan2(nz, nx);
                double wobble = 1.0 + 0.15 * Math.sin(angle * 4 + ((deposit.noiseSeed() >> 16) & 0xFF))
                        + 0.08 * Math.sin(angle * 9 + ((deposit.noiseSeed() >> 24) & 0xFF));
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
