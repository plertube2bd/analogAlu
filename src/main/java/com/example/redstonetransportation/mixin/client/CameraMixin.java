package com.example.redstonetransportation.mixin.client;

import com.example.redstonetransportation.structure.MovingStructures;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.world.level.material.FogType;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Below the waterline inside a hull, the view stays clear of the sea the hull is sitting in. */
@Mixin(Camera.class)
abstract class CameraMixin {
    @Shadow
    public abstract Vec3 position();

    @Inject(method = "getFluidInCamera", at = @At("RETURN"), cancellable = true)
    private void redstoneTransportation$dryHull(CallbackInfoReturnable<FogType> info) {
        var level = Minecraft.getInstance().level;
        Vec3 camera = position();
        if (info.getReturnValue() == FogType.WATER && level != null && MovingStructures.isInHull(level, camera.x, camera.y, camera.z)) {
            info.setReturnValue(FogType.NONE);
        }
    }
}
