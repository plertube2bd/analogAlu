package com.bossmode.entity.tempest.attack;

import com.bossmode.config.TempestConfig;
import com.bossmode.entity.action.ActionContext;
import com.bossmode.entity.boss.combat.CombatUtil;
import com.bossmode.entity.projectile.WyrmShardProjectile;
import com.bossmode.entity.tempest.TempestWyvernEntity;
import com.bossmode.registry.ModItems;
import com.bossmode.registry.ModSounds;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Feather Barrage: the wings snap forward and loose a fan of razor storm feathers at the target (more every phase).
 * Like the Shard Dragon's shards, a feather can be punched back at the Wyvern.
 */
public final class FeatherBarrageAttack extends TempestAction {
	public FeatherBarrageAttack() {
		super("feather_barrage", true, TempestConfig.FEATHER_HIT_TICK, 1, TempestConfig.FEATHER_TOTAL_TICKS, TempestConfig.FEATHER_COOLDOWN,
				TempestConfig.FEATHER_WEIGHT);
	}

	@Override
	public boolean canUse(ServerLevel level, TempestWyvernEntity boss, LivingEntity target, double gap) {
		return gap <= TempestConfig.FEATHER_RANGE && boss.getSensing().hasLineOfSight(target);
	}

	@Override
	public Result tick(ServerLevel level, TempestWyvernEntity boss, @Nullable LivingEntity target, ActionContext ctx, int tick) {
		if (tick < prepareTicks()) {
			if (target != null) {
				boss.faceTarget(target, 20.0F);
			}
			return Result.CONTINUE;
		}
		if (tick == prepareTicks()) {
			boss.playSound(ModSounds.TEMPEST_WYVERN_FEATHERS, 4.0F, 1.0F);
			// just in front of the hitbox, at chest height
			Vec3 from = boss.position().add(CombatUtil.horizontalForward(boss).scale(boss.getBbWidth() * 0.5 + 0.4)).add(0, boss.getBbHeight() * 0.65, 0);
			Vec3 aim = target != null ? target.position().add(0, target.getBbHeight() * 0.5, 0).add(target.getDeltaMovement().scale(6.0)).subtract(from)
					: CombatUtil.horizontalForward(boss).add(0, boss.isFlying() ? -0.4 : 0.0, 0); // no target: straight ahead
			int count = TempestConfig.FEATHER_BASE + (boss.getPhase().number() - 1) + (boss.isEnraged() ? 2 : 0);
			double spread = Math.toRadians(TempestConfig.FEATHER_SPREAD_DEGREES);
			float damage = scaled(boss, TempestConfig.FEATHER_DAMAGE);
			for (int i = 0; i < count; i++) {
				double yaw = count == 1 ? 0.0 : -spread / 2.0 + spread * i / (count - 1);
				Vec3 dir = aim.normalize().yRot((float) yaw);
				WyrmShardProjectile feather = new WyrmShardProjectile(level, boss, from, dir.scale(TempestConfig.FEATHER_SPEED), damage);
				feather.setItem(new ItemStack(ModItems.STORM_FEATHER));
				level.addFreshEntity(feather);
			}
			level.sendParticles(ParticleTypes.CLOUD, from.x, from.y, from.z, 16, 0.8, 0.5, 0.8, 0.1);
		}
		return Result.CONTINUE;
	}
}
