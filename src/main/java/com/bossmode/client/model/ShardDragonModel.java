package com.bossmode.client.model;

import com.bossmode.BossMode;
import com.bossmode.entity.boss.ShardDragonEntity;
import com.geckolib.model.DefaultedEntityGeoModel;

/**
 * Resolves the Shard Dragon's assets:
 * <ul>
 *   <li>model: {@code assets/boss_mode/geckolib/models/entity/shard_dragon.geo.json} (baked by {@link PolyMeshGeoLoader})</li>
 *   <li>animations: {@code assets/boss_mode/geckolib/animations/entity/shard_dragon.animation.json}</li>
 *   <li>texture: {@code assets/boss_mode/textures/entity/shard_dragon.png} (+ {@code _glowmask.png})</li>
 * </ul>
 */
public class ShardDragonModel extends DefaultedEntityGeoModel<ShardDragonEntity> {
	public ShardDragonModel() {
		super(BossMode.id("shard_dragon"));
	}
}
