package com.example.redstonetransportation.client;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** The solid body of a tram: spinning wheels, plus a pantograph that lifts while the car moves. */
final class TramModel extends EntityModel<TramRenderState> {
    private static final int WHEELS = 8;
    private static final float PANTOGRAPH_TRAVEL = 0.2F;
    private static final float FULL_LIFT_SPEED = 0.2F;

    private final List<ModelPart> wheels = new ArrayList<>(WHEELS);
    private final Optional<ModelPart> pantograph;
    private final Optional<ModelPart> pantographUpper;

    TramModel(ModelPart root) {
        super(root);
        for (int wheel = 0; wheel < WHEELS; wheel++) {
            wheels.add(root.getChild("wheel_" + wheel));
        }
        pantograph = root.hasChild("pantograph") ? Optional.of(root.getChild("pantograph")) : Optional.empty();
        pantographUpper = pantograph.map(part -> part.getChild("pantograph_upper"));
    }

    @Override
    public void setupAnim(TramRenderState state) {
        super.setupAnim(state);
        for (ModelPart wheel : wheels) {
            wheel.zRot = state.wheelAngle;
        }
        float lift = Math.min(state.speed / FULL_LIFT_SPEED, 1.0F) * PANTOGRAPH_TRAVEL;
        pantograph.ifPresent(part -> part.zRot -= lift);
        pantographUpper.ifPresent(part -> part.zRot += lift * 2.0F);
    }
}
