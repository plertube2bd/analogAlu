package com.example.redstonetransportation.drive;

public record DriveCommand(boolean enabled, float throttle, String reason) {
    public static DriveCommand stop(String reason) {
        return new DriveCommand(false, 0.0f, reason);
    }
}
