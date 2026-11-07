package com.bossmode.entity.boss;

/**
 * Server-authoritative state of the boss, synced to clients through entity data.
 * <pre>
 * SPAWNING -> IDLE <-> CHASE -> ATTACK_PREPARATION -> ATTACKING -> ATTACK_RECOVERY -> CHASE
 *                           \-> SPECIAL_PREPARATION -> SPECIAL_EXECUTION -> SPECIAL_RECOVERY -> CHASE
 * any living state -> PHASE_TRANSITION -> CHASE        any state -> DYING
 * </pre>
 */
public enum BossState {
	SPAWNING,
	IDLE,
	CHASE,
	ATTACK_PREPARATION,
	ATTACKING,
	ATTACK_RECOVERY,
	SPECIAL_PREPARATION,
	SPECIAL_EXECUTION,
	SPECIAL_RECOVERY,
	PHASE_TRANSITION,
	DYING;

	private static final BossState[] VALUES = values();

	public static BossState byId(int id) {
		return id >= 0 && id < VALUES.length ? VALUES[id] : IDLE;
	}

	/** The boss may walk / wander / look around freely only in these states. */
	public boolean allowsFreeMovement() {
		return this == IDLE || this == CHASE;
	}

	/** Damage is ignored while the boss plays its spawn or phase-change roar. */
	public boolean isInvulnerable() {
		return this == SPAWNING || this == PHASE_TRANSITION;
	}

	public boolean isBusy() {
		return !allowsFreeMovement();
	}
}
