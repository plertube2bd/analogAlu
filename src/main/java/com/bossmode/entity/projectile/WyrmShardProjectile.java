package com.bossmode.entity.projectile;

import com.bossmode.config.BossConfig;
import com.bossmode.entity.ShardFaction;
import com.bossmode.entity.boss.combat.CombatUtil;
import com.bossmode.registry.ModEntities;
import com.bossmode.registry.ModItems;
import com.bossmode.registry.ModSounds;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.hurtingprojectile.Fireball;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Crystal shard spat by the Shard Volley (and, with the storm feather item, the Tempest Wyvern's Feather Barrage). Spawned and simulated on the server (vanilla entity tracking syncs it to
 * clients, where {@code ThrownItemRenderer} draws the Wyrm Shard item). Straight, gravity-less flight at constant
 * speed, single target, no block damage, removed on impact or after {@link BossConfig#PROJECTILE_LIFETIME} ticks.
 * Players can punch it back like a ghast fireball, after which it can hurt the boss.
 */
public class WyrmShardProjectile extends Fireball {
	private int life;
	private float damage = BossConfig.PROJECTILE_DAMAGE;

	public WyrmShardProjectile(EntityType<? extends WyrmShardProjectile> type, Level level) {
		super(type, level);
		this.accelerationPower = 0.0;
		setItem(new ItemStack(ModItems.WYRM_SHARD));
	}

	public WyrmShardProjectile(Level level, LivingEntity owner, Vec3 position, Vec3 velocity, float damage) {
		super(ModEntities.WYRM_SHARD, owner, velocity, level);
		this.accelerationPower = 0.0;
		this.damage = damage;
		setItem(new ItemStack(ModItems.WYRM_SHARD));
		snapTo(position.x, position.y, position.z, getYRot(), getXRot());
		setDeltaMovement(velocity);
	}

	@Override
	protected boolean shouldBurn() {
		return false;
	}

	@Override
	protected ParticleOptions getTrailParticle() {
		return getItem().is(ModItems.STORM_FEATHER) ? ParticleTypes.CLOUD : ParticleTypes.END_ROD;
	}

	@Override
	protected float getInertia() {
		return 1.0F; // constant speed
	}

	@Override
	public void tick() {
		if (BossConfig.PROJECTILE_GRAVITY) {
			setDeltaMovement(getDeltaMovement().add(0, -0.03, 0));
		}
		super.tick();
		if (!level().isClientSide() && ++this.life > BossConfig.PROJECTILE_LIFETIME) {
			discard();
		}
	}

	@Override
	protected boolean canHitEntity(Entity entity) {
		if (!super.canHitEntity(entity)) {
			return false;
		}
		// a shard fired by the dragon's side never hits the dragon, its minions or its mid-bosses
		Entity owner = getOwner();
		return owner == null || !ShardFaction.isMember(owner) || CombatUtil.canHit(owner, entity);
	}

	@Override
	protected void onHitEntity(EntityHitResult result) {
		super.onHitEntity(result);
		if (level() instanceof ServerLevel level) {
			Entity target = result.getEntity();
			Entity owner = getOwner();
			target.hurtServer(level, damageSources().mobProjectile(this, owner instanceof LivingEntity living ? living : null), this.damage);
		}
	}

	@Override
	protected void onHit(HitResult result) {
		super.onHit(result);
		if (level() instanceof ServerLevel level) {
			if (getItem().is(ModItems.STORM_FEATHER)) {
				// the Tempest Wyvern's feathers burst in sparks and wind
				level.sendParticles(ParticleTypes.ELECTRIC_SPARK, getX(), getY(), getZ(), 10, 0.2, 0.2, 0.2, 0.2);
				level.sendParticles(ParticleTypes.CLOUD, getX(), getY(), getZ(), 4, 0.1, 0.1, 0.1, 0.05);
			} else {
				level.sendParticles(ParticleTypes.END_ROD, getX(), getY(), getZ(), 12, 0.2, 0.2, 0.2, 0.15);
				level.sendParticles(ParticleTypes.WITCH, getX(), getY(), getZ(), 6, 0.2, 0.2, 0.2, 0.0);
			}
			level.playSound(null, getX(), getY(), getZ(), ModSounds.WYRM_SHARD_IMPACT, getSoundSource(), 1.5F, 0.9F + this.random.nextFloat() * 0.3F);
			discard();
		}
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		super.addAdditionalSaveData(output);
		output.putInt("Life", this.life);
		output.putFloat("Damage", this.damage);
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		super.readAdditionalSaveData(input);
		this.life = input.getIntOr("Life", 0);
		this.damage = input.getFloatOr("Damage", BossConfig.PROJECTILE_DAMAGE);
	}
}
