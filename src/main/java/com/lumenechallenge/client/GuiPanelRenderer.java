package com.lumenechallenge.client;

import com.lumenechallenge.LumeneChallengeMod;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.util.Identifier;

import java.util.Locale;

public final class GuiPanelRenderer {
    public static final Identifier OBJECTIVE_PANEL = Identifier.of(LumeneChallengeMod.MOD_ID, "textures/gui/objective_panel.png");
    public static final Identifier OBJECTIVE_HIDDEN_PANEL = Identifier.of(LumeneChallengeMod.MOD_ID, "textures/gui/objective_panel_hidden.png");
    public static final Identifier TIMER_PANEL = Identifier.of(LumeneChallengeMod.MOD_ID, "textures/gui/timer_panel.png");
    public static final Identifier COMPLETE_PANEL = Identifier.of(LumeneChallengeMod.MOD_ID, "textures/gui/complete_panel.png");
    public static final Identifier COMPLETE_PANEL_SS = Identifier.of(LumeneChallengeMod.MOD_ID, "textures/gui/complete_panel_ss.png");
    public static final Identifier COMPLETE_PANEL_S = Identifier.of(LumeneChallengeMod.MOD_ID, "textures/gui/complete_panel_s.png");
    public static final Identifier COMPLETE_PANEL_A = Identifier.of(LumeneChallengeMod.MOD_ID, "textures/gui/complete_panel_a.png");
    public static final Identifier COMPLETE_PANEL_B = Identifier.of(LumeneChallengeMod.MOD_ID, "textures/gui/complete_panel_b.png");
    public static final Identifier COMPLETE_PANEL_C = Identifier.of(LumeneChallengeMod.MOD_ID, "textures/gui/complete_panel_c.png");
    public static final Identifier COMPLETE_REROLL_PANEL = Identifier.of(LumeneChallengeMod.MOD_ID, "textures/gui/complete_reroll_panel.png");

    public static final Identifier REROLL_PANEL = Identifier.of(LumeneChallengeMod.MOD_ID, "textures/gui/reroll_panel.png");
    public static final Identifier REROLL_BUTTON = Identifier.of(LumeneChallengeMod.MOD_ID, "textures/gui/reroll_button.png");
    public static final Identifier REROLL_BUTTON_HOVER = Identifier.of(LumeneChallengeMod.MOD_ID, "textures/gui/reroll_button_hover.png");
    public static final Identifier REROLL_BUTTON_CANCEL = Identifier.of(LumeneChallengeMod.MOD_ID, "textures/gui/reroll_button_cancel.png");
    public static final Identifier REROLL_BUTTON_CANCEL_HOVER = Identifier.of(LumeneChallengeMod.MOD_ID, "textures/gui/reroll_button_cancel_hover.png");

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
    public static final int MODIFIER_ICON_SIZE = 32;
    public static final int COMPLETE_PANEL_WIDTH = 280;
    public static final int COMPLETE_PANEL_HEIGHT = 180;
    public static final int COMPLETE_REROLL_PANEL_WIDTH = 90;
    public static final int COMPLETE_REROLL_PANEL_HEIGHT = 23;

    public static final int REROLL_PANEL_WIDTH = 200;
    public static final int REROLL_PANEL_HEIGHT = 80;
    public static final int REROLL_BUTTON_WIDTH = 85;
    public static final int REROLL_BUTTON_HEIGHT = 15;

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

    public static final Identifier STAGE1_PANEL = Identifier.of(LumeneChallengeMod.MOD_ID, "textures/gui/start_stage1_panel.png");
    public static final Identifier STAGE1_FIELD = Identifier.of(LumeneChallengeMod.MOD_ID, "textures/gui/start_stage1_field.png");
    public static final Identifier STAGE1_FIELD_HOVER = Identifier.of(LumeneChallengeMod.MOD_ID, "textures/gui/start_stage1_field_hover.png");
    public static final Identifier STAGE1_NEXT_BUTTON = Identifier.of(LumeneChallengeMod.MOD_ID, "textures/gui/start_stage1_next_button.png");
    public static final Identifier STAGE1_NEXT_BUTTON_HOVER = Identifier.of(LumeneChallengeMod.MOD_ID, "textures/gui/start_stage1_next_button_hover.png");
    public static final Identifier STAGE1_NEXT_BUTTON_DISABLED = Identifier.of(LumeneChallengeMod.MOD_ID, "textures/gui/start_stage1_next_button_disabled.png");
    public static final Identifier STAGE1_RANDOM_BUTTON = Identifier.of(LumeneChallengeMod.MOD_ID, "textures/gui/start_stage1_random_button.png");
    public static final Identifier STAGE1_RANDOM_BUTTON_HOVER = Identifier.of(LumeneChallengeMod.MOD_ID, "textures/gui/start_stage1_random_button_hover.png");

    public static final Identifier STAGE2_PANEL = Identifier.of(LumeneChallengeMod.MOD_ID, "textures/gui/start_stage2_panel.png");
    public static final Identifier STAGE2_DIFFICULTY_NORMAL = Identifier.of(LumeneChallengeMod.MOD_ID, "textures/gui/start_stage2_difficulty_normal.png");
    public static final Identifier STAGE2_DIFFICULTY_NORMAL_SELECTED = Identifier.of(LumeneChallengeMod.MOD_ID, "textures/gui/start_stage2_difficulty_normal_selected.png");
    public static final Identifier STAGE2_DIFFICULTY_HARD = Identifier.of(LumeneChallengeMod.MOD_ID, "textures/gui/start_stage2_difficulty_hard.png");
    public static final Identifier STAGE2_DIFFICULTY_HARD_SELECTED = Identifier.of(LumeneChallengeMod.MOD_ID, "textures/gui/start_stage2_difficulty_hard_selected.png");
    public static final Identifier STAGE2_DIFFICULTY_INSANE = Identifier.of(LumeneChallengeMod.MOD_ID, "textures/gui/start_stage2_difficulty_insane.png");
    public static final Identifier STAGE2_DIFFICULTY_INSANE_SELECTED = Identifier.of(LumeneChallengeMod.MOD_ID, "textures/gui/start_stage2_difficulty_insane_selected.png");
    public static final Identifier STAGE2_NEXT_BUTTON = Identifier.of(LumeneChallengeMod.MOD_ID, "textures/gui/start_stage2_next_button.png");
    public static final Identifier STAGE2_NEXT_BUTTON_HOVER = Identifier.of(LumeneChallengeMod.MOD_ID, "textures/gui/start_stage2_next_button_hover.png");
    public static final Identifier STAGE2_TOOLTIP = Identifier.of(LumeneChallengeMod.MOD_ID, "textures/gui/start_stage2_tooltip.png");

    public static final Identifier STAGE3_PANEL = Identifier.of(LumeneChallengeMod.MOD_ID, "textures/gui/start_stage3_panel.png");
    public static final Identifier STAGE3_CARD = Identifier.of(LumeneChallengeMod.MOD_ID, "textures/gui/start_stage3_card.png");
    public static final Identifier STAGE3_ARROW_LEFT = Identifier.of(LumeneChallengeMod.MOD_ID, "textures/gui/start_stage3_arrow_left.png");
    public static final Identifier STAGE3_ARROW_LEFT_HOVER = Identifier.of(LumeneChallengeMod.MOD_ID, "textures/gui/start_stage3_arrow_left_hover.png");
    public static final Identifier STAGE3_ARROW_RIGHT = Identifier.of(LumeneChallengeMod.MOD_ID, "textures/gui/start_stage3_arrow_right.png");
    public static final Identifier STAGE3_ARROW_RIGHT_HOVER = Identifier.of(LumeneChallengeMod.MOD_ID, "textures/gui/start_stage3_arrow_right_hover.png");
    public static final Identifier STAGE3_START_BUTTON = Identifier.of(LumeneChallengeMod.MOD_ID, "textures/gui/start_stage3_start_button.png");
    public static final Identifier STAGE3_START_BUTTON_HOVER = Identifier.of(LumeneChallengeMod.MOD_ID, "textures/gui/start_stage3_start_button_hover.png");
    public static final Identifier NOTIFICATION_PANEL = Identifier.of(LumeneChallengeMod.MOD_ID, "textures/gui/notification.png");

    private static final int STAGE2_DIFFICULTY_CARD_WIDTH = 150;
    private static final int STAGE2_DIFFICULTY_CARD_HEIGHT = 130;
    private static final int STAGE2_TOOLTIP_WIDTH = 200;
    private static final int STAGE2_TOOLTIP_HEIGHT = 100;

    private static final int STAGE3_CARD_WIDTH = 200;
    private static final int STAGE3_CARD_HEIGHT = 150;
    private static final int STAGE3_ARROW_WIDTH = 50;
    private static final int STAGE3_ARROW_HEIGHT = 50;
    private static final int STAGE3_ICON_SIZE = 96;

    private GuiPanelRenderer() {
    }

    public static void drawObjectivePanel(DrawContext context, int x, int y) {
        drawTexture(context, OBJECTIVE_PANEL, x, y, OBJECTIVE_PANEL_WIDTH, OBJECTIVE_PANEL_HEIGHT);
    }

    public static void drawObjectivePanel(DrawContext context, int x, int y, int width, int height) {
        drawTexture(context, OBJECTIVE_PANEL, x, y, width, height, OBJECTIVE_PANEL_WIDTH, OBJECTIVE_PANEL_HEIGHT);
    }

    public static void drawObjectiveHiddenPanel(DrawContext context, int x, int y) {
        drawTexture(context, OBJECTIVE_HIDDEN_PANEL, x, y, OBJECTIVE_HIDDEN_PANEL_WIDTH, OBJECTIVE_HIDDEN_PANEL_HEIGHT);
    }

    public static void drawTimerPanel(DrawContext context, int x, int y) {
        drawTexture(context, TIMER_PANEL, x, y, TIMER_PANEL_WIDTH, TIMER_PANEL_HEIGHT);
    }

    public static void drawCompletePanel(DrawContext context, int x, int y) {
        drawCompletePanel(context, x, y, null);
    }

    public static void drawCompletePanel(DrawContext context, int x, int y, String rank) {
        drawTexture(context, completePanelTexture(rank), x, y, COMPLETE_PANEL_WIDTH, COMPLETE_PANEL_HEIGHT);
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

    public static void drawStartPanel(DrawContext context, int x, int y) {
        drawTexture(context, START_PANEL, x, y, START_PANEL_WIDTH, START_PANEL_HEIGHT);
    }

    public static void drawStartField(DrawContext context, int x, int y, boolean focused) {
        drawStartField(context, x, y, START_INPUT_WIDTH, START_INPUT_HEIGHT, focused);
    }

    public static void drawStartField(DrawContext context, int x, int y, int width, int height, boolean focused) {
        drawTexture(context, focused ? START_FIELD_FOCUSED : START_FIELD, x, y, width, height, START_INPUT_WIDTH, START_INPUT_HEIGHT);
    }

    public static void drawStartSelector(DrawContext context, int x, int y) {
        drawTexture(context, START_SELECTOR, x, y, START_SELECTOR_WIDTH, START_SELECTOR_HEIGHT);
    }

    public static void drawStartButton(DrawContext context, int x, int y, boolean hovered) {
        drawTexture(context, hovered ? START_BUTTON_HOVER : START_BUTTON, x, y, START_BUTTON_WIDTH, START_BUTTON_HEIGHT);
    }

    public static void drawStartYoutubeButton(DrawContext context, int x, int y, boolean hovered) {
        drawTexture(context, hovered ? START_YOUTUBE_HOVER : START_YOUTUBE, x, y, START_LINK_BUTTON_SIZE, START_LINK_BUTTON_SIZE);
    }

    public static void drawStartTelegramButton(DrawContext context, int x, int y, boolean hovered) {
        drawTexture(context, hovered ? START_TELEGRAM_HOVER : START_TELEGRAM, x, y, START_LINK_BUTTON_SIZE, START_LINK_BUTTON_SIZE);
    }

    public static void drawStartTooltip(DrawContext context, int x, int y) {
        drawTexture(context, START_TOOLTIP, x, y, 220, 64);
    }

    public static void drawNotificationPanel(DrawContext context, int x, int y, int width, int height, float alpha) {
        int color = Math.max(0, Math.min(255, Math.round(alpha * 255.0f)));
        drawTexture(context, NOTIFICATION_PANEL, x, y, width, height, 100, 20, color << 24 | 0x00ADFF2F);
    }

    public static void drawStartArrowLeft(DrawContext context, int x, int y) {
        drawStartArrowLeft(context, x, y, false);
    }

    public static void drawStartArrowLeft(DrawContext context, int x, int y, boolean hovered) {
        drawTexture(context, hovered ? START_ARROW_LEFT_HOVER : START_ARROW_LEFT, x, y, START_ARROW_SIZE, START_ARROW_SIZE);
    }

    public static void drawStartArrowRight(DrawContext context, int x, int y) {
        drawStartArrowRight(context, x, y, false);
    }

    public static void drawStartArrowRight(DrawContext context, int x, int y, boolean hovered) {
        drawTexture(context, hovered ? START_ARROW_RIGHT_HOVER : START_ARROW_RIGHT, x, y, START_ARROW_SIZE, START_ARROW_SIZE);
    }

    public static void drawStage1Panel(DrawContext context, int x, int y) {
        drawTexture(context, STAGE1_PANEL, x, y, 500, 270);
    }

    public static void drawStage1Field(DrawContext context, int x, int y, boolean hovered) {
        drawTexture(context, hovered ? STAGE1_FIELD_HOVER : STAGE1_FIELD, x, y, 200, 40);
    }

    public static void drawStage1NextButton(DrawContext context, int x, int y, boolean hovered) {
        drawTexture(context, hovered ? STAGE1_NEXT_BUTTON_HOVER : STAGE1_NEXT_BUTTON, x, y, 140, 50);
    }

    public static void drawStage1NextButtonDisabled(DrawContext context, int x, int y) {
        drawTexture(context, STAGE1_NEXT_BUTTON_DISABLED, x, y, 140, 50);
    }

    public static void drawStage1RandomButton(DrawContext context, int x, int y, boolean hovered) {
        drawTexture(context, hovered ? STAGE1_RANDOM_BUTTON_HOVER : STAGE1_RANDOM_BUTTON, x, y, 140, 70);
    }

    public static void drawStage2Panel(DrawContext context, int x, int y) {
        drawTexture(context, STAGE2_PANEL, x, y, 500, 270);
    }

    public static void drawStage2DifficultyCard(DrawContext context, Identifier texture, int x, int y, boolean selected) {
        Identifier id = texture;
        if (selected) {
            if (texture == STAGE2_DIFFICULTY_NORMAL) {
                id = STAGE2_DIFFICULTY_NORMAL_SELECTED;
            } else if (texture == STAGE2_DIFFICULTY_HARD) {
                id = STAGE2_DIFFICULTY_HARD_SELECTED;
            } else if (texture == STAGE2_DIFFICULTY_INSANE) {
                id = STAGE2_DIFFICULTY_INSANE_SELECTED;
            }
        }
        drawTexture(context, id, x, y, STAGE2_DIFFICULTY_CARD_WIDTH, STAGE2_DIFFICULTY_CARD_HEIGHT);
    }

    public static void drawStage2NextButton(DrawContext context, int x, int y, boolean hovered) {
        drawTexture(context, hovered ? STAGE2_NEXT_BUTTON_HOVER : STAGE2_NEXT_BUTTON, x, y, 200, 30);
    }

    public static void drawStage2Tooltip(DrawContext context, int x, int y) {
        drawTexture(context, STAGE2_TOOLTIP, x, y, STAGE2_TOOLTIP_WIDTH, STAGE2_TOOLTIP_HEIGHT);
    }

    public static void drawStage3Panel(DrawContext context, int x, int y) {
        drawTexture(context, STAGE3_PANEL, x, y, 500, 270);
    }

    public static void drawStage3Card(DrawContext context, int x, int y) {
        drawTexture(context, STAGE3_CARD, x, y, STAGE3_CARD_WIDTH, STAGE3_CARD_HEIGHT);
    }

    public static void drawStage3ArrowLeft(DrawContext context, int x, int y, boolean hovered) {
        drawTexture(context, hovered ? STAGE3_ARROW_LEFT_HOVER : STAGE3_ARROW_LEFT, x, y, STAGE3_ARROW_WIDTH, STAGE3_ARROW_HEIGHT);
    }

    public static void drawStage3ArrowRight(DrawContext context, int x, int y, boolean hovered) {
        drawTexture(context, hovered ? STAGE3_ARROW_RIGHT_HOVER : STAGE3_ARROW_RIGHT, x, y, STAGE3_ARROW_WIDTH, STAGE3_ARROW_HEIGHT);
    }

    public static void drawStage3StartButton(DrawContext context, int x, int y, boolean hovered) {
        drawTexture(context, hovered ? STAGE3_START_BUTTON_HOVER : STAGE3_START_BUTTON, x, y, 200, 30);
    }

    private static Identifier completePanelTexture(String rank) {
        if (rank == null || rank.isBlank()) {
            return COMPLETE_PANEL_C;
        }
        return switch (rank.trim().toUpperCase(Locale.ROOT)) {
            case "SS" -> COMPLETE_PANEL_SS;
            case "S" -> COMPLETE_PANEL_S;
            case "A" -> COMPLETE_PANEL_A;
            case "B" -> COMPLETE_PANEL_B;
            case "C" -> COMPLETE_PANEL_C;
            default -> COMPLETE_PANEL_C;
        };
    }

    private static void drawTexture(DrawContext context, Identifier texture, int x, int y, int width, int height) {
        drawTexture(context, texture, x, y, width, height, width, height);
    }

    private static void drawTexture(DrawContext context, Identifier texture, int x, int y, int width, int height, int textureWidth, int textureHeight) {
        drawTexture(context, texture, x, y, width, height, textureWidth, textureHeight, 0xFFFFFFFF);
    }

    private static void drawTexture(DrawContext context, Identifier texture, int x, int y, int width, int height, int textureWidth, int textureHeight, int color) {
        if (!textureExists(texture)) {
            context.fill(x, y, x + width, y + height, color);
            return;
        }
        context.drawTexture(RenderLayer::getGuiTextured, texture, x, y, 0, 0, width, height, textureWidth, textureHeight, color);
    }

    private static boolean textureExists(Identifier texture) {
        MinecraftClient client = MinecraftClient.getInstance();
        return client != null && client.getResourceManager().getResource(texture).isPresent();
    }
}
