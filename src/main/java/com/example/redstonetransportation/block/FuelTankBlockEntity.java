package com.example.redstonetransportation.block;

import com.example.redstonetransportation.content.ModContent;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/** The diesel in a car's tank, counted in ticks of driving at full throttle. */
public final class FuelTankBlockEntity extends BlockEntity {
    /** One piece of diesel drives a car for two minutes. */
    public static final int TICKS_PER_DIESEL = 2400;
    public static final int CAPACITY_DIESEL = 16;
    public static final int CAPACITY = TICKS_PER_DIESEL * CAPACITY_DIESEL;

    private int fuel;

    public FuelTankBlockEntity(BlockPos pos, BlockState state) {
        super(ModContent.FUEL_TANK_ENTITY, pos, state);
    }

    public int fuel() {
        return fuel;
    }

    /** Pours in one piece of diesel; false when there is no room for it. */
    public boolean addDiesel() {
        if (fuel + TICKS_PER_DIESEL > CAPACITY) return false;
        fuel += TICKS_PER_DIESEL;
        setChanged();
        return true;
    }

    /** Takes up to this much; returns how much there was. */
    public int drain(int amount) {
        int taken = Math.min(amount, fuel);
        fuel -= taken;
        if (taken > 0) setChanged();
        return taken;
    }

    /** Whole pieces of diesel left, rounded up, for showing to the player. */
    public int dieselLeft() {
        return (fuel + TICKS_PER_DIESEL - 1) / TICKS_PER_DIESEL;
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        fuel = Math.clamp(input.getIntOr("fuel", 0), 0, CAPACITY);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("fuel", fuel);
    }
}
