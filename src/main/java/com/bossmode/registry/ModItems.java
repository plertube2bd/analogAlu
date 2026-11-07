package com.bossmode.registry;

import com.bossmode.BossMode;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.SpawnEggItem;

import java.util.function.Function;

public final class ModItems {
	/** Boss drop, also the item the crystal projectiles render as. */
	public static final Item WYRM_SHARD = register("wyrm_shard", Item::new, new Item.Properties().rarity(Rarity.RARE));

	/** The summoning altar; found in Shard Shrines, can be mined (diamond pickaxe) and moved. */
	public static final Item SHARD_ALTAR = register("shard_altar", p -> new BlockItem(ModBlocks.SHARD_ALTAR, p),
			new Item.Properties().useBlockDescriptionPrefix().rarity(Rarity.EPIC));

	/** The Tempest Wyvern's altar (Tempest Aerie) and the Bastion Behemoth's (Bastion Den). */
	public static final Item TEMPEST_ALTAR = register("tempest_altar", p -> new BlockItem(ModBlocks.TEMPEST_ALTAR, p),
			new Item.Properties().useBlockDescriptionPrefix().rarity(Rarity.EPIC));
	public static final Item BASTION_ALTAR = register("bastion_altar", p -> new BlockItem(ModBlocks.BASTION_ALTAR, p),
			new Item.Properties().useBlockDescriptionPrefix().rarity(Rarity.EPIC));

	/** Boss trophies: one of each opens the Void Altar. The Wyrm Heart drops from every slain Shard Dragon. */
	public static final Item WYRM_HEART = register("wyrm_heart", Item::new, new Item.Properties().rarity(Rarity.EPIC).stacksTo(16).fireResistant());
	/** Dropped by every slain Tempest Wyvern. */
	public static final Item TEMPEST_EYE = register("tempest_eye", Item::new, new Item.Properties().rarity(Rarity.EPIC).stacksTo(16).fireResistant());
	/** Dropped by every slain Bastion Behemoth. */
	public static final Item BEHEMOTH_CORE = register("behemoth_core", Item::new, new Item.Properties().rarity(Rarity.EPIC).stacksTo(16).fireResistant());
	/** Only what the Tempest Wyvern's feathers render as. */
	public static final Item STORM_FEATHER = register("storm_feather", Item::new, new Item.Properties());
	/** The final altar (Void Sanctum). */
	public static final Item VOID_ALTAR = register("void_altar", p -> new BlockItem(ModBlocks.VOID_ALTAR, p),
			new Item.Properties().useBlockDescriptionPrefix().rarity(Rarity.EPIC));
	/** Only what the Void Sovereign's orbs render as. */
	public static final Item VOID_ORB = register("void_orb", Item::new, new Item.Properties());

	public static final Item SHARD_DRAGON_SPAWN_EGG = egg("shard_dragon_spawn_egg", ModEntities.SHARD_DRAGON, Rarity.EPIC);
	public static final Item SHARD_WARLORD_SPAWN_EGG = egg("shard_warlord_spawn_egg", ModEntities.SHARD_WARLORD, Rarity.RARE);
	public static final Item SHARD_HOUND_SPAWN_EGG = egg("shard_hound_spawn_egg", ModEntities.SHARD_HOUND, Rarity.COMMON);
	public static final Item SHARD_SPITTER_SPAWN_EGG = egg("shard_spitter_spawn_egg", ModEntities.SHARD_SPITTER, Rarity.COMMON);
	public static final Item VOLATILE_HOUND_SPAWN_EGG = egg("volatile_hound_spawn_egg", ModEntities.VOLATILE_HOUND, Rarity.COMMON);
	public static final Item VOID_SOVEREIGN_SPAWN_EGG = egg("void_sovereign_spawn_egg", ModEntities.VOID_SOVEREIGN, Rarity.EPIC);
	public static final Item TEMPEST_WYVERN_SPAWN_EGG = egg("tempest_wyvern_spawn_egg", ModEntities.TEMPEST_WYVERN, Rarity.EPIC);
	public static final Item BASTION_BEHEMOTH_SPAWN_EGG = egg("bastion_behemoth_spawn_egg", ModEntities.BASTION_BEHEMOTH, Rarity.EPIC);

	// 26.3 keeps the vanilla tab keys private, so they are rebuilt from their registry ids.
	private static final ResourceKey<CreativeModeTab> SPAWN_EGGS_TAB = vanillaTab("spawn_eggs");
	private static final ResourceKey<CreativeModeTab> INGREDIENTS_TAB = vanillaTab("ingredients");
	private static final ResourceKey<CreativeModeTab> FUNCTIONAL_TAB = vanillaTab("functional_blocks");

	private ModItems() {
	}

	private static Item egg(String name, EntityType<?> type, Rarity rarity) {
		return register(name, SpawnEggItem::new, new Item.Properties().spawnEgg(type).rarity(rarity));
	}

	private static Item register(String name, Function<Item.Properties, Item> factory, Item.Properties properties) {
		ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, BossMode.id(name));
		return Registry.register(BuiltInRegistries.ITEM, key, factory.apply(properties.setId(key)));
	}

	private static ResourceKey<CreativeModeTab> vanillaTab(String path) {
		return ResourceKey.create(Registries.CREATIVE_MODE_TAB, Identifier.withDefaultNamespace(path));
	}

	public static void init() {
		CreativeModeTabEvents.modifyOutputEvent(SPAWN_EGGS_TAB).register(output -> {
			output.accept(VOID_SOVEREIGN_SPAWN_EGG);
			output.accept(SHARD_DRAGON_SPAWN_EGG);
			output.accept(TEMPEST_WYVERN_SPAWN_EGG);
			output.accept(BASTION_BEHEMOTH_SPAWN_EGG);
			output.accept(SHARD_WARLORD_SPAWN_EGG);
			output.accept(SHARD_HOUND_SPAWN_EGG);
			output.accept(SHARD_SPITTER_SPAWN_EGG);
			output.accept(VOLATILE_HOUND_SPAWN_EGG);
		});
		CreativeModeTabEvents.modifyOutputEvent(INGREDIENTS_TAB).register(output -> {
			output.accept(WYRM_SHARD);
			output.accept(WYRM_HEART);
			output.accept(TEMPEST_EYE);
			output.accept(BEHEMOTH_CORE);
		});
		CreativeModeTabEvents.modifyOutputEvent(FUNCTIONAL_TAB).register(output -> {
			output.accept(SHARD_ALTAR);
			output.accept(TEMPEST_ALTAR);
			output.accept(BASTION_ALTAR);
			output.accept(VOID_ALTAR);
		});
	}
}
