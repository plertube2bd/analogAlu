package com.example.analogalu;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type;
import net.minecraft.resources.Identifier;

public record OpenConverterScreenPayload(BlockPos pos, boolean stepUp, double setpoint, double min, double max) implements CustomPacketPayload {
   public static final Type<OpenConverterScreenPayload> TYPE = new Type(Identifier.fromNamespaceAndPath("analog_alu", "open_converter_screen"));
   public static final StreamCodec<RegistryFriendlyByteBuf, OpenConverterScreenPayload> CODEC = StreamCodec.composite(
      BlockPos.STREAM_CODEC,
      OpenConverterScreenPayload::pos,
      ByteBufCodecs.BOOL,
      OpenConverterScreenPayload::stepUp,
      ByteBufCodecs.DOUBLE,
      OpenConverterScreenPayload::setpoint,
      ByteBufCodecs.DOUBLE,
      OpenConverterScreenPayload::min,
      ByteBufCodecs.DOUBLE,
      OpenConverterScreenPayload::max,
      OpenConverterScreenPayload::new
   );

   public Type<? extends CustomPacketPayload> type() {
      return TYPE;
   }
}
