package com.bossmode.entity;

import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

/** The ways a player can hurt the dragon. Later phases seal them off one by one (see BossPhase). */
public enum DamageKind {
	/** a player's own weapon / fist / mace / spear hit */
	MELEE,
	/** arrows, tridents, fireballs, wind charges... */
	PROJECTILE,
	/** TNT, beds, respawn anchors, end crystals, creepers... */
	EXPLOSION,
	/** splash / lingering potions, thorns, magic */
	MAGIC,
	/** tamed wolves, iron golems and any other creature */
	ALLIES,
	/** lava, lightning, falling anvils, cactus... */
	ENVIRONMENT;

	public int bit() {
		return 1 << ordinal();
	}

	public static DamageKind classify(DamageSource source) {
		if (source.is(DamageTypeTags.IS_EXPLOSION)) {
			return EXPLOSION;
		}
		if (source.is(DamageTypeTags.IS_PROJECTILE)) {
			return PROJECTILE;
		}
		if (source.is(DamageTypeTags.WITCH_RESISTANT_TO) || source.is(DamageTypes.DRAGON_BREATH)) {
			return MAGIC;
		}
		Entity attacker = source.getEntity();
		if (attacker instanceof LivingEntity && !(attacker instanceof Player)) {
			return ALLIES;
		}
		if (attacker instanceof Player && source.getDirectEntity() == attacker) {
			return MELEE;
		}
		return ENVIRONMENT;
	}
}
