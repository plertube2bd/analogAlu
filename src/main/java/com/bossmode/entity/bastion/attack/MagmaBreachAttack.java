package com.bossmode.entity.bastion.attack;

import com.bossmode.config.BastionConfig;
import com.bossmode.entity.action.ActionContext;
import com.bossmode.entity.bastion.BastionBehemothEntity;
import com.bossmode.entity.boss.combat.CombatUtil;
import com.bossmode.registry.ModSounds;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Magma Breach: the Behemoth drives its head into the ground; glowing fissures open under everyone nearby (and a few
 * more around its target), and a second later they erupt in fountains of magma. Get off the glowing circles.
 */
public final class MagmaBreachAttack extends BastionAction {
	public MagmaBreachAttack() {
		super("magma_breach", true, BastionConfig.BREACH_ERUPT_TICK, 1, BastionConfig.BREACH_TOTAL_TICKS, BastionConfig.BREACH_COOLDOWN,
				BastionConfig.BREACH_WEIGHT);
	}

	@Override
	public boolean canUse(ServerLevel level, BastionBehemothEntity boss, LivingEntity target, double gap) {
		return gap <= BastionConfig.BREACH_RANGE;
	}

	@Override
	public Result tick(ServerLevel level, BastionBehemothEntity boss, @Nullable LivingEntity target, ActionContext ctx, int tick) {
		double radius = BastionConfig.BREACH_RADIUS;
		if (tick < BastionConfig.BREACH_MARK_TICK) {
			if (target != null) {
				boss.faceTarget(target, 8.0F);
			}
			return Result.CONTINUE;
		}
		if (tick == BastionConfig.BREACH_MARK_TICK) {
			boss.playSound(ModSounds.BASTION_BEHEMOTH_QUAKE, 4.0F, 1.2F);
			for (LivingEntity victim : CombatUtil.targetsInRadius(level, boss, boss.position(), BastionConfig.BREACH_RANGE)) {
				if (victim instanceof Player || victim == target) {
					ctx.points.add(ground(level, victim.position()));
				}
			}
			Vec3 center = target != null ? target.position() : boss.position();
			int extra = BastionConfig.BREACH_EXTRA + (boss.isEnraged() ? 2 : 0);
			for (int i = 0; i < extra; i++) {
				double a = Math.PI * 2 * i / extra + boss.getRandom().nextDouble();
				double r = 4.0 + boss.getRandom().nextDouble() * 5.0;
				ctx.points.add(ground(level, center.add(Math.cos(a) * r, 0, Math.sin(a) * r)));
			}
		}
		if (tick < prepareTicks()) {
			if (tick % 2 == 0) {
				for (Vec3 p : ctx.points) {
					ring(level, p, radius, BastionBehemothEntity.EMBER_DUST, 16);
					level.sendParticles(ParticleTypes.SMOKE, p.x, p.y + 0.2, p.z, 3, radius * 0.4, 0.05, radius * 0.4, 0.01);
					if (tick % 6 == 0) {
						level.sendParticles(ParticleTypes.LAVA, p.x, p.y + 0.2, p.z, 1, radius * 0.3, 0.05, radius * 0.3, 0);
					}
				}
			}
			return Result.CONTINUE;
		}
		if (tick == prepareTicks()) {
			boss.playSound(ModSounds.BASTION_BEHEMOTH_MAGMA, 6.0F, 0.8F);
			float damage = scaled(boss, BastionConfig.BREACH_DAMAGE);
			for (Vec3 p : ctx.points) {
				level.sendParticles(ParticleTypes.LAVA, p.x, p.y + 0.5, p.z, 16, radius * 0.4, 0.5, radius * 0.4, 0);
				level.sendParticles(ParticleTypes.FLAME, p.x, p.y + 0.5, p.z, 0, 0, 1, 0, 0.5);
				level.sendParticles(ParticleTypes.FLAME, p.x, p.y + 1.0, p.z, 40, radius * 0.3, 1.5, radius * 0.3, 0.08);
				level.sendParticles(ParticleTypes.LARGE_SMOKE, p.x, p.y + 2.0, p.z, 10, radius * 0.3, 1.0, radius * 0.3, 0.05);
				for (LivingEntity victim : CombatUtil.targetsInRadius(level, boss, p.add(0, 1.0, 0), radius)) {
					if (ctx.markHit(victim.getUUID()) && victim.hurtServer(level, boss.damageSources().mobAttack(boss), damage)) {
						victim.igniteForSeconds(4.0F);
						victim.setDeltaMovement(victim.getDeltaMovement().x * 0.4, BastionConfig.BREACH_LAUNCH, victim.getDeltaMovement().z * 0.4);
						victim.needsSync = true;
					}
				}
			}
		}
		return Result.CONTINUE;
	}
}
