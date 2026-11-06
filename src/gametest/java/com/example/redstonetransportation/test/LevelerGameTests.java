package com.example.redstonetransportation.test;

import com.example.redstonetransportation.content.ModContent;
import com.example.redstonetransportation.machine.LandLevelerBlockEntity;
import com.example.redstonetransportation.machine.LandLevelerMenu;
import com.example.redstonetransportation.machine.MachineBlock;
import com.example.redstonetransportation.machine.MachineStatus;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;

/** The land leveler cutting, filling, moving and stopping in a real world. */
public final class LevelerGameTests {
    private static final int LANE_X = 15;
    private static final BlockPos START = new BlockPos(LANE_X, 1, 3);

    private static LandLevelerBlockEntity place(GameTestHelper helper, BlockPos pos) {
        helper.setBlock(pos, ModContent.LAND_LEVELER.defaultBlockState().setValue(MachineBlock.FACING, Direction.SOUTH));
        return leveler(helper, pos);
    }

    private static LandLevelerBlockEntity leveler(GameTestHelper helper, BlockPos pos) {
        return helper.getBlockEntity(pos, LandLevelerBlockEntity.class);
    }

    private static boolean isLeveler(GameTestHelper helper, BlockPos pos) {
        return helper.getBlockState(pos).is(ModContent.LAND_LEVELER);
    }

    private static int count(LandLevelerBlockEntity leveler, Item item) {
        int total = 0;
        for (int slot = 1; slot < LandLevelerBlockEntity.SLOTS; slot++) {
            ItemStack stack = leveler.getItem(slot);
            if (stack.is(item)) total += stack.getCount();
        }
        return total;
    }

    /** A chest across the lane: the leveler stops in front of it, so it never leaves the arena. */
    private static void stopAt(GameTestHelper helper, int z) {
        helper.setBlock(new BlockPos(LANE_X, 1, z), Blocks.CHEST);
    }

    private static void start(LandLevelerBlockEntity leveler) {
        leveler.setItem(LandLevelerBlockEntity.FUEL_SLOT, new ItemStack(Items.COAL));
        if (!leveler.isRunning()) leveler.press(LandLevelerBlockEntity.BUTTON_RUN);
    }

    /** It cuts a hill away to its own level, fills the holes under it and keeps what it carries. */
    @GameTest(structure = TestWorld.ARENA, maxTicks = 420)
    public void levelsAHillAndFillsItsHoles(GameTestHelper helper) {
        TestWorld.floor(helper, Blocks.STONE);
        for (int x = LANE_X - 4; x <= LANE_X + 4; x++) {
            for (int z = 7; z <= 9; z++) {
                for (int y = 1; y <= 4; y++) helper.setBlock(new BlockPos(x, y, z), y == 4 ? Blocks.DIRT : Blocks.STONE);
            }
        }
        for (int y = 5; y <= 7; y++) helper.setBlock(new BlockPos(LANE_X - 2, y, 8), Blocks.OAK_LOG);
        BlockPos leftHole = new BlockPos(LANE_X - 3, 0, 11);
        BlockPos rightHole = new BlockPos(LANE_X + 3, 0, 11);
        helper.setBlock(leftHole, Blocks.AIR);
        helper.setBlock(rightHole, Blocks.AIR);
        stopAt(helper, 15);

        LandLevelerBlockEntity leveler = place(helper, START);
        leveler.setItem(5, new ItemStack(Items.DIAMOND, 10));
        start(leveler);

        BlockPos end = new BlockPos(LANE_X, 1, 14);
        helper.succeedWhen(() -> {
            helper.assertTrue(isLeveler(helper, end), "the leveler did not reach the chest");
            LandLevelerBlockEntity there = leveler(helper, end);
            helper.assertValueEqual(there.problem(), LandLevelerBlockEntity.Problem.OBSTACLE, "why it stopped");
            helper.assertBlockState(end, state -> state.getValue(MachineBlock.STATUS) == MachineStatus.BLOCKED, state -> Component.literal("a stopped leveler shows red"));
            for (int x = LANE_X - 4; x <= LANE_X + 4; x++) {
                for (int z = 7; z <= 9; z++) {
                    for (int y = 1; y <= 7; y++) helper.assertBlockPresent(Blocks.AIR, new BlockPos(x, y, z));
                }
            }
            helper.assertFalse(helper.getBlockState(leftHole).isAir(), "the left hole was not filled");
            helper.assertFalse(helper.getBlockState(rightHole).isAir(), "the right hole was not filled");
            helper.assertBlockPresent(Blocks.CHEST, new BlockPos(LANE_X, 1, 15));
            helper.assertValueEqual(count(there, Items.DIAMOND), 10, "diamonds carried along");
            helper.assertTrue(count(there, Items.OAK_LOG) == 3, "logs kept");
            helper.assertTrue(count(there, Items.COBBLESTONE) + count(there, Items.DIRT) > 0, "rubble kept for filling");
            helper.assertTrue(helper.getEntities(EntityTypes.ITEM).isEmpty(), "something was dropped on the way");
            for (int z = START.getZ(); z < end.getZ(); z++) {
                helper.assertBlockPresent(Blocks.AIR, new BlockPos(LANE_X, 1, z));
            }
        });
    }

    @GameTest(structure = TestWorld.ARENA, maxTicks = 60)
    public void standsStillWithoutFuel(GameTestHelper helper) {
        TestWorld.floor(helper, Blocks.STONE);
        LandLevelerBlockEntity leveler = place(helper, START);
        leveler.press(LandLevelerBlockEntity.BUTTON_RUN);
        helper.runAfterDelay(30, () -> {
            helper.assertTrue(isLeveler(helper, START), "it moved with no fuel");
            helper.assertValueEqual(leveler(helper, START).problem(), LandLevelerBlockEntity.Problem.NO_FUEL, "why it stopped");
            helper.succeed();
        });
    }

    /** With everything kept, a full store stops it; dumping rubble lets it carry on without losing the rest. */
    @GameTest(structure = TestWorld.ARENA, maxTicks = 160)
    public void aFullStoreStopsItUnlessRubbleIsDumped(GameTestHelper helper) {
        TestWorld.floor(helper, Blocks.STONE);
        for (int x = LANE_X - 4; x <= LANE_X + 4; x++) helper.setBlock(new BlockPos(x, 1, 4), Blocks.STONE);
        stopAt(helper, 7);
        LandLevelerBlockEntity leveler = place(helper, START);
        for (int slot = LandLevelerBlockEntity.FIRST_STORAGE; slot < LandLevelerBlockEntity.STORAGE_END; slot++) leveler.setItem(slot, new ItemStack(Items.OAK_PLANKS, 64));
        leveler.press(LandLevelerBlockEntity.BUTTON_DISCARD);
        start(leveler);
        helper.runAfterDelay(30, () -> {
            helper.assertTrue(isLeveler(helper, START), "it moved with nowhere to put the stone");
            helper.assertValueEqual(leveler(helper, START).problem(), LandLevelerBlockEntity.Problem.FULL, "why it stopped");
            leveler(helper, START).press(LandLevelerBlockEntity.BUTTON_DISCARD);
            BlockPos end = new BlockPos(LANE_X, 1, 6);
            helper.succeedWhen(() -> {
                helper.assertTrue(isLeveler(helper, end), "dumping rubble did not let it go on");
                helper.assertValueEqual(count(leveler(helper, end), Items.OAK_PLANKS), 27 * 64, "planks kept");
                helper.assertBlockPresent(Blocks.AIR, new BlockPos(LANE_X + 4, 1, 4));
            });
        });
    }

    /** A hole it has nothing to fill with stops it, and a single cobblestone gets it going again. */
    @GameTest(structure = TestWorld.ARENA, maxTicks = 160)
    public void aHoleWithNothingToFillItWaits(GameTestHelper helper) {
        TestWorld.floor(helper, Blocks.STONE);
        BlockPos hole = new BlockPos(LANE_X + 1, 0, 4);
        helper.setBlock(hole, Blocks.AIR);
        stopAt(helper, 7);
        LandLevelerBlockEntity leveler = place(helper, START);
        start(leveler);
        helper.runAfterDelay(30, () -> {
            helper.assertTrue(isLeveler(helper, START), "it moved over a hole it could not fill");
            helper.assertValueEqual(leveler(helper, START).problem(), LandLevelerBlockEntity.Problem.NO_FILL, "why it stopped");
            leveler(helper, START).setItem(1, new ItemStack(Items.COBBLESTONE));
            helper.succeedWhen(() -> {
                helper.assertTrue(isLeveler(helper, START.south()), "the cobblestone did not get it going");
                helper.assertBlockPresent(Blocks.COBBLESTONE, hole);
            });
        });
    }

    /** The menu's own buttons start it, and the menu stays open and current while it moves along. */
    @GameTest(structure = TestWorld.ARENA, maxTicks = 120)
    public void itsMenuFollowsItAlong(GameTestHelper helper) {
        TestWorld.floor(helper, Blocks.STONE);
        stopAt(helper, 7);
        LandLevelerBlockEntity leveler = place(helper, START);
        leveler.setItem(LandLevelerBlockEntity.FUEL_SLOT, new ItemStack(Items.COAL));
        leveler.setItem(3, new ItemStack(Items.EMERALD, 5));
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        BlockPos stand = helper.absolutePos(new BlockPos(LANE_X + 2, 1, 5));
        player.snapTo(stand.getX() + 0.5D, stand.getY(), stand.getZ() + 0.5D);
        player.openMenu(leveler);
        if (!(player.containerMenu instanceof LandLevelerMenu menu)) {
            helper.fail("the leveler's menu did not open");
            return;
        }
        helper.assertTrue(menu.clickMenuButton(player, LandLevelerBlockEntity.BUTTON_RUN), "the start button did nothing");
        BlockPos end = new BlockPos(LANE_X, 1, 6);
        helper.succeedWhen(() -> {
            helper.assertTrue(isLeveler(helper, end), "it did not drive off");
            helper.assertTrue(player.containerMenu == menu && menu.stillValid(player), "the menu closed when it moved");
            helper.assertTrue(menu.getSlot(3).getItem().is(Items.EMERALD), "the open menu lost track of the storage");
            menu.broadcastChanges();
            helper.assertTrue(menu.isRunning(), "the menu does not show it running");
        });
    }
    /** A chest up on a pillar is out of its way: the pillar goes, the chest stays where it was, and so does the ceiling. */
    @GameTest(structure = TestWorld.ARENA, maxTicks = 120)
    public void whatItWillNotCutHighUpIsLeftStanding(GameTestHelper helper) {
        TestWorld.floor(helper, Blocks.STONE);
        BlockPos shelf = new BlockPos(LANE_X + 2, 4, 5);
        for (int y = 1; y < shelf.getY(); y++) helper.setBlock(new BlockPos(shelf.getX(), y, shelf.getZ()), Blocks.STONE);
        helper.setBlock(shelf, Blocks.CHEST);
        helper.setBlock(shelf.above(), Blocks.DIRT);
        stopAt(helper, 7);
        start(place(helper, START));
        BlockPos end = new BlockPos(LANE_X, 1, 6);
        helper.succeedWhen(() -> {
            helper.assertTrue(isLeveler(helper, end), "the chest overhead stopped it");
            helper.assertBlockPresent(Blocks.CHEST, shelf);
            helper.assertBlockPresent(Blocks.DIRT, shelf.above());
            for (int y = 1; y < shelf.getY(); y++) helper.assertBlockPresent(Blocks.AIR, new BlockPos(shelf.getX(), y, shelf.getZ()));
        });
    }
    /** The distances in the guide: a step costs 20 ticks of burning, so these are 80, 800, 1000 and 15 blocks. */
    @GameTest(structure = TestWorld.ARENA, maxTicks = 20)
    public void fuelLastsAsTheGuideSays(GameTestHelper helper) {
        LandLevelerBlockEntity leveler = place(helper, START);
        helper.assertValueEqual(LandLevelerBlockEntity.STEP_FUEL, 20, "burn ticks per step");
        helper.assertValueEqual(leveler.burnTime(helper.getLevel(), new ItemStack(Items.COAL)), 1600, "coal");
        helper.assertValueEqual(leveler.burnTime(helper.getLevel(), new ItemStack(Items.CHARCOAL)), 1600, "charcoal");
        helper.assertValueEqual(leveler.burnTime(helper.getLevel(), new ItemStack(Items.COAL_BLOCK)), 16000, "coal block");
        helper.assertValueEqual(leveler.burnTime(helper.getLevel(), new ItemStack(Items.LAVA_BUCKET)), 20000, "lava bucket");
        helper.assertValueEqual(leveler.burnTime(helper.getLevel(), new ItemStack(Items.OAK_LOG)), 300, "oak log");
        helper.assertValueEqual(leveler.burnTime(helper.getLevel(), new ItemStack(Items.OAK_PLANKS)), 300, "oak planks");
        helper.assertFalse(LandLevelerBlockEntity.isFuel(new ItemStack(Items.COBBLESTONE)), "cobblestone counts as fuel");
        helper.succeed();
    }
    private static void assertFloor(GameTestHelper helper, int fromZ, int toZ, net.minecraft.world.level.block.Block block) {
        for (int x = LANE_X - 4; x <= LANE_X + 4; x++) {
            for (int z = fromZ; z <= toZ; z++) helper.assertBlockPresent(block, new BlockPos(x, 0, z));
        }
    }

    private static int storageCount(LandLevelerBlockEntity leveler) {
        int total = 0;
        for (int slot = LandLevelerBlockEntity.FIRST_STORAGE; slot < LandLevelerBlockEntity.STORAGE_END; slot++) {
            total += leveler.getItem(slot).getCount();
        }
        return total;
    }

    /** A block in the fill slot paves the whole floor, holes and ground alike, and the ground it digs up is kept. */
    @GameTest(structure = TestWorld.ARENA, maxTicks = 160)
    public void theFillBlockPavesTheWholeFloor(GameTestHelper helper) {
        TestWorld.floor(helper, Blocks.STONE);
        helper.setBlock(new BlockPos(LANE_X - 1, 0, 4), Blocks.AIR);
        helper.setBlock(new BlockPos(LANE_X + 2, 0, 5), Blocks.AIR);
        stopAt(helper, 7);
        LandLevelerBlockEntity leveler = place(helper, START);
        leveler.setItem(LandLevelerBlockEntity.FILL_SLOT, new ItemStack(Items.STONE_BRICKS, 64));
        start(leveler);
        BlockPos end = new BlockPos(LANE_X, 1, 6);
        helper.succeedWhen(() -> {
            helper.assertTrue(isLeveler(helper, end), "it did not reach the chest");
            assertFloor(helper, 4, 6, Blocks.STONE_BRICKS);
            LandLevelerBlockEntity there = leveler(helper, end);
            helper.assertValueEqual(there.getItem(LandLevelerBlockEntity.FILL_SLOT).getCount(), 64 - 27, "bricks left in the fill slot");
            helper.assertValueEqual(count(there, Items.COBBLESTONE), 25, "the stone floor it dug up");
        });
    }

    /** When the fill slot runs dry it takes the same block out of storage and carries on. */
    @GameTest(structure = TestWorld.ARENA, maxTicks = 120)
    public void theFillSlotRefillsFromStorage(GameTestHelper helper) {
        TestWorld.floor(helper, Blocks.STONE);
        stopAt(helper, 6);
        LandLevelerBlockEntity leveler = place(helper, START);
        leveler.setItem(LandLevelerBlockEntity.FILL_SLOT, new ItemStack(Items.STONE_BRICKS, 5));
        leveler.setItem(3, new ItemStack(Items.STONE_BRICKS, 20));
        start(leveler);
        BlockPos end = new BlockPos(LANE_X, 1, 5);
        helper.succeedWhen(() -> {
            helper.assertTrue(isLeveler(helper, end), "it did not reach the chest");
            assertFloor(helper, 4, 5, Blocks.STONE_BRICKS);
            LandLevelerBlockEntity there = leveler(helper, end);
            helper.assertValueEqual(there.getItem(LandLevelerBlockEntity.FILL_SLOT).getCount(), 7, "bricks in the refilled slot");
            helper.assertValueEqual(count(there, Items.STONE_BRICKS) - there.getItem(LandLevelerBlockEntity.FILL_SLOT).getCount(), 0, "bricks left in storage");
        });
    }

    /** Out of paving blocks it stops rather than change road; starting it again with the slot empty goes on unpaved. */
    @GameTest(structure = TestWorld.ARENA, maxTicks = 220)
    public void runningOutOfPavingStopsItUntilRestarted(GameTestHelper helper) {
        TestWorld.floor(helper, Blocks.STONE);
        stopAt(helper, 8);
        LandLevelerBlockEntity leveler = place(helper, START);
        leveler.setItem(LandLevelerBlockEntity.FILL_SLOT, new ItemStack(Items.STONE_BRICKS, 9));
        start(leveler);
        BlockPos paved = START.south();
        helper.runAfterDelay(50, () -> {
            helper.assertTrue(isLeveler(helper, paved), "it did not stop after the last brick");
            helper.assertValueEqual(leveler(helper, paved).problem(), LandLevelerBlockEntity.Problem.PAVING_OUT, "why it stopped");
            assertFloor(helper, 4, 4, Blocks.STONE_BRICKS);
            leveler(helper, paved).press(LandLevelerBlockEntity.BUTTON_RUN);
            leveler(helper, paved).press(LandLevelerBlockEntity.BUTTON_RUN);
            BlockPos end = new BlockPos(LANE_X, 1, 7);
            helper.succeedWhen(() -> {
                helper.assertTrue(isLeveler(helper, end), "starting again did not let it go on");
                assertFloor(helper, 5, 7, Blocks.STONE);
            });
        });
    }

    /** Taking the blocks out of the fill slot by hand ends the paving, even with more of them in storage. */
    @GameTest(structure = TestWorld.ARENA, maxTicks = 120)
    public void emptyingTheFillSlotEndsPaving(GameTestHelper helper) {
        TestWorld.floor(helper, Blocks.STONE);
        stopAt(helper, 7);
        LandLevelerBlockEntity leveler = place(helper, START);
        leveler.setItem(LandLevelerBlockEntity.FILL_SLOT, new ItemStack(Items.STONE_BRICKS, 9));
        leveler.setItem(3, new ItemStack(Items.STONE_BRICKS, 20));
        leveler.removeItem(LandLevelerBlockEntity.FILL_SLOT, 9);
        start(leveler);
        BlockPos end = new BlockPos(LANE_X, 1, 6);
        helper.succeedWhen(() -> {
            helper.assertTrue(isLeveler(helper, end), "it did not reach the chest");
            assertFloor(helper, 4, 6, Blocks.STONE);
            helper.assertValueEqual(count(leveler(helper, end), Items.STONE_BRICKS), 20, "bricks in storage");
        });
    }

    /** With a lighter it keeps only the rubble reserve for holes; logs, ore and the rest burn. */
    @GameTest(structure = TestWorld.ARENA, maxTicks = 220)
    public void aLighterBurnsWhatItDigs(GameTestHelper helper) {
        TestWorld.floor(helper, Blocks.STONE);
        for (int x = LANE_X - 4; x <= LANE_X + 4; x++) {
            for (int z = 5; z <= 6; z++) {
                for (int y = 1; y <= 3; y++) helper.setBlock(new BlockPos(x, y, z), y == 3 ? Blocks.DIRT : Blocks.STONE);
            }
        }
        for (int y = 4; y <= 6; y++) helper.setBlock(new BlockPos(LANE_X + 1, y, 6), Blocks.OAK_LOG);
        helper.setBlock(new BlockPos(LANE_X + 1, 1, 6), Blocks.IRON_ORE);
        BlockPos hole = new BlockPos(LANE_X - 2, 0, 8);
        helper.setBlock(hole, Blocks.AIR);
        stopAt(helper, 10);
        LandLevelerBlockEntity leveler = place(helper, START);
        leveler.setItem(LandLevelerBlockEntity.LIGHTER_SLOT, new ItemStack(Items.FLINT_AND_STEEL));
        start(leveler);
        BlockPos end = new BlockPos(LANE_X, 1, 9);
        helper.succeedWhen(() -> {
            helper.assertTrue(isLeveler(helper, end), "it did not reach the chest");
            LandLevelerBlockEntity there = leveler(helper, end);
            int rubble = count(there, Items.COBBLESTONE) + count(there, Items.DIRT);
            helper.assertValueEqual(storageCount(there), rubble, "anything but rubble kept despite the lighter");
            helper.assertTrue(rubble <= LandLevelerBlockEntity.RUBBLE_RESERVE, "more rubble kept than the reserve");
            helper.assertFalse(helper.getBlockState(hole).isAir(), "the hole was not filled from the kept rubble");
            helper.assertBlockPresent(Blocks.AIR, new BlockPos(LANE_X + 1, 5, 6));
            ItemStack lighter = there.getItem(LandLevelerBlockEntity.LIGHTER_SLOT);
            helper.assertTrue(lighter.is(Items.FLINT_AND_STEEL) && lighter.getDamageValue() == 0, "the lighter wore out or went");
            helper.assertTrue(helper.getEntities(EntityTypes.ITEM).isEmpty(), "something was dropped on the way");
        });
    }

    /** Only a flint and steel goes in the lighter slot, one at a time. */
    @GameTest(structure = TestWorld.ARENA, maxTicks = 20)
    public void theLighterSlotTakesFlintAndSteel(GameTestHelper helper) {
        LandLevelerBlockEntity leveler = place(helper, START);
        int lighter = LandLevelerBlockEntity.LIGHTER_SLOT;
        helper.assertTrue(leveler.canPlaceItem(lighter, new ItemStack(Items.FLINT_AND_STEEL)), "flint and steel refused");
        helper.assertFalse(leveler.canPlaceItem(lighter, new ItemStack(Items.FIRE_CHARGE)), "a fire charge accepted");
        helper.assertFalse(leveler.canPlaceItem(lighter, new ItemStack(Items.COAL)), "coal accepted");
        helper.succeed();
    }

    private static ChestBlockEntity chestOnTop(GameTestHelper helper, BlockPos leveler) {
        helper.setBlock(leveler.above(), Blocks.CHEST);
        return helper.getBlockEntity(leveler.above(), ChestBlockEntity.class);
    }

    private static int chestCount(GameTestHelper helper, BlockPos leveler, Item item) {
        return helper.getBlockEntity(leveler.above(), ChestBlockEntity.class).countItem(item);
    }

    /** A chest set on top rides along with what is in it, and nothing is dropped on the way. */
    @GameTest(structure = TestWorld.ARENA, maxTicks = 120)
    public void aChestOnTopRidesAlong(GameTestHelper helper) {
        TestWorld.floor(helper, Blocks.STONE);
        stopAt(helper, 7);
        LandLevelerBlockEntity leveler = place(helper, START);
        chestOnTop(helper, START).setItem(0, new ItemStack(Items.DIAMOND, 5));
        start(leveler);
        BlockPos end = new BlockPos(LANE_X, 1, 6);
        helper.succeedWhen(() -> {
            helper.assertTrue(isLeveler(helper, end), "it did not reach the chest ahead");
            helper.assertBlockPresent(Blocks.CHEST, end.above());
            helper.assertValueEqual(chestCount(helper, end, Items.DIAMOND), 5, "diamonds in the chest on top");
            helper.assertBlockPresent(Blocks.AIR, START.above());
            helper.assertTrue(leveler(helper, end).cargo() != null, "the machine does not see the chest on top");
            helper.assertTrue(helper.getEntities(EntityTypes.ITEM).isEmpty(), "something was dropped on the way");
        });
    }

    /** With its own storage full, what it digs goes into the chest on top. */
    @GameTest(structure = TestWorld.ARENA, maxTicks = 120)
    public void aFullStoreSpillsIntoTheChestOnTop(GameTestHelper helper) {
        TestWorld.floor(helper, Blocks.STONE);
        for (int x = LANE_X - 4; x <= LANE_X + 4; x++) helper.setBlock(new BlockPos(x, 1, 4), Blocks.STONE);
        stopAt(helper, 7);
        LandLevelerBlockEntity leveler = place(helper, START);
        for (int slot = LandLevelerBlockEntity.FIRST_STORAGE; slot < LandLevelerBlockEntity.STORAGE_END; slot++) {
            leveler.setItem(slot, new ItemStack(Items.OAK_PLANKS, 64));
        }
        chestOnTop(helper, START);
        leveler.press(LandLevelerBlockEntity.BUTTON_DISCARD);
        start(leveler);
        BlockPos end = new BlockPos(LANE_X, 1, 6);
        helper.succeedWhen(() -> {
            helper.assertTrue(isLeveler(helper, end), "a full store stopped it with a chest on top");
            helper.assertValueEqual(chestCount(helper, end, Items.COBBLESTONE), 9, "cobblestone in the chest on top");
            helper.assertValueEqual(count(leveler(helper, end), Items.OAK_PLANKS), 27 * 64, "planks kept");
        });
    }

    /** When the fuel slot runs dry it takes the same fuel from storage, then from the chest on top. */
    @GameTest(structure = TestWorld.ARENA, maxTicks = 340)
    public void fuelComesFromStorageAndTheChestOnTop(GameTestHelper helper) {
        TestWorld.floor(helper, Blocks.STONE);
        stopAt(helper, 16);
        LandLevelerBlockEntity leveler = place(helper, START);
        leveler.setItem(LandLevelerBlockEntity.FUEL_SLOT, new ItemStack(Items.STICK));
        leveler.setItem(LandLevelerBlockEntity.FIRST_STORAGE + 4, new ItemStack(Items.STICK));
        chestOnTop(helper, START).setItem(10, new ItemStack(Items.STICK));
        leveler.press(LandLevelerBlockEntity.BUTTON_RUN);
        BlockPos end = new BlockPos(LANE_X, 1, 15);
        helper.succeedWhen(() -> {
            helper.assertTrue(isLeveler(helper, end), "it ran out of fuel with sticks left in storage or the chest");
            LandLevelerBlockEntity there = leveler(helper, end);
            helper.assertValueEqual(count(there, Items.STICK) + there.getItem(LandLevelerBlockEntity.FUEL_SLOT).getCount()
                    + chestCount(helper, end, Items.STICK), 0, "sticks left over");
        });
    }

    /** The paving refills from the chest on top as well. */
    @GameTest(structure = TestWorld.ARENA, maxTicks = 120)
    public void pavingRefillsFromTheChestOnTop(GameTestHelper helper) {
        TestWorld.floor(helper, Blocks.STONE);
        stopAt(helper, 7);
        LandLevelerBlockEntity leveler = place(helper, START);
        leveler.setItem(LandLevelerBlockEntity.FILL_SLOT, new ItemStack(Items.STONE_BRICKS, 9));
        chestOnTop(helper, START).setItem(5, new ItemStack(Items.STONE_BRICKS, 18));
        start(leveler);
        BlockPos end = new BlockPos(LANE_X, 1, 6);
        helper.succeedWhen(() -> {
            helper.assertTrue(isLeveler(helper, end), "it did not reach the chest ahead");
            assertFloor(helper, 4, 6, Blocks.STONE_BRICKS);
            helper.assertValueEqual(chestCount(helper, end, Items.STONE_BRICKS), 0, "bricks left in the chest on top");
        });
    }

    /** The fill slot takes whole blocks that stay put, and nothing else. */
    @GameTest(structure = TestWorld.ARENA, maxTicks = 20)
    public void theFillSlotTakesWholeBlocksOnly(GameTestHelper helper) {
        LandLevelerBlockEntity leveler = place(helper, START);
        int fill = LandLevelerBlockEntity.FILL_SLOT;
        helper.assertTrue(leveler.canPlaceItem(fill, new ItemStack(Items.STONE_BRICKS)), "stone bricks refused");
        helper.assertTrue(leveler.canPlaceItem(fill, new ItemStack(Items.GLASS)), "glass refused");
        helper.assertTrue(leveler.canPlaceItem(fill, new ItemStack(Items.GRASS_BLOCK)), "grass refused");
        helper.assertTrue(leveler.canPlaceItem(fill, new ItemStack(Items.OAK_PLANKS)), "planks refused");
        helper.assertFalse(leveler.canPlaceItem(fill, new ItemStack(Items.OAK_SLAB)), "a slab accepted");
        helper.assertFalse(leveler.canPlaceItem(fill, new ItemStack(Items.SAND)), "sand accepted");
        helper.assertFalse(leveler.canPlaceItem(fill, new ItemStack(Items.CHEST)), "a chest accepted");
        helper.assertFalse(leveler.canPlaceItem(fill, new ItemStack(Items.TORCH)), "a torch accepted");
        helper.assertFalse(leveler.canPlaceItem(fill, new ItemStack(Items.DIAMOND)), "a diamond accepted");
        helper.succeed();
    }
}
