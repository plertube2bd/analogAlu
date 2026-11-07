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
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/** Shatter Slam: rears up (a ring telegraphs the zone), slams - radial AOE, outward knockback, slowness. */
public final class ShatterSlamAttack extends BossAction<ShardDragonEntity> {
	private static final DustParticleOptions WARNING_DUST = new DustParticleOptions(0xB040FF, 2.0F);

	public ShatterSlamAttack() {
		super("shatter_slam", true, BossConfig.SLAM_HIT_TICK, 2, BossConfig.SLAM_TOTAL_TICKS);
	}

	@Override
	public int cooldown() {
		return BossConfig.SLAM_COOLDOWN;
	}

	@Override
	public String animationTrigger() {
		return "special_slam";
	}

	@Override
	public int weight(ShardDragonEntity boss, LivingEntity target) {
		return BossConfig.SLAM_WEIGHT;
	}

	@Override
	public boolean canUse(ServerLevel level, ShardDragonEntity boss, LivingEntity target, double gap) {
		return gap <= BossConfig.SLAM_MAX_TARGET_DISTANCE * boss.sizeFactor() && boss.onGround();
	}

	@Override
	public void start(ServerLevel level, ShardDragonEntity boss, @Nullable LivingEntity target, ActionContext ctx) {
		boss.playSound(ModSounds.SHARD_DRAGON_ROAR, 3.0F, 1.25F);
	}

	@Override
	public Result tick(ServerLevel level, ShardDragonEntity boss, @Nullable LivingEntity target, ActionContext ctx, int tick) {
		double radius = BossConfig.SLAM_RADIUS * boss.sizeFactor();
		Vec3 center = boss.frontPoint(1.0);
		if (tick < prepareTicks()) {
			if (target != null && tick < prepareTicks() - 6) {
				boss.faceTarget(target, 12.0F);
			}
			if (tick >= 6 && tick % 4 == 0) {
				for (int i = 0; i < 32; i++) {
					double a = Math.PI * 2 * i / 32;
					level.sendParticles(WARNING_DUST, center.x + Math.cos(a) * radius, boss.getY() + 0.1, center.z + Math.sin(a) * radius, 1, 0, 0, 0, 0);
				}
			}
			return Result.CONTINUE;
		}
		if (tick == prepareTicks()) {
			boss.playSound(ModSounds.SHARD_DRAGON_SLAM, 5.0F, 0.8F);
			BlockState ground = level.getBlockState(boss.blockPosition().below());
			BlockParticleOption debris = new BlockParticleOption(ParticleTypes.BLOCK, ground.isAir() ? Blocks.OBSIDIAN.defaultBlockState() : ground);
			for (int ring = 1; ring <= 3; ring++) {
				double rr = radius * ring / 3.0;
				for (int i = 0; i < 16; i++) {
					double a = Math.PI * 2 * i / 16;
					level.sendParticles(debris, center.x + Math.cos(a) * rr, boss.getY() + 0.2, center.z + Math.sin(a) * rr, 4, 0.3, 0.1, 0.3, 0.15);
				}
			}
			level.sendParticles(ParticleTypes.EXPLOSION, center.x, boss.getY() + 0.5, center.z, 6, radius * 0.3, 0.2, radius * 0.3, 0);
			float damage = (float) (BossConfig.SLAM_DAMAGE * boss.getDamageMultiplier());
			for (LivingEntity victim : CombatUtil.targetsInRadius(level, boss, center, radius)) {
				if (ctx.markHit(victim.getUUID()) && victim.hurtServer(level, boss.damageSources().mobAttack(boss), damage)) {
					CombatUtil.knockbackFrom(victim, center, BossConfig.SLAM_KNOCKBACK, BossConfig.SLAM_KNOCKUP);
					victim.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, BossConfig.SLAM_SLOWNESS_TICKS, 1), boss);
				}
			}
		}
		return Result.CONTINUE;
	}
}
