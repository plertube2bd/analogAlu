package com.example.redstonetransportation.test;

import com.example.redstonetransportation.RedstoneTransportation;
import com.example.redstonetransportation.block.ConveyorBeltBlock;
import com.example.redstonetransportation.block.ConveyorShape;
import com.example.redstonetransportation.content.ModContent;
import com.example.redstonetransportation.machine.FactoryControllerBlock;
import com.example.redstonetransportation.machine.MachineBlock;
import com.example.redstonetransportation.machine.MachineBlockEntity;
import com.example.redstonetransportation.machine.MachineStatus;
import com.example.redstonetransportation.machine.MachineType;
import com.example.redstonetransportation.machine.WorkArea;
import com.example.redstonetransportation.vehicle.CargoTramEntity;
import com.example.redstonetransportation.vehicle.CargoTramMenu;
import com.example.redstonetransportation.vehicle.ModEntityTypes;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.animal.pig.Pig;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.RailShape;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

public final class FactoryGameTests {
    private static final BlockPos MACHINE = new BlockPos(5, 1, 2);
    private static final BlockPos CHEST = MACHINE.north();
    /** Centre of the work area of a machine at {@link #MACHINE} facing south. */
    private static final BlockPos AREA_CENTER = MACHINE.south(5);
    /** Where a trunk touches the machine at {@link #MACHINE}. */
    private static final BlockPos TREE = MACHINE.south();

    private static void machine(GameTestHelper helper, BlockPos pos, MachineType type, Direction facing) {
        helper.setBlock(pos, ModContent.MACHINES.get(type).defaultBlockState().setValue(MachineBlock.FACING, facing));
    }

    private static MachineBlockEntity machineAt(GameTestHelper helper, BlockPos pos) {
        return helper.getBlockEntity(pos, MachineBlockEntity.class);
    }

    private static int chestCount(GameTestHelper helper, BlockPos pos, Item item) {
        return helper.getBlockEntity(pos, ChestBlockEntity.class).countItem(item);
    }

    private static void oakTree(GameTestHelper helper, BlockPos base, int height) {
        for (int y = 0; y < height; y++) {
            helper.setBlock(base.above(y), Blocks.OAK_LOG);
        }
        BlockPos top = base.above(height - 1);
        for (Direction side : Direction.Plane.HORIZONTAL) {
            helper.setBlock(top.relative(side), Blocks.OAK_LEAVES);
            helper.setBlock(top.relative(side).below(), Blocks.OAK_LEAVES);
        }
        helper.setBlock(top.above(), Blocks.OAK_LEAVES);
    }

    /** The headline feature: a tree touching the machine is felled completely, and the wood lands in the chest. */
    @GameTest(structure = TestWorld.ARENA, maxTicks = 400)
    public void treeCutterFellsAWholeTree(GameTestHelper helper) {
        TestWorld.floor(helper, Blocks.GRASS_BLOCK);
        machine(helper, MACHINE, MachineType.TREE_CUTTER, Direction.SOUTH);
        helper.setBlock(CHEST, Blocks.CHEST);
        helper.setBlock(MACHINE.east(), TestContent.SIGNAL);

        BlockPos tree = TREE;
        oakTree(helper, tree, 5);

        helper.succeedWhen(() -> {
            for (int y = 0; y < 5; y++) helper.assertBlockNotPresent(Blocks.OAK_LOG, tree.above(y));
            helper.assertBlockNotPresent(Blocks.OAK_LEAVES, tree.above(5));
            helper.assertValueEqual(chestCount(helper, CHEST, Items.OAK_LOG), 5, "logs in chest");
        });
    }

    /** Any face of the machine counts, not only the arrow side. */
    @GameTest(structure = TestWorld.ARENA, maxTicks = 200)
    public void treeCutterWorksOnAnyFace(GameTestHelper helper) {
        TestWorld.floor(helper, Blocks.GRASS_BLOCK);
        machine(helper, MACHINE, MachineType.TREE_CUTTER, Direction.SOUTH);
        helper.setBlock(MACHINE.east(), TestContent.SIGNAL);
        oakTree(helper, MACHINE.west(), 4);

        helper.succeedWhen(() -> helper.assertBlockNotPresent(Blocks.OAK_LOG, MACHINE.west()));
    }

    /** A machine touching the trunk partway up must still take the part of the trunk below it. */
    @GameTest(structure = TestWorld.ARENA, maxTicks = 200)
    public void treeCutterFellsTheTrunkBelowItToo(GameTestHelper helper) {
        TestWorld.floor(helper, Blocks.GRASS_BLOCK);
        BlockPos raised = MACHINE.above(2);
        machine(helper, raised, MachineType.TREE_CUTTER, Direction.SOUTH);
        helper.setBlock(raised.north(), TestContent.SIGNAL);
        oakTree(helper, TREE, 6);

        helper.succeedWhen(() -> {
            for (int y = 0; y < 6; y++) helper.assertBlockNotPresent(Blocks.OAK_LOG, TREE.above(y));
        });
    }

    /** One block of air between trunk and machine is enough to keep it from working. */
    @GameTest(structure = TestWorld.ARENA, maxTicks = 120)
    public void treeCutterIgnoresTreesItDoesNotTouch(GameTestHelper helper) {
        TestWorld.floor(helper, Blocks.GRASS_BLOCK);
        machine(helper, MACHINE, MachineType.TREE_CUTTER, Direction.SOUTH);
        helper.setBlock(MACHINE.east(), TestContent.SIGNAL);
        BlockPos apart = MACHINE.south(2);
        oakTree(helper, apart, 5);

        helper.runAtTickTime(100, () -> {
            for (int y = 0; y < 5; y++) helper.assertBlockPresent(Blocks.OAK_LOG, apart.above(y));
            helper.assertBlockProperty(MACHINE, MachineBlock.STATUS, MachineStatus.READY);
            helper.succeed();
        });
    }

    /** A cabin's log walls have no natural leaves, so the cutter must leave them alone. */
    @GameTest(structure = TestWorld.ARENA, maxTicks = 120)
    public void treeCutterSparesPlayerBuildings(GameTestHelper helper) {
        TestWorld.floor(helper, Blocks.GRASS_BLOCK);
        machine(helper, MACHINE, MachineType.TREE_CUTTER, Direction.SOUTH);
        helper.setBlock(MACHINE.east(), TestContent.SIGNAL);

        BlockPos wall = TREE;
        helper.setBlock(wall, Blocks.OAK_LOG);
        helper.setBlock(wall.above(), Blocks.OAK_LOG);
        helper.setBlock(wall.above().east(), Blocks.OAK_LEAVES.defaultBlockState().setValue(LeavesBlock.PERSISTENT, true));

        helper.runAtTickTime(100, () -> {
            helper.assertBlockPresent(Blocks.OAK_LOG, wall);
            helper.assertBlockPresent(Blocks.OAK_LOG, wall.above());
            helper.succeed();
        });
    }

    @GameTest(structure = TestWorld.ARENA, maxTicks = 60)
    public void machineStopsWithoutASignal(GameTestHelper helper) {
        TestWorld.floor(helper, Blocks.GRASS_BLOCK);
        machine(helper, MACHINE, MachineType.TREE_CUTTER, Direction.SOUTH);
        oakTree(helper, TREE, 4);

        helper.runAtTickTime(40, () -> {
            helper.assertBlockPresent(Blocks.OAK_LOG, TREE);
            helper.assertBlockProperty(MACHINE, MachineBlock.STATUS, MachineStatus.OFF);
            helper.succeed();
        });
    }

    /** A full buffer stops the machine instead of spilling items. */
    @GameTest(structure = TestWorld.ARENA, maxTicks = 120)
    public void fullBufferBlocksTheMachine(GameTestHelper helper) {
        TestWorld.floor(helper, Blocks.GRASS_BLOCK);
        machine(helper, MACHINE, MachineType.TREE_CUTTER, Direction.SOUTH);
        MachineBlockEntity cutter = machineAt(helper, MACHINE);
        for (int slot = 0; slot < cutter.getContainerSize(); slot++) {
            cutter.setItem(slot, new ItemStack(Items.DIRT, 64));
        }
        oakTree(helper, TREE, 4);
        helper.setBlock(MACHINE.east(), TestContent.SIGNAL);

        helper.succeedWhen(() -> {
            helper.assertBlockProperty(MACHINE, MachineBlock.STATUS, MachineStatus.BLOCKED);
            helper.assertBlockPresent(Blocks.OAK_LOG, TREE);
        });
    }

    /** Signal strength is a 4-bit speed: weak signals work the same job slower. */
    @GameTest(structure = TestWorld.ARENA, maxTicks = 40)
    public void signalStrengthSetsTheWorkRate(GameTestHelper helper) {
        helper.assertValueEqual(MachineType.TREE_CUTTER.interval(15), 6, "interval at signal 15");
        helper.assertValueEqual(MachineType.TREE_CUTTER.interval(1), 90, "interval at signal 1");
        helper.succeed();
    }

    @GameTest(structure = TestWorld.ARENA, maxTicks = 200)
    public void planterSpacesSaplingsOnSoil(GameTestHelper helper) {
        TestWorld.floor(helper, Blocks.GRASS_BLOCK);
        machine(helper, MACHINE, MachineType.AUTO_PLANTER, Direction.SOUTH);
        machineAt(helper, MACHINE).setItem(0, new ItemStack(Items.OAK_SAPLING, 16));
        helper.setBlock(MACHINE.east(), TestContent.SIGNAL);

        helper.succeedWhen(() -> {
            int planted = 0;
            for (int x = 1; x <= 9; x++) {
                for (int z = 3; z <= 11; z++) {
                    if (helper.getBlockState(new BlockPos(x, 1, z)).is(Blocks.OAK_SAPLING)) planted++;
                }
            }
            helper.assertValueInBetween(9, planted, 16, "saplings planted");
            helper.assertValueEqual(machineAt(helper, MACHINE).countItem(Items.OAK_SAPLING), 16 - planted, "saplings left");
        });
    }

    @GameTest(structure = TestWorld.ARENA, maxTicks = 200)
    public void harvesterReplantsRipeCrops(GameTestHelper helper) {
        TestWorld.floor(helper, Blocks.FARMLAND);
        machine(helper, MACHINE, MachineType.CROP_HARVESTER, Direction.SOUTH);
        helper.setBlock(MACHINE.below(), Blocks.STONE);
        helper.setBlock(CHEST, Blocks.CHEST);
        helper.setBlock(MACHINE.east(), TestContent.SIGNAL);

        CropBlock wheat = (CropBlock) Blocks.WHEAT;
        BlockPos ripe = AREA_CENTER;
        BlockPos young = AREA_CENTER.east();
        helper.setBlock(ripe, wheat.getStateForAge(wheat.getMaxAge()));
        helper.setBlock(young, wheat.getStateForAge(2));

        helper.succeedWhen(() -> {
            helper.assertBlockState(ripe, state -> state.is(Blocks.WHEAT) && wheat.getAge(state) <= 1, state -> Component.literal("ripe crop was not replanted: " + state));
            helper.assertBlockState(young, state -> wheat.getAge(state) >= 2, state -> Component.literal("unripe crop was touched"));
            helper.assertValueEqual(chestCount(helper, CHEST, Items.WHEAT), 1, "wheat in chest");
        });
    }

    @GameTest(structure = TestWorld.ARENA, maxTicks = 300)
    public void quarryDigsLayerByLayerAndSparesRails(GameTestHelper helper) {
        for (int x = 0; x < TestWorld.SIZE; x++) {
            for (int z = 0; z < TestWorld.SIZE; z++) {
                helper.setBlock(x, 1, z, Blocks.STONE);
                helper.setBlock(x, 2, z, Blocks.DIRT);
            }
        }
        BlockPos drill = new BlockPos(5, 3, 2);
        BlockPos rail = new BlockPos(5, 2, 7);
        helper.setBlock(rail.below(), Blocks.STONE);
        machine(helper, drill, MachineType.QUARRY_DRILL, Direction.SOUTH);
        helper.setBlock(drill.north(), Blocks.CHEST);
        helper.setBlock(drill.east(), TestContent.SIGNAL);
        helper.setBlock(rail, Blocks.RAIL);

        helper.succeedWhen(() -> {
            helper.assertBlockPresent(Blocks.AIR, new BlockPos(6, 2, 7));
            helper.assertBlockPresent(Blocks.RAIL, rail);
            helper.assertTrue(chestCount(helper, drill.north(), Items.DIRT) >= 40, "dirt was not stored");
        });
    }

    @GameTest(structure = TestWorld.ARENA, maxTicks = 100)
    public void collectorPullsLooseItems(GameTestHelper helper) {
        TestWorld.floor(helper, Blocks.STONE);
        machine(helper, MACHINE, MachineType.ITEM_COLLECTOR, Direction.SOUTH);
        helper.setBlock(CHEST, Blocks.CHEST);
        helper.setBlock(MACHINE.east(), TestContent.SIGNAL);
        helper.spawnItem(Items.APPLE, AREA_CENTER.above());

        helper.succeedWhen(() -> helper.assertValueEqual(chestCount(helper, CHEST, Items.APPLE), 1, "apples in chest"));
    }

    /** Machines in a row must plant, feed and fell the same 9x9, or a tree can grow where nothing reaches it. */
    @GameTest(structure = TestWorld.ARENA, maxTicks = 80)
    public void linkedMachinesShareOneWorkArea(GameTestHelper helper) {
        TestWorld.floor(helper, Blocks.GRASS_BLOCK);
        BlockPos planter = new BlockPos(14, 1, 5);
        BlockPos cutter = new BlockPos(15, 1, 5);
        BlockPos booster = new BlockPos(16, 1, 5);
        BlockPos collector = new BlockPos(15, 1, 4);
        machine(helper, planter, MachineType.AUTO_PLANTER, Direction.SOUTH);
        machine(helper, cutter, MachineType.TREE_CUTTER, Direction.SOUTH);
        machine(helper, booster, MachineType.GROWTH_BOOSTER, Direction.SOUTH);
        machine(helper, collector, MachineType.ITEM_COLLECTOR, Direction.SOUTH);
        helper.setBlock(new BlockPos(14, 1, 4), ModContent.FACTORY_CONTROLLER.defaultBlockState()
                .setValue(FactoryControllerBlock.MODE, FactoryControllerBlock.Mode.RUNNING));

        BlockPos expected = helper.absolutePos(cutter.south(5));
        helper.succeedWhen(() -> {
            for (BlockPos pos : java.util.List.of(planter, cutter, booster, collector)) {
                BlockPos center = WorkArea.center(machineAt(helper, pos));
                helper.assertValueEqual(center.getX(), expected.getX(), "area centre x of " + pos);
                helper.assertValueEqual(center.getZ(), expected.getZ(), "area centre z of " + pos);
            }
        });
    }

    /** One switch: everything touching the controller obeys it, even with its own signal present. */
    @GameTest(structure = TestWorld.ARENA, maxTicks = 200)
    public void controllerRunsAndStopsLinkedMachines(GameTestHelper helper) {
        TestWorld.floor(helper, Blocks.GRASS_BLOCK);
        machine(helper, MACHINE, MachineType.TREE_CUTTER, Direction.SOUTH);
        machine(helper, MACHINE.east(), MachineType.AUTO_PLANTER, Direction.SOUTH);
        BlockPos controller = MACHINE.west();
        helper.setBlock(controller, ModContent.FACTORY_CONTROLLER);
        helper.setBlock(controller.west(), TestContent.SIGNAL);
        oakTree(helper, TREE, 4);

        helper.runAtTickTime(40, () -> {
            helper.assertBlockPresent(Blocks.OAK_LOG, TREE);
            helper.setBlock(controller, ModContent.FACTORY_CONTROLLER.defaultBlockState()
                    .setValue(FactoryControllerBlock.MODE, FactoryControllerBlock.Mode.RUNNING));
        });
        helper.succeedWhen(() -> {
            helper.assertBlockNotPresent(Blocks.OAK_LOG, TREE);
            // Blocked without saplings, ready if the felled leaves happened to drop one: either way it is running.
            helper.assertFalse(helper.getBlockState(MACHINE.east()).getValue(MachineBlock.STATUS) == MachineStatus.OFF, "the planter was not switched on");
        });
    }

    @GameTest(structure = TestWorld.ARENA, maxTicks = 200)
    public void conveyorCarriesItemsIntoAChest(GameTestHelper helper) {
        TestWorld.floor(helper, TestWorld.beltFloor());
        BlockState belt = ModContent.CONVEYOR_BELT.defaultBlockState().setValue(ConveyorBeltBlock.FACING, Direction.EAST);
        for (int x = 3; x <= 12; x++) {
            helper.setBlock(new BlockPos(x, 1, 8), belt);
        }
        BlockPos end = new BlockPos(13, 1, 8);
        helper.setBlock(end, Blocks.CHEST);
        helper.spawnItem(Items.DIAMOND, new BlockPos(3, 1, 8).above());

        helper.succeedWhen(() -> helper.assertValueEqual(chestCount(helper, end, Items.DIAMOND), 1, "diamonds in chest"));
    }

    private static BlockState belt(Direction facing) {
        return ModContent.CONVEYOR_BELT.defaultBlockState().setValue(ConveyorBeltBlock.FACING, facing);
    }

    private static void beltLine(GameTestHelper helper, int y, int z, int fromX, int toX) {
        for (int x = fromX; x <= toX; x++) helper.setBlock(new BlockPos(x, y, z), belt(Direction.EAST));
    }

    /** What placing the belt by hand adds to setting the block. */
    private static void placeBelt(GameTestHelper helper, BlockPos pos, Direction facing) {
        helper.setBlock(pos, belt(facing));
        ModContent.CONVEYOR_BELT.setPlacedBy(helper.getLevel(), helper.absolutePos(pos), helper.getBlockState(pos), null, ItemStack.EMPTY);
    }

    /** Fed from the side with nothing behind, a belt bends, and carries an item round the bend either way. */
    @GameTest(structure = TestWorld.ARENA, maxTicks = 400)
    public void conveyorBendsBothWays(GameTestHelper helper) {
        TestWorld.floor(helper, TestWorld.beltFloor());
        beltLine(helper, 1, 4, 3, 4);
        helper.setBlock(new BlockPos(5, 1, 4), belt(Direction.SOUTH));
        helper.setBlock(new BlockPos(5, 1, 5), belt(Direction.SOUTH));
        beltLine(helper, 1, 6, 5, 6);
        helper.setBlock(new BlockPos(7, 1, 6), belt(Direction.NORTH));
        helper.setBlock(new BlockPos(7, 1, 5), belt(Direction.NORTH));
        beltLine(helper, 1, 4, 7, 8);
        BlockPos end = new BlockPos(9, 1, 4);
        helper.setBlock(end, Blocks.CHEST);

        helper.assertBlockProperty(new BlockPos(5, 1, 4), ConveyorBeltBlock.SHAPE, ConveyorShape.RIGHT);
        helper.assertBlockProperty(new BlockPos(5, 1, 6), ConveyorBeltBlock.SHAPE, ConveyorShape.LEFT);
        helper.assertBlockProperty(new BlockPos(7, 1, 6), ConveyorBeltBlock.SHAPE, ConveyorShape.LEFT);
        helper.assertBlockProperty(new BlockPos(7, 1, 4), ConveyorBeltBlock.SHAPE, ConveyorShape.RIGHT);
        helper.assertBlockProperty(new BlockPos(8, 1, 4), ConveyorBeltBlock.SHAPE, ConveyorShape.STRAIGHT);

        helper.spawnItem(Items.DIAMOND, new BlockPos(3, 2, 4));
        helper.succeedWhen(() -> helper.assertValueEqual(chestCount(helper, end, Items.DIAMOND), 1, "diamonds in chest"));
    }

    /** Two lines coming in from opposite sides join into one, and both get through. */
    @GameTest(structure = TestWorld.ARENA, maxTicks = 300)
    public void conveyorTakesBothSides(GameTestHelper helper) {
        TestWorld.floor(helper, TestWorld.beltFloor());
        BlockPos join = new BlockPos(6, 1, 6);
        beltLine(helper, 1, 6, 3, 5);
        for (int x = 9; x >= 7; x--) helper.setBlock(new BlockPos(x, 1, 6), belt(Direction.WEST));
        helper.setBlock(join, belt(Direction.SOUTH));
        helper.setBlock(join.south(), belt(Direction.SOUTH));
        BlockPos end = join.south(2);
        helper.setBlock(end, Blocks.CHEST);
        helper.assertBlockProperty(join, ConveyorBeltBlock.SHAPE, ConveyorShape.BOTH);

        helper.spawnItem(Items.DIAMOND, new BlockPos(3, 2, 6));
        helper.spawnItem(Items.EMERALD, new BlockPos(9, 2, 6));
        helper.succeedWhen(() -> {
            helper.assertValueEqual(chestCount(helper, end, Items.DIAMOND), 1, "diamonds from the west");
            helper.assertValueEqual(chestCount(helper, end, Items.EMERALD), 1, "emeralds from the east");
        });
    }

    /** Losing one side leaves an ordinary bend, and losing the other a straight belt. */
    @GameTest(structure = TestWorld.ARENA, maxTicks = 20)
    public void aTwoSidedJoinFollowsItsFeeders(GameTestHelper helper) {
        TestWorld.floor(helper, Blocks.STONE);
        BlockPos join = new BlockPos(6, 1, 6);
        helper.setBlock(join, belt(Direction.SOUTH));
        helper.setBlock(join.west(), belt(Direction.EAST));
        helper.setBlock(join.east(), belt(Direction.WEST));
        helper.assertBlockProperty(join, ConveyorBeltBlock.SHAPE, ConveyorShape.BOTH);

        helper.setBlock(join.east(), Blocks.AIR);
        helper.assertBlockProperty(join, ConveyorBeltBlock.SHAPE, ConveyorShape.RIGHT);
        helper.setBlock(join.west(), Blocks.AIR);
        helper.assertBlockProperty(join, ConveyorBeltBlock.SHAPE, ConveyorShape.STRAIGHT);
        helper.succeed();
    }

    /** With a line behind it too, the belt stays straight and both sides simply join it. */
    @GameTest(structure = TestWorld.ARENA, maxTicks = 20)
    public void aThroughLineTakesBothSidesStraight(GameTestHelper helper) {
        TestWorld.floor(helper, Blocks.STONE);
        BlockPos join = new BlockPos(6, 1, 6);
        helper.setBlock(join.north(), belt(Direction.SOUTH));
        helper.setBlock(join, belt(Direction.SOUTH));
        helper.setBlock(join.west(), belt(Direction.EAST));
        helper.setBlock(join.east(), belt(Direction.WEST));
        helper.assertBlockProperty(join, ConveyorBeltBlock.SHAPE, ConveyorShape.STRAIGHT);
        helper.succeed();
    }

    /** A line that already runs through keeps running straight when another joins it from the side. */
    @GameTest(structure = TestWorld.ARENA, maxTicks = 200)
    public void aSideLineJoinsAStraightBelt(GameTestHelper helper) {
        TestWorld.floor(helper, TestWorld.beltFloor());
        beltLine(helper, 1, 8, 3, 9);
        helper.setBlock(new BlockPos(6, 1, 7), belt(Direction.SOUTH));
        BlockPos end = new BlockPos(10, 1, 8);
        helper.setBlock(end, Blocks.CHEST);
        helper.assertBlockProperty(new BlockPos(6, 1, 8), ConveyorBeltBlock.SHAPE, ConveyorShape.STRAIGHT);

        helper.spawnItem(Items.DIAMOND, new BlockPos(6, 2, 7));
        helper.succeedWhen(() -> helper.assertValueEqual(chestCount(helper, end, Items.DIAMOND), 1, "diamonds in chest"));
    }

    @GameTest(structure = TestWorld.ARENA, maxTicks = 20)
    public void aBendStraightensWhenItsFeederGoes(GameTestHelper helper) {
        TestWorld.floor(helper, Blocks.STONE);
        BlockPos feeder = new BlockPos(4, 1, 4);
        BlockPos bend = feeder.east();
        helper.setBlock(feeder, belt(Direction.EAST));
        helper.setBlock(bend, belt(Direction.SOUTH));
        helper.assertBlockProperty(bend, ConveyorBeltBlock.SHAPE, ConveyorShape.RIGHT);

        helper.setBlock(feeder, Blocks.AIR);
        helper.assertBlockProperty(bend, ConveyorBeltBlock.SHAPE, ConveyorShape.STRAIGHT);
        helper.succeed();
    }

    /** Laying a line round a corner: the belt laid before the turn still pointed straight on. */
    @GameTest(structure = TestWorld.ARENA, maxTicks = 200)
    public void aNewBeltTurnsTheOpenEndOfALine(GameTestHelper helper) {
        TestWorld.floor(helper, TestWorld.beltFloor());
        placeBelt(helper, new BlockPos(3, 1, 4), Direction.EAST);
        placeBelt(helper, new BlockPos(4, 1, 4), Direction.EAST);
        BlockPos corner = new BlockPos(5, 1, 4);
        placeBelt(helper, corner, Direction.EAST);
        placeBelt(helper, corner.south(), Direction.SOUTH);
        helper.assertBlockProperty(corner, ConveyorBeltBlock.FACING, Direction.SOUTH);
        helper.assertBlockProperty(corner, ConveyorBeltBlock.SHAPE, ConveyorShape.RIGHT);

        placeBelt(helper, corner.south(2), Direction.SOUTH);
        BlockPos end = corner.south(3);
        helper.setBlock(end, Blocks.CHEST);
        helper.spawnItem(Items.DIAMOND, new BlockPos(3, 2, 4));
        helper.succeedWhen(() -> helper.assertValueEqual(chestCount(helper, end, Items.DIAMOND), 1, "diamonds in chest"));
    }

    /** Only a belt that leads on from the end turns it: a parallel line must not merge into its neighbour. */
    @GameTest(structure = TestWorld.ARENA, maxTicks = 20)
    public void aParallelBeltLeavesAnOpenEndAlone(GameTestHelper helper) {
        TestWorld.floor(helper, Blocks.STONE);
        BlockPos end = new BlockPos(5, 1, 4);
        placeBelt(helper, end, Direction.EAST);
        placeBelt(helper, end.south(), Direction.EAST);
        helper.assertBlockProperty(end, ConveyorBeltBlock.FACING, Direction.EAST);
        helper.succeed();
    }

    /** An end that feeds a chest is already doing its job. */
    @GameTest(structure = TestWorld.ARENA, maxTicks = 20)
    public void aBeltFeedingAChestStaysPut(GameTestHelper helper) {
        TestWorld.floor(helper, Blocks.STONE);
        BlockPos end = new BlockPos(5, 1, 4);
        placeBelt(helper, end, Direction.EAST);
        helper.setBlock(end.east(), Blocks.CHEST);
        placeBelt(helper, end.south(), Direction.SOUTH);
        helper.assertBlockProperty(end, ConveyorBeltBlock.FACING, Direction.EAST);
        helper.succeed();
    }

    /** Like rails, the belt below the step becomes the slope, and items ride up two levels of it. */
    @GameTest(structure = TestWorld.ARENA, maxTicks = 300)
    public void conveyorClimbs(GameTestHelper helper) {
        TestWorld.floor(helper, TestWorld.beltFloor());
        beltLine(helper, 1, 8, 3, 5);
        helper.setBlock(new BlockPos(6, 1, 8), Blocks.STONE);
        helper.setBlock(new BlockPos(6, 2, 8), belt(Direction.EAST));
        helper.setBlock(new BlockPos(7, 1, 8), Blocks.STONE);
        helper.setBlock(new BlockPos(7, 2, 8), Blocks.STONE);
        beltLine(helper, 3, 8, 7, 9);
        BlockPos end = new BlockPos(10, 3, 8);
        helper.setBlock(end, Blocks.CHEST);

        helper.assertBlockProperty(new BlockPos(5, 1, 8), ConveyorBeltBlock.SHAPE, ConveyorShape.UP);
        helper.assertBlockProperty(new BlockPos(6, 2, 8), ConveyorBeltBlock.SHAPE, ConveyorShape.UP);
        helper.assertBlockProperty(new BlockPos(7, 3, 8), ConveyorBeltBlock.SHAPE, ConveyorShape.STRAIGHT);

        helper.spawnItem(Items.DIAMOND, new BlockPos(3, 2, 8));
        helper.succeedWhen(() -> helper.assertValueEqual(chestCount(helper, end, Items.DIAMOND), 1, "diamonds in chest"));
    }

    @GameTest(structure = TestWorld.ARENA, maxTicks = 300)
    public void conveyorDescends(GameTestHelper helper) {
        TestWorld.floor(helper, TestWorld.beltFloor());
        for (int x = 3; x <= 5; x++) {
            helper.setBlock(new BlockPos(x, 1, 8), Blocks.STONE);
            helper.setBlock(new BlockPos(x, 2, 8), Blocks.STONE);
        }
        beltLine(helper, 3, 8, 3, 5);
        helper.setBlock(new BlockPos(6, 1, 8), Blocks.STONE);
        helper.setBlock(new BlockPos(6, 2, 8), belt(Direction.EAST));
        beltLine(helper, 1, 8, 7, 9);
        BlockPos end = new BlockPos(10, 1, 8);
        helper.setBlock(end, Blocks.CHEST);

        helper.assertBlockProperty(new BlockPos(6, 2, 8), ConveyorBeltBlock.SHAPE, ConveyorShape.DOWN);
        helper.assertBlockProperty(new BlockPos(7, 1, 8), ConveyorBeltBlock.SHAPE, ConveyorShape.DOWN);
        helper.assertBlockProperty(new BlockPos(8, 1, 8), ConveyorBeltBlock.SHAPE, ConveyorShape.STRAIGHT);

        helper.spawnItem(Items.DIAMOND, new BlockPos(3, 4, 8));
        helper.succeedWhen(() -> helper.assertValueEqual(chestCount(helper, end, Items.DIAMOND), 1, "diamonds in chest"));
    }

    /** Stacked lines are not a slope: each belt has its own line right next to it. */
    @GameTest(structure = TestWorld.ARENA, maxTicks = 20)
    public void stackedLinesStayFlat(GameTestHelper helper) {
        TestWorld.floor(helper, Blocks.STONE);
        beltLine(helper, 3, 8, 3, 9);
        beltLine(helper, 1, 8, 5, 12);
        helper.assertBlockProperty(new BlockPos(5, 1, 8), ConveyorBeltBlock.SHAPE, ConveyorShape.STRAIGHT);
        helper.assertBlockProperty(new BlockPos(9, 3, 8), ConveyorBeltBlock.SHAPE, ConveyorShape.STRAIGHT);
        helper.succeed();
    }

    /** A stopped slope holds what is on it, rather than dropping it through the ramp. */
    @GameTest(structure = TestWorld.ARENA, maxTicks = 80)
    public void aStoppedSlopeHoldsItsItems(GameTestHelper helper) {
        TestWorld.floor(helper, Blocks.STONE);
        BlockPos slope = new BlockPos(5, 1, 8);
        helper.setBlock(slope, belt(Direction.EAST));
        helper.setBlock(slope.east(), Blocks.STONE);
        helper.setBlock(slope.east().above(), belt(Direction.EAST));
        helper.setBlock(slope.north(), Blocks.REDSTONE_BLOCK);
        helper.assertBlockProperty(slope, ConveyorBeltBlock.SHAPE, ConveyorShape.UP);
        var item = helper.spawnItem(Items.DIAMOND, slope.above());

        helper.runAtTickTime(60, () -> {
            Vec3 at = helper.relativeVec(item.position());
            helper.assertTrue(at.y > 1.5D, "the item sank into the ramp: " + at);
            helper.assertTrue(at.x < 6.2D, "the stopped slope still moved the item: " + at);
            helper.succeed();
        });
    }

    /** Animals climb on the ramp's steps. */
    @GameTest(structure = TestWorld.ARENA, maxTicks = 200)
    public void conveyorCarriesAnAnimalUpASlope(GameTestHelper helper) {
        TestWorld.floor(helper, TestWorld.beltFloor());
        beltLine(helper, 1, 8, 3, 5);
        helper.setBlock(new BlockPos(6, 1, 8), Blocks.STONE);
        beltLine(helper, 2, 8, 6, 10);
        Pig pig = helper.spawnWithNoFreeWill(EntityTypes.PIG, new BlockPos(3, 1, 8));

        helper.succeedWhen(() -> {
            Vec3 at = helper.relativeVec(pig.position());
            helper.assertTrue(at.y >= 2.2D && at.x > 7.0D, "the pig is at " + at);
        });
    }

    /** Moving structures turn the blocks they carry, so the belt has to turn with them. */
    @GameTest(structure = TestWorld.ARENA, maxTicks = 20)
    public void aBeltTurnsAndMirrors(GameTestHelper helper) {
        BlockState bend = belt(Direction.EAST).setValue(ConveyorBeltBlock.SHAPE, ConveyorShape.LEFT);
        helper.assertValueEqual(bend.rotate(Rotation.CLOCKWISE_90).getValue(ConveyorBeltBlock.FACING), Direction.SOUTH, "turned facing");
        BlockState mirrored = bend.mirror(Mirror.FRONT_BACK);
        helper.assertValueEqual(mirrored.getValue(ConveyorBeltBlock.FACING), Direction.WEST, "mirrored facing");
        helper.assertValueEqual(mirrored.getValue(ConveyorBeltBlock.SHAPE), ConveyorShape.RIGHT, "mirrored bend");
        helper.succeed();
    }

    /** With no chest beside it, a machine hands its output to a neighbouring belt. */
    @GameTest(structure = TestWorld.ARENA, maxTicks = 300)
    public void machineFeedsAConveyorIntoAChest(GameTestHelper helper) {
        TestWorld.floor(helper, TestWorld.beltFloor());
        machine(helper, MACHINE, MachineType.ITEM_COLLECTOR, Direction.SOUTH);
        machineAt(helper, MACHINE).setItem(0, new ItemStack(Items.OAK_LOG, 8));
        BlockState belt = ModContent.CONVEYOR_BELT.defaultBlockState().setValue(ConveyorBeltBlock.FACING, Direction.EAST);
        for (int x = 6; x <= 10; x++) {
            helper.setBlock(new BlockPos(x, 1, 2), belt);
        }
        BlockPos end = new BlockPos(11, 1, 2);
        helper.setBlock(end, Blocks.CHEST);

        helper.succeedWhen(() -> helper.assertValueEqual(chestCount(helper, end, Items.OAK_LOG), 8, "logs in chest"));
    }

    @GameTest(structure = TestWorld.ARENA, maxTicks = 60)
    public void poweredConveyorStops(GameTestHelper helper) {
        TestWorld.floor(helper, Blocks.STONE);
        BlockPos pos = new BlockPos(5, 1, 8);
        helper.setBlock(pos, ModContent.CONVEYOR_BELT.defaultBlockState().setValue(ConveyorBeltBlock.FACING, Direction.EAST));
        helper.setBlock(pos.north(), Blocks.REDSTONE_BLOCK);
        var item = helper.spawnItem(Items.DIAMOND, pos.above());

        helper.runAtTickTime(40, () -> {
            helper.assertBlockProperty(pos, ConveyorBeltBlock.POWERED, true);
            helper.assertTrue(helper.relativeVec(item.position()).x < 6.2D, "stopped belt still moved the item");
            helper.succeed();
        });
    }

    /** With the cart machines gone, a hopper over the rail fills a tram waiting on a drive rail. */
    @GameTest(structure = TestWorld.ARENA, maxTicks = 200)
    public void hopperLoadsAWaitingCargoTram(GameTestHelper helper) {
        TestWorld.floor(helper, Blocks.STONE);
        BlockPos rail = new BlockPos(5, 1, 10);
        TestWorld.rail(helper, rail, ModContent.DRIVE_RAIL, RailShape.EAST_WEST);
        helper.setBlock(rail.above(), Blocks.HOPPER);
        helper.getBlockEntity(rail.above(), HopperBlockEntity.class).setItem(0, new ItemStack(Items.OAK_LOG, 8));
        CargoTramEntity tram = helper.spawn(ModEntityTypes.CARGO_TRAM, rail);

        helper.succeedWhen(() -> helper.assertValueEqual(tram.countItem(Items.OAK_LOG), 8, "logs in the tram"));
    }

    /** And a hopper under the rail empties it. */
    @GameTest(structure = TestWorld.ARENA, maxTicks = 200)
    public void hopperUnderTheRailEmptiesACargoTram(GameTestHelper helper) {
        TestWorld.floor(helper, Blocks.STONE);
        BlockPos rail = new BlockPos(5, 1, 10);
        helper.setBlock(rail.below(), Blocks.HOPPER);
        TestWorld.rail(helper, rail, ModContent.DRIVE_RAIL, RailShape.EAST_WEST);
        CargoTramEntity tram = helper.spawn(ModEntityTypes.CARGO_TRAM, rail);
        tram.setItem(0, new ItemStack(Items.OAK_LOG, 3));

        helper.succeedWhen(() -> {
            helper.assertTrue(tram.isEmpty(), "the tram still holds cargo");
            helper.assertValueEqual(helper.getBlockEntity(rail.below(), HopperBlockEntity.class).countItem(Items.OAK_LOG), 3, "logs in the hopper");
        });
    }

    /** Shift-clicking moves stacks into the hold and back out, as in a chest. */
    @GameTest(structure = TestWorld.ARENA, maxTicks = 20)
    public void cargoHoldShiftClicks(GameTestHelper helper) {
        TestWorld.floor(helper, Blocks.STONE);
        BlockPos rail = new BlockPos(5, 1, 10);
        TestWorld.rail(helper, rail, Blocks.RAIL, RailShape.EAST_WEST);
        CargoTramEntity tram = helper.spawn(ModEntityTypes.CARGO_TRAM, rail);
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.getInventory().setItem(9, new ItemStack(Items.OAK_LOG, 20));
        CargoTramMenu menu = new CargoTramMenu(1, player.getInventory(), tram);

        menu.quickMoveStack(player, CargoTramMenu.SLOTS);
        helper.assertValueEqual(tram.countItem(Items.OAK_LOG), 20, "logs in the hold");
        helper.assertValueEqual(player.getInventory().countItem(Items.OAK_LOG), 0, "logs left on the player");

        menu.quickMoveStack(player, 0);
        helper.assertValueEqual(tram.countItem(Items.OAK_LOG), 0, "logs left in the hold");
        helper.assertValueEqual(player.getInventory().countItem(Items.OAK_LOG), 20, "logs back on the player");
        helper.succeed();
    }

    /** The hold is 32 slots now; a tram saved with the old 36 still gets everything back. */
    @GameTest(structure = TestWorld.ARENA, maxTicks = 40)
    public void anOldCargoTramKeepsItsLastFourStacks(GameTestHelper helper) {
        TestWorld.floor(helper, Blocks.STONE);
        BlockPos rail = new BlockPos(5, 1, 10);
        TestWorld.rail(helper, rail, Blocks.RAIL, RailShape.EAST_WEST);
        CargoTramEntity roomy = helper.spawn(ModEntityTypes.CARGO_TRAM, rail);
        helper.assertValueEqual(roomy.getContainerSize(), 32, "slots in the hold");
        roomy.getItemStacks().set(35, new ItemStack(Items.DIAMOND, 5));

        BlockPos fullRail = new BlockPos(15, 1, 10);
        TestWorld.rail(helper, fullRail, Blocks.RAIL, RailShape.EAST_WEST);
        CargoTramEntity full = helper.spawn(ModEntityTypes.CARGO_TRAM, fullRail);
        for (int slot = 0; slot < 32; slot++) full.setItem(slot, new ItemStack(Items.DIRT, 64));
        full.getItemStacks().set(33, new ItemStack(Items.EMERALD, 7));

        helper.succeedWhen(() -> {
            helper.assertValueEqual(roomy.countItem(Items.DIAMOND), 5, "diamonds moved up into the hold");
            helper.assertItemEntityCountIs(Items.EMERALD, fullRail, 3.0D, 7);
            helper.assertTrue(full.getItemStacks().get(33).isEmpty(), "the stack stayed behind in the old slot");
        });
    }

    /** ALU's semiconductor makes a batch of four; with no ALU to get one from, the plain recipe makes one. */
    @GameTest(structure = TestWorld.ARENA, maxTicks = 20)
    public void machineRecipesFollowAnalogAlu(GameTestHelper helper) {
        boolean alu = FabricLoader.getInstance().isModLoaded("analog_alu");
        ItemStack semiconductor = new ItemStack(BuiltInRegistries.ITEM.getValue(Identifier.fromNamespaceAndPath("analog_alu", "semiconductor")));
        ItemStack redstone = new ItemStack(Items.REDSTONE);

        ItemStack made = craftTreeCutter(helper, alu ? semiconductor : redstone);
        helper.assertTrue(made.is(ModContent.MACHINES.get(MachineType.TREE_CUTTER).asItem()), "no tree cutter came out: " + made);
        helper.assertValueEqual(made.getCount(), alu ? 4 : 1, "tree cutters made");
        if (alu) helper.assertTrue(craftTreeCutter(helper, redstone).isEmpty(), "redstone still makes a machine with ALU installed");

        List<String> machines = new ArrayList<>(List.of("factory_controller"));
        for (MachineType type : MachineType.values()) machines.add(type.id());
        RecipeManager recipes = helper.getLevel().getServer().getRecipeManager();
        for (String machine : machines) {
            ResourceKey<Recipe<?>> key = ResourceKey.create(Registries.RECIPE,
                    Identifier.fromNamespaceAndPath(RedstoneTransportation.MOD_ID, alu ? machine : machine + "_without_alu"));
            helper.assertTrue(recipes.byKey(key).isPresent(), "no recipe " + key.identifier());
        }
        helper.succeed();
    }

    private static ItemStack craftTreeCutter(GameTestHelper helper, ItemStack middle) {
        ItemStack iron = new ItemStack(Items.IRON_INGOT);
        ItemStack motor = new ItemStack(ModContent.REDSTONE_MOTOR_ITEM);
        CraftingInput input = CraftingInput.of(3, 3, List.of(
                iron, new ItemStack(Items.IRON_AXE), iron,
                middle, motor, middle,
                iron, new ItemStack(Items.CHEST), iron));
        return helper.getLevel().getServer().getRecipeManager().getRecipeFor(RecipeType.CRAFTING, input, helper.getLevel())
                .map(recipe -> recipe.value().assemble(input))
                .orElse(ItemStack.EMPTY);
    }

    /** Places the tree factory: planter, cutter and booster in a row, chest and controller behind them. */
    private static void treeFactory(GameTestHelper helper, BlockPos cutter) {
        machine(helper, cutter, MachineType.TREE_CUTTER, Direction.SOUTH);
        machine(helper, cutter.east(), MachineType.AUTO_PLANTER, Direction.SOUTH);
        machine(helper, cutter.west(), MachineType.GROWTH_BOOSTER, Direction.SOUTH);
        helper.setBlock(cutter.north(), Blocks.CHEST);
        helper.setBlock(cutter.north().east(), ModContent.FACTORY_CONTROLLER.defaultBlockState()
                .setValue(FactoryControllerBlock.MODE, FactoryControllerBlock.Mode.RUNNING));
        helper.setBlock(cutter.north(2).east(), TestContent.SIGNAL);
    }

    /**
     * The full automatic tree factory: fed saplings and bone meal once, it must plant, grow,
     * fell and replant on the one spot touching the cutter with no further help.
     */
    @GameTest(structure = TestWorld.ARENA, maxTicks = 2400, skyAccess = true)
    public void treeFactoryRunsTheWholeCycle(GameTestHelper helper) {
        TestWorld.floor(helper, Blocks.GRASS_BLOCK);
        BlockPos cutter = new BlockPos(15, 1, 9);
        BlockPos planter = cutter.east();
        BlockPos booster = cutter.west();
        BlockPos chest = cutter.north();
        BlockPos spot = cutter.south();
        treeFactory(helper, cutter);
        machineAt(helper, planter).setItem(0, new ItemStack(Items.OAK_SAPLING, 4));
        machineAt(helper, booster).setItem(0, new ItemStack(Items.BONE_MEAL, 64));

        helper.succeedWhen(() -> {
            helper.assertTrue(chestCount(helper, chest, Items.OAK_LOG) >= 12,
                    "logs so far: " + chestCount(helper, chest, Items.OAK_LOG)
                            + ", planter saplings: " + machineAt(helper, planter).countItem(Items.OAK_SAPLING)
                            + ", planter " + helper.getBlockState(planter).getValue(MachineBlock.STATUS)
                            + ", booster " + helper.getBlockState(booster).getValue(MachineBlock.STATUS)
                            + ", cutter " + helper.getBlockState(cutter).getValue(MachineBlock.STATUS)
                            + ", spot " + helper.getBlockState(spot));
            // Saplings from the felled leaves must have come back around to the planter or the spot.
            helper.assertTrue(machineAt(helper, planter).countItem(Items.OAK_SAPLING) > 0
                    || helper.getBlockState(spot).is(Blocks.OAK_SAPLING), "no saplings left: the factory would stop");
        });
    }

    /** With a cutter in reach, a sapling planted anywhere else would grow into a tree nothing can fell. */
    @GameTest(structure = TestWorld.ARENA, maxTicks = 80)
    public void planterPlantsOnlyAgainstTheCutter(GameTestHelper helper) {
        TestWorld.floor(helper, Blocks.GRASS_BLOCK);
        BlockPos cutter = new BlockPos(15, 1, 9);
        BlockPos planter = cutter.east();
        machine(helper, cutter, MachineType.TREE_CUTTER, Direction.SOUTH);
        machine(helper, planter, MachineType.AUTO_PLANTER, Direction.SOUTH);
        machineAt(helper, planter).setItem(0, new ItemStack(Items.OAK_SAPLING, 8));
        helper.setBlock(planter.north(), TestContent.SIGNAL);

        helper.runAtTickTime(60, () -> {
            helper.assertBlockPresent(Blocks.OAK_SAPLING, cutter.south());
            helper.assertValueEqual(machineAt(helper, planter).countItem(Items.OAK_SAPLING), 7, "saplings left");
            helper.succeed();
        });
    }

    /** Leaves go first, so what is left after a reload no longer looks like a tree. It must still be finished. */
    @GameTest(structure = TestWorld.ARENA, maxTicks = 200)
    public void halfFelledTreeFinishesAfterReload(GameTestHelper helper) {
        TestWorld.floor(helper, Blocks.GRASS_BLOCK);
        machine(helper, MACHINE, MachineType.TREE_CUTTER, Direction.SOUTH);
        helper.setBlock(MACHINE.east(), TestContent.SIGNAL);
        oakTree(helper, TREE, 6);

        helper.runAtTickTime(16, () -> {
            helper.assertBlockPresent(Blocks.OAK_LOG, TREE);
            helper.assertBlockNotPresent(Blocks.OAK_LEAVES, TREE.above(6));
            MachineBlockEntity running = machineAt(helper, MACHINE);
            CompoundTag saved = running.saveWithoutMetadata(helper.getLevel().registryAccess());
            BlockState state = helper.getBlockState(MACHINE);
            running.clearContent();
            helper.setBlock(MACHINE, Blocks.AIR);
            helper.setBlock(MACHINE, state);
            machineAt(helper, MACHINE).loadWithComponents(
                    TagValueInput.create(ProblemReporter.DISCARDING, helper.getLevel().registryAccess(), saved));
        });
        helper.succeedWhen(() -> {
            for (int y = 0; y < 6; y++) helper.assertBlockNotPresent(Blocks.OAK_LOG, TREE.above(y));
        });
    }
}
