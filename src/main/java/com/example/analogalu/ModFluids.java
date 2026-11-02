package com.example.analogalu;

import com.example.analogalu.fluid.OilFluid;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.material.Fluid;

public final class ModFluids {
    public static final Fluid STILL = register("oil", new OilFluid(true));
    public static final Fluid FLOWING = register("flowing_oil", new OilFluid(false));

    private ModFluids() {}

    private static Fluid register(String name, Fluid fluid) {
        ResourceKey<Fluid> key = ResourceKey.create(Registries.FLUID,
                Identifier.fromNamespaceAndPath(AnalogAlu.MOD_ID, name));
        return Registry.register(BuiltInRegistries.FLUID, key, fluid);
    }
}