package com.bossmode.entity.sovereign;

import com.bossmode.config.SovereignConfig;
import com.bossmode.entity.boss.combat.CombatUtil;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;

/**
 * Chase movement for the Void Sovereign: like the Shard Dragon it is too big for path finding, so it strides straight
 * at its target with the move control, steps over terrain and crushes what blocks it (mobGriefing). A target it cannot
 * reach is flagged so ranged patterns (orbs, rifts, void step) get preferred.
 */
public class SovereignChaseGoal extends Goal {
	private final VoidSovereignEntity boss;
	private int stuckTicks;

	public SovereignChaseGoal(VoidSovereignEntity boss) {
		this.boss = boss;
		this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
	}

	@Override
	public boolean canUse() {
		LivingEntity target = this.boss.getTarget();
		return target != null && target.isAlive() && this.boss.getBossState().allowsFreeMovement();
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
		boolean moving = this.boss.gapTo(target) > SovereignConfig.CLAW_RANGE * this.boss.sizeFactor() * 0.5;
		if (moving) {
			this.boss.getMoveControl().setWantedPosition(target.getX(), target.getY(), target.getZ(), 1.0);
		} else {
			this.boss.getMoveControl().setWantedPosition(this.boss.getX(), this.boss.getY(), this.boss.getZ(), 0.0);
			this.boss.faceTarget(target, SovereignConfig.MAX_TURN_PER_TICK);
		}
		if (moving && this.boss.horizontalCollision) {
			this.stuckTicks++;
			if (this.stuckTicks % 5 == 0 && this.boss.level() instanceof ServerLevel level) {
				this.boss.crushObstacles(level, CombatUtil.horizontalForward(this.boss));
			}
		} else {
			this.stuckTicks = Math.max(0, this.stuckTicks - 2);
		}
		this.boss.setTargetUnreachable(this.stuckTicks > SovereignConfig.STUCK_TICKS_FOR_UNREACHABLE
				|| Math.abs(target.getY() - this.boss.getY()) > 6.0 * this.boss.sizeFactor());
	}
}
