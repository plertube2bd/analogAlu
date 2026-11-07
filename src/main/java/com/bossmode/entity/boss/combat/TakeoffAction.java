package com.bossmode.entity.boss.combat;

import com.bossmode.config.BossConfig;
import com.bossmode.entity.action.ActionContext;
import com.bossmode.entity.action.BossAction;
import com.bossmode.entity.boss.ShardDragonEntity;
import com.bossmode.registry.ModSounds;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Takeoff (flight cycle, phases 6-10): a crouch, then one huge downstroke that batters everything around the dragon
 * and lifts it to cruising height. Forced by the dragon when its ground time is up - never picked at random.
 */
public final class TakeoffAction extends BossAction<ShardDragonEntity> {
	public TakeoffAction() {
		super("takeoff", true, BossConfig.TAKEOFF_LAUNCH_TICK, BossConfig.TAKEOFF_TOTAL_TICKS - BossConfig.TAKEOFF_LAUNCH_TICK - 2,
				BossConfig.TAKEOFF_TOTAL_TICKS);
	}

	@Override
	public int cooldown() {
		return 1;
	}

	@Override
	public String animationTrigger() {
		return "takeoff";
	}

	@Override
	public boolean canUse(ServerLevel level, ShardDragonEntity boss, LivingEntity target, double gap) {
		return true;
	}

	@Override
	public Result tick(ServerLevel level, ShardDragonEntity boss, @Nullable LivingEntity target, ActionContext ctx, int tick) {
		if (tick < prepareTicks()) {
			boss.setDeltaMovement(0, boss.getDeltaMovement().y, 0);
			if (target != null) {
				boss.faceTarget(target, 15.0F);
			}
			return Result.CONTINUE;
		}
		if (tick == prepareTicks()) {
			boss.setFlying(true);
			boss.playSound(ModSounds.SHARD_DRAGON_TEMPEST, 5.0F, 0.7F);
			double radius = BossConfig.TAKEOFF_RADIUS * boss.sizeFactor() + boss.getBbWidth() * 0.5;
			for (int i = 0; i < 40; i++) {
				double a = Math.PI * 2 * i / 40;
				level.sendParticles(ParticleTypes.CLOUD, boss.getX() + Math.cos(a) * boss.getBbWidth() * 0.5, boss.getY() + 0.3,
						boss.getZ() + Math.sin(a) * boss.getBbWidth() * 0.5, 0, Math.cos(a), 0.05, Math.sin(a), 0.8);
			}
			float damage = (float) (BossConfig.TAKEOFF_DAMAGE * boss.getDamageMultiplier());
			for (LivingEntity victim : CombatUtil.targetsInRadius(level, boss, boss.position(), radius)) {
				if (ctx.markHit(victim.getUUID()) && victim.hurtServer(level, boss.damageSources().mobAttack(boss), damage)) {
					CombatUtil.knockbackFrom(victim, boss.position(), 1.4, 0.6);
				}
			}
		}
		double climb = Mth.clamp((boss.flightAltitude(level, target) - boss.getY()) * 0.12, 0.0, BossConfig.TAKEOFF_RISE_SPEED);
		Vec3 v = boss.getDeltaMovement();
		boss.setDeltaMovement(v.x * 0.8, climb, v.z * 0.8);
		if (target != null) {
			boss.faceTarget(target, 10.0F);
		}
		return Result.CONTINUE;
	}
}
