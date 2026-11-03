package com.example.analogalu.block;

import com.example.analogalu.ModBlocks;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public class CopperCableBlockEntity extends BlockEntity implements HoverInfo {
    public static final double AMBIENT_TEMPERATURE_C = 15.0;
    public static final double COPPER_RESISTIVITY_OHM_METERS = 1.68e-8;
    public static final double CROSS_SECTION_SQUARE_METERS = 1.5e-6;
    public static final double TEMPERATURE_COEFFICIENT = 0.00393;
    /** 게임 밸런스를 위해 실제 구리보다 저항을 5배 낮춘다. */
    public static final double RESISTANCE_SCALE = 0.2;
    private static final double BASE_RESISTANCE_OHMS =
            COPPER_RESISTIVITY_OHM_METERS / CROSS_SECTION_SQUARE_METERS * RESISTANCE_SCALE;
    private static final double MAX_DAMAGE = 100.0;
    private static final double THERMAL_GAIN_PER_TICK = 0.01;
    private static final double COOLING_PER_TICK = 0.0005;

    private double voltage;
    private double current;
    private double temperatureCelsius = AMBIENT_TEMPERATURE_C;
    private double damage;
    private int lastSyncSignature = Integer.MIN_VALUE;

    public CopperCableBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlocks.COPPER_CABLE_ENTITY, pos, state);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        voltage = finiteOr(input.getDoubleOr("Voltage", 0.0), 0.0);
        current = finiteOr(input.getDoubleOr("Current", 0.0), 0.0);
        temperatureCelsius = Math.clamp(finiteOr(input.getDoubleOr("TemperatureCelsius", AMBIENT_TEMPERATURE_C),
                AMBIENT_TEMPERATURE_C), -100.0, 1000.0);
        damage = Math.clamp(finiteOr(input.getDoubleOr("Damage", 0.0), 0.0), 0.0, MAX_DAMAGE);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putDouble("Voltage", voltage);
        output.putDouble("Current", current);
        output.putDouble("TemperatureCelsius", temperatureCelsius);
        output.putDouble("Damage", damage);
    }

    // ---- 클라이언트 동기화 (툴팁에 쓰는 값)

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    /** 표시값이 눈에 띄게 바뀐 경우에만 주변 클라이언트로 전송한다. */
    private void syncIfChanged() {
        int signature = java.util.Objects.hash(Math.round(voltage * 100), Math.round(current * 1000),
                Math.round(temperatureCelsius * 10), Math.round(getHealth() * 10));
        if (signature == lastSyncSignature) return;
        lastSyncSignature = signature;
        BlockEntitySync.send(this);
    }

    @Override
    public List<Component> hoverInfo(Level level) {
        boolean locked = getBlockState().getValue(CopperCableBlock.LOCK) != 0;
        double health = getHealth();
        List<Component> lines = new ArrayList<>();
        lines.add(getBlockState().getBlock().getName().withStyle(ChatFormatting.GOLD));
        lines.add(HoverText.line("voltage", String.format(Locale.ROOT, "%.2f V", voltage), ChatFormatting.YELLOW));
        lines.add(HoverText.line("current", String.format(Locale.ROOT, "%.3f A", current), ChatFormatting.AQUA));
        lines.add(HoverText.line("resistance", String.format(Locale.ROOT, "%.5f \u03a9", getResistanceOhms()),
                ChatFormatting.WHITE));
        lines.add(HoverText.line("temperature", String.format(Locale.ROOT, "%.1f \u00b0C", temperatureCelsius),
                temperatureCelsius >= 75.0 ? ChatFormatting.RED
                        : temperatureCelsius >= 50.0 ? ChatFormatting.GOLD : ChatFormatting.GREEN));
        lines.add(HoverText.line("health", String.format(Locale.ROOT, "%.1f / 100", health),
                health < 30.0 ? ChatFormatting.RED : health < 70.0 ? ChatFormatting.GOLD : ChatFormatting.GREEN));
        lines.add(HoverText.line("wiring", HoverText.value(locked ? "locked" : "unlocked"),
                locked ? ChatFormatting.BLUE : ChatFormatting.GRAY));
        return lines;
    }

    public static void tick(Level level, BlockPos pos, BlockState state, CopperCableBlockEntity cable) {
        if (Math.floorMod(level.getGameTime(), 10L) == Math.floorMod(pos.asLong(), 10L)) cable.syncIfChanged();
        if (Math.floorMod(level.getGameTime(), 20L) == Math.floorMod(pos.asLong(), 20L)) {
            CircuitSolver.solveFrom(level, pos);
        }
        double resistance = cable.getResistanceOhms();
        double jouleHeating = cable.current * cable.current * resistance * THERMAL_GAIN_PER_TICK;
        double ambientCooling = (AMBIENT_TEMPERATURE_C - cable.temperatureCelsius) * COOLING_PER_TICK;
        cable.temperatureCelsius = Math.clamp(cable.temperatureCelsius + jouleHeating + ambientCooling,
                -100.0, 1000.0);

        if (cable.temperatureCelsius >= 75.0) {
            cable.damage += damageRatePerSecond(cable.temperatureCelsius) / 20.0;
        } else if (cable.temperatureCelsius < 40.0) {
            cable.damage = Math.max(0.0, cable.damage - repairRatePerSecond(cable.temperatureCelsius) / 20.0);
        }

        if (cable.damage >= MAX_DAMAGE) {
            level.playSound(null, pos, SoundEvents.FIRE_AMBIENT, SoundSource.BLOCKS, 0.8F, 1.2F);
            level.removeBlock(pos, false);
            return;
        }
        if (level.getGameTime() % 20L == 0L) cable.setChanged();
    }

    public static double damageRatePerSecond(double temperatureCelsius) {
        if (temperatureCelsius < 75.0) return 0.0;
        return Math.pow(100.0, (temperatureCelsius - 75.0) / 25.0);
    }

    public static double repairRatePerSecond(double temperatureCelsius) {
        if (temperatureCelsius >= 40.0) return 0.0;
        return Math.pow(100.0, (40.0 - temperatureCelsius) / 25.0);
    }

    public double getResistanceOhms() {
        return BASE_RESISTANCE_OHMS * (1.0 + TEMPERATURE_COEFFICIENT * (temperatureCelsius - 20.0));
    }

    public double getVoltage() {
        return voltage;
    }

    public double getCurrent() {
        return current;
    }

    public double getTemperatureCelsius() {
        return temperatureCelsius;
    }

    public double getHealth() {
        return Math.max(0.0, MAX_DAMAGE - damage);
    }

    public void setCircuitState(double voltage, double current) {
        this.voltage = finiteOr(voltage, 0.0);
        this.current = finiteOr(current, 0.0);
        if (level != null && !level.isClientSide()) {
            BlockState state = level.getBlockState(worldPosition);
            boolean powered = Math.abs(this.current) > 1.0e-6;
            if (state.is(ModBlocks.COPPER_CABLE) && state.getValue(CopperCableBlock.POWERED) != powered) {
                level.setBlock(worldPosition, state.setValue(CopperCableBlock.POWERED, powered),
                        net.minecraft.world.level.block.Block.UPDATE_ALL);
            }
        }
        setChanged();
    }

    private static double finiteOr(double value, double fallback) {
        return Double.isFinite(value) ? value : fallback;
    }
}