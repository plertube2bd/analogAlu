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

/**
 * Void Grasp: tentacles seize the target and drag it toward the Sovereign for two seconds (hurting as they go), then
 * a claw slam lands in front of it. Break line of sight or outrun the pull to slip free.
 */
public final class VoidGraspAttack extends SovereignAction {
	public VoidGraspAttack() {
		super("void_grasp", true, SovereignConfig.GRASP_SEIZE_TICK, SovereignConfig.GRASP_SLAM_TICK - SovereignConfig.GRASP_SEIZE_TICK + 1,
				SovereignConfig.GRASP_TOTAL_TICKS, SovereignConfig.GRASP_COOLDOWN, SovereignConfig.GRASP_WEIGHT);
	}

	@Override
	public boolean canUse(ServerLevel level, VoidSovereignEntity boss, LivingEntity target, double gap) {
		double distance = boss.distanceTo(target);
		return distance >= 5.0 && distance <= SovereignConfig.GRASP_RANGE * boss.sizeFactor() && boss.getSensing().hasLineOfSight(target);
	}

	@Override
	public Result tick(ServerLevel level, VoidSovereignEntity boss, @Nullable LivingEntity target, ActionContext ctx, int tick) {
		if (target != null && tick < SovereignConfig.GRASP_SLAM_TICK) {
			boss.faceTarget(target, 12.0F);
		}
		if (tick < prepareTicks()) {
			return Result.CONTINUE;
		}
		double sf = boss.sizeFactor();
		if (tick == prepareTicks()) {
			boolean seized = target != null && target.isAlive() && boss.distanceTo(target) <= SovereignConfig.GRASP_RANGE * sf * 1.2
					&& boss.getSensing().hasLineOfSight(target);
			ctx.travelled = seized ? 1 : 0;
			boss.playSound(ModSounds.VOID_SOVEREIGN_GRASP, 4.0F, seized ? 0.7F : 1.1F);
		}
		if (tick < SovereignConfig.GRASP_RELEASE_TICK && ctx.travelled > 0 && target != null && target.isAlive()) {
			Vec3 chest = boss.chestPosition();
			Vec3 to = target.position().add(0, target.getBbHeight() * 0.5, 0);
			if (boss.distanceTo(target) > SovereignConfig.GRASP_RANGE * sf * 1.4) {
				ctx.travelled = 0; // slipped free
			} else {
				CombatUtil.pull(target, boss.frontPoint(1.0), SovereignConfig.GRASP_PULL);
				Vec3 span = to.subtract(chest);
				for (int i = 0; i < 10; i++) {
					Vec3 p = chest.add(span.scale((i + level.getRandom().nextDouble()) / 10.0));
					level.sendParticles(i % 2 == 0 ? ParticleTypes.SQUID_INK : VoidSovereignEntity.VOID_DUST, p.x, p.y, p.z, 1, 0.08, 0.08, 0.08, 0);
				}
				if ((tick - prepareTicks()) % 10 == 0) {
					target.hurtServer(level, boss.damageSources().mobAttack(boss), scaled(boss, SovereignConfig.GRASP_DAMAGE_PER_SECOND * 0.5));
				}
			}
		}
		if (tick == SovereignConfig.GRASP_SLAM_TICK) {
			Vec3 front = boss.frontPoint(2.0);
			boss.playSound(ModSounds.VOID_SOVEREIGN_POUND, 4.5F, 0.9F);
			level.sendParticles(ParticleTypes.EXPLOSION, front.x, front.y + 0.5, front.z, 2, 0.8, 0.3, 0.8, 0);
			float damage = scaled(boss, SovereignConfig.GRASP_SLAM_DAMAGE);
			for (LivingEntity victim : CombatUtil.targetsInRadius(level, boss, front, 4.5 * sf)) {
				if (ctx.markHit(victim.getUUID()) && victim.hurtServer(level, boss.damageSources().mobAttack(boss), damage)) {
					CombatUtil.knockbackFrom(victim, front, 1.6, 0.6);
				}
			}
		}
		return Result.CONTINUE;
	}
}
