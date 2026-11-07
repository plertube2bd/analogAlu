package com.bossmode.entity.tempest.attack;

import com.bossmode.config.TempestConfig;
import com.bossmode.entity.action.ActionContext;
import com.bossmode.entity.boss.combat.CombatUtil;
import com.bossmode.entity.tempest.TempestWyvernEntity;
import com.bossmode.registry.ModSounds;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Lightning Breath: after a crackling wind-up the Wyvern breathes a beam of lightning that slowly follows its target
 * for almost two seconds. Walls stop it; keep moving sideways or break line of sight.
 */
public final class LightningBreathAttack extends TempestAction {
	public LightningBreathAttack() {
		super("lightning_breath", true, TempestConfig.BREATH_START_TICK, TempestConfig.BREATH_END_TICK - TempestConfig.BREATH_START_TICK,
				TempestConfig.BREATH_TOTAL_TICKS, TempestConfig.BREATH_COOLDOWN, TempestConfig.BREATH_WEIGHT);
	}

	@Override
	public boolean canUse(ServerLevel level, TempestWyvernEntity boss, LivingEntity target, double gap) {
		return gap <= TempestConfig.BREATH_RANGE * 0.8 && boss.getSensing().hasLineOfSight(target);
	}

	@Override
	public void start(ServerLevel level, TempestWyvernEntity boss, @Nullable LivingEntity target, ActionContext ctx) {
		ctx.destination = target != null ? target.position().add(0, target.getBbHeight() * 0.5, 0) : boss.frontPoint(10.0);
	}

	@Override
	public Result tick(ServerLevel level, TempestWyvernEntity boss, @Nullable LivingEntity target, ActionContext ctx, int tick) {
		Vec3 mouth = boss.mouthPosition();
		if (target != null) {
			Vec3 aim = target.position().add(0, target.getBbHeight() * 0.5, 0);
			ctx.destination = ctx.destination.lerp(aim, tick < prepareTicks() ? 0.5 : TempestConfig.BREATH_TRACKING);
			boss.faceDirection(ctx.destination.subtract(boss.position()), tick < prepareTicks() ? 20.0F : 3.0F);
		}
		if (tick < prepareTicks()) {
			if (tick % 2 == 0) {
				level.sendParticles(ParticleTypes.ELECTRIC_SPARK, mouth.x, mouth.y, mouth.z, 8, 0.4, 0.4, 0.4, 0.2);
			}
			if (tick == 2) {
				boss.playSound(ModSounds.TEMPEST_WYVERN_STATIC, 4.0F, 1.2F);
			}
			return Result.CONTINUE;
		}
		if (tick >= executionEnd()) {
			return Result.CONTINUE;
		}
		if (tick == prepareTicks() || tick % 12 == 0) {
			boss.playSound(ModSounds.TEMPEST_WYVERN_BREATH, 4.0F, 0.9F + boss.getRandom().nextFloat() * 0.2F);
		}
		Vec3 dir = ctx.destination.subtract(mouth);
		if (dir.lengthSqr() < 1.0E-4) {
			return Result.CONTINUE;
		}
		Vec3 end = mouth.add(dir.normalize().scale(TempestConfig.BREATH_RANGE * boss.sizeFactor()));
		HitResult wall = level.clip(new ClipContext(mouth, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, boss));
		if (wall.getType() != HitResult.Type.MISS) {
			end = wall.getLocation();
		}
		double length = end.distanceTo(mouth);
		Vec3 step = end.subtract(mouth).normalize();
		for (double d = 0; d < length; d += 0.8) {
			Vec3 p = mouth.add(step.scale(d));
			level.sendParticles(ParticleTypes.ELECTRIC_SPARK, p.x, p.y, p.z, 1, 0.15, 0.15, 0.15, 0.05);
			if (tick % 2 == 0) {
				level.sendParticles(TempestWyvernEntity.STORM_DUST, p.x, p.y, p.z, 1, 0.1, 0.1, 0.1, 0);
			}
		}
		level.sendParticles(ParticleTypes.ELECTRIC_SPARK, end.x, end.y, end.z, 6, 0.3, 0.3, 0.3, 0.3);
		if ((tick - prepareTicks()) % TempestConfig.BREATH_HIT_INTERVAL == 0) {
			ctx.resetHits();
			float damage = scaled(boss, TempestConfig.BREATH_DAMAGE);
			AABB area = new AABB(mouth, end).inflate(TempestConfig.BREATH_WIDTH);
			for (LivingEntity victim : CombatUtil.targetsInBox(level, boss, area)) {
				if (victim.getBoundingBox().inflate(TempestConfig.BREATH_WIDTH * 0.5).clip(mouth, end).isPresent() && ctx.markHit(victim.getUUID())) {
					victim.hurtServer(level, boss.damageSources().indirectMagic(boss, boss), damage);
				}
			}
		}
		return Result.CONTINUE;
	}
}
