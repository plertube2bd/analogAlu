package com.bossmode.entity.sovereign.attack;

import com.bossmode.config.SovereignConfig;
import com.bossmode.entity.action.ActionContext;
import com.bossmode.entity.boss.combat.CombatUtil;
import com.bossmode.entity.sovereign.VoidSovereignEntity;
import com.bossmode.registry.ModSounds;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import org.jspecify.annotations.Nullable;

/**
 * Skyward Lash - the answer to aerial smashes (a falling mace, wind-charge jumps): the tentacles whip straight up and
 * sweep everything airborne above and around the Sovereign aside, wiping out the fall it was building. Besides being
 * picked like any pattern, the Sovereign lashes out by itself the moment someone dives at it
 * ({@link VoidSovereignEntity#aerialThreats}) - except while it recovers from an attack: that opening is the reward.
 */
public final class SkywardLashAttack extends SovereignAction {
	public SkywardLashAttack() {
		super("skyward_lash", true, SovereignConfig.LASH_UP_HIT_TICK, 1, SovereignConfig.LASH_UP_TOTAL_TICKS, SovereignConfig.LASH_UP_COOLDOWN,
				SovereignConfig.LASH_UP_WEIGHT);
	}

	@Override
	public int weight(VoidSovereignEntity boss, LivingEntity target) {
		return counterWeight(boss, super.weight(boss, target));
	}

	@Override
	public boolean canUse(ServerLevel level, VoidSovereignEntity boss, LivingEntity target, double gap) {
		return !boss.aerialThreats(level).isEmpty()
				|| !target.onGround() && gap <= SovereignConfig.LASH_UP_REACH * boss.sizeFactor() && target.getY() > boss.getY() + 1.5;
	}

	@Override
	public Result tick(ServerLevel level, VoidSovereignEntity boss, @Nullable LivingEntity target, ActionContext ctx, int tick) {
		double reach = boss.getBbWidth() * 0.5 + SovereignConfig.LASH_UP_REACH * boss.sizeFactor();
		if (tick < prepareTicks()) {
			level.sendParticles(ParticleTypes.SQUID_INK, boss.getX(), boss.getY(0.9), boss.getZ(), 10, boss.getBbWidth() * 0.4, 0.4, boss.getBbWidth() * 0.4, 0.05);
			return Result.CONTINUE;
		}
		if (tick != prepareTicks()) {
			return Result.CONTINUE;
		}
		boss.playSound(ModSounds.VOID_SOVEREIGN_COUNTER, 5.0F, 1.0F);
		for (int i = 0; i < 10; i++) {
			double a = Math.PI * 2 * i / 10;
			double x = boss.getX() + Math.cos(a) * reach * 0.6, z = boss.getZ() + Math.sin(a) * reach * 0.6;
			for (double y = 0; y < boss.getBbHeight() + SovereignConfig.LASH_UP_HEIGHT * 0.6; y += 1.5) {
				level.sendParticles(DARK, x, boss.getY() + y, z, 1, 0.2, 0.3, 0.2, 0);
			}
			level.sendParticles(ParticleTypes.REVERSE_PORTAL, x, boss.getY() + boss.getBbHeight(), z, 6, 0.3, 2.0, 0.3, 0.2);
		}
		float air = scaled(boss, SovereignConfig.LASH_UP_DAMAGE), ground = scaled(boss, SovereignConfig.LASH_UP_GROUND_DAMAGE);
		double top = boss.getY() + boss.getBbHeight() + SovereignConfig.LASH_UP_HEIGHT;
		for (LivingEntity victim : CombatUtil.targetsInBox(level, boss, boss.getBoundingBox().inflate(reach, SovereignConfig.LASH_UP_HEIGHT, reach))) {
			double dx = victim.getX() - boss.getX(), dz = victim.getZ() - boss.getZ();
			if (dx * dx + dz * dz > reach * reach || victim.getY() > top || !ctx.markHit(victim.getUUID())) {
				continue;
			}
			if (!victim.onGround()) {
				// caught in the air: flung aside, the fall it was building is gone
				victim.hurtServer(level, boss.damageSources().mobAttack(boss), air);
				victim.resetFallDistance();
				CombatUtil.knockbackFrom(victim, boss.position(), SovereignConfig.LASH_UP_FLING, 0.3);
				level.sendParticles(ParticleTypes.SWEEP_ATTACK, victim.getX(), victim.getY(0.5), victim.getZ(), 2, 0.3, 0.3, 0.3, 0);
			} else if (dx * dx + dz * dz <= Math.pow(boss.getBbWidth() * 0.5 + 3.0, 2)) {
				victim.hurtServer(level, boss.damageSources().mobAttack(boss), ground);
				CombatUtil.knockbackFrom(victim, boss.position(), 1.2, 0.3);
			}
		}
		return Result.CONTINUE;
	}
}
