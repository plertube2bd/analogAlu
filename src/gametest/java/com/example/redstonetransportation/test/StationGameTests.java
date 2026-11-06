package com.example.redstonetransportation.test;

import com.example.redstonetransportation.block.VehicleStationBlock;
import com.example.redstonetransportation.content.ModContent;
import com.example.redstonetransportation.vehicle.ModEntityTypes;
import com.example.redstonetransportation.vehicle.RedstoneTramEntity;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.animal.pig.Pig;
import net.minecraft.world.entity.vehicle.minecart.AbstractMinecart;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.properties.RailShape;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

public final class StationGameTests {
    private static final BlockPos STATION = new BlockPos(8, 1, 7);
    private static final BlockPos STOP = STATION.south();
    private static final BlockPos CHEST = STATION.north();

    /** A station looking onto a straight line. */
    private static void build(GameTestHelper helper) {
        TestWorld.floor(helper, Blocks.STONE);
        for (int x = 3; x <= 26; x++) {
            TestWorld.rail(helper, new BlockPos(x, 1, STOP.getZ()), Blocks.RAIL, RailShape.EAST_WEST);
        }
        helper.setBlock(STATION, ModContent.VEHICLE_STATION.defaultBlockState().setValue(VehicleStationBlock.FACING, Direction.SOUTH));
    }

    private static ChestBlockEntity chestWith(GameTestHelper helper, Item item, int count) {
        helper.setBlock(CHEST, Blocks.CHEST);
        ChestBlockEntity chest = helper.getBlockEntity(CHEST, ChestBlockEntity.class);
        chest.setItem(3, new ItemStack(item, count));
        return chest;
    }

    private static ServerPlayer survivalPlayer(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.setGameMode(GameType.SURVIVAL);
        return player;
    }

    private static void rightClick(GameTestHelper helper, ServerPlayer player) {
        BlockPos pos = helper.absolutePos(STATION);
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false);
        helper.getLevel().getBlockState(pos).useWithoutItem(helper.getLevel(), player, hit);
    }

    private static Pig rider(GameTestHelper helper, BlockPos pos) {
        return helper.spawnWithNoFreeWill(EntityTypes.PIG, pos.north());
    }

    /** With the station held by an empty drive rail, a tram waits there until something releases it. */
    private static RedstoneTramEntity tramHeldAtTheStop(GameTestHelper helper) {
        build(helper);
        TestWorld.rail(helper, STOP, ModContent.DRIVE_RAIL, RailShape.EAST_WEST);
        return helper.spawn(ModEntityTypes.REDSTONE_TRAM, STOP);
    }

    @GameTest(structure = TestWorld.ARENA, maxTicks = 20)
    public void noTramMeansNothingToSummon(GameTestHelper helper) {
        build(helper);
        rightClick(helper, survivalPlayer(helper));
        helper.assertEntityNotPresent(ModEntityTypes.REDSTONE_TRAM);
        helper.succeed();
    }

    @GameTest(structure = TestWorld.ARENA, maxTicks = 20)
    public void aChestBesideTheStationSuppliesTheTram(GameTestHelper helper) {
        build(helper);
        ChestBlockEntity chest = chestWith(helper, ModContent.REDSTONE_TRAM_ITEM, 2);
        rightClick(helper, survivalPlayer(helper));
        helper.assertEntityPresent(ModEntityTypes.REDSTONE_TRAM);
        helper.assertValueEqual(chest.countItem(ModContent.REDSTONE_TRAM_ITEM), 1, "trams left in the chest");
        helper.succeed();
    }

    @GameTest(structure = TestWorld.ARENA, maxTicks = 20)
    public void theInventorySuppliesTheTram(GameTestHelper helper) {
        build(helper);
        ServerPlayer player = survivalPlayer(helper);
        player.getInventory().setItem(20, new ItemStack(ModContent.REDSTONE_TRAM_ITEM));
        rightClick(helper, player);
        helper.assertEntityPresent(ModEntityTypes.REDSTONE_TRAM);
        helper.assertValueEqual(player.getInventory().countItem(ModContent.REDSTONE_TRAM_ITEM), 0, "trams left on the player");
        helper.succeed();
    }

    /** The chest is the station's own stock, so the player's stays where it is. */
    @GameTest(structure = TestWorld.ARENA, maxTicks = 20)
    public void theChestGoesBeforeTheInventory(GameTestHelper helper) {
        build(helper);
        ChestBlockEntity chest = chestWith(helper, ModContent.REDSTONE_TRAM_ITEM, 1);
        ServerPlayer player = survivalPlayer(helper);
        player.getInventory().setItem(20, new ItemStack(ModContent.REDSTONE_TRAM_ITEM));
        rightClick(helper, player);
        helper.assertTrue(chest.isEmpty(), "the chest kept its tram");
        helper.assertValueEqual(player.getInventory().countItem(ModContent.REDSTONE_TRAM_ITEM), 1, "trams left on the player");
        helper.succeed();
    }

    @GameTest(structure = TestWorld.ARENA, maxTicks = 20)
    public void creativePlayersKeepTheirTram(GameTestHelper helper) {
        build(helper);
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.getInventory().setItem(20, new ItemStack(ModContent.REDSTONE_TRAM_ITEM));
        rightClick(helper, player);
        helper.assertEntityPresent(ModEntityTypes.REDSTONE_TRAM);
        helper.assertValueEqual(player.getInventory().countItem(ModContent.REDSTONE_TRAM_ITEM), 1, "trams left on the player");
        helper.succeed();
    }

    /** Even in creative the station needs a tram to send out. */
    @GameTest(structure = TestWorld.ARENA, maxTicks = 20)
    public void creativePlayersStillNeedATram(GameTestHelper helper) {
        build(helper);
        rightClick(helper, helper.makeMockServerPlayerInLevel());
        helper.assertEntityNotPresent(ModEntityTypes.REDSTONE_TRAM);
        helper.succeed();
    }

    @GameTest(structure = TestWorld.ARENA, maxTicks = 20)
    public void aCargoTramComesOutAsACargoTram(GameTestHelper helper) {
        build(helper);
        chestWith(helper, ModContent.CARGO_TRAM_ITEM, 1);
        rightClick(helper, survivalPlayer(helper));
        helper.assertEntityPresent(ModEntityTypes.CARGO_TRAM);
        helper.assertEntityNotPresent(ModEntityTypes.REDSTONE_TRAM);
        helper.succeed();
    }

    /** Refused dispatches must not cost the tram. */
    @GameTest(structure = TestWorld.ARENA, maxTicks = 20)
    public void aRefusedDispatchKeepsTheTram(GameTestHelper helper) {
        build(helper);
        ChestBlockEntity chest = chestWith(helper, ModContent.REDSTONE_TRAM_ITEM, 1);
        helper.spawn(EntityTypes.MINECART, STOP.east(3));
        rightClick(helper, survivalPlayer(helper));
        helper.assertEntityNotPresent(ModEntityTypes.REDSTONE_TRAM);
        helper.assertValueEqual(chest.countItem(ModContent.REDSTONE_TRAM_ITEM), 1, "trams left in the chest");
        helper.succeed();
    }

    /** A circuit has no pockets: only the chest can supply it. */
    @GameTest(structure = TestWorld.ARENA, maxTicks = 40)
    public void aSignalDispatchesFromTheChest(GameTestHelper helper) {
        build(helper);
        ChestBlockEntity chest = chestWith(helper, ModContent.REDSTONE_TRAM_ITEM, 1);
        helper.setBlock(STATION.west(), TestContent.SIGNAL);
        helper.succeedWhen(() -> {
            helper.assertEntityPresent(ModEntityTypes.REDSTONE_TRAM);
            helper.assertTrue(chest.isEmpty(), "the chest kept its tram");
        });
    }

    @GameTest(structure = TestWorld.ARENA, maxTicks = 40)
    public void aSignalWithNoChestDispatchesNothing(GameTestHelper helper) {
        build(helper);
        helper.setBlock(STATION.west(), TestContent.SIGNAL);
        helper.runAtTickTime(25, () -> {
            helper.assertBlockProperty(STATION, VehicleStationBlock.AUTO_TRIGGERED, true);
            helper.assertEntityNotPresent(ModEntityTypes.REDSTONE_TRAM);
            helper.succeed();
        });
    }

    /** The signal comes on when someone gets on, stays while they ride, and goes when they get off. */
    @GameTest(structure = TestWorld.ARENA, maxTicks = 80)
    public void boardingRaisesTheSignal(GameTestHelper helper) {
        RedstoneTramEntity tram = tramHeldAtTheStop(helper);
        Pig pig = rider(helper, STOP);
        helper.runAtTickTime(15, () -> {
            helper.assertBlockProperty(STATION, VehicleStationBlock.ACTIVE, true);
            helper.assertBlockProperty(STATION, VehicleStationBlock.POWER, 0);
            pig.startRiding(tram);
        });
        helper.runAtTickTime(25, () -> {
            helper.assertBlockProperty(STATION, VehicleStationBlock.POWER, 15);
            pig.stopRiding();
            tram.setDeltaMovement(Vec3.ZERO);
        });
        helper.runAtTickTime(40, () -> {
            helper.assertBlockProperty(STATION, VehicleStationBlock.POWER, 0);
            helper.succeed();
        });
    }

    /** The whole idea: step aboard a held tram and it leaves. */
    @GameTest(structure = TestWorld.ARENA, maxTicks = 120)
    public void boardingSendsATramOffADriveRail(GameTestHelper helper) {
        RedstoneTramEntity tram = tramHeldAtTheStop(helper);
        Pig pig = rider(helper, STOP);
        helper.runAtTickTime(15, () -> {
            helper.assertTrue(Math.abs(helper.relativeVec(tram.position()).x - 8.5D) < 0.5D, "the drive rail did not hold the tram");
            pig.startRiding(tram);
        });
        helper.succeedWhen(() -> helper.assertTrue(helper.relativeVec(tram.position()).x > 14.0D, "the tram has not left"));
    }

    /** A tram that pulls in with a rider stops at the station like any other, rather than releasing itself. */
    @GameTest(structure = TestWorld.ARENA, maxTicks = 200)
    public void arrivingWithARiderIsNotBoarding(GameTestHelper helper) {
        build(helper);
        TestWorld.rail(helper, STOP, ModContent.DRIVE_RAIL, RailShape.EAST_WEST);
        BlockPos start = new BlockPos(3, 1, STOP.getZ());
        TestWorld.rail(helper, start.east(), ModContent.DRIVE_RAIL, RailShape.EAST_WEST);
        helper.setBlock(start.east().south(), TestContent.SIGNAL);
        RedstoneTramEntity tram = helper.spawn(ModEntityTypes.REDSTONE_TRAM, start);
        Pig pig = rider(helper, start);
        pig.startRiding(tram);
        tram.setDeltaMovement(0.1D, 0.0D, 0.0D);

        helper.runAtTickTime(120, () -> {
            helper.assertBlockProperty(STATION, VehicleStationBlock.ACTIVE, true);
            helper.assertBlockProperty(STATION, VehicleStationBlock.POWER, 0);
            helper.assertTrue(Math.abs(helper.relativeVec(tram.position()).x - 8.5D) < 1.0D, "the tram did not stop at the station");
            helper.succeed();
        });
    }

    /** The second car of a train comes into range after the first; its rider has not just boarded. */
    @GameTest(structure = TestWorld.ARENA, maxTicks = 200)
    public void aRiderFurtherBackInATrainIsNotBoarding(GameTestHelper helper) {
        build(helper);
        // The hold is past the station, so the second car comes to rest in front of it.
        TestWorld.rail(helper, STOP.east(2), ModContent.DRIVE_RAIL, RailShape.EAST_WEST);
        TestWorld.rail(helper, new BlockPos(6, 1, STOP.getZ()), ModContent.DRIVE_RAIL, RailShape.EAST_WEST);
        helper.setBlock(new BlockPos(6, 1, STOP.getZ() + 1), TestContent.SIGNAL);
        AbstractMinecart lead = helper.spawn(EntityTypes.MINECART, new BlockPos(5, 1, STOP.getZ()));
        AbstractMinecart tail = helper.spawn(EntityTypes.MINECART, new BlockPos(3, 1, STOP.getZ()));
        RailGameTests.link(lead, tail);
        Pig pig = rider(helper, new BlockPos(3, 1, STOP.getZ()));
        pig.startRiding(tail);
        lead.setDeltaMovement(0.1D, 0.0D, 0.0D);

        boolean[] seen = new boolean[1];
        helper.onEachTick(() -> {
            seen[0] |= helper.getBlockState(STATION).getValue(VehicleStationBlock.RIDDEN);
            helper.assertBlockProperty(STATION, VehicleStationBlock.POWER, 0);
        });
        helper.runAtTickTime(120, () -> {
            helper.assertTrue(seen[0], "the rider never came into the station's range");
            helper.succeed();
        });
    }

    /** The signal now means boarding; a comparator still reads whether a cart is here. */
    @GameTest(structure = TestWorld.ARENA, maxTicks = 40)
    public void aComparatorReadsACartAtTheStation(GameTestHelper helper) {
        build(helper);
        BlockPos station = helper.absolutePos(STATION);
        helper.assertValueEqual(helper.getBlockState(STATION).getAnalogOutputSignal(helper.getLevel(), station, Direction.NORTH), 0, "comparator with no cart");
        helper.spawn(EntityTypes.MINECART, STOP);
        helper.runAtTickTime(15, () -> {
            helper.assertValueEqual(helper.getBlockState(STATION).getAnalogOutputSignal(helper.getLevel(), station, Direction.NORTH), 15, "comparator with a cart");
            helper.succeed();
        });
    }
}
