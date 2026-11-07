package com.bossmode.entity.bastion.attack;

import com.bossmode.config.BastionConfig;
import com.bossmode.entity.action.ActionContext;
import com.bossmode.entity.bastion.BastionBehemothEntity;
import com.bossmode.entity.boss.combat.CombatUtil;
import com.bossmode.registry.ModSounds;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import org.jspecify.annotations.Nullable;

/**
 * Normal attack - Tail Sweep: the club tail sweeps the Behemoth's flanks and back - the answer to anyone dodging its
 * armoured front. Whatever stands in front of it is safe.
 */
public final class TailSweepAttack extends BastionAction {
	private static final double SAFE_FRONT_DEGREES = 100.0;

	public TailSweepAttack() {
		super("tail_sweep", false, BastionConfig.SWEEP_HIT_TICK, 1, BastionConfig.SWEEP_TOTAL_TICKS, BastionConfig.SWEEP_COOLDOWN, 10);
	}

	@Override
	public boolean canUse(ServerLevel level, BastionBehemothEntity boss, LivingEntity target, double gap) {
		return gap <= BastionConfig.SWEEP_RADIUS * boss.sizeFactor() * 0.7 && !CombatUtil.isInFront(boss, target, SAFE_FRONT_DEGREES);
	}

	@Override
	public Result tick(ServerLevel level, BastionBehemothEntity boss, @Nullable LivingEntity target, ActionContext ctx, int tick) {
		if (tick != prepareTicks()) {
			return Result.CONTINUE;
		}
		boss.playSound(ModSounds.BASTION_BEHEMOTH_MAUL, 4.0F, 0.6F);
		double radius = boss.getBbWidth() / 2.0 + BastionConfig.SWEEP_RADIUS * boss.sizeFactor();
		for (int i = 0; i < 32; i++) {
			double a = Math.PI * 2 * i / 32;
			double x = boss.getX() + Math.cos(a) * radius * 0.75, z = boss.getZ() + Math.sin(a) * radius * 0.75;
			level.sendParticles(ParticleTypes.SWEEP_ATTACK, x, boss.getY() + 1.0, z, 1, 0, 0, 0, 0);
		}
		float damage = scaled(boss, BastionConfig.SWEEP_DAMAGE);
		for (LivingEntity victim : CombatUtil.targetsInRadius(level, boss, boss.position().add(0, 1.0, 0), radius)) {
			if (!CombatUtil.isInFront(boss, victim, SAFE_FRONT_DEGREES) && ctx.markHit(victim.getUUID())
					&& victim.hurtServer(level, boss.damageSources().mobAttack(boss), damage)) {
				CombatUtil.knockbackFrom(victim, boss.position(), BastionConfig.SWEEP_KNOCKBACK, 0.5);
			}
		}
		return Result.CONTINUE;
	}
}
