package com.bossmode.entity.bastion.attack;

import com.bossmode.config.BastionConfig;
import com.bossmode.entity.action.ActionContext;
import com.bossmode.entity.bastion.BastionBehemothEntity;
import com.bossmode.registry.ModSounds;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/** Summon: the Behemoth stamps and bellows, and shard hounds claw their way out of the cracked ground around it. */
public final class BastionSummonAttack extends BastionAction {
	public BastionSummonAttack() {
		super("summon", true, BastionConfig.SUMMON_HIT_TICK, 1, BastionConfig.SUMMON_TOTAL_TICKS, BastionConfig.SUMMON_COOLDOWN,
				BastionConfig.SUMMON_WEIGHT);
	}

	@Override
	public boolean canUse(ServerLevel level, BastionBehemothEntity boss, LivingEntity target, double gap) {
		return !boss.getPhase().minions().isEmpty() && boss.countMinions(level) < BastionConfig.MAX_ACTIVE_MINIONS;
	}

	@Override
	public void start(ServerLevel level, BastionBehemothEntity boss, @Nullable LivingEntity target, ActionContext ctx) {
		boss.playSound(ModSounds.BASTION_BEHEMOTH_SUMMON, 5.0F, 0.8F);
		int count = Math.min(boss.getPhase().summonCount(), BastionConfig.MAX_ACTIVE_MINIONS - boss.countMinions(level));
		ctx.points.addAll(boss.ringAround(level, count, boss.getBbWidth() * 0.5 + 5.0));
	}

	@Override
	public Result tick(ServerLevel level, BastionBehemothEntity boss, @Nullable LivingEntity target, ActionContext ctx, int tick) {
		if (tick < prepareTicks()) {
			if (tick % 2 == 0) {
				for (Vec3 p : ctx.points) {
					level.sendParticles(debris(level, p), p.x, p.y + 0.2, p.z, 4, 0.4, 0.1, 0.4, 0.1);
					level.sendParticles(ParticleTypes.SMOKE, p.x, p.y + 0.3, p.z, 2, 0.3, 0.1, 0.3, 0.01);
				}
			}
		} else if (tick == prepareTicks()) {
			boss.spawnMinionsAt(level, ctx.points, target);
		}
		return Result.CONTINUE;
	}
}
