package com.example.redstonetransportation.test;

import com.example.redstonetransportation.block.ConveyorBeltBlock;
import com.example.redstonetransportation.block.DirectionalSignalBlock;
import com.example.redstonetransportation.block.DriveRailBlock;
import com.example.redstonetransportation.block.PowerCellBlock;
import com.example.redstonetransportation.block.RailMotorBlock;
import com.example.redstonetransportation.content.ModContent;
import com.example.redstonetransportation.integration.analogalu.ElectricPower;
import com.example.redstonetransportation.machine.FactoryControllerBlock;
import com.example.redstonetransportation.machine.MachineBlock;
import com.example.redstonetransportation.machine.MachineStatus;
import com.example.redstonetransportation.machine.MachineType;
import com.example.redstonetransportation.rail.RailDrive;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.vehicle.minecart.AbstractMinecart;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.RailShape;
import net.minecraft.world.phys.Vec3;

/** Real ALU circuits driving this mod's blocks. With an ALU that has none, there is nothing here to test. */
public final class ElectricGameTests {
    private static final BlockPos TAP = new BlockPos(6, 1, 6);
    private static final BlockPos BESIDE = TAP.east();
    /** Long enough for ALU to solve the circuit and for a block to look at it. */
    private static final int LIVE_TICKS = 50;

    private static boolean noCircuits(GameTestHelper helper) {
        if (ElectricPower.isAvailable()) return false;
        helper.succeed();
        return true;
    }

    private static void wired(GameTestHelper helper) {
        TestWorld.floor(helper, Blocks.STONE);
        TestWorld.circuit(helper.getLevel(), helper.absolutePos(TAP));
    }

    private static BlockState belt() {
        return ModContent.CONVEYOR_BELT.defaultBlockState().setValue(ConveyorBeltBlock.FACING, Direction.EAST);
    }

    private static void beltLine(GameTestHelper helper, int fromX, int toX) {
        for (int x = fromX; x <= toX; x++) helper.setBlock(new BlockPos(x, 1, BESIDE.getZ()), belt());
    }

    private static void machineAt(GameTestHelper helper, BlockPos pos) {
        helper.setBlock(pos, ModContent.MACHINES.get(MachineType.ITEM_COLLECTOR).defaultBlockState()
                .setValue(MachineBlock.FACING, Direction.SOUTH));
    }

    private static void railLine(GameTestHelper helper, int z, int fromX) {
        for (int x = fromX; x <= 28; x++) {
            TestWorld.rail(helper, new BlockPos(x, 1, z), Blocks.RAIL, RailShape.EAST_WEST);
        }
    }

    /** A 9 volt battery through a 10 ohm load: a little under a three quarters signal. */
    @GameTest(structure = TestWorld.ARENA, maxTicks = 80)
    public void aLiveCableReadsAsASignal(GameTestHelper helper) {
        if (noCircuits(helper)) return;
        wired(helper);
        helper.runAtTickTime(LIVE_TICKS, () -> {
            int level = ElectricPower.cableLevel(helper.getLevel(), helper.absolutePos(BESIDE));
            helper.assertValueInBetween(10, level, 12, "signal from a live cable");
            helper.succeed();
        });
    }

    /** Past the load the voltage is spent, so the wire back to the battery runs nothing. */
    @GameTest(structure = TestWorld.ARENA, maxTicks = 80)
    public void theReturnWireIsDead(GameTestHelper helper) {
        if (noCircuits(helper)) return;
        wired(helper);
        helper.runAtTickTime(LIVE_TICKS, () -> {
            int level = ElectricPower.cableLevel(helper.getLevel(), helper.absolutePos(TAP.west(3)));
            helper.assertValueEqual(level, 0, "signal from the return wire");
            helper.succeed();
        });
    }

    /** No load, no closed circuit, no voltage. */
    @GameTest(structure = TestWorld.ARENA, maxTicks = 80)
    public void anOpenCircuitIsDead(GameTestHelper helper) {
        if (noCircuits(helper)) return;
        wired(helper);
        helper.setBlock(TAP.west().north(), Blocks.AIR);
        helper.runAtTickTime(LIVE_TICKS, () -> {
            helper.assertValueEqual(ElectricPower.cableLevel(helper.getLevel(), helper.absolutePos(BESIDE)), 0, "signal from an open circuit");
            helper.succeed();
        });
    }

    @GameTest(structure = TestWorld.ARENA, maxTicks = 200)
    public void aDriveRailRunsOnElectricity(GameTestHelper helper) {
        if (noCircuits(helper)) return;
        wired(helper);
        railLine(helper, BESIDE.getZ(), BESIDE.getX() + 1);
        TestWorld.rail(helper, BESIDE, ModContent.DRIVE_RAIL, RailShape.EAST_WEST);
        AbstractMinecart cart = helper.spawn(EntityTypes.MINECART, BESIDE);
        helper.succeedWhen(() -> {
            helper.assertBlockProperty(BESIDE, DriveRailBlock.POWERED, true);
            helper.assertTrue(helper.relativeVec(cart.position()).x > 14.0D, "the cart has not left");
            double speed = cart.getDeltaMovement().horizontalDistance();
            helper.assertValueInBetween(RailDrive.cruiseSpeed(10) - 0.05D, speed, RailDrive.cruiseSpeed(12) + 0.05D, "speed for the voltage");
        });
    }

    @GameTest(structure = TestWorld.ARENA, maxTicks = 80)
    public void redstoneNoLongerDrivesARail(GameTestHelper helper) {
        if (noCircuits(helper)) return;
        TestWorld.floor(helper, Blocks.STONE);
        railLine(helper, BESIDE.getZ(), BESIDE.getX() + 1);
        TestWorld.rail(helper, BESIDE, ModContent.DRIVE_RAIL, RailShape.EAST_WEST);
        helper.setBlock(BESIDE.north(), Blocks.REDSTONE_BLOCK);
        AbstractMinecart cart = helper.spawn(EntityTypes.MINECART, BESIDE);
        helper.runAtTickTime(60, () -> {
            helper.assertTrue(Math.abs(helper.relativeVec(cart.position()).x - 7.5D) < 0.5D, "redstone moved the cart");
            helper.succeed();
        });
    }

    @GameTest(structure = TestWorld.ARENA, maxTicks = 200)
    public void aMotorRunsOnElectricity(GameTestHelper helper) {
        if (noCircuits(helper)) return;
        wired(helper);
        helper.setBlock(BESIDE, ModContent.REDSTONE_MOTOR.defaultBlockState().setValue(RailMotorBlock.FACING, Direction.EAST));
        railLine(helper, BESIDE.getZ() + 1, BESIDE.getX());
        AbstractMinecart cart = helper.spawn(EntityTypes.MINECART, BESIDE.south().east());
        helper.succeedWhen(() -> {
            helper.assertBlockProperty(BESIDE, RailMotorBlock.POWERED, true);
            helper.assertTrue(helper.relativeVec(cart.position()).x > 14.0D, "the cart has not left");
        });
    }

    @GameTest(structure = TestWorld.ARENA, maxTicks = 80)
    public void redstoneNoLongerDrivesAMotor(GameTestHelper helper) {
        if (noCircuits(helper)) return;
        TestWorld.floor(helper, Blocks.STONE);
        helper.setBlock(BESIDE, ModContent.REDSTONE_MOTOR.defaultBlockState().setValue(RailMotorBlock.FACING, Direction.EAST));
        helper.setBlock(BESIDE.north(), Blocks.REDSTONE_BLOCK);
        helper.runAtTickTime(40, () -> {
            helper.assertBlockProperty(BESIDE, RailMotorBlock.POWERED, false);
            helper.succeed();
        });
    }

    @GameTest(structure = TestWorld.ARENA, maxTicks = 120)
    public void aMachineRunsOnElectricity(GameTestHelper helper) {
        if (noCircuits(helper)) return;
        wired(helper);
        // The tree cutter runs on any live cable it touches; a collector on the same cable needs a controller.
        helper.setBlock(BESIDE, ModContent.MACHINES.get(MachineType.TREE_CUTTER).defaultBlockState()
                .setValue(MachineBlock.FACING, Direction.SOUTH));
        BlockPos collector = TAP.north().east();
        machineAt(helper, collector);
        helper.runAtTickTime(LIVE_TICKS, () -> {
            helper.assertTrue(helper.getBlockState(BESIDE).getValue(MachineBlock.STATUS) != MachineStatus.OFF, "the cutter is off");
            helper.assertBlockProperty(collector, MachineBlock.STATUS, MachineStatus.OFF);
            helper.succeed();
        });
    }

    @GameTest(structure = TestWorld.ARENA, maxTicks = 80)
    public void redstoneNoLongerRunsAMachine(GameTestHelper helper) {
        if (noCircuits(helper)) return;
        TestWorld.floor(helper, Blocks.STONE);
        machineAt(helper, BESIDE);
        helper.setBlock(BESIDE.north(), Blocks.REDSTONE_BLOCK);
        helper.runAtTickTime(40, () -> {
            helper.assertBlockProperty(BESIDE, MachineBlock.STATUS, MachineStatus.OFF);
            helper.succeed();
        });
    }

    /** Left on "running", a controller still needs electricity: touching one live cable is not being wired in. */
    @GameTest(structure = TestWorld.ARENA, maxTicks = 120)
    public void aRunningControllerNeedsElectricity(GameTestHelper helper) {
        if (noCircuits(helper)) return;
        wired(helper);
        helper.setBlock(BESIDE, ModContent.FACTORY_CONTROLLER.defaultBlockState()
                .setValue(FactoryControllerBlock.MODE, FactoryControllerBlock.Mode.RUNNING));
        machineAt(helper, BESIDE.east());
        helper.runAtTickTime(LIVE_TICKS, () -> {
            helper.assertBlockProperty(BESIDE.east(), MachineBlock.STATUS, MachineStatus.OFF);
            helper.succeed();
        });
    }

    /** One touch is enough: the far end of the line carries an item into a chest. */
    @GameTest(structure = TestWorld.ARENA, maxTicks = 300)
    public void oneTouchRunsAWholeBeltLine(GameTestHelper helper) {
        if (noCircuits(helper)) return;
        wired(helper);
        beltLine(helper, 7, 15);
        BlockPos end = new BlockPos(16, 1, BESIDE.getZ());
        helper.setBlock(end, Blocks.CHEST);
        helper.spawnItem(Items.DIAMOND, new BlockPos(12, 2, BESIDE.getZ()));
        helper.succeedWhen(() -> helper.assertValueEqual(helper.getBlockEntity(end, ChestBlockEntity.class).countItem(Items.DIAMOND), 1, "diamonds in chest"));
    }

    /** Cutting the circuit is how a line is stopped now. */
    @GameTest(structure = TestWorld.ARENA, maxTicks = 160)
    public void cuttingThePowerStopsABeltLine(GameTestHelper helper) {
        if (noCircuits(helper)) return;
        wired(helper);
        beltLine(helper, 7, 15);
        BlockPos far = new BlockPos(15, 1, BESIDE.getZ());
        helper.runAtTickTime(LIVE_TICKS, () -> {
            helper.assertBlockProperty(far, ConveyorBeltBlock.POWERED, false);
            helper.setBlock(TAP.west().north(), Blocks.AIR);
        });
        helper.runAtTickTime(LIVE_TICKS + 60, () -> {
            helper.assertBlockProperty(far, ConveyorBeltBlock.POWERED, true);
            helper.succeed();
        });
    }

    @GameTest(structure = TestWorld.ARENA, maxTicks = 80)
    public void aBeltWithNoPowerStandsStill(GameTestHelper helper) {
        if (noCircuits(helper)) return;
        TestWorld.floor(helper, Blocks.STONE);
        beltLine(helper, 7, 15);
        helper.setBlock(BESIDE.north(), Blocks.REDSTONE_BLOCK);
        var item = helper.spawnItem(Items.DIAMOND, new BlockPos(9, 2, BESIDE.getZ()));
        helper.runAtTickTime(60, () -> {
            helper.assertBlockProperty(BESIDE, ConveyorBeltBlock.POWERED, true);
            Vec3 at = helper.relativeVec(item.position());
            helper.assertTrue(at.x < 11.0D, "an unpowered belt carried the item to " + at);
            helper.succeed();
        });
    }

    /** The relays between a supply and what it feeds pass electricity along, and redstone no longer. */
    @GameTest(structure = TestWorld.ARENA, maxTicks = 120)
    public void aControllerPassesElectricityAlong(GameTestHelper helper) {
        if (noCircuits(helper)) return;
        wired(helper);
        helper.setBlock(BESIDE, ModContent.VEHICLE_CONTROLLER.defaultBlockState().setValue(DirectionalSignalBlock.FACING, Direction.EAST));
        BlockPos rail = BESIDE.east();
        railLine(helper, BESIDE.getZ(), rail.getX() + 1);
        TestWorld.rail(helper, rail, ModContent.DRIVE_RAIL, RailShape.EAST_WEST);
        helper.succeedWhen(() -> helper.assertBlockProperty(rail, DriveRailBlock.POWERED, true));
    }

    @GameTest(structure = TestWorld.ARENA, maxTicks = 160)
    public void aPowerCellChargesFromACable(GameTestHelper helper) {
        if (noCircuits(helper)) return;
        wired(helper);
        helper.setBlock(BESIDE, ModContent.POWER_CELL.defaultBlockState().setValue(PowerCellBlock.FACING, Direction.EAST));
        helper.succeedWhen(() -> helper.assertTrue(helper.getBlockState(BESIDE).getValue(PowerCellBlock.CHARGE) > 0, "the cell has no charge"));
    }

    /** A station's boarding signal is a control, not power, so it still releases a drive rail with no cable near. */
    @GameTest(structure = TestWorld.ARENA, maxTicks = 40)
    public void furnacesAndStationsStayOnSignals(GameTestHelper helper) {
        if (noCircuits(helper)) return;
        helper.assertTrue(ElectricPower.ignores(Blocks.REDSTONE_BLOCK.defaultBlockState()), "plain redstone still counts as power");
        helper.assertFalse(ElectricPower.ignores(ModContent.VEHICLE_STATION.defaultBlockState()), "the station's signal stopped counting");
        helper.succeed();
    }
    private static boolean arm(GameTestHelper helper, BlockPos cable, Direction side) {
        BlockState state = helper.getBlockState(cable);
        return state.getValue((BooleanProperty) state.getBlock().getStateDefinition().getProperty(side.getSerializedName()));
    }

    /** True for a cable lit by its current; ALU 0.1.5's cables never light, so there is nothing to see. */
    private static boolean litOrUnlightable(GameTestHelper helper, BlockPos cable) {
        BlockState state = helper.getBlockState(cable);
        return !(state.getBlock().getStateDefinition().getProperty("powered") instanceof BooleanProperty powered)
                || state.getValue(powered);
    }

    /** A live cable draws a lit arm into the machine it runs, so the connection shows. */
    @GameTest(structure = TestWorld.ARENA, maxTicks = 80)
    public void aLiveCableReachesIntoItsMachine(GameTestHelper helper) {
        if (noCircuits(helper)) return;
        wired(helper);
        machineAt(helper, BESIDE);
        helper.runAtTickTime(LIVE_TICKS, () -> {
            helper.assertTrue(arm(helper, TAP, Direction.EAST), "no arm into the machine");
            helper.assertTrue(litOrUnlightable(helper, TAP), "the cable feeding the machine is not lit");
            helper.assertFalse(arm(helper, TAP, Direction.WEST), "an arm into the empty middle of the ring");
            helper.succeed();
        });
    }

    /** Laid before or after, a cable reaches every block that runs on it and nothing else, and lets go of what is gone. */
    @GameTest(structure = TestWorld.ARENA, maxTicks = 40)
    public void cablesReachOnlyWhatRunsOnThem(GameTestHelper helper) {
        if (noCircuits(helper)) return;
        TestWorld.floor(helper, Blocks.STONE);
        BlockPos cable = new BlockPos(12, 1, 6);
        helper.setBlock(cable.east(), ModContent.REDSTONE_MOTOR_2);
        helper.setBlock(cable.west(), Blocks.STONE);
        helper.setBlock(cable.above(), Blocks.REDSTONE_LAMP);
        helper.setBlock(cable, TestWorld.aluBlock("copper_cable"));
        helper.setBlock(cable.south(), belt());
        helper.setBlock(cable.north(), ModContent.FACTORY_CONTROLLER);
        helper.runAfterDelay(2, () -> {
            helper.assertTrue(arm(helper, cable, Direction.EAST), "no arm into a motor laid before the cable");
            helper.assertTrue(arm(helper, cable, Direction.SOUTH), "no arm into a belt laid after the cable");
            helper.assertTrue(arm(helper, cable, Direction.NORTH), "no arm into a factory controller");
            helper.assertFalse(arm(helper, cable, Direction.WEST), "an arm into plain stone");
            helper.assertFalse(arm(helper, cable, Direction.UP), "an arm into a lamp, which electricity does not run");
            helper.setBlock(cable.east(), Blocks.AIR);
            helper.runAfterDelay(1, () -> {
                helper.assertFalse(arm(helper, cable, Direction.EAST), "the arm stayed after the motor went");
                helper.assertTrue(arm(helper, cable, Direction.SOUTH), "the belt lost its arm when the motor went");
                helper.succeed();
            });
        });
    }
}
