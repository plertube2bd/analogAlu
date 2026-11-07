package com.bossmode.entity.bastion.attack;

import com.bossmode.config.BastionConfig;
import com.bossmode.entity.action.ActionContext;
import com.bossmode.entity.bastion.BastionBehemothEntity;
import com.bossmode.entity.boss.combat.CombatUtil;
import com.bossmode.registry.ModSounds;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Rolling Charge: the Behemoth curls into a boulder of stone and rolls through everything in its way, crushing
 * blocks (mobGriefing) and flattening whoever it runs over; it steers a little toward its target. It uncurls dizzy -
 * EXPOSED, its plating open. Hitting something it cannot break ends the roll early.
 */
public final class RollingChargeAttack extends BastionAction {
	public RollingChargeAttack() {
		super("rolling_charge", true, BastionConfig.ROLL_START_TICK, BastionConfig.ROLL_END_TICK - BastionConfig.ROLL_START_TICK,
				BastionConfig.ROLL_TOTAL_TICKS, BastionConfig.ROLL_COOLDOWN, BastionConfig.ROLL_WEIGHT);
	}

	@Override
	public boolean canUse(ServerLevel level, BastionBehemothEntity boss, LivingEntity target, double gap) {
		return gap >= BastionConfig.ROLL_MIN_RANGE && gap <= BastionConfig.ROLL_MAX_RANGE && Math.abs(target.getY() - boss.getY()) < 5.0;
	}

	@Override
	public void start(ServerLevel level, BastionBehemothEntity boss, @Nullable LivingEntity target, ActionContext ctx) {
		ctx.direction = CombatUtil.horizontalForward(boss);
	}

	@Override
	public Result tick(ServerLevel level, BastionBehemothEntity boss, @Nullable LivingEntity target, ActionContext ctx, int tick) {
		if (tick < prepareTicks()) {
			if (target != null) {
				boss.faceTarget(target, 15.0F);
				ctx.direction = CombatUtil.horizontalForward(boss);
			}
			boss.setDeltaMovement(0, boss.getDeltaMovement().y, 0);
			return Result.CONTINUE;
		}
		if (tick >= executionEnd()) {
			boss.setDeltaMovement(0, boss.getDeltaMovement().y, 0);
			if (tick % 5 == 0) {
				level.sendParticles(ParticleTypes.CRIT, boss.getX(), boss.getY(1.0) + 0.5, boss.getZ(), 6, 1.0, 0.3, 1.0, 0.1);
			}
			return Result.CONTINUE;
		}
		if (tick == prepareTicks()) {
			boss.playSound(ModSounds.BASTION_BEHEMOTH_ROLL, 5.0F, 0.8F);
		} else if (tick % 10 == 0) {
			boss.playSound(ModSounds.BASTION_BEHEMOTH_ROLL, 4.0F, 0.9F);
		}
		if (target != null) {
			Vec3 to = target.position().subtract(boss.position()).multiply(1, 0, 1);
			if (to.lengthSqr() > 1.0E-4) {
				double current = Math.atan2(ctx.direction.z, ctx.direction.x);
				double wanted = Math.atan2(to.z, to.x);
				double diff = Math.atan2(Math.sin(wanted - current), Math.cos(wanted - current));
				double turn = Math.toRadians(BastionConfig.ROLL_TURN);
				double angle = current + Math.max(-turn, Math.min(turn, diff));
				ctx.direction = new Vec3(Math.cos(angle), 0, Math.sin(angle));
			}
		}
		boss.faceDirection(ctx.direction, 30.0F);
		double speed = BastionConfig.ROLL_SPEED * boss.getPhase().speedMultiplier() * (boss.isEnraged() ? 1.15 : 1.0);
		boss.setDeltaMovement(ctx.direction.x * speed, boss.getDeltaMovement().y, ctx.direction.z * speed);
		if (tick % 2 == 0) {
			boss.crushObstacles(level, ctx.direction);
			Vec3 feet = boss.position();
			level.sendParticles(debris(level, feet), feet.x, feet.y + 0.2, feet.z, 10, boss.getBbWidth() * 0.4, 0.1, boss.getBbWidth() * 0.4, 0.2);
		}
		if (boss.horizontalCollision) {
			ctx.travelled++;
			if (ctx.travelled >= 4) {
				boss.playSound(ModSounds.BASTION_BEHEMOTH_QUAKE, 4.0F, 1.1F);
				level.sendParticles(ParticleTypes.EXPLOSION, boss.getX(), boss.getY(0.5), boss.getZ(), 3, 1.0, 0.5, 1.0, 0);
				boss.setDeltaMovement(0, boss.getDeltaMovement().y, 0);
				return Result.SKIP_TO_RECOVERY;
			}
		} else {
			ctx.travelled = 0;
		}
		float damage = scaled(boss, BastionConfig.ROLL_DAMAGE);
		for (LivingEntity victim : CombatUtil.targetsInBox(level, boss, boss.getBoundingBox().inflate(0.8))) {
			if (ctx.markHit(victim.getUUID()) && victim.hurtServer(level, boss.damageSources().mobAttack(boss), damage)) {
				Vec3 side = victim.position().subtract(boss.position()).multiply(1, 0, 1);
				Vec3 push = ctx.direction.add(side.lengthSqr() > 1.0E-4 ? side.normalize().scale(0.6) : Vec3.ZERO);
				CombatUtil.knockback(victim, push.x, push.z, BastionConfig.ROLL_KNOCKBACK, 0.6);
			}
		}
		return Result.CONTINUE;
	}
}
