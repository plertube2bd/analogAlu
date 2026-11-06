package com.example.redstonetransportation.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.EnumMap;
import java.util.Map;

/** A car seat: a cushion with a backrest behind whoever sits in it. FACING is the way the seat faces. */
public final class CarSeatBlock extends Block {
    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
    private static final VoxelShape CUSHION = Block.box(1.0D, 0.0D, 1.0D, 15.0D, 7.0D, 15.0D);
    private static final Map<Direction, VoxelShape> SHAPES = new EnumMap<>(Direction.class);

    static {
        SHAPES.put(Direction.NORTH, Shapes.or(CUSHION, Block.box(1.0D, 7.0D, 13.0D, 15.0D, 16.0D, 15.0D)));
        SHAPES.put(Direction.SOUTH, Shapes.or(CUSHION, Block.box(1.0D, 7.0D, 1.0D, 15.0D, 16.0D, 3.0D)));
        SHAPES.put(Direction.WEST, Shapes.or(CUSHION, Block.box(13.0D, 7.0D, 1.0D, 15.0D, 16.0D, 15.0D)));
        SHAPES.put(Direction.EAST, Shapes.or(CUSHION, Block.box(1.0D, 7.0D, 1.0D, 3.0D, 16.0D, 15.0D)));
    }

    public CarSeatBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    /** It faces the way the player placing it looks, so the backrest ends up behind them. */
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection());
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES.get(state.getValue(FACING));
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }
}
