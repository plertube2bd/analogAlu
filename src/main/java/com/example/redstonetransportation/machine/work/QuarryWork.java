package com.example.redstonetransportation.machine.work;

import com.example.redstonetransportation.RedstoneTransportation;
import com.example.redstonetransportation.machine.MachineBlockEntity;
import com.example.redstonetransportation.machine.MachineWork;
import com.example.redstonetransportation.machine.WorkArea;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import java.util.List;

/** Digs the work area layer by layer down to bedrock, one block per cycle. */
public final class QuarryWork implements MachineWork {
    private static final ItemStack TOOL = new ItemStack(Items.DIAMOND_PICKAXE);
    private static final int NOT_STARTED = Integer.MAX_VALUE;

    private int layerY = NOT_STARTED;

    @Override
    public Result run(MachineBlockEntity machine, ServerLevel level) {
        if (!machine.hasRoom()) return Result.BLOCKED;
        if (layerY == NOT_STARTED) layerY = machine.getBlockPos().getY() - 1;

        // A worked-out layer costs nothing extra, so empty layers are skipped within one cycle.
        while (layerY >= level.getMinY()) {
            for (BlockPos pos : WorkArea.layer(machine, layerY)) {
                BlockState state = level.getBlockState(pos);
                if (!canMine(level, pos, state)) continue;
                List<ItemStack> drops = Block.getDrops(state, level, pos, null, null, TOOL);
                level.destroyBlock(pos, false);
                machine.store(level, drops);
                return Result.WORKED;
            }
            layerY--;
        }
        return Result.IDLE;
    }

    /** Leaves rails, containers and other machines standing even when they are inside the pit. */
    private static boolean canMine(ServerLevel level, BlockPos pos, BlockState state) {
        return !state.isAir()
                && state.getFluidState().isEmpty()
                && state.getDestroySpeed(level, pos) >= 0.0F
                && !state.hasBlockEntity()
                && !state.is(BlockTags.RAILS)
                && !RedstoneTransportation.MOD_ID.equals(BuiltInRegistries.BLOCK.getKey(state.getBlock()).getNamespace());
    }

    @Override
    public void save(ValueOutput output) {
        output.putInt("quarry_layer", layerY);
    }

    @Override
    public void load(ValueInput input) {
        layerY = input.getIntOr("quarry_layer", NOT_STARTED);
    }
}
