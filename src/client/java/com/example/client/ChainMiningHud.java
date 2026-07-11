package com.example.client;

import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

public class ChainMiningHud {

	private static final int BG_COLOR = 0xCC0A0A14;
	private static final int BORDER_COLOR = 0xFF4A4A5A;
	private static final int TEXT_COLOR = 0xFFE8E8E8;
	private static final int ACCENT_COLOR = 0xFF55FF55;
	private static final int WARN_COLOR = 0xFFFF5555;
	private static final int DIM_COLOR = 0xFF888888;
	private static final int PADDING = 7;
	private static final int LINE_HEIGHT = 10;

	private static final long FADE_IN_MS = 300;
	private static final long HOLD_MS = 1000;
	private static final long FADE_OUT_MS = 2000;

	private static long pressStartTime = 0;
	private static long releaseTime = 0;
	private static float alpha = 0f;

	private static int applyAlpha(int color, float a) {
		int baseAlpha = (color >> 24) & 0xFF;
		int newAlpha = Math.min(255, Math.max(0, (int)(baseAlpha * a)));
		return (newAlpha << 24) | (color & 0x00FFFFFF);
	}

	public static void register() {
		HudRenderCallback.EVENT.register((drawContext, tickDelta) -> {
			Minecraft client = Minecraft.getInstance();
			if (client.player == null) return;

			boolean active = ChainMiningHandler.INSTANCE.isActive();
			int oreCount = ChainMiningHandler.INSTANCE.getFoundOres().size();
			boolean hasTarget = active && oreCount > 0;
			long now = System.currentTimeMillis();

			if (active) {
				releaseTime = 0;
				if (pressStartTime == 0) {
					pressStartTime = now;
				}
				long elapsed = now - pressStartTime;
				alpha = Math.min(1f, (float)elapsed / FADE_IN_MS);
			} else {
				if (releaseTime == 0) {
					releaseTime = now;
				}
				if (pressStartTime > 0) {
					long elapsed = now - releaseTime;
					if (elapsed < HOLD_MS) {
						alpha = 1f;
					} else if (elapsed < HOLD_MS + FADE_OUT_MS) {
						alpha = 1f - (float)(elapsed - HOLD_MS) / FADE_OUT_MS;
					} else {
						alpha = 0f;
						pressStartTime = 0;
					}
				}
			}

			if (alpha <= 0f) return;

			Font font = client.font;

			String status = active ? "已开启" : "已关闭";
			int statusColor = active ? ACCENT_COLOR : WARN_COLOR;

			String label = "连锁状态：";
			int labelWidth = font.width(label);
			int statusWidth = font.width(status);
			int line1Width = labelWidth + statusWidth;
			int maxWidth = line1Width;
			int visibleLines = 1;

			String line2 = null;
			String line3 = null;
			int line3Color = DIM_COLOR;

			if (hasTarget) {
				line2 = "预计破坏：" + oreCount + " 个方块";
				int l2w = font.width(line2);
				maxWidth = Math.max(maxWidth, l2w);
				visibleLines = 2;

				if (ChainMiningHandler.INSTANCE.canBreak()) {
					line3 = "可破坏";
					line3Color = ACCENT_COLOR;
				} else {
					line3 = "不可破坏";
					line3Color = WARN_COLOR;
				}
				int l3w = font.width(line3);
				maxWidth = Math.max(maxWidth, l3w);
				visibleLines = 3;
			}

			int boxWidth = maxWidth + PADDING * 2;
			int boxHeight = LINE_HEIGHT * visibleLines + PADDING * 2;
			int x = 0;
			int y = 0;

			drawContext.fill(x + 1, y, x + boxWidth - 1, y + boxHeight, applyAlpha(BG_COLOR, alpha));

			int border = applyAlpha(BORDER_COLOR, alpha);
			drawContext.fill(x, y, x + boxWidth, y + 1, border);
			drawContext.fill(x, y + boxHeight - 1, x + boxWidth, y + boxHeight, border);
			drawContext.fill(x, y, x + 1, y + boxHeight, border);
			drawContext.fill(x + boxWidth - 1, y, x + boxWidth, y + boxHeight, border);

			int textX = x + PADDING;
			int textY = y + PADDING;

			int textColor = applyAlpha(TEXT_COLOR, alpha);
			int statusFinal = applyAlpha(statusColor, alpha);
			int shadowAlpha = Math.min(128, (int)(60 * alpha));

			drawContext.drawString(font, label, textX + 1, textY + 1, shadowAlpha << 24, false);
			drawContext.drawString(font, label, textX, textY, textColor, false);
			drawContext.drawString(font, status, textX + labelWidth + 1, textY + 1, shadowAlpha << 24, false);
			drawContext.drawString(font, status, textX + labelWidth, textY, statusFinal, false);

			if (line2 != null) {
				int line2Y = textY + LINE_HEIGHT;
				int dimColor = applyAlpha(DIM_COLOR, alpha);
				drawContext.drawString(font, line2, textX + 1, line2Y + 1, shadowAlpha << 24, false);
				drawContext.drawString(font, line2, textX, line2Y, dimColor, false);
			}

			if (line3 != null) {
				int line3Y = textY + LINE_HEIGHT * 2;
				int l3Final = applyAlpha(line3Color, alpha);
				drawContext.drawString(font, line3, textX + 1, line3Y + 1, shadowAlpha << 24, false);
				drawContext.drawString(font, line3, textX, line3Y, l3Final, false);
			}
		});
	}
}
