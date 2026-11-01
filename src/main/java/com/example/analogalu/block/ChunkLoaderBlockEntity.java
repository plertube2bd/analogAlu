package com.example.analogalu.block;

import com.example.analogalu.ModBlocks;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public final class ChunkLoaderBlockEntity extends BlockEntity {
    private static final Map<ServerLevel, TicketSet> TICKETS = new IdentityHashMap<>();
    private boolean registered;

    public ChunkLoaderBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlocks.CHUNK_LOADER_ENTITY, pos, state);
    }

    @Override
    public void setLevel(Level level) {
        super.setLevel(level);
        updateTickets();
    }

    @Override
    public void clearRemoved() {
        super.clearRemoved();
        updateTickets();
    }

    @Override
    public void setRemoved() {
        releaseTickets();
        super.setRemoved();
    }

    public void releaseTickets() {
        if (!registered || !(level instanceof ServerLevel serverLevel)) return;
        TicketSet ticketSet = TICKETS.get(serverLevel);
        if (ticketSet != null) {
            long[] chunks = ticketSet.owners.remove(this);
            if (chunks != null) {
                for (long chunk : chunks) {
                    int remaining = ticketSet.references.merge(chunk, -1, Integer::sum);
                    if (remaining <= 0) {
                        ticketSet.references.remove(chunk);
                        if (!ticketSet.preexisting.remove(chunk)) {
                            serverLevel.setChunkForced((int) chunk, (int) (chunk >> 32), false);
                        }
                    }
                }
            }
            if (ticketSet.owners.isEmpty()) TICKETS.remove(serverLevel);
        }
        registered = false;
    }

    private void updateTickets() {
        if (registered || isRemoved() || !(level instanceof ServerLevel serverLevel)) return;
        int centerX = worldPosition.getX() >> 4;
        int centerZ = worldPosition.getZ() >> 4;
        int radius = getBlockState().is(ModBlocks.CHUNK_LOADER_TIER_2) ? 1 : 0;
        long[] chunks = new long[(radius * 2 + 1) * (radius * 2 + 1)];
        TicketSet ticketSet = TICKETS.computeIfAbsent(serverLevel, ignored -> new TicketSet());
        int index = 0;
        for (int x = centerX - radius; x <= centerX + radius; x++) {
            for (int z = centerZ - radius; z <= centerZ + radius; z++) {
                long key = ((long) z << 32) | (x & 0xFFFFFFFFL);
                chunks[index++] = key;
                if (ticketSet.references.merge(key, 1, Integer::sum) == 1) {
                    if (serverLevel.getForceLoadedChunks().contains(key)) {
                        ticketSet.preexisting.add(key);
                    } else {
                        serverLevel.setChunkForced(x, z, true);
                    }
                }
            }
        }
        ticketSet.owners.put(this, chunks);
        registered = true;
    }

    private static final class TicketSet {
        private final Map<Long, Integer> references = new HashMap<>();
        private final Set<Long> preexisting = new HashSet<>();
        private final IdentityHashMap<ChunkLoaderBlockEntity, long[]> owners = new IdentityHashMap<>();
    }
}