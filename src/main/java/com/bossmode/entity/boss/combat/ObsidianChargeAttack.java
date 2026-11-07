package com.bossmode.entity.boss.combat;

import com.bossmode.config.BossConfig;
import com.bossmode.entity.action.ActionContext;
import com.bossmode.entity.action.BossAction;
import com.bossmode.entity.boss.ShardDragonEntity;
import com.bossmode.registry.ModSounds;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Obsidian Charge: paws the ground, locks its direction, dashes at 24 blocks/s for up to 30 blocks. Everything the
 * (inflated) hitbox touches is hit once per dash. Hitting a wall stuns (exposes) the dragon.
 */
public final class ObsidianChargeAttack extends BossAction<ShardDragonEntity> {
	private static final DustParticleOptions TRAIL = new DustParticleOptions(0x7B3FC4, 2.5F);

	public ObsidianChargeAttack() {
		super("obsidian_charge", true, BossConfig.CHARGE_PREPARATION_TICKS, BossConfig.CHARGE_DASH_TICKS, BossConfig.CHARGE_TOTAL_TICKS);
	}

	@Override
	public int cooldown() {
		return BossConfig.CHARGE_COOLDOWN;
	}

	@Override
	public String animationTrigger() {
		return "charge";
	}

	@Override
	public int weight(ShardDragonEntity boss, LivingEntity target) {
		return BossConfig.CHARGE_WEIGHT;
	}

	@Override
	public boolean canUse(ServerLevel level, ShardDragonEntity boss, LivingEntity target, double gap) {
		double distance = boss.distanceTo(target);
		return distance >= BossConfig.CHARGE_MIN_RANGE && distance <= BossConfig.CHARGE_MAX_RANGE && boss.onGround()
				&& boss.getSensing().hasLineOfSight(target);
	}

	@Override
	public void start(ServerLevel level, ShardDragonEntity boss, @Nullable LivingEntity target, ActionContext ctx) {
		boss.playSound(ModSounds.SHARD_DRAGON_CHARGE, 4.0F, 1.0F);
	}

	@Override
	public Result tick(ServerLevel level, ShardDragonEntity boss, @Nullable LivingEntity target, ActionContext ctx, int tick) {
		if (tick < prepareTicks()) {
			boss.setDeltaMovement(0, boss.getDeltaMovement().y, 0);
			if (target != null) {
				boss.faceTarget(target, 25.0F);
			}
			if (tick % 3 == 0) {
				BlockState ground = level.getBlockState(boss.blockPosition().below());
				if (!ground.isAir()) {
					Vec3 feet = boss.frontPoint(0.2);
					level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, ground), feet.x, boss.getY() + 0.1, feet.z, 10, boss.getBbWidth() * 0.3, 0.05, boss.getBbWidth() * 0.3, 0.1);
				}
			}
			if (tick == prepareTicks() - 1) {
				Vec3 toTarget = target != null ? target.position().subtract(boss.position()) : CombatUtil.horizontalForward(boss);
				Vec3 flat = new Vec3(toTarget.x, 0, toTarget.z);
				ctx.direction = flat.lengthSqr() < 1.0E-4 ? CombatUtil.horizontalForward(boss) : flat.normalize();
				ctx.origin = boss.position();
				boss.faceDirection(ctx.direction);
			}
			return Result.CONTINUE;
		}
		if (tick < executionEnd()) {
			return dash(level, boss, ctx, tick);
		}
		Vec3 motion = boss.getDeltaMovement();
		boss.setDeltaMovement(motion.x * 0.6, motion.y, motion.z * 0.6);
		return Result.CONTINUE;
	}

	private Result dash(ServerLevel level, ShardDragonEntity boss, ActionContext ctx, int tick) {
		Vec3 dir = ctx.direction;
		boss.faceDirection(dir);
		boss.setDeltaMovement(dir.x * BossConfig.CHARGE_SPEED, boss.getDeltaMovement().y, dir.z * BossConfig.CHARGE_SPEED);
		Vec3 behind = boss.position().subtract(dir.scale(boss.getBbWidth() * 0.5));
		level.sendParticles(ParticleTypes.LARGE_SMOKE, behind.x, boss.getY() + 0.5, behind.z, 3, 0.8, 0.4, 0.8, 0.01);
		level.sendParticles(TRAIL, behind.x, boss.getY() + 1.0, behind.z, 4, 1.0, 0.6, 1.0, 0);
		double inflate = BossConfig.CHARGE_HITBOX_INFLATE * boss.sizeFactor();
		AABB sweep = boss.getBoundingBox().inflate(inflate, 0, inflate).expandTowards(dir.scale(BossConfig.CHARGE_SPEED));
		float damage = (float) (BossConfig.CHARGE_DAMAGE * boss.getDamageMultiplier());
		for (LivingEntity victim : CombatUtil.targetsInBox(level, boss, sweep)) {
			if (ctx.markHit(victim.getUUID()) && victim.hurtServer(level, boss.damageSources().mobAttack(boss), damage)) {
				Vec3 side = victim.position().subtract(boss.position());
				double sideSign = Math.signum(dir.x * side.z - dir.z * side.x);
				Vec3 fling = dir.add(new Vec3(-dir.z, 0, dir.x).scale(sideSign == 0 ? 1 : sideSign * 0.8));
				CombatUtil.knockback(victim, fling.x, fling.z, BossConfig.CHARGE_KNOCKBACK, 0.6);
				boss.playSound(ModSounds.SHARD_DRAGON_BITE, 3.0F, 0.7F);
			}
		}
		if (boss.horizontalCollision) {
			boss.crushObstacles(level, dir);
		}
		double dx = boss.getX() - ctx.origin.x;
		double dz = boss.getZ() - ctx.origin.z;
		ctx.travelled = Math.sqrt(dx * dx + dz * dz);
		if (tick > prepareTicks() + 3 && boss.horizontalCollision && boss.getDeltaMovement().horizontalDistance() < 0.2) {
			boss.setDeltaMovement(0, boss.getDeltaMovement().y, 0);
			boss.playSound(ModSounds.SHARD_DRAGON_SLAM, 4.0F, 1.2F);
			Vec3 front = boss.frontPoint(0.3);
			level.sendParticles(ParticleTypes.EXPLOSION, front.x, boss.getY() + 1.5, front.z, 4, 0.8, 0.8, 0.8, 0);
			ctx.extraRecoveryTicks = BossConfig.CHARGE_WALL_STUN_TICKS;
			return Result.SKIP_TO_RECOVERY;
		}
		return ctx.travelled >= BossConfig.CHARGE_DISTANCE ? Result.SKIP_TO_RECOVERY : Result.CONTINUE;
	}

	@Override
	public void stop(ServerLevel level, ShardDragonEntity boss, ActionContext ctx, boolean interrupted) {
		Vec3 motion = boss.getDeltaMovement();
		boss.setDeltaMovement(motion.x * 0.2, motion.y, motion.z * 0.2);
	}
}
