package com.example.redstonetransportation.rail;

import com.example.redstonetransportation.RedstoneTransportation;
import com.example.redstonetransportation.block.DriveRailBlock;
import com.example.redstonetransportation.vehicle.TrainLinkManager;
import com.example.redstonetransportation.vehicle.TramVehicle;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.vehicle.minecart.AbstractMinecart;
import net.minecraft.world.entity.vehicle.minecart.NewMinecartBehavior;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.PoweredRailBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;

/**
 * Rail driving for every minecart type. A drive rail hands a cart a cruise speed, and the cart
 * then holds that speed along any rail until another drive rail changes it.
 */
public final class RailDrive {
    /** Motor IV maximum, 103 m/s at 20 ticks/second. */
    public static final double MAX_SPEED = 103.0D / 20.0D;
    /** A drive rail at signal 1 runs a little under vanilla rail speed. */
    public static final double DRIVE_RAIL_LOW_SPEED = 7.0D / 20.0D;
    /** A drive rail at signal 15 matches Motor I; going faster takes the higher motors. */
    public static final double DRIVE_RAIL_TOP_SPEED = 21.0D / 20.0D;
    private static final int VANILLA_SPEED_LIMIT = 8;
    private static final double VANILLA_SPEED = VANILLA_SPEED_LIMIT / 20.0D;
    private static final double ACCELERATION = 0.085D;
    private static final double BRAKING = 0.18D;
    private static final double FOLLOWER_ACCELERATION = 0.3D;
    private static final double FOLLOWER_BRAKING = 0.5D;
    private static final double STOPPED = 0.001D;
    /** Room above the cruise speed for a coupled car to close up on the car ahead. */
    private static final double COUPLING_MARGIN = 1.1D;
    /** Below this speed a coupled car still takes its direction from the car it follows. */
    private static final double STARTING_SPEED = 0.3D;

    public static final AttachmentType<CartDrive> DRIVE = AttachmentRegistry.createPersistent(
            Identifier.fromNamespaceAndPath(RedstoneTransportation.MOD_ID, "cart_drive"), CartDrive.CODEC);

    private RailDrive() {}

    public static void initialize() {
        // Triggers this class's static attachment registration.
    }

    /** The vanilla lookup throws here because the max-speed game rule needs the experimental feature flag. */
    public static double maxSpeed(AbstractMinecart cart, ServerLevel level) {
        CartDrive drive = cart.getAttached(DRIVE);
        if (cart instanceof TramVehicle) return drive == null ? MAX_SPEED : Math.max(MAX_SPEED, drive.limit());

        int rule = level.enabledFeatures().contains(FeatureFlags.MINECART_IMPROVEMENTS)
                ? level.getGameRules().get(GameRules.MAX_MINECART_SPEED)
                : VANILLA_SPEED_LIMIT;
        double base = rule * (cart.isInWater() ? 0.5D : 1.0D) / 20.0D;
        return drive == null ? base : Math.max(base, drive.limit());
    }

    /** Runs once per tick before the cart moves along its rail. */
    public static void tick(AbstractMinecart cart, ServerLevel level) {
        CartDrive drive = cart.getAttached(DRIVE);
        if (drive == null) return;

        Vec3 movement = cart.getDeltaMovement();
        double speed = movement.horizontalDistance();
        if (drive.cruise() <= 0.0F || !cart.isOnRails()) {
            // Coasting: release the raised cap as soon as the cart is back under vanilla speed.
            if (speed <= VANILLA_SPEED) {
                cart.removeAttached(DRIVE);
            } else if (drive.cruise() > 0.0F || drive.limit() > speed) {
                cart.setAttached(DRIVE, CartDrive.coasting((float) speed));
            }
            return;
        }

        Vec3 direction = speed > STOPPED
                ? new Vec3(movement.x / speed, 0.0D, movement.z / speed)
                : new Vec3(drive.headingX(), 0.0D, drive.headingZ());

        double target = drive.cruise();
        double acceleration = ACCELERATION;
        double braking = BRAKING;
        AbstractMinecart front = drive.follows().map(id -> TrainLinkManager.findNearby(level, cart, id)).orElse(null);
        if (front != null) {
            // A following car matches the car ahead, not the cruise speed, so it can never pass it.
            Vec3 toFront = front.position().subtract(cart.position()).horizontal();
            // Only while starting off: at speed, a tight bend can briefly put the front car behind this one.
            if (speed < STARTING_SPEED && toFront.dot(direction) < 0.0D) direction = direction.reverse();
            double frontSpeed = front.getDeltaMovement().horizontalDistance();
            target = Math.clamp(frontSpeed + TrainLinkManager.gapCorrection(cart, front), 0.0D, drive.limit());
            acceleration = FOLLOWER_ACCELERATION;
            braking = FOLLOWER_BRAKING;
        }
        if (direction.lengthSqr() < 0.5D) return;

        double next = speed < target ? Math.min(target, speed + acceleration) : Math.max(target, speed - braking);
        cart.setDeltaMovement(direction.x * next, movement.y, direction.z * next);
    }

    public static boolean isCruising(AbstractMinecart cart) {
        CartDrive drive = cart.getAttached(DRIVE);
        return drive != null && drive.cruise() > 0.0F;
    }

    /** Runs for every rail block a cart steps onto. Returns true when the cart is held on this rail. */
    public static boolean onRailStep(AbstractMinecart cart, BlockPos pos) {
        if (!(cart.level() instanceof ServerLevel level)) return false;

        BlockState state = level.getBlockState(pos);
        if (state.is(Blocks.POWERED_RAIL) && !state.getValue(PoweredRailBlock.POWERED)) {
            stopCruise(cart);
            return false;
        }
        if (!(state.getBlock() instanceof DriveRailBlock)) return false;

        DriveInput input = DriveInput.read(level, pos);
        if (input.brake() || input.speed() <= 0) {
            hold(cart, pos, state, level);
            return true;
        }
        Vec3 heading = DriveRailBlock.heading(state, input.reverse());
        setCruise(cart, cruiseSpeed(input.speed()), heading, level);
        return false;
    }

    /** Stays above {@link #STARTING_SPEED}, or a cruising cart would count as standing at the next drive rail. */
    public static double cruiseSpeed(int signal) {
        return DRIVE_RAIL_LOW_SPEED + (DRIVE_RAIL_TOP_SPEED - DRIVE_RAIL_LOW_SPEED) * (signal - 1) / 14.0D;
    }

    /** Lets a motor push any cart past the vanilla speed limit. */
    public static void allowSpeed(AbstractMinecart cart, double speed) {
        CartDrive drive = cart.getAttached(DRIVE);
        if (drive == null) {
            cart.setAttached(DRIVE, CartDrive.coasting((float) speed));
        } else if (drive.limit() < speed) {
            cart.setAttached(DRIVE, new CartDrive(drive.cruise(), (float) speed, drive.headingX(), drive.headingZ(), drive.follows()));
        }
    }

    private static void setCruise(AbstractMinecart cart, double speed, Vec3 heading, ServerLevel level) {
        CartDrive current = cart.getAttached(DRIVE);
        if (current != null && current.cruise() == (float) speed) return;

        // A moving train keeps going its way; only a standing one departs along the rail's arrow.
        // Coupling springs leave a standing car with a little drift, which must not count as moving.
        Vec3 movement = cart.getDeltaMovement().horizontal();
        boolean moving = movement.length() >= STARTING_SPEED;
        Vec3 travel = moving ? movement.normalize() : heading;
        float limit = (float) (speed + COUPLING_MARGIN);
        TrainLinkManager.walkFromFront(cart, travel, level, (car, ahead) -> {
            if (!moving) car.setDeltaMovement(Vec3.ZERO);
            car.setAttached(DRIVE, new CartDrive((float) speed, limit, (float) travel.x, (float) travel.z,
                    Optional.ofNullable(ahead).map(AbstractMinecart::getUUID)));
        });
    }

    private static void stopCruise(AbstractMinecart cart) {
        CartDrive drive = cart.getAttached(DRIVE);
        if (drive != null && drive.cruise() > 0.0F) {
            cart.setAttached(DRIVE, CartDrive.coasting(drive.limit()));
        }
    }

    private static void hold(AbstractMinecart cart, BlockPos pos, BlockState state, ServerLevel level) {
        TrainLinkManager.forEachCar(cart, level, car -> {
            car.setDeltaMovement(Vec3.ZERO);
            car.removeAttached(DRIVE);
        });

        Vec3 center = Vec3.atBottomCenterOf(pos);
        if (cart.position().horizontal().distanceToSqr(center.horizontal()) < 0.0001D) return;
        cart.setPos(center.x, cart.getY(), center.z);
        if (cart.getBehavior() instanceof NewMinecartBehavior behavior) {
            behavior.adjustToRails(pos, state, false);
        }
    }
}
