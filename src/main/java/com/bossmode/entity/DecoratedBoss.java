package com.bossmode.entity;

/**
 * Data the client HUD needs to draw the decorated boss bar of an entity. Everything is derived from synced entity
 * data, so the HUD never needs custom packets: the boss bar's UUID equals the entity's UUID.
 */
public interface DecoratedBoss {
	enum BarStyle {
		/** the Shard Dragon: phase pips, sealed-damage icons, portrait */
		MAIN,
		/** mid-bosses: compact frame */
		MID
	}

	BarStyle barStyle();

	/** Current phase (1-based) or 0 when the boss has no phases. */
	int barPhase();

	int barMaxPhase();

	boolean barEnraged();

	/** True while the boss is open to damage in a phase that requires it (phase 10). */
	boolean barExposed();

	/** True when this phase only accepts damage while exposed. */
	boolean barRequiresExposure();

	/** Bit mask of {@link DamageKind#bit()} the boss currently accepts. */
	int barAllowedDamage();

	/** ARGB accent colour for the frame and bar. */
	int barAccentColor();

	/** Resource path (without namespace) of the 64x64 portrait under textures/gui, or null. */
	String barPortrait();
}
