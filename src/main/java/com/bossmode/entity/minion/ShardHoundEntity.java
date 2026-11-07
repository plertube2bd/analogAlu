package com.bossmode.entity.minion;

import com.bossmode.config.BossConfig;
import com.bossmode.entity.ShardFaction;
import com.bossmode.entity.boss.ShardDragonEntity;
import com.bossmode.entity.boss.combat.CombatUtil;
import com.bossmode.entity.projectile.WyrmShardProjectile;
import com.bossmode.entity.warlord.ShardWarlordEntity;
import com.bossmode.registry.ModEntities;
import com.bossmode.registry.ModSounds;
import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.animation.object.PlayState;
import com.geckolib.animation.state.AnimationTest;
import com.geckolib.util.GeckoLibUtil;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.Vex;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.ExplosionDamageCalculator;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.EnumSet;
import java.util.UUID;

/**
 * The Shard Dragon's minions (Mob.obj). One class, three entity types that share the model and animations:
 * <ul>
 *   <li>{@code shard_hound} - pack hunter, lunging bite</li>
 *   <li>{@code shard_spitter} - keeps 8-16 blocks away and spits Wyrm Shards</li>
 *   <li>{@code volatile_hound} - sprints in, swells for 1.5 s and detonates (never breaks blocks)</li>
 * </ul>
 */
public class ShardHoundEntity extends Monster implements GeoEntity, ShardFaction {
	public enum Variant { HOUND, SPITTER, VOLATILE }

	private static final EntityDataAccessor<Boolean> DATA_EMERGING = SynchedEntityData.defineId(ShardHoundEntity.class, EntityDataSerializers.BOOLEAN);
	private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.shard_hound.idle");
	private static final RawAnimation WALK = RawAnimation.begin().thenLoop("animation.shard_hound.walk");
	private static final RawAnimation RUN = RawAnimation.begin().thenLoop("animation.shard_hound.run");
	private static final RawAnimation SPAWN = RawAnimation.begin().thenPlayAndHold("animation.shard_hound.spawn");
	private static final RawAnimation DEATH = RawAnimation.begin().thenPlayAndHold("animation.shard_hound.death");
	/** Faction-safe blast of a Volatile Hound: hurts players, never other faction members, never the loot. */
	private static final ExplosionDamageCalculator FACTION_SAFE_EXPLOSION = new ExplosionDamageCalculator() {
		@Override
		public boolean shouldDamageEntity(Explosion explosion, Entity entity) {
			return !ShardFaction.isMember(entity) && !(entity instanceof ItemEntity) && super.shouldDamageEntity(explosion, entity);
		}

		@Override
		public float getKnockbackMultiplier(Entity entity) {
			return ShardFaction.isMember(entity) ? 0.0F : super.getKnockbackMultiplier(entity);
		}
	};

	private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);
	private final Variant variant;
	private @Nullable UUID summoner;
	private int emergeTicks;
	private int fuseTicks = -1;

	public ShardHoundEntity(EntityType<? extends ShardHoundEntity> type, Level level) {
		super(type, level);
		this.variant = type == ModEntities.SHARD_SPITTER ? Variant.SPITTER : type == ModEntities.VOLATILE_HOUND ? Variant.VOLATILE : Variant.HOUND;
		this.xpReward = 12;
	}

	public static AttributeSupplier.Builder createHoundAttributes() {
		return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, BossConfig.HOUND_HEALTH).add(Attributes.ATTACK_DAMAGE, BossConfig.HOUND_DAMAGE)
				.add(Attributes.MOVEMENT_SPEED, BossConfig.HOUND_SPEED).add(Attributes.FOLLOW_RANGE, 40.0).add(Attributes.ARMOR, 4.0);
	}

	public static AttributeSupplier.Builder createSpitterAttributes() {
		return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, BossConfig.SPITTER_HEALTH).add(Attributes.ATTACK_DAMAGE, BossConfig.SPITTER_DAMAGE)
				.add(Attributes.MOVEMENT_SPEED, BossConfig.SPITTER_SPEED).add(Attributes.FOLLOW_RANGE, 40.0).add(Attributes.ARMOR, 2.0);
	}

	public static AttributeSupplier.Builder createVolatileAttributes() {
		return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, BossConfig.VOLATILE_HEALTH).add(Attributes.ATTACK_DAMAGE, 1.0)
				.add(Attributes.MOVEMENT_SPEED, BossConfig.VOLATILE_SPEED).add(Attributes.FOLLOW_RANGE, 40.0);
	}

	public Variant getVariant() {
		return this.variant;
	}

	public void setSummoner(Entity entity) {
		this.summoner = entity.getUUID();
	}

	public boolean isSummonedBy(Entity entity) {
		return entity.getUUID().equals(this.summoner);
	}

	public boolean isFusing() {
		return this.fuseTicks >= 0;
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(DATA_EMERGING, true);
	}

	public boolean isEmerging() {
		return this.entityData.get(DATA_EMERGING);
	}

	@Override
	protected void registerGoals() {
		this.goalSelector.addGoal(0, new FloatGoal(this));
		this.goalSelector.addGoal(2, new HoundAttackGoal());
		this.goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.8));
		this.goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 12.0F));
		this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));
		this.targetSelector.addGoal(1, new HurtByTargetGoal(this, ShardDragonEntity.class, ShardHoundEntity.class, ShardWarlordEntity.class, Vex.class));
		this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, 10, true, false, (t, l) -> CombatUtil.isValidTarget(this, t)));
		this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, IronGolem.class, 10, true, false, (t, l) -> CombatUtil.isValidTarget(this, t)));
	}

	@Override
	protected void customServerAiStep(ServerLevel level) {
		super.customServerAiStep(level);
		if (isEmerging()) {
			getNavigation().stop();
			if (++this.emergeTicks == 1) {
				playSound(SoundEvents.AMETHYST_CLUSTER_BREAK, 1.0F, 0.6F);
			}
			if (this.emergeTicks >= BossConfig.HOUND_EMERGE_TICKS) {
				this.entityData.set(DATA_EMERGING, false);
			}
		}
		this.goalSelector.setControlFlag(Goal.Flag.MOVE, !isEmerging() && !isFusing());
		if (isFusing()) {
			tickFuse(level);
		}
	}

	private void tickFuse(ServerLevel level) {
		getNavigation().stop();
		setDeltaMovement(0, getDeltaMovement().y, 0);
		level.sendParticles(ParticleTypes.FLAME, getX(), getY(0.6), getZ(), 2, 0.3, 0.3, 0.3, 0.02);
		if (this.fuseTicks % 6 == 0) {
			level.sendParticles(ParticleTypes.SMOKE, getX(), getY(1.0), getZ(), 4, 0.2, 0.1, 0.2, 0.02);
		}
		if (--this.fuseTicks <= 0) {
			this.fuseTicks = -1;
			level.explode(this, null, FACTION_SAFE_EXPLOSION, getX(), getY(0.5), getZ(), BossConfig.VOLATILE_EXPLOSION_POWER, false, Level.ExplosionInteraction.NONE);
			level.sendParticles(ParticleTypes.LAVA, getX(), getY(0.5), getZ(), 20, 0.5, 0.5, 0.5, 0.2);
			discard();
		}
	}

	private void startFuse() {
		if (!isFusing()) {
			this.fuseTicks = BossConfig.VOLATILE_FUSE_TICKS;
			triggerAnim("action", "fuse");
			playSound(ModSounds.VOLATILE_HOUND_FUSE, 1.5F, 0.8F);
		}
	}

	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
		if (source.getEntity() != null && ShardFaction.isMember(source.getEntity())) {
			return false;
		}
		boolean hurt = super.hurtServer(level, source, amount);
		if (hurt && isAlive()) {
			triggerAnim("hurt", "hurt");
		}
		return hurt;
	}

	@Override
	public boolean isInvulnerableTo(ServerLevel level, DamageSource source) {
		return (isEmerging() && !source.is(net.minecraft.tags.DamageTypeTags.BYPASSES_INVULNERABILITY)) || super.isInvulnerableTo(level, source);
	}

	@Override
	protected SoundEvent getAmbientSound() {
		return ModSounds.SHARD_HOUND_AMBIENT;
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return ModSounds.SHARD_HOUND_HURT;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return ModSounds.SHARD_HOUND_DEATH;
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		super.addAdditionalSaveData(output);
		if (this.summoner != null) {
			output.store("Summoner", UUIDUtil.CODEC, this.summoner);
		}
		output.putBoolean("Emerged", !isEmerging());
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		super.readAdditionalSaveData(input);
		this.summoner = input.read("Summoner", UUIDUtil.CODEC).orElse(null);
		this.entityData.set(DATA_EMERGING, !input.getBooleanOr("Emerged", true));
	}

	// ------------------------------------------------------------------------------------------------ GeckoLib
	@Override
	public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
		controllers.add(new AnimationController<>("base", 4, this::baseAnimation));
		controllers.add(new AnimationController<ShardHoundEntity>("action", 2, test -> PlayState.STOP)
				.triggerableAnim("attack", RawAnimation.begin().thenPlay("animation.shard_hound.attack"))
				.triggerableAnim("spit", RawAnimation.begin().thenPlay("animation.shard_hound.spit"))
				.triggerableAnim("fuse", RawAnimation.begin().thenPlayAndHold("animation.shard_hound.fuse")));
		controllers.add(new AnimationController<ShardHoundEntity>("hurt", 0, test -> PlayState.STOP).additiveAnimations()
				.triggerableAnim("hurt", RawAnimation.begin().thenPlay("animation.shard_hound.hurt")));
	}

	private PlayState baseAnimation(AnimationTest<ShardHoundEntity> test) {
		if (isDeadOrDying()) {
			return test.setAndContinue(DEATH);
		}
		if (isEmerging()) {
			return test.setAndContinue(SPAWN);
		}
		if (test.isMoving()) {
			return test.setAndContinue(isAggressive() ? RUN : WALK);
		}
		return test.setAndContinue(IDLE);
	}

	@Override
	public AnimatableInstanceCache getAnimatableInstanceCache() {
		return this.geoCache;
	}

	/** Variant-specific combat: bite / keep distance and spit / rush and detonate. */
	private final class HoundAttackGoal extends Goal {
		private int cooldown;
		private int windup = -1;
		private int repath;

		HoundAttackGoal() {
			setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
		}

		@Override
		public boolean canUse() {
			LivingEntity target = getTarget();
			return target != null && target.isAlive() && !isEmerging();
		}

		@Override
		public boolean canContinueToUse() {
			return canUse() || this.windup >= 0;
		}

		@Override
		public void start() {
			setAggressive(true);
		}

		@Override
		public void stop() {
			setAggressive(false);
			getNavigation().stop();
			this.windup = -1;
		}

		@Override
		public boolean requiresUpdateEveryTick() {
			return true;
		}

		@Override
		public void tick() {
			LivingEntity target = getTarget();
			if (this.cooldown > 0) {
				this.cooldown--;
			}
			if (target == null || !(level() instanceof ServerLevel level)) {
				return;
			}
			getLookControl().setLookAt(target, 30.0F, 30.0F);
			double distance = distanceTo(target);
			switch (ShardHoundEntity.this.variant) {
				case HOUND -> tickHound(level, target, distance);
				case SPITTER -> tickSpitter(level, target, distance);
				case VOLATILE -> {
					if (distance <= 2.5 || isFusing()) {
						startFuse();
					} else {
						moveTo(target, 1.35);
					}
				}
			}
		}

		private void tickHound(ServerLevel level, LivingEntity target, double distance) {
			if (this.windup >= 0) {
				if (--this.windup == 0 && distanceTo(target) <= 3.2) {
					doHurtTarget(level, target);
					playSound(ModSounds.SHARD_DRAGON_BITE, 0.8F, 1.6F);
				}
				return;
			}
			if (distance <= 2.4 && this.cooldown <= 0) {
				getNavigation().stop();
				triggerAnim("action", "attack");
				this.windup = BossConfig.HOUND_ATTACK_HIT_TICK;
				this.cooldown = 20;
			} else {
				moveTo(target, 1.25);
			}
		}

		private void tickSpitter(ServerLevel level, LivingEntity target, double distance) {
			if (this.windup >= 0) {
				if (--this.windup == 0) {
					Vec3 mouth = position().add(0, getBbHeight() * 0.55, 0).add(com.bossmode.entity.boss.combat.CombatUtil.horizontalForward(ShardHoundEntity.this).scale(0.9));
					Vec3 aim = target.getBoundingBox().getCenter().subtract(mouth).normalize();
					level.addFreshEntity(new WyrmShardProjectile(level, ShardHoundEntity.this, mouth, aim.scale(1.1),
							(float) getAttributeValue(Attributes.ATTACK_DAMAGE)));
					playSound(ModSounds.SHARD_SPITTER_SPIT, 1.0F, 1.2F);
				}
				return;
			}
			if (distance < 7.0) {
				Vec3 away = position().subtract(target.position()).multiply(1, 0, 1).normalize().scale(6.0).add(position());
				getNavigation().moveTo(away.x, away.y, away.z, 1.2);
			} else if (distance > 16.0 || !getSensing().hasLineOfSight(target)) {
				moveTo(target, 1.0);
			} else {
				getNavigation().stop();
				if (this.cooldown <= 0) {
					triggerAnim("action", "spit");
					this.windup = BossConfig.SPITTER_SHOT_TICK;
					this.cooldown = 45 + getRandom().nextInt(20);
				}
			}
		}

		private void moveTo(LivingEntity target, double speed) {
			if (--this.repath <= 0) {
				this.repath = 8;
				getNavigation().moveTo(target, speed);
			}
		}
	}
}
