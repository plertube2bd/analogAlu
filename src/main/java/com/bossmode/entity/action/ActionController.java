package com.bossmode.entity.action;

import com.bossmode.entity.boss.BossState;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiPredicate;

/**
 * Server-side action state machine. Exactly one action runs at a time; every action ends through {@link #finish},
 * which always applies its cooldown and restores CHASE / IDLE. This rules out overlapping normal + special attacks,
 * cooldown bypasses, duplicate execution and stuck states (every timeline is finite). It is only ticked while the
 * host is alive, so nothing can attack after death.
 */
public final class ActionController<E extends PathfinderMob & ActionHost> {
	private final E host;
	private final List<BossAction<E>> actions;
	private final int[] cooldowns;
	private final List<BossAction<E>> candidates;
	private final BiPredicate<E, LivingEntity> targetFilter;
	private final double normalChance;
	private final int globalAfterNormal;
	private final int globalAfterSpecial;
	private @Nullable BossAction<E> current;
	private ActionContext context = new ActionContext();
	private int actionTick;
	private int globalCooldown;

	public ActionController(E host, List<BossAction<E>> actions, BiPredicate<E, LivingEntity> targetFilter,
			double normalChance, int initialSpecialCooldown, int globalAfterNormal, int globalAfterSpecial) {
		this.host = host;
		this.actions = actions;
		this.cooldowns = new int[actions.size()];
		this.candidates = new ArrayList<>(actions.size());
		this.targetFilter = targetFilter;
		this.normalChance = normalChance;
		this.globalAfterNormal = globalAfterNormal;
		this.globalAfterSpecial = globalAfterSpecial;
		for (int i = 0; i < actions.size(); i++) {
			this.cooldowns[i] = actions.get(i).isSpecial() ? initialSpecialCooldown : 0;
		}
	}

	public boolean isActing() {
		return this.current != null;
	}

	/** Id of the running action or null. */
	public @Nullable String currentActionId() {
		return this.current == null ? null : this.current.id();
	}

	public void tick(ServerLevel level) {
		for (int i = 0; i < this.cooldowns.length; i++) {
			if (this.cooldowns[i] > 0) {
				this.cooldowns[i]--;
			}
		}
		if (this.globalCooldown > 0) {
			this.globalCooldown--;
		}
		LivingEntity target = this.host.getTarget();
		if (this.current != null) {
			tickCurrent(level, target);
			return;
		}
		if (target == null || !target.isAlive() || !this.targetFilter.test(this.host, target)) {
			this.host.setBossState(BossState.IDLE);
			return;
		}
		this.host.setBossState(BossState.CHASE);
		if (this.globalCooldown > 0) {
			return;
		}
		BossAction<E> next = select(level, target);
		if (next != null) {
			start(level, next, target);
		}
	}

	private void tickCurrent(ServerLevel level, @Nullable LivingEntity target) {
		BossAction<E> action = this.current;
		this.host.setBossState(action.stateAt(this.actionTick));
		this.host.getNavigation().stop();
		BossAction.Result result = action.tick(level, this.host, target, this.context, this.actionTick);
		this.actionTick++;
		if (result == BossAction.Result.FINISH) {
			finish(level, false);
			return;
		}
		if (result == BossAction.Result.SKIP_TO_RECOVERY && this.actionTick < action.executionEnd()) {
			this.actionTick = action.executionEnd();
			this.host.stopTriggeredAnim(this.host.actionControllerName(), action.animationTrigger());
		}
		if (this.actionTick >= action.totalTicks() + this.context.extraRecoveryTicks) {
			finish(level, false);
		}
	}

	/**
	 * Candidates must be allowed by the host (pool), off cooldown and pass their own range / sight checks.
	 * A usable normal attack is preferred with {@code normalChance} (always when no special is usable),
	 * otherwise a weighted random special is chosen.
	 */
	private @Nullable BossAction<E> select(ServerLevel level, LivingEntity target) {
		double gap = gap(this.host, target);
		BossAction<E> normal = null;
		this.candidates.clear();
		int totalWeight = 0;
		for (int i = 0; i < this.actions.size(); i++) {
			BossAction<E> action = this.actions.get(i);
			if (this.cooldowns[i] > 0 || !this.host.isActionAllowed(action.id()) || !action.canUse(level, this.host, target, gap)) {
				continue;
			}
			if (!action.isSpecial()) {
				if (normal == null || this.host.getRandom().nextBoolean()) {
					normal = action;
				}
			} else {
				this.candidates.add(action);
				totalWeight += Math.max(1, action.weight(this.host, target));
			}
		}
		if (normal != null && (this.candidates.isEmpty() || this.host.getRandom().nextDouble() < this.normalChance)) {
			return normal;
		}
		if (this.candidates.isEmpty()) {
			return null;
		}
		int roll = this.host.getRandom().nextInt(totalWeight);
		for (BossAction<E> action : this.candidates) {
			roll -= Math.max(1, action.weight(this.host, target));
			if (roll < 0) {
				return action;
			}
		}
		return this.candidates.getLast();
	}

	/** Horizontal distance between the two hitboxes' edges. */
	public static double gap(Entity a, Entity b) {
		double dx = b.getX() - a.getX();
		double dz = b.getZ() - a.getZ();
		return Math.max(0.0, Math.sqrt(dx * dx + dz * dz) - a.getBbWidth() / 2.0 - b.getBbWidth() / 2.0);
	}

	private void start(ServerLevel level, BossAction<E> action, @Nullable LivingEntity target) {
		this.current = action;
		this.context = new ActionContext();
		this.actionTick = 0;
		this.host.getNavigation().stop();
		if (target != null) {
			this.host.faceTarget(target, 45.0F);
		}
		this.host.setBossState(action.stateAt(0));
		this.host.triggerAnim(this.host.actionControllerName(), action.animationTrigger());
		action.start(level, this.host, target, this.context);
	}

	private void finish(ServerLevel level, boolean interrupted) {
		BossAction<E> action = this.current;
		if (action == null) {
			return;
		}
		this.current = null;
		action.stop(level, this.host, this.context, interrupted);
		if (interrupted) {
			this.host.stopTriggeredAnim(this.host.actionControllerName(), action.animationTrigger());
		}
		this.cooldowns[this.actions.indexOf(action)] = Math.max(1, (int) Math.round(action.cooldown() * this.host.getCooldownMultiplier()));
		this.globalCooldown = action.isSpecial() ? this.globalAfterSpecial : this.globalAfterNormal;
		this.context = new ActionContext();
		this.host.setBossState(this.host.getTarget() != null ? BossState.CHASE : BossState.IDLE);
	}

	/** Aborts the running action (phase change, death). Its cooldown is still applied. */
	public void cancel(ServerLevel level) {
		finish(level, true);
	}

	/** Debug / test hook: starts the named action now, ignoring pool, cooldown and range. */
	public boolean forceAction(ServerLevel level, String id) {
		for (BossAction<E> action : this.actions) {
			if (action.id().equals(id)) {
				cancel(level);
				start(level, action, this.host.getTarget());
				return true;
			}
		}
		return false;
	}

	public List<String> actionIds() {
		return this.actions.stream().map(BossAction::id).toList();
	}

	public void save(ValueOutput output) {
		for (int i = 0; i < this.actions.size(); i++) {
			output.putInt(this.actions.get(i).id(), this.cooldowns[i]);
		}
		output.putInt("global", this.globalCooldown);
	}

	public void load(ValueInput input) {
		for (int i = 0; i < this.actions.size(); i++) {
			this.cooldowns[i] = Math.max(0, input.getIntOr(this.actions.get(i).id(), this.cooldowns[i]));
		}
		this.globalCooldown = Math.max(0, input.getIntOr("global", 0));
	}
}
