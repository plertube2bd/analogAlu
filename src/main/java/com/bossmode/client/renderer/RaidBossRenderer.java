package com.bossmode.client.renderer;

import com.bossmode.BossMode;
import com.bossmode.entity.raid.RaidBossEntity;
import com.geckolib.model.DefaultedEntityGeoModel;
import com.geckolib.renderer.GeoEntityRenderer;
import com.geckolib.renderer.base.GeoRenderState;
import com.geckolib.renderer.layer.builtin.AutoGlowingGeoLayer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.world.phys.AABB;

/**
 * Renderer for the raid bosses (Tempest Wyvern, Bastion Behemoth - baked by the poly-mesh loader from BOSS2 / BOSS3.obj):
 * scaled by their SCALE attribute (they grow every phase), with the glowmask layer for their crystal veins.
 */
public class RaidBossRenderer<T extends RaidBossEntity<T>> extends GeoEntityRenderer<T, LivingEntityRenderState> {
	/** How far wings, tails and snouts reach outside the hitbox, as a fraction of its width. */
	private final double cullingGrow;

	public RaidBossRenderer(EntityRendererProvider.Context context, String id, float shadowRadius, double cullingGrow) {
		super(context, new DefaultedEntityGeoModel<>(BossMode.id(id)));
		this.shadowRadius = shadowRadius;
		this.cullingGrow = cullingGrow;
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

	@Override
	protected AABB getBoundingBoxForCulling(T entity, float partialTick) {
		double grow = entity.getBbWidth() * this.cullingGrow;
		return super.getBoundingBoxForCulling(entity, partialTick).inflate(grow, grow * 0.3, grow);
	}
}
