package com.example.redstonetransportation.test;

import net.fabricmc.api.ModInitializer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

/** What only the tests need in the registries, which close before the first test runs. */
public final class TestContent implements ModInitializer {
    /** A constant signal that is not redstone, so a test means the same with or without ALU's circuits. */
    static final Block SIGNAL = signal("signal", 15);
    /** The same at a strength of 6. */
    static final Block WEAK_SIGNAL = signal("weak_signal", 6);

    private static Block signal(String name, int strength) {
        ResourceKey<Block> key = ResourceKey.create(Registries.BLOCK,
                Identifier.fromNamespaceAndPath("redstone_transportation_test", name));
        return Registry.register(BuiltInRegistries.BLOCK, key, new SignalBlock(BlockBehaviour.Properties.of().setId(key), strength));
    }

    @Override
    public void onInitialize() {
        // Loading this class has already registered everything above.
    }

    private static final class SignalBlock extends Block {
        private final int strength;

        private SignalBlock(Properties properties, int strength) {
            super(properties);
            this.strength = strength;
        }

        @Override
        protected boolean isSignalSource(BlockState state) {
            return true;
        }

        @Override
        protected int getSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
            return strength;
        }
    }
}
