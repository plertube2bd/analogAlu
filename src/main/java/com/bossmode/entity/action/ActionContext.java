package com.bossmode.entity.action;

import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Mutable per-action scratch data. A fresh instance is used for every action, so hit bookkeeping can never leak
 * from one attack into the next.
 */
public final class ActionContext {
	/** Entities already damaged by the current hit window - guarantees one hit per entity per window. */
	private final Set<UUID> hit = new HashSet<>();
	public Vec3 direction = Vec3.ZERO;
	public Vec3 origin = Vec3.ZERO;
	public Vec3 destination = Vec3.ZERO;
	public double travelled;
	public int extraRecoveryTicks;
	public final List<Vec3> points = new ArrayList<>();
	public final List<Vec3> directions = new ArrayList<>();
	/** Entities an action keeps track of (e.g. the players held by Soul Chains), parallel to {@link #points} where it matters. */
	public final List<UUID> targets = new ArrayList<>();
	/** Scheduled effects: (tick, position) pairs, e.g. crystal rain impacts. */
	public final List<Scheduled> scheduled = new ArrayList<>();

	public record Scheduled(int tick, Vec3 position) {
	}

	/** @return true the first time an entity is registered in the current window */
	public boolean markHit(UUID id) {
		return this.hit.add(id);
	}

	public void resetHits() {
		this.hit.clear();
	}
}
