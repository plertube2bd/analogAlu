package com.bossmode.entity.raid;

import com.bossmode.entity.minion.MinionType;
import net.minecraft.world.BossEvent;

import java.util.List;
import java.util.Set;

/** One row of a raid boss's phase table (every phase is its own full health pool). */
public interface RaidPhase {
	int number();

	/** Model / hitbox scale while this phase lasts. */
	float scale();

	double speedMultiplier();

	double damageMultiplier();

	double cooldownMultiplier();

	/** ARGB accent of the decorated boss bar. */
	int accentColor();

	BossEvent.BossBarColor barColor();

	/** Action ids the boss may pick in this phase. */
	Set<String> attacks();

	/** Minion types it can call in this phase. */
	List<MinionType> minions();

	int summonCount();

	/** Minions spawned the moment this phase begins. */
	int entryWave();

	/** Shard Warlords spawned the moment this phase begins. */
	int midBosses();

	boolean darkensSky();
}
