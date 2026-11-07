package com.bossmode.entity.tempest.attack;

import com.bossmode.config.TempestConfig;
import com.bossmode.entity.action.ActionContext;
import com.bossmode.entity.boss.combat.CombatUtil;
import com.bossmode.entity.tempest.TempestWyvernEntity;
import com.bossmode.registry.ModSounds;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Dive Bomb (air only): the Wyvern rears up, locks onto where its target stands and plunges onto that spot, landing
 * with a thunderous impact. It stays on the ground afterwards - the melee window. Dodge sideways when it tucks its wings.
 */
public final class DiveBombAttack extends TempestAction {
	public DiveBombAttack() {
		super("dive_bomb", true, TempestConfig.DIVE_IMPACT_TICK, 1, TempestConfig.DIVE_TOTAL_TICKS, TempestConfig.DIVE_COOLDOWN, TempestConfig.DIVE_WEIGHT);
	}

	@Override
	public boolean canUse(ServerLevel level, TempestWyvernEntity boss, LivingEntity target, double gap) {
		return boss.isFlying() && boss.airTicks() >= TempestConfig.DIVE_MIN_AIR_TICKS && gap >= TempestConfig.DIVE_MIN_RANGE && gap <= TempestConfig.DIVE_MAX_RANGE && boss.getSensing().hasLineOfSight(target);
	}

	@Override
	public void start(ServerLevel level, TempestWyvernEntity boss, @Nullable LivingEntity target, ActionContext ctx) {
		ctx.destination = ground(level, target != null ? target.position() : boss.frontPoint(6.0));
		boss.playSound(ModSounds.TEMPEST_WYVERN_ROAR, 5.0F, 1.3F);
	}

	@Override
	public Result tick(ServerLevel level, TempestWyvernEntity boss, @Nullable LivingEntity target, ActionContext ctx, int tick) {
		if (tick < TempestConfig.DIVE_AIM_TICK) {
			// rear up and aim
			if (target != null) {
				ctx.destination = ground(level, target.position());
				boss.faceTarget(target, 20.0F);
			}
			boss.setDeltaMovement(boss.getDeltaMovement().multiply(0.5, 0, 0.5).add(0, 0.12, 0));
			if (tick % 2 == 0) {
				ring(level, ctx.destination, TempestConfig.DIVE_RADIUS * boss.sizeFactor(), TempestWyvernEntity.STORM_DUST, 20);
			}
			return Result.CONTINUE;
		}
		if (tick < prepareTicks()) {
			if (tick == TempestConfig.DIVE_AIM_TICK) {
				boss.playSound(ModSounds.TEMPEST_WYVERN_DIVE, 5.0F, 0.9F);
			}
			Vec3 to = ctx.destination.subtract(boss.position());
			int left = Math.max(1, prepareTicks() - tick);
			Vec3 velocity = to.scale(1.0 / left);
			if (velocity.length() > 2.5) {
				velocity = velocity.normalize().scale(2.5);
			}
			boss.setDeltaMovement(velocity);
			boss.faceDirection(to, 25.0F);
			if (tick % 2 == 0) {
				ring(level, ctx.destination, TempestConfig.DIVE_RADIUS * boss.sizeFactor(), TempestWyvernEntity.STORM_DUST, 20);
				level.sendParticles(ParticleTypes.CLOUD, boss.getX(), boss.getY(0.5), boss.getZ(), 4, 0.6, 0.6, 0.6, 0.02);
			}
			if (boss.horizontalCollision && tick % 3 == 0) {
				boss.crushObstacles(level, to.multiply(1, 0, 1).normalize());
			}
			return Result.CONTINUE;
		}
		if (tick == prepareTicks()) {
			boss.setFlying(false);
			if (!boss.onGround()) {
				double ground = boss.groundBelow(level);
				if (boss.getY() - ground < 3.0) {
					boss.setPos(boss.getX(), ground, boss.getZ());
				}
			}
			boss.setDeltaMovement(0, -0.5, 0);
			boss.playSound(ModSounds.TEMPEST_WYVERN_THUNDER, 6.0F, 0.6F);
			double radius = TempestConfig.DIVE_RADIUS * boss.sizeFactor() + boss.getBbWidth() * 0.5;
			visualLightning(level, boss.position());
			level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, boss.getX(), boss.getY() + 0.5, boss.getZ(), 1, 0, 0, 0, 0);
			level.sendParticles(ParticleTypes.ELECTRIC_SPARK, boss.getX(), boss.getY() + 0.5, boss.getZ(), 60, radius * 0.4, 0.3, radius * 0.4, 0.4);
			float damage = scaled(boss, TempestConfig.DIVE_DAMAGE);
			for (LivingEntity victim : CombatUtil.targetsInRadius(level, boss, boss.position(), radius)) {
				if (ctx.markHit(victim.getUUID()) && victim.hurtServer(level, boss.damageSources().mobAttack(boss), damage)) {
					CombatUtil.knockbackFrom(victim, boss.position(), 1.6, 0.6);
				}
			}
			return Result.CONTINUE;
		}
		boss.setDeltaMovement(0, boss.getDeltaMovement().y, 0);
		return Result.CONTINUE;
	}
}
