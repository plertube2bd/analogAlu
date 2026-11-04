package com.example.analogalu.mixin;

import com.example.analogalu.ModItems;
import com.example.analogalu.WaferProgramData;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.FurnaceResultSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.util.Prediction;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FurnaceResultSlot.class)
public abstract class FurnaceResultSlotMixin {
    @Shadow @Final private Player player;
    @Shadow private int removeCount;

    @Inject(method = "onTake", at = @At("HEAD"))
    private void analog_alu$preserveWaferProgram(Player player, ItemStack result, CallbackInfo callbackInfo) {
        WaferProgramData.copyFromMenu(player, result);
    }

    /**
     * 일반 클릭과 Shift+클릭 모두 실제로 꺼낸 개수(removeCount)를 들고 여기를 지난다.
     * onTake는 Shift+클릭이면 이미 인벤토리로 옮겨진 뒤의 빈 스택을 받으므로 거기서는 셀 수 없다.
     */
    @Inject(method = "checkTakeAchievements", at = @At("HEAD"))
    private void analog_alu$giveStrongIonGas(ItemStack carried, CallbackInfo callbackInfo) {
        if (player.level().isClientSide() || removeCount <= 0 || !carried.is(ModItems.ION_IMPLANTED_SILICON)) return;
        ItemStack gas = new ItemStack(ModItems.STRONG_ION_GAS, removeCount);
        if (!player.getInventory().add(gas)) player.drop(gas, false, Prediction.SERVER_ONLY);
    }
}
