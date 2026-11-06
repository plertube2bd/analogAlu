package com.example.redstonetransportation.client;

import com.example.redstonetransportation.content.ModContent;
import com.example.redstonetransportation.structure.MovingStructures;
import com.example.redstonetransportation.vehicle.ModEntityTypes;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientEntityEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.renderer.entity.EntityRenderers;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;

import java.util.HashMap;
import java.util.Map;

public final class RedstoneTransportationClient implements ClientModInitializer {
    /** Items using the shared tooltip pattern, with how many detail lines each has. */
    private static final Map<Item, Integer> FACTORY_TOOLTIP_LINES = factoryTooltipLines();

    private static Map<Item, Integer> factoryTooltipLines() {
        Map<Item, Integer> lines = new HashMap<>();
        ModContent.MACHINE_ITEMS.forEach(item -> lines.put(item, 1));
        lines.put(ModContent.LAND_LEVELER_ITEM, 2);
        lines.put(ModContent.FACTORY_CONTROLLER_ITEM, 1);
        lines.put(ModContent.CONVEYOR_BELT_ITEM, 2);
        lines.put(ModContent.DRIVE_RAIL_ITEM, 2);
        lines.put(ModContent.TRACK_LAYER_ITEM, 3);
        lines.put(ModContent.CARGO_TRAM_ITEM, 1);
        lines.put(ModContent.ELEVATOR_CORE_ITEM, 2);
        lines.put(ModContent.ELEVATOR_CALL_ITEM, 1);
        lines.put(ModContent.SHIP_HELM_ITEM, 2);
        lines.put(ModContent.AIRSHIP_HELM_ITEM, 2);
        lines.put(ModContent.LIFT_BALLOON_ITEM, 1);
        lines.put(ModContent.STEERING_WHEEL_ITEM, 2);
        lines.put(ModContent.CAR_WHEEL_ITEM, 1);
        lines.put(ModContent.CAR_SEAT_ITEM, 1);
        lines.put(ModContent.CAR_ENGINE_ITEM, 1);
        lines.put(ModContent.FUEL_TANK_ITEM, 1);
        lines.put(ModContent.STRUCTURE_MARKER_ITEM, 2);
        lines.put(ModContent.ROUTE_BEACON_ITEM, 2);
        ModContent.UPGRADED_FURNACE_ITEMS.forEach(item -> lines.put(item, 1));
        return lines;
    }

    @Override
    public void onInitializeClient() {
        RedstoneTramRenderer.TRAM.registerLayers();
        RedstoneTramRenderer.CARGO.registerLayers();
        EntityRenderers.register(ModEntityTypes.REDSTONE_TRAM, context -> new RedstoneTramRenderer<>(context, RedstoneTramRenderer.TRAM));
        EntityRenderers.register(ModEntityTypes.CARGO_TRAM, context -> new RedstoneTramRenderer<>(context, RedstoneTramRenderer.CARGO));
        MenuScreens.register(ModContent.CARGO_TRAM_MENU, CargoTramScreen::new);
        MenuScreens.register(ModContent.LAND_LEVELER_MENU, LandLevelerScreen::new);

        EntityRenderers.register(ModEntityTypes.MOVING_STRUCTURE, MovingStructureRenderer::new);
        ClientEntityEvents.ENTITY_LOAD.register((entity, level) -> MovingStructures.add(level, entity));
        ClientEntityEvents.ENTITY_UNLOAD.register((entity, level) -> MovingStructures.remove(level, entity));
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.level != null) MovingStructures.finishTick(client.level);
        });

        // Compact Korean/English tooltips for all obtainable blocks and transportation items.
        ItemTooltipCallback.EVENT.register((stack, context, type, tooltip) -> {
            String descriptionKey = null;
            String[] detailKeys = new String[0];
            int motorSpeed = 0;
            int energyInterval = 0;

            if (stack.getItem() == ModContent.ALU_TRAIN_CONTROLLER_ITEM) {
                descriptionKey = "tooltip.redstone_transportation.alu_train_controller";
                detailKeys = details("alu_train_controller", 2);
            } else if (stack.getItem() == ModContent.VEHICLE_CONTROLLER_ITEM) {
                descriptionKey = "tooltip.redstone_transportation.vehicle_controller";
                detailKeys = details("vehicle_controller", 1);
            } else if (stack.getItem() == ModContent.REDSTONE_MOTOR_ITEM) {
                descriptionKey = "tooltip.redstone_transportation.redstone_motor";
                detailKeys = details("redstone_motor", 1);
                motorSpeed = 21;
                energyInterval = 20;
            } else if (stack.getItem() == ModContent.REDSTONE_MOTOR_2_ITEM) {
                descriptionKey = "tooltip.redstone_transportation.redstone_motor_2";
                detailKeys = details("redstone_motor_2", 1);
                motorSpeed = 43;
                energyInterval = 12;
            } else if (stack.getItem() == ModContent.REDSTONE_MOTOR_3_ITEM) {
                descriptionKey = "tooltip.redstone_transportation.redstone_motor_3";
                detailKeys = details("redstone_motor_3", 1);
                motorSpeed = 72;
                energyInterval = 8;
            } else if (stack.getItem() == ModContent.REDSTONE_MOTOR_4_ITEM) {
                descriptionKey = "tooltip.redstone_transportation.redstone_motor_4";
                detailKeys = details("redstone_motor_4", 1);
                motorSpeed = 103;
                energyInterval = 4;
            } else if (stack.getItem() == ModContent.POWER_CELL_ITEM) {
                descriptionKey = "tooltip.redstone_transportation.power_cell";
                detailKeys = details("power_cell", 1);
            } else if (stack.getItem() == ModContent.VEHICLE_STATION_ITEM) {
                descriptionKey = "tooltip.redstone_transportation.vehicle_station";
                detailKeys = details("vehicle_station", 1);
            } else if (stack.getItem() == ModContent.SIGNAL_RELAY_ITEM) {
                descriptionKey = "tooltip.redstone_transportation.signal_relay";
                detailKeys = details("signal_relay", 1);
            } else if (stack.getItem() == ModContent.REDSTONE_TRAM_ITEM) {
                descriptionKey = "tooltip.redstone_transportation.redstone_tram";
                detailKeys = details("redstone_tram", 1);
            } else if (stack.getItem() == ModContent.TRAM_COUPLER_ITEM) {
                descriptionKey = "tooltip.redstone_transportation.tram_coupler";
                detailKeys = details("tram_coupler", 1);
            } else if (FACTORY_TOOLTIP_LINES.containsKey(stack.getItem())) {
                String path = BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath();
                descriptionKey = "tooltip.redstone_transportation." + path;
                detailKeys = details(path, FACTORY_TOOLTIP_LINES.get(stack.getItem()));
            }

            if (descriptionKey != null) {
                tooltip.add(Component.translatable(descriptionKey).withStyle(ChatFormatting.GRAY));
                for (String detailKey : detailKeys) {
                    tooltip.add(Component.translatable(detailKey).withStyle(ChatFormatting.DARK_GRAY));
                }
            }

            if (motorSpeed > 0) {
                addMotorStats(tooltip, motorSpeed, energyInterval);
            }
        });
    }

    private static String[] details(String itemPath, int count) {
        String[] keys = new String[count];
        for (int i = 0; i < count; i++) {
            keys[i] = "tooltip.redstone_transportation." + itemPath + ".detail" + (i + 1);
        }
        return keys;
    }

    private static void addMotorStats(java.util.List<Component> tooltip, int metersPerSecond, int energyInterval) {
        tooltip.add(Component.translatable(
                "tooltip.redstone_transportation.motor_stats", metersPerSecond, energyInterval
        ).withStyle(ChatFormatting.AQUA));
    }
}
