package com.example.analogalu;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionHand;

public record RunWaferProgramPayload(InteractionHand hand) implements CustomPacketPayload {
    public static final Type<RunWaferProgramPayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(AnalogAlu.MOD_ID, "run_wafer_program"));
    public static final StreamCodec<RegistryFriendlyByteBuf, RunWaferProgramPayload> CODEC =
            StreamCodec.composite(InteractionHand.STREAM_CODEC, RunWaferProgramPayload::hand,
                    RunWaferProgramPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
