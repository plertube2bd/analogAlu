package com.example.modernizationworld.redcoat;

/** Seven British infantry roles used by the raid. */
public enum RedcoatKind {
    MUSKETEER("musketeer", 30.0F, 48.0D, 60, false),
    GRENADIER("grenadier", 30.0F, 48.0D, 65, false),
    SERGEANT("sergeant", 0.0F, 0.0D, 0, true),
    OFFICER("officer", 25.0F, 32.0D, 45, true),
    RIFLEMAN("rifleman", 30.0F, 64.0D, 75, false),
    DRUMMER("drummer", 0.0F, 0.0D, 0, false),
    DRAGOON("dragoon", 25.0F, 32.0D, 45, true);

    private final String id;
    private final float damage;
    private final double range;
    private final int cooldown;
    private final boolean melee;

    RedcoatKind(String id, float damage, double range, int cooldown, boolean melee) {
        this.id = id;
        this.damage = damage;
        this.range = range;
        this.cooldown = cooldown;
        this.melee = melee;
    }

    public String id() { return id; }
    public float damage() { return damage; }
    public double range() { return range; }
    public int cooldown() { return cooldown; }
    public boolean melee() { return melee; }
}
