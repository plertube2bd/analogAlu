package com.bossmode.entity.bastion.attack;

import com.bossmode.config.BastionConfig;
import com.bossmode.entity.action.ActionContext;
import com.bossmode.entity.bastion.BastionBehemothEntity;
import com.bossmode.entity.projectile.BoulderProjectile;
import com.bossmode.registry.ModSounds;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Boulder Throw: the Behemoth rips a molten boulder out of the ground and lobs it at its target (a second one lands
 * nearby from phase 5). The landing spot glows on the ground - leave it.
 */
public final class BoulderThrowAttack extends BastionAction {
	public BoulderThrowAttack() {
		super("boulder_throw", true, BastionConfig.BOULDER_HIT_TICK, 1, BastionConfig.BOULDER_TOTAL_TICKS, BastionConfig.BOULDER_COOLDOWN,
				BastionConfig.BOULDER_WEIGHT);
	}

	@Override
	public boolean canUse(ServerLevel level, BastionBehemothEntity boss, LivingEntity target, double gap) {
		return gap >= BastionConfig.BOULDER_MIN_RANGE && gap <= BastionConfig.BOULDER_MAX_RANGE
				|| boss.isTargetUnreachable() && gap <= BastionConfig.BOULDER_MAX_RANGE;
	}

	@Override
	public Result tick(ServerLevel level, BastionBehemothEntity boss, @Nullable LivingEntity target, ActionContext ctx, int tick) {
		if (tick < prepareTicks()) {
			if (target != null) {
				boss.faceTarget(target, 10.0F);
			}
			if (tick == 10) {
				Vec3 front = ground(level, boss.frontPoint(1.0));
				level.sendParticles(debris(level, front), front.x, front.y + 0.3, front.z, 30, 1.0, 0.3, 1.0, 0.2);
				level.sendParticles(ParticleTypes.LAVA, front.x, front.y + 0.5, front.z, 6, 0.8, 0.2, 0.8, 0);
			}
			return Result.CONTINUE;
		}
		if (tick == prepareTicks() && target != null) {
			boss.playSound(ModSounds.BASTION_BEHEMOTH_THROW, 4.0F, 0.8F);
			Vec3 from = boss.headPosition().add(0, 1.0, 0);
			int count = boss.getPhase().number() >= 5 ? 2 : 1;
			for (int i = 0; i < count; i++) {
				Vec3 aim = target.position().add(target.getDeltaMovement().scale(10.0));
				if (i > 0) {
					double a = boss.getRandom().nextDouble() * Math.PI * 2;
					aim = aim.add(Math.cos(a) * 4.0, 0, Math.sin(a) * 4.0);
				}
				aim = ground(level, aim);
				int ticks = Mth.clamp((int) (from.distanceTo(aim) / 0.9), 14, 44);
				level.addFreshEntity(new BoulderProjectile(level, boss, from, BoulderProjectile.ballisticVelocity(from, aim, ticks),
						scaled(boss, BastionConfig.BOULDER_DAMAGE)));
				ctx.points.add(aim);
			}
		}
		if (tick > prepareTicks() && tick % 3 == 0) {
			for (Vec3 p : ctx.points) {
				ring(level, p, BastionConfig.BOULDER_RADIUS, BastionBehemothEntity.EMBER_DUST, 16);
			}
		}
		return Result.CONTINUE;
	}
}
