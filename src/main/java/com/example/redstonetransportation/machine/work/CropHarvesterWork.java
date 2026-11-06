package com.example.redstonetransportation.machine.work;

import com.example.redstonetransportation.machine.MachineBlockEntity;
import com.example.redstonetransportation.machine.MachineWork;
import com.example.redstonetransportation.machine.WorkArea;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CocoaBlock;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.NetherWartBlock;
import net.minecraft.world.level.block.SweetBerryBushBlock;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;

/** Harvests ripe crops in the work area and leaves each one replanted, so a field keeps itself going. */
public final class CropHarvesterWork implements MachineWork {
    private static final ItemStack TOOL = new ItemStack(Items.DIAMOND_HOE);

    @Override
    public Result run(MachineBlockEntity machine, ServerLevel level) {
        if (!machine.hasRoom()) return Result.BLOCKED;

        int machineY = machine.getBlockPos().getY();
        for (int y = machineY - 1; y <= machineY + 1; y++) {
            for (BlockPos pos : WorkArea.layer(machine, y)) {
                if (harvest(machine, level, pos)) return Result.WORKED;
            }
        }
        return Result.IDLE;
    }

    private static boolean harvest(MachineBlockEntity machine, ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        Block block = state.getBlock();

        if (block instanceof CropBlock crop) {
            return crop.isMaxAge(state) && replant(machine, level, pos, state, crop.getStateForAge(0));
        }
        if (block instanceof NetherWartBlock) {
            return state.getValue(NetherWartBlock.AGE) == NetherWartBlock.MAX_AGE
                    && replant(machine, level, pos, state, state.setValue(NetherWartBlock.AGE, 0));
        }
        if (block instanceof SweetBerryBushBlock) {
            return state.getValue(SweetBerryBushBlock.AGE) == SweetBerryBushBlock.MAX_AGE
                    && replant(machine, level, pos, state, state.setValue(SweetBerryBushBlock.AGE, 1));
        }
        if (block instanceof CocoaBlock) {
            return state.getValue(CocoaBlock.AGE) == CocoaBlock.MAX_AGE
                    && replant(machine, level, pos, state, state.setValue(CocoaBlock.AGE, 0));
        }
        if (state.is(Blocks.PUMPKIN) || state.is(Blocks.MELON)) {
            return cut(machine, level, pos, state);
        }
        // Tall plants keep their bottom block, which regrows the stalk.
        if (state.is(Blocks.SUGAR_CANE) || state.is(Blocks.CACTUS) || state.is(Blocks.BAMBOO)) {
            return level.getBlockState(pos.below()).is(block) && cutStalk(machine, level, pos, block);
        }
        return false;
    }

    private static boolean cut(MachineBlockEntity machine, ServerLevel level, BlockPos pos, BlockState state) {
        List<ItemStack> drops = Block.getDrops(state, level, pos, null, null, TOOL);
        level.destroyBlock(pos, false);
        machine.store(level, drops);
        return true;
    }

    private static boolean cutStalk(MachineBlockEntity machine, ServerLevel level, BlockPos pos, Block block) {
        BlockPos top = pos;
        while (level.getBlockState(top.above()).is(block)) top = top.above();
        return cut(machine, level, top, level.getBlockState(top));
    }

    private static boolean replant(MachineBlockEntity machine, ServerLevel level, BlockPos pos,
                                   BlockState ripe, BlockState young) {
        List<ItemStack> drops = new ArrayList<>(Block.getDrops(ripe, level, pos, null, null, TOOL));
        // The replanted crop pays for itself with one of its own seeds.
        ItemStack seed = young.getBlock().asItem().getDefaultInstance();
        for (ItemStack drop : drops) {
            if (drop.is(seed.getItem())) {
                drop.shrink(1);
                break;
            }
        }
        level.setBlock(pos, young, Block.UPDATE_ALL);
        level.levelEvent(2001, pos, Block.getId(ripe));
        machine.store(level, drops.stream().filter(stack -> !stack.isEmpty()).toList());
        return true;
    }
}
