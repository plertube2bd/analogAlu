package com.bossmode.entity.action;

import com.bossmode.entity.boss.BossState;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import org.jspecify.annotations.Nullable;

/**
 * One action with a fixed, animation-synchronised timeline:
 * <pre>
 * tick 0 .. prepareTicks-1                    PREPARATION (telegraph, turn toward the target)
 * prepareTicks .. prepareTicks+activeTicks-1  EXECUTION   (server-side hit detection happens only here)
 * .. totalTicks-1                             RECOVERY    (the boss is EXPOSED)
 * </pre>
 * Instances are stateless templates shared by every entity; per-use state lives in {@link ActionContext}.
 */
public abstract class BossAction<E extends PathfinderMob & ActionHost> {
	public enum Result {
		CONTINUE,
		/** jump straight to the recovery part of the timeline (e.g. a charge hit a wall) */
		SKIP_TO_RECOVERY,
		FINISH
	}

	private final String id;
	private final boolean special;
	private final int prepareTicks;
	private final int activeTicks;
	private final int totalTicks;

	protected BossAction(String id, boolean special, int prepareTicks, int activeTicks, int totalTicks) {
		this.id = id;
		this.special = special;
		this.prepareTicks = prepareTicks;
		this.activeTicks = activeTicks;
		this.totalTicks = totalTicks;
	}

	public String id() {
		return this.id;
	}

	public boolean isSpecial() {
		return this.special;
	}

	public int prepareTicks() {
		return this.prepareTicks;
	}

	public int executionEnd() {
		return this.prepareTicks + this.activeTicks;
	}

	public int totalTicks() {
		return this.totalTicks;
	}

	public BossState stateAt(int tick) {
		if (tick < this.prepareTicks) {
			return this.special ? BossState.SPECIAL_PREPARATION : BossState.ATTACK_PREPARATION;
		}
		if (tick < executionEnd()) {
			return this.special ? BossState.SPECIAL_EXECUTION : BossState.ATTACKING;
		}
		return this.special ? BossState.SPECIAL_RECOVERY : BossState.ATTACK_RECOVERY;
	}

	public abstract int cooldown();

	/** Selection weight among usable specials (normal attacks ignore it). */
	public int weight(E host, LivingEntity target) {
		return 10;
	}

	/** Range / line of sight / other checks. Only called when the pool and cooldown already allow the action. */
	public abstract boolean canUse(ServerLevel level, E host, LivingEntity target, double gap);

	/** GeckoLib trigger name played on the host's action controller. */
	public abstract String animationTrigger();

	public void start(ServerLevel level, E host, @Nullable LivingEntity target, ActionContext ctx) {
	}

	public abstract Result tick(ServerLevel level, E host, @Nullable LivingEntity target, ActionContext ctx, int tick);

	/** Always called exactly once when the action ends, finished or interrupted. */
	public void stop(ServerLevel level, E host, ActionContext ctx, boolean interrupted) {
	}
}
