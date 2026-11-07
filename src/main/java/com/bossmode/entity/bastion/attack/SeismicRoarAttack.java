package com.bossmode.entity.bastion.attack;

import com.bossmode.config.BastionConfig;
import com.bossmode.entity.action.ActionContext;
import com.bossmode.entity.bastion.BastionBehemothEntity;
import com.bossmode.entity.boss.combat.CombatUtil;
import com.bossmode.registry.ModSounds;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import org.jspecify.annotations.Nullable;

/** Seismic Roar: a roar that shakes the ground - everyone close is struck, thrown back, slowed and weakened. */
public final class SeismicRoarAttack extends BastionAction {
	public SeismicRoarAttack() {
		super("seismic_roar", true, BastionConfig.SEISMIC_HIT_TICK, 1, BastionConfig.SEISMIC_TOTAL_TICKS, BastionConfig.SEISMIC_COOLDOWN,
				BastionConfig.SEISMIC_WEIGHT);
	}

	@Override
	public boolean canUse(ServerLevel level, BastionBehemothEntity boss, LivingEntity target, double gap) {
		return gap <= BastionConfig.SEISMIC_RADIUS * boss.sizeFactor() * 0.8;
	}

	@Override
	public Result tick(ServerLevel level, BastionBehemothEntity boss, @Nullable LivingEntity target, ActionContext ctx, int tick) {
		if (tick < prepareTicks()) {
			if (target != null) {
				boss.faceTarget(target, 10.0F);
			}
			return Result.CONTINUE;
		}
		if (tick == prepareTicks()) {
			boss.playSound(ModSounds.BASTION_BEHEMOTH_ROAR, 7.0F, 0.6F);
			double radius = BastionConfig.SEISMIC_RADIUS * boss.sizeFactor();
			for (int i = 0; i < 64; i++) {
				double a = Math.PI * 2 * i / 64;
				level.sendParticles(ParticleTypes.CLOUD, boss.getX(), boss.getY() + 0.5, boss.getZ(), 0, Math.cos(a), 0.02, Math.sin(a), 1.2);
			}
			level.sendParticles(debris(level, boss.position()), boss.getX(), boss.getY() + 0.3, boss.getZ(), 80, radius * 0.4, 0.2, radius * 0.4, 0.3);
			float damage = scaled(boss, BastionConfig.SEISMIC_DAMAGE);
			for (LivingEntity victim : CombatUtil.targetsInRadius(level, boss, boss.position().add(0, 1.0, 0), radius)) {
				if (ctx.markHit(victim.getUUID())) {
					victim.hurtServer(level, boss.damageSources().mobAttack(boss), damage);
					victim.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 80, 1), boss);
					victim.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 100, 0), boss);
					CombatUtil.knockbackFrom(victim, boss.position(), BastionConfig.SEISMIC_KNOCKBACK, 0.5);
				}
			}
		}
		return Result.CONTINUE;
	}
}
