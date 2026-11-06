package com.example.redstonetransportation.mixin;

import com.example.redstonetransportation.rail.CartDrive;
import com.example.redstonetransportation.rail.RailDrive;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.vehicle.minecart.AbstractMinecart;
import net.minecraft.world.entity.vehicle.minecart.MinecartFurnace;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Vanilla halves a furnace cart's top speed, which would hold back any train it is coupled into. */
@Mixin(MinecartFurnace.class)
abstract class MinecartFurnaceMixin {
    @Inject(method = "getMaxSpeed", at = @At("HEAD"), cancellable = true)
    private void redstoneTransportation$useDriveLimit(ServerLevel level, CallbackInfoReturnable<Double> info) {
        CartDrive drive = ((AbstractMinecart) (Object) this).getAttached(RailDrive.DRIVE);
        if (drive != null) info.setReturnValue((double) drive.limit());
    }
}
