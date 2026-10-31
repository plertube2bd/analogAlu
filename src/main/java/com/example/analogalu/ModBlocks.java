package com.example.analogalu;

import com.example.analogalu.block.AluBlocks;
import com.example.analogalu.block.CopperWireBlock;
import com.example.analogalu.block.UpwardHopperBlock;
import com.example.analogalu.block.UpwardHopperBlockEntity;
import com.example.analogalu.block.MassAnalyzerBlock;
import com.example.analogalu.block.MassAnalyzerBlockEntity;
import com.example.analogalu.block.SemiconductorBlock;
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

        public static final Block UPWARD_HOPPER = registerBlock("upward_hopper", UpwardHopperBlock::new,
            BlockBehaviour.Properties.ofFullCopy(net.minecraft.world.level.block.Blocks.HOPPER));
        public static final Item UPWARD_HOPPER_ITEM = registerItem("upward_hopper", UPWARD_HOPPER);
            public static final BlockEntityType<UpwardHopperBlockEntity> UPWARD_HOPPER_ENTITY = Registry.register(
                BuiltInRegistries.BLOCK_ENTITY_TYPE,
                ResourceKey.create(Registries.BLOCK_ENTITY_TYPE, id("upward_hopper")),
                new BlockEntityType<>(UpwardHopperBlockEntity::new, Set.of(UPWARD_HOPPER)));

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
    public static final Block ADDER = alu("adder", AluBlocks.Adder::new);

        public static final Block SEMICONDUCTOR_BLOCK = registerBlock("semiconductor_block",
            properties -> new SemiconductorBlock(properties, false),
            BlockBehaviour.Properties.of().strength(1.0F).sound(SoundType.COPPER));
        public static final Item SEMICONDUCTOR_BLOCK_ITEM = registerItem("semiconductor_block", SEMICONDUCTOR_BLOCK);
        public static final Block WATERPROOF_SEMICONDUCTOR_BLOCK = registerBlock("waterproof_semiconductor_block",
            properties -> new SemiconductorBlock(properties, true),
            BlockBehaviour.Properties.of().strength(2.0F, 6.0F).requiresCorrectToolForDrops()
                .sound(SoundType.COPPER));
        public static final Item WATERPROOF_SEMICONDUCTOR_BLOCK_ITEM = registerItem(
            "waterproof_semiconductor_block", WATERPROOF_SEMICONDUCTOR_BLOCK);

        public static final Block MASS_ANALYZER = registerBlock("mass_analyzer", MassAnalyzerBlock::new,
            BlockBehaviour.Properties.of().strength(4.0F, 6.0F).requiresCorrectToolForDrops()
                .sound(SoundType.COPPER));
        public static final Item MASS_ANALYZER_ITEM = registerItem("mass_analyzer", MASS_ANALYZER);
        public static final BlockEntityType<MassAnalyzerBlockEntity> MASS_ANALYZER_ENTITY = Registry.register(
            BuiltInRegistries.BLOCK_ENTITY_TYPE,
            ResourceKey.create(Registries.BLOCK_ENTITY_TYPE, id("mass_analyzer")),
            new BlockEntityType<>(MassAnalyzerBlockEntity::new, Set.of(MASS_ANALYZER)));

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
