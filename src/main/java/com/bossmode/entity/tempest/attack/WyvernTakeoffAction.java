package com.bossmode.entity.tempest.attack;

import com.bossmode.config.TempestConfig;
import com.bossmode.entity.action.ActionContext;
import com.bossmode.entity.boss.combat.CombatUtil;
import com.bossmode.entity.tempest.TempestWyvernEntity;
import com.bossmode.registry.ModSounds;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Takeoff (flight cycle): a crouch, then one great downstroke that batters everything around the Wyvern and lifts it
 * to cruising height. Forced when its ground time is up - never picked at random.
 */
public final class WyvernTakeoffAction extends TempestAction {
	public WyvernTakeoffAction() {
		super("takeoff", true, TempestConfig.TAKEOFF_LAUNCH_TICK, TempestConfig.TAKEOFF_TOTAL_TICKS - TempestConfig.TAKEOFF_LAUNCH_TICK - 2,
				TempestConfig.TAKEOFF_TOTAL_TICKS, 1, 1);
	}

	@Override
	public boolean canUse(ServerLevel level, TempestWyvernEntity boss, LivingEntity target, double gap) {
		return true;
	}

	@Override
	public Result tick(ServerLevel level, TempestWyvernEntity boss, @Nullable LivingEntity target, ActionContext ctx, int tick) {
		if (tick < prepareTicks()) {
			boss.setDeltaMovement(0, boss.getDeltaMovement().y, 0);
			if (target != null) {
				boss.faceTarget(target, 15.0F);
			}
			return Result.CONTINUE;
		}
		if (tick == prepareTicks()) {
			boss.setFlying(true);
			boss.playSound(ModSounds.TEMPEST_WYVERN_GUST, 5.0F, 1.1F);
			double radius = TempestConfig.TAKEOFF_RADIUS * boss.sizeFactor() + boss.getBbWidth() * 0.5;
			for (int i = 0; i < 40; i++) {
				double a = Math.PI * 2 * i / 40;
				level.sendParticles(ParticleTypes.CLOUD, boss.getX() + Math.cos(a) * boss.getBbWidth() * 0.5, boss.getY() + 0.3,
						boss.getZ() + Math.sin(a) * boss.getBbWidth() * 0.5, 0, Math.cos(a), 0.05, Math.sin(a), 0.8);
			}
			float damage = scaled(boss, TempestConfig.TAKEOFF_DAMAGE);
			for (LivingEntity victim : CombatUtil.targetsInRadius(level, boss, boss.position(), radius)) {
				if (ctx.markHit(victim.getUUID()) && victim.hurtServer(level, boss.damageSources().mobAttack(boss), damage)) {
					CombatUtil.knockbackFrom(victim, boss.position(), 1.3, 0.5);
				}
			}
		}
		double climb = Mth.clamp((boss.flightAltitude(level, target) - boss.getY()) * 0.12, 0.0, TempestConfig.TAKEOFF_RISE_SPEED);
		Vec3 v = boss.getDeltaMovement();
		boss.setDeltaMovement(v.x * 0.8, climb, v.z * 0.8);
		if (target != null) {
			boss.faceTarget(target, 10.0F);
		}
		return Result.CONTINUE;
	}
}
