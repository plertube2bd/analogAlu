package com.example.analogalu;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;

public record CircuitStatusPayload(String itemId, String text) implements CustomPacketPayload {
    public static final Type<CircuitStatusPayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(AnalogAlu.MOD_ID, "circuit_status"));
    public static final StreamCodec<RegistryFriendlyByteBuf, CircuitStatusPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.stringUtf8(128), CircuitStatusPayload::itemId,
            ByteBufCodecs.stringUtf8(1024), CircuitStatusPayload::text,
            CircuitStatusPayload::new);

    public static void send(Player player, Item item, String text) {
        if (player instanceof ServerPlayer serverPlayer) {
            String itemId = BuiltInRegistries.ITEM.getKey(item).toString();
            ServerPlayNetworking.send(serverPlayer, new CircuitStatusPayload(itemId, text));
        }
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}