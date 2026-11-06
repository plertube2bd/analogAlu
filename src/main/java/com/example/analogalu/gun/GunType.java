package com.example.analogalu.gun;

import net.minecraft.world.item.Item;

import java.util.function.Supplier;

/**
 * The three guns. Damage is in health points (a player has 20) and armour reduces it as it would any
 * hit. A shotgun's damage is per pellet, and the pellets that hit one target are added up.
 */
public enum GunType {
    /** Six rounds, R to cock before every shot, a long reload once all six are gone. */
    REVOLVER("revolver", 6, 25.0F, 32.0D, 1, 0.0D, 80, 4, true, () -> ModGuns.BULLET_9MM),
    /** One ball at a time straight from the inventory, but a long wait between shots. */
    MUSKET("musket", 1, 30.0F, 48.0D, 1, 0.0D, 0, 60, false, () -> ModGuns.MUSKET_BALL),
    /** Two shells per long reload, forty pellets in a cone each. */
    SHOTGUN("shotgun", 2, 1.5F, 16.0D, 40, 9.0D, 70, 10, false, () -> ModGuns.SHOTGUN_SHELL);

    private final String id;
    private final int capacity;
    private final float damage;
    private final double range;
    private final int pellets;
    private final double spreadDegrees;
    private final int reloadTicks;
    private final int shotCooldown;
    private final boolean cocked;
    private final Supplier<Item> ammo;

    GunType(String id, int capacity, float damage, double range, int pellets, double spreadDegrees,
            int reloadTicks, int shotCooldown, boolean cocked, Supplier<Item> ammo) {
        this.id = id;
        this.capacity = capacity;
        this.damage = damage;
        this.range = range;
        this.pellets = pellets;
        this.spreadDegrees = spreadDegrees;
        this.reloadTicks = reloadTicks;
        this.shotCooldown = shotCooldown;
        this.cocked = cocked;
        this.ammo = ammo;
    }

    public String id() {
        return id;
    }

    /** Rounds one load holds. The musket loads from the inventory with every shot instead. */
    public int capacity() {
        return capacity;
    }

    /** Health points one bullet, or one pellet, takes before armour. */
    public float damage() {
        return damage;
    }

    /** Blocks a shot carries; past this nothing is hit. */
    public double range() {
        return range;
    }

    public int pellets() {
        return pellets;
    }

    /** Half-angle of the cone the pellets spread over. */
    public double spreadDegrees() {
        return spreadDegrees;
    }

    public int reloadTicks() {
        return reloadTicks;
    }

    public int shotCooldown() {
        return shotCooldown;
    }

    /** True when the hammer has to be pulled back with R before each shot. */
    public boolean needsCocking() {
        return cocked;
    }

    /** True for the guns loaded with R; the musket takes a ball for every shot as it is fired. */
    public boolean usesMagazine() {
        return this != MUSKET;
    }

    public Item ammo() {
        return ammo.get();
    }
}
