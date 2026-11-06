package com.example.analogalu.mixin;

import com.example.analogalu.block.AsphaltBlock;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractClientPlayer.class)
public abstract class AsphaltFovMixin {
   private static final float ASPHALT_FOV = 1.1F;

   @Inject(method = "getFieldOfViewModifier", at = @At("RETURN"), cancellable = true)
   private void analog_alu$asphaltFov(boolean firstPerson, float effectScale, CallbackInfoReturnable<Float> cir) {
      AbstractClientPlayer player = (AbstractClientPlayer) (Object) this;
      if (!player.isScoping() && !player.getAbilities().flying) {
         Vec3 motion = player.getDeltaMovement();
         if (!(motion.x * motion.x + motion.z * motion.z < 0.0025)) {
            BlockPos feet = BlockPos.containing(player.getX(), player.getY() - 0.5, player.getZ());
            boolean onAsphalt = player.level().getBlockState(feet).getBlock() instanceof AsphaltBlock
               || !player.onGround() && player.level().getBlockState(feet.below()).getBlock() instanceof AsphaltBlock;
            if (onAsphalt) {
               cir.setReturnValue(cir.getReturnValueF() * (1.0F + 0.100000024F * effectScale));
            }
         }
      }
   }
}
