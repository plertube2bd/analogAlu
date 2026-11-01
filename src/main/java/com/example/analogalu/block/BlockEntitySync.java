package com.example.analogalu.block;

import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.entity.BlockEntity;

/** 블록 엔티티 데이터만 주변 클라이언트에 보낸다 (블록 갱신/리메시 없이 툴팁용 값만 갱신). */
public final class BlockEntitySync {
    private BlockEntitySync() {}

    public static void send(BlockEntity blockEntity) {
        if (!(blockEntity.getLevel() instanceof ServerLevel level)) return;
        Packet<ClientGamePacketListener> packet = blockEntity.getUpdatePacket();
        if (packet == null) return;
        for (ServerPlayer player : level.getChunkSource().chunkMap.getPlayers(ChunkPos.containing(blockEntity.getBlockPos()), false)) {
            player.connection.send(packet);
        }
    }
}
