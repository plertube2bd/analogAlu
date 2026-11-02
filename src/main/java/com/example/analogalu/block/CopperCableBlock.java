package com.example.analogalu.block;

import com.example.analogalu.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.BlockHitResult;
import java.util.Map;

public class CopperCableBlock extends BaseEntityBlock {
    public static final IntegerProperty LOCK = IntegerProperty.create("lock", 0, 64);
    public static final BooleanProperty POWERED = BooleanProperty.create("powered");
    /** 그 방향으로 전선 팔이 뻗어 있는가 (모델/선택 영역용). 연결 계산은 CircuitSolver가 따로 한다. */
    public static final Map<Direction, BooleanProperty> ARMS = Map.of(
            Direction.NORTH, BooleanProperty.create("north"),
            Direction.EAST, BooleanProperty.create("east"),
            Direction.SOUTH, BooleanProperty.create("south"),
            Direction.WEST, BooleanProperty.create("west"),
            Direction.UP, BooleanProperty.create("up"),
            Direction.DOWN, BooleanProperty.create("down"));

    /** 중심 기둥(6x6x6)과 팔(4x4)의 선택 영역. 팔 조합(6비트)별로 미리 만들어 둔다. */
    private static final VoxelShape[] SHAPES = new VoxelShape[64];

    static {
        VoxelShape core = Block.box(5.0, 5.0, 5.0, 11.0, 11.0, 11.0);
        for (int mask = 0; mask < 64; mask++) {
            VoxelShape shape = core;
            for (Direction direction : Direction.values()) {
                if ((mask & (1 << direction.get3DDataValue())) != 0) shape = Shapes.or(shape, armShape(direction));
            }
            SHAPES[mask] = shape;
        }
    }

    private static VoxelShape armShape(Direction direction) {
        return switch (direction) {
            case NORTH -> Block.box(6.0, 6.0, 0.0, 10.0, 10.0, 5.0);
            case SOUTH -> Block.box(6.0, 6.0, 11.0, 10.0, 10.0, 16.0);
            case WEST -> Block.box(0.0, 6.0, 6.0, 5.0, 10.0, 10.0);
            case EAST -> Block.box(11.0, 6.0, 6.0, 16.0, 10.0, 10.0);
            case DOWN -> Block.box(6.0, 0.0, 6.0, 10.0, 5.0, 10.0);
            case UP -> Block.box(6.0, 11.0, 6.0, 10.0, 16.0, 10.0);
        };
    }

    public CopperCableBlock(BlockBehaviour.Properties properties) {
        super(properties);
        BlockState state = defaultBlockState().setValue(LOCK, 0).setValue(POWERED, false);
        for (BooleanProperty arm : ARMS.values()) state = state.setValue(arm, false);
        registerDefaultState(state);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState> builder) {
        builder.add(LOCK, POWERED);
        for (BooleanProperty arm : ARMS.values()) builder.add(arm);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        int mask = 0;
        for (Direction direction : Direction.values()) {
            if (state.getValue(ARMS.get(direction))) mask |= 1 << direction.get3DDataValue();
        }
        return SHAPES[mask];
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return withArms(context.getLevel(), context.getClickedPos(), defaultBlockState());
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos,
            Direction directionToNeighbor, BlockPos neighborPos, BlockState neighborState, RandomSource random) {
        return withArms(level, pos, state);
    }

    /** 이웃 상태에 맞춰 팔(연결 모양)을 다시 계산한다. 구리선끼리는 잠금 규칙(allowsConnection)을 따른다. */
    private static BlockState withArms(BlockGetter level, BlockPos pos, BlockState state) {
        for (Direction direction : Direction.values()) {
            state = state.setValue(ARMS.get(direction), isConnected(level, pos, state, direction));
        }
        return state;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CopperCableBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
            BlockEntityType<T> type) {
        return level.isClientSide() ? null
                : createTickerHelper(type, ModBlocks.COPPER_CABLE_ENTITY, CopperCableBlockEntity::tick);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState,
            boolean movedByPiston) {
        if (level.isClientSide() || oldState.is(this)) return;
        BlockState updated = withArms(level, pos, state);
        if (updated != state) level.setBlock(pos, updated, Block.UPDATE_CLIENTS);
        CircuitSolver.recomputeFrom(level, pos);
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block block,
            Orientation orientation, boolean movedByPiston) {
        if (level.isClientSide()) return;
        BlockState shaped = withArms(level, pos, state);
        if (shaped != state) level.setBlock(pos, shaped, Block.UPDATE_CLIENTS);
    CircuitSolver.recomputeFrom(level, pos);
    }

    @Override
    protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos,
            boolean movedByPiston) {
        if (!movedByPiston) {
            CircuitSolver.recomputeAround(level, pos);
        }
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
            BlockHitResult hit) {
        if (!player.getAbilities().mayBuild) return InteractionResult.PASS;
        if (level.isClientSide()) return InteractionResult.SUCCESS;

        boolean locking = state.getValue(LOCK) == 0;
        int mask = 0;
        if (locking) {
            for (Direction direction : Direction.values()) {
                if (isConnected(level, pos, direction)) mask |= 1 << direction.get3DDataValue();
            }
        }
        BlockState toggled = state.setValue(LOCK, locking ? mask + 1 : 0);
        level.setBlock(pos, withArms(level, pos, toggled), Block.UPDATE_CLIENTS);
        level.playSound(null, pos, SoundEvents.COMPARATOR_CLICK, SoundSource.BLOCKS, 0.3F,
                locking ? 0.55F : 0.6F);
        for (Direction direction : Direction.values()) {
            level.updateNeighborsAt(pos.relative(direction), this);
        }
        CircuitSolver.recomputeFrom(level, pos);

        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof CopperCableBlockEntity cable) {
            player.sendOverlayMessage(Component.literal(String.format(java.util.Locale.ROOT,
                "저항 %.5f Ω  |  전압 %.2f V  |  전류 %.3f A  |  온도 %.1f °C  |  내구도 %.1f/100  |  %s",
                cable.getResistanceOhms(), cable.getVoltage(), cable.getCurrent(), cable.getTemperatureCelsius(),
                cable.getHealth(), locking ? "잠김" : "해제")));
        }
        return InteractionResult.SUCCESS;
    }

    public static boolean allowsConnection(BlockState state, Direction direction) {
        int lock = state.getValue(LOCK);
        return lock == 0 || (((lock - 1) >> direction.get3DDataValue()) & 1) != 0;
    }

    public static boolean isConnected(BlockGetter level, BlockPos pos, Direction direction) {
        return isConnected(level, pos, level.getBlockState(pos), direction);
    }

    private static boolean isConnected(BlockGetter level, BlockPos pos, BlockState state, Direction direction) {
        if (!state.is(ModBlocks.COPPER_CABLE)) return false;
        BlockPos neighbor = pos.relative(direction);
        BlockState neighborState = level.getBlockState(neighbor);
        if (!allowsConnection(state, direction)) return false;
        if (neighborState.getBlock() instanceof CopperCableBlock) {
            return allowsConnection(neighborState, direction.getOpposite());
        }
        if (neighborState.getBlock() instanceof CircuitDeviceBlock) {
            CircuitDeviceBlock device = (CircuitDeviceBlock) neighborState.getBlock();
            if (device.kind() == CircuitDeviceBlock.Kind.MINER) {
                Direction facing = neighborState.getValue(CircuitDeviceBlock.FACING);
                return direction == Direction.UP || direction == Direction.DOWN
                        || direction == facing.getClockWise() || direction == facing.getCounterClockWise();
            }
            Direction facing = neighborState.getValue(CircuitDeviceBlock.FACING);
            return direction.getOpposite() == facing || direction.getOpposite() == facing.getOpposite();
        }
        return false;
    }

}