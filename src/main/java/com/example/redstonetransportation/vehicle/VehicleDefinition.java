package com.example.redstonetransportation.vehicle;

import com.example.redstonetransportation.drive.DriveMode;

public record VehicleDefinition(
        String id,
        String behaviorId,
        String displayNameKey,
        double maxSpeed,
        double acceleration,
        int minimumSignal,
        DriveMode driveMode,
        ModelDefinition model
) {
    public VehicleDefinition {
        if (id == null || !id.matches("[a-z0-9_.-]+:[a-z0-9/._-]+")) {
            throw new IllegalArgumentException("Invalid vehicle id: " + id);
        }
        if (behaviorId == null || behaviorId.isBlank()) {
            throw new IllegalArgumentException("behaviorId must not be blank");
        }
        if (!Double.isFinite(maxSpeed) || maxSpeed <= 0) {
            throw new IllegalArgumentException("maxSpeed must be positive and finite");
        }
        if (!Double.isFinite(acceleration) || acceleration <= 0) {
            throw new IllegalArgumentException("acceleration must be positive and finite");
        }
        if (minimumSignal < 0 || minimumSignal > 15) {
            throw new IllegalArgumentException("minimumSignal must be between 0 and 15");
        }
        if (driveMode == null || model == null) {
            throw new IllegalArgumentException("driveMode and model are required");
        }
    }
}
