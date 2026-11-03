package com.example.analogalu;

import com.example.analogalu.block.AluBlocks;
import com.example.analogalu.block.CopperWireBlock;
import com.example.analogalu.block.CopperCableBlock;
import com.example.analogalu.block.CopperCableBlockEntity;
import com.example.analogalu.block.CircuitDeviceBlock;
import com.example.analogalu.block.CircuitDeviceBlockEntity;
import com.example.analogalu.block.UpwardHopperBlock;
import com.example.analogalu.block.UpwardHopperBlockEntity;
import com.example.analogalu.block.MassAnalyzerBlock;
import com.example.analogalu.block.MassAnalyzerBlockEntity;
import com.example.analogalu.block.SemiconductorBlock;
import com.example.analogalu.block.ChunkLoaderBlock;
import com.example.analogalu.block.ChunkLoaderBlockEntity;
import com.example.analogalu.block.AnalogLedBlock;
import com.example.analogalu.block.OilLiquidBlock;
import com.example.analogalu.block.OilDrillBlock;
import com.example.analogalu.block.OilDrillBlockEntity;
import com.example.analogalu.block.OilProcessorBlock;
import com.example.analogalu.block.OilProcessorBlockEntity;
import com.example.analogalu.ModFluids;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.PushReaction;

public final class ModBlocks {
    /** 크리에이티브 탭에 등록할 아이템 (등록 순서대로) */
    public static final List<Item> ITEMS = new ArrayList<>();

    public static final CopperWireBlock COPPER_WIRE = registerBlock("copper_wire", CopperWireBlock::new,
            BlockBehaviour.Properties.of().noCollision().instabreak()
                    .sound(SoundType.STONE).pushReaction(PushReaction.POPPED));
    public static final Item COPPER_WIRE_ITEM = registerItem("copper_wire", COPPER_WIRE);

        public static final Block OIL = registerBlock("oil",
            properties -> new OilLiquidBlock((net.minecraft.world.level.material.FlowingFluid) ModFluids.STILL,
                properties),
            BlockBehaviour.Properties.of().noLootTable().replaceable().strength(100.0F));
            public static final OilProcessorBlock OIL_PROCESSOR = registerBlock("oil_processor", OilProcessorBlock::new,
                BlockBehaviour.Properties.of().strength(3.0F).sound(SoundType.METAL));
            public static final Item OIL_PROCESSOR_ITEM = registerItem("oil_processor", OIL_PROCESSOR);
            public static final BlockEntityType<OilProcessorBlockEntity> OIL_PROCESSOR_ENTITY = Registry.register(
                BuiltInRegistries.BLOCK_ENTITY_TYPE,
                ResourceKey.create(Registries.BLOCK_ENTITY_TYPE, id("oil_processor")),
                new BlockEntityType<>(OilProcessorBlockEntity::new, Set.of(OIL_PROCESSOR)));
            public static final OilDrillBlock OIL_DRILL = registerBlock("oil_drill", OilDrillBlock::new,
                BlockBehaviour.Properties.of().strength(4.0F).sound(SoundType.METAL));
            public static final Item OIL_DRILL_ITEM = registerItem("oil_drill", OIL_DRILL);
            public static final BlockEntityType<OilDrillBlockEntity> OIL_DRILL_ENTITY = Registry.register(
                BuiltInRegistries.BLOCK_ENTITY_TYPE,
                ResourceKey.create(Registries.BLOCK_ENTITY_TYPE, id("oil_drill")),
                new BlockEntityType<>(OilDrillBlockEntity::new, Set.of(OIL_DRILL)));

        public static final CopperCableBlock COPPER_CABLE = registerBlock("copper_cable", CopperCableBlock::new,
            BlockBehaviour.Properties.of().noCollision().instabreak()
                .sound(SoundType.COPPER).lightLevel(state -> state.getValue(CopperCableBlock.POWERED) ? 8 : 0)
                .pushReaction(PushReaction.POPPED));
        public static final Item COPPER_CABLE_ITEM = registerItem("copper_cable", COPPER_CABLE);
        public static final BlockEntityType<CopperCableBlockEntity> COPPER_CABLE_ENTITY = Registry.register(
            BuiltInRegistries.BLOCK_ENTITY_TYPE,
            ResourceKey.create(Registries.BLOCK_ENTITY_TYPE, id("copper_cable")),
            new BlockEntityType<>(CopperCableBlockEntity::new, Set.of(COPPER_CABLE)));

            public static final CircuitDeviceBlock BATTERY = registerBlock("battery",
                properties -> new CircuitDeviceBlock(properties, CircuitDeviceBlock.Kind.BATTERY),
                BlockBehaviour.Properties.of().strength(2.0F).sound(SoundType.COPPER));
            public static final Item BATTERY_ITEM = registerItem("battery", BATTERY);
            public static final CircuitDeviceBlock RECHARGEABLE_BATTERY = registerBlock("rechargeable_battery",
                properties -> new CircuitDeviceBlock(properties, CircuitDeviceBlock.Kind.RECHARGEABLE_BATTERY),
                BlockBehaviour.Properties.of().strength(2.0F).sound(SoundType.COPPER));
            public static final Item RECHARGEABLE_BATTERY_ITEM = registerItem("rechargeable_battery",
                RECHARGEABLE_BATTERY);
            public static final CircuitDeviceBlock STEP_UP_CONVERTER = registerBlock("step_up_converter",
                properties -> new CircuitDeviceBlock(properties, CircuitDeviceBlock.Kind.STEP_UP),
                BlockBehaviour.Properties.of().strength(3.0F).sound(SoundType.METAL));
            public static final Item STEP_UP_CONVERTER_ITEM = registerItem("step_up_converter", STEP_UP_CONVERTER);
            public static final CircuitDeviceBlock STEP_DOWN_TRANSFORMER = registerBlock("step_down_transformer",
                properties -> new CircuitDeviceBlock(properties, CircuitDeviceBlock.Kind.STEP_DOWN),
                BlockBehaviour.Properties.of().strength(3.0F).sound(SoundType.METAL));
            public static final Item STEP_DOWN_TRANSFORMER_ITEM = registerItem("step_down_transformer",
                STEP_DOWN_TRANSFORMER);
            public static final CircuitDeviceBlock CIRCUIT_SWITCH = registerBlock("circuit_switch",
                properties -> new CircuitDeviceBlock(properties, CircuitDeviceBlock.Kind.SWITCH),
                BlockBehaviour.Properties.of().strength(1.0F).sound(SoundType.COPPER));
            public static final Item CIRCUIT_SWITCH_ITEM = registerItem("circuit_switch", CIRCUIT_SWITCH);
            public static final CircuitDeviceBlock RESISTOR_LOAD = registerBlock("resistor_load",
                properties -> new CircuitDeviceBlock(properties, CircuitDeviceBlock.Kind.RESISTOR),
                BlockBehaviour.Properties.of().strength(1.0F).sound(SoundType.COPPER));
            public static final Item RESISTOR_LOAD_ITEM = registerItem("resistor_load", RESISTOR_LOAD);
            public static final CircuitDeviceBlock COAL_GENERATOR = registerBlock("coal_generator",
                properties -> new CircuitDeviceBlock(properties, CircuitDeviceBlock.Kind.COAL_GENERATOR),
                BlockBehaviour.Properties.of().strength(3.0F).sound(SoundType.METAL));
            public static final Item COAL_GENERATOR_ITEM = registerItem("coal_generator", COAL_GENERATOR);
            public static final CircuitDeviceBlock DIESEL_GENERATOR = registerBlock("diesel_generator",
                properties -> new CircuitDeviceBlock(properties, CircuitDeviceBlock.Kind.DIESEL_GENERATOR),
                BlockBehaviour.Properties.of().strength(3.0F).sound(SoundType.METAL));
            public static final Item DIESEL_GENERATOR_ITEM = registerItem("diesel_generator", DIESEL_GENERATOR);
            public static final CircuitDeviceBlock WIND_GENERATOR = registerBlock("wind_generator",
                properties -> new CircuitDeviceBlock(properties, CircuitDeviceBlock.Kind.WIND_GENERATOR),
                BlockBehaviour.Properties.of().strength(2.0F).sound(SoundType.METAL));
            public static final Item WIND_GENERATOR_ITEM = registerItem("wind_generator", WIND_GENERATOR);
            public static final CircuitDeviceBlock SOLAR_GENERATOR = registerBlock("solar_generator",
                properties -> new CircuitDeviceBlock(properties, CircuitDeviceBlock.Kind.SOLAR_GENERATOR),
                BlockBehaviour.Properties.of().strength(2.0F).sound(SoundType.COPPER));
            public static final Item SOLAR_GENERATOR_ITEM = registerItem("solar_generator", SOLAR_GENERATOR);
            public static final CircuitDeviceBlock MINER = registerBlock("miner",
                properties -> new CircuitDeviceBlock(properties, CircuitDeviceBlock.Kind.MINER),
                BlockBehaviour.Properties.of().strength(3.0F).sound(SoundType.METAL));
            public static final Item MINER_ITEM = registerItem("miner", MINER);
            public static final BlockEntityType<CircuitDeviceBlockEntity> CIRCUIT_DEVICE_ENTITY = Registry.register(
                BuiltInRegistries.BLOCK_ENTITY_TYPE,
                ResourceKey.create(Registries.BLOCK_ENTITY_TYPE, id("circuit_device")),
                new BlockEntityType<>(CircuitDeviceBlockEntity::new,
                        Set.of(BATTERY, RECHARGEABLE_BATTERY, STEP_UP_CONVERTER, STEP_DOWN_TRANSFORMER, CIRCUIT_SWITCH, RESISTOR_LOAD, COAL_GENERATOR, WIND_GENERATOR,
                                SOLAR_GENERATOR, MINER, DIESEL_GENERATOR)));

        public static final Block UPWARD_HOPPER = registerBlock("upward_hopper", UpwardHopperBlock::new,
            BlockBehaviour.Properties.of().mapColor(net.minecraft.world.level.material.MapColor.STONE)
                .strength(3.0F, 4.8F).sound(SoundType.METAL).noOcclusion());
        public static final Item UPWARD_HOPPER_ITEM = registerItem("upward_hopper", UPWARD_HOPPER);
            public static final BlockEntityType<UpwardHopperBlockEntity> UPWARD_HOPPER_ENTITY = Registry.register(
                BuiltInRegistries.BLOCK_ENTITY_TYPE,
                ResourceKey.create(Registries.BLOCK_ENTITY_TYPE, id("upward_hopper")),
                new BlockEntityType<>(UpwardHopperBlockEntity::new, Set.of(UPWARD_HOPPER)));

                public static final Block CHUNK_LOADER = registerBlock("chunk_loader", ChunkLoaderBlock::new,
                        BlockBehaviour.Properties.of().strength(3.0F).sound(SoundType.METAL));
                public static final Item CHUNK_LOADER_ITEM = registerItem("chunk_loader", CHUNK_LOADER);
                public static final Block CHUNK_LOADER_TIER_2 = registerBlock("chunk_loader_tier_2", ChunkLoaderBlock::new,
                        BlockBehaviour.Properties.of().strength(3.0F).sound(SoundType.METAL));
                public static final Item CHUNK_LOADER_TIER_2_ITEM = registerItem("chunk_loader_tier_2", CHUNK_LOADER_TIER_2);
                public static final BlockEntityType<ChunkLoaderBlockEntity> CHUNK_LOADER_ENTITY = Registry.register(
                    BuiltInRegistries.BLOCK_ENTITY_TYPE,
                    ResourceKey.create(Registries.BLOCK_ENTITY_TYPE, id("chunk_loader")),
                    new BlockEntityType<>(ChunkLoaderBlockEntity::new, Set.of(CHUNK_LOADER, CHUNK_LOADER_TIER_2)));

    public static final Block SUBTRACTOR = alu("subtractor", AluBlocks.Subtractor::new);
    public static final Block EXTENDER = alu("extender", AluBlocks.Extender::new);
    public static final Block SHIFT_RIGHT = alu("shift_right", AluBlocks.ShiftRight::new);
    public static final Block SHIFT_LEFT = alu("shift_left", AluBlocks.ShiftLeft::new);
    public static final Block BITWISE_NOT = alu("bitwise_not", AluBlocks.BitwiseNot::new);
    public static final Block LOGICAL_NOT = alu("logical_not", AluBlocks.LogicalNot::new);
    public static final Block CRAM = registerBlock("cram", AluBlocks.Cram::new,
            BlockBehaviour.Properties.of().strength(1.0F).sound(SoundType.STONE));
    public static final Item CRAM_ITEM = registerItem("cram", CRAM);

    public static final Block OR_GATE = alu("or_gate", AluBlocks.OrGate::new);
    public static final Block AND_GATE = alu("and_gate", AluBlocks.AndGate::new);
    public static final Block XOR_GATE = alu("xor_gate", AluBlocks.XorGate::new);
    public static final Block LOGICAL_OR = alu("logical_or", AluBlocks.LogicalOr::new);
    public static final Block LOGICAL_AND = alu("logical_and", AluBlocks.LogicalAnd::new);
    public static final Block LOGICAL_XOR = alu("logical_xor", AluBlocks.LogicalXor::new);
    public static final Block ADDER = alu("adder", AluBlocks.Adder::new);

        public static final Block SEMICONDUCTOR_BLOCK = registerBlock("semiconductor_block",
            properties -> new SemiconductorBlock(properties, false),
            BlockBehaviour.Properties.of().strength(4.0F, 6.0F).sound(SoundType.COPPER));
        public static final Item SEMICONDUCTOR_BLOCK_ITEM = registerItem("semiconductor_block", SEMICONDUCTOR_BLOCK);
        public static final Block WATERPROOF_SEMICONDUCTOR_BLOCK = registerBlock("waterproof_semiconductor_block",
            properties -> new SemiconductorBlock(properties, true),
            BlockBehaviour.Properties.of().strength(2.0F, 6.0F).sound(SoundType.COPPER));
        public static final Item WATERPROOF_SEMICONDUCTOR_BLOCK_ITEM = registerItem(
            "waterproof_semiconductor_block", WATERPROOF_SEMICONDUCTOR_BLOCK);

        public static final Block MASS_ANALYZER = registerBlock("mass_analyzer", MassAnalyzerBlock::new,
            BlockBehaviour.Properties.of().strength(4.0F, 6.0F).sound(SoundType.COPPER));
        public static final Item MASS_ANALYZER_ITEM = registerItem("mass_analyzer", MASS_ANALYZER);
        public static final BlockEntityType<MassAnalyzerBlockEntity> MASS_ANALYZER_ENTITY = Registry.register(
            BuiltInRegistries.BLOCK_ENTITY_TYPE,
            ResourceKey.create(Registries.BLOCK_ENTITY_TYPE, id("mass_analyzer")),
            new BlockEntityType<>(MassAnalyzerBlockEntity::new, Set.of(MASS_ANALYZER)));

    public static final Block ANALOG_LED_RED = led("analog_led_red", true);
    public static final Block ANALOG_LED_GREEN = led("analog_led_green", true);
    public static final Block ANALOG_LED_BLUE = led("analog_led_blue", true);
    public static final Block ANALOG_LED_WHITE = led("analog_led_white", true);
    public static final Block DIGITAL_LED_RED = led("digital_led_red", false);
    public static final Block DIGITAL_LED_GREEN = led("digital_led_green", false);
    public static final Block DIGITAL_LED_BLUE = led("digital_led_blue", false);
    public static final Block DIGITAL_LED_WHITE = led("digital_led_white", false);

    private ModBlocks() {}

    public static void init() {}

    private static Identifier id(String name) {
        return Identifier.fromNamespaceAndPath(AnalogAlu.MOD_ID, name);
    }

    private static <T extends Block> T alu(String name, Function<BlockBehaviour.Properties, T> factory) {
        T block = registerBlock(name, factory,
                BlockBehaviour.Properties.of().instabreak().sound(SoundType.STONE));
        registerItem(name, block);
        return block;
    }

    private static Block led(String name, boolean analog) {
        Block block = registerBlock(name, properties -> new AnalogLedBlock(properties, analog),
                BlockBehaviour.Properties.of().strength(0.5F)
                        .lightLevel(state -> state.getValue(AnalogLedBlock.POWER)).sound(SoundType.GLASS));
        registerItem(name, block);
        return block;
    }

    private static <T extends Block> T registerBlock(String name,
            Function<BlockBehaviour.Properties, T> factory, BlockBehaviour.Properties props) {
        ResourceKey<Block> key = ResourceKey.create(Registries.BLOCK, id(name));
        return Registry.register(BuiltInRegistries.BLOCK, key, factory.apply(props.setId(key)));
    }

    private static Item registerItem(String name, Block block) {
        ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, id(name));
        Item item = Registry.register(BuiltInRegistries.ITEM, key,
                new BlockItem(block, new Item.Properties().setId(key).useBlockDescriptionPrefix()));
        ITEMS.add(item);
        return item;
    }
}
