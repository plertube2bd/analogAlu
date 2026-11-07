package com.example.analogalu.mixin;

import com.example.analogalu.gun.GunClient;
import com.example.analogalu.gun.GunItem;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Aiming a gun (right-click held) zooms the view in, the musket the most. */
@Mixin(AbstractClientPlayer.class)
public abstract class GunAimFovMixin {
    @Inject(method = "getFieldOfViewModifier", at = @At("RETURN"), cancellable = true)
    private void analog_alu$aimZoom(boolean firstPerson, float effectScale, CallbackInfoReturnable<Float> cir) {
        AbstractClientPlayer player = (AbstractClientPlayer) (Object) this;
        ItemStack held = player.getUseItem();
        if (!firstPerson || !(held.getItem() instanceof GunItem gun) || !GunItem.isAiming(player, held)) return;
        cir.setReturnValue(cir.getReturnValueF() * GunClient.aimZoom(gun.type()));
    }
}
