package com.example.redstonetransportation.block;

import com.example.redstonetransportation.integration.analogalu.ElectricPower;
import com.example.redstonetransportation.rail.DriveInput;
import com.example.redstonetransportation.util.BlockMessages;
import com.mojang.datafixers.util.Pair;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.minecart.AbstractMinecart;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseRailBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.block.state.properties.RailShape;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * A rail that is its own motor. Signal 1..15 sets the speed every passing cart then keeps;
 * signal 0 holds carts on this rail. The RailDrive mixin reads this block on each rail step.
 */
public final class DriveRailBlock extends BaseRailBlock {
    public static final EnumProperty<RailShape> SHAPE = BlockStateProperties.RAIL_SHAPE;
    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;
    /** False departs toward the shape's second exit, true toward its first. */
    public static final BooleanProperty REVERSED = BooleanProperty.create("reversed");

    public DriveRailBlock(Properties properties) {
        super(false, properties);
        registerDefaultState(stateDefinition.any()
                .setValue(SHAPE, RailShape.NORTH_SOUTH)
                .setValue(POWERED, false)
                .setValue(REVERSED, false)
                .setValue(WATERLOGGED, false));
    }

    public static Vec3 heading(BlockState state, boolean reverse) {
        Pair<Vec3i, Vec3i> exits = AbstractMinecart.exits(state.getValue(SHAPE));
        Vec3i exit = state.getValue(REVERSED) != reverse ? exits.getFirst() : exits.getSecond();
        return new Vec3(exit.getX(), 0.0D, exit.getZ()).normalize();
    }

    @Override
    public Property<RailShape> getShapeProperty() {
        return SHAPE;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(SHAPE, POWERED, REVERSED, WATERLOGGED);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction look = context.getHorizontalDirection();
        return super.getStateForPlacement(context)
                .setValue(REVERSED, look == Direction.NORTH || look == Direction.WEST);
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (!level.isClientSide()) {
            refreshPowered(level, pos);
            if (ElectricPower.isAvailable()) level.scheduleTick(pos, this, ElectricPower.CHECK_TICKS);
        }
    }

    @Override
    protected void updateState(BlockState state, Level level, BlockPos pos, Block block) {
        refreshPowered(level, pos);
    }

    /** A cable's voltage changes without any neighbour update, so the lit look has to be kept up by hand. */
    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!ElectricPower.isAvailable()) return;
        refreshPowered(level, pos);
        level.scheduleTick(pos, this, ElectricPower.CHECK_TICKS);
    }

    private static void refreshPowered(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof DriveRailBlock)) return;
        DriveInput input = DriveInput.read(level, pos);
        boolean powered = input.speed() > 0 && !input.brake();
        if (state.getValue(POWERED) != powered) {
            level.setBlock(pos, state.setValue(POWERED, powered), Block.UPDATE_ALL);
        }
    }

    /** Sneak + empty-hand right-click flips the departure direction. */
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                               Player player, BlockHitResult hit) {
        if (!player.isShiftKeyDown()) return InteractionResult.PASS;
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        BlockState flipped = state.cycle(REVERSED);
        level.setBlock(pos, flipped, Block.UPDATE_ALL);
        Vec3 heading = heading(flipped, false);
        player.sendOverlayMessage(Component.translatable("message.redstone_transportation.drive_rail_heading",
                BlockMessages.directionName(Direction.getApproximateNearest(heading.x, 0.0D, heading.z))));
        return InteractionResult.SUCCESS;
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(SHAPE, rotate(state.getValue(SHAPE), rotation));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.setValue(SHAPE, mirror(state.getValue(SHAPE), mirror));
    }
}
