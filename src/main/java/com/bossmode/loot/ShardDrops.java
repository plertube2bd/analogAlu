package com.bossmode.loot;

import com.bossmode.BossMode;
import com.bossmode.config.BossConfig;
import com.bossmode.registry.ModEntities;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import net.fabricmc.fabric.api.loot.v3.LootTableEvents;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/**
 * The single Wyrm Shard drop rule. The same pool is appended to the loot table of every hostile ({@link MobCategory#MONSTER})
 * entity - zombies, skeletons, creepers, spiders, endermen... and the dragon's own minions and mid-boss - so a shard is
 * exactly as likely from a natural spawn as from a raid summon: {@link BossConfig#SHARD_DROP_CHANCE} per player kill,
 * +{@link BossConfig#SHARD_DROP_LOOTING_BONUS} per Looting level. Bosses (Ender Dragon, Wither, Shard Dragon, Void Sovereign) are excluded;
 * the Shard Dragon keeps its own trophy table. Data-pack replacements of a table are left alone.
 */
public final class ShardDrops {
	private static final Set<EntityType<?>> EXCLUDED = Set.of(EntityTypes.ENDER_DRAGON, EntityTypes.WITHER, ModEntities.SHARD_DRAGON,
			ModEntities.VOID_SOVEREIGN, ModEntities.TEMPEST_WYVERN, ModEntities.BASTION_BEHEMOTH);

	private static Set<ResourceKey<LootTable>> dropTables;

	private ShardDrops() {
	}

	public static void init() {
		LootTableEvents.MODIFY.register((key, table, source, registries) -> {
			if (source.isBuiltin() && dropTables().contains(key)) {
				table.pool(createPool(registries));
			}
		});
	}

	/** Loot tables of every hostile entity type that receives the shard pool. */
	public static synchronized Set<ResourceKey<LootTable>> dropTables() {
		if (dropTables == null) {
			Set<ResourceKey<LootTable>> tables = new HashSet<>();
			for (EntityType<?> type : BuiltInRegistries.ENTITY_TYPE) {
				if (type.getCategory() == MobCategory.MONSTER && !EXCLUDED.contains(type)) {
					type.getDefaultLootTable().ifPresent(tables::add);
				}
			}
			dropTables = Set.copyOf(tables);
			BossMode.LOGGER.info("Wyrm Shard drop added to {} hostile loot tables", dropTables.size());
		}
		return dropTables;
	}

	/** Written in the vanilla loot-table JSON format and decoded with the game's own codec, like a data-pack pool. */
	private static LootPool createPool(HolderLookup.Provider registries) {
		String json = String.format(Locale.ROOT, """
				{
				  "rolls": 1,
				  "entries": [ { "type": "minecraft:item", "name": "boss_mode:wyrm_shard" } ],
				  "condition": {
				    "type": "minecraft:all_of",
				    "terms": [
				      { "type": "minecraft:killed_by_player" },
				      {
				        "type": "minecraft:random_chance_with_enchanted_bonus",
				        "enchantment": "minecraft:looting",
				        "unenchanted_chance": %1$s,
				        "enchanted_chance": { "type": "minecraft:linear", "base": %2$s, "per_level_above_first": %3$s }
				      }
				    ]
				  }
				}""", BossConfig.SHARD_DROP_CHANCE, BossConfig.SHARD_DROP_CHANCE + BossConfig.SHARD_DROP_LOOTING_BONUS,
				BossConfig.SHARD_DROP_LOOTING_BONUS);
		return LootPool.CODEC.parse(registries.createSerializationContext(JsonOps.INSTANCE), JsonParser.parseString(json))
				.getOrThrow(error -> new IllegalStateException("Invalid Wyrm Shard loot pool: " + error));
	}
}
