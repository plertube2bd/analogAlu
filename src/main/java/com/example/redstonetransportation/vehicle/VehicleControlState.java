package com.example.redstonetransportation.vehicle;

public record VehicleControlState(
        int redstoneSignal,
        float manualThrottle,
        boolean brake
) {
    public VehicleControlState {
        redstoneSignal = Math.clamp(redstoneSignal, 0, 15);
        manualThrottle = Float.isFinite(manualThrottle)
                ? Math.clamp(manualThrottle, 0.0f, 1.0f)
                : 0.0f;
    }
}
