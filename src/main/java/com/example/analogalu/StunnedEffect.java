package com.example.analogalu;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

public final class StunnedEffect extends MobEffect {
   public StunnedEffect() {
      super(MobEffectCategory.HARMFUL, 10148095, ParticleTypes.ELECTRIC_SPARK);
   }

   public boolean shouldApplyEffectTickThisTick(int tickCount, int amplification) {
      return true;
   }

   public boolean applyEffectTick(ServerLevel level, LivingEntity entity, int amplification) {
      entity.xxa = 0.0F;
      entity.zza = 0.0F;
      entity.setJumping(false);
      entity.setSprinting(false);
      Vec3 motion = entity.getDeltaMovement();
      entity.setDeltaMovement(0.0, Math.min(motion.y, 0.0), 0.0);
      return true;
   }
}
