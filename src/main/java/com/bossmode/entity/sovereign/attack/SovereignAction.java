package com.bossmode.entity.sovereign.attack;

import com.bossmode.config.SovereignConfig;
import com.bossmode.entity.action.BossAction;
import com.bossmode.entity.boss.combat.CombatUtil;
import com.bossmode.entity.sovereign.VoidSovereignEntity;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/** Shared helpers of the Void Sovereign's patterns. */
abstract class SovereignAction extends BossAction<VoidSovereignEntity> {
	static final DustParticleOptions MARK = new DustParticleOptions(0xFF2D6A, 1.6F);
	static final DustParticleOptions DARK = new DustParticleOptions(0x14041C, 2.6F);

	private final int cooldown;
	private final int weight;
	private final String trigger;

	SovereignAction(String id, boolean special, int prepareTicks, int activeTicks, int totalTicks, int cooldown, int weight) {
		super(id, special, prepareTicks, activeTicks, totalTicks);
		this.cooldown = cooldown;
		this.weight = weight;
		this.trigger = id;
	}

	@Override
	public int cooldown() {
		return this.cooldown;
	}

	@Override
	public int weight(VoidSovereignEntity boss, LivingEntity target) {
		return this.weight;
	}

	@Override
	public String animationTrigger() {
		return this.trigger;
	}

	/** The mace counters are three times as likely while someone nearby holds a mace. */
	static int counterWeight(VoidSovereignEntity boss, int weight) {
		return boss.isMaceThreat() ? weight * SovereignConfig.COUNTER_WEIGHT_FACTOR : weight;
	}

	static float scaled(VoidSovereignEntity boss, double base) {
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
}
