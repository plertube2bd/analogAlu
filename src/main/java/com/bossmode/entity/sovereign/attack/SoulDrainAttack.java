package com.bossmode.entity.sovereign.attack;

import com.bossmode.config.SovereignConfig;
import com.bossmode.entity.action.ActionContext;
import com.bossmode.entity.boss.combat.CombatUtil;
import com.bossmode.entity.sovereign.VoidSovereignEntity;
import com.bossmode.registry.ModSounds;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Soul Drain: a three-second channel that tears the life out of everything within {@link SovereignConfig#DRAIN_RADIUS}
 * (the ring on the ground) and heals the Sovereign by twice the damage. Leave the ring - or hit it hard enough to make
 * the heal not matter.
 */
public final class SoulDrainAttack extends SovereignAction {
	public SoulDrainAttack() {
		super("soul_drain", true, SovereignConfig.DRAIN_START_TICK, SovereignConfig.DRAIN_END_TICK - SovereignConfig.DRAIN_START_TICK,
				SovereignConfig.DRAIN_TOTAL_TICKS, SovereignConfig.DRAIN_COOLDOWN, SovereignConfig.DRAIN_WEIGHT);
	}

	@Override
	public boolean canUse(ServerLevel level, VoidSovereignEntity boss, LivingEntity target, double gap) {
		return gap <= SovereignConfig.DRAIN_RADIUS * boss.sizeFactor() * 0.7;
	}

	@Override
	public void start(ServerLevel level, VoidSovereignEntity boss, @Nullable LivingEntity target, ActionContext ctx) {
		boss.playSound(ModSounds.VOID_SOVEREIGN_DRAIN, 5.0F, 0.6F);
	}

	@Override
	public Result tick(ServerLevel level, VoidSovereignEntity boss, @Nullable LivingEntity target, ActionContext ctx, int tick) {
		double radius = boss.getBbWidth() * 0.5 + SovereignConfig.DRAIN_RADIUS * boss.sizeFactor();
		if (tick % 5 == 0) {
			ring(level, boss.position(), radius, MARK, 48);
		}
		if (tick < prepareTicks() || tick >= executionEnd()) {
			return Result.CONTINUE;
		}
		Vec3 chest = boss.chestPosition();
		for (LivingEntity victim : CombatUtil.targetsInRadius(level, boss, boss.position(), radius)) {
			Vec3 from = victim.position().add(0, victim.getBbHeight() * 0.6, 0);
			Vec3 span = chest.subtract(from);
			double t = ((tick * 0.08) % 1.0);
			Vec3 p = from.add(span.scale(t));
			level.sendParticles(ParticleTypes.SCULK_SOUL, p.x, p.y, p.z, 1, 0.1, 0.1, 0.1, 0.01);
			if ((tick - prepareTicks()) % 10 == 0) {
				float before = victim.getHealth();
				if (victim.hurtServer(level, boss.damageSources().indirectMagic(boss, boss), scaled(boss, SovereignConfig.DRAIN_DAMAGE_PER_SECOND * 0.5))) {
					boss.heal((float) (Math.max(0.0F, before - victim.getHealth()) * SovereignConfig.DRAIN_HEAL_FACTOR));
					level.sendParticles(ParticleTypes.SOUL, from.x, from.y, from.z, 4, 0.2, 0.3, 0.2, 0.02);
				}
			}
		}
		if (tick % 20 == 0) {
			boss.playSound(ModSounds.VOID_SOVEREIGN_DRAIN, 3.0F, 0.8F);
		}
		return Result.CONTINUE;
	}
}
