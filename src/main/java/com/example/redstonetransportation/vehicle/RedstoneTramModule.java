package com.example.redstonetransportation.vehicle;

import com.example.redstonetransportation.drive.DriveMode;

public final class RedstoneTramModule implements VehicleModule {
    @Override
    public String moduleId() {
        return "redstone_transportation:redstone_tram_module";
    }

    @Override
    public void register(VehicleCatalog catalog) {
        catalog.register(new VehicleDefinition(
                "redstone_transportation:redstone_tram",
                "rail_vehicle",
                "entity.redstone_transportation.redstone_tram",
                0.35,
                0.04,
                1,
                DriveMode.SIGNAL_AS_THROTTLE,
                new ModelDefinition(
                        "tram_entity_model",
                        "redstone_transportation:redstone_tram",
                        "redstone_transportation:textures/entity/redstone_tram.png",
                        ""
                )
        ));
    }
}
