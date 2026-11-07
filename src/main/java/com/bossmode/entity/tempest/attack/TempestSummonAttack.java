package com.bossmode.entity.tempest.attack;

import com.bossmode.config.TempestConfig;
import com.bossmode.entity.action.ActionContext;
import com.bossmode.entity.tempest.TempestWyvernEntity;
import com.bossmode.registry.ModSounds;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/** Summon: the storm answers - breezes (and in later phases wraiths) whirl out of the wind around the Wyvern. */
public final class TempestSummonAttack extends TempestAction {
	public TempestSummonAttack() {
		super("summon", true, TempestConfig.SUMMON_HIT_TICK, 1, TempestConfig.SUMMON_TOTAL_TICKS, TempestConfig.SUMMON_COOLDOWN, TempestConfig.SUMMON_WEIGHT);
	}

	@Override
	public boolean canUse(ServerLevel level, TempestWyvernEntity boss, LivingEntity target, double gap) {
		return !boss.getPhase().minions().isEmpty() && boss.countMinions(level) < TempestConfig.MAX_ACTIVE_MINIONS;
	}

	@Override
	public void start(ServerLevel level, TempestWyvernEntity boss, @Nullable LivingEntity target, ActionContext ctx) {
		boss.playSound(ModSounds.TEMPEST_WYVERN_SUMMON, 5.0F, 0.9F);
		int count = Math.min(boss.getPhase().summonCount(), TempestConfig.MAX_ACTIVE_MINIONS - boss.countMinions(level));
		ctx.points.addAll(boss.ringAround(level, count, boss.getBbWidth() * 0.5 + 5.0));
	}

	@Override
	public Result tick(ServerLevel level, TempestWyvernEntity boss, @Nullable LivingEntity target, ActionContext ctx, int tick) {
		if (tick < prepareTicks()) {
			if (tick % 2 == 0) {
				for (Vec3 p : ctx.points) {
					level.sendParticles(ParticleTypes.CLOUD, p.x, p.y + 0.5, p.z, 3, 0.4, 0.4, 0.4, 0.05);
					level.sendParticles(ParticleTypes.GUST_EMITTER_SMALL, p.x, p.y + 0.5, p.z, 1, 0, 0, 0, 0);
				}
			}
		} else if (tick == prepareTicks()) {
			boss.spawnMinionsAt(level, ctx.points, target);
		}
		return Result.CONTINUE;
	}
}
