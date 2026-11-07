package com.bossmode.entity.tempest.attack;

import com.bossmode.config.TempestConfig;
import com.bossmode.entity.action.ActionContext;
import com.bossmode.entity.boss.combat.CombatUtil;
import com.bossmode.entity.tempest.TempestWyvernEntity;
import com.bossmode.registry.ModSounds;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Storm Bolts: the Wyvern calls the storm - glowing circles appear under everyone nearby and around its target, and a
 * second later lightning strikes each of them in turn. Step out of the circles.
 */
public final class StormBoltsAttack extends TempestAction {
	public StormBoltsAttack() {
		super("storm_bolts", true, TempestConfig.BOLTS_MARK_TICK, 1, TempestConfig.BOLTS_TOTAL_TICKS, TempestConfig.BOLTS_COOLDOWN, TempestConfig.BOLTS_WEIGHT);
	}

	@Override
	public boolean canUse(ServerLevel level, TempestWyvernEntity boss, LivingEntity target, double gap) {
		return gap <= TempestConfig.BOLTS_RANGE;
	}

	@Override
	public Result tick(ServerLevel level, TempestWyvernEntity boss, @Nullable LivingEntity target, ActionContext ctx, int tick) {
		if (tick < prepareTicks()) {
			if (target != null) {
				boss.faceTarget(target, 15.0F);
			}
			if (tick % 3 == 0) {
				level.sendParticles(ParticleTypes.ELECTRIC_SPARK, boss.getX(), boss.getY(1.0) + 1.0, boss.getZ(), 10, 1.0, 0.6, 1.0, 0.2);
			}
			return Result.CONTINUE;
		}
		if (tick == prepareTicks()) {
			boss.playSound(ModSounds.TEMPEST_WYVERN_THUNDER, 4.0F, 1.3F);
			int count = TempestConfig.BOLTS_BASE + boss.getPhase().number() - 1 + (boss.isEnraged() ? 2 : 0);
			int i = 0;
			for (LivingEntity victim : CombatUtil.targetsInRadius(level, boss, boss.position(), TempestConfig.BOLTS_RANGE)) {
				if (victim instanceof net.minecraft.world.entity.player.Player && i < count) {
					ctx.scheduled.add(new ActionContext.Scheduled(tick + TempestConfig.BOLTS_DELAY + i * 2, ground(level, victim.position())));
					i++;
				}
			}
			Vec3 center = target != null ? target.position() : boss.position();
			for (; i < count; i++) {
				double a = boss.getRandom().nextDouble() * Math.PI * 2;
				double r = 2.0 + boss.getRandom().nextDouble() * TempestConfig.BOLTS_SCATTER;
				ctx.scheduled.add(new ActionContext.Scheduled(tick + TempestConfig.BOLTS_DELAY + i * 2,
						ground(level, center.add(Math.cos(a) * r, 0, Math.sin(a) * r))));
			}
			int last = ctx.scheduled.stream().mapToInt(ActionContext.Scheduled::tick).max().orElse(tick);
			ctx.extraRecoveryTicks = Math.max(0, last + 2 - totalTicks());
		}
		double radius = TempestConfig.BOLTS_RADIUS;
		for (ActionContext.Scheduled strike : ctx.scheduled) {
			Vec3 p = strike.position();
			if (tick < strike.tick()) {
				if (tick % 2 == 0) {
					ring(level, p, radius, TempestWyvernEntity.STORM_DUST, 14);
					level.sendParticles(ParticleTypes.ELECTRIC_SPARK, p.x, p.y + 0.2, p.z, 2, radius * 0.4, 0.05, radius * 0.4, 0.02);
				}
			} else if (tick == strike.tick()) {
				visualLightning(level, p);
				level.playSound(null, p.x, p.y, p.z, ModSounds.TEMPEST_WYVERN_THUNDER, SoundSource.HOSTILE, 3.0F, 0.9F + boss.getRandom().nextFloat() * 0.3F);
				level.sendParticles(ParticleTypes.ELECTRIC_SPARK, p.x, p.y + 0.5, p.z, 30, radius * 0.5, 0.5, radius * 0.5, 0.3);
				float damage = scaled(boss, TempestConfig.BOLTS_DAMAGE);
				for (LivingEntity victim : CombatUtil.targetsInRadius(level, boss, p.add(0, 1.0, 0), radius)) {
					victim.hurtServer(level, boss.damageSources().indirectMagic(boss, boss), damage);
				}
			}
		}
		return Result.CONTINUE;
	}
}
