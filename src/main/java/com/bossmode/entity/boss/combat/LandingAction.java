package com.bossmode.entity.boss.combat;

import com.bossmode.config.BossConfig;
import com.bossmode.entity.action.ActionContext;
import com.bossmode.entity.action.BossAction;
import com.bossmode.entity.boss.ShardDragonEntity;
import com.bossmode.registry.ModSounds;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Landing (flight cycle): a braking descent that reaches the ground exactly at the impact tick, where the touchdown
 * shakes everything close to the dragon. Afterwards it fights on the ground until it takes off again.
 */
public final class LandingAction extends BossAction<ShardDragonEntity> {
	public LandingAction() {
		super("land", true, BossConfig.LAND_IMPACT_TICK, 1, BossConfig.LAND_TOTAL_TICKS);
	}

	@Override
	public int cooldown() {
		return 1;
	}

	@Override
	public String animationTrigger() {
		return "land";
	}

	@Override
	public boolean canUse(ServerLevel level, ShardDragonEntity boss, LivingEntity target, double gap) {
		return true;
	}

	@Override
	public Result tick(ServerLevel level, ShardDragonEntity boss, @Nullable LivingEntity target, ActionContext ctx, int tick) {
		if (tick < prepareTicks()) {
			if (target != null) {
				boss.faceTarget(target, 10.0F);
			}
			double ground = boss.groundBelow(level);
			double fall = Mth.clamp((ground - boss.getY()) / Math.max(1, prepareTicks() - tick), -1.5, 0.1);
			Vec3 v = boss.getDeltaMovement();
			boss.setDeltaMovement(v.x * 0.7, fall, v.z * 0.7);
			return Result.CONTINUE;
		}
		if (tick == prepareTicks()) {
			boss.setFlying(false);
			boss.setDeltaMovement(0, -0.5, 0);
			boss.playSound(ModSounds.SHARD_DRAGON_SLAM, 5.0F, 0.8F);
			double radius = BossConfig.LAND_RADIUS * boss.sizeFactor() + boss.getBbWidth() * 0.5;
			level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.OBSIDIAN.defaultBlockState()),
					boss.getX(), boss.getY() + 0.3, boss.getZ(), 60, radius * 0.4, 0.2, radius * 0.4, 0.25);
			level.sendParticles(ParticleTypes.EXPLOSION, boss.getX(), boss.getY() + 0.5, boss.getZ(), 6, radius * 0.4, 0.2, radius * 0.4, 0);
			float damage = (float) (BossConfig.LAND_DAMAGE * boss.getDamageMultiplier());
			for (LivingEntity victim : CombatUtil.targetsInRadius(level, boss, boss.position(), radius)) {
				if (ctx.markHit(victim.getUUID()) && victim.hurtServer(level, boss.damageSources().mobAttack(boss), damage)) {
					CombatUtil.knockbackFrom(victim, boss.position(), 1.2, 0.5);
				}
			}
		}
		return Result.CONTINUE;
	}
}
