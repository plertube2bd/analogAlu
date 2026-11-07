package com.bossmode.entity.sovereign.attack;

import com.bossmode.config.SovereignConfig;
import com.bossmode.entity.action.ActionContext;
import com.bossmode.entity.boss.combat.CombatUtil;
import com.bossmode.entity.sovereign.VoidSovereignEntity;
import com.bossmode.registry.ModSounds;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Null Sweep (phase 4+): a knee-high blade of void sweeps 270 degrees around the Sovereign, starting beside its target.
 * Jump over it when it comes (anything standing on the ground in its path is cut), or stay beyond its reach.
 */
public final class NullSweepAttack extends SovereignAction {
	public NullSweepAttack() {
		super("null_sweep", true, SovereignConfig.SWEEP_START_TICK, SovereignConfig.SWEEP_END_TICK - SovereignConfig.SWEEP_START_TICK,
				SovereignConfig.SWEEP_TOTAL_TICKS, SovereignConfig.SWEEP_COOLDOWN, SovereignConfig.SWEEP_WEIGHT);
	}

	@Override
	public boolean canUse(ServerLevel level, VoidSovereignEntity boss, LivingEntity target, double gap) {
		return gap <= SovereignConfig.SWEEP_LENGTH * boss.sizeFactor() * 0.8;
	}

	@Override
	public void start(ServerLevel level, VoidSovereignEntity boss, @Nullable LivingEntity target, ActionContext ctx) {
		double towards = target != null ? Math.atan2(target.getZ() - boss.getZ(), target.getX() - boss.getX())
				: Math.toRadians(boss.getYRot() + 90.0F);
		double sign = boss.getRandom().nextBoolean() ? 1.0 : -1.0;
		// start a quarter turn before the target so it has a moment to see the blade coming
		ctx.travelled = towards - sign * Math.toRadians(45.0);
		ctx.direction = new Vec3(sign, 0, 0);
		boss.playSound(ModSounds.VOID_SOVEREIGN_SWEEP, 4.0F, 0.6F);
	}

	private static double angleAt(ActionContext ctx, int tick, int from, int to) {
		double progress = Mth.clamp((tick - from) / (double) (to - from), 0.0, 1.0);
		return ctx.travelled + ctx.direction.x * Math.toRadians(SovereignConfig.SWEEP_ARC_DEGREES) * progress;
	}

	@Override
	public Result tick(ServerLevel level, VoidSovereignEntity boss, @Nullable LivingEntity target, ActionContext ctx, int tick) {
		double length = boss.getBbWidth() * 0.5 + SovereignConfig.SWEEP_LENGTH * boss.sizeFactor();
		double beamY = boss.getY() + SovereignConfig.SWEEP_BEAM_HEIGHT;
		if (tick < prepareTicks()) {
			if (tick % 3 == 0) {
				ring(level, boss.position(), length, MARK, 64);
				double a = ctx.travelled;
				for (double d = boss.getBbWidth() * 0.5; d < length; d += 1.0) {
					level.sendParticles(MARK, boss.getX() + Math.cos(a) * d, beamY + 0.1, boss.getZ() + Math.sin(a) * d, 1, 0, 0, 0, 0);
				}
			}
			return Result.CONTINUE;
		}
		if (tick >= executionEnd()) {
			return Result.CONTINUE;
		}
		double previous = angleAt(ctx, tick - 1, prepareTicks(), executionEnd());
		double current = angleAt(ctx, tick, prepareTicks(), executionEnd());
		for (double d = boss.getBbWidth() * 0.4; d < length; d += 0.9) {
			level.sendParticles(DARK, boss.getX() + Math.cos(current) * d, beamY + 0.15, boss.getZ() + Math.sin(current) * d, 1, 0.05, 0.05, 0.05, 0);
		}
		level.sendParticles(ParticleTypes.REVERSE_PORTAL, boss.getX() + Math.cos(current) * length, beamY + 0.3, boss.getZ() + Math.sin(current) * length,
				4, 0.2, 0.2, 0.2, 0.1);
		if (tick % 8 == 0) {
			boss.playSound(ModSounds.VOID_SOVEREIGN_SWEEP, 3.0F, 0.9F);
		}
		float damage = scaled(boss, SovereignConfig.SWEEP_DAMAGE);
		double lo = Math.min(previous, current), hi = Math.max(previous, current);
		for (LivingEntity victim : CombatUtil.targetsInRadius(level, boss, boss.position(), length + 1.0)) {
			double dx = victim.getX() - boss.getX(), dz = victim.getZ() - boss.getZ();
			double dist = Math.sqrt(dx * dx + dz * dz);
			if (dist > length + victim.getBbWidth() * 0.5 || victim.getY() > beamY + 0.25 || victim.getY() + victim.getBbHeight() < beamY) {
				continue; // out of reach, or jumping over the blade
			}
			double margin = Math.atan2(victim.getBbWidth() * 0.5 + 0.3, Math.max(0.5, dist));
			double angle = Math.atan2(dz, dx);
			// bring the victim's angle into the swept interval's turn
			while (angle < lo - Math.PI) {
				angle += Math.PI * 2;
			}
			while (angle > hi + Math.PI) {
				angle -= Math.PI * 2;
			}
			if (angle >= lo - margin && angle <= hi + margin && ctx.markHit(victim.getUUID())
					&& victim.hurtServer(level, boss.damageSources().mobAttack(boss), damage)) {
				double tangent = current + ctx.direction.x * Math.PI / 2;
				CombatUtil.knockback(victim, Math.cos(tangent), Math.sin(tangent), 1.4, 0.4);
			}
		}
		return Result.CONTINUE;
	}
}
