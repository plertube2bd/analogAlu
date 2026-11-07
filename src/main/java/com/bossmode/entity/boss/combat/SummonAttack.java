package com.bossmode.entity.boss.combat;

import com.bossmode.config.BossConfig;
import com.bossmode.entity.action.ActionContext;
import com.bossmode.entity.action.BossAction;
import com.bossmode.entity.boss.BossPhase;
import com.bossmode.entity.boss.ShardDragonEntity;
import com.bossmode.entity.minion.MinionType;
import com.bossmode.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * Summon: opens portals around the dragon and calls minions from the current phase's roster (the roster narrows
 * every few phases while the count grows). Server-only, capped at {@link BossConfig#MAX_ACTIVE_MINIONS}.
 */
public final class SummonAttack extends BossAction<ShardDragonEntity> {
	public SummonAttack() {
		super("summon", true, BossConfig.SUMMON_HIT_TICK, 1, BossConfig.SUMMON_TOTAL_TICKS);
	}

	@Override
	public int cooldown() {
		return BossConfig.SUMMON_COOLDOWN;
	}

	@Override
	public String animationTrigger() {
		return "summon";
	}

	@Override
	public int weight(ShardDragonEntity boss, LivingEntity target) {
		return BossConfig.SUMMON_WEIGHT;
	}

	@Override
	public boolean canUse(ServerLevel level, ShardDragonEntity boss, LivingEntity target, double gap) {
		return !boss.getPhase().minions().isEmpty() && boss.countMinions(level) < BossConfig.MAX_ACTIVE_MINIONS;
	}

	@Override
	public void start(ServerLevel level, ShardDragonEntity boss, @Nullable LivingEntity target, ActionContext ctx) {
		boss.playSound(ModSounds.SHARD_DRAGON_SUMMON, 4.0F, 1.0F);
		int count = Math.max(0, Math.min(boss.getPhase().summonCount(), BossConfig.MAX_ACTIVE_MINIONS - boss.countMinions(level)));
		ctx.points.addAll(ringAround(level, boss, count, BossConfig.SUMMON_RADIUS * boss.sizeFactor() + boss.getBbWidth() * 0.5));
	}

	/** Ground-snapped points evenly spread around the dragon. */
	public static List<Vec3> ringAround(ServerLevel level, ShardDragonEntity boss, int count, double radius) {
		double startAngle = boss.getRandom().nextDouble() * Math.PI * 2;
		List<Vec3> points = new java.util.ArrayList<>();
		for (int i = 0; i < count; i++) {
			double a = startAngle + Math.PI * 2 * i / Math.max(1, count);
			double x = boss.getX() + Math.cos(a) * radius;
			double z = boss.getZ() + Math.sin(a) * radius;
			int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, BlockPos.containing(x, boss.getY(), z));
			points.add(new Vec3(x, y, z));
		}
		return points;
	}

	@Override
	public Result tick(ServerLevel level, ShardDragonEntity boss, @Nullable LivingEntity target, ActionContext ctx, int tick) {
		if (tick < prepareTicks()) {
			if (tick % 2 == 0) {
				for (Vec3 p : ctx.points) {
					level.sendParticles(ParticleTypes.REVERSE_PORTAL, p.x, p.y + 1, p.z, 4, 0.3, 0.6, 0.3, 0.05);
				}
			}
		} else if (tick == prepareTicks()) {
			BossPhase phase = boss.getPhase();
			for (Vec3 p : ctx.points) {
				if (boss.countMinions(level) >= BossConfig.MAX_ACTIVE_MINIONS || phase.minions().isEmpty()) {
					break;
				}
				MinionType type = phase.minions().get(boss.getRandom().nextInt(phase.minions().size()));
				type.spawn(level, boss, type == MinionType.WRAITH ? p.add(0, 1.5, 0) : p, target);
			}
		}
		return Result.CONTINUE;
	}
}
