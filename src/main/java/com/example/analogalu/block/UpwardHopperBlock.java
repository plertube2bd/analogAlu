package com.example.analogalu.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.HopperBlock;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.Block;
import com.example.analogalu.ModBlocks;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.network.chat.Component;

public class UpwardHopperBlock extends BaseEntityBlock {
    public static final BooleanProperty EJECTION_ENABLED = BooleanProperty.create("ejection_enabled");

    public UpwardHopperBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(HopperBlock.FACING, net.minecraft.core.Direction.DOWN)
                .setValue(EJECTION_ENABLED, true));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState> builder) {
        builder.add(HopperBlock.FACING, EJECTION_ENABLED);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState();
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new UpwardHopperBlockEntity(pos, state);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
            BlockHitResult hit) {
        if (player.isShiftKeyDown()) {
            if (!level.isClientSide()) {
                BlockState updated = state.cycle(EJECTION_ENABLED);
                level.setBlock(pos, updated, Block.UPDATE_ALL);
                level.playSound(null, pos, net.minecraft.sounds.SoundEvents.COMPARATOR_CLICK,
                        net.minecraft.sounds.SoundSource.BLOCKS, 0.3F,
                        updated.getValue(EJECTION_ENABLED) ? 0.7F : 0.5F);
                player.sendOverlayMessage(Component.translatable(updated.getValue(EJECTION_ENABLED)
                        ? "block.analog_alu.upward_hopper.ejection_enabled"
                        : "block.analog_alu.upward_hopper.ejection_disabled"));
            }
            return InteractionResult.SUCCESS;
        }
        if (level.getBlockEntity(pos) instanceof UpwardHopperBlockEntity hopper) {
            player.openMenu(hopper);
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
            BlockEntityType<T> type) {
        return level.isClientSide() ? null
                : createTickerHelper(type, ModBlocks.UPWARD_HOPPER_ENTITY, UpwardHopperBlockEntity::tick);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }
}