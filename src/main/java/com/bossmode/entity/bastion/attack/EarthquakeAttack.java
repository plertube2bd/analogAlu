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
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Earthquake: the Behemoth rears up and stamps - the ground shakes around it and everything standing on it is struck
 * and slowed. Be in the air at the stamp (jump) to avoid it.
 */
public final class EarthquakeAttack extends BastionAction {
	public EarthquakeAttack() {
		super("earthquake", true, BastionConfig.QUAKE_HIT_TICK, 10, BastionConfig.QUAKE_TOTAL_TICKS, BastionConfig.QUAKE_COOLDOWN,
				BastionConfig.QUAKE_WEIGHT);
	}

	@Override
	public boolean canUse(ServerLevel level, BastionBehemothEntity boss, LivingEntity target, double gap) {
		return gap <= BastionConfig.QUAKE_RADIUS * boss.sizeFactor() * 0.8;
	}

	@Override
	public Result tick(ServerLevel level, BastionBehemothEntity boss, @Nullable LivingEntity target, ActionContext ctx, int tick) {
		double radius = BastionConfig.QUAKE_RADIUS * boss.sizeFactor();
		if (tick < prepareTicks()) {
			if (target != null) {
				boss.faceTarget(target, 8.0F);
			}
			if (tick % 3 == 0) {
				ring(level, boss.position(), radius, BastionBehemothEntity.EMBER_DUST, 48);
			}
			return Result.CONTINUE;
		}
		if (tick == prepareTicks()) {
			boss.playSound(ModSounds.BASTION_BEHEMOTH_QUAKE, 6.0F, 0.7F);
			level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, boss.getX(), boss.getY() + 0.5, boss.getZ(), 1, 0, 0, 0, 0);
			float damage = scaled(boss, BastionConfig.QUAKE_DAMAGE);
			for (LivingEntity victim : CombatUtil.targetsInRadius(level, boss, boss.position(), radius)) {
				if (victim.onGround() && ctx.markHit(victim.getUUID()) && victim.hurtServer(level, boss.damageSources().mobAttack(boss), damage)) {
					victim.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 60, 1), boss);
					victim.setDeltaMovement(victim.getDeltaMovement().add(0, 0.5, 0));
					victim.needsSync = true;
				}
			}
		}
		if (tick < executionEnd()) {
			// the shockwave ripples outward for the visuals
			double r = radius * (tick - prepareTicks() + 1) / (executionEnd() - prepareTicks());
			int points = Math.max(12, (int) (r * 5));
			for (int i = 0; i < points; i++) {
				double a = Math.PI * 2 * i / points;
				Vec3 p = ground(level, boss.position().add(Math.cos(a) * r, 0, Math.sin(a) * r));
				level.sendParticles(debris(level, p), p.x, p.y + 0.2, p.z, 2, 0.2, 0.1, 0.2, 0.15);
			}
		}
		return Result.CONTINUE;
	}
}
