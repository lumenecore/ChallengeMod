package com.lumenechallenge.client;

import com.lumenechallenge.LumeneChallengeMod;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.Identifier;
import net.minecraft.resource.ResourceManager;

import java.util.HashMap;
import java.util.Map;


public final class GuiPanelRenderer {
    public static final Identifier OBJECTIVE_PANEL = Identifier.of(LumeneChallengeMod.MOD_ID, "textures/gui/objective_panel.png");
    public static final Identifier TIMER_PANEL = Identifier.of(LumeneChallengeMod.MOD_ID, "textures/gui/timer_panel.png");
    public static final Identifier COMPLETE_PANEL = Identifier.of(LumeneChallengeMod.MOD_ID, "textures/gui/complete_panel.png");
    public static final Identifier COMPLETE_REROLL_PANEL = Identifier.of(LumeneChallengeMod.MOD_ID, "textures/gui/complete_reroll_panel.png");

    public static final Identifier REROLL_PANEL = Identifier.of(LumeneChallengeMod.MOD_ID, "textures/gui/reroll_panel.png");
    public static final Identifier REROLL_BUTTON = Identifier.of(LumeneChallengeMod.MOD_ID, "textures/gui/reroll_button.png");
    public static final Identifier REROLL_BUTTON_HOVER = Identifier.of(LumeneChallengeMod.MOD_ID, "textures/gui/reroll_button_hover.png");
    public static final Identifier REROLL_BUTTON_CANCEL = Identifier.of(LumeneChallengeMod.MOD_ID, "textures/gui/reroll_button_cancel.png");
    public static final Identifier REROLL_BUTTON_CANCEL_HOVER = Identifier.of(LumeneChallengeMod.MOD_ID, "textures/gui/reroll_button_cancel_hover.png");

    public static final int OBJECTIVE_PANEL_WIDTH = 150;
    public static final int OBJECTIVE_PANEL_HEIGHT = 100;
    public static final int TIMER_PANEL_WIDTH = 80;
    public static final int TIMER_PANEL_HEIGHT = 25;
    public static final int TIMER_TEXT_LEFT = 7;
    public static final int TIMER_TEXT_WIDTH = 48;
    public static final int TIMER_TEXT_HEIGHT = 13;
    public static final int MODIFIER_ICON_SIZE = 32;
    public static final int COMPLETE_PANEL_WIDTH = 280;
    public static final int COMPLETE_PANEL_HEIGHT = 180;
    public static final int COMPLETE_REROLL_PANEL_WIDTH = 40;
    public static final int COMPLETE_REROLL_PANEL_HEIGHT = 25;
    public static final int REROLL_TEXT_LEFT = 7;
    public static final int REROLL_TEXT_WIDTH = 11;
    public static final int REROLL_TEXT_HEIGHT = 13;

    public static final int REROLL_PANEL_WIDTH = 200;
    public static final int REROLL_PANEL_HEIGHT = 80;
    public static final int REROLL_BUTTON_WIDTH = 85;
    public static final int REROLL_BUTTON_HEIGHT = 15;

    public static final Identifier NOTIFICATION_PANEL = Identifier.of(LumeneChallengeMod.MOD_ID, "textures/gui/notification.png");

    // Layout-editor previews are deliberately separate from the textures used during a run.
    public static final Identifier OBJECTIVE_PANEL_PREVIEW = Identifier.of(LumeneChallengeMod.MOD_ID, "textures/gui/preview/objective_panel.png");
    public static final Identifier MODIFIER_PREVIEW = Identifier.of(LumeneChallengeMod.MOD_ID, "textures/gui/preview/modifier.png");
    public static final Identifier TIMER_PANEL_PREVIEW = Identifier.of(LumeneChallengeMod.MOD_ID, "textures/gui/preview/timer_panel.png");
    public static final Identifier REROLL_PANEL_PREVIEW = Identifier.of(LumeneChallengeMod.MOD_ID, "textures/gui/preview/reroll_panel.png");
    public static final Identifier SEED_PANEL_PREVIEW = Identifier.of(LumeneChallengeMod.MOD_ID, "textures/gui/preview/seed_panel.png");
    public static final Identifier NOTIFICATION_PANEL_PREVIEW = Identifier.of(LumeneChallengeMod.MOD_ID, "textures/gui/preview/notification.png");

    public static final Identifier OBJECTIVE_PANEL_PREVIEW_HOVER = Identifier.of(LumeneChallengeMod.MOD_ID, "textures/gui/preview/objective_panel_hover.png");
    public static final Identifier MODIFIER_PREVIEW_HOVER = Identifier.of(LumeneChallengeMod.MOD_ID, "textures/gui/preview/modifier_hover.png");
    public static final Identifier TIMER_PANEL_PREVIEW_HOVER = Identifier.of(LumeneChallengeMod.MOD_ID, "textures/gui/preview/timer_panel_hover.png");
    public static final Identifier REROLL_PANEL_PREVIEW_HOVER = Identifier.of(LumeneChallengeMod.MOD_ID, "textures/gui/preview/reroll_panel_hover.png");
    public static final Identifier SEED_PANEL_PREVIEW_HOVER = Identifier.of(LumeneChallengeMod.MOD_ID, "textures/gui/preview/seed_panel_hover.png");
    public static final Identifier NOTIFICATION_PANEL_PREVIEW_HOVER = Identifier.of(LumeneChallengeMod.MOD_ID, "textures/gui/preview/notification_hover.png");

    private static ResourceManager cachedResourceManager;
    private static final Map<Identifier, Boolean> TEXTURE_EXISTS = new HashMap<>();

    private GuiPanelRenderer() {
    }

    /**
     * Draws the HUD notification background, fading it with the notification's alpha.
     * The matching notification.png texture is already part of the project resources.
     */
    public static void drawNotificationPanel(DrawContext context, int x, int y, int width, int height, float alpha) {
        int clampedAlpha = Math.max(0, Math.min(255, Math.round(alpha * 255.0f)));
        drawTexture(context, NOTIFICATION_PANEL, x, y, width, height, 100, 20,
                (clampedAlpha << 24) | 0x00ADFF2F);
    }

    public static void drawObjectivePanelPreview(DrawContext context, int x, int y, boolean hovered) {
        drawTexture(context, hovered ? OBJECTIVE_PANEL_PREVIEW_HOVER : OBJECTIVE_PANEL_PREVIEW,
                x, y, OBJECTIVE_PANEL_WIDTH, OBJECTIVE_PANEL_HEIGHT);
    }

    public static void drawModifierPreview(DrawContext context, int x, int y, boolean hovered) {
        drawTexture(context, hovered ? MODIFIER_PREVIEW_HOVER : MODIFIER_PREVIEW,
                x, y, MODIFIER_ICON_SIZE, MODIFIER_ICON_SIZE);
    }

    public static void drawTimerPanelPreview(DrawContext context, int x, int y, boolean hovered) {
        drawTexture(context, hovered ? TIMER_PANEL_PREVIEW_HOVER : TIMER_PANEL_PREVIEW,
                x, y, TIMER_PANEL_WIDTH, TIMER_PANEL_HEIGHT);
    }

    public static void drawRerollPanelPreview(DrawContext context, int x, int y, boolean hovered) {
        drawTexture(context, hovered ? REROLL_PANEL_PREVIEW_HOVER : REROLL_PANEL_PREVIEW,
                x, y, COMPLETE_REROLL_PANEL_WIDTH, COMPLETE_REROLL_PANEL_HEIGHT);
    }

    public static void drawSeedPanelPreview(DrawContext context, int x, int y, boolean hovered) {
        drawTexture(context, hovered ? SEED_PANEL_PREVIEW_HOVER : SEED_PANEL_PREVIEW,
                x, y, com.lumenechallenge.client.layout.HudLayoutConfig.SEED_PANEL_WIDTH,
                com.lumenechallenge.client.layout.HudLayoutConfig.SEED_PANEL_HEIGHT);
    }

    public static void drawNotificationPanelPreview(DrawContext context, int x, int y, boolean hovered) {
        drawTexture(context, hovered ? NOTIFICATION_PANEL_PREVIEW_HOVER : NOTIFICATION_PANEL_PREVIEW,
                x, y, 100, 20);
    }

    public static void drawObjectivePanel(DrawContext context, int x, int y) {
        drawTexture(context, OBJECTIVE_PANEL, x, y, OBJECTIVE_PANEL_WIDTH, OBJECTIVE_PANEL_HEIGHT);
    }

    public static void drawObjectivePanel(DrawContext context, int x, int y, int width, int height) {
        drawTexture(context, OBJECTIVE_PANEL, x, y, width, height, OBJECTIVE_PANEL_WIDTH, OBJECTIVE_PANEL_HEIGHT);
    }

    public static void drawTimerPanel(DrawContext context, int x, int y) {
        drawTexture(context, TIMER_PANEL, x, y, TIMER_PANEL_WIDTH, TIMER_PANEL_HEIGHT);
    }

    public static void drawCompletePanel(DrawContext context, int x, int y) {
        drawTexture(context, COMPLETE_PANEL, x, y, COMPLETE_PANEL_WIDTH, COMPLETE_PANEL_HEIGHT);
    }

    public static void drawCompleteRerollPanel(DrawContext context, int x, int y) {
        drawTexture(context, COMPLETE_REROLL_PANEL, x, y, COMPLETE_REROLL_PANEL_WIDTH, COMPLETE_REROLL_PANEL_HEIGHT);
    }

    public static void drawRerollPanel(DrawContext context, int x, int y) {
        drawTexture(context, REROLL_PANEL, x, y, REROLL_PANEL_WIDTH, REROLL_PANEL_HEIGHT);
    }

    public static void drawRerollButton(DrawContext context, int x, int y, boolean hovered) {
        drawTexture(context, hovered ? REROLL_BUTTON_HOVER : REROLL_BUTTON, x, y, REROLL_BUTTON_WIDTH, REROLL_BUTTON_HEIGHT);
    }

    public static void drawRerollCancelButton(DrawContext context, int x, int y, boolean hovered) {
        drawTexture(context, hovered ? REROLL_BUTTON_CANCEL_HOVER : REROLL_BUTTON_CANCEL, x, y, REROLL_BUTTON_WIDTH, REROLL_BUTTON_HEIGHT);
    }

    private static void drawTexture(DrawContext context, Identifier texture, int x, int y, int width, int height) {
        drawTexture(context, texture, x, y, width, height, width, height);
    }

    private static void drawTexture(DrawContext context, Identifier texture, int x, int y, int width, int height, int textureWidth, int textureHeight) {
        drawTexture(context, texture, x, y, width, height, textureWidth, textureHeight, 0xFFFFFFFF);
    }

    private static void drawTexture(DrawContext context, Identifier texture, int x, int y, int width, int height, int textureWidth, int textureHeight, int color) {
        if (!hasTexture(texture)) {
            context.fill(x, y, x + width, y + height, color);
            return;
        }
        context.drawTexture(net.minecraft.client.gl.RenderPipelines.GUI_TEXTURED, texture, x, y, 0, 0, width, height, textureWidth, textureHeight, color);
    }

    /** Shared, resource-manager-aware texture draw helper for custom screens. */
    public static void drawTextureIfAvailable(DrawContext context, Identifier texture,
                                              int x, int y, int width, int height,
                                              int textureWidth, int textureHeight, int fallbackColor) {
        if (!hasTexture(texture)) {
            context.fill(x, y, x + width, y + height, fallbackColor);
            return;
        }

        // fallbackColor is only for the missing-texture fallback. Passing it as the
        // texture tint darkens every textured screen element (e.g. SeedSelectionScreen).
        context.drawTexture(net.minecraft.client.gl.RenderPipelines.GUI_TEXTURED,
                texture, x, y, 0, 0, width, height, textureWidth, textureHeight, 0xFFFFFFFF);
    }

    public static boolean hasTexture(Identifier texture) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null) return false;
        ResourceManager manager = client.getResourceManager();
        if (manager != cachedResourceManager) {
            cachedResourceManager = manager;
            TEXTURE_EXISTS.clear();
        }
        return TEXTURE_EXISTS.computeIfAbsent(texture, id -> manager.getResource(id).isPresent());
    }
}
