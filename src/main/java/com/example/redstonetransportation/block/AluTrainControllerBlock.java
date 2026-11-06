package com.example.redstonetransportation.block;

import com.example.redstonetransportation.integration.analogalu.AnalogAluSignalAdapter;
import com.example.redstonetransportation.util.RedstoneSignals;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Four-channel bridge between Analog ALU circuits and a tiered rail motor.
 * FACING points from this controller toward the motor it drives.
 *
 * Inputs relative to FACING:
 * - back: 4-bit speed command (0..15)
 * - left: enable permission (>0)
 * - right: reverse request (>0)
 * - top: emergency brake (>0)
 * Output: speed command toward the adjacent motor.
 */
public final class AluTrainControllerBlock extends Block {
    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final IntegerProperty POWER = BlockStateProperties.POWER;
    public static final BooleanProperty ACTIVE = BooleanProperty.create("active");
    public static final BooleanProperty REVERSE = BooleanProperty.create("reverse");
    public static final BooleanProperty BRAKING = BooleanProperty.create("braking");
    public static final BooleanProperty ALU_LINKED = BooleanProperty.create("alu_linked");

    public AluTrainControllerBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(POWER, 0)
                .setValue(ACTIVE, false)
                .setValue(REVERSE, false)
                .setValue(BRAKING, false)
                .setValue(ALU_LINKED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, POWER, ACTIVE, REVERSE, BRAKING, ALU_LINKED);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        // The arrow is the controller output direction; point it at the motor.
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected boolean isSignalSource(BlockState state) {
        return true;
    }

    @Override
    protected int getSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        // Match the direction convention used by the other Transportation signal blocks.
        return direction == state.getValue(FACING).getOpposite() ? state.getValue(POWER) : 0;
    }

    @Override
    protected int getDirectSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        return getSignal(state, level, pos, direction);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                               Player player, BlockHitResult hit) {
        if (!player.isShiftKeyDown()) return InteractionResult.PASS;
        if (level.isClientSide()) return InteractionResult.SUCCESS;

        Direction next = state.getValue(FACING).getClockWise();
        level.setBlock(pos, state.setValue(FACING, next), 3);
        level.scheduleTick(pos, this, 1);
        player.sendOverlayMessage(Component.literal("ALU 출력 방향: " + directionName(next)));
        return InteractionResult.SUCCESS;
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos,
                           BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (!level.isClientSide() && oldState.getBlock() != this) {
            level.scheduleTick(pos, this, 1);
        }
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos,
                                   Block sourceBlock, Orientation orientation, boolean movedByPiston) {
        if (!level.isClientSide()) level.scheduleTick(pos, this, 1);
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        Direction facing = state.getValue(FACING);
        int speed = AnalogAluSignalAdapter.readInput(level, pos, facing.getOpposite());
        int enable = AnalogAluSignalAdapter.readInput(level, pos, facing.getCounterClockWise());
        int reverse = AnalogAluSignalAdapter.readInput(level, pos, facing.getClockWise());
        int brake = AnalogAluSignalAdapter.readInput(level, pos, Direction.UP);
        boolean aluLinked = isAluAt(level, pos, facing.getOpposite())
                || isAluAt(level, pos, facing.getCounterClockWise())
                || isAluAt(level, pos, facing.getClockWise())
                || isAluAt(level, pos, Direction.UP);

        boolean braking = brake > 0;
        boolean reverseActive = reverse > 0;
        int output = (enable > 0 && !braking) ? speed : 0;
        boolean active = output > 0;

        BlockState updated = state
                .setValue(POWER, output)
                .setValue(ACTIVE, active)
                .setValue(REVERSE, reverseActive)
                .setValue(BRAKING, braking)
                .setValue(ALU_LINKED, aluLinked);

        boolean driveCommandChanged = state.getValue(POWER) != output
                || state.getValue(REVERSE) != reverseActive
                || state.getValue(BRAKING) != braking;
        if (!updated.equals(state)) {
            level.setBlock(pos, updated, 3);
            // A reverse/brake command can change while the speed byte stays the same.
            // Always wake the adjacent motor when any drive command channel changes.
            if (driveCommandChanged) RedstoneSignals.notifyAllNeighbors(level, pos, this);
        }

        // ALU blocks update after their own calculation delay. Poll briefly while a channel is live.
        if (speed > 0 || enable > 0 || reverse > 0 || brake > 0) {
            level.scheduleTick(pos, this, 2);
        }
    }

    private static boolean isAluAt(Level level, BlockPos receiverPos, Direction direction) {
        return AnalogAluSignalAdapter.isAnalogAluComponent(level.getBlockState(receiverPos.relative(direction)));
    }

    private static String directionName(Direction direction) {
        return switch (direction) {
            case NORTH -> "북쪽";
            case SOUTH -> "남쪽";
            case EAST -> "동쪽";
            case WEST -> "서쪽";
            default -> "방향";
        };
    }
}
