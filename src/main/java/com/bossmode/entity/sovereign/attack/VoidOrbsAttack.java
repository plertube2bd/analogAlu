package com.bossmode.entity.sovereign.attack;

import com.bossmode.config.SovereignConfig;
import com.bossmode.entity.action.ActionContext;
import com.bossmode.entity.projectile.VoidOrbProjectile;
import com.bossmode.entity.sovereign.VoidSovereignEntity;
import com.bossmode.registry.ModSounds;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/** Void Orbs: a fan of slow homing orbs (3, +1 every two phases). They turn slowly - sidestep late, or punch them back. */
public final class VoidOrbsAttack extends SovereignAction {
	public VoidOrbsAttack() {
		super("void_orbs", true, SovereignConfig.ORBS_HIT_TICK, 1, SovereignConfig.ORBS_TOTAL_TICKS, SovereignConfig.ORBS_COOLDOWN,
				SovereignConfig.ORBS_WEIGHT);
	}

	@Override
	public int weight(VoidSovereignEntity boss, LivingEntity target) {
		return boss.isTargetUnreachable() ? SovereignConfig.ORBS_WEIGHT * 3 : SovereignConfig.ORBS_WEIGHT;
	}

	@Override
	public boolean canUse(ServerLevel level, VoidSovereignEntity boss, LivingEntity target, double gap) {
		return boss.distanceTo(target) <= SovereignConfig.ORBS_MAX_RANGE && boss.getSensing().hasLineOfSight(target);
	}

	@Override
	public Result tick(ServerLevel level, VoidSovereignEntity boss, @Nullable LivingEntity target, ActionContext ctx, int tick) {
		Vec3 chest = boss.chestPosition();
		if (tick < prepareTicks()) {
			if (target != null) {
				boss.faceTarget(target, 20.0F);
			}
			level.sendParticles(ParticleTypes.REVERSE_PORTAL, chest.x, chest.y, chest.z, 6, 1.2, 1.2, 1.2, 0.05);
			return Result.CONTINUE;
		}
		if (tick == prepareTicks()) {
			boss.playSound(ModSounds.VOID_ORB_CAST, 4.0F, 0.6F);
			int count = SovereignConfig.ORBS_BASE_COUNT + boss.getPhase().number() / 2;
			Vec3 aim = target != null ? target.getEyePosition().subtract(chest).normalize() : boss.frontPoint(8).subtract(chest).normalize();
			double spread = Math.toRadians(70.0);
			for (int i = 0; i < count; i++) {
				double offset = count == 1 ? 0 : -spread / 2 + spread * i / (count - 1);
				double cos = Math.cos(offset), sin = Math.sin(offset);
				Vec3 dir = new Vec3(aim.x * cos - aim.z * sin, aim.y + 0.25, aim.x * sin + aim.z * cos).normalize();
				level.addFreshEntity(new VoidOrbProjectile(level, boss, chest.add(dir.scale(2.0)), dir.scale(SovereignConfig.ORB_SPEED),
						scaled(boss, SovereignConfig.ORB_DAMAGE), target));
			}
			level.sendParticles(VoidSovereignEntity.VOID_DUST, chest.x, chest.y, chest.z, 30, 1.0, 1.0, 1.0, 0);
		}
		return Result.CONTINUE;
	}
}
