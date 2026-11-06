package com.example.redstonetransportation.block;

import com.example.redstonetransportation.content.ModContent;
import com.example.redstonetransportation.item.StructureMarkerItem;
import com.example.redstonetransportation.structure.StructureAssembler;
import com.example.redstonetransportation.structure.StructureKind;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

/**
 * The block that turns a structure into a vehicle. Right-click lifts everything joined to it out of
 * the world; holding the structure marker limits that to the marked area. The top arrow is the
 * structure's front.
 */
public final class StructureCoreBlock extends Block {
    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;

    private final StructureKind kind;

    public StructureCoreBlock(Properties properties, StructureKind kind) {
        super(properties);
        this.kind = kind;
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    public StructureKind kind() {
        return kind;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection());
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        return assemble(level, pos, player, null);
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                          Player player, InteractionHand hand, BlockHitResult hit) {
        if (!stack.is(ModContent.STRUCTURE_MARKER_ITEM)) return super.useItemOn(stack, state, level, pos, player, hand, hit);
        // Until both corners are marked this is just another block to mark.
        BoundingBox marked = StructureMarkerItem.selection(player);
        return marked == null || player.isShiftKeyDown() ? InteractionResult.PASS : assemble(level, pos, player, marked);
    }

    private static InteractionResult assemble(Level level, BlockPos pos, Player player, @Nullable BoundingBox limit) {
        if (level instanceof ServerLevel serverLevel) {
            StructureAssembler.Result result = StructureAssembler.assemble(serverLevel, pos, limit);
            player.sendOverlayMessage(result.message());
            if (result.structure() != null) StructureAssembler.awardAssembly(player, result.structure());
        }
        return InteractionResult.SUCCESS;
    }
}
