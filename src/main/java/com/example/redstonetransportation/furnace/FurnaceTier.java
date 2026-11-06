package com.example.redstonetransportation.furnace;

/**
 * @param number    the tier as shown to players
 * @param speed     how many times faster than the vanilla block
 * @param fuelValue how many times longer each fuel item lasts, measured in items cooked
 */
public enum FurnaceTier {
    ONE(1, 2, 1.5F),
    TWO(2, 4, 2.0F),
    THREE(3, 8, 3.0F);

    private final int number;
    private final int speed;
    private final float fuelValue;

    FurnaceTier(int number, int speed, float fuelValue) {
        this.number = number;
        this.speed = speed;
        this.fuelValue = fuelValue;
    }

    public int number() {
        return number;
    }

    public int speed() {
        return speed;
    }

    public float fuelValue() {
        return fuelValue;
    }
}
