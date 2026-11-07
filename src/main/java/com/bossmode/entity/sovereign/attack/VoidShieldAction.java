package com.bossmode.entity.sovereign.attack;

import com.bossmode.config.SovereignConfig;
import com.bossmode.entity.action.ActionContext;
import com.bossmode.entity.sovereign.VoidSovereignEntity;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import org.jspecify.annotations.Nullable;

/**
 * Void Shield: forced by the Sovereign when a pylon phase (3, 5) begins - never picked at random. It crosses its arms,
 * the shield closes around it and the phase's pylons rise around the arena ({@link VoidSovereignEntity#raiseShield}).
 */
public final class VoidShieldAction extends SovereignAction {
	public VoidShieldAction() {
		super("shield", true, SovereignConfig.SHIELD_RAISE_TICK, 1, SovereignConfig.SHIELD_TOTAL_TICKS, 1, 1);
	}

	@Override
	public boolean canUse(ServerLevel level, VoidSovereignEntity boss, LivingEntity target, double gap) {
		return false;
	}

	@Override
	public Result tick(ServerLevel level, VoidSovereignEntity boss, @Nullable LivingEntity target, ActionContext ctx, int tick) {
		if (tick < prepareTicks()) {
			level.sendParticles(ParticleTypes.PORTAL, boss.getX(), boss.getY(0.5), boss.getZ(), 20, boss.getBbWidth() * 0.6, boss.getBbHeight() * 0.4,
					boss.getBbWidth() * 0.6, 0.5);
		} else if (tick == prepareTicks()) {
			boss.raiseShield(level);
		}
		return Result.CONTINUE;
	}
}
