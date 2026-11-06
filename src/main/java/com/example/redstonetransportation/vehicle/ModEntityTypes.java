package com.example.redstonetransportation.vehicle;

import com.example.redstonetransportation.RedstoneTransportation;
import com.example.redstonetransportation.structure.MovingStructureEntity;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

/** Dedicated entity types for Transportation vehicles. */
public final class ModEntityTypes {
    public static final ResourceKey<EntityType<?>> REDSTONE_TRAM_KEY = key("redstone_tram");
    public static final ResourceKey<EntityType<?>> CARGO_TRAM_KEY = key("cargo_tram");
    public static final ResourceKey<EntityType<?>> MOVING_STRUCTURE_KEY = key("moving_structure");

    /** The attachment height seats a rider on the cabin bench instead of on the hitbox top. */
    public static final EntityType<RedstoneTramEntity> REDSTONE_TRAM = Registry.register(
            BuiltInRegistries.ENTITY_TYPE,
            REDSTONE_TRAM_KEY,
            EntityType.Builder.<RedstoneTramEntity>of(RedstoneTramEntity::new, MobCategory.MISC)
                    .noLootTable()
                    .sized(0.98F, 0.90F)
                    .passengerAttachments(0.5F)
                    .clientTrackingRange(8)
                    .build(REDSTONE_TRAM_KEY)
    );

    public static final EntityType<CargoTramEntity> CARGO_TRAM = Registry.register(
            BuiltInRegistries.ENTITY_TYPE,
            CARGO_TRAM_KEY,
            EntityType.Builder.<CargoTramEntity>of(CargoTramEntity::new, MobCategory.MISC)
                    .noLootTable()
                    .sized(0.98F, 0.90F)
                    .clientTrackingRange(8)
                    .build(CARGO_TRAM_KEY)
    );

    /** Only the core block's size: the rest of a structure is drawn and collided with apart from the hitbox. */
    public static final EntityType<MovingStructureEntity> MOVING_STRUCTURE = Registry.register(
            BuiltInRegistries.ENTITY_TYPE,
            MOVING_STRUCTURE_KEY,
            EntityType.Builder.<MovingStructureEntity>of(MovingStructureEntity::new, MobCategory.MISC)
                    .noLootTable()
                    .sized(1.0F, 1.0F)
                    .passengerAttachments(1.0F)
                    .fireImmune()
                    .clientTrackingRange(16)
                    .updateInterval(1)
                    .build(MOVING_STRUCTURE_KEY)
    );

    private ModEntityTypes() {}

    private static ResourceKey<EntityType<?>> key(String path) {
        return ResourceKey.create(Registries.ENTITY_TYPE,
                Identifier.fromNamespaceAndPath(RedstoneTransportation.MOD_ID, path));
    }

    public static void initialize() {
        // Calling this method initializes the static entity registrations.
    }
}
