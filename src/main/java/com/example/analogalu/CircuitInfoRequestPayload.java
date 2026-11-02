package com.example.analogalu;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record CircuitInfoRequestPayload(long blockPos) implements CustomPacketPayload {
    public static final Type<CircuitInfoRequestPayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(AnalogAlu.MOD_ID, "circuit_info_request"));
    public static final StreamCodec<RegistryFriendlyByteBuf, CircuitInfoRequestPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_LONG, CircuitInfoRequestPayload::blockPos,
            CircuitInfoRequestPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}