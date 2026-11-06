package com.example.redstonetransportation.machine;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * The one job a machine performs. A new factory machine is this interface plus one
 * {@link MachineType} entry; buffering, signals, GUI and saving all come from the shared frame.
 */
public interface MachineWork {
    Result run(MachineBlockEntity machine, ServerLevel level);

    default void save(ValueOutput output) {}

    default void load(ValueInput input) {}

    enum Result {
        /** Did one unit of work. */
        WORKED,
        /** Nothing in range right now; checks again after a longer pause. */
        IDLE,
        /** Cannot continue: the buffer is full or an input is missing. */
        BLOCKED
    }
}
