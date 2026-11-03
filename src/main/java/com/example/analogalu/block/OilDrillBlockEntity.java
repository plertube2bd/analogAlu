package com.example.analogalu.block;

import com.example.analogalu.ModBlocks;
import com.example.analogalu.ModFluids;
import com.example.analogalu.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public final class OilDrillBlockEntity extends BlockEntity {
    private static final int DRILL_INTERVAL = 600;
    private long nextDrillTick;

    public OilDrillBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlocks.OIL_DRILL_ENTITY, pos, state);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        nextDrillTick = input.getLongOr("NextDrillTick", 0L);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putLong("NextDrillTick", nextDrillTick);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, OilDrillBlockEntity drill) {
        if (level.isClientSide() || level.getGameTime() < drill.nextDrillTick) return;
        for (int depth = 1; depth <= 64 && pos.getY() - depth > level.getMinY(); depth++) {
            BlockPos target = pos.below(depth);
            if (!level.getFluidState(target).isSourceOfType(ModFluids.STILL)) continue;
            level.setBlock(target, Blocks.AIR.defaultBlockState(), 3);
            ItemEntity output = new ItemEntity(level, pos.getX() + 0.5, pos.getY() + 1.05,
                    pos.getZ() + 0.5, new ItemStack(ModItems.OIL));
            level.addFreshEntity(output);
            drill.nextDrillTick = level.getGameTime() + DRILL_INTERVAL;
            drill.setChanged();
            return;
        }
        drill.nextDrillTick = level.getGameTime() + 100L;
    }
}