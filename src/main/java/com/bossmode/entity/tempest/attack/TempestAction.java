package com.bossmode.entity.tempest.attack;

import com.bossmode.entity.action.BossAction;
import com.bossmode.entity.boss.combat.CombatUtil;
import com.bossmode.entity.tempest.TempestWyvernEntity;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/** Shared helpers of the Tempest Wyvern's patterns. */
abstract class TempestAction extends BossAction<TempestWyvernEntity> {
	private final int cooldown;
	private final int weight;

	TempestAction(String id, boolean special, int prepareTicks, int activeTicks, int totalTicks, int cooldown, int weight) {
		super(id, special, prepareTicks, activeTicks, totalTicks);
		this.cooldown = cooldown;
		this.weight = weight;
	}

	@Override
	public int cooldown() {
		return this.cooldown;
	}

	@Override
	public int weight(TempestWyvernEntity boss, LivingEntity target) {
		return this.weight;
	}

	@Override
	public String animationTrigger() {
		return id();
	}

	static float scaled(TempestWyvernEntity boss, double base) {
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

	/** A harmless lightning bolt for the looks (damage is always dealt by the attack itself). */
	static void visualLightning(ServerLevel level, Vec3 at) {
		LightningBolt bolt = EntityTypes.LIGHTNING_BOLT.create(level, EntitySpawnReason.TRIGGERED);
		if (bolt != null) {
			bolt.snapTo(at.x, at.y, at.z);
			bolt.setVisualOnly(true);
			level.addFreshEntity(bolt);
		}
	}
}
