package com.bossmode.entity.boss;

import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent;
import net.minecraft.world.entity.LivingEntity;

import java.util.ArrayList;
import java.util.List;

/**
 * Owns the single {@link ServerBossEvent} of one boss and keeps its audience in sync: players join it inside
 * {@code range}, and leave it when they move away, die, switch to spectator, change dimension or log out.
 * <p>
 * The event is created on the first server tick with the entity's (final, loaded) UUID - the client HUD uses this
 * to find the entity and draw the decorated bar. Server-side only.
 */
public final class BossBarController {
	private final LivingEntity boss;
	private final double range;
	private final int membershipInterval;
	private BossEvent.BossBarColor color;
	private final BossEvent.BossBarOverlay overlay;
	private boolean darken;
	private ServerBossEvent event;

	public BossBarController(LivingEntity boss, double range, int membershipInterval, BossEvent.BossBarColor color, BossEvent.BossBarOverlay overlay) {
		this.boss = boss;
		this.range = range;
		this.membershipInterval = membershipInterval;
		this.color = color;
		this.overlay = overlay;
	}

	public void tick(ServerLevel level) {
		if (this.event == null) {
			this.event = new ServerBossEvent(this.boss.getUUID(), this.boss.getDisplayName(), this.color, this.overlay);
			this.event.setDarkenScreen(this.darken);
			this.event.setCreateWorldFog(false);
			this.event.setPlayBossMusic(true);
		}
		this.event.setProgress(Mth.clamp(this.boss.getHealth() / this.boss.getMaxHealth(), 0.0F, 1.0F));
		if (this.boss.tickCount % this.membershipInterval != 0) {
			return;
		}
		double rangeSqr = this.range * this.range;
		List<ServerPlayer> toRemove = new ArrayList<>();
		for (ServerPlayer player : this.event.getPlayers()) {
			if (!shouldSee(level, player, rangeSqr)) {
				toRemove.add(player);
			}
		}
		toRemove.forEach(this.event::removePlayer);
		for (ServerPlayer player : level.players()) {
			if (shouldSee(level, player, rangeSqr)) {
				this.event.addPlayer(player);
			}
		}
	}

	private boolean shouldSee(ServerLevel level, ServerPlayer player, double rangeSqr) {
		return !this.boss.isRemoved() && this.boss.isAlive() && !player.hasDisconnected() && player.isAlive() && !player.isSpectator()
				&& player.level() == level && player.distanceToSqr(this.boss) <= rangeSqr;
	}

	public int viewerCount() {
		return this.event == null ? 0 : this.event.getPlayers().size();
	}

	public boolean isDarkeningSky() {
		return this.darken;
	}

	public void setStyle(BossEvent.BossBarColor color, boolean darken) {
		this.color = color;
		this.darken = darken;
		if (this.event != null) {
			this.event.setColor(color);
			this.event.setDarkenScreen(darken);
		}
	}

	public void updateName() {
		if (this.event != null) {
			this.event.setName(this.boss.getDisplayName());
		}
	}

	/** Called on death and whenever the entity is removed (killed, discarded, unloaded, changed dimension). */
	public void removeAll() {
		if (this.event != null) {
			this.event.setProgress(0.0F);
			this.event.removeAllPlayers();
			this.event.setVisible(false);
		}
	}
}
