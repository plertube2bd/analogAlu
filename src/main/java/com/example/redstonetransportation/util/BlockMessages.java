package com.example.redstonetransportation.util;

import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;

/** Shared overlay-message wording. */
public final class BlockMessages {
    private BlockMessages() {}

    public static Component directionName(Direction direction) {
        return Component.translatable("direction.redstone_transportation." + direction.getName());
    }
}
