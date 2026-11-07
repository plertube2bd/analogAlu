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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Music discs that play in a jukebox like any other. The songs themselves are data
 * (data/modernization_world/jukebox_song, from tools/gen_music_discs.py) and the audio is in
 * assets/modernization_world/sounds/records.
 */
public final class MusicDiscs {
    /** One disc: its sound event, its jukebox song and its item. */
    public record Disc(String name, SoundEvent sound, ResourceKey<JukeboxSong> song, Item item) {}

    private static final List<Disc> ALL = new ArrayList<>();

    public static final Disc SNOWMAN_DISC = disc("snowman");
    public static final Disc PAYPHONE_DISC = disc("payphone");
    public static final Disc DIE_FOR_YOU_DISC = disc("die_for_you");
    public static final Disc MICE_ON_VENUS_DISC = disc("mice_on_venus");
    public static final Disc STAY_AT_YOUR_HOUSE_DISC = disc("stay_at_your_house");
    public static final Disc HAI_YOROKONDE_DISC = disc("hai_yorokonde");
    public static final Disc DIGITAL_CIRCUS_DISC = disc("digital_circus");

    public static final ResourceKey<JukeboxSong> SNOWMAN = SNOWMAN_DISC.song();
    public static final ResourceKey<JukeboxSong> PAYPHONE = PAYPHONE_DISC.song();
    public static final Item MUSIC_DISC_SNOWMAN = SNOWMAN_DISC.item();
    public static final Item MUSIC_DISC_PAYPHONE = PAYPHONE_DISC.item();

    /** Every disc, in the order they appear in the creative tab. */
    public static final List<Disc> DISCS = Collections.unmodifiableList(ALL);

    private MusicDiscs() {}

    static void init() {
        // Next to the vanilla discs in Tools & Utilities.
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.TOOLS_AND_UTILITIES)
                .register(output -> DISCS.forEach(disc -> output.accept(disc.item())));
    }

    private static Identifier id(String name) {
        return Identifier.fromNamespaceAndPath(ModernizationWorld.MOD_ID, name);
    }

    private static Disc disc(String name) {
        Identifier soundId = id("music_disc." + name);
        SoundEvent sound = Registry.register(BuiltInRegistries.SOUND_EVENT, soundId, SoundEvent.createVariableRangeEvent(soundId));
        ResourceKey<JukeboxSong> song = ResourceKey.create(Registries.JUKEBOX_SONG, id(name));
        ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, id("music_disc_" + name));
        Item item = Registry.register(BuiltInRegistries.ITEM, key, new Item(new Item.Properties().setId(key)
                .stacksTo(1).rarity(Rarity.RARE).jukeboxPlayable(song)));
        Disc disc = new Disc(name, sound, song, item);
        ALL.add(disc);
        return disc;
    }
}
