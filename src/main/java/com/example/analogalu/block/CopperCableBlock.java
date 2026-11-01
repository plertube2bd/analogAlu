package com.example.analogalu.block;

import com.example.analogalu.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
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
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;

public class CopperCableBlock extends BaseEntityBlock {
    public static final IntegerProperty LOCK = IntegerProperty.create("lock", 0, 64);

    public CopperCableBlock(BlockBehaviour.Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(LOCK, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState> builder) {
        builder.add(LOCK);
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
        if (!level.isClientSide() && !oldState.is(this)) CircuitSolver.solveFrom(level, pos);
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block block,
            Orientation orientation, boolean movedByPiston) {
        if (!level.isClientSide()) CircuitSolver.solveFrom(level, pos);
    }

    @Override
    protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos,
            boolean movedByPiston) {
        if (!movedByPiston) CircuitSolver.recomputeAround(level, pos);
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
        level.setBlock(pos, state.setValue(LOCK, locking ? mask + 1 : 0), Block.UPDATE_CLIENTS);
        level.playSound(null, pos, SoundEvents.COMPARATOR_CLICK, SoundSource.BLOCKS, 0.3F,
                locking ? 0.55F : 0.6F);
        for (Direction direction : Direction.values()) {
            level.updateNeighborsAt(pos.relative(direction), this);
        }
        CircuitSolver.solveFrom(level, pos);

        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof CopperCableBlockEntity cable) {
                player.sendSystemMessage(Component.literal(String.format(java.util.Locale.ROOT,
                    "R %.5f Ω | V %.2f V | I %.3f A | %.1f°C | HP %.1f/100 | %s",
                    cable.getResistanceOhms(), cable.getVoltage(), cable.getCurrent(), cable.getTemperatureCelsius(),
                    cable.getHealth(), locking ? "LOCKED" : "UNLOCKED")));
        }
        return InteractionResult.SUCCESS;
    }

    public static boolean allowsConnection(BlockState state, Direction direction) {
        int lock = state.getValue(LOCK);
        return lock == 0 || (((lock - 1) >> direction.get3DDataValue()) & 1) != 0;
    }

    public static boolean isConnected(Level level, BlockPos pos, Direction direction) {
        BlockPos neighbor = pos.relative(direction);
        BlockState neighborState = level.getBlockState(neighbor);
        BlockState state = level.getBlockState(pos);
        return neighborState.getBlock() instanceof CopperCableBlock
                && allowsConnection(state, direction)
                && allowsConnection(neighborState, direction.getOpposite());
    }
}