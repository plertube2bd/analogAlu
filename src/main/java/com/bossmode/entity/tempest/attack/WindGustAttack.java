package com.bossmode.entity.tempest.attack;

import com.bossmode.config.TempestConfig;
import com.bossmode.entity.action.ActionContext;
import com.bossmode.entity.boss.combat.CombatUtil;
import com.bossmode.entity.tempest.TempestWyvernEntity;
import com.bossmode.registry.ModSounds;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/** Wind Gust: one enormous wingbeat blasts everything in front of the Wyvern away (on the ground or from the air). */
public final class WindGustAttack extends TempestAction {
	public WindGustAttack() {
		super("wind_gust", true, TempestConfig.GUST_HIT_TICK, 1, TempestConfig.GUST_TOTAL_TICKS, TempestConfig.GUST_COOLDOWN, TempestConfig.GUST_WEIGHT);
	}

	@Override
	public boolean canUse(ServerLevel level, TempestWyvernEntity boss, LivingEntity target, double gap) {
		return gap <= TempestConfig.GUST_RANGE * boss.sizeFactor() * 0.75;
	}

	@Override
	public Result tick(ServerLevel level, TempestWyvernEntity boss, @Nullable LivingEntity target, ActionContext ctx, int tick) {
		if (tick < prepareTicks()) {
			if (target != null) {
				boss.faceTarget(target, 20.0F);
			}
			if (tick % 3 == 0) {
				level.sendParticles(ParticleTypes.CLOUD, boss.getX(), boss.getY(0.7), boss.getZ(), 6, boss.getBbWidth() * 0.5, 0.4, boss.getBbWidth() * 0.5, 0.05);
			}
			return Result.CONTINUE;
		}
		if (tick == prepareTicks()) {
			boss.playSound(ModSounds.TEMPEST_WYVERN_GUST, 5.0F, 0.8F);
			double range = TempestConfig.GUST_RANGE * boss.sizeFactor();
			Vec3 forward = CombatUtil.horizontalForward(boss);
			Vec3 origin = boss.position().add(0, boss.getBbHeight() * 0.5, 0);
			double down = boss.isFlying() ? -0.35 : 0.0;
			for (int i = 0; i < 40; i++) {
				double spread = (boss.getRandom().nextDouble() - 0.5) * Math.toRadians(TempestConfig.GUST_CONE_DEGREES);
				Vec3 dir = forward.yRot((float) spread);
				level.sendParticles(ParticleTypes.CLOUD, origin.x, origin.y, origin.z, 0, dir.x, down, dir.z, 1.2 + boss.getRandom().nextDouble());
			}
			float damage = scaled(boss, TempestConfig.GUST_DAMAGE);
			for (LivingEntity victim : CombatUtil.targetsInRadius(level, boss, boss.position(), range)) {
				if (CombatUtil.isInFront(boss, victim, TempestConfig.GUST_CONE_DEGREES) && ctx.markHit(victim.getUUID())) {
					victim.hurtServer(level, boss.damageSources().mobAttack(boss), damage);
					CombatUtil.knockback(victim, forward.x, forward.z, TempestConfig.GUST_KNOCKBACK, 0.5);
				}
			}
		}
		return Result.CONTINUE;
	}
}
