package com.example.redstonetransportation.mixin;

import com.example.redstonetransportation.rail.RailDrive;
import com.example.redstonetransportation.rail.TickedCart;
import com.example.redstonetransportation.vehicle.TrainLinkManager;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.vehicle.minecart.AbstractMinecart;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.properties.RailShape;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Puts every minecart type on the step-based rail physics and hooks in the drive rails. */
@Mixin(AbstractMinecart.class)
abstract class AbstractMinecartMixin implements TickedCart {
    @Unique
    private long redstoneTransportation$lastTick;
    @Unique
    private Vec3 redstoneTransportation$tickStart = Vec3.ZERO;

    @Override
    public Vec3 redstoneTransportation$positionAtTickStart(long gameTime) {
        AbstractMinecart cart = (AbstractMinecart) (Object) this;
        return redstoneTransportation$lastTick == gameTime ? redstoneTransportation$tickStart : cart.position();
    }

    @Inject(method = "tick", at = @At("HEAD"))
    private void redstoneTransportation$markTickStart(CallbackInfo info) {
        AbstractMinecart cart = (AbstractMinecart) (Object) this;
        redstoneTransportation$lastTick = cart.level().getGameTime();
        redstoneTransportation$tickStart = cart.position();
    }

    /** The legacy physics caps carts at 2 blocks/tick and derails them on curves above 1 block/tick. */
    @Inject(method = "useExperimentalMovement", at = @At("HEAD"), cancellable = true)
    private static void redstoneTransportation$useStepPhysics(Level level, CallbackInfoReturnable<Boolean> info) {
        info.setReturnValue(true);
    }

    @Inject(method = "moveAlongTrack", at = @At("HEAD"))
    private void redstoneTransportation$applyDrive(ServerLevel level, CallbackInfo info) {
        RailDrive.tick((AbstractMinecart) (Object) this, level);
    }

    @Inject(method = "makeStepAlongTrack", at = @At("HEAD"), cancellable = true)
    private void redstoneTransportation$readRail(BlockPos pos, RailShape shape, double movementLeft,
                                                 CallbackInfoReturnable<Double> info) {
        if (RailDrive.onRailStep((AbstractMinecart) (Object) this, pos)) info.setReturnValue(0.0D);
    }

    /** Vanilla carts lose 80% of their speed when they touch, which would stop a coupled train in every bend. */
    @Inject(method = "canCollideWith", at = @At("HEAD"), cancellable = true)
    private void redstoneTransportation$ignoreCoupledCars(Entity entity, CallbackInfoReturnable<Boolean> info) {
        if (TrainLinkManager.isCoupled((AbstractMinecart) (Object) this, entity)) info.setReturnValue(false);
    }

    @Inject(method = "push(Lnet/minecraft/world/entity/Entity;)V", at = @At("HEAD"), cancellable = true)
    private void redstoneTransportation$noPushBetweenCoupledCars(Entity entity, CallbackInfo info) {
        if (TrainLinkManager.isCoupled((AbstractMinecart) (Object) this, entity)) info.cancel();
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void redstoneTransportation$tickLinks(CallbackInfo info) {
        AbstractMinecart cart = (AbstractMinecart) (Object) this;
        if (cart.level() instanceof ServerLevel serverLevel) TrainLinkManager.tick(cart, serverLevel);
    }
}
