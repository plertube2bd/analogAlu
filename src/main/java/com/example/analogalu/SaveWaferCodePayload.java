package com.example.analogalu;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionHand;

public record SaveWaferCodePayload(InteractionHand hand, String code) implements CustomPacketPayload {
        public static final Type<SaveWaferCodePayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(AnalogAlu.MOD_ID, "save_wafer_code"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SaveWaferCodePayload> CODEC = StreamCodec.composite(
            InteractionHand.STREAM_CODEC, SaveWaferCodePayload::hand,
            ByteBufCodecs.stringUtf8(4096), SaveWaferCodePayload::code,
            SaveWaferCodePayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}