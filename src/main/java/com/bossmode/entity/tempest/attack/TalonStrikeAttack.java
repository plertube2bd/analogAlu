package com.bossmode.entity.tempest.attack;

import com.bossmode.config.TempestConfig;
import com.bossmode.entity.action.ActionContext;
import com.bossmode.entity.boss.combat.CombatUtil;
import com.bossmode.entity.tempest.TempestWyvernEntity;
import com.bossmode.registry.ModSounds;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/** Normal attack (ground) - Talon Strike: both claws rake down in front of the Wyvern. */
public final class TalonStrikeAttack extends TempestAction {
	public TalonStrikeAttack() {
		super("talon_strike", false, TempestConfig.TALON_HIT_TICK, 1, TempestConfig.TALON_TOTAL_TICKS, TempestConfig.TALON_COOLDOWN, 10);
	}

	@Override
	public boolean canUse(ServerLevel level, TempestWyvernEntity boss, LivingEntity target, double gap) {
		return gap <= TempestConfig.TALON_RANGE * boss.sizeFactor() && boss.getSensing().hasLineOfSight(target);
	}

	@Override
	public Result tick(ServerLevel level, TempestWyvernEntity boss, @Nullable LivingEntity target, ActionContext ctx, int tick) {
		if (tick < prepareTicks()) {
			if (target != null) {
				boss.faceTarget(target, 25.0F);
			}
			return Result.CONTINUE;
		}
		if (tick == prepareTicks()) {
			boss.playSound(ModSounds.TEMPEST_WYVERN_STRIKE, 3.0F, 0.9F + boss.getRandom().nextFloat() * 0.2F);
			Vec3 front = boss.frontPoint(TempestConfig.TALON_RANGE * boss.sizeFactor() * 0.5).add(0, boss.getBbHeight() * 0.3, 0);
			level.sendParticles(ParticleTypes.SWEEP_ATTACK, front.x, front.y, front.z, 4, 1.2, 0.5, 1.2, 0);
			level.sendParticles(TempestWyvernEntity.STORM_DUST, front.x, front.y, front.z, 12, 1.2, 0.6, 1.2, 0);
			double reach = boss.getBbWidth() / 2.0 + TempestConfig.TALON_RANGE * boss.sizeFactor();
			float damage = scaled(boss, boss.getAttributeValue(Attributes.ATTACK_DAMAGE));
			Vec3 forward = CombatUtil.horizontalForward(boss);
			for (LivingEntity victim : CombatUtil.targetsInRadius(level, boss, boss.position().add(0, 1.2, 0), reach)) {
				if (CombatUtil.isInFront(boss, victim, TempestConfig.TALON_CONE_DEGREES) && ctx.markHit(victim.getUUID())
						&& victim.hurtServer(level, boss.damageSources().mobAttack(boss), damage)) {
					CombatUtil.knockback(victim, forward.x, forward.z, TempestConfig.TALON_KNOCKBACK, 0.3);
				}
			}
		}
		return Result.CONTINUE;
	}
}
