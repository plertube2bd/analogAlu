package com.example.redstonetransportation.test;

import com.example.redstonetransportation.integration.analogalu.ElectricPower;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseRailBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.RailShape;

/** Small building helpers shared by the game tests. */
final class TestWorld {
    static final String ARENA = "redstone_transportation_test:arena";
    static final int SIZE = 32;

    private TestWorld() {}

    static void floor(GameTestHelper helper, Block block) {
        for (int x = 0; x < SIZE; x++) {
            for (int z = 0; z < SIZE; z++) {
                helper.setBlock(x, 0, z, block);
            }
        }
    }

    /** Belts need power where ALU has electricity, and stop for a signal where it has not. */
    static Block beltFloor() {
        return ElectricPower.isAvailable() ? TestContent.SIGNAL : Blocks.STONE;
    }

    /**
     * A battery and a load in a closed three by three ring, in world coordinates. The east side is
     * live and {@code tap} is the middle of it, so whatever stands east of {@code tap} has power.
     */
    static void circuit(Level level, BlockPos tap) {
        BlockState cable = aluBlock("copper_cable").defaultBlockState();
        BlockPos middle = tap.west();
        for (Direction row : new Direction[] {Direction.NORTH, null, Direction.SOUTH}) {
            BlockPos live = row == null ? tap : tap.relative(row);
            level.setBlock(live, cable, Block.UPDATE_ALL);
            level.setBlock(live.west(2), cable, Block.UPDATE_ALL);
        }
        level.setBlock(middle.north(), device("resistor_load"), Block.UPDATE_ALL);
        level.setBlock(middle.south(), device("battery"), Block.UPDATE_ALL);
    }

    private static BlockState device(String name) {
        return aluBlock(name).defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.EAST);
    }

    static Block aluBlock(String name) {
        return BuiltInRegistries.BLOCK.getValue(Identifier.fromNamespaceAndPath("analog_alu", name));
    }

    /** Places a rail with an exact shape, whatever its neighbours would have chosen. */
    static void rail(GameTestHelper helper, BlockPos pos, Block block, RailShape shape) {
        helper.setBlock(pos, block);
        BlockState state = helper.getBlockState(pos);
        helper.getLevel().setBlock(helper.absolutePos(pos),
                state.setValue(((BaseRailBlock) block).getShapeProperty(), shape), Block.UPDATE_CLIENTS);
    }

    /** A closed rectangle of plain rail at height 1. */
    static void railLoop(GameTestHelper helper, int x0, int z0, int x1, int z1) {
        for (int x = x0 + 1; x < x1; x++) {
            rail(helper, new BlockPos(x, 1, z0), Blocks.RAIL, RailShape.EAST_WEST);
            rail(helper, new BlockPos(x, 1, z1), Blocks.RAIL, RailShape.EAST_WEST);
        }
        for (int z = z0 + 1; z < z1; z++) {
            rail(helper, new BlockPos(x0, 1, z), Blocks.RAIL, RailShape.NORTH_SOUTH);
            rail(helper, new BlockPos(x1, 1, z), Blocks.RAIL, RailShape.NORTH_SOUTH);
        }
        rail(helper, new BlockPos(x0, 1, z0), Blocks.RAIL, RailShape.SOUTH_EAST);
        rail(helper, new BlockPos(x1, 1, z0), Blocks.RAIL, RailShape.SOUTH_WEST);
        rail(helper, new BlockPos(x0, 1, z1), Blocks.RAIL, RailShape.NORTH_EAST);
        rail(helper, new BlockPos(x1, 1, z1), Blocks.RAIL, RailShape.NORTH_WEST);
    }

    /** The mock player's game type alone does not grant creative building. */
    static Player creativePlayer(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.CREATIVE);
        player.getAbilities().instabuild = true;
        return player;
    }
}
