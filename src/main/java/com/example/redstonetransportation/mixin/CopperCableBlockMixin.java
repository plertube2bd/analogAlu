package com.example.redstonetransportation.mixin;

import com.example.analogalu.block.CopperCableBlock;
import com.example.redstonetransportation.integration.analogalu.CableArms;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** The copper cable works out its arms in one place; this adds the arms into the blocks it powers. */
@Mixin(CopperCableBlock.class)
abstract class CopperCableBlockMixin {
    @Inject(method = "withArms", at = @At("RETURN"), cancellable = true)
    private static void redstoneTransportation$reachPoweredBlocks(BlockGetter level, BlockPos pos, BlockState state,
            CallbackInfoReturnable<BlockState> info) {
        info.setReturnValue(CableArms.reach(level, pos, info.getReturnValue()));
    }
}
