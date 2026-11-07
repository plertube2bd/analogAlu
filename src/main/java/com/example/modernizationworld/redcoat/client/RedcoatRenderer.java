package com.example.modernizationworld.redcoat.client;

import com.example.modernizationworld.redcoat.RedcoatSoldierEntity;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.resources.Identifier;

/** Skin-based renderer for the redcoat infantry. */
public final class RedcoatRenderer extends HumanoidMobRenderer<RedcoatSoldierEntity, RedcoatRenderState, RedcoatHumanoidModel> {
    public RedcoatRenderer(EntityRendererProvider.Context context) {
        super(context, new RedcoatHumanoidModel(context.bakeLayer(ModelLayers.PLAYER)), 0.5F);
        this.addLayer(new ItemInHandLayer<>(this));
    }

    @Override
    public RedcoatRenderState createRenderState() {
        return new RedcoatRenderState();
    }

    @Override
    public void extractRenderState(RedcoatSoldierEntity entity, RedcoatRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.texture = Identifier.fromNamespaceAndPath("modernization_world",
                "textures/entity/redcoat/" + entity.kind().id() + ".png");
    }

    @Override
    public Identifier getTextureLocation(RedcoatRenderState state) {
        return state.texture;
    }
}
