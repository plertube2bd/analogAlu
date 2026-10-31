package com.example.analogalu.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * 연산 블록 공통 베이스. 중계기처럼 납작한 형태.
 *
 * FACING = 출력(정면) 방향. 플레이어가 바라보는 방향으로 설치되므로 신호는 플레이어 반대쪽으로 흐른다.
 * 입력 A = 정면 기준 왼쪽, 입력 B = 정면 기준 오른쪽, 단일 입력 블록(연장기/시프트)은 뒷면.
 * Carry/Borrow 출력은 뒷면(세기 0 또는 1).
 * 입력이 바뀌면 1게임 틱 뒤에 결과를 반영한다. 같은 틱의 여러 변경은 틱 시점에 한 번만 계산한다.
 */
public abstract class AluBlock extends Block {
    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final IntegerProperty POWER = BlockStateProperties.POWER;
    public static final BooleanProperty CARRY = BooleanProperty.create("carry");
    public static final BooleanProperty CARRY_ENABLED = BooleanProperty.create("carry_enabled");
    public static final BooleanProperty INVERTED = BooleanProperty.create("inverted");
    private static final VoxelShape SHAPE = Block.box(0, 0, 0, 16, 2, 16);

    public static final int DELAY_TICKS = 1;

    public record Out(int main, boolean carry) {}

    protected AluBlock(BlockBehaviour.Properties props) {
        super(props);
        BlockState s = stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(POWER, 0);
        if (hasCarry()) s = s.setValue(CARRY, false).setValue(CARRY_ENABLED, true);
        if (hasInvert()) s = s.setValue(INVERTED, false);
        registerDefaultState(s);
    }

    /** Carry/Borrow 출력이 있는 블록인지 (상수를 반환해야 함) */
    protected boolean hasCarry() {
        return false;
    }

    /** 우클릭으로 반전(NOT) 모드를 전환할 수 있는 블록인지: 논리 게이트 (상수를 반환해야 함) */
    protected boolean hasInvert() {
        return false;
    }

    protected abstract Out compute(Level level, BlockPos pos, BlockState state);

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, POWER);
        if (hasCarry()) builder.add(CARRY, CARRY_ENABLED);
        if (hasInvert()) builder.add(INVERTED);
    }

    // ------------------------------------------------------------ 입력

    private static int readInput(Level level, BlockPos pos, Direction side) {
        return level.getSignal(pos.relative(side), side);
    }

    protected static int inputA(Level level, BlockPos pos, BlockState state) {
        return readInput(level, pos, state.getValue(FACING).getCounterClockWise());
    }

    protected static int inputB(Level level, BlockPos pos, BlockState state) {
        return readInput(level, pos, state.getValue(FACING).getClockWise());
    }

    protected static int inputBack(Level level, BlockPos pos, BlockState state) {
        return readInput(level, pos, state.getValue(FACING).getOpposite());
    }

    // ------------------------------------------------------------ 출력

    @Override
    protected boolean isSignalSource(BlockState state) {
        return true;
    }

    @Override
    protected int getSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        Direction f = state.getValue(FACING);
        if (direction == f.getOpposite()) return state.getValue(POWER);       // 정면으로 출력
        if (hasCarry() && direction == f) return state.getValue(CARRY) ? 1 : 0; // 뒷면으로 Carry 출력
        return 0;
    }

    @Override
    protected int getDirectSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        return getSignal(state, level, pos, direction);
    }

    private BlockState applyOut(BlockState state, Out out) {
        state = state.setValue(POWER, out.main() & 15);
        if (hasCarry()) state = state.setValue(CARRY, out.carry() && state.getValue(CARRY_ENABLED));
        return state;
    }

    private void checkUpdate(Level level, BlockPos pos, BlockState state) {
        if (level.isClientSide()) return;
        if (applyOut(state, compute(level, pos, state)) != state
                && !level.getBlockTicks().hasScheduledTick(pos, this)) {
            level.scheduleTick(pos, this, DELAY_TICKS);
        }
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        BlockState next = applyOut(state, compute(level, pos, state));
        if (next != state) {
            level.setBlock(pos, next, Block.UPDATE_CLIENTS);
            notifyOutputs(level, pos, next);
        }
    }

    private void notifyOutputs(Level level, BlockPos pos, BlockState state) {
        Direction f = state.getValue(FACING);
        notifySide(level, pos.relative(f));
        if (hasCarry()) notifySide(level, pos.relative(f.getOpposite()));
    }

    private void notifySide(Level level, BlockPos np) {
        level.neighborChanged(np, this, null);
        level.updateNeighborsAt(np, this);
    }

    // ------------------------------------------------------------ 설치/업데이트

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return defaultBlockState().setValue(FACING, ctx.getHorizontalDirection());
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        BlockPos below = pos.below();
        BlockState b = level.getBlockState(below);
        return b.isFaceSturdy(level, below, Direction.UP) || b.is(Blocks.HOPPER);
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        checkUpdate(level, pos, state);
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block block,
                                   Orientation orientation, boolean movedByPiston) {
        if (level.isClientSide()) return;
        if (!state.canSurvive(level, pos)) {
            dropResources(state, level, pos);
            level.removeBlock(pos, false);
            return;
        }
        checkUpdate(level, pos, state);
    }

    @Override
    protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
        if (!movedByPiston) notifyOutputs(level, pos, state);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return SHAPE;
    }

    /** 우클릭: Carry/Borrow 출력 활성/비활성 전환 (가산기, 감산기) */
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                               Player player, BlockHitResult hit) {
        if (!(hasCarry() || hasInvert()) || !player.getAbilities().mayBuild) return InteractionResult.PASS;
        if (!level.isClientSide()) {
            state = hasCarry() ? state.cycle(CARRY_ENABLED) : state.cycle(INVERTED);
            boolean on = hasCarry() ? state.getValue(CARRY_ENABLED) : state.getValue(INVERTED);
            level.setBlock(pos, state, Block.UPDATE_CLIENTS);
            level.playSound(null, pos, SoundEvents.COMPARATOR_CLICK, SoundSource.BLOCKS, 0.3F,
                    on ? 0.55F : 0.5F);
            checkUpdate(level, pos, state);
        }
        return InteractionResult.SUCCESS;
    }
}
