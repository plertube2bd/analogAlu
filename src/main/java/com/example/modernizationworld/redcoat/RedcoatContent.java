
package com.example.modernizationworld.redcoat;

import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.equipment.Equippable;
import net.minecraft.world.item.equipment.EquipmentAsset;
import net.minecraft.world.item.equipment.EquipmentAssets;

import java.util.List;
import java.util.function.Function;

/** Registry for the British raid's effect, bottle and spawn eggs. */
public final class RedcoatContent {
    public static final Holder<MobEffect> REDCOAT_OMEN = Registry.registerForHolder(
            BuiltInRegistries.MOB_EFFECT, id("redcoat_omen"), new RedcoatOmenEffect());

    public static final Item REDCOAT_OMEN_BOTTLE = register("redcoat_omen_bottle",
            properties -> new RedcoatOmenBottleItem(properties.stacksTo(1)));

    public static final Item MUSKETEER_SPAWN_EGG = egg("redcoat_musketeer_spawn_egg", RedcoatEntities.MUSKETEER, 0x8E1717, 0xE7D9B7);
    public static final Item GRENADIER_SPAWN_EGG = egg("redcoat_grenadier_spawn_egg", RedcoatEntities.GRENADIER, 0x791414, 0xD7C28D);
    public static final Item SERGEANT_SPAWN_EGG = egg("redcoat_sergeant_spawn_egg", RedcoatEntities.SERGEANT, 0xA11E1E, 0x252525);
    public static final Item OFFICER_SPAWN_EGG = egg("redcoat_officer_spawn_egg", RedcoatEntities.OFFICER, 0x8C1717, 0x1E283B);
    public static final Item RIFLEMAN_SPAWN_EGG = egg("redcoat_rifleman_spawn_egg", RedcoatEntities.RIFLEMAN, 0x344B2A, 0x8B9B5D);
    public static final Item DRUMMER_SPAWN_EGG = egg("redcoat_drummer_spawn_egg", RedcoatEntities.DRUMMER, 0x9C1B1B, 0xC5A24D);
    public static final Item DRAGOON_SPAWN_EGG = egg("redcoat_dragoon_spawn_egg", RedcoatEntities.DRAGOON, 0x8E1717, 0xD6D6D6);

    public static final Item SHAKO = hat("redcoat_shako");
    public static final Item GRENADIER_MITRE = hat("redcoat_mitre");
    public static final Item SERGEANT_SHAKO = hat("redcoat_sergeant_shako");
    public static final Item OFFICER_BICORNE = hat("redcoat_bicorne");

    private static final List<Item> HEADGEAR = List.of(SHAKO, GRENADIER_MITRE, SERGEANT_SHAKO, OFFICER_BICORNE);
    private static final List<Item> EGGS = List.of(MUSKETEER_SPAWN_EGG, GRENADIER_SPAWN_EGG, SERGEANT_SPAWN_EGG,
            OFFICER_SPAWN_EGG, RIFLEMAN_SPAWN_EGG, DRUMMER_SPAWN_EGG, DRAGOON_SPAWN_EGG);

    private RedcoatContent() {}

    public static void init() {
        RedcoatEntities.init();
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.COMBAT).register(entries -> {
            entries.accept(REDCOAT_OMEN_BOTTLE);
            EGGS.forEach(entries::accept);
            HEADGEAR.forEach(entries::accept);
        });
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.SPAWN_EGGS).register(entries ->
                EGGS.forEach(entries::accept));
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.TOOLS_AND_UTILITIES).register(entries ->
                HEADGEAR.forEach(entries::accept));
    }

    private static Item egg(String path, net.minecraft.world.entity.EntityType<? extends net.minecraft.world.entity.Mob> type,
                            int primary, int secondary) {
        return register(path, properties -> new SpawnEggItem(properties.spawnEgg(type)));
    }

    private static Item hat(String path) {
        ResourceKey<EquipmentAsset> asset = ResourceKey.create(EquipmentAssets.ROOT_ID, id(path));
        return register(path, properties -> new Item(properties.stacksTo(1).component(
                DataComponents.EQUIPPABLE, Equippable.builder(EquipmentSlot.HEAD)
                        .setEquipSound(SoundEvents.ARMOR_EQUIP_LEATHER).setAsset(asset).build())));
    }

    private static Item register(String path, Function<Item.Properties, Item> factory) {
        ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, id(path));
        return Registry.register(BuiltInRegistries.ITEM, key, factory.apply(new Item.Properties().setId(key)));
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath("modernization_world", path);
    }
}
