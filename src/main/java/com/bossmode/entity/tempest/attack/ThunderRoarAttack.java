package com.bossmode.entity.tempest.attack;

import com.bossmode.config.TempestConfig;
import com.bossmode.entity.action.ActionContext;
import com.bossmode.entity.boss.combat.CombatUtil;
import com.bossmode.entity.tempest.TempestWyvernEntity;
import com.bossmode.registry.ModSounds;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import org.jspecify.annotations.Nullable;

/** Thunder Roar: a thunderclap bursts out of the Wyvern - everyone close is struck, thrown back and slowed. */
public final class ThunderRoarAttack extends TempestAction {
	public ThunderRoarAttack() {
		super("thunder_roar", true, TempestConfig.ROAR_HIT_TICK, 1, TempestConfig.ROAR_TOTAL_TICKS, TempestConfig.ROAR_COOLDOWN, TempestConfig.ROAR_WEIGHT);
	}

	@Override
	public boolean canUse(ServerLevel level, TempestWyvernEntity boss, LivingEntity target, double gap) {
		return boss.distanceTo(target) <= TempestConfig.ROAR_RADIUS * boss.sizeFactor() * 0.9;
	}

	@Override
	public Result tick(ServerLevel level, TempestWyvernEntity boss, @Nullable LivingEntity target, ActionContext ctx, int tick) {
		if (tick < prepareTicks()) {
			if (tick % 2 == 0) {
				level.sendParticles(ParticleTypes.ELECTRIC_SPARK, boss.getX(), boss.getY(0.6), boss.getZ(), 12, boss.getBbWidth() * 0.5, boss.getBbHeight() * 0.3,
						boss.getBbWidth() * 0.5, 0.3);
			}
			return Result.CONTINUE;
		}
		if (tick == prepareTicks()) {
			boss.playSound(ModSounds.TEMPEST_WYVERN_ROAR, 6.0F, 0.8F);
			boss.playSound(ModSounds.TEMPEST_WYVERN_THUNDER, 6.0F, 0.7F);
			visualLightning(level, boss.position());
			double radius = TempestConfig.ROAR_RADIUS * boss.sizeFactor();
			for (int i = 0; i < 60; i++) {
				double a = Math.PI * 2 * i / 60;
				level.sendParticles(ParticleTypes.ELECTRIC_SPARK, boss.getX(), boss.getY(0.5), boss.getZ(), 0, Math.cos(a), 0.05, Math.sin(a), 1.4);
			}
			float damage = scaled(boss, TempestConfig.ROAR_DAMAGE);
			for (LivingEntity victim : CombatUtil.targetsInRadius(level, boss, boss.position().add(0, boss.getBbHeight() * 0.5, 0), radius)) {
				if (ctx.markHit(victim.getUUID())) {
					victim.hurtServer(level, boss.damageSources().indirectMagic(boss, boss), damage);
					victim.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 60, 1), boss);
					CombatUtil.knockbackFrom(victim, boss.position(), TempestConfig.ROAR_KNOCKBACK, 0.4);
				}
			}
		}
		return Result.CONTINUE;
	}
}
