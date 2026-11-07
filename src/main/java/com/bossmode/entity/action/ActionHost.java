package com.bossmode.entity.action;

import com.bossmode.entity.boss.BossState;
import com.geckolib.animatable.GeoEntity;
import net.minecraft.world.entity.Entity;

/** An entity driven by an {@link ActionController} (the Shard Dragon and the Shard Warlord). */
public interface ActionHost extends GeoEntity {
	BossState getBossState();

	void setBossState(BossState state);

	void faceTarget(Entity target, float maxTurn);

	double getCooldownMultiplier();

	/** Whether the action may be chosen right now (e.g. the dragon's phase pool). */
	boolean isActionAllowed(String id);

	/** GeckoLib controller that plays the actions' triggered animations. */
	default String actionControllerName() {
		return "action";
	}
}
