package com.example.redstonetransportation.vehicle;

public interface VehicleModule {
    String moduleId();
    void register(VehicleCatalog catalog);
}
