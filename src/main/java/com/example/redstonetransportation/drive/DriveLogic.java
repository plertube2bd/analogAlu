package com.example.redstonetransportation.drive;

import com.example.redstonetransportation.vehicle.VehicleDefinition;

/** Pure control logic; a vehicle entity applies the returned command to its movement. */
public final class DriveLogic {
    private DriveLogic() {}

    public static DriveCommand evaluate(
            VehicleDefinition vehicle,
            int redstoneSignal,
            float manualThrottle,
            boolean brake
    ) {
        int signal = Math.clamp(redstoneSignal, 0, 15);
        float manual = Float.isFinite(manualThrottle)
                ? Math.clamp(manualThrottle, 0.0f, 1.0f)
                : 0.0f;

        if (brake) {
            return DriveCommand.stop("BRAKING");
        }

        float throttle;
        switch (vehicle.driveMode()) {
            case IGNORE_SIGNAL -> throttle = manual;
            case ENABLE_WHEN_POWERED -> {
                if (signal < vehicle.minimumSignal()) {
                    return DriveCommand.stop("NO_REDSTONE_SIGNAL");
                }
                throttle = manual;
            }
            case DISABLE_WHEN_POWERED -> {
                if (signal >= vehicle.minimumSignal()) {
                    return DriveCommand.stop("REDSTONE_STOP");
                }
                throttle = manual;
            }
            case SIGNAL_AS_THROTTLE -> {
                if (signal < vehicle.minimumSignal()) {
                    return DriveCommand.stop("SIGNAL_BELOW_MINIMUM");
                }
                throttle = signal / 15.0f;
            }
            default -> {
                return DriveCommand.stop("UNKNOWN_DRIVE_MODE");
            }
        }

        if (throttle <= 0.0f) {
            return DriveCommand.stop("NO_THROTTLE");
        }
        return new DriveCommand(true, throttle, "RUNNING");
    }
}
