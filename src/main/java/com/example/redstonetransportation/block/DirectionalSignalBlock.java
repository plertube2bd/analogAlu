package com.example.redstonetransportation.block;

import com.example.redstonetransportation.integration.analogalu.ElectricPower;
import com.example.redstonetransportation.util.RedstoneSignals;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.network.chat.Component;

/** Directional 0..15 signal repeater used by the vehicle controller and signal relay. */
public class DirectionalSignalBlock extends Block {
    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final IntegerProperty POWER = BlockStateProperties.POWER;
    /** Visual indicator for a non-zero signal passing through the block. */
    public static final BooleanProperty ACTIVE = BooleanProperty.create("active");

    public DirectionalSignalBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(POWER, 0)
                .setValue(ACTIVE, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, POWER, ACTIVE);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected boolean isSignalSource(BlockState state) { return true; }

    @Override
    protected int getSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        return direction == state.getValue(FACING).getOpposite() ? state.getValue(POWER) : 0;
    }

    @Override
    protected int getDirectSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        return getSignal(state, level, pos, direction);
    }

    /** Sneak + empty-hand right-click rotates the output/drive direction clockwise. */
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                               Player player, BlockHitResult hit) {
        if (!player.isShiftKeyDown()) return InteractionResult.PASS;
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        Direction next = state.getValue(FACING).getClockWise();
        level.setBlock(pos, state.setValue(FACING, next), 3);
        level.scheduleTick(pos, this, 1);
        String koreanDirection = switch (next) {
            case NORTH -> "북쪽";
            case SOUTH -> "남쪽";
            case EAST -> "동쪽";
            case WEST -> "서쪽";
            default -> "방향";
        };
        player.sendOverlayMessage(Component.literal("출력 방향: " + koreanDirection));
        return InteractionResult.SUCCESS;
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (!level.isClientSide() && oldState.getBlock() != this) level.scheduleTick(pos, this, 1);
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos,
                                   Block sourceBlock, Orientation orientation, boolean movedByPiston) {
        if (!level.isClientSide()) level.scheduleTick(pos, this, 1);
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        Direction output = state.getValue(FACING);
        int input = ElectricPower.isAvailable()
                ? ElectricPower.inputExcept(level, pos, output)
                : RedstoneSignals.readInputExceptOutput(level, pos, output);
        boolean active = input > 0;
        if (state.getValue(POWER) != input || state.getValue(ACTIVE) != active) {
            level.setBlock(pos, state.setValue(POWER, input).setValue(ACTIVE, active), 3);
            if (state.getValue(POWER) != input) {
                RedstoneSignals.notifyAllNeighbors(level, pos, this);
            }
        }
        if (ElectricPower.isAvailable()) level.scheduleTick(pos, this, ElectricPower.CHECK_TICKS);
    }
}
