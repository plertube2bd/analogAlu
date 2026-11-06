package com.example.redstonetransportation.vehicle;

/** Marks this mod's own carts: they may always reach the full rail speed and share a wheel animation. */
public interface TramVehicle {
    /** Half the drawn body length in blocks, which is far longer than the hitbox. */
    double BODY_HALF_LENGTH = 1.15D;

    /** Wheel angle in radians at the given partial tick; only advanced on the client. */
    float wheelAngle(float partialTick);
}
