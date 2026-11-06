package com.example.analogalu.block;

import com.example.analogalu.ModBlocks;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.phys.BlockHitResult;

public class ChemicalMachineBlock extends CircuitDeviceBlock {
   public ChemicalMachineBlock(Properties properties, CircuitDeviceBlock.Kind kind) {
      super(properties, kind);
   }

   @Override
   public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
      return new ChemicalMachineBlockEntity(pos, state);
   }

   @Override
   public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
      return level.isClientSide() ? null : createTickerHelper(type, ModBlocks.CHEMICAL_MACHINE_ENTITY, ChemicalMachineBlockEntity::tick);
   }

   @Override
   protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
      if (level.getBlockEntity(pos) instanceof ChemicalMachineBlockEntity machine) {
         if (!machine.recipe().uses(stack.getItem())) {
            if (!stack.isEmpty() && !level.isClientSide()) {
               player.sendOverlayMessage(
                  Component.translatable("message.analog_alu.machine_wrong_input", new Object[]{ChemicalMachineBlockEntity.recipeName(machine.recipe())})
                     .withStyle(ChatFormatting.RED)
               );
            }

            return InteractionResult.TRY_WITH_EMPTY_HAND;
         } else {
            if (level.isClientSide()) {
               return InteractionResult.SUCCESS;
            }

            boolean water = stack.is(Items.WATER_BUCKET);
            Item item = stack.getItem();
            Component name = stack.getItemName();
            int inserted = machine.insert(stack, player.isShiftKeyDown() ? stack.getCount() : 1);
            if (inserted <= 0) {
               player.sendOverlayMessage(Component.translatable("message.analog_alu.machine_full").withStyle(ChatFormatting.RED));
               return InteractionResult.FAIL;
            }

            if (water) {
               player.setItemInHand(hand, ItemUtils.createFilledResult(stack, player, new ItemStack(Items.BUCKET)));
               level.playSound(null, pos, SoundEvents.BUCKET_EMPTY, SoundSource.BLOCKS, 1.0F, 1.0F);
            } else {
               if (!player.hasInfiniteMaterials()) {
                  stack.shrink(inserted);
               }

               level.playSound(null, pos, SoundEvents.ITEM_PICKUP, SoundSource.BLOCKS, 0.4F, 0.8F);
            }

            player.sendOverlayMessage(
               Component.translatable("message.analog_alu.machine_inserted", new Object[]{name, machine.stored(item)}).withStyle(ChatFormatting.GOLD)
            );
            return InteractionResult.SUCCESS;
         }
      } else {
         return InteractionResult.PASS;
      }
   }

   @Override
   protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
      if (this.kind() == CircuitDeviceBlock.Kind.CHEMICAL_REACTOR && player.getAbilities().mayBuild) {
         if (level.getBlockEntity(pos) instanceof ChemicalMachineBlockEntity machine) {
            if (level.isClientSide()) {
               return InteractionResult.SUCCESS;
            }

            ChemicalRecipes.Recipe recipe = machine.cycleRecipe(!player.isShiftKeyDown());
            player.sendOverlayMessage(
               Component.translatable("message.analog_alu.reactor_process", new Object[]{ChemicalMachineBlockEntity.recipeName(recipe)})
                  .withStyle(ChatFormatting.LIGHT_PURPLE)
            );
            level.playSound(null, pos, SoundEvents.COMPARATOR_CLICK, SoundSource.BLOCKS, 0.5F, player.isShiftKeyDown() ? 0.5F : 0.7F);
            return InteractionResult.SUCCESS;
         } else {
            return InteractionResult.PASS;
         }
      } else {
         return InteractionResult.PASS;
      }
   }
}
