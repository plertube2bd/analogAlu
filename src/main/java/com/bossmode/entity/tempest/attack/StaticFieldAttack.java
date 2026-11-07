package com.bossmode.entity.tempest.attack;

import com.bossmode.config.TempestConfig;
import com.bossmode.entity.action.ActionContext;
import com.bossmode.entity.boss.combat.CombatUtil;
import com.bossmode.entity.tempest.TempestWyvernEntity;
import com.bossmode.registry.ModSounds;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Static Field: the Wyvern charges the ground around its target - a crackling circle that shocks everything inside it
 * every half second for over two seconds. Get out of the circle.
 */
public final class StaticFieldAttack extends TempestAction {
	public StaticFieldAttack() {
		super("static_field", true, TempestConfig.FIELD_START_TICK, TempestConfig.FIELD_END_TICK - TempestConfig.FIELD_START_TICK,
				TempestConfig.FIELD_TOTAL_TICKS, TempestConfig.FIELD_COOLDOWN, TempestConfig.FIELD_WEIGHT);
	}

	@Override
	public boolean canUse(ServerLevel level, TempestWyvernEntity boss, LivingEntity target, double gap) {
		return gap <= TempestConfig.FIELD_RANGE;
	}

	@Override
	public void start(ServerLevel level, TempestWyvernEntity boss, @Nullable LivingEntity target, ActionContext ctx) {
		ctx.destination = ground(level, target != null ? target.position() : boss.position());
		boss.playSound(ModSounds.TEMPEST_WYVERN_STATIC, 5.0F, 0.8F);
	}

	@Override
	public Result tick(ServerLevel level, TempestWyvernEntity boss, @Nullable LivingEntity target, ActionContext ctx, int tick) {
		Vec3 center = ctx.destination;
		double radius = TempestConfig.FIELD_RADIUS * boss.sizeFactor();
		if (target != null) {
			boss.faceTarget(target, 10.0F);
		}
		if (tick < prepareTicks()) {
			if (tick % 2 == 0) {
				ring(level, center, radius, TempestWyvernEntity.STORM_DUST, 36);
			}
			return Result.CONTINUE;
		}
		if (tick >= executionEnd()) {
			return Result.CONTINUE;
		}
		if (tick == prepareTicks()) {
			visualLightning(level, center);
		}
		if (tick % 2 == 0) {
			ring(level, center, radius, TempestWyvernEntity.STORM_DUST, 36);
			level.sendParticles(ParticleTypes.ELECTRIC_SPARK, center.x, center.y + 0.3, center.z, 24, radius * 0.5, 0.2, radius * 0.5, 0.15);
		}
		if ((tick - prepareTicks()) % TempestConfig.FIELD_PULSE == 0) {
			boss.playSound(ModSounds.TEMPEST_WYVERN_STATIC, 3.0F, 1.4F);
			float damage = scaled(boss, TempestConfig.FIELD_DAMAGE);
			for (LivingEntity victim : CombatUtil.targetsInRadius(level, boss, center, radius + 1.0)) {
				double dx = victim.getX() - center.x, dz = victim.getZ() - center.z;
				if (dx * dx + dz * dz <= radius * radius && Math.abs(victim.getY() - center.y) < 3.0) {
					victim.hurtServer(level, boss.damageSources().indirectMagic(boss, boss), damage);
				}
			}
		}
		return Result.CONTINUE;
	}
}
