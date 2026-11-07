package com.bossmode.block;

import com.bossmode.config.BastionConfig;
import com.bossmode.config.BossConfig;
import com.bossmode.entity.bastion.BastionBehemothEntity;
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
 * Bastion Altar: calls the Bastion Behemoth with twelve Wyrm Shards, exactly like the Shard Altar calls the dragon. Found
 * in the Bastion Den; the arena it raises is the scorched Ember Waste.
 */
public class BastionAltarBlock extends ShardAltarBlock {
	private static final DustParticleOptions RITUAL_DUST = new DustParticleOptions(0xFF9A2A, 1.6F);

	public BastionAltarBlock(Properties properties) {
		super(properties);
	}

	@Override
	protected @Nullable Mob createBoss(ServerLevel level) {
		return ModEntities.BASTION_BEHEMOTH.create(level, EntitySpawnReason.TRIGGERED);
	}

	@Override
	public @Nullable Entity findBoss(ServerLevel level, BlockPos pos) {
		return level.getEntitiesOfClass(BastionBehemothEntity.class, new AABB(pos).inflate(BossConfig.ALTAR_DRAGON_EXCLUSION_RADIUS),
				BastionBehemothEntity::isAlive).stream().findFirst().orElse(null);
	}

	@Override
	protected ArenaTerraformer.Theme arenaTheme() {
		return ArenaTerraformer.Theme.EMBER;
	}

	@Override
	protected int summonDistance() {
		return BastionConfig.ALTAR_SUMMON_DISTANCE;
	}

	@Override
	protected String messagePrefix() {
		return "message.boss_mode.bastion_altar.";
	}

	@Override
	protected DustParticleOptions ritualDust() {
		return RITUAL_DUST;
	}

	@Override
	protected ParticleOptions ritualRingParticle() {
		return ParticleTypes.FLAME;
	}

	@Override
	protected ParticleOptions ritualPillarParticle() {
		return ParticleTypes.LAVA;
	}

	@Override
	protected ParticleOptions moteParticle() {
		return ParticleTypes.SMALL_FLAME;
	}
}
