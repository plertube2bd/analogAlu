package com.example.analogalu.block;

import com.example.analogalu.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.Container;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.block.entity.Hopper;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.HopperMenu;

public class UpwardHopperBlockEntity extends RandomizableContainerBlockEntity implements Hopper, WorldlyContainer {
    private static final int[] SLOTS = {0, 1, 2, 3, 4};
    private NonNullList<ItemStack> items = NonNullList.withSize(5, ItemStack.EMPTY);
    private int transferCooldown;

    public UpwardHopperBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlocks.UPWARD_HOPPER_ENTITY, pos, state);
    }

    public int getContainerSize() {
        return items.size();
    }

    @Override
    protected NonNullList<ItemStack> getItems() {
        return items;
    }

    @Override
    protected void setItems(NonNullList<ItemStack> items) {
        this.items = items;
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.hopper");
    }

    @Override
    protected AbstractContainerMenu createMenu(int syncId, Inventory inventory) {
        return new HopperMenu(syncId, inventory, this);
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return SLOTS;
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, Direction side) {
        return canPlaceItem(slot, stack);
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return side == Direction.DOWN;
    }

    @Override
    public double getLevelX() {
        return worldPosition.getX() + 0.5;
    }

    @Override
    public double getLevelY() {
        return worldPosition.getY() - 1.5;
    }

    @Override
    public double getLevelZ() {
        return worldPosition.getZ() + 0.5;
    }

    @Override
    public boolean isGridAligned() {
        return true;
    }

    @Override
    public AABB getSuckAabb() {
        return Hopper.SUCK_AABB.move(0.0, 1.0, 0.0);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, UpwardHopperBlockEntity blockEntity) {
        if (blockEntity.transferCooldown > 0) {
            blockEntity.transferCooldown--;
            return;
        }

        boolean moved = HopperBlockEntity.suckInItems(level, blockEntity);
        Container destination = HopperBlockEntity.getContainerAt(level, pos.above());
        if (destination == null) {
            for (int slot = 0; slot < blockEntity.getContainerSize(); slot++) {
                ItemStack stack = blockEntity.getItem(slot);
                if (stack.isEmpty()) continue;
                ItemStack output = stack.split(1);
                level.addFreshEntity(new ItemEntity(level, pos.getX() + 0.5, pos.getY() + 1.0,
                        pos.getZ() + 0.5, output));
                blockEntity.setItem(slot, stack);
                moved = true;
                break;
            }
        } else {
            for (int slot = 0; slot < blockEntity.getContainerSize(); slot++) {
                ItemStack stack = blockEntity.getItem(slot);
                if (stack.isEmpty()) continue;
                int previousCount = stack.getCount();
                ItemStack remainder = HopperBlockEntity.addItem(blockEntity, destination, stack, Direction.DOWN);
                blockEntity.setItem(slot, remainder);
                if (remainder.getCount() < previousCount) {
                    moved = true;
                    break;
                }
            }
        }

        if (moved) {
            blockEntity.transferCooldown = 8;
            blockEntity.setChanged();
        }
    }
}