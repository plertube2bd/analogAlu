package com.example.modernizationworld.redcoat;

import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

/** Entity types for the seven British soldiers. */
public final class RedcoatEntities {
    public static final EntityType<RedcoatSoldierEntity> MUSKETEER = register("redcoat_musketeer");
    public static final EntityType<RedcoatSoldierEntity> GRENADIER = register("redcoat_grenadier");
    public static final EntityType<RedcoatSoldierEntity> SERGEANT = register("redcoat_sergeant");
    public static final EntityType<RedcoatSoldierEntity> OFFICER = register("redcoat_officer");
    public static final EntityType<RedcoatSoldierEntity> RIFLEMAN = register("redcoat_rifleman");
    public static final EntityType<RedcoatSoldierEntity> DRUMMER = register("redcoat_drummer");
    public static final EntityType<RedcoatSoldierEntity> DRAGOON = register("redcoat_dragoon");

    private RedcoatEntities() {}

    private static EntityType<RedcoatSoldierEntity> register(String path) {
        ResourceKey<EntityType<?>> key = ResourceKey.create(Registries.ENTITY_TYPE,
                Identifier.fromNamespaceAndPath("modernization_world", path));
        return Registry.register(BuiltInRegistries.ENTITY_TYPE, key,
                EntityType.Builder.of(RedcoatSoldierEntity::new, MobCategory.MONSTER)
                        .sized(0.6F, 1.95F).eyeHeight(1.62F)
                        .noLootTable().clientTrackingRange(8).build(key));
    }

    public static void init() {
        var attributes = RedcoatSoldierEntity.createAttributes();
        FabricDefaultAttributeRegistry.register(MUSKETEER, attributes);
        FabricDefaultAttributeRegistry.register(GRENADIER, attributes);
        FabricDefaultAttributeRegistry.register(SERGEANT, attributes);
        FabricDefaultAttributeRegistry.register(OFFICER, attributes);
        FabricDefaultAttributeRegistry.register(RIFLEMAN, attributes);
        FabricDefaultAttributeRegistry.register(DRUMMER, attributes);
        FabricDefaultAttributeRegistry.register(DRAGOON, attributes);
    }
}
