package com.bossmode.entity.tempest;

import com.bossmode.entity.minion.MinionType;
import com.bossmode.entity.raid.RaidPhase;
import net.minecraft.world.BossEvent;

import java.util.List;
import java.util.Set;

import static com.bossmode.entity.minion.MinionType.BREEZE;
import static com.bossmode.entity.minion.MinionType.WRAITH;

/**
 * The five phases of the Tempest Wyvern, each a full 1,024 HP pool. The storm builds: lightning from phase 2,
 * cyclones and breezes from phase 3, the thunder roar and static field from phase 4, everything at once in phase 5
 * (enrages at 30 %). It grows x1.6 -> x2.2 and flies faster every phase.
 */
public enum TempestPhase implements RaidPhase {
	//        scale  speed  dmg   cd    accent       vanilla bar
	ONE(1, 1.60F, 1.00, 1.00, 1.00, 0xFF4FE0FF, BossEvent.BossBarColor.BLUE,
			Set.of("talon_strike", "tail_whip", "wind_gust", "dive_bomb", "feather_barrage"),
			List.of(), 0, 0, 0, false),
	TWO(2, 1.75F, 1.05, 1.10, 0.95, 0xFF3FB8FF, BossEvent.BossBarColor.BLUE,
			Set.of("talon_strike", "tail_whip", "wind_gust", "dive_bomb", "feather_barrage", "lightning_breath", "storm_bolts"),
			List.of(BREEZE), 0, 2, 0, false),
	THREE(3, 1.90F, 1.10, 1.22, 0.90, 0xFF6A9CFF, BossEvent.BossBarColor.WHITE,
			Set.of("talon_strike", "tail_whip", "wind_gust", "dive_bomb", "feather_barrage", "lightning_breath", "storm_bolts", "cyclone", "summon"),
			List.of(BREEZE), 3, 3, 0, false),
	FOUR(4, 2.05F, 1.15, 1.36, 0.84, 0xFF9A7CFF, BossEvent.BossBarColor.PURPLE,
			Set.of("talon_strike", "tail_whip", "wind_gust", "dive_bomb", "feather_barrage", "lightning_breath", "storm_bolts", "cyclone", "summon",
					"thunder_roar", "static_field"),
			List.of(BREEZE, WRAITH), 3, 3, 1, true),
	FIVE(5, 2.20F, 1.22, 1.52, 0.76, 0xFFE8F4FF, BossEvent.BossBarColor.WHITE,
			Set.of("talon_strike", "tail_whip", "wind_gust", "dive_bomb", "feather_barrage", "lightning_breath", "storm_bolts", "cyclone", "summon",
					"thunder_roar", "static_field"),
			List.of(BREEZE, WRAITH), 4, 4, 1, true);

	public static final int MAX = 5;
	private static final TempestPhase[] VALUES = values();

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

	TempestPhase(int number, float scale, double speedMultiplier, double damageMultiplier, double cooldownMultiplier, int accentColor,
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

	public static TempestPhase byNumber(int number) {
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
