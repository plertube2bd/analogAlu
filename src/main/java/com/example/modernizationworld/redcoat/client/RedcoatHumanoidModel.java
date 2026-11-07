package com.example.modernizationworld.redcoat.client;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;

/** Humanoid model using Minecraft 26.3's render-state based model API. */
public final class RedcoatHumanoidModel extends HumanoidModel<RedcoatRenderState> {
    public RedcoatHumanoidModel(ModelPart root) {
        super(root);
    }
}
