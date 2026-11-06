package com.example.analogalu.gun;

import com.example.analogalu.AnalogAlu;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** The R key on a gun in the main hand: cock the hammer or reload. */
public record GunActionPayload() implements CustomPacketPayload {
    public static final Type<GunActionPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath(AnalogAlu.MOD_ID, "gun_action"));
    public static final StreamCodec<RegistryFriendlyByteBuf, GunActionPayload> CODEC = StreamCodec.unit(new GunActionPayload());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
