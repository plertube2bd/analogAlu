package com.example.analogalu.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.Container;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.HopperBlockEntity;

public final class MachineOutput {
   private MachineOutput() {
   }

   public static void emit(Level level, BlockPos pos, ItemStack stack) {
      if (level != null && !level.isClientSide() && !stack.isEmpty()) {
         ItemStack remaining = stack.copy();
         Container below = hopperAt(level, pos.below());
         if (below != null) {
            remaining = HopperBlockEntity.addItem(null, below, remaining, Direction.UP);
         } else {
            Container above = hopperAt(level, pos.above());
            if (above != null) {
               remaining = HopperBlockEntity.addItem(null, above, remaining, Direction.DOWN);
            }
         }

         if (!remaining.isEmpty()) {
            ItemEntity entity = new ItemEntity(level, pos.getX() + 0.5, pos.getY() + 1.05, pos.getZ() + 0.5, remaining);
            entity.setDeltaMovement(0.0, 0.1, 0.0);
            level.addFreshEntity(entity);
         }
      }
   }

   private static Container hopperAt(Level level, BlockPos pos) {
      BlockEntity blockEntity = level.getBlockEntity(pos);
      if (blockEntity instanceof HopperBlockEntity hopper) {
         return hopper;
      } else {
         return blockEntity instanceof UpwardHopperBlockEntity hopper ? hopper : null;
      }
   }
}
