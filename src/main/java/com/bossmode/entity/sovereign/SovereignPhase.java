package com.bossmode.entity.sovereign;

import com.bossmode.entity.minion.MinionType;
import net.minecraft.world.BossEvent;

import java.util.List;
import java.util.Set;

import static com.bossmode.entity.minion.MinionType.HOUND;
import static com.bossmode.entity.minion.MinionType.SPITTER;
import static com.bossmode.entity.minion.MinionType.VOLATILE;
import static com.bossmode.entity.minion.MinionType.WRAITH;

/**
 * The ten phases of the Void Sovereign. Each is a full 1,024 HP pool; emptying it moves on to the next. Unlike the
 * Shard Dragon, whose options narrow, the Sovereign <b>evolves</b>: every phase adds patterns, the shard legion answers
 * its call, and phases 3, 6 and 9 open behind a <b>Void Shield</b> that only falls when its Sovereign Shards are broken.
 * Skyward Lash (the counter to aerial smashes) is there from the start; Gravity Well from phase 4; phase 6 adds the
 * Abyssal Carapace, Dimension Rend and Soul Chains; phase 9 the Oblivion ultimate. Phase 10 enrages at 35 %.
 */
public enum SovereignPhase {
	//        scale  speed  dmg   cd    accent       vanilla bar
	ONE(1, 1.00F, 1.00, 1.00, 1.00, 0xFFD01A5A, BossEvent.BossBarColor.RED,
			Set.of("claw_swipe", "tentacle_lash", "ground_pound", "void_orbs", "skyward_lash"),
			List.of(), 0, 0, 0, 0),
	TWO(2, 1.05F, 1.03, 1.08, 0.96, 0xFFD81C62, BossEvent.BossBarColor.RED,
			Set.of("claw_swipe", "tentacle_lash", "ground_pound", "void_orbs", "rift_eruption", "void_grasp", "skyward_lash"),
			List.of(HOUND, SPITTER), 3, 3, 0, 0),
	THREE(3, 1.10F, 1.06, 1.16, 0.92, 0xFFE0206E, BossEvent.BossBarColor.RED,
			Set.of("claw_swipe", "tentacle_lash", "ground_pound", "void_orbs", "rift_eruption", "void_grasp", "void_step", "summon", "skyward_lash"),
			List.of(HOUND, SPITTER, WRAITH), 3, 4, 0, 4),
	FOUR(4, 1.15F, 1.09, 1.24, 0.88, 0xFFC0207E, BossEvent.BossBarColor.PURPLE,
			Set.of("claw_swipe", "tentacle_lash", "void_orbs", "void_grasp", "black_hole", "void_step", "soul_drain", "summon", "skyward_lash",
					"gravity_well", "null_sweep"),
			List.of(SPITTER, VOLATILE, WRAITH), 4, 4, 1, 0),
	FIVE(5, 1.20F, 1.12, 1.32, 0.85, 0xFFB0208A, BossEvent.BossBarColor.PURPLE,
			Set.of("claw_swipe", "tentacle_lash", "ground_pound", "rift_eruption", "black_hole", "void_step", "eclipse", "summon", "skyward_lash",
					"gravity_well", "null_sweep", "tentacle_field"),
			List.of(HOUND, VOLATILE, WRAITH), 4, 4, 1, 0),
	SIX(6, 1.25F, 1.15, 1.40, 0.82, 0xFF8A1CB0, BossEvent.BossBarColor.PURPLE,
			Set.of("claw_swipe", "tentacle_lash", "ground_pound", "void_orbs", "rift_eruption", "void_grasp", "void_step", "soul_drain",
					"skyward_lash", "gravity_well", "tentacle_field", "abyssal_carapace", "dimension_rend", "soul_chains"),
			List.of(SPITTER, VOLATILE, WRAITH), 4, 4, 0, 5),
	SEVEN(7, 1.30F, 1.18, 1.50, 0.78, 0xFF7018A8, BossEvent.BossBarColor.BLUE,
			Set.of("claw_swipe", "tentacle_lash", "ground_pound", "void_orbs", "black_hole", "void_step", "eclipse", "summon", "skyward_lash",
					"gravity_well", "null_sweep", "abyssal_carapace", "dimension_rend", "soul_chains", "abyssal_spears"),
			List.of(HOUND, SPITTER, VOLATILE, WRAITH), 4, 5, 1, 0),
	EIGHT(8, 1.35F, 1.21, 1.60, 0.75, 0xFF5A14A0, BossEvent.BossBarColor.BLUE,
			Set.of("claw_swipe", "tentacle_lash", "rift_eruption", "void_grasp", "black_hole", "void_step", "soul_drain", "summon", "skyward_lash",
					"gravity_well", "null_sweep", "tentacle_field", "abyssal_carapace", "dimension_rend", "soul_chains", "abyssal_spears"),
			List.of(VOLATILE, WRAITH), 4, 5, 2, 0),
	NINE(9, 1.42F, 1.24, 1.72, 0.72, 0xFF3A0E70, BossEvent.BossBarColor.WHITE,
			Set.of("claw_swipe", "tentacle_lash", "ground_pound", "void_orbs", "rift_eruption", "void_grasp", "black_hole", "void_step", "eclipse",
					"soul_drain", "skyward_lash", "gravity_well", "null_sweep", "tentacle_field", "abyssal_carapace", "dimension_rend", "soul_chains",
					"abyssal_spears", "oblivion"),
			List.of(VOLATILE, WRAITH), 3, 4, 0, 6),
	TEN(10, 1.50F, 1.28, 1.85, 0.68, 0xFF2A0A40, BossEvent.BossBarColor.WHITE,
			Set.of("claw_swipe", "tentacle_lash", "ground_pound", "void_orbs", "rift_eruption", "void_grasp", "black_hole", "void_step", "eclipse",
					"soul_drain", "summon", "skyward_lash", "gravity_well", "null_sweep", "tentacle_field", "abyssal_carapace", "dimension_rend",
					"soul_chains", "abyssal_spears", "oblivion"),
			List.of(HOUND, VOLATILE, WRAITH), 4, 4, 1, 0);

	public static final int MAX = 10;
	private static final SovereignPhase[] VALUES = values();

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
	private final int pylons;

	SovereignPhase(int number, float scale, double speedMultiplier, double damageMultiplier, double cooldownMultiplier, int accentColor,
			BossEvent.BossBarColor barColor, Set<String> attacks, List<MinionType> minions, int summonCount, int entryWave, int midBosses, int pylons) {
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
		this.pylons = pylons;
	}

	public int number() { return this.number; }
	public float scale() { return this.scale; }
	public double speedMultiplier() { return this.speedMultiplier; }
	public double damageMultiplier() { return this.damageMultiplier; }
	public double cooldownMultiplier() { return this.cooldownMultiplier; }
	public int accentColor() { return this.accentColor; }
	public BossEvent.BossBarColor barColor() { return this.barColor; }
	public Set<String> attacks() { return this.attacks; }
	/** Minion types this phase can call (Shard Dragon's legion). */
	public List<MinionType> minions() { return this.minions; }
	public int summonCount() { return this.summonCount; }
	/** Minions spawned the moment this phase begins. */
	public int entryWave() { return this.entryWave; }
	/** Shard Warlords spawned the moment this phase begins. */
	public int midBosses() { return this.midBosses; }
	/** Sovereign Shards (void pylons) raised with the shield when this phase begins (0 = no shield). */
	public int pylons() { return this.pylons; }
	public boolean darkensSky() { return this.number >= 4; }

	public SovereignPhase next() {
		return this.ordinal() + 1 < VALUES.length ? VALUES[this.ordinal() + 1] : this;
	}

	public boolean isLast() {
		return this.number == MAX;
	}

	public static SovereignPhase byNumber(int number) {
		return number >= 1 && number <= VALUES.length ? VALUES[number - 1] : ONE;
	}
}
