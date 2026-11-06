package com.example.analogalu.legacy;

import com.example.analogalu.ModItems;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents.ModifyOutput;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

public final class ResinConverter {
   public static final Item PLASTIC_RESIN = (Item)Registry.register(
      BuiltInRegistries.ITEM, key(Registries.ITEM, "plastic_resin"), new Item(new Properties().setId(key(Registries.ITEM, "plastic_resin")))
   );
   public static final Block BLOCK = (Block)Registry.register(
      BuiltInRegistries.BLOCK,
      key(Registries.BLOCK, "resin_converter"),
      new ResinConverter.ConverterBlock(
         net.minecraft.world.level.block.state.BlockBehaviour.Properties.of()
            .strength(1.0F)
            .sound(SoundType.METAL)
            .setId(key(Registries.BLOCK, "resin_converter"))
      )
   );
   public static final Item BLOCK_ITEM = (Item)Registry.register(
      BuiltInRegistries.ITEM,
      key(Registries.ITEM, "resin_converter"),
      new BlockItem(BLOCK, new Properties().setId(key(Registries.ITEM, "resin_converter")).useBlockDescriptionPrefix())
   );

   private ResinConverter() {
   }

   private static <T> ResourceKey<T> key(ResourceKey<? extends Registry<T>> registry, String name) {
      return ResourceKey.create(registry, Identifier.fromNamespaceAndPath("analog_alu", name));
   }

   public static void init() {
      CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.REDSTONE_BLOCKS).register((ModifyOutput)output -> output.accept(BLOCK_ITEM));
   }

   static int convert(Player player) {
      Inventory inventory = player.getInventory();
      int converted = 0;

      for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
         ItemStack stack = inventory.getItem(slot);
         if (stack.is(PLASTIC_RESIN)) {
            converted += stack.getCount();
            inventory.setItem(slot, new ItemStack(ModItems.NAPHTHA, stack.getCount()));
         }
      }

      ItemStack carried = player.containerMenu.getCarried();
      if (carried.is(PLASTIC_RESIN)) {
         converted += carried.getCount();
         player.containerMenu.setCarried(new ItemStack(ModItems.NAPHTHA, carried.getCount()));
      }

      if (converted > 0) {
         inventory.setChanged();
      }

      return converted;
   }

   private static final class ConverterBlock extends Block {
      ConverterBlock(net.minecraft.world.level.block.state.BlockBehaviour.Properties properties) {
         super(properties);
      }

      protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
         if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
         }

         int converted = ResinConverter.convert(player);
         if (converted > 0) {
            level.playSound(null, pos, SoundEvents.BREWING_STAND_BREW, SoundSource.BLOCKS, 0.8F, 1.2F);
            player.sendOverlayMessage(Component.translatable("message.analog_alu.resin_converted", new Object[]{converted}).withStyle(ChatFormatting.GOLD));
         } else {
            player.sendOverlayMessage(Component.translatable("message.analog_alu.resin_none").withStyle(ChatFormatting.GRAY));
         }

         return InteractionResult.SUCCESS;
      }
   }
}
