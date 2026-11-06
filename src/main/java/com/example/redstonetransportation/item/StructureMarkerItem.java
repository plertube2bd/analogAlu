package com.example.redstonetransportation.item;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Marks exactly which blocks belong to a structure, for one that touches something it must leave
 * behind, like an elevator car against its shaft. Two clicks mark the corners; using it on a core
 * then assembles only what is inside.
 */
public final class StructureMarkerItem extends Item {
    private static final Map<UUID, Marks> MARKS = new ConcurrentHashMap<>();

    private record Marks(BlockPos first, @Nullable BlockPos second) {}

    public StructureMarkerItem(Properties properties) {
        super(properties);
    }

    public static @Nullable BoundingBox selection(Player player) {
        Marks marks = MARKS.get(player.getUUID());
        return marks == null || marks.second() == null ? null : BoundingBox.fromCorners(marks.first(), marks.second());
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        if (player == null) return InteractionResult.PASS;
        if (!(context.getLevel() instanceof ServerLevel)) return InteractionResult.SUCCESS;

        UUID id = player.getUUID();
        if (player.isShiftKeyDown()) {
            MARKS.remove(id);
            player.sendOverlayMessage(Component.translatable("message.redstone_transportation.marker_cleared"));
            return InteractionResult.SUCCESS;
        }

        BlockPos clicked = context.getClickedPos();
        Marks marks = MARKS.get(id);
        if (marks == null || marks.second() != null) {
            MARKS.put(id, new Marks(clicked, null));
            player.sendOverlayMessage(Component.translatable("message.redstone_transportation.marker_first"));
            return InteractionResult.SUCCESS;
        }

        MARKS.put(id, new Marks(marks.first(), clicked));
        BoundingBox box = BoundingBox.fromCorners(marks.first(), clicked);
        player.sendOverlayMessage(Component.translatable("message.redstone_transportation.marker_second",
                box.getXSpan(), box.getYSpan(), box.getZSpan()));
        return InteractionResult.SUCCESS;
    }
}
