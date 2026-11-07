package com.example.redstonetransportation.content;

import com.example.redstonetransportation.RedstoneTransportation;
import com.example.redstonetransportation.block.ConveyorBeltBlock;
import com.example.redstonetransportation.block.DirectionalSignalBlock;
import com.example.redstonetransportation.block.AluTrainControllerBlock;
import com.example.redstonetransportation.block.DriveRailBlock;
import com.example.redstonetransportation.block.CarSeatBlock;
import com.example.redstonetransportation.block.ElevatorCallBlock;
import com.example.redstonetransportation.block.FuelTankBlock;
import com.example.redstonetransportation.block.FuelTankBlockEntity;
import com.example.redstonetransportation.block.PowerCellBlock;
import com.example.redstonetransportation.block.RailMotorBlock;
import com.example.redstonetransportation.block.RouteBeaconBlock;
import com.example.redstonetransportation.block.StructureCoreBlock;
import com.example.redstonetransportation.block.VehicleStationBlock;
import com.example.redstonetransportation.furnace.FurnaceKind;
import com.example.redstonetransportation.furnace.FurnaceTier;
import com.example.redstonetransportation.furnace.UpgradedFurnaceBlock;
import com.example.redstonetransportation.furnace.UpgradedFurnaceBlockEntity;
import com.example.redstonetransportation.item.StructureMarkerItem;
import com.example.redstonetransportation.item.TrackLayerItem;
import com.example.redstonetransportation.machine.FactoryControllerBlock;
import com.example.redstonetransportation.machine.LandLevelerBlock;
import com.example.redstonetransportation.machine.LandLevelerBlockEntity;
import com.example.redstonetransportation.machine.LandLevelerMenu;
import com.example.redstonetransportation.machine.MachineBlock;
import com.example.redstonetransportation.machine.MachineBlockEntity;
import com.example.redstonetransportation.machine.MachineStatus;
import com.example.redstonetransportation.machine.MachineType;
import com.example.redstonetransportation.structure.StructureKind;
import com.example.redstonetransportation.vehicle.CargoTramMenu;
import com.example.redstonetransportation.vehicle.ModEntityTypes;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.MinecartItem;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/** All built-in Transportation blocks, items, and the technical tram display block. */
public final class ModContent {
    public static final Block VEHICLE_CONTROLLER = registerBlock(
            "vehicle_controller", new DirectionalSignalBlock(baseProperties("vehicle_controller")));
    public static final Block ALU_TRAIN_CONTROLLER = registerBlock(
            "alu_train_controller", new AluTrainControllerBlock(baseProperties("alu_train_controller")));
    public static final Block REDSTONE_MOTOR = registerBlock(
            "redstone_motor", new RailMotorBlock(baseProperties("redstone_motor"), 1));
    public static final Block REDSTONE_MOTOR_2 = registerBlock(
            "redstone_motor_2", new RailMotorBlock(baseProperties("redstone_motor_2"), 2));
    public static final Block REDSTONE_MOTOR_3 = registerBlock(
            "redstone_motor_3", new RailMotorBlock(baseProperties("redstone_motor_3"), 3));
    public static final Block REDSTONE_MOTOR_4 = registerBlock(
            "redstone_motor_4", new RailMotorBlock(baseProperties("redstone_motor_4"), 4));
    public static final Block POWER_CELL = registerBlock(
            "power_cell", new PowerCellBlock(baseProperties("power_cell")));
    public static final Block VEHICLE_STATION = registerBlock(
            "vehicle_station", new VehicleStationBlock(baseProperties("vehicle_station")));
    public static final Block SIGNAL_RELAY = registerBlock(
            "signal_relay", new DirectionalSignalBlock(baseProperties("signal_relay")));

    public static final Block DRIVE_RAIL = registerBlock(
            "drive_rail",
            new DriveRailBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.POWERED_RAIL)
                    .emissiveRendering(ModContent::isEmissiveState)
                    .lightLevel(ModContent::getLightLevel)
                    .setId(blockKey("drive_rail"))));
    public static final Block CONVEYOR_BELT = registerBlock(
            "conveyor_belt",
            new ConveyorBeltBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK)
                    .strength(2.0F)
                    .requiresCorrectToolForDrops()
                    .noOcclusion()
                    .setId(blockKey("conveyor_belt"))));
    public static final Block FACTORY_CONTROLLER = registerBlock(
            "factory_controller", new FactoryControllerBlock(baseProperties("factory_controller")));

    public static final Block ELEVATOR_CORE = registerBlock(
            "elevator_core", new StructureCoreBlock(baseProperties("elevator_core"), StructureKind.ELEVATOR));
    public static final Block SHIP_HELM = registerBlock(
            "ship_helm", new StructureCoreBlock(baseProperties("ship_helm"), StructureKind.SHIP));
    public static final Block AIRSHIP_HELM = registerBlock(
            "airship_helm", new StructureCoreBlock(baseProperties("airship_helm"), StructureKind.AIRSHIP));
    public static final Block LIFT_BALLOON = registerBlock(
            "lift_balloon", new Block(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.WOOL)
                    .strength(0.8F)
                    .sound(SoundType.WOOL)
                    .ignitedByLava()
                    .setId(blockKey("lift_balloon"))));
    /** The car's core, and its steering. */
    public static final Block STEERING_WHEEL = registerBlock(
            "steering_wheel", new StructureCoreBlock(baseProperties("steering_wheel"), StructureKind.CAR));
    public static final Block CAR_WHEEL = registerBlock("car_wheel", new Block(baseProperties("car_wheel")));
    public static final Block CAR_SEAT = registerBlock("car_seat", new CarSeatBlock(baseProperties("car_seat").noOcclusion()));
    public static final Block CAR_ENGINE = registerBlock("car_engine", new Block(baseProperties("car_engine")));
    public static final Block FUEL_TANK = registerBlock("fuel_tank", new FuelTankBlock(baseProperties("fuel_tank")));
    /** Each one aboard a ship (or airship) adds a quarter to its top speed, up to four; see MovingStructureEntity. */
    public static final Block SHIP_MOTOR = registerBlock("ship_motor", new Block(baseProperties("ship_motor")));
    public static final Block AIRSHIP_MOTOR = registerBlock("airship_motor", new Block(baseProperties("airship_motor")));
    public static final BlockEntityType<FuelTankBlockEntity> FUEL_TANK_ENTITY = Registry.register(
            BuiltInRegistries.BLOCK_ENTITY_TYPE,
            id("fuel_tank"),
            FabricBlockEntityTypeBuilder.create(FuelTankBlockEntity::new, FUEL_TANK).build()
    );
    public static final Block ELEVATOR_CALL = registerBlock(
            "elevator_call", new ElevatorCallBlock(baseProperties("elevator_call")));

    public static final Block ROUTE_BEACON = registerBlock(
            "route_beacon", new RouteBeaconBlock(baseProperties("route_beacon")));

    /** Every upgraded furnace, blast furnace and smoker; they all share one block entity type. */
    public static final List<Block> UPGRADED_FURNACES = registerFurnaces();

    public static final BlockEntityType<UpgradedFurnaceBlockEntity> UPGRADED_FURNACE_ENTITY = Registry.register(
            BuiltInRegistries.BLOCK_ENTITY_TYPE,
            id("upgraded_furnace"),
            FabricBlockEntityTypeBuilder.create(UpgradedFurnaceBlockEntity::new, UPGRADED_FURNACES.toArray(Block[]::new)).build()
    );

    /** One block per factory machine; they all share the machine block entity type. */
    public static final Map<MachineType, Block> MACHINES = registerMachines();

    public static final BlockEntityType<MachineBlockEntity> MACHINE_ENTITY = Registry.register(
            BuiltInRegistries.BLOCK_ENTITY_TYPE,
            id("machine"),
            FabricBlockEntityTypeBuilder.create(MachineBlockEntity::new, MACHINES.values().toArray(Block[]::new)).build()
    );

    /** Drives itself forward on fuel, leaving a flat strip nine blocks wide. */
    public static final Block LAND_LEVELER = registerBlock(
            "land_leveler", new LandLevelerBlock(baseProperties("land_leveler")));

    public static final BlockEntityType<LandLevelerBlockEntity> LAND_LEVELER_ENTITY = Registry.register(
            BuiltInRegistries.BLOCK_ENTITY_TYPE,
            id("land_leveler"),
            FabricBlockEntityTypeBuilder.create(LandLevelerBlockEntity::new, LAND_LEVELER).build()
    );

    /** Technical block from the tram's previous look. Saved trams may still name it, so it stays registered. */
    public static final Block REDSTONE_TRAM_BODY = registerBlock(
            "redstone_tram_body",
            new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.COPPER_BLOCK.weathering().unaffected())
                    .strength(2.0F)
                    .setId(blockKey("redstone_tram_body")))
    );

    public static final Item VEHICLE_CONTROLLER_ITEM = blockItem("vehicle_controller", VEHICLE_CONTROLLER);
    public static final Item ALU_TRAIN_CONTROLLER_ITEM = blockItem("alu_train_controller", ALU_TRAIN_CONTROLLER);
    public static final Item REDSTONE_MOTOR_ITEM = blockItem("redstone_motor", REDSTONE_MOTOR);
    public static final Item REDSTONE_MOTOR_2_ITEM = blockItem("redstone_motor_2", REDSTONE_MOTOR_2);
    public static final Item REDSTONE_MOTOR_3_ITEM = blockItem("redstone_motor_3", REDSTONE_MOTOR_3);
    public static final Item REDSTONE_MOTOR_4_ITEM = blockItem("redstone_motor_4", REDSTONE_MOTOR_4);
    public static final Item POWER_CELL_ITEM = blockItem("power_cell", POWER_CELL);
    public static final Item VEHICLE_STATION_ITEM = blockItem("vehicle_station", VEHICLE_STATION);
    public static final Item SIGNAL_RELAY_ITEM = blockItem("signal_relay", SIGNAL_RELAY);

    public static final Item DRIVE_RAIL_ITEM = blockItem("drive_rail", DRIVE_RAIL);
    public static final Item CONVEYOR_BELT_ITEM = blockItem("conveyor_belt", CONVEYOR_BELT);
    public static final Item FACTORY_CONTROLLER_ITEM = blockItem("factory_controller", FACTORY_CONTROLLER);
    public static final Item ELEVATOR_CORE_ITEM = blockItem("elevator_core", ELEVATOR_CORE);
    public static final Item SHIP_HELM_ITEM = blockItem("ship_helm", SHIP_HELM);
    public static final Item AIRSHIP_HELM_ITEM = blockItem("airship_helm", AIRSHIP_HELM);
    public static final Item LIFT_BALLOON_ITEM = blockItem("lift_balloon", LIFT_BALLOON);
    public static final Item ELEVATOR_CALL_ITEM = blockItem("elevator_call", ELEVATOR_CALL);
    public static final Item STEERING_WHEEL_ITEM = blockItem("steering_wheel", STEERING_WHEEL);
    public static final Item CAR_WHEEL_ITEM = blockItem("car_wheel", CAR_WHEEL);
    public static final Item CAR_SEAT_ITEM = blockItem("car_seat", CAR_SEAT);
    public static final Item CAR_ENGINE_ITEM = blockItem("car_engine", CAR_ENGINE);
    public static final Item FUEL_TANK_ITEM = blockItem("fuel_tank", FUEL_TANK);
    public static final Item SHIP_MOTOR_ITEM = blockItem("ship_motor", SHIP_MOTOR);
    public static final Item AIRSHIP_MOTOR_ITEM = blockItem("airship_motor", AIRSHIP_MOTOR);
    public static final Item ROUTE_BEACON_ITEM = blockItem("route_beacon", ROUTE_BEACON);
    public static final List<Item> UPGRADED_FURNACE_ITEMS = UPGRADED_FURNACES.stream()
            .map(block -> blockItem(BuiltInRegistries.BLOCK.getKey(block).getPath(), block))
            .toList();
    public static final List<Item> MACHINE_ITEMS = Arrays.stream(MachineType.values())
            .map(type -> blockItem(type.id(), MACHINES.get(type)))
            .toList();
    public static final Item LAND_LEVELER_ITEM = blockItem("land_leveler", LAND_LEVELER);

    /** Separate spawn/place item for transportation:redstone_tram. */
    public static final Item REDSTONE_TRAM_ITEM = registerItem(
            "redstone_tram",
            new MinecartItem(ModEntityTypes.REDSTONE_TRAM,
                    new Item.Properties().setId(itemKey("redstone_tram")))
    );

    public static final Item CARGO_TRAM_ITEM = registerItem(
            "cargo_tram",
            new MinecartItem(ModEntityTypes.CARGO_TRAM,
                    new Item.Properties().setId(itemKey("cargo_tram")))
    );

    public static final Item TRAM_COUPLER_ITEM = registerItem(
            "tram_coupler",
            new Item(new Item.Properties().setId(itemKey("tram_coupler")))
    );

    public static final MenuType<CargoTramMenu> CARGO_TRAM_MENU = Registry.register(
            BuiltInRegistries.MENU,
            Identifier.fromNamespaceAndPath(RedstoneTransportation.MOD_ID, "cargo_tram"),
            new MenuType<>(CargoTramMenu::new, FeatureFlags.VANILLA_SET));
    public static final MenuType<LandLevelerMenu> LAND_LEVELER_MENU = Registry.register(
            BuiltInRegistries.MENU,
            Identifier.fromNamespaceAndPath(RedstoneTransportation.MOD_ID, "land_leveler"),
            new MenuType<>(LandLevelerMenu::new, FeatureFlags.VANILLA_SET));
    public static final Item TRACK_LAYER_ITEM = registerItem(
            "track_layer",
            new TrackLayerItem(new Item.Properties().stacksTo(1).setId(itemKey("track_layer")))
    );

    public static final Item STRUCTURE_MARKER_ITEM = registerItem(
            "structure_marker",
            new StructureMarkerItem(new Item.Properties().stacksTo(1).setId(itemKey("structure_marker")))
    );

    private ModContent() {}

    public static void initialize() {
        // Triggers this class's static initializers.
    }

    private static List<Block> registerFurnaces() {
        List<Block> furnaces = new ArrayList<>();
        for (FurnaceKind kind : FurnaceKind.values()) {
            for (FurnaceTier tier : FurnaceTier.values()) {
                String path = kind.id(tier.number());
                furnaces.add(registerBlock(path, new UpgradedFurnaceBlock(
                        BlockBehaviour.Properties.ofFullCopy(Blocks.FURNACE).setId(blockKey(path)), kind, tier)));
            }
        }
        return List.copyOf(furnaces);
    }

    private static Map<MachineType, Block> registerMachines() {
        Map<MachineType, Block> machines = new EnumMap<>(MachineType.class);
        for (MachineType type : MachineType.values()) {
            machines.put(type, registerBlock(type.id(), new MachineBlock(baseProperties(type.id()), type)));
        }
        return machines;
    }

    private static Block registerBlock(String path, Block block) {
        ResourceKey<Block> key = blockKey(path);
        return Registry.register(BuiltInRegistries.BLOCK, key, block);
    }

    private static BlockBehaviour.Properties baseProperties(String path) {
        return BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK)
                .strength(3.5F)
                .requiresCorrectToolForDrops()
                .emissiveRendering(ModContent::isEmissiveState)
                .lightLevel(ModContent::getLightLevel)
                .setId(blockKey(path));
    }

    private static boolean isEmissiveState(BlockState state) {
        return (state.hasProperty(RailMotorBlock.ACTIVE) && state.getValue(RailMotorBlock.ACTIVE))
                || (state.hasProperty(RailMotorBlock.POWERED) && state.getValue(RailMotorBlock.POWERED))
                || (state.hasProperty(PowerCellBlock.CHARGING) && state.getValue(PowerCellBlock.CHARGING))
                || (state.hasProperty(PowerCellBlock.CHARGED) && state.getValue(PowerCellBlock.CHARGED))
                || (state.hasProperty(VehicleStationBlock.ACTIVE) && state.getValue(VehicleStationBlock.ACTIVE))
                || (state.hasProperty(DirectionalSignalBlock.ACTIVE) && state.getValue(DirectionalSignalBlock.ACTIVE))
                || (state.hasProperty(AluTrainControllerBlock.ACTIVE) && state.getValue(AluTrainControllerBlock.ACTIVE))
                || (state.hasProperty(AluTrainControllerBlock.BRAKING) && state.getValue(AluTrainControllerBlock.BRAKING))
                || (state.hasProperty(AluTrainControllerBlock.ALU_LINKED) && state.getValue(AluTrainControllerBlock.ALU_LINKED))
                || (state.hasProperty(MachineBlock.STATUS) && state.getValue(MachineBlock.STATUS) != MachineStatus.OFF)
                || (state.hasProperty(FactoryControllerBlock.MODE) && state.getValue(FactoryControllerBlock.MODE) != FactoryControllerBlock.Mode.STOPPED)
                || (state.hasProperty(DriveRailBlock.REVERSED) && state.getValue(DriveRailBlock.POWERED));
    }

    private static int getLightLevel(BlockState state) {
        if (state.hasProperty(RailMotorBlock.ACTIVE) && state.getValue(RailMotorBlock.ACTIVE)) return 12;
        if (state.hasProperty(RailMotorBlock.POWERED) && state.getValue(RailMotorBlock.POWERED)) return 5;
        if (state.hasProperty(PowerCellBlock.CHARGING) && state.getValue(PowerCellBlock.CHARGING)) return 11;
        if (state.hasProperty(PowerCellBlock.CHARGED) && state.getValue(PowerCellBlock.CHARGED)) return 5;
        if (state.hasProperty(VehicleStationBlock.ACTIVE) && state.getValue(VehicleStationBlock.ACTIVE)) return 10;
        if (state.hasProperty(DirectionalSignalBlock.ACTIVE) && state.getValue(DirectionalSignalBlock.ACTIVE)) return 7;
        if (state.hasProperty(AluTrainControllerBlock.BRAKING) && state.getValue(AluTrainControllerBlock.BRAKING)) return 12;
        if (state.hasProperty(AluTrainControllerBlock.ACTIVE) && state.getValue(AluTrainControllerBlock.ACTIVE)) return 10;
        if (state.hasProperty(AluTrainControllerBlock.ALU_LINKED) && state.getValue(AluTrainControllerBlock.ALU_LINKED)) return 4;
        if (state.hasProperty(MachineBlock.STATUS) && state.getValue(MachineBlock.STATUS) == MachineStatus.WORKING) return 10;
        if (state.hasProperty(MachineBlock.STATUS) && state.getValue(MachineBlock.STATUS) != MachineStatus.OFF) return 5;
        if (state.hasProperty(FactoryControllerBlock.MODE) && state.getValue(FactoryControllerBlock.MODE) != FactoryControllerBlock.Mode.STOPPED) return 7;
        if (state.hasProperty(DriveRailBlock.REVERSED) && state.getValue(DriveRailBlock.POWERED)) return 7;
        return 0;
    }

    private static Item blockItem(String path, Block block) {
        return registerItem(path, new BlockItem(block,
                new Item.Properties().setId(itemKey(path)).useBlockDescriptionPrefix()));
    }

    private static Item registerItem(String path, Item item) {
        return Registry.register(BuiltInRegistries.ITEM, itemKey(path), item);
    }

    private static ResourceKey<Block> blockKey(String path) {
        return ResourceKey.create(Registries.BLOCK, id(path));
    }

    private static ResourceKey<Item> itemKey(String path) {
        return ResourceKey.create(Registries.ITEM, id(path));
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(RedstoneTransportation.MOD_ID, path);
    }
}
