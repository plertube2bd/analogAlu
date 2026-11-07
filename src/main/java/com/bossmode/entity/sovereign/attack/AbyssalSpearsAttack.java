package com.bossmode.entity.sovereign.attack;

import com.bossmode.config.SovereignConfig;
import com.bossmode.entity.action.ActionContext;
import com.bossmode.entity.boss.combat.CombatUtil;
import com.bossmode.entity.sovereign.VoidSovereignEntity;
import com.bossmode.registry.ModSounds;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Abyssal Spears (phase 7+): five volleys of spears drop out of the dark sky onto where everyone is heading - each one
 * marked on the ground and visibly falling for 0.7 s. Keep moving and change direction.
 */
public final class AbyssalSpearsAttack extends SovereignAction {
	public AbyssalSpearsAttack() {
		super("abyssal_spears", true, SovereignConfig.SPEARS_START_TICK,
				SovereignConfig.SPEARS_INTERVAL * (SovereignConfig.SPEARS_VOLLEYS - 1) + SovereignConfig.SPEARS_FALL_TICKS + 1,
				SovereignConfig.SPEARS_TOTAL_TICKS, SovereignConfig.SPEARS_COOLDOWN, SovereignConfig.SPEARS_WEIGHT);
	}

	@Override
	public boolean canUse(ServerLevel level, VoidSovereignEntity boss, LivingEntity target, double gap) {
		return gap <= SovereignConfig.SPEARS_RANGE;
	}

	@Override
	public Result tick(ServerLevel level, VoidSovereignEntity boss, @Nullable LivingEntity target, ActionContext ctx, int tick) {
		if (target != null && tick < prepareTicks()) {
			boss.faceTarget(target, 10.0F);
		}
		int since = tick - SovereignConfig.SPEARS_START_TICK;
		if (since >= 0 && since % SovereignConfig.SPEARS_INTERVAL == 0 && since / SovereignConfig.SPEARS_INTERVAL < SovereignConfig.SPEARS_VOLLEYS) {
			boss.playSound(ModSounds.VOID_ORB_CAST, 3.0F, 0.6F);
			for (LivingEntity victim : CombatUtil.targetsInRadius(level, boss, boss.position(), SovereignConfig.SPEARS_RANGE)) {
				Vec3 lead = victim.position().add(victim.getDeltaMovement().multiply(1, 0, 1).scale(SovereignConfig.SPEARS_FALL_TICKS * 0.6));
				ctx.scheduled.add(new ActionContext.Scheduled(tick + SovereignConfig.SPEARS_FALL_TICKS, ground(level, lead)));
			}
		}
		double radius = SovereignConfig.SPEARS_RADIUS;
		for (ActionContext.Scheduled spear : ctx.scheduled) {
			Vec3 p = spear.position();
			int left = spear.tick() - tick;
			if (left > 0 && left <= SovereignConfig.SPEARS_FALL_TICKS) {
				if (tick % 2 == 0) {
					ring(level, p, radius, MARK, 12);
				}
				double y = p.y + 1.0 + 14.0 * left / SovereignConfig.SPEARS_FALL_TICKS;
				level.sendParticles(DARK, p.x, y, p.z, 2, 0.05, 0.6, 0.05, 0);
				level.sendParticles(ParticleTypes.REVERSE_PORTAL, p.x, y + 1.0, p.z, 1, 0.05, 0.3, 0.05, 0.02);
			} else if (left == 0) {
				level.sendParticles(ParticleTypes.SQUID_INK, p.x, p.y + 0.4, p.z, 12, radius * 0.4, 0.2, radius * 0.4, 0.08);
				level.sendParticles(ParticleTypes.CRIT, p.x, p.y + 0.6, p.z, 10, radius * 0.4, 0.3, radius * 0.4, 0.2);
				level.playSound(null, p.x, p.y, p.z, ModSounds.VOID_SOVEREIGN_METEOR, SoundSource.HOSTILE, 1.2F, 1.4F);
				float damage = scaled(boss, SovereignConfig.SPEARS_DAMAGE);
				for (LivingEntity victim : CombatUtil.targetsInRadius(level, boss, p.add(0, 1.0, 0), radius)) {
					if (victim.hurtServer(level, boss.damageSources().mobAttack(boss), damage)) {
						victim.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 30, 1), boss);
					}
				}
			}
		}
		return Result.CONTINUE;
	}
}
