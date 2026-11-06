package com.example.redstonetransportation.machine;

import com.example.redstonetransportation.RedstoneTransportation;
import com.example.redstonetransportation.content.ModContent;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.level.block.BarrelBlock;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.level.storage.TagValueInput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CookingFuel;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.level.block.LevelEvent;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.level.storage.loot.providers.number.ints.ResolvableInt;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * A machine that drives itself forward on fuel, cutting a nine-wide strip down to its own level and
 * filling the holes under it, so a flat road is left behind. It moves one block a step by placing
 * itself ahead and handing over everything it carries; whoever has its menu open keeps it open.
 */
public final class LandLevelerBlockEntity extends BaseContainerBlockEntity {
    public static final int FUEL_SLOT = 0;
    public static final int FIRST_STORAGE = 1;
    public static final int STORAGE_SLOTS = 27;
    /** One past the last storage slot. */
    public static final int STORAGE_END = FIRST_STORAGE + STORAGE_SLOTS;
    /**
     * The block the player paves with: the whole floor of the strip becomes it, topped up from storage.
     * The extra slots come last, so saves from earlier versions still line up.
     */
    public static final int FILL_SLOT = STORAGE_END;
    /** A flint and steel here burns everything it digs up instead of storing it. */
    public static final int LIGHTER_SLOT = FILL_SLOT + 1;
    public static final int SLOTS = LIGHTER_SLOT + 1;
    /** Blocks either side of the centre line: a nine-wide cut. */
    public static final int HALF_WIDTH = 4;
    /** How high above the new ground it clears, its own level included. */
    public static final int HEIGHT = 32;
    /** Something it will not cut this close to the ground stops it; higher up it only ends that column. */
    public static final int CLEARANCE = 2;
    public static final int STEP_TICKS = 20;
    /** Burn time one step costs, so a piece of coal (1600) lasts 80 blocks. */
    public static final int STEP_FUEL = 20;
    /** While rubble is being thrown away, this much is kept back for filling holes: four stacks. */
    public static final int RUBBLE_RESERVE = 256;
    /** Analog ALU's diesel has no furnace value; its generators burn one four times as long as coal. */
    public static final int DIESEL_TICKS = 6400;
    public static final TagKey<Item> RUBBLE = TagKey.create(Registries.ITEM,
            Identifier.fromNamespaceAndPath(RedstoneTransportation.MOD_ID, "leveler_rubble"));
    private static final Identifier DIESEL = Identifier.fromNamespaceAndPath("analog_alu", "diesel");
    private static final ItemStack TOOL = new ItemStack(Items.DIAMOND_PICKAXE);
    private static final Logger LOGGER = LoggerFactory.getLogger(RedstoneTransportation.MOD_ID);
    /** Break effects per step: enough to see it work without a wall of noise. */
    private static final int EFFECTS_PER_STEP = 9;

    public static final int DATA_RUNNING = 0;
    public static final int DATA_BURN_LEFT = 1;
    public static final int DATA_BURN_TOTAL = 2;
    public static final int DATA_DISCARD = 3;
    public static final int DATA_PROBLEM = 4;
    public static final int DATA_TRAVELLED = 5;
    public static final int DATA_CARGO = 6;
    public static final int DATA_COUNT = 7;
    public static final int BUTTON_RUN = 0;
    public static final int BUTTON_DISCARD = 1;

    /** Why the last step did not happen, or NONE when it did. */
    public enum Problem { NONE, WAITING, NO_FUEL, NO_FILL, FULL, OBSTACLE, PAVING_OUT }

    private NonNullList<ItemStack> items = NonNullList.withSize(SLOTS, ItemStack.EMPTY);
    private boolean running;
    private int burnLeft;
    private int burnTotal;
    private boolean discardRubble = true;
    private Problem problem = Problem.NONE;
    private int travelled;
    private int cooldown;
    /** The block being paved with, remembered after the fill slot runs dry so it can stop instead of changing road. */
    private Item paving;
    /** The fuel last burned, so the slot can be refilled with more of the same and never with the logs it dug up. */
    private Item fuelKind;
    private Link link = new Link(this);

    public LandLevelerBlockEntity(BlockPos pos, BlockState state) {
        super(ModContent.LAND_LEVELER_ENTITY, pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, LandLevelerBlockEntity leveler) {
        leveler.tick((ServerLevel) level, state);
    }

    private void tick(ServerLevel level, BlockState state) {
        if (!running) {
            cooldown = 0;
            showStatus(level, state, MachineStatus.OFF);
            return;
        }
        if (--cooldown > 0) return;
        cooldown = STEP_TICKS;
        problem = step(level, state);
        switch (problem) {
            case NONE -> moveTo(level, worldPosition.relative(facing(state)), state.setValue(MachineBlock.STATUS, MachineStatus.WORKING));
            case WAITING -> showStatus(level, state, MachineStatus.READY);
            default -> showStatus(level, state, MachineStatus.BLOCKED);
        }
    }

    private void showStatus(ServerLevel level, BlockState state, MachineStatus status) {
        if (state.getValue(MachineBlock.STATUS) != status) {
            level.setBlock(worldPosition, state.setValue(MachineBlock.STATUS, status), Block.UPDATE_ALL);
        }
    }

    private static Direction facing(BlockState state) {
        return state.getValue(MachineBlock.FACING);
    }

    /**
     * One block of progress: everything in the strip ahead is worked out first, and the world only
     * changes once the fuel, the room for what comes out and the filling for every hole are all there.
     */
    private Problem step(ServerLevel level, BlockState state) {
        Direction ahead = facing(state);
        Direction right = ahead.getClockWise();
        BlockPos front = worldPosition.relative(ahead);
        if (level.isOutsideBuildHeight(front.below())) return Problem.OBSTACLE;

        List<BlockPos> columns = new ArrayList<>();
        for (int offset = -HALF_WIDTH; offset <= HALF_WIDTH; offset++) {
            BlockPos base = front.relative(right, offset);
            if (!level.isLoaded(base)) return Problem.WAITING;
            columns.add(base);
        }

        Container cargo = cargo();
        Hold hold = new Hold(items, cargo);
        ItemStack fuel = hold.stock.get(FUEL_SLOT);
        if (burnTime(level, fuel) > 0) fuelKind = fuel.getItem();
        if (burnLeft < STEP_FUEL && burnTime(level, fuel) <= 0) {
            // Refill the fuel slot with more of the same fuel from storage or the chest on top.
            if (!hold.refill(FUEL_SLOT, fuelKind)) return Problem.NO_FUEL;
        }

        ItemStack chosen = items.get(FILL_SLOT);
        if (!chosen.isEmpty()) paving = chosen.getItem();
        Block pavingBlock = paving instanceof BlockItem item ? item.getBlock() : null;

        List<BlockPos> clear = new ArrayList<>();
        List<BlockPos> beds = new ArrayList<>();
        List<ItemStack> drops = new ArrayList<>();
        for (BlockPos base : columns) {
            for (int height = 0; height < HEIGHT; height++) {
                BlockPos pos = base.above(height);
                if (level.isOutsideBuildHeight(pos)) break;
                BlockState block = level.getBlockState(pos);
                if (block.isAir()) continue;
                if (!canClear(level, pos, block)) {
                    // In the way of the machine and anyone walking behind it, it stops. Higher up it is a
                    // ceiling, such as the Nether's bedrock or a tree house, left standing with what is on it.
                    if (height < CLEARANCE) return Problem.OBSTACLE;
                    break;
                }
                clear.add(pos);
                drops.addAll(Block.getDrops(block, level, pos, level.getBlockEntity(pos), null, TOOL));
            }
            // Holes always get filled. Paving also digs up the floor that is there and lays the chosen
            // block instead; what it cannot dig, such as bedrock, stays.
            BlockPos floor = base.below();
            BlockState ground = level.getBlockState(floor);
            boolean hole = ground.canBeReplaced();
            boolean repave = pavingBlock != null && !hole && !ground.is(pavingBlock);
            if (hole || repave) {
                if (!canClear(level, floor, ground)) {
                    if (hole) return Problem.OBSTACLE;
                    continue;
                }
                beds.add(floor);
                drops.addAll(Block.getDrops(ground, level, floor, level.getBlockEntity(floor), null, TOOL));
            }
        }

        List<Block> fills = new ArrayList<>();
        List<ItemStack> kept = new ArrayList<>();
        if (pavingBlock != null) {
            // Paving lays only the chosen block, refilling the slot from storage and the chest on top;
            // when that runs out it stops rather than carry on as a different road.
            while (fills.size() < beds.size()) {
                if (!hold.takePaving(paving)) return Problem.PAVING_OUT;
                fills.add(pavingBlock);
            }
            for (ItemStack drop : drops) kept.add(drop.copy());
        } else {
            // Holes take what this step digs up first, and only then rubble from storage.
            for (ItemStack drop : drops) {
                ItemStack rest = drop.copy();
                while (fills.size() < beds.size() && !rest.isEmpty() && isFill(rest)) {
                    fills.add(((BlockItem) rest.split(1).getItem()).getBlock());
                }
                if (!rest.isEmpty()) kept.add(rest);
            }
            while (fills.size() < beds.size()) {
                Block fill = hold.takeFill();
                if (fill == null) return Problem.NO_FILL;
                fills.add(fill);
            }
        }
        // A lighter burns what it digs up. Without a paving block it still keeps the rubble reserve,
        // or the next hole would have nothing to fill it.
        boolean lit = isLighter(hold.stock.get(LIGHTER_SLOT));
        boolean burning = false;
        int rubble = hold.countRubble();
        for (ItemStack stack : kept) {
            boolean reserve = stack.is(RUBBLE) && (lit ? pavingBlock == null : discardRubble);
            if (reserve) {
                // Past the reserve, and whatever finds no room, is thrown away, or burned.
                ItemStack keep = stack.split(Math.max(0, Math.min(stack.getCount(), RUBBLE_RESERVE - rubble)));
                int offered = keep.getCount();
                rubble += offered - hold.store(keep).getCount();
                burning |= lit && (!stack.isEmpty() || !keep.isEmpty());
                continue;
            }
            if (lit) {
                burning = true;
                continue;
            }
            if (!hold.store(stack).isEmpty()) return Problem.FULL;
        }

        hold.commit(items, cargo);
        burn(level);
        int effects = 0;
        for (BlockPos pos : clear) {
            BlockState old = level.getBlockState(pos);
            if (effects < EFFECTS_PER_STEP && old.getFluidState().isEmpty()) {
                level.levelEvent(LevelEvent.PARTICLES_DESTROY_BLOCK, pos, Block.getId(old));
                effects++;
            }
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        }
        for (int bed = 0; bed < beds.size(); bed++) {
            level.setBlock(beds.get(bed), fills.get(bed).defaultBlockState(), Block.UPDATE_ALL);
        }
        if (burning) {
            BlockPos fire = front.above();
            level.sendParticles(ParticleTypes.FLAME, fire.getX() + 0.5D, fire.getY() + 0.5D, fire.getZ() + 0.5D,
                    12, HALF_WIDTH * 0.5D, 0.6D, 0.4D, 0.01D);
            level.sendParticles(ParticleTypes.LARGE_SMOKE, fire.getX() + 0.5D, fire.getY() + 1.0D, fire.getZ() + 0.5D,
                    4, 1.0D, 0.4D, 0.4D, 0.01D);
        }
        travelled++;
        setChanged();
        return Problem.NONE;
    }

    /** Rails, storage and anything from this mod or Analog ALU are someone's work, so they stop it instead. */
    private static boolean canClear(ServerLevel level, BlockPos pos, BlockState state) {
        if (state.getDestroySpeed(level, pos) < 0.0F || state.is(BlockTags.RAILS)) return false;
        String namespace = BuiltInRegistries.BLOCK.getKey(state.getBlock()).getNamespace();
        if (RedstoneTransportation.MOD_ID.equals(namespace) || "analog_alu".equals(namespace)) return false;
        BlockEntity entity = level.getBlockEntity(pos);
        return !(entity instanceof Container);
    }

    /** Rubble that stays where it is put; sand and gravel would only fall back into the hole. */
    public static boolean isFill(ItemStack stack) {
        return stack.is(RUBBLE) && isFillBlock(stack);
    }

    /**
     * What the fill slot takes: any whole, solid block that stays put. Slabs, glass panes and the like
     * would leave gaps, sand and gravel would fall, and chests and other block entities would come out empty.
     */
    public static boolean isFillBlock(ItemStack stack) {
        if (!(stack.getItem() instanceof BlockItem item)) return false;
        Block block = item.getBlock();
        BlockState state = block.defaultBlockState();
        return !(block instanceof FallingBlock) && !state.hasBlockEntity() && !state.isAir()
                && state.isCollisionShapeFullBlock(EmptyBlockGetter.INSTANCE, BlockPos.ZERO);
    }

    public static boolean isLighter(ItemStack stack) {
        return stack.is(Items.FLINT_AND_STEEL);
    }

    /** A chest, barrel or shulker box sitting on the machine rides along as more storage. */
    public static boolean isCargo(BlockState state) {
        Block block = state.getBlock();
        return block instanceof ChestBlock || block instanceof BarrelBlock || block instanceof ShulkerBoxBlock;
    }

    /** The container riding on top, or null with none. */
    public Container cargo() {
        if (level == null) return null;
        BlockPos above = worldPosition.above();
        if (!isCargo(level.getBlockState(above))) return null;
        return level.getBlockEntity(above) instanceof Container container ? container : null;
    }

    /**
     * Storage as one step sees it: copies of the machine's own slots and of the chest on top, written
     * back only once the step is sure to happen. Storage is always tried before the chest.
     */
    private static final class Hold {
        private final NonNullList<ItemStack> stock = NonNullList.withSize(SLOTS, ItemStack.EMPTY);
        private final NonNullList<ItemStack> load;

        private Hold(NonNullList<ItemStack> items, Container cargo) {
            for (int slot = 0; slot < SLOTS; slot++) stock.set(slot, items.get(slot).copy());
            load = NonNullList.withSize(cargo == null ? 0 : cargo.getContainerSize(), ItemStack.EMPTY);
            for (int slot = 0; slot < load.size(); slot++) load.set(slot, cargo.getItem(slot).copy());
        }

        /** Storage slots, then the chest's. */
        private int places() {
            return STORAGE_SLOTS + load.size();
        }

        private ItemStack get(int place) {
            return place < STORAGE_SLOTS ? stock.get(FIRST_STORAGE + place) : load.get(place - STORAGE_SLOTS);
        }

        private void set(int place, ItemStack stack) {
            if (place < STORAGE_SLOTS) {
                stock.set(FIRST_STORAGE + place, stack);
            } else {
                load.set(place - STORAGE_SLOTS, stack);
            }
        }

        /**
         * Fills an empty special slot with a stack of the same item out of storage or the chest. An empty
         * bucket left behind by lava is put away first to make room.
         */
        private boolean refill(int slot, Item kind) {
            if (kind == null) return false;
            ItemStack held = stock.get(slot);
            if (!held.isEmpty() && !held.is(kind)) {
                if (!store(held.copy()).isEmpty()) return false;
                stock.set(slot, ItemStack.EMPTY);
            }
            if (!stock.get(slot).isEmpty()) return true;
            for (int place = 0; place < places(); place++) {
                if (get(place).is(kind)) {
                    stock.set(slot, get(place));
                    set(place, ItemStack.EMPTY);
                    return true;
                }
            }
            return false;
        }

        /** Takes one paving block, refilling the fill slot when it has run dry. */
        private boolean takePaving(Item paving) {
            if (stock.get(FILL_SLOT).isEmpty() && !refill(FILL_SLOT, paving)) return false;
            ItemStack slot = stock.get(FILL_SLOT);
            if (!slot.is(paving)) return false;
            slot.shrink(1);
            return true;
        }

        private Block takeFill() {
            for (int place = 0; place < places(); place++) {
                ItemStack stack = get(place);
                if (isFill(stack)) return ((BlockItem) stack.split(1).getItem()).getBlock();
            }
            return null;
        }

        private int countRubble() {
            int count = 0;
            for (int place = 0; place < places(); place++) {
                if (get(place).is(RUBBLE)) count += get(place).getCount();
            }
            return count;
        }

        /** Puts a stack away, topping up matching stacks first; returns what did not fit. */
        private ItemStack store(ItemStack stack) {
            for (int place = 0; place < places() && !stack.isEmpty(); place++) {
                ItemStack held = get(place);
                if (!held.isEmpty() && ItemStack.isSameItemSameComponents(held, stack)) {
                    int moved = Math.min(stack.getCount(), held.getMaxStackSize() - held.getCount());
                    held.grow(moved);
                    stack.shrink(moved);
                }
            }
            for (int place = 0; place < places() && !stack.isEmpty(); place++) {
                if (get(place).isEmpty()) set(place, stack.split(stack.getMaxStackSize()));
            }
            return stack;
        }

        private void commit(NonNullList<ItemStack> items, Container cargo) {
            for (int slot = 0; slot < SLOTS; slot++) items.set(slot, stock.get(slot));
            if (cargo == null) return;
            for (int slot = 0; slot < load.size(); slot++) {
                if (!ItemStack.matches(cargo.getItem(slot), load.get(slot))) cargo.setItem(slot, load.get(slot));
            }
            cargo.setChanged();
        }
    }

    private void burn(ServerLevel level) {
        if (burnLeft < STEP_FUEL) {
            ItemStack fuel = items.get(FUEL_SLOT);
            int ticks = burnTime(level, fuel);
            ItemStackTemplate remainder = fuel.getItem().getCraftingRemainder();
            fuel.shrink(1);
            if (remainder != null) {
                if (fuel.isEmpty()) {
                    items.set(FUEL_SLOT, remainder.create());
                } else {
                    Containers.dropItemStack(level, worldPosition.getX(), worldPosition.getY() + 1, worldPosition.getZ(), remainder.create());
                }
            }
            burnLeft += ticks;
            burnTotal = burnLeft;
        }
        burnLeft -= STEP_FUEL;
    }

    public static boolean isFuel(ItemStack stack) {
        return stack.has(DataComponents.COOKING_FUEL) || DIESEL.equals(BuiltInRegistries.ITEM.getKey(stack.getItem()));
    }

    /** Burn ticks one piece of this gives, the same a furnace would get, or ALU's figure for diesel. */
    public int burnTime(ServerLevel level, ItemStack stack) {
        if (stack.isEmpty()) return 0;
        if (DIESEL.equals(BuiltInRegistries.ITEM.getKey(stack.getItem()))) return DIESEL_TICKS;
        return ResolvableInt.getFromItem(stack, DataComponents.COOKING_FUEL, CookingFuel::burnTime, getLootContext(level), 0);
    }

    /**
     * Puts the machine on the block ahead and hands it everything, so nothing drops when this one goes.
     * The open menus follow through the shared link.
     */
    private void moveTo(ServerLevel level, BlockPos target, BlockState state) {
        level.setBlock(target, state, Block.UPDATE_ALL);
        if (!(level.getBlockEntity(target) instanceof LandLevelerBlockEntity next)) return;
        next.items = items;
        next.running = running;
        next.burnLeft = burnLeft;
        next.burnTotal = burnTotal;
        next.discardRubble = discardRubble;
        next.problem = problem;
        next.travelled = travelled;
        next.paving = paving;
        next.fuelKind = fuelKind;
        next.cooldown = cooldown;
        next.link = link;
        link.owner = next;
        next.setChanged();
        items = NonNullList.withSize(SLOTS, ItemStack.EMPTY);
        link = new Link(this);
        moveCargo(level, worldPosition.above(), target.above());
        level.setBlock(worldPosition, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
    }

    /**
     * Carries the chest on top along, contents, name and all. If something has got into the space it
     * would go to, it stays behind rather than crush anything.
     */
    private static void moveCargo(ServerLevel level, BlockPos from, BlockPos to) {
        BlockState state = level.getBlockState(from);
        if (!isCargo(state) || !level.getBlockState(to).canBeReplaced()) return;
        BlockEntity chest = level.getBlockEntity(from);
        if (chest == null) return;
        CompoundTag data = chest.saveCustomOnly(level.registryAccess());
        DataComponentMap components = chest.components();
        if (chest instanceof Container container) container.clearContent();
        if (state.hasProperty(ChestBlock.TYPE)) state = state.setValue(ChestBlock.TYPE, ChestType.SINGLE);
        level.setBlock(from, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        level.setBlock(to, state, Block.UPDATE_ALL);
        BlockEntity moved = level.getBlockEntity(to);
        if (moved == null) return;
        try (ProblemReporter.ScopedCollector reporter = new ProblemReporter.ScopedCollector(moved.problemPath(), LOGGER)) {
            moved.loadCustomOnly(TagValueInput.create(reporter, level.registryAccess(), data));
        }
        moved.setComponents(components);
        moved.setChanged();
    }

    /** What the menu's two buttons do. */
    public void press(int button) {
        if (button == BUTTON_RUN) {
            running = !running;
            cooldown = 0;
            problem = Problem.NONE;
            // Starting again with an empty fill slot is how a road that ran out goes on unpaved.
            if (running && items.get(FILL_SLOT).isEmpty()) paving = null;
        } else if (button == BUTTON_DISCARD) {
            discardRubble = !discardRubble;
        }
        setChanged();
    }

    public boolean isRunning() {
        return running;
    }

    public Problem problem() {
        return problem;
    }

    public int travelled() {
        return travelled;
    }

    public boolean discardsRubble() {
        return discardRubble;
    }

    private int data(int index) {
        return switch (index) {
            case DATA_RUNNING -> running ? 1 : 0;
            case DATA_BURN_LEFT -> Math.min(burnLeft, Short.MAX_VALUE);
            case DATA_BURN_TOTAL -> Math.min(burnTotal, Short.MAX_VALUE);
            case DATA_DISCARD -> discardRubble ? 1 : 0;
            case DATA_PROBLEM -> problem.ordinal();
            case DATA_TRAVELLED -> Math.min(travelled, Short.MAX_VALUE);
            case DATA_CARGO -> cargo() != null ? 1 : 0;
            default -> 0;
        };
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        items = NonNullList.withSize(SLOTS, ItemStack.EMPTY);
        ContainerHelper.loadAllItems(input, items);
        running = input.getBooleanOr("running", false);
        burnLeft = input.getIntOr("burn_left", 0);
        burnTotal = input.getIntOr("burn_total", 0);
        discardRubble = input.getBooleanOr("discard_rubble", true);
        travelled = input.getIntOr("travelled", 0);
        int saved = input.getIntOr("problem", 0);
        problem = saved >= 0 && saved < Problem.values().length ? Problem.values()[saved] : Problem.NONE;
        paving = savedItem(input, "paving");
        fuelKind = savedItem(input, "fuel_kind");
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        ContainerHelper.saveAllItems(output, items);
        output.putBoolean("running", running);
        output.putInt("burn_left", burnLeft);
        output.putInt("burn_total", burnTotal);
        output.putBoolean("discard_rubble", discardRubble);
        output.putInt("travelled", travelled);
        output.putInt("problem", problem.ordinal());
        if (paving != null) output.putString("paving", BuiltInRegistries.ITEM.getKey(paving).toString());
        if (fuelKind != null) output.putString("fuel_kind", BuiltInRegistries.ITEM.getKey(fuelKind).toString());
    }

    private static Item savedItem(ValueInput input, String key) {
        return input.getString(key)
                .map(Identifier::tryParse)
                .flatMap(id -> id == null ? Optional.<Item>empty() : BuiltInRegistries.ITEM.getOptional(id))
                .orElse(null);
    }

    @Override
    protected Component getDefaultName() {
        return getBlockState().getBlock().getName();
    }

    @Override
    protected AbstractContainerMenu createMenu(int containerId, Inventory inventory) {
        return new LandLevelerMenu(containerId, inventory, link);
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
        if (slot == FUEL_SLOT) return isFuel(stack);
        if (slot == FILL_SLOT) return isFillBlock(stack);
        if (slot == LIGHTER_SLOT) return isLighter(stack);
        return true;
    }

    /** Emptying the fill slot by hand stops the paving; the machine's own use of it does not come through here. */
    @Override
    public ItemStack removeItem(int slot, int count) {
        ItemStack removed = super.removeItem(slot, count);
        if (slot == FILL_SLOT && items.get(FILL_SLOT).isEmpty()) paving = null;
        return removed;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        ItemStack removed = super.removeItemNoUpdate(slot);
        if (slot == FILL_SLOT) paving = null;
        return removed;
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        super.setItem(slot, stack);
        if (slot == FILL_SLOT) paving = stack.isEmpty() ? null : stack.getItem();
    }

    /**
     * What an open menu holds on to instead of the block entity itself. The machine becomes a new
     * block entity every block it moves, and this link is handed along so the menu stays open.
     */
    public static final class Link implements Container {
        private LandLevelerBlockEntity owner;
        private final ContainerData data = new ContainerData() {
            @Override
            public int get(int index) {
                return owner.data(index);
            }

            @Override
            public void set(int index, int value) {}

            @Override
            public int getCount() {
                return DATA_COUNT;
            }
        };

        private Link(LandLevelerBlockEntity owner) {
            this.owner = owner;
        }

        public ContainerData data() {
            return data;
        }

        public void press(int button) {
            owner.press(button);
        }

        @Override
        public int getContainerSize() {
            return owner.getContainerSize();
        }

        @Override
        public boolean isEmpty() {
            return owner.isEmpty();
        }

        @Override
        public ItemStack getItem(int slot) {
            return owner.getItem(slot);
        }

        @Override
        public ItemStack removeItem(int slot, int count) {
            return owner.removeItem(slot, count);
        }

        @Override
        public ItemStack removeItemNoUpdate(int slot) {
            return owner.removeItemNoUpdate(slot);
        }

        @Override
        public void setItem(int slot, ItemStack stack) {
            owner.setItem(slot, stack);
        }

        @Override
        public boolean canPlaceItem(int slot, ItemStack stack) {
            return owner.canPlaceItem(slot, stack);
        }

        @Override
        public void setChanged() {
            owner.setChanged();
        }

        @Override
        public boolean stillValid(Player player) {
            return owner.stillValid(player);
        }

        @Override
        public void clearContent() {
            owner.clearContent();
        }
    }
}
