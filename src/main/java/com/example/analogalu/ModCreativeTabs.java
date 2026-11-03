package com.example.analogalu;

import java.util.List;
import java.util.Set;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.creativetab.v1.FabricCreativeModeTab;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * 크리에이티브 탭 배치.
 * 전기 장치는 전용 "전기" 탭, 석유 재료는 재료 탭, 석유 기계는 기능 블록 탭, 탱크 양동이는 도구 탭,
 * 나머지(ALU, LED, 반도체 등)는 기존처럼 레드스톤 탭에 둔다.
 */
public final class ModCreativeTabs {
    public static final ResourceKey<CreativeModeTab> ELECTRICAL = ResourceKey.create(Registries.CREATIVE_MODE_TAB,
            Identifier.fromNamespaceAndPath(AnalogAlu.MOD_ID, "electrical"));

    private ModCreativeTabs() {}

    private static List<Item> electrical() {
        return List.of(ModBlocks.COPPER_CABLE_ITEM, ModBlocks.CIRCUIT_SWITCH_ITEM, ModBlocks.RESISTOR_LOAD_ITEM,
                ModBlocks.BATTERY_ITEM, ModBlocks.COAL_GENERATOR_ITEM, ModBlocks.DIESEL_GENERATOR_ITEM,
                ModBlocks.WIND_GENERATOR_ITEM, ModBlocks.SOLAR_GENERATOR_ITEM, ModBlocks.MINER_ITEM);
    }

    private static List<Item> oilMaterials() {
        return List.of(ModItems.OIL_BUCKET, ModItems.SOLID_OIL, ModItems.DIESEL, ModItems.PLASTIC_RESIN,
                ModItems.ASPHALT);
    }

    private static List<Item> oilMachines() {
        return List.of(ModBlocks.OIL_DRILL_ITEM, ModBlocks.OIL_PROCESSOR_ITEM);
    }

    private static List<Item> tankBuckets() {
        return List.of(ModItems.TANK_BUCKET_4, ModItems.TANK_BUCKET_8, ModItems.TANK_BUCKET_16);
    }

    public static void init() {
        Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, ELECTRICAL, FabricCreativeModeTab.builder()
                .title(Component.translatable("itemGroup.analog_alu.electrical"))
                .icon(() -> new ItemStack(ModBlocks.COAL_GENERATOR_ITEM))
                .displayItems((parameters, output) -> electrical().forEach(output::accept))
                .build());

        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.INGREDIENTS)
                .register(output -> oilMaterials().forEach(output::accept));
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.FUNCTIONAL_BLOCKS)
                .register(output -> oilMachines().forEach(output::accept));
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.TOOLS_AND_UTILITIES)
                .register(output -> tankBuckets().forEach(output::accept));

        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.REDSTONE_BLOCKS).register(output -> {
            Set<Item> elsewhere = new java.util.HashSet<>();
            elsewhere.addAll(electrical());
            elsewhere.addAll(oilMaterials());
            elsewhere.addAll(oilMachines());
            elsewhere.addAll(tankBuckets());
            ModBlocks.ITEMS.stream().filter(item -> !elsewhere.contains(item)).forEach(output::accept);
            ModItems.ITEMS.stream().filter(item -> !elsewhere.contains(item)).forEach(output::accept);
        });
    }
}
