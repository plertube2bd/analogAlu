package com.bossmode.entity.bastion.attack;

import com.bossmode.config.BastionConfig;
import com.bossmode.entity.action.ActionContext;
import com.bossmode.entity.bastion.BastionBehemothEntity;
import com.bossmode.entity.boss.combat.CombatUtil;
import com.bossmode.registry.ModSounds;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/** Normal attack - Maul: a forefoot crashes down on whatever stands in front of the Behemoth. */
public final class MaulAttack extends BastionAction {
	public MaulAttack() {
		super("maul", false, BastionConfig.MAUL_HIT_TICK, 1, BastionConfig.MAUL_TOTAL_TICKS, BastionConfig.MAUL_COOLDOWN, 10);
	}

	@Override
	public boolean canUse(ServerLevel level, BastionBehemothEntity boss, LivingEntity target, double gap) {
		return gap <= BastionConfig.MAUL_RANGE * boss.sizeFactor() && CombatUtil.isInFront(boss, target, 200.0);
	}

	@Override
	public Result tick(ServerLevel level, BastionBehemothEntity boss, @Nullable LivingEntity target, ActionContext ctx, int tick) {
		if (tick < prepareTicks()) {
			if (target != null) {
				boss.faceTarget(target, 12.0F);
			}
			return Result.CONTINUE;
		}
		if (tick == prepareTicks()) {
			boss.playSound(ModSounds.BASTION_BEHEMOTH_MAUL, 4.0F, 0.8F + boss.getRandom().nextFloat() * 0.2F);
			Vec3 front = ground(level, boss.frontPoint(BastionConfig.MAUL_RANGE * boss.sizeFactor() * 0.5));
			level.sendParticles(debris(level, front), front.x, front.y + 0.3, front.z, 40, 1.5, 0.2, 1.5, 0.25);
			level.sendParticles(ParticleTypes.EXPLOSION, front.x, front.y + 0.5, front.z, 2, 0.8, 0.2, 0.8, 0);
			double reach = boss.getBbWidth() / 2.0 + BastionConfig.MAUL_RANGE * boss.sizeFactor();
			float damage = scaled(boss, boss.getAttributeValue(Attributes.ATTACK_DAMAGE));
			Vec3 forward = CombatUtil.horizontalForward(boss);
			for (LivingEntity victim : CombatUtil.targetsInRadius(level, boss, boss.position().add(0, 1.5, 0), reach)) {
				if (CombatUtil.isInFront(boss, victim, BastionConfig.MAUL_CONE_DEGREES) && ctx.markHit(victim.getUUID())
						&& victim.hurtServer(level, boss.damageSources().mobAttack(boss), damage)) {
					CombatUtil.knockback(victim, forward.x, forward.z, BastionConfig.MAUL_KNOCKBACK, 0.4);
				}
			}
		}
		return Result.CONTINUE;
	}
}
