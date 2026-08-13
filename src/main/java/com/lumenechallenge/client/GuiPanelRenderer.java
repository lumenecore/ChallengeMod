package com.lumenechallenge.client;

import com.lumenechallenge.LumeneChallengeMod;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.util.Identifier;

public final class GuiPanelRenderer {
    public static final Identifier OBJECTIVE_PANEL = Identifier.of(LumeneChallengeMod.MOD_ID, "textures/gui/objective_panel.png");
    public static final Identifier OBJECTIVE_HIDDEN_PANEL = Identifier.of(LumeneChallengeMod.MOD_ID, "textures/gui/objective_panel_hidden.png");
    public static final Identifier TIMER_PANEL = Identifier.of(LumeneChallengeMod.MOD_ID, "textures/gui/timer_panel.png");
    public static final Identifier COMPLETE_PANEL = Identifier.of(LumeneChallengeMod.MOD_ID, "textures/gui/complete_panel.png");
    public static final Identifier SEED_PANEL = Identifier.of(LumeneChallengeMod.MOD_ID, "textures/gui/seed_panel.png");
    public static final Identifier CHECKBOX_EMPTY = Identifier.of(LumeneChallengeMod.MOD_ID, "textures/gui/checkbox_empty.png");
    public static final Identifier CHECKBOX_DONE = Identifier.of(LumeneChallengeMod.MOD_ID, "textures/gui/checkbox_done.png");

    public static final Identifier START_PANEL = Identifier.of(LumeneChallengeMod.MOD_ID, "textures/gui/start_panel.png");
    public static final Identifier START_FIELD = Identifier.of(LumeneChallengeMod.MOD_ID, "textures/gui/start_field.png");
    public static final Identifier START_FIELD_FOCUSED = Identifier.of(LumeneChallengeMod.MOD_ID, "textures/gui/start_field_focused.png");
    public static final Identifier START_SELECTOR = Identifier.of(LumeneChallengeMod.MOD_ID, "textures/gui/start_selector.png");
    public static final Identifier START_BUTTON = Identifier.of(LumeneChallengeMod.MOD_ID, "textures/gui/start_button.png");
    public static final Identifier START_BUTTON_HOVER = Identifier.of(LumeneChallengeMod.MOD_ID, "textures/gui/start_button_hover.png");
    public static final Identifier START_TOOLTIP = Identifier.of(LumeneChallengeMod.MOD_ID, "textures/gui/start_tooltip.png");
    public static final Identifier START_ARROW_LEFT = Identifier.of(LumeneChallengeMod.MOD_ID, "textures/gui/start_arrow_left.png");
    public static final Identifier START_ARROW_LEFT_HOVER = Identifier.of(LumeneChallengeMod.MOD_ID, "textures/gui/start_arrow_left_hover.png");
    public static final Identifier START_ARROW_RIGHT = Identifier.of(LumeneChallengeMod.MOD_ID, "textures/gui/start_arrow_right.png");
    public static final Identifier START_ARROW_RIGHT_HOVER = Identifier.of(LumeneChallengeMod.MOD_ID, "textures/gui/start_arrow_right_hover.png");
    public static final Identifier START_YOUTUBE = Identifier.of(LumeneChallengeMod.MOD_ID, "textures/gui/start_youtube.png");
    public static final Identifier START_YOUTUBE_HOVER = Identifier.of(LumeneChallengeMod.MOD_ID, "textures/gui/start_youtube_hover.png");
    public static final Identifier START_TELEGRAM = Identifier.of(LumeneChallengeMod.MOD_ID, "textures/gui/start_telegram.png");
    public static final Identifier START_TELEGRAM_HOVER = Identifier.of(LumeneChallengeMod.MOD_ID, "textures/gui/start_telegram_hover.png");

    public static final int OBJECTIVE_PANEL_WIDTH = 150;
    public static final int OBJECTIVE_PANEL_HEIGHT = 100;
    public static final int OBJECTIVE_HIDDEN_PANEL_WIDTH = 171;
    public static final int OBJECTIVE_HIDDEN_PANEL_HEIGHT = 37;
    public static final int TIMER_PANEL_WIDTH = 102;
    public static final int TIMER_PANEL_HEIGHT = 23;
    public static final int COMPLETE_PANEL_WIDTH = 267;
    public static final int COMPLETE_PANEL_HEIGHT = 173;
    public static final int SEED_PANEL_WIDTH = 240;
    public static final int SEED_PANEL_HEIGHT = 220;
    public static final int MODIFIER_ICON_SIZE = 32;
    public static final int CHECKBOX_SIZE = 8;

    public static final int START_PANEL_WIDTH = 440;
    public static final int START_PANEL_HEIGHT = 250;
    public static final int START_INPUT_WIDTH = 102;
    public static final int START_INPUT_HEIGHT = 22;
    public static final int START_SELECTOR_WIDTH = 206;
    public static final int START_SELECTOR_HEIGHT = 32;
    public static final int START_BUTTON_WIDTH = 172;
    public static final int START_BUTTON_HEIGHT = 24;
    public static final int START_LINK_BUTTON_SIZE = 24;
    public static final int START_ARROW_SIZE = 12;

    private GuiPanelRenderer() {
    }

    public static void drawObjectivePanel(DrawContext context, int x, int y) {
        drawObjectivePanel(context, x, y, OBJECTIVE_PANEL_WIDTH, OBJECTIVE_PANEL_HEIGHT);
    }

    public static void drawObjectivePanel(DrawContext context, int x, int y, int width, int height) {
        context.drawTexture(RenderLayer::getGuiTextured, OBJECTIVE_PANEL, x, y, 0, 0, width, height, OBJECTIVE_PANEL_WIDTH, OBJECTIVE_PANEL_HEIGHT);
    }

    public static void drawObjectiveHiddenPanel(DrawContext context, int x, int y) {
        context.drawTexture(RenderLayer::getGuiTextured, OBJECTIVE_HIDDEN_PANEL, x, y, 0, 0, OBJECTIVE_HIDDEN_PANEL_WIDTH, OBJECTIVE_HIDDEN_PANEL_HEIGHT, OBJECTIVE_HIDDEN_PANEL_WIDTH, OBJECTIVE_HIDDEN_PANEL_HEIGHT);
    }

    public static void drawTimerPanel(DrawContext context, int x, int y) {
        context.drawTexture(RenderLayer::getGuiTextured, TIMER_PANEL, x, y, 0, 0, TIMER_PANEL_WIDTH, TIMER_PANEL_HEIGHT, TIMER_PANEL_WIDTH, TIMER_PANEL_HEIGHT);
    }

    public static void drawCompletePanel(DrawContext context, int x, int y) {
        context.drawTexture(RenderLayer::getGuiTextured, COMPLETE_PANEL, x, y, 0, 0, COMPLETE_PANEL_WIDTH, COMPLETE_PANEL_HEIGHT, COMPLETE_PANEL_WIDTH, COMPLETE_PANEL_HEIGHT);
    }

    public static void drawSeedPanel(DrawContext context, int x, int y) {
        context.drawTexture(RenderLayer::getGuiTextured, SEED_PANEL, x, y, 0, 0, SEED_PANEL_WIDTH, SEED_PANEL_HEIGHT, SEED_PANEL_WIDTH, SEED_PANEL_HEIGHT);
    }

    public static void drawCheckbox(DrawContext context, boolean done, int x, int y) {
        context.drawTexture(RenderLayer::getGuiTextured, done ? CHECKBOX_DONE : CHECKBOX_EMPTY, x, y, 0, 0, CHECKBOX_SIZE, CHECKBOX_SIZE, CHECKBOX_SIZE, CHECKBOX_SIZE);
    }

    public static void drawStartPanel(DrawContext context, int x, int y) {
        context.drawTexture(RenderLayer::getGuiTextured, START_PANEL, x, y, 0, 0, START_PANEL_WIDTH, START_PANEL_HEIGHT, START_PANEL_WIDTH, START_PANEL_HEIGHT);
    }

    public static void drawStartField(DrawContext context, int x, int y, boolean focused) {
        drawStartField(context, x, y, START_INPUT_WIDTH, START_INPUT_HEIGHT, focused);
    }

    public static void drawStartField(DrawContext context, int x, int y, int width, int height, boolean focused) {
        Identifier id = focused ? START_FIELD_FOCUSED : START_FIELD;
        context.drawTexture(RenderLayer::getGuiTextured, id, x, y, 0, 0, width, height, START_INPUT_WIDTH, START_INPUT_HEIGHT);
    }

    public static void drawStartSelector(DrawContext context, int x, int y) {
        context.drawTexture(RenderLayer::getGuiTextured, START_SELECTOR, x, y, 0, 0, START_SELECTOR_WIDTH, START_SELECTOR_HEIGHT, START_SELECTOR_WIDTH, START_SELECTOR_HEIGHT);
    }

    public static void drawStartButton(DrawContext context, int x, int y, boolean hovered) {
        Identifier id = hovered ? START_BUTTON_HOVER : START_BUTTON;
        context.drawTexture(RenderLayer::getGuiTextured, id, x, y, 0, 0, START_BUTTON_WIDTH, START_BUTTON_HEIGHT, START_BUTTON_WIDTH, START_BUTTON_HEIGHT);
    }

    public static void drawStartYoutubeButton(DrawContext context, int x, int y, boolean hovered) {
        Identifier id = hovered ? START_YOUTUBE_HOVER : START_YOUTUBE;
        context.drawTexture(RenderLayer::getGuiTextured, id, x, y, 0, 0, START_LINK_BUTTON_SIZE, START_LINK_BUTTON_SIZE, START_LINK_BUTTON_SIZE, START_LINK_BUTTON_SIZE);
    }

    public static void drawStartTelegramButton(DrawContext context, int x, int y, boolean hovered) {
        Identifier id = hovered ? START_TELEGRAM_HOVER : START_TELEGRAM;
        context.drawTexture(RenderLayer::getGuiTextured, id, x, y, 0, 0, START_LINK_BUTTON_SIZE, START_LINK_BUTTON_SIZE, START_LINK_BUTTON_SIZE, START_LINK_BUTTON_SIZE);
    }

    public static void drawStartTooltip(DrawContext context, int x, int y) {
        context.drawTexture(RenderLayer::getGuiTextured, START_TOOLTIP, x, y, 0, 0, START_TOOLTIP_WIDTH, START_TOOLTIP_HEIGHT, START_TOOLTIP_WIDTH, START_TOOLTIP_HEIGHT);
    }

    private static final int START_TOOLTIP_WIDTH = 220;
    private static final int START_TOOLTIP_HEIGHT = 64;

    public static void drawStartArrowLeft(DrawContext context, int x, int y) {
        drawStartArrowLeft(context, x, y, false);
    }

    public static void drawStartArrowLeft(DrawContext context, int x, int y, boolean hovered) {
        Identifier id = hovered ? START_ARROW_LEFT_HOVER : START_ARROW_LEFT;
        context.drawTexture(RenderLayer::getGuiTextured, id, x, y, 0, 0, START_ARROW_SIZE, START_ARROW_SIZE, START_ARROW_SIZE, START_ARROW_SIZE);
    }

    public static void drawStartArrowRight(DrawContext context, int x, int y) {
        drawStartArrowRight(context, x, y, false);
    }

    public static void drawStartArrowRight(DrawContext context, int x, int y, boolean hovered) {
        Identifier id = hovered ? START_ARROW_RIGHT_HOVER : START_ARROW_RIGHT;
        context.drawTexture(RenderLayer::getGuiTextured, id, x, y, 0, 0, START_ARROW_SIZE, START_ARROW_SIZE, START_ARROW_SIZE, START_ARROW_SIZE);
    }
}
