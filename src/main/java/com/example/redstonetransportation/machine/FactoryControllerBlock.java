package com.example.redstonetransportation.machine;

import com.example.redstonetransportation.content.ModAdvancements;
import com.example.redstonetransportation.integration.analogalu.AnalogAluSignalAdapter;
import com.example.redstonetransportation.integration.analogalu.ElectricPower;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * One switch for a whole factory: every machine touching this block, directly or through other
 * machines, runs from its mode. Right-click cycles the mode.
 */
public final class FactoryControllerBlock extends Block {
    public static final EnumProperty<Mode> MODE = EnumProperty.create("mode", Mode.class);
    private static final int REFRESH_TICKS = 20;
    private static final int FULL_SPEED = 15;
    private static final int MAX_MACHINES = 128;
    private static final int FACTORY_MACHINES = 3;

    public enum Mode implements StringRepresentable {
        /** Every linked machine stops, whatever signal it receives. */
        STOPPED("stopped"),
        /** Every linked machine runs at full speed. */
        RUNNING("running"),
        /** This block's 0..15 input sets the speed of every linked machine. */
        SIGNAL("signal");

        private final String name;

        Mode(String name) {
            this.name = name;
        }

        @Override
        public String getSerializedName() {
            return name;
        }

        private Mode next() {
            return values()[(ordinal() + 1) % values().length];
        }
    }

    public FactoryControllerBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(MODE, Mode.STOPPED));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(MODE);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                               Player player, BlockHitResult hit) {
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        Mode next = state.getValue(MODE).next();
        level.setBlock(pos, state.setValue(MODE, next), Block.UPDATE_ALL);
        int machines = linkMachines((ServerLevel) level, pos, signalFor(next, level, pos));
        if (machines >= FACTORY_MACHINES) ModAdvancements.award(player, ModAdvancements.FACTORY);
        player.sendOverlayMessage(Component.translatable(
                "message.redstone_transportation.factory_mode." + next.getSerializedName(), machines));
        return InteractionResult.SUCCESS;
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (!level.isClientSide() && oldState.getBlock() != this) level.scheduleTick(pos, this, 1);
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        linkMachines(level, pos, signalFor(state.getValue(MODE), level, pos));
        level.scheduleTick(pos, this, REFRESH_TICKS);
    }

    private static int signalFor(Mode mode, Level level, BlockPos pos) {
        return switch (mode) {
            case STOPPED -> 0;
            // Left running, it still has to be getting electricity if there is any to be had.
            case RUNNING -> ElectricPower.isAvailable() && ElectricPower.input(level, pos) == 0 ? 0 : FULL_SPEED;
            case SIGNAL -> AnalogAluSignalAdapter.readBestNeighborSignal(level, pos);
        };
    }

    /** Links the machines connected to this controller and returns how many were reached. */
    private static int linkMachines(ServerLevel level, BlockPos controller, int signal) {
        List<MachineBlockEntity> machines = findMachines(level, controller);
        Map<Direction, BlockPos> areas = new EnumMap<>(Direction.class);
        for (MachineBlockEntity machine : machines) {
            BlockPos area = areas.computeIfAbsent(machine.facing(), facing -> WorkArea.sharedCenter(machines, facing));
            machine.link(signal, area);
        }
        return machines.size();
    }

    private static List<MachineBlockEntity> findMachines(ServerLevel level, BlockPos controller) {
        List<MachineBlockEntity> machines = new ArrayList<>();
        Set<BlockPos> seen = new HashSet<>();
        ArrayDeque<BlockPos> frontier = new ArrayDeque<>();
        seen.add(controller);
        frontier.add(controller);
        while (!frontier.isEmpty() && machines.size() < MAX_MACHINES) {
            BlockPos current = frontier.removeFirst();
            for (Direction direction : Direction.values()) {
                BlockPos next = current.relative(direction);
                if (seen.add(next) && level.getBlockEntity(next) instanceof MachineBlockEntity machine) {
                    machines.add(machine);
                    frontier.addLast(next);
                }
            }
        }
        return machines;
    }
}
