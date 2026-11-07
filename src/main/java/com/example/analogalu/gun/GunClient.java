package com.example.analogalu.gun;

import com.example.analogalu.AnalogAlu;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;

/**
 * The gun controls on the client. R cocks a revolver or reloads; left-click fires (GunAttackMixin catches
 * the attack before it can mine or hit); holding right-click aims, which zooms the view (GunAimFovMixin).
 */
public final class GunClient {
    public static final KeyMapping ACTION = KeyMappingHelper.registerKeyMapping(new KeyMapping(
            "key.analog_alu.gun_action", InputConstants.KEY_R,
            KeyMapping.Category.register(Identifier.fromNamespaceAndPath(AnalogAlu.MOD_ID, "guns"))));

    /** How far the view zooms in while aiming: the long musket the most. */
    public static float aimZoom(GunType type) {
        return switch (type) {
            case MUSKET -> 0.55F;
            case REVOLVER -> 0.75F;
            case SHOTGUN -> 0.85F;
        };
    }

    private GunClient() {}

    public static void init() {
        // While right-click is held the game drops attack clicks, so a shot while aiming is taken here first.
        ClientTickEvents.START_CLIENT_TICK.register(client -> {
            if (client.player == null || client.gui.screen() != null || !client.player.isUsingItem()
                    || !(client.player.getMainHandItem().getItem() instanceof GunItem)) return;
            while (client.options.keyAttack.consumeClick()) {
                ClientPlayNetworking.send(new GunActionPayload(GunActionPayload.Action.FIRE));
            }
        });
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (ACTION.consumeClick()) {
                if (client.player != null && client.player.getMainHandItem().getItem() instanceof GunItem) {
                    ClientPlayNetworking.send(new GunActionPayload(GunActionPayload.Action.COCK_OR_RELOAD));
                }
            }
        });
    }
}
