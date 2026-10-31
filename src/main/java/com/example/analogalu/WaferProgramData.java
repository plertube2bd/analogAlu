package com.example.analogalu;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.inventory.AbstractFurnaceMenu;
import net.minecraft.world.inventory.InventoryMenu;

public final class WaferProgramData {
    public static final String CODE_KEY = "wafer_code";
    public static final String BYTECODE_KEY = "wafer_bytecode";
    public static final String HASH_KEY = "wafer_sha256";
    public static final String STATEMENTS_KEY = "wafer_statements";

    private WaferProgramData() {}

    public static CompoundTag get(ItemStack stack) {
        return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
    }

    public static boolean hasCompiledProgram(ItemStack stack) {
        return WaferProgramCompiler.isBytecode(get(stack).getStringOr(BYTECODE_KEY, ""));
    }

    public static void write(ItemStack stack, String source, String bytecode, String hash, int statements) {
        CompoundTag tag = get(stack);
        tag.putString(CODE_KEY, source);
        tag.putString(BYTECODE_KEY, bytecode);
        tag.putString(HASH_KEY, hash);
        tag.putInt(STATEMENTS_KEY, statements);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    public static void copyProgram(ItemStack source, ItemStack target) {
        CompoundTag sourceTag = get(source);
        if (!WaferProgramCompiler.isBytecode(sourceTag.getStringOr(BYTECODE_KEY, ""))) return;

        CompoundTag targetTag = get(target);
        targetTag.putString(CODE_KEY, sourceTag.getStringOr(CODE_KEY, ""));
        targetTag.putString(BYTECODE_KEY, sourceTag.getStringOr(BYTECODE_KEY, ""));
        targetTag.putString(HASH_KEY, sourceTag.getStringOr(HASH_KEY, ""));
        targetTag.putInt(STATEMENTS_KEY, sourceTag.getIntOr(STATEMENTS_KEY, 0));
        target.set(DataComponents.CUSTOM_DATA, CustomData.of(targetTag));
    }

    public static void copyFromMenu(Player player, ItemStack result) {
        if (player.containerMenu instanceof CraftingMenu) {
            int end = Math.min(10, player.containerMenu.slots.size());
            for (int index = 1; index < end; index++) {
                copyFromSlot(player.containerMenu.getSlot(index), result);
                if (hasCompiledProgram(result)) return;
            }
        } else if (player.containerMenu instanceof InventoryMenu) {
            int end = Math.min(5, player.containerMenu.slots.size());
            for (int index = 1; index < end; index++) {
                copyFromSlot(player.containerMenu.getSlot(index), result);
                if (hasCompiledProgram(result)) return;
            }
        } else if (player.containerMenu instanceof AbstractFurnaceMenu && !player.containerMenu.slots.isEmpty()) {
            copyFromSlot(player.containerMenu.getSlot(0), result);
        }
    }

    private static void copyFromSlot(Slot slot, ItemStack result) {
        ItemStack ingredient = slot.getItem();
        if (!ingredient.isEmpty() && ingredient != result) copyProgram(ingredient, result);
    }
}
