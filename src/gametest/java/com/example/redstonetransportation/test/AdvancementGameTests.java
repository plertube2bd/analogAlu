package com.example.redstonetransportation.test;

import com.example.redstonetransportation.RedstoneTransportation;
import com.example.redstonetransportation.block.RailMotorBlock;
import com.example.redstonetransportation.block.StructureCoreBlock;
import com.example.redstonetransportation.content.ModAdvancements;
import com.example.redstonetransportation.content.ModContent;
import com.example.redstonetransportation.machine.MachineBlock;
import com.example.redstonetransportation.machine.MachineType;
import com.example.redstonetransportation.structure.MovingStructureEntity;
import com.example.redstonetransportation.structure.StructureActions;
import com.example.redstonetransportation.vehicle.TrainLinkManager;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.vehicle.minecart.AbstractMinecart;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.RailShape;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public final class AdvancementGameTests {
    private static final List<String> EVENT_ADVANCEMENTS = List.of(
            ModAdvancements.TRAIN, ModAdvancements.FULL_SPEED, ModAdvancements.FACTORY, ModAdvancements.STRUCTURE,
            ModAdvancements.ELEVATOR, ModAdvancements.SHIP, ModAdvancements.AIRSHIP, ModAdvancements.ROUTE,
            ModAdvancements.REPAIRS, ModAdvancements.GIANT);

    private static boolean done(GameTestHelper helper, ServerPlayer player, String path) {
        AdvancementHolder advancement = holder(helper, path);
        return player.getAdvancements().getOrStartProgress(advancement).isDone();
    }

    private static AdvancementHolder holder(GameTestHelper helper, String path) {
        Identifier id = Identifier.fromNamespaceAndPath(RedstoneTransportation.MOD_ID, path);
        AdvancementHolder advancement = helper.getLevel().getServer().getAdvancements().get(id);
        helper.assertTrue(advancement != null, "no advancement " + id);
        return advancement;
    }

    private static void click(GameTestHelper helper, ServerPlayer player, BlockPos pos) {
        BlockPos absolute = helper.absolutePos(pos);
        helper.getLevel().getBlockState(absolute).useWithoutItem(helper.getLevel(), player,
                new BlockHitResult(Vec3.atCenterOf(absolute), Direction.UP, absolute, false));
    }

    private static void core(GameTestHelper helper, BlockPos pos, Block block) {
        helper.setBlock(pos, block.defaultBlockState().setValue(StructureCoreBlock.FACING, Direction.NORTH));
    }

    /** Every name the code grants has to exist, or the grant fails without a word. */
    @GameTest(structure = TestWorld.ARENA, maxTicks = 20)
    public void everyEventAdvancementExists(GameTestHelper helper) {
        for (String path : EVENT_ADVANCEMENTS) holder(helper, path);
        boolean alu = helper.getLevel().getServer().getAdvancements()
                .get(Identifier.fromNamespaceAndPath(RedstoneTransportation.MOD_ID, "rail/alu")) != null;
        helper.assertTrue(alu == FabricLoader.getInstance().isModLoaded("analog_alu"), "the ALU advancement must exist exactly when ALU does");
        helper.succeed();
    }

    /** A real click on each core, the way the block itself hands it out. */
    @GameTest(structure = TestWorld.ARENA, maxTicks = 20)
    public void assemblingEarnsItsKind(GameTestHelper helper) {
        TestWorld.floor(helper, Blocks.STONE);
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        BlockPos elevator = new BlockPos(5, 2, 5);
        core(helper, elevator, ModContent.ELEVATOR_CORE);
        click(helper, player, elevator);
        helper.assertTrue(done(helper, player, ModAdvancements.STRUCTURE), "assembling earned nothing");
        helper.assertTrue(done(helper, player, ModAdvancements.ELEVATOR), "the elevator advancement was not earned");
        helper.assertTrue(!done(helper, player, ModAdvancements.SHIP), "an elevator earned the ship advancement");

        BlockPos airship = new BlockPos(15, 2, 15);
        core(helper, airship, ModContent.AIRSHIP_HELM);
        helper.setBlock(airship.above(), ModContent.LIFT_BALLOON);
        click(helper, player, airship);
        helper.assertTrue(done(helper, player, ModAdvancements.AIRSHIP), "the airship advancement was not earned");
        helper.assertTrue(!done(helper, player, ModAdvancements.GIANT), "two blocks earned the giant advancement");
        helper.succeed();
    }

    /** 32 by 16 blocks and a core: just over the mark. */
    @GameTest(structure = TestWorld.ARENA, maxTicks = 20)
    public void aHugeStructureEarnsTheGiant(GameTestHelper helper) {
        TestWorld.floor(helper, Blocks.STONE);
        for (int x = 0; x < 32; x++) {
            for (int z = 0; z < 16; z++) helper.setBlock(new BlockPos(x, 2, z), Blocks.OAK_PLANKS);
        }
        BlockPos pos = new BlockPos(5, 3, 5);
        core(helper, pos, ModContent.ELEVATOR_CORE);
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        click(helper, player, pos);
        helper.assertTrue(done(helper, player, ModAdvancements.GIANT), "513 blocks did not earn the giant advancement");
        helper.succeed();
    }

    @GameTest(structure = TestWorld.ARENA, maxTicks = 20)
    public void aFailedAssemblyEarnsNothing(GameTestHelper helper) {
        TestWorld.floor(helper, Blocks.STONE);
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        BlockPos ship = new BlockPos(5, 2, 5);
        core(helper, ship, ModContent.SHIP_HELM);
        click(helper, player, ship);
        helper.assertTrue(!done(helper, player, ModAdvancements.STRUCTURE), "a refused structure counted");
        helper.succeed();
    }

    @GameTest(structure = TestWorld.ARENA, maxTicks = 20)
    public void placingABlockAboardEarnsRepairs(GameTestHelper helper) {
        TestWorld.floor(helper, Blocks.STONE);
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        BlockPos pos = new BlockPos(5, 2, 5);
        core(helper, pos, ModContent.ELEVATOR_CORE);
        helper.setBlock(pos.east(), Blocks.OAK_PLANKS);
        click(helper, player, pos);
        MovingStructureEntity structure = helper.getLevel().getEntitiesOfClass(MovingStructureEntity.class, new net.minecraft.world.phys.AABB(helper.absolutePos(pos)).inflate(2.0D)).getFirst();
        player.setPos(structure.position().add(0.0D, 1.0D, 0.0D));
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.BRICKS));
        StructureActions.use(player, structure, new BlockPos(1, 0, 0), Direction.UP, InteractionHand.MAIN_HAND);
        helper.assertTrue(done(helper, player, ModAdvancements.REPAIRS), "placing a block aboard earned nothing");
        helper.succeed();
    }

    /** Whoever is near a vessel when it docks earns the route, and it does not take a click on the vessel. */
    @GameTest(structure = TestWorld.ARENA, maxTicks = 300)
    public void dockingEarnsTheRoute(GameTestHelper helper) {
        TestWorld.floor(helper, Blocks.STONE);
        for (int x = 0; x < TestWorld.SIZE; x++) {
            for (int z = 0; z < TestWorld.SIZE; z++) helper.setBlock(new BlockPos(x, 1, z), Blocks.WATER);
        }
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.setPos(helper.absoluteVec(new Vec3(10.5D, 3.0D, 10.5D)));
        BlockPos helm = new BlockPos(5, 2, 15);
        core(helper, helm, ModContent.SHIP_HELM);
        click(helper, player, helm);
        helper.assertTrue(done(helper, player, ModAdvancements.SHIP), "the ship advancement was not earned");
        BlockPos beacon = new BlockPos(14, 1, 15);
        helper.setBlock(beacon, ModContent.ROUTE_BEACON);
        helper.runAtTickTime(1, () -> MovingStructureEntity.sendTo(helper.getLevel(), helper.absolutePos(beacon), 1.0D));
        helper.succeedWhen(() -> helper.assertTrue(done(helper, player, ModAdvancements.ROUTE), "docking earned nothing"));
    }

    @GameTest(structure = TestWorld.ARENA, maxTicks = 40)
    public void couplingEarnsTheTrain(GameTestHelper helper) {
        TestWorld.floor(helper, Blocks.STONE);
        for (int x = 2; x < 12; x++) {
            TestWorld.rail(helper, new BlockPos(x, 1, 5), Blocks.RAIL, RailShape.EAST_WEST);
        }
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        AbstractMinecart first = helper.spawn(EntityTypes.MINECART, new BlockPos(4, 1, 5));
        AbstractMinecart second = helper.spawn(EntityTypes.MINECART, new BlockPos(6, 1, 5));
        TrainLinkManager.handleCouplerUse(player, first, helper.getLevel());
        helper.assertTrue(!done(helper, player, ModAdvancements.TRAIN), "selecting one cart was enough");
        TrainLinkManager.handleCouplerUse(player, second, helper.getLevel());
        helper.assertTrue(done(helper, player, ModAdvancements.TRAIN), "coupling earned nothing");
        helper.succeed();
    }

    /** Two machines are a pair; three are a factory. */
    @GameTest(structure = TestWorld.ARENA, maxTicks = 20)
    public void threeMachinesEarnTheFactory(GameTestHelper helper) {
        TestWorld.floor(helper, Blocks.STONE);
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        BlockPos controller = new BlockPos(5, 1, 5);
        helper.setBlock(controller, ModContent.FACTORY_CONTROLLER);
        helper.setBlock(controller.east(), ModContent.MACHINES.get(MachineType.ITEM_COLLECTOR).defaultBlockState().setValue(MachineBlock.FACING, Direction.NORTH));
        helper.setBlock(controller.east(2), ModContent.MACHINES.get(MachineType.ITEM_COLLECTOR).defaultBlockState().setValue(MachineBlock.FACING, Direction.NORTH));
        click(helper, player, controller);
        helper.assertTrue(!done(helper, player, ModAdvancements.FACTORY), "two machines were enough");
        helper.setBlock(controller.east(3), ModContent.MACHINES.get(MachineType.ITEM_COLLECTOR).defaultBlockState().setValue(MachineBlock.FACING, Direction.NORTH));
        click(helper, player, controller);
        helper.assertTrue(done(helper, player, ModAdvancements.FACTORY), "three machines earned nothing");
        helper.succeed();
    }

    /** Only a rider counts, and only at the top of Motor IV's range. */
    @GameTest(structure = TestWorld.ARENA, maxTicks = 40)
    public void motorTopSpeedEarnsFullSpeed(GameTestHelper helper) {
        TestWorld.floor(helper, Blocks.STONE);
        BlockPos railAt = new BlockPos(10, 1, 10);
        TestWorld.rail(helper, railAt, Blocks.RAIL, RailShape.EAST_WEST);
        helper.setBlock(railAt.below(), ModContent.REDSTONE_MOTOR_4.defaultBlockState().setValue(RailMotorBlock.FACING, Direction.EAST));
        helper.setBlock(railAt.below().north(), TestContent.SIGNAL);
        AbstractMinecart cart = helper.spawn(EntityTypes.MINECART, railAt);
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.startRiding(cart);
        Vec3 place = cart.position();
        // Held on the motor at the speed a long run would reach, since the arena has no room for one.
        helper.onEachTick(() -> {
            cart.setPos(place);
            cart.setDeltaMovement(5.1D, 0.0D, 0.0D);
        });
        helper.succeedWhen(() -> helper.assertTrue(done(helper, player, ModAdvancements.FULL_SPEED), "top speed earned nothing"));
    }
}
