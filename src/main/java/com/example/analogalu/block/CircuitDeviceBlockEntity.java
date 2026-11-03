package com.example.analogalu.block;

import com.example.analogalu.ModBlocks;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public class CircuitDeviceBlockEntity extends BlockEntity implements HoverInfo {
    private static final int COAL_FUEL_TICKS = 1600;
    private static final int DIESEL_FUEL_TICKS = COAL_FUEL_TICKS * 4;
    /** 디젤 128개(석탄 512개)분량. */
    private static final int MAX_FUEL_TICKS = DIESEL_FUEL_TICKS * 128;
    private double storedEnergyJoules = CircuitDeviceBlock.BATTERY_CAPACITY_JOULES;
    private int fuelTicks;
    private long lastEnergyTick = -1L;
    private int lastSyncSignature = Integer.MIN_VALUE;
    private double loadVoltage;
    private double loadCurrent;
    private double sourceTerminalVoltage;
    private long nextMiningTick;

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
        loadVoltage = finiteOr(input.getDoubleOr("LoadVoltage", 0.0), 0.0);
        loadCurrent = finiteOr(input.getDoubleOr("LoadCurrent", 0.0), 0.0);
        sourceTerminalVoltage = finiteOr(input.getDoubleOr("SourceTerminalVoltage", 0.0), 0.0);
        nextMiningTick = input.getLongOr("NextMiningTick", 0L);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putDouble("StoredEnergyJoules", storedEnergyJoules);
        output.putInt("FuelTicks", fuelTicks);
        output.putLong("LastEnergyTick", lastEnergyTick);
        output.putDouble("LoadVoltage", loadVoltage);
        output.putDouble("LoadCurrent", loadCurrent);
        output.putDouble("SourceTerminalVoltage", sourceTerminalVoltage);
        output.putLong("NextMiningTick", nextMiningTick);
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

    private void syncIfChanged() {
        int signature = java.util.Objects.hash(Math.round(storedEnergyJoules / 10.0), fuelTicks / 10,
            Math.round(loadVoltage * 100.0), Math.round(loadCurrent * 1000.0),
            Math.round(sourceTerminalVoltage * 100.0));
        if (signature == lastSyncSignature) return;
        lastSyncSignature = signature;
        BlockEntitySync.send(this);
    }

    @Override
    public List<Component> hoverInfo(Level level) {
        return CircuitDeviceBlock.hoverLines(level, worldPosition, getBlockState(), this);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, CircuitDeviceBlockEntity device) {
        if (!level.isClientSide() && Math.floorMod(level.getGameTime(), 10L) == Math.floorMod(pos.asLong(), 10L)) {
            device.syncIfChanged();
        }
        if (device.level == null || device.level.isClientSide()
            || !(state.getBlock() instanceof CircuitDeviceBlock block)
            || level.getGameTime() % 10L != 0L) return;

        if (block.kind() == CircuitDeviceBlock.Kind.MINER) device.tickMiner(level, pos, state);
    }

    /** 발전기 연료 1개의 연소 시간(틱). 해당 발전기에 맞지 않는 아이템이면 0. */
    public static int fuelValue(CircuitDeviceBlock.Kind kind, ItemStack stack) {
        if (kind == CircuitDeviceBlock.Kind.COAL_GENERATOR) {
            return stack.is(Items.COAL) || stack.is(Items.CHARCOAL) ? COAL_FUEL_TICKS : 0;
        }
        if (kind == CircuitDeviceBlock.Kind.DIESEL_GENERATOR) {
            return stack.is(com.example.analogalu.ModItems.DIESEL) ? DIESEL_FUEL_TICKS : 0;
        }
        return 0;
    }

    /** 연료를 최대 maxItems개까지 넣고 실제로 넣은 개수를 돌려준다. */
    public int insertFuel(int fuelValue, int available, int maxItems) {
        if (fuelValue <= 0) return 0;
        int room = (MAX_FUEL_TICKS - fuelTicks) / fuelValue;
        int inserted = Math.min(Math.min(available, maxItems), room);
        if (inserted <= 0) return 0;
        fuelTicks += inserted * fuelValue;
        setChanged();
        syncIfChanged();
        return inserted;
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

    public double getLoadVoltage() {
        return loadVoltage;
    }

    public double getLoadCurrent() {
        return loadCurrent;
    }

    public double getSourceTerminalVoltage() {
        return sourceTerminalVoltage;
    }

    public void setSourceTerminalVoltage(double voltage) {
        sourceTerminalVoltage = finiteOr(voltage, 0.0);
        setChanged();
    }

    public void setLoadState(double voltage, double current) {
        loadVoltage = finiteOr(voltage, 0.0);
        loadCurrent = finiteOr(current, 0.0);
        setChanged();
    }

    private void tickMiner(Level level, BlockPos pos, BlockState state) {
        if (loadVoltage < CircuitDeviceBlock.MINER_REQUIRED_VOLTAGE
                || !level.hasNeighborSignal(pos) || level.getGameTime() < nextMiningTick) return;

        Direction facing = state.getValue(CircuitDeviceBlock.FACING);
        BlockPos targetPos = pos.relative(facing);
        BlockState targetState = level.getBlockState(targetPos);
        if (targetState.isAir() || targetState.getDestroySpeed(level, targetPos) < 0.0F
                || level.getBlockEntity(targetPos) != null) return;

        if (level.destroyBlock(targetPos, true)) {
            nextMiningTick = level.getGameTime() + 10L;
            level.playSound(null, pos, SoundEvents.STONE_BREAK, SoundSource.BLOCKS, 0.7F, 0.9F);
            setChanged();
        }
    }

    public void accountSourcePower(double watts, long gameTime, CircuitDeviceBlock.Kind kind) {
        long elapsedTicks = lastEnergyTick < 0L ? 0L : Math.max(0L, gameTime - lastEnergyTick);
        lastEnergyTick = gameTime;
        if (Double.isFinite(watts) && watts > 0.0 && elapsedTicks > 0L) {
            if (kind == CircuitDeviceBlock.Kind.BATTERY) {
                storedEnergyJoules = Math.max(0.0, storedEnergyJoules - watts * elapsedTicks / 20.0);
            } else if (kind == CircuitDeviceBlock.Kind.COAL_GENERATOR
                    || kind == CircuitDeviceBlock.Kind.DIESEL_GENERATOR) {
                fuelTicks = Math.max(0, fuelTicks - (int) Math.min(Integer.MAX_VALUE, elapsedTicks));
            }
        }
        setChanged();
    }

    public void consumeEnergy(double watts, long gameTime) {
        accountSourcePower(watts, gameTime, CircuitDeviceBlock.Kind.BATTERY);
    }

    private static double finiteOr(double value, double fallback) {
        return Double.isFinite(value) ? value : fallback;
    }
}