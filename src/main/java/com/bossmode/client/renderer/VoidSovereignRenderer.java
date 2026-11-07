package com.bossmode.client.renderer;

import com.bossmode.BossMode;
import com.bossmode.entity.sovereign.VoidSovereignEntity;
import com.geckolib.model.DefaultedEntityGeoModel;
import com.geckolib.renderer.GeoEntityRenderer;
import com.geckolib.renderer.base.GeoRenderState;
import com.geckolib.renderer.layer.builtin.AutoGlowingGeoLayer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.world.phys.AABB;

/**
 * Renderer for the Void Sovereign (Final BOSS.obj, baked by the poly-mesh loader): scaled by its SCALE attribute (it grows
 * every phase), with the glowmask layer for its crimson veins, claws and crown.
 */
public class VoidSovereignRenderer extends GeoEntityRenderer<VoidSovereignEntity, LivingEntityRenderState> {
	public VoidSovereignRenderer(EntityRendererProvider.Context context) {
		super(context, new DefaultedEntityGeoModel<>(BossMode.id("void_sovereign")));
		this.shadowRadius = 2.6F;
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

	/** Tentacles, claws and tail reach far outside the hitbox. */
	@Override
	protected AABB getBoundingBoxForCulling(VoidSovereignEntity entity, float partialTick) {
		double grow = entity.getBbWidth() * 0.5;
		return super.getBoundingBoxForCulling(entity, partialTick).inflate(grow, grow * 0.3, grow);
	}
}
