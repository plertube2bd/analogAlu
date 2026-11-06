package com.example.analogalu;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type;
import net.minecraft.resources.Identifier;

public record SetConverterSetpointPayload(BlockPos pos, double volts) implements CustomPacketPayload {
   public static final Type<SetConverterSetpointPayload> TYPE = new Type(Identifier.fromNamespaceAndPath("analog_alu", "set_converter_setpoint"));
   public static final StreamCodec<RegistryFriendlyByteBuf, SetConverterSetpointPayload> CODEC = StreamCodec.composite(
      BlockPos.STREAM_CODEC, SetConverterSetpointPayload::pos, ByteBufCodecs.DOUBLE, SetConverterSetpointPayload::volts, SetConverterSetpointPayload::new
   );

   public Type<? extends CustomPacketPayload> type() {
      return TYPE;
   }
}
