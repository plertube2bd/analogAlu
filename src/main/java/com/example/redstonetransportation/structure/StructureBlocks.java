package com.example.redstonetransportation.structure;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongArrayFIFOQueue;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.syncher.EntityDataSerializer;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** The blocks of a moving structure, relative to its core. An edit makes a new one. */
public final class StructureBlocks {
    /**
     * @param pos         position relative to the core
     * @param blockEntity everything the block entity saved; only the server holds this
     * @param clientData  what a client needs to draw the block entity, such as sign text
     */
    public record Cell(BlockPos pos, BlockState state, Optional<CompoundTag> blockEntity, Optional<CompoundTag> clientData) {
        private static final Codec<Cell> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                BlockPos.CODEC.fieldOf("pos").forGetter(Cell::pos),
                BlockState.CODEC.fieldOf("state").forGetter(Cell::state),
                CompoundTag.CODEC.optionalFieldOf("block_entity").forGetter(Cell::blockEntity),
                CompoundTag.CODEC.optionalFieldOf("client_data").forGetter(Cell::clientData)
        ).apply(instance, Cell::new));

        private static final StreamCodec<RegistryFriendlyByteBuf, Cell> STREAM_CODEC = StreamCodec.composite(
                BlockPos.STREAM_CODEC, Cell::pos,
                ByteBufCodecs.idMapper(Block.BLOCK_STATE_REGISTRY), Cell::state,
                ByteBufCodecs.OPTIONAL_COMPOUND_TAG, Cell::clientData,
                (pos, state, clientData) -> new Cell(pos, state, Optional.empty(), clientData));

        public Cell withState(BlockState state) {
            return new Cell(pos, state, blockEntity, clientData);
        }
    }

    public static final StructureBlocks EMPTY = new StructureBlocks(List.of());
    public static final Codec<StructureBlocks> CODEC = Cell.CODEC.listOf().xmap(StructureBlocks::new, StructureBlocks::cells);
    public static final EntityDataSerializer<StructureBlocks> SERIALIZER = EntityDataSerializer.forValueType(
            Cell.STREAM_CODEC.apply(ByteBufCodecs.list()).map(StructureBlocks::new, StructureBlocks::cells));

    private static final AABB[] NO_BOXES = new AABB[0];

    private final List<Cell> cells;
    private final Long2ObjectOpenHashMap<Cell> byPosition = new Long2ObjectOpenHashMap<>();
    private final Map<BlockState, AABB[]> collisionBoxes = new IdentityHashMap<>();
    private final List<BlockPos> shell = new ArrayList<>();
    private final List<BlockPos> bottom = new ArrayList<>();
    private LongOpenHashSet hull;
    private int minX = Integer.MAX_VALUE;
    private int maxX = Integer.MIN_VALUE;
    private int minY = Integer.MAX_VALUE;
    private int maxY = Integer.MIN_VALUE;
    private int minZ = Integer.MAX_VALUE;
    private int maxZ = Integer.MIN_VALUE;
    private int reach;

    public StructureBlocks(List<Cell> cells) {
        this.cells = List.copyOf(cells);
        for (Cell cell : this.cells) {
            BlockPos pos = cell.pos();
            byPosition.put(pos.asLong(), cell);
            minX = Math.min(minX, pos.getX());
            maxX = Math.max(maxX, pos.getX());
            minY = Math.min(minY, pos.getY());
            maxY = Math.max(maxY, pos.getY());
            minZ = Math.min(minZ, pos.getZ());
            maxZ = Math.max(maxZ, pos.getZ());
            reach = Math.max(reach, Math.max(Math.abs(pos.getX()), Math.abs(pos.getZ())));
        }
        for (Cell cell : this.cells) {
            BlockPos pos = cell.pos();
            if (pos.getY() == minY) bottom.add(pos);
            for (Direction side : Direction.values()) {
                if (!byPosition.containsKey(pos.relative(side).asLong())) {
                    shell.add(pos);
                    break;
                }
            }
        }
    }

    public List<Cell> cells() {
        return cells;
    }

    public boolean isEmpty() {
        return cells.isEmpty();
    }

    public int size() {
        return cells.size();
    }

    /** The blocks with an open side, which are the only ones that can run into anything. */
    public List<BlockPos> shell() {
        return shell;
    }

    public List<BlockPos> bottom() {
        return bottom;
    }

    public int minY() {
        return minY;
    }

    public int maxY() {
        return maxY;
    }

    /** The furthest any block sits from the core along a horizontal axis. */
    public int reach() {
        return reach;
    }

    public @Nullable Cell cell(BlockPos pos) {
        return byPosition.get(pos.asLong());
    }

    public @Nullable BlockState at(int x, int y, int z) {
        Cell cell = byPosition.get(BlockPos.asLong(x, y, z));
        return cell == null ? null : cell.state();
    }

    /** A copy with this cell set, or with the block at {@code pos} gone when {@code cell} is null. */
    public StructureBlocks with(BlockPos pos, @Nullable Cell cell) {
        List<Cell> changed = new ArrayList<>(cells.size() + 1);
        for (Cell existing : cells) {
            if (!existing.pos().equals(pos)) changed.add(existing);
        }
        if (cell != null) changed.add(cell);
        return new StructureBlocks(changed);
    }

    public AABB[] collisionBoxes(BlockState state) {
        return collisionBoxes.computeIfAbsent(state,
                key -> key.getCollisionShape(EmptyBlockGetter.INSTANCE, BlockPos.ZERO).toAabbs().toArray(NO_BOXES));
    }

    public boolean isSolidAt(int x, int y, int z) {
        BlockState state = at(x, y, z);
        return state != null && collisionBoxes(state).length > 0;
    }

    /** True for an empty space shut in by walls on every side, with a floor somewhere beneath it. */
    public boolean isHull(int x, int y, int z) {
        return x >= minX && x <= maxX && y >= minY && y <= maxY && z >= minZ && z <= maxZ
                && hull().contains(BlockPos.asLong(x, y, z));
    }

    /** The hull spaces of one layer, as positions. */
    public List<BlockPos> hullLayer(int y) {
        List<BlockPos> layer = new ArrayList<>();
        for (long packed : hull()) {
            if (BlockPos.getY(packed) == y) layer.add(BlockPos.of(packed));
        }
        return layer;
    }

    private LongOpenHashSet hull() {
        if (hull != null) return hull;
        hull = new LongOpenHashSet();
        for (int y = minY; y <= maxY; y++) {
            LongOpenHashSet outside = outsideOfLayer(y);
            for (int x = minX; x <= maxX; x++) {
                for (int z = minZ; z <= maxZ; z++) {
                    long pos = BlockPos.asLong(x, y, z);
                    if (!byPosition.containsKey(pos) && !outside.contains(pos) && hasFloor(x, y, z)) hull.add(pos);
                }
            }
        }
        return hull;
    }

    /** Spreads in from a ring around the layer's bounds, through everything that is not a block. */
    private LongOpenHashSet outsideOfLayer(int y) {
        LongOpenHashSet reached = new LongOpenHashSet();
        LongArrayFIFOQueue open = new LongArrayFIFOQueue();
        long start = BlockPos.asLong(minX - 1, y, minZ - 1);
        reached.add(start);
        open.enqueue(start);
        while (!open.isEmpty()) {
            long pos = open.dequeueLong();
            for (Direction side : Direction.Plane.HORIZONTAL) {
                int x = BlockPos.getX(pos) + side.getStepX();
                int z = BlockPos.getZ(pos) + side.getStepZ();
                if (x < minX - 1 || x > maxX + 1 || z < minZ - 1 || z > maxZ + 1) continue;
                long next = BlockPos.asLong(x, y, z);
                if (!byPosition.containsKey(next) && reached.add(next)) open.enqueue(next);
            }
        }
        return reached;
    }

    private boolean hasFloor(int x, int y, int z) {
        for (int below = y - 1; below >= minY; below--) {
            if (byPosition.containsKey(BlockPos.asLong(x, below, z))) return true;
        }
        return false;
    }
}
