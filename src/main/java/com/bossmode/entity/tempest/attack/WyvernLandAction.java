package com.bossmode.entity.tempest.attack;

import com.bossmode.config.TempestConfig;
import com.bossmode.entity.action.ActionContext;
import com.bossmode.entity.boss.combat.CombatUtil;
import com.bossmode.entity.tempest.TempestWyvernEntity;
import com.bossmode.registry.ModSounds;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/** Landing (flight cycle): a braking descent that touches down exactly at the impact tick with a gust around the Wyvern. */
public final class WyvernLandAction extends TempestAction {
	public WyvernLandAction() {
		super("land", true, TempestConfig.LAND_IMPACT_TICK, 1, TempestConfig.LAND_TOTAL_TICKS, 1, 1);
	}

	@Override
	public boolean canUse(ServerLevel level, TempestWyvernEntity boss, LivingEntity target, double gap) {
		return true;
	}

	@Override
	public Result tick(ServerLevel level, TempestWyvernEntity boss, @Nullable LivingEntity target, ActionContext ctx, int tick) {
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
			boss.playSound(ModSounds.TEMPEST_WYVERN_GUST, 4.0F, 0.7F);
			double radius = TempestConfig.LAND_RADIUS * boss.sizeFactor() + boss.getBbWidth() * 0.5;
			for (int i = 0; i < 36; i++) {
				double a = Math.PI * 2 * i / 36;
				level.sendParticles(ParticleTypes.CLOUD, boss.getX(), boss.getY() + 0.3, boss.getZ(), 0, Math.cos(a), 0.02, Math.sin(a), 0.9);
			}
			float damage = scaled(boss, TempestConfig.LAND_DAMAGE);
			for (LivingEntity victim : CombatUtil.targetsInRadius(level, boss, boss.position(), radius)) {
				if (ctx.markHit(victim.getUUID()) && victim.hurtServer(level, boss.damageSources().mobAttack(boss), damage)) {
					CombatUtil.knockbackFrom(victim, boss.position(), 1.2, 0.4);
				}
			}
		}
		return Result.CONTINUE;
	}
}
