package com.lumenechallenge.client;

import com.lumenechallenge.client.layout.ModI18n;

import com.lumenechallenge.challenge.ChallengeState;
import com.lumenechallenge.challenge.ModifierType;
import com.lumenechallenge.challenge.condition.Condition;
import com.lumenechallenge.client.layout.HudLayoutConfig;
import com.lumenechallenge.client.layout.NotificationManager;
import com.lumenechallenge.util.TimeUtil;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;

import java.util.List;

public final class LumeneChallengeHud {
    private static final int MARGIN = 8;
    private static final int MODIFIER_GAP = 4;
    private static final float MODIFIER_LABEL_SCALE = 0.80f;
    private static final int MODIFIER_TEXT_GAP = 3;

    private LumeneChallengeHud() {
    }

    public static void render(DrawContext context, RenderTickCounter tickCounter) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.world == null || client.textRenderer == null) {
            return;
        }

        if (client.currentScreen instanceof SeedSelectionScreen) {
            return;
        }

        TextRenderer textRenderer = client.textRenderer;
        int screenWidth = client.getWindow().getScaledWidth();
        int screenHeight = client.getWindow().getScaledHeight();

        NotificationManager.render(context, textRenderer, screenWidth, screenHeight);

        ChallengeState state = ClientChallengeBridge.getVisibleState();
        if (state == null) {
            return;
        }


        if (!state.completed() && state.challenge() != null) {
            HudLayoutConfig layout = HudLayoutConfig.get(screenWidth, screenHeight);
            int panelWidth = GuiPanelRenderer.OBJECTIVE_PANEL_WIDTH;
            int panelHeight = GuiPanelRenderer.OBJECTIVE_PANEL_HEIGHT;
            int panelX = layout.objectivePanel.x(screenWidth, panelWidth);
            int panelY = layout.objectivePanel.y(screenHeight, panelHeight);

            GuiPanelRenderer.drawObjectivePanel(context, panelX, panelY, panelWidth, panelHeight);

            int titleHeight = 25;
            int gap = 10;
            int currentBlockHeight = 40;
            int progressBlockHeight = 15;

            int currentTop = panelY + titleHeight + gap;
            int progressTop = currentTop + currentBlockHeight + gap;

            Condition current = state.challenge().currentCondition();
            if (current != null) {
                drawCenteredWrappedText(
                        context,
                        textRenderer,
                        current.getDescription(),
                        panelX + 6,
                        currentTop,
                        panelWidth - 12,
                        currentBlockHeight,
                        0xFFFFFF
                );
            }

            int totalSteps = Math.max(1, state.challenge().conditions().size());
            int percent = (int) ((state.completedSteps() * 100L) / totalSteps);
            drawCenteredWrappedText(
                    context,
                    textRenderer,
                    ModI18n.text("hud.lumenechallenge.progress", percent),
                    panelX + 6,
                    progressTop,
                    panelWidth - 12,
                    progressBlockHeight,
                    0xFFFFFF
            );
        }

        if (state.timerStarted()) {
            HudLayoutConfig layout = HudLayoutConfig.get(screenWidth, screenHeight);
            int timerWidth = GuiPanelRenderer.TIMER_PANEL_WIDTH;
            int timerHeight = GuiPanelRenderer.TIMER_PANEL_HEIGHT;
            int timerX = layout.timerPanel.x(screenWidth, timerWidth);
            int timerY = layout.timerPanel.y(screenHeight, timerHeight);

            int rerollWidth = GuiPanelRenderer.COMPLETE_REROLL_PANEL_WIDTH;
            int rerollX = layout.rerollPanel.x(screenWidth, rerollWidth);
            int rerollY = layout.rerollPanel.y(screenHeight, GuiPanelRenderer.COMPLETE_REROLL_PANEL_HEIGHT);

            GuiPanelRenderer.drawCompleteRerollPanel(context, rerollX, rerollY);
            Text rerolls = Text.literal(state.rerollsRemaining() + "/" + state.maxRerolls());
            context.drawCenteredTextWithShadow(textRenderer, rerolls, rerollX + rerollWidth / 2, rerollY + 7, 0xFFFFFF);

            GuiPanelRenderer.drawTimerPanel(context, timerX, timerY);
            Text timer = Text.literal(TimeUtil.formatTicks(state.getCurrentElapsedTicks(client.getServer())));
            int timerTextY = timerY + (timerHeight - textRenderer.fontHeight) / 2 - 1;
            context.drawCenteredTextWithShadow(textRenderer, timer, timerX + timerWidth / 2, timerTextY, 0xFFFFFF);

            ModifierType modifier = state.modifier();
            if (state.modifierEnabled() && modifier.isVisible()) {
                int modifierX = layout.modifierPanel.x(screenWidth, GuiPanelRenderer.MODIFIER_ICON_SIZE);
                int modifierY = layout.modifierPanel.y(screenHeight, GuiPanelRenderer.MODIFIER_ICON_SIZE);

                context.drawTexture(
                        net.minecraft.client.render.RenderLayer::getGuiTextured,
                        modifier.icon(),
                        modifierX, modifierY, 0, 0,
                        GuiPanelRenderer.MODIFIER_ICON_SIZE, GuiPanelRenderer.MODIFIER_ICON_SIZE,
                        32, 32, 32, 32
                );

                String modifierLabel = ModI18n.text(modifier.translationKey()).getString();
                if (modifier == ModifierType.POCKETS) {
                    int spaceIndex = modifierLabel.indexOf(' ');
                    if (spaceIndex > 0) {
                        modifierLabel = modifierLabel.substring(0, spaceIndex) + "\n" + modifierLabel.substring(spaceIndex + 1);
                    }
                }

                int labelWidth = modifier == ModifierType.POCKETS ? 72 : 120;
                drawTopAlignedMultiline(
                        context,
                        textRenderer,
                        Text.literal(modifierLabel),
                        modifierX + GuiPanelRenderer.MODIFIER_ICON_SIZE / 2,
                        modifierY + GuiPanelRenderer.MODIFIER_ICON_SIZE + MODIFIER_TEXT_GAP,
                        Math.max(labelWidth, GuiPanelRenderer.MODIFIER_ICON_SIZE + 30),
                        MODIFIER_LABEL_SCALE,
                        0xFFFFFF
                );

            }
        }

        if (state.seedSelected() && LumeneChallengeClient.isSeedVisible()) {
            HudLayoutConfig layout = HudLayoutConfig.get(screenWidth, screenHeight);
            Text seedText = ModI18n.text("hud.lumenechallenge.seed", Long.toString(state.challengeSeed()));
            int seedWidth = textRenderer.getWidth(seedText);
            int seedHeight = textRenderer.fontHeight;
            int seedX = layout.seedPanel.x(screenWidth, seedWidth);
            int seedY = layout.seedPanel.y(screenHeight, seedHeight);

            context.drawTextWithShadow(textRenderer, seedText, seedX, seedY, 0xFFFFFF);
        }
    }

    private static void drawCenteredWrappedText(DrawContext context, TextRenderer textRenderer, Text text, int x, int y, int width, int height, int color) {
        List<OrderedText> lines = textRenderer.wrapLines(text, width);
        int lineHeight = textRenderer.fontHeight + 1;
        int totalHeight = Math.max(textRenderer.fontHeight, lines.size() * lineHeight - 1);
        int startY = y + Math.max(0, (height - totalHeight) / 2);

        for (int i = 0; i < lines.size(); i++) {
            context.drawCenteredTextWithShadow(textRenderer, lines.get(i), x + width / 2, startY + i * lineHeight, color);
        }
    }

    private static boolean isInside(double mouseX, double mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }

    private static void drawTopAlignedMultiline(DrawContext context, TextRenderer textRenderer, Text text, int centerX, int topY, int width, float scale, int color) {
        String[] parts = text.getString().split("\\R", -1);
        int lineHeight = Math.max(1, Math.round((textRenderer.fontHeight + 1) * scale));
        int scaledCenterX = Math.round(centerX / scale);
        int scaledTopY = Math.round(topY / scale);
        int scaledWidth = Math.round(width / scale);

        var matrices = context.getMatrices();
        matrices.push();
        matrices.scale(scale, scale, 1.0F);

        int y = scaledTopY;
        for (String part : parts) {
            String trimmed = textRenderer.trimToWidth(part, scaledWidth);
            context.drawCenteredTextWithShadow(textRenderer, trimmed, scaledCenterX, y, color);
            y += lineHeight;
        }

        matrices.pop();
    }
}
