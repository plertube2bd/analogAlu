package com.example.redstonetransportation.machine;

import com.example.redstonetransportation.content.ModContent;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/** The land leveler's storage, its fuel slot, and the start and rubble buttons. */
public final class LandLevelerMenu extends AbstractContainerMenu {
    public static final int GRID_LEFT = 8;
    public static final int GRID_TOP = 18;
    public static final int FUEL_LEFT = 8;
    public static final int FILL_LEFT = 36;
    public static final int LIGHTER_LEFT = 54;
    public static final int CONTROL_TOP = 76;
    public static final int INVENTORY_TOP = 120;
    private static final int MACHINE_SLOTS = LandLevelerBlockEntity.SLOTS;

    private final Container container;
    private final ContainerData data;
    private final LandLevelerBlockEntity.Link link;

    public LandLevelerMenu(int containerId, Inventory inventory) {
        this(containerId, inventory, new SimpleContainer(MACHINE_SLOTS),
                new SimpleContainerData(LandLevelerBlockEntity.DATA_COUNT), null);
    }

    public LandLevelerMenu(int containerId, Inventory inventory, LandLevelerBlockEntity.Link link) {
        this(containerId, inventory, link, link.data(), link);
    }

    private LandLevelerMenu(int containerId, Inventory inventory, Container container, ContainerData data,
                            LandLevelerBlockEntity.Link link) {
        super(ModContent.LAND_LEVELER_MENU, containerId);
        checkContainerSize(container, MACHINE_SLOTS);
        checkContainerDataCount(data, LandLevelerBlockEntity.DATA_COUNT);
        this.container = container;
        this.data = data;
        this.link = link;
        container.startOpen(inventory.player);
        addSlot(new Slot(container, LandLevelerBlockEntity.FUEL_SLOT, FUEL_LEFT, CONTROL_TOP) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return LandLevelerBlockEntity.isFuel(stack);
            }
        });
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new Slot(container, LandLevelerBlockEntity.FIRST_STORAGE + column + row * 9,
                        GRID_LEFT + column * 18, GRID_TOP + row * 18));
            }
        }
        addSlot(new Slot(container, LandLevelerBlockEntity.FILL_SLOT, FILL_LEFT, CONTROL_TOP) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return LandLevelerBlockEntity.isFillBlock(stack);
            }
        });
        addSlot(new Slot(container, LandLevelerBlockEntity.LIGHTER_SLOT, LIGHTER_LEFT, CONTROL_TOP) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return LandLevelerBlockEntity.isLighter(stack);
            }

            @Override
            public int getMaxStackSize() {
                return 1;
            }
        });
        addStandardInventorySlots(inventory, GRID_LEFT, INVENTORY_TOP);
        addDataSlots(data);
    }

    public boolean isRunning() {
        return data.get(LandLevelerBlockEntity.DATA_RUNNING) != 0;
    }

    /** A flint and steel in its slot: what it digs up is burned. */
    public boolean isBurning() {
        return LandLevelerBlockEntity.isLighter(getSlot(LandLevelerBlockEntity.LIGHTER_SLOT).getItem());
    }

    public boolean discardsRubble() {
        return data.get(LandLevelerBlockEntity.DATA_DISCARD) != 0;
    }

    public LandLevelerBlockEntity.Problem problem() {
        int index = data.get(LandLevelerBlockEntity.DATA_PROBLEM);
        LandLevelerBlockEntity.Problem[] problems = LandLevelerBlockEntity.Problem.values();
        return index >= 0 && index < problems.length ? problems[index] : LandLevelerBlockEntity.Problem.NONE;
    }

    /** A chest, barrel or shulker box rides on top as more storage. */
    public boolean hasCargo() {
        return data.get(LandLevelerBlockEntity.DATA_CARGO) != 0;
    }

    public int travelled() {
        return data.get(LandLevelerBlockEntity.DATA_TRAVELLED);
    }

    /** How much of the current piece of fuel is left, 0 to 1. */
    public float fuelLeft() {
        int total = data.get(LandLevelerBlockEntity.DATA_BURN_TOTAL);
        return total <= 0 ? 0.0F : Math.clamp(data.get(LandLevelerBlockEntity.DATA_BURN_LEFT) / (float) total, 0.0F, 1.0F);
    }

    @Override
    public boolean clickMenuButton(Player player, int button) {
        if (link == null || (button != LandLevelerBlockEntity.BUTTON_RUN && button != LandLevelerBlockEntity.BUTTON_DISCARD)) {
            return false;
        }
        link.press(button);
        return true;
    }

    @Override
    public boolean stillValid(Player player) {
        return container.stillValid(player);
    }

    /** Fuel, a paving block and a flint and steel go to their own slots, anything else to storage; out of the machine it goes to the player. */
    @Override
    public ItemStack quickMoveStack(Player player, int slotIndex) {
        Slot slot = slots.get(slotIndex);
        if (!slot.hasItem()) return ItemStack.EMPTY;

        ItemStack stack = slot.getItem();
        ItemStack moved = stack.copy();
        if (slotIndex < MACHINE_SLOTS) {
            if (!moveItemStackTo(stack, MACHINE_SLOTS, slots.size(), true)) return ItemStack.EMPTY;
        } else if (LandLevelerBlockEntity.isLighter(stack)
                && moveItemStackTo(stack, LandLevelerBlockEntity.LIGHTER_SLOT, LandLevelerBlockEntity.LIGHTER_SLOT + 1, false)) {
            // Moved into the lighter slot.
        } else if (LandLevelerBlockEntity.isFuel(stack) && moveItemStackTo(stack, LandLevelerBlockEntity.FUEL_SLOT, LandLevelerBlockEntity.FUEL_SLOT + 1, false)) {
            // Moved into the fuel slot.
        } else if (LandLevelerBlockEntity.isFillBlock(stack)
                && moveItemStackTo(stack, LandLevelerBlockEntity.FILL_SLOT, LandLevelerBlockEntity.FILL_SLOT + 1, false)) {
            // Moved into the fill slot.
        } else if (!moveItemStackTo(stack, LandLevelerBlockEntity.FIRST_STORAGE, LandLevelerBlockEntity.STORAGE_END, false)) {
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
