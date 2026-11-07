package com.example.modernizationworld.redcoat;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

/** Marker effect consumed by the raid manager when its bearer enters a village. */
public final class RedcoatOmenEffect extends MobEffect {
    public RedcoatOmenEffect() {
        super(MobEffectCategory.HARMFUL, 0x8E1B1B);
    }
}
