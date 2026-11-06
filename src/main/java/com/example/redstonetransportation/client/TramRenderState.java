package com.example.redstonetransportation.client;

import net.minecraft.client.renderer.entity.state.MinecartRenderState;

/** What the tram models need beyond a plain minecart. */
public final class TramRenderState extends MinecartRenderState {
    public float wheelAngle;
    /** Horizontal speed in blocks/tick. */
    public float speed;
    /** +1 when the model's +X end leads, -1 when its -X end leads, 0 when standing still. */
    public int leadingEnd;
}
