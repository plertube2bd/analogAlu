package com.bossmode.client.renderer;

import com.bossmode.BossMode;
import com.bossmode.entity.sovereign.VoidPylonEntity;
import com.geckolib.model.DefaultedEntityGeoModel;
import com.geckolib.renderer.GeoEntityRenderer;
import com.geckolib.renderer.base.GeoRenderState;
import com.geckolib.renderer.layer.builtin.AutoGlowingGeoLayer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;

/** The Void Shield pylon: a spinning crystal (plain GeckoLib cube model, tools/pylon/gen_void_pylon.py) that glows. */
public class VoidPylonRenderer extends GeoEntityRenderer<VoidPylonEntity, LivingEntityRenderState> {
	public VoidPylonRenderer(EntityRendererProvider.Context context) {
		super(context, new DefaultedEntityGeoModel<>(BossMode.id("void_pylon")));
		this.shadowRadius = 0.5F;
		withRenderLayer(new AutoGlowingGeoLayer<>(this));
	}

	@Override
	protected float getDeathMaxRotation(GeoRenderState renderState) {
		return 0.0F;
	}
}
