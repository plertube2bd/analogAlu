package com.bossmode.block;

import com.bossmode.config.BossConfig;
import com.bossmode.config.TempestConfig;
import com.bossmode.entity.tempest.TempestWyvernEntity;
import com.bossmode.registry.ModEntities;
import com.bossmode.world.ArenaTerraformer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.AABB;
import org.jspecify.annotations.Nullable;

/**
 * Tempest Altar: calls the Tempest Wyvern with twelve Wyrm Shards, exactly like the Shard Altar calls the dragon. Found at
 * the heart of the Tempest Aerie; the arena it raises is the storm-swept Storm Reach.
 */
public class TempestAltarBlock extends ShardAltarBlock {
	private static final DustParticleOptions RITUAL_DUST = new DustParticleOptions(0x4FE0FF, 1.6F);

	public TempestAltarBlock(Properties properties) {
		super(properties);
	}

	@Override
	protected @Nullable Mob createBoss(ServerLevel level) {
		return ModEntities.TEMPEST_WYVERN.create(level, EntitySpawnReason.TRIGGERED);
	}

	@Override
	public @Nullable Entity findBoss(ServerLevel level, BlockPos pos) {
		return level.getEntitiesOfClass(TempestWyvernEntity.class, new AABB(pos).inflate(BossConfig.ALTAR_DRAGON_EXCLUSION_RADIUS),
				TempestWyvernEntity::isAlive).stream().findFirst().orElse(null);
	}

	@Override
	protected ArenaTerraformer.Theme arenaTheme() {
		return ArenaTerraformer.Theme.STORM;
	}

	@Override
	protected int summonDistance() {
		return TempestConfig.ALTAR_SUMMON_DISTANCE;
	}

	@Override
	protected String messagePrefix() {
		return "message.boss_mode.tempest_altar.";
	}

	@Override
	protected DustParticleOptions ritualDust() {
		return RITUAL_DUST;
	}

	@Override
	protected ParticleOptions ritualRingParticle() {
		return ParticleTypes.ELECTRIC_SPARK;
	}

	@Override
	protected ParticleOptions ritualPillarParticle() {
		return ParticleTypes.CLOUD;
	}
}
