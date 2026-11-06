package com.example.redstonetransportation.machine.work;

import com.example.redstonetransportation.content.ModContent;
import com.example.redstonetransportation.machine.MachineBlockEntity;
import com.example.redstonetransportation.machine.MachineType;
import com.example.redstonetransportation.machine.MachineWork;
import com.example.redstonetransportation.machine.WorkArea;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.Mth;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.NetherWartBlock;
import net.minecraft.world.level.block.SaplingBlock;
import net.minecraft.world.level.block.StemBlock;
import net.minecraft.world.level.block.state.BlockState;

import java.util.function.Predicate;

/**
 * Plants saplings and crops from its buffer on free soil in the work area. A tree cutter only
 * reaches a trunk touching it, so with one nearby, saplings go on the spots touching it.
 */
public final class PlanterWork implements MachineWork {
    /** Saplings go on a 3-block grid so each grown tree has room for its canopy. */
    private static final int SAPLING_SPACING = 3;

    public static boolean isPlantable(ItemStack stack) {
        return stack.getItem() instanceof BlockItem item && isPlant(item.getBlock());
    }

    private static boolean isPlant(Block block) {
        return block instanceof SaplingBlock || block instanceof CropBlock
                || block instanceof StemBlock || block instanceof NetherWartBlock;
    }

    @Override
    public Result run(MachineBlockEntity machine, ServerLevel level) {
        ItemStack seed = firstPlantable(machine);
        if (seed == null) return Result.BLOCKED;

        BlockState plant = ((BlockItem) seed.getItem()).getBlock().defaultBlockState();
        Predicate<BlockPos> spots = plantingSpots(machine, level, seed);
        int machineY = machine.getBlockPos().getY();
        for (int y = machineY - 1; y <= machineY + 1; y++) {
            for (BlockPos pos : WorkArea.layer(machine, y)) {
                if (!level.getBlockState(pos).isAir() || !plant.canSurvive(level, pos) || !spots.test(pos)) continue;
                level.setBlock(pos, plant, Block.UPDATE_ALL);
                level.playSound(null, pos, SoundEvents.CROP_PLANTED, SoundSource.BLOCKS, 1.0F, 1.0F);
                seed.shrink(1);
                machine.setChanged();
                return Result.WORKED;
            }
        }
        return Result.IDLE;
    }

    private static ItemStack firstPlantable(MachineBlockEntity machine) {
        for (int slot = 0; slot < machine.getContainerSize(); slot++) {
            ItemStack stack = machine.getItem(slot);
            if (isPlantable(stack)) return stack;
        }
        return null;
    }

    private static Predicate<BlockPos> plantingSpots(MachineBlockEntity machine, ServerLevel level, ItemStack seed) {
        if (!seed.is(ItemTags.SAPLINGS)) return pos -> true;
        if (hasCutterNearby(machine, level)) return pos -> touchesCutter(level, pos);
        return PlanterWork::onSaplingGrid;
    }

    /** One block past the work area, because a cutter stands in the row behind the spot it fells. */
    private static boolean hasCutterNearby(MachineBlockEntity machine, ServerLevel level) {
        BlockPos center = WorkArea.center(machine);
        int reach = WorkArea.RADIUS + 1;
        int y = machine.getBlockPos().getY();
        BlockPos from = new BlockPos(center.getX() - reach, y - 1, center.getZ() - reach);
        BlockPos to = new BlockPos(center.getX() + reach, y + 1, center.getZ() + reach);
        for (BlockPos pos : BlockPos.betweenClosed(from, to)) {
            if (isCutter(level.getBlockState(pos))) return true;
        }
        return false;
    }

    private static boolean touchesCutter(ServerLevel level, BlockPos pos) {
        for (Direction side : Direction.Plane.HORIZONTAL) {
            if (isCutter(level.getBlockState(pos.relative(side)))) return true;
        }
        return false;
    }

    private static boolean isCutter(BlockState state) {
        return state.is(ModContent.MACHINES.get(MachineType.TREE_CUTTER));
    }

    private static boolean onSaplingGrid(BlockPos pos) {
        return Mth.positiveModulo(pos.getX(), SAPLING_SPACING) == 0 && Mth.positiveModulo(pos.getZ(), SAPLING_SPACING) == 0;
    }
}
