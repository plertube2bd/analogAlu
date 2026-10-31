package com.example.analogalu.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.redstone.Orientation;

public class SemiconductorBlock extends Block {
    private final boolean waterproof;

    public SemiconductorBlock(BlockBehaviour.Properties properties, boolean waterproof) {
        super(properties);
        this.waterproof = waterproof;
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        if (!waterproof && !oldState.is(this) && touchesWater(level, pos)) {
            breakBlock(state, level, pos);
        }
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block block,
            Orientation orientation, boolean movedByPiston) {
        if (!waterproof && !level.isClientSide() && touchesWater(level, pos)) {
            breakBlock(state, level, pos);
        }
    }

    private static boolean touchesWater(LevelReader level, BlockPos pos) {
        if (level.getFluidState(pos).is(net.minecraft.tags.FluidTags.WATER)) return true;
        for (Direction direction : Direction.values()) {
            if (level.getFluidState(pos.relative(direction)).is(net.minecraft.tags.FluidTags.WATER)) return true;
        }
        return false;
    }

    private static void breakBlock(BlockState state, Level level, BlockPos pos) {
        if (level.isClientSide()) return;
        Block.dropResources(state, level, pos);
        level.removeBlock(pos, false);
    }
}