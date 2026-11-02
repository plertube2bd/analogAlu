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

@Mixin(ChunkGenerator.class)
public abstract class OilWorldgenMixin {
    private static final int MIN_OIL_Y = -19;
    private static final int SEA_LEVEL = 63;

    @Inject(method = "applyBiomeDecoration", at = @At("TAIL"))
    private void analog_alu$generateOil(WorldGenLevel level, ChunkAccess chunk, StructureManager structures,
            CallbackInfo callbackInfo) {
        ChunkPos chunkPos = chunk.getPos();
        RandomSource random = RandomSource.create(level.getSeed() ^ ChunkPos.pack(chunkPos.x(), chunkPos.z())
            * 0x9E3779B97F4A7C15L);
        if (random.nextInt(28) != 0) return;

        int x = chunkPos.getMinBlockX() + 3 + random.nextInt(10);
        int z = chunkPos.getMinBlockZ() + 3 + random.nextInt(10);
        int surfaceY = level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z);
        int highestOilY = Math.min(SEA_LEVEL - 1, surfaceY - 5);
        if (highestOilY < MIN_OIL_Y) return;

        int centerY = MIN_OIL_Y + random.nextInt(highestOilY - MIN_OIL_Y + 1);
        int radiusX = 5 + random.nextInt(5);
        int radiusZ = 5 + random.nextInt(5);
        int radiusY = 2 + random.nextInt(3);
        int placed = 0;
        BlockState oil = ModBlocks.OIL.defaultBlockState();
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();

        for (int dx = -radiusX; dx <= radiusX; dx++) {
            for (int dz = -radiusZ; dz <= radiusZ; dz++) {
                for (int dy = -radiusY; dy <= radiusY; dy++) {
                    double shape = square(dx / (double) radiusX) + square(dz / (double) radiusZ)
                            + square(dy / (double) radiusY);
                    if (shape > 1.0) continue;
                    int targetX = x + dx;
                    int targetY = centerY + dy;
                    int targetZ = z + dz;
                    if (targetY <= MIN_OIL_Y || targetY >= surfaceY - 3) continue;
                    cursor.set(targetX, targetY, targetZ);
                    if (!level.ensureCanWrite(cursor)) continue;
                    if (level.getBlockState(cursor).is(BlockTags.BASE_STONE_OVERWORLD)) {
                        level.setBlock(cursor, oil, 2);
                        placed++;
                    }
                }
            }
        }

        if (placed == 0 || random.nextInt(5) != 0) return;
        for (int targetY = centerY + radiusY; targetY < surfaceY; targetY++) {
            cursor.set(x, targetY, z);
            BlockState pathState = level.getBlockState(cursor);
            if (level.ensureCanWrite(cursor) && (pathState.is(BlockTags.BASE_STONE_OVERWORLD)
                    || pathState.is(BlockTags.DIRT))) {
                level.setBlock(cursor, oil, 2);
            }
        }
        for (int offsetX = -1; offsetX <= 1; offsetX++) {
            for (int offsetZ = -1; offsetZ <= 1; offsetZ++) {
                if (random.nextBoolean()) continue;
                int seepX = x + offsetX;
                int seepZ = z + offsetZ;
                int seepY = level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, seepX, seepZ);
                cursor.set(seepX, seepY, seepZ);
                if (level.ensureCanWrite(cursor) && level.getBlockState(cursor).isAir()) {
                    level.setBlock(cursor, oil, 2);
                }
            }
        }
    }

    private static double square(double value) {
        return value * value;
    }
}