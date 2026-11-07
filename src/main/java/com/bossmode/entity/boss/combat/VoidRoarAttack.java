package com.bossmode.entity.boss.combat;

import com.bossmode.config.BossConfig;
import com.bossmode.entity.action.ActionContext;
import com.bossmode.entity.action.BossAction;
import com.bossmode.entity.boss.ShardDragonEntity;
import com.bossmode.registry.ModSounds;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import org.jspecify.annotations.Nullable;

/**
 * Void Roar: a shriek that blinds (Darkness), slows and inflicts "shard sickness" (Wither) on everything nearby,
 * then blasts it away - a nod to the Wither Storm's wither sickness.
 */
public final class VoidRoarAttack extends BossAction<ShardDragonEntity> {
	public VoidRoarAttack() {
		super("void_roar", true, BossConfig.ROAR_HIT_TICK, 1, BossConfig.ROAR_TOTAL_TICKS);
	}

	@Override
	public int cooldown() {
		return BossConfig.ROAR_COOLDOWN;
	}

	@Override
	public String animationTrigger() {
		return "void_roar";
	}

	@Override
	public int weight(ShardDragonEntity boss, LivingEntity target) {
		return BossConfig.ROAR_WEIGHT;
	}

	@Override
	public boolean canUse(ServerLevel level, ShardDragonEntity boss, LivingEntity target, double gap) {
		return gap <= BossConfig.ROAR_RADIUS * boss.sizeFactor() * 0.7;
	}

	@Override
	public Result tick(ServerLevel level, ShardDragonEntity boss, @Nullable LivingEntity target, ActionContext ctx, int tick) {
		if (tick != prepareTicks()) {
			return Result.CONTINUE;
		}
		double radius = BossConfig.ROAR_RADIUS * boss.sizeFactor();
		boss.playSound(ModSounds.SHARD_DRAGON_VOID_ROAR, 6.0F, 0.8F);
		for (int ring = 1; ring <= 3; ring++) {
			for (int i = 0; i < 20; i++) {
				double a = Math.PI * 2 * i / 20;
				double r = radius * ring / 3.0;
				level.sendParticles(ParticleTypes.SONIC_BOOM, boss.getX() + Math.cos(a) * r, boss.getY() + 1.5, boss.getZ() + Math.sin(a) * r, 1, 0, 0, 0, 0);
			}
		}
		level.sendParticles(ParticleTypes.SCULK_SOUL, boss.getX(), boss.getY() + 2, boss.getZ(), 40, radius * 0.4, 1.5, radius * 0.4, 0.05);
		float damage = (float) (BossConfig.ROAR_DAMAGE * boss.getDamageMultiplier());
		for (LivingEntity victim : CombatUtil.targetsInRadius(level, boss, boss.position(), radius)) {
			if (!ctx.markHit(victim.getUUID())) {
				continue;
			}
			victim.hurtServer(level, boss.damageSources().indirectMagic(boss, boss), damage);
			victim.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 100, 0), boss);
			victim.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 60, 1), boss);
			victim.addEffect(new MobEffectInstance(MobEffects.WITHER, 100, 0), boss);
			CombatUtil.knockbackFrom(victim, boss.position(), 1.6, 0.5);
		}
		return Result.CONTINUE;
	}
}
