package com.bossmode.entity.bastion.attack;

import com.bossmode.config.BastionConfig;
import com.bossmode.entity.action.ActionContext;
import com.bossmode.entity.bastion.BastionBehemothEntity;
import com.bossmode.entity.boss.combat.CombatUtil;
import com.bossmode.registry.ModSounds;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Crystal Spikes: both forefeet stamp and lines of amber spikes burst out of the ground, racing toward its target (and
 * everyone else nearby), launching whoever they catch. Step sideways out of a line.
 */
public final class CrystalSpikesAttack extends BastionAction {
	private static final BlockParticleOption AMBER = new BlockParticleOption(ParticleTypes.BLOCK, Blocks.RAW_GOLD_BLOCK.defaultBlockState());

	public CrystalSpikesAttack() {
		super("crystal_spikes", true, BastionConfig.SPIKES_HIT_TICK, 1, BastionConfig.SPIKES_TOTAL_TICKS, BastionConfig.SPIKES_COOLDOWN,
				BastionConfig.SPIKES_WEIGHT);
	}

	@Override
	public boolean canUse(ServerLevel level, BastionBehemothEntity boss, LivingEntity target, double gap) {
		return gap <= BastionConfig.SPIKES_RANGE;
	}

	@Override
	public Result tick(ServerLevel level, BastionBehemothEntity boss, @Nullable LivingEntity target, ActionContext ctx, int tick) {
		if (tick < prepareTicks()) {
			if (target != null) {
				boss.faceTarget(target, 10.0F);
			}
			return Result.CONTINUE;
		}
		if (tick == prepareTicks()) {
			boss.playSound(ModSounds.BASTION_BEHEMOTH_SPIKES, 5.0F, 0.8F);
			ctx.origin = boss.position();
			Vec3 forward = CombatUtil.horizontalForward(boss);
			ctx.directions.add(forward);
			ctx.directions.add(forward.yRot((float) Math.toRadians(25)));
			ctx.directions.add(forward.yRot((float) Math.toRadians(-25)));
			for (LivingEntity victim : CombatUtil.targetsInRadius(level, boss, boss.position(), BastionConfig.SPIKES_RANGE)) {
				Vec3 to = victim.position().subtract(boss.position()).multiply(1, 0, 1);
				if (victim instanceof Player && victim != target && to.lengthSqr() > 1.0E-4 && ctx.directions.size() < 6) {
					ctx.directions.add(to.normalize());
				}
			}
			ctx.extraRecoveryTicks = Math.max(0, prepareTicks() + (int) (BastionConfig.SPIKES_LENGTH / BastionConfig.SPIKES_STEP) + 2 - totalTicks());
		}
		int step = tick - prepareTicks();
		double start = boss.getBbWidth() * 0.5;
		double distance = start + step * BastionConfig.SPIKES_STEP;
		if (distance > start + BastionConfig.SPIKES_LENGTH) {
			return Result.CONTINUE;
		}
		float damage = scaled(boss, BastionConfig.SPIKES_DAMAGE);
		for (Vec3 dir : ctx.directions) {
			Vec3 p = ground(level, ctx.origin.add(dir.scale(distance)));
			level.sendParticles(AMBER, p.x, p.y + 0.6, p.z, 14, 0.3, 0.6, 0.3, 0.15);
			level.sendParticles(ParticleTypes.WAX_ON, p.x, p.y + 1.2, p.z, 3, 0.2, 0.4, 0.2, 0.1);
			for (LivingEntity victim : CombatUtil.targetsInRadius(level, boss, p.add(0, 0.8, 0), BastionConfig.SPIKES_RADIUS)) {
				if (ctx.markHit(victim.getUUID()) && victim.hurtServer(level, boss.damageSources().mobAttack(boss), damage)) {
					victim.setDeltaMovement(victim.getDeltaMovement().x * 0.3, 0.8, victim.getDeltaMovement().z * 0.3);
					victim.needsSync = true;
				}
			}
		}
		if (step % 3 == 0) {
			level.playSound(null, ctx.origin.x, ctx.origin.y, ctx.origin.z, ModSounds.BASTION_BEHEMOTH_SPIKES, boss.getSoundSource(), 1.5F,
					1.0F + step * 0.02F);
		}
		return Result.CONTINUE;
	}
}
