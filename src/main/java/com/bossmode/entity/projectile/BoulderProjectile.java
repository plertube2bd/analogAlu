package com.bossmode.entity.projectile;

import com.bossmode.config.BastionConfig;
import com.bossmode.entity.ShardFaction;
import com.bossmode.entity.boss.combat.CombatUtil;
import com.bossmode.registry.ModEntities;
import com.bossmode.registry.ModSounds;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Molten boulder lobbed by the Bastion Behemoth. A heavy arc (server-simulated, drawn by {@code ThrownItemRenderer} as a
 * big magma block) that bursts on the first thing it touches: damage and fire in a radius around the impact, no block
 * damage. Never hurts the bosses' side.
 */
public class BoulderProjectile extends ThrowableItemProjectile {
	private int life;
	private float damage = BastionConfig.BOULDER_DAMAGE;

	public BoulderProjectile(EntityType<? extends BoulderProjectile> type, Level level) {
		super(type, level);
	}

	public BoulderProjectile(Level level, LivingEntity owner, Vec3 position, Vec3 velocity, float damage) {
		super(ModEntities.BOULDER, owner, level, new ItemStack(Items.MAGMA_BLOCK));
		this.damage = damage;
		snapTo(position.x, position.y, position.z, owner.getYRot(), 0.0F);
		setDeltaMovement(velocity);
	}

	/**
	 * Launch velocity that lands on {@code target} after {@code ticks} ticks, accounting for this projectile's gravity
	 * and the 0.99 air drag of throwables.
	 */
	public static Vec3 ballisticVelocity(Vec3 from, Vec3 target, int ticks) {
		double d = 0.99;
		double g = BastionConfig.BOULDER_GRAVITY;
		double sum = d * (1 - Math.pow(d, ticks)) / (1 - d);
		double vx = (target.x - from.x) / sum;
		double vz = (target.z - from.z) / sum;
		double vy = (target.y - from.y + g * d / (1 - d) * (ticks - sum)) / sum;
		return new Vec3(vx, vy, vz);
	}

	@Override
	protected Item getDefaultItem() {
		return Items.MAGMA_BLOCK;
	}

	@Override
	protected double getDefaultGravity() {
		return BastionConfig.BOULDER_GRAVITY;
	}

	@Override
	public void tick() {
		super.tick();
		if (level() instanceof ServerLevel level) {
			if (this.tickCount % 2 == 0) {
				level.sendParticles(ParticleTypes.LARGE_SMOKE, getX(), getY(), getZ(), 2, 0.3, 0.3, 0.3, 0.01);
				level.sendParticles(ParticleTypes.FLAME, getX(), getY(), getZ(), 3, 0.4, 0.4, 0.4, 0.01);
			}
			if (++this.life > BastionConfig.BOULDER_LIFETIME) {
				discard();
			}
		}
	}

	@Override
	protected boolean canHitEntity(Entity entity) {
		if (!super.canHitEntity(entity)) {
			return false;
		}
		Entity owner = getOwner();
		return owner == null || !ShardFaction.isMember(owner) || CombatUtil.canHit(owner, entity);
	}

	@Override
	protected void onHit(HitResult result) {
		super.onHit(result);
		if (!(level() instanceof ServerLevel level) || isRemoved()) {
			return;
		}
		Vec3 at = result.getLocation();
		Entity owner = getOwner();
		double radius = BastionConfig.BOULDER_RADIUS;
		level.sendParticles(ParticleTypes.EXPLOSION, at.x, at.y + 0.5, at.z, 3, radius * 0.3, 0.3, radius * 0.3, 0);
		level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.MAGMA_BLOCK.defaultBlockState()), at.x, at.y + 0.5, at.z, 50,
				radius * 0.4, 0.4, radius * 0.4, 0.3);
		level.sendParticles(ParticleTypes.LAVA, at.x, at.y + 0.5, at.z, 12, radius * 0.4, 0.3, radius * 0.4, 0);
		level.playSound(null, at.x, at.y, at.z, ModSounds.BOULDER_IMPACT, getSoundSource(), 3.0F, 0.8F + this.random.nextFloat() * 0.2F);
		Entity attacker = owner != null ? owner : this;
		for (LivingEntity victim : CombatUtil.targetsInRadius(level, attacker, at.add(0, 0.5, 0), radius)) {
			if (victim.hurtServer(level, damageSources().mobProjectile(this, owner instanceof LivingEntity living ? living : null), this.damage)) {
				victim.igniteForSeconds(3.0F);
				CombatUtil.knockbackFrom(victim, at, 0.9, 0.4);
			}
		}
		discard();
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
		this.damage = input.getFloatOr("Damage", BastionConfig.BOULDER_DAMAGE);
	}
}
