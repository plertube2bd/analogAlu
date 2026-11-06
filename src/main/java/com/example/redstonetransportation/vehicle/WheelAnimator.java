package com.example.redstonetransportation.vehicle;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/** Client-side wheel rotation derived from how far the cart actually moved. */
final class WheelAnimator {
    private static final float WHEEL_RADIUS_BLOCKS = 0.125F;

    private Vec3 lastPosition;
    private float angle;
    private float previousAngle;

    void tick(Vec3 position) {
        previousAngle = angle;
        if (lastPosition != null) {
            angle = (angle + (float) position.distanceTo(lastPosition) / WHEEL_RADIUS_BLOCKS) % Mth.TWO_PI;
        }
        lastPosition = position;
    }

    float angle(float partialTick) {
        return Mth.rotLerpRad(partialTick, previousAngle, angle);
    }
}
