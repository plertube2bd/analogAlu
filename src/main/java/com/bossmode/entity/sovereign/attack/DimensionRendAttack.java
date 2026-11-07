package com.bossmode.entity.sovereign.attack;

import com.bossmode.config.SovereignConfig;
import com.bossmode.entity.action.ActionContext;
import com.bossmode.entity.boss.combat.CombatUtil;
import com.bossmode.entity.sovereign.VoidSovereignEntity;
import com.bossmode.registry.ModSounds;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Dimension Rend (phase 6+): the Sovereign locks onto its target and tears reality open along a straight line - a
 * crushing slash, and the rift it leaves keeps burning whoever stands on it for three seconds. Step off the marked line.
 */
public final class DimensionRendAttack extends SovereignAction {
	private static final int AIM_TICK = 14;

	public DimensionRendAttack() {
		super("dimension_rend", true, SovereignConfig.REND_SLASH_TICK, SovereignConfig.REND_END_TICK - SovereignConfig.REND_SLASH_TICK,
				SovereignConfig.REND_TOTAL_TICKS, SovereignConfig.REND_COOLDOWN, SovereignConfig.REND_WEIGHT);
	}

	@Override
	public boolean canUse(ServerLevel level, VoidSovereignEntity boss, LivingEntity target, double gap) {
		return gap <= SovereignConfig.REND_LENGTH * 0.7;
	}

	@Override
	public void start(ServerLevel level, VoidSovereignEntity boss, @Nullable LivingEntity target, ActionContext ctx) {
		ctx.direction = CombatUtil.horizontalForward(boss);
	}

	@Override
	public Result tick(ServerLevel level, VoidSovereignEntity boss, @Nullable LivingEntity target, ActionContext ctx, int tick) {
		double length = boss.getBbWidth() * 0.5 + SovereignConfig.REND_LENGTH;
		if (tick < AIM_TICK) {
			if (target != null) {
				boss.faceTarget(target, 20.0F);
				Vec3 to = target.position().subtract(boss.position()).multiply(1, 0, 1);
				if (to.lengthSqr() > 1.0E-4) {
					ctx.direction = to.normalize();
				}
			}
			return Result.CONTINUE;
		}
		if (tick == AIM_TICK) {
			ctx.origin = ground(level, boss.position());
			boss.playSound(ModSounds.VOID_SOVEREIGN_RIFT, 4.0F, 1.2F);
		}
		Vec3 from = ctx.origin, dir = ctx.direction;
		if (tick < prepareTicks()) {
			if (tick % 2 == 0) {
				for (double d = boss.getBbWidth() * 0.5; d < length; d += 1.0) {
					Vec3 p = ground(level, from.add(dir.scale(d)));
					level.sendParticles(MARK, p.x, p.y + 0.15, p.z, 1, 0.3, 0, 0.3, 0);
				}
			}
			return Result.CONTINUE;
		}
		boolean slash = tick == prepareTicks();
		boolean burn = !slash && tick < executionEnd() && (tick - prepareTicks()) % 10 == 0;
		if (slash) {
			boss.playSound(ModSounds.VOID_SOVEREIGN_CLAW, 6.0F, 0.5F);
			boss.playSound(ModSounds.VOID_SOVEREIGN_HOLE_COLLAPSE, 3.0F, 1.4F);
		}
		if (tick < executionEnd() && (slash || tick % 3 == 0)) {
			for (double d = boss.getBbWidth() * 0.5; d < length; d += slash ? 0.8 : 1.4) {
				Vec3 p = ground(level, from.add(dir.scale(d)));
				level.sendParticles(slash ? ParticleTypes.SWEEP_ATTACK : ParticleTypes.PORTAL, p.x, p.y + (slash ? 1.0 : 0.3), p.z, 1, 0.2, 0.2, 0.2, 0.05);
				level.sendParticles(DARK, p.x, p.y + 0.2, p.z, 1, 0.3, 0.05, 0.3, 0);
			}
		}
		if (slash || burn) {
			ctx.resetHits();
			float damage = scaled(boss, slash ? SovereignConfig.REND_DAMAGE : SovereignConfig.REND_RIFT_DAMAGE);
			Vec3 to = from.add(dir.scale(length));
			AABB area = new AABB(from, to).inflate(SovereignConfig.REND_WIDTH + 1.0, 3.0, SovereignConfig.REND_WIDTH + 1.0);
			for (LivingEntity victim : CombatUtil.targetsInBox(level, boss, area)) {
				Vec3 rel = victim.position().subtract(from).multiply(1, 0, 1);
				double along = rel.dot(dir);
				double across = rel.subtract(dir.scale(along)).length();
				if (along < 0 || along > length || across > SovereignConfig.REND_WIDTH + victim.getBbWidth() * 0.5 || !ctx.markHit(victim.getUUID())) {
					continue;
				}
				if (victim.hurtServer(level, boss.damageSources().indirectMagic(boss, boss), damage) && slash) {
					Vec3 side = rel.subtract(dir.scale(along));
					Vec3 push = side.lengthSqr() > 1.0E-4 ? side.normalize() : new Vec3(-dir.z, 0, dir.x);
					CombatUtil.knockback(victim, push.x, push.z, 1.6, 0.5);
				}
			}
		}
		return Result.CONTINUE;
	}
}
