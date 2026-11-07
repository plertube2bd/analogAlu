package com.example.redstonetransportation.test;

import com.example.redstonetransportation.block.ElevatorCallBlock;
import com.example.redstonetransportation.block.RouteBeaconBlock;
import com.example.redstonetransportation.block.StructureCoreBlock;
import com.example.redstonetransportation.content.ModContent;
import com.example.redstonetransportation.item.StructureMarkerItem;
import com.example.redstonetransportation.structure.MovingStructureEntity;
import com.example.redstonetransportation.structure.StructureActions;
import com.example.redstonetransportation.structure.StructureAssembler;
import com.example.redstonetransportation.structure.StructureKind;
import com.example.redstonetransportation.structure.StructurePose;
import com.mojang.logging.LogUtils;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

public final class StructureGameTests {
    private static final Input FORWARD = new Input(true, false, false, false, false, false, false);
    private static final Input RIGHT = new Input(false, false, false, true, false, false, false);
    private static final Input CLIMB = new Input(false, false, false, false, true, false, false);

    private static void core(GameTestHelper helper, BlockPos pos, Block block, Direction facing) {
        helper.setBlock(pos, block.defaultBlockState().setValue(StructureCoreBlock.FACING, facing));
        if (block == ModContent.SHIP_HELM || block == ModContent.AIRSHIP_HELM) fuelTank(helper, pos, facing, 4);
    }

    /**
     * Ships and airships run on diesel: a tank with some in it takes the place of the deck block under the
     * helm, or goes right behind the helm where there is no deck under it.
     */
    static BlockPos fuelTank(GameTestHelper helper, BlockPos helm, Direction facing, int diesel) {
        BlockState under = helper.getBlockState(helm.below());
        boolean deck = !under.isAir() && !under.is(Blocks.STONE) && !under.is(ModContent.LIFT_BALLOON)
                && under.getFluidState().isEmpty();
        BlockPos tank = deck ? helm.below() : helm.relative(facing.getOpposite());
        helper.setBlock(tank, ModContent.FUEL_TANK);
        com.example.redstonetransportation.block.FuelTankBlockEntity fuel =
                helper.getBlockEntity(tank, com.example.redstonetransportation.block.FuelTankBlockEntity.class);
        for (int i = 0; i < diesel; i++) fuel.addDiesel();
        return tank;
    }

    private static void plate(GameTestHelper helper, int x0, int z0, int x1, int z1, int y, Block block) {
        for (int x = x0; x <= x1; x++) {
            for (int z = z0; z <= z1; z++) {
                helper.setBlock(new BlockPos(x, y, z), block);
            }
        }
    }

    private static StructureAssembler.Result assemble(GameTestHelper helper, BlockPos core) {
        return StructureAssembler.assemble(helper.getLevel(), helper.absolutePos(core), null);
    }

    private static MovingStructureEntity assembled(GameTestHelper helper, BlockPos core) {
        StructureAssembler.Result result = assemble(helper, core);
        helper.assertTrue(result.structure() != null, "not assembled: " + result.message().getString());
        return result.structure();
    }

    private static ServerPlayer driver(GameTestHelper helper, MovingStructureEntity structure, Input input) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.setPos(structure.position());
        player.startRiding(structure);
        player.setLastClientInput(input);
        return player;
    }

    private static void water(GameTestHelper helper, int y) {
        for (int x = 0; x < TestWorld.SIZE; x++) {
            for (int z = 0; z < TestWorld.SIZE; z++) {
                helper.setBlock(new BlockPos(x, y, z), Blocks.WATER);
            }
        }
    }

    /** Every block goes, every block comes back, and a chest keeps what was in it. */
    @GameTest(structure = TestWorld.ARENA, maxTicks = 40)
    public void assemblyAndRestoreKeepEveryBlock(GameTestHelper helper) {
        TestWorld.floor(helper, Blocks.STONE);
        BlockPos coreAt = new BlockPos(10, 2, 10);
        plate(helper, 8, 8, 12, 12, 1, Blocks.OAK_PLANKS);
        core(helper, coreAt, ModContent.ELEVATOR_CORE, Direction.NORTH);
        BlockPos chestAt = new BlockPos(9, 2, 10);
        helper.setBlock(chestAt, Blocks.CHEST);
        ((Container) helper.getBlockEntity(chestAt, net.minecraft.world.level.block.entity.ChestBlockEntity.class))
                .setItem(0, new ItemStack(Items.DIAMOND, 7));

        MovingStructureEntity structure = assembled(helper, coreAt);
        helper.assertTrue(structure.blocks().size() == 27, "expected 27 blocks, got " + structure.blocks().size());
        helper.assertBlockPresent(Blocks.AIR, coreAt);
        helper.assertBlockPresent(Blocks.AIR, chestAt);
        helper.assertBlockPresent(Blocks.AIR, new BlockPos(8, 1, 8));
        helper.assertBlockPresent(Blocks.STONE, new BlockPos(8, 0, 8));

        helper.runAtTickTime(5, () -> {
            StructureAssembler.restore(helper.getLevel(), structure);
            helper.assertTrue(structure.isRemoved(), "the entity stayed after restoring");
            helper.assertBlockPresent(ModContent.ELEVATOR_CORE, coreAt);
            helper.assertBlockPresent(Blocks.OAK_PLANKS, new BlockPos(12, 1, 12));
            Container chest = helper.getBlockEntity(chestAt, net.minecraft.world.level.block.entity.ChestBlockEntity.class);
            helper.assertTrue(chest.getItem(0).is(Items.DIAMOND) && chest.getItem(0).getCount() == 7, "chest contents were lost");
            helper.succeed();
        });
    }

    /** A structure on the ground takes none of the ground, and a hill never counts as part of it. */
    @GameTest(structure = TestWorld.ARENA, maxTicks = 20)
    public void searchStopsAtTerrain(GameTestHelper helper) {
        TestWorld.floor(helper, Blocks.GRASS_BLOCK);
        helper.setBlock(new BlockPos(11, 0, 10), Blocks.DIRT);
        BlockPos coreAt = new BlockPos(10, 2, 10);
        helper.setBlock(new BlockPos(10, 1, 10), Blocks.OAK_PLANKS);
        core(helper, coreAt, ModContent.ELEVATOR_CORE, Direction.NORTH);
        helper.assertTrue(assembled(helper, coreAt).blocks().size() == 2, "the ground came along");
        helper.assertBlockPresent(Blocks.GRASS_BLOCK, new BlockPos(10, 0, 10));
        helper.succeed();
    }

    /** Leaves a player put there are part of the build; a tree's own leaves are not. */
    @GameTest(structure = TestWorld.ARENA, maxTicks = 20)
    public void onlyPlacedLeavesComeAlong(GameTestHelper helper) {
        TestWorld.floor(helper, Blocks.STONE);
        BlockPos coreAt = new BlockPos(10, 2, 10);
        core(helper, coreAt, ModContent.ELEVATOR_CORE, Direction.NORTH);
        helper.setBlock(new BlockPos(11, 2, 10), Blocks.OAK_LEAVES.defaultBlockState().setValue(LeavesBlock.PERSISTENT, true));
        helper.setBlock(new BlockPos(9, 2, 10), Blocks.OAK_LEAVES.defaultBlockState().setValue(LeavesBlock.PERSISTENT, false));
        helper.assertTrue(assembled(helper, coreAt).blocks().size() == 2, "leaves were not told apart");
        helper.assertBlockPresent(Blocks.OAK_LEAVES, new BlockPos(9, 2, 10));
        helper.assertBlockPresent(Blocks.AIR, new BlockPos(11, 2, 10));
        helper.succeed();
    }

    /** Without a marked area a car touching its shaft would take the building with it. */
    @GameTest(structure = TestWorld.ARENA, maxTicks = 20)
    public void markedAreaLeavesTheShaftBehind(GameTestHelper helper) {
        TestWorld.floor(helper, Blocks.STONE);
        BlockPos coreAt = new BlockPos(10, 2, 10);
        plate(helper, 9, 9, 11, 11, 1, Blocks.OAK_PLANKS);
        core(helper, coreAt, ModContent.ELEVATOR_CORE, Direction.NORTH);
        for (int y = 1; y <= 4; y++) helper.setBlock(new BlockPos(12, y, 10), Blocks.BRICKS);

        BoundingBox car = BoundingBox.fromCorners(helper.absolutePos(new BlockPos(9, 1, 9)), helper.absolutePos(new BlockPos(11, 2, 11)));
        StructureAssembler.Result result = StructureAssembler.assemble(helper.getLevel(), helper.absolutePos(coreAt), car);
        helper.assertTrue(result.structure() != null && result.structure().blocks().size() == 10, "the marked car was not taken alone");
        helper.assertBlockPresent(Blocks.BRICKS, new BlockPos(12, 2, 10));
        helper.succeed();
    }

    /** The marker on a core: one corner marked is another mark, both corners marked is assembly. */
    @GameTest(structure = TestWorld.ARENA, maxTicks = 20)
    public void markerUsesTheCoreOnlyOnceBothCornersAreSet(GameTestHelper helper) {
        TestWorld.floor(helper, Blocks.STONE);
        BlockPos coreAt = new BlockPos(10, 2, 10);
        BlockPos plank = new BlockPos(10, 2, 11);
        core(helper, coreAt, ModContent.ELEVATOR_CORE, Direction.NORTH);
        helper.setBlock(plank, Blocks.OAK_PLANKS);
        helper.setBlock(new BlockPos(10, 2, 12), Blocks.BRICKS);

        Player player = TestWorld.creativePlayer(helper);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModContent.STRUCTURE_MARKER_ITEM));
        helper.assertTrue(useOn(helper, player, coreAt) == InteractionResult.PASS, "one click was treated as assembly");
        ModContent.STRUCTURE_MARKER_ITEM.useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, hit(helper, coreAt)));
        helper.assertTrue(StructureMarkerItem.selection(player) == null, "one corner made an area");
        ModContent.STRUCTURE_MARKER_ITEM.useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, hit(helper, plank)));
        helper.assertTrue(StructureMarkerItem.selection(player) != null, "two corners made no area");

        helper.assertTrue(useOn(helper, player, coreAt).consumesAction(), "the marked core did not assemble");
        helper.assertBlockPresent(Blocks.AIR, plank);
        helper.assertBlockPresent(Blocks.BRICKS, new BlockPos(10, 2, 12));
        helper.succeed();
    }

    private static BlockHitResult hit(GameTestHelper helper, BlockPos pos) {
        return new BlockHitResult(Vec3.atCenterOf(helper.absolutePos(pos)), Direction.UP, helper.absolutePos(pos), false);
    }

    private static InteractionResult useOn(GameTestHelper helper, Player player, BlockPos pos) {
        BlockPos absolute = helper.absolutePos(pos);
        return helper.getLevel().getBlockState(absolute).useItemOn(player.getMainHandItem(), helper.getLevel(), player, InteractionHand.MAIN_HAND, hit(helper, pos));
    }

    @GameTest(structure = TestWorld.ARENA, maxTicks = 20)
    public void twoCoresAreRefusedAndNothingChanges(GameTestHelper helper) {
        TestWorld.floor(helper, Blocks.STONE);
        BlockPos coreAt = new BlockPos(10, 2, 10);
        plate(helper, 9, 9, 11, 11, 1, Blocks.OAK_PLANKS);
        core(helper, coreAt, ModContent.SHIP_HELM, Direction.NORTH);
        core(helper, new BlockPos(11, 2, 10), ModContent.AIRSHIP_HELM, Direction.NORTH);
        helper.assertTrue(assemble(helper, coreAt).structure() == null, "two cores were accepted");
        helper.assertBlockPresent(ModContent.SHIP_HELM, coreAt);
        helper.succeed();
    }

    @GameTest(structure = TestWorld.ARENA, maxTicks = 20)
    public void shipNeedsWaterAndAirshipNeedsBalloons(GameTestHelper helper) {
        TestWorld.floor(helper, Blocks.STONE);
        BlockPos coreAt = new BlockPos(10, 2, 10);
        plate(helper, 5, 5, 15, 15, 1, Blocks.OAK_PLANKS);
        core(helper, coreAt, ModContent.SHIP_HELM, Direction.NORTH);
        helper.assertTrue(assemble(helper, coreAt).structure() == null, "a ship was assembled on dry land");

        core(helper, coreAt, ModContent.AIRSHIP_HELM, Direction.NORTH);
        helper.assertTrue(assemble(helper, coreAt).structure() == null, "an airship was assembled with no balloons");
        for (int x = 5; x <= 14; x++) helper.setBlock(new BlockPos(x, 2, 5), ModContent.LIFT_BALLOON);
        helper.assertTrue(assemble(helper, coreAt).structure() != null, "enough balloons did not lift the airship");
        helper.succeed();
    }

    /** A call block brings the elevator to its floor, with what stands on the car riding along. */
    @GameTest(structure = TestWorld.ARENA, maxTicks = 200, skyAccess = true)
    public void callBlockBringsTheElevatorAndCarriesRiders(GameTestHelper helper) {
        TestWorld.floor(helper, Blocks.STONE);
        BlockPos coreAt = new BlockPos(10, 1, 10);
        plate(helper, 9, 9, 11, 11, 1, Blocks.IRON_BLOCK);
        core(helper, coreAt, ModContent.ELEVATOR_CORE, Direction.NORTH);
        BlockPos top = new BlockPos(13, 7, 10);
        helper.setBlock(top, ModContent.ELEVATOR_CALL);
        MovingStructureEntity elevator = assembled(helper, coreAt);

        Zombie rider = helper.spawnWithNoFreeWill(EntityTypes.ZOMBIE, new BlockPos(9, 2, 9));
        rider.setPos(helper.absoluteVec(new Vec3(9.5D, 2.0D, 9.5D)));

        helper.runAtTickTime(5, () -> helper.setBlock(top.east(), Blocks.REDSTONE_BLOCK));
        helper.runAtTickTime(60, () -> {
            helper.assertBlockProperty(top, ElevatorCallBlock.POWERED, true);
            Vec3 car = helper.relativeVec(elevator.position());
            helper.assertValueInBetween(6.99D, car.y, 7.01D, "elevator height");
            helper.assertTrue(!elevator.isMoving(), "the elevator did not stop at the floor");
            helper.assertValueInBetween(7.9D, helper.relativeVec(rider.position()).y, 8.2D, "rider height");
            helper.succeed();
        });
    }

    /** Seated on the core, Space goes to the next floor up, past any height with no call block. */
    @GameTest(structure = TestWorld.ARENA, maxTicks = 200, skyAccess = true)
    public void seatedRiderGoesToTheNextFloor(GameTestHelper helper) {
        TestWorld.floor(helper, Blocks.STONE);
        BlockPos coreAt = new BlockPos(10, 1, 10);
        core(helper, coreAt, ModContent.ELEVATOR_CORE, Direction.NORTH);
        // Mounted at head height on the ground floor, so the floors above count from that height.
        helper.setBlock(new BlockPos(12, 3, 10), ModContent.ELEVATOR_CALL);
        helper.setBlock(new BlockPos(12, 9, 10), ModContent.ELEVATOR_CALL);
        MovingStructureEntity elevator = assembled(helper, coreAt);
        ServerPlayer rider = driver(helper, elevator, CLIMB);

        helper.runAtTickTime(4, () -> rider.setLastClientInput(Input.EMPTY));
        helper.runAtTickTime(60, () -> {
            helper.assertValueInBetween(6.99D, helper.relativeVec(elevator.position()).y, 7.01D, "elevator height");
            helper.assertTrue(!elevator.isMoving(), "the elevator did not stop at the floor");
            helper.succeed();
        });
    }

    @GameTest(structure = TestWorld.ARENA, maxTicks = 100)
    public void elevatorStopsUnderACeiling(GameTestHelper helper) {
        TestWorld.floor(helper, Blocks.STONE);
        BlockPos coreAt = new BlockPos(10, 1, 10);
        core(helper, coreAt, ModContent.ELEVATOR_CORE, Direction.NORTH);
        helper.setBlock(new BlockPos(10, 5, 10), Blocks.STONE);
        BlockPos above = new BlockPos(12, 8, 10);
        helper.setBlock(above, ModContent.ELEVATOR_CALL);
        MovingStructureEntity elevator = assembled(helper, coreAt);
        helper.runAtTickTime(2, () -> MovingStructureEntity.callTo(helper.getLevel(), helper.absolutePos(above)));
        helper.runAtTickTime(60, () -> {
            helper.assertValueInBetween(3.99D, helper.relativeVec(elevator.position()).y, 4.01D, "stopped height");
            helper.succeed();
        });
    }

    /** Ahead of the bow is water, then a wall: the ship sails, stops, and stays clear of the wall. */
    @GameTest(structure = TestWorld.ARENA, maxTicks = 300)
    public void shipSailsAndStopsAtTheShore(GameTestHelper helper) {
        TestWorld.floor(helper, Blocks.STONE);
        water(helper, 1);
        BlockPos coreAt = new BlockPos(5, 3, 15);
        plate(helper, 4, 14, 6, 16, 2, Blocks.OAK_PLANKS);
        core(helper, coreAt, ModContent.SHIP_HELM, Direction.EAST);
        for (int z = 0; z < TestWorld.SIZE; z++) {
            for (int y = 1; y <= 4; y++) helper.setBlock(new BlockPos(20, y, z), Blocks.STONE);
        }
        MovingStructureEntity ship = assembled(helper, coreAt);
        driver(helper, ship, FORWARD);

        helper.runAtTickTime(20, () -> helper.assertTrue(ship.isMoving(), "the ship is not sailing"));
        helper.runAtTickTime(250, () -> {
            double x = helper.relativeVec(ship.position()).x;
            helper.assertValueInBetween(17.0D, x, 18.6D, "bow against the wall");
            helper.succeed();
        });
    }

    /** A ship cannot leave the water, however clear the way is. */
    @GameTest(structure = TestWorld.ARENA, maxTicks = 300)
    public void shipStaysOnWater(GameTestHelper helper) {
        TestWorld.floor(helper, Blocks.STONE);
        for (int z = 0; z < TestWorld.SIZE; z++) {
            for (int x = 0; x < 12; x++) helper.setBlock(new BlockPos(x, 1, z), Blocks.WATER);
            // The bank, so the water ends here rather than running out over the floor.
            for (int x = 12; x < TestWorld.SIZE; x++) helper.setBlock(new BlockPos(x, 1, z), Blocks.STONE);
        }
        BlockPos coreAt = new BlockPos(5, 3, 15);
        plate(helper, 4, 14, 6, 16, 2, Blocks.OAK_PLANKS);
        core(helper, coreAt, ModContent.SHIP_HELM, Direction.EAST);
        MovingStructureEntity ship = assembled(helper, coreAt);
        driver(helper, ship, FORWARD);
        helper.runAtTickTime(200, () -> {
            // The hull is one block wider than the core each way, and its bow has to stay short of the bank.
            helper.assertValueInBetween(10.0D, helper.relativeVec(ship.position()).x, 11.01D, "stopped at the shoreline");
            helper.succeed();
        });
    }

    /** A turned structure sets down on the nearest quarter turn, with its blocks turned to match. */
    @GameTest(structure = TestWorld.ARENA, maxTicks = 300)
    public void turnedAirshipRestoresTurned(GameTestHelper helper) {
        TestWorld.floor(helper, Blocks.STONE);
        BlockPos coreAt = new BlockPos(15, 4, 15);
        core(helper, coreAt, ModContent.AIRSHIP_HELM, Direction.NORTH);
        helper.setBlock(new BlockPos(15, 5, 15), ModContent.LIFT_BALLOON);
        helper.setBlock(new BlockPos(15, 4, 13), Blocks.OAK_PLANKS);
        helper.setBlock(new BlockPos(15, 4, 14), Blocks.OAK_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.NORTH));
        MovingStructureEntity airship = assembled(helper, coreAt);
        ServerPlayer driver = driver(helper, airship, RIGHT);

        helper.runAtTickTime(70, () -> driver.setLastClientInput(Input.EMPTY));
        helper.runAtTickTime(90, () -> {
            helper.assertValueInBetween(45.0D, (double) Math.abs(airship.turned(airship.getYRot())), 135.0D, "turned about a quarter");
            driver.stopRiding();
            helper.assertTrue(!airship.isMoving(), "still turning");
            StructureAssembler.restore(helper.getLevel(), airship);
            // Yaw grows clockwise from above: the block north of the core is now east of it.
            helper.assertBlockPresent(Blocks.OAK_PLANKS, new BlockPos(17, 4, 15));
            BlockState stair = helper.getBlockState(new BlockPos(16, 4, 15));
            helper.assertTrue(stair.is(Blocks.OAK_STAIRS) && stair.getValue(StairBlock.FACING) == Direction.EAST, "stairs did not turn: " + stair);
            helper.succeed();
        });
    }

    @GameTest(structure = TestWorld.ARENA, maxTicks = 100)
    public void airshipClimbsAndStopsUnderABlock(GameTestHelper helper) {
        TestWorld.floor(helper, Blocks.STONE);
        BlockPos coreAt = new BlockPos(15, 2, 15);
        core(helper, coreAt, ModContent.AIRSHIP_HELM, Direction.NORTH);
        helper.setBlock(new BlockPos(15, 3, 15), ModContent.LIFT_BALLOON);
        helper.setBlock(new BlockPos(15, 7, 15), Blocks.STONE);
        MovingStructureEntity airship = assembled(helper, coreAt);
        driver(helper, airship, CLIMB);
        helper.runAtTickTime(70, () -> {
            helper.assertValueInBetween(4.9D, helper.relativeVec(airship.position()).y, 5.02D, "stopped under the block");
            helper.succeed();
        });
    }

    /** A signal sends the ship on its way, and a comparator can see it arrive. */
    @GameTest(structure = TestWorld.ARENA, maxTicks = 300)
    public void beaconSendsTheShipAndSeesItDock(GameTestHelper helper) {
        TestWorld.floor(helper, Blocks.STONE);
        water(helper, 1);
        BlockPos coreAt = new BlockPos(5, 3, 15);
        plate(helper, 4, 14, 6, 16, 2, Blocks.OAK_PLANKS);
        core(helper, coreAt, ModContent.SHIP_HELM, Direction.NORTH);
        BlockPos beacon = new BlockPos(20, 1, 15);
        helper.setBlock(beacon, ModContent.ROUTE_BEACON);
        MovingStructureEntity ship = assembled(helper, coreAt);

        helper.runAtTickTime(2, () -> helper.setBlock(beacon.north(), Blocks.REDSTONE_BLOCK));
        helper.succeedWhen(() -> {
            helper.assertBlockProperty(beacon, RouteBeaconBlock.DOCKED, true);
            Vec3 at = helper.relativeVec(ship.position());
            // The beacon stands where the water ends, so the bow stops at it rather than the core reaching it.
            helper.assertValueInBetween(15.0D, at.x, 22.0D, "ship x");
            helper.assertValueInBetween(13.0D, at.z, 18.0D, "ship z");
            helper.assertTrue(helper.getBlockState(beacon).getAnalogOutputSignal(helper.getLevel(), helper.absolutePos(beacon), Direction.NORTH) == 15, "a comparator reads nothing");
        });
    }

    /** The beacon's signal strength sets the speed, the same scale a drive rail uses. */
    @GameTest(structure = TestWorld.ARENA, maxTicks = 100)
    public void weakBeaconSignalSailsSlowly(GameTestHelper helper) {
        TestWorld.floor(helper, Blocks.STONE);
        water(helper, 1);
        BlockPos coreAt = new BlockPos(5, 3, 15);
        plate(helper, 4, 14, 6, 16, 2, Blocks.OAK_PLANKS);
        core(helper, coreAt, ModContent.SHIP_HELM, Direction.EAST);
        // Close enough that no ship in a neighbouring test is nearer to it.
        BlockPos beacon = new BlockPos(16, 1, 15);
        helper.setBlock(beacon, ModContent.ROUTE_BEACON);
        MovingStructureEntity ship = assembled(helper, coreAt);
        double start = helper.relativeVec(ship.position()).x;
        helper.runAtTickTime(1, () -> MovingStructureEntity.sendTo(helper.getLevel(), helper.absolutePos(beacon), 0.25D));
        helper.runAtTickTime(61, () -> {
            helper.assertValueInBetween(1.0D, helper.relativeVec(ship.position()).x - start, 6.0D, "distance at quarter speed");
            helper.succeed();
        });
    }

    /** An airship with a wall in its way goes over it, then comes down onto the beacon. */
    @GameTest(structure = TestWorld.ARENA, maxTicks = 500, skyAccess = true)
    public void airshipFliesOverAWallToItsBeacon(GameTestHelper helper) {
        TestWorld.floor(helper, Blocks.STONE);
        BlockPos coreAt = new BlockPos(5, 2, 15);
        core(helper, coreAt, ModContent.AIRSHIP_HELM, Direction.EAST);
        helper.setBlock(coreAt.above(), ModContent.LIFT_BALLOON);
        for (int z = 0; z < TestWorld.SIZE; z++) {
            for (int y = 1; y <= 6; y++) helper.setBlock(new BlockPos(12, y, z), Blocks.STONE);
        }
        BlockPos beacon = new BlockPos(20, 1, 15);
        helper.setBlock(beacon, ModContent.ROUTE_BEACON);
        MovingStructureEntity airship = assembled(helper, coreAt);
        helper.runAtTickTime(1, () -> MovingStructureEntity.sendTo(helper.getLevel(), helper.absolutePos(beacon), 1.0D));

        helper.succeedWhen(() -> {
            helper.assertBlockProperty(beacon, RouteBeaconBlock.DOCKED, true);
            helper.assertValueInBetween(1.9D, helper.relativeVec(airship.position()).y, 2.1D, "landing height");
        });
    }

    /** A moving wall shoves what stands in its way. */
    @GameTest(structure = TestWorld.ARENA, maxTicks = 100)
    public void movingStructurePushesMobs(GameTestHelper helper) {
        TestWorld.floor(helper, Blocks.STONE);
        BlockPos coreAt = new BlockPos(5, 1, 15);
        core(helper, coreAt, ModContent.AIRSHIP_HELM, Direction.EAST);
        helper.setBlock(coreAt.above(), ModContent.LIFT_BALLOON);
        Zombie zombie = helper.spawnWithNoFreeWill(EntityTypes.ZOMBIE, new Vec3(9.5D, 1.0D, 15.5D));
        MovingStructureEntity airship = assembled(helper, coreAt);
        driver(helper, airship, FORWARD);
        helper.runAtTickTime(60, () -> {
            double wall = helper.relativeVec(airship.position()).x + 0.5D;
            double mob = helper.relativeVec(zombie.position()).x;
            helper.assertTrue(wall > 12.0D, "the airship did not get going: " + wall);
            helper.assertTrue(mob > wall, "the zombie was left inside the wall: " + mob + " against " + wall);
            helper.succeed();
        });
    }

    /** Pinned against a wall, a mob stops the structure rather than being pushed through. */
    @GameTest(structure = TestWorld.ARENA, maxTicks = 100)
    public void structureStopsRatherThanCrush(GameTestHelper helper) {
        TestWorld.floor(helper, Blocks.STONE);
        BlockPos coreAt = new BlockPos(5, 1, 15);
        core(helper, coreAt, ModContent.AIRSHIP_HELM, Direction.EAST);
        helper.setBlock(coreAt.above(), ModContent.LIFT_BALLOON);
        for (int z = 0; z < TestWorld.SIZE; z++) {
            for (int y = 1; y <= 3; y++) helper.setBlock(new BlockPos(11, y, z), Blocks.STONE);
        }
        Zombie zombie = helper.spawnWithNoFreeWill(EntityTypes.ZOMBIE, new Vec3(10.5D, 1.0D, 15.5D));
        MovingStructureEntity airship = assembled(helper, coreAt);
        driver(helper, airship, FORWARD);
        helper.runAtTickTime(60, () -> {
            double wall = helper.relativeVec(airship.position()).x + 0.5D;
            helper.assertTrue(wall < helper.relativeVec(zombie.position()).x, "the airship drove through the zombie: " + wall);
            helper.assertTrue(helper.relativeVec(zombie.position()).x <= 11.0D, "the zombie went through the wall");
            helper.succeed();
        });
    }

    /** An elevator stops above someone standing under it. */
    @GameTest(structure = TestWorld.ARENA, maxTicks = 100, skyAccess = true)
    public void elevatorStopsAboveWhoIsUnderIt(GameTestHelper helper) {
        TestWorld.floor(helper, Blocks.STONE);
        BlockPos coreAt = new BlockPos(10, 8, 10);
        core(helper, coreAt, ModContent.ELEVATOR_CORE, Direction.NORTH);
        helper.setBlock(new BlockPos(13, 1, 10), ModContent.ELEVATOR_CALL);
        Zombie zombie = helper.spawnWithNoFreeWill(EntityTypes.ZOMBIE, new Vec3(10.5D, 1.0D, 10.5D));
        MovingStructureEntity elevator = assembled(helper, coreAt);
        helper.runAtTickTime(1, () -> MovingStructureEntity.callTo(helper.getLevel(), helper.absolutePos(new BlockPos(13, 1, 10))));
        helper.runAtTickTime(70, () -> {
            helper.assertValueInBetween(2.9D, helper.relativeVec(elevator.position()).y, 4.1D, "stopped above the zombie");
            helper.assertValueInBetween(0.9D, helper.relativeVec(zombie.position()).y, 1.1D, "zombie height");
            helper.succeed();
        });
    }

    /** Rising towards a ceiling, the car stops while whoever stands on it still fits. */
    @GameTest(structure = TestWorld.ARENA, maxTicks = 100, skyAccess = true)
    public void elevatorStopsBeforeCrushingItsRider(GameTestHelper helper) {
        TestWorld.floor(helper, Blocks.STONE);
        BlockPos coreAt = new BlockPos(10, 1, 10);
        core(helper, coreAt, ModContent.ELEVATOR_CORE, Direction.NORTH);
        helper.setBlock(new BlockPos(10, 7, 10), Blocks.STONE);
        BlockPos top = new BlockPos(13, 6, 10);
        helper.setBlock(top, ModContent.ELEVATOR_CALL);
        Zombie rider = helper.spawnWithNoFreeWill(EntityTypes.ZOMBIE, new Vec3(10.5D, 2.0D, 10.5D));
        MovingStructureEntity elevator = assembled(helper, coreAt);
        helper.runAtTickTime(1, () -> MovingStructureEntity.callTo(helper.getLevel(), helper.absolutePos(top)));
        helper.runAtTickTime(70, () -> {
            helper.assertValueInBetween(3.9D, helper.relativeVec(elevator.position()).y, 4.1D, "stopped below the ceiling");
            helper.assertTrue(helper.relativeVec(rider.position()).y + rider.getBbHeight() <= 7.01D, "the rider was pushed into the ceiling");
            helper.succeed();
        });
    }

    /** At 45 degrees a block's collision follows the drawn diamond, not a square around its centre. */
    @GameTest(structure = TestWorld.ARENA, maxTicks = 40)
    public void turnedBlocksCollideWhereTheyAreDrawn(GameTestHelper helper) {
        TestWorld.floor(helper, Blocks.STONE);
        BlockPos coreAt = new BlockPos(15, 5, 15);
        core(helper, coreAt, ModContent.AIRSHIP_HELM, Direction.NORTH);
        helper.setBlock(coreAt.above(), ModContent.LIFT_BALLOON);
        helper.setBlock(new BlockPos(15, 5, 12), Blocks.OAK_PLANKS);
        helper.setBlock(new BlockPos(15, 5, 13), Blocks.OAK_PLANKS);
        helper.setBlock(new BlockPos(15, 5, 14), Blocks.OAK_PLANKS);
        MovingStructureEntity airship = assembled(helper, coreAt);
        helper.runAtTickTime(1, () -> airship.setYRot(airship.getYRot() + 45.0F));
        helper.runAtTickTime(3, () -> {
            StructurePose pose = airship.pose();
            double centerX = pose.worldX(0.0D, -3.0D);
            double centerZ = pose.worldZ(0.0D, -3.0D);
            double y = airship.getY() + 0.5D;
            helper.assertTrue(!helper.getLevel().noCollision(probe(centerX, y, centerZ)), "nothing at the block's centre");
            helper.assertTrue(helper.getLevel().noCollision(probe(centerX + 0.42D, y, centerZ + 0.42D)), "the corner of the old square still collides");
            helper.assertTrue(!helper.getLevel().noCollision(probe(centerX + 0.3D, y, centerZ + 0.3D)), "the diamond's edge does not collide");
            helper.succeed();
        });
    }

    private static AABB probe(double x, double y, double z) {
        return new AABB(x - 0.02D, y - 0.02D, z - 0.02D, x + 0.02D, y + 0.02D, z + 0.02D);
    }

    /** A chest aboard opens while the ship moves, and what goes in it stays there. */
    @GameTest(structure = TestWorld.ARENA, maxTicks = 80)
    public void chestsWorkWhileAboard(GameTestHelper helper) {
        TestWorld.floor(helper, Blocks.STONE);
        BlockPos coreAt = new BlockPos(10, 2, 10);
        plate(helper, 9, 9, 11, 11, 1, Blocks.OAK_PLANKS);
        core(helper, coreAt, ModContent.ELEVATOR_CORE, Direction.NORTH);
        helper.setBlock(new BlockPos(9, 2, 10), Blocks.CHEST);
        MovingStructureEntity structure = assembled(helper, coreAt);
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.setPos(structure.position().add(0.0D, 1.0D, 0.0D));

        helper.runAtTickTime(2, () -> {
            StructureActions.use(player, structure, new BlockPos(-1, 0, 0), Direction.UP, InteractionHand.MAIN_HAND);
            helper.assertTrue(player.containerMenu instanceof ChestMenu, "no chest menu opened: " + player.containerMenu);
            player.containerMenu.getSlot(4).set(new ItemStack(Items.EMERALD, 5));
            player.closeContainer();
        });
        helper.runAtTickTime(4, () -> {
            StructureAssembler.restore(helper.getLevel(), structure);
            Container chest = helper.getBlockEntity(new BlockPos(9, 2, 10), net.minecraft.world.level.block.entity.ChestBlockEntity.class);
            helper.assertTrue(chest.getItem(4).is(Items.EMERALD) && chest.getItem(4).getCount() == 5, "what went into the chest aboard was lost");
            helper.succeed();
        });
    }

    /** Doors swing, blocks go on and come off, and all of it is there when the structure sets down. */
    @GameTest(structure = TestWorld.ARENA, maxTicks = 40)
    public void blocksAboardCanBeEdited(GameTestHelper helper) {
        TestWorld.floor(helper, Blocks.STONE);
        BlockPos coreAt = new BlockPos(10, 2, 10);
        plate(helper, 8, 8, 12, 12, 1, Blocks.OAK_PLANKS);
        core(helper, coreAt, ModContent.ELEVATOR_CORE, Direction.NORTH);
        helper.setBlock(new BlockPos(11, 2, 10), Blocks.OAK_TRAPDOOR);
        MovingStructureEntity structure = assembled(helper, coreAt);
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.setGameMode(GameType.SURVIVAL);
        player.setPos(structure.position().add(0.0D, 1.0D, 0.0D));

        helper.runAtTickTime(2, () -> {
            StructureActions.use(player, structure, new BlockPos(1, 0, 0), Direction.UP, InteractionHand.MAIN_HAND);
            helper.assertTrue(structure.blocks().at(1, 0, 0).getValue(BlockStateProperties.OPEN), "the trapdoor did not open");

            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.BRICKS, 2));
            StructureActions.use(player, structure, new BlockPos(-1, -1, -1), Direction.UP, InteractionHand.MAIN_HAND);
            helper.assertTrue(structure.blocks().at(-1, 0, -1) != null, "the block was not placed");
            helper.assertTrue(player.getMainHandItem().getCount() == 1, "placing did not use up the item");

            player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            StructureActions.breakCell(player, structure, new BlockPos(2, -1, 2));
            helper.assertTrue(structure.blocks().at(2, -1, 2) == null, "the block was not broken");
            helper.assertTrue(player.getInventory().hasAnyMatching(stack -> stack.is(Items.OAK_PLANKS)), "the broken block was not handed over");

            StructureActions.breakCell(player, structure, BlockPos.ZERO);
            helper.assertTrue(structure.blocks().at(0, 0, 0) != null, "the core was broken");
        });
        helper.runAtTickTime(4, () -> {
            StructureAssembler.restore(helper.getLevel(), structure);
            helper.assertBlockPresent(Blocks.BRICKS, new BlockPos(9, 2, 9));
            helper.assertBlockPresent(Blocks.AIR, new BlockPos(12, 1, 12));
            helper.assertBlockProperty(new BlockPos(11, 2, 10), BlockStateProperties.OPEN, true);
            helper.succeed();
        });
    }

    /** A block put on a turned structure lands where it was aimed, and sets down facing the right way. */
    @GameTest(structure = TestWorld.ARENA, maxTicks = 40)
    public void placingOnATurnedStructureKeepsItsDirection(GameTestHelper helper) {
        TestWorld.floor(helper, Blocks.STONE);
        BlockPos coreAt = new BlockPos(15, 4, 15);
        core(helper, coreAt, ModContent.AIRSHIP_HELM, Direction.NORTH);
        helper.setBlock(coreAt.above(), ModContent.LIFT_BALLOON);
        MovingStructureEntity airship = assembled(helper, coreAt);
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.setPos(airship.position().add(3.0D, 0.0D, 0.0D));
        helper.runAtTickTime(1, () -> airship.setYRot(airship.getYRot() + 90.0F));
        helper.runAtTickTime(3, () -> {
            player.setYRot(90.0F);
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.OAK_STAIRS));
            StructureActions.use(player, airship, BlockPos.ZERO.above(), Direction.SOUTH, InteractionHand.MAIN_HAND);
            helper.assertTrue(airship.blocks().at(0, 1, 1) != null, "the stairs were not placed");
            StructureAssembler.restore(helper.getLevel(), airship);
            BlockState stairs = helper.getBlockState(new BlockPos(14, 5, 15));
            helper.assertTrue(stairs.is(Blocks.OAK_STAIRS) && stairs.getValue(StairBlock.FACING) == Direction.WEST, "the stairs turned: " + stairs);
            helper.succeed();
        });
    }

    /** Inside a hull below the waterline nothing is in the water, though the sea is still there. */
    @GameTest(structure = TestWorld.ARENA, maxTicks = 40, skyAccess = true)
    public void hullKeepsTheWaterOut(GameTestHelper helper) {
        TestWorld.floor(helper, Blocks.STONE);
        for (int y = 1; y <= 3; y++) water(helper, y);
        BlockPos coreAt = new BlockPos(10, 4, 7);
        plate(helper, 7, 7, 13, 13, 2, Blocks.OAK_PLANKS);
        for (int i = 7; i <= 13; i++) {
            for (int y = 3; y <= 4; y++) {
                helper.setBlock(new BlockPos(i, y, 7), Blocks.OAK_PLANKS);
                helper.setBlock(new BlockPos(i, y, 13), Blocks.OAK_PLANKS);
                helper.setBlock(new BlockPos(7, y, i), Blocks.OAK_PLANKS);
                helper.setBlock(new BlockPos(13, y, i), Blocks.OAK_PLANKS);
            }
        }
        core(helper, coreAt, ModContent.SHIP_HELM, Direction.NORTH);
        MovingStructureEntity ship = assembled(helper, coreAt);
        Zombie inside = helper.spawnWithNoFreeWill(EntityTypes.ZOMBIE, new Vec3(10.5D, 3.0D, 10.5D));
        Zombie outside = helper.spawnWithNoFreeWill(EntityTypes.ZOMBIE, new Vec3(20.5D, 3.0D, 10.5D));
        inside.setNoGravity(true);
        outside.setNoGravity(true);
        helper.runAtTickTime(10, () -> {
            helper.assertTrue(helper.getLevel().getFluidState(helper.absolutePos(new BlockPos(10, 3, 10))).is(FluidTags.WATER), "the sea filled no hole to keep out");
            helper.assertTrue(!inside.isInWater(), "the zombie in the hull is in the water");
            helper.assertTrue(outside.isInWater(), "the zombie outside is not in the water");
            helper.succeed();
        });
    }

    /** The biggest structure there is, moving: the time it costs a server tick. */
    @GameTest(structure = TestWorld.ARENA, maxTicks = 40)
    public void biggestStructureStaysCheap(GameTestHelper helper) {
        TestWorld.floor(helper, Blocks.STONE);
        BlockPos coreAt = new BlockPos(15, 3, 15);
        int placed = 1;
        for (int y = 3; y <= 7 && placed < StructureAssembler.MAX_BLOCKS; y++) {
            for (int x = 0; x < TestWorld.SIZE && placed < StructureAssembler.MAX_BLOCKS; x++) {
                for (int z = 0; z < TestWorld.SIZE && placed < StructureAssembler.MAX_BLOCKS; z++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    if (pos.equals(coreAt) || y == 5 && Math.abs(x - 15) > 13) continue;
                    helper.setBlock(pos, x % 4 == 0 && z % 4 == 0 ? ModContent.LIFT_BALLOON : Blocks.OAK_PLANKS);
                    placed++;
                }
            }
        }
        core(helper, coreAt, ModContent.AIRSHIP_HELM, Direction.EAST);
        MovingStructureEntity airship = assembled(helper, coreAt);
        helper.assertTrue(airship.blocks().size() > 900, "built too small a test: " + airship.blocks().size());
        driver(helper, airship, new Input(true, false, false, true, true, false, false));
        helper.runAtTickTime(20, () -> {
            long start = System.nanoTime();
            for (int i = 0; i < 20; i++) airship.tick();
            double milliseconds = (System.nanoTime() - start) / 20.0D / 1_000_000.0D;
            LogUtils.getLogger().info("Moving a structure of {} blocks costs {} ms per tick", airship.blocks().size(), String.format("%.3f", milliseconds));
            helper.assertTrue(milliseconds < 10.0D, "a tick took " + milliseconds + " ms");
            helper.succeed();
        });
    }

    /** Saving keeps the blocks and what an elevator knows, and the world can load it again. */
    @GameTest(structure = TestWorld.ARENA, maxTicks = 20)
    public void structureSurvivesSaveAndLoad(GameTestHelper helper) {
        TestWorld.floor(helper, Blocks.STONE);
        BlockPos coreAt = new BlockPos(10, 2, 10);
        plate(helper, 9, 9, 11, 11, 1, Blocks.OAK_PLANKS);
        core(helper, coreAt, ModContent.ELEVATOR_CORE, Direction.WEST);
        helper.setBlock(new BlockPos(9, 2, 10), Blocks.CHEST);
        ((Container) helper.getBlockEntity(new BlockPos(9, 2, 10), net.minecraft.world.level.block.entity.ChestBlockEntity.class))
                .setItem(3, new ItemStack(Items.APPLE, 2));
        MovingStructureEntity original = assembled(helper, coreAt);

        TagValueOutput output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, helper.getLevel().registryAccess());
        original.saveWithoutId(output);
        MovingStructureEntity loaded = com.example.redstonetransportation.vehicle.ModEntityTypes.MOVING_STRUCTURE
                .create(helper.getLevel(), EntitySpawnReason.LOAD);
        loaded.load(TagValueInput.create(ProblemReporter.DISCARDING, helper.getLevel().registryAccess(), output.buildResult()));

        helper.assertTrue(loaded.blocks().size() == original.blocks().size(), "blocks were lost in saving");
        helper.assertTrue(loaded.kind() == StructureKind.ELEVATOR, "kind was lost in saving");
        original.discard();
        helper.getLevel().addFreshEntity(loaded);
        StructureAssembler.restore(helper.getLevel(), loaded);
        Container chest = helper.getBlockEntity(new BlockPos(9, 2, 10), net.minecraft.world.level.block.entity.ChestBlockEntity.class);
        helper.assertTrue(chest.getItem(3).is(Items.APPLE) && chest.getItem(3).getCount() == 2, "chest contents were lost in saving");
        helper.succeed();
    }

    // ---- Diesel for ships and airships ----------------------------------------------------------

    private static com.example.redstonetransportation.block.FuelTankBlockEntity tank(GameTestHelper helper, BlockPos pos) {
        return helper.getBlockEntity(pos, com.example.redstonetransportation.block.FuelTankBlockEntity.class);
    }

    private static String messageKey(StructureAssembler.Result result) {
        return result.message().getContents() instanceof net.minecraft.network.chat.contents.TranslatableContents contents
                ? contents.getKey() : "";
    }

    /** With no fuel tank aboard, a ship or an airship is not assembled, and says what it needs. */
    @GameTest(structure = TestWorld.ARENA, maxTicks = 20)
    public void shipsAndAirshipsNeedAFuelTank(GameTestHelper helper) {
        TestWorld.floor(helper, Blocks.STONE);
        water(helper, 1);
        BlockPos shipAt = new BlockPos(5, 3, 15);
        plate(helper, 4, 14, 6, 16, 2, Blocks.OAK_PLANKS);
        helper.setBlock(shipAt, ModContent.SHIP_HELM.defaultBlockState().setValue(StructureCoreBlock.FACING, Direction.EAST));
        StructureAssembler.Result ship = assemble(helper, shipAt);
        helper.assertTrue(ship.structure() == null, "a ship with no fuel tank was assembled");
        helper.assertValueEqual(messageKey(ship), "message.redstone_transportation.structure_no_fuel_tank", "the ship's message");

        BlockPos airshipAt = new BlockPos(20, 4, 5);
        helper.setBlock(airshipAt, ModContent.AIRSHIP_HELM.defaultBlockState().setValue(StructureCoreBlock.FACING, Direction.EAST));
        helper.setBlock(airshipAt.above(), ModContent.LIFT_BALLOON);
        StructureAssembler.Result airship = assemble(helper, airshipAt);
        helper.assertTrue(airship.structure() == null, "an airship with no fuel tank was assembled");
        helper.assertValueEqual(messageKey(airship), "message.redstone_transportation.structure_no_fuel_tank", "the airship's message");

        // An empty tank is enough to assemble; it is the diesel in it that moves the vessel.
        fuelTank(helper, airshipAt, Direction.EAST, 0);
        helper.assertTrue(assemble(helper, airshipAt).structure() != null, "an airship with a fuel tank was refused");
        fuelTank(helper, shipAt, Direction.EAST, 0);
        helper.assertTrue(assemble(helper, shipAt).structure() != null, "a ship with a fuel tank was refused");
        helper.succeed();
    }

    /** Sailing burns a tick of diesel per tick; when the tank runs dry the ship slows to a stop. */
    @GameTest(structure = TestWorld.ARENA, maxTicks = 220)
    public void aShipRunsOutOfDieselAndStops(GameTestHelper helper) {
        TestWorld.floor(helper, Blocks.STONE);
        water(helper, 1);
        BlockPos coreAt = new BlockPos(5, 3, 15);
        plate(helper, 4, 14, 6, 16, 2, Blocks.OAK_PLANKS);
        core(helper, coreAt, ModContent.SHIP_HELM, Direction.EAST);
        var fuel = tank(helper, coreAt.below());
        fuel.drain(fuel.fuel() - 40);
        MovingStructureEntity ship = assembled(helper, coreAt);
        double startX = ship.getX();
        driver(helper, ship, FORWARD);
        double[] stoppedAt = new double[1];
        helper.runAtTickTime(20, () -> helper.assertTrue(ship.fuel() < 40 && ship.fuel() > 0, "fuel after 20 ticks: " + ship.fuel()));
        helper.runAtTickTime(140, () -> {
            helper.assertValueEqual(ship.fuel(), 0, "fuel left");
            helper.assertTrue(ship.getX() - startX > 2.0D, "the ship only went " + (ship.getX() - startX));
            stoppedAt[0] = ship.getX();
        });
        helper.runAtTickTime(200, () -> {
            helper.assertTrue(Math.abs(ship.getX() - stoppedAt[0]) < 1.0E-6D, "the ship kept going with no diesel");
            helper.assertFalse(ship.isMoving(), "the ship still counts as moving");
            helper.succeed();
        });
    }

    /** Out of diesel an airship hangs where its balloons hold it; diesel poured in gets it going again. */
    @GameTest(structure = TestWorld.ARENA, maxTicks = 160)
    public void anAirshipWithNoDieselHangsUntilRefuelled(GameTestHelper helper) {
        TestWorld.floor(helper, Blocks.STONE);
        BlockPos coreAt = new BlockPos(5, 4, 15);
        core(helper, coreAt, ModContent.AIRSHIP_HELM, Direction.EAST);
        helper.setBlock(coreAt.above(), ModContent.LIFT_BALLOON);
        var fuel = tank(helper, coreAt.west());
        fuel.drain(fuel.fuel());
        MovingStructureEntity airship = assembled(helper, coreAt);
        Vec3 start = airship.position();
        driver(helper, airship, new Input(true, false, false, false, true, false, false));
        helper.runAtTickTime(60, () -> {
            helper.assertTrue(airship.position().distanceTo(start) < 1.0E-6D, "an airship with no diesel moved");
            ServerPlayer crew = helper.makeMockServerPlayerInLevel();
            crew.setGameMode(GameType.SURVIVAL);
            crew.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(com.example.analogalu.ModItems.DIESEL, 2));
            airship.interact(crew, InteractionHand.MAIN_HAND, airship.position());
            helper.assertValueEqual(airship.fuel(), com.example.redstonetransportation.block.FuelTankBlockEntity.TICKS_PER_DIESEL,
                    "fuel after one diesel");
            helper.assertValueEqual(crew.getMainHandItem().getCount(), 1, "diesel left in hand");
        });
        helper.runAtTickTime(140, () -> {
            Vec3 moved = airship.position().subtract(start);
            helper.assertTrue(moved.x > 1.0D && moved.y > 0.5D, "the refuelled airship went only " + moved);
            helper.succeed();
        });
    }

    /** A route beacon only calls a vessel that can make the trip. */
    @GameTest(structure = TestWorld.ARENA, maxTicks = 40)
    public void aBeaconDoesNotCallAVesselWithNoDiesel(GameTestHelper helper) {
        TestWorld.floor(helper, Blocks.STONE);
        water(helper, 1);
        BlockPos coreAt = new BlockPos(5, 3, 15);
        plate(helper, 4, 14, 6, 16, 2, Blocks.OAK_PLANKS);
        core(helper, coreAt, ModContent.SHIP_HELM, Direction.EAST);
        var fuel = tank(helper, coreAt.below());
        fuel.drain(fuel.fuel());
        BlockPos beacon = new BlockPos(20, 1, 15);
        helper.setBlock(beacon, ModContent.ROUTE_BEACON);
        MovingStructureEntity ship = assembled(helper, coreAt);
        // Other tests' vessels may be in reach of the beacon, so watch this ship rather than the answer.
        MovingStructureEntity.sendTo(helper.getLevel(), helper.absolutePos(beacon), 1.0D);
        helper.assertFalse(ship.isMoving(), "the beacon called a ship with an empty tank");
        ServerPlayer crew = helper.makeMockServerPlayerInLevel();
        crew.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(com.example.analogalu.ModItems.DIESEL));
        ship.interact(crew, InteractionHand.MAIN_HAND, ship.position());
        MovingStructureEntity.sendTo(helper.getLevel(), helper.absolutePos(beacon), 1.0D);
        helper.assertTrue(ship.isMoving(), "the beacon did not call the refuelled ship");
        helper.succeed();
    }

    // ---- Setting down against blocks that were already there -----------------------------------

    /**
     * Set down touching a wall that was already there, an airship leaves the wall behind the next time it
     * is assembled, even after the world is saved and loaded; a block put against it afterwards joins it.
     */
    @GameTest(structure = TestWorld.ARENA, maxTicks = 20)
    public void aSetDownAirshipLeavesBehindTheWallItLandedAgainst(GameTestHelper helper) {
        TestWorld.floor(helper, Blocks.STONE);
        BlockPos coreAt = new BlockPos(10, 3, 10);
        plate(helper, 9, 9, 11, 11, 2, Blocks.OAK_PLANKS);
        core(helper, coreAt, ModContent.AIRSHIP_HELM, Direction.EAST);
        helper.setBlock(coreAt.above(), ModContent.LIFT_BALLOON);
        MovingStructureEntity airship = assembled(helper, coreAt);
        int size = airship.blocks().size();

        // While it is away, a wall goes up right where it comes back to, touching its deck.
        for (int y = 2; y <= 4; y++) helper.setBlock(new BlockPos(12, y, 10), Blocks.BRICKS);
        StructureAssembler.restore(helper.getLevel(), airship);
        helper.assertBlockPresent(ModContent.AIRSHIP_HELM, coreAt);

        // What it was set down against is kept with the world.
        var parked = com.example.redstonetransportation.structure.ParkedStructures.in(helper.getLevel());
        var saved = com.example.redstonetransportation.structure.ParkedStructures.TYPE.codec()
                .encodeStart(net.minecraft.nbt.NbtOps.INSTANCE, parked).getOrThrow();
        var loaded = com.example.redstonetransportation.structure.ParkedStructures.TYPE.codec()
                .parse(net.minecraft.nbt.NbtOps.INSTANCE, saved).getOrThrow();
        helper.assertTrue(loaded.wasThere(helper.absolutePos(coreAt), helper.absolutePos(new BlockPos(12, 2, 10)),
                Blocks.BRICKS.defaultBlockState()), "the wall it landed against was not saved");

        // A block a player adds to the parked airship does belong to it.
        helper.setBlock(new BlockPos(10, 2, 8), Blocks.OAK_PLANKS);
        MovingStructureEntity again = assembled(helper, coreAt);
        helper.assertValueEqual(again.blocks().size(), size + 1, "blocks in the airship assembled again");
        for (int y = 2; y <= 4; y++) helper.assertBlockPresent(Blocks.BRICKS, new BlockPos(12, y, 10));
        helper.assertBlockPresent(Blocks.AIR, new BlockPos(10, 2, 8));

        // Set down where nothing touches it, and assembled again, it is just itself.
        StructureAssembler.restore(helper.getLevel(), again);
        helper.assertValueEqual(assembled(helper, coreAt).blocks().size(), size + 1, "blocks the third time");
        helper.succeed();
    }

    /** Two vessels set down side by side stay two vessels. */
    @GameTest(structure = TestWorld.ARENA, maxTicks = 20)
    public void twoVesselsSetDownSideBySideStaySeparate(GameTestHelper helper) {
        TestWorld.floor(helper, Blocks.STONE);
        BlockPos first = new BlockPos(6, 3, 10);
        BlockPos second = new BlockPos(9, 3, 10);
        plate(helper, 5, 9, 7, 11, 2, Blocks.OAK_PLANKS);
        core(helper, first, ModContent.AIRSHIP_HELM, Direction.EAST);
        helper.setBlock(first.above(), ModContent.LIFT_BALLOON);
        MovingStructureEntity one = assembled(helper, first);
        int oneSize = one.blocks().size();
        // The second is built where it ends up right against the first once that is set down.
        plate(helper, 8, 9, 10, 11, 2, Blocks.SPRUCE_PLANKS);
        core(helper, second, ModContent.AIRSHIP_HELM, Direction.EAST);
        helper.setBlock(second.above(), ModContent.LIFT_BALLOON);
        MovingStructureEntity two = assembled(helper, second);
        int twoSize = two.blocks().size();
        StructureAssembler.restore(helper.getLevel(), one);
        StructureAssembler.restore(helper.getLevel(), two);

        StructureAssembler.Result again = assemble(helper, first);
        helper.assertTrue(again.structure() != null, "the first would not assemble beside the second: " + again.message().getString());
        helper.assertValueEqual(again.structure().blocks().size(), oneSize, "blocks in the first");
        helper.assertBlockPresent(ModContent.AIRSHIP_HELM, second);
        helper.assertValueEqual(assembled(helper, second).blocks().size(), twoSize, "blocks in the second");
        helper.succeed();
    }

    // ---- Motors ---------------------------------------------------------------------------------

    private static MovingStructureEntity motorShip(GameTestHelper helper, int z, Block motor, int motors) {
        BlockPos helm = new BlockPos(3, 3, z);
        plate(helper, 2, z - 1, 4, z + 1, 2, Blocks.OAK_PLANKS);
        core(helper, helm, ModContent.SHIP_HELM, Direction.EAST);
        BlockPos[] spots = {helm.offset(-1, 0, -1), helm.offset(-1, 0, 1), helm.offset(1, 0, -1), helm.offset(1, 0, 1), helm.west()};
        for (int i = 0; i < motors; i++) helper.setBlock(spots[i], motor);
        return assembled(helper, helm);
    }

    /**
     * Ship motors make a ship faster, a quarter more each up to four (five count as four), and burn more
     * diesel; airship motors on a ship do nothing.
     */
    @GameTest(structure = TestWorld.ARENA, maxTicks = 80)
    public void shipMotorsMakeAShipFaster(GameTestHelper helper) {
        TestWorld.floor(helper, Blocks.STONE);
        water(helper, 1);
        MovingStructureEntity plain = motorShip(helper, 5, Blocks.AIR, 0);
        MovingStructureEntity wrongKind = motorShip(helper, 15, ModContent.AIRSHIP_MOTOR, 4);
        MovingStructureEntity motored = motorShip(helper, 25, ModContent.SHIP_MOTOR, 5);
        helper.assertValueEqual(plain.motors(), 0, "motors on the plain ship");
        helper.assertValueEqual(wrongKind.motors(), 0, "airship motors counted on a ship");
        helper.assertValueEqual(motored.motors(), MovingStructureEntity.MAX_MOTORS, "ship motors counted");
        helper.assertTrue(Math.abs(motored.motorBoost() - 2.0D) < 1.0E-9D, "four motors give " + motored.motorBoost() + " times the speed");
        int[] fuelBefore = {plain.fuel(), wrongKind.fuel(), motored.fuel()};
        for (MovingStructureEntity ship : java.util.List.of(plain, wrongKind, motored)) driver(helper, ship, FORWARD);
        double[] at40 = new double[3];
        helper.runAtTickTime(40, () -> {
            at40[0] = plain.getX();
            at40[1] = wrongKind.getX();
            at40[2] = motored.getX();
        });
        helper.runAtTickTime(50, () -> {
            double plainRun = plain.getX() - at40[0];
            double wrongRun = wrongKind.getX() - at40[1];
            double motoredRun = motored.getX() - at40[2];
            helper.assertTrue(motoredRun > plainRun * 1.5D, "with motors " + motoredRun + " blocks in 10 ticks, without " + plainRun);
            helper.assertTrue(Math.abs(wrongRun - plainRun) < 0.05D, "airship motors changed a ship's speed: " + wrongRun + " vs " + plainRun);
            int plainUsed = fuelBefore[0] - plain.fuel();
            int motoredUsed = fuelBefore[2] - motored.fuel();
            helper.assertTrue(motoredUsed > plainUsed * 1.8D, "with motors it burned " + motoredUsed + ", without " + plainUsed);
            helper.succeed();
        });
    }

    /** Airship motors count on an airship; ship motors on it do nothing. */
    @GameTest(structure = TestWorld.ARENA, maxTicks = 20)
    public void airshipMotorsCountOnAnAirship(GameTestHelper helper) {
        TestWorld.floor(helper, Blocks.STONE);
        BlockPos helm = new BlockPos(10, 4, 10);
        core(helper, helm, ModContent.AIRSHIP_HELM, Direction.EAST);
        helper.setBlock(helm.above(), ModContent.LIFT_BALLOON);
        helper.setBlock(helm.north(), ModContent.AIRSHIP_MOTOR);
        helper.setBlock(helm.south(), ModContent.AIRSHIP_MOTOR);
        helper.setBlock(helm.east(), ModContent.SHIP_MOTOR);
        MovingStructureEntity airship = assembled(helper, helm);
        helper.assertValueEqual(airship.motors(), 2, "motors counted on the airship");
        helper.assertTrue(Math.abs(airship.motorBoost() - 1.5D) < 1.0E-9D, "two motors give " + airship.motorBoost());
        helper.succeed();
    }
}
