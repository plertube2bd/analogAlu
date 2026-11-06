package com.example.redstonetransportation.machine.work;

import com.example.redstonetransportation.machine.MachineBlockEntity;
import com.example.redstonetransportation.machine.MachineWork;
import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Fells a whole tree whose trunk touches any face of the machine: leaves first so saplings
 * reach the planter, then logs from the top down. Log structures without natural leaves are
 * left alone, so a player's log building against the machine is safe.
 */
public final class TreeCutterWork implements MachineWork {
    private static final ItemStack TOOL = new ItemStack(Items.DIAMOND_AXE);
    private static final Codec<List<BlockPos>> TARGETS_CODEC = BlockPos.CODEC.listOf();
    private static final int MAX_LOGS = 256;
    private static final int MAX_LEAVES = 1024;
    private static final int LEAF_REACH = 6;
    private static final int LEAVES_PER_CYCLE = 8;

    private final ArrayDeque<BlockPos> targets = new ArrayDeque<>();

    @Override
    public Result run(MachineBlockEntity machine, ServerLevel level) {
        if (!machine.hasRoom()) return Result.BLOCKED;
        if (targets.isEmpty() && !findTree(machine, level)) return Result.IDLE;

        int leaves = LEAVES_PER_CYCLE;
        while (leaves > 0 && !targets.isEmpty()) {
            BlockPos pos = targets.pollFirst();
            BlockState state = level.getBlockState(pos);
            if (state.is(BlockTags.LOGS)) {
                harvest(machine, level, pos, state);
                break;
            }
            if (isNaturalLeaf(state)) {
                harvest(machine, level, pos, state);
                leaves--;
            }
        }
        return Result.WORKED;
    }

    private static void harvest(MachineBlockEntity machine, ServerLevel level, BlockPos pos, BlockState state) {
        List<ItemStack> drops = Block.getDrops(state, level, pos, null, null, TOOL);
        level.destroyBlock(pos, false);
        machine.store(level, drops);
    }

    private boolean findTree(MachineBlockEntity machine, ServerLevel level) {
        for (Direction side : Direction.values()) {
            BlockPos touching = machine.getBlockPos().relative(side);
            if (level.getBlockState(touching).is(BlockTags.LOGS) && collectTree(level, touching)) return true;
        }
        return false;
    }

    /** The trunk may touch at any height, so the search spreads both up and down from there. */
    private boolean collectTree(ServerLevel level, BlockPos start) {
        List<BlockPos> logs = new ArrayList<>();
        Set<BlockPos> seen = new HashSet<>();
        ArrayDeque<BlockPos> frontier = new ArrayDeque<>();
        frontier.add(start);
        seen.add(start);
        while (!frontier.isEmpty() && logs.size() < MAX_LOGS) {
            BlockPos pos = frontier.removeFirst();
            logs.add(pos);
            for (BlockPos near : BlockPos.betweenClosed(pos.offset(-1, -1, -1), pos.offset(1, 1, 1))) {
                if (level.getBlockState(near).is(BlockTags.LOGS) && seen.add(near.immutable())) {
                    frontier.addLast(near.immutable());
                }
            }
        }

        List<BlockPos> leaves = collectLeaves(level, logs);
        if (leaves.isEmpty()) return false;

        targets.addAll(leaves);
        logs.sort(Comparator.comparingInt((BlockPos pos) -> pos.getY()).reversed());
        targets.addAll(logs);
        return true;
    }

    private static List<BlockPos> collectLeaves(ServerLevel level, List<BlockPos> logs) {
        List<BlockPos> leaves = new ArrayList<>();
        Set<BlockPos> seen = new HashSet<>();
        ArrayDeque<BlockPos> frontier = new ArrayDeque<>(logs);
        for (int distance = 0; distance <= LEAF_REACH && !frontier.isEmpty(); distance++) {
            ArrayDeque<BlockPos> next = new ArrayDeque<>();
            for (BlockPos pos : frontier) {
                for (BlockPos near : BlockPos.betweenClosed(pos.offset(-1, -1, -1), pos.offset(1, 1, 1))) {
                    if (leaves.size() < MAX_LEAVES && isNaturalLeaf(level.getBlockState(near)) && seen.add(near.immutable())) {
                        leaves.add(near.immutable());
                        next.add(near.immutable());
                    }
                }
            }
            frontier = next;
        }
        return leaves;
    }

    private static boolean isNaturalLeaf(BlockState state) {
        if (state.getBlock() instanceof LeavesBlock) return !state.getValue(LeavesBlock.PERSISTENT);
        return state.is(BlockTags.WART_BLOCKS) || state.is(Blocks.SHROOMLIGHT);
    }

    /** A half-felled tree has lost the leaves that mark it as a tree, so its remaining blocks must be saved. */
    @Override
    public void save(ValueOutput output) {
        output.store("tree_targets", TARGETS_CODEC, List.copyOf(targets));
    }

    @Override
    public void load(ValueInput input) {
        targets.clear();
        input.read("tree_targets", TARGETS_CODEC).ifPresent(targets::addAll);
    }
}
