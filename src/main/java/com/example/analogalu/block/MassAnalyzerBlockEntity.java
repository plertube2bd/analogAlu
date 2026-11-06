package com.example.analogalu.block;

import com.example.analogalu.ModBlocks;
import com.example.analogalu.ModItems;
import com.example.analogalu.WaferProgramCompiler;
import com.example.analogalu.WaferProgramData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;

public class MassAnalyzerBlockEntity extends BlockEntity implements WorldlyContainer {
    public static final int REQUIRED_SIGNAL = 8;
    public static final int PROCESSING_TICKS = 3000;
    private static final String HAS_INPUT_KEY = "HasInput";
    private static final String PROGRESS_KEY = "Progress";
    private boolean hasInput;
    private int progress;
    private String programCode = "";
    private String programBytecode = "";
    private String programHash = "";
    private int programStatements;

    public MassAnalyzerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlocks.MASS_ANALYZER_ENTITY, pos, state);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        hasInput = input.getBooleanOr(HAS_INPUT_KEY, false);
        progress = Math.clamp(input.getIntOr(PROGRESS_KEY, 0), 0, PROCESSING_TICKS);
        programCode = input.getStringOr(WaferProgramData.CODE_KEY, "");
        programBytecode = input.getStringOr(WaferProgramData.BYTECODE_KEY, "");
        programHash = input.getStringOr(WaferProgramData.HASH_KEY, "");
        programStatements = input.getIntOr(WaferProgramData.STATEMENTS_KEY, 0);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putBoolean(HAS_INPUT_KEY, hasInput);
        output.putInt(PROGRESS_KEY, progress);
        output.putString(WaferProgramData.CODE_KEY, programCode);
        output.putString(WaferProgramData.BYTECODE_KEY, programBytecode);
        output.putString(WaferProgramData.HASH_KEY, programHash);
        output.putInt(WaferProgramData.STATEMENTS_KEY, programStatements);
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        super.preRemoveSideEffects(pos, state);
        if (hasInput && level != null && !level.isClientSide()) {
            ItemStack output = new ItemStack(ModItems.ION_IMPLANTED_SILICON);
            applyProgram(output);
            level.addFreshEntity(new ItemEntity(level, pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, output));
        }
    }

    public static void tick(Level level, BlockPos pos, BlockState state, MassAnalyzerBlockEntity machine) {
        if (machine.level == null || machine.level.isClientSide()) return;

        if (!machine.hasInput && machine.collectInput()) {
            machine.hasInput = true;
            machine.setChanged();
        }

        boolean active = machine.hasInput && level.getBestNeighborSignal(pos) >= REQUIRED_SIGNAL;
        int stage = machine.hasInput ? 1 + Math.min(7, machine.progress * 8 / PROCESSING_TICKS) : 0;
        if (state.getValue(MassAnalyzerBlock.ACTIVE) != active || state.getValue(MassAnalyzerBlock.STAGE) != stage) {
            level.setBlock(pos, state.setValue(MassAnalyzerBlock.ACTIVE, active).setValue(MassAnalyzerBlock.STAGE, stage), 3);
        }
        if (!active) return;

        machine.progress++;
        if (machine.progress % 20 == 0) machine.setChanged();
        if (machine.progress >= PROCESSING_TICKS) {
            machine.completeProcessing();
        }
    }

    private boolean collectInput() {
        AABB inputArea = new AABB(worldPosition.getX(), worldPosition.getY() + 1.0, worldPosition.getZ(),
                worldPosition.getX() + 1.0, worldPosition.getY() + 1.8, worldPosition.getZ() + 1.0);
        for (ItemEntity itemEntity : level.getEntitiesOfClass(ItemEntity.class, inputArea)) {
            ItemStack stack = itemEntity.getItem();
            if (!stack.is(ModItems.ION_IMPLANTED_SILICON)) continue;
            captureProgram(stack);
            stack.shrink(1);
            if (stack.isEmpty()) itemEntity.discard();
            return true;
        }
        return false;
    }

    // ---- 호퍼 입력: 이온 주입 실리콘 1개를 받아 바로 가공 대기열에 넣는다.

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
        return !hasInput && stack.is(ModItems.ION_IMPLANTED_SILICON);
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
        if (hasInput || !stack.is(ModItems.ION_IMPLANTED_SILICON)) return;
        captureProgram(stack);
        hasInput = true;
        progress = 0;
        setChanged();
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

    private void completeProcessing() {
        ItemStack outputStack = new ItemStack(ModItems.MASS_SELECTED_SILICON);
        applyProgram(outputStack);
        MachineOutput.emit(level, worldPosition, outputStack);
        hasInput = false;
        progress = 0;
        setChanged();
        BlockState state = getBlockState();
        level.setBlock(worldPosition, state.setValue(MassAnalyzerBlock.ACTIVE, false).setValue(MassAnalyzerBlock.STAGE, 0), 3);
    }

    private void captureProgram(ItemStack stack) {
        CompoundTag data = WaferProgramData.get(stack);
        programBytecode = data.getStringOr(WaferProgramData.BYTECODE_KEY, "");
        if (!WaferProgramCompiler.isBytecode(programBytecode)) {
            programCode = "";
            programBytecode = "";
            programHash = "";
            programStatements = 0;
            return;
        }
        programCode = data.getStringOr(WaferProgramData.CODE_KEY, "");
        programHash = data.getStringOr(WaferProgramData.HASH_KEY, "");
        programStatements = data.getIntOr(WaferProgramData.STATEMENTS_KEY, 0);
    }

    private void applyProgram(ItemStack stack) {
        if (!WaferProgramCompiler.isBytecode(programBytecode)) return;
        CompoundTag data = new CompoundTag();
        data.putString(WaferProgramData.CODE_KEY, programCode);
        data.putString(WaferProgramData.BYTECODE_KEY, programBytecode);
        data.putString(WaferProgramData.HASH_KEY, programHash);
        data.putInt(WaferProgramData.STATEMENTS_KEY, programStatements);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(data));
    }
}