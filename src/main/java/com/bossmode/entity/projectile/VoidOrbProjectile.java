package com.bossmode.entity.projectile;

import com.bossmode.config.SovereignConfig;
import com.bossmode.entity.ShardFaction;
import com.bossmode.entity.boss.combat.CombatUtil;
import com.bossmode.entity.sovereign.VoidSovereignEntity;
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
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Void orb of the Void Sovereign and its pylons: slow, homing (limited turn rate - sidestep it), bursts on impact and
 * hurts everything within {@link SovereignConfig#ORB_BLAST_RADIUS}. No block damage. It can be punched back like a
 * ghast fireball; a returned orb stops homing and can hurt the Sovereign.
 */
public class VoidOrbProjectile extends Fireball {
	private int life;
	private float damage = SovereignConfig.ORB_DAMAGE;
	private @Nullable LivingEntity homingTarget;

	public VoidOrbProjectile(EntityType<? extends VoidOrbProjectile> type, Level level) {
		super(type, level);
		this.accelerationPower = 0.0;
		setItem(new ItemStack(ModItems.VOID_ORB));
	}

	public VoidOrbProjectile(Level level, LivingEntity owner, Vec3 position, Vec3 velocity, float damage, @Nullable LivingEntity target) {
		super(ModEntities.VOID_ORB, owner, velocity, level);
		this.accelerationPower = 0.0;
		this.damage = damage;
		this.homingTarget = target;
		setItem(new ItemStack(ModItems.VOID_ORB));
		snapTo(position.x, position.y, position.z, getYRot(), getXRot());
		setDeltaMovement(velocity);
	}

	@Override
	protected boolean shouldBurn() {
		return false;
	}

	@Override
	protected ParticleOptions getTrailParticle() {
		return ParticleTypes.REVERSE_PORTAL;
	}

	@Override
	protected float getInertia() {
		return 1.0F;
	}

	@Override
	public void tick() {
		if (!level().isClientSide()) {
			Entity owner = getOwner();
			LivingEntity target = this.homingTarget;
			if (target != null && target.isAlive() && owner != null && ShardFaction.isMember(owner)) {
				Vec3 velocity = getDeltaMovement();
				double speed = Math.max(0.05, velocity.length());
				Vec3 wanted = target.getBoundingBox().getCenter().subtract(position()).normalize();
				setDeltaMovement(velocity.normalize().lerp(wanted, SovereignConfig.ORB_TURN).normalize().scale(speed));
				this.needsSync = true;
			}
			if (++this.life > SovereignConfig.ORB_LIFETIME) {
				burst();
				return;
			}
		}
		super.tick();
	}

	@Override
	protected boolean canHitEntity(Entity entity) {
		if (!super.canHitEntity(entity) || entity instanceof VoidOrbProjectile) {
			return false;
		}
		Entity owner = getOwner();
		return owner == null || !ShardFaction.isMember(owner) || CombatUtil.canHit(owner, entity);
	}

	@Override
	protected void onHit(HitResult result) {
		super.onHit(result);
		if (level() instanceof ServerLevel) {
			burst();
		}
	}

	private void burst() {
		if (!(level() instanceof ServerLevel level) || isRemoved()) {
			return;
		}
		Entity owner = getOwner();
		LivingEntity attacker = owner instanceof LivingEntity living ? living : null;
		for (LivingEntity victim : level.getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(SovereignConfig.ORB_BLAST_RADIUS),
				e -> e.isAlive() && e.distanceTo(this) <= SovereignConfig.ORB_BLAST_RADIUS + e.getBbWidth() * 0.5)) {
			boolean friendly = owner != null && ShardFaction.isMember(owner) && !CombatUtil.canHit(owner, victim);
			if (friendly || (victim instanceof VoidSovereignEntity && owner == victim)) {
				continue;
			}
			if (victim.hurtServer(level, damageSources().indirectMagic(this, attacker), this.damage)) {
				CombatUtil.knockbackFrom(victim, position(), 0.6, 0.3);
			}
		}
		level.sendParticles(ParticleTypes.SQUID_INK, getX(), getY(), getZ(), 14, 0.4, 0.4, 0.4, 0.08);
		level.sendParticles(VoidSovereignEntity.VOID_DUST, getX(), getY(), getZ(), 12, 0.6, 0.6, 0.6, 0);
		level.sendParticles(ParticleTypes.REVERSE_PORTAL, getX(), getY(), getZ(), 20, 0.3, 0.3, 0.3, 0.2);
		level.playSound(null, getX(), getY(), getZ(), ModSounds.VOID_ORB_BURST, getSoundSource(), 1.6F, 0.8F + this.random.nextFloat() * 0.3F);
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
		this.damage = input.getFloatOr("Damage", SovereignConfig.ORB_DAMAGE);
	}
}
