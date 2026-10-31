package com.example.analogalu.mixin;

import com.example.analogalu.WaferProgramData;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ResultSlot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ResultSlot.class)
public abstract class ResultSlotMixin {
    @Inject(method = "onTake", at = @At("HEAD"))
    private void analog_alu$preserveWaferProgram(Player player, ItemStack result, CallbackInfo callbackInfo) {
        WaferProgramData.copyFromMenu(player, result);
    }
}
