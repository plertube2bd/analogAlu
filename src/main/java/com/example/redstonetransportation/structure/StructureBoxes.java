package com.example.redstonetransportation.structure;

import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import java.util.function.Consumer;

/**
 * The collision boxes of a structure in world space. The game's collision only knows boxes lined
 * up with the world, so a turned block is cut into thin strips along X. That leaves a deck exactly
 * flat, and a slanted wall within an eighth of a block of what is drawn.
 */
final class StructureBoxes {
    private static final int STRIPS = 8;

    private StructureBoxes() {}

    /** Hands {@code output} every box of every block near {@code area}. */
    static void near(StructureBlocks blocks, StructurePose pose, AABB area, Consumer<AABB> output) {
        double centerX = (area.minX + area.maxX) / 2.0D;
        double centerZ = (area.minZ + area.maxZ) / 2.0D;
        double spread = Math.max(area.getXsize(), area.getZsize()) * Mth.SQRT_OF_TWO / 2.0D + 1.0D;
        double reach = (blocks.reach() + 1.0D) * Mth.SQRT_OF_TWO + spread;
        double offsetX = centerX - pose.position().x;
        double offsetZ = centerZ - pose.position().z;
        if (offsetX * offsetX + offsetZ * offsetZ > reach * reach) return;

        double localX = pose.localX(centerX, centerZ);
        double localZ = pose.localZ(centerX, centerZ);
        boolean square = pose.isSquare();
        int quarters = pose.quarters();
        int minY = Math.max(blocks.minY(), Mth.floor(area.minY - pose.position().y) - 1);
        int maxY = Math.min(blocks.maxY(), Mth.floor(area.maxY - pose.position().y));
        for (int x = Mth.floor(localX - spread); x <= Mth.ceil(localX + spread); x++) {
            for (int z = Mth.floor(localZ - spread); z <= Mth.ceil(localZ + spread); z++) {
                for (int y = minY; y <= maxY; y++) {
                    BlockState state = blocks.at(x, y, z);
                    if (state == null) continue;
                    double worldX = pose.worldX(x, z);
                    double worldZ = pose.worldZ(x, z);
                    double worldY = pose.position().y + y;
                    for (AABB box : blocks.collisionBoxes(state)) {
                        if (square) {
                            output.accept(quarterTurned(box, quarters).move(worldX - 0.5D, worldY, worldZ - 0.5D));
                        } else {
                            strips(box, pose, worldX, worldY, worldZ, area, output);
                        }
                    }
                }
            }
        }
    }

    static boolean overlap(StructureBlocks blocks, StructurePose pose, AABB area) {
        AABB inner = area.deflate(0.001D);
        boolean[] found = {false};
        near(blocks, pose, area, box -> found[0] |= box.intersects(inner));
        return found[0];
    }

    private static AABB quarterTurned(AABB box, int quarters) {
        return switch (quarters) {
            case 1 -> new AABB(1.0D - box.maxZ, box.minY, box.minX, 1.0D - box.minZ, box.maxY, box.maxX);
            case 2 -> new AABB(1.0D - box.maxX, box.minY, 1.0D - box.maxZ, 1.0D - box.minX, box.maxY, 1.0D - box.minZ);
            case 3 -> new AABB(box.minZ, box.minY, 1.0D - box.maxX, box.maxZ, box.maxY, 1.0D - box.minX);
            default -> box;
        };
    }

    private static void strips(AABB box, StructurePose pose, double cellX, double cellY, double cellZ, AABB area, Consumer<AABB> output) {
        double[] cornerX = {box.minX, box.maxX, box.maxX, box.minX};
        double[] cornerZ = {box.minZ, box.minZ, box.maxZ, box.maxZ};
        double[] xs = new double[4];
        double[] zs = new double[4];
        double left = Double.MAX_VALUE;
        double right = -Double.MAX_VALUE;
        for (int i = 0; i < 4; i++) {
            double x = cornerX[i] - 0.5D;
            double z = cornerZ[i] - 0.5D;
            xs[i] = cellX + x * pose.cos() - z * pose.sin();
            zs[i] = cellZ + x * pose.sin() + z * pose.cos();
            left = Math.min(left, xs[i]);
            right = Math.max(right, xs[i]);
        }

        double width = (right - left) / STRIPS;
        for (int strip = 0; strip < STRIPS; strip++) {
            double from = left + strip * width;
            double to = from + width;
            if (to < area.minX || from > area.maxX) continue;
            double near = Double.MAX_VALUE;
            double far = -Double.MAX_VALUE;
            for (int i = 0; i < 4; i++) {
                int next = (i + 1) % 4;
                double low = Math.min(xs[i], xs[next]);
                double high = Math.max(xs[i], xs[next]);
                if (high < from || low > to) continue;
                double run = xs[next] - xs[i];
                for (double x : new double[]{Math.max(low, from), Math.min(high, to)}) {
                    double z = run == 0.0D ? zs[i] : zs[i] + (x - xs[i]) / run * (zs[next] - zs[i]);
                    near = Math.min(near, z);
                    far = Math.max(far, z);
                }
                if (run == 0.0D) {
                    near = Math.min(near, zs[next]);
                    far = Math.max(far, zs[next]);
                }
            }
            if (near < far) output.accept(new AABB(from, cellY + box.minY, near, to, cellY + box.maxY, far));
        }
    }
}
