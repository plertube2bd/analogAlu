package com.bossmode.entity.sovereign.attack;

import com.bossmode.config.SovereignConfig;
import com.bossmode.entity.action.ActionContext;
import com.bossmode.entity.boss.combat.CombatUtil;
import com.bossmode.entity.sovereign.VoidSovereignEntity;
import com.bossmode.registry.ModSounds;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Oblivion (ultimate, phases 9-10): the Sovereign draws the whole arena's darkness into itself for five seconds while
 * three circles of light open around it. When it releases, everything outside those circles is struck with the void
 * itself. Reach a circle in time.
 */
public final class OblivionAttack extends SovereignAction {
	public OblivionAttack() {
		super("oblivion", true, SovereignConfig.OBLIVION_BLAST_TICK, 1, SovereignConfig.OBLIVION_TOTAL_TICKS, SovereignConfig.OBLIVION_COOLDOWN,
				SovereignConfig.OBLIVION_WEIGHT);
	}

	@Override
	public boolean canUse(ServerLevel level, VoidSovereignEntity boss, LivingEntity target, double gap) {
		return gap <= SovereignConfig.OBLIVION_RANGE * 0.6;
	}

	@Override
	public void start(ServerLevel level, VoidSovereignEntity boss, @Nullable LivingEntity target, ActionContext ctx) {
		boss.playSound(ModSounds.VOID_SOVEREIGN_OBLIVION, 6.0F, 0.6F);
		boss.announce(level, Component.translatable("message.boss_mode.sovereign.oblivion"));
	}

	/** Whether {@code victim} stands in one of the safe circles. */
	private static boolean safe(ActionContext ctx, LivingEntity victim) {
		for (Vec3 c : ctx.points) {
			double dx = victim.getX() - c.x, dz = victim.getZ() - c.z;
			if (dx * dx + dz * dz <= SovereignConfig.OBLIVION_SAFE_RADIUS * SovereignConfig.OBLIVION_SAFE_RADIUS && Math.abs(victim.getY() - c.y) < 4.0) {
				return true;
			}
		}
		return false;
	}

	@Override
	public Result tick(ServerLevel level, VoidSovereignEntity boss, @Nullable LivingEntity target, ActionContext ctx, int tick) {
		if (tick == SovereignConfig.OBLIVION_MARK_TICK) {
			double start = boss.getRandom().nextDouble() * Math.PI * 2;
			double distance = boss.getBbWidth() * 0.5 + SovereignConfig.OBLIVION_SAFE_DISTANCE;
			for (int i = 0; i < 3; i++) {
				double a = start + i * Math.PI * 2 / 3;
				ctx.points.add(ground(level, boss.position().add(Math.cos(a) * distance, 0, Math.sin(a) * distance)));
			}
		}
		if (tick < prepareTicks()) {
			if (tick % 2 == 0) {
				// the arena's darkness streams into the Sovereign
				for (int i = 0; i < 16; i++) {
					double a = boss.getRandom().nextDouble() * Math.PI * 2;
					double r = 8.0 + boss.getRandom().nextDouble() * 24.0;
					level.sendParticles(ParticleTypes.SQUID_INK, boss.getX() + Math.cos(a) * r, boss.getY() + 0.5 + boss.getRandom().nextDouble() * 4,
							boss.getZ() + Math.sin(a) * r, 0, -Math.cos(a), 0.05, -Math.sin(a), 0.9);
				}
				level.sendParticles(DARK, boss.getX(), boss.getY(0.6), boss.getZ(), 10, boss.getBbWidth() * 0.4, boss.getBbHeight() * 0.3,
						boss.getBbWidth() * 0.4, 0);
				for (Vec3 c : ctx.points) {
					ring(level, c, SovereignConfig.OBLIVION_SAFE_RADIUS, ParticleTypes.END_ROD, 20);
					level.sendParticles(ParticleTypes.END_ROD, c.x, c.y + 2.0, c.z, 3, 0.2, 2.0, 0.2, 0.01);
				}
			}
			if (tick % 20 == 0) {
				boss.playSound(ModSounds.VOID_SOVEREIGN_OBLIVION, 4.0F, 0.6F + tick / (float) prepareTicks() * 0.6F);
			}
			return Result.CONTINUE;
		}
		if (tick == prepareTicks()) {
			boss.playSound(ModSounds.VOID_SOVEREIGN_HOLE_COLLAPSE, 7.0F, 0.5F);
			level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, boss.getX(), boss.getY(0.5), boss.getZ(), 3, 3.0, 2.0, 3.0, 0);
			for (int i = 0; i < 96; i++) {
				double a = Math.PI * 2 * i / 96;
				level.sendParticles(ParticleTypes.SQUID_INK, boss.getX(), boss.getY() + 1.0, boss.getZ(), 0, Math.cos(a), 0.02, Math.sin(a), 2.5);
			}
			float damage = scaled(boss, SovereignConfig.OBLIVION_DAMAGE);
			for (LivingEntity victim : CombatUtil.targetsInRadius(level, boss, boss.position(), SovereignConfig.OBLIVION_RANGE)) {
				if (safe(ctx, victim)) {
					level.sendParticles(ParticleTypes.END_ROD, victim.getX(), victim.getY(0.5), victim.getZ(), 10, 0.4, 0.6, 0.4, 0.05);
					continue;
				}
				if (victim.hurtServer(level, boss.damageSources().indirectMagic(boss, boss), damage)) {
					victim.addEffect(new MobEffectInstance(MobEffects.WITHER, 100, 1), boss);
					CombatUtil.knockbackFrom(victim, boss.position(), 1.2, 0.5);
				}
			}
		}
		return Result.CONTINUE;
	}
}
