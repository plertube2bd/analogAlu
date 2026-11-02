package com.example.analogalu;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.BlockHitResult;

@Environment(EnvType.CLIENT)
public final class CircuitInfoHud {
    private static BlockPos targetPos;
    private static long responsePos = Long.MIN_VALUE;
    private static String response = "";
    private static int requestDelay;
    private static boolean showing;

    private CircuitInfoHud() {}

    public static void init() {
        ClientPlayNetworking.registerGlobalReceiver(CircuitLookInfoPayload.TYPE, (payload, context) ->
                context.client().execute(() -> {
                    if (targetPos != null && targetPos.asLong() == payload.blockPos()) {
                        responsePos = payload.blockPos();
                        response = payload.message();
                    }
                }));
        ClientTickEvents.END_CLIENT_TICK.register(CircuitInfoHud::tick);
    }

    private static void tick(Minecraft client) {
        if (client.player == null || client.level == null) {
            reset();
            return;
        }

        BlockPos lookedAt = null;
        if (client.hitResult instanceof BlockHitResult hit) {
            BlockPos hitPos = hit.getBlockPos();
            var state = client.level.getBlockState(hitPos);
            if (state.is(ModBlocks.COPPER_CABLE) || state.getBlock() instanceof com.example.analogalu.block.CircuitDeviceBlock) {
                lookedAt = hitPos.immutable();
            }
        }

        if (lookedAt == null) {
            if (showing) client.player.sendOverlayMessage(Component.empty());
            reset();
            return;
        }

        if (!lookedAt.equals(targetPos)) {
            targetPos = lookedAt;
            responsePos = Long.MIN_VALUE;
            response = "";
            requestDelay = 0;
        }

        if (requestDelay <= 0) {
            ClientPlayNetworking.send(new CircuitInfoRequestPayload(lookedAt.asLong()));
            requestDelay = 10;
        }
        requestDelay--;

        if (responsePos == lookedAt.asLong() && !response.isEmpty()) {
            client.player.sendOverlayMessage(Component.literal(response));
            showing = true;
        }
    }

    private static void reset() {
        targetPos = null;
        responsePos = Long.MIN_VALUE;
        response = "";
        requestDelay = 0;
        showing = false;
    }
}