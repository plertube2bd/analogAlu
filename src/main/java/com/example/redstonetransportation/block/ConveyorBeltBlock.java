package com.example.redstonetransportation.block;

import com.example.redstonetransportation.integration.analogalu.AnalogAluSignalAdapter;
import com.example.redstonetransportation.integration.analogalu.ElectricPower;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Container;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.EntityCollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * Carries items and entities in its FACING direction and feeds items straight into a container
 * at its end. It runs by default and any signal stops it, so a circuit can gate the flow; where
 * Analog ALU has electricity, a line instead runs only while a live cable touches one of its belts.
 * Like a rail, it takes its shape from its neighbours: fed from the side it bends, and with the
 * next belt a block higher or lower it slopes.
 */
public final class ConveyorBeltBlock extends Block {
    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;
    public static final EnumProperty<ConveyorShape> SHAPE = EnumProperty.create("shape", ConveyorShape.class);

    private static final VoxelShape FLAT = Block.column(16.0D, 0.0D, 4.0D);
    private static final VoxelShape FLAT_INSIDE = Block.column(16.0D, 0.0D, 5.0D);
    /** Steps rather than a slope, so anything that walks can climb it. Keyed by the way it rises. */
    private static final Map<Direction, VoxelShape> RAMPS = Shapes.rotateHorizontal(Shapes.or(
            Block.box(0.0D, 0.0D, 12.0D, 16.0D, 4.0D, 16.0D),
            Block.box(0.0D, 0.0D, 8.0D, 16.0D, 8.0D, 12.0D),
            Block.box(0.0D, 0.0D, 4.0D, 16.0D, 12.0D, 8.0D),
            Block.box(0.0D, 0.0D, 0.0D, 16.0D, 16.0D, 4.0D)));
    private static final double BELT_SPEED = 0.14D;
    private static final double CENTERING = 0.05D;
    private static final double END_REACH = 0.3D;
    private static final double CURVE_RADIUS = 0.5D;
    private static final double SURFACE = 0.25D;
    private static final double MOST_CLIMB = 0.3D;
    private static final int LONGEST_POWERED_LINE = 256;
    /** One look along a line per tick, however many of its belts ask. */
    private static final Map<Level, LineCheck> LINE_CHECKS = new WeakHashMap<>();

    private record LineCheck(long tick, Map<BlockPos, Boolean> lines) {}

    public ConveyorBeltBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(POWERED, false)
                .setValue(SHAPE, ConveyorShape.STRAIGHT));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, POWERED, SHAPE);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        Direction facing = context.getHorizontalDirection();
        return defaultBlockState()
                .setValue(FACING, facing)
                .setValue(SHAPE, shapeAt(level, pos, facing))
                .setValue(POWERED, ElectricPower.isAvailable() || level.hasNeighborSignal(pos));
    }

    /** A belt laid on from the open end of a line turns that end towards it, as a rail bends to the next one. */
    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity by, ItemStack itemStack) {
        if (level.isClientSide()) return;
        Direction facing = state.getValue(FACING);
        BlockPos behind = pos.relative(facing.getOpposite());
        BlockState end = level.getBlockState(behind);
        if (isBelt(end) && end.getValue(FACING).getAxis() != facing.getAxis() && isOpenEnd(level, behind, end.getValue(FACING))) {
            level.setBlock(behind, end.setValue(FACING, facing), Block.UPDATE_ALL);
        }
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        if (!oldState.is(this) || oldState.getValue(FACING) != state.getValue(FACING)) reshapeAround(level, pos);
        if (!level.isClientSide() && !oldState.is(this) && ElectricPower.isAvailable()) checkPowerSoon(level, pos, this);
    }

    /** The whole line looks on the same tick, so that it can share one walk along its belts. */
    private static void checkPowerSoon(Level level, BlockPos pos, Block block) {
        int check = ElectricPower.CHECK_TICKS;
        level.scheduleTick(pos, block, check - (int) (level.getGameTime() % check));
    }

    /** A cable's voltage changes without any neighbour update, so the line has to keep looking. */
    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!ElectricPower.isAvailable()) return;
        refreshStopped(level, pos, state);
        checkPowerSoon(level, pos, this);
    }

    private static void refreshStopped(Level level, BlockPos pos, BlockState state) {
        boolean stopped = ElectricPower.isAvailable()
                ? !isLineLive(level, pos)
                : AnalogAluSignalAdapter.readBestNeighborSignal(level, pos) > 0;
        if (state.getValue(POWERED) != stopped) level.setBlock(pos, state.setValue(POWERED, stopped), Block.UPDATE_ALL);
    }

    private static boolean isLineLive(Level level, BlockPos start) {
        LineCheck check = LINE_CHECKS.get(level);
        if (check == null || check.tick() != level.getGameTime()) {
            check = new LineCheck(level.getGameTime(), new HashMap<>());
            LINE_CHECKS.put(level, check);
        }
        Boolean known = check.lines().get(start);
        if (known != null) return known;

        Set<BlockPos> line = new HashSet<>();
        ArrayDeque<BlockPos> next = new ArrayDeque<>();
        next.add(start);
        boolean live = false;
        while (!next.isEmpty() && line.size() < LONGEST_POWERED_LINE) {
            BlockPos pos = next.poll();
            if (!line.add(pos)) continue;
            live |= ElectricPower.input(level, pos) > 0;
            addJoinedBelts(level, pos, next);
        }
        for (BlockPos pos : line) check.lines().put(pos, live);
        return live;
    }

    /** The belts this one hands on to or takes from, on its level or a slope's step away. */
    private static void addJoinedBelts(Level level, BlockPos pos, ArrayDeque<BlockPos> next) {
        Direction facing = level.getBlockState(pos).getValue(FACING);
        for (Direction side : Direction.Plane.HORIZONTAL) {
            BlockPos beside = pos.relative(side);
            for (BlockPos other : new BlockPos[] {beside, beside.above(), beside.below()}) {
                BlockState state = level.getBlockState(other);
                if (isBelt(state) && (side == facing || state.getValue(FACING) == side.getOpposite())) next.add(other);
            }
        }
    }

    @Override
    protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
        reshapeAround(level, pos);
    }

    /** A slope depends on belts a block up and across, which ordinary neighbour updates never reach. */
    private static void reshapeAround(Level level, BlockPos pos) {
        reshape(level, pos);
        for (Direction side : Direction.Plane.HORIZONTAL) {
            BlockPos beside = pos.relative(side);
            reshape(level, beside);
            reshape(level, beside.above());
            reshape(level, beside.below());
        }
    }

    private static void reshape(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (!isBelt(state)) return;
        ConveyorShape shape = shapeAt(level, pos, state.getValue(FACING));
        if (shape != state.getValue(SHAPE)) level.setBlock(pos, state.setValue(SHAPE, shape), Block.UPDATE_CLIENTS);
    }

    private static ConveyorShape shapeAt(Level level, BlockPos pos, Direction facing) {
        BlockPos ahead = pos.relative(facing);
        BlockPos behind = pos.relative(facing.getOpposite());
        boolean roofed = isBelt(level.getBlockState(pos.above()));

        BlockState aheadAbove = level.getBlockState(ahead.above());
        if (!roofed && !isBelt(level.getBlockState(ahead)) && isBelt(aheadAbove) && aheadAbove.getValue(FACING) != facing.getOpposite()) {
            return ConveyorShape.UP;
        }
        if (!roofed && !isBelt(level.getBlockState(behind)) && headsOver(level.getBlockState(behind.above()), facing)) {
            return ConveyorShape.DOWN;
        }

        if (headsOver(level.getBlockState(behind), facing) || headsOver(level.getBlockState(behind.below()), facing)) {
            return ConveyorShape.STRAIGHT;
        }
        boolean left = isFedFrom(level, pos, facing.getCounterClockWise());
        boolean right = isFedFrom(level, pos, facing.getClockWise());
        if (left && right) return ConveyorShape.BOTH;
        if (left) return ConveyorShape.LEFT;
        return right ? ConveyorShape.RIGHT : ConveyorShape.STRAIGHT;
    }

    private static boolean isFedFrom(Level level, BlockPos pos, Direction side) {
        BlockPos beside = pos.relative(side);
        return headsOver(level.getBlockState(beside), side.getOpposite())
                || headsOver(level.getBlockState(beside.below()), side.getOpposite());
    }

    private static boolean headsOver(BlockState state, Direction facing) {
        return isBelt(state) && state.getValue(FACING) == facing;
    }

    private static boolean isBelt(BlockState state) {
        return state.getBlock() instanceof ConveyorBeltBlock;
    }

    /** True when a belt delivers to nothing, so it is free to turn. */
    private static boolean isOpenEnd(Level level, BlockPos pos, Direction facing) {
        BlockPos ahead = pos.relative(facing);
        return !isBelt(level.getBlockState(ahead))
                && !isBelt(level.getBlockState(ahead.above()))
                && !isBelt(level.getBlockState(ahead.below()))
                && HopperBlockEntity.getContainerAt(level, ahead) == null;
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        if (mirror == Mirror.NONE) return state;
        ConveyorShape shape = switch (state.getValue(SHAPE)) {
            case LEFT -> ConveyorShape.RIGHT;
            case RIGHT -> ConveyorShape.LEFT;
            default -> state.getValue(SHAPE);
        };
        return state.setValue(FACING, mirror.mirror(state.getValue(FACING))).setValue(SHAPE, shape);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        Direction facing = state.getValue(FACING);
        return switch (state.getValue(SHAPE)) {
            case UP -> RAMPS.get(facing);
            case DOWN -> RAMPS.get(facing.getOpposite());
            default -> FLAT;
        };
    }

    /** An item has no step to climb with, so on a slope {@link #climb} carries it along the surface instead. */
    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        boolean item = context instanceof EntityCollisionContext entityContext && entityContext.getEntity() instanceof ItemEntity;
        return item && state.getValue(SHAPE).isSlope() ? Shapes.empty() : getShape(state, level, pos, context);
    }

    /** Taller than the belt surface so anything resting on it counts as inside. */
    @Override
    protected VoxelShape getEntityInsideCollisionShape(BlockState state, BlockGetter level, BlockPos pos, Entity entity) {
        return state.getValue(SHAPE).isSlope() ? Shapes.block() : FLAT_INSIDE;
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos,
                                   Block sourceBlock, Orientation orientation, boolean movedByPiston) {
        if (level.isClientSide()) return;
        refreshStopped(level, pos, state);
    }

    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity,
                                InsideBlockEffectApplier effectApplier, boolean isPrecise) {
        // Belts laid before there were cables have nothing telling them to look.
        if (!level.isClientSide() && ElectricPower.isAvailable() && !level.getBlockTicks().hasScheduledTick(pos, this)) {
            checkPowerSoon(level, pos, this);
        }
        ConveyorShape shape = state.getValue(SHAPE);
        boolean stopped = state.getValue(POWERED);
        boolean carriedUpSlope = shape.isSlope() && entity instanceof ItemEntity;
        if (stopped && !carriedUpSlope) return;

        Direction facing = state.getValue(FACING);
        if (entity instanceof ItemEntity item && !stopped && !level.isClientSide() && isAtEnd(entity, pos, facing)
                && feedContainer(level, pos.relative(facing), item)) {
            return;
        }

        Vec3 fromCenter = entity.position().subtract(Vec3.atBottomCenterOf(pos));
        Direction left = facing.getCounterClockWise();
        Direction right = facing.getClockWise();
        Vec3 flow = stopped ? Vec3.ZERO : switch (shape) {
            case LEFT -> roundCorner(fromCenter, facing, left);
            case RIGHT -> roundCorner(fromCenter, facing, right);
            // Whichever side it came in on, it takes that side's bend to the middle of the exit.
            case BOTH -> roundCorner(fromCenter, facing, fromCenter.dot(flat(left)) > 0.0D ? left : right);
            default -> alongBelt(fromCenter, facing);
        };
        double rise = carriedUpSlope ? climb(entity, pos, shape, fromCenter.add(flow), facing) : entity.getDeltaMovement().y;
        entity.setDeltaMovement(flow.x, rise, flow.z);
    }

    /** Steers toward the belt's centre line so items do not slide off the sides. */
    private static Vec3 alongBelt(Vec3 fromCenter, Direction facing) {
        double sideways = facing.getAxis() == Direction.Axis.X ? -fromCenter.z : -fromCenter.x;
        double correction = Math.clamp(sideways, -CENTERING, CENTERING);
        return new Vec3(
                facing.getStepX() * BELT_SPEED + (facing.getAxis() == Direction.Axis.Z ? correction : 0.0D),
                0.0D,
                facing.getStepZ() * BELT_SPEED + (facing.getAxis() == Direction.Axis.X ? correction : 0.0D));
    }

    /** Follows a quarter circle round the inner corner, which is where the entry and exit sides meet. */
    private static Vec3 roundCorner(Vec3 fromCenter, Direction facing, Direction entry) {
        Vec3 out = flat(facing);
        Vec3 in = flat(entry);
        Vec3 radius = new Vec3(fromCenter.x, 0.0D, fromCenter.z).subtract(out.add(in).scale(CURVE_RADIUS));
        double distance = radius.length();
        if (distance < 1.0E-3D) return out.scale(BELT_SPEED);

        Vec3 tangent = new Vec3(-radius.z, 0.0D, radius.x).scale(1.0D / distance);
        if (tangent.dot(out.subtract(in)) < 0.0D) tangent = tangent.reverse();
        double correction = Math.clamp(CURVE_RADIUS - distance, -CENTERING, CENTERING);
        return tangent.scale(BELT_SPEED).add(radius.scale(correction / distance));
    }

    /** The upward speed that keeps an item on a slope's surface at the point it is heading for. */
    private static double climb(Entity entity, BlockPos pos, ConveyorShape shape, Vec3 heading, Direction facing) {
        double along = heading.dot(flat(facing)) + 0.5D;
        double surface = SURFACE + (shape == ConveyorShape.UP ? along : 1.0D - along);
        return Math.clamp(pos.getY() + surface - entity.getY() + entity.getGravity(), -MOST_CLIMB, MOST_CLIMB);
    }

    private static Vec3 flat(Direction direction) {
        return new Vec3(direction.getStepX(), 0.0D, direction.getStepZ());
    }

    private static boolean isAtEnd(Entity entity, BlockPos pos, Direction facing) {
        Vec3 fromCenter = entity.position().subtract(Vec3.atBottomCenterOf(pos));
        return fromCenter.dot(flat(facing)) > END_REACH;
    }

    private static boolean feedContainer(Level level, BlockPos targetPos, ItemEntity item) {
        Container target = HopperBlockEntity.getContainerAt(level, targetPos);
        return target != null && HopperBlockEntity.addItem(target, item);
    }
}
