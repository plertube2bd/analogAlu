package com.example.redstonetransportation.structure;

import com.example.redstonetransportation.RedstoneTransportation;
import com.example.redstonetransportation.content.ModAdvancements;
import com.example.redstonetransportation.content.ModContent;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.CompoundContainer;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.inventory.DispenserMenu;
import net.minecraft.world.inventory.FurnaceMenu;
import net.minecraft.world.inventory.HopperMenu;
import net.minecraft.world.inventory.MenuConstructor;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.DoubleHighBlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.LeverBlock;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;

/**
 * What a player can do to the blocks of a structure while it moves. The player's client says which
 * block it is aiming at, since the server sees the structure a little ahead of what is on screen.
 */
public final class StructureActions {
    /** How much further than normal reach a click may land, for the time it took to get here. */
    private static final double REACH_ALLOWANCE = 3.0D;

    public record Payload(int structure, boolean attack, BlockPos cell, Direction face) implements CustomPacketPayload {
        public static final Type<Payload> TYPE = new Type<>(Identifier.fromNamespaceAndPath(RedstoneTransportation.MOD_ID, "structure_action"));
        static final StreamCodec<RegistryFriendlyByteBuf, Payload> CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, Payload::structure,
                ByteBufCodecs.BOOL, Payload::attack,
                BlockPos.STREAM_CODEC, Payload::cell,
                Direction.STREAM_CODEC, Payload::face,
                Payload::new);

        @Override
        public Type<Payload> type() {
            return TYPE;
        }
    }

    /** Hands a structure's contents to a menu, without any of the block entity's ties to the world. */
    private record Aboard(Container items, MovingStructureEntity structure) implements Container {
        @Override
        public int getContainerSize() {
            return items.getContainerSize();
        }

        @Override
        public boolean isEmpty() {
            return items.isEmpty();
        }

        @Override
        public ItemStack getItem(int slot) {
            return items.getItem(slot);
        }

        @Override
        public ItemStack removeItem(int slot, int count) {
            return items.removeItem(slot, count);
        }

        @Override
        public ItemStack removeItemNoUpdate(int slot) {
            return items.removeItemNoUpdate(slot);
        }

        @Override
        public void setItem(int slot, ItemStack stack) {
            items.setItem(slot, stack);
        }

        @Override
        public void setChanged() {
        }

        @Override
        public boolean stillValid(Player player) {
            double reach = (structure.blocks().reach() + 1.0D) * Mth.SQRT_OF_TWO + player.blockInteractionRange() + REACH_ALLOWANCE;
            return !structure.isRemoved() && player.distanceToSqr(structure) < reach * reach;
        }

        @Override
        public void stopOpen(net.minecraft.world.entity.ContainerUser user) {
            structure.storeBlockEntities();
        }

        @Override
        public void clearContent() {
            items.clearContent();
        }
    }

    private StructureActions() {}

    static void initialize() {
        PayloadTypeRegistry.serverboundPlay().register(Payload.TYPE, Payload.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(Payload.TYPE, (payload, context) -> receive(context.player(), payload));
    }

    private static void receive(ServerPlayer player, Payload payload) {
        if (!(player.level().getEntity(payload.structure()) instanceof MovingStructureEntity structure)) return;
        if (structure.blocks().cell(payload.cell()) == null) return;

        StructurePose pose = structure.pose();
        BlockPos cell = payload.cell();
        Vec3 center = new Vec3(pose.worldX(cell.getX(), cell.getZ()), pose.position().y + cell.getY() + 0.5D, pose.worldZ(cell.getX(), cell.getZ()));
        double reach = player.blockInteractionRange() + REACH_ALLOWANCE;
        if (player.getEyePosition().distanceToSqr(center) > reach * reach) return;

        if (payload.attack()) {
            breakCell(player, structure, cell);
        } else {
            use(player, structure, cell, payload.face(), InteractionHand.MAIN_HAND);
        }
    }

    /** Right-click on a block aboard: the same order as in the world, so Shift places instead of opening. */
    public static void use(ServerPlayer player, MovingStructureEntity structure, BlockPos cell, Direction face, InteractionHand hand) {
        if (cell.equals(BlockPos.ZERO)) {
            structure.interact(player, hand, Vec3.ZERO);
            return;
        }
        StructureBlocks.Cell target = structure.blocks().cell(cell);
        if (target == null) return;
        if (!player.isSecondaryUseActive() && (toggle(player, structure, target) || open(player, structure, target))) return;
        if (player.getItemInHand(hand).getItem() instanceof BlockItem item) placeCell(player, structure, cell.relative(face), face, hand, item);
    }

    private static boolean toggle(ServerPlayer player, MovingStructureEntity structure, StructureBlocks.Cell target) {
        BlockState state = target.state();
        boolean opens = state.hasProperty(BlockStateProperties.OPEN) && !state.is(Blocks.IRON_DOOR) && !state.is(Blocks.IRON_TRAPDOOR);
        boolean lever = state.getBlock() instanceof LeverBlock;
        if (!opens && !lever) return false;

        var property = opens ? BlockStateProperties.OPEN : BlockStateProperties.POWERED;
        structure.setCell(target.pos(), target.withState(state.cycle(property)));
        if (state.getBlock() instanceof DoorBlock) {
            BlockPos other = target.pos().relative(state.getValue(BlockStateProperties.DOUBLE_BLOCK_HALF) == DoubleBlockHalf.LOWER ? Direction.UP : Direction.DOWN);
            StructureBlocks.Cell half = structure.blocks().cell(other);
            if (half != null && half.state().is(state.getBlock())) structure.setCell(other, half.withState(half.state().cycle(property)));
        }
        structure.level().playSound(null, player.blockPosition(), SoundEvents.WOODEN_TRAPDOOR_OPEN, SoundSource.BLOCKS, 1.0F, 1.0F);
        return true;
    }

    private static boolean open(ServerPlayer player, MovingStructureEntity structure, StructureBlocks.Cell target) {
        BlockState state = target.state();
        if (state.is(Blocks.CRAFTING_TABLE)) {
            return show(player, (id, inventory, user) -> new CraftingMenu(id, inventory, ContainerLevelAccess.NULL), state.getBlock().getName());
        }
        if (!(structure.blockEntityAt(target.pos()) instanceof Container container)) return false;

        Component title = state.getBlock().getName();
        Container items = doubleChest(structure, target, container);
        Aboard aboard = new Aboard(items, structure);
        return switch (items.getContainerSize()) {
            case 54 -> show(player, (id, inventory, user) -> ChestMenu.sixRows(id, inventory, aboard), title);
            case 27 -> show(player, (id, inventory, user) -> ChestMenu.threeRows(id, inventory, aboard), title);
            case 9 -> show(player, (id, inventory, user) -> new DispenserMenu(id, inventory, aboard), title);
            case 5 -> show(player, (id, inventory, user) -> new HopperMenu(id, inventory, aboard), title);
            case 3 -> container instanceof AbstractFurnaceBlockEntity
                    && show(player, (id, inventory, user) -> new FurnaceMenu(id, inventory, aboard, new SimpleContainerData(4)), title);
            default -> false;
        };
    }

    private static Container doubleChest(MovingStructureEntity structure, StructureBlocks.Cell target, Container container) {
        BlockState state = target.state();
        if (!(state.getBlock() instanceof ChestBlock) || state.getValue(ChestBlock.TYPE) == ChestType.SINGLE) return container;
        BlockPos other = target.pos().relative(ChestBlock.getConnectedDirection(state));
        StructureBlocks.Cell half = structure.blocks().cell(other);
        if (half == null || !half.state().is(state.getBlock())) return container;
        if (!(structure.blockEntityAt(other) instanceof Container second)) return container;
        return state.getValue(ChestBlock.TYPE) == ChestType.RIGHT ? new CompoundContainer(container, second) : new CompoundContainer(second, container);
    }

    private static boolean show(ServerPlayer player, MenuConstructor menu, Component title) {
        player.openMenu(new SimpleMenuProvider(menu, title));
        return true;
    }

    private static void placeCell(ServerPlayer player, MovingStructureEntity structure, BlockPos cell, Direction face, InteractionHand hand, BlockItem item) {
        StructureBlocks blocks = structure.blocks();
        if (blocks.cell(cell) != null) return;
        if (blocks.size() >= StructureAssembler.MAX_BLOCKS) {
            player.sendOverlayMessage(Component.translatable("message.redstone_transportation.structure_too_large", StructureAssembler.MAX_BLOCKS));
            return;
        }
        // Blocks that fill two spaces, or that place a second block of their own, need a real world.
        Block block = item.getBlock();
        if (item instanceof DoubleHighBlockItem || block instanceof BedBlock) {
            player.sendOverlayMessage(Component.translatable("message.redstone_transportation.structure_cannot_place"));
            return;
        }

        StructurePose pose = structure.pose();
        ServerLevel level = (ServerLevel) structure.level();
        BlockPos world = worldPos(pose, cell);
        if (!level.getBlockState(world).getCollisionShape(level, world).isEmpty()) return;
        if (!hasLift(structure, blocks.size() + 1, balloons(blocks) + (block == ModContent.LIFT_BALLOON ? 1 : 0))) {
            player.sendOverlayMessage(Component.translatable("message.redstone_transportation.structure_no_lift", 1, StructureAssembler.BLOCKS_PER_BALLOON));
            return;
        }

        ItemStack stack = player.getItemInHand(hand);
        Direction worldFace = pose.toWorld(face);
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(world), worldFace, world.relative(worldFace.getOpposite()), false);
        BlockState placed = block.getStateForPlacement(new BlockPlaceContext(player, hand, stack, hit));
        if (placed == null) return;
        if (placed.hasProperty(BlockStateProperties.WATERLOGGED)) placed = placed.setValue(BlockStateProperties.WATERLOGGED, false);

        // Placed the way it looks in the world, then turned back into the structure's own coordinates.
        BlockState local = placed.rotate(pose.inverseRotation());
        Optional<CompoundTag> clientData = block instanceof EntityBlock ? Optional.of(new CompoundTag()) : Optional.empty();
        structure.setCell(cell, new StructureBlocks.Cell(cell, local, Optional.empty(), clientData));
        ModAdvancements.award(player, ModAdvancements.REPAIRS);
        if (!player.hasInfiniteMaterials()) stack.shrink(1);
        level.playSound(null, world, local.getSoundType().getPlaceSound(), SoundSource.BLOCKS, 1.0F, 0.8F);
    }

    /** Left-click on a block aboard. What it held and what it drops go straight to the player, since it may be over the sea. */
    public static void breakCell(ServerPlayer player, MovingStructureEntity structure, BlockPos cell) {
        if (cell.equals(BlockPos.ZERO)) {
            player.sendOverlayMessage(Component.translatable("message.redstone_transportation.structure_core_protected"));
            return;
        }
        StructureBlocks blocks = structure.blocks();
        StructureBlocks.Cell target = blocks.cell(cell);
        if (target == null) return;
        BlockState state = target.state();
        if (!hasLift(structure, blocks.size() - 1, balloons(blocks) - (state.is(ModContent.LIFT_BALLOON) ? 1 : 0))) {
            player.sendOverlayMessage(Component.translatable("message.redstone_transportation.structure_no_lift", 1, StructureAssembler.BLOCKS_PER_BALLOON));
            return;
        }

        ServerLevel level = (ServerLevel) structure.level();
        BlockPos world = worldPos(structure.pose(), cell);
        BlockEntity blockEntity = structure.blockEntityAt(cell);
        if (!player.hasInfiniteMaterials()) {
            if (blockEntity instanceof Container container) {
                for (int slot = 0; slot < container.getContainerSize(); slot++) give(player, container.removeItemNoUpdate(slot));
            }
            if (player.hasCorrectToolForDrops(state)) {
                List<ItemStack> drops = Block.getDrops(state, level, world, blockEntity, player, player.getMainHandItem());
                drops.forEach(stack -> give(player, stack));
            }
        }
        structure.setCell(cell, null);
        level.playSound(null, world, state.getSoundType().getBreakSound(), SoundSource.BLOCKS, 1.0F, 0.8F);
    }

    private static void give(ServerPlayer player, ItemStack stack) {
        if (!stack.isEmpty() && !player.getInventory().add(stack)) Block.popResource(player.level(), player.blockPosition(), stack);
    }

    private static BlockPos worldPos(StructurePose pose, BlockPos cell) {
        return BlockPos.containing(pose.worldX(cell.getX(), cell.getZ()), pose.position().y + cell.getY() + 0.5D, pose.worldZ(cell.getX(), cell.getZ()));
    }

    private static boolean hasLift(MovingStructureEntity structure, int blocks, int balloons) {
        return structure.kind() != StructureKind.AIRSHIP || blocks <= balloons * StructureAssembler.BLOCKS_PER_BALLOON;
    }

    private static int balloons(StructureBlocks blocks) {
        return (int) blocks.cells().stream().filter(cell -> cell.state().is(ModContent.LIFT_BALLOON)).count();
    }
}
