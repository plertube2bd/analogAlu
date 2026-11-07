package com.bossmode.client.renderer;

import com.bossmode.BossMode;
import com.bossmode.entity.minion.ShardHoundEntity;
import com.geckolib.model.DefaultedEntityGeoModel;
import com.geckolib.renderer.GeoEntityRenderer;
import com.geckolib.renderer.base.GeoRenderState;
import com.geckolib.renderer.layer.builtin.AutoGlowingGeoLayer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;

/**
 * Renderer shared by the three minion types: same Mob.obj model and animations ({@code shard_hound}),
 * per-type texture ({@code shard_hound / shard_spitter / volatile_hound}) with its glowmask.
 */
public class ShardHoundRenderer extends GeoEntityRenderer<ShardHoundEntity, LivingEntityRenderState> {
	public ShardHoundRenderer(EntityRendererProvider.Context context, String texture) {
		super(context, new DefaultedEntityGeoModel<ShardHoundEntity>(BossMode.id("shard_hound")).withAltTexture(BossMode.id(texture)));
		this.shadowRadius = 0.6F;
		withRenderLayer(new AutoGlowingGeoLayer<>(this));
	}

	@Override
	protected float getDeathMaxRotation(GeoRenderState renderState) {
		return 0.0F;
	}
}
