package com.example.redstonetransportation.block;

import net.minecraft.util.StringRepresentable;

/** How a belt lies, worked out from the belts around it the way a rail's shape is. */
public enum ConveyorShape implements StringRepresentable {
    STRAIGHT("straight"),
    /** Fed from the left of its facing. */
    LEFT("left"),
    /** Fed from the right of its facing. */
    RIGHT("right"),
    /** Fed from both sides, with nothing behind. */
    BOTH("both"),
    /** Climbs to a belt one block higher ahead. */
    UP("up"),
    /** Falls away from a belt one block higher behind. */
    DOWN("down");

    private final String name;

    ConveyorShape(String name) {
        this.name = name;
    }

    public boolean isSlope() {
        return this == UP || this == DOWN;
    }

    @Override
    public String getSerializedName() {
        return name;
    }
}
