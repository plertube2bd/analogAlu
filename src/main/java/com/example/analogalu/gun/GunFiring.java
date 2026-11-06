package com.example.analogalu.gun;

import net.minecraft.core.Holder;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Bullets travel instantly along a straight line from the eye, stop at the first solid block, and hit
 * the first thing in the way within the gun's range. Pellets that hit the same target are added into
 * one blow, since a target ignores a second hit in the same instant.
 */
public final class GunFiring {
    private GunFiring() {}

    public static void fire(ServerLevel level, Player shooter, GunType type) {
        Vec3 eye = shooter.getEyePosition();
        // The body's rotation is what the client sends with every move; the head's can lag behind it.
        Vec3 look = Vec3.directionFromRotation(shooter.getXRot(), shooter.getYRot());
        RandomSource random = shooter.getRandom();
        Map<Entity, Float> hits = new LinkedHashMap<>();
        for (int pellet = 0; pellet < type.pellets(); pellet++) {
            Vec3 direction = type.spreadDegrees() > 0.0D ? spread(look, random, type.spreadDegrees()) : look;
            Vec3 end = eye.add(direction.scale(type.range()));
            BlockHitResult block = level.clip(new ClipContext(eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, shooter));
            Vec3 stop = block.getType() == HitResult.Type.MISS ? end : block.getLocation();
            EntityHitResult entity = ProjectileUtil.getEntityHitResult(level, shooter, eye, stop,
                    new AABB(eye, stop).inflate(1.0D), target -> canHit(shooter, target), 0.0F);
            if (entity != null) {
                hits.merge(entity.getEntity(), type.damage(), Float::sum);
                stop = entity.getLocation();
            } else if (block.getType() == HitResult.Type.BLOCK) {
                level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, level.getBlockState(block.getBlockPos())),
                        stop.x, stop.y, stop.z, type.pellets() > 1 ? 1 : 6, 0.05D, 0.05D, 0.05D, 0.1D);
            }
            tracer(level, eye.add(direction.scale(1.2D)), stop, type.pellets() > 1 ? 5.0D : 1.5D);
        }
        Holder<DamageType> bullet = level.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE).getOrThrow(ModGuns.BULLET_DAMAGE);
        DamageSource source = new DamageSource(bullet, shooter, shooter);
        // Bullets bypass the hurt cooldown (damage type tag), so a quick second shot lands in full.
        hits.forEach((target, damage) -> target.hurtServer(level, source, damage));
        muzzle(level, eye, look, type);
    }

    private static boolean canHit(Player shooter, Entity target) {
        return target != shooter && target.isPickable() && !target.isSpectator() && !shooter.isPassengerOfSameVehicle(target);
    }

    /** A direction picked evenly over a cone around {@code look}. */
    static Vec3 spread(Vec3 look, RandomSource random, double halfAngleDegrees) {
        double angle = Math.toRadians(halfAngleDegrees) * Math.sqrt(random.nextDouble());
        double around = random.nextDouble() * Math.PI * 2.0D;
        Vec3 up = Math.abs(look.y) > 0.99D ? new Vec3(1.0D, 0.0D, 0.0D) : new Vec3(0.0D, 1.0D, 0.0D);
        Vec3 side = look.cross(up).normalize();
        Vec3 lift = side.cross(look).normalize();
        return look.scale(Math.cos(angle))
                .add(side.scale(Math.sin(angle) * Math.cos(around)))
                .add(lift.scale(Math.sin(angle) * Math.sin(around)))
                .normalize();
    }

    private static void tracer(ServerLevel level, Vec3 from, Vec3 to, double step) {
        Vec3 path = to.subtract(from);
        double length = path.length();
        if (length <= 0.0D) return;
        Vec3 unit = path.scale(1.0D / length);
        for (double travelled = 0.0D; travelled <= length; travelled += step) {
            Vec3 at = from.add(unit.scale(travelled));
            level.sendParticles(ParticleTypes.CRIT, at.x, at.y, at.z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
        }
    }

    private static void muzzle(ServerLevel level, Vec3 eye, Vec3 look, GunType type) {
        Vec3 side = look.cross(new Vec3(0.0D, 1.0D, 0.0D));
        side = side.lengthSqr() < 1.0e-6 ? Vec3.ZERO : side.normalize();
        // Far enough out that the shooter's own view is not filled with smoke.
        Vec3 at = eye.add(look.scale(2.2D)).add(side.scale(0.25D)).add(0.0D, -0.15D, 0.0D);
        int smoke = type == GunType.MUSKET ? 6 : type == GunType.SHOTGUN ? 4 : 2;
        level.sendParticles(ParticleTypes.SMOKE, at.x, at.y, at.z, smoke, 0.05D, 0.05D, 0.05D, 0.01D);
        level.sendParticles(ParticleTypes.SMALL_FLAME, at.x, at.y, at.z, 1, 0.02D, 0.02D, 0.02D, 0.0D);
        if (type == GunType.MUSKET) {
            level.sendParticles(ParticleTypes.POOF, at.x, at.y, at.z, 2, 0.08D, 0.08D, 0.08D, 0.01D);
        }
        switch (type) {
            case REVOLVER -> {
                level.playSound(null, at.x, at.y, at.z, SoundEvents.FIREWORK_ROCKET_LARGE_BLAST, SoundSource.PLAYERS, 1.4F, 1.5F);
                level.playSound(null, at.x, at.y, at.z, SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 0.35F, 1.9F);
            }
            case MUSKET -> {
                level.playSound(null, at.x, at.y, at.z, SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 0.9F, 1.5F);
                level.playSound(null, at.x, at.y, at.z, SoundEvents.FIREWORK_ROCKET_LARGE_BLAST, SoundSource.PLAYERS, 1.2F, 0.7F);
            }
            case SHOTGUN -> {
                level.playSound(null, at.x, at.y, at.z, SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 1.1F, 1.25F);
                level.playSound(null, at.x, at.y, at.z, SoundEvents.FIREWORK_ROCKET_LARGE_BLAST, SoundSource.PLAYERS, 1.0F, 0.9F);
            }
        }
    }
}
