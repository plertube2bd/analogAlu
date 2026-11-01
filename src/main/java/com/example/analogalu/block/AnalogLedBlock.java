package com.example.analogalu.block;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.redstone.Orientation;

public final class AnalogLedBlock extends Block {
    public static final IntegerProperty POWER = IntegerProperty.create("power", 0, 15);
    private final boolean analog;

    public AnalogLedBlock(BlockBehaviour.Properties properties, boolean analog) {
        super(properties);
        this.analog = analog;
        registerDefaultState(defaultBlockState().setValue(POWER, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(POWER);
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState,
            boolean movedByPiston) {
        scheduleUpdate(level, pos);
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block block,
            Orientation orientation, boolean movedByPiston) {
        scheduleUpdate(level, pos);
    }

    private void scheduleUpdate(Level level, BlockPos pos) {
        if (!level.isClientSide() && !level.getBlockTicks().hasScheduledTick(pos, this)) {
            level.scheduleTick(pos, this, 1);
        }
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        int input = level.getBestNeighborSignal(pos);
        int output = analog ? input : input == 0 ? 0 : 9;
        if (state.getValue(POWER) != output) {
            level.setBlock(pos, state.setValue(POWER, output), Block.UPDATE_ALL);
        }
    }
}