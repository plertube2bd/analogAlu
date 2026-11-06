package com.example.redstonetransportation.furnace;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.BlastFurnaceMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.FurnaceMenu;
import net.minecraft.world.inventory.SmokerMenu;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.item.crafting.RecipeType;

/** The three vanilla cookers that have upgraded versions: same recipes and same menus, just faster. */
public enum FurnaceKind {
    FURNACE("upgraded_furnace", RecipeType.SMELTING, SoundEvents.FURNACE_FIRE_CRACKLE),
    BLAST_FURNACE("upgraded_blast_furnace", RecipeType.BLASTING, SoundEvents.BLASTFURNACE_FIRE_CRACKLE),
    SMOKER("upgraded_smoker", RecipeType.SMOKING, SoundEvents.SMOKER_SMOKE);

    private final String id;
    private final RecipeType<? extends AbstractCookingRecipe> recipes;
    private final SoundEvent sound;

    FurnaceKind(String id, RecipeType<? extends AbstractCookingRecipe> recipes, SoundEvent sound) {
        this.id = id;
        this.recipes = recipes;
        this.sound = sound;
    }

    public String id(int tier) {
        return id + "_" + tier;
    }

    public RecipeType<? extends AbstractCookingRecipe> recipes() {
        return recipes;
    }

    public SoundEvent sound() {
        return sound;
    }

    AbstractContainerMenu createMenu(int id, Inventory inventory, Container container, ContainerData data) {
        return switch (this) {
            case FURNACE -> new FurnaceMenu(id, inventory, container, data);
            case BLAST_FURNACE -> new BlastFurnaceMenu(id, inventory, container, data);
            case SMOKER -> new SmokerMenu(id, inventory, container, data);
        };
    }
}
