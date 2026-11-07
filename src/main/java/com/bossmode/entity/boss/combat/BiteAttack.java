package com.bossmode.entity.boss.combat;

import com.bossmode.config.BossConfig;
import com.bossmode.entity.action.ActionContext;
import com.bossmode.entity.action.BossAction;
import com.bossmode.entity.boss.ShardDragonEntity;
import com.bossmode.registry.ModSounds;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/** Normal attack - Shard Bite: wind-up, one server-side hit test on the bite keyframe (frontal cone), recovery. */
public final class BiteAttack extends BossAction<ShardDragonEntity> {
	public BiteAttack() {
		super("bite", false, BossConfig.BITE_HIT_TICK, 1, BossConfig.BITE_TOTAL_TICKS);
	}

	@Override
	public int cooldown() {
		return BossConfig.BITE_COOLDOWN;
	}

	@Override
	public String animationTrigger() {
		return "attack";
	}

	@Override
	public boolean canUse(ServerLevel level, ShardDragonEntity boss, LivingEntity target, double gap) {
		return gap <= BossConfig.BITE_RANGE * boss.sizeFactor() && boss.getSensing().hasLineOfSight(target);
	}

	@Override
	public Result tick(ServerLevel level, ShardDragonEntity boss, @Nullable LivingEntity target, ActionContext ctx, int tick) {
		if (tick < prepareTicks()) {
			if (target != null) {
				boss.faceTarget(target, 30.0F);
			}
		} else if (tick == prepareTicks()) {
			Vec3 mouth = boss.mouthPosition();
			boss.playSound(ModSounds.SHARD_DRAGON_BITE, 3.0F, 0.9F + boss.getRandom().nextFloat() * 0.2F);
			level.sendParticles(ParticleTypes.CRIT, mouth.x, mouth.y, mouth.z, 16, 0.8, 0.5, 0.8, 0.2);
			double reach = boss.getBbWidth() / 2.0 + BossConfig.BITE_RANGE * boss.sizeFactor();
			float damage = (float) (boss.getAttributeValue(Attributes.ATTACK_DAMAGE) * boss.getDamageMultiplier());
			Vec3 forward = CombatUtil.horizontalForward(boss);
			for (LivingEntity victim : CombatUtil.targetsInRadius(level, boss, boss.position().add(0, 1.0, 0), reach)) {
				if (CombatUtil.isInFront(boss, victim, BossConfig.BITE_CONE_DEGREES) && ctx.markHit(victim.getUUID())
						&& victim.hurtServer(level, boss.damageSources().mobAttack(boss), damage)) {
					CombatUtil.knockback(victim, forward.x, forward.z, BossConfig.BITE_KNOCKBACK, 0.3);
				}
			}
		}
		return Result.CONTINUE;
	}
}
