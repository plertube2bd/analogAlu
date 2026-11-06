package com.example.redstonetransportation.furnace;

import com.example.redstonetransportation.content.ModContent;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Runs the vanilla furnace logic several times per tick. Burning and cooking speed up together, so
 * fuel per item stays the same until {@link #getBurnDuration} stretches it.
 */
public final class UpgradedFurnaceBlockEntity extends AbstractFurnaceBlockEntity {
    public UpgradedFurnaceBlockEntity(BlockPos pos, BlockState state) {
        super(ModContent.UPGRADED_FURNACE_ENTITY, pos, state, block(state).kind().recipes());
    }

    private static UpgradedFurnaceBlock block(BlockState state) {
        return (UpgradedFurnaceBlock) state.getBlock();
    }

    void tick(ServerLevel level, BlockPos pos, BlockState state) {
        if (state.getValue(UpgradedFurnaceBlock.POWERED)) return;
        for (int step = block(state).tier().speed(); step > 0; step--) {
            serverTick(level, pos, level.getBlockState(pos), this);
        }
    }

    @Override
    protected int getBurnDuration(ServerLevel level, ItemStack fuelItem) {
        return Math.round(super.getBurnDuration(level, fuelItem) * block(getBlockState()).tier().fuelValue());
    }

    @Override
    protected Component getDefaultName() {
        return getBlockState().getBlock().getName();
    }

    @Override
    protected AbstractContainerMenu createMenu(int id, Inventory inventory) {
        return block(getBlockState()).kind().createMenu(id, inventory, this, dataAccess);
    }
}
