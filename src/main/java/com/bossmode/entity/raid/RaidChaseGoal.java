package com.bossmode.entity.raid;

import com.bossmode.entity.boss.combat.CombatUtil;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;

/**
 * Ground chase of the raid bosses: too big for path finding, they stride straight at the target with the move control,
 * step over terrain and crush what blocks them (mobGriefing). A target out of reach is flagged so ranged patterns get
 * preferred. Idle while {@link RaidBossEntity#canChase()} says no (e.g. the Tempest Wyvern is airborne).
 */
public class RaidChaseGoal extends Goal {
	private final RaidBossEntity<?> boss;
	private int stuckTicks;

	public RaidChaseGoal(RaidBossEntity<?> boss) {
		this.boss = boss;
		this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
	}

	@Override
	public boolean canUse() {
		LivingEntity target = this.boss.getTarget();
		return target != null && target.isAlive() && this.boss.getBossState().allowsFreeMovement() && this.boss.canChase();
	}

	@Override
	public boolean canContinueToUse() {
		return canUse();
	}

	@Override
	public void start() {
		this.boss.setAggressive(true);
		this.stuckTicks = 0;
	}

	@Override
	public void stop() {
		this.boss.setAggressive(this.boss.getTarget() != null);
		this.boss.getNavigation().stop();
		this.boss.getMoveControl().setWantedPosition(this.boss.getX(), this.boss.getY(), this.boss.getZ(), 0.0);
	}

	@Override
	public boolean requiresUpdateEveryTick() {
		return true;
	}

	@Override
	public void tick() {
		LivingEntity target = this.boss.getTarget();
		if (target == null) {
			return;
		}
		this.boss.getLookControl().setLookAt(target, 30.0F, 30.0F);
		boolean moving = this.boss.gapTo(target) > this.boss.chaseStopGap();
		if (moving) {
			this.boss.getMoveControl().setWantedPosition(target.getX(), target.getY(), target.getZ(), 1.0);
		} else {
			this.boss.getMoveControl().setWantedPosition(this.boss.getX(), this.boss.getY(), this.boss.getZ(), 0.0);
			this.boss.faceTarget(target, this.boss.spec().maxTurn());
		}
		if (moving && this.boss.horizontalCollision) {
			this.stuckTicks++;
			if (this.stuckTicks % 5 == 0 && this.boss.level() instanceof ServerLevel level) {
				this.boss.crushObstacles(level, CombatUtil.horizontalForward(this.boss));
			}
		} else {
			this.stuckTicks = Math.max(0, this.stuckTicks - 2);
		}
		this.boss.setTargetUnreachable(this.stuckTicks > 40 || Math.abs(target.getY() - this.boss.getY()) > 6.0 * this.boss.sizeFactor());
	}
}
