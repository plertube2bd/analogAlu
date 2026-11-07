package com.bossmode.entity.boss.combat;

import com.bossmode.config.BossConfig;
import com.bossmode.entity.action.ActionContext;
import com.bossmode.entity.action.BossAction;
import com.bossmode.entity.boss.ShardDragonEntity;
import com.bossmode.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Meteor Dive: crouches, leaps along an arc to where the target stood (the landing zone is marked the whole time)
 * and crashes down. From phase 6 it is also how a flight often ends - a swoop from the sky onto the target. The leap is moved by the server tick by tick; the impact hits everything in the zone once.
 */
public final class MeteorDiveAttack extends BossAction<ShardDragonEntity> {
	private static final DustParticleOptions MARK = new DustParticleOptions(0xFF3050, 2.0F);

	public MeteorDiveAttack() {
		super("meteor_dive", true, BossConfig.DIVE_LAND_TICK, 1, BossConfig.DIVE_TOTAL_TICKS);
	}

	@Override
	public int cooldown() {
		return BossConfig.DIVE_COOLDOWN;
	}

	@Override
	public String animationTrigger() {
		return "meteor_dive";
	}

	@Override
	public int weight(ShardDragonEntity boss, LivingEntity target) {
		return boss.isTargetUnreachable() ? BossConfig.DIVE_WEIGHT * 3 : BossConfig.DIVE_WEIGHT;
	}

	@Override
	public boolean canUse(ServerLevel level, ShardDragonEntity boss, LivingEntity target, double gap) {
		double distance = boss.distanceTo(target);
		return (boss.onGround() || boss.isFlying()) && distance >= BossConfig.DIVE_MIN_RANGE && distance <= BossConfig.DIVE_MAX_RANGE;
	}

	@Override
	public Result tick(ServerLevel level, ShardDragonEntity boss, @Nullable LivingEntity target, ActionContext ctx, int tick) {
		double radius = BossConfig.DIVE_RADIUS * boss.sizeFactor();
		if (tick < BossConfig.DIVE_LAUNCH_TICK) {
			boss.setDeltaMovement(0, boss.getDeltaMovement().y, 0);
			if (target != null) {
				boss.faceTarget(target, 20.0F);
			}
			if (tick == BossConfig.DIVE_LAUNCH_TICK - 1) {
				Vec3 aim = target != null ? target.position() : boss.frontPoint(12.0);
				int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, BlockPos.containing(aim));
				ctx.origin = boss.position();
				ctx.destination = new Vec3(aim.x, y, aim.z);
				ctx.direction = ctx.destination.subtract(ctx.origin);
				boss.playSound(ModSounds.SHARD_DRAGON_TEMPEST, 5.0F, 0.6F);
			}
			return Result.CONTINUE;
		}
		if (tick < prepareTicks()) {
			double u = (tick - BossConfig.DIVE_LAUNCH_TICK + 1) / (double) (BossConfig.DIVE_LAND_TICK - BossConfig.DIVE_LAUNCH_TICK);
			Vec3 p = ctx.origin.lerp(ctx.destination, u).add(0, BossConfig.DIVE_ARC_HEIGHT * boss.sizeFactor() * 4 * u * (1 - u), 0);
			boss.noPhysics = true;
			boss.setDeltaMovement(Vec3.ZERO);
			boss.setPos(p.x, p.y, p.z);
			boss.resetFallDistance();
			boss.faceDirection(ctx.direction);
			if (tick % 3 == 0) {
				for (int i = 0; i < 24; i++) {
					double a = Math.PI * 2 * i / 24;
					level.sendParticles(MARK, ctx.destination.x + Math.cos(a) * radius, ctx.destination.y + 0.15, ctx.destination.z + Math.sin(a) * radius, 1, 0, 0, 0, 0);
				}
			}
			return Result.CONTINUE;
		}
		if (tick == prepareTicks()) {
			boss.noPhysics = false;
			if (boss.isFlying()) {
				boss.setFlying(false); // a dive from the air ends the flight
			}
			boss.setPos(ctx.destination.x, ctx.destination.y, ctx.destination.z);
			boss.playSound(ModSounds.SHARD_DRAGON_SLAM, 6.0F, 0.6F);
			level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, ctx.destination.x, ctx.destination.y + 1, ctx.destination.z, 1, 0, 0, 0, 0);
			level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.OBSIDIAN.defaultBlockState()),
					ctx.destination.x, ctx.destination.y + 0.5, ctx.destination.z, 80, radius * 0.4, 0.3, radius * 0.4, 0.3);
			float damage = (float) (BossConfig.DIVE_DAMAGE * boss.getDamageMultiplier());
			for (LivingEntity victim : CombatUtil.targetsInRadius(level, boss, ctx.destination, radius)) {
				if (ctx.markHit(victim.getUUID()) && victim.hurtServer(level, boss.damageSources().mobAttack(boss), damage)) {
					CombatUtil.knockbackFrom(victim, ctx.destination, 1.8, 0.8);
				}
			}
			boss.crushObstacles(level, Vec3.ZERO);
		}
		return Result.CONTINUE;
	}

	@Override
	public void stop(ServerLevel level, ShardDragonEntity boss, ActionContext ctx, boolean interrupted) {
		boss.noPhysics = false;
	}
}
