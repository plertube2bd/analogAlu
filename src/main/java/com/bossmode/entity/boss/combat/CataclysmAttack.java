package com.bossmode.entity.boss.combat;

import com.bossmode.config.BossConfig;
import com.bossmode.entity.action.ActionContext;
import com.bossmode.entity.action.BossAction;
import com.bossmode.entity.boss.ShardDragonEntity;
import com.bossmode.registry.ModSounds;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Cataclysm (ultimate, phases 8-10). The dragon rises and gathers power for 2.5 s, then everything within 64 blocks
 * that can see its chest takes a massive hit. Hiding behind blocks reduces it to 15 %, standing right under the
 * dragon avoids it. Afterwards the dragon collapses, EXPOSED, for 3.5 s - falling out of the sky if it was airborne.
 */
public final class CataclysmAttack extends BossAction<ShardDragonEntity> {
	public CataclysmAttack() {
		super("cataclysm", true, BossConfig.CATA_BLAST_TICK, 1, BossConfig.CATA_TOTAL_TICKS);
	}

	@Override
	public int cooldown() {
		return BossConfig.CATA_COOLDOWN;
	}

	@Override
	public String animationTrigger() {
		return "cataclysm";
	}

	@Override
	public int weight(ShardDragonEntity boss, LivingEntity target) {
		return BossConfig.CATA_WEIGHT;
	}

	@Override
	public boolean canUse(ServerLevel level, ShardDragonEntity boss, LivingEntity target, double gap) {
		return boss.distanceTo(target) <= BossConfig.CATA_RADIUS;
	}

	@Override
	public Result tick(ServerLevel level, ShardDragonEntity boss, @Nullable LivingEntity target, ActionContext ctx, int tick) {
		Vec3 core = boss.position().add(0, boss.getBbHeight() * 0.6, 0);
		if (tick < prepareTicks()) {
			boss.setDeltaMovement(0, boss.getDeltaMovement().y, 0);
			if (tick == 0 || tick == 25) {
				boss.playSound(ModSounds.SHARD_DRAGON_CATACLYSM_CHARGE, 6.0F, 0.7F);
			}
			if (tick == 5 || tick == 30) {
				warn(level, boss, Component.translatable("message.boss_mode.cataclysm_warning"));
			}
			if (tick % 2 == 0) {
				double r = boss.getBbWidth() * (1.5 - tick / (double) prepareTicks());
				for (int i = 0; i < 12; i++) {
					double a = Math.PI * 2 * i / 12 + tick * 0.25;
					level.sendParticles(ParticleTypes.REVERSE_PORTAL, core.x + Math.cos(a) * r, core.y + Math.sin(tick * 0.3) * 2, core.z + Math.sin(a) * r, 1, 0, 0, 0, 0);
				}
				level.sendParticles(ParticleTypes.END_ROD, core.x, core.y, core.z, 3, 0.5, 0.5, 0.5, 0.05);
			}
			return Result.CONTINUE;
		}
		if (tick == prepareTicks()) {
			blast(level, boss, core, ctx);
			if (boss.isFlying()) {
				boss.setFlying(false); // spent, it drops out of the sky for the EXPOSED collapse
			}
			warn(level, boss, Component.translatable("message.boss_mode.exposed"));
		} else if (tick % 5 == 0) {
			level.sendParticles(ParticleTypes.ELECTRIC_SPARK, core.x, core.y, core.z, 12, boss.getBbWidth() * 0.3, 0.8, boss.getBbWidth() * 0.3, 0.1);
			level.sendParticles(ParticleTypes.GLOW, core.x, core.y, core.z, 4, boss.getBbWidth() * 0.3, 0.8, boss.getBbWidth() * 0.3, 0.02);
		}
		return Result.CONTINUE;
	}

	private void blast(ServerLevel level, ShardDragonEntity boss, Vec3 core, ActionContext ctx) {
		boss.playSound(ModSounds.SHARD_DRAGON_CATACLYSM, 8.0F, 0.6F);
		level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, core.x, core.y, core.z, 3, 2.0, 1.0, 2.0, 0);
		for (int ring = 1; ring <= 4; ring++) {
			double r = ring * BossConfig.CATA_RADIUS / 6.0;
			for (int i = 0; i < 24; i++) {
				double a = Math.PI * 2 * i / 24;
				level.sendParticles(ParticleTypes.SONIC_BOOM, core.x + Math.cos(a) * r, boss.getY() + 1.0, core.z + Math.sin(a) * r, 1, 0, 0, 0, 0);
			}
		}
		float damage = (float) (BossConfig.CATA_DAMAGE * boss.getDamageMultiplier());
		for (LivingEntity victim : CombatUtil.targetsInRadius(level, boss, boss.position(), BossConfig.CATA_RADIUS)) {
			if (!ctx.markHit(victim.getUUID()) || boss.gapTo(victim) <= 1.5) {
				continue; // safe under the dragon
			}
			HitResult cover = level.clip(new ClipContext(core, victim.getEyePosition(), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, boss));
			boolean covered = cover.getType() != HitResult.Type.MISS;
			if (victim.hurtServer(level, boss.damageSources().mobAttack(boss), covered ? (float) (damage * BossConfig.CATA_COVER_MULTIPLIER) : damage) && !covered) {
				CombatUtil.knockbackFrom(victim, boss.position(), 2.5, 0.8);
			}
		}
	}

	private static void warn(ServerLevel level, ShardDragonEntity boss, Component message) {
		for (ServerPlayer player : level.players()) {
			if (player.distanceTo(boss) <= BossConfig.CATA_RADIUS + 16) {
				player.sendOverlayMessage(message);
			}
		}
	}
}
