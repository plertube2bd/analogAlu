package com.example.redstonetransportation.vehicle;

import com.example.redstonetransportation.content.ModContent;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/** The cargo hold: four rows of eight, which none of the vanilla chest menus can lay out. */
public final class CargoTramMenu extends AbstractContainerMenu {
    public static final int COLUMNS = 8;
    public static final int ROWS = 4;
    public static final int SLOTS = COLUMNS * ROWS;
    private static final int GRID_LEFT = 17;
    private static final int GRID_TOP = 18;
    private static final int INVENTORY_LEFT = 8;
    private static final int INVENTORY_TOP = GRID_TOP + ROWS * 18 + 13;

    private final Container container;

    public CargoTramMenu(int containerId, Inventory inventory) {
        this(containerId, inventory, new SimpleContainer(SLOTS));
    }

    public CargoTramMenu(int containerId, Inventory inventory, Container container) {
        super(ModContent.CARGO_TRAM_MENU, containerId);
        checkContainerSize(container, SLOTS);
        this.container = container;
        container.startOpen(inventory.player);
        for (int row = 0; row < ROWS; row++) {
            for (int column = 0; column < COLUMNS; column++) {
                addSlot(new Slot(container, column + row * COLUMNS, GRID_LEFT + column * 18, GRID_TOP + row * 18));
            }
        }
        addStandardInventorySlots(inventory, INVENTORY_LEFT, INVENTORY_TOP);
    }

    @Override
    public boolean stillValid(Player player) {
        return container.stillValid(player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int slotIndex) {
        Slot slot = slots.get(slotIndex);
        if (!slot.hasItem()) return ItemStack.EMPTY;

        ItemStack stack = slot.getItem();
        ItemStack moved = stack.copy();
        boolean fromHold = slotIndex < SLOTS;
        if (!moveItemStackTo(stack, fromHold ? SLOTS : 0, fromHold ? slots.size() : SLOTS, fromHold)) {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        return moved;
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        container.stopOpen(player);
    }
}
