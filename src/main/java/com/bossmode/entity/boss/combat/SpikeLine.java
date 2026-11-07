package com.bossmode.entity.boss.combat;

import com.bossmode.entity.action.ActionContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * A line of crystal spikes bursting out of the ground and racing forward one step per tick (Spike Eruption,
 * Warlord Ground Spikes). Purely server-side particles + damage; the world is never modified.
 */
public final class SpikeLine {
	private static final BlockParticleOption CRYSTAL = new BlockParticleOption(ParticleTypes.BLOCK, Blocks.AMETHYST_CLUSTER.defaultBlockState());

	private SpikeLine() {
	}

	/**
	 * Erupts step {@code step} of the line starting at {@code origin} going along {@code dir}.
	 * @return false when the line hit a cliff / wall and should stop
	 */
	public static boolean erupt(ServerLevel level, Entity source, DamageSource damageSource, Vec3 origin, Vec3 dir, int step, double stepLength,
			double width, float damage, double knockUp, ActionContext ctx) {
		Vec3 flat = origin.add(dir.scale(step * stepLength));
		int groundY = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, BlockPos.containing(flat));
		if (Math.abs(groundY - origin.y) > 4.0 + step * 0.5) {
			return false;
		}
		Vec3 pos = new Vec3(flat.x, groundY, flat.z);
		level.sendParticles(CRYSTAL, pos.x, pos.y + 0.3, pos.z, 10, width * 0.3, 0.6, width * 0.3, 0.25);
		level.sendParticles(ParticleTypes.END_ROD, pos.x, pos.y + 1.0, pos.z, 3, width * 0.2, 0.8, width * 0.2, 0.02);
		if (step % 2 == 0) {
			level.playSound(null, pos.x, pos.y, pos.z, net.minecraft.sounds.SoundEvents.AMETHYST_CLUSTER_BREAK, source.getSoundSource(), 1.2F, 0.6F);
		}
		AABB box = new AABB(pos.x - width, pos.y - 0.5, pos.z - width, pos.x + width, pos.y + 2.5, pos.z + width);
		for (LivingEntity victim : CombatUtil.targetsInBox(level, source, box)) {
			if (ctx.markHit(victim.getUUID()) && victim.hurtServer(level, damageSource, damage)) {
				Vec3 current = victim.getDeltaMovement();
				victim.setDeltaMovement(current.x, Math.max(current.y, 0) + knockUp, current.z);
				victim.needsSync = true;
			}
		}
		return true;
	}
}
