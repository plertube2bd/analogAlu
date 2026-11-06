package com.example.redstonetransportation;

import com.example.redstonetransportation.content.ModContent;
import com.example.redstonetransportation.rail.RailDrive;
import com.example.redstonetransportation.structure.MovingStructures;
import com.example.redstonetransportation.vehicle.RedstoneTramModule;
import com.example.redstonetransportation.vehicle.ModEntityTypes;
import com.example.redstonetransportation.vehicle.RailVehicleBehavior;
import com.example.redstonetransportation.vehicle.TrainLinkManager;
import com.example.redstonetransportation.vehicle.VehicleBehaviorRegistry;
import com.example.redstonetransportation.vehicle.VehicleCatalog;
import com.example.redstonetransportation.vehicle.VehicleJsonLoader;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.vehicle.minecart.AbstractMinecart;
import net.minecraft.world.item.CreativeModeTabs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.Set;

public final class RedstoneTransportation implements ModInitializer {
    public static final String MOD_ID = "redstone_transportation";
    private static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    private static final Set<String> RECIPE_BOOK_NAMESPACES = Set.of(MOD_ID, "analog_alu");

    private static VehicleCatalog vehicleCatalog;
    private static VehicleBehaviorRegistry behaviorRegistry;

    @Override
    public void onInitialize() {
        MovingStructures.initialize();
        // Entity type must exist before its dedicated MinecartItem is registered.
        ModEntityTypes.initialize();
        ModContent.initialize();
        RailDrive.initialize();
        TrainLinkManager.initialize();

        Path configRoot = FabricLoader.getInstance().getConfigDir().resolve(MOD_ID);

        behaviorRegistry = new VehicleBehaviorRegistry();
        behaviorRegistry.register(new RailVehicleBehavior());
        behaviorRegistry.freeze();

        vehicleCatalog = new VehicleCatalog();
        new RedstoneTramModule().register(vehicleCatalog);
        VehicleJsonLoader.load(configRoot.resolve("vehicles"), vehicleCatalog, behaviorRegistry)
                .forEach(LOGGER::info);
        vehicleCatalog.freeze();

        registerCartEvents();
        registerRecipeBookUnlock();

        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.REDSTONE_BLOCKS)
                .register(entries -> {
                    entries.accept(ModContent.VEHICLE_CONTROLLER_ITEM);
                    entries.accept(ModContent.ALU_TRAIN_CONTROLLER_ITEM);
                    entries.accept(ModContent.REDSTONE_MOTOR_ITEM);
                    entries.accept(ModContent.REDSTONE_MOTOR_2_ITEM);
                    entries.accept(ModContent.REDSTONE_MOTOR_3_ITEM);
                    entries.accept(ModContent.REDSTONE_MOTOR_4_ITEM);
                    entries.accept(ModContent.POWER_CELL_ITEM);
                    entries.accept(ModContent.VEHICLE_STATION_ITEM);
                    entries.accept(ModContent.SIGNAL_RELAY_ITEM);
                    entries.accept(ModContent.REDSTONE_TRAM_ITEM);
                    entries.accept(ModContent.CARGO_TRAM_ITEM);
                    entries.accept(ModContent.TRAM_COUPLER_ITEM);
                    entries.accept(ModContent.DRIVE_RAIL_ITEM);
                    entries.accept(ModContent.TRACK_LAYER_ITEM);
                    entries.accept(ModContent.FACTORY_CONTROLLER_ITEM);
                    ModContent.MACHINE_ITEMS.forEach(entries::accept);
                    entries.accept(ModContent.LAND_LEVELER_ITEM);
                    entries.accept(ModContent.CONVEYOR_BELT_ITEM);
                    entries.accept(ModContent.ELEVATOR_CORE_ITEM);
                    entries.accept(ModContent.ELEVATOR_CALL_ITEM);
                    entries.accept(ModContent.ROUTE_BEACON_ITEM);
                    entries.accept(ModContent.SHIP_HELM_ITEM);
                    entries.accept(ModContent.AIRSHIP_HELM_ITEM);
                    entries.accept(ModContent.LIFT_BALLOON_ITEM);
                    entries.accept(ModContent.STRUCTURE_MARKER_ITEM);
                    ModContent.UPGRADED_FURNACE_ITEMS.forEach(entries::accept);
                });

        LOGGER.info("Redstone Transportation initialized: ALU train control, drive rails for every minecart, track layer and factory machines registered.");
    }

    /**
     * The recipe book only lists recipes a player has unlocked, and neither this mod nor Analog ALU
     * ships unlock advancements. Checking on every join also covers Analog ALU being added later.
     */
    private static void registerRecipeBookUnlock() {
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> handler.getPlayer().awardRecipes(
                server.getRecipeManager().getRecipes().stream()
                        .filter(recipe -> RECIPE_BOOK_NAMESPACES.contains(recipe.id().identifier().getNamespace()))
                        .toList()));
    }

    /** Couplings work on any minecart, so they hook the shared events rather than the tram entity. */
    private static void registerCartEvents() {
        UseEntityCallback.EVENT.register((player, level, hand, entity, hit) -> {
            if (!(entity instanceof AbstractMinecart cart) || player.getItemInHand(hand).getItem() != ModContent.TRAM_COUPLER_ITEM) {
                return InteractionResult.PASS;
            }
            if (level instanceof ServerLevel serverLevel) {
                TrainLinkManager.handleCouplerUse(player, cart, serverLevel);
            }
            return InteractionResult.SUCCESS;
        });

        ServerEntityEvents.ENTITY_UNLOAD.register((entity, level) -> {
            if (entity instanceof AbstractMinecart cart && entity.getRemovalReason() != null && entity.getRemovalReason().shouldDestroy()) {
                TrainLinkManager.onCartRemoved(cart, level);
            }
        });
    }

    public static VehicleBehaviorRegistry getBehaviorRegistry() {
        if (behaviorRegistry == null) throw new IllegalStateException("Transportation has not initialized yet.");
        return behaviorRegistry;
    }

    public static VehicleCatalog getVehicleCatalog() {
        if (vehicleCatalog == null) throw new IllegalStateException("Transportation has not initialized yet.");
        return vehicleCatalog;
    }
}
