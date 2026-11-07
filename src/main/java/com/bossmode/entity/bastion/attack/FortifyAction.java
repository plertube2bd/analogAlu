package com.bossmode.entity.bastion.attack;

import com.bossmode.config.BastionConfig;
import com.bossmode.entity.action.ActionContext;
import com.bossmode.entity.bastion.BastionBehemothEntity;
import com.bossmode.entity.boss.combat.CombatUtil;
import com.bossmode.registry.ModSounds;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import org.jspecify.annotations.Nullable;

/**
 * Fortify: the Behemoth digs in and seals its plating - for four and a half seconds it takes only a fifth of any damage
 * and spikes back at whoever strikes it in melee. Then it bursts out of the stance with a shockwave. Back off and wait.
 */
public final class FortifyAction extends BastionAction {
	public FortifyAction() {
		super("fortify", true, BastionConfig.FORTIFY_START_TICK, BastionConfig.FORTIFY_END_TICK - BastionConfig.FORTIFY_START_TICK,
				BastionConfig.FORTIFY_TOTAL_TICKS, BastionConfig.FORTIFY_COOLDOWN, BastionConfig.FORTIFY_WEIGHT);
	}

	@Override
	public boolean canUse(ServerLevel level, BastionBehemothEntity boss, LivingEntity target, double gap) {
		return gap <= 16.0;
	}

	@Override
	public Result tick(ServerLevel level, BastionBehemothEntity boss, @Nullable LivingEntity target, ActionContext ctx, int tick) {
		boss.setDeltaMovement(0, boss.getDeltaMovement().y, 0);
		if (tick == prepareTicks()) {
			boss.setFortified(true);
			boss.playSound(ModSounds.BASTION_BEHEMOTH_FORTIFY, 5.0F, 0.7F);
			level.sendParticles(debris(level, boss.position()), boss.getX(), boss.getY() + 0.3, boss.getZ(), 60, boss.getBbWidth() * 0.5, 0.2,
					boss.getBbWidth() * 0.5, 0.2);
			boss.announce(level, Component.translatable("message.boss_mode.bastion.fortify"));
		}
		if (tick == executionEnd()) {
			boss.setFortified(false);
			boss.playSound(ModSounds.BASTION_BEHEMOTH_QUAKE, 5.0F, 0.9F);
			double radius = BastionConfig.FORTIFY_BURST_RADIUS * boss.sizeFactor() + boss.getBbWidth() * 0.5;
			for (int i = 0; i < 48; i++) {
				double a = Math.PI * 2 * i / 48;
				level.sendParticles(ParticleTypes.FLAME, boss.getX(), boss.getY() + 0.5, boss.getZ(), 0, Math.cos(a), 0.05, Math.sin(a), 0.8);
			}
			level.sendParticles(ParticleTypes.EXPLOSION, boss.getX(), boss.getY(0.5), boss.getZ(), 4, boss.getBbWidth() * 0.4, 0.5, boss.getBbWidth() * 0.4, 0);
			float damage = scaled(boss, BastionConfig.FORTIFY_BURST_DAMAGE);
			for (LivingEntity victim : CombatUtil.targetsInRadius(level, boss, boss.position(), radius)) {
				if (ctx.markHit(victim.getUUID()) && victim.hurtServer(level, boss.damageSources().mobAttack(boss), damage)) {
					CombatUtil.knockbackFrom(victim, boss.position(), 1.8, 0.5);
				}
			}
		}
		return Result.CONTINUE;
	}

	@Override
	public void stop(ServerLevel level, BastionBehemothEntity boss, ActionContext ctx, boolean interrupted) {
		boss.setFortified(false);
	}
}
