package com.bossmode.entity.sovereign.attack;

import com.bossmode.config.SovereignConfig;
import com.bossmode.entity.action.ActionContext;
import com.bossmode.entity.boss.combat.CombatUtil;
import com.bossmode.entity.sovereign.VoidSovereignEntity;
import com.bossmode.registry.ModSounds;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Void Step: the Sovereign sinks into the void, rises again right behind its target and brings both claws down. The
 * answer to running away and to hiding - listen for the sink and turn around.
 */
public final class VoidStepAttack extends SovereignAction {
	public VoidStepAttack() {
		super("void_step", true, SovereignConfig.STEP_VANISH_TICK, SovereignConfig.STEP_SLAM_TICK - SovereignConfig.STEP_VANISH_TICK + 1,
				SovereignConfig.STEP_TOTAL_TICKS, SovereignConfig.STEP_COOLDOWN, SovereignConfig.STEP_WEIGHT);
	}

	@Override
	public int weight(VoidSovereignEntity boss, LivingEntity target) {
		return boss.isTargetUnreachable() ? SovereignConfig.STEP_WEIGHT * 3 : SovereignConfig.STEP_WEIGHT;
	}

	@Override
	public boolean canUse(ServerLevel level, VoidSovereignEntity boss, LivingEntity target, double gap) {
		double distance = boss.distanceTo(target);
		return distance >= SovereignConfig.STEP_MIN_RANGE && distance <= SovereignConfig.STEP_MAX_RANGE;
	}

	@Override
	public void start(ServerLevel level, VoidSovereignEntity boss, @Nullable LivingEntity target, ActionContext ctx) {
		boss.playSound(ModSounds.VOID_SOVEREIGN_STEP, 4.0F, 0.6F);
	}

	@Override
	public Result tick(ServerLevel level, VoidSovereignEntity boss, @Nullable LivingEntity target, ActionContext ctx, int tick) {
		if (tick < prepareTicks()) {
			level.sendParticles(ParticleTypes.SQUID_INK, boss.getX(), boss.getY() + 0.3, boss.getZ(), 10, boss.getBbWidth() * 0.4, 0.1, boss.getBbWidth() * 0.4, 0.02);
			level.sendParticles(ParticleTypes.REVERSE_PORTAL, boss.getX(), boss.getY(0.5), boss.getZ(), 8, boss.getBbWidth() * 0.4, boss.getBbHeight() * 0.3, boss.getBbWidth() * 0.4, 0.05);
			return Result.CONTINUE;
		}
		if (tick == prepareTicks()) {
			Vec3 from = boss.position();
			Vec3 to = from;
			if (target != null && target.isAlive()) {
				Vec3 look = target.getLookAngle();
				Vec3 back = new Vec3(-look.x, 0, -look.z);
				if (back.lengthSqr() < 1.0E-4) {
					back = from.subtract(target.position()).multiply(1, 0, 1);
				}
				double behind = SovereignConfig.STEP_BEHIND + boss.getBbWidth() * 0.5;
				to = ground(level, target.position().add(back.normalize().scale(behind)));
			}
			boss.teleportTo(to.x, to.y, to.z);
			if (target != null) {
				boss.faceTarget(target, 360.0F);
			}
			boss.playSound(ModSounds.VOID_SOVEREIGN_STEP, 4.0F, 1.0F);
			level.sendParticles(ParticleTypes.REVERSE_PORTAL, from.x, from.y + 2, from.z, 60, 1.5, 2.0, 1.5, 0.2);
			level.sendParticles(ParticleTypes.REVERSE_PORTAL, to.x, to.y + 2, to.z, 60, 1.5, 2.0, 1.5, 0.2);
			level.sendParticles(ParticleTypes.SQUID_INK, to.x, to.y + 0.3, to.z, 30, 1.5, 0.1, 1.5, 0.05);
			return Result.CONTINUE;
		}
		if (tick < SovereignConfig.STEP_SLAM_TICK) {
			if (target != null) {
				boss.faceTarget(target, 25.0F);
			}
			return Result.CONTINUE;
		}
		if (tick == SovereignConfig.STEP_SLAM_TICK) {
			Vec3 front = boss.frontPoint(1.5);
			boss.playSound(ModSounds.VOID_SOVEREIGN_POUND, 5.0F, 0.9F);
			level.sendParticles(ParticleTypes.EXPLOSION, front.x, front.y + 0.5, front.z, 3, 1.0, 0.3, 1.0, 0);
			float damage = scaled(boss, SovereignConfig.STEP_DAMAGE);
			for (LivingEntity victim : CombatUtil.targetsInRadius(level, boss, front, SovereignConfig.STEP_RADIUS * boss.sizeFactor())) {
				if (ctx.markHit(victim.getUUID()) && victim.hurtServer(level, boss.damageSources().mobAttack(boss), damage)) {
					CombatUtil.knockbackFrom(victim, front, 1.5, 0.6);
				}
			}
		}
		return Result.CONTINUE;
	}
}
