package com.example.redstonetransportation.mixin.client;

import com.example.redstonetransportation.client.StructureTargeting;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Aims at the blocks of moving structures, which the game's own aiming cannot see. */
@Mixin(Minecraft.class)
abstract class MinecraftMixin {
    private static final int USE_DELAY = 4;

    @Shadow
    private int rightClickDelay;

    @Inject(method = "pick", at = @At("TAIL"))
    private void redstoneTransportation$aimAtStructures(float partialTicks, CallbackInfo info) {
        StructureTargeting.update((Minecraft) (Object) this, partialTicks);
    }

    @Inject(method = "startUseItem", at = @At("HEAD"), cancellable = true)
    private void redstoneTransportation$useOnStructure(CallbackInfo info) {
        if (StructureTargeting.send((Minecraft) (Object) this, false)) {
            rightClickDelay = USE_DELAY;
            info.cancel();
        }
    }

    @Inject(method = "startAttack", at = @At("HEAD"), cancellable = true)
    private void redstoneTransportation$attackStructure(CallbackInfoReturnable<Boolean> info) {
        if (StructureTargeting.send((Minecraft) (Object) this, true)) info.setReturnValue(true);
    }
}
