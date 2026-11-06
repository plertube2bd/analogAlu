package com.example.redstonetransportation.content;

import com.example.redstonetransportation.RedstoneTransportation;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

/**
 * Advancements for things no vanilla trigger can see, like assembling a structure. Their one
 * criterion can never fire by itself, so the code that sees the event grants it here.
 */
public final class ModAdvancements {
    public static final String TRAIN = "rail/train";
    public static final String FULL_SPEED = "rail/full_speed";
    public static final String FACTORY = "factory/controller";
    public static final String STRUCTURE = "structure/first";
    public static final String ELEVATOR = "structure/elevator";
    public static final String SHIP = "structure/ship";
    public static final String AIRSHIP = "structure/airship";
    public static final String CAR = "structure/car";
    public static final String ROUTE = "structure/route";
    public static final String REPAIRS = "structure/repairs";
    public static final String GIANT = "structure/giant";

    private static final String CRITERION = "done";

    private ModAdvancements() {}

    /** Does nothing for anything but a real player, so mobs can ride and tests can click freely. */
    public static void award(Entity entity, String path) {
        if (!(entity instanceof ServerPlayer player)) return;
        AdvancementHolder advancement = player.level().getServer().getAdvancements()
                .get(Identifier.fromNamespaceAndPath(RedstoneTransportation.MOD_ID, path));
        if (advancement != null) player.getAdvancements().award(advancement, CRITERION);
    }
}
