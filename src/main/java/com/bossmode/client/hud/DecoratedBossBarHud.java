package com.bossmode.client.hud;

import com.bossmode.BossMode;
import com.bossmode.client.mixin.BossHealthOverlayAccessor;
import com.bossmode.entity.DamageKind;
import com.bossmode.entity.DecoratedBoss;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement;
import net.minecraft.ChatFormatting;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.BossHealthOverlay;
import net.minecraft.client.gui.components.LerpingBossEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * Replaces the vanilla boss bar HUD element. Bars that belong to a {@link DecoratedBoss} (boss bar UUID == entity
 * UUID) get a decorated frame: portrait, name, phase counter, gradient health bar with a lagging damage trail,
 * exact HP, ten phase gems, the damage types that still work (sealed ones crossed out) and ENRAGED / EXPOSED /
 * SEALED status. Every other boss bar (vanilla / other mods) is drawn exactly like vanilla.
 */
public final class DecoratedBossBarHud implements HudElement {
	public static final Identifier ID = BossMode.id("decorated_boss_bars");
	private static final String[] ROMAN = {"", "I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X"};
	private static final int MAIN_WIDTH = 276;
	private static final int MAIN_HEIGHT = 50;
	private static final int MID_WIDTH = 208;
	private static final int MID_HEIGHT = 32;

	private final Map<UUID, float[]> trails = new HashMap<>();
	/** Last known decoration of each boss bar, so the bar stays decorated while the entity is not loaded client-side. */
	private final Map<UUID, BarData> known = new HashMap<>();
	private final long createdAt = System.currentTimeMillis();
	private Object lastLevel;
	private ItemStack[] icons;

	/** Everything the decorated bar draws, captured from the (synced) entity. */
	private record BarData(DecoratedBoss.BarStyle style, int phase, int maxPhase, boolean enraged, boolean exposed, boolean requiresExposure,
			int allowedMask, int accent, String portrait, float health, float maxHealth) {
		static BarData of(LivingEntity entity) {
			DecoratedBoss boss = (DecoratedBoss) entity;
			return new BarData(boss.barStyle(), boss.barPhase(), boss.barMaxPhase(), boss.barEnraged(), boss.barExposed(), boss.barRequiresExposure(),
					boss.barAllowedDamage(), boss.barAccentColor(), boss.barPortrait(), entity.getHealth(), entity.getMaxHealth());
		}

		/** Last known data with the health taken from the boss bar's own progress. */
		BarData withProgress(float progress) {
			return new BarData(style, phase, maxPhase, enraged, exposed, requiresExposure, allowedMask, accent, portrait, progress * maxHealth, maxHealth);
		}
	}

	private ItemStack icon(DamageKind kind) {
		if (this.icons == null) {
			this.icons = new ItemStack[] {new ItemStack(Items.NETHERITE_SWORD), new ItemStack(Items.BOW), new ItemStack(Items.TNT),
					new ItemStack(Items.SPLASH_POTION), new ItemStack(Items.BONE), new ItemStack(Items.LAVA_BUCKET)};
		}
		return this.icons[kind.ordinal()];
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor g, DeltaTracker deltaTracker) {
		Minecraft mc = Minecraft.getInstance();
		BossHealthOverlay overlay = mc.gui.hud.getBossOverlay();
		BossHealthOverlayAccessor accessor = (BossHealthOverlayAccessor) overlay;
		Map<UUID, LerpingBossEvent> events = accessor.boss_mode$getEvents();
		if (mc.level != this.lastLevel) {
			// new world / dimension: forget everything we knew about boss bars
			this.lastLevel = mc.level;
			this.trails.clear();
			this.known.clear();
		}
		if (events.isEmpty()) {
			return;
		}
		Map<UUID, LivingEntity> decorated = new HashMap<>();
		if (mc.level != null) {
			for (Entity entity : mc.level.entitiesForRendering()) {
				if (entity instanceof DecoratedBoss && entity instanceof LivingEntity living && events.containsKey(entity.getUUID())) {
					decorated.put(entity.getUUID(), living);
				}
			}
		}
		g.nextStratum();
		int y = 4;
		for (LerpingBossEvent event : events.values()) {
			LivingEntity entity = decorated.get(event.getId());
			BarData data = entity != null ? BarData.of(entity) : this.known.containsKey(event.getId())
					? this.known.get(event.getId()).withProgress(event.getProgress()) : null;
			if (data != null) {
				this.known.put(event.getId(), data);
			}
			if (data != null && data.style() == DecoratedBoss.BarStyle.MAIN) {
				y = drawMain(g, mc.font, event, data, y);
			} else if (data != null) {
				y = drawMid(g, mc.font, event, data, y);
			} else {
				int x = g.guiWidth() / 2 - 91;
				accessor.boss_mode$extractBar(g, x, y + 9, event);
				Component name = event.getName();
				g.text(mc.font, name, g.guiWidth() / 2 - mc.font.width(name) / 2, y, 0xFFFFFFFF);
				y += 19;
			}
			if (y >= g.guiHeight() / 3) {
				break;
			}
		}
	}

	// ------------------------------------------------------------------------------------------------ main (dragon)
	private int drawMain(GuiGraphicsExtractor g, Font font, LerpingBossEvent event, BarData boss, int y) {
		int accent = boss.accent();
		long now = System.currentTimeMillis();
		float pulse = 0.5F + 0.5F * Mth.sin(now / 180.0F);
		int x0 = g.guiWidth() / 2 - MAIN_WIDTH / 2;
		int x1 = x0 + MAIN_WIDTH;
		frame(g, x0, y, MAIN_WIDTH, MAIN_HEIGHT, accent, pulse);

		// portrait
		int px = x0 + 5;
		int py = y + 5;
		g.fill(px, py, px + 40, py + 40, 0xFF140A1E);
		g.fillGradient(px, py, px + 40, py + 40, withAlpha(accent, 0x50), 0x00000000);
		g.blit(RenderPipelines.GUI_TEXTURED, BossMode.id(boss.portrait()), px + 2, py + 2, 0, 0, 36, 36, 64, 64, 64, 64);
		g.outline(px, py, 40, 40, withAlpha(accent, 0xFF));

		// name + phase
		int tx = x0 + 51;
		Component name = event.getName().copy().withStyle(ChatFormatting.BOLD);
		g.text(font, name, tx, y + 6, lighten(accent, 0.55F), true);
		String phase = "PHASE " + ROMAN[Mth.clamp(boss.phase(), 0, 10)] + " / " + ROMAN[Mth.clamp(boss.maxPhase(), 0, 10)];
		g.text(font, phase, x1 - 7 - font.width(phase), y + 6, boss.enraged() ? blend(0xFFFF4040, 0xFFFFD0D0, pulse) : 0xFFE8DCF4, true);

		// health bar
		int bx = tx;
		int by = y + 18;
		int bw = x1 - 7 - bx;
		healthBar(g, font, event, boss, bx, by, bw, 11, accent);

		// phase gems
		int gy = y + 36;
		for (int i = 1; i <= boss.maxPhase(); i++) {
			int gx = bx + (i - 1) * 11;
			if (i < boss.phase()) {
				gem(g, gx, gy, 0xFF3A2E44, 0xFF5A4A66);
			} else if (i == boss.phase()) {
				gem(g, gx, gy, blend(accent, 0xFFFFFFFF, pulse * 0.35F), 0xFFFFFFFF);
			} else {
				gem(g, gx, gy, 0xFF1A1224, withAlpha(accent, 0x90));
			}
		}

		// status tag
		String status = null;
		int statusColor = 0;
		if (boss.enraged()) {
			status = "ENRAGED";
			statusColor = blend(0xFFFF3030, 0xFFFFB0B0, pulse);
		}
		if (boss.requiresExposure()) {
			status = boss.exposed() ? "EXPOSED" : "SEALED";
			statusColor = boss.exposed() ? blend(0xFF40FF70, 0xFFD0FFD8, pulse) : 0xFF9A90A8;
		}
		int iconsX = x1 - 7 - DamageKind.values().length * 12 + 2;
		if (status != null) {
			scaledText(g, font, status, bx + boss.maxPhase() * 11 + 4, gy + 1, statusColor, 0.75F);
		}

		// damage types that still hurt the boss
		int mask = boss.allowedMask();
		for (DamageKind kind : DamageKind.values()) {
			int ix = iconsX + kind.ordinal() * 12;
			g.pose().pushMatrix();
			g.pose().translate(ix, gy - 1);
			g.pose().scale(0.625F, 0.625F);
			g.item(icon(kind), 0, 0);
			g.pose().popMatrix();
			if ((mask & kind.bit()) == 0) {
				g.fill(ix - 1, gy - 2, ix + 11, gy + 10, 0xB0100010);
				cross(g, ix, gy - 1, 10, 0xFFFF3040);
			}
		}
		return y + MAIN_HEIGHT + 4;
	}

	// ------------------------------------------------------------------------------------------------ mid-boss
	private int drawMid(GuiGraphicsExtractor g, Font font, LerpingBossEvent event, BarData boss, int y) {
		int accent = boss.accent();
		float pulse = 0.5F + 0.5F * Mth.sin(System.currentTimeMillis() / 180.0F);
		int x0 = g.guiWidth() / 2 - MID_WIDTH / 2;
		int x1 = x0 + MID_WIDTH;
		frame(g, x0, y, MID_WIDTH, MID_HEIGHT, accent, pulse);
		int px = x0 + 4;
		int py = y + 4;
		g.fill(px, py, px + 24, py + 24, 0xFF140A1E);
		g.blit(RenderPipelines.GUI_TEXTURED, BossMode.id(boss.portrait()), px + 1, py + 1, 0, 0, 22, 22, 64, 64, 64, 64);
		g.outline(px, py, 24, 24, accent);
		int tx = x0 + 33;
		g.text(font, event.getName(), tx, y + 4, lighten(accent, 0.6F), true);
		scaledText(g, font, "MID-BOSS", x1 - 6 - (int) (font.width("MID-BOSS") * 0.75F), y + 5, blend(accent, 0xFFFFFFFF, pulse * 0.4F), 0.75F);
		healthBar(g, font, event, boss, tx, y + 16, x1 - 6 - tx, 9, accent);
		return y + MID_HEIGHT + 4;
	}

	// ------------------------------------------------------------------------------------------------ pieces
	private void frame(GuiGraphicsExtractor g, int x, int y, int w, int h, int accent, float pulse) {
		g.fill(x, y, x + w, y + h, 0xD0100716);
		g.fillGradient(x, y, x + w, y + h / 2, withAlpha(accent, 0x28), 0x00000000);
		g.outline(x - 1, y - 1, w + 2, h + 2, 0xFF000000);
		g.outline(x, y, w, h, withAlpha(accent, 0xB0));
		g.fill(x + 2, y + 1, x + w - 2, y + 2, withAlpha(lighten(accent, 0.4F), (int) (0x60 + 0x60 * pulse)));
		// crystal corner studs
		for (int[] c : new int[][] {{x - 2, y - 2}, {x + w - 2, y - 2}, {x - 2, y + h - 2}, {x + w - 2, y + h - 2}}) {
			g.fill(c[0], c[1], c[0] + 4, c[1] + 4, 0xFF000000);
			g.fill(c[0] + 1, c[1] + 1, c[0] + 3, c[1] + 3, lighten(accent, 0.3F));
		}
	}

	private void healthBar(GuiGraphicsExtractor g, Font font, LerpingBossEvent event, BarData boss, int x, int y, int w, int h, int accent) {
		float progress = Mth.clamp(event.getProgress(), 0.0F, 1.0F);
		float trail = trail(event.getId(), progress);
		g.fill(x - 1, y - 1, x + w + 1, y + h + 1, 0xFF000000);
		g.fill(x, y, x + w, y + h, 0xFF22142E);
		if (trail > progress) {
			g.fill(x + (int) (w * progress), y, x + (int) (w * trail), y + h, 0xE0F4E4FF);
		}
		int filled = (int) (w * progress);
		if (filled > 0) {
			g.fillGradient(x, y, x + filled, y + h, lighten(accent, 0.25F), darken(accent, 0.45F));
			g.fill(x, y, x + filled, y + 1, 0x70FFFFFF);
		}
		for (int i = 1; i < 10; i++) {
			g.verticalLine(x + w * i / 10, y, y + h - 1, 0x70000000);
		}
		String hp = String.format(Locale.ROOT, "%,d / %,d", Mth.ceil(boss.health()), Mth.ceil(boss.maxHealth()));
		float scale = h >= 11 ? 0.8F : 0.7F;
		scaledText(g, font, hp, x + w / 2 - (int) (font.width(hp) * scale / 2), y + (h - (int) (8 * scale)) / 2, 0xFFFFFFFF, scale);
	}

	/** White "recent damage" segment that lags behind the real health for a moment. */
	private float trail(UUID id, float progress) {
		// seconds since this HUD was created - small enough to keep full float precision
		float now = (System.currentTimeMillis() - this.createdAt) / 1000.0F;
		float[] state = this.trails.computeIfAbsent(id, k -> new float[] {progress, progress, 0, now});
		float dt = Mth.clamp(now - state[3], 0.0F, 0.25F);
		state[3] = now;
		if (progress < state[1]) {
			state[2] = now + 0.45F;
		}
		state[1] = progress;
		if (progress > state[0]) {
			state[0] = progress;
		} else if (now > state[2]) {
			state[0] = Math.max(progress, state[0] - dt * 0.5F);
		}
		return Mth.clamp(state[0], progress, 1.0F);
	}

	private static void gem(GuiGraphicsExtractor g, int x, int y, int fill, int edge) {
		g.fill(x + 3, y, x + 5, y + 8, edge);
		g.fill(x + 1, y + 2, x + 7, y + 6, edge);
		g.fill(x + 3, y + 1, x + 5, y + 7, fill);
		g.fill(x + 2, y + 3, x + 6, y + 5, fill);
	}

	private static void cross(GuiGraphicsExtractor g, int x, int y, int size, int color) {
		for (int i = 0; i < size; i++) {
			g.fill(x + i, y + i, x + i + 2, y + i + 1, color);
			g.fill(x + size - i - 2, y + i, x + size - i, y + i + 1, color);
		}
	}

	private static void scaledText(GuiGraphicsExtractor g, Font font, String text, int x, int y, int color, float scale) {
		g.pose().pushMatrix();
		g.pose().translate(x, y);
		g.pose().scale(scale, scale);
		g.text(font, text, 0, 0, color, true);
		g.pose().popMatrix();
	}

	private static int withAlpha(int color, int alpha) {
		return (Mth.clamp(alpha, 0, 255) << 24) | (color & 0xFFFFFF);
	}

	private static int blend(int a, int b, float t) {
		int ar = a >> 16 & 255, ag = a >> 8 & 255, ab = a & 255;
		int br = b >> 16 & 255, bg = b >> 8 & 255, bb = b & 255;
		return 0xFF000000 | (int) Mth.lerp(t, ar, br) << 16 | (int) Mth.lerp(t, ag, bg) << 8 | (int) Mth.lerp(t, ab, bb);
	}

	private static int lighten(int color, float amount) {
		return blend(color, 0xFFFFFFFF, amount);
	}

	private static int darken(int color, float amount) {
		return blend(color, 0xFF000000, amount);
	}
}
