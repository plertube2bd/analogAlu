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
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.BlockHitResult;

public class CircuitDeviceBlock extends BaseEntityBlock {
    public enum Kind { BATTERY, COAL_GENERATOR, WIND_GENERATOR, SOLAR_GENERATOR, SWITCH, RESISTOR }

    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty CLOSED = BooleanProperty.create("closed");
    public static final double BATTERY_VOLTAGE = 9.0;
    public static final double BATTERY_INTERNAL_RESISTANCE = 0.15;
    public static final double LOAD_RESISTANCE = 10.0;
    public static final double BATTERY_CAPACITY_JOULES = 36_000.0;
    public static final double COAL_GENERATOR_VOLTAGE = 12.0;
    public static final double COAL_GENERATOR_RESISTANCE = 0.8;
    public static final double WIND_GENERATOR_RESISTANCE = 2.0;
    public static final double SOLAR_GENERATOR_VOLTAGE = 9.0;
    public static final double SOLAR_GENERATOR_RESISTANCE = 1.5;
    private final Kind kind;

    public CircuitDeviceBlock(BlockBehaviour.Properties properties, Kind kind) {
        super(properties);
        this.kind = kind;
        registerDefaultState(defaultBlockState().setValue(FACING, Direction.NORTH).setValue(CLOSED, false));
    }

    public Kind kind() {
        return kind;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, CLOSED);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite())
                .setValue(CLOSED, false);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CircuitDeviceBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
            BlockEntityType<T> type) {
        return level.isClientSide() ? null
            : createTickerHelper(type, ModBlocks.CIRCUIT_DEVICE_ENTITY, CircuitDeviceBlockEntity::tick);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState,
            boolean movedByPiston) {
        if (!level.isClientSide() && !oldState.is(this)) CircuitSolver.recomputeAround(level, pos);
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block block,
            Orientation orientation, boolean movedByPiston) {
        if (!level.isClientSide()) CircuitSolver.recomputeAround(level, pos);
    }

    @Override
    protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos,
            boolean movedByPiston) {
        if (movedByPiston) return;
        CircuitSolver.recomputeAround(level, pos);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
            BlockHitResult hit) {
        if (!player.getAbilities().mayBuild) return InteractionResult.PASS;
        if (level.isClientSide()) return InteractionResult.SUCCESS;

        if (kind == Kind.SWITCH) {
            boolean closed = !state.getValue(CLOSED);
            level.setBlock(pos, state.setValue(CLOSED, closed), Block.UPDATE_ALL);
            level.playSound(null, pos, SoundEvents.LEVER_CLICK, SoundSource.BLOCKS, 0.4F, closed ? 0.6F : 0.5F);
            CircuitSolver.recomputeAround(level, pos);
            player.sendSystemMessage(Component.literal(closed ? "Circuit switch: CLOSED" : "Circuit switch: OPEN"));
        } else if (kind == Kind.BATTERY) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            double joules = blockEntity instanceof CircuitDeviceBlockEntity battery
                    ? battery.getStoredEnergyJoules() : 0.0;
            Direction positive = state.getValue(FACING);
            player.sendSystemMessage(Component.literal(String.format(java.util.Locale.ROOT,
                "Battery: %.1f V | +%s / -%s | %.0f / %.0f J", BATTERY_VOLTAGE, positive.getName(),
                positive.getOpposite().getName(), joules, BATTERY_CAPACITY_JOULES)));
            } else if (kind == Kind.COAL_GENERATOR) {
                BlockEntity blockEntity = level.getBlockEntity(pos);
                int fuelTicks = blockEntity instanceof CircuitDeviceBlockEntity generator
                    ? generator.getFuelTicks() : 0;
                player.sendSystemMessage(Component.literal(String.format(java.util.Locale.ROOT,
                    "Coal generator: %.1f V | fuel %.0f s (drop coal on top)", COAL_GENERATOR_VOLTAGE,
                    fuelTicks / 20.0)));
            } else if (kind == Kind.WIND_GENERATOR || kind == Kind.SOLAR_GENERATOR) {
                boolean producing = hasNaturalInput(level, pos, kind);
                player.sendSystemMessage(Component.literal(kind == Kind.WIND_GENERATOR
                    ? (producing ? "Wind generator: producing" : "Wind generator: place outdoors above Y=80")
                    : (producing ? "Solar generator: producing" : "Solar generator: needs daylight and open sky")));
        } else {
            player.sendSystemMessage(Component.literal(String.format(java.util.Locale.ROOT,
                    "Resistive load: %.1f Ω", LOAD_RESISTANCE)));
        }
        return InteractionResult.SUCCESS;
    }

    public static boolean hasNaturalInput(Level level, BlockPos pos, Kind kind) {
        boolean openSky = level.canSeeSky(pos.above());
        if (kind == Kind.WIND_GENERATOR) return openSky && pos.getY() >= 80;
        if (kind == Kind.SOLAR_GENERATOR) {
            return openSky && level.dimensionType().hasSkyLight()
                    && Math.floorMod(level.getOverworldClockTime(), 24_000L) < 12_000L;
        }
        return true;
    }

    public static double sourceVoltage(Level level, BlockPos pos, Kind kind) {
        return switch (kind) {
            case BATTERY -> BATTERY_VOLTAGE;
            case COAL_GENERATOR -> COAL_GENERATOR_VOLTAGE;
            case WIND_GENERATOR -> Math.min(12.0, 6.0 + Math.max(0, pos.getY() - 80) * 0.05);
            case SOLAR_GENERATOR -> SOLAR_GENERATOR_VOLTAGE;
            default -> 0.0;
        };
    }

    public static double sourceResistance(Kind kind) {
        return switch (kind) {
            case BATTERY -> BATTERY_INTERNAL_RESISTANCE;
            case COAL_GENERATOR -> COAL_GENERATOR_RESISTANCE;
            case WIND_GENERATOR -> WIND_GENERATOR_RESISTANCE;
            case SOLAR_GENERATOR -> SOLAR_GENERATOR_RESISTANCE;
            default -> 0.0;
        };
    }
}