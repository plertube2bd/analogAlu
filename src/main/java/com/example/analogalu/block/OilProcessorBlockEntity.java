package com.example.analogalu.block;

import com.example.analogalu.ModBlocks;
import com.example.analogalu.ModItems;
import com.example.analogalu.item.TankBucketItem;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.AbstractFurnaceBlock;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;

public final class OilProcessorBlockEntity extends BlockEntity {
    private static final int PROCESSING_TICKS = 400;
    public static final int TANK_CAPACITY = 64;
    /** 대기 중인 석유(양동이 단위). */
    private int tankUnits;
    /** 지금 가공 중인 석유. 가공이 끝나면 이 개수만큼 디젤/플라스틱/아스팔트가 나온다. */
    private int batchUnits;
    private int progress;

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
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("TankUnits", tankUnits);
        output.putInt("BatchUnits", batchUnits);
        output.putInt("Progress", progress);
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

        boolean active = processor.batchUnits > 0 && isHeated(level, pos);
        if (state.getValue(OilProcessorBlock.ACTIVE) != active) {
            level.setBlock(pos, state.setValue(OilProcessorBlock.ACTIVE, active), 3);
        }
        if (!active) return;

        processor.progress += 10;
        if (processor.progress >= PROCESSING_TICKS) processor.finishProcessing();
        else if (processor.progress % 100 == 0) processor.setChanged();
    }

    /** 석유가 든 용기 1개에 들어 있는 양. 석유 용기가 아니면 0. */
    public static int oilAmount(ItemStack stack) {
        if (stack.is(ModItems.OIL_BUCKET)) return 1;
        if (stack.getItem() instanceof TankBucketItem && TankBucketItem.storedFluid(stack).equals("oil")) {
            return Math.max(0, TankBucketItem.storedAmount(stack));
        }
        return 0;
    }

    public static ItemStack emptiedContainer(ItemStack stack) {
        return stack.is(ModItems.OIL_BUCKET) ? new ItemStack(Items.BUCKET) : TankBucketItem.emptyContainer(stack);
    }

    /** 석유를 탱크에 넣는다. 공간이 부족하면 넣지 않고 false. */
    public boolean addOil(int amount) {
        if (amount <= 0 || tankUnits + amount > TANK_CAPACITY) return false;
        tankUnits += amount;
        setChanged();
        return true;
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
            int amount = oilAmount(stack);
            while (amount > 0 && !stack.isEmpty() && addOil(amount)) {
                ItemStack empty = emptiedContainer(stack);
                stack.shrink(1);
                level.addFreshEntity(new ItemEntity(level, worldPosition.getX() + 0.5, worldPosition.getY() + 1.0,
                        worldPosition.getZ() + 0.5, empty));
            }
            if (stack.isEmpty()) entity.discard();
        }
    }

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