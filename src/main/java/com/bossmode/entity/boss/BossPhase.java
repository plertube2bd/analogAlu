package com.bossmode.entity.boss;

import com.bossmode.entity.DamageKind;
import com.bossmode.entity.minion.MinionType;
import net.minecraft.world.BossEvent;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import static com.bossmode.entity.DamageKind.ALLIES;
import static com.bossmode.entity.DamageKind.ENVIRONMENT;
import static com.bossmode.entity.DamageKind.EXPLOSION;
import static com.bossmode.entity.DamageKind.MAGIC;
import static com.bossmode.entity.DamageKind.MELEE;
import static com.bossmode.entity.DamageKind.PROJECTILE;
import static com.bossmode.entity.minion.MinionType.HOUND;
import static com.bossmode.entity.minion.MinionType.SPITTER;
import static com.bossmode.entity.minion.MinionType.VOLATILE;
import static com.bossmode.entity.minion.MinionType.WRAITH;

/**
 * The ten phases of the Shard Dragon. Each phase is a full 1,024 HP pool; emptying it advances to the next phase
 * (phases never go back). As the fight goes on the dragon grows, gets faster and deadlier, while
 * <ul>
 *   <li>its attack pool <b>shrinks</b> to fewer but deadlier patterns,</li>
 *   <li>its minion roster <b>narrows</b> to the most dangerous type while more of them (and mid-bosses) appear,</li>
 *   <li>the ways players can hurt it are <b>sealed</b> one by one (phases 9-10: melee only),</li>
 *   <li>from phase 6 it <b>flies</b>: {@code airTicks} aloft (ranged patterns, circling its target), then it lands (or
 *       meteor-dives) for {@code groundTicks}. While airborne projectiles always hurt it, and enough damage knocks it
 *       out of the sky - so the melee-only phases alternate "shoot it down" with "strike it on the ground".</li>
 * </ul>
 */
public enum BossPhase {
	//    scale speed  dmg   cd    accent       vanilla bar                         attacks
	ONE(1, 2.0F, 1.00, 1.00, 1.00, 0xFF9B5CFF, BossEvent.BossBarColor.PURPLE,
			Set.of("bite", "tail_sweep", "shatter_slam", "shard_volley", "wing_tempest", "obsidian_charge", "crystal_rain", "spike_eruption", "void_roar"),
			List.of(), 0, 0, 0, EnumSet.allOf(DamageKind.class), 0, 0),
	TWO(2, 2.2F, 1.03, 1.10, 0.95, 0xFFB45CFF, BossEvent.BossBarColor.PURPLE,
			Set.of("bite", "tail_sweep", "shatter_slam", "shard_volley", "wing_tempest", "obsidian_charge", "crystal_rain", "spike_eruption", "summon"),
			List.of(WRAITH, HOUND, SPITTER, VOLATILE), 2, 2, 0, EnumSet.of(MELEE, PROJECTILE, EXPLOSION, MAGIC, ALLIES), 0, 0),
	THREE(3, 2.4F, 1.06, 1.20, 0.90, 0xFFCC5CF0, BossEvent.BossBarColor.PURPLE,
			Set.of("bite", "tail_sweep", "shatter_slam", "shard_volley", "obsidian_charge", "crystal_rain", "spike_eruption", "summon"),
			List.of(WRAITH, HOUND, SPITTER, VOLATILE), 3, 3, 0, EnumSet.of(MELEE, PROJECTILE, EXPLOSION, MAGIC, ALLIES), 0, 0),
	FOUR(4, 2.6F, 1.10, 1.30, 0.85, 0xFFE05CD8, BossEvent.BossBarColor.PINK,
			Set.of("bite", "shatter_slam", "shard_volley", "obsidian_charge", "crystal_rain", "prism_beam", "summon"),
			List.of(HOUND, SPITTER, VOLATILE), 3, 3, 0, EnumSet.of(MELEE, PROJECTILE, EXPLOSION, MAGIC), 0, 0),
	FIVE(5, 2.8F, 1.13, 1.40, 0.80, 0xFFF05CB8, BossEvent.BossBarColor.PINK,
			Set.of("bite", "shatter_slam", "obsidian_charge", "crystal_rain", "prism_beam", "meteor_dive", "summon"),
			List.of(HOUND, SPITTER, VOLATILE), 4, 3, 1, EnumSet.of(MELEE, PROJECTILE, EXPLOSION, MAGIC), 0, 0),
	//                                                                                              flight: air / ground ticks
	SIX(6, 3.0F, 1.16, 1.55, 0.75, 0xFFFF5C94, BossEvent.BossBarColor.PINK,
			Set.of("bite", "obsidian_charge", "shard_volley", "prism_beam", "meteor_dive", "summon"),
			List.of(SPITTER, VOLATILE), 4, 4, 0, EnumSet.of(MELEE, PROJECTILE, MAGIC), 240, 300),
	SEVEN(7, 3.2F, 1.20, 1.70, 0.70, 0xFFFF4A6E, BossEvent.BossBarColor.RED,
			Set.of("bite", "prism_beam", "meteor_dive", "void_roar", "summon"),
			List.of(SPITTER, VOLATILE), 5, 4, 1, EnumSet.of(MELEE, PROJECTILE), 260, 280),
	EIGHT(8, 3.4F, 1.23, 1.85, 0.65, 0xFFFF3A4A, BossEvent.BossBarColor.RED,
			Set.of("prism_beam", "meteor_dive", "cataclysm", "summon"),
			List.of(VOLATILE), 5, 5, 0, EnumSet.of(MELEE, PROJECTILE), 280, 260),
	NINE(9, 3.6F, 1.26, 2.00, 0.60, 0xFFE8282E, BossEvent.BossBarColor.RED,
			Set.of("prism_beam", "cataclysm", "summon"),
			List.of(VOLATILE), 6, 5, 2, EnumSet.of(MELEE), 200, 300),
	TEN(10, 4.0F, 1.30, 2.10, 0.60, 0xFFB01020, BossEvent.BossBarColor.RED,
			Set.of("cataclysm", "prism_beam"),
			List.of(), 0, 0, 1, EnumSet.of(MELEE), 200, 300);

	public static final int MAX = 10;
	private static final BossPhase[] VALUES = values();

	private final int number;
	private final float scale;
	private final double speedMultiplier;
	private final double damageMultiplier;
	private final double cooldownMultiplier;
	private final int accentColor;
	private final BossEvent.BossBarColor barColor;
	private final Set<String> attacks;
	private final List<MinionType> minions;
	private final int summonCount;
	private final int entryWave;
	private final int midBosses;
	private final Set<DamageKind> allowedDamage;
	private final int airTicks;
	private final int groundTicks;

	BossPhase(int number, float scale, double speedMultiplier, double damageMultiplier, double cooldownMultiplier, int accentColor,
			BossEvent.BossBarColor barColor, Set<String> attacks, List<MinionType> minions, int summonCount, int entryWave, int midBosses,
			Set<DamageKind> allowedDamage, int airTicks, int groundTicks) {
		this.number = number;
		this.scale = scale;
		this.speedMultiplier = speedMultiplier;
		this.damageMultiplier = damageMultiplier;
		this.cooldownMultiplier = cooldownMultiplier;
		this.accentColor = accentColor;
		this.barColor = barColor;
		this.attacks = attacks;
		this.minions = minions;
		this.summonCount = summonCount;
		this.entryWave = entryWave;
		this.midBosses = midBosses;
		this.allowedDamage = allowedDamage;
		this.airTicks = airTicks;
		this.groundTicks = groundTicks;
	}

	public int number() { return this.number; }
	public float scale() { return this.scale; }
	public double speedMultiplier() { return this.speedMultiplier; }
	public double damageMultiplier() { return this.damageMultiplier; }
	public double cooldownMultiplier() { return this.cooldownMultiplier; }
	public int accentColor() { return this.accentColor; }
	public BossEvent.BossBarColor barColor() { return this.barColor; }
	public Set<String> attacks() { return this.attacks; }
	/** Minion types this phase can summon - the list only ever shrinks. */
	public List<MinionType> minions() { return this.minions; }
	public int summonCount() { return this.summonCount; }
	/** Minions spawned the moment this phase begins. */
	public int entryWave() { return this.entryWave; }
	/** Shard Warlords spawned the moment this phase begins. */
	public int midBosses() { return this.midBosses; }
	public Set<DamageKind> allowedDamage() { return this.allowedDamage; }
	/** Whether the dragon flies in this phase. */
	public boolean canFly() { return this.airTicks > 0; }
	/** How long a flight lasts before the dragon lands (or meteor-dives). */
	public int airTicks() { return this.airTicks; }
	/** How long the dragon stays on the ground before taking off again. */
	public int groundTicks() { return this.groundTicks; }
	public boolean darkensSky() { return this.number >= 7; }

	public int allowedDamageMask() {
		int mask = 0;
		for (DamageKind kind : this.allowedDamage) {
			mask |= kind.bit();
		}
		return mask;
	}

	public boolean atLeast(BossPhase other) {
		return this.ordinal() >= other.ordinal();
	}

	public BossPhase next() {
		return this.ordinal() + 1 < VALUES.length ? VALUES[this.ordinal() + 1] : this;
	}

	public boolean isLast() {
		return this.number == MAX;
	}

	public static BossPhase byNumber(int number) {
		return number >= 1 && number <= VALUES.length ? VALUES[number - 1] : ONE;
	}
}
