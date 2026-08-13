package com.lumenechallenge.client;

import com.lumenechallenge.challenge.ModifierType;
import com.lumenechallenge.network.SelectSeedPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import net.minecraft.util.Util;
import org.lwjgl.glfw.GLFW;

import java.net.URI;
import java.util.List;

public class SeedSelectionScreen extends Screen {
    private static final int DEFAULT_CHALLENGE_COUNT = 10;
    private static final int MIN_CHALLENGE_COUNT = 1;
    private static final int MAX_CHALLENGE_COUNT = 50;

    private static final List<ModifierType> MENU_MODIFIERS = ModifierType.menuValues();
    private static final int MENU_MODIFIER_ICON_SIZE = 64;

    private static final int TOOLTIP_WIDTH = 220;
    private static final int TOOLTIP_HEIGHT = 64;

    private static final int HEADER_PADDING_X = 24;
    private static final int HEADER_TOP_Y = 18;
    private static final int HEADER_BLOCK_WIDTH = 160;
    private static final int HEADER_BLOCK_GAP = 72;

    private static final int FIELD_PADDING_X = 10;
    private static final int FIELD_TEXT_TOP = 7;

    private static final int SELECTOR_TOP_Y = 154;
    private static final int SELECTOR_TEXT_LEFT = 26;
    private static final int SELECTOR_TEXT_RIGHT = 26;
    private static final int SELECTOR_TEXT_TOP = 8;
    private static final int SELECTOR_TEXT_BOTTOM = 14;

    private static final int LINK_BUTTON_SIZE = 24;
    private static final int LINK_BUTTON_GAP = 6;
    private static final int LINK_BUTTON_RIGHT_PADDING = 14;

    private static final URI YOUTUBE_URI = URI.create("https://www.youtube.com/@lumenecore");
    private static final URI TELEGRAM_URI = URI.create("https://t.me/lumenecore");

    private String seedInput = "";
    private int seedCursorPos = 0;
    private boolean seedFocused = false;

    private String challengeCountInput = Integer.toString(DEFAULT_CHALLENGE_COUNT);
    private int challengeCountCursorPos = challengeCountInput.length();
    private boolean challengeCountFocused = false;

    private int selectedModifierIndex = 0;
    private int blinkTicks = 0;

    private int panelX;
    private int panelY;

    private int seedBlockX;
    private int seedBlockY;
    private int seedBlockWidth;
    private int seedFieldX;
    private int seedFieldY;
    private int seedFieldWidth;
    private int seedFieldHeight;

    private int countBlockX;
    private int countBlockY;
    private int countBlockWidth;
    private int countFieldX;
    private int countFieldY;
    private int countFieldWidth;
    private int countFieldHeight;

    private int selectorX;
    private int selectorY;
    private int selectorWidth;
    private int selectorHeight;

    private int acceptButtonX;
    private int acceptButtonY;
    private int acceptButtonWidth;
    private int acceptButtonHeight;

    private int youtubeButtonX;
    private int telegramButtonX;
    private int socialButtonsY;

    public SeedSelectionScreen() {
        super(Text.literal("ChallengeMod"));
    }

    @Override
    protected void init() {
        int panelWidth = GuiPanelRenderer.START_PANEL_WIDTH;
        int panelHeight = GuiPanelRenderer.START_PANEL_HEIGHT;
        panelX = (width - panelWidth) / 2;
        panelY = (height - panelHeight) / 2;

        int headerTotalWidth = (HEADER_BLOCK_WIDTH * 2) + HEADER_BLOCK_GAP;
        int headerStartX = panelX + (panelWidth - headerTotalWidth) / 2;

        seedBlockWidth = HEADER_BLOCK_WIDTH;
        countBlockWidth = HEADER_BLOCK_WIDTH;

        seedBlockX = headerStartX;
        seedBlockY = panelY + HEADER_TOP_Y;
        countBlockX = seedBlockX + HEADER_BLOCK_WIDTH + HEADER_BLOCK_GAP;
        countBlockY = seedBlockY;

        seedFieldHeight = GuiPanelRenderer.START_INPUT_HEIGHT;
        countFieldHeight = GuiPanelRenderer.START_INPUT_HEIGHT;
        seedFieldWidth = GuiPanelRenderer.START_INPUT_WIDTH;
        countFieldWidth = GuiPanelRenderer.START_INPUT_WIDTH;
        seedFieldX = seedBlockX + (seedBlockWidth - seedFieldWidth) / 2;
        countFieldX = countBlockX + (countBlockWidth - countFieldWidth) / 2;
        seedFieldY = seedBlockY + 15;
        countFieldY = countBlockY + 15;

        selectorWidth = GuiPanelRenderer.START_SELECTOR_WIDTH;
        selectorHeight = GuiPanelRenderer.START_SELECTOR_HEIGHT;
        selectorX = panelX + (panelWidth - selectorWidth) / 2;
        selectorY = panelY + SELECTOR_TOP_Y;

        acceptButtonWidth = GuiPanelRenderer.START_BUTTON_WIDTH;
        acceptButtonHeight = GuiPanelRenderer.START_BUTTON_HEIGHT;
        acceptButtonX = panelX + (panelWidth - acceptButtonWidth) / 2;
        acceptButtonY = selectorY + selectorHeight + 16;

        int socialRightEdge = panelX + panelWidth - LINK_BUTTON_RIGHT_PADDING;
        telegramButtonX = socialRightEdge - LINK_BUTTON_SIZE;
        youtubeButtonX = telegramButtonX - LINK_BUTTON_GAP - LINK_BUTTON_SIZE;
        socialButtonsY = acceptButtonY + (acceptButtonHeight - LINK_BUTTON_SIZE) / 2;

        seedCursorPos = seedInput.length();
        challengeCountCursorPos = challengeCountInput.length();
        seedFocused = false;
        challengeCountFocused = false;
        blinkTicks = 0;
    }

    @Override
    public void tick() {
        blinkTicks++;
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        context.fill(0, 0, width, height, 0xA0000000);
        GuiPanelRenderer.drawStartPanel(context, panelX, panelY);

        TextRenderer tr = textRenderer;

        drawHeaderBlock(
                context,
                tr,
                Text.translatable("screen.lumenechallenge.seed.seed_label"),
                seedBlockX,
                seedBlockY,
                seedBlockWidth,
                seedFieldX,
                seedFieldY,
                seedFieldWidth,
                seedFieldHeight,
                seedInput,
                seedCursorPos,
                seedFocused,
                mouseX,
                mouseY
        );

        drawHeaderBlock(
                context,
                tr,
                Text.translatable("screen.lumenechallenge.seed.challenges_label"),
                countBlockX,
                countBlockY,
                countBlockWidth,
                countFieldX,
                countFieldY,
                countFieldWidth,
                countFieldHeight,
                challengeCountInput,
                challengeCountCursorPos,
                challengeCountFocused,
                mouseX,
                mouseY
        );

        int modifierIconXValue = modifierIconX();
        int modifierIconYValue = modifierIconY();
        ModifierType modifier = selectedModifier();
        context.drawTexture(
                net.minecraft.client.render.RenderLayer::getGuiTextured,
                modifier.icon(),
                modifierIconXValue, modifierIconYValue, 0, 0,
                MENU_MODIFIER_ICON_SIZE, MENU_MODIFIER_ICON_SIZE,
                MENU_MODIFIER_ICON_SIZE, MENU_MODIFIER_ICON_SIZE
        );

        boolean leftArrowHovered = isInside(mouseX, mouseY,
                selectorX + 8,
                selectorY + (selectorHeight - GuiPanelRenderer.START_ARROW_SIZE) / 2,
                GuiPanelRenderer.START_ARROW_SIZE,
                GuiPanelRenderer.START_ARROW_SIZE);
        boolean rightArrowHovered = isInside(mouseX, mouseY,
                selectorX + selectorWidth - 8 - GuiPanelRenderer.START_ARROW_SIZE,
                selectorY + (selectorHeight - GuiPanelRenderer.START_ARROW_SIZE) / 2,
                GuiPanelRenderer.START_ARROW_SIZE,
                GuiPanelRenderer.START_ARROW_SIZE);

        GuiPanelRenderer.drawStartSelector(context, selectorX, selectorY);
        int arrowY = selectorY + 7;
        GuiPanelRenderer.drawStartArrowLeft(context, selectorX + 8, arrowY, leftArrowHovered);
        GuiPanelRenderer.drawStartArrowRight(context, selectorX + selectorWidth - 8 - GuiPanelRenderer.START_ARROW_SIZE, arrowY, rightArrowHovered);

        drawCenteredMultiline(
                context,
                tr,
                Text.translatable(modifier.translationKey()),
                selectorX + SELECTOR_TEXT_LEFT,
                selectorY + SELECTOR_TEXT_TOP,
                selectorWidth - SELECTOR_TEXT_LEFT - SELECTOR_TEXT_RIGHT,
                selectorHeight - SELECTOR_TEXT_TOP - SELECTOR_TEXT_BOTTOM,
                0xFFFFFF
        );

        boolean buttonHovered = isInside(mouseX, mouseY, acceptButtonX, acceptButtonY, acceptButtonWidth, acceptButtonHeight);
        GuiPanelRenderer.drawStartButton(context, acceptButtonX, acceptButtonY, buttonHovered);
        context.drawCenteredTextWithShadow(
                tr,
                Text.translatable("screen.lumenechallenge.seed.accept"),
                acceptButtonX + acceptButtonWidth / 2,
                acceptButtonY + 8,
                0xFFFFFF
        );

        boolean youtubeHovered = isInside(mouseX, mouseY, youtubeButtonX, socialButtonsY, LINK_BUTTON_SIZE, LINK_BUTTON_SIZE);
        boolean telegramHovered = isInside(mouseX, mouseY, telegramButtonX, socialButtonsY, LINK_BUTTON_SIZE, LINK_BUTTON_SIZE);
        GuiPanelRenderer.drawStartYoutubeButton(context, youtubeButtonX, socialButtonsY, youtubeHovered);
        GuiPanelRenderer.drawStartTelegramButton(context, telegramButtonX, socialButtonsY, telegramHovered);

        if (isModifierHovered(mouseX, mouseY)) {
            drawModifierTooltip(context, tr, mouseX, mouseY);
        }
    }

    private void drawHeaderBlock(
            DrawContext context,
            TextRenderer tr,
            Text title,
            int blockX,
            int blockY,
            int blockWidth,
            int fieldX,
            int fieldY,
            int fieldWidth,
            int fieldHeight,
            String value,
            int cursorPos,
            boolean focused,
            int mouseX,
            int mouseY
    ) {
        context.drawCenteredTextWithShadow(tr, title, blockX + blockWidth / 2, blockY + 3, 0xFFFFFF);

        boolean hovered = isInside(mouseX, mouseY, fieldX, fieldY, fieldWidth, fieldHeight);
        GuiPanelRenderer.drawStartField(context, fieldX, fieldY, fieldWidth, fieldHeight, focused || hovered);
        drawFieldText(context, tr, value, cursorPos, fieldX, fieldY, fieldWidth, focused);
    }

    private void drawFieldText(DrawContext context, TextRenderer tr, String value, int cursorPos, int fieldX, int fieldY, int fieldWidth, boolean focused) {
        int maxWidth = fieldWidth - (FIELD_PADDING_X * 2) - 4;
        int textX = fieldX + FIELD_PADDING_X;
        int textY = fieldY + FIELD_TEXT_TOP;
        int safeCursor = Math.max(0, Math.min(cursorPos, value.length()));

        int startIndex = 0;
        if (focused && tr.getWidth(value) > maxWidth) {
            startIndex = visibleTextStartIndex(tr, value, safeCursor, maxWidth);
        }

        int endIndex = value.length();
        while (endIndex > startIndex && tr.getWidth(value.substring(startIndex, endIndex)) > maxWidth) {
            endIndex--;
        }

        String visible = value.substring(startIndex, endIndex);
        if (!focused && tr.getWidth(visible) > maxWidth) {
            visible = tr.trimToWidth(visible, maxWidth);
        }

        context.drawTextWithShadow(tr, Text.literal(visible), textX, textY, 0xFFFFFF);

        if (focused && (blinkTicks / 6) % 2 == 0) {
            String beforeCursor = value.substring(startIndex, safeCursor);
            int cursorX = textX + tr.getWidth(beforeCursor);
            context.fill(cursorX, textY - 1, cursorX + 1, textY + tr.fontHeight + 1, 0xFFFFFFFF);
        }
    }

    private static int visibleTextStartIndex(TextRenderer tr, String value, int cursorPos, int maxWidth) {
        int start = cursorPos;
        int end = cursorPos;

        while (start > 0 && tr.getWidth(value.substring(start - 1, end)) <= maxWidth) {
            start--;
        }

        while (end < value.length() && tr.getWidth(value.substring(start, end + 1)) <= maxWidth) {
            end++;
        }

        return start;
    }

    private void drawModifierTooltip(DrawContext context, TextRenderer tr, int mouseX, int mouseY) {
        ModifierType modifier = selectedModifier();
        Text tooltip = Text.translatable(modifier.tooltipKey());
        List<OrderedText> lines = wrapTooltip(tr, tooltip, TOOLTIP_WIDTH - 20);
        int lineHeight = tr.fontHeight + 1;
        int textHeight = lines.size() * lineHeight - 1;

        int x = Math.min(this.width - TOOLTIP_WIDTH - 8, Math.max(8, mouseX + 12));
        int y = mouseY + 12;
        if (y + TOOLTIP_HEIGHT > this.height - 8) {
            y = selectorY - TOOLTIP_HEIGHT - 8;
        }
        y = Math.min(this.height - TOOLTIP_HEIGHT - 8, Math.max(8, y));

        GuiPanelRenderer.drawStartTooltip(context, x, y);
        int textTop = y + Math.max(0, (TOOLTIP_HEIGHT - textHeight) / 2);
        for (int i = 0; i < lines.size(); i++) {
            OrderedText line = lines.get(i);
            context.drawCenteredTextWithShadow(tr, line, x + TOOLTIP_WIDTH / 2, textTop + i * lineHeight, 0xFFFFFF);
        }
    }

    private static List<OrderedText> wrapTooltip(TextRenderer tr, Text text, int maxWidth) {
        return tr.wrapLines(text, maxWidth);
    }

    private void drawCenteredMultiline(DrawContext context, TextRenderer tr, Text text, int x, int y, int width, int height, int color) {
        String[] lines = text.getString().split("\\R", -1);
        int lineHeight = tr.fontHeight + 1;
        int totalHeight = lines.length * lineHeight - 1;
        int startY = y + Math.max(0, (height - totalHeight) / 2);
        for (int i = 0; i < lines.length; i++) {
            String line = tr.trimToWidth(lines[i], width);
            context.drawCenteredTextWithShadow(tr, Text.literal(line), x + width / 2, startY + i * lineHeight, color);
        }
    }

    private void submitSeed() {
        if (client != null && client.getNetworkHandler() != null) {
            ClientPlayNetworking.send(new SelectSeedPayload(seedInput.trim(), selectedModifier().name(), parseChallengeCount()));
            client.setScreen(null);
        }
    }

    private int parseChallengeCount() {
        try {
            int value = Integer.parseInt(challengeCountInput.trim());
            return Math.max(MIN_CHALLENGE_COUNT, Math.min(MAX_CHALLENGE_COUNT, value));
        } catch (NumberFormatException ignored) {
            return DEFAULT_CHALLENGE_COUNT;
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) {
            return super.mouseClicked(mouseX, mouseY, button);
        }

        if (isInside(mouseX, mouseY, seedFieldX, seedFieldY, seedFieldWidth, seedFieldHeight)) {
            seedFocused = true;
            challengeCountFocused = false;
            seedCursorPos = seedInput.length();
            return true;
        }

        if (isInside(mouseX, mouseY, countFieldX, countFieldY, countFieldWidth, countFieldHeight)) {
            seedFocused = false;
            challengeCountFocused = true;
            challengeCountCursorPos = challengeCountInput.length();
            return true;
        }

        seedFocused = false;
        challengeCountFocused = false;

        int arrowY = selectorY + 7;
        int leftArrowX = selectorX + 8;
        int rightArrowX = selectorX + selectorWidth - 8 - GuiPanelRenderer.START_ARROW_SIZE;

        if (isInside(mouseX, mouseY, leftArrowX, arrowY, GuiPanelRenderer.START_ARROW_SIZE, GuiPanelRenderer.START_ARROW_SIZE)) {
            cycleModifier(-1);
            return true;
        }

        if (isInside(mouseX, mouseY, rightArrowX, arrowY, GuiPanelRenderer.START_ARROW_SIZE, GuiPanelRenderer.START_ARROW_SIZE)) {
            cycleModifier(1);
            return true;
        }

        if (isInside(mouseX, mouseY, acceptButtonX, acceptButtonY, acceptButtonWidth, acceptButtonHeight)) {
            submitSeed();
            return true;
        }

        if (isInside(mouseX, mouseY, youtubeButtonX, socialButtonsY, LINK_BUTTON_SIZE, LINK_BUTTON_SIZE)) {
            openLink(YOUTUBE_URI);
            return true;
        }

        if (isInside(mouseX, mouseY, telegramButtonX, socialButtonsY, LINK_BUTTON_SIZE, LINK_BUTTON_SIZE)) {
            openLink(TELEGRAM_URI);
            return true;
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void openLink(URI uri) {
        try {
            Util.getOperatingSystem().open(uri);
        } catch (IllegalArgumentException | IllegalStateException ex) {
            // ignore opening failures
        }
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        if (seedFocused) {
            if (Character.isISOControl(chr) || seedInput.length() >= 64) {
                return super.charTyped(chr, modifiers);
            }
            seedInput = seedInput.substring(0, seedCursorPos) + chr + seedInput.substring(seedCursorPos);
            seedCursorPos++;
            return true;
        }

        if (challengeCountFocused) {
            if (!Character.isDigit(chr) || challengeCountInput.length() >= 2) {
                return super.charTyped(chr, modifiers);
            }
            challengeCountInput = challengeCountInput.substring(0, challengeCountCursorPos) + chr + challengeCountInput.substring(challengeCountCursorPos);
            challengeCountCursorPos++;
            return true;
        }

        return super.charTyped(chr, modifiers);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (seedFocused && keyCode == GLFW.GLFW_KEY_V && (modifiers & GLFW.GLFW_MOD_CONTROL) != 0) {
            String clip = net.minecraft.client.MinecraftClient.getInstance().keyboard.getClipboard();
            if (clip != null) { seedInput = clip; seedCursorPos = seedInput.length(); }
            return true;
        }
        if (seedFocused) {
            switch (keyCode) {
                case GLFW.GLFW_KEY_BACKSPACE -> {
                    if (seedCursorPos > 0 && !seedInput.isEmpty()) {
                        seedInput = seedInput.substring(0, seedCursorPos - 1) + seedInput.substring(seedCursorPos);
                        seedCursorPos--;
                    }
                    return true;
                }
                case GLFW.GLFW_KEY_DELETE -> {
                    if (seedCursorPos < seedInput.length()) {
                        seedInput = seedInput.substring(0, seedCursorPos) + seedInput.substring(seedCursorPos + 1);
                    }
                    return true;
                }
                case GLFW.GLFW_KEY_LEFT -> {
                    seedCursorPos = Math.max(0, seedCursorPos - 1);
                    return true;
                }
                case GLFW.GLFW_KEY_RIGHT -> {
                    seedCursorPos = Math.min(seedInput.length(), seedCursorPos + 1);
                    return true;
                }
                case GLFW.GLFW_KEY_HOME -> {
                    seedCursorPos = 0;
                    return true;
                }
                case GLFW.GLFW_KEY_END -> {
                    seedCursorPos = seedInput.length();
                    return true;
                }
                case GLFW.GLFW_KEY_TAB -> {
                    seedFocused = false;
                    challengeCountFocused = true;
                    challengeCountCursorPos = challengeCountInput.length();
                    return true;
                }
                case GLFW.GLFW_KEY_ENTER, GLFW.GLFW_KEY_KP_ENTER -> {
                    submitSeed();
                    return true;
                }
                default -> {
                    return super.keyPressed(keyCode, scanCode, modifiers);
                }
            }
        }

        if (challengeCountFocused) {
            switch (keyCode) {
                case GLFW.GLFW_KEY_BACKSPACE -> {
                    if (challengeCountCursorPos > 0 && !challengeCountInput.isEmpty()) {
                        challengeCountInput = challengeCountInput.substring(0, challengeCountCursorPos - 1) + challengeCountInput.substring(challengeCountCursorPos);
                        challengeCountCursorPos--;
                    }
                    return true;
                }
                case GLFW.GLFW_KEY_DELETE -> {
                    if (challengeCountCursorPos < challengeCountInput.length()) {
                        challengeCountInput = challengeCountInput.substring(0, challengeCountCursorPos) + challengeCountInput.substring(challengeCountCursorPos + 1);
                    }
                    return true;
                }
                case GLFW.GLFW_KEY_LEFT -> {
                    challengeCountCursorPos = Math.max(0, challengeCountCursorPos - 1);
                    return true;
                }
                case GLFW.GLFW_KEY_RIGHT -> {
                    challengeCountCursorPos = Math.min(challengeCountInput.length(), challengeCountCursorPos + 1);
                    return true;
                }
                case GLFW.GLFW_KEY_HOME -> {
                    challengeCountCursorPos = 0;
                    return true;
                }
                case GLFW.GLFW_KEY_END -> {
                    challengeCountCursorPos = challengeCountInput.length();
                    return true;
                }
                case GLFW.GLFW_KEY_TAB -> {
                    challengeCountFocused = false;
                    seedFocused = true;
                    seedCursorPos = seedInput.length();
                    return true;
                }
                case GLFW.GLFW_KEY_ENTER, GLFW.GLFW_KEY_KP_ENTER -> {
                    submitSeed();
                    return true;
                }
                default -> {
                    return super.keyPressed(keyCode, scanCode, modifiers);
                }
            }
        }

        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private void cycleModifier(int delta) {
        if (MENU_MODIFIERS.isEmpty()) {
            return;
        }
        selectedModifierIndex = Math.floorMod(selectedModifierIndex + delta, MENU_MODIFIERS.size());
    }

    private ModifierType selectedModifier() {
        if (MENU_MODIFIERS.isEmpty()) {
            return ModifierType.NONE;
        }
        return MENU_MODIFIERS.get(Math.floorMod(selectedModifierIndex, MENU_MODIFIERS.size()));
    }

    private boolean isModifierHovered(int mouseX, int mouseY) {
        if (isInside(mouseX, mouseY, modifierIconX(), modifierIconY(), MENU_MODIFIER_ICON_SIZE, MENU_MODIFIER_ICON_SIZE)) {
            return true;
        }

        int textX = selectorX + 28;
        int textY = selectorY + 4;
        int textWidth = selectorWidth - 56;
        int textHeight = selectorHeight - 8;
        return isInside(mouseX, mouseY, textX, textY, textWidth, textHeight);
    }

    private int modifierIconX() {
        return selectorX + (selectorWidth - MENU_MODIFIER_ICON_SIZE) / 2;
    }

    private int modifierIconY() {
        return selectorY - MENU_MODIFIER_ICON_SIZE - 12;
    }

    private boolean isInside(double mouseX, double mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }
}
