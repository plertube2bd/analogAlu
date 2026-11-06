package com.example.redstonetransportation.machine.work;

import com.example.redstonetransportation.machine.MachineBlockEntity;
import com.example.redstonetransportation.machine.MachineWork;
import com.example.redstonetransportation.machine.WorkArea;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.block.entity.HopperBlockEntity;

import java.util.List;

/** Vacuums loose item drops out of the work area into its buffer. */
public final class ItemCollectorWork implements MachineWork {
    private static final int REACH_HEIGHT = 8;

    @Override
    public Result run(MachineBlockEntity machine, ServerLevel level) {
        if (!machine.hasRoom()) return Result.BLOCKED;

        List<ItemEntity> drops = level.getEntitiesOfClass(ItemEntity.class, WorkArea.box(machine, REACH_HEIGHT));
        boolean collected = false;
        for (ItemEntity drop : drops) {
            collected |= HopperBlockEntity.addItem(machine, drop);
        }
        return collected ? Result.WORKED : Result.IDLE;
    }
}
