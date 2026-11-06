package com.example.analogalu.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.BlockHitResult;

public class AsphaltBlock extends Block {
   public static final IntegerProperty COLOR = IntegerProperty.create("color", 0, 16);

   public AsphaltBlock(Properties properties) {
      super(properties);
      this.registerDefaultState((BlockState)this.defaultBlockState().setValue(COLOR, 0));
   }

   protected void createBlockStateDefinition(Builder<Block, BlockState> builder) {
      builder.add(new Property[]{COLOR});
   }

   protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
      DyeColor dye = (DyeColor)stack.get(DataComponents.DYE);
      if (dye != null && player.getAbilities().mayBuild) {
         int color = dye.getId() + 1;
         if ((Integer)state.getValue(COLOR) == color) {
            return InteractionResult.PASS;
         }

         if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
         }

         level.setBlock(pos, (BlockState)state.setValue(COLOR, color), 3);
         level.playSound(null, pos, SoundEvents.DYE_USE, SoundSource.BLOCKS, 1.0F, 1.0F);
         if (!player.hasInfiniteMaterials()) {
            stack.shrink(1);
         }

         return InteractionResult.SUCCESS;
      } else {
         return InteractionResult.TRY_WITH_EMPTY_HAND;
      }
   }
}
