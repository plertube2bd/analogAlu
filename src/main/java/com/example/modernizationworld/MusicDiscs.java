package com.example.modernizationworld;

import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.JukeboxSong;
import net.minecraft.world.item.Rarity;

import java.util.List;

/**
 * Two music discs that play in a jukebox like any other: Snowman and Payphone. The songs themselves
 * are data (data/modernization_world/jukebox_song) and the audio is in assets/modernization_world/sounds.
 */
public final class MusicDiscs {
    public static final SoundEvent SNOWMAN_SOUND = sound("music_disc.snowman");
    public static final SoundEvent PAYPHONE_SOUND = sound("music_disc.payphone");

    public static final ResourceKey<JukeboxSong> SNOWMAN = song("snowman");
    public static final ResourceKey<JukeboxSong> PAYPHONE = song("payphone");

    public static final Item MUSIC_DISC_SNOWMAN = disc("music_disc_snowman", SNOWMAN);
    public static final Item MUSIC_DISC_PAYPHONE = disc("music_disc_payphone", PAYPHONE);
    public static final List<Item> DISCS = List.of(MUSIC_DISC_SNOWMAN, MUSIC_DISC_PAYPHONE);

    private MusicDiscs() {}

    static void init() {
        // Next to the vanilla discs in Tools & Utilities.
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.TOOLS_AND_UTILITIES)
                .register(output -> DISCS.forEach(output::accept));
    }

    private static Identifier id(String name) {
        return Identifier.fromNamespaceAndPath(ModernizationWorld.MOD_ID, name);
    }

    private static SoundEvent sound(String name) {
        Identifier id = id(name);
        return Registry.register(BuiltInRegistries.SOUND_EVENT, id, SoundEvent.createVariableRangeEvent(id));
    }

    private static ResourceKey<JukeboxSong> song(String name) {
        return ResourceKey.create(Registries.JUKEBOX_SONG, id(name));
    }

    private static Item disc(String name, ResourceKey<JukeboxSong> song) {
        ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, id(name));
        return Registry.register(BuiltInRegistries.ITEM, key, new Item(new Item.Properties().setId(key)
                .stacksTo(1).rarity(Rarity.RARE).jukeboxPlayable(song)));
    }
}
