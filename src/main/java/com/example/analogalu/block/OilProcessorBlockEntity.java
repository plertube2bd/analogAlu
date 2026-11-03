package com.example.analogalu.block;

import com.example.analogalu.ModBlocks;
import com.example.analogalu.ModItems;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.AbstractFurnaceBlock;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;

public final class OilProcessorBlockEntity extends BlockEntity implements HoverInfo, WorldlyContainer {
    private static final int PROCESSING_TICKS = 400;
    public static final int TANK_CAPACITY = 64;
    /** 대기 중인 석유(양동이 단위). */
    private int tankUnits;
    /** 지금 가공 중인 석유. 가공이 끝나면 이 개수만큼 디젤/플라스틱/아스팔트가 나온다. */
    private int batchUnits;
    private int progress;
    /** 아래에 열원이 있는지 (툴팁 표시용). */
    private boolean heated;
    private int lastSyncSignature;

    public OilProcessorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlocks.OIL_PROCESSOR_ENTITY, pos, state);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        boolean legacyHasOil = input.getBooleanOr("HasOil", false);
        int legacyUnits = input.getIntOr("OilUnits", 0);
        progress = Math.clamp(input.getIntOr("Progress", 0), 0, PROCESSING_TICKS);
        batchUnits = Math.clamp(input.getIntOr("BatchUnits", legacyHasOil ? legacyUnits : 0), 0, TANK_CAPACITY);
        tankUnits = Math.clamp(input.getIntOr("TankUnits", 0), 0, TANK_CAPACITY);
        heated = input.getBooleanOr("Heated", false);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("TankUnits", tankUnits);
        output.putInt("BatchUnits", batchUnits);
        output.putInt("Progress", progress);
        output.putBoolean("Heated", heated);
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
        int signature = java.util.Objects.hash(tankUnits, batchUnits, progress, heated);
        if (signature == lastSyncSignature) return;
        lastSyncSignature = signature;
        BlockEntitySync.send(this);
    }

    @Override
    public List<Component> hoverInfo(Level level) {
        Component state;
        if (batchUnits == 0) {
            state = HoverText.value("empty").withStyle(ChatFormatting.GRAY);
        } else if (!heated) {
            state = HoverText.value("no_heat").withStyle(ChatFormatting.RED);
        } else {
            state = HoverText.value("distilling").withStyle(ChatFormatting.GREEN);
        }
        List<Component> lines = new java.util.ArrayList<>();
        lines.add(getBlockState().getBlock().getName().copy().withStyle(ChatFormatting.WHITE));
        lines.add(HoverText.line("oil", storedOil() + " / " + TANK_CAPACITY, ChatFormatting.GOLD));
        lines.add(HoverText.line("status", state, ChatFormatting.WHITE));
        if (batchUnits > 0) {
            lines.add(HoverText.line("batch", Component.translatable("hover.analog_alu.batch_value", batchUnits,
                    progress * 100 / PROCESSING_TICKS), ChatFormatting.AQUA));
            lines.add(HoverText.line("time_left",
                    HoverText.seconds((PROCESSING_TICKS - progress) / 20.0), ChatFormatting.AQUA));
        }
        lines.add(HoverText.hint(heated ? "oil_processor" : "oil_processor_heat"));
        return lines;
    }

    public static void tick(Level level, BlockPos pos, BlockState state, OilProcessorBlockEntity processor) {
        if (level.isClientSide() || level.getGameTime() % 10L != 0L) return;
        processor.collectDroppedOil();
        if (processor.batchUnits == 0 && processor.tankUnits > 0) {
            processor.batchUnits = processor.tankUnits;
            processor.tankUnits = 0;
            processor.progress = 0;
            processor.setChanged();
        }

        processor.heated = isHeated(level, pos);
        boolean active = processor.batchUnits > 0 && processor.heated;
        if (state.getValue(OilProcessorBlock.ACTIVE) != active) {
            level.setBlock(pos, state.setValue(OilProcessorBlock.ACTIVE, active), 3);
        }
        if (active) {
            processor.progress += 10;
            if (processor.progress >= PROCESSING_TICKS) processor.finishProcessing();
            else if (processor.progress % 100 == 0) processor.setChanged();
        }
        processor.syncIfChanged();
    }

    /** 석유를 최대 amount개까지 탱크에 넣고 실제로 넣은 개수를 돌려준다. */
    public int addOil(int amount) {
        int inserted = Math.min(amount, TANK_CAPACITY - storedOil());
        if (inserted <= 0) return 0;
        tankUnits += inserted;
        setChanged();
        return inserted;
    }

    public int storedOil() {
        return tankUnits + batchUnits;
    }

    /** 위에 떨어진 석유 용기를 전부 비워 탱크에 넣는다. */
    private void collectDroppedOil() {
        AABB inputArea = new AABB(worldPosition.getX() + 0.1, worldPosition.getY() + 1.0,
                worldPosition.getZ() + 0.1, worldPosition.getX() + 0.9, worldPosition.getY() + 1.8,
                worldPosition.getZ() + 0.9);
        for (ItemEntity entity : level.getEntitiesOfClass(ItemEntity.class, inputArea)) {
            ItemStack stack = entity.getItem();
            if (!stack.is(ModItems.OIL)) continue;
            stack.shrink(addOil(stack.getCount()));
            if (stack.isEmpty()) entity.discard();
        }
    }

    // ---- 호퍼 입력: 슬롯 하나짜리 가상 인벤토리. 넣은 석유는 바로 탱크로 들어가므로 슬롯은 늘 비어 있다.

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
        return stack.is(ModItems.OIL) && storedOil() < TANK_CAPACITY;
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
        if (stack.is(ModItems.OIL)) addOil(stack.getCount());
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

    private static boolean isHeated(Level level, BlockPos pos) {
        BlockState heat = level.getBlockState(pos.below());
        return heat.is(Blocks.LAVA) || heat.is(Blocks.FIRE) || heat.is(Blocks.SOUL_FIRE)
                || heat.hasProperty(AbstractFurnaceBlock.LIT) && heat.getValue(AbstractFurnaceBlock.LIT)
                || heat.hasProperty(CampfireBlock.LIT) && heat.getValue(CampfireBlock.LIT);
    }

    private void finishProcessing() {
        drop(ModItems.DIESEL, batchUnits);
        drop(ModItems.PLASTIC_RESIN, batchUnits);
        drop(ModItems.ASPHALT, batchUnits);
        progress = 0;
        batchUnits = 0;
        setChanged();
        level.setBlock(worldPosition, getBlockState().setValue(OilProcessorBlock.ACTIVE, false), 3);
    }

    private void drop(net.minecraft.world.item.Item item, int count) {
        ItemEntity output = new ItemEntity(level, worldPosition.getX() + 0.5, worldPosition.getY() + 1.05,
                worldPosition.getZ() + 0.5, new ItemStack(item, count));
        level.addFreshEntity(output);
    }
}