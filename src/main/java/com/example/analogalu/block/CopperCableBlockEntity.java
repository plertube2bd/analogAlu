package com.example.analogalu.block;

import com.example.analogalu.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public class CopperCableBlockEntity extends BlockEntity {
    public static final double AMBIENT_TEMPERATURE_C = 15.0;
    public static final double COPPER_RESISTIVITY_OHM_METERS = 1.68e-8;
    public static final double CROSS_SECTION_SQUARE_METERS = 1.5e-6;
    public static final double TEMPERATURE_COEFFICIENT = 0.00393;
    private static final double BASE_RESISTANCE_OHMS =
            COPPER_RESISTIVITY_OHM_METERS / CROSS_SECTION_SQUARE_METERS;
    private static final double MAX_DAMAGE = 100.0;
    private static final double THERMAL_GAIN_PER_TICK = 0.01;
    private static final double COOLING_PER_TICK = 0.0005;

    private double voltage;
    private double current;
    private double temperatureCelsius = AMBIENT_TEMPERATURE_C;
    private double damage;

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

    public static void tick(Level level, BlockPos pos, BlockState state, CopperCableBlockEntity cable) {
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

        if (cable.damage > MAX_DAMAGE) {
            level.destroyBlock(pos, true);
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
        setChanged();
    }

    private static double finiteOr(double value, double fallback) {
        return Double.isFinite(value) ? value : fallback;
    }
}