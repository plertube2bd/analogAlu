package com.example.analogalu.mixin;

import com.example.analogalu.ModItems;
import com.example.analogalu.WaferProgramData;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.FurnaceResultSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.util.Prediction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FurnaceResultSlot.class)
public abstract class FurnaceResultSlotMixin {
    @Inject(method = "onTake", at = @At("HEAD"))
    private void analog_alu$giveStrongIonGas(Player player, ItemStack result, CallbackInfo callbackInfo) {
        WaferProgramData.copyFromMenu(player, result);
        if (player.level().isClientSide() || !result.is(ModItems.ION_IMPLANTED_SILICON)) return;
        ItemStack gas = new ItemStack(ModItems.STRONG_ION_GAS, result.getCount());
        if (!player.getInventory().add(gas)) player.drop(gas, false, Prediction.SERVER_ONLY);
    }
}