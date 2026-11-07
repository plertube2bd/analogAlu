package com.example.modernizationworld.test;

import com.example.modernizationworld.redcoat.RedcoatEntities;
import com.example.modernizationworld.redcoat.RedcoatKind;
import com.example.modernizationworld.redcoat.RedcoatRaidData;
import com.example.modernizationworld.redcoat.RedcoatSoldierEntity;
import com.example.analogalu.gun.GunType;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.util.RandomSource;

/** Server-side smoke tests for the British infantry registry and persistent raid state. */
public final class RedcoatGameTests {
    @GameTest(structure = "redstone_transportation_test:arena", maxTicks = 20)
    public void allSevenRedcoatRolesCanSpawn(GameTestHelper helper) {
        EntityType<?>[] types = {
                RedcoatEntities.MUSKETEER, RedcoatEntities.GRENADIER, RedcoatEntities.SERGEANT,
                RedcoatEntities.OFFICER, RedcoatEntities.RIFLEMAN, RedcoatEntities.DRUMMER,
                RedcoatEntities.DRAGOON
        };
        RedcoatKind[] expected = RedcoatKind.values();
        for (int i = 0; i < types.length; i++) {
            Mob mob = (Mob) types[i].create(helper.getLevel(), EntitySpawnReason.EVENT);
            helper.assertTrue(mob instanceof RedcoatSoldierEntity, "redcoat entity " + i + " did not construct");
            RedcoatSoldierEntity soldier = (RedcoatSoldierEntity) mob;
            var spawn = helper.absolutePos(new net.minecraft.core.BlockPos(3 + i * 2, 1, 5));
            soldier.setPos(spawn.getX() + 0.5D, spawn.getY(), spawn.getZ() + 0.5D);
            helper.assertValueEqual(soldier.kind(), expected[i], "redcoat role " + i);
            helper.assertTrue(helper.getLevel().addFreshEntity(soldier), "redcoat entity " + i + " did not enter world");
        }
        helper.succeed();
    }

    @GameTest(structure = "redstone_transportation_test:arena", maxTicks = 20)
    public void redcoatWeaponsUsePlayerGunDamageAndRanges(GameTestHelper helper) {
        helper.assertValueEqual(GunType.MUSKET.damage(), 30.0F, "musket damage matches player musket");
        helper.assertValueEqual(GunType.REVOLVER.damage(), 25.0F, "revolver damage matches player revolver");
        helper.assertValueEqual(GunType.MUSKET.range(), 48.0D, "musketeer range");
        helper.assertValueEqual(GunType.REVOLVER.range(), 32.0D, "officer pistol range");
        helper.succeed();
    }

    @GameTest(structure = "redstone_transportation_test:arena", maxTicks = 20)
    public void soldierAccuracyIsApproximatelyFiftyPercent(GameTestHelper helper) {
        RandomSource random = RandomSource.create(0xB17A1L);
        int hits = 0;
        for (int i = 0; i < 10_000; i++) {
            if (RedcoatSoldierEntity.rollAccurate(random)) hits++;
        }
        helper.assertValueInBetween(4_700, hits, 5_300, "accuracy rolls out of 10,000");
        helper.succeed();
    }

    @GameTest(structure = "redstone_transportation_test:arena", maxTicks = 20)
    public void redcoatRaidStateIsSavedDataBacked(GameTestHelper helper) {
        RedcoatRaidData data = RedcoatRaidData.get(helper.getLevel());
        data.start(10, 2, 10, 5);
        helper.assertTrue(data.active(), "raid should be active after start");
        helper.assertValueEqual(data.waveCount(), 5, "normal raid has five waves");
        helper.assertValueEqual(data.wave(), 0, "first wave is index zero");
        data.addSoldier(java.util.UUID.randomUUID());
        helper.assertValueEqual(data.soldiers().size(), 1, "spawned soldier UUID is retained");
        helper.succeed();
    }
}
