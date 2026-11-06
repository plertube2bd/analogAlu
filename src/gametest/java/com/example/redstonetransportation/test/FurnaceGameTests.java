package com.example.redstonetransportation.test;

import com.example.redstonetransportation.content.ModContent;
import com.example.redstonetransportation.furnace.UpgradedFurnaceBlock;
import com.example.redstonetransportation.furnace.UpgradedFurnaceBlockEntity;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

public final class FurnaceGameTests {
    private static Container furnace(GameTestHelper helper, BlockPos pos, Block block, ItemStack input, ItemStack fuel) {
        helper.setBlock(pos, block);
        Container furnace = helper.getBlockEntity(pos, UpgradedFurnaceBlockEntity.class);
        furnace.setItem(0, input);
        furnace.setItem(1, fuel);
        return furnace;
    }

    /** A vanilla furnace takes 200 ticks per item: tier I takes 100, tier II 50, tier III 25. */
    @GameTest(structure = TestWorld.ARENA, maxTicks = 120)
    public void tiersCookAtTheirOwnSpeed(GameTestHelper helper) {
        TestWorld.floor(helper, Blocks.STONE);
        Container first = furnace(helper, new BlockPos(5, 1, 5), ModContent.UPGRADED_FURNACES.get(0),
                new ItemStack(Items.RAW_IRON, 16), new ItemStack(Items.COAL, 8));
        Container second = furnace(helper, new BlockPos(7, 1, 5), ModContent.UPGRADED_FURNACES.get(1),
                new ItemStack(Items.RAW_IRON, 16), new ItemStack(Items.COAL, 8));
        Container third = furnace(helper, new BlockPos(9, 1, 5), ModContent.UPGRADED_FURNACES.get(2),
                new ItemStack(Items.RAW_IRON, 16), new ItemStack(Items.COAL, 8));

        helper.runAtTickTime(102, () -> {
            helper.assertTrue(first.getItem(2).getCount() == 1, "tier I made " + first.getItem(2).getCount());
            helper.assertTrue(second.getItem(2).getCount() == 2, "tier II made " + second.getItem(2).getCount());
            helper.assertTrue(third.getItem(2).getCount() == 4, "tier III made " + third.getItem(2).getCount());
            helper.assertTrue(third.getItem(2).is(Items.IRON_INGOT), "tier III made the wrong thing");
            helper.succeed();
        });
    }

    /** One coal smelts 8 items in a vanilla furnace; tier III stretches that to 24. */
    @GameTest(structure = TestWorld.ARENA, maxTicks = 700)
    public void betterTiersBurnLessFuelPerItem(GameTestHelper helper) {
        TestWorld.floor(helper, Blocks.STONE);
        Container furnace = furnace(helper, new BlockPos(5, 1, 5), ModContent.UPGRADED_FURNACES.get(2),
                new ItemStack(Items.RAW_IRON, 24), new ItemStack(Items.COAL, 2));
        helper.runAtTickTime(610, () -> {
            helper.assertTrue(furnace.getItem(2).getCount() == 24, "made only " + furnace.getItem(2).getCount());
            helper.assertTrue(furnace.getItem(1).getCount() == 1, "one coal did not last for 24 items");
            helper.succeed();
        });
    }

    /** Blast furnaces and smokers keep to their own recipes, as the vanilla blocks do. */
    @GameTest(structure = TestWorld.ARENA, maxTicks = 100)
    public void blastFurnaceAndSmokerKeepTheirRecipes(GameTestHelper helper) {
        TestWorld.floor(helper, Blocks.STONE);
        Container blast = furnace(helper, new BlockPos(5, 1, 5), ModContent.UPGRADED_FURNACES.get(5),
                new ItemStack(Items.RAW_IRON), new ItemStack(Items.COAL));
        Container smoker = furnace(helper, new BlockPos(7, 1, 5), ModContent.UPGRADED_FURNACES.get(8),
                new ItemStack(Items.BEEF), new ItemStack(Items.COAL));
        Container wrong = furnace(helper, new BlockPos(9, 1, 5), ModContent.UPGRADED_FURNACES.get(5),
                new ItemStack(Items.BEEF), new ItemStack(Items.COAL));
        helper.runAtTickTime(30, () -> {
            helper.assertTrue(blast.getItem(2).is(Items.IRON_INGOT), "the blast furnace did not smelt");
            helper.assertTrue(smoker.getItem(2).is(Items.COOKED_BEEF), "the smoker did not cook");
            helper.assertTrue(wrong.getItem(0).is(Items.BEEF) && wrong.getItem(2).isEmpty(), "the blast furnace cooked food");
            helper.succeed();
        });
    }

    /** A signal pauses the work, and it carries on where it left off. */
    @GameTest(structure = TestWorld.ARENA, maxTicks = 100)
    public void signalPausesTheFurnace(GameTestHelper helper) {
        TestWorld.floor(helper, Blocks.STONE);
        BlockPos pos = new BlockPos(5, 1, 5);
        Container furnace = furnace(helper, pos, ModContent.UPGRADED_FURNACES.get(2), new ItemStack(Items.RAW_IRON, 8), new ItemStack(Items.COAL));
        helper.setBlock(pos.north(), Blocks.REDSTONE_BLOCK);
        helper.runAtTickTime(40, () -> {
            helper.assertBlockProperty(pos, UpgradedFurnaceBlock.POWERED, true);
            helper.assertTrue(furnace.getItem(2).isEmpty(), "a powered furnace kept cooking");
            helper.setBlock(pos.north(), Blocks.AIR);
        });
        helper.runAtTickTime(70, () -> {
            helper.assertTrue(!furnace.getItem(2).isEmpty(), "the furnace did not start again");
            helper.succeed();
        });
    }

    /** Hoppers feed it and empty it like the vanilla blocks, which is what lets it join a factory. */
    @GameTest(structure = TestWorld.ARENA, maxTicks = 200)
    public void hoppersFeedAndEmptyIt(GameTestHelper helper) {
        TestWorld.floor(helper, Blocks.STONE);
        BlockPos pos = new BlockPos(5, 2, 5);
        helper.setBlock(pos.above(), Blocks.HOPPER.defaultBlockState());
        helper.setBlock(pos.below(), Blocks.HOPPER.defaultBlockState());
        Container feed = helper.getBlockEntity(pos.above(), net.minecraft.world.level.block.entity.HopperBlockEntity.class);
        feed.setItem(0, new ItemStack(Items.RAW_IRON, 4));
        furnace(helper, pos, ModContent.UPGRADED_FURNACES.get(2), ItemStack.EMPTY, new ItemStack(Items.COAL));
        Container output = helper.getBlockEntity(pos.below(), net.minecraft.world.level.block.entity.HopperBlockEntity.class);

        helper.succeedWhen(() -> {
            int made = 0;
            for (int slot = 0; slot < output.getContainerSize(); slot++) {
                if (output.getItem(slot).is(Items.IRON_INGOT)) made += output.getItem(slot).getCount();
            }
            helper.assertTrue(made == 4, "only " + made + " ingots came out");
        });
    }

    /** Breaking one hands back what was in it. */
    @GameTest(structure = TestWorld.ARENA, maxTicks = 40)
    public void breakingItDropsItsContents(GameTestHelper helper) {
        TestWorld.floor(helper, Blocks.STONE);
        BlockPos pos = new BlockPos(5, 1, 5);
        furnace(helper, pos, ModContent.UPGRADED_FURNACES.get(0), new ItemStack(Items.RAW_IRON, 3), ItemStack.EMPTY);
        helper.getLevel().destroyBlock(helper.absolutePos(pos), true);
        helper.runAtTickTime(2, () -> {
            helper.assertItemEntityCountIs(Items.RAW_IRON, pos, 2.0D, 3);
            helper.succeed();
        });
    }
}
