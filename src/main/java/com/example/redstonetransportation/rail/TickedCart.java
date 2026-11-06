package com.example.redstonetransportation.rail;

import net.minecraft.world.phys.Vec3;

/**
 * Added to every minecart. Carts move one after another within a tick, so comparing live
 * positions overstates the gap to a car that has already moved. This gives a common reference.
 */
public interface TickedCart {
    /** Where this cart was when the given game tick began for it. */
    Vec3 redstoneTransportation$positionAtTickStart(long gameTime);
}
