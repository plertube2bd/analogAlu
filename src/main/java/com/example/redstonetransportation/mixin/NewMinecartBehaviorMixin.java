package com.example.redstonetransportation.mixin;

import com.example.redstonetransportation.rail.RailDrive;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.vehicle.minecart.AbstractMinecart;
import net.minecraft.world.entity.vehicle.minecart.MinecartBehavior;
import net.minecraft.world.entity.vehicle.minecart.NewMinecartBehavior;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Replaces the game-rule lookup, which is only registered when the experimental flag is on. */
@Mixin(NewMinecartBehavior.class)
abstract class NewMinecartBehaviorMixin extends MinecartBehavior {
    private NewMinecartBehaviorMixin(AbstractMinecart minecart) {
        super(minecart);
    }

    @Inject(method = "getMaxSpeed", at = @At("HEAD"), cancellable = true)
    private void redstoneTransportation$maxSpeed(ServerLevel level, CallbackInfoReturnable<Double> info) {
        info.setReturnValue(RailDrive.maxSpeed(minecart, level));
    }

    /** Each cart type has its own friction, and a lit furnace cart loses 20% a tick; a cruising cart ignores all of it. */
    @WrapOperation(method = "calculateTrackSpeed", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/vehicle/minecart/AbstractMinecart;applyNaturalSlowdown(Lnet/minecraft/world/phys/Vec3;)Lnet/minecraft/world/phys/Vec3;"))
    private Vec3 redstoneTransportation$noFrictionWhileCruising(AbstractMinecart cart, Vec3 movement, Operation<Vec3> original) {
        return RailDrive.isCruising(cart) ? movement : original.call(cart, movement);
    }
}
