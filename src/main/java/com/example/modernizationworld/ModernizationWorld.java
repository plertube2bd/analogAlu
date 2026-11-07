package com.example.modernizationworld;

import net.fabricmc.api.ModInitializer;

/** What belongs to the merged mod itself rather than to the Analog ALU or Redstone Transportation halves. */
public final class ModernizationWorld implements ModInitializer {
    public static final String MOD_ID = "modernization_world";

    @Override
    public void onInitialize() {
        MusicDiscs.init();
        com.example.modernizationworld.redcoat.RedcoatContent.init();
        com.example.modernizationworld.redcoat.RedcoatRaidManager.init();
    }
}
