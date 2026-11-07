package com.bossmode.entity;

import com.bossmode.entity.minion.MinionType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.Vex;

/**
 * Marker for the bosses' side (bosses, hounds, warlords, bound wraiths and tagged vanilla minions such as the Tempest
 * Wyvern's breezes). Members never hurt or target each other.
 */
public interface ShardFaction {
	static boolean isMember(Entity entity) {
		return entity instanceof ShardFaction || (entity instanceof Vex vex && vex.getOwner() instanceof ShardFaction)
				|| entity.entityTags().contains(MinionType.FACTION_TAG);
	}
}
