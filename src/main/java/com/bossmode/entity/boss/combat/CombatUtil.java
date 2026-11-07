package com.bossmode.entity.boss.combat;

import com.bossmode.entity.ShardFaction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/** Server-side target filtering, bounded area queries and knockback shared by every Shard Dragon faction attack. */
public final class CombatUtil {
	private CombatUtil() {
	}

	/** Whether a faction member may pick {@code target} (used by the targeting goals). */
	public static boolean isValidTarget(Entity attacker, LivingEntity target) {
		return canHit(attacker, target);
	}

	/**
	 * Whether an attack of {@code attacker} may damage {@code entity}: never itself, another faction member (dragon,
	 * hounds, warlords, bound wraiths), team mates, spectators / creative players, dead or non-attackable entities.
	 */
	public static boolean canHit(Entity attacker, Entity entity) {
		if (entity == attacker || !(entity instanceof LivingEntity living) || !living.isAlive() || !living.isAttackable()) {
			return false;
		}
		if (ShardFaction.isMember(living) || attacker.isAlliedTo(living)) {
			return false;
		}
		return !(living instanceof Player player) || !(player.isCreative() || player.isSpectator());
	}

	/** Living entities {@code attacker} may hit whose hitbox centre lies within {@code radius} of {@code center}. */
	public static List<LivingEntity> targetsInRadius(ServerLevel level, Entity attacker, Vec3 center, double radius) {
		AABB box = new AABB(center, center).inflate(radius, Math.max(3.0, radius * 0.75), radius);
		double radiusSqr = radius * radius;
		return level.getEntitiesOfClass(LivingEntity.class, box,
				e -> canHit(attacker, e) && e.getBoundingBox().getCenter().distanceToSqr(center) <= radiusSqr);
	}

	public static List<LivingEntity> targetsInBox(ServerLevel level, Entity attacker, AABB box) {
		return level.getEntitiesOfClass(LivingEntity.class, box, e -> canHit(attacker, e));
	}

	/** Whether {@code target} is within {@code coneDegrees} (full angle) in front of {@code source}. */
	public static boolean isInFront(Entity source, Entity target, double coneDegrees) {
		Vec3 forward = horizontalForward(source);
		Vec3 toTarget = target.position().subtract(source.position()).multiply(1, 0, 1);
		if (toTarget.lengthSqr() < 1.0E-4) {
			return true;
		}
		return forward.dot(toTarget.normalize()) >= Math.cos(Math.toRadians(coneDegrees / 2.0));
	}

	public static Vec3 horizontalForward(Entity entity) {
		float yaw = entity.getYRot() * Mth.DEG_TO_RAD;
		return new Vec3(-Mth.sin(yaw), 0, Mth.cos(yaw));
	}

	/** Pushes {@code target} along the horizontal direction {@code (dirX, dirZ)}, honouring knockback resistance. */
	public static void knockback(LivingEntity target, double dirX, double dirZ, double horizontal, double vertical) {
		double scale = 1.0 - Mth.clamp(target.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE), 0.0, 1.0);
		if (scale <= 0) {
			return;
		}
		double length = Math.sqrt(dirX * dirX + dirZ * dirZ);
		if (length < 1.0E-4) {
			dirX = target.getRandom().nextDouble() - 0.5;
			dirZ = target.getRandom().nextDouble() - 0.5;
			length = Math.sqrt(dirX * dirX + dirZ * dirZ);
		}
		Vec3 current = target.getDeltaMovement();
		target.setDeltaMovement(current.x * 0.5 + dirX / length * horizontal * scale, Math.max(current.y, 0) + vertical * scale,
				current.z * 0.5 + dirZ / length * horizontal * scale);
		target.needsSync = true; // sends the new velocity to the client (players)
	}

	public static void knockbackFrom(LivingEntity target, Vec3 source, double horizontal, double vertical) {
		knockback(target, target.getX() - source.x, target.getZ() - source.z, horizontal, vertical);
	}

	/** Top of the terrain at a column (leaves ignored). */
	public static double groundY(net.minecraft.server.level.ServerLevel level, double x, double z) {
		return level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Mth.floor(x), Mth.floor(z));
	}

	/** Pulls {@code target} toward {@code point} (tractor beam). */
	public static void pull(LivingEntity target, Vec3 point, double strength) {
		double scale = 1.0 - Mth.clamp(target.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE), 0.0, 1.0);
		Vec3 dir = point.subtract(target.position());
		if (scale <= 0 || dir.lengthSqr() < 1.0E-4) {
			return;
		}
		target.setDeltaMovement(target.getDeltaMovement().add(dir.normalize().scale(strength * scale)));
		target.needsSync = true;
	}
}
