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
 * Black Hole: a singularity opens between the Sovereign and its target (60 % of the way) and drags everything within
 * {@link SovereignConfig#HOLE_PULL_RADIUS} toward it for four seconds (harder the closer you are), then collapses
 * with a devastating blast at its core. Sprint away from it - or put a wall between you and it.
 */
public final class BlackHoleAttack extends SovereignAction {
	public BlackHoleAttack() {
		super("black_hole", true, SovereignConfig.HOLE_CAST_TICK, SovereignConfig.HOLE_COLLAPSE_TICK - SovereignConfig.HOLE_CAST_TICK + 1,
				SovereignConfig.HOLE_TOTAL_TICKS, SovereignConfig.HOLE_COOLDOWN, SovereignConfig.HOLE_WEIGHT);
	}

	@Override
	public boolean canUse(ServerLevel level, VoidSovereignEntity boss, LivingEntity target, double gap) {
		return boss.distanceTo(target) <= 36.0;
	}

	@Override
	public Result tick(ServerLevel level, VoidSovereignEntity boss, @Nullable LivingEntity target, ActionContext ctx, int tick) {
		if (tick < prepareTicks()) {
			if (target != null) {
				boss.faceTarget(target, 15.0F);
			}
			return Result.CONTINUE;
		}
		double sf = boss.sizeFactor();
		if (tick == prepareTicks()) {
			Vec3 toward = target != null ? target.position().subtract(boss.position()) : boss.frontPoint(1).subtract(boss.position());
			toward = new Vec3(toward.x, 0, toward.z);
			// between the two - never on top of the target, so there is always a way out
			double distance = Math.max(4.0, Math.min(SovereignConfig.HOLE_DISTANCE * sf + boss.getBbWidth() * 0.5, toward.length() * 0.6));
			Vec3 at = boss.position().add(toward.lengthSqr() < 1.0E-4 ? Vec3.ZERO : toward.normalize().scale(distance));
			ctx.destination = ground(level, at).add(0, 3.0, 0);
			boss.playSound(ModSounds.VOID_SOVEREIGN_HOLE, 6.0F, 0.6F);
		}
		Vec3 core = ctx.destination;
		double pullRadius = SovereignConfig.HOLE_PULL_RADIUS * sf;
		if (tick < SovereignConfig.HOLE_COLLAPSE_TICK) {
			double progress = (tick - prepareTicks()) / (double) (SovereignConfig.HOLE_COLLAPSE_TICK - prepareTicks());
			level.sendParticles(DARK, core.x, core.y, core.z, 5, 0.3 + progress * 0.5, 0.3 + progress * 0.5, 0.3 + progress * 0.5, 0);
			for (int i = 0; i < 10; i++) {
				double a = tick * 0.35 + i * Math.PI * 2 / 10;
				double r = pullRadius * (1.0 - ((tick * 0.05 + i * 0.1) % 1.0));
				level.sendParticles(i % 2 == 0 ? ParticleTypes.REVERSE_PORTAL : ParticleTypes.SQUID_INK, core.x + Math.cos(a) * r,
						core.y + Math.sin(a * 0.5) * 1.5, core.z + Math.sin(a) * r, 1, 0, 0, 0, 0);
			}
			if (tick % 20 == 0) {
				level.playSound(null, core.x, core.y, core.z, ModSounds.VOID_SOVEREIGN_HOLE, boss.getSoundSource(), 3.0F, 0.5F + (float) progress * 0.5F);
			}
			for (LivingEntity victim : CombatUtil.targetsInRadius(level, boss, core, pullRadius)) {
				double closeness = 1.0 - Math.min(1.0, victim.position().distanceTo(core) / pullRadius);
				CombatUtil.pull(victim, core, SovereignConfig.HOLE_PULL * (0.4 + closeness));
			}
			return Result.CONTINUE;
		}
		if (tick == SovereignConfig.HOLE_COLLAPSE_TICK) {
			level.playSound(null, core.x, core.y, core.z, ModSounds.VOID_SOVEREIGN_HOLE_COLLAPSE, boss.getSoundSource(), 7.0F, 0.6F);
			level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, core.x, core.y, core.z, 2, 1.0, 1.0, 1.0, 0);
			level.sendParticles(ParticleTypes.REVERSE_PORTAL, core.x, core.y, core.z, 200, 1.0, 1.0, 1.0, 1.2);
			float damage = scaled(boss, SovereignConfig.HOLE_DAMAGE);
			for (LivingEntity victim : CombatUtil.targetsInRadius(level, boss, core, SovereignConfig.HOLE_CORE_RADIUS * sf)) {
				if (ctx.markHit(victim.getUUID()) && victim.hurtServer(level, boss.damageSources().indirectMagic(boss, boss), damage)) {
					CombatUtil.knockbackFrom(victim, core, 2.2, 0.8);
				}
			}
		}
		return Result.CONTINUE;
	}
}
