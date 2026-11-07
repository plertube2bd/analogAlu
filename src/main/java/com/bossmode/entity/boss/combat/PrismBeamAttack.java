package com.bossmode.entity.boss.combat;

import com.bossmode.config.BossConfig;
import com.bossmode.entity.action.ActionContext;
import com.bossmode.entity.action.BossAction;
import com.bossmode.entity.boss.ShardDragonEntity;
import com.bossmode.registry.ModSounds;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Prism Beam: charges, then breathes a sustained crystal beam for two seconds. The beam tracks its target at a
 * limited turn rate (strafe to escape), is stopped by blocks, damages every few ticks and drags victims toward the
 * jaws like a tractor beam.
 */
public final class PrismBeamAttack extends BossAction<ShardDragonEntity> {
	private static final DustParticleOptions BEAM = new DustParticleOptions(0xE8C8FF, 1.1F);

	public PrismBeamAttack() {
		super("prism_beam", true, BossConfig.BEAM_START_TICK, BossConfig.BEAM_END_TICK - BossConfig.BEAM_START_TICK, BossConfig.BEAM_TOTAL_TICKS);
	}

	@Override
	public int cooldown() {
		return BossConfig.BEAM_COOLDOWN;
	}

	@Override
	public String animationTrigger() {
		return "prism_beam";
	}

	@Override
	public int weight(ShardDragonEntity boss, LivingEntity target) {
		return BossConfig.BEAM_WEIGHT;
	}

	@Override
	public boolean canUse(ServerLevel level, ShardDragonEntity boss, LivingEntity target, double gap) {
		return boss.distanceTo(target) >= BossConfig.BEAM_MIN_RANGE && boss.distanceTo(target) <= BossConfig.BEAM_LENGTH
				&& boss.getSensing().hasLineOfSight(target);
	}

	@Override
	public void start(ServerLevel level, ShardDragonEntity boss, @Nullable LivingEntity target, ActionContext ctx) {
		boss.playSound(ModSounds.SHARD_DRAGON_BEAM_CHARGE, 4.0F, 1.0F);
	}

	@Override
	public Result tick(ServerLevel level, ShardDragonEntity boss, @Nullable LivingEntity target, ActionContext ctx, int tick) {
		Vec3 mouth = boss.mouthPosition();
		if (tick < prepareTicks()) {
			if (target != null) {
				boss.faceTarget(target, 15.0F);
			}
			level.sendParticles(ParticleTypes.END_ROD, mouth.x, mouth.y, mouth.z, 4, 1.5, 1.5, 1.5, -0.08);
			return Result.CONTINUE;
		}
		if (tick >= executionEnd()) {
			return Result.CONTINUE;
		}
		if (tick == prepareTicks()) {
			ctx.direction = target != null ? target.getEyePosition().subtract(mouth).normalize() : CombatUtil.horizontalForward(boss);
			boss.playSound(ModSounds.SHARD_DRAGON_BEAM, 5.0F, 0.8F);
		} else if (target != null && target.isAlive()) {
			ctx.direction = turnToward(ctx.direction, target.getEyePosition().subtract(mouth).normalize(), BossConfig.BEAM_TURN_PER_TICK);
		}
		boss.faceDirection(ctx.direction);
		Vec3 end = mouth.add(ctx.direction.scale(BossConfig.BEAM_LENGTH));
		HitResult hit = level.clip(new ClipContext(mouth, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, boss));
		if (hit.getType() != HitResult.Type.MISS) {
			end = hit.getLocation();
		}
		Vec3 segment = end.subtract(mouth);
		double length = segment.length();
		int count = (int) Math.min(64, length / 0.8);
		for (int i = 0; i < count; i++) {
			Vec3 p = mouth.add(segment.scale(i / (double) count));
			level.sendParticles(BEAM, p.x, p.y, p.z, 1, 0.08, 0.08, 0.08, 0);
		}
		level.sendParticles(ParticleTypes.END_ROD, end.x, end.y, end.z, 6, 0.4, 0.4, 0.4, 0.15);
		if ((tick - prepareTicks()) % BossConfig.BEAM_DAMAGE_INTERVAL == 0) {
			ctx.resetHits();
			double width = BossConfig.BEAM_WIDTH * boss.sizeFactor();
			float damage = (float) (BossConfig.BEAM_DAMAGE * boss.getDamageMultiplier());
			for (LivingEntity victim : CombatUtil.targetsInBox(level, boss, new AABB(mouth, end).inflate(width + 1.0))) {
				Vec3 center = victim.getBoundingBox().getCenter();
				if (distanceToSegment(center, mouth, end) <= width + victim.getBbWidth() / 2.0 && ctx.markHit(victim.getUUID())
						&& victim.hurtServer(level, boss.damageSources().mobAttack(boss), damage)) {
					CombatUtil.pull(victim, mouth, BossConfig.BEAM_PULL * 4);
				}
			}
		}
		return Result.CONTINUE;
	}

	private static Vec3 turnToward(Vec3 current, Vec3 wanted, float maxDegrees) {
		double angle = Math.acos(Math.max(-1.0, Math.min(1.0, current.dot(wanted))));
		double max = Math.toRadians(maxDegrees);
		if (angle <= max || angle < 1.0E-4) {
			return wanted;
		}
		return current.lerp(wanted, max / angle).normalize();
	}

	private static double distanceToSegment(Vec3 p, Vec3 a, Vec3 b) {
		Vec3 ab = b.subtract(a);
		double lengthSqr = ab.lengthSqr();
		double t = lengthSqr == 0 ? 0 : Math.max(0, Math.min(1, p.subtract(a).dot(ab) / lengthSqr));
		return p.distanceTo(a.add(ab.scale(t)));
	}
}
