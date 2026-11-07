package com.bossmode.entity.sovereign.attack;

import com.bossmode.config.SovereignConfig;
import com.bossmode.entity.action.ActionContext;
import com.bossmode.entity.sovereign.VoidSovereignEntity;
import com.bossmode.registry.ModSounds;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import org.jspecify.annotations.Nullable;

/**
 * Abyssal Carapace (phase 6+): a shell of hardened void closes around the Sovereign for four seconds. Every blow is
 * capped at {@link SovereignConfig#CARAPACE_HIT_CAP} and a share of what it would have dealt beyond that is thrown back
 * at the attacker - a full mace smash into the carapace mostly hurts the one who swung it. Steady, ordinary hits lose
 * nothing; wait for the shell to crack before the next big one.
 */
public final class AbyssalCarapaceAction extends SovereignAction {
	public AbyssalCarapaceAction() {
		super("abyssal_carapace", true, SovereignConfig.CARAPACE_START_TICK, SovereignConfig.CARAPACE_END_TICK - SovereignConfig.CARAPACE_START_TICK,
				SovereignConfig.CARAPACE_TOTAL_TICKS, SovereignConfig.CARAPACE_COOLDOWN, SovereignConfig.CARAPACE_WEIGHT);
	}

	@Override
	public int weight(VoidSovereignEntity boss, LivingEntity target) {
		return counterWeight(boss, super.weight(boss, target));
	}

	@Override
	public boolean canUse(ServerLevel level, VoidSovereignEntity boss, LivingEntity target, double gap) {
		return gap <= 20.0;
	}

	@Override
	public Result tick(ServerLevel level, VoidSovereignEntity boss, @Nullable LivingEntity target, ActionContext ctx, int tick) {
		if (tick < prepareTicks()) {
			level.sendParticles(DARK, boss.getX(), boss.getY(0.5), boss.getZ(), 12, boss.getBbWidth() * 0.7, boss.getBbHeight() * 0.4, boss.getBbWidth() * 0.7, 0);
			return Result.CONTINUE;
		}
		if (tick == prepareTicks()) {
			boss.setCarapace(true);
			boss.playSound(ModSounds.VOID_SOVEREIGN_CARAPACE, 5.0F, 0.7F);
			boss.announce(level, Component.translatable("message.boss_mode.sovereign.carapace"));
		}
		if (tick < executionEnd()) {
			if (tick % 3 == 0) {
				// the shell: a slowly turning lattice of dark dust around the body
				double r = boss.getBbWidth() * 0.75;
				for (int i = 0; i < 18; i++) {
					double a = Math.PI * 2 * i / 18 + tick * 0.04;
					double h = (i % 6) / 5.0;
					level.sendParticles(DARK, boss.getX() + Math.cos(a) * r, boss.getY(0.1 + 0.85 * h), boss.getZ() + Math.sin(a) * r, 1, 0, 0, 0, 0);
				}
				level.sendParticles(MARK, boss.getX(), boss.getY(0.5), boss.getZ(), 4, r * 0.6, boss.getBbHeight() * 0.35, r * 0.6, 0);
			}
		} else if (tick == executionEnd()) {
			boss.setCarapace(false);
			boss.playSound(ModSounds.VOID_SOVEREIGN_CARAPACE, 4.0F, 1.3F);
			level.sendParticles(ParticleTypes.EXPLOSION, boss.getX(), boss.getY(0.5), boss.getZ(), 4, boss.getBbWidth() * 0.4, boss.getBbHeight() * 0.3,
					boss.getBbWidth() * 0.4, 0);
		}
		return Result.CONTINUE;
	}

	@Override
	public void stop(ServerLevel level, VoidSovereignEntity boss, ActionContext ctx, boolean interrupted) {
		boss.setCarapace(false);
	}
}
