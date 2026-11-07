package com.bossmode.entity.bastion;

import com.bossmode.entity.minion.MinionType;
import com.bossmode.entity.raid.RaidPhase;
import net.minecraft.world.BossEvent;

import java.util.List;
import java.util.Set;

import static com.bossmode.entity.minion.MinionType.HOUND;
import static com.bossmode.entity.minion.MinionType.SPITTER;
import static com.bossmode.entity.minion.MinionType.VOLATILE;

/**
 * The eight phases of the Bastion Behemoth, each a full 1,024 HP pool - the longest fight of the three raid bosses.
 * Its arsenal widens slowly: the Rolling Charge in phase 2, Fortify in phase 3, spikes and hounds in phase 4, the
 * Magma Breach in phase 5, the Seismic Roar and a Shard Warlord in phase 6, everything from phase 7 (enrages at 30 %
 * of phase 8). It grows x1.0 -> x1.35.
 */
public enum BastionPhase implements RaidPhase {
	//          scale  speed  dmg   cd    accent       vanilla bar
	ONE(1, 1.00F, 1.00, 1.00, 1.00, 0xFFFF9A2A, BossEvent.BossBarColor.YELLOW,
			Set.of("maul", "tail_sweep", "earthquake", "boulder_throw"),
			List.of(), 0, 0, 0, false),
	TWO(2, 1.05F, 1.03, 1.06, 0.97, 0xFFFF8A20, BossEvent.BossBarColor.YELLOW,
			Set.of("maul", "tail_sweep", "earthquake", "boulder_throw", "rolling_charge"),
			List.of(), 0, 0, 0, false),
	THREE(3, 1.10F, 1.06, 1.12, 0.94, 0xFFFF7A18, BossEvent.BossBarColor.YELLOW,
			Set.of("maul", "tail_sweep", "earthquake", "boulder_throw", "rolling_charge", "fortify"),
			List.of(HOUND), 0, 3, 0, false),
	FOUR(4, 1.15F, 1.09, 1.18, 0.91, 0xFFFF6A10, BossEvent.BossBarColor.RED,
			Set.of("maul", "tail_sweep", "earthquake", "boulder_throw", "rolling_charge", "fortify", "crystal_spikes", "summon"),
			List.of(HOUND, SPITTER), 3, 3, 0, false),
	FIVE(5, 1.20F, 1.12, 1.25, 0.88, 0xFFF05A10, BossEvent.BossBarColor.RED,
			Set.of("maul", "tail_sweep", "earthquake", "boulder_throw", "rolling_charge", "fortify", "crystal_spikes", "summon", "magma_breach"),
			List.of(HOUND, SPITTER), 3, 4, 0, false),
	SIX(6, 1.25F, 1.15, 1.32, 0.84, 0xFFE04A0C, BossEvent.BossBarColor.RED,
			Set.of("maul", "tail_sweep", "earthquake", "boulder_throw", "rolling_charge", "fortify", "crystal_spikes", "summon", "magma_breach",
					"seismic_roar"),
			List.of(HOUND, SPITTER, VOLATILE), 4, 4, 1, true),
	SEVEN(7, 1.30F, 1.18, 1.40, 0.80, 0xFFD03A08, BossEvent.BossBarColor.RED,
			Set.of("maul", "tail_sweep", "earthquake", "boulder_throw", "rolling_charge", "fortify", "crystal_spikes", "summon", "magma_breach",
					"seismic_roar"),
			List.of(HOUND, SPITTER, VOLATILE), 4, 4, 0, true),
	EIGHT(8, 1.35F, 1.22, 1.50, 0.75, 0xFFFFD27A, BossEvent.BossBarColor.WHITE,
			Set.of("maul", "tail_sweep", "earthquake", "boulder_throw", "rolling_charge", "fortify", "crystal_spikes", "summon", "magma_breach",
					"seismic_roar"),
			List.of(HOUND, SPITTER, VOLATILE), 4, 5, 1, true);

	public static final int MAX = 8;
	private static final BastionPhase[] VALUES = values();

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
	private final boolean darkensSky;

	BastionPhase(int number, float scale, double speedMultiplier, double damageMultiplier, double cooldownMultiplier, int accentColor,
			BossEvent.BossBarColor barColor, Set<String> attacks, List<MinionType> minions, int summonCount, int entryWave, int midBosses,
			boolean darkensSky) {
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
		this.darkensSky = darkensSky;
	}

	public static BastionPhase byNumber(int number) {
		return VALUES[Math.max(1, Math.min(MAX, number)) - 1];
	}

	@Override public int number() { return this.number; }
	@Override public float scale() { return this.scale; }
	@Override public double speedMultiplier() { return this.speedMultiplier; }
	@Override public double damageMultiplier() { return this.damageMultiplier; }
	@Override public double cooldownMultiplier() { return this.cooldownMultiplier; }
	@Override public int accentColor() { return this.accentColor; }
	@Override public BossEvent.BossBarColor barColor() { return this.barColor; }
	@Override public Set<String> attacks() { return this.attacks; }
	@Override public List<MinionType> minions() { return this.minions; }
	@Override public int summonCount() { return this.summonCount; }
	@Override public int entryWave() { return this.entryWave; }
	@Override public int midBosses() { return this.midBosses; }
	@Override public boolean darkensSky() { return this.darkensSky; }
}
