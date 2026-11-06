package com.example.redstonetransportation.vehicle;

import com.example.redstonetransportation.drive.DriveCommand;
import com.example.redstonetransportation.drive.DriveLogic;

/** Shared control behavior for vehicles that follow a rail path. */
public final class RailVehicleBehavior implements VehicleBehavior {
    @Override
    public String id() {
        return "rail_vehicle";
    }

    @Override
    public DriveCommand calculate(
            VehicleDefinition definition,
            VehicleControlState controlState
    ) {
        return DriveLogic.evaluate(
                definition,
                controlState.redstoneSignal(),
                controlState.manualThrottle(),
                controlState.brake()
        );
    }
}
