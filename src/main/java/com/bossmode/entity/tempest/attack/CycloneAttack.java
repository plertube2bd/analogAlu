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
 * Cyclone: a spinning flap looses whirlwinds that roam the arena for seven seconds, hunting the nearest enemy -
 * they drag in whatever comes close and fling what they catch into the air. Outrun them; they are slow.
 */
public final class CycloneAttack extends TempestAction {
	public CycloneAttack() {
		super("cyclone", true, TempestConfig.CYCLONE_HIT_TICK, 1, TempestConfig.CYCLONE_TOTAL_TICKS, TempestConfig.CYCLONE_COOLDOWN,
				TempestConfig.CYCLONE_WEIGHT);
	}

	@Override
	public boolean canUse(ServerLevel level, TempestWyvernEntity boss, LivingEntity target, double gap) {
		return boss.whirlwindCount() < 3 && gap <= 32.0;
	}

	@Override
	public Result tick(ServerLevel level, TempestWyvernEntity boss, @Nullable LivingEntity target, ActionContext ctx, int tick) {
		if (tick < prepareTicks()) {
			if (target != null) {
				boss.faceTarget(target, 12.0F);
			}
			if (tick % 2 == 0) {
				double a = tick * 0.8;
				double r = boss.getBbWidth() * 0.8;
				level.sendParticles(ParticleTypes.CLOUD, boss.getX() + Math.cos(a) * r, boss.getY(0.4), boss.getZ() + Math.sin(a) * r, 0,
						-Math.sin(a), 0.1, Math.cos(a), 0.5);
			}
			return Result.CONTINUE;
		}
		if (tick == prepareTicks()) {
			boss.playSound(ModSounds.TEMPEST_WYVERN_CYCLONE, 5.0F, 0.8F);
			int count = 1 + (boss.getPhase().number() >= 4 ? 1 : 0) + (boss.isEnraged() ? 1 : 0);
			Vec3 forward = CombatUtil.horizontalForward(boss);
			for (int i = 0; i < count; i++) {
				double angle = count == 1 ? 0.0 : Math.toRadians(-35.0 + 70.0 * i / (count - 1));
				Vec3 at = boss.position().add(forward.yRot((float) angle).scale(boss.getBbWidth() * 0.5 + 3.0));
				boss.spawnWhirlwind(level, at);
			}
		}
		return Result.CONTINUE;
	}
}
