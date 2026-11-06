package com.example.analogalu;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.item.equipment.EquipmentAsset;
import net.minecraft.world.item.equipment.EquipmentAssets;
import net.minecraft.world.item.equipment.Equippable;
import net.minecraft.world.item.Item;
import java.util.function.Function;

public final class ModItems {
    public static final List<Item> ITEMS = new ArrayList<>();

    public static final Item SILICON_BOULE = register("silicon_boule");
    public static final Item SILICON = register("silicon");
    public static final Item SILICON_PURIFICATION_CHARGE = register("silicon_purification_charge");
    public static final Item PURIFIED_SILICON = register("purified_silicon");
    public static final Item SEEDED_SILICON = register("seeded_silicon");
    public static final Item MONOCRYSTALLINE_SILICON = register("monocrystalline_silicon");
    public static final Item WAFER_CUTTING_WIRE = register("wafer_cutting_wire");
    public static final Item SILICON_WAFER = register("silicon_wafer", SiliconWaferItem::new);
    public static final Item PROGRAMMED_SILICON_WAFER = register("programmed_silicon_wafer", SiliconWaferItem::new);
    public static final Item QUARTZ_TREATED_WAFER = register("quartz_treated_wafer");
    public static final Item ION_IMPLANTED_SILICON = register("ion_implanted_silicon");
    public static final Item STRONG_ION_GAS = register("strong_ion_gas");
    public static final Item MASS_SELECTED_SILICON = register("mass_selected_silicon");
    public static final Item ACCELERATED_FOCUSED_SILICON = register("accelerated_focused_silicon");
    public static final Item INACTIVE_DOPED_WAFER = register("inactive_doped_wafer");
    public static final Item SEMICONDUCTOR = register("semiconductor", SemiconductorItem::new);
    /** 석유. 빈 양동이로 석유 원천을 우클릭하면 얻는다(양동이는 그대로 남는다). */
    public static final Item OIL = register("oil");
            public static final Item SOLID_OIL = register("solid_oil");
            public static final Item DIESEL = register("diesel");
            /** Took the place of plastic resin in 0.2.2; old resin turns into it at a resin converter. */
            public static final Item NAPHTHA = register("naphtha");
            public static final Item ASPHALT = register("asphalt");

    // Petrochemicals, from naphtha up to fibres, made in the chemical reactor and chlorine generator.
    public static final Item ETHYLENE = register("ethylene");
    public static final Item PROPYLENE = register("propylene");
    public static final Item C4_FRACTION = register("c4_fraction");
    public static final Item PYROLYSIS_GASOLINE = register("pyrolysis_gasoline");
    public static final Item BENZENE = register("benzene");
    public static final Item P_XYLENE = register("p_xylene");
    public static final Item HYDROGEN = register("hydrogen");
    public static final Item CHLORINE = register("chlorine");
    public static final Item AMMONIA = register("ammonia");
    public static final Item VINYL_CHLORIDE = register("vinyl_chloride");
    public static final Item ETHYLENE_GLYCOL = register("ethylene_glycol");
    public static final Item TEREPHTHALIC_ACID = register("terephthalic_acid");
    public static final Item ACRYLONITRILE = register("acrylonitrile");
    public static final Item ADIPIC_ACID = register("adipic_acid");
    public static final Item HEXAMETHYLENEDIAMINE = register("hexamethylenediamine");
    public static final Item BUTADIENE = register("butadiene");
    public static final Item STYRENE = register("styrene");
    public static final Item POLYETHYLENE = register("polyethylene");
    public static final Item POLYVINYL_CHLORIDE = register("polyvinyl_chloride");
    public static final Item POLYETHYLENE_TEREPHTHALATE = register("polyethylene_terephthalate");
    public static final Item POLYACRYLONITRILE = register("polyacrylonitrile");
    public static final Item POLYAMIDE = register("polyamide");
    public static final Item POLYPROPYLENE = register("polypropylene");
    public static final Item SYNTHETIC_RUBBER = register("synthetic_rubber");
    public static final Item POLYESTER_FIBER = register("polyester_fiber");
    public static final Item NYLON_FIBER = register("nylon_fiber");
    public static final Item ACRYLIC_FIBER = register("acrylic_fiber");
    public static final Item STUN_GUN = register("stun_gun", StunGunItem::new);
    public static final Item SUIT = clothing("suit", EquipmentSlot.CHEST);
    public static final Item DECORATED_SUIT = clothing("decorated_suit", EquipmentSlot.CHEST);
    public static final Item WOOL_TROUSERS = clothing("wool_trousers", EquipmentSlot.LEGS);
    public static final Item COTTON_TROUSERS = clothing("cotton_trousers", EquipmentSlot.LEGS);
    public static final Item SKIRT = clothing("skirt", EquipmentSlot.LEGS);

    public static final TagKey<Item> REPAIRS_RUBBER_FOOTWEAR = TagKey.create(Registries.ITEM,
            Identifier.fromNamespaceAndPath(AnalogAlu.MOD_ID, "repairs_rubber_footwear"));
    private static final ArmorMaterial SNEAKERS_MATERIAL = new ArmorMaterial(15, Map.of(ArmorType.BOOTS, 1), 12,
            SoundEvents.ARMOR_EQUIP_LEATHER, 0.0F, 0.0F, REPAIRS_RUBBER_FOOTWEAR, asset("sneakers"));
    private static final ArmorMaterial INSULATED_BOOTS_MATERIAL = new ArmorMaterial(20, Map.of(ArmorType.BOOTS, 1), 8,
            SoundEvents.ARMOR_EQUIP_LEATHER, 0.0F, 0.0F, REPAIRS_RUBBER_FOOTWEAR, asset("insulated_boots"));

    /** How much faster sneakers make the wearer: +20% of their base speed. */
    public static final double SNEAKERS_SPEED_BONUS = 0.2D;
    /** The tallest step a wearer walks straight up, like a stair: 1.8 blocks (0.6 barefoot). */
    public static final double SNEAKERS_STEP_HEIGHT = 1.8D;
    private static final double BASE_STEP_HEIGHT = 0.6D;

    /**
     * Sneakers never wear out (they have no durability at all), take boot enchantments, make the wearer
     * 20% faster and let them step up 1.8 blocks; walking, sprinting and jumping in them cost no hunger
     * (see SneakersExhaustionMixin).
     */
    public static final Item SNEAKERS = register("sneakers", properties -> new Item(properties
            .stacksTo(1)
            .enchantable(SNEAKERS_MATERIAL.enchantmentValue())
            .component(DataComponents.EQUIPPABLE, Equippable.builder(EquipmentSlot.FEET)
                    .setEquipSound(SNEAKERS_MATERIAL.equipSound()).setAsset(SNEAKERS_MATERIAL.assetId()).build())
            .attributes(SNEAKERS_MATERIAL.createAttributes(ArmorType.BOOTS)
                    .withModifierAdded(Attributes.STEP_HEIGHT, new AttributeModifier(
                            Identifier.fromNamespaceAndPath(AnalogAlu.MOD_ID, "sneakers_step_height"),
                            SNEAKERS_STEP_HEIGHT - BASE_STEP_HEIGHT, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.FEET)
                    .withModifierAdded(Attributes.MOVEMENT_SPEED, new AttributeModifier(
                            Identifier.fromNamespaceAndPath(AnalogAlu.MOD_ID, "sneakers_speed"),
                            SNEAKERS_SPEED_BONUS, AttributeModifier.Operation.ADD_MULTIPLIED_BASE), EquipmentSlotGroup.FEET))));
    public static final Item INSULATED_BOOTS = register("insulated_boots",
            properties -> new Item(properties.humanoidArmor(INSULATED_BOOTS_MATERIAL, ArmorType.BOOTS)));

    private ModItems() {}

    public static void init() {}

    private static ResourceKey<EquipmentAsset> asset(String name) {
        return ResourceKey.create(EquipmentAssets.ROOT_ID, Identifier.fromNamespaceAndPath(AnalogAlu.MOD_ID, name));
    }

    /** Clothes: worn for looks, no armour. */
    private static Item clothing(String name, EquipmentSlot slot) {
        return register(name, properties -> new Item(properties.stacksTo(1).component(DataComponents.EQUIPPABLE,
                Equippable.builder(slot).setEquipSound(SoundEvents.ARMOR_EQUIP_LEATHER).setAsset(asset(name)).build())));
    }

    private static Item register(String name) {
        return register(name, Item::new);
    }

    private static Item register(String name, Function<Item.Properties, Item> factory) {
        Identifier id = Identifier.fromNamespaceAndPath(AnalogAlu.MOD_ID, name);
        ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, id);
        Item item = Registry.register(BuiltInRegistries.ITEM, key, factory.apply(new Item.Properties().setId(key)));
        ITEMS.add(item);
        return item;
    }
}