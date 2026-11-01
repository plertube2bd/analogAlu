package com.example.analogalu.mixin;

import com.example.analogalu.HoverInfoRenderer;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Hud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** HUD를 다 그린 뒤 조준 대상의 정보 창을 덧그린다 (클라이언트 전용). */
@Mixin(Hud.class)
public abstract class HudHoverInfoMixin {
    @Inject(method = "extractRenderState", at = @At("TAIL"))
    private void analog_alu$extractHoverInfo(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker,
            CallbackInfo callbackInfo) {
        HoverInfoRenderer.render(graphics);
    }
}
