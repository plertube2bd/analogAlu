package com.example.analogalu;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
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

    private ModItems() {}

    public static void init() {}

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