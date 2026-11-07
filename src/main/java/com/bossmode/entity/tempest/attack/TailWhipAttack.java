package com.bossmode.entity.tempest.attack;

import com.bossmode.config.TempestConfig;
import com.bossmode.entity.action.ActionContext;
import com.bossmode.entity.boss.combat.CombatUtil;
import com.bossmode.entity.tempest.TempestWyvernEntity;
import com.bossmode.registry.ModSounds;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import org.jspecify.annotations.Nullable;

/** Normal attack (ground) - Tail Whip: a full turn of the barbed tail, hitting everything around the Wyvern. */
public final class TailWhipAttack extends TempestAction {
	public TailWhipAttack() {
		super("tail_whip", false, TempestConfig.WHIP_HIT_TICK, 1, TempestConfig.WHIP_TOTAL_TICKS, TempestConfig.WHIP_COOLDOWN, 10);
	}

	@Override
	public boolean canUse(ServerLevel level, TempestWyvernEntity boss, LivingEntity target, double gap) {
		return gap <= TempestConfig.WHIP_RADIUS * boss.sizeFactor() * 0.7;
	}

	@Override
	public Result tick(ServerLevel level, TempestWyvernEntity boss, @Nullable LivingEntity target, ActionContext ctx, int tick) {
		if (tick != prepareTicks()) {
			return Result.CONTINUE;
		}
		boss.playSound(ModSounds.TEMPEST_WYVERN_STRIKE, 3.0F, 0.7F);
		double radius = boss.getBbWidth() / 2.0 + TempestConfig.WHIP_RADIUS * boss.sizeFactor();
		for (int i = 0; i < 24; i++) {
			double a = Math.PI * 2 * i / 24;
			level.sendParticles(ParticleTypes.SWEEP_ATTACK, boss.getX() + Math.cos(a) * radius * 0.7, boss.getY() + 0.8, boss.getZ() + Math.sin(a) * radius * 0.7,
					1, 0, 0, 0, 0);
		}
		float damage = scaled(boss, TempestConfig.WHIP_DAMAGE);
		for (LivingEntity victim : CombatUtil.targetsInRadius(level, boss, boss.position().add(0, 1.0, 0), radius)) {
			if (ctx.markHit(victim.getUUID()) && victim.hurtServer(level, boss.damageSources().mobAttack(boss), damage)) {
				CombatUtil.knockbackFrom(victim, boss.position(), TempestConfig.WHIP_KNOCKBACK, 0.45);
			}
		}
		return Result.CONTINUE;
	}
}
