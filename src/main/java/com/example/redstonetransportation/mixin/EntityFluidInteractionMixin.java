package com.example.redstonetransportation.mixin;

import com.example.redstonetransportation.structure.MovingStructures;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityFluidInteraction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Inside a ship's hull nothing swims or drowns, though the sea runs through the world there. */
@Mixin(EntityFluidInteraction.class)
abstract class EntityFluidInteractionMixin {
    @Inject(method = "update", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;getFluidInteractionBox()Lnet/minecraft/world/phys/AABB;"), cancellable = true)
    private void redstoneTransportation$dryHull(Entity entity, boolean ignoreCurrent, CallbackInfoReturnable<Boolean> info) {
        if (MovingStructures.isInHull(entity.level(), entity.getX(), entity.getY() + 0.1D, entity.getZ())) info.setReturnValue(false);
    }
}
