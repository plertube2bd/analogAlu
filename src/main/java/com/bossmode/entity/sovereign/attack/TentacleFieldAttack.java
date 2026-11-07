package com.bossmode.entity.sovereign.attack;

import com.bossmode.config.SovereignConfig;
import com.bossmode.entity.action.ActionContext;
import com.bossmode.entity.boss.combat.CombatUtil;
import com.bossmode.entity.sovereign.VoidSovereignEntity;
import com.bossmode.registry.ModSounds;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * Tentacle Field (phase 5+): the ground around the Sovereign splits into a checkerboard and tentacles burst out of every
 * other square - then the other half, then the first again. Each wave is marked a second ahead: step onto a square that
 * just erupted.
 */
public final class TentacleFieldAttack extends SovereignAction {
	public TentacleFieldAttack() {
		super("tentacle_field", true, SovereignConfig.FIELD_FIRST_TICK, SovereignConfig.FIELD_GAP * (SovereignConfig.FIELD_WAVES - 1) + 1,
				SovereignConfig.FIELD_TOTAL_TICKS, SovereignConfig.FIELD_COOLDOWN, SovereignConfig.FIELD_WEIGHT);
	}

	@Override
	public boolean canUse(ServerLevel level, VoidSovereignEntity boss, LivingEntity target, double gap) {
		return gap <= SovereignConfig.FIELD_RADIUS * boss.sizeFactor();
	}

	@Override
	public void start(ServerLevel level, VoidSovereignEntity boss, @Nullable LivingEntity target, ActionContext ctx) {
		ctx.origin = ground(level, boss.position());
		double radius = boss.getBbWidth() * 0.5 + SovereignConfig.FIELD_RADIUS * boss.sizeFactor();
		double cell = SovereignConfig.FIELD_CELL;
		int n = (int) Math.ceil(radius / cell);
		for (int i = -n; i < n; i++) {
			for (int j = -n; j < n; j++) {
				Vec3 c = ctx.origin.add((i + 0.5) * cell, 0, (j + 0.5) * cell);
				double d = Math.hypot(c.x - ctx.origin.x, c.z - ctx.origin.z);
				if (d > radius || d < boss.getBbWidth() * 0.5) {
					continue;
				}
				// even squares in points, odd squares in directions
				((i + j & 1) == 0 ? ctx.points : ctx.directions).add(ground(level, c));
			}
		}
		boss.playSound(ModSounds.VOID_SOVEREIGN_RIFT, 5.0F, 0.6F);
	}

	@Override
	public Result tick(ServerLevel level, VoidSovereignEntity boss, @Nullable LivingEntity target, ActionContext ctx, int tick) {
		double half = SovereignConfig.FIELD_CELL * 0.5;
		for (int wave = 0; wave < SovereignConfig.FIELD_WAVES; wave++) {
			int erupt = SovereignConfig.FIELD_FIRST_TICK + wave * SovereignConfig.FIELD_GAP;
			List<Vec3> cells = wave % 2 == 0 ? ctx.points : ctx.directions;
			if (tick >= erupt - 20 && tick < erupt && tick % 3 == 0) {
				for (Vec3 c : cells) {
					level.sendParticles(MARK, c.x - half + 0.4, c.y + 0.15, c.z - half + 0.4, 1, 0, 0, 0, 0);
					level.sendParticles(MARK, c.x + half - 0.4, c.y + 0.15, c.z + half - 0.4, 1, 0, 0, 0, 0);
					level.sendParticles(ParticleTypes.SQUID_INK, c.x, c.y + 0.1, c.z, 1, half * 0.5, 0.02, half * 0.5, 0.01);
				}
			}
			if (tick == erupt) {
				ctx.resetHits();
				boss.playSound(ModSounds.VOID_SOVEREIGN_LASH, 5.0F, 0.7F + wave * 0.1F);
				float damage = scaled(boss, SovereignConfig.FIELD_DAMAGE);
				for (Vec3 c : cells) {
					level.sendParticles(DARK, c.x, c.y + 1.5, c.z, 8, half * 0.4, 1.2, half * 0.4, 0);
					level.sendParticles(ParticleTypes.REVERSE_PORTAL, c.x, c.y + 1.0, c.z, 6, half * 0.4, 1.0, half * 0.4, 0.15);
					for (LivingEntity victim : CombatUtil.targetsInBox(level, boss, new AABB(c.x - half, c.y - 1.0, c.z - half, c.x + half, c.y + 3.0, c.z + half))) {
						if (ctx.markHit(victim.getUUID()) && victim.hurtServer(level, boss.damageSources().mobAttack(boss), damage)) {
							victim.setDeltaMovement(victim.getDeltaMovement().x * 0.3, SovereignConfig.FIELD_LAUNCH, victim.getDeltaMovement().z * 0.3);
							victim.needsSync = true;
						}
					}
				}
			}
		}
		return Result.CONTINUE;
	}
}
