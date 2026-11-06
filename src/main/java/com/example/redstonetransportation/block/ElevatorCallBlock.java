package com.example.redstonetransportation.block;

import com.example.redstonetransportation.rail.DriveInput;
import com.example.redstonetransportation.structure.MovingStructureEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.BlockHitResult;

/** Marks a floor. Right-click, or a rising signal of any kind, calls the nearest elevator to it. */
public final class ElevatorCallBlock extends Block {
    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;

    public ElevatorCallBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(POWERED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(POWERED);
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block sourceBlock,
                                   Orientation orientation, boolean movedByPiston) {
        if (!(level instanceof ServerLevel serverLevel)) return;
        boolean powered = DriveInput.readSignals(level, pos).speed() > 0;
        if (powered == state.getValue(POWERED)) return;
        level.setBlock(pos, state.setValue(POWERED, powered), Block.UPDATE_CLIENTS);
        if (powered) MovingStructureEntity.callTo(serverLevel, pos);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level instanceof ServerLevel serverLevel) {
            boolean called = MovingStructureEntity.callTo(serverLevel, pos);
            player.sendOverlayMessage(Component.translatable(
                    "message.redstone_transportation." + (called ? "elevator_called" : "elevator_missing")));
        }
        return InteractionResult.SUCCESS;
    }
}
