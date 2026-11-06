package com.example.redstonetransportation.structure;

import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.phys.Vec3;

/**
 * Where a structure stands: its core's position, and how far it has turned from the way it was
 * built. "Local" coordinates are the block coordinates it was built with, relative to the core.
 */
public record StructurePose(Vec3 position, float turned, double cos, double sin) {
    private static final float SQUARE_TOLERANCE = 0.05F;

    public static StructurePose of(Vec3 position, float turned) {
        double radians = turned * Mth.DEG_TO_RAD;
        return new StructurePose(position, turned, Math.cos(radians), Math.sin(radians));
    }

    public double worldX(double x, double z) {
        return position.x + x * cos - z * sin;
    }

    public double worldZ(double x, double z) {
        return position.z + x * sin + z * cos;
    }

    public double localX(double worldX, double worldZ) {
        return (worldX - position.x) * cos + (worldZ - position.z) * sin;
    }

    public double localZ(double worldX, double worldZ) {
        return -(worldX - position.x) * sin + (worldZ - position.z) * cos;
    }

    /** The nearest whole number of clockwise quarter turns. */
    public int quarters() {
        return Mth.positiveModulo(Math.round(turned / 90.0F), 4);
    }

    public boolean isSquare() {
        return Math.abs(Mth.wrapDegrees(turned - quarters() * 90.0F)) < SQUARE_TOLERANCE;
    }

    public Rotation rotation() {
        return switch (quarters()) {
            case 1 -> Rotation.CLOCKWISE_90;
            case 2 -> Rotation.CLOCKWISE_180;
            case 3 -> Rotation.COUNTERCLOCKWISE_90;
            default -> Rotation.NONE;
        };
    }

    public Rotation inverseRotation() {
        return switch (quarters()) {
            case 1 -> Rotation.COUNTERCLOCKWISE_90;
            case 2 -> Rotation.CLOCKWISE_180;
            case 3 -> Rotation.CLOCKWISE_90;
            default -> Rotation.NONE;
        };
    }

    public Direction toWorld(Direction local) {
        return rotation().rotate(local);
    }
}
