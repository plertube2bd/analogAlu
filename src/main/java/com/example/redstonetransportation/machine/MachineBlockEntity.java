package com.example.redstonetransportation.machine;

import com.example.redstonetransportation.block.ConveyorBeltBlock;
import com.example.redstonetransportation.content.ModContent;
import com.example.redstonetransportation.integration.analogalu.AnalogAluSignalAdapter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.stream.IntStream;

/**
 * The frame every machine runs in: a 9-slot buffer, signal-driven work cycles, and the item
 * routing between neighbours. The job itself lives in a {@link MachineWork}.
 */
public final class MachineBlockEntity extends BaseContainerBlockEntity implements WorldlyContainer {
    private static final int SLOTS = 9;
    private static final int[] SLOT_INDEXES = IntStream.range(0, SLOTS).toArray();
    private static final int TRANSFER_INTERVAL = 8;
    private static final int TRANSFER_ITEMS = 8;
    private static final int IDLE_PAUSE = 20;
    /** A factory controller refreshes its link well inside this many ticks. */
    private static final int LINK_TICKS = 50;

    private final MachineType type;
    private final MachineWork work;
    private NonNullList<ItemStack> items = NonNullList.withSize(SLOTS, ItemStack.EMPTY);
    private int workCooldown;
    private int transferCooldown;
    private int linkedSignal;
    private int linkTicks;
    private BlockPos linkedAreaCenter;

    public MachineBlockEntity(BlockPos pos, BlockState state) {
        super(ModContent.MACHINE_ENTITY, pos, state);
        this.type = ((MachineBlock) state.getBlock()).type();
        this.work = type.createWork();
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, MachineBlockEntity machine) {
        machine.tick((ServerLevel) level, state);
    }

    private void tick(ServerLevel level, BlockState state) {
        if (--transferCooldown <= 0) {
            transferCooldown = TRANSFER_INTERVAL;
            moveItems(level, state.getValue(MachineBlock.FACING));
        }

        int signal = workSignal(level);
        MachineStatus status = state.getValue(MachineBlock.STATUS);
        if (signal <= 0) {
            status = MachineStatus.OFF;
            workCooldown = 0;
        } else if (--workCooldown <= 0) {
            switch (work.run(this, level)) {
                case WORKED -> {
                    status = MachineStatus.WORKING;
                    workCooldown = type.interval(signal);
                }
                case IDLE -> {
                    status = MachineStatus.READY;
                    workCooldown = IDLE_PAUSE;
                }
                case BLOCKED -> {
                    status = MachineStatus.BLOCKED;
                    workCooldown = IDLE_PAUSE;
                }
            }
        }
        if (status != state.getValue(MachineBlock.STATUS)) setState(level, state.setValue(MachineBlock.STATUS, status));
    }

    private int workSignal(ServerLevel level) {
        if (linkTicks > 0) {
            linkTicks--;
            return linkedSignal;
        }
        return AnalogAluSignalAdapter.readBestNeighborSignal(level, worldPosition);
    }

    /** Called by a factory controller; its signal and work area replace this machine's own while the link lasts. */
    public void link(int signal, BlockPos areaCenter) {
        linkedSignal = signal;
        linkTicks = LINK_TICKS;
        linkedAreaCenter = areaCenter;
    }

    /** The centre of the work area shared with linked machines, or null when working alone. */
    public BlockPos sharedAreaCenter() {
        return linkTicks > 0 ? linkedAreaCenter : null;
    }

    public MachineType type() {
        return type;
    }

    public Direction facing() {
        return getBlockState().getValue(MachineBlock.FACING);
    }

    private void setState(ServerLevel level, BlockState state) {
        level.setBlock(worldPosition, state, Block.UPDATE_ALL);
    }

    private void moveItems(ServerLevel level, Direction facing) {
        if (type.role() == MachineType.Role.CONSUMER) {
            pullFromNeighbors(level, facing);
        } else if (!pushInto(level, facing, true)) {
            pushInto(level, facing, false);
        }
    }

    private void pullFromNeighbors(ServerLevel level, Direction facing) {
        for (Direction side : Direction.values()) {
            if (side == facing) continue;
            Container neighbor = HopperBlockEntity.getContainerAt(level, worldPosition.relative(side));
            if (neighbor != null && !(neighbor instanceof MachineBlockEntity)
                    && ItemTransfer.move(neighbor, this, side.getOpposite(), null, TRANSFER_ITEMS, type::accepts)) {
                return;
            }
        }
    }

    /**
     * Neighbouring machines are served before storage. Otherwise a chest would swallow every
     * sapling and the planter next to the cutter would run dry.
     */
    private boolean pushInto(ServerLevel level, Direction facing, boolean machines) {
        for (Direction side : Direction.values()) {
            if (side == facing) continue;
            BlockPos neighborPos = worldPosition.relative(side);
            Container neighbor = HopperBlockEntity.getContainerAt(level, neighborPos);
            if (neighbor != null) {
                if (neighbor instanceof MachineBlockEntity == machines
                        && ItemTransfer.move(this, neighbor, side.getOpposite(), TRANSFER_ITEMS)) {
                    return true;
                }
            } else if (!machines && level.getBlockState(neighborPos).getBlock() instanceof ConveyorBeltBlock
                    && dropOnBelt(level, neighborPos)) {
                return true;
            }
        }
        return false;
    }


    private boolean dropOnBelt(ServerLevel level, BlockPos beltPos) {
        for (int slot = 0; slot < SLOTS; slot++) {
            ItemStack stack = items.get(slot);
            if (stack.isEmpty()) continue;
            ItemStack moved = removeItem(slot, TRANSFER_ITEMS);
            Vec3 spawn = Vec3.upFromBottomCenterOf(beltPos, 0.35D);
            ItemEntity item = new ItemEntity(level, spawn.x, spawn.y, spawn.z, moved, 0.0D, 0.0D, 0.0D);
            level.addFreshEntity(item);
            return true;
        }
        return false;
    }

    /** True while at least one more drop can be stored without displacing anything. */
    public boolean hasRoom() {
        return items.stream().anyMatch(ItemStack::isEmpty);
    }

    /** Stores harvested items; anything that does not fit drops next to the machine instead of being lost. */
    public void store(ServerLevel level, List<ItemStack> drops) {
        for (ItemStack drop : drops) {
            ItemStack rest = HopperBlockEntity.addItem(null, this, drop, null);
            if (!rest.isEmpty()) Block.popResource(level, worldPosition.above(), rest);
        }
        setChanged();
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        items = NonNullList.withSize(SLOTS, ItemStack.EMPTY);
        ContainerHelper.loadAllItems(input, items);
        work.load(input);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        ContainerHelper.saveAllItems(output, items);
        work.save(output);
    }

    @Override
    protected Component getDefaultName() {
        return getBlockState().getBlock().getName();
    }

    @Override
    protected AbstractContainerMenu createMenu(int containerId, Inventory inventory) {
        return new ChestMenu(MenuType.GENERIC_9x1, containerId, inventory, this, 1);
    }

    @Override
    protected NonNullList<ItemStack> getItems() {
        return items;
    }

    @Override
    protected void setItems(NonNullList<ItemStack> newItems) {
        items = newItems;
    }

    @Override
    public int getContainerSize() {
        return SLOTS;
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return type.accepts(stack);
    }

    @Override
    public int[] getSlotsForFace(Direction face) {
        return SLOT_INDEXES;
    }

    /** Only consumers take items in through a face; a producer's buffer holds only what it harvested. */
    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, Direction face) {
        // A null face is the machine storing its own harvest.
        return (face == null || type.role() == MachineType.Role.CONSUMER) && type.accepts(stack);
    }

    /** Only producers give items out through a face; a consumer's buffer is its feedstock. */
    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction face) {
        return type.role() == MachineType.Role.PRODUCER;
    }
}
