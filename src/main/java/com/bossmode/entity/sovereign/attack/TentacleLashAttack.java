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

/** Normal attack - Tentacle Lash: the shoulder tentacles and tail whip all the way around; nowhere near it is safe. */
public final class TentacleLashAttack extends SovereignAction {
	public TentacleLashAttack() {
		super("tentacle_lash", false, SovereignConfig.LASH_HIT_TICK, 1, SovereignConfig.LASH_TOTAL_TICKS, SovereignConfig.LASH_COOLDOWN, 10);
	}

	@Override
	public boolean canUse(ServerLevel level, VoidSovereignEntity boss, LivingEntity target, double gap) {
		return gap <= SovereignConfig.LASH_RADIUS * boss.sizeFactor() * 0.8;
	}

	@Override
	public Result tick(ServerLevel level, VoidSovereignEntity boss, @Nullable LivingEntity target, ActionContext ctx, int tick) {
		if (tick != prepareTicks()) {
			return Result.CONTINUE;
		}
		double radius = boss.getBbWidth() / 2.0 + SovereignConfig.LASH_RADIUS * boss.sizeFactor();
		boss.playSound(ModSounds.VOID_SOVEREIGN_LASH, 4.0F, 0.8F);
		for (int i = 0; i < 24; i++) {
			double a = Math.PI * 2 * i / 24;
			double r = radius * (0.5 + 0.5 * (i % 2));
			level.sendParticles(ParticleTypes.SWEEP_ATTACK, boss.getX() + Math.cos(a) * r, boss.getY() + 1.5, boss.getZ() + Math.sin(a) * r, 1, 0, 0, 0, 0);
		}
		ring(level, boss.position(), radius, MARK, 40);
		float damage = scaled(boss, SovereignConfig.LASH_DAMAGE);
		for (LivingEntity victim : CombatUtil.targetsInRadius(level, boss, boss.position().add(0, 1.0, 0), radius)) {
			if (ctx.markHit(victim.getUUID()) && victim.hurtServer(level, boss.damageSources().mobAttack(boss), damage)) {
				CombatUtil.knockbackFrom(victim, boss.position(), SovereignConfig.LASH_KNOCKBACK, 0.5);
			}
		}
		return Result.CONTINUE;
	}
}
