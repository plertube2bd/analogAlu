package com.example.redstonetransportation.machine;

import net.minecraft.core.Direction;
import net.minecraft.world.Container;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.HopperBlockEntity;

import java.util.function.Predicate;

/** Moves one stack's worth of items between containers, honouring their faces and filters. */
public final class ItemTransfer {
    private ItemTransfer() {}

    /** Returns true if at least one item moved. */
    public static boolean move(Container from, Container to, Direction intoFace, int limit) {
        return move(from, to, null, intoFace, limit, stack -> true);
    }

    public static boolean move(Container from, Container to, Direction outOfFace, Direction intoFace,
                               int limit, Predicate<ItemStack> filter) {
        for (int slot = 0; slot < from.getContainerSize(); slot++) {
            ItemStack stack = from.getItem(slot);
            if (stack.isEmpty() || !filter.test(stack)) continue;
            if (outOfFace != null && from instanceof WorldlyContainer worldly
                    && !worldly.canTakeItemThroughFace(slot, stack, outOfFace)) continue;

            ItemStack offer = stack.copyWithCount(Math.min(limit, stack.getCount()));
            int offered = offer.getCount();
            int moved = offered - HopperBlockEntity.addItem(from, to, offer, intoFace).getCount();
            if (moved > 0) {
                from.removeItem(slot, moved);
                from.setChanged();
                return true;
            }
        }
        return false;
    }
}
