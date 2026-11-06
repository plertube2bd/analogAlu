package com.example.redstonetransportation.item;

import com.example.redstonetransportation.rail.TrackPlanner;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseRailBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RailState;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.block.state.properties.RailShape;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Lays a whole line between two clicks, with slopes, bends, tunnels through terrain and supports
 * over gaps. Each further click extends the same line. The off-hand rail type is the one laid.
 * Clicking the air switches between a diagonal line and an L.
 */
public final class TrackLayerItem extends Item {
    private static final int MAX_CELLS = 256;
    private static final int CLEARANCE = 2;
    private static final int STYLE_COOLDOWN = 8;
    private static final Map<UUID, BlockPos> ANCHORS = new ConcurrentHashMap<>();
    private static final Map<UUID, TrackPlanner.Style> STYLES = new ConcurrentHashMap<>();

    private record Survey(int rails, List<BlockPos> supports, List<BlockPos> clearing, Set<BlockPos> kept, BlockPos obstruction) {}

    public TrackLayerItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        if (player == null) return InteractionResult.PASS;
        if (!(context.getLevel() instanceof ServerLevel level)) return InteractionResult.SUCCESS;

        UUID id = player.getUUID();
        if (player.isShiftKeyDown()) {
            clearAnchor(player);
            return InteractionResult.SUCCESS;
        }

        BlockPos clicked = context.getClickedPos();
        BlockPos target = BaseRailBlock.isRail(level, clicked) ? clicked : clicked.above();
        BlockPos anchor = ANCHORS.get(id);
        if (anchor == null) {
            ANCHORS.put(id, target);
            player.sendOverlayMessage(Component.translatable("message.redstone_transportation.track_start", styleName(player)));
            return InteractionResult.SUCCESS;
        }
        if (target.equals(anchor)) return InteractionResult.SUCCESS;

        // The track itself says what a line has to join, so this works on rails laid any time, by anyone.
        Direction startJoin = soleJoin(level, anchor);
        if (TrackPlanner.doublesBack(anchor, target, startJoin)) {
            player.sendOverlayMessage(Component.translatable("message.redstone_transportation.track_doubles_back"));
            return InteractionResult.FAIL;
        }
        List<TrackPlanner.Cell> cells = TrackPlanner.plan(anchor, target, startJoin, soleJoin(level, target), style(player));
        Component error = lay(level, player, cells);
        if (error != null) {
            player.sendOverlayMessage(error);
            return InteractionResult.FAIL;
        }
        ANCHORS.put(id, target);
        player.sendOverlayMessage(Component.translatable("message.redstone_transportation.track_laid", cells.size()));
        return InteractionResult.SUCCESS;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        if (player.isShiftKeyDown()) {
            clearAnchor(player);
            return InteractionResult.SUCCESS;
        }
        STYLES.put(player.getUUID(), style(player) == TrackPlanner.Style.DIAGONAL ? TrackPlanner.Style.CORNER : TrackPlanner.Style.DIAGONAL);
        // Holding the button repeats the click, which would flip the style straight back.
        player.getCooldowns().addCooldown(player.getItemInHand(hand), STYLE_COOLDOWN);
        player.sendOverlayMessage(Component.translatable("message.redstone_transportation.track_style", styleName(player)));
        return InteractionResult.SUCCESS;
    }

    private static void clearAnchor(Player player) {
        ANCHORS.remove(player.getUUID());
        player.sendOverlayMessage(Component.translatable("message.redstone_transportation.track_cleared"));
    }

    private static TrackPlanner.Style style(Player player) {
        return STYLES.getOrDefault(player.getUUID(), TrackPlanner.Style.DIAGONAL);
    }

    private static Component styleName(Player player) {
        return Component.translatable(style(player) == TrackPlanner.Style.DIAGONAL
                ? "message.redstone_transportation.track_style.diagonal"
                : "message.redstone_transportation.track_style.corner");
    }

    /** The rails this one really joins: it leads to them, and they lead back. */
    public static List<BlockPos> joinedRails(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (!BaseRailBlock.isRail(state)) return List.of();

        List<BlockPos> joined = new ArrayList<>();
        for (BlockPos end : new RailState(level, pos, state).getConnections()) {
            for (BlockPos candidate : List.of(end, end.above(), end.below())) {
                BlockState other = level.getBlockState(candidate);
                if (!BaseRailBlock.isRail(other)) continue;
                boolean leadsBack = new RailState(level, candidate, other).getConnections().stream()
                        .anyMatch(back -> back.getX() == pos.getX() && back.getZ() == pos.getZ());
                if (leadsBack) joined.add(candidate);
                break;
            }
        }
        return joined;
    }

    /** The side an end of existing track runs off to, for a line that should turn off it. */
    private static Direction soleJoin(ServerLevel level, BlockPos pos) {
        List<BlockPos> joined = joinedRails(level, pos);
        if (joined.size() != 1) return null;
        BlockPos other = joined.getFirst();
        return Direction.getApproximateNearest(other.getX() - pos.getX(), 0.0D, other.getZ() - pos.getZ());
    }

    /** Lays the planned line, or returns why it cannot be laid without changing anything. */
    public static Component lay(ServerLevel level, Player player, List<TrackPlanner.Cell> cells) {
        if (cells == null) return Component.translatable("message.redstone_transportation.track_too_steep");
        if (cells.size() > MAX_CELLS) return Component.translatable("message.redstone_transportation.track_too_long", MAX_CELLS);

        Survey survey = survey(level, player, cells);
        if (survey.obstruction() != null) {
            BlockPos pos = survey.obstruction();
            return Component.translatable("message.redstone_transportation.track_blocked", pos.getX(), pos.getY(), pos.getZ());
        }

        Block rail = railInOffhand(player);
        boolean free = player.hasInfiniteMaterials();
        if (!free) {
            int haveRails = count(player, rail);
            int haveSupports = count(player, Blocks.COBBLESTONE);
            if (haveRails < survey.rails()) {
                return Component.translatable("message.redstone_transportation.track_need_rails", survey.rails() - haveRails);
            }
            if (haveSupports < survey.supports().size()) {
                return Component.translatable("message.redstone_transportation.track_need_supports", survey.supports().size() - haveSupports);
            }
            take(player, rail, survey.rails());
            take(player, Blocks.COBBLESTONE, survey.supports().size());
        }

        for (BlockPos pos : survey.clearing()) level.destroyBlock(pos, !free, player);
        for (BlockPos pos : survey.supports()) level.setBlock(pos, Blocks.COBBLESTONE.defaultBlockState(), Block.UPDATE_ALL);
        place(level, cells, rail, survey.kept());
        return null;
    }

    private static void place(ServerLevel level, List<TrackPlanner.Cell> cells, Block rail, Set<BlockPos> kept) {
        // A rail left to find its own shape as it lands would also pull at any track beside the line.
        for (TrackPlanner.Cell cell : cells) {
            if (kept.contains(cell.pos())) continue;
            BlockState state = level.getBlockState(cell.pos());
            if (!BaseRailBlock.isRail(state)) state = railFor(rail, cell).defaultBlockState();
            Property<RailShape> shape = ((BaseRailBlock) state.getBlock()).getShapeProperty();
            if (shape.getPossibleValues().contains(cell.shape())) state = state.setValue(shape, cell.shape());
            level.setBlock(cell.pos(), state, Block.UPDATE_CLIENTS | Block.UPDATE_SKIP_ON_PLACE);
        }
        for (TrackPlanner.Cell cell : cells) {
            BlockState state = level.getBlockState(cell.pos());
            level.neighborChanged(state, cell.pos(), state.getBlock(), null, false);
            level.updateNeighborsAt(cell.pos(), state.getBlock());
        }
    }

    /** Powered, detector and activator rails cannot bend, so a bend always uses a plain rail. */
    private static Block railFor(Block rail, TrackPlanner.Cell cell) {
        boolean straightOnly = rail instanceof BaseRailBlock railBlock && railBlock.isStraight();
        return cell.bend() && straightOnly ? Blocks.RAIL : rail;
    }

    private static Survey survey(ServerLevel level, Player player, List<TrackPlanner.Cell> cells) {
        int rails = 0;
        List<BlockPos> supports = new ArrayList<>();
        List<BlockPos> clearing = new ArrayList<>();
        Set<BlockPos> kept = new HashSet<>();
        for (TrackPlanner.Cell cell : cells) {
            BlockPos pos = cell.pos();
            if (!level.isLoaded(pos) || !level.isInWorldBounds(pos) || !level.mayInteract(player, pos)) {
                return blocked(pos);
            }

            if (holdsItsShape(level, cell)) {
                // A branch may end against other track, but no rail can carry two lines across each other.
                if (cell != cells.getFirst() && cell != cells.getLast()) return blocked(pos);
                kept.add(pos);
            } else if (needsRail(level, cell)) {
                rails++;
                if (needsClearing(level, pos)) {
                    if (!canClear(level, pos)) return blocked(pos);
                    clearing.add(pos);
                }
            }
            for (int up = 1; up <= CLEARANCE; up++) {
                BlockPos above = pos.above(up);
                if (needsClearing(level, above)) {
                    if (!canClear(level, above)) return blocked(above);
                    clearing.add(above);
                }
            }

            BlockPos below = pos.below();
            if (!Block.canSupportRigidBlock(level, below)) {
                if (!level.getBlockState(below).canBeReplaced()) return blocked(below);
                supports.add(below);
            }
        }
        return new Survey(rails, supports, clearing, kept, null);
    }

    private static Survey blocked(BlockPos pos) {
        return new Survey(0, List.of(), List.of(), Set.of(), pos);
    }

    /** True for track the line must not reshape: a rail joined on both sides, or a slope, which cannot bend. */
    private static boolean holdsItsShape(ServerLevel level, TrackPlanner.Cell cell) {
        BlockState state = level.getBlockState(cell.pos());
        if (!BaseRailBlock.isRail(state)) return false;
        RailShape shape = state.getValue(((BaseRailBlock) state.getBlock()).getShapeProperty());
        return shape != cell.shape() && (shape.isSlope() || joinedRails(level, cell.pos()).size() == 2);
    }

    /** A rail that cannot bend has to make way for one that can, wherever the line turns on it. */
    private static boolean needsRail(ServerLevel level, TrackPlanner.Cell cell) {
        BlockState state = level.getBlockState(cell.pos());
        return !BaseRailBlock.isRail(state) || cell.bend() && ((BaseRailBlock) state.getBlock()).isStraight();
    }

    private static boolean needsClearing(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        return !state.isAir() && !state.canBeReplaced();
    }

    private static boolean canClear(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        return state.getDestroySpeed(level, pos) >= 0.0F && !state.hasBlockEntity();
    }

    private static Block railInOffhand(Player player) {
        ItemStack offhand = player.getOffhandItem();
        if (offhand.getItem() instanceof BlockItem item && item.getBlock() instanceof BaseRailBlock) return item.getBlock();
        return Blocks.RAIL;
    }

    private static int count(Player player, Block block) {
        return ContainerHelper.clearOrCountMatchingItems(player.getInventory(), stack -> stack.is(block.asItem()), 0, true);
    }

    private static void take(Player player, Block block, int amount) {
        if (amount > 0) {
            ContainerHelper.clearOrCountMatchingItems(player.getInventory(), stack -> stack.is(block.asItem()), amount, false);
        }
    }
}
