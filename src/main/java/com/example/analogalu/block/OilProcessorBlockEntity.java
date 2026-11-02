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
    private boolean hasOil;
    private int progress;
    private int oilUnits;

    public OilProcessorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlocks.OIL_PROCESSOR_ENTITY, pos, state);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        hasOil = input.getBooleanOr("HasOil", false);
        progress = Math.clamp(input.getIntOr("Progress", 0), 0, PROCESSING_TICKS);
        oilUnits = Math.clamp(input.getIntOr("OilUnits", 0), 0, 16);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putBoolean("HasOil", hasOil);
        output.putInt("Progress", progress);
        output.putInt("OilUnits", oilUnits);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, OilProcessorBlockEntity processor) {
        if (level.isClientSide() || level.getGameTime() % 10L != 0L) return;
        if (!processor.hasOil && processor.collectOil()) {
            processor.hasOil = true;
            processor.progress = 0;
            processor.setChanged();
        }

        boolean active = processor.hasOil && isHeated(level, pos);
        if (state.getValue(OilProcessorBlock.ACTIVE) != active) {
            level.setBlock(pos, state.setValue(OilProcessorBlock.ACTIVE, active), 3);
        }
        if (!active) return;

        processor.progress += 10;
        if (processor.progress >= PROCESSING_TICKS) processor.finishProcessing();
        else if (processor.progress % 100 == 0) processor.setChanged();
    }

    private boolean collectOil() {
        AABB inputArea = new AABB(worldPosition.getX() + 0.1, worldPosition.getY() + 1.0,
                worldPosition.getZ() + 0.1, worldPosition.getX() + 0.9, worldPosition.getY() + 1.8,
                worldPosition.getZ() + 0.9);
        for (ItemEntity entity : level.getEntitiesOfClass(ItemEntity.class, inputArea)) {
            ItemStack stack = entity.getItem();
            int amount;
            ItemStack emptyContainer;
            if (stack.is(ModItems.OIL_BUCKET)) {
                amount = 1;
                emptyContainer = new ItemStack(Items.BUCKET);
            } else if (stack.getItem() instanceof TankBucketItem
                    && TankBucketItem.storedFluid(stack).equals("oil")) {
                amount = TankBucketItem.storedAmount(stack);
                if (amount <= 0) continue;
                emptyContainer = TankBucketItem.emptyContainer(stack);
            } else {
                continue;
            }
            stack.shrink(1);
            if (stack.isEmpty()) entity.discard();
            level.addFreshEntity(new ItemEntity(level, worldPosition.getX() + 0.5, worldPosition.getY() + 1.0,
                    worldPosition.getZ() + 0.5, emptyContainer));
            oilUnits = amount;
            return true;
        }
        return false;
    }

    private static boolean isHeated(Level level, BlockPos pos) {
        BlockState heat = level.getBlockState(pos.below());
        return heat.is(Blocks.LAVA) || heat.is(Blocks.FIRE) || heat.is(Blocks.SOUL_FIRE)
                || heat.hasProperty(AbstractFurnaceBlock.LIT) && heat.getValue(AbstractFurnaceBlock.LIT)
                || heat.hasProperty(CampfireBlock.LIT) && heat.getValue(CampfireBlock.LIT);
    }

    private void finishProcessing() {
        drop(ModItems.DIESEL, oilUnits);
        drop(ModItems.PLASTIC_RESIN, oilUnits);
        drop(ModItems.ASPHALT, oilUnits);
        hasOil = false;
        progress = 0;
        oilUnits = 0;
        setChanged();
        level.setBlock(worldPosition, getBlockState().setValue(OilProcessorBlock.ACTIVE, false), 3);
    }

    private void drop(net.minecraft.world.item.Item item, int count) {
        ItemEntity output = new ItemEntity(level, worldPosition.getX() + 0.5, worldPosition.getY() + 1.05,
                worldPosition.getZ() + 0.5, new ItemStack(item, count));
        level.addFreshEntity(output);
    }
}