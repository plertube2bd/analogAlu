package com.example.redstonetransportation.vehicle;

import com.example.redstonetransportation.drive.DriveCommand;

/** Strategy interface for a vehicle's control behavior. */
public interface VehicleBehavior {
    String id();

    DriveCommand calculate(
            VehicleDefinition definition,
            VehicleControlState controlState
    );
}
