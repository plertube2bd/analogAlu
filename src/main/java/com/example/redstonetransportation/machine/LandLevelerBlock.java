package com.example.redstonetransportation.machine;

import com.example.redstonetransportation.content.ModContent;
import com.example.redstonetransportation.util.BlockMessages;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.BlockHitResult;

/**
 * The land leveler. FACING is the way it drives and STATUS lights the same arrow as the factory
 * machines. Right-click opens it; sneaking turns it a quarter, the way the other machines turn.
 */
public final class LandLevelerBlock extends BaseEntityBlock {
    public LandLevelerBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any()
                .setValue(MachineBlock.FACING, Direction.NORTH)
                .setValue(MachineBlock.STATUS, MachineStatus.OFF));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(MachineBlock.FACING, MachineBlock.STATUS);
    }

    /** It sets off away from whoever placed it. */
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(MachineBlock.FACING, context.getHorizontalDirection());
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new LandLevelerBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide() ? null : createTickerHelper(type, ModContent.LAND_LEVELER_ENTITY, LandLevelerBlockEntity::serverTick);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        if (player.isShiftKeyDown()) {
            Direction next = state.getValue(MachineBlock.FACING).getClockWise();
            level.setBlock(pos, state.setValue(MachineBlock.FACING, next), Block.UPDATE_ALL);
            player.sendOverlayMessage(Component.translatable(
                    "message.redstone_transportation.land_leveler_facing", BlockMessages.directionName(next)));
        } else if (level.getBlockEntity(pos) instanceof LandLevelerBlockEntity leveler) {
            player.openMenu(leveler);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
        Containers.updateNeighboursAfterDestroy(state, level, pos);
    }
}
