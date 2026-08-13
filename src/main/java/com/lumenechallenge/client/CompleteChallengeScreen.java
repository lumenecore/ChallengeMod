package com.lumenechallenge.client;

import com.lumenechallenge.client.layout.ModI18n;

import com.lumenechallenge.LumeneChallengeMod;
import com.lumenechallenge.challenge.ChallengeState;
import com.lumenechallenge.challenge.ModifierType;
import com.lumenechallenge.client.layout.HudLayoutConfig;
import com.lumenechallenge.client.layout.NotificationManager;
import com.lumenechallenge.util.TimeUtil;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.text.Text;
import net.minecraft.util.Util;
import org.lwjgl.glfw.GLFW;

import java.net.URI;
import java.util.List;

public class CompleteChallengeScreen extends Screen {
    private static final int PANEL_WIDTH = GuiPanelRenderer.COMPLETE_PANEL_WIDTH;
    private static final int PANEL_HEIGHT = GuiPanelRenderer.COMPLETE_PANEL_HEIGHT;

    private static final int TOP_EMPTY_HEIGHT = 30;
    private static final int LEFT_WIDTH = 130;
    private static final int RIGHT_WIDTH = 150;
    private static final int LEFT_HEIGHT = 115;
    private static final int RIGHT_HEIGHT = 115;
    private static final int FOOTER_HEIGHT = 35;

    private static final int TIMER_HEIGHT = 40;
    private static final int SEED_HEIGHT = 25;
    private static final int LINKS_HEIGHT = 50;

    private static final int CLOSE_BUTTON_WIDTH = 100;
    private static final int CLOSE_BUTTON_HEIGHT = 16;
    private static final int CLOSE_BUTTON_BOTTOM_MARGIN = 10;

    private static final int MODIFIER_BADGE_WIDTH = 124;
    private static final int MODIFIER_BADGE_HEIGHT = 24;
    private static final int MODIFIER_BADGE_LEFT_MARGIN = 3;
    private static final int MODIFIER_BADGE_RIGHT_MARGIN = 3;
    private static final int MODIFIER_BADGE_BOTTOM_MARGIN = 3;
    private static final int MODIFIER_ICON_SIZE = 24;
    private static final int MODIFIER_LABEL_WIDTH = 100;
    private static final int MODIFIER_ICON_LEFT_PADDING = 2;

    private static final int LINK_BUTTON_WIDTH = 72;
    private static final int LINK_BUTTON_HEIGHT = 22;
    private static final int LINK_INNER_MARGIN = 3;

    private static final float TIMER_TEXT_SCALE = 1.6f;

    private static final String YOUTUBE_URL = "https://www.youtube.com/@lumenecore";
    private static final String TELEGRAM_URL = "https://t.me/lumenecore";
    private static final String MODRINTH_URL = "https://modrinth.com/mod/challengemod";
    private static final String DONATEPAY_URL = "https://new.donatepay.ru/@lumenecore";

    private static final net.minecraft.util.Identifier CLOSE_BUTTON =
            net.minecraft.util.Identifier.of(LumeneChallengeMod.MOD_ID, "textures/gui/complete_close_button.png");
    private static final net.minecraft.util.Identifier CLOSE_BUTTON_HOVER =
            net.minecraft.util.Identifier.of(LumeneChallengeMod.MOD_ID, "textures/gui/complete_close_button_hover.png");

    private static final net.minecraft.util.Identifier YOUTUBE_BUTTON =
            net.minecraft.util.Identifier.of(LumeneChallengeMod.MOD_ID, "textures/gui/complete_youtube.png");
    private static final net.minecraft.util.Identifier YOUTUBE_BUTTON_HOVER =
            net.minecraft.util.Identifier.of(LumeneChallengeMod.MOD_ID, "textures/gui/complete_youtube_hover.png");
    private static final net.minecraft.util.Identifier TELEGRAM_BUTTON =
            net.minecraft.util.Identifier.of(LumeneChallengeMod.MOD_ID, "textures/gui/complete_telegram.png");
    private static final net.minecraft.util.Identifier TELEGRAM_BUTTON_HOVER =
            net.minecraft.util.Identifier.of(LumeneChallengeMod.MOD_ID, "textures/gui/complete_telegram_hover.png");
    private static final net.minecraft.util.Identifier MODRINTH_BUTTON =
            net.minecraft.util.Identifier.of(LumeneChallengeMod.MOD_ID, "textures/gui/complete_modrinth.png");
    private static final net.minecraft.util.Identifier MODRINTH_BUTTON_HOVER =
            net.minecraft.util.Identifier.of(LumeneChallengeMod.MOD_ID, "textures/gui/complete_modrinth_hover.png");
    private static final net.minecraft.util.Identifier DONATEPAY_BUTTON =
            net.minecraft.util.Identifier.of(LumeneChallengeMod.MOD_ID, "textures/gui/complete_donatepay.png");
    private static final net.minecraft.util.Identifier DONATEPAY_BUTTON_HOVER =
            net.minecraft.util.Identifier.of(LumeneChallengeMod.MOD_ID, "textures/gui/complete_donatepay_hover.png");

    private static final net.minecraft.util.Identifier MODIFIER_BADGE =
            net.minecraft.util.Identifier.of(LumeneChallengeMod.MOD_ID, "textures/gui/complete_modifier_badge.png");

    private final ChallengeState state;

    private int panelX;
    private int panelY;
    private Bounds seedBounds = new Bounds(0, 0, 0, 0);

    public CompleteChallengeScreen(ChallengeState state) {
        super(ModI18n.text("screen.lumenechallenge.complete.title"));
        this.state = state;
    }

    @Override
    protected void init() {
        HudLayoutConfig layout = HudLayoutConfig.get(width, height);
        panelX = layout.completePanel.x(width, PANEL_WIDTH);
        panelY = layout.completePanel.y(height, PANEL_HEIGHT);
    }

    @Override
    public boolean shouldPause() {
        return true;
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return true;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        context.fill(0, 0, width, height, 0xA0000000);
        GuiPanelRenderer.drawCompletePanel(context, panelX, panelY, state == null ? null : state.rank());

        drawModifierBadge(context);
        drawTimer(context);
        drawSeed(context, mouseX, mouseY);
        drawLinkButtons(context, mouseX, mouseY);
        drawCloseButton(context, mouseX, mouseY);
        NotificationManager.render(context, textRenderer, width, height);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) {
            return super.mouseClicked(mouseX, mouseY, button);
        }

        if (seedBounds.contains(mouseX, mouseY)) {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client != null) {
                client.keyboard.setClipboard(displaySeedText());
                NotificationManager.show(ModI18n.text("notification.lumenechallenge.seed_copied"));
            }
            return true;
        }

        Bounds closeButton = closeButtonBounds();
        if (closeButton.contains(mouseX, mouseY)) {
            close();
            return true;
        }

        for (ButtonInfo info : linkButtons()) {
            if (info.bounds().contains(mouseX, mouseY)) {
                openLink(info.url());
                return true;
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER || keyCode == GLFW.GLFW_KEY_ESCAPE) {
            closeOrDeleteWorld();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private void closeOrDeleteWorld() {
        if (LumeneChallengeClient.shouldAutoDeleteCompletedWorlds()) {
            LumeneChallengeClient.finishCompletedWorldAndReturnToWorldList();
            return;
        }
        close();
    }

    private void drawTimer(DrawContext context) {
        int x = panelX + LEFT_WIDTH;
        int y = panelY + TOP_EMPTY_HEIGHT;
        drawCenteredScaledText(
                context,
                Text.literal(TimeUtil.formatTicks(state == null ? 0L : state.finishTime() - state.startTime())),
                x,
                y,
                RIGHT_WIDTH,
                TIMER_HEIGHT,
                TIMER_TEXT_SCALE,
                0xFFFFFF
        );
    }

    private void drawSeed(DrawContext context, int mouseX, int mouseY) {
        int x = panelX + LEFT_WIDTH;
        int y = panelY + TOP_EMPTY_HEIGHT + TIMER_HEIGHT;
        Text seedText = ModI18n.text("hud.lumenechallenge.seed", displaySeedText());

        int textX = x + 3;
        int textY = y + Math.max(0, (SEED_HEIGHT - textRenderer.fontHeight) / 2);
        int textWidth = textRenderer.getWidth(seedText);
        int textHeight = textRenderer.fontHeight;

        context.drawTextWithShadow(textRenderer, seedText, textX, textY, 0xFFFFFF);

        seedBounds = new Bounds(textX, textY, textWidth, textHeight);
        if (seedBounds.contains(mouseX, mouseY)) {
            int underlineY = textY + textHeight + 1;
            context.fill(textX, underlineY, textX + textWidth, underlineY + 1, 0xFFFFFFFF);
        }
    }

    private void drawLinkButtons(DrawContext context, int mouseX, int mouseY) {
        for (ButtonInfo info : linkButtons()) {
            boolean hovered = info.bounds().contains(mouseX, mouseY);
            drawTexture(
                    context,
                    hovered ? info.hoverTexture() : info.texture(),
                    info.bounds().x(),
                    info.bounds().y(),
                    info.bounds().width(),
                    info.bounds().height()
            );
        }
    }

    private void drawCloseButton(DrawContext context, int mouseX, int mouseY) {
        Bounds bounds = closeButtonBounds();
        boolean hovered = bounds.contains(mouseX, mouseY);
        drawTexture(context, hovered ? CLOSE_BUTTON_HOVER : CLOSE_BUTTON, bounds.x(), bounds.y(), bounds.width(), bounds.height());

        Text label = ModI18n.text("screen.lumenechallenge.complete.button");
        context.drawCenteredTextWithShadow(
                textRenderer,
                label,
                bounds.x() + bounds.width() / 2,
                bounds.y() + (bounds.height() - textRenderer.fontHeight) / 2,
                0xFFFFFF
        );
    }

    private void drawModifierBadge(DrawContext context) {
        if (state == null || !state.modifierEnabled() || !state.modifier().isVisible()) {
            return;
        }

        int x = panelX + MODIFIER_BADGE_LEFT_MARGIN;
        int y = panelY + TOP_EMPTY_HEIGHT + LEFT_HEIGHT - MODIFIER_BADGE_HEIGHT - MODIFIER_BADGE_BOTTOM_MARGIN;
        drawTexture(context, MODIFIER_BADGE, x, y, MODIFIER_BADGE_WIDTH, MODIFIER_BADGE_HEIGHT);

        ModifierType modifier = state.modifier();
        context.drawTexture(
                RenderLayer::getGuiTextured,
                modifier.icon(),
                x + MODIFIER_ICON_LEFT_PADDING,
                y,
                0.0f,
                0.0f,
                MODIFIER_ICON_SIZE,
                MODIFIER_ICON_SIZE,
                32,
                32,
                32,
                32
        );

        String modifierLabel = textRenderer.trimToWidth(ModI18n.text(modifier.translationKey()).getString(), MODIFIER_LABEL_WIDTH - 8);
        context.drawCenteredTextWithShadow(
                textRenderer,
                Text.literal(modifierLabel),
                x + MODIFIER_ICON_SIZE + MODIFIER_LABEL_WIDTH / 2,
                y + (MODIFIER_BADGE_HEIGHT - textRenderer.fontHeight) / 2,
                0xFFFFFF
        );
    }

    private List<ButtonInfo> linkButtons() {
        int statsX = panelX + LEFT_WIDTH;
        int statsY = panelY + TOP_EMPTY_HEIGHT;
        int innerX = statsX + LINK_INNER_MARGIN;
        int innerY = statsY + TIMER_HEIGHT + SEED_HEIGHT + LINK_INNER_MARGIN;

        return List.of(
                new ButtonInfo(YOUTUBE_URL, YOUTUBE_BUTTON, YOUTUBE_BUTTON_HOVER, new Bounds(innerX, innerY, LINK_BUTTON_WIDTH, LINK_BUTTON_HEIGHT)),
                new ButtonInfo(TELEGRAM_URL, TELEGRAM_BUTTON, TELEGRAM_BUTTON_HOVER, new Bounds(innerX + LINK_BUTTON_WIDTH, innerY, LINK_BUTTON_WIDTH, LINK_BUTTON_HEIGHT)),
                new ButtonInfo(MODRINTH_URL, MODRINTH_BUTTON, MODRINTH_BUTTON_HOVER, new Bounds(innerX, innerY + LINK_BUTTON_HEIGHT, LINK_BUTTON_WIDTH, LINK_BUTTON_HEIGHT)),
                new ButtonInfo(DONATEPAY_URL, DONATEPAY_BUTTON, DONATEPAY_BUTTON_HOVER, new Bounds(innerX + LINK_BUTTON_WIDTH, innerY + LINK_BUTTON_HEIGHT, LINK_BUTTON_WIDTH, LINK_BUTTON_HEIGHT))
        );
    }

    private Bounds closeButtonBounds() {
        int x = panelX + (PANEL_WIDTH - CLOSE_BUTTON_WIDTH) / 2;
        int y = panelY + PANEL_HEIGHT - CLOSE_BUTTON_HEIGHT - CLOSE_BUTTON_BOTTOM_MARGIN;
        return new Bounds(x, y, CLOSE_BUTTON_WIDTH, CLOSE_BUTTON_HEIGHT);
    }

    private void drawCenteredScaledText(DrawContext context, Text text, int x, int y, int width, int height, float scale, int color) {
        int textWidth = textRenderer.getWidth(text);
        float scaledWidth = textWidth * scale;
        float scaledHeight = textRenderer.fontHeight * scale;
        float drawX = x + (width - scaledWidth) / 2.0f;
        float drawY = y + (height - scaledHeight) / 2.0f;

        var matrices = context.getMatrices();
        matrices.push();
        matrices.scale(scale, scale, 1.0f);
        context.drawTextWithShadow(
                textRenderer,
                text,
                Math.round(drawX / scale),
                Math.round(drawY / scale),
                color
        );
        matrices.pop();
    }

    private void openLink(String url) {
        try {
            URI uri = Util.validateUri(url);
            Util.getOperatingSystem().open(uri);
        } catch (Exception ignored) {
        }
    }

    private void drawTexture(DrawContext context, net.minecraft.util.Identifier texture, int x, int y, int width, int height) {
        if (!textureExists(texture)) {
            context.fill(x, y, x + width, y + height, 0xFFFFFFFF);
            return;
        }
        context.drawTexture(RenderLayer::getGuiTextured, texture, x, y, 0, 0, width, height, width, height);
    }

    private boolean textureExists(net.minecraft.util.Identifier texture) {
        MinecraftClient client = MinecraftClient.getInstance();
        return client != null && client.getResourceManager().getResource(texture).isPresent();
    }

    private String displaySeedText() {
        if (state == null) {
            return "0";
        }
        String text = state.challengeSeedText();
        if (text == null || text.isBlank()) {
            return Long.toString(state.challengeSeed());
        }
        return text;
    }

    @Override
    public void close() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client != null) {
            client.setScreen(null);
        }
    }

    private record Bounds(int x, int y, int width, int height) {
        private boolean contains(double mouseX, double mouseY) {
            return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
        }
    }

    private record ButtonInfo(String url, net.minecraft.util.Identifier texture, net.minecraft.util.Identifier hoverTexture, Bounds bounds) {
    }
}
