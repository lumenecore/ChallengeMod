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
        Notification notification = current;
        if (notification == null || textRenderer == null) {
            return;
        }

        long elapsed = Util.getMeasuringTimeMs() - notification.startedAtMs;
        long total = FADE_IN_MS + HOLD_MS + FADE_OUT_MS;
        if (elapsed >= total) {
            if (current == notification) current = null;
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

        Layout layout = notification.layout(textRenderer);
        HudLayoutConfig config = HudLayoutConfig.get(screenWidth, screenHeight);
        HudLayoutConfig.PanelPlacement placement = config.notificationPanel;
        int x = placement.x(screenWidth, layout.panelWidth);
        int y = placement.y(screenHeight, layout.panelHeight);

        GuiPanelRenderer.drawNotificationPanel(context, x, y, layout.panelWidth, layout.panelHeight, alpha);

        int textColor = ((int) (alpha * 255.0f) << 24) | 0xFFFFFFFF;
        for (int i = 0; i < layout.lines.size(); i++) {
            context.drawCenteredTextWithShadow(textRenderer, layout.lines.get(i),
                    x + layout.panelWidth / 2, y + layout.startY + i * layout.lineHeight, textColor);
        }
    }

    private static Layout createLayout(TextRenderer textRenderer, Text text) {
        List<OrderedText> lines = wrapLines(textRenderer, text, 260);
        int maxWidth = 0;
        for (OrderedText line : lines) {
            maxWidth = Math.max(maxWidth, textRenderer.getWidth(line));
        }

        int paddingX = 12;
        int paddingY = 5;
        int lineHeight = textRenderer.fontHeight + 1;
        int panelWidth = Math.max(100, maxWidth + paddingX * 2);
        int totalTextHeight = lines.size() * lineHeight - 1;
        int panelHeight = Math.max(20, totalTextHeight + paddingY * 2);
        int startY = Math.max(0, (panelHeight - totalTextHeight) / 2);
        return new Layout(List.copyOf(lines), panelWidth, panelHeight, lineHeight, startY);
    }

    private static List<OrderedText> wrapLines(TextRenderer textRenderer, Text text, int maxWidth) {
        List<OrderedText> result = new ArrayList<>();
        String[] rawLines = text.getString().split("\\R", -1);
        for (String rawLine : rawLines) {
            List<OrderedText> wrapped = textRenderer.wrapLines(Text.literal(rawLine), maxWidth);
            if (!wrapped.isEmpty()) {
                result.addAll(wrapped);
            }
        }
        if (result.isEmpty()) {
            result.addAll(textRenderer.wrapLines(text, maxWidth));
        }
        if (result.isEmpty()) {
            result.add(Text.literal("").asOrderedText());
        }
        return result;
    }

    private static final class Notification {
        private final Text text;
        private final long startedAtMs;
        private TextRenderer layoutRenderer;
        private Layout cachedLayout;

        private Notification(Text text, long startedAtMs) {
            this.text = text;
            this.startedAtMs = startedAtMs;
        }

        private Layout layout(TextRenderer textRenderer) {
            if (cachedLayout == null || layoutRenderer != textRenderer) {
                cachedLayout = createLayout(textRenderer, text);
                layoutRenderer = textRenderer;
            }
            return cachedLayout;
        }
    }

    private record Layout(List<OrderedText> lines, int panelWidth, int panelHeight, int lineHeight, int startY) {
    }
}
