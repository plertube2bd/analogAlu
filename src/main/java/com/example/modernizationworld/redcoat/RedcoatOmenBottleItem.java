package com.example.modernizationworld.redcoat;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/** A crafted ominous bottle that marks its drinker for a British raid. */
public final class RedcoatOmenBottleItem extends Item {
    public RedcoatOmenBottleItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        player.startUsingItem(hand);
        return InteractionResult.CONSUME;
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity user) {
        if (!level.isClientSide()) {
            user.addEffect(new MobEffectInstance(RedcoatContent.REDCOAT_OMEN, 120_000, 0, false, true, true));
        }
        if (user instanceof ServerPlayer player) {
            player.awardStat(Stats.ITEM_USED.get(this));
            if (!player.getAbilities().instabuild) {
                stack.shrink(1);
                if (stack.isEmpty()) return new ItemStack(net.minecraft.world.item.Items.GLASS_BOTTLE);
                ItemStack bottle = new ItemStack(net.minecraft.world.item.Items.GLASS_BOTTLE);
                if (!player.getInventory().add(bottle)) {
                    player.drop(bottle, false, net.minecraft.util.Prediction.SERVER_ONLY);
                }
            }
        }
        return stack;
    }

    @Override
    public ItemUseAnimation getUseAnimation(ItemStack stack) {
        return ItemUseAnimation.DRINK;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity user) {
        return 32;
    }
}
