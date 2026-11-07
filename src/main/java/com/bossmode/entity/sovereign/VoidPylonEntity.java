package com.bossmode.entity.sovereign;

import com.bossmode.config.SovereignConfig;
import com.bossmode.entity.ShardFaction;
import com.bossmode.entity.projectile.VoidOrbProjectile;
import com.bossmode.registry.ModSounds;
import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.util.GeckoLibUtil;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.Comparator;
import java.util.UUID;

/**
 * Sovereign Shard (entity id {@code void_pylon}): a floating crystal the Void Sovereign raises with its shield (phases 3,
 * 6 and 9). While any shard stands the Sovereign cannot be hurt. Pylons pour a stream of void energy into it, lob a void orb at the nearest player every few
 * seconds, and only players can break them (arrows included). Removed with the Sovereign.
 */
public class VoidPylonEntity extends Mob implements GeoEntity, ShardFaction {
	private static final RawAnimation SPIN = RawAnimation.begin().thenLoop("animation.void_pylon.idle");
	private static final DustParticleOptions BEAM = new DustParticleOptions(0xFF2D6A, 1.1F);

	private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);
	private @Nullable UUID sovereign;
	private int shotTimer;

	public VoidPylonEntity(EntityType<? extends VoidPylonEntity> type, Level level) {
		super(type, level);
		this.xpReward = 0;
		setNoGravity(true);
		setPersistenceRequired();
		this.shotTimer = SovereignConfig.PYLON_SHOT_INTERVAL / 2 + level.getRandom().nextInt(SovereignConfig.PYLON_SHOT_INTERVAL);
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Mob.createMobAttributes()
				.add(Attributes.MAX_HEALTH, SovereignConfig.PYLON_HEALTH)
				.add(Attributes.ARMOR, 4.0)
				.add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
				.add(Attributes.MOVEMENT_SPEED, 0.0);
	}

	public void setSovereign(VoidSovereignEntity boss) {
		this.sovereign = boss.getUUID();
	}

	private @Nullable VoidSovereignEntity sovereign(ServerLevel level) {
		return this.sovereign != null && level.getEntity(this.sovereign) instanceof VoidSovereignEntity boss && boss.isAlive() ? boss : null;
	}

	@Override
	public void tick() {
		super.tick();
		setDeltaMovement(Vec3.ZERO);
		if (!(level() instanceof ServerLevel level)) {
			return;
		}
		VoidSovereignEntity boss = sovereign(level);
		if (boss == null) {
			if (this.tickCount > 40) {
				level.sendParticles(ParticleTypes.POOF, getX(), getY(0.5), getZ(), 12, 0.3, 0.6, 0.3, 0.05);
				discard();
			}
			return;
		}
		if (this.tickCount % 3 == 0) {
			Vec3 from = position().add(0, getBbHeight() * 0.5, 0);
			Vec3 span = boss.chestPosition().subtract(from);
			int points = (int) Math.min(48, span.length() / 0.7);
			double phase = (this.tickCount % 12) / 12.0;
			for (int i = 0; i < points; i++) {
				double t = (i + phase) / points;
				level.sendParticles(BEAM, from.x + span.x * t, from.y + span.y * t, from.z + span.z * t, 1, 0.02, 0.02, 0.02, 0);
			}
		}
		if (--this.shotTimer <= 0) {
			this.shotTimer = SovereignConfig.PYLON_SHOT_INTERVAL;
			Player player = level.getEntitiesOfClass(Player.class, getBoundingBox().inflate(32.0), p -> p.isAlive() && !p.isSpectator() && !p.isCreative())
					.stream().min(Comparator.comparingDouble(p -> p.distanceToSqr(this))).orElse(null);
			if (player != null) {
				Vec3 from = position().add(0, getBbHeight() * 0.7, 0);
				Vec3 dir = player.getEyePosition().subtract(from).normalize();
				level.addFreshEntity(new VoidOrbProjectile(level, boss, from.add(dir.scale(1.2)), dir.scale(SovereignConfig.ORB_SPEED * 0.8),
						SovereignConfig.PYLON_ORB_DAMAGE, player));
				playSound(ModSounds.VOID_ORB_CAST, 1.5F, 1.3F);
			}
		}
	}

	/** Only players (and their projectiles) can break a pylon. */
	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
		Entity attacker = source.getEntity();
		if (!(attacker instanceof Player) && !source.is(DamageTypes.GENERIC_KILL)) {
			return false;
		}
		boolean hurt = super.hurtServer(level, source, amount);
		if (hurt) {
			level.sendParticles(VoidSovereignEntity.VOID_DUST, getX(), getY(0.5), getZ(), 8, 0.3, 0.6, 0.3, 0);
		}
		return hurt;
	}

	@Override
	public void die(DamageSource source) {
		super.die(source);
		if (level() instanceof ServerLevel level) {
			level.sendParticles(ParticleTypes.EXPLOSION, getX(), getY(0.5), getZ(), 3, 0.4, 0.6, 0.4, 0);
			level.sendParticles(ParticleTypes.REVERSE_PORTAL, getX(), getY(0.5), getZ(), 60, 0.4, 0.8, 0.4, 0.3);
			playSound(SoundEvents.GLASS_BREAK, 2.5F, 0.6F);
			VoidSovereignEntity boss = sovereign(level);
			if (boss != null) {
				boss.onShardBroken(level);
			}
		}
	}

	@Override
	protected void tickDeath() {
		// no death animation: the crystal shatters at once
		if (!level().isClientSide() && !isRemoved()) {
			remove(RemovalReason.KILLED);
		}
	}

	@Override
	protected @Nullable SoundEvent getHurtSound(DamageSource source) {
		return SoundEvents.AMETHYST_BLOCK_HIT;
	}

	@Override
	protected @Nullable SoundEvent getDeathSound() {
		return SoundEvents.AMETHYST_CLUSTER_BREAK;
	}

	@Override
	public boolean isPushable() {
		return false;
	}

	@Override
	protected void doPush(Entity entity) {
	}

	@Override
	public boolean causeFallDamage(double fallDistance, float multiplier, DamageSource source) {
		return false;
	}

	@Override
	public boolean removeWhenFarAway(double distanceToClosestPlayer) {
		return false;
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		super.addAdditionalSaveData(output);
		if (this.sovereign != null) {
			output.store("Sovereign", UUIDUtil.CODEC, this.sovereign);
		}
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		super.readAdditionalSaveData(input);
		this.sovereign = input.read("Sovereign", UUIDUtil.CODEC).orElse(null);
		setNoGravity(true);
	}

	@Override
	public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
		controllers.add(new AnimationController<>("base", 0, test -> test.setAndContinue(SPIN)));
	}

	@Override
	public AnimatableInstanceCache getAnimatableInstanceCache() {
		return this.geoCache;
	}
}
