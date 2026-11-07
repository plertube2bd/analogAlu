package com.example.redstonetransportation.test;

import com.example.analogalu.gun.GunItem;
import com.example.analogalu.gun.ModGuns;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;

import java.util.ArrayList;
import java.util.List;

/**
 * The guns, fired by a player standing at z=2 and facing down a 60 block range. Distances are from the
 * eye to the near face of the target. Iron golems have no armour, so they show the raw damage.
 */
public final class GunGameTests {
    private static final String RANGE = "redstone_transportation_test:range";
    private static final int STAND_Z = 2;

    private static ServerPlayer shooter(GameTestHelper helper, Item gun, int rounds) {
        for (int x = 0; x < 9; x++) {
            for (int z = 0; z < 60; z++) helper.setBlock(new BlockPos(x, 0, z), Blocks.STONE);
        }
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.setGameMode(GameType.SURVIVAL);
        BlockPos at = helper.absolutePos(new BlockPos(4, 1, STAND_Z));
        player.snapTo(at.getX() + 0.5D, at.getY(), at.getZ() + 0.5D, 0.0F, 0.0F);
        // A player looks where its head points; a mock player's head starts a few degrees off.
        player.setYHeadRot(0.0F);
        player.setYBodyRot(0.0F);
        ItemStack stack = new ItemStack(gun);
        if (rounds > 0) stack.set(ModGuns.ROUNDS, rounds);
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        return player;
    }

    private static <T extends LivingEntity> T target(GameTestHelper helper, net.minecraft.world.entity.EntityType<T> type, int z, double health) {
        T mob = (T) helper.spawnWithNoFreeWill((net.minecraft.world.entity.EntityType) type, new BlockPos(4, 1, z));
        if (mob instanceof net.minecraft.world.entity.monster.zombie.Zombie zombie) {
            // Spawning may hand a husk armour or make it a baby; the tests need a plain grown one.
            zombie.setBaby(false);
            for (EquipmentSlot slot : new EquipmentSlot[] {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS,
                    EquipmentSlot.FEET, EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND}) {
                zombie.setItemSlot(slot, ItemStack.EMPTY);
            }
        }
        mob.getAttribute(Attributes.MAX_HEALTH).setBaseValue(health);
        mob.setHealth((float) health);
        return mob;
    }

    private static ItemStack gun(ServerPlayer player) {
        return player.getMainHandItem();
    }

    /** Left-click. */
    private static void fire(GameTestHelper helper, ServerPlayer player) {
        ((GunItem) gun(player).getItem()).fire(player, gun(player));
    }

    /** Right-click, held down. */
    private static void aim(GameTestHelper helper, ServerPlayer player) {
        gun(player).getItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
    }

    /** The R key. */
    private static void pressR(ServerPlayer player) {
        ((GunItem) gun(player).getItem()).action(player, gun(player));
    }

    /** What the cooldown between shots would do given time; the tests skip the wait. */
    private static void waitOut(ServerPlayer player) {
        player.getCooldowns().removeCooldown(player.getCooldowns().getCooldownGroup(gun(player)));
    }

    private static void assertLoss(GameTestHelper helper, LivingEntity mob, double expected, String what) {
        double loss = mob.getMaxHealth() - mob.getHealth();
        helper.assertTrue(Math.abs(loss - expected) < 0.01D, what + ": lost " + loss + ", expected " + expected);
    }

    /** Right-click raises the gun to aim: it does not fire, and a shot fired while aiming lands as usual. */
    @GameTest(structure = RANGE, maxTicks = 40)
    public void rightClickAimsAndLeftClickFires(GameTestHelper helper) {
        ServerPlayer player = shooter(helper, ModGuns.REVOLVER, 6);
        LivingEntity golem = target(helper, EntityTypes.IRON_GOLEM, 12, 100.0D);
        pressR(player);
        aim(helper, player);
        helper.assertTrue(player.isUsingItem() && GunItem.isAiming(player, gun(player)), "right-click did not raise the gun to aim");
        assertLoss(helper, golem, 0.0D, "aiming");
        helper.assertValueEqual(GunItem.rounds(gun(player)), 6, "rounds after aiming");
        fire(helper, player);
        assertLoss(helper, golem, 25.0D, "a shot while aiming");
        helper.assertValueEqual(GunItem.rounds(gun(player)), 5, "rounds after the shot");
        player.stopUsingItem();
        helper.assertFalse(GunItem.isAiming(player, gun(player)), "still aiming after letting go");
        helper.succeed();
    }

    /** Aimed, the shotgun's cone is narrower, so more of the forty pellets find a target at its edge of range. */
    @GameTest(structure = RANGE, maxTicks = 40)
    public void aimingTightensTheShotgun(GameTestHelper helper) {
        ServerPlayer player = shooter(helper, ModGuns.SHOTGUN, 2);
        LivingEntity golem = target(helper, EntityTypes.IRON_GOLEM, 15, 1000.0D);
        double hip = 0.0D;
        double aimed = 0.0D;
        for (int round = 0; round < 6; round++) {
            gun(player).set(ModGuns.ROUNDS, 2);
            waitOut(player);
            float before = golem.getHealth();
            fire(helper, player);
            hip += before - golem.getHealth();
            aim(helper, player);
            waitOut(player);
            before = golem.getHealth();
            fire(helper, player);
            aimed += before - golem.getHealth();
            player.stopUsingItem();
        }
        helper.assertTrue(aimed > hip, "aimed pellets did " + aimed + ", hip-fired " + hip);
        helper.succeed();
    }

    @GameTest(structure = RANGE, maxTicks = 40)
    public void theRevolverNeedsCockingBeforeEveryShot(GameTestHelper helper) {
        ServerPlayer player = shooter(helper, ModGuns.REVOLVER, 6);
        LivingEntity golem = target(helper, EntityTypes.IRON_GOLEM, 12, 100.0D);
        fire(helper, player);
        assertLoss(helper, golem, 0.0D, "fired without cocking");
        helper.assertValueEqual(GunItem.rounds(gun(player)), 6, "rounds after an uncocked pull");
        pressR(player);
        helper.assertTrue(GunItem.isCocked(gun(player)), "R did not cock the hammer");
        fire(helper, player);
        assertLoss(helper, golem, 25.0D, "one shot");
        helper.assertValueEqual(GunItem.rounds(gun(player)), 5, "rounds after one shot");
        waitOut(player);
        fire(helper, player);
        assertLoss(helper, golem, 25.0D, "a second pull without cocking");
        pressR(player);
        waitOut(player);
        fire(helper, player);
        assertLoss(helper, golem, 50.0D, "two shots");
        helper.succeed();
    }

    @GameTest(structure = RANGE, maxTicks = 40)
    public void theRevolverHitsAtThirtyBlocks(GameTestHelper helper) {
        ServerPlayer player = shooter(helper, ModGuns.REVOLVER, 6);
        LivingEntity golem = target(helper, EntityTypes.IRON_GOLEM, 32, 100.0D);
        pressR(player);
        fire(helper, player);
        assertLoss(helper, golem, 25.0D, "a shot at 29 blocks");
        helper.succeed();
    }

    @GameTest(structure = RANGE, maxTicks = 40)
    public void theRevolverFallsShortPastThirtyTwoBlocks(GameTestHelper helper) {
        ServerPlayer player = shooter(helper, ModGuns.REVOLVER, 6);
        LivingEntity golem = target(helper, EntityTypes.IRON_GOLEM, 36, 100.0D);
        pressR(player);
        fire(helper, player);
        assertLoss(helper, golem, 0.0D, "a shot at 33 blocks");
        helper.succeed();
    }

    /** 25 damage takes 24.6 off a husk's own 2 armour and only about 9 off one in full diamond. */
    @GameTest(structure = RANGE, maxTicks = 40)
    public void armourSoaksUpPartOfAShot(GameTestHelper helper) {
        ServerPlayer player = shooter(helper, ModGuns.REVOLVER, 6);
        LivingEntity bare = target(helper, EntityTypes.HUSK, 10, 100.0D);
        pressR(player);
        fire(helper, player);
        double bareLoss = bare.getMaxHealth() - bare.getHealth();
        bare.discard();
        LivingEntity armoured = target(helper, EntityTypes.HUSK, 10, 100.0D);
        armoured.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.DIAMOND_HELMET));
        armoured.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.DIAMOND_CHESTPLATE));
        armoured.setItemSlot(EquipmentSlot.LEGS, new ItemStack(Items.DIAMOND_LEGGINGS));
        armoured.setItemSlot(EquipmentSlot.FEET, new ItemStack(Items.DIAMOND_BOOTS));
        // Armour counts once the husk has worn it for a tick.
        helper.runAfterDelay(3, () -> {
            waitOut(player);
            pressR(player);
            fire(helper, player);
            double armouredLoss = armoured.getMaxHealth() - armoured.getHealth();
            helper.assertTrue(bareLoss > 24.0D && bareLoss <= 25.0D, "bare husk lost " + bareLoss);
            helper.assertTrue(armouredLoss > 7.0D && armouredLoss < 12.0D, "husk in diamond lost " + armouredLoss);
            helper.succeed();
        });
    }

    @GameTest(structure = RANGE, maxTicks = 40)
    public void theMusketLoadsBallsFromTheInventory(GameTestHelper helper) {
        ServerPlayer player = shooter(helper, ModGuns.MUSKET, 0);
        LivingEntity golem = target(helper, EntityTypes.IRON_GOLEM, 42, 100.0D);
        fire(helper, player);
        assertLoss(helper, golem, 0.0D, "fired with no balls");
        player.getInventory().add(new ItemStack(ModGuns.MUSKET_BALL, 3));
        fire(helper, player);
        assertLoss(helper, golem, 30.0D, "a shot at 39 blocks");
        helper.assertValueEqual(GunItem.countAmmo(player, ModGuns.MUSKET_BALL), 2, "balls left");
        helper.assertTrue(player.getCooldowns().isOnCooldown(gun(player)), "no wait after a musket shot");
        fire(helper, player);
        assertLoss(helper, golem, 30.0D, "a shot straight after");
        waitOut(player);
        fire(helper, player);
        assertLoss(helper, golem, 60.0D, "a shot after the wait");
        helper.succeed();
    }

    @GameTest(structure = RANGE, maxTicks = 40)
    public void theMusketFallsShortPastFortyEightBlocks(GameTestHelper helper) {
        ServerPlayer player = shooter(helper, ModGuns.MUSKET, 0);
        player.getInventory().add(new ItemStack(ModGuns.MUSKET_BALL, 3));
        LivingEntity golem = target(helper, EntityTypes.IRON_GOLEM, 53, 100.0D);
        fire(helper, player);
        assertLoss(helper, golem, 0.0D, "a shot at 50 blocks");
        helper.succeed();
    }

    /** Forty pellets of 1.5 at close range nearly all land: up to 60 per shot, two shots per load. */
    @GameTest(structure = RANGE, maxTicks = 40)
    public void theShotgunSpraysFortyPellets(GameTestHelper helper) {
        ServerPlayer player = shooter(helper, ModGuns.SHOTGUN, 2);
        LivingEntity golem = target(helper, EntityTypes.IRON_GOLEM, 7, 500.0D);
        fire(helper, player);
        double first = golem.getMaxHealth() - golem.getHealth();
        helper.assertTrue(first >= 45.0D && first <= 60.0D, "one shot at 4 blocks took " + first);
        waitOut(player);
        fire(helper, player);
        double second = golem.getMaxHealth() - golem.getHealth();
        helper.assertTrue(second - first >= 45.0D, "the second barrel took " + (second - first));
        helper.assertValueEqual(GunItem.rounds(gun(player)), 0, "shells left");
        waitOut(player);
        fire(helper, player);
        helper.assertValueEqual(golem.getMaxHealth() - golem.getHealth(), (float) second, "a third shot from two shells");
        helper.succeed();
    }

    @GameTest(structure = RANGE, maxTicks = 40)
    public void theShotgunFallsShortPastSixteenBlocks(GameTestHelper helper) {
        ServerPlayer player = shooter(helper, ModGuns.SHOTGUN, 2);
        LivingEntity golem = target(helper, EntityTypes.IRON_GOLEM, 21, 100.0D);
        fire(helper, player);
        assertLoss(helper, golem, 0.0D, "a shot at 18 blocks");
        helper.succeed();
    }

    /** An empty revolver reloads on R, takes four seconds, and loads six rounds out of the inventory. */
    @GameTest(structure = RANGE, maxTicks = 120)
    public void theRevolverReloadsWhenEmpty(GameTestHelper helper) {
        ServerPlayer player = shooter(helper, ModGuns.REVOLVER, 0);
        player.getInventory().add(new ItemStack(ModGuns.BULLET_9MM, 10));
        pressR(player);
        helper.assertTrue(GunItem.isReloading(gun(player)), "R did not start a reload");
        ServerLevel level = helper.getLevel();
        helper.runAfterDelay(40, () -> {
            gun(player).getItem().inventoryTick(gun(player), level, player, EquipmentSlot.MAINHAND);
            helper.assertValueEqual(GunItem.rounds(gun(player)), 0, "rounds two seconds into the reload");
        });
        helper.runAfterDelay(82, () -> {
            gun(player).getItem().inventoryTick(gun(player), level, player, EquipmentSlot.MAINHAND);
            helper.assertValueEqual(GunItem.rounds(gun(player)), 6, "rounds after the reload");
            helper.assertValueEqual(GunItem.countAmmo(player, ModGuns.BULLET_9MM), 4, "rounds left in the inventory");
            helper.assertTrue(!GunItem.isCocked(gun(player)), "a fresh load came cocked");
            helper.succeed();
        });
    }

    @GameTest(structure = RANGE, maxTicks = 120)
    public void theShotgunReloadsTwoShells(GameTestHelper helper) {
        ServerPlayer player = shooter(helper, ModGuns.SHOTGUN, 0);
        player.getInventory().add(new ItemStack(ModGuns.SHOTGUN_SHELL, 5));
        pressR(player);
        ServerLevel level = helper.getLevel();
        helper.runAfterDelay(72, () -> {
            gun(player).getItem().inventoryTick(gun(player), level, player, EquipmentSlot.MAINHAND);
            helper.assertValueEqual(GunItem.rounds(gun(player)), 2, "shells after the reload");
            helper.assertValueEqual(GunItem.countAmmo(player, ModGuns.SHOTGUN_SHELL), 3, "shells left in the inventory");
            helper.succeed();
        });
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

    private static void assertMakes(GameTestHelper helper, ItemStack made, Item item, int count, String what) {
        helper.assertTrue(made.is(item) && made.getCount() == count, what + " made " + made);
    }

    @GameTest(structure = TestWorld.ARENA, maxTicks = 20)
    public void blackPowderAndAmmunitionRecipes(GameTestHelper helper) {
        assertMakes(helper, craft(helper, 2, 1, Items.GUNPOWDER, Items.CHARCOAL), ModGuns.BLACK_POWDER, 1, "gunpowder + charcoal");
        assertMakes(helper, craft(helper, 2, 1, Items.GUNPOWDER, Items.COAL), ModGuns.BLACK_POWDER, 1, "gunpowder + coal");
        assertMakes(helper, craft(helper, 3, 1, Items.BONE_MEAL, Items.COAL, Items.FLINT), Items.GUNPOWDER, 4, "bone meal + coal + flint");
        assertMakes(helper, craft(helper, 3, 1, Items.FLINT, Items.CHARCOAL, Items.BONE_MEAL), Items.GUNPOWDER, 4, "flint + charcoal + bone meal");
        helper.assertTrue(craft(helper, 2, 1, Items.BONE_MEAL, Items.COAL).isEmpty(), "bone meal and coal alone made gunpowder");
        assertMakes(helper, craft(helper, 2, 1, ModGuns.BLACK_POWDER, Items.IRON_INGOT), ModGuns.BULLET_9MM, 4, "black powder + iron");
        assertMakes(helper, craft(helper, 3, 1, ModGuns.BLACK_POWDER, Items.IRON_INGOT, Items.IRON_INGOT), ModGuns.MUSKET_BALL, 4,
                "black powder + two iron");
        Item n = Items.IRON_NUGGET;
        assertMakes(helper, craft(helper, 3, 3, ModGuns.BLACK_POWDER, n, n, n, n, n, n, n, n), ModGuns.SHOTGUN_SHELL, 1,
                "black powder + eight nuggets");
        helper.assertTrue(craft(helper, 2, 1, Items.GUNPOWDER, Items.DIAMOND).isEmpty(), "a diamond made black powder");
        helper.succeed();
    }

    @GameTest(structure = TestWorld.ARENA, maxTicks = 20)
    public void gunRecipes(GameTestHelper helper) {
        Item i = Items.IRON_INGOT;
        Item t = Items.TRIPWIRE_HOOK;
        Item f = Items.FLINT_AND_STEEL;
        Item p = Items.OAK_PLANKS;
        Item l = Items.OAK_LOG;
        assertMakes(helper, craft(helper, 3, 3, i, i, i, null, t, p, null, null, p), ModGuns.REVOLVER, 1, "revolver");
        assertMakes(helper, craft(helper, 3, 3, i, null, null, null, i, f, null, null, l), ModGuns.MUSKET, 1, "musket");
        assertMakes(helper, craft(helper, 3, 3, i, i, null, i, t, f, null, l, l), ModGuns.SHOTGUN, 1, "shotgun");
        helper.succeed();
    }
}
