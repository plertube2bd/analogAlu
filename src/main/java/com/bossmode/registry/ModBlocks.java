package com.bossmode.registry;

import com.bossmode.BossMode;
import com.bossmode.block.BastionAltarBlock;
import com.bossmode.block.ShardAltarBlock;
import com.bossmode.block.TempestAltarBlock;
import com.bossmode.block.VoidAltarBlock;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;

import java.util.function.Function;

public final class ModBlocks {
	/** Obsidian-hard (diamond pickaxe, blast proof, immovable) so neither the dragon nor its explosions can wreck it. */
	public static final Block SHARD_ALTAR = register("shard_altar", ShardAltarBlock::new, BlockBehaviour.Properties.of()
			.mapColor(MapColor.COLOR_PURPLE)
			.instrument(NoteBlockInstrument.BASEDRUM)
			.requiresCorrectToolForDrops()
			.strength(50.0F, 1200.0F)
			.sound(SoundType.LODESTONE)
			.lightLevel(ShardAltarBlock::lightLevel)
			.pushReaction(PushReaction.IMMOVEABLE)
			.noOcclusion());

	/** Twelve Wyrm Shards call the Tempest Wyvern (Tempest Aerie). Same hardness rules as the Shard Altar. */
	public static final Block TEMPEST_ALTAR = register("tempest_altar", TempestAltarBlock::new, BlockBehaviour.Properties.of()
			.mapColor(MapColor.COLOR_CYAN)
			.instrument(NoteBlockInstrument.BASEDRUM)
			.requiresCorrectToolForDrops()
			.strength(50.0F, 1200.0F)
			.sound(SoundType.LODESTONE)
			.lightLevel(ShardAltarBlock::lightLevel)
			.pushReaction(PushReaction.IMMOVEABLE)
			.noOcclusion());

	/** Twelve Wyrm Shards call the Bastion Behemoth (Bastion Den). Same hardness rules as the Shard Altar. */
	public static final Block BASTION_ALTAR = register("bastion_altar", BastionAltarBlock::new, BlockBehaviour.Properties.of()
			.mapColor(MapColor.COLOR_ORANGE)
			.instrument(NoteBlockInstrument.BASEDRUM)
			.requiresCorrectToolForDrops()
			.strength(50.0F, 1200.0F)
			.sound(SoundType.LODESTONE)
			.lightLevel(ShardAltarBlock::lightLevel)
			.pushReaction(PushReaction.IMMOVEABLE)
			.noOcclusion());

	/** The final altar: one trophy of each raid boss calls the Void Sovereign. Same hardness rules as the Shard Altar. */
	public static final Block VOID_ALTAR = register("void_altar", VoidAltarBlock::new, BlockBehaviour.Properties.of()
			.mapColor(MapColor.COLOR_BLACK)
			.instrument(NoteBlockInstrument.BASEDRUM)
			.requiresCorrectToolForDrops()
			.strength(50.0F, 1200.0F)
			.sound(SoundType.LODESTONE)
			.lightLevel(VoidAltarBlock::lightLevel)
			.pushReaction(PushReaction.IMMOVEABLE)
			.noOcclusion());

	private ModBlocks() {
	}

	private static Block register(String name, Function<BlockBehaviour.Properties, Block> factory, BlockBehaviour.Properties properties) {
		ResourceKey<Block> key = ResourceKey.create(Registries.BLOCK, BossMode.id(name));
		return Registry.register(BuiltInRegistries.BLOCK, key, factory.apply(properties.setId(key)));
	}

	public static void init() {
	}
}
