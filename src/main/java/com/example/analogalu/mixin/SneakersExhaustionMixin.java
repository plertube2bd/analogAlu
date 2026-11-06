package com.example.analogalu.mixin;

import com.example.analogalu.ModItems;
import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Walking, sprinting and jumping in sneakers cost no hunger. Swimming still does, and so does
 * everything that is not moving (fighting, breaking blocks, healing).
 */
@Mixin(ServerPlayer.class)
public abstract class SneakersExhaustionMixin {
    @WrapWithCondition(method = "checkMovementStatistics",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerPlayer;causeFoodExhaustion(F)V"))
    private boolean analog_alu$sneakersWalkFree(ServerPlayer player, float amount) {
        return !analog_alu$inSneakers(player) || !player.onGround() || player.isInWater() || player.isSwimming();
    }

    @WrapWithCondition(method = "jumpFromGround",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerPlayer;causeFoodExhaustion(F)V"))
    private boolean analog_alu$sneakersJumpFree(ServerPlayer player, float amount) {
        return !analog_alu$inSneakers(player);
    }

    private static boolean analog_alu$inSneakers(ServerPlayer player) {
        return player.getItemBySlot(EquipmentSlot.FEET).is(ModItems.SNEAKERS);
    }
}
