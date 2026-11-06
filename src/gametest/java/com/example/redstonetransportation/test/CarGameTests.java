package com.example.redstonetransportation.test;

import com.example.analogalu.ModItems;
import com.example.redstonetransportation.block.CarSeatBlock;
import com.example.redstonetransportation.block.FuelTankBlockEntity;
import com.example.redstonetransportation.block.StructureCoreBlock;
import com.example.redstonetransportation.content.ModContent;
import com.example.redstonetransportation.structure.MovingStructureEntity;
import com.example.redstonetransportation.structure.StructureAssembler;
import com.example.redstonetransportation.structure.StructureKind;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/**
 * Cars: built from blocks with a seat, four wheels, a diesel engine and a fuel tank around a steering
 * wheel, then driven on diesel. The test car faces south (+z):
 * <pre>
 *  y=2:  tank (4,4)   seat (5,5)   wheel/core (5,6)   engine (5,7)
 *  y=1:  a 3 x 4 chassis, x 4..6 and z 4..7, wheels on its four corners
 * </pre>
 */
public final class CarGameTests {
    private static final Input FORWARD = new Input(true, false, false, false, false, false, false);
    private static final BlockPos CORE = new BlockPos(5, 2, 6);

    private static void chassis(GameTestHelper helper, int y, boolean allWheels) {
        for (int x = 4; x <= 6; x++) {
            for (int z = 4; z <= 7; z++) {
                boolean corner = (x == 4 || x == 6) && (z == 4 || z == 7);
                boolean wheel = corner && (allWheels || !(x == 6 && z == 7));
                helper.setBlock(new BlockPos(x, y, z), wheel ? ModContent.CAR_WHEEL : Blocks.IRON_BLOCK);
            }
        }
    }

    private static void car(GameTestHelper helper, int lift, boolean engine, boolean allWheels, int diesel) {
        chassis(helper, 1 + lift, allWheels);
        BlockPos core = CORE.above(lift);
        helper.setBlock(core, ModContent.STEERING_WHEEL.defaultBlockState().setValue(StructureCoreBlock.FACING, Direction.SOUTH));
        helper.setBlock(core.north(), ModContent.CAR_SEAT.defaultBlockState().setValue(CarSeatBlock.FACING, Direction.SOUTH));
        if (engine) helper.setBlock(core.south(), ModContent.CAR_ENGINE);
        BlockPos tank = new BlockPos(4, 2 + lift, 4);
        helper.setBlock(tank, ModContent.FUEL_TANK);
        FuelTankBlockEntity fuel = helper.getBlockEntity(tank, FuelTankBlockEntity.class);
        for (int i = 0; i < diesel; i++) fuel.addDiesel();
    }

    private static StructureAssembler.Result assemble(GameTestHelper helper, BlockPos core) {
        return StructureAssembler.assemble(helper.getLevel(), helper.absolutePos(core), null);
    }

    private static String key(StructureAssembler.Result result) {
        return result.message().getContents() instanceof TranslatableContents contents ? contents.getKey() : "";
    }

    private static MovingStructureEntity build(GameTestHelper helper, int lift, int diesel) {
        car(helper, lift, true, true, diesel);
        StructureAssembler.Result result = assemble(helper, CORE.above(lift));
        if (result.structure() == null) throw new AssertionError("the car did not assemble: " + key(result));
        return result.structure();
    }

    private static Vec3 relative(GameTestHelper helper, MovingStructureEntity car) {
        BlockPos origin = helper.absolutePos(BlockPos.ZERO);
        return car.position().subtract(origin.getX(), origin.getY(), origin.getZ());
    }

    /** Missing an engine or a wheel, it does not assemble and says what is missing; complete, it does. */
    @GameTest(structure = TestWorld.ARENA, maxTicks = 20)
    public void aCarNeedsEveryPart(GameTestHelper helper) {
        TestWorld.floor(helper, Blocks.STONE);
        car(helper, 0, false, true, 0);
        StructureAssembler.Result noEngine = assemble(helper, CORE);
        helper.assertTrue(noEngine.structure() == null, "a car without an engine assembled");
        helper.assertValueEqual(key(noEngine), "message.redstone_transportation.car_missing", "message");
        helper.assertBlockPresent(ModContent.STEERING_WHEEL, CORE);

        car(helper, 0, true, false, 0);
        StructureAssembler.Result threeWheels = assemble(helper, CORE);
        helper.assertTrue(threeWheels.structure() == null, "a car on three wheels assembled");
        helper.assertValueEqual(key(threeWheels), "message.redstone_transportation.car_missing", "message");

        car(helper, 0, true, true, 0);
        StructureAssembler.Result whole = assemble(helper, CORE);
        helper.assertTrue(whole.structure() != null, "a complete car did not assemble: " + key(whole));
        helper.assertValueEqual(whole.structure().kind(), StructureKind.CAR, "kind");
        helper.assertBlockPresent(Blocks.AIR, CORE);
        helper.succeed();
    }

    @GameTest(structure = TestWorld.ARENA, maxTicks = 80)
    public void aCarDrivesOnDiesel(GameTestHelper helper) {
        TestWorld.floor(helper, Blocks.STONE);
        MovingStructureEntity car = build(helper, 0, 1);
        int fuel = car.carFuel();
        double startZ = car.getZ();
        car.steerForTest(FORWARD);
        helper.runAfterDelay(30, () -> {
            helper.assertTrue(car.getZ() - startZ > 4.0D, "the car went only " + (car.getZ() - startZ) + " blocks");
            helper.assertTrue(car.carFuel() < fuel, "driving burned no fuel");
            helper.assertTrue(Math.abs(car.getX() - helper.absolutePos(CORE).getX() - 0.5D) < 1.0E-6D, "the car drifted sideways");
            car.steerForTest(Input.EMPTY);
            helper.succeed();
        });
    }

    @GameTest(structure = TestWorld.ARENA, maxTicks = 60)
    public void aCarWithAnEmptyTankStaysPut(GameTestHelper helper) {
        TestWorld.floor(helper, Blocks.STONE);
        MovingStructureEntity car = build(helper, 0, 0);
        Vec3 start = car.position();
        car.steerForTest(FORWARD);
        helper.runAfterDelay(40, () -> {
            helper.assertTrue(car.position().distanceTo(start) < 1.0E-6D, "a car with no diesel moved");
            helper.succeed();
        });
    }

    /** Built on a platform that is then taken away, it falls to the floor and lands on whole blocks. */
    @GameTest(structure = TestWorld.ARENA, maxTicks = 80)
    public void aCarFallsWhenNothingHoldsItUp(GameTestHelper helper) {
        TestWorld.floor(helper, Blocks.STONE);
        for (int x = 3; x <= 7; x++) {
            for (int z = 3; z <= 8; z++) helper.setBlock(new BlockPos(x, 2, z), Blocks.STONE);
        }
        MovingStructureEntity car = build(helper, 2, 0);
        helper.assertTrue(Math.abs(relative(helper, car).y - 4.0D) < 1.0E-6D, "the car did not start on the platform");
        for (int x = 3; x <= 7; x++) {
            for (int z = 3; z <= 8; z++) helper.setBlock(new BlockPos(x, 2, z), Blocks.AIR);
        }
        helper.succeedWhen(() -> {
            double y = relative(helper, car).y;
            helper.assertTrue(Math.abs(y - 2.0D) < 1.0E-6D, "the car is at y " + y + ", not resting on the floor");
        });
    }

    /** A step one block high across its path is climbed. */
    @GameTest(structure = TestWorld.ARENA, maxTicks = 120)
    public void aCarClimbsAOneBlockStep(GameTestHelper helper) {
        TestWorld.floor(helper, Blocks.STONE);
        MovingStructureEntity car = build(helper, 0, 1);
        for (int x = 2; x <= 8; x++) {
            for (int z = 12; z <= 22; z++) helper.setBlock(new BlockPos(x, 1, z), Blocks.STONE);
        }
        car.steerForTest(FORWARD);
        helper.succeedWhen(() -> {
            Vec3 at = relative(helper, car);
            helper.assertTrue(at.z > 14.0D, "the car is only at z " + at.z);
            helper.assertTrue(Math.abs(at.y - 3.0D) < 1.0E-6D, "the car is at y " + at.y + ", not up on the step");
            car.steerForTest(Input.EMPTY);
        });
    }

    /** Diesel goes into the car's tank by right-clicking the car, and the tank keeps what is left when it is set down. */
    @GameTest(structure = TestWorld.ARENA, maxTicks = 80)
    public void aCarIsRefuelledAndKeepsItsFuel(GameTestHelper helper) {
        TestWorld.floor(helper, Blocks.STONE);
        MovingStructureEntity car = build(helper, 0, 0);
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.DIESEL, 3));
        car.interact(player, InteractionHand.MAIN_HAND, car.position());
        helper.assertValueEqual(car.carFuel(), FuelTankBlockEntity.TICKS_PER_DIESEL, "fuel after one diesel");
        helper.assertValueEqual(player.getMainHandItem().getCount(), 2, "diesel left in hand");
        car.steerForTest(FORWARD);
        helper.runAfterDelay(20, () -> {
            car.steerForTest(Input.EMPTY);
            helper.runAfterDelay(40, () -> {
                int left = car.carFuel();
                helper.assertTrue(left < FuelTankBlockEntity.TICKS_PER_DIESEL, "no fuel was used");
                StructureAssembler.restore(helper.getLevel(), car);
                BlockPos tank = BlockPos.containing(car.getX(), car.getY(), car.getZ()).offset(-1, 0, -2);
                FuelTankBlockEntity placed = helper.getLevel().getBlockEntity(tank) instanceof FuelTankBlockEntity found ? found : null;
                helper.assertTrue(placed != null, "no tank where it should be at " + tank);
                helper.assertValueEqual(placed.fuel(), left, "fuel in the tank once set down");
                helper.succeed();
            });
        });
    }
}
