package com.example.analogalu.block;

import com.example.analogalu.ModBlocks;
import java.util.List;
import com.example.analogalu.ModComponents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Player;
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

public class CircuitDeviceBlockEntity extends BlockEntity implements HoverInfo, WorldlyContainer {
    private static final int COAL_FUEL_TICKS = 1600;
    private static final int DIESEL_FUEL_TICKS = COAL_FUEL_TICKS * 4;
    /** 디젤 128개(석탄 512개)분량. */
    private static final int MAX_FUEL_TICKS = DIESEL_FUEL_TICKS * 128;
    /** 한 번에 정산하는 최대 경과 시간. 회로에서 떨어져 있던 시간이 한꺼번에 계산되지 않게 한다. */
    private static final long MAX_ACCOUNTED_TICKS = 40L;
    private double storedEnergyJoules;
    private int fuelTicks;
    private long lastEnergyTick = -1L;
    private int lastSyncSignature = Integer.MIN_VALUE;
    private double loadVoltage;
    private double loadCurrent;
    private double sourceTerminalVoltage;
    private long nextMiningTick;
    // 승압기/변압기 상태
    private int setpointIndex;
    private double converterInputVoltage;
    private double converterInputCurrent;
    /** 입력 회로 전원의 기전력. 권선비(설정 전압 / 기전력)를 정하는 데 쓴다. */
    private double converterInputEmf;
    private long lastConverterInputTick = -1L;
    private double converterOutputWatts;
    /** 출력 회로 전원이 본 전체 저항(내부 저항 포함). 음수면 출력 회로가 열려 있음. 선형 회로라 전압과 무관하다. */
    private double converterLoadResistance = -1.0;
    private long lastConverterOutputTick = -1L;

    public CircuitDeviceBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlocks.CIRCUIT_DEVICE_ENTITY, pos, state);
        // 일반 건전지는 가득 찬 채로, 충전식 건전지는 빈 채로 시작한다.
        storedEnergyJoules = kind() == CircuitDeviceBlock.Kind.BATTERY ? capacity() : 0.0;
        if (kind() == CircuitDeviceBlock.Kind.STEP_DOWN) setpointIndex = CircuitDeviceBlock.STEP_DOWN_DEFAULT_INDEX;
    }

    public CircuitDeviceBlock.Kind kindOrNull() {
        return kind();
    }

    private CircuitDeviceBlock.Kind kind() {
        return getBlockState().getBlock() instanceof CircuitDeviceBlock block ? block.kind() : null;
    }

    private double capacity() {
        return CircuitDeviceBlock.batteryCapacity(kind());
    }

    private boolean isBattery() {
        return capacity() > 0.0;
    }

    // ---- 건전지를 캐면 남은 에너지가 아이템에 남고, 다시 설치하면 복원된다.

    @Override
    protected void applyImplicitComponents(DataComponentGetter components) {
        super.applyImplicitComponents(components);
        Double stored = components.get(ModComponents.STORED_ENERGY);
        if (stored != null && isBattery()) storedEnergyJoules = Math.clamp(finiteOr(stored, 0.0), 0.0, capacity());
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder components) {
        super.collectImplicitComponents(components);
        if (isBattery()) components.set(ModComponents.STORED_ENERGY, storedEnergyJoules);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        double stored = input.getDoubleOr("StoredEnergyJoules", storedEnergyJoules);
        storedEnergyJoules = Double.isFinite(stored) ? Math.clamp(stored, 0.0, Math.max(0.0, capacity())) : 0.0;
        fuelTicks = Math.clamp(input.getIntOr("FuelTicks", 0), 0, MAX_FUEL_TICKS);
        lastEnergyTick = input.getLongOr("LastEnergyTick", -1L);
        loadVoltage = finiteOr(input.getDoubleOr("LoadVoltage", 0.0), 0.0);
        loadCurrent = finiteOr(input.getDoubleOr("LoadCurrent", 0.0), 0.0);
        sourceTerminalVoltage = finiteOr(input.getDoubleOr("SourceTerminalVoltage", 0.0), 0.0);
        nextMiningTick = input.getLongOr("NextMiningTick", 0L);
        if (CircuitDeviceBlock.isConverter(kind())) {
            setpointIndex = Math.clamp(input.getIntOr("Setpoint", setpointIndex), 0,
                    CircuitDeviceBlock.setpoints(kind()).length - 1);
        }
        converterInputVoltage = finiteOr(input.getDoubleOr("ConverterInputVoltage", 0.0), 0.0);
        converterInputCurrent = finiteOr(input.getDoubleOr("ConverterInputCurrent", 0.0), 0.0);
        converterOutputWatts = finiteOr(input.getDoubleOr("ConverterOutputWatts", 0.0), 0.0);
        converterInputEmf = finiteOr(input.getDoubleOr("ConverterInputEmf", 0.0), 0.0);
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
        output.putInt("Setpoint", setpointIndex);
        output.putDouble("ConverterInputVoltage", converterInputVoltage);
        output.putDouble("ConverterInputCurrent", converterInputCurrent);
        output.putDouble("ConverterOutputWatts", converterOutputWatts);
        output.putDouble("ConverterInputEmf", converterInputEmf);
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
            Math.round(sourceTerminalVoltage * 100.0), setpointIndex, Math.round(converterInputVoltage * 100.0),
            Math.round(converterOutputWatts * 10.0));
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
        if (CircuitDeviceBlock.isConverter(kind)) {
            // 출력 회로가 열렸거나 입력이 끊긴 경우(전력 0): 입력 쪽에서는 거의 열린 회로로 보이게 한다.
            if (!(watts > 0.0)) {
                converterOutputWatts = 0.0;
                converterLoadResistance = -1.0;
            }
            lastConverterOutputTick = gameTime;
            setChanged();
            return;
        }
        long elapsedTicks = elapsedSince(gameTime);
        if (Double.isFinite(watts) && watts > 0.0 && elapsedTicks > 0L) {
            if (kind == CircuitDeviceBlock.Kind.BATTERY || kind == CircuitDeviceBlock.Kind.RECHARGEABLE_BATTERY) {
                storedEnergyJoules = Math.max(0.0, storedEnergyJoules - watts * elapsedTicks / 20.0);
            } else if (kind == CircuitDeviceBlock.Kind.COAL_GENERATOR
                    || kind == CircuitDeviceBlock.Kind.DIESEL_GENERATOR) {
                fuelTicks = Math.max(0, fuelTicks - (int) Math.min(Integer.MAX_VALUE, elapsedTicks));
            }
        }
        setChanged();
    }

    // ---- 승압기/변압기 (권선비가 고정된 이상적인 변압기 + 효율)
    // 입력 쪽 회로에서는 저항 부하로, 출력 쪽 회로에서는 전원으로 풀린다. 두 회로는 지난 계산값으로 연결된다:
    //  - 출력 전압 = 권선비 x 입력 회로에서 측정한 전압. 권선비 = 설정 전압 / 입력 전원 기전력.
    //  - 입력 저항 = 효율 x 출력 쪽 전체 저항 / 권선비^2 (반사 임피던스). 입력에서 (출력 전력 / 효율)을 끌어온다.
    // 전원이 약해 입력 전압이 처지면 출력 전압도 같은 비율로 처지므로 에너지가 새로 생기지 않는다.

    public double converterSetpoint() {
        double[] setpoints = CircuitDeviceBlock.setpoints(kind());
        return setpoints.length == 0 ? 0.0 : setpoints[Math.clamp(setpointIndex, 0, setpoints.length - 1)];
    }

    /** 설정 전압을 한 단계 올리거나 내리고 새 값을 돌려준다. */
    public double cycleSetpoint(boolean up) {
        int count = CircuitDeviceBlock.setpoints(kind()).length;
        if (count == 0) return 0.0;
        setpointIndex = Math.floorMod(setpointIndex + (up ? 1 : -1), count);
        setChanged();
        syncIfChanged();
        return converterSetpoint();
    }

    public double converterInputResistance() {
        double ratio = converterRatio();
        boolean outputStale = level == null || lastConverterOutputTick < 0L
                || level.getGameTime() - lastConverterOutputTick > MAX_ACCOUNTED_TICKS;
        if (ratio <= 0.0 || converterLoadResistance <= 0.0 || outputStale) {
            return CircuitDeviceBlock.CONVERTER_IDLE_RESISTANCE;
        }
        return Math.clamp(CircuitDeviceBlock.CONVERTER_EFFICIENCY * converterLoadResistance / (ratio * ratio),
                1.0e-6, CircuitDeviceBlock.CONVERTER_IDLE_RESISTANCE);
    }

    /** 출력 회로를 풀고 난 뒤 호출: 전원 전압과 전력으로 출력 쪽 전체 저항을 기록한다. */
    public void setConverterOutput(double sourceVoltage, double watts, long gameTime) {
        converterOutputWatts = Double.isFinite(watts) ? Math.max(0.0, watts) : 0.0;
        if (sourceVoltage > 1.0e-9 && converterOutputWatts > 1.0e-12) {
            converterLoadResistance = sourceVoltage * sourceVoltage / converterOutputWatts;
        }
        lastConverterOutputTick = gameTime;
        setChanged();
    }

    public boolean converterAvailable(long gameTime) {
        return lastConverterInputTick >= 0L && gameTime - lastConverterInputTick <= MAX_ACCOUNTED_TICKS
                && converterInputVoltage > 1.0e-3 && converterInputEmf > 0.0;
    }

    /** 권선비. 승압기는 1 이상(입력이 이미 설정값보다 높으면 1), 변압기는 1 이하. */
    public double converterRatio() {
        if (converterInputEmf <= 0.0) return 0.0;
        double ratio = converterSetpoint() / converterInputEmf;
        return kind() == CircuitDeviceBlock.Kind.STEP_UP
                ? Math.min(Math.max(1.0, ratio), CircuitDeviceBlock.STEP_UP_MAX_VOLTAGE / converterInputEmf)
                : Math.min(1.0, ratio);
    }

    public double converterOutputVoltage() {
        return converterRatio() * converterInputVoltage;
    }

    public void setConverterInput(double voltage, double current, long gameTime, double sourceEmf) {
        converterInputVoltage = finiteOr(voltage, 0.0);
        converterInputCurrent = finiteOr(current, 0.0);
        converterInputEmf = finiteOr(sourceEmf, 0.0);
        lastConverterInputTick = gameTime;
        setChanged();
    }

    public double getConverterInputVoltage() {
        return converterInputVoltage;
    }

    public double getConverterInputCurrent() {
        return converterInputCurrent;
    }

    public double getConverterOutputWatts() {
        return converterOutputWatts;
    }

    /** 충전식 건전지가 회로에서 충전 부하로 쓰일 때 받은 전력(W)을 저장한다. */
    public void accountCharge(double watts, long gameTime) {
        long elapsedTicks = elapsedSince(gameTime);
        if (Double.isFinite(watts) && watts > 0.0 && elapsedTicks > 0L) {
            storedEnergyJoules = Math.min(capacity(), storedEnergyJoules + watts * elapsedTicks / 20.0);
        }
        setChanged();
    }

    public boolean isFullyCharged() {
        return storedEnergyJoules >= capacity();
    }

    private long elapsedSince(long gameTime) {
        long elapsed = lastEnergyTick < 0L ? 0L : Math.clamp(gameTime - lastEnergyTick, 0L, MAX_ACCOUNTED_TICKS);
        lastEnergyTick = gameTime;
        return elapsed;
    }

    // ---- 호퍼 입력: 석탄/디젤 발전기에 연료를 넣는다. 넣은 연료는 바로 연소 시간으로 바뀌므로 슬롯은 늘 비어 있다.

    private static final int[] SLOTS = {0};

    @Override
    public int[] getSlotsForFace(Direction side) {
        return SLOTS;
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, Direction side) {
        return canPlaceItem(slot, stack);
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return false;
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        int value = kind() == null ? 0 : fuelValue(kind(), stack);
        return value > 0 && fuelTicks + value <= MAX_FUEL_TICKS;
    }

    @Override
    public int getContainerSize() {
        return 1;
    }

    @Override
    public boolean isEmpty() {
        return true;
    }

    @Override
    public ItemStack getItem(int slot) {
        return ItemStack.EMPTY;
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        return ItemStack.EMPTY;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        return ItemStack.EMPTY;
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        if (kind() == null || stack.isEmpty()) return;
        boolean wasRunning = hasFuel();
        insertFuel(fuelValue(kind(), stack), stack.getCount(), stack.getCount());
        if (!wasRunning && hasFuel() && level != null) CircuitSolver.recomputeAround(level, worldPosition);
    }

    @Override
    public int getMaxStackSize() {
        return 1;
    }

    @Override
    public boolean stillValid(Player player) {
        return false;
    }

    @Override
    public void clearContent() {}

    public void consumeEnergy(double watts, long gameTime) {
        accountSourcePower(watts, gameTime, CircuitDeviceBlock.Kind.BATTERY);
    }

    private static double finiteOr(double value, double fallback) {
        return Double.isFinite(value) ? value : fallback;
    }
}