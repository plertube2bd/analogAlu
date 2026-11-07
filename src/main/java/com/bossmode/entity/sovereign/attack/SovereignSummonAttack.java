package com.bossmode.entity.sovereign.attack;

import com.bossmode.config.SovereignConfig;
import com.bossmode.entity.action.ActionContext;
import com.bossmode.entity.minion.MinionType;
import com.bossmode.entity.sovereign.SovereignPhase;
import com.bossmode.entity.sovereign.VoidSovereignEntity;
import com.bossmode.registry.ModSounds;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/** Summon: the Shard Dragon's legion answers the Sovereign - the phase's minions crawl out of rifts around it. */
public final class SovereignSummonAttack extends SovereignAction {
	public SovereignSummonAttack() {
		super("summon", true, SovereignConfig.SUMMON_HIT_TICK, 1, SovereignConfig.SUMMON_TOTAL_TICKS, SovereignConfig.SUMMON_COOLDOWN,
				SovereignConfig.SUMMON_WEIGHT);
	}

	@Override
	public boolean canUse(ServerLevel level, VoidSovereignEntity boss, LivingEntity target, double gap) {
		return !boss.getPhase().minions().isEmpty() && boss.countMinions(level) < SovereignConfig.MAX_ACTIVE_MINIONS;
	}

	@Override
	public void start(ServerLevel level, VoidSovereignEntity boss, @Nullable LivingEntity target, ActionContext ctx) {
		boss.playSound(ModSounds.VOID_SOVEREIGN_SUMMON, 5.0F, 0.7F);
		int count = Math.min(boss.getPhase().summonCount(), SovereignConfig.MAX_ACTIVE_MINIONS - boss.countMinions(level));
		ctx.points.addAll(boss.ringAround(level, count, boss.getBbWidth() * 0.5 + 5.0));
	}

	@Override
	public Result tick(ServerLevel level, VoidSovereignEntity boss, @Nullable LivingEntity target, ActionContext ctx, int tick) {
		if (tick < prepareTicks()) {
			if (tick % 2 == 0) {
				for (Vec3 p : ctx.points) {
					level.sendParticles(ParticleTypes.REVERSE_PORTAL, p.x, p.y + 1, p.z, 4, 0.3, 0.6, 0.3, 0.05);
					level.sendParticles(ParticleTypes.SQUID_INK, p.x, p.y + 0.2, p.z, 2, 0.4, 0.05, 0.4, 0.01);
				}
			}
		} else if (tick == prepareTicks()) {
			SovereignPhase phase = boss.getPhase();
			for (Vec3 p : ctx.points) {
				if (boss.countMinions(level) >= SovereignConfig.MAX_ACTIVE_MINIONS || phase.minions().isEmpty()) {
					break;
				}
				MinionType type = phase.minions().get(boss.getRandom().nextInt(phase.minions().size()));
				type.spawn(level, boss, type == MinionType.WRAITH ? p.add(0, 1.5, 0) : p, target);
			}
		}
		return Result.CONTINUE;
	}
}
