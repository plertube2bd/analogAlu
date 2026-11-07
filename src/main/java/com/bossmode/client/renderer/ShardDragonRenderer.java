package com.bossmode.client.renderer;

import com.bossmode.client.model.ShardDragonModel;
import com.bossmode.entity.boss.ShardDragonEntity;
import com.geckolib.renderer.GeoEntityRenderer;
import com.geckolib.renderer.base.GeoRenderState;
import com.geckolib.renderer.layer.builtin.AutoGlowingGeoLayer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.world.phys.AABB;

/**
 * GeckoLib renderer for the Shard Dragon. The model is scaled by the entity's SCALE attribute (it grows every
 * phase); GeckoLib's default {@code RenderTypes.entityCutout} is double-sided in 26.3, which the single-sided wing
 * membranes need. Adds the glowmask layer for the crystal veins.
 */
public class ShardDragonRenderer extends GeoEntityRenderer<ShardDragonEntity, LivingEntityRenderState> {
	public ShardDragonRenderer(EntityRendererProvider.Context context) {
		super(context, new ShardDragonModel());
		this.shadowRadius = 1.8F;
		withRenderLayer(new AutoGlowingGeoLayer<>(this));
	}

	@Override
	protected float getDeathMaxRotation(GeoRenderState renderState) {
		return 0.0F;
	}

	@Override
	protected float getShadowRadius(LivingEntityRenderState state) {
		return this.shadowRadius * state.scale;
	}

	/** Wings and tail extend beyond the (square, non-rotating) hitbox. */
	@Override
	protected AABB getBoundingBoxForCulling(ShardDragonEntity entity, float partialTick) {
		double grow = entity.getBbWidth() * 0.6;
		return super.getBoundingBoxForCulling(entity, partialTick).inflate(grow, grow * 0.3, grow);
	}
}
