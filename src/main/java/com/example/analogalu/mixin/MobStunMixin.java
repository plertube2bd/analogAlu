package com.example.analogalu.mixin;

import com.example.analogalu.ModEffects;
import net.minecraft.world.entity.Mob;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Mob.class)
public abstract class MobStunMixin {
   @Inject(method = "serverAiStep", at = @At("HEAD"), cancellable = true)
   private void analog_alu$stunned(CallbackInfo ci) {
      if (((Mob) (Object) this).hasEffect(ModEffects.STUNNED)) {
         ci.cancel();
      }
   }
}
