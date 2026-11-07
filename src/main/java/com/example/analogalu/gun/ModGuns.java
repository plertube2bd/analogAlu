package com.example.analogalu.gun;

import com.example.analogalu.AnalogAlu;
import com.mojang.serialization.Codec;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.function.Function;

/** Black powder, the three guns and their ammunition. */
public final class ModGuns {
    /** Rounds loaded in a revolver or shotgun. */
    public static final DataComponentType<Integer> ROUNDS = component("gun_rounds", Codec.INT, ByteBufCodecs.VAR_INT);
    /** A revolver with its hammer pulled back, ready to fire. */
    public static final DataComponentType<Boolean> COCKED = component("gun_cocked", Codec.BOOL, ByteBufCodecs.BOOL);
    /** Game time a reload in progress finishes at. */
    public static final DataComponentType<Long> RELOAD_UNTIL = component("gun_reload_until", Codec.LONG, ByteBufCodecs.VAR_LONG);
    /** Armour and Projectile Protection both count against it. */
    public static final ResourceKey<DamageType> BULLET_DAMAGE = ResourceKey.create(Registries.DAMAGE_TYPE, id("bullet"));

    public static final Item BLACK_POWDER = item("black_powder", Item::new, 64);
    public static final Item BULLET_9MM = item("bullet_9mm", Item::new, 64);
    public static final Item MUSKET_BALL = item("musket_ball", Item::new, 64);
    public static final Item SHOTGUN_SHELL = item("shotgun_shell", Item::new, 64);
    public static final GunItem REVOLVER = gun(GunType.REVOLVER);
    public static final GunItem MUSKET = gun(GunType.MUSKET);
    public static final GunItem SHOTGUN = gun(GunType.SHOTGUN);

    private ModGuns() {}

    public static void init() {
        PayloadTypeRegistry.serverboundPlay().register(GunActionPayload.TYPE, GunActionPayload.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(GunActionPayload.TYPE, (payload, context) ->
                context.server().execute(() -> {
                    ServerPlayer player = context.player();
                    ItemStack held = player.getMainHandItem();
                    if (!(held.getItem() instanceof GunItem gun)) return;
                    switch (payload.action()) {
                        case FIRE -> gun.fire(player, held);
                        case COCK_OR_RELOAD -> gun.action(player, held);
                    }
                }));
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.COMBAT).register(output -> {
            output.accept(REVOLVER);
            output.accept(BULLET_9MM);
            output.accept(MUSKET);
            output.accept(MUSKET_BALL);
            output.accept(SHOTGUN);
            output.accept(SHOTGUN_SHELL);
        });
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.INGREDIENTS).register(output -> output.accept(BLACK_POWDER));
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(AnalogAlu.MOD_ID, path);
    }

    private static <T> DataComponentType<T> component(String name, Codec<T> codec, StreamCodec<? super net.minecraft.network.RegistryFriendlyByteBuf, T> stream) {
        return Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE, id(name),
                DataComponentType.<T>builder().persistent(codec).networkSynchronized(stream).build());
    }

    private static Item item(String name, Function<Item.Properties, Item> factory, int stack) {
        ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, id(name));
        return Registry.register(BuiltInRegistries.ITEM, key, factory.apply(new Item.Properties().setId(key).stacksTo(stack)));
    }

    private static GunItem gun(GunType type) {
        return (GunItem) item(type.id(), properties -> new GunItem(properties.component(ROUNDS, 0), type), 1);
    }
}
