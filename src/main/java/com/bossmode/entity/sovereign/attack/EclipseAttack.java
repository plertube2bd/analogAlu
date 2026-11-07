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

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Eclipse (ultimate, phases 5-6): the Sovereign raises its arms, darkness falls on everyone near, and void meteors
 * rain on the arena for five seconds - each one marked on the ground {@link SovereignConfig#ECLIPSE_FALL_TICKS}
 * ticks before it lands, most of them aimed at the players. Keep moving.
 */
public final class EclipseAttack extends SovereignAction {
	public EclipseAttack() {
		super("eclipse", true, SovereignConfig.ECLIPSE_START_TICK, SovereignConfig.ECLIPSE_END_TICK - SovereignConfig.ECLIPSE_START_TICK + 1,
				SovereignConfig.ECLIPSE_TOTAL_TICKS, SovereignConfig.ECLIPSE_COOLDOWN, SovereignConfig.ECLIPSE_WEIGHT);
	}

	@Override
	public boolean canUse(ServerLevel level, VoidSovereignEntity boss, LivingEntity target, double gap) {
		return boss.distanceTo(target) <= SovereignConfig.ECLIPSE_RANGE;
	}

	@Override
	public void start(ServerLevel level, VoidSovereignEntity boss, @Nullable LivingEntity target, ActionContext ctx) {
		boss.playSound(ModSounds.VOID_SOVEREIGN_ECLIPSE, 7.0F, 0.6F);
		boss.announce(level, Component.translatable("message.boss_mode.sovereign.eclipse"));
		for (LivingEntity victim : CombatUtil.targetsInRadius(level, boss, boss.position(), SovereignConfig.ECLIPSE_RANGE + 12)) {
			victim.addEffect(new MobEffectInstance(MobEffects.DARKNESS, SovereignConfig.ECLIPSE_TOTAL_TICKS, 0), boss);
		}
	}

	@Override
	public Result tick(ServerLevel level, VoidSovereignEntity boss, @Nullable LivingEntity target, ActionContext ctx, int tick) {
		if (tick % 4 == 0) {
			Vec3 above = boss.position().add(0, boss.getBbHeight() + 2.0, 0);
			level.sendParticles(ParticleTypes.SQUID_INK, above.x, above.y, above.z, 8, 1.5, 0.6, 1.5, 0.02);
			level.sendParticles(VoidSovereignEntity.VOID_DUST, above.x, above.y, above.z, 6, 2.0, 0.8, 2.0, 0);
		}
		int lastMark = SovereignConfig.ECLIPSE_END_TICK - SovereignConfig.ECLIPSE_FALL_TICKS;
		int interval = Math.max(1, (lastMark - prepareTicks()) / SovereignConfig.ECLIPSE_METEORS);
		if (tick >= prepareTicks() && tick <= lastMark && (tick - prepareTicks()) % interval == 0) {
			List<LivingEntity> victims = CombatUtil.targetsInRadius(level, boss, boss.position(), SovereignConfig.ECLIPSE_RANGE);
			Vec3 around;
			if (!victims.isEmpty() && boss.getRandom().nextInt(3) > 0) {
				LivingEntity victim = victims.get(boss.getRandom().nextInt(victims.size()));
				Vec3 lead = victim.getDeltaMovement().multiply(SovereignConfig.ECLIPSE_FALL_TICKS * 0.6, 0, SovereignConfig.ECLIPSE_FALL_TICKS * 0.6);
				around = victim.position().add(lead).add((boss.getRandom().nextDouble() - 0.5) * 3, 0, (boss.getRandom().nextDouble() - 0.5) * 3);
			} else {
				double r = SovereignConfig.ECLIPSE_RANGE * 0.8;
				around = boss.position().add((boss.getRandom().nextDouble() - 0.5) * 2 * r, 0, (boss.getRandom().nextDouble() - 0.5) * 2 * r);
			}
			ctx.scheduled.add(new ActionContext.Scheduled(tick + SovereignConfig.ECLIPSE_FALL_TICKS, ground(level, around)));
		}
		Iterator<ActionContext.Scheduled> it = ctx.scheduled.iterator();
		while (it.hasNext()) {
			ActionContext.Scheduled meteor = it.next();
			Vec3 p = meteor.position();
			int left = meteor.tick() - tick;
			if (left > 0) {
				if (tick % 3 == 0) {
					ring(level, p, SovereignConfig.ECLIPSE_RADIUS, MARK, 16);
				}
				double height = left * 1.2;
				level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, p.x, p.y + height, p.z, 3, 0.3, 0.3, 0.3, 0.02);
				level.sendParticles(ParticleTypes.LARGE_SMOKE, p.x, p.y + height + 1.0, p.z, 2, 0.3, 0.5, 0.3, 0.01);
				continue;
			}
			impact(level, boss, p);
			it.remove();
		}
		return Result.CONTINUE;
	}

	private void impact(ServerLevel level, VoidSovereignEntity boss, Vec3 p) {
		level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, p.x, p.y + 0.5, p.z, 1, 0, 0, 0, 0);
		level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, p.x, p.y + 0.5, p.z, 30, 1.2, 0.4, 1.2, 0.15);
		level.sendParticles(ParticleTypes.SQUID_INK, p.x, p.y + 0.5, p.z, 20, 1.2, 0.4, 1.2, 0.05);
		level.playSound(null, p.x, p.y, p.z, ModSounds.VOID_SOVEREIGN_METEOR, boss.getSoundSource(), 3.0F, 0.7F + level.getRandom().nextFloat() * 0.3F);
		float damage = scaled(boss, SovereignConfig.ECLIPSE_DAMAGE);
		Set<UUID> hit = new HashSet<>();
		for (LivingEntity victim : CombatUtil.targetsInRadius(level, boss, p.add(0, 0.8, 0), SovereignConfig.ECLIPSE_RADIUS)) {
			if (hit.add(victim.getUUID()) && victim.hurtServer(level, boss.damageSources().indirectMagic(boss, boss), damage)) {
				CombatUtil.knockbackFrom(victim, p, 1.0, 0.6);
			}
		}
	}

	@Override
	public void stop(ServerLevel level, VoidSovereignEntity boss, ActionContext ctx, boolean interrupted) {
		ctx.scheduled.clear();
	}
}
