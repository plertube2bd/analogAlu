package com.example.redstonetransportation.block;

import com.example.redstonetransportation.content.ModContent;
import com.example.redstonetransportation.util.RedstoneSignals;
import com.example.redstonetransportation.vehicle.ModEntityTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.minecart.AbstractMinecart;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseRailBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Sends out a tram taken from a container beside it or from the player who asks, by hand or on a
 * rising Analog ALU/redstone command. Its signal comes on when someone boards a cart standing here.
 */
public class VehicleStationBlock extends Block {
    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final IntegerProperty POWER = BlockStateProperties.POWER;
    public static final BooleanProperty ACTIVE = BooleanProperty.create("active");
    /** Latches a high auto-dispatch command so one sustained ALU output spawns only one tram. */
    public static final BooleanProperty AUTO_TRIGGERED = BooleanProperty.create("auto_triggered");
    /** Whether a cart here had someone aboard at the last look, which tells a new rider from an old one. */
    public static final BooleanProperty RIDDEN = BooleanProperty.create("ridden");
    /** Slower than any cart under way; a cart being boarded is at rest. */
    private static final double STANDING_SPEED = 0.1D;

    public VehicleStationBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(POWER, 0)
                .setValue(ACTIVE, false)
                .setValue(AUTO_TRIGGERED, false)
                .setValue(RIDDEN, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, POWER, ACTIVE, AUTO_TRIGGERED, RIDDEN);
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

    /** The signal means boarding, so this is how a circuit tells that a cart is here at all. */
    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos, Direction direction) {
        return state.getValue(ACTIVE) ? 15 : 0;
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos,
                           BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (!level.isClientSide() && oldState.getBlock() != this) level.scheduleTick(pos, this, 5);
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos,
                                   Block sourceBlock, Orientation orientation, boolean movedByPiston) {
        if (!level.isClientSide()) level.scheduleTick(pos, this, 1);
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        AABB area = new AABB(pos).inflate(1.2, 0.7, 1.2);
        List<AbstractMinecart> carts = level.getEntitiesOfClass(AbstractMinecart.class, area);
        boolean cartPresent = !carts.isEmpty();
        boolean ridden = carts.stream().anyMatch(AbstractMinecart::isVehicle);
        // A rider who comes in with the cart has not boarded: a train must be able to stop here.
        boolean boarded = !state.getValue(RIDDEN) && state.getValue(ACTIVE) && carts.stream()
                .anyMatch(cart -> cart.isVehicle() && cart.getDeltaMovement().horizontalDistance() < STANDING_SPEED);
        int nextPower = ridden && (boarded || state.getValue(POWER) > 0) ? 15 : 0;

        // The station's arrival output is excluded, preventing its own signal from retriggering it.
        int dispatchSignal = RedstoneSignals.readInputExceptOutput(
                level, pos, state.getValue(FACING));
        boolean commandHigh = dispatchSignal > 0;
        boolean risingEdge = commandHigh && !state.getValue(AUTO_TRIGGERED);

        BlockState updated = state
                .setValue(POWER, nextPower)
                .setValue(ACTIVE, cartPresent)
                .setValue(AUTO_TRIGGERED, commandHigh)
                .setValue(RIDDEN, ridden);
        if (!updated.equals(state)) {
            level.setBlock(pos, updated, 3);
            if (state.getValue(POWER) != nextPower) RedstoneSignals.notifyAllNeighbors(level, pos, this);
        }

        if (risingEdge) dispatchTram(level, pos, updated, null);
        level.scheduleTick(pos, this, 5);
    }

    /** Empty-hand right-click is manual dispatch; Shift + right-click rotates the station output direction. */
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                               Player player, BlockHitResult hit) {
        if (player.isShiftKeyDown()) {
            if (level.isClientSide()) return InteractionResult.SUCCESS;
            Direction next = state.getValue(FACING).getClockWise();
            level.setBlock(pos, state.setValue(FACING, next), 3);
            level.scheduleTick(pos, this, 1);
            player.sendOverlayMessage(Component.literal("출발 방향: " + directionName(next)));
            return InteractionResult.SUCCESS;
        }
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        return dispatchTram(level, pos, state, player) ? InteractionResult.SUCCESS : InteractionResult.FAIL;
    }

    private static boolean dispatchTram(Level level, BlockPos pos, BlockState state, Player player) {
        if (!(level instanceof ServerLevel serverLevel)) return false;

        BlockPos railPos = findNearbyRail(level, pos);
        if (railPos == null) {
            if (player != null) player.sendOverlayMessage(
                    Component.translatable("message.redstone_transportation.station_no_rail"));
            return false;
        }

        AABB dispatchArea = new AABB(pos).inflate(8.0, 3.0, 8.0);
        boolean alreadyDispatched = !level.getEntitiesOfClass(AbstractMinecart.class, dispatchArea).isEmpty();
        if (alreadyDispatched) {
            if (player != null) player.sendOverlayMessage(
                    Component.translatable("message.redstone_transportation.station_occupied_tram"));
            return false;
        }

        EntityType<? extends AbstractMinecart> type = takeTram(level, pos, player);
        if (type == null) {
            if (player != null) player.sendOverlayMessage(
                    Component.translatable("message.redstone_transportation.station_no_tram"));
            return false;
        }
        AbstractMinecart tram = type.create(level, EntitySpawnReason.TRIGGERED);
        if (tram == null) return false;

        tram.setPos(railPos.getX() + 0.5, railPos.getY() + 0.12, railPos.getZ() + 0.5);
        Direction facing = state.getValue(FACING);
        tram.setYRot(facing.toYRot());
        tram.setDeltaMovement(new Vec3(facing.getStepX() * 0.02, 0.0, facing.getStepZ() * 0.02));
        serverLevel.addFreshEntity(tram);

        if (player != null) player.sendOverlayMessage(
                Component.translatable("message.redstone_transportation.station_dispatched_tram"));
        return true;
    }

    /** The station's own stock comes first, and only then what the player who asked is carrying. */
    private static EntityType<? extends AbstractMinecart> takeTram(Level level, BlockPos pos, Player player) {
        for (Direction side : Direction.values()) {
            Container stock = HopperBlockEntity.getContainerAt(level, pos.relative(side));
            EntityType<? extends AbstractMinecart> type = stock != null ? takeTram(stock, true) : null;
            if (type != null) return type;
        }
        return player != null ? takeTram(player.getInventory(), !player.hasInfiniteMaterials()) : null;
    }

    private static EntityType<? extends AbstractMinecart> takeTram(Container container, boolean use) {
        for (int slot = 0; slot < container.getContainerSize(); slot++) {
            EntityType<? extends AbstractMinecart> type = tramFor(container.getItem(slot));
            if (type != null) {
                if (use) container.removeItem(slot, 1);
                return type;
            }
        }
        return null;
    }

    private static EntityType<? extends AbstractMinecart> tramFor(ItemStack stack) {
        if (stack.is(ModContent.REDSTONE_TRAM_ITEM)) return ModEntityTypes.REDSTONE_TRAM;
        if (stack.is(ModContent.CARGO_TRAM_ITEM)) return ModEntityTypes.CARGO_TRAM;
        return null;
    }

    private static BlockPos findNearbyRail(Level level, BlockPos center) {
        BlockPos best = null;
        double bestDistance = Double.MAX_VALUE;
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                for (int dy = -1; dy <= 2; dy++) {
                    BlockPos candidate = center.offset(dx, dy, dz);
                    if (!BaseRailBlock.isRail(level, candidate)) continue;
                    double distance = candidate.distSqr(center);
                    if (distance < bestDistance) {
                        best = candidate;
                        bestDistance = distance;
                    }
                }
            }
        }
        return best;
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
