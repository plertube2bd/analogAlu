package com.example.redstonetransportation.test;

import com.example.redstonetransportation.block.AluTrainControllerBlock;
import com.example.redstonetransportation.block.DriveRailBlock;
import com.example.redstonetransportation.content.ModContent;
import com.example.redstonetransportation.integration.analogalu.AnalogAluSignalAdapter;
import com.example.redstonetransportation.item.TrackLayerItem;
import com.example.redstonetransportation.machine.FactoryControllerBlock;
import com.example.redstonetransportation.machine.MachineBlock;
import com.example.redstonetransportation.machine.MachineStatus;
import com.example.redstonetransportation.machine.MachineType;
import com.example.redstonetransportation.rail.CartDrive;
import com.example.redstonetransportation.rail.RailDrive;
import com.example.redstonetransportation.rail.TrackPlanner;
import com.example.redstonetransportation.vehicle.ModEntityTypes;
import com.example.redstonetransportation.vehicle.RedstoneTramEntity;
import com.example.redstonetransportation.vehicle.TrainLinkManager;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.minecart.AbstractMinecart;
import net.minecraft.world.entity.vehicle.minecart.MinecartFurnace;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.BaseRailBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.RailShape;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public final class RailGameTests {
    private static final double VANILLA_SPEED = 0.4D;

    /** The whole point of the new rail: one powered rail, then full speed around corners without derailing. */
    @GameTest(structure = TestWorld.ARENA, maxTicks = 400)
    public void tramHoldsFullSpeedThroughCurves(GameTestHelper helper) {
        TestWorld.floor(helper, Blocks.STONE);
        TestWorld.railLoop(helper, 2, 2, 28, 28);
        BlockPos driveRail = new BlockPos(14, 1, 2);
        TestWorld.rail(helper, driveRail, ModContent.DRIVE_RAIL, RailShape.EAST_WEST);
        helper.setBlock(driveRail.north(), TestContent.SIGNAL);

        RedstoneTramEntity tram = helper.spawn(ModEntityTypes.REDSTONE_TRAM, new BlockPos(12, 1, 2));
        tram.setDeltaMovement(0.1D, 0.0D, 0.0D);

        helper.runAtTickTime(300, () -> {
            double speed = tram.getDeltaMovement().horizontalDistance();
            helper.assertTrue(tram.isOnRails(), "tram left the rails");
            helper.assertValueInBetween(RailDrive.DRIVE_RAIL_TOP_SPEED - 0.1D, speed, RailDrive.DRIVE_RAIL_TOP_SPEED + 0.1D, "tram speed");
            helper.succeed();
        });
    }

    /** Drive rails no longer reach this speed, but Motor IV does, and curves must hold at it. */
    @GameTest(structure = TestWorld.ARENA, maxTicks = 400)
    public void tramHoldsMotorSpeedThroughCurves(GameTestHelper helper) {
        TestWorld.floor(helper, Blocks.STONE);
        TestWorld.railLoop(helper, 2, 2, 28, 28);

        RedstoneTramEntity tram = helper.spawn(ModEntityTypes.REDSTONE_TRAM, new BlockPos(12, 1, 2));
        float speed = (float) RailDrive.MAX_SPEED;
        tram.setAttached(RailDrive.DRIVE, new CartDrive(speed, speed + 1.0F, 1.0F, 0.0F, Optional.empty()));
        tram.setDeltaMovement(RailDrive.MAX_SPEED, 0.0D, 0.0D);

        helper.runAtTickTime(300, () -> {
            helper.assertTrue(tram.isOnRails(), "tram left the rails");
            helper.assertValueInBetween(RailDrive.MAX_SPEED - 0.3D, tram.getDeltaMovement().horizontalDistance(), RailDrive.MAX_SPEED + 0.3D, "tram speed");
            helper.succeed();
        });
    }

    /** Vanilla carts were clamped to 0.4 blocks/tick and ignored by motors before. */
    @GameTest(structure = TestWorld.ARENA, maxTicks = 200)
    public void everyCartTypeRunsOnDriveRails(GameTestHelper helper) {
        TestWorld.floor(helper, Blocks.STONE);
        List<EntityType<? extends AbstractMinecart>> types = List.of(
                EntityTypes.MINECART, EntityTypes.CHEST_MINECART, EntityTypes.HOPPER_MINECART,
                EntityTypes.FURNACE_MINECART, EntityTypes.TNT_MINECART,
                ModEntityTypes.REDSTONE_TRAM, ModEntityTypes.CARGO_TRAM
        );

        int row = 2;
        for (EntityType<? extends AbstractMinecart> type : types) {
            for (int x = 1; x < 31; x++) {
                TestWorld.rail(helper, new BlockPos(x, 1, row), Blocks.RAIL, RailShape.EAST_WEST);
            }
            TestWorld.rail(helper, new BlockPos(3, 1, row), ModContent.DRIVE_RAIL, RailShape.EAST_WEST);
            helper.setBlock(new BlockPos(3, 0, row), TestContent.SIGNAL);
            AbstractMinecart cart = helper.spawn(type, new BlockPos(2, 1, row));
            cart.setDeltaMovement(0.05D, 0.0D, 0.0D);

            helper.runAtTickTime(30, () -> {
                double speed = cart.getDeltaMovement().horizontalDistance();
                helper.assertTrue(speed > VANILLA_SPEED + 0.5D, type + " only reached " + speed);
            });
            row += 2;
        }
        helper.runAtTickTime(31, helper::succeed);
    }

    @GameTest(structure = TestWorld.ARENA, maxTicks = 200)
    public void motorsDriveVanillaCarts(GameTestHelper helper) {
        TestWorld.floor(helper, Blocks.STONE);
        for (int x = 1; x < 31; x++) {
            TestWorld.rail(helper, new BlockPos(x, 1, 10), Blocks.RAIL, RailShape.EAST_WEST);
        }
        for (int x = 4; x <= 12; x += 2) {
            helper.setBlock(new BlockPos(x, 0, 10), ModContent.REDSTONE_MOTOR_4.defaultBlockState()
                    .setValue(com.example.redstonetransportation.block.RailMotorBlock.FACING, Direction.EAST));
            helper.setBlock(new BlockPos(x, 0, 9), TestContent.SIGNAL);
        }
        AbstractMinecart cart = helper.spawn(EntityTypes.MINECART, new BlockPos(3, 1, 10));
        cart.setDeltaMovement(0.1D, 0.0D, 0.0D);

        helper.succeedWhen(() -> helper.assertTrue(
                cart.getDeltaMovement().horizontalDistance() > RailDrive.DRIVE_RAIL_TOP_SPEED + 0.2D, "motor did not take the cart past drive rail speed"));
    }

    /** A motor only pushes a cruising cart while it is in reach; past the motors it settles back to the cruise. */
    @GameTest(structure = TestWorld.ARENA, maxTicks = 200)
    public void cruisingCartReturnsToItsSpeedPastMotors(GameTestHelper helper) {
        TestWorld.floor(helper, Blocks.STONE);
        for (int x = 1; x < 31; x++) {
            TestWorld.rail(helper, new BlockPos(x, 1, 10), Blocks.RAIL, RailShape.EAST_WEST);
        }
        BlockPos driveRail = new BlockPos(3, 1, 10);
        TestWorld.rail(helper, driveRail, ModContent.DRIVE_RAIL, RailShape.EAST_WEST);
        helper.setBlock(driveRail.north(), TestContent.SIGNAL);
        for (int x = 8; x <= 16; x += 2) {
            helper.setBlock(new BlockPos(x, 0, 10), ModContent.REDSTONE_MOTOR_4.defaultBlockState()
                    .setValue(com.example.redstonetransportation.block.RailMotorBlock.FACING, Direction.EAST));
            helper.setBlock(new BlockPos(x, 0, 9), TestContent.SIGNAL);
        }
        AbstractMinecart cart = helper.spawn(EntityTypes.MINECART, new BlockPos(2, 1, 10));
        cart.setDeltaMovement(0.1D, 0.0D, 0.0D);

        helper.succeedWhen(() -> {
            helper.assertTrue(helper.relativeVec(cart.position()).x > 24.0D, "cart has not passed the motors");
            double speed = cart.getDeltaMovement().horizontalDistance();
            helper.assertValueInBetween(RailDrive.DRIVE_RAIL_TOP_SPEED - 0.1D, speed, RailDrive.DRIVE_RAIL_TOP_SPEED + 0.1D, "speed past the motors");
        });
    }

    /** Signal 0 holds a cart on the rail centre; raising the signal sends it off along the rail's arrow. */
    @GameTest(structure = TestWorld.ARENA, maxTicks = 200)
    public void unpoweredDriveRailHoldsThenReleases(GameTestHelper helper) {
        TestWorld.floor(helper, Blocks.STONE);
        for (int x = 1; x < 31; x++) {
            TestWorld.rail(helper, new BlockPos(x, 1, 10), Blocks.RAIL, RailShape.EAST_WEST);
        }
        BlockPos stop = new BlockPos(15, 1, 10);
        TestWorld.rail(helper, stop, ModContent.DRIVE_RAIL, RailShape.EAST_WEST);
        AbstractMinecart cart = helper.spawn(EntityTypes.MINECART, new BlockPos(5, 1, 10));
        cart.setDeltaMovement(0.4D, 0.0D, 0.0D);

        helper.runAtTickTime(60, () -> {
            Vec3 relative = helper.relativeVec(cart.position());
            helper.assertValueInBetween(15.4D, relative.x, 15.6D, "held cart x");
            helper.assertTrue(cart.getDeltaMovement().lengthSqr() < 1.0E-6D, "held cart is still moving");
            helper.setBlock(stop.north(), TestContent.SIGNAL);
        });
        helper.runAtTickTime(80, () -> {
            helper.assertTrue(helper.relativeVec(cart.position()).x > 16.5D, "released cart did not leave eastward");
            helper.assertBlockProperty(stop, DriveRailBlock.POWERED, true);
            helper.succeed();
        });
    }

    /** Sneak-flip the arrow and a standing cart leaves the other way. */
    @GameTest(structure = TestWorld.ARENA, maxTicks = 100)
    public void reversedDriveRailDepartsBackwards(GameTestHelper helper) {
        TestWorld.floor(helper, Blocks.STONE);
        for (int x = 1; x < 31; x++) {
            TestWorld.rail(helper, new BlockPos(x, 1, 10), Blocks.RAIL, RailShape.EAST_WEST);
        }
        BlockPos start = new BlockPos(15, 1, 10);
        TestWorld.rail(helper, start, ModContent.DRIVE_RAIL, RailShape.EAST_WEST);
        helper.getLevel().setBlock(helper.absolutePos(start),
                helper.getBlockState(start).setValue(DriveRailBlock.REVERSED, true), Block.UPDATE_CLIENTS);
        helper.setBlock(start.north(), TestContent.SIGNAL);
        AbstractMinecart cart = helper.spawn(EntityTypes.MINECART, start);

        helper.succeedWhen(() -> helper.assertTrue(helper.relativeVec(cart.position()).x < 13.0D, "cart did not leave westward"));
    }

    /** The speed set by one rail survives until the next one changes it. Signal 6 is 12 m/s, or 0.6 blocks/tick. */
    @GameTest(structure = TestWorld.ARENA, maxTicks = 200)
    public void secondDriveRailChangesTheCruiseSpeed(GameTestHelper helper) {
        TestWorld.floor(helper, Blocks.STONE);
        TestWorld.railLoop(helper, 2, 12, 28, 28);
        BlockPos fast = new BlockPos(10, 1, 12);
        BlockPos slow = new BlockPos(20, 1, 12);
        TestWorld.rail(helper, fast, ModContent.DRIVE_RAIL, RailShape.EAST_WEST);
        TestWorld.rail(helper, slow, ModContent.DRIVE_RAIL, RailShape.EAST_WEST);
        helper.setBlock(fast.north(), TestContent.SIGNAL);
        helper.setBlock(slow.north(), TestContent.WEAK_SIGNAL);
        AbstractMinecart cart = helper.spawn(EntityTypes.MINECART, new BlockPos(8, 1, 12));
        cart.setDeltaMovement(0.1D, 0.0D, 0.0D);

        helper.runAtTickTime(150, () -> {
            double speed = cart.getDeltaMovement().horizontalDistance();
            helper.assertTrue(cart.isOnRails(), "cart left the rails");
            helper.assertValueInBetween(0.55D, speed, 0.65D, "cruise speed");
            helper.succeed();
        });
    }

    /** Couplings now live on any cart, and the whole train takes the cruise speed. */
    @GameTest(structure = TestWorld.ARENA, maxTicks = 200)
    public void coupledVanillaCartsCruiseAsOneTrain(GameTestHelper helper) {
        TestWorld.floor(helper, Blocks.STONE);
        TestWorld.railLoop(helper, 2, 2, 28, 28);
        BlockPos driveRail = new BlockPos(14, 1, 2);
        TestWorld.rail(helper, driveRail, ModContent.DRIVE_RAIL, RailShape.EAST_WEST);
        helper.setBlock(driveRail.north(), TestContent.SIGNAL);

        AbstractMinecart lead = helper.spawn(EntityTypes.MINECART, new BlockPos(13, 1, 2));
        AbstractMinecart middle = helper.spawn(EntityTypes.CHEST_MINECART, new BlockPos(12, 1, 2));
        AbstractMinecart tail = helper.spawn(ModEntityTypes.CARGO_TRAM, new BlockPos(10, 1, 2));
        link(lead, middle);
        link(middle, tail);
        lead.setDeltaMovement(0.1D, 0.0D, 0.0D);

        helper.runAtTickTime(120, () -> {
            helper.assertTrue(lead.isOnRails() && middle.isOnRails() && tail.isOnRails(), "a car left the rails");
            helper.assertTrue(middle.getAttached(RailDrive.DRIVE) != null, "the middle car did not take the cruise");
            helper.assertTrue(lead.getDeltaMovement().horizontalDistance() > RailDrive.DRIVE_RAIL_TOP_SPEED - 0.3D, "train is not at speed: " + lead.getDeltaMovement());
            // Carts sample mid-tick, so a car can read up to one tick of travel further from its neighbour.
            helper.assertTrue(lead.distanceTo(middle) < 8.0F, "train stretched: " + lead.distanceTo(middle));
            helper.assertTrue(middle.distanceTo(tail) < 8.0F, "train stretched: " + middle.distanceTo(tail));
            helper.succeed();
        });
    }

    /** The coupler goes through the same click event a player's right-click does, on every cart that has its own click action. */
    @GameTest(structure = TestWorld.ARENA, maxTicks = 40)
    public void couplerClicksLinkEveryCartType(GameTestHelper helper) {
        TestWorld.floor(helper, Blocks.STONE);
        for (int x = 1; x < 16; x++) {
            TestWorld.rail(helper, new BlockPos(x, 1, 10), Blocks.RAIL, RailShape.EAST_WEST);
        }
        AbstractMinecart chest = helper.spawn(EntityTypes.CHEST_MINECART, new BlockPos(3, 1, 10));
        AbstractMinecart hopper = helper.spawn(EntityTypes.HOPPER_MINECART, new BlockPos(6, 1, 10));
        AbstractMinecart furnace = helper.spawn(EntityTypes.FURNACE_MINECART, new BlockPos(9, 1, 10));
        AbstractMinecart tram = helper.spawn(ModEntityTypes.REDSTONE_TRAM, new BlockPos(12, 1, 10));
        Player player = TestWorld.creativePlayer(helper);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModContent.TRAM_COUPLER_ITEM));

        clickCoupler(helper, player, chest);
        clickCoupler(helper, player, hopper);
        clickCoupler(helper, player, hopper);
        clickCoupler(helper, player, furnace);
        clickCoupler(helper, player, furnace);
        clickCoupler(helper, player, tram);

        helper.assertTrue(TrainLinkManager.isCoupled(chest, hopper), "chest and hopper carts did not couple");
        helper.assertTrue(TrainLinkManager.isCoupled(hopper, furnace), "hopper and furnace carts did not couple");
        helper.assertTrue(TrainLinkManager.isCoupled(furnace, tram), "furnace cart and tram did not couple");
        helper.succeed();
    }

    private static void clickCoupler(GameTestHelper helper, Player player, AbstractMinecart cart) {
        InteractionResult result = UseEntityCallback.EVENT.invoker().interact(player, helper.getLevel(), InteractionHand.MAIN_HAND, cart,
                new EntityHitResult(cart));
        helper.assertTrue(result == InteractionResult.SUCCESS, "the coupler click reached the cart's own action");
    }

    /** All the vanilla freight carts in one train, furnace cart included, at full speed through curves. */
    @GameTest(structure = TestWorld.ARENA, maxTicks = 300)
    public void vanillaFreightCartsCruiseAsOneTrain(GameTestHelper helper) {
        TestWorld.floor(helper, Blocks.STONE);
        TestWorld.railLoop(helper, 2, 2, 28, 28);
        BlockPos driveRail = new BlockPos(14, 1, 2);
        TestWorld.rail(helper, driveRail, ModContent.DRIVE_RAIL, RailShape.EAST_WEST);
        helper.setBlock(driveRail.north(), TestContent.SIGNAL);

        MinecartFurnace furnace = helper.spawn(EntityTypes.FURNACE_MINECART, new BlockPos(13, 1, 2));
        AbstractMinecart hopper = helper.spawn(EntityTypes.HOPPER_MINECART, new BlockPos(11, 1, 2));
        AbstractMinecart chest = helper.spawn(EntityTypes.CHEST_MINECART, new BlockPos(9, 1, 2));
        furnace.addFuel(furnace.position().subtract(1.0D, 0.0D, 0.0D), new ItemStack(Items.COAL));
        link(furnace, hopper);
        link(hopper, chest);
        furnace.setDeltaMovement(0.1D, 0.0D, 0.0D);

        helper.runAtTickTime(200, () -> {
            helper.assertTrue(furnace.isOnRails() && hopper.isOnRails() && chest.isOnRails(), "a car left the rails");
            for (AbstractMinecart car : List.of(furnace, hopper, chest)) {
                double speed = car.getDeltaMovement().horizontalDistance();
                helper.assertTrue(speed > RailDrive.DRIVE_RAIL_TOP_SPEED - 0.3D, car.getType() + " is not at speed: " + speed);
            }
            helper.assertTrue(furnace.distanceTo(hopper) < 8.0F, "train stretched: " + furnace.distanceTo(hopper));
            helper.assertTrue(hopper.distanceTo(chest) < 8.0F, "train stretched: " + hopper.distanceTo(chest));
            helper.succeed();
        });
    }

    /** With no drive rails at all, a lit furnace cart works as a vanilla locomotive for the cars coupled to it. */
    @GameTest(structure = TestWorld.ARENA, maxTicks = 300)
    public void fueledFurnaceCartPullsCoupledCarts(GameTestHelper helper) {
        TestWorld.floor(helper, Blocks.STONE);
        for (int x = 1; x < 31; x++) {
            TestWorld.rail(helper, new BlockPos(x, 1, 10), Blocks.RAIL, RailShape.EAST_WEST);
        }
        MinecartFurnace furnace = helper.spawn(EntityTypes.FURNACE_MINECART, new BlockPos(8, 1, 10));
        AbstractMinecart hopper = helper.spawn(EntityTypes.HOPPER_MINECART, new BlockPos(6, 1, 10));
        AbstractMinecart chest = helper.spawn(EntityTypes.CHEST_MINECART, new BlockPos(4, 1, 10));
        link(furnace, hopper);
        link(hopper, chest);
        furnace.addFuel(furnace.position().subtract(1.0D, 0.0D, 0.0D), new ItemStack(Items.COAL));

        helper.runAtTickTime(200, () -> {
            double chestX = helper.relativeVec(chest.position()).x;
            helper.assertTrue(chestX > 8.0D, "the last car was not pulled along: x=" + chestX);
            helper.assertTrue(furnace.distanceTo(chest) < 7.0F, "train stretched: " + furnace.distanceTo(chest));
            helper.succeed();
        });
    }

    @GameTest(structure = TestWorld.ARENA, maxTicks = 40)
    public void couplingSurvivesOnlyWhileBothCartsExist(GameTestHelper helper) {
        TestWorld.floor(helper, Blocks.STONE);
        for (int x = 1; x < 12; x++) {
            TestWorld.rail(helper, new BlockPos(x, 1, 10), Blocks.RAIL, RailShape.EAST_WEST);
        }
        AbstractMinecart first = helper.spawn(EntityTypes.MINECART, new BlockPos(3, 1, 10));
        AbstractMinecart second = helper.spawn(EntityTypes.MINECART, new BlockPos(6, 1, 10));
        UUID firstId = first.getUUID();
        link(first, second);

        helper.runAtTickTime(2, () -> first.discard());
        helper.runAtTickTime(4, () -> {
            helper.assertFalse(TrainLinkManager.getLinks(second).contains(firstId), "dangling coupling remains");
            helper.succeed();
        });
    }

    /** Couplings and the cruise speed are saved on the cart, for vanilla carts too. */
    @GameTest(structure = TestWorld.ARENA, maxTicks = 20)
    public void cartStateSurvivesSaveAndLoad(GameTestHelper helper) {
        TestWorld.floor(helper, Blocks.STONE);
        AbstractMinecart cart = helper.spawn(EntityTypes.MINECART, new BlockPos(3, 1, 3));
        UUID partner = UUID.randomUUID();
        TrainLinkManager.addLink(cart, partner);
        RailDrive.allowSpeed(cart, 2.5D);

        TagValueOutput output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, helper.getLevel().registryAccess());
        cart.saveWithoutId(output);
        AbstractMinecart loaded = EntityTypes.MINECART.create(helper.getLevel(), EntitySpawnReason.LOAD);
        loaded.load(TagValueInput.create(ProblemReporter.DISCARDING, helper.getLevel().registryAccess(), output.buildResult()));

        helper.assertTrue(TrainLinkManager.getLinks(loaded).contains(partner), "coupling was not saved");
        helper.assertTrue(loaded.getAttached(RailDrive.DRIVE) != null, "drive state was not saved");
        helper.assertValueEqual(loaded.getAttached(RailDrive.DRIVE).limit(), 2.5F, "saved speed limit");
        helper.succeed();
    }

    /** Trams from 1.5.0 worlds kept their couplings in a string field and carried a display block. */
    @GameTest(structure = TestWorld.ARENA, maxTicks = 20)
    public void oldTramSaveDataStillLoads(GameTestHelper helper) {
        TestWorld.floor(helper, Blocks.STONE);
        UUID partner = UUID.randomUUID();
        RedstoneTramEntity tram = helper.spawn(ModEntityTypes.REDSTONE_TRAM, new BlockPos(3, 1, 3));

        TagValueOutput output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, helper.getLevel().registryAccess());
        tram.saveWithoutId(output);
        CompoundTag saved = output.buildResult();
        saved.putString("train_links", partner + ", not-a-uuid");
        saved.store("DisplayState", BlockState.CODEC, ModContent.REDSTONE_TRAM_BODY.defaultBlockState());

        RedstoneTramEntity loaded = ModEntityTypes.REDSTONE_TRAM.create(helper.getLevel(), EntitySpawnReason.LOAD);
        loaded.load(TagValueInput.create(ProblemReporter.DISCARDING, helper.getLevel().registryAccess(), saved));

        helper.assertTrue(TrainLinkManager.getLinks(loaded).contains(partner), "old coupling was lost");
        helper.assertValueEqual(TrainLinkManager.getLinks(loaded).size(), 1, "couplings loaded");
        helper.succeed();
    }

    /** A long train stopped by a signal-0 rail must start again as one, whichever way it was parked. */
    @GameTest(structure = TestWorld.ARENA, maxTicks = 200)
    public void heldTrainDepartsTogether(GameTestHelper helper) {
        TestWorld.floor(helper, Blocks.STONE);
        for (int x = 1; x < 31; x++) {
            TestWorld.rail(helper, new BlockPos(x, 1, 10), Blocks.RAIL, RailShape.EAST_WEST);
        }
        BlockPos stop = new BlockPos(16, 1, 10);
        TestWorld.rail(helper, stop, ModContent.DRIVE_RAIL, RailShape.EAST_WEST);
        AbstractMinecart lead = helper.spawn(EntityTypes.MINECART, new BlockPos(13, 1, 10));
        AbstractMinecart tail = helper.spawn(EntityTypes.MINECART, new BlockPos(11, 1, 10));
        link(lead, tail);
        lead.setDeltaMovement(0.3D, 0.0D, 0.0D);
        tail.setDeltaMovement(0.3D, 0.0D, 0.0D);

        helper.runAtTickTime(40, () -> {
            helper.assertValueInBetween(16.4D, helper.relativeVec(lead.position()).x, 16.6D, "held front car x");
            helper.assertTrue(helper.relativeVec(tail.position()).x < 15.5D, "the rear car ran past the front one");
            helper.setBlock(stop.north(), TestContent.SIGNAL);
        });
        helper.runAtTickTime(60, () -> {
            helper.assertTrue(helper.relativeVec(lead.position()).x > 18.0D, "the front car did not leave");
            helper.assertTrue(helper.relativeVec(tail.position()).x > 15.0D, "the rear car was left behind");
            helper.assertTrue(lead.distanceTo(tail) < 5.0F, "train stretched: " + lead.distanceTo(tail));
            helper.succeed();
        });
    }

    /** Two clicks lay a line with a bend and climbs, and a cart can ride it from end to end. */
    @GameTest(structure = TestWorld.ARENA, maxTicks = 300)
    public void trackLayerBuildsARideableLine(GameTestHelper helper) {
        TestWorld.floor(helper, Blocks.STONE);
        Player player = TestWorld.creativePlayer(helper);
        // Terrain in the way, and a gap under the climb, are the layer's job.
        for (int z = 2; z < 8; z++) {
            helper.setBlock(new BlockPos(12, 1, z), Blocks.STONE);
        }

        BlockPos firstRail = new BlockPos(3, 1, 4);
        BlockPos lastRail = new BlockPos(24, 4, 20);
        List<TrackPlanner.Cell> cells = plan(helper, firstRail, lastRail, TrackPlanner.Style.CORNER);
        helper.assertTrue(cells != null, "plan failed");
        helper.assertTrue(TrackLayerItem.lay(helper.getLevel(), player, cells) == null, "layer refused the line");

        for (TrackPlanner.Cell cell : cells) {
            helper.assertTrue(BaseRailBlock.isRail(helper.getLevel().getBlockState(cell.pos())), "missing rail at " + cell.pos());
        }
        TestWorld.rail(helper, firstRail, ModContent.DRIVE_RAIL, cells.getFirst().shape());
        helper.setBlock(firstRail.south(), TestContent.SIGNAL);

        AbstractMinecart cart = helper.spawn(EntityTypes.MINECART, firstRail);
        helper.succeedWhen(() -> {
            Vec3 at = helper.relativeVec(cart.position());
            helper.assertTrue(at.distanceTo(Vec3.atCenterOf(lastRail)) < 3.0D, "cart is at " + at);
        });
    }

    @GameTest(structure = TestWorld.ARENA, maxTicks = 20)
    public void trackLayerChargesSurvivalPlayers(GameTestHelper helper) {
        TestWorld.floor(helper, Blocks.STONE);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        List<TrackPlanner.Cell> cells = plan(helper, new BlockPos(3, 1, 4), new BlockPos(13, 1, 4), TrackPlanner.Style.CORNER);

        helper.assertTrue(TrackLayerItem.lay(helper.getLevel(), player, cells) != null, "laid rails it could not pay for");
        helper.assertBlockNotPresent(Blocks.RAIL, new BlockPos(5, 1, 4));

        player.getInventory().add(new ItemStack(Items.RAIL, 32));
        helper.assertTrue(TrackLayerItem.lay(helper.getLevel(), player, cells) == null, "refused a paid line");
        helper.assertBlockPresent(Blocks.RAIL, new BlockPos(5, 1, 4));
        helper.assertValueEqual(player.getInventory().countItem(Items.RAIL), 32 - cells.size(), "rails left");
        helper.succeed();
    }

    /** A right click with the layer, the way a player makes one: on a rail, or on the ground under the next rail. */
    private static void clickLayer(GameTestHelper helper, ServerPlayer player, BlockPos rail) {
        BlockPos clicked = helper.absolutePos(rail);
        if (!BaseRailBlock.isRail(helper.getLevel(), clicked)) clicked = clicked.below();
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(clicked), Direction.UP, clicked, false);
        ModContent.TRACK_LAYER_ITEM.useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, hit));
    }

    private static List<TrackPlanner.Cell> plan(GameTestHelper helper, BlockPos from, BlockPos to, TrackPlanner.Style style) {
        return TrackPlanner.plan(helper.absolutePos(from), helper.absolutePos(to), null, null, style);
    }

    /** Follows the joints between rails, as a cart would, to see whether two rails really are on one line. */
    private static boolean railsLink(GameTestHelper helper, BlockPos from, BlockPos to) {
        BlockPos goal = helper.absolutePos(to);
        Set<BlockPos> seen = new HashSet<>();
        ArrayDeque<BlockPos> next = new ArrayDeque<>(List.of(helper.absolutePos(from)));
        while (!next.isEmpty()) {
            BlockPos pos = next.poll();
            if (pos.equals(goal)) return true;
            if (seen.add(pos)) next.addAll(TrackLayerItem.joinedRails(helper.getLevel(), pos));
        }
        return false;
    }

    /** Sends a cart off the first rail and passes once it reaches the last. */
    private static void assertCartRides(GameTestHelper helper, BlockPos first, RailShape firstShape, BlockPos last) {
        TestWorld.rail(helper, first, ModContent.DRIVE_RAIL, firstShape);
        helper.setBlock(first.west(), TestContent.SIGNAL);
        AbstractMinecart cart = helper.spawn(EntityTypes.MINECART, first);
        helper.succeedWhen(() -> {
            Vec3 at = helper.relativeVec(cart.position());
            helper.assertTrue(at.distanceTo(Vec3.atCenterOf(last)) < 2.5D, "cart is at " + at);
        });
    }

    private static void layStraightRails(GameTestHelper helper, int fromX, int toX, int z) {
        for (int x = fromX; x <= toX; x++) TestWorld.rail(helper, new BlockPos(x, 1, z), Blocks.RAIL, RailShape.EAST_WEST);
    }

    /** The third click turns a corner on the rail the second click left, and a cart gets round it. */
    @GameTest(structure = TestWorld.ARENA, maxTicks = 200)
    public void extendingALineTurnsTheCorner(GameTestHelper helper) {
        TestWorld.floor(helper, Blocks.STONE);
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        clickLayer(helper, player, new BlockPos(3, 1, 4));
        clickLayer(helper, player, new BlockPos(13, 1, 4));
        clickLayer(helper, player, new BlockPos(13, 1, 14));
        helper.assertTrue(railsLink(helper, new BlockPos(3, 1, 4), new BlockPos(13, 1, 14)), "the corner does not connect");
        assertCartRides(helper, new BlockPos(3, 1, 4), RailShape.EAST_WEST, new BlockPos(13, 1, 14));
    }

    /** A powered rail cannot bend, so the one at the corner gives way to a plain rail. */
    @GameTest(structure = TestWorld.ARENA, maxTicks = 20)
    public void aPoweredRailLineStillTurns(GameTestHelper helper) {
        TestWorld.floor(helper, Blocks.STONE);
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(Items.POWERED_RAIL));
        clickLayer(helper, player, new BlockPos(3, 1, 4));
        clickLayer(helper, player, new BlockPos(13, 1, 4));
        clickLayer(helper, player, new BlockPos(13, 1, 14));
        helper.assertTrue(railsLink(helper, new BlockPos(3, 1, 4), new BlockPos(13, 1, 14)), "the corner does not connect");
        helper.assertBlockPresent(Blocks.POWERED_RAIL, new BlockPos(8, 1, 4));
        helper.assertBlockPresent(Blocks.RAIL, new BlockPos(13, 1, 4));
        helper.succeed();
    }

    /** A line started on the end of old track turns off it, however long ago that track was laid. */
    @GameTest(structure = TestWorld.ARENA, maxTicks = 20)
    public void aLineStartedOnOldTrackJoinsIt(GameTestHelper helper) {
        TestWorld.floor(helper, Blocks.STONE);
        layStraightRails(helper, 3, 13, 4);
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        clickLayer(helper, player, new BlockPos(13, 1, 4));
        clickLayer(helper, player, new BlockPos(13, 1, 14));
        helper.assertTrue(railsLink(helper, new BlockPos(3, 1, 4), new BlockPos(13, 1, 14)), "the new line did not join the old one");
        helper.succeed();
    }

    @GameTest(structure = TestWorld.ARENA, maxTicks = 20)
    public void aLineEndingOnOldTrackJoinsIt(GameTestHelper helper) {
        TestWorld.floor(helper, Blocks.STONE);
        layStraightRails(helper, 3, 13, 4);
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        clickLayer(helper, player, new BlockPos(13, 1, 14));
        clickLayer(helper, player, new BlockPos(13, 1, 4));
        helper.assertTrue(railsLink(helper, new BlockPos(3, 1, 4), new BlockPos(13, 1, 14)), "the new line did not join the old one");
        helper.succeed();
    }

    /** A branch that meets a through line must not break it. */
    @GameTest(structure = TestWorld.ARENA, maxTicks = 20)
    public void aBranchLeavesTheThroughLineWhole(GameTestHelper helper) {
        TestWorld.floor(helper, Blocks.STONE);
        layStraightRails(helper, 3, 13, 4);
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        clickLayer(helper, player, new BlockPos(8, 1, 14));
        clickLayer(helper, player, new BlockPos(8, 1, 4));
        helper.assertTrue(railsLink(helper, new BlockPos(3, 1, 4), new BlockPos(13, 1, 4)), "the through line was cut");
        helper.assertTrue(railsLink(helper, new BlockPos(8, 1, 14), new BlockPos(8, 1, 5)), "the branch was not laid");
        helper.succeed();
    }

    /** Rails beside the line keep their own shape while it goes past, even the free end of a line. */
    @GameTest(structure = TestWorld.ARENA, maxTicks = 20)
    public void aLineLeavesNeighbouringTrackAlone(GameTestHelper helper) {
        TestWorld.floor(helper, Blocks.STONE);
        layStraightRails(helper, 3, 13, 4);
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        clickLayer(helper, player, new BlockPos(3, 1, 5));
        clickLayer(helper, player, new BlockPos(13, 1, 5));
        helper.assertTrue(railsLink(helper, new BlockPos(3, 1, 4), new BlockPos(13, 1, 4)), "the old line was changed");
        helper.assertTrue(railsLink(helper, new BlockPos(3, 1, 5), new BlockPos(13, 1, 5)), "the new line was not laid");
        helper.assertBlockProperty(new BlockPos(3, 1, 4), BlockStateProperties.RAIL_SHAPE, RailShape.EAST_WEST);
        helper.assertBlockProperty(new BlockPos(13, 1, 4), BlockStateProperties.RAIL_SHAPE, RailShape.EAST_WEST);
        helper.succeed();
    }

    /** No rail can carry two lines across each other, so the layer refuses rather than cut the old one. */
    @GameTest(structure = TestWorld.ARENA, maxTicks = 20)
    public void aLineWillNotCutAcrossOldTrack(GameTestHelper helper) {
        TestWorld.floor(helper, Blocks.STONE);
        layStraightRails(helper, 3, 13, 4);
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        clickLayer(helper, player, new BlockPos(8, 1, 1));
        clickLayer(helper, player, new BlockPos(8, 1, 10));
        helper.assertTrue(railsLink(helper, new BlockPos(3, 1, 4), new BlockPos(13, 1, 4)), "the old line was cut");
        helper.assertBlockNotPresent(Blocks.RAIL, new BlockPos(8, 1, 2));
        helper.succeed();
    }

    /** A click back along the line lays nothing and leaves the line ready to carry on. */
    @GameTest(structure = TestWorld.ARENA, maxTicks = 20)
    public void aLineWillNotDoubleBack(GameTestHelper helper) {
        TestWorld.floor(helper, Blocks.STONE);
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        clickLayer(helper, player, new BlockPos(3, 1, 4));
        clickLayer(helper, player, new BlockPos(13, 1, 4));
        clickLayer(helper, player, new BlockPos(6, 1, 4));
        helper.assertBlockProperty(new BlockPos(13, 1, 4), BlockStateProperties.RAIL_SHAPE, RailShape.EAST_WEST);
        clickLayer(helper, player, new BlockPos(13, 1, 10));
        helper.assertTrue(railsLink(helper, new BlockPos(3, 1, 4), new BlockPos(13, 1, 10)), "the line did not carry on");
        helper.succeed();
    }

    /** With no room to go on, an L runs its short leg first rather than back down the line. */
    @GameTest(structure = TestWorld.ARENA, maxTicks = 20)
    public void aCornerTurnsAwayFromTheLineBehindIt(GameTestHelper helper) {
        TestWorld.floor(helper, Blocks.STONE);
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        ModContent.TRACK_LAYER_ITEM.use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
        clickLayer(helper, player, new BlockPos(13, 1, 4));
        clickLayer(helper, player, new BlockPos(23, 1, 4));
        clickLayer(helper, player, new BlockPos(13, 1, 8));
        helper.assertTrue(railsLink(helper, new BlockPos(13, 1, 4), new BlockPos(13, 1, 8)), "the line did not come round");
        helper.succeed();
    }

    /** Two points out of line join on the diagonal, a staircase of bends, and a cart can ride it. */
    @GameTest(structure = TestWorld.ARENA, maxTicks = 300)
    public void aDiagonalLineCarriesACart(GameTestHelper helper) {
        TestWorld.floor(helper, Blocks.STONE);
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        BlockPos first = new BlockPos(3, 1, 4);
        BlockPos last = new BlockPos(21, 1, 22);
        clickLayer(helper, player, first);
        clickLayer(helper, player, last);
        helper.assertTrue(railsLink(helper, first, last), "the diagonal does not connect");
        // Nothing runs straight along either edge: the line has to have gone across.
        helper.assertBlockNotPresent(Blocks.RAIL, new BlockPos(21, 1, 4));
        helper.assertBlockNotPresent(Blocks.RAIL, new BlockPos(3, 1, 22));
        assertCartRides(helper, first, RailShape.EAST_WEST, last);
    }

    /** What the diagonal cannot cover runs straight, and joins on without a break. */
    @GameTest(structure = TestWorld.ARENA, maxTicks = 300)
    public void aLongerLineFinishesOnTheDiagonal(GameTestHelper helper) {
        TestWorld.floor(helper, Blocks.STONE);
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        BlockPos first = new BlockPos(3, 1, 4);
        BlockPos last = new BlockPos(27, 1, 12);
        clickLayer(helper, player, first);
        clickLayer(helper, player, last);
        helper.assertTrue(railsLink(helper, first, last), "the line does not connect");
        helper.assertBlockPresent(Blocks.RAIL, new BlockPos(15, 1, 4));
        assertCartRides(helper, first, RailShape.EAST_WEST, last);
    }

    /** A climb needs straight rails, so the stairs get wider just where it asks for them. */
    @GameTest(structure = TestWorld.ARENA, maxTicks = 300)
    public void aDiagonalLineClimbs(GameTestHelper helper) {
        TestWorld.floor(helper, Blocks.STONE);
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        BlockPos first = new BlockPos(3, 1, 4);
        BlockPos last = new BlockPos(19, 4, 20);
        clickLayer(helper, player, first);
        clickLayer(helper, player, last);
        helper.assertTrue(railsLink(helper, first, last), "the climb does not connect");
        assertCartRides(helper, first, RailShape.EAST_WEST, last);
    }

    /** A coupled train takes the stairs together, without stretching or leaving the rails. */
    @GameTest(structure = TestWorld.ARENA, maxTicks = 200)
    public void aCoupledTrainRidesTheDiagonal(GameTestHelper helper) {
        TestWorld.floor(helper, Blocks.STONE);
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        BlockPos last = new BlockPos(27, 1, 16);
        clickLayer(helper, player, new BlockPos(3, 1, 4));
        clickLayer(helper, player, last);
        BlockPos driveRail = new BlockPos(9, 1, 4);
        TestWorld.rail(helper, driveRail, ModContent.DRIVE_RAIL, RailShape.EAST_WEST);
        helper.setBlock(driveRail.north(), TestContent.SIGNAL);

        RedstoneTramEntity lead = helper.spawn(ModEntityTypes.REDSTONE_TRAM, new BlockPos(7, 1, 4));
        RedstoneTramEntity tail = helper.spawn(ModEntityTypes.REDSTONE_TRAM, new BlockPos(4, 1, 4));
        link(lead, tail);
        lead.setDeltaMovement(0.1D, 0.0D, 0.0D);

        helper.succeedWhen(() -> {
            helper.assertTrue(helper.relativeVec(tail.position()).z > 10.0D, "the train has not reached the stairs");
            helper.assertTrue(tail.isOnRails(), "the rear car left the rails");
            helper.assertTrue(lead.distanceTo(tail) < 5.0F, "train stretched: " + lead.distanceTo(tail));
        });
    }

    /** Motor IV speed, on rails that bend at every step. */
    @GameTest(structure = TestWorld.ARENA, maxTicks = 100)
    public void tramHoldsMotorSpeedOnTheDiagonal(GameTestHelper helper) {
        TestWorld.floor(helper, Blocks.STONE);
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        clickLayer(helper, player, new BlockPos(3, 1, 4));
        clickLayer(helper, player, new BlockPos(27, 1, 28));

        RedstoneTramEntity tram = helper.spawn(ModEntityTypes.REDSTONE_TRAM, new BlockPos(3, 1, 4));
        float speed = (float) RailDrive.MAX_SPEED;
        tram.setAttached(RailDrive.DRIVE, new CartDrive(speed, speed + 1.0F, 1.0F, 0.0F, Optional.empty()));
        tram.setDeltaMovement(RailDrive.MAX_SPEED, 0.0D, 0.0D);

        helper.succeedWhen(() -> {
            Vec3 at = helper.relativeVec(tram.position());
            helper.assertTrue(at.x > 20.0D && at.z > 20.0D, "tram is at " + at);
            helper.assertTrue(tram.isOnRails(), "tram left the rails");
        });
    }

    /** Clicking the air switches to an L, and back. */
    @GameTest(structure = TestWorld.ARENA, maxTicks = 20)
    public void clickingTheAirSwitchesTheStyle(GameTestHelper helper) {
        TestWorld.floor(helper, Blocks.STONE);
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        ModContent.TRACK_LAYER_ITEM.use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
        clickLayer(helper, player, new BlockPos(3, 1, 4));
        clickLayer(helper, player, new BlockPos(13, 1, 10));
        helper.assertBlockProperty(new BlockPos(13, 1, 4), BlockStateProperties.RAIL_SHAPE, RailShape.SOUTH_WEST);
        helper.assertTrue(railsLink(helper, new BlockPos(3, 1, 4), new BlockPos(13, 1, 10)), "the L does not connect");

        ModContent.TRACK_LAYER_ITEM.use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
        clickLayer(helper, player, new BlockPos(23, 1, 16));
        helper.assertBlockNotPresent(Blocks.RAIL, new BlockPos(23, 1, 10));
        helper.assertTrue(railsLink(helper, new BlockPos(13, 1, 10), new BlockPos(23, 1, 16)), "the diagonal does not connect");
        helper.succeed();
    }

    /** The ALU train controller drives a drive rail exactly as it drives a motor. */
    @GameTest(structure = TestWorld.ARENA, maxTicks = 100)
    public void aluControllerSetsDriveRailSpeedAndDirection(GameTestHelper helper) {
        TestWorld.floor(helper, Blocks.STONE);
        for (int x = 1; x < 31; x++) {
            TestWorld.rail(helper, new BlockPos(x, 1, 10), Blocks.RAIL, RailShape.EAST_WEST);
        }
        BlockPos rail = new BlockPos(15, 1, 10);
        TestWorld.rail(helper, rail, ModContent.DRIVE_RAIL, RailShape.EAST_WEST);

        // Arrow at the rail: speed from behind, enable on its left, reverse on its right.
        BlockPos controller = rail.north();
        helper.setBlock(controller, ModContent.ALU_TRAIN_CONTROLLER.defaultBlockState()
                .setValue(AluTrainControllerBlock.FACING, Direction.SOUTH));
        helper.setBlock(controller.north(), TestContent.SIGNAL);
        helper.setBlock(controller.east(), TestContent.SIGNAL);
        helper.setBlock(controller.west(), TestContent.SIGNAL);

        AbstractMinecart cart = helper.spawn(EntityTypes.MINECART, rail);
        helper.succeedWhen(() -> helper.assertTrue(helper.relativeVec(cart.position()).x < 12.0D,
                "reverse channel did not send the cart westward: " + helper.relativeVec(cart.position())));
    }

    /** Runs only with analog-alu in libs/: a real Copper Wire must power a drive rail. */
    @GameTest(structure = TestWorld.ARENA, maxTicks = 60)
    public void realAluCopperWireDrivesARail(GameTestHelper helper) {
        if (!FabricLoader.getInstance().isModLoaded("analog_alu")) {
            helper.succeed();
            return;
        }
        Block copperWire = BuiltInRegistries.BLOCK.getValue(Identifier.fromNamespaceAndPath("analog_alu", "copper_wire"));
        helper.assertTrue(copperWire != Blocks.AIR, "copper wire is not registered");

        TestWorld.floor(helper, Blocks.STONE);
        BlockPos rail = new BlockPos(15, 1, 10);
        TestWorld.rail(helper, rail, ModContent.DRIVE_RAIL, RailShape.EAST_WEST);
        helper.setBlock(rail.north(), copperWire);
        helper.setBlock(rail.north().north(), TestContent.SIGNAL);

        helper.succeedWhen(() -> helper.assertBlockProperty(rail, DriveRailBlock.POWERED, true));
    }

    /** Runs only with analog-alu 0.1.2+ in libs/: the new logic gates must drive rails and count as ALU parts. */
    @GameTest(structure = TestWorld.ARENA, maxTicks = 80)
    public void realAluLogicGateDrivesARail(GameTestHelper helper) {
        Block gate = BuiltInRegistries.BLOCK.getValue(Identifier.fromNamespaceAndPath("analog_alu", "logical_and"));
        if (gate == Blocks.AIR) {
            helper.succeed();
            return;
        }
        helper.assertTrue(AnalogAluSignalAdapter.isAnalogAluComponent(gate.defaultBlockState()), "logical_and is not seen as an ALU part");

        TestWorld.floor(helper, Blocks.STONE);
        BlockPos rail = new BlockPos(15, 1, 10);
        TestWorld.rail(helper, rail, ModContent.DRIVE_RAIL, RailShape.EAST_WEST);

        // The gate's arrow faces the rail; AND needs both side inputs.
        BlockPos gatePos = rail.north();
        BlockState state = gate.defaultBlockState();
        @SuppressWarnings("unchecked")
        EnumProperty<Direction> facing = (EnumProperty<Direction>) gate.getStateDefinition().getProperty("facing");
        helper.setBlock(gatePos, state.setValue(facing, Direction.SOUTH));
        helper.setBlock(gatePos.east(), TestContent.SIGNAL);

        helper.runAtTickTime(10, () -> {
            helper.assertBlockProperty(rail, DriveRailBlock.POWERED, false);
            helper.setBlock(gatePos.west(), TestContent.SIGNAL);
        });
        helper.runAtTickTime(40, () -> {
            helper.assertBlockProperty(rail, DriveRailBlock.POWERED, true);
            helper.succeed();
        });
    }

    /** Runs only with analog-alu in libs/: wire signal must run a machine, directly and through a controller. */
    @GameTest(structure = TestWorld.ARENA, maxTicks = 80)
    public void realAluCopperWireRunsMachines(GameTestHelper helper) {
        Block copperWire = BuiltInRegistries.BLOCK.getValue(Identifier.fromNamespaceAndPath("analog_alu", "copper_wire"));
        if (copperWire == Blocks.AIR) {
            helper.succeed();
            return;
        }
        TestWorld.floor(helper, Blocks.STONE);
        // The tree cutter draws no power, so a control signal alone runs it; the others need the controller's volts too.
        BlockPos direct = new BlockPos(5, 1, 5);
        helper.setBlock(direct, ModContent.MACHINES.get(MachineType.TREE_CUTTER));
        helper.setBlock(direct.west(), copperWire);
        helper.setBlock(direct.west().west(), TestContent.SIGNAL);

        BlockPos linked = new BlockPos(5, 1, 10);
        BlockPos controller = linked.east();
        helper.setBlock(linked, ModContent.MACHINES.get(MachineType.ITEM_COLLECTOR));
        TestWorld.poweredController(helper, controller, FactoryControllerBlock.Mode.SIGNAL);
        helper.setBlock(controller.east(), copperWire);
        helper.setBlock(controller.east().east(), TestContent.SIGNAL);

        helper.succeedWhen(() -> {
            helper.assertBlockProperty(direct, MachineBlock.STATUS, MachineStatus.READY);
            helper.assertBlockProperty(linked, MachineBlock.STATUS, MachineStatus.READY);
        });
    }

    static void link(AbstractMinecart first, AbstractMinecart second) {
        TrainLinkManager.addLink(first, second.getUUID());
        TrainLinkManager.addLink(second, first.getUUID());
    }
}
