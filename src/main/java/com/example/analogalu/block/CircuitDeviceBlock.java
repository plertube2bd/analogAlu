package com.example.analogalu.block;

import com.example.analogalu.ModBlocks;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.ChatFormatting;
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
    public enum Kind { BATTERY, COAL_GENERATOR, DIESEL_GENERATOR, WIND_GENERATOR, SOLAR_GENERATOR, SWITCH, RESISTOR, MINER }

    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty CLOSED = BooleanProperty.create("closed");
    public static final double BATTERY_VOLTAGE = 9.0;
    public static final double BATTERY_INTERNAL_RESISTANCE = 0.15;
    public static final double LOAD_RESISTANCE = 10.0;
    public static final double MINER_RESISTANCE = 3.0;
    public static final double MINER_REQUIRED_VOLTAGE = 4.0;
    public static final double BATTERY_CAPACITY_JOULES = 36_000.0;
    public static final double COAL_GENERATOR_VOLTAGE = 12.0;
    public static final double COAL_GENERATOR_RESISTANCE = 0.8;
    public static final double DIESEL_GENERATOR_VOLTAGE = 12.0;
    public static final double DIESEL_GENERATOR_RESISTANCE = 0.5;
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

    /** 스위치만 우클릭으로 토글한다. 나머지 장치의 상태는 조준했을 때 뜨는 툴팁 창(HoverInfo)으로 확인한다. */
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
            BlockHitResult hit) {
        if (kind != Kind.SWITCH) return InteractionResult.PASS;
        if (!player.getAbilities().mayBuild) return InteractionResult.PASS;
        if (level.isClientSide()) return InteractionResult.SUCCESS;

        boolean closed = !state.getValue(CLOSED);
        level.setBlock(pos, state.setValue(CLOSED, closed), Block.UPDATE_ALL);
        level.playSound(null, pos, SoundEvents.LEVER_CLICK, SoundSource.BLOCKS, 0.4F, closed ? 0.6F : 0.5F);
        CircuitSolver.recomputeAround(level, pos);
        return InteractionResult.SUCCESS;
    }

    /** 툴팁 창에 보여줄 줄들. 첫 줄은 제목. 클라이언트에서도 호출된다. */
    public static List<Component> hoverLines(Level level, BlockPos pos, BlockState state,
            CircuitDeviceBlockEntity device) {
        Kind kind = ((CircuitDeviceBlock) state.getBlock()).kind();
        Direction positive = state.getValue(FACING);
        List<Component> lines = new ArrayList<>();
        switch (kind) {
            case BATTERY -> {
                double joules = device.getStoredEnergyJoules();
                lines.add(Component.literal("Battery").withStyle(ChatFormatting.GOLD));
                lines.add(CopperCableBlockEntity.line("Voltage", String.format(Locale.ROOT, "%.1f V", BATTERY_VOLTAGE),
                        ChatFormatting.YELLOW));
                lines.add(CopperCableBlockEntity.line("Energy", String.format(Locale.ROOT, "%.0f / %.0f J (%.0f%%)",
                        joules, BATTERY_CAPACITY_JOULES, 100.0 * joules / BATTERY_CAPACITY_JOULES),
                        joules < BATTERY_CAPACITY_JOULES * 0.2 ? ChatFormatting.RED : ChatFormatting.GREEN));
                lines.add(CopperCableBlockEntity.line("Terminals", "+" + positive.getName() + " / -"
                        + positive.getOpposite().getName(), ChatFormatting.WHITE));
            }
            case SWITCH -> {
                boolean closed = state.getValue(CLOSED);
                lines.add(Component.literal("Circuit Switch").withStyle(ChatFormatting.GOLD));
                lines.add(CopperCableBlockEntity.line("State", closed ? "CLOSED (conducting)" : "OPEN",
                        closed ? ChatFormatting.GREEN : ChatFormatting.RED));
                lines.add(Component.literal("Right-click to toggle").withStyle(ChatFormatting.DARK_GRAY));
            }
            case RESISTOR -> {
                lines.add(Component.literal("Resistive Load").withStyle(ChatFormatting.GOLD));
                lines.add(CopperCableBlockEntity.line("Resistance", String.format(Locale.ROOT, "%.1f \u03a9",
                        LOAD_RESISTANCE), ChatFormatting.WHITE));
            }
            case COAL_GENERATOR -> {
                int fuelTicks = device.getFuelTicks();
                lines.add(Component.literal("Coal Generator").withStyle(ChatFormatting.GOLD));
                lines.add(CopperCableBlockEntity.line("Voltage", String.format(Locale.ROOT, "%.1f V",
                        COAL_GENERATOR_VOLTAGE), ChatFormatting.YELLOW));
                lines.add(CopperCableBlockEntity.line("Fuel", String.format(Locale.ROOT, "%.0f s", fuelTicks / 20.0),
                        fuelTicks > 0 ? ChatFormatting.GREEN : ChatFormatting.RED));
                lines.add(Component.literal("Drop coal on top to refuel").withStyle(ChatFormatting.DARK_GRAY));
            }
            case DIESEL_GENERATOR -> {
                int fuelTicks = device.getFuelTicks();
                lines.add(Component.literal("Diesel Generator").withStyle(ChatFormatting.GOLD));
                lines.add(CopperCableBlockEntity.line("Voltage", String.format(Locale.ROOT, "%.1f V",
                        DIESEL_GENERATOR_VOLTAGE), ChatFormatting.YELLOW));
                lines.add(CopperCableBlockEntity.line("Fuel", String.format(Locale.ROOT, "%.0f s", fuelTicks / 20.0),
                        fuelTicks > 0 ? ChatFormatting.GREEN : ChatFormatting.RED));
                lines.add(Component.literal("Drop diesel on top to refuel").withStyle(ChatFormatting.DARK_GRAY));
            }
            case MINER -> {
                lines.add(Component.literal("Miner").withStyle(ChatFormatting.GOLD));
                lines.add(CopperCableBlockEntity.line("Resistance", String.format(Locale.ROOT, "%.1f \\u03a9",
                        MINER_RESISTANCE), ChatFormatting.WHITE));
                lines.add(CopperCableBlockEntity.line("Load voltage", String.format(Locale.ROOT, "%.2f V",
                        device.getLoadVoltage()), device.getLoadVoltage() >= MINER_REQUIRED_VOLTAGE
                                ? ChatFormatting.GREEN : ChatFormatting.RED));
                lines.add(Component.literal("Requires redstone power").withStyle(ChatFormatting.DARK_GRAY));
            }
            case WIND_GENERATOR, SOLAR_GENERATOR -> {
                boolean wind = kind == Kind.WIND_GENERATOR;
                boolean producing = hasNaturalInput(level, pos, kind);
                lines.add(Component.literal(wind ? "Wind Generator" : "Solar Generator").withStyle(ChatFormatting.GOLD));
                lines.add(CopperCableBlockEntity.line("Status", producing ? "producing" : "idle",
                        producing ? ChatFormatting.GREEN : ChatFormatting.RED));
                if (!producing) {
                    lines.add(Component.literal(wind ? "Place outdoors above Y=80" : "Needs daylight and open sky")
                            .withStyle(ChatFormatting.DARK_GRAY));
                }
            }
        }
        return lines;
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
            case DIESEL_GENERATOR -> DIESEL_GENERATOR_VOLTAGE;
            case WIND_GENERATOR -> Math.min(12.0, 6.0 + Math.max(0, pos.getY() - 80) * 0.05);
            case SOLAR_GENERATOR -> SOLAR_GENERATOR_VOLTAGE;
            default -> 0.0;
        };
    }

    public static double sourceResistance(Kind kind) {
        return switch (kind) {
            case BATTERY -> BATTERY_INTERNAL_RESISTANCE;
            case COAL_GENERATOR -> COAL_GENERATOR_RESISTANCE;
            case DIESEL_GENERATOR -> DIESEL_GENERATOR_RESISTANCE;
            case WIND_GENERATOR -> WIND_GENERATOR_RESISTANCE;
            case SOLAR_GENERATOR -> SOLAR_GENERATOR_RESISTANCE;
            default -> 0.0;
        };
    }
}