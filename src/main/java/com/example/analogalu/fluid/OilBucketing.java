package com.example.analogalu.fluid;

import com.example.analogalu.ModItems;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * 빈 양동이로 석유 원천을 우클릭하면 원천이 사라지고 석유 아이템 1개를 얻는다. 양동이는 소모되지 않는다.
 * (석유 양동이 아이템은 없다. OilFluid.getBucket()이 빈 아이템을 돌려주므로 바닐라 양동이 동작은 석유를 무시한다.)
 */
public final class OilBucketing {
    private OilBucketing() {}

    public static void init() {
        UseItemCallback.EVENT.register((player, level, hand) -> {
            ItemStack stack = player.getItemInHand(hand);
            if (!stack.is(Items.BUCKET) || player.isSpectator()) return InteractionResult.PASS;
            BlockHitResult hit = pickSource(level, player);
            if (hit == null) return InteractionResult.PASS;
            BlockPos pos = hit.getBlockPos();
            if (!level.mayInteract(player, pos) || !player.mayUseItemAt(pos, hit.getDirection(), stack)) {
                return InteractionResult.FAIL;
            }
            if (level.isClientSide()) return InteractionResult.SUCCESS;

            level.setBlock(pos, Blocks.AIR.defaultBlockState(), 11);
            ItemStack oil = new ItemStack(ModItems.OIL);
            if (!player.getInventory().add(oil)) player.drop(oil, false, net.minecraft.util.Prediction.SERVER_ONLY);
            level.playSound(null, pos, SoundEvents.BUCKET_FILL_LAVA, SoundSource.BLOCKS, 1.0F, 0.8F);
            return InteractionResult.SUCCESS;
        });
    }

    /** 시선 방향으로 가장 가까운 유체 원천을 찾는다. 석유 원천이 아니면 null. */
    private static BlockHitResult pickSource(Level level, Player player) {
        Vec3 eye = player.getEyePosition();
        Vec3 end = eye.add(player.getViewVector(1.0F).scale(player.blockInteractionRange()));
        BlockHitResult hit = level.clip(new ClipContext(eye, end, ClipContext.Block.OUTLINE,
                ClipContext.Fluid.SOURCE_ONLY, player));
        if (hit.getType() != HitResult.Type.BLOCK) return null;
        return level.getFluidState(hit.getBlockPos()).getType() instanceof OilFluid
                && level.getFluidState(hit.getBlockPos()).isSource() ? hit : null;
    }
}
