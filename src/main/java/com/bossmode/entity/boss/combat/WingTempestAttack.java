package com.bossmode.entity.boss.combat;

import com.bossmode.config.BossConfig;
import com.bossmode.entity.action.ActionContext;
import com.bossmode.entity.action.BossAction;
import com.bossmode.entity.boss.ShardDragonEntity;
import com.bossmode.registry.ModSounds;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/** Wing Tempest: a 360 degree damaging gust, then a wider damage-free push (each gust is its own hit window). */
public final class WingTempestAttack extends BossAction<ShardDragonEntity> {
	public WingTempestAttack() {
		super("wing_tempest", true, BossConfig.TEMPEST_DAMAGE_TICK, BossConfig.TEMPEST_PUSH_TICK - BossConfig.TEMPEST_DAMAGE_TICK + 1,
				BossConfig.TEMPEST_TOTAL_TICKS);
	}

	@Override
	public int cooldown() {
		return BossConfig.TEMPEST_COOLDOWN;
	}

	@Override
	public String animationTrigger() {
		return "special_tempest";
	}

	@Override
	public int weight(ShardDragonEntity boss, LivingEntity target) {
		if (!(boss.level() instanceof ServerLevel level)) {
			return BossConfig.TEMPEST_WEIGHT;
		}
		int crowd = CombatUtil.targetsInRadius(level, boss, boss.position(), BossConfig.TEMPEST_RADIUS * boss.sizeFactor()).size();
		return BossConfig.TEMPEST_WEIGHT + Math.max(0, crowd - 1) * 15;
	}

	@Override
	public boolean canUse(ServerLevel level, ShardDragonEntity boss, LivingEntity target, double gap) {
		return gap <= BossConfig.TEMPEST_MAX_TARGET_DISTANCE * boss.sizeFactor();
	}

	@Override
	public Result tick(ServerLevel level, ShardDragonEntity boss, @Nullable LivingEntity target, ActionContext ctx, int tick) {
		double sf = boss.sizeFactor();
		if (tick == 6 || tick == 22) {
			boss.playSound(ModSounds.SHARD_DRAGON_TEMPEST, 4.0F, 0.8F);
		}
		if (tick < prepareTicks() && tick % 3 == 0) {
			for (int i = 0; i < 8; i++) {
				double a = Math.PI * 2 * i / 8 + tick * 0.3;
				level.sendParticles(ParticleTypes.GUST, boss.getX() + Math.cos(a) * 4 * sf, boss.getY() + 1.5 * sf, boss.getZ() + Math.sin(a) * 4 * sf, 1, 0, 0, 0, 0);
			}
		}
		if (tick == BossConfig.TEMPEST_DAMAGE_TICK) {
			gust(level, boss, ctx, BossConfig.TEMPEST_RADIUS * sf, (float) (BossConfig.TEMPEST_DAMAGE * boss.getDamageMultiplier()), BossConfig.TEMPEST_KNOCKBACK);
		} else if (tick == BossConfig.TEMPEST_PUSH_TICK) {
			ctx.resetHits();
			gust(level, boss, ctx, BossConfig.TEMPEST_PUSH_RADIUS * sf, 0.0F, BossConfig.TEMPEST_PUSH_KNOCKBACK);
		}
		return Result.CONTINUE;
	}

	private void gust(ServerLevel level, ShardDragonEntity boss, ActionContext ctx, double radius, float damage, double knockback) {
		Vec3 center = boss.position().add(0, 1.0, 0);
		level.sendParticles(ParticleTypes.GUST_EMITTER_LARGE, center.x, center.y, center.z, 1, 0, 0, 0, 0);
		for (int i = 0; i < 40; i++) {
			double a = Math.PI * 2 * i / 40;
			level.sendParticles(ParticleTypes.CLOUD, center.x + Math.cos(a) * radius * 0.5, boss.getY() + 0.4, center.z + Math.sin(a) * radius * 0.5,
					0, Math.cos(a), 0.05, Math.sin(a), 0.8);
		}
		for (LivingEntity victim : CombatUtil.targetsInRadius(level, boss, center, radius)) {
			if (!ctx.markHit(victim.getUUID())) {
				continue;
			}
			if (damage > 0) {
				victim.hurtServer(level, boss.damageSources().mobAttack(boss), damage);
			}
			CombatUtil.knockbackFrom(victim, center, knockback, 0.5);
		}
	}
}
