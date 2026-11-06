package com.example.redstonetransportation.vehicle;

import com.example.redstonetransportation.content.ModContent;
import net.minecraft.core.NonNullList;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.ContainerUser;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.piglin.PiglinAi;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.minecart.AbstractMinecartContainer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.Vec3;

/** Freight cart: a 32-slot container on the tram chassis, loaded and unloaded by hoppers. */
public final class CargoTramEntity extends AbstractMinecartContainer implements TramVehicle {
    private final WheelAnimator wheels = new WheelAnimator();

    public CargoTramEntity(EntityType<? extends CargoTramEntity> type, Level level) {
        super(type, level);
    }

    @Override
    protected Item getDropItem() {
        return ModContent.CARGO_TRAM_ITEM;
    }

    @Override
    public ItemStack getPickResult() {
        return new ItemStack(ModContent.CARGO_TRAM_ITEM);
    }

    @Override
    public int getContainerSize() {
        return CargoTramMenu.SLOTS;
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory) {
        return new CargoTramMenu(containerId, inventory, this);
    }

    /** Keeps the list at its full length, so a tram saved with the old 36-slot hold loads all of it. */
    @Override
    public void clearItemStacks() {
        getItemStacks().clear();
    }

    /** What an old tram held past the end of the hold moves up into it, or falls out if there is no room. */
    private void unpackOldSlots(ServerLevel level) {
        NonNullList<ItemStack> stacks = getItemStacks();
        for (int slot = CargoTramMenu.SLOTS; slot < stacks.size(); slot++) {
            if (stacks.get(slot).isEmpty()) continue;
            ItemStack left = HopperBlockEntity.addItem(null, this, stacks.set(slot, ItemStack.EMPTY), null);
            if (!left.isEmpty()) spawnAtLocation(level, left);
        }
    }

    @Override
    public void stopOpen(ContainerUser containerUser) {
        level().gameEvent(GameEvent.CONTAINER_CLOSE, position(), GameEvent.Context.of(containerUser.getLivingEntity()));
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand, Vec3 location) {
        InteractionResult result = interactWithContainerVehicle(player);
        if (result.consumesAction() && player.level() instanceof ServerLevel serverLevel) {
            gameEvent(GameEvent.CONTAINER_OPEN, player);
            PiglinAi.angerNearbyPiglins(serverLevel, player, true);
        }
        return result;
    }

    @Override
    public float wheelAngle(float partialTick) {
        return wheels.angle(partialTick);
    }

    @Override
    public void tick() {
        super.tick();
        if (level() instanceof ServerLevel serverLevel) {
            unpackOldSlots(serverLevel);
        } else {
            wheels.tick(position());
        }
    }
}
