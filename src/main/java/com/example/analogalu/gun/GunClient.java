package com.example.analogalu.gun;

import com.example.analogalu.AnalogAlu;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;

/** The R key: cock a revolver or reload, sent to the server whenever a gun is in the main hand. */
public final class GunClient {
    public static final KeyMapping ACTION = KeyMappingHelper.registerKeyMapping(new KeyMapping(
            "key.analog_alu.gun_action", InputConstants.KEY_R,
            KeyMapping.Category.register(Identifier.fromNamespaceAndPath(AnalogAlu.MOD_ID, "guns"))));

    private GunClient() {}

    public static void init() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (ACTION.consumeClick()) {
                if (client.player != null && client.player.getMainHandItem().getItem() instanceof GunItem) {
                    ClientPlayNetworking.send(new GunActionPayload());
                }
            }
        });
    }
}
