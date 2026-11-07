package com.bossmode.entity.boss.ai;

import com.bossmode.config.BossConfig;
import com.bossmode.entity.boss.ShardDragonEntity;
import com.bossmode.entity.boss.combat.CombatUtil;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;

/**
 * Chase movement for the (huge, growing) Shard Dragon. Vanilla path finding cannot fit a 6-12 block wide body
 * through terrain, so the dragon steers straight at its target with the move control, climbs with its large step
 * height and - when the mobGriefing rule allows it - crushes blocks in its way. If it stays blocked, or the target
 * is far above / below it, the target is flagged unreachable so ranged patterns get preferred. In the air (phases
 * 6-10) the dragon's own flight control steers instead.
 */
public class DragonChaseGoal extends Goal {
	private final ShardDragonEntity boss;
	private int stuckTicks;

	public DragonChaseGoal(ShardDragonEntity boss) {
		this.boss = boss;
		this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
	}

	@Override
	public boolean canUse() {
		LivingEntity target = this.boss.getTarget();
		return target != null && target.isAlive() && this.boss.getBossState().allowsFreeMovement() && !this.boss.isFlying();
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
		boolean moving = this.boss.gapTo(target) > BossConfig.BITE_RANGE * this.boss.sizeFactor() * 0.5;
		if (moving) {
			this.boss.getMoveControl().setWantedPosition(target.getX(), target.getY(), target.getZ(), 1.0);
		} else {
			this.boss.getMoveControl().setWantedPosition(this.boss.getX(), this.boss.getY(), this.boss.getZ(), 0.0);
			this.boss.faceTarget(target, BossConfig.MAX_TURN_PER_TICK);
		}
		if (moving && this.boss.horizontalCollision) {
			this.stuckTicks++;
			if (this.stuckTicks % 5 == 0 && this.boss.level() instanceof ServerLevel level) {
				this.boss.crushObstacles(level, CombatUtil.horizontalForward(this.boss));
			}
		} else {
			this.stuckTicks = Math.max(0, this.stuckTicks - 2);
		}
		this.boss.setTargetUnreachable(this.stuckTicks > BossConfig.STUCK_TICKS_FOR_UNREACHABLE
				|| Math.abs(target.getY() - this.boss.getY()) > 6.0 * this.boss.sizeFactor());
	}
}
