package com.example.redstonetransportation.block;

import com.example.analogalu.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/** A car's diesel tank. Right-click with diesel to fill it, before or after the car is assembled. */
public final class FuelTankBlock extends BaseEntityBlock {
    public FuelTankBlock(Properties properties) {
        super(properties);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new FuelTankBlockEntity(pos, state);
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                          InteractionHand hand, BlockHitResult hit) {
        if (!stack.is(ModItems.DIESEL)) return InteractionResult.TRY_WITH_EMPTY_HAND;
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        if (level.getBlockEntity(pos) instanceof FuelTankBlockEntity tank) fill(tank, stack, player);
        return InteractionResult.SUCCESS;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof FuelTankBlockEntity tank) report(tank, player);
        return InteractionResult.SUCCESS;
    }

    /** Pours one piece of diesel from the hand into the tank and says how full it is. */
    public static void fill(FuelTankBlockEntity tank, ItemStack diesel, Player player) {
        if (!tank.addDiesel()) {
            player.sendOverlayMessage(Component.translatable("message.redstone_transportation.fuel_tank_full", FuelTankBlockEntity.CAPACITY_DIESEL));
            return;
        }
        if (!player.hasInfiniteMaterials()) diesel.shrink(1);
        report(tank, player);
    }

    public static void report(FuelTankBlockEntity tank, Player player) {
        player.sendOverlayMessage(Component.translatable("message.redstone_transportation.fuel_tank", tank.dieselLeft(),
                FuelTankBlockEntity.CAPACITY_DIESEL));
    }
}
