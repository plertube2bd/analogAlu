package com.example.redstonetransportation.vehicle;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

public final class VehicleBehaviorRegistry {
    private final Map<String, VehicleBehavior> behaviors = new LinkedHashMap<>();
    private boolean frozen;

    public synchronized VehicleBehavior register(VehicleBehavior behavior) {
        if (frozen) {
            throw new IllegalStateException("Vehicle behavior registry is frozen");
        }
        if (behaviors.putIfAbsent(behavior.id(), behavior) != null) {
            throw new IllegalStateException("Duplicate vehicle behavior: " + behavior.id());
        }
        return behavior;
    }

    public Optional<VehicleBehavior> get(String id) {
        return Optional.ofNullable(behaviors.get(id));
    }

    public void freeze() {
        frozen = true;
    }
}
