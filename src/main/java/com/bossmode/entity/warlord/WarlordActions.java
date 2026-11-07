package com.bossmode.entity.warlord;

import com.bossmode.config.BossConfig;
import com.bossmode.entity.ShardFaction;
import com.bossmode.entity.action.ActionContext;
import com.bossmode.entity.action.BossAction;
import com.bossmode.entity.boss.combat.CombatUtil;
import com.bossmode.entity.boss.combat.SpikeLine;
import com.bossmode.entity.minion.ShardHoundEntity;
import com.bossmode.entity.projectile.WyrmShardProjectile;
import com.bossmode.registry.ModEntities;
import com.bossmode.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.List;

/** The Shard Warlord's five actions (animation keyframes in tools/blender/model_shard_warlord.py). */
public final class WarlordActions {
	private WarlordActions() {
	}

	public static List<BossAction<ShardWarlordEntity>> all() {
		return List.of(new StaffSwing(), new ShardBolt(), new GroundSpikes(), new Warcry(), new Blink());
	}

	/** Normal attack: wide staff swing in front. */
	static final class StaffSwing extends BossAction<ShardWarlordEntity> {
		StaffSwing() {
			super("staff_swing", false, BossConfig.WARLORD_SWING_HIT_TICK, 1, BossConfig.WARLORD_SWING_TOTAL);
		}

		@Override
		public int cooldown() {
			return BossConfig.WARLORD_SWING_COOLDOWN;
		}

		@Override
		public String animationTrigger() {
			return "attack";
		}

		@Override
		public boolean canUse(ServerLevel level, ShardWarlordEntity host, LivingEntity target, double gap) {
			return gap <= BossConfig.WARLORD_SWING_RANGE;
		}

		@Override
		public Result tick(ServerLevel level, ShardWarlordEntity host, @Nullable LivingEntity target, ActionContext ctx, int tick) {
			if (tick < prepareTicks() && target != null) {
				host.faceTarget(target, 25.0F);
			} else if (tick == prepareTicks()) {
				host.playSound(ModSounds.SHARD_WARLORD_SWING, 1.5F, 0.8F);
				float damage = (float) host.getAttributeValue(Attributes.ATTACK_DAMAGE);
				for (LivingEntity victim : CombatUtil.targetsInRadius(level, host, host.position().add(0, 1, 0), host.getBbWidth() / 2 + BossConfig.WARLORD_SWING_RANGE + 0.5)) {
					if (CombatUtil.isInFront(host, victim, BossConfig.WARLORD_SWING_CONE) && ctx.markHit(victim.getUUID())
							&& victim.hurtServer(level, host.damageSources().mobAttack(host), damage)) {
						CombatUtil.knockbackFrom(victim, host.position(), 1.0, 0.3);
					}
				}
			}
			return Result.CONTINUE;
		}
	}

	/** Thrusts the staff: three fast crystal bolts. */
	static final class ShardBolt extends BossAction<ShardWarlordEntity> {
		ShardBolt() {
			super("shard_bolt", true, BossConfig.WARLORD_BOLT_HIT_TICK, 1, BossConfig.WARLORD_BOLT_TOTAL);
		}

		@Override
		public int cooldown() {
			return BossConfig.WARLORD_BOLT_COOLDOWN;
		}

		@Override
		public String animationTrigger() {
			return "cast";
		}

		@Override
		public int weight(ShardWarlordEntity host, LivingEntity target) {
			return 30;
		}

		@Override
		public boolean canUse(ServerLevel level, ShardWarlordEntity host, LivingEntity target, double gap) {
			double d = host.distanceTo(target);
			return d >= 5.0 && d <= 28.0 && host.getSensing().hasLineOfSight(target);
		}

		@Override
		public Result tick(ServerLevel level, ShardWarlordEntity host, @Nullable LivingEntity target, ActionContext ctx, int tick) {
			Vec3 staff = host.position().add(0, host.getBbHeight() * 0.75, 0).add(CombatUtil.horizontalForward(host).scale(host.getBbWidth() * 0.8));
			if (tick < prepareTicks()) {
				if (target != null) {
					host.faceTarget(target, 20.0F);
				}
				level.sendParticles(ParticleTypes.WITCH, staff.x, staff.y, staff.z, 2, 0.2, 0.2, 0.2, 0);
			} else if (tick == prepareTicks() && target != null) {
				Vec3 aim = target.getBoundingBox().getCenter().subtract(staff).normalize();
				for (int i = -1; i <= 1; i++) {
					level.addFreshEntity(new WyrmShardProjectile(level, host, staff, aim.yRot((float) Math.toRadians(i * 12)).scale(1.6), BossConfig.WARLORD_BOLT_DAMAGE));
				}
				host.playSound(ModSounds.SHARD_WARLORD_CAST, 2.0F, 1.0F);
			}
			return Result.CONTINUE;
		}
	}

	/** Slams the staff: a line of crystal spikes races at the target. */
	static final class GroundSpikes extends BossAction<ShardWarlordEntity> {
		private static final int STEPS = 20;

		GroundSpikes() {
			super("ground_spikes", true, BossConfig.WARLORD_SPIKES_HIT_TICK, STEPS, BossConfig.WARLORD_SPIKES_TOTAL);
		}

		@Override
		public int cooldown() {
			return BossConfig.WARLORD_SPIKES_COOLDOWN;
		}

		@Override
		public String animationTrigger() {
			return "slam";
		}

		@Override
		public int weight(ShardWarlordEntity host, LivingEntity target) {
			return 30;
		}

		@Override
		public boolean canUse(ServerLevel level, ShardWarlordEntity host, LivingEntity target, double gap) {
			return host.onGround() && host.distanceTo(target) <= STEPS;
		}

		@Override
		public Result tick(ServerLevel level, ShardWarlordEntity host, @Nullable LivingEntity target, ActionContext ctx, int tick) {
			if (tick < prepareTicks()) {
				if (target != null) {
					host.faceTarget(target, 15.0F);
				}
				return Result.CONTINUE;
			}
			if (tick == prepareTicks()) {
				Vec3 dir = target != null ? target.position().subtract(host.position()).multiply(1, 0, 1) : CombatUtil.horizontalForward(host);
				ctx.direction = dir.lengthSqr() < 1.0E-4 ? CombatUtil.horizontalForward(host) : dir.normalize();
				ctx.origin = host.position().add(ctx.direction.scale(host.getBbWidth()));
				host.playSound(ModSounds.SHARD_DRAGON_SPIKES, 2.0F, 1.2F);
			}
			int step = tick - prepareTicks();
			if (step < STEPS && ctx.direction != Vec3.ZERO
					&& !SpikeLine.erupt(level, host, host.damageSources().mobAttack(host), ctx.origin, ctx.direction, step, 1.0, 1.2,
					(float) BossConfig.WARLORD_SPIKES_DAMAGE, 0.8, ctx)) {
				ctx.direction = Vec3.ZERO;
			}
			return Result.CONTINUE;
		}
	}

	/** War cry: strengthens nearby faction members and calls two hounds. */
	static final class Warcry extends BossAction<ShardWarlordEntity> {
		Warcry() {
			super("warcry", true, BossConfig.WARLORD_WARCRY_HIT_TICK, 1, BossConfig.WARLORD_WARCRY_TOTAL);
		}

		@Override
		public int cooldown() {
			return BossConfig.WARLORD_WARCRY_COOLDOWN;
		}

		@Override
		public String animationTrigger() {
			return "warcry";
		}

		@Override
		public int weight(ShardWarlordEntity host, LivingEntity target) {
			return 15;
		}

		@Override
		public boolean canUse(ServerLevel level, ShardWarlordEntity host, LivingEntity target, double gap) {
			return true;
		}

		@Override
		public Result tick(ServerLevel level, ShardWarlordEntity host, @Nullable LivingEntity target, ActionContext ctx, int tick) {
			if (tick != prepareTicks()) {
				return Result.CONTINUE;
			}
			host.playSound(ModSounds.SHARD_WARLORD_WARCRY, 3.0F, 0.8F);
			level.sendParticles(ParticleTypes.ANGRY_VILLAGER, host.getX(), host.getY(1.1), host.getZ(), 6, 0.6, 0.3, 0.6, 0);
			for (LivingEntity ally : level.getEntitiesOfClass(LivingEntity.class, host.getBoundingBox().inflate(20), ShardFaction::isMember)) {
				ally.addEffect(new MobEffectInstance(MobEffects.STRENGTH, 200, 0), host);
				ally.addEffect(new MobEffectInstance(MobEffects.SPEED, 200, 0), host);
				level.sendParticles(ParticleTypes.ENCHANT, ally.getX(), ally.getY(1.0), ally.getZ(), 10, 0.4, 0.5, 0.4, 0.5);
			}
			for (int i = 0; i < 2; i++) {
				double a = host.getRandom().nextDouble() * Math.PI * 2;
				double x = host.getX() + Math.cos(a) * 3;
				double z = host.getZ() + Math.sin(a) * 3;
				int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, BlockPos.containing(x, host.getY(), z));
				Mob hound = (i == 0 ? ModEntities.SHARD_HOUND : ModEntities.SHARD_SPITTER).create(level, EntitySpawnReason.MOB_SUMMONED);
				if (hound instanceof ShardHoundEntity shardHound) {
					shardHound.snapTo(x, y, z, host.getYRot(), 0);
					shardHound.finalizeSpawn(level, level.getCurrentDifficultyAt(BlockPos.containing(x, y, z)), EntitySpawnReason.MOB_SUMMONED, null);
					shardHound.setSummoner(host);
					if (target != null) {
						shardHound.setTarget(target);
					}
					level.addFreshEntity(shardHound);
				}
			}
			return Result.CONTINUE;
		}
	}

	/** Blink: vanishes and reappears behind the target. */
	static final class Blink extends BossAction<ShardWarlordEntity> {
		Blink() {
			super("blink", true, BossConfig.WARLORD_BLINK_TICK, 1, BossConfig.WARLORD_BLINK_TOTAL);
		}

		@Override
		public int cooldown() {
			return BossConfig.WARLORD_BLINK_COOLDOWN;
		}

		@Override
		public String animationTrigger() {
			return "blink";
		}

		@Override
		public int weight(ShardWarlordEntity host, LivingEntity target) {
			return host.distanceTo(target) > 10 ? 40 : 10;
		}

		@Override
		public boolean canUse(ServerLevel level, ShardWarlordEntity host, LivingEntity target, double gap) {
			return host.distanceTo(target) > 4.0;
		}

		@Override
		public Result tick(ServerLevel level, ShardWarlordEntity host, @Nullable LivingEntity target, ActionContext ctx, int tick) {
			if (tick == 0) {
				host.playSound(ModSounds.SHARD_WARLORD_BLINK, 1.5F, 0.8F);
			}
			if (tick == prepareTicks() && target != null) {
				level.sendParticles(ParticleTypes.REVERSE_PORTAL, host.getX(), host.getY(0.5), host.getZ(), 40, 0.5, 1.0, 0.5, 0.1);
				Vec3 behind = target.position().subtract(CombatUtil.horizontalForward(target).scale(3.5));
				int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, BlockPos.containing(behind));
				if (Math.abs(y - target.getY()) < 4) {
					host.teleportTo(behind.x, y, behind.z);
					host.faceTarget(target, 360.0F);
				}
				level.sendParticles(ParticleTypes.REVERSE_PORTAL, host.getX(), host.getY(0.5), host.getZ(), 40, 0.5, 1.0, 0.5, 0.1);
				host.playSound(ModSounds.SHARD_WARLORD_BLINK, 1.5F, 1.2F);
			}
			return Result.CONTINUE;
		}
	}
}
