package com.example.analogalu;

import com.mojang.serialization.Codec;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.Identifier;

/** 아이템에 붙는 데이터 컴포넌트. */
public final class ModComponents {
    /** Shots left in a stun gun. */
    public static final DataComponentType<Integer> STUN_CHARGES = Registry.register(
            BuiltInRegistries.DATA_COMPONENT_TYPE,
            Identifier.fromNamespaceAndPath("analog_alu", "stun_charges"),
            DataComponentType.<Integer>builder().persistent(com.mojang.serialization.Codec.INT).networkSynchronized(net.minecraft.network.codec.ByteBufCodecs.VAR_INT).build());

    /** 건전지 블록을 캤을 때 남아 있던 에너지(J). 다시 설치하면 그대로 복원된다. */
    public static final DataComponentType<Double> STORED_ENERGY = Registry.register(
            BuiltInRegistries.DATA_COMPONENT_TYPE, Identifier.fromNamespaceAndPath(AnalogAlu.MOD_ID, "stored_energy"),
            DataComponentType.<Double>builder().persistent(Codec.DOUBLE).networkSynchronized(ByteBufCodecs.DOUBLE)
                    .build());

    private ModComponents() {}

    public static void init() {}
}
