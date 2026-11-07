package com.example.analogalu.mixin;

import com.example.analogalu.gun.GunActionPayload;
import com.example.analogalu.gun.GunItem;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * With a gun in the main hand, left-click fires it instead of punching or mining: the click goes to the
 * server as a fire action, and holding the button down does not start breaking the block in front.
 */
@Mixin(Minecraft.class)
public abstract class GunAttackMixin {
    @Shadow
    @Nullable
    public LocalPlayer player;

    @Inject(method = "startAttack", at = @At("HEAD"), cancellable = true)
    private void analog_alu$fireInsteadOfAttacking(CallbackInfoReturnable<Boolean> cir) {
        if (player == null || !(player.getMainHandItem().getItem() instanceof GunItem)) return;
        ClientPlayNetworking.send(new GunActionPayload(GunActionPayload.Action.FIRE));
        cir.setReturnValue(false);
    }

    @Inject(method = "continueAttack", at = @At("HEAD"), cancellable = true)
    private void analog_alu$noMiningWithAGun(boolean attacking, CallbackInfo ci) {
        if (player != null && player.getMainHandItem().getItem() instanceof GunItem) ci.cancel();
    }
}
