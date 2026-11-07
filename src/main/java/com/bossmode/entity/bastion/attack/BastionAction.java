package com.bossmode.entity.bastion.attack;

import com.bossmode.entity.action.BossAction;
import com.bossmode.entity.bastion.BastionBehemothEntity;
import com.bossmode.entity.boss.combat.CombatUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/** Shared helpers of the Bastion Behemoth's patterns. */
abstract class BastionAction extends BossAction<BastionBehemothEntity> {
	private final int cooldown;
	private final int weight;

	BastionAction(String id, boolean special, int prepareTicks, int activeTicks, int totalTicks, int cooldown, int weight) {
		super(id, special, prepareTicks, activeTicks, totalTicks);
		this.cooldown = cooldown;
		this.weight = weight;
	}

	@Override
	public int cooldown() {
		return this.cooldown;
	}

	@Override
	public int weight(BastionBehemothEntity boss, LivingEntity target) {
		return this.weight;
	}

	@Override
	public String animationTrigger() {
		return id();
	}

	static float scaled(BastionBehemothEntity boss, double base) {
		return (float) (base * boss.getDamageMultiplier());
	}

	/** A flat ring of particles on the ground (telegraphs). */
	static void ring(ServerLevel level, Vec3 center, double radius, ParticleOptions particle, int points) {
		for (int i = 0; i < points; i++) {
			double a = Math.PI * 2 * i / points;
			level.sendParticles(particle, center.x + Math.cos(a) * radius, center.y + 0.15, center.z + Math.sin(a) * radius, 1, 0, 0, 0, 0);
		}
	}

	static Vec3 ground(ServerLevel level, Vec3 around) {
		return new Vec3(around.x, CombatUtil.groundY(level, around.x, around.z), around.z);
	}

	/** Debris of whatever the ground is made of at {@code at}. */
	static BlockParticleOption debris(ServerLevel level, Vec3 at) {
		BlockState state = level.getBlockState(BlockPos.containing(at.x, at.y - 0.5, at.z));
		return new BlockParticleOption(ParticleTypes.BLOCK, state.isAir() ? Blocks.BASALT.defaultBlockState() : state);
	}
}
