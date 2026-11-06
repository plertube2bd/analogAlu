package com.example.redstonetransportation.machine;

import com.example.redstonetransportation.machine.work.CropHarvesterWork;
import com.example.redstonetransportation.machine.work.GrowthBoosterWork;
import com.example.redstonetransportation.machine.work.ItemCollectorWork;
import com.example.redstonetransportation.machine.work.PlanterWork;
import com.example.redstonetransportation.machine.work.QuarryWork;
import com.example.redstonetransportation.machine.work.TreeCutterWork;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.function.Predicate;
import java.util.function.Supplier;

/** Every factory machine. Each entry names its job, base speed and what its buffer does. */
public enum MachineType {
    TREE_CUTTER("tree_cutter", Role.PRODUCER, 6, stack -> true, TreeCutterWork::new),
    AUTO_PLANTER("auto_planter", Role.CONSUMER, 10, PlanterWork::isPlantable, PlanterWork::new),
    GROWTH_BOOSTER("growth_booster", Role.CONSUMER, 20, stack -> stack.is(Items.BONE_MEAL), GrowthBoosterWork::new),
    CROP_HARVESTER("crop_harvester", Role.PRODUCER, 8, stack -> true, CropHarvesterWork::new),
    QUARRY_DRILL("quarry_drill", Role.PRODUCER, 5, stack -> true, QuarryWork::new),
    ITEM_COLLECTOR("item_collector", Role.PRODUCER, 10, stack -> true, ItemCollectorWork::new);

    /** Which way items cross the machine's buffer. */
    public enum Role {
        /** Fills its buffer from the world and pushes it into neighbouring containers. */
        PRODUCER,
        /** Takes items from neighbours and uses up its buffer. */
        CONSUMER
    }

    private final String id;
    private final Role role;
    private final int baseInterval;
    private final Predicate<ItemStack> accepts;
    private final Supplier<MachineWork> work;

    MachineType(String id, Role role, int baseInterval, Predicate<ItemStack> accepts, Supplier<MachineWork> work) {
        this.id = id;
        this.role = role;
        this.baseInterval = baseInterval;
        this.accepts = accepts;
        this.work = work;
    }

    public String id() {
        return id;
    }

    public Role role() {
        return role;
    }

    public boolean accepts(ItemStack stack) {
        return accepts.test(stack);
    }

    public MachineWork createWork() {
        return work.get();
    }

    /** Ticks between work cycles: the base interval at signal 15, 15x slower at signal 1. */
    public int interval(int signal) {
        return baseInterval * 15 / Math.max(signal, 1);
    }
}
