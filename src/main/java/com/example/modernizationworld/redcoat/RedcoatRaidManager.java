package com.example.modernizationworld.redcoat;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.world.BossEvent;
import net.minecraft.world.Difficulty;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.animal.equine.Horse;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.InteractionResult;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Starts, advances and resolves British raids. The wave and soldier UUID list are SavedData-backed. */
public final class RedcoatRaidManager {
    private static final Map<ServerLevel, ServerBossEvent> BARS = new HashMap<>();
    private static final Map<ServerLevel, Integer> BELL_REVEALS = new HashMap<>();
    private static final int VILLAGE_RADIUS = 64;

    private RedcoatRaidManager() {}

    public static void init() {
        ServerTickEvents.END_LEVEL_TICK.register(RedcoatRaidManager::tick);
        UseBlockCallback.EVENT.register((player, level, hand, hit) -> {
            if (!(level instanceof ServerLevel server)
                    || !level.getBlockState(hit.getBlockPos()).is(Blocks.BELL)) {
                return InteractionResult.PASS;
            }
            RedcoatRaidData data = RedcoatRaidData.get(server);
            if (!data.active()) return InteractionResult.PASS;
            int highlighted = 0;
            for (RedcoatSoldierEntity soldier : server.getEntitiesOfClass(
                    RedcoatSoldierEntity.class, player.getBoundingBox().inflate(64.0D), Entity::isAlive)) {
                soldier.addEffect(new MobEffectInstance(MobEffects.GLOWING, 200, 0, false, false, true));
                highlighted++;
            }
            if (highlighted > 0) BELL_REVEALS.put(server, 200);
            return InteractionResult.PASS;
        });
    }

    private static void tick(ServerLevel level) {
        RedcoatRaidData data = RedcoatRaidData.get(level);
        if (!data.active()) {
            for (ServerPlayer player : level.players()) {
                if (player.hasEffect(RedcoatContent.REDCOAT_OMEN) && isVillageOrNearVillager(level, player)) {
                    start(level, player, data);
                    break;
                }
            }
            return;
        }

        ServerBossEvent bar = BARS.computeIfAbsent(level, ignored -> new ServerBossEvent(
                UUID.randomUUID(), Component.translatable("raid.modernization_world.redcoat"),
                BossEvent.BossBarColor.RED, BossEvent.BossBarOverlay.NOTCHED_10));
        for (ServerPlayer player : level.players()) {
            if (player.distanceToSqr(data.x() + 0.5D, data.y(), data.z() + 0.5D) < 128.0D * 128.0D) bar.addPlayer(player);
            else bar.removePlayer(player);
        }

        long alive = level.getEntitiesOfClass(RedcoatSoldierEntity.class,
                new net.minecraft.world.phys.AABB(data.x() - VILLAGE_RADIUS, level.getMinY(), data.z() - VILLAGE_RADIUS,
                        data.x() + VILLAGE_RADIUS, level.getMaxY(), data.z() + VILLAGE_RADIUS),
                Entity::isAlive).size();

        if (alive == 0) data.tick(); else data.resetElapsed();

        boolean villagersRemain = !level.getEntitiesOfClass(Villager.class,
                new net.minecraft.world.phys.AABB(data.x() - VILLAGE_RADIUS, level.getMinY(), data.z() - VILLAGE_RADIUS,
                        data.x() + VILLAGE_RADIUS, level.getMaxY(), data.z() + VILLAGE_RADIUS),
                Villager::isAlive).isEmpty();
        if (!villagersRemain) {
            finish(level, data, false);
            return;
        }

        if (alive == 0 && data.elapsed() >= 100) {
            if (data.wave() + 1 >= data.waveCount()) {
                finish(level, data, true);
            } else {
                data.nextWave();
                spawnWave(level, data);
            }
        }
        int revealTicks = BELL_REVEALS.getOrDefault(level, 0);
        float maximum = Math.max(1.0F, 3.0F + (data.wave() + 1) * 2.0F);
        float remainingProgress = Math.max(0.0F, Math.min(1.0F, alive / maximum));
        if (revealTicks > 0) {
            BELL_REVEALS.put(level, revealTicks - 1);
            bar.setProgress(remainingProgress);
            bar.setName(Component.translatable("raid.modernization_world.redcoat.bell_remaining", alive));
        } else {
            bar.setProgress(remainingProgress);
            bar.setName(Component.translatable("raid.modernization_world.redcoat.remaining",
                    alive, Math.min(data.wave() + 1, data.waveCount()), data.waveCount()));
        }
    }

    private static boolean isVillageOrNearVillager(ServerLevel level, ServerPlayer player) {
        if (level.isVillage(player.blockPosition())) return true;
        net.minecraft.world.phys.AABB area = player.getBoundingBox().inflate(VILLAGE_RADIUS);
        return !level.getEntitiesOfClass(Villager.class, area, Villager::isAlive).isEmpty();
    }

    private static void start(ServerLevel level, ServerPlayer player, RedcoatRaidData data) {
        int waves = switch (level.getDifficulty()) {
            case PEACEFUL -> 0;
            case EASY -> 3;
            case NORMAL -> 5;
            case HARD -> 7;
        };
        if (waves == 0) return;
        data.start(player.getBlockX(), player.getBlockY(), player.getBlockZ(), waves);
        player.removeEffect(RedcoatContent.REDCOAT_OMEN);
        spawnWave(level, data);
    }

    private static void spawnWave(ServerLevel level, RedcoatRaidData data) {
        int wave = data.wave() + 1;
        int count = 3 + wave * 2;
        for (int i = 0; i < count; i++) {
            double angle = (Math.PI * 2.0D * i / count) + level.getRandom().nextDouble() * 0.2D;
            double radius = 24.0D + level.getRandom().nextDouble() * 8.0D;
            int x = data.x() + (int) Math.round(Math.cos(angle) * radius);
            int z = data.z() + (int) Math.round(Math.sin(angle) * radius);
            int y = level.getHeightmapPos(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                    new net.minecraft.core.BlockPos(x, data.y(), z)).getY();

            RedcoatKind kind = chooseKind(level, wave, i);
            RedcoatSoldierEntity soldier = create(level, kind);
            if (soldier == null) continue;
            soldier.setPos(x + 0.5D, y, z + 0.5D);
            soldier.setYRot((float) (angle * 180.0D / Math.PI));
            soldier.setMarchTarget(new net.minecraft.core.BlockPos(data.x(), data.y(), data.z()));
            if (level.addFreshEntity(soldier)) {
                data.addSoldier(soldier.getUUID());
                if (kind == RedcoatKind.DRAGOON) mountDragoon(level, soldier, x, y, z);
            }
        }
    }

    private static RedcoatKind chooseKind(ServerLevel level, int wave, int index) {
        if (wave >= 3 && index % 11 == 0) return RedcoatKind.DRAGOON;
        if (index % 9 == 0) return RedcoatKind.DRUMMER;
        if (wave >= 2 && index % 7 == 0) return RedcoatKind.RIFLEMAN;
        if (index % 6 == 0) return RedcoatKind.OFFICER;
        if (index % 5 == 0) return RedcoatKind.SERGEANT;
        if (index % 4 == 0) return RedcoatKind.GRENADIER;
        return RedcoatKind.MUSKETEER;
    }

    private static RedcoatSoldierEntity create(ServerLevel level, RedcoatKind kind) {
        var type = switch (kind) {
            case MUSKETEER -> RedcoatEntities.MUSKETEER;
            case GRENADIER -> RedcoatEntities.GRENADIER;
            case SERGEANT -> RedcoatEntities.SERGEANT;
            case OFFICER -> RedcoatEntities.OFFICER;
            case RIFLEMAN -> RedcoatEntities.RIFLEMAN;
            case DRUMMER -> RedcoatEntities.DRUMMER;
            case DRAGOON -> RedcoatEntities.DRAGOON;
        };
        return type.create(level, EntitySpawnReason.EVENT);
    }

    private static void mountDragoon(ServerLevel level, RedcoatSoldierEntity soldier, int x, int y, int z) {
        Horse horse = (Horse) BuiltInRegistries.ENTITY_TYPE.getValue(
                Identifier.fromNamespaceAndPath("minecraft", "horse")).create(level, EntitySpawnReason.EVENT);
        if (horse == null) return;
        horse.setPos(x + 0.5D, y, z + 0.5D);
        horse.setYRot(soldier.getYRot());
        horse.setTamed(true);
        horse.setItemSlot(net.minecraft.world.entity.EquipmentSlot.SADDLE, new ItemStack(Items.SADDLE));
        if (level.addFreshEntity(horse)) soldier.startRiding(horse, true, true);
    }

    private static void finish(ServerLevel level, RedcoatRaidData data, boolean won) {
        ServerBossEvent bar = BARS.remove(level);
        if (bar != null) bar.removeAllPlayers();
        if (won) {
            for (ServerPlayer player : level.players()) {
                if (player.distanceToSqr(data.x() + 0.5D, data.y(), data.z() + 0.5D) <= 96.0D * 96.0D) {
                    player.addEffect(new MobEffectInstance(MobEffects.HERO_OF_THE_VILLAGE, 20 * 60 * 60, 0));
                    rewardDefender(player);
                }
            }
            level.players().forEach(player -> player.sendSystemMessage(Component.translatable("message.modernization_world.redcoat.victory")));
        } else {
            level.players().forEach(player -> player.sendSystemMessage(Component.translatable("message.modernization_world.redcoat.defeat")));
        }
        data.finish();
    }

    private static void rewardDefender(ServerPlayer player) {
        AdvancementHolder advancement = player.level().getServer().getAdvancements().get(
                Identifier.fromNamespaceAndPath("modernization_world", "redcoat/defender"));
        if (advancement != null) player.getAdvancements().award(advancement, "won_raid");
        giveOrDrop(player, new ItemStack(Items.DIAMOND, 3));
        giveOrDrop(player, new ItemStack(Items.EMERALD, 8));
    }

    private static void giveOrDrop(ServerPlayer player, ItemStack stack) {
        if (!player.getInventory().add(stack)) {
            player.drop(stack, false, net.minecraft.util.Prediction.SERVER_ONLY);
        }
    }
}
