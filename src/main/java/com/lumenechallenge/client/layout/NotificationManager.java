package com.lumenechallenge.client.layout;

import com.lumenechallenge.client.GuiPanelRenderer;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import net.minecraft.util.Util;

import java.util.ArrayList;
import java.util.List;

public final class NotificationManager {
    private static final long FADE_IN_MS = 500L;
    private static final long HOLD_MS = 2000L;
    private static final long FADE_OUT_MS = 500L;

    private static Notification current;

    private NotificationManager() {
    }

    public static void show(Text text) {
        if (text == null) {
            return;
        }
        current = new Notification(text, Util.getMeasuringTimeMs());
    }

    public static void clear() {
        current = null;
    }

    public static void render(DrawContext context, TextRenderer textRenderer, int screenWidth, int screenHeight) {
        if (current == null || textRenderer == null) {
            return;
        }

        long elapsed = Util.getMeasuringTimeMs() - current.startedAtMs;
        long total = FADE_IN_MS + HOLD_MS + FADE_OUT_MS;
        if (elapsed >= total) {
            current = null;
            return;
        }

        float alpha;
        if (elapsed < FADE_IN_MS) {
            alpha = elapsed / (float) FADE_IN_MS;
        } else if (elapsed < FADE_IN_MS + HOLD_MS) {
            alpha = 1.0f;
        } else {
            alpha = 1.0f - ((elapsed - FADE_IN_MS - HOLD_MS) / (float) FADE_OUT_MS);
        }

        alpha = Math.max(0.0f, Math.min(1.0f, alpha));

        List<OrderedText> lines = wrapLines(textRenderer, current.text, 260);
        int maxWidth = 0;
        for (OrderedText line : lines) {
            maxWidth = Math.max(maxWidth, textRenderer.getWidth(line));
        }

        int paddingX = 12;
        int paddingY = 5;
        int panelWidth = Math.max(100, maxWidth + paddingX * 2);
        int panelHeight = Math.max(20, lines.size() * (textRenderer.fontHeight + 1) + paddingY * 2 - 1);

        HudLayoutConfig config = HudLayoutConfig.get(screenWidth, screenHeight);
        HudLayoutConfig.PanelPlacement placement = config.notificationPanel;
        int x = placement.x(screenWidth, panelWidth);
        int y = placement.y(screenHeight, panelHeight);

        GuiPanelRenderer.drawNotificationPanel(context, x, y, panelWidth, panelHeight, alpha);

        int textColor = ((int) (alpha * 255.0f) << 24) | 0xFFFFFF;
        int totalTextHeight = lines.size() * (textRenderer.fontHeight + 1) - 1;
        int startY = y + Math.max(0, (panelHeight - totalTextHeight) / 2);
        for (int i = 0; i < lines.size(); i++) {
            context.drawCenteredTextWithShadow(textRenderer, lines.get(i), x + panelWidth / 2, startY + i * (textRenderer.fontHeight + 1), textColor);
        }
    }

    private static List<OrderedText> wrapLines(TextRenderer textRenderer, Text text, int maxWidth) {
        List<OrderedText> result = new ArrayList<>();
        String[] rawLines = text.getString().split("\\R", -1);
        for (String rawLine : rawLines) {
            List<OrderedText> wrapped = textRenderer.wrapLines(Text.literal(rawLine), maxWidth);
            if (wrapped.isEmpty()) {
                continue;
            }
            result.addAll(wrapped);
        }
        if (result.isEmpty()) {
            result.addAll(textRenderer.wrapLines(text, maxWidth));
        }
        if (result.isEmpty()) {
            result.add(Text.literal("").asOrderedText());
        }
        return result;
    }

    private record Notification(Text text, long startedAtMs) {
    }
}
