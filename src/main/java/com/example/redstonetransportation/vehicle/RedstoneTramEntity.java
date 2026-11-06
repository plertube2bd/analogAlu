package com.example.redstonetransportation.vehicle;

import com.example.redstonetransportation.content.ModContent;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.minecart.AbstractMinecart;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

/**
 * Dedicated rideable tram. Rail physics, couplings and drive come from the code shared with
 * every other minecart; this class keeps its own item, top speed and renderer.
 */
public final class RedstoneTramEntity extends AbstractMinecart implements TramVehicle {
    private final WheelAnimator wheels = new WheelAnimator();

    public RedstoneTramEntity(EntityType<? extends RedstoneTramEntity> type, Level level) {
        super(type, level);
    }

    @Override
    protected Item getDropItem() {
        return ModContent.REDSTONE_TRAM_ITEM;
    }

    @Override
    public ItemStack getPickResult() {
        return new ItemStack(ModContent.REDSTONE_TRAM_ITEM);
    }

    @Override
    public boolean isRideable() {
        return true;
    }

    @Override
    public float wheelAngle(float partialTick) {
        return wheels.angle(partialTick);
    }

    /** Trams saved before 1.6.0 keep their couplings in this string instead of the shared attachment. */
    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        String stored = input.getStringOr("train_links", "");
        if (stored.isBlank()) return;

        for (String value : stored.split(",")) {
            try {
                TrainLinkManager.addLink(this, UUID.fromString(value.trim()));
            } catch (IllegalArgumentException ignored) {
                // Ignore malformed legacy/custom data rather than preventing the world from loading.
            }
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide()) wheels.tick(position());
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand, Vec3 location) {
        if (player.isShiftKeyDown() || !getPassengers().isEmpty()) {
            return InteractionResult.PASS;
        }

        if (!level().isClientSide()) {
            player.startRiding(this);
        }
        return InteractionResult.SUCCESS;
    }
}
