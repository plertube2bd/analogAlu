package com.example.analogalu.mixin;

import com.example.analogalu.fluid.OilFluid;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.fog.FogData;
import net.minecraft.client.renderer.fog.FogRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** 카메라가 석유 속에 있으면 거의 아무것도 안 보이도록 짙은 검은 안개를 씌운다 (클라이언트 전용). */
@Mixin(FogRenderer.class)
public abstract class OilFogMixin {
    @Inject(method = "setupFog", at = @At("RETURN"))
    private void analog_alu$oilFog(Camera camera, int renderDistance, DeltaTracker deltaTracker, float darkness,
            ClientLevel level, CallbackInfoReturnable<FogData> cir) {
        if (camera.entity() != null && camera.entity().isSpectator()) return;
        if (!OilFluid.isInOil(level, camera.position())) return;
        FogData fog = cir.getReturnValue();
        fog.environmentalStart = 0.0F;
        fog.environmentalEnd = 0.6F;
        fog.skyEnd = 0.6F;
        fog.cloudEnd = 0.6F;
        fog.color.set(0.03F, 0.025F, 0.02F, 1.0F);
    }
}
