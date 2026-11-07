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
 * Gravity Well: for six seconds the void around the Sovereign drags everything airborne straight down and swallows its
 * momentum - fall distance keeps resetting, so no aerial smash (mace) can build up inside the ring, and wind-charge
 * jumps fall flat. On the ground nothing changes: fight it on foot while the well lasts, or leave the ring.
 */
public final class GravityWellAttack extends SovereignAction {
	public GravityWellAttack() {
		super("gravity_well", true, SovereignConfig.WELL_START_TICK, SovereignConfig.WELL_END_TICK - SovereignConfig.WELL_START_TICK,
				SovereignConfig.WELL_TOTAL_TICKS, SovereignConfig.WELL_COOLDOWN, SovereignConfig.WELL_WEIGHT);
	}

	@Override
	public int weight(VoidSovereignEntity boss, LivingEntity target) {
		return counterWeight(boss, super.weight(boss, target));
	}

	@Override
	public boolean canUse(ServerLevel level, VoidSovereignEntity boss, LivingEntity target, double gap) {
		return gap <= SovereignConfig.WELL_RADIUS * boss.sizeFactor();
	}

	@Override
	public void start(ServerLevel level, VoidSovereignEntity boss, @Nullable LivingEntity target, ActionContext ctx) {
		ctx.origin = boss.position();
		boss.playSound(ModSounds.VOID_SOVEREIGN_GRAVITY, 5.0F, 0.6F);
	}

	@Override
	public Result tick(ServerLevel level, VoidSovereignEntity boss, @Nullable LivingEntity target, ActionContext ctx, int tick) {
		double radius = boss.getBbWidth() * 0.5 + SovereignConfig.WELL_RADIUS * boss.sizeFactor();
		Vec3 center = ctx.origin;
		if (tick < prepareTicks()) {
			if (tick % 3 == 0) {
				ring(level, center, radius * (0.4 + 0.6 * tick / prepareTicks()), MARK, 40);
			}
			return Result.CONTINUE;
		}
		if (tick >= executionEnd()) {
			return Result.CONTINUE;
		}
		if (tick % 3 == 0) {
			ring(level, center, radius, DARK, 56);
			for (int i = 0; i < 12; i++) {
				double a = boss.getRandom().nextDouble() * Math.PI * 2, r = boss.getRandom().nextDouble() * radius;
				level.sendParticles(ParticleTypes.REVERSE_PORTAL, center.x + Math.cos(a) * r, center.y + 6.0 + boss.getRandom().nextDouble() * 6.0,
						center.z + Math.sin(a) * r, 0, 0, -1.0, 0, 0.6);
			}
		}
		if (tick % 20 == 0) {
			boss.playSound(ModSounds.VOID_SOVEREIGN_GRAVITY, 3.0F, 0.8F);
		}
		float damage = scaled(boss, SovereignConfig.WELL_DAMAGE);
		for (LivingEntity victim : CombatUtil.targetsInBox(level, boss, new net.minecraft.world.phys.AABB(center, center).inflate(radius, 24.0, radius))) {
			double dx = victim.getX() - center.x, dz = victim.getZ() - center.z;
			if (dx * dx + dz * dz > radius * radius) {
				continue;
			}
			if (!victim.onGround()) {
				Vec3 v = victim.getDeltaMovement();
				victim.setDeltaMovement(v.x * 0.5, -SovereignConfig.WELL_PULL_DOWN, v.z * 0.5);
				victim.needsSync = true;
				victim.resetFallDistance();
				if ((tick - prepareTicks()) % 10 == 0) {
					victim.hurtServer(level, boss.damageSources().indirectMagic(boss, boss), damage);
				}
				if (tick % 2 == 0) {
					level.sendParticles(DARK, victim.getX(), victim.getY(0.5), victim.getZ(), 3, 0.3, 0.5, 0.3, 0);
				}
			}
		}
		return Result.CONTINUE;
	}
}
