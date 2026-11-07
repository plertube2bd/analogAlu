package com.bossmode.entity.boss.combat;

import com.bossmode.config.BossConfig;
import com.bossmode.entity.action.ActionContext;
import com.bossmode.entity.action.BossAction;
import com.bossmode.entity.boss.ShardDragonEntity;
import com.bossmode.registry.ModSounds;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Shot down: forced when the dragon takes enough damage during one flight. It drops out of the sky, crashes onto
 * whatever is below and lies stunned - EXPOSED - for three seconds: the reward for shooting it down, and the melee
 * window of the melee-only phases.
 */
public final class CrashLandingAction extends BossAction<ShardDragonEntity> {
	public CrashLandingAction() {
		super("crash", true, BossConfig.CRASH_IMPACT_TICK, 1, BossConfig.CRASH_TOTAL_TICKS);
	}

	@Override
	public int cooldown() {
		return 1;
	}

	@Override
	public String animationTrigger() {
		return "crash";
	}

	@Override
	public boolean canUse(ServerLevel level, ShardDragonEntity boss, LivingEntity target, double gap) {
		return true;
	}

	@Override
	public void start(ServerLevel level, ShardDragonEntity boss, @Nullable LivingEntity target, ActionContext ctx) {
		boss.setFlying(false);
		boss.playSound(ModSounds.SHARD_DRAGON_HURT, 6.0F, 0.6F);
		boss.playSound(ModSounds.SHARD_DRAGON_ROAR, 5.0F, 1.4F);
	}

	@Override
	public Result tick(ServerLevel level, ShardDragonEntity boss, @Nullable LivingEntity target, ActionContext ctx, int tick) {
		if (tick < prepareTicks()) {
			Vec3 v = boss.getDeltaMovement();
			boss.setDeltaMovement(v.x * 0.8, Math.min(v.y, -0.9), v.z * 0.8);
			if (tick % 2 == 0) {
				level.sendParticles(ParticleTypes.LARGE_SMOKE, boss.getX(), boss.getY(0.6), boss.getZ(), 6, boss.getBbWidth() * 0.3, 0.5, boss.getBbWidth() * 0.3, 0.02);
			}
			return Result.CONTINUE;
		}
		if (tick == prepareTicks()) {
			if (!boss.onGround()) {
				boss.setPos(boss.getX(), boss.groundBelow(level), boss.getZ());
			}
			boss.setDeltaMovement(Vec3.ZERO);
			boss.playSound(ModSounds.SHARD_DRAGON_SLAM, 7.0F, 0.5F);
			double radius = BossConfig.CRASH_RADIUS * boss.sizeFactor() + boss.getBbWidth() * 0.5;
			level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, boss.getX(), boss.getY() + 1.0, boss.getZ(), 1, 0, 0, 0, 0);
			level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.AMETHYST_BLOCK.defaultBlockState()),
					boss.getX(), boss.getY() + 0.5, boss.getZ(), 90, radius * 0.4, 0.3, radius * 0.4, 0.3);
			float damage = (float) (BossConfig.CRASH_DAMAGE * boss.getDamageMultiplier());
			for (LivingEntity victim : CombatUtil.targetsInRadius(level, boss, boss.position(), radius)) {
				if (ctx.markHit(victim.getUUID()) && victim.hurtServer(level, boss.damageSources().mobAttack(boss), damage)) {
					CombatUtil.knockbackFrom(victim, boss.position(), 1.6, 0.6);
				}
			}
			return Result.CONTINUE;
		}
		// stunned: sparks around the head while it lies EXPOSED
		boss.setDeltaMovement(0, boss.getDeltaMovement().y, 0);
		if (tick % 6 == 0) {
			Vec3 head = boss.mouthPosition();
			level.sendParticles(ParticleTypes.CRIT, head.x, head.y + 1.0, head.z, 8, 0.8, 0.4, 0.8, 0.1);
			level.sendParticles(ParticleTypes.ELECTRIC_SPARK, boss.getX(), boss.getY(0.5), boss.getZ(), 6, boss.getBbWidth() * 0.3, 0.6, boss.getBbWidth() * 0.3, 0.1);
		}
		return Result.CONTINUE;
	}
}
