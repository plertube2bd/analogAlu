package com.example.redstonetransportation.vehicle;

import com.example.redstonetransportation.content.ModAdvancements;
import com.example.redstonetransportation.RedstoneTransportation;
import com.example.redstonetransportation.rail.RailDrive;
import com.example.redstonetransportation.rail.TickedCart;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.minecart.AbstractMinecart;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Handles player-operated couplings and the spring constraint between linked carts.
 * Connections are stored as an attachment on each cart, so any minecart type can be coupled
 * and the links survive world saves/restarts.
 */
public final class TrainLinkManager {
    public static final AttachmentType<List<UUID>> LINKS = AttachmentRegistry.createPersistent(
            Identifier.fromNamespaceAndPath(RedstoneTransportation.MOD_ID, "train_links"), UUIDUtil.CODEC.listOf());

    private static final Map<UUID, UUID> PENDING_FIRST_CART = new ConcurrentHashMap<>();
    private static final int MAX_COUPLINGS = 2;
    private static final double MAX_COUPLE_DISTANCE = 5.0D;
    private static final double MIN_GAP = 1.48D;
    private static final double VANILLA_HALF_LENGTH = 0.5D;
    private static final double SPRING_STRENGTH = 0.060D;
    private static final double DAMPING_STRENGTH = 0.22D;
    private static final double MAX_LINK_IMPULSE = 0.075D;
    private static final double CRUISE_CORRECTION = 0.5D;
    private static final double MAX_CRUISE_CORRECTION = 1.0D;

    private TrainLinkManager() {}

    public static void initialize() {
        // Triggers this class's static attachment registration.
    }

    public static List<UUID> getLinks(AbstractMinecart cart) {
        return cart.getAttachedOrElse(LINKS, List.of());
    }

    public static boolean isCoupled(AbstractMinecart cart, Entity other) {
        return other instanceof AbstractMinecart && getLinks(cart).contains(other.getUUID());
    }

    public static boolean addLink(AbstractMinecart cart, UUID id) {
        List<UUID> links = getLinks(cart);
        if (id == null || id.equals(cart.getUUID()) || links.contains(id) || links.size() >= MAX_COUPLINGS) {
            return false;
        }
        List<UUID> updated = new ArrayList<>(links);
        updated.add(id);
        cart.setAttached(LINKS, List.copyOf(updated));
        return true;
    }

    public static void removeLink(AbstractMinecart cart, UUID id) {
        List<UUID> updated = new ArrayList<>(getLinks(cart));
        if (!updated.remove(id)) return;
        if (updated.isEmpty()) {
            cart.removeAttached(LINKS);
        } else {
            cart.setAttached(LINKS, List.copyOf(updated));
        }
    }

    /** Called when a player uses the tram coupler item on any minecart. */
    public static void handleCouplerUse(Player player, AbstractMinecart clicked, ServerLevel level) {
        UUID playerId = player.getUUID();

        if (player.isShiftKeyDown()) {
            PENDING_FIRST_CART.remove(playerId);
            boolean hadLinks = !getLinks(clicked).isEmpty();
            disconnectAll(clicked, level);
            player.sendOverlayMessage(Component.translatable(
                    hadLinks
                            ? "message.redstone_transportation.coupler_disconnected"
                            : "message.redstone_transportation.coupler_no_links"
            ));
            return;
        }

        UUID firstId = PENDING_FIRST_CART.remove(playerId);
        if (firstId == null) {
            PENDING_FIRST_CART.put(playerId, clicked.getUUID());
            player.sendOverlayMessage(Component.translatable("message.redstone_transportation.coupler_select_second"));
            return;
        }

        if (firstId.equals(clicked.getUUID())) {
            player.sendOverlayMessage(Component.translatable("message.redstone_transportation.coupler_selection_cancelled"));
            return;
        }

        Map<UUID, AbstractMinecart> loadedCarts = getLoadedCarts(level);
        AbstractMinecart first = loadedCarts.get(firstId);
        if (first == null) {
            player.sendOverlayMessage(Component.translatable("message.redstone_transportation.coupler_first_missing"));
            return;
        }

        if (first.level() != clicked.level()) {
            player.sendOverlayMessage(Component.translatable("message.redstone_transportation.coupler_wrong_dimension"));
            return;
        }

        if (first.distanceTo(clicked) > MAX_COUPLE_DISTANCE) {
            player.sendOverlayMessage(Component.translatable("message.redstone_transportation.coupler_too_far"));
            return;
        }

        if (getLinks(first).size() >= MAX_COUPLINGS || getLinks(clicked).size() >= MAX_COUPLINGS) {
            player.sendOverlayMessage(Component.translatable("message.redstone_transportation.coupler_no_free_end"));
            return;
        }

        if (wouldCreateCycle(first, clicked, loadedCarts)) {
            player.sendOverlayMessage(Component.translatable("message.redstone_transportation.coupler_same_train"));
            return;
        }

        addLink(first, clicked.getUUID());
        addLink(clicked, first.getUUID());
        player.sendOverlayMessage(Component.translatable("message.redstone_transportation.coupler_connected"));
        ModAdvancements.award(player, ModAdvancements.TRAIN);
    }

    /** Remove both ends of every coupling when a cart is permanently destroyed/discarded. */
    public static void onCartRemoved(AbstractMinecart cart, ServerLevel level) {
        disconnectAll(cart, level);
    }

    /** Applies a small spring-and-damper force once for each linked pair per entity tick. */
    public static void tick(AbstractMinecart cart, ServerLevel level) {
        for (UUID otherId : getLinks(cart)) {
            // Both entities tick. Process the pair only from the lower UUID to avoid double force.
            if (cart.getUUID().compareTo(otherId) >= 0) continue;

            AbstractMinecart other = findNearby(level, cart, otherId);
            if (other == null || (RailDrive.isCruising(cart) && RailDrive.isCruising(other))) continue;

            Vec3 offset = other.getPosition(1.0F).subtract(cart.getPosition(1.0F));
            double distance = offset.length();
            if (distance < 0.001D) continue;

            Vec3 axis = offset.scale(1.0D / distance);
            Vec3 cartVelocity = cart.getDeltaMovement();
            Vec3 otherVelocity = other.getDeltaMovement();
            double relativeSpeed = otherVelocity.subtract(cartVelocity).dot(axis);
            double distanceError = distance - targetGap(cart, other);

            double impulse = Math.clamp(
                    distanceError * SPRING_STRENGTH + relativeSpeed * DAMPING_STRENGTH,
                    -MAX_LINK_IMPULSE,
                    MAX_LINK_IMPULSE
            );

            cart.setDeltaMovement(cartVelocity.add(axis.scale(impulse)));
            other.setDeltaMovement(otherVelocity.subtract(axis.scale(impulse)));
        }
    }

    /** Keeps the long tram bodies from overlapping; plain carts keep the original spacing. */
    private static double targetGap(AbstractMinecart first, AbstractMinecart second) {
        return Math.max(MIN_GAP, halfLength(first) + halfLength(second));
    }

    private static double halfLength(AbstractMinecart cart) {
        return cart instanceof TramVehicle ? TramVehicle.BODY_HALF_LENGTH : VANILLA_HALF_LENGTH;
    }

    /** How much faster or slower than the cruise speed a car should run to hold its gap to the car ahead. */
    public static double gapCorrection(AbstractMinecart cart, AbstractMinecart ahead) {
        Vec3 aheadPosition = ((TickedCart) ahead).redstoneTransportation$positionAtTickStart(cart.level().getGameTime());
        double gapError = cart.position().distanceTo(aheadPosition) - targetGap(cart, ahead);
        return Math.clamp(gapError * CRUISE_CORRECTION, -MAX_CRUISE_CORRECTION, MAX_CRUISE_CORRECTION);
    }

    /** Runs an action on a cart and every car coupled to it, directly or through other cars. */
    public static void forEachCar(AbstractMinecart cart, ServerLevel level, Consumer<AbstractMinecart> action) {
        walkTrain(cart, level, (car, ahead) -> action.accept(car));
    }

    /**
     * Visits a train starting from its front car in the {@code travel} direction, passing each
     * car together with the car one step closer to the front (null for the front car).
     */
    public static void walkFromFront(AbstractMinecart cart, Vec3 travel, ServerLevel level,
                                     BiConsumer<AbstractMinecart, AbstractMinecart> action) {
        List<AbstractMinecart> cars = new ArrayList<>();
        forEachCar(cart, level, cars::add);
        AbstractMinecart front = cars.stream()
                .max(Comparator.comparingDouble(car -> car.position().dot(travel)))
                .orElse(cart);
        walkTrain(front, level, action);
    }

    private static void walkTrain(AbstractMinecart start, ServerLevel level,
                                  BiConsumer<AbstractMinecart, AbstractMinecart> action) {
        action.accept(start, null);
        Set<UUID> visited = new HashSet<>();
        visited.add(start.getUUID());
        ArrayDeque<AbstractMinecart> queue = new ArrayDeque<>();
        queue.add(start);
        while (!queue.isEmpty()) {
            AbstractMinecart current = queue.removeFirst();
            for (UUID linkedId : getLinks(current)) {
                if (!visited.add(linkedId)) continue;
                AbstractMinecart linked = findNearby(level, current, linkedId);
                if (linked == null) continue;
                action.accept(linked, current);
                queue.addLast(linked);
            }
        }
    }

    private static void disconnectAll(AbstractMinecart cart, ServerLevel level) {
        List<UUID> oldLinks = getLinks(cart);
        if (oldLinks.isEmpty()) return;

        Map<UUID, AbstractMinecart> loadedCarts = getLoadedCarts(level);
        for (UUID id : oldLinks) {
            AbstractMinecart other = loadedCarts.get(id);
            if (other != null) removeLink(other, cart.getUUID());
            removeLink(cart, id);
        }
    }

    private static boolean wouldCreateCycle(
            AbstractMinecart start,
            AbstractMinecart target,
            Map<UUID, AbstractMinecart> loadedCarts
    ) {
        ArrayDeque<UUID> queue = new ArrayDeque<>();
        Set<UUID> visited = new HashSet<>();
        queue.add(start.getUUID());

        while (!queue.isEmpty()) {
            UUID currentId = queue.removeFirst();
            if (!visited.add(currentId)) continue;
            if (currentId.equals(target.getUUID())) return true;

            AbstractMinecart current = loadedCarts.get(currentId);
            if (current == null) continue;
            for (UUID linkedId : getLinks(current)) {
                if (!visited.contains(linkedId) && loadedCarts.containsKey(linkedId)) {
                    queue.addLast(linkedId);
                }
            }
        }
        return false;
    }

    private static Map<UUID, AbstractMinecart> getLoadedCarts(ServerLevel level) {
        Map<UUID, AbstractMinecart> result = new HashMap<>();
        for (Entity entity : level.getAllEntities()) {
            if (entity instanceof AbstractMinecart cart) {
                result.put(cart.getUUID(), cart);
            }
        }
        return result;
    }

    public static AbstractMinecart findNearby(ServerLevel level, AbstractMinecart origin, UUID wantedId) {
        AABB search = origin.getBoundingBox().inflate(16.0D, 8.0D, 16.0D);
        for (AbstractMinecart candidate : level.getEntitiesOfClass(AbstractMinecart.class, search)) {
            if (candidate.getUUID().equals(wantedId)) return candidate;
        }
        return null;
    }
}
