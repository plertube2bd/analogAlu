package com.example.redstonetransportation.vehicle;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/** In-memory catalog for the reusable vehicle definitions. */
public final class VehicleCatalog {
    private final Map<String, VehicleDefinition> vehicles = new LinkedHashMap<>();
    private boolean frozen;

    public synchronized VehicleDefinition register(VehicleDefinition definition) {
        if (frozen) {
            throw new IllegalStateException("Vehicle catalog is frozen");
        }
        if (vehicles.putIfAbsent(definition.id(), definition) != null) {
            throw new IllegalStateException("Duplicate vehicle id: " + definition.id());
        }
        return definition;
    }

    public Optional<VehicleDefinition> get(String id) {
        return Optional.ofNullable(vehicles.get(id));
    }

    public Collection<VehicleDefinition> values() {
        return java.util.List.copyOf(vehicles.values());
    }

    public void freeze() {
        frozen = true;
    }
}
