package com.example.redstonetransportation.test;

import com.example.analogalu.ModBlocks;
import com.example.analogalu.ModItems;
import com.example.analogalu.block.CircuitDeviceBlock;
import com.example.analogalu.block.CopperCableBlockEntity;
import com.example.modernizationworld.MusicDiscs;
import com.example.redstonetransportation.integration.analogalu.ElectricPower;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.JukeboxSong;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.JukeboxBlockEntity;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

/**
 * What came with ALU 0.2.2 and after: sneakers, the two music discs, and insulated cable.
 */
public final class ModernGameTests {
    // ---- Sneakers -------------------------------------------------------------------------------

    @GameTest(structure = TestWorld.ARENA, maxTicks = 20)
    public void sneakersNeverWearOutAndTakeEnchantments(GameTestHelper helper) {
        ItemStack sneakers = new ItemStack(ModItems.SNEAKERS);
        helper.assertFalse(sneakers.has(DataComponents.MAX_DAMAGE), "sneakers still have a durability");
        helper.assertFalse(sneakers.isDamageableItem(), "sneakers can wear out");
        helper.assertTrue(sneakers.isEnchantable(), "sneakers cannot be enchanted");
        for (var key : List.of(Enchantments.FEATHER_FALLING, Enchantments.PROTECTION, Enchantments.DEPTH_STRIDER, Enchantments.SOUL_SPEED)) {
            Holder<Enchantment> enchantment = helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(key);
            helper.assertTrue(enchantment.value().canEnchant(sneakers), key.identifier() + " does not go on sneakers");
        }
        Holder<Enchantment> featherFalling = helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT)
                .getOrThrow(Enchantments.FEATHER_FALLING);
        sneakers.enchant(featherFalling, 4);
        helper.assertValueEqual(sneakers.getEnchantments().getLevel(featherFalling), 4, "feather falling on the sneakers");

        // Walking a long way in them, they stay as they were.
        ServerPlayer player = wearer(helper, sneakers);
        player.getItemBySlot(EquipmentSlot.FEET).hurtAndBreak(100, player, EquipmentSlot.FEET);
        helper.assertTrue(player.getItemBySlot(EquipmentSlot.FEET).is(ModItems.SNEAKERS), "the sneakers broke");
        helper.succeed();
    }

    @GameTest(structure = TestWorld.ARENA, maxTicks = 20)
    public void sneakersMakeTheWearerFasterAndStepHigher(GameTestHelper helper) {
        ServerPlayer player = wearer(helper, new ItemStack(ModItems.SNEAKERS));
        double baseSpeed = player.getAttributeBaseValue(Attributes.MOVEMENT_SPEED);
        // Worn equipment's attributes apply on the wearer's next tick; nothing ticks a mock player, so do it here.
        player.doTick();
        helper.runAfterDelay(1, () -> {
            helper.assertValueEqual(Math.round(player.getAttributeValue(Attributes.MOVEMENT_SPEED) / baseSpeed * 100.0D), 120L,
                    "speed with sneakers, as a percentage of barefoot");
            helper.assertTrue(Math.abs(player.getAttributeValue(Attributes.STEP_HEIGHT) - 1.8D) < 1.0E-6D,
                    "steps up " + player.getAttributeValue(Attributes.STEP_HEIGHT) + " blocks");
            helper.assertTrue(Math.abs(player.maxUpStep() - 1.8F) < 1.0E-6F, "max step " + player.maxUpStep());
            player.setItemSlot(EquipmentSlot.FEET, ItemStack.EMPTY);
            player.doTick();
            helper.runAfterDelay(1, () -> {
                helper.assertTrue(Math.abs(player.getAttributeValue(Attributes.MOVEMENT_SPEED) - baseSpeed) < 1.0E-9D, "still fast barefoot");
                helper.assertTrue(Math.abs(player.getAttributeValue(Attributes.STEP_HEIGHT) - 0.6D) < 1.0E-6D, "still steps high barefoot");
                helper.succeed();
            });
        });
    }

    /** Sprinting 20 blocks and jumping 10 times: free in sneakers, not barefoot. */
    @GameTest(structure = TestWorld.ARENA, maxTicks = 20)
    public void sneakersSaveHungerWalkingSprintingAndJumping(GameTestHelper helper) {
        ServerPlayer shod = wearer(helper, new ItemStack(ModItems.SNEAKERS));
        ServerPlayer barefoot = wearer(helper, ItemStack.EMPTY);
        for (ServerPlayer player : List.of(shod, barefoot)) {
            player.setOnGround(true);
            player.setSprinting(true);
            for (int step = 0; step < 20; step++) player.checkMovementStatistics(1.0D, 0.0D, 0.0D);
            player.setSprinting(false);
            for (int step = 0; step < 20; step++) player.checkMovementStatistics(0.0D, 0.0D, 1.0D);
            for (int jump = 0; jump < 10; jump++) {
                player.setOnGround(true);
                player.jumpFromGround();
            }
        }
        helper.assertValueEqual(exhaustion(shod), 0.0F, "exhaustion in sneakers");
        helper.assertTrue(exhaustion(barefoot) > 2.0F, "barefoot exhaustion is only " + exhaustion(barefoot));
        helper.succeed();
    }

    @GameTest(structure = TestWorld.ARENA, maxTicks = 20)
    public void sneakersKeepTheirRecipe(GameTestHelper helper) {
        Item n = ModItems.NYLON_FIBER;
        Item r = ModItems.SYNTHETIC_RUBBER;
        ItemStack made = craft(helper, 3, 2, n, null, n, r, null, r);
        helper.assertTrue(made.is(ModItems.SNEAKERS) && made.getCount() == 1, "nylon and rubber made " + made);
        helper.succeed();
    }

    private static ServerPlayer wearer(GameTestHelper helper, ItemStack boots) {
        TestWorld.floor(helper, Blocks.STONE);
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.setGameMode(GameType.SURVIVAL);
        BlockPos at = helper.absolutePos(new BlockPos(4, 1, 4));
        player.snapTo(at.getX() + 0.5D, at.getY(), at.getZ() + 0.5D, 0.0F, 0.0F);
        player.setItemSlot(EquipmentSlot.FEET, boots);
        return player;
    }

    private static float exhaustion(ServerPlayer player) {
        try {
            Field field = FoodData.class.getDeclaredField("exhaustionLevel");
            field.setAccessible(true);
            return field.getFloat(player.getFoodData());
        } catch (ReflectiveOperationException failure) {
            throw new AssertionError("cannot read exhaustion", failure);
        }
    }

    // ---- Music discs ----------------------------------------------------------------------------

    @GameTest(structure = TestWorld.ARENA, maxTicks = 40)
    public void musicDiscsPlayInAJukebox(GameTestHelper helper) {
        BlockPos at = new BlockPos(3, 1, 3);
        helper.setBlock(at, Blocks.JUKEBOX);
        JukeboxBlockEntity jukebox = helper.getBlockEntity(at, JukeboxBlockEntity.class);
        record Disc(Item item, String song, int comparator, int seconds) {}
        for (Disc disc : List.of(new Disc(MusicDiscs.MUSIC_DISC_SNOWMAN, "snowman", 13, 166),
                new Disc(MusicDiscs.MUSIC_DISC_PAYPHONE, "payphone", 14, 233))) {
            ItemStack stack = new ItemStack(disc.item());
            Holder<JukeboxSong> song = JukeboxSong.fromStack(stack).orElseThrow(() -> new AssertionError(disc.song() + " is not a disc"));
            helper.assertTrue(song.is(MusicDiscs.SNOWMAN) == disc.song().equals("snowman"), "wrong song on " + disc.song());
            helper.assertValueEqual(song.value().lengthInTicks(), disc.seconds() * 20, disc.song() + " length in ticks");
            jukebox.setTheItem(stack);
            helper.assertTrue(jukebox.getSongPlayer().isPlaying(), "the jukebox is not playing " + disc.song());
            helper.assertValueEqual(jukebox.getComparatorOutput(), disc.comparator(), "comparator reading for " + disc.song());
            jukebox.popOutTheItem();
        }
        helper.succeed();
    }

    @GameTest(structure = TestWorld.ARENA, maxTicks = 20)
    public void musicDiscRecipes(GameTestHelper helper) {
        // Any disc will do, not only the ones creepers drop.
        for (Item disc : List.of(Items.MUSIC_DISC_CAT, Items.MUSIC_DISC_PIGSTEP, Items.MUSIC_DISC_OTHERSIDE, Items.MUSIC_DISC_RELIC,
                Items.MUSIC_DISC_CREATOR, MusicDiscs.MUSIC_DISC_PAYPHONE)) {
            ItemStack snowman = craft(helper, 2, 1, disc, Items.SNOW_BLOCK);
            helper.assertTrue(snowman.is(MusicDiscs.MUSIC_DISC_SNOWMAN), disc + " and a snow block made " + snowman);
        }
        for (Item disc : List.of(Items.MUSIC_DISC_13, Items.MUSIC_DISC_5, Items.MUSIC_DISC_PRECIPICE, MusicDiscs.MUSIC_DISC_SNOWMAN)) {
            ItemStack payphone = craft(helper, 2, 1, disc, Items.BELL);
            helper.assertTrue(payphone.is(MusicDiscs.MUSIC_DISC_PAYPHONE), disc + " and a bell made " + payphone);
        }
        helper.assertTrue(craft(helper, 2, 1, Items.DIAMOND, Items.SNOW_BLOCK).isEmpty(), "a diamond made a disc");
        helper.assertTrue(new ItemStack(MusicDiscs.MUSIC_DISC_SNOWMAN).is(net.minecraft.tags.ItemTags.CREEPER_DROP_MUSIC_DISCS),
                "creepers do not drop the new discs");
        helper.succeed();
    }

    // ---- Insulated cable ------------------------------------------------------------------------

    private static final BlockPos TAP = new BlockPos(6, 1, 6);

    /** The same 9 V battery and 10 ohm load as the other tests, wired with the given cable. */
    private static void circuit(GameTestHelper helper, Block cable) {
        TestWorld.floor(helper, Blocks.STONE);
        for (Direction row : new Direction[] {Direction.NORTH, null, Direction.SOUTH}) {
            BlockPos live = row == null ? TAP : TAP.relative(row);
            helper.setBlock(live, cable);
            helper.setBlock(live.west(2), cable);
        }
        helper.setBlock(TAP.west().north(), ModBlocks.RESISTOR_LOAD.defaultBlockState().setValue(CircuitDeviceBlock.FACING, Direction.EAST));
        helper.setBlock(TAP.west().south(), ModBlocks.BATTERY.defaultBlockState().setValue(CircuitDeviceBlock.FACING, Direction.EAST));
    }

    private static ServerPlayer standingIn(GameTestHelper helper, BlockPos pos) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.setGameMode(GameType.SURVIVAL);
        // Until its client says it has loaded, a player cannot be hurt; a mock one never says so.
        player.connection.handleAcceptPlayerLoad(new net.minecraft.network.protocol.game.ServerboundPlayerLoadedPacket());
        BlockPos at = helper.absolutePos(pos);
        player.snapTo(at.getX() + 0.5D, at.getY(), at.getZ() + 0.5D, 0.0F, 0.0F);
        return player;
    }

    /** It carries the circuit like copper, runs this mod's blocks, and is safe to stand in. */
    @GameTest(structure = TestWorld.ARENA, maxTicks = 120)
    public void insulatedCableCarriesPowerSafely(GameTestHelper helper) {
        circuit(helper, ModBlocks.PVC_INSULATED_CABLE);
        ServerPlayer player = standingIn(helper, TAP);
        helper.runAtTickTime(60, () -> {
            double volts = helper.getBlockEntity(TAP, CopperCableBlockEntity.class).getVoltage();
            helper.assertTrue(volts > 8.0D, "the insulated cable is at " + volts + " V");
            helper.assertValueInBetween(10, ElectricPower.cableLevel(helper.getLevel(), helper.absolutePos(TAP.east())), 12,
                    "signal for a block beside the insulated cable");
            helper.assertValueEqual(player.getHealth(), player.getMaxHealth(), "health standing in insulated cable");
            helper.succeed();
        });
    }

    /**
     * Standing in live bare copper hurts; in insulated cable it does not. A mock player always says it is
     * creative, which the shock check skips, so the check is run on it directly here.
     */
    @GameTest(structure = TestWorld.ARENA, maxTicks = 120)
    public void bareCopperCableShocksButInsulatedDoesNot(GameTestHelper helper) {
        circuit(helper, ModBlocks.COPPER_CABLE);
        ServerPlayer player = standingIn(helper, TAP);
        helper.runAtTickTime(60, () -> {
            helper.assertTrue(shock(helper, player), "a live bare cable gave no shock");
            helper.assertTrue(player.getHealth() < player.getMaxHealth(), "the shock did no harm");
            for (Direction row : new Direction[] {Direction.NORTH, null, Direction.SOUTH}) {
                BlockPos live = row == null ? TAP : TAP.relative(row);
                helper.setBlock(live, ModBlocks.PP_INSULATED_CABLE);
            }
        });
        helper.runAtTickTime(110, () -> {
            helper.assertTrue(helper.getBlockEntity(TAP, CopperCableBlockEntity.class).getVoltage() > 8.0D, "the insulated cable is dead");
            float before = player.getHealth();
            helper.assertFalse(shock(helper, player), "insulated cable gave a shock");
            helper.assertValueEqual(player.getHealth(), before, "health after touching insulated cable");
            helper.succeed();
        });
    }

    private static boolean shock(GameTestHelper helper, ServerPlayer player) {
        try {
            java.lang.reflect.Method check = com.example.analogalu.Electrocution.class
                    .getDeclaredMethod("check", ServerLevel.class, ServerPlayer.class);
            check.setAccessible(true);
            return (boolean) check.invoke(null, helper.getLevel(), player);
        } catch (ReflectiveOperationException failure) {
            throw new AssertionError("cannot run the shock check", failure);
        }
    }

    private static ItemStack craft(GameTestHelper helper, int width, int height, Item... grid) {
        List<ItemStack> stacks = new ArrayList<>();
        for (Item item : grid) stacks.add(item == null ? ItemStack.EMPTY : new ItemStack(item));
        CraftingInput input = CraftingInput.of(width, height, stacks);
        ServerLevel level = helper.getLevel();
        return level.getServer().getRecipeManager().getRecipeFor(RecipeType.CRAFTING, input, level)
                .map(recipe -> recipe.value().assemble(input))
                .orElse(ItemStack.EMPTY);
    }
}
