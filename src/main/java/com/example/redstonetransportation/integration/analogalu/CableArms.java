package com.example.redstonetransportation.integration.analogalu;

import com.example.redstonetransportation.RedstoneTransportation;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.Property;

/**
 * Lets Analog ALU's copper cable reach into the blocks that run on it. ALU only draws an arm towards
 * its own cables and devices, so a motor or a machine taking power from a cable looked unconnected.
 * The arms are only the cable's look and outline; ALU works out its circuits separately, so this
 * changes nothing about where electricity goes. A lit arm into a block means that block is powered.
 */
public final class CableArms {
    /** Everything that reads a cable's voltage as its power. */
    public static final TagKey<Block> CONNECTS_TO_CABLE = TagKey.create(Registries.BLOCK,
            Identifier.fromNamespaceAndPath(RedstoneTransportation.MOD_ID, "connects_to_cable"));

    private CableArms() {}

    /**
     * The cable's shape once it also reaches every powered block it touches. A locked cable still
     * reaches them: its lock only picks which cables and devices it joins, and these blocks read the
     * cable whatever the lock says.
     */
    public static BlockState reach(BlockGetter level, BlockPos pos, BlockState cable) {
        for (Direction side : Direction.values()) {
            Property<?> property = cable.getBlock().getStateDefinition().getProperty(side.getSerializedName());
            if (!(property instanceof BooleanProperty arm) || cable.getValue(arm)) continue;
            if (level.getBlockState(pos.relative(side)).is(CONNECTS_TO_CABLE)) cable = cable.setValue(arm, true);
        }
        return cable;
    }
}
