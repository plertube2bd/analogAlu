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
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
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
    public enum Kind { BATTERY, RECHARGEABLE_BATTERY, COAL_GENERATOR, DIESEL_GENERATOR, WIND_GENERATOR, SOLAR_GENERATOR, SWITCH, RESISTOR, MINER, STEP_UP, STEP_DOWN }

    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty CLOSED = BooleanProperty.create("closed");
    public static final double BATTERY_VOLTAGE = 9.0;
    public static final double BATTERY_INTERNAL_RESISTANCE = 0.15;
    public static final double LOAD_RESISTANCE = 10.0;
    public static final double MINER_RESISTANCE = 3.0;
    public static final double MINER_REQUIRED_VOLTAGE = 4.0;
    public static final double BATTERY_CAPACITY_JOULES = 36_000.0;
    public static final double RECHARGEABLE_VOLTAGE = 12.0;
    public static final double RECHARGEABLE_INTERNAL_RESISTANCE = 0.2;
    public static final double RECHARGEABLE_CAPACITY_JOULES = 72_000.0;
    /** 다른 전원에 연결되어 충전될 때 충전식 건전지가 갖는 저항. 여기서 소비되는 전력이 그대로 저장된다. */
    public static final double RECHARGE_RESISTANCE = 4.0;
    /** 승압기 설정 전압(V). 입력이 설정값보다 높으면 입력 전압을 그대로 내보낸다. */
    private static final double[] STEP_UP_SETPOINTS = {24, 48, 120, 240, 480, 1000, 2000, 3000};
    /** 변압기 설정 전압(V). 입력이 설정값보다 낮으면 입력 전압을 그대로 내보낸다. */
    private static final double[] STEP_DOWN_SETPOINTS = {4, 5, 6, 9, 12, 24, 48, 120, 240, 480, 1000};
    public static final int STEP_DOWN_DEFAULT_INDEX = 4;
    public static final double STEP_UP_MAX_VOLTAGE = 3000.0;
    public static final double CONVERTER_EFFICIENCY = 0.9;
    public static final double CONVERTER_OUTPUT_RESISTANCE = 0.5;
    public static final double CONVERTER_IDLE_RESISTANCE = 1.0e6;
    public static final double COAL_GENERATOR_VOLTAGE = 12.0;
    public static final double COAL_GENERATOR_RESISTANCE = 0.8;
    public static final double DIESEL_GENERATOR_VOLTAGE = 24.0;
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

    /** 석탄/디젤 발전기: 연료를 들고 우클릭하면 1개, 웅크리고 우클릭하면 들어가는 만큼 넣는다. */
    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
            Player player, InteractionHand hand, BlockHitResult hit) {
        int fuelValue = CircuitDeviceBlockEntity.fuelValue(kind, stack);
        if (fuelValue <= 0) return InteractionResult.TRY_WITH_EMPTY_HAND;
        if (!(level.getBlockEntity(pos) instanceof CircuitDeviceBlockEntity device)) return InteractionResult.PASS;
        if (level.isClientSide()) return InteractionResult.SUCCESS;

        boolean wasRunning = device.hasFuel();
        int inserted = device.insertFuel(fuelValue, stack.getCount(), player.isShiftKeyDown() ? stack.getCount() : 1);
        if (inserted <= 0) {
            player.sendOverlayMessage(Component.translatable("message.analog_alu.fuel_full").withStyle(ChatFormatting.RED));
            return InteractionResult.FAIL;
        }
        if (!player.hasInfiniteMaterials()) stack.shrink(inserted);
        level.playSound(null, pos, kind == Kind.COAL_GENERATOR ? SoundEvents.FIRECHARGE_USE : SoundEvents.BUCKET_EMPTY,
                SoundSource.BLOCKS, 0.6F, 1.0F);
        if (!wasRunning) CircuitSolver.recomputeAround(level, pos);
        return InteractionResult.SUCCESS;
    }

    /** 스위치만 우클릭으로 토글한다. 나머지 장치의 상태는 조준했을 때 뜨는 툴팁 창(HoverInfo)으로 확인한다. */
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
            BlockHitResult hit) {
        if (isConverter(kind)) {
            if (!player.getAbilities().mayBuild) return InteractionResult.PASS;
            if (level.isClientSide()) return InteractionResult.SUCCESS;
            if (!(level.getBlockEntity(pos) instanceof CircuitDeviceBlockEntity device)) return InteractionResult.PASS;
            double setpoint = device.cycleSetpoint(!player.isShiftKeyDown());
            player.sendOverlayMessage(Component.translatable("message.analog_alu.converter_setpoint", formatVolts(setpoint))
                    .withStyle(ChatFormatting.AQUA));
            level.playSound(null, pos, SoundEvents.COMPARATOR_CLICK, SoundSource.BLOCKS, 0.5F,
                    player.isShiftKeyDown() ? 0.5F : 0.7F);
            CircuitSolver.recomputeAround(level, pos);
            return InteractionResult.SUCCESS;
        }
        if (kind != Kind.SWITCH) return InteractionResult.PASS;
        if (!player.getAbilities().mayBuild) return InteractionResult.PASS;
        if (level.isClientSide()) return InteractionResult.SUCCESS;

        boolean closed = !state.getValue(CLOSED);
        level.setBlock(pos, state.setValue(CLOSED, closed), Block.UPDATE_ALL);
        level.playSound(null, pos, SoundEvents.LEVER_CLICK, SoundSource.BLOCKS, 0.4F, closed ? 0.6F : 0.5F);
        CircuitSolver.recomputeAround(level, pos);
        return InteractionResult.SUCCESS;
    }

    /** 툴팁 창에 보여줄 줄들. 첫 줄은 제목. 클라이언트에서 호출되므로 번역 키는 플레이어 언어로 표시된다. */
    public static List<Component> hoverLines(Level level, BlockPos pos, BlockState state,
            CircuitDeviceBlockEntity device) {
        Kind kind = ((CircuitDeviceBlock) state.getBlock()).kind();
        Direction positive = state.getValue(FACING);
        List<Component> lines = new ArrayList<>();
        lines.add(state.getBlock().getName().withStyle(ChatFormatting.GOLD));
        switch (kind) {
            case BATTERY, RECHARGEABLE_BATTERY -> {
                double joules = device.getStoredEnergyJoules();
                double capacity = batteryCapacity(kind);
                if (kind == Kind.RECHARGEABLE_BATTERY) {
                    boolean charging = device.getLoadCurrent() > 1.0e-3;
                    boolean discharging = !charging && device.getSourceTerminalVoltage() > 1.0e-3;
                    lines.add(HoverText.line("status",
                            HoverText.value(charging ? "charging" : discharging ? "discharging" : "idle"),
                            charging ? ChatFormatting.GREEN : discharging ? ChatFormatting.YELLOW : ChatFormatting.GRAY));
                }
                lines.add(HoverText.line("rated_voltage", volts(sourceVoltage(level, pos, kind), 1),
                        ChatFormatting.YELLOW));
                lines.add(HoverText.line("terminal_voltage", volts(device.getSourceTerminalVoltage(), 2),
                        ChatFormatting.AQUA));
                lines.add(HoverText.line("energy", String.format(Locale.ROOT, "%.0f / %.0f J (%.0f%%)",
                        joules, capacity, 100.0 * joules / capacity),
                        joules < capacity * 0.2 ? ChatFormatting.RED : ChatFormatting.GREEN));
                lines.add(HoverText.line("terminals", Component.translatable("hover.analog_alu.terminals_value",
                        HoverText.direction(positive), HoverText.direction(positive.getOpposite())),
                        ChatFormatting.WHITE));
                if (kind == Kind.RECHARGEABLE_BATTERY) lines.add(HoverText.hint("rechargeable"));
            }
            case SWITCH -> {
                boolean closed = state.getValue(CLOSED);
                lines.add(HoverText.line("state", HoverText.value(closed ? "closed" : "open"),
                        closed ? ChatFormatting.GREEN : ChatFormatting.RED));
                lines.add(HoverText.hint("switch"));
            }
            case RESISTOR -> lines.add(HoverText.line("resistance", ohms(LOAD_RESISTANCE), ChatFormatting.WHITE));
            case COAL_GENERATOR, DIESEL_GENERATOR -> {
                boolean coal = kind == Kind.COAL_GENERATOR;
                int fuelTicks = device.getFuelTicks();
                lines.add(HoverText.line("rated_voltage",
                        volts(coal ? COAL_GENERATOR_VOLTAGE : DIESEL_GENERATOR_VOLTAGE, 1), ChatFormatting.YELLOW));
                lines.add(HoverText.line("terminal_voltage", volts(device.getSourceTerminalVoltage(), 2),
                        ChatFormatting.AQUA));
                lines.add(HoverText.line("fuel", HoverText.seconds(fuelTicks / 20.0),
                        fuelTicks > 0 ? ChatFormatting.GREEN : ChatFormatting.RED));
                lines.add(HoverText.hint(coal ? "refuel_coal" : "refuel_diesel"));
            }
            case MINER -> {
                double loadVoltage = device.getLoadVoltage();
                lines.add(HoverText.line("resistance", ohms(MINER_RESISTANCE), ChatFormatting.WHITE));
                lines.add(HoverText.line("load_voltage", volts(loadVoltage, 2),
                        loadVoltage >= MINER_REQUIRED_VOLTAGE ? ChatFormatting.GREEN : ChatFormatting.RED));
                lines.add(HoverText.hint("miner"));
            }
            case STEP_UP, STEP_DOWN -> {
                boolean running = device.getSourceTerminalVoltage() > 1.0e-3;
                lines.add(HoverText.line("status", HoverText.value(running ? "running" : "no_input"),
                        running ? ChatFormatting.GREEN : ChatFormatting.RED));
                lines.add(HoverText.line("setpoint", formatVolts(device.converterSetpoint()), ChatFormatting.YELLOW));
                lines.add(HoverText.line("input_voltage", volts(device.getConverterInputVoltage(), 2),
                        ChatFormatting.AQUA));
                lines.add(HoverText.line("output_voltage", volts(device.getSourceTerminalVoltage(), 2),
                        ChatFormatting.AQUA));
                lines.add(HoverText.line("power", String.format(Locale.ROOT, "%.1f W", device.getConverterOutputWatts()),
                        ChatFormatting.WHITE));
                lines.add(HoverText.line("terminals", Component.translatable("hover.analog_alu.converter_terminals",
                        HoverText.direction(positive), HoverText.direction(positive.getOpposite())),
                        ChatFormatting.WHITE));
                lines.add(HoverText.hint("converter"));
            }
            case WIND_GENERATOR, SOLAR_GENERATOR -> {
                boolean producing = hasNaturalInput(level, pos, kind);
                lines.add(HoverText.line("status", HoverText.value(producing ? "producing" : "idle"),
                        producing ? ChatFormatting.GREEN : ChatFormatting.RED));
                lines.add(HoverText.line("rated_voltage", volts(sourceVoltage(level, pos, kind), 1),
                        ChatFormatting.YELLOW));
                lines.add(HoverText.line("terminal_voltage", volts(device.getSourceTerminalVoltage(), 2),
                        ChatFormatting.AQUA));
                if (!producing) lines.add(HoverText.hint(kind == Kind.WIND_GENERATOR ? "wind" : "solar"));
            }
        }
        return lines;
    }

    private static String formatVolts(double value) {
        return value >= 1000.0 ? String.format(Locale.ROOT, "%.0f kV", value / 1000.0)
                : String.format(Locale.ROOT, "%.0f V", value);
    }

    private static String volts(double value, int decimals) {
        return String.format(Locale.ROOT, "%." + decimals + "f V", value);
    }

    private static String ohms(double value) {
        return String.format(Locale.ROOT, "%.1f \u03a9", value);
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
            case RECHARGEABLE_BATTERY -> RECHARGEABLE_VOLTAGE;
            case COAL_GENERATOR -> COAL_GENERATOR_VOLTAGE;
            case DIESEL_GENERATOR -> DIESEL_GENERATOR_VOLTAGE;
            case WIND_GENERATOR -> Math.min(12.0, 6.0 + Math.max(0, pos.getY() - 80) * 0.05);
            case SOLAR_GENERATOR -> SOLAR_GENERATOR_VOLTAGE;
            default -> 0.0;
        };
    }

    public static boolean isConverter(Kind kind) {
        return kind == Kind.STEP_UP || kind == Kind.STEP_DOWN;
    }

    public static double[] setpoints(Kind kind) {
        if (kind == Kind.STEP_UP) return STEP_UP_SETPOINTS;
        if (kind == Kind.STEP_DOWN) return STEP_DOWN_SETPOINTS;
        return new double[0];
    }

    /** 건전지 용량(J). 건전지가 아니면 0. */
    public static double batteryCapacity(Kind kind) {
        if (kind == Kind.BATTERY) return BATTERY_CAPACITY_JOULES;
        if (kind == Kind.RECHARGEABLE_BATTERY) return RECHARGEABLE_CAPACITY_JOULES;
        return 0.0;
    }

    public static double sourceResistance(Kind kind) {
        return switch (kind) {
            case BATTERY -> BATTERY_INTERNAL_RESISTANCE;
            case RECHARGEABLE_BATTERY -> RECHARGEABLE_INTERNAL_RESISTANCE;
            case COAL_GENERATOR -> COAL_GENERATOR_RESISTANCE;
            case DIESEL_GENERATOR -> DIESEL_GENERATOR_RESISTANCE;
            case WIND_GENERATOR -> WIND_GENERATOR_RESISTANCE;
            case SOLAR_GENERATOR -> SOLAR_GENERATOR_RESISTANCE;
            case STEP_UP, STEP_DOWN -> CONVERTER_OUTPUT_RESISTANCE;
            default -> 0.0;
        };
    }
}