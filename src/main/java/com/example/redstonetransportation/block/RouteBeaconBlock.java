package com.example.redstonetransportation.block;

import com.example.redstonetransportation.rail.DriveInput;
import com.example.redstonetransportation.structure.MovingStructureEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
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

/**
 * Sends the nearest ship or airship here. A rising signal's strength sets its speed, the same scale
 * a drive rail uses, and an ALU brake input stops it. A comparator reads 15 while a vessel is
 * docked, which is what chains beacons into a route.
 */
public final class RouteBeaconBlock extends Block {
    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;
    public static final BooleanProperty DOCKED = BooleanProperty.create("docked");
    private static final int DOCK_CHECK_TICKS = 20;
    private static final int FULL_SIGNAL = 15;

    public RouteBeaconBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(POWERED, false).setValue(DOCKED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(POWERED, DOCKED);
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block sourceBlock,
                                   Orientation orientation, boolean movedByPiston) {
        if (!(level instanceof ServerLevel serverLevel)) return;
        DriveInput input = DriveInput.readSignals(level, pos);
        if (input.brake()) MovingStructureEntity.cancelRouteTo(serverLevel, pos);

        boolean powered = input.speed() > 0 && !input.brake();
        if (powered == state.getValue(POWERED)) return;
        level.setBlock(pos, state.setValue(POWERED, powered), Block.UPDATE_CLIENTS);
        if (powered) MovingStructureEntity.sendTo(serverLevel, pos, (double) input.speed() / FULL_SIGNAL);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level instanceof ServerLevel serverLevel) {
            boolean sent = MovingStructureEntity.sendTo(serverLevel, pos, 1.0D);
            player.sendOverlayMessage(Component.translatable(
                    "message.redstone_transportation." + (sent ? "route_sent" : "route_missing")));
        }
        return InteractionResult.SUCCESS;
    }

    /** Called by a vessel that has arrived. */
    public static void dock(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof RouteBeaconBlock)) return;
        level.setBlock(pos, state.setValue(DOCKED, true), Block.UPDATE_ALL);
        level.scheduleTick(pos, state.getBlock(), DOCK_CHECK_TICKS);
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!state.getValue(DOCKED)) return;
        if (MovingStructureEntity.isDockedAt(level, pos)) {
            level.scheduleTick(pos, this, DOCK_CHECK_TICKS);
        } else {
            level.setBlock(pos, state.setValue(DOCKED, false), Block.UPDATE_ALL);
        }
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos, Direction direction) {
        return state.getValue(DOCKED) ? FULL_SIGNAL : 0;
    }
}
