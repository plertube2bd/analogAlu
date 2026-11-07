package com.bossmode.entity.boss.combat;

import com.bossmode.config.BossConfig;
import com.bossmode.entity.action.ActionContext;
import com.bossmode.entity.action.BossAction;
import com.bossmode.entity.boss.ShardDragonEntity;
import com.bossmode.registry.ModSounds;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import org.jspecify.annotations.Nullable;

/** Normal attack - Tail Sweep: punishes players standing beside / behind the dragon (everything outside the jaws' cone). */
public final class TailSweepAttack extends BossAction<ShardDragonEntity> {
	private static final double FRONT_CONE = 100.0;

	public TailSweepAttack() {
		super("tail_sweep", false, BossConfig.TAIL_HIT_TICK, 1, BossConfig.TAIL_TOTAL_TICKS);
	}

	@Override
	public int cooldown() {
		return BossConfig.TAIL_COOLDOWN;
	}

	@Override
	public String animationTrigger() {
		return "tail_sweep";
	}

	@Override
	public boolean canUse(ServerLevel level, ShardDragonEntity boss, LivingEntity target, double gap) {
		return gap <= BossConfig.TAIL_RANGE * boss.sizeFactor() && !CombatUtil.isInFront(boss, target, 70.0);
	}

	@Override
	public Result tick(ServerLevel level, ShardDragonEntity boss, @Nullable LivingEntity target, ActionContext ctx, int tick) {
		if (tick != prepareTicks()) {
			return Result.CONTINUE;
		}
		double radius = boss.getBbWidth() / 2.0 + BossConfig.TAIL_RANGE * boss.sizeFactor();
		boss.playSound(ModSounds.SHARD_DRAGON_TAIL, 3.0F, 0.8F);
		for (int i = 0; i < 24; i++) {
			double a = Math.toRadians(boss.getYRot() + 90 + i * 11.25);
			level.sendParticles(ParticleTypes.CLOUD, boss.getX() - Math.sin(a) * radius * 0.8, boss.getY() + 0.6, boss.getZ() + Math.cos(a) * radius * 0.8,
					1, 0.2, 0.1, 0.2, 0.02);
		}
		float damage = (float) (BossConfig.TAIL_DAMAGE * boss.getDamageMultiplier());
		for (LivingEntity victim : CombatUtil.targetsInRadius(level, boss, boss.position().add(0, 1.0, 0), radius)) {
			if (!CombatUtil.isInFront(boss, victim, FRONT_CONE) && ctx.markHit(victim.getUUID())
					&& victim.hurtServer(level, boss.damageSources().mobAttack(boss), damage)) {
				CombatUtil.knockbackFrom(victim, boss.position(), BossConfig.TAIL_KNOCKBACK, 0.45);
			}
		}
		return Result.CONTINUE;
	}
}
