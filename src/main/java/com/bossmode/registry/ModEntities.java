package com.bossmode.registry;

import com.bossmode.config.BastionConfig;
import com.bossmode.config.SovereignConfig;
import com.bossmode.config.TempestConfig;
import com.bossmode.entity.bastion.BastionBehemothEntity;
import com.bossmode.entity.projectile.BoulderProjectile;
import com.bossmode.entity.tempest.TempestWyvernEntity;
import com.bossmode.entity.projectile.VoidOrbProjectile;
import com.bossmode.entity.sovereign.VoidPylonEntity;
import com.bossmode.entity.sovereign.VoidSovereignEntity;
import com.bossmode.BossMode;
import com.bossmode.config.BossConfig;
import com.bossmode.entity.boss.ShardDragonEntity;
import com.bossmode.entity.minion.ShardHoundEntity;
import com.bossmode.entity.projectile.WyrmShardProjectile;
import com.bossmode.entity.warlord.ShardWarlordEntity;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

public final class ModEntities {
	public static final Identifier SHARD_DRAGON_ID = BossMode.id("shard_dragon");

	/** Boss. Hitbox is at scale 1; the SCALE attribute (2.0 -> 4.0 by phase) grows it to 6 x 5.8 -> 12 x 11.6 blocks. */
	public static final EntityType<ShardDragonEntity> SHARD_DRAGON = register("shard_dragon",
			EntityType.Builder.of(ShardDragonEntity::new, MobCategory.MONSTER)
					.sized(BossConfig.HITBOX_WIDTH, BossConfig.HITBOX_HEIGHT)
					.eyeHeight(BossConfig.HITBOX_HEIGHT * 0.8F)
					.fireImmune()
					.clientTrackingRange(12));

	/** Minions (Mob.obj), 1.2 x 1.3 blocks. */
	public static final EntityType<ShardHoundEntity> SHARD_HOUND = register("shard_hound", hound());
	public static final EntityType<ShardHoundEntity> SHARD_SPITTER = register("shard_spitter", hound());
	public static final EntityType<ShardHoundEntity> VOLATILE_HOUND = register("volatile_hound", hound());

	/** Mid-boss (Mob2.obj), 1.1 x 2.2 blocks at scale 1 (spawns at scale 1.5). */
	public static final EntityType<ShardWarlordEntity> SHARD_WARLORD = register("shard_warlord",
			EntityType.Builder.of(ShardWarlordEntity::new, MobCategory.MONSTER)
					.sized(1.1F, 2.2F)
					.eyeHeight(2.0F)
					.fireImmune()
					.clientTrackingRange(10));

	/** Final boss (Final BOSS.obj). 4.4 x 7.2 blocks at scale 1, grown by the SCALE attribute to x1.4 in phase 6. */
	public static final EntityType<VoidSovereignEntity> VOID_SOVEREIGN = register("void_sovereign",
			EntityType.Builder.of(VoidSovereignEntity::new, MobCategory.MONSTER)
					.sized(SovereignConfig.HITBOX_WIDTH, SovereignConfig.HITBOX_HEIGHT)
					.eyeHeight(SovereignConfig.HITBOX_HEIGHT * 0.85F)
					.fireImmune()
					.clientTrackingRange(14));

	/** Flying raid boss (BOSS2.obj). 3.0 x 3.2 blocks at scale 1; the SCALE attribute (1.6 -> 2.2) grows it to 4.8 x 5.1 -> 6.6 x 7.0. */
	public static final EntityType<TempestWyvernEntity> TEMPEST_WYVERN = register("tempest_wyvern",
			EntityType.Builder.of(TempestWyvernEntity::new, MobCategory.MONSTER)
					.sized(TempestConfig.HITBOX_WIDTH, TempestConfig.HITBOX_HEIGHT)
					.eyeHeight(TempestConfig.HITBOX_HEIGHT * 0.85F)
					.fireImmune()
					.clientTrackingRange(14));

	/** Armoured raid boss (BOSS3.obj). 5.0 x 4.6 blocks at scale 1, grown by the SCALE attribute to x1.35 in phase 8. */
	public static final EntityType<BastionBehemothEntity> BASTION_BEHEMOTH = register("bastion_behemoth",
			EntityType.Builder.of(BastionBehemothEntity::new, MobCategory.MONSTER)
					.sized(BastionConfig.HITBOX_WIDTH, BastionConfig.HITBOX_HEIGHT)
					.eyeHeight(BastionConfig.HITBOX_HEIGHT * 0.75F)
					.fireImmune()
					.clientTrackingRange(12));

	/** The Bastion Behemoth's molten boulders. */
	public static final EntityType<BoulderProjectile> BOULDER = register("boulder",
			EntityType.Builder.<BoulderProjectile>of(BoulderProjectile::new, MobCategory.MISC)
					.sized(1.2F, 1.2F)
					.clientTrackingRange(8)
					.updateInterval(2)
					.noLootTable());

	/** The Void Shield's pylons (phases 3 and 5). */
	public static final EntityType<VoidPylonEntity> VOID_PYLON = register("void_pylon",
			EntityType.Builder.of(VoidPylonEntity::new, MobCategory.MISC)
					.sized(1.2F, 3.0F)
					.eyeHeight(1.5F)
					.fireImmune()
					.noSummon()
					.noLootTable()
					.clientTrackingRange(10));

	public static final EntityType<VoidOrbProjectile> VOID_ORB = register("void_orb",
			EntityType.Builder.<VoidOrbProjectile>of(VoidOrbProjectile::new, MobCategory.MISC)
					.sized(0.8F, 0.8F)
					.clientTrackingRange(6)
					.updateInterval(2)
					.noLootTable());

	public static final EntityType<WyrmShardProjectile> WYRM_SHARD = register("wyrm_shard",
			EntityType.Builder.<WyrmShardProjectile>of(WyrmShardProjectile::new, MobCategory.MISC)
					.sized(0.5F, 0.5F)
					.clientTrackingRange(4)
					.updateInterval(10)
					.noLootTable());

	private ModEntities() {
	}

	private static EntityType.Builder<ShardHoundEntity> hound() {
		return EntityType.Builder.of(ShardHoundEntity::new, MobCategory.MONSTER).sized(1.2F, 1.3F).eyeHeight(0.9F).clientTrackingRange(8);
	}

	private static <T extends Entity> EntityType<T> register(String name, EntityType.Builder<T> builder) {
		ResourceKey<EntityType<?>> key = ResourceKey.create(Registries.ENTITY_TYPE, BossMode.id(name));
		return Registry.register(BuiltInRegistries.ENTITY_TYPE, key, builder.build(key));
	}

	public static void init() {
		FabricDefaultAttributeRegistry.register(SHARD_DRAGON, ShardDragonEntity.createAttributes());
		FabricDefaultAttributeRegistry.register(SHARD_HOUND, ShardHoundEntity.createHoundAttributes());
		FabricDefaultAttributeRegistry.register(SHARD_SPITTER, ShardHoundEntity.createSpitterAttributes());
		FabricDefaultAttributeRegistry.register(VOLATILE_HOUND, ShardHoundEntity.createVolatileAttributes());
		FabricDefaultAttributeRegistry.register(SHARD_WARLORD, ShardWarlordEntity.createAttributes());
		FabricDefaultAttributeRegistry.register(VOID_SOVEREIGN, VoidSovereignEntity.createAttributes());
		FabricDefaultAttributeRegistry.register(VOID_PYLON, VoidPylonEntity.createAttributes());
		FabricDefaultAttributeRegistry.register(TEMPEST_WYVERN, TempestWyvernEntity.createAttributes());
		FabricDefaultAttributeRegistry.register(BASTION_BEHEMOTH, BastionBehemothEntity.createAttributes());
	}
}
