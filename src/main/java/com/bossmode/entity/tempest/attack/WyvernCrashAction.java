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

/**
 * Shot down: forced when the Wyvern takes enough damage during one flight. It tumbles out of the sky, crashes and lies
 * stunned - EXPOSED - for almost three seconds: the reward for shooting it down.
 */
public final class WyvernCrashAction extends TempestAction {
	public WyvernCrashAction() {
		super("crash", true, TempestConfig.CRASH_IMPACT_TICK, 1, TempestConfig.CRASH_TOTAL_TICKS, 1, 1);
	}

	@Override
	public boolean canUse(ServerLevel level, TempestWyvernEntity boss, LivingEntity target, double gap) {
		return true;
	}

	@Override
	public void start(ServerLevel level, TempestWyvernEntity boss, @Nullable LivingEntity target, ActionContext ctx) {
		boss.setFlying(false);
		boss.playSound(ModSounds.TEMPEST_WYVERN_HURT, 6.0F, 0.6F);
		boss.playSound(ModSounds.TEMPEST_WYVERN_ROAR, 5.0F, 1.4F);
	}

	@Override
	public Result tick(ServerLevel level, TempestWyvernEntity boss, @Nullable LivingEntity target, ActionContext ctx, int tick) {
		if (tick < prepareTicks()) {
			Vec3 v = boss.getDeltaMovement();
			boss.setDeltaMovement(v.x * 0.8, Math.min(v.y, -0.9), v.z * 0.8);
			if (tick % 2 == 0) {
				level.sendParticles(ParticleTypes.LARGE_SMOKE, boss.getX(), boss.getY(0.6), boss.getZ(), 6, boss.getBbWidth() * 0.3, 0.5, boss.getBbWidth() * 0.3, 0.02);
			}
			return Result.CONTINUE;
		}
		if (tick == prepareTicks()) {
			if (!boss.onGround()) {
				boss.setPos(boss.getX(), boss.groundBelow(level), boss.getZ());
			}
			boss.setDeltaMovement(Vec3.ZERO);
			boss.playSound(ModSounds.TEMPEST_WYVERN_THUNDER, 6.0F, 0.5F);
			double radius = TempestConfig.CRASH_RADIUS * boss.sizeFactor() + boss.getBbWidth() * 0.5;
			level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, boss.getX(), boss.getY() + 1.0, boss.getZ(), 1, 0, 0, 0, 0);
			float damage = scaled(boss, TempestConfig.CRASH_DAMAGE);
			for (LivingEntity victim : CombatUtil.targetsInRadius(level, boss, boss.position(), radius)) {
				if (ctx.markHit(victim.getUUID()) && victim.hurtServer(level, boss.damageSources().mobAttack(boss), damage)) {
					CombatUtil.knockbackFrom(victim, boss.position(), 1.5, 0.6);
				}
			}
			return Result.CONTINUE;
		}
		// stunned: sparks crawl over it while it lies EXPOSED
		boss.setDeltaMovement(0, boss.getDeltaMovement().y, 0);
		if (tick % 6 == 0) {
			Vec3 head = boss.mouthPosition();
			level.sendParticles(ParticleTypes.CRIT, head.x, head.y, head.z, 8, 0.6, 0.4, 0.6, 0.1);
			level.sendParticles(ParticleTypes.ELECTRIC_SPARK, boss.getX(), boss.getY(0.5), boss.getZ(), 8, boss.getBbWidth() * 0.3, 0.6, boss.getBbWidth() * 0.3, 0.1);
		}
		return Result.CONTINUE;
	}
}
