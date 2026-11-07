package com.example.analogalu.gun;

import com.example.analogalu.AnalogAlu;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** What the player did with the gun in the main hand: left-click fires, R cocks the hammer or reloads. */
public record GunActionPayload(Action action) implements CustomPacketPayload {
    public enum Action { FIRE, COCK_OR_RELOAD }

    public static final Type<GunActionPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath(AnalogAlu.MOD_ID, "gun_action"));
    public static final StreamCodec<RegistryFriendlyByteBuf, GunActionPayload> CODEC = ByteBufCodecs.VAR_INT
            .map(index -> new GunActionPayload(Action.values()[Math.clamp(index, 0, Action.values().length - 1)]),
                    payload -> payload.action().ordinal())
            .cast();

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
