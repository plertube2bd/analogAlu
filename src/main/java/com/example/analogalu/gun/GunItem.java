package com.example.analogalu.gun;

import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;

import java.util.function.Consumer;

/**
 * A gun. Left-click fires, holding right-click aims down the sights (the view zooms in, and a shotgun's
 * pellets spread less), and R cocks a revolver or starts a reload; left-click and R reach the server as a
 * {@link GunActionPayload}. Everything happens on the server and the state lives on the stack.
 *
 */
public final class GunItem extends Item {
    private static final String KEY = "message.analog_alu.gun.";

    private final GunType type;

    public GunItem(Properties properties, GunType type) {
        super(properties);
        this.type = type;
    }

    public GunType type() {
        return type;
    }

    public static int rounds(ItemStack stack) {
        return stack.getOrDefault(ModGuns.ROUNDS, 0);
    }

    public static boolean isCocked(ItemStack stack) {
        return stack.getOrDefault(ModGuns.COCKED, false);
    }

    public static boolean isReloading(ItemStack stack) {
        return stack.getOrDefault(ModGuns.RELOAD_UNTIL, 0L) > 0L;
    }

    /** Right-click: raise the gun to the eye and keep it there while the button is held. */
    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (hand != InteractionHand.MAIN_HAND) return InteractionResult.PASS;
        player.startUsingItem(hand);
        return InteractionResult.CONSUME;
    }

    @Override
    public ItemUseAnimation getUseAnimation(ItemStack stack) {
        return ItemUseAnimation.SPYGLASS;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity user) {
        return 72_000;
    }

    /** True while the player holds right-click with this gun raised. */
    public static boolean isAiming(LivingEntity user, ItemStack stack) {
        return user.isUsingItem() && user.getUseItem() == stack;
    }

    /** Left-click: fire, if the gun is ready to. */
    public InteractionResult fire(ServerPlayer player, ItemStack stack) {
        ServerLevel server = player.level();
        if (player.getCooldowns().isOnCooldown(stack)) return InteractionResult.FAIL;
        if (isReloading(stack)) {
            tell(player, "reloading");
            return InteractionResult.FAIL;
        }
        if (type.usesMagazine()) {
            int rounds = rounds(stack);
            if (rounds <= 0) {
                dryFire(server, player);
                tell(player, "empty");
                return InteractionResult.FAIL;
            }
            if (type.needsCocking() && !isCocked(stack)) {
                dryFire(server, player);
                tell(player, "cock_first");
                return InteractionResult.FAIL;
            }
            stack.set(ModGuns.ROUNDS, rounds - 1);
            stack.set(ModGuns.COCKED, false);
        } else if (!player.hasInfiniteMaterials() && takeAmmo(player, type.ammo(), 1) == 0) {
            dryFire(server, player);
            player.sendOverlayMessage(Component.translatable(KEY + "no_ammo", Component.translatable(type.ammo().getDescriptionId())));
            return InteractionResult.FAIL;
        }
        GunFiring.fire(server, player, type, isAiming(player, stack));
        player.getCooldowns().addCooldown(stack, type.shotCooldown());
        showLoad(player, stack);
        return InteractionResult.CONSUME;
    }

    /** The R key. */
    public void action(ServerPlayer player, ItemStack stack) {
        ServerLevel level = player.level();
        if (!type.usesMagazine()) {
            tell(player, "musket_loads_itself");
            return;
        }
        if (isReloading(stack)) {
            tell(player, "reloading");
            return;
        }
        int rounds = rounds(stack);
        if (type.needsCocking() && rounds > 0) {
            if (isCocked(stack)) {
                tell(player, "already_cocked");
                return;
            }
            stack.set(ModGuns.COCKED, true);
            sound(level, player, SoundEvents.CROSSBOW_LOADING_END, 0.7F, 1.7F);
            player.sendOverlayMessage(Component.translatable(KEY + "cocked", rounds, type.capacity()));
            return;
        }
        if (rounds >= type.capacity()) {
            tell(player, "full");
            return;
        }
        if (!player.hasInfiniteMaterials() && countAmmo(player, type.ammo()) == 0) {
            player.sendOverlayMessage(Component.translatable(KEY + "no_ammo", Component.translatable(type.ammo().getDescriptionId())));
            return;
        }
        stack.set(ModGuns.RELOAD_UNTIL, level.getGameTime() + type.reloadTicks());
        stack.set(ModGuns.COCKED, false);
        player.getCooldowns().addCooldown(stack, type.reloadTicks());
        sound(level, player, SoundEvents.CROSSBOW_LOADING_START, 0.8F, 0.8F);
        player.sendOverlayMessage(Component.translatable(KEY + "reload_started", String.format("%.1f", type.reloadTicks() / 20.0)));
    }

    /** A reload finishes here, wherever in the inventory the gun has been put. */
    @Override
    public void inventoryTick(ItemStack stack, ServerLevel level, Entity entity, EquipmentSlot slot) {
        long until = stack.getOrDefault(ModGuns.RELOAD_UNTIL, 0L);
        if (until <= 0L || level.getGameTime() < until) return;
        stack.remove(ModGuns.RELOAD_UNTIL);
        if (!(entity instanceof Player player)) return;
        int missing = type.capacity() - rounds(stack);
        int loaded = player.hasInfiniteMaterials() ? missing : takeAmmo(player, type.ammo(), missing);
        stack.set(ModGuns.ROUNDS, rounds(stack) + loaded);
        stack.set(ModGuns.COCKED, false);
        sound(level, player, SoundEvents.CROSSBOW_LOADING_END, 0.9F, 0.9F);
        player.sendOverlayMessage(Component.translatable(KEY + "reloaded", rounds(stack), type.capacity()));
    }

    /** Loading, cocking and firing change the stack; the gun stays raised instead of being put away and drawn again. */
    @Override
    public boolean allowComponentsUpdateAnimation(Player player, InteractionHand hand, ItemStack oldStack, ItemStack newStack) {
        return false;
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return type.usesMagazine();
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        return Math.round(13.0F * rounds(stack) / type.capacity());
    }

    @Override
    public int getBarColor(ItemStack stack) {
        if (isReloading(stack)) return 0x9A9A9A;
        return rounds(stack) > 0 ? 0xE8B04A : 0xB03A2E;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
                                Consumer<Component> tooltip, TooltipFlag flag) {
        String key = "tooltip.analog_alu." + type.id();
        if (type.pellets() > 1) {
            tooltip.accept(Component.translatable("tooltip.analog_alu.gun.pellets", type.pellets(), number(type.damage()),
                    number(type.range())).withStyle(ChatFormatting.GRAY));
        } else {
            tooltip.accept(Component.translatable("tooltip.analog_alu.gun.damage", number(type.damage()),
                    number(type.range())).withStyle(ChatFormatting.GRAY));
        }
        tooltip.accept(Component.translatable(key).withStyle(ChatFormatting.GRAY));
        tooltip.accept(Component.translatable("tooltip.analog_alu.gun.ammo", Component.translatable(type.ammo().getDescriptionId())).withStyle(ChatFormatting.DARK_GRAY));
        if (type.usesMagazine()) {
            tooltip.accept(Component.translatable("tooltip.analog_alu.gun.loaded", rounds(stack), type.capacity())
                    .withStyle(rounds(stack) > 0 ? ChatFormatting.GOLD : ChatFormatting.RED));
        }
    }

    private static String number(double value) {
        return value == Math.rint(value) ? String.valueOf((long) value) : String.valueOf(value);
    }

    private void showLoad(Player player, ItemStack stack) {
        if (type.usesMagazine()) {
            player.sendOverlayMessage(Component.translatable(KEY + "load", rounds(stack), type.capacity()));
        } else if (!player.hasInfiniteMaterials()) {
            player.sendOverlayMessage(Component.translatable(KEY + "spare", Component.translatable(type.ammo().getDescriptionId()), countAmmo(player, type.ammo())));
        }
    }

    private static void tell(Player player, String what) {
        player.sendOverlayMessage(Component.translatable(KEY + what));
    }

    private static void dryFire(ServerLevel level, Player player) {
        level.playSound(null, player.getX(), player.getEyeY(), player.getZ(), SoundEvents.DISPENSER_FAIL,
                SoundSource.PLAYERS, 0.6F, 1.9F);
    }

    private static void sound(ServerLevel level, Player player, Holder<SoundEvent> sound, float volume, float pitch) {
        level.playSound(null, player.getX(), player.getEyeY(), player.getZ(), sound, SoundSource.PLAYERS, volume, pitch);
    }

    public static int countAmmo(Player player, Item ammo) {
        Inventory inventory = player.getInventory();
        int count = 0;
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (stack.is(ammo)) count += stack.getCount();
        }
        return count;
    }

    /** Takes up to {@code wanted} of the ammunition out of the inventory; returns how many it found. */
    public static int takeAmmo(Player player, Item ammo, int wanted) {
        Inventory inventory = player.getInventory();
        int taken = 0;
        for (int slot = 0; slot < inventory.getContainerSize() && taken < wanted; slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (!stack.is(ammo)) continue;
            int take = Math.min(stack.getCount(), wanted - taken);
            stack.shrink(take);
            taken += take;
        }
        if (taken > 0) inventory.setChanged();
        return taken;
    }
}
