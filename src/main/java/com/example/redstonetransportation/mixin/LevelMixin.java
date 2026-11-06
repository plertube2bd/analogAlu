package com.example.redstonetransportation.mixin;

import com.example.redstonetransportation.structure.MovingStructureEntity;
import com.example.redstonetransportation.structure.MovingStructures;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.EntityGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/** Lets entities walk on moving structures: their blocks join what an entity collides with. */
@Mixin(Level.class)
abstract class LevelMixin implements EntityGetter, MovingStructures.Holder {
    @Unique
    private final List<MovingStructureEntity> redstoneTransportation$structures = new CopyOnWriteArrayList<>();

    @Override
    public List<MovingStructureEntity> redstoneTransportation$structures() {
        return redstoneTransportation$structures;
    }

    @Override
    public List<VoxelShape> getEntityCollisions(@Nullable Entity source, AABB area) {
        return MovingStructures.withCollisions((Level) (Object) this, source, area, EntityGetter.super.getEntityCollisions(source, area));
    }
}
