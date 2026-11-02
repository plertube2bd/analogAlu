package com.example.analogalu;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.core.BlockPos;

public record CircuitLookInfoPayload(long blockPos, String message) implements CustomPacketPayload {
    public static final Type<CircuitLookInfoPayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(AnalogAlu.MOD_ID, "circuit_look_info"));
    public static final StreamCodec<RegistryFriendlyByteBuf, CircuitLookInfoPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_LONG, CircuitLookInfoPayload::blockPos,
            ByteBufCodecs.stringUtf8(512), CircuitLookInfoPayload::message,
            CircuitLookInfoPayload::new);

    public static void send(ServerPlayer player, BlockPos pos, String message) {
        ServerPlayNetworking.send(player, new CircuitLookInfoPayload(pos.asLong(), message));
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}