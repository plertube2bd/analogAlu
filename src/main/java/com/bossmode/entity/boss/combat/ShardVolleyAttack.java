package com.bossmode.entity.boss.combat;

import com.bossmode.config.BossConfig;
import com.bossmode.entity.action.ActionContext;
import com.bossmode.entity.action.BossAction;
import com.bossmode.entity.boss.ShardDragonEntity;
import com.bossmode.entity.projectile.WyrmShardProjectile;
import com.bossmode.registry.ModSounds;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/** Shard Volley: a fan of Wyrm Shard projectiles (3 + 2 per phase). Preferred when the target cannot be reached. */
public final class ShardVolleyAttack extends BossAction<ShardDragonEntity> {
	public ShardVolleyAttack() {
		super("shard_volley", true, BossConfig.VOLLEY_HIT_TICK, 1, BossConfig.VOLLEY_TOTAL_TICKS);
	}

	@Override
	public int cooldown() {
		return BossConfig.VOLLEY_COOLDOWN;
	}

	@Override
	public String animationTrigger() {
		return "special_volley";
	}

	@Override
	public int weight(ShardDragonEntity boss, LivingEntity target) {
		return boss.isTargetUnreachable() ? BossConfig.VOLLEY_WEIGHT * 4 : BossConfig.VOLLEY_WEIGHT;
	}

	@Override
	public boolean canUse(ServerLevel level, ShardDragonEntity boss, LivingEntity target, double gap) {
		double distance = boss.distanceTo(target);
		return distance >= BossConfig.VOLLEY_MIN_RANGE && distance <= BossConfig.VOLLEY_MAX_RANGE && boss.getSensing().hasLineOfSight(target);
	}

	@Override
	public Result tick(ServerLevel level, ShardDragonEntity boss, @Nullable LivingEntity target, ActionContext ctx, int tick) {
		Vec3 mouth = boss.mouthPosition();
		if (tick < prepareTicks()) {
			if (target != null) {
				boss.faceTarget(target, 15.0F);
			}
			if (tick % 2 == 0) {
				level.sendParticles(ParticleTypes.REVERSE_PORTAL, mouth.x, mouth.y, mouth.z, 6, 0.6, 0.6, 0.6, 0.05);
			}
		} else if (tick == prepareTicks() && target != null && target.isAlive()) {
			int count = BossConfig.VOLLEY_BASE_COUNT + 2 * (boss.getPhase().number() - 1);
			Vec3 aim = target.getBoundingBox().getCenter().subtract(mouth).normalize();
			float damage = (float) (BossConfig.PROJECTILE_DAMAGE * boss.getDamageMultiplier());
			double spread = Math.toRadians(BossConfig.VOLLEY_SPREAD_DEGREES);
			for (int i = 0; i < count; i++) {
				double offset = count == 1 ? 0.0 : spread * ((double) i / (count - 1) - 0.5);
				level.addFreshEntity(new WyrmShardProjectile(level, boss, mouth, aim.yRot((float) offset).scale(BossConfig.PROJECTILE_SPEED), damage));
			}
			boss.playSound(ModSounds.SHARD_DRAGON_VOLLEY, 4.0F, 1.0F);
			level.sendParticles(ParticleTypes.END_ROD, mouth.x, mouth.y, mouth.z, 24, 0.4, 0.4, 0.4, 0.2);
		}
		return Result.CONTINUE;
	}
}
