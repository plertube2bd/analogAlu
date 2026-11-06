package com.example.redstonetransportation.vehicle;

import com.example.redstonetransportation.drive.DriveCommand;
import com.example.redstonetransportation.integration.analogalu.AnalogAluSignalAdapter;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

/** Bridges world redstone input to the registered vehicle behavior. */
public final class VehicleControlService {
    private final VehicleBehaviorRegistry behaviors;

    public VehicleControlService(VehicleBehaviorRegistry behaviors) {
        this.behaviors = behaviors;
    }

    public DriveCommand evaluate(
            Level level,
            BlockPos receiverPos,
            VehicleDefinition definition,
            float manualThrottle,
            boolean brake
    ) {
        int signal = AnalogAluSignalAdapter.readBestNeighborSignal(level, receiverPos);
        VehicleControlState state = new VehicleControlState(signal, manualThrottle, brake);

        return behaviors.get(definition.behaviorId())
                .map(behavior -> behavior.calculate(definition, state))
                .orElseGet(() -> DriveCommand.stop("MISSING_BEHAVIOR:" + definition.behaviorId()));
    }
}
