package com.example.analogalu.block;

import com.example.analogalu.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;

public class CircuitDeviceBlockEntity extends BlockEntity {
    private static final int COAL_FUEL_TICKS = 1600;
    private static final int MAX_FUEL_TICKS = COAL_FUEL_TICKS * 8;
    private double storedEnergyJoules = CircuitDeviceBlock.BATTERY_CAPACITY_JOULES;
    private int fuelTicks;
    private long lastEnergyTick = -1L;

    public CircuitDeviceBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlocks.CIRCUIT_DEVICE_ENTITY, pos, state);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        double stored = input.getDoubleOr("StoredEnergyJoules", CircuitDeviceBlock.BATTERY_CAPACITY_JOULES);
        storedEnergyJoules = Double.isFinite(stored)
            ? Math.clamp(stored, 0.0, CircuitDeviceBlock.BATTERY_CAPACITY_JOULES)
            : 0.0;
        fuelTicks = Math.clamp(input.getIntOr("FuelTicks", 0), 0, MAX_FUEL_TICKS);
        lastEnergyTick = input.getLongOr("LastEnergyTick", -1L);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putDouble("StoredEnergyJoules", storedEnergyJoules);
        output.putInt("FuelTicks", fuelTicks);
        output.putLong("LastEnergyTick", lastEnergyTick);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, CircuitDeviceBlockEntity device) {
        if (device.level == null || device.level.isClientSide()
                || !(state.getBlock() instanceof CircuitDeviceBlock block)
                || block.kind() != CircuitDeviceBlock.Kind.COAL_GENERATOR
                || device.fuelTicks >= MAX_FUEL_TICKS || level.getGameTime() % 10L != 0L) return;

        AABB inputArea = new AABB(pos.getX() + 0.1, pos.getY() + 0.95, pos.getZ() + 0.1,
                pos.getX() + 0.9, pos.getY() + 1.8, pos.getZ() + 0.9);
        for (ItemEntity itemEntity : level.getEntitiesOfClass(ItemEntity.class, inputArea)) {
            var stack = itemEntity.getItem();
            if (!stack.is(Items.COAL) && !stack.is(Items.CHARCOAL)) continue;
            stack.shrink(1);
            if (stack.isEmpty()) itemEntity.discard();
            device.fuelTicks = Math.min(MAX_FUEL_TICKS, device.fuelTicks + COAL_FUEL_TICKS);
            device.setChanged();
            break;
        }
    }

    public double getStoredEnergyJoules() {
        return storedEnergyJoules;
    }

    public int getFuelTicks() {
        return fuelTicks;
    }

    public boolean hasFuel() {
        return fuelTicks > 0;
    }

    public void accountSourcePower(double watts, long gameTime, CircuitDeviceBlock.Kind kind) {
        long elapsedTicks = lastEnergyTick < 0L ? 0L : Math.max(0L, gameTime - lastEnergyTick);
        lastEnergyTick = gameTime;
        if (Double.isFinite(watts) && watts > 0.0 && elapsedTicks > 0L) {
            if (kind == CircuitDeviceBlock.Kind.BATTERY) {
                storedEnergyJoules = Math.max(0.0, storedEnergyJoules - watts * elapsedTicks / 20.0);
            } else if (kind == CircuitDeviceBlock.Kind.COAL_GENERATOR) {
                fuelTicks = Math.max(0, fuelTicks - (int) Math.min(Integer.MAX_VALUE, elapsedTicks));
            }
        }
        setChanged();
    }

    public void consumeEnergy(double watts, long gameTime) {
        accountSourcePower(watts, gameTime, CircuitDeviceBlock.Kind.BATTERY);
    }
}