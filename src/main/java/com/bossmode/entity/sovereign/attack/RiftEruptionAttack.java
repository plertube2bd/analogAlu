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

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Rift Eruption: the Sovereign drives its claws into the ground; a rift opens under every player nearby (and at random
 * spots) and one second later bursts into void tendrils that fling whoever still stands on it into the air.
 */
public final class RiftEruptionAttack extends SovereignAction {
	public RiftEruptionAttack() {
		super("rift_eruption", true, SovereignConfig.RIFT_MARK_TICK, SovereignConfig.RIFT_ERUPT_TICK - SovereignConfig.RIFT_MARK_TICK + 1,
				SovereignConfig.RIFT_TOTAL_TICKS, SovereignConfig.RIFT_COOLDOWN, SovereignConfig.RIFT_WEIGHT);
	}

	@Override
	public boolean canUse(ServerLevel level, VoidSovereignEntity boss, LivingEntity target, double gap) {
		return boss.distanceTo(target) <= SovereignConfig.RIFT_RANGE;
	}

	@Override
	public Result tick(ServerLevel level, VoidSovereignEntity boss, @Nullable LivingEntity target, ActionContext ctx, int tick) {
		if (tick < prepareTicks()) {
			if (target != null) {
				boss.faceTarget(target, 15.0F);
			}
			return Result.CONTINUE;
		}
		if (tick == prepareTicks()) {
			boss.playSound(ModSounds.VOID_SOVEREIGN_RIFT, 5.0F, 0.6F);
			for (LivingEntity victim : CombatUtil.targetsInRadius(level, boss, boss.position(), SovereignConfig.RIFT_RANGE)) {
				ctx.points.add(ground(level, victim.position()));
			}
			double spread = 14.0 * boss.sizeFactor();
			for (int i = 0; i < SovereignConfig.RIFT_EXTRA; i++) {
				ctx.points.add(ground(level, boss.position().add((boss.getRandom().nextDouble() - 0.5) * 2 * spread, 0,
						(boss.getRandom().nextDouble() - 0.5) * 2 * spread)));
			}
		}
		if (tick < SovereignConfig.RIFT_ERUPT_TICK) {
			if (tick % 3 == 0) {
				for (Vec3 p : ctx.points) {
					ring(level, p, SovereignConfig.RIFT_RADIUS, MARK, 14);
					level.sendParticles(ParticleTypes.SQUID_INK, p.x, p.y + 0.2, p.z, 2, SovereignConfig.RIFT_RADIUS * 0.4, 0.05, SovereignConfig.RIFT_RADIUS * 0.4, 0.01);
				}
			}
			return Result.CONTINUE;
		}
		if (tick == SovereignConfig.RIFT_ERUPT_TICK) {
			float damage = scaled(boss, SovereignConfig.RIFT_DAMAGE);
			Set<UUID> hit = new HashSet<>();
			for (Vec3 p : ctx.points) {
				level.sendParticles(ParticleTypes.SQUID_INK, p.x, p.y + 1.5, p.z, 30, 0.6, 1.5, 0.6, 0.05);
				level.sendParticles(VoidSovereignEntity.VOID_DUST, p.x, p.y + 2.0, p.z, 24, 0.5, 2.0, 0.5, 0);
				level.sendParticles(ParticleTypes.SCULK_SOUL, p.x, p.y + 1.0, p.z, 10, 0.5, 1.0, 0.5, 0.08);
				level.playSound(null, p.x, p.y, p.z, ModSounds.VOID_SOVEREIGN_RIFT, boss.getSoundSource(), 2.5F, 1.1F);
				for (LivingEntity victim : CombatUtil.targetsInRadius(level, boss, p.add(0, 0.8, 0), SovereignConfig.RIFT_RADIUS)) {
					if (hit.add(victim.getUUID()) && victim.hurtServer(level, boss.damageSources().mobAttack(boss), damage)) {
						CombatUtil.knockback(victim, 0, 0, 0, SovereignConfig.RIFT_LAUNCH);
					}
				}
			}
		}
		return Result.CONTINUE;
	}
}
