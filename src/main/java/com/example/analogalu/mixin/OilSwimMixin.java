package com.example.analogalu.mixin;

import com.example.analogalu.fluid.OilFluid;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class OilSwimMixin extends Entity {
   @Shadow
   protected boolean jumping;

   @Shadow
   public abstract boolean isAffectedByFluids();

   @Shadow
   public abstract boolean isFallFlying();

   @Shadow
   protected abstract double getEffectiveGravity();

   protected OilSwimMixin(EntityType<?> type, Level level) {
      super(type, level);
   }

   @Inject(method = "travel", at = @At("HEAD"), cancellable = true)
   private void analog_alu$swimInOil(Vec3 input, CallbackInfo ci) {
      if (this.isAffectedByFluids() && !this.isFallFlying() && !this.isInWater() && !this.isInLava()) {
         if (!((Object) this instanceof Player player && player.getAbilities().flying)) {
            double depth = OilFluid.submergedDepth(this);
            if (!(depth <= 0.0) && (!this.onGround() || !(depth < 0.4))) {
               double oldY = this.getY();
               boolean deep = depth > 0.4;
               this.moveRelative(deep ? 0.018F : 0.03F, input);
               this.move(MoverType.SELF, this.getDeltaMovement());
               Vec3 movement = this.getDeltaMovement().multiply(0.6, 0.7, 0.6);
               if (this.jumping && deep) {
                  movement = movement.add(0.0, 0.045, 0.0);
               } else {
                  movement = movement.add(0.0, -this.getEffectiveGravity() / 5.0, 0.0);
               }

               if (this.horizontalCollision && this.isFree(movement.x, movement.y + 0.6 - this.getY() + oldY, movement.z)) {
                  movement = new Vec3(movement.x, 0.3, movement.z);
               }

               this.setDeltaMovement(movement);
               this.resetFallDistance();
               ci.cancel();
            }
         }
      }
   }
}
