package com.example.analogalu.fluid;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;

/**
 * 석유에 머리가 잠기면 숨을 쉴 수 없다.
 * 바닐라 익사 로직(물 태그 기반)을 거치지 않고 공기를 직접 깎으므로
 * 호흡 인챈트, 수중 호흡 효과, 거북 등딱지, 전달체 효과가 전부 통하지 않는다.
 */
public final class OilDrowning {
    /** 바닐라는 물 밖에서 매 틱 공기를 4 회복시키므로, 5를 빼면 실제로는 틱당 1씩 줄어든다. */
    private static final int AIR_LOSS_PER_TICK = 5;
    private static final float DROWN_DAMAGE = 2.0F;

    private OilDrowning() {}

    public static void init() {
        ServerTickEvents.END_LEVEL_TICK.register(OilDrowning::tick);
    }

    private static void tick(ServerLevel level) {
        for (Entity entity : level.getAllEntities()) {
            if (!(entity instanceof LivingEntity living) || !living.isAlive() || living instanceof ArmorStand) continue;
            if (living instanceof Player player && (player.isCreative() || player.isSpectator())) continue;
            if (!OilFluid.isInOil(level, living.getEyePosition())) continue;

            int air = living.getAirSupply() - AIR_LOSS_PER_TICK;
            if (air <= -20) {
                air = 0;
                living.hurtServer(level, level.damageSources().drown(), DROWN_DAMAGE);
            }
            living.setAirSupply(air);
        }
    }
}
