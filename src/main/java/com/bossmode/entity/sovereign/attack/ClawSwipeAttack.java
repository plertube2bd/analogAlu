package com.bossmode.entity.sovereign.attack;

import com.bossmode.config.SovereignConfig;
import com.bossmode.entity.action.ActionContext;
import com.bossmode.entity.boss.combat.CombatUtil;
import com.bossmode.entity.sovereign.VoidSovereignEntity;
import com.bossmode.registry.ModSounds;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/** Normal attack - Claw Swipe: both claws rake a wide arc in front of the Sovereign. */
public final class ClawSwipeAttack extends SovereignAction {
	public ClawSwipeAttack() {
		super("claw_swipe", false, SovereignConfig.CLAW_HIT_TICK, 1, SovereignConfig.CLAW_TOTAL_TICKS, SovereignConfig.CLAW_COOLDOWN, 10);
	}

	@Override
	public boolean canUse(ServerLevel level, VoidSovereignEntity boss, LivingEntity target, double gap) {
		return gap <= SovereignConfig.CLAW_RANGE * boss.sizeFactor() && boss.getSensing().hasLineOfSight(target);
	}

	@Override
	public Result tick(ServerLevel level, VoidSovereignEntity boss, @Nullable LivingEntity target, ActionContext ctx, int tick) {
		if (tick < prepareTicks()) {
			if (target != null) {
				boss.faceTarget(target, 25.0F);
			}
			return Result.CONTINUE;
		}
		if (tick == prepareTicks()) {
			boss.playSound(ModSounds.VOID_SOVEREIGN_CLAW, 4.0F, 0.8F + boss.getRandom().nextFloat() * 0.2F);
			Vec3 front = boss.frontPoint(SovereignConfig.CLAW_RANGE * boss.sizeFactor() * 0.5).add(0, boss.getBbHeight() * 0.35, 0);
			level.sendParticles(ParticleTypes.SWEEP_ATTACK, front.x, front.y, front.z, 5, 1.5, 0.6, 1.5, 0);
			level.sendParticles(VoidSovereignEntity.VOID_DUST, front.x, front.y, front.z, 16, 1.5, 0.8, 1.5, 0);
			double reach = boss.getBbWidth() / 2.0 + SovereignConfig.CLAW_RANGE * boss.sizeFactor();
			float damage = scaled(boss, boss.getAttributeValue(Attributes.ATTACK_DAMAGE));
			Vec3 forward = CombatUtil.horizontalForward(boss);
			for (LivingEntity victim : CombatUtil.targetsInRadius(level, boss, boss.position().add(0, 1.5, 0), reach)) {
				if (CombatUtil.isInFront(boss, victim, SovereignConfig.CLAW_CONE_DEGREES) && ctx.markHit(victim.getUUID())
						&& victim.hurtServer(level, boss.damageSources().mobAttack(boss), damage)) {
					CombatUtil.knockback(victim, forward.x, forward.z, SovereignConfig.CLAW_KNOCKBACK, 0.35);
				}
			}
		}
		return Result.CONTINUE;
	}
}
