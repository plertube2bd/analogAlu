package com.bossmode.entity.boss.combat;

import com.bossmode.config.BossConfig;
import com.bossmode.entity.action.ActionContext;
import com.bossmode.entity.action.BossAction;
import com.bossmode.entity.boss.ShardDragonEntity;
import com.bossmode.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.UUID;

/**
 * Crystal Rain: three waves. Every wave marks the ground under each nearby target (plus random spots) with a
 * glowing ring; {@link BossConfig#RAIN_FALL_TICKS} later a crystal crashes down on each mark. Move to dodge.
 */
public final class CrystalRainAttack extends BossAction<ShardDragonEntity> {
	private static final DustParticleOptions MARK = new DustParticleOptions(0xD070FF, 1.6F);
	private static final BlockParticleOption CRYSTAL = new BlockParticleOption(ParticleTypes.BLOCK, Blocks.AMETHYST_BLOCK.defaultBlockState());

	public CrystalRainAttack() {
		super("crystal_rain", true, BossConfig.RAIN_PREPARATION_TICKS,
				(BossConfig.RAIN_WAVES - 1) * BossConfig.RAIN_WAVE_INTERVAL + BossConfig.RAIN_FALL_TICKS + 1, BossConfig.RAIN_TOTAL_TICKS);
	}

	@Override
	public int cooldown() {
		return BossConfig.RAIN_COOLDOWN;
	}

	@Override
	public String animationTrigger() {
		return "crystal_rain";
	}

	@Override
	public int weight(ShardDragonEntity boss, LivingEntity target) {
		return BossConfig.RAIN_WEIGHT;
	}

	@Override
	public boolean canUse(ServerLevel level, ShardDragonEntity boss, LivingEntity target, double gap) {
		return boss.distanceTo(target) <= BossConfig.RAIN_RANGE;
	}

	@Override
	public void start(ServerLevel level, ShardDragonEntity boss, @Nullable LivingEntity target, ActionContext ctx) {
		boss.playSound(ModSounds.SHARD_DRAGON_ROAR, 4.0F, 1.1F);
	}

	@Override
	public Result tick(ServerLevel level, ShardDragonEntity boss, @Nullable LivingEntity target, ActionContext ctx, int tick) {
		int sinceStart = tick - prepareTicks();
		if (sinceStart >= 0 && sinceStart % BossConfig.RAIN_WAVE_INTERVAL == 0 && sinceStart / BossConfig.RAIN_WAVE_INTERVAL < BossConfig.RAIN_WAVES) {
			markWave(level, boss, ctx, tick);
		}
		if (tick % 4 == 0) {
			for (ActionContext.Scheduled s : ctx.scheduled) {
				ring(level, s.position(), BossConfig.RAIN_IMPACT_RADIUS);
				level.sendParticles(ParticleTypes.END_ROD, s.position().x, s.position().y + 6 + level.getRandom().nextDouble() * 10, s.position().z, 1, 0.1, 2.0, 0.1, 0.0);
			}
		}
		Iterator<ActionContext.Scheduled> it = ctx.scheduled.iterator();
		while (it.hasNext()) {
			ActionContext.Scheduled s = it.next();
			if (s.tick() == tick) {
				impact(level, boss, s.position());
				it.remove();
			}
		}
		return Result.CONTINUE;
	}

	private void markWave(ServerLevel level, ShardDragonEntity boss, ActionContext ctx, int tick) {
		boss.playSound(ModSounds.SHARD_DRAGON_VOLLEY, 3.0F, 0.6F);
		for (LivingEntity victim : CombatUtil.targetsInRadius(level, boss, boss.position(), BossConfig.RAIN_RANGE)) {
			addMark(level, ctx, tick, victim.position().add((level.getRandom().nextDouble() - 0.5) * 2, 0, (level.getRandom().nextDouble() - 0.5) * 2));
		}
		double spread = 16.0 * boss.sizeFactor();
		for (int i = 0; i < BossConfig.RAIN_EXTRA_IMPACTS; i++) {
			addMark(level, ctx, tick, boss.position().add((level.getRandom().nextDouble() - 0.5) * 2 * spread, 0, (level.getRandom().nextDouble() - 0.5) * 2 * spread));
		}
	}

	private void addMark(ServerLevel level, ActionContext ctx, int tick, Vec3 around) {
		int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, BlockPos.containing(around));
		ctx.scheduled.add(new ActionContext.Scheduled(tick + BossConfig.RAIN_FALL_TICKS, new Vec3(around.x, y, around.z)));
	}

	private void ring(ServerLevel level, Vec3 pos, double radius) {
		for (int i = 0; i < 12; i++) {
			double a = Math.PI * 2 * i / 12;
			level.sendParticles(MARK, pos.x + Math.cos(a) * radius, pos.y + 0.15, pos.z + Math.sin(a) * radius, 1, 0, 0, 0, 0);
		}
	}

	private void impact(ServerLevel level, ShardDragonEntity boss, Vec3 pos) {
		level.sendParticles(ParticleTypes.EXPLOSION, pos.x, pos.y + 0.5, pos.z, 2, 0.5, 0.3, 0.5, 0);
		level.sendParticles(CRYSTAL, pos.x, pos.y + 0.5, pos.z, 30, 0.8, 0.5, 0.8, 0.3);
		level.playSound(null, pos.x, pos.y, pos.z, ModSounds.SHARD_DRAGON_IMPACT, boss.getSoundSource(), 2.5F, 0.9F + level.getRandom().nextFloat() * 0.3F);
		float damage = (float) (BossConfig.RAIN_DAMAGE * boss.getDamageMultiplier());
		Set<UUID> hitByThisImpact = new HashSet<>();
		for (LivingEntity victim : CombatUtil.targetsInRadius(level, boss, pos.add(0, 0.8, 0), BossConfig.RAIN_IMPACT_RADIUS)) {
			if (hitByThisImpact.add(victim.getUUID()) && victim.hurtServer(level, boss.damageSources().mobAttack(boss), damage)) {
				CombatUtil.knockbackFrom(victim, pos, 0.6, 0.4);
			}
		}
	}

	@Override
	public void stop(ServerLevel level, ShardDragonEntity boss, ActionContext ctx, boolean interrupted) {
		ctx.scheduled.clear();
	}
}
