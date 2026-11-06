package com.example.redstonetransportation.machine.work;

import com.example.redstonetransportation.machine.MachineBlockEntity;
import com.example.redstonetransportation.machine.MachineWork;
import com.example.redstonetransportation.machine.WorkArea;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.BoneMealItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.state.BlockState;

/** Spends bone meal from its buffer on saplings and crops in the work area. */
public final class GrowthBoosterWork implements MachineWork {
    @Override
    public Result run(MachineBlockEntity machine, ServerLevel level) {
        ItemStack boneMeal = firstBoneMeal(machine);
        if (boneMeal == null) return Result.BLOCKED;

        int machineY = machine.getBlockPos().getY();
        for (int y = machineY - 1; y <= machineY + 1; y++) {
            for (BlockPos pos : WorkArea.layer(machine, y)) {
                if (isGrowing(level, pos) && BoneMealItem.growCrop(boneMeal, level, pos)) {
                    level.levelEvent(1505, pos, 15);
                    machine.setChanged();
                    return Result.WORKED;
                }
            }
        }
        return Result.IDLE;
    }

    private static ItemStack firstBoneMeal(MachineBlockEntity machine) {
        for (int slot = 0; slot < machine.getContainerSize(); slot++) {
            ItemStack stack = machine.getItem(slot);
            if (stack.is(Items.BONE_MEAL)) return stack;
        }
        return null;
    }

    /** Skips grass and other spreading blocks, which would otherwise burn all the bone meal. */
    private static boolean isGrowing(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        return (state.is(BlockTags.SAPLINGS) || state.is(BlockTags.CROPS))
                && state.getBlock() instanceof BonemealableBlock;
    }
}
