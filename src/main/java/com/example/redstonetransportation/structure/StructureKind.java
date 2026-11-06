package com.example.redstonetransportation.structure;

/** How a built structure moves once its core assembles it. */
public enum StructureKind {
    /** Moves straight up and down between floors. */
    ELEVATOR,
    /** Stays at its waterline and sails wherever there is water under it. */
    SHIP,
    /** Flies freely, as long as its balloons can lift it. */
    AIRSHIP,
    /** Drives on land on diesel, falls when nothing holds it up and climbs one block at a time. */
    CAR
}
