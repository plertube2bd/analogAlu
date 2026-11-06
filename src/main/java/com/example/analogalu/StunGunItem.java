package com.example.analogalu;

import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.tag.convention.v2.ConventionalEntityTypeTags;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item.Properties;

public final class StunGunItem extends Item {
   public static final int MAX_CHARGES = 32;
   public static final float DAMAGE = 2.0F;
   public static final int STUN_TICKS = 40;
   public static final int COOLDOWN_TICKS = 200;
   public static final ResourceKey<DamageType> DAMAGE_TYPE = ResourceKey.create(
      Registries.DAMAGE_TYPE, Identifier.fromNamespaceAndPath("analog_alu", "stun_gun")
   );

   public StunGunItem(Properties properties) {
      super(properties.stacksTo(1).component(ModComponents.STUN_CHARGES, 32));
   }

   public static int charges(ItemStack stack) {
      return Math.clamp(((Integer)stack.getOrDefault(ModComponents.STUN_CHARGES, 32)).intValue(), 0, 32);
   }

   public boolean isBarVisible(ItemStack stack) {
      return charges(stack) < 32;
   }

   public int getBarWidth(ItemStack stack) {
      return Math.round(13.0F * charges(stack) / 32.0F);
   }

   public int getBarColor(ItemStack stack) {
      return 5623039;
   }

   public static boolean isBoss(LivingEntity target) {
      return target.is(ConventionalEntityTypeTags.BOSSES) || target instanceof EnderDragon || target instanceof WitherBoss;
   }

   public static void init() {
      UseEntityCallback.EVENT.register((UseEntityCallback)(player, level, hand, entity, hit) -> {
         ItemStack stack = player.getItemInHand(hand);
         if (stack.is(ModItems.STUN_GUN) && !player.isSpectator()) {
            if (!(entity instanceof LivingEntity target && !(target instanceof ArmorStand) && target.isAlive())) {
               return InteractionResult.PASS;
            } else {
               if (player.getCooldowns().isOnCooldown(stack)) {
                  return InteractionResult.FAIL;
               }

               if (isBoss(target)) {
                  if (!level.isClientSide()) {
                     player.sendOverlayMessage(Component.translatable("message.analog_alu.stun_gun_boss").withStyle(ChatFormatting.RED));
                  }

                  return InteractionResult.FAIL;
               } else if (level instanceof ServerLevel serverLevel) {
                  zap(serverLevel, player, stack, target);
                  return InteractionResult.SUCCESS;
               } else {
                  return InteractionResult.SUCCESS;
               }
            }
         } else {
            return InteractionResult.PASS;
         }
      });
   }

   private static void zap(ServerLevel level, Player player, ItemStack stack, LivingEntity target) {
      boolean hurt = target.hurtServer(level, level.damageSources().source(DAMAGE_TYPE, player), 2.0F);
      if (hurt) {
         target.addEffect(new MobEffectInstance(ModEffects.STUNNED, 40, 0, false, true, true), player);
      }

      player.getCooldowns().addCooldown(stack, 200);
      level.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.COPPER_BULB_TURN_ON, SoundSource.PLAYERS, 1.0F, 1.8F);
      level.sendParticles(ParticleTypes.ELECTRIC_SPARK, target.getX(), target.getY() + target.getBbHeight() / 2.0, target.getZ(), 20, 0.3, 0.4, 0.3, 0.3);
      if (!player.hasInfiniteMaterials()) {
         int left = charges(stack) - 1;
         if (left <= 0) {
            level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ITEM_BREAK, SoundSource.PLAYERS, 0.8F, 1.0F);
            stack.shrink(1);
         } else {
            stack.set(ModComponents.STUN_CHARGES, left);
         }
      }
   }
}
