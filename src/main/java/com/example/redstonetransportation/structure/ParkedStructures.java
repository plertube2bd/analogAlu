package com.example.redstonetransportation.structure;

import com.example.redstonetransportation.RedstoneTransportation;
import com.example.redstonetransportation.block.StructureCoreBlock;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Structures that have been set down, and what they were set down against. Assembly takes everything
 * joined to the core, so a ship, airship or car that comes back and is set down touching blocks that were
 * already there (a house, a tree, a pier) would take them along the next time; so would one that another
 * vessel was later set down beside. When a structure is set down, its own blocks and the blocks already
 * touching it are noted here, and assembly leaves behind both those and any other parked structure's
 * blocks. Blocks a player puts against a parked vessel afterwards are neither, and join it.
 */
public final class ParkedStructures extends SavedData {
    private record Noted(long pos, String block) {
        static final Codec<Noted> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.LONG.fieldOf("pos").forGetter(Noted::pos),
                Codec.STRING.fieldOf("block").forGetter(Noted::block)).apply(instance, Noted::new));
    }

    private record Saved(long core, List<Noted> members, List<Noted> neighbours) {
        static final Codec<Saved> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.LONG.fieldOf("core").forGetter(Saved::core),
                Noted.CODEC.listOf().fieldOf("members").forGetter(Saved::members),
                Noted.CODEC.listOf().fieldOf("neighbours").forGetter(Saved::neighbours)).apply(instance, Saved::new));
    }

    /** A parked structure: its own blocks, and the blocks that were touching it when it was set down. */
    private record Parked(Map<Long, String> members, Map<Long, String> neighbours) {}

    private static final Codec<ParkedStructures> CODEC = Saved.CODEC.listOf().xmap(ParkedStructures::new, ParkedStructures::toList);
    public static final SavedDataType<ParkedStructures> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath(RedstoneTransportation.MOD_ID, "parked_structures"), ParkedStructures::new, CODEC, null);

    private final Map<Long, Parked> parked = new HashMap<>();

    public ParkedStructures() {}

    private ParkedStructures(List<Saved> saved) {
        for (Saved entry : saved) parked.put(entry.core(), new Parked(toMap(entry.members()), toMap(entry.neighbours())));
    }

    private static Map<Long, String> toMap(List<Noted> list) {
        Map<Long, String> map = new HashMap<>();
        for (Noted noted : list) map.put(noted.pos(), noted.block());
        return map;
    }

    private static List<Noted> toList(Map<Long, String> map) {
        List<Noted> list = new ArrayList<>();
        map.forEach((pos, block) -> list.add(new Noted(pos, block)));
        return list;
    }

    private List<Saved> toList() {
        List<Saved> list = new ArrayList<>();
        parked.forEach((core, entry) -> list.add(new Saved(core, toList(entry.members()), toList(entry.neighbours()))));
        return list;
    }

    public static ParkedStructures in(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(TYPE);
    }

    /** Notes a structure just set down with its core at {@code core}: its blocks and what was already against them. */
    void remember(BlockPos core, Iterable<BlockPos> members, Map<BlockPos, BlockState> neighbours, ServerLevel level) {
        Map<Long, String> own = new HashMap<>();
        for (BlockPos pos : members) own.put(pos.asLong(), key(level.getBlockState(pos)));
        Map<Long, String> against = new HashMap<>();
        neighbours.forEach((pos, state) -> against.put(pos.asLong(), key(state)));
        parked.put(core.asLong(), new Parked(own, against));
        setDirty();
    }

    /**
     * True when assembly from {@code core} should leave the block at {@code pos} where it is: it was already
     * there when this structure was set down, or it is part of another structure still parked.
     */
    public boolean keepsOut(ServerLevel level, BlockPos core, BlockPos pos, BlockState state) {
        long at = pos.asLong();
        String block = key(state);
        Parked own = parked.get(core.asLong());
        if (own != null && block.equals(own.neighbours().get(at))) return true;
        Iterator<Map.Entry<Long, Parked>> entries = parked.entrySet().iterator();
        while (entries.hasNext()) {
            Map.Entry<Long, Parked> entry = entries.next();
            if (entry.getKey() == core.asLong() || !block.equals(entry.getValue().members().get(at))) continue;
            // Only a structure whose core is still standing is still parked; one taken apart by hand is forgotten.
            if (level.getBlockState(BlockPos.of(entry.getKey())).getBlock() instanceof StructureCoreBlock) return true;
            entries.remove();
            setDirty();
        }
        return false;
    }

    /** True when {@code pos} held this block when the structure with this core was set down against it. */
    public boolean wasThere(BlockPos core, BlockPos pos, BlockState state) {
        Parked own = parked.get(core.asLong());
        return own != null && key(state).equals(own.neighbours().get(pos.asLong()));
    }

    /** The structure has been assembled again, so it is no longer parked. */
    void forget(BlockPos core) {
        if (parked.remove(core.asLong()) != null) setDirty();
    }

    private static String key(BlockState state) {
        return BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString();
    }
}
