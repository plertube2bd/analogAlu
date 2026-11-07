package com.bossmode.entity.sovereign.attack;

import com.bossmode.config.SovereignConfig;
import com.bossmode.entity.action.ActionContext;
import com.bossmode.entity.boss.combat.CombatUtil;
import com.bossmode.entity.sovereign.VoidSovereignEntity;
import com.bossmode.registry.ModSounds;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.UUID;

/**
 * Ground Pound: both fists hammer the ground, then {@link SovereignConfig#POUND_RINGS} shockwave rings race outward
 * along the ground. A ring only hurts what is standing on the ground when it passes - jump over it.
 */
public final class GroundPoundAttack extends SovereignAction {
	private static final BlockParticleOption DEBRIS = new BlockParticleOption(ParticleTypes.BLOCK, Blocks.BLACKSTONE.defaultBlockState());

	public GroundPoundAttack() {
		super("ground_pound", true, SovereignConfig.POUND_HIT_TICK, SovereignConfig.POUND_TOTAL_TICKS - SovereignConfig.POUND_HIT_TICK - 6,
				SovereignConfig.POUND_TOTAL_TICKS, SovereignConfig.POUND_COOLDOWN, SovereignConfig.POUND_WEIGHT);
	}

	@Override
	public boolean canUse(ServerLevel level, VoidSovereignEntity boss, LivingEntity target, double gap) {
		return boss.onGround() && boss.distanceTo(target) <= SovereignConfig.POUND_MAX_RANGE * boss.sizeFactor();
	}

	@Override
	public Result tick(ServerLevel level, VoidSovereignEntity boss, @Nullable LivingEntity target, ActionContext ctx, int tick) {
		if (tick < prepareTicks()) {
			if (target != null) {
				boss.faceTarget(target, 15.0F);
			}
			return Result.CONTINUE;
		}
		double sf = boss.sizeFactor();
		if (tick == prepareTicks()) {
			ctx.origin = boss.position();
			boss.playSound(ModSounds.VOID_SOVEREIGN_POUND, 6.0F, 0.6F);
			Vec3 fists = boss.frontPoint(1.5);
			level.sendParticles(ParticleTypes.EXPLOSION, fists.x, fists.y + 0.5, fists.z, 3, 1.0, 0.3, 1.0, 0);
			level.sendParticles(DEBRIS, fists.x, fists.y + 0.5, fists.z, 60, 2.0, 0.3, 2.0, 0.3);
			float damage = scaled(boss, SovereignConfig.POUND_DAMAGE);
			for (LivingEntity victim : CombatUtil.targetsInRadius(level, boss, fists, 4.0 * sf)) {
				if (victim.hurtServer(level, boss.damageSources().mobAttack(boss), damage)) {
					CombatUtil.knockbackFrom(victim, fists, 1.2, 0.6);
				}
			}
		}
		float damage = scaled(boss, SovereignConfig.POUND_DAMAGE * 0.75);
		double start = boss.getBbWidth() * 0.5;
		for (int ring = 0; ring < SovereignConfig.POUND_RINGS; ring++) {
			int age = tick - prepareTicks() - ring * SovereignConfig.POUND_RING_GAP;
			if (age < 0) {
				continue;
			}
			double radius = start + age * SovereignConfig.POUND_RING_SPEED * sf;
			if (radius > SovereignConfig.POUND_RING_MAX * sf) {
				continue;
			}
			int points = (int) Math.min(96, 12 + radius * 4);
			for (int i = 0; i < points; i++) {
				double a = Math.PI * 2 * i / points;
				double x = ctx.origin.x + Math.cos(a) * radius, z = ctx.origin.z + Math.sin(a) * radius;
				double y = CombatUtil.groundY(level, x, z);
				level.sendParticles(i % 3 == 0 ? DEBRIS : VoidSovereignEntity.VOID_DUST, x, y + 0.2, z, 1, 0.05, 0.1, 0.05, 0.05);
			}
			double half = SovereignConfig.POUND_RING_WIDTH * 0.5;
			for (LivingEntity victim : CombatUtil.targetsInRadius(level, boss, ctx.origin, radius + half + 2)) {
				double dx = victim.getX() - ctx.origin.x, dz = victim.getZ() - ctx.origin.z;
				double d = Math.sqrt(dx * dx + dz * dz);
				boolean grounded = victim.onGround() || victim.getY() - CombatUtil.groundY(level, victim.getX(), victim.getZ()) < 0.3;
				UUID ringHit = new UUID(victim.getUUID().getMostSignificantBits() ^ (ring + 1), victim.getUUID().getLeastSignificantBits());
				if (grounded && Math.abs(d - radius) <= half + victim.getBbWidth() * 0.5 && ctx.markHit(ringHit)
						&& victim.hurtServer(level, boss.damageSources().mobAttack(boss), damage)) {
					CombatUtil.knockbackFrom(victim, ctx.origin, 0.8, 0.7);
				}
			}
		}
		return Result.CONTINUE;
	}
}
