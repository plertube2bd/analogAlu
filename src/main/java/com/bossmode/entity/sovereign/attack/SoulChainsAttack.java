package com.bossmode.entity.sovereign.attack;

import com.bossmode.config.SovereignConfig;
import com.bossmode.entity.action.ActionContext;
import com.bossmode.entity.boss.combat.CombatUtil;
import com.bossmode.entity.sovereign.VoidSovereignEntity;
import com.bossmode.registry.ModSounds;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.UUID;

/**
 * Soul Chains (phase 6+): everyone near the Sovereign is chained by the soul to the spot they stand on for five seconds.
 * Stretch the chain - run, launch yourself with a wind charge, pearl away - and it yanks you back, hurts, and takes the
 * height you gained with it. Stand your ground and fight where you are.
 */
public final class SoulChainsAttack extends SovereignAction {
	public SoulChainsAttack() {
		super("soul_chains", true, SovereignConfig.CHAINS_BIND_TICK, SovereignConfig.CHAINS_END_TICK - SovereignConfig.CHAINS_BIND_TICK,
				SovereignConfig.CHAINS_TOTAL_TICKS, SovereignConfig.CHAINS_COOLDOWN, SovereignConfig.CHAINS_WEIGHT);
	}

	@Override
	public int weight(VoidSovereignEntity boss, LivingEntity target) {
		return counterWeight(boss, super.weight(boss, target));
	}

	@Override
	public boolean canUse(ServerLevel level, VoidSovereignEntity boss, LivingEntity target, double gap) {
		return gap <= SovereignConfig.CHAINS_RANGE;
	}

	@Override
	public Result tick(ServerLevel level, VoidSovereignEntity boss, @Nullable LivingEntity target, ActionContext ctx, int tick) {
		double range = boss.getBbWidth() * 0.5 + SovereignConfig.CHAINS_RANGE;
		if (tick < prepareTicks()) {
			if (tick % 3 == 0) {
				for (LivingEntity victim : CombatUtil.targetsInRadius(level, boss, boss.position(), range)) {
					level.sendParticles(ParticleTypes.SOUL, victim.getX(), victim.getY() + 0.1, victim.getZ(), 3, 0.4, 0.05, 0.4, 0.01);
				}
			}
			return Result.CONTINUE;
		}
		if (tick == prepareTicks()) {
			boss.playSound(ModSounds.VOID_SOVEREIGN_CHAINS, 5.0F, 0.8F);
			for (LivingEntity victim : CombatUtil.targetsInRadius(level, boss, boss.position(), range)) {
				ctx.targets.add(victim.getUUID());
				ctx.points.add(ground(level, victim.position()));
			}
		}
		if (tick >= executionEnd()) {
			if (tick == executionEnd()) {
				boss.playSound(ModSounds.VOID_SOVEREIGN_CHAINS, 3.0F, 1.5F);
				for (Vec3 anchor : ctx.points) {
					level.sendParticles(ParticleTypes.SOUL, anchor.x, anchor.y + 0.5, anchor.z, 12, 0.4, 0.4, 0.4, 0.05);
				}
			}
			return Result.CONTINUE;
		}
		float damage = scaled(boss, SovereignConfig.CHAINS_DAMAGE);
		for (int i = 0; i < ctx.targets.size(); i++) {
			UUID id = ctx.targets.get(i);
			Vec3 anchor = ctx.points.get(i);
			Entity e = level.getEntity(id);
			if (!(e instanceof LivingEntity victim) || !victim.isAlive() || !CombatUtil.canHit(boss, victim)) {
				continue;
			}
			Vec3 to = anchor.add(0, 0.5, 0).subtract(victim.position());
			double stretch = to.length();
			if (tick % 2 == 0) {
				// the chain: links of soul fire from the anchor to the victim
				for (double d = 0; d < stretch; d += 0.7) {
					Vec3 p = victim.position().add(0, 0.6, 0).add(to.normalize().scale(d));
					level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, p.x, p.y, p.z, 1, 0.02, 0.02, 0.02, 0);
				}
				ring(level, anchor, 0.8, MARK, 8);
			}
			if (stretch > 12.0) {
				victim.teleportTo(anchor.x, anchor.y, anchor.z); // pearls and blinks do not break a soul chain
				victim.resetFallDistance();
			} else if (stretch > SovereignConfig.CHAINS_LENGTH) {
				Vec3 yank = to.normalize().scale(SovereignConfig.CHAINS_YANK);
				victim.setDeltaMovement(yank.x, Math.min(yank.y, 0.2), yank.z);
				victim.needsSync = true;
				victim.resetFallDistance();
				if ((tick - prepareTicks()) % 10 == 0) {
					victim.hurtServer(level, boss.damageSources().indirectMagic(boss, boss), damage);
				}
			}
		}
		return Result.CONTINUE;
	}
}
