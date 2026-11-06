package com.example.redstonetransportation.structure;

import com.example.redstonetransportation.RedstoneTransportation;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityDataRegistry;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.ArrayList;
import java.util.List;

/** Finds the moving structures of a level, for the code that runs for every entity on every tick. */
public final class MovingStructures {
    /** Implemented by Level through LevelMixin, so a level with no structures pays nothing. */
    public interface Holder {
        List<MovingStructureEntity> redstoneTransportation$structures();
    }

    private MovingStructures() {}

    public static void initialize() {
        FabricEntityDataRegistry.register(
                Identifier.fromNamespaceAndPath(RedstoneTransportation.MOD_ID, "structure_blocks"), StructureBlocks.SERIALIZER);
        ServerEntityEvents.ENTITY_LOAD.register((entity, level) -> add(level, entity));
        ServerEntityEvents.ENTITY_UNLOAD.register((entity, level) -> remove(level, entity));
        ServerTickEvents.END_LEVEL_TICK.register(MovingStructures::finishTick);
        StructureActions.initialize();
    }

    public static void add(Level level, Entity entity) {
        if (entity instanceof MovingStructureEntity structure) in(level).add(structure);
    }

    public static void remove(Level level, Entity entity) {
        if (entity instanceof MovingStructureEntity structure) in(level).remove(structure);
    }

    /**
     * Everything in a level sees a structure where the last tick left it, and whatever stands on
     * it moves along here, once. That keeps riders level with the deck whichever ticked first.
     */
    public static void finishTick(Level level) {
        for (MovingStructureEntity structure : in(level)) structure.settle();
    }

    /**
     * Adds the blocks of nearby structures to what an entity collides with. A server player is left
     * out: its own client works out where it stands, against the structure as that client sees it.
     */
    public static List<VoxelShape> withCollisions(Level level, Entity source, AABB area, List<VoxelShape> others) {
        List<MovingStructureEntity> structures = in(level);
        if (structures.isEmpty() || source instanceof ServerPlayer || (source != null && source.isSpectator())) return others;

        List<VoxelShape> shapes = new ArrayList<>(others);
        for (MovingStructureEntity structure : structures) {
            if (source == null || source.getVehicle() != structure) structure.collectCollisions(area, shapes);
        }
        return shapes;
    }

    /** True inside the hull of a ship: the sea is still there in the world, but nothing in there should feel it. */
    public static boolean isInHull(Level level, double x, double y, double z) {
        for (MovingStructureEntity structure : in(level)) {
            if (structure.isInHull(x, y, z)) return true;
        }
        return false;
    }

    public static List<MovingStructureEntity> in(Level level) {
        return ((Holder) level).redstoneTransportation$structures();
    }
}
