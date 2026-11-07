package com.bossmode.entity.boss.combat;

import com.bossmode.config.BossConfig;
import com.bossmode.entity.action.ActionContext;
import com.bossmode.entity.action.BossAction;
import com.bossmode.entity.boss.ShardDragonEntity;
import com.bossmode.registry.ModSounds;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Spike Eruption: the dragon stomps and {@link BossConfig#SPIKES_LINES} lines of crystal spikes race outward - one
 * aimed at each nearby target, the rest spread evenly. Each entity is hit at most once per eruption.
 */
public final class SpikeEruptionAttack extends BossAction<ShardDragonEntity> {
	private static final int STEPS = 16;

	public SpikeEruptionAttack() {
		super("spike_eruption", true, BossConfig.SPIKES_HIT_TICK, STEPS, BossConfig.SPIKES_TOTAL_TICKS);
	}

	@Override
	public int cooldown() {
		return BossConfig.SPIKES_COOLDOWN;
	}

	@Override
	public String animationTrigger() {
		return "spike_eruption";
	}

	@Override
	public int weight(ShardDragonEntity boss, LivingEntity target) {
		return BossConfig.SPIKES_WEIGHT;
	}

	@Override
	public boolean canUse(ServerLevel level, ShardDragonEntity boss, LivingEntity target, double gap) {
		return boss.onGround() && boss.distanceTo(target) <= STEPS * BossConfig.SPIKES_STEP * boss.sizeFactor();
	}

	@Override
	public Result tick(ServerLevel level, ShardDragonEntity boss, @Nullable LivingEntity target, ActionContext ctx, int tick) {
		if (tick < prepareTicks()) {
			if (target != null) {
				boss.faceTarget(target, 15.0F);
			}
			return Result.CONTINUE;
		}
		if (tick == prepareTicks()) {
			boss.playSound(ModSounds.SHARD_DRAGON_SLAM, 4.0F, 1.1F);
			boss.playSound(ModSounds.SHARD_DRAGON_SPIKES, 4.0F, 0.8F);
			int aimed = 0;
			for (LivingEntity victim : CombatUtil.targetsInRadius(level, boss, boss.position(), STEPS * BossConfig.SPIKES_STEP * boss.sizeFactor())) {
				if (aimed++ >= BossConfig.SPIKES_LINES / 2) {
					break;
				}
				addLine(boss, ctx, victim.position().subtract(boss.position()));
			}
			double base = Math.toRadians(boss.getYRot());
			for (int i = 0; ctx.directions.size() < BossConfig.SPIKES_LINES; i++) {
				double a = base + Math.PI * 2 * i / (BossConfig.SPIKES_LINES - aimed);
				addLine(boss, ctx, new Vec3(-Math.sin(a), 0, Math.cos(a)));
			}
		}
		int step = tick - prepareTicks();
		if (step >= 0 && step < STEPS) {
			float damage = (float) (BossConfig.SPIKES_DAMAGE * boss.getDamageMultiplier());
			double stepLength = BossConfig.SPIKES_STEP * boss.sizeFactor();
			for (int i = ctx.directions.size() - 1; i >= 0; i--) {
				Vec3 dir = ctx.directions.get(i);
				if (dir == Vec3.ZERO) {
					continue;
				}
				if (!SpikeLine.erupt(level, boss, boss.damageSources().mobAttack(boss), ctx.points.get(i), dir, step, stepLength,
						BossConfig.SPIKES_WIDTH * boss.sizeFactor(), damage, 0.9, ctx)) {
					ctx.directions.set(i, Vec3.ZERO);
				}
			}
		}
		return Result.CONTINUE;
	}

	private static void addLine(ShardDragonEntity boss, ActionContext ctx, Vec3 towards) {
		Vec3 flat = new Vec3(towards.x, 0, towards.z);
		if (flat.lengthSqr() < 1.0E-4) {
			return;
		}
		Vec3 dir = flat.normalize();
		ctx.directions.add(dir);
		ctx.points.add(boss.position().add(dir.scale(boss.getBbWidth() * 0.5)));
	}
}
