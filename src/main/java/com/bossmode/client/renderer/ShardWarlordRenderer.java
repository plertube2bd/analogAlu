package com.bossmode.client.renderer;

import com.bossmode.BossMode;
import com.bossmode.entity.warlord.ShardWarlordEntity;
import com.geckolib.model.DefaultedEntityGeoModel;
import com.geckolib.renderer.GeoEntityRenderer;
import com.geckolib.renderer.base.GeoRenderState;
import com.geckolib.renderer.layer.builtin.AutoGlowingGeoLayer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.world.phys.AABB;

/** Renderer for the Shard Warlord mid-boss (Mob2.obj), scaled by its SCALE attribute (1.5). */
public class ShardWarlordRenderer extends GeoEntityRenderer<ShardWarlordEntity, LivingEntityRenderState> {
	public ShardWarlordRenderer(EntityRendererProvider.Context context) {
		super(context, new DefaultedEntityGeoModel<>(BossMode.id("shard_warlord")));
		this.shadowRadius = 0.7F;
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

	/** The staff and pauldrons stick out of the narrow hitbox. */
	@Override
	protected AABB getBoundingBoxForCulling(ShardWarlordEntity entity, float partialTick) {
		return super.getBoundingBoxForCulling(entity, partialTick).inflate(entity.getBbWidth() * 0.6, 0.2, entity.getBbWidth() * 0.6);
	}
}
