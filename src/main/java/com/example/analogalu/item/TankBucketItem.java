package com.example.analogalu.item;

import com.example.analogalu.ModBlocks;
import com.example.analogalu.ModFluids;
import com.example.analogalu.WaferProgramData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

public final class TankBucketItem extends Item {
    private static final String FLUID_KEY = "TankFluid";
    private static final String AMOUNT_KEY = "TankAmount";
    private final int capacity;

    public TankBucketItem(Properties properties, int capacity) {
        super(properties.stacksTo(1));
        this.capacity = capacity;
    }

    @Override
    public InteractionResult use(Level level, Player player, net.minecraft.world.InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        BlockHitResult hit = getPlayerPOVHitResult(level, player, ClipContext.Fluid.SOURCE_ONLY);
        if (hit.getType() != HitResult.Type.BLOCK) return InteractionResult.PASS;
        BlockPos pos = hit.getBlockPos();
        FluidState fluid = level.getFluidState(pos);
        if (!fluid.isSource()) return InteractionResult.PASS;

        String fluidId = fluid.is(FluidTags.LAVA) ? "lava"
                : fluid.is(ModFluids.STILL) ? "oil" : "";
        if (fluidId.isEmpty()) return InteractionResult.PASS;
        CompoundTag data = data(stack);
        String stored = data.getStringOr(FLUID_KEY, "");
        int amount = data.getIntOr(AMOUNT_KEY, 0);
        if ((!stored.isEmpty() && !stored.equals(fluidId)) || amount >= capacity) return InteractionResult.PASS;

        if (!level.isClientSide()) {
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
            data.putString(FLUID_KEY, fluidId);
            data.putInt(AMOUNT_KEY, amount + 1);
            stack.set(DataComponents.CUSTOM_DATA, CustomData.of(data));
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        ItemStack stack = context.getItemInHand();
        CompoundTag data = data(stack);
        String fluid = data.getStringOr(FLUID_KEY, "");
        int amount = data.getIntOr(AMOUNT_KEY, 0);
        if (fluid.isEmpty() || amount <= 0) return InteractionResult.PASS;

        Level level = context.getLevel();
        BlockPos target = context.getClickedPos().relative(context.getClickedFace());
        if (!level.getBlockState(target).canBeReplaced()) return InteractionResult.PASS;
        if (!level.isClientSide()) {
            level.setBlock(target, fluid.equals("oil") ? ModBlocks.OIL.defaultBlockState()
                    : Blocks.LAVA.defaultBlockState(), 3);
            amount--;
            if (amount == 0) {
                data.remove(FLUID_KEY);
                data.remove(AMOUNT_KEY);
            } else {
                data.putInt(AMOUNT_KEY, amount);
            }
            stack.set(DataComponents.CUSTOM_DATA, CustomData.of(data));
        }
        return InteractionResult.SUCCESS;
    }

    public int capacity() {
        return capacity;
    }

    public static String storedFluid(ItemStack stack) {
        return data(stack).getStringOr(FLUID_KEY, "");
    }

    public static int storedAmount(ItemStack stack) {
        return Math.clamp(data(stack).getIntOr(AMOUNT_KEY, 0), 0,
                stack.getItem() instanceof TankBucketItem tank ? tank.capacity : 0);
    }

    public static ItemStack emptyContainer(ItemStack filled) {
        ItemStack empty = filled.copyWithCount(1);
        CompoundTag tag = data(empty);
        tag.remove(FLUID_KEY);
        tag.remove(AMOUNT_KEY);
        if (tag.isEmpty()) empty.remove(DataComponents.CUSTOM_DATA);
        else empty.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        return empty;
    }

    private static CompoundTag data(ItemStack stack) {
        return WaferProgramData.get(stack);
    }
}