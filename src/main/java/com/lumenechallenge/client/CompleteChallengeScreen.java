package com.lumenechallenge.client;

import com.lumenechallenge.challenge.ChallengeState;
import com.lumenechallenge.challenge.ModifierType;
import com.lumenechallenge.util.TimeUtil;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

public class CompleteChallengeScreen extends Screen {
    private static final int PANEL_WIDTH = GuiPanelRenderer.COMPLETE_PANEL_WIDTH;
    private static final int PANEL_HEIGHT = GuiPanelRenderer.COMPLETE_PANEL_HEIGHT;

    private static final int TITLE_BLOCK_HEIGHT = 30;
    private static final int INFO_BLOCK_HEIGHT = 89;
    private static final int BUTTON_BLOCK_HEIGHT = 24;
    private static final int BLOCK_GAP = 15;
    private static final int BUTTON_WIDTH = GuiPanelRenderer.START_BUTTON_WIDTH;
    private static final int BUTTON_HEIGHT = GuiPanelRenderer.START_BUTTON_HEIGHT;

    private final ChallengeState state;
    private int panelX;
    private int panelY;
    private int buttonX;
    private int buttonY;
    private int seedLineY;

    public CompleteChallengeScreen(ChallengeState state) {
        super(Text.translatable("screen.lumenechallenge.complete.title"));
        this.state = state;
    }

    @Override
    protected void init() {
        panelX = (width - PANEL_WIDTH) / 2;
        panelY = (height - PANEL_HEIGHT) / 2;
        buttonX = panelX + (PANEL_WIDTH - BUTTON_WIDTH) / 2;
        buttonY = panelY + TITLE_BLOCK_HEIGHT + BLOCK_GAP + INFO_BLOCK_HEIGHT + BLOCK_GAP;
    }

    @Override
    public boolean shouldPause() {
        return true;
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        context.fill(0, 0, width, height, 0xA0000000);
        GuiPanelRenderer.drawCompletePanel(context, panelX, panelY);

        drawCenteredText(context, Text.translatable("screen.lumenechallenge.complete.title"),
                panelY, TITLE_BLOCK_HEIGHT, 0xFFFFFF);

        drawInfoBlock(context);

        boolean hovered = isInside(mouseX, mouseY, buttonX, buttonY, BUTTON_WIDTH, BUTTON_HEIGHT);
        GuiPanelRenderer.drawStartButton(context, buttonX, buttonY, hovered);
        context.drawCenteredTextWithShadow(textRenderer,
                Text.translatable("screen.lumenechallenge.complete.button"),
                buttonX + BUTTON_WIDTH / 2,
                buttonY + 8,
                0xFFFFFF);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && isInside(mouseX, mouseY, panelX, seedLineY, PANEL_WIDTH, 14)) {
            net.minecraft.client.MinecraftClient.getInstance().keyboard.setClipboard(displaySeedText());
            return true;
        }
        if (button == 0 && isInside(mouseX, mouseY, buttonX, buttonY, BUTTON_WIDTH, BUTTON_HEIGHT)) {
            close();
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
            close();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private void drawInfoBlock(DrawContext context) {
        int infoTop = panelY + TITLE_BLOCK_HEIGHT + BLOCK_GAP;
        int centerX = panelX + PANEL_WIDTH / 2;
        int lineHeight = textRenderer.fontHeight + 1;
        int lineGap = 8;

        Text timeText = Text.translatable("screen.lumenechallenge.complete.time", TimeUtil.formatTicks(state.finishTime() - state.startTime()));
        Text seedText = Text.translatable("screen.lumenechallenge.complete.seed", displaySeedText());
        Text modifierText = state.modifierEnabled() && state.modifier().isVisible()
                ? Text.translatable("screen.lumenechallenge.complete.modifier", Text.translatable(state.modifier().translationKey()))
                : Text.literal("Модификатор: нет");

        int groupHeight = (lineHeight * 3) + (lineGap * 2);
        int firstLineY = infoTop + Math.max(0, (INFO_BLOCK_HEIGHT - groupHeight) / 2);
        context.drawCenteredTextWithShadow(textRenderer, timeText, centerX, firstLineY, 0xFFFFFF);
        context.drawCenteredTextWithShadow(textRenderer, modifierText, centerX, firstLineY + lineHeight + lineGap, 0xFFFFFF);
        seedLineY = firstLineY + (lineHeight + lineGap) * 2;
        context.drawCenteredTextWithShadow(textRenderer, seedText, centerX, seedLineY, 0xFFFFFF);
    }

    private void drawCenteredText(DrawContext context, Text text, int topY, int blockHeight, int color) {
        context.drawCenteredTextWithShadow(textRenderer, text, panelX + PANEL_WIDTH / 2, topY + (blockHeight - textRenderer.fontHeight) / 2, color);
    }

    private boolean isInside(double mouseX, double mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }
    private String displaySeedText() {
        return state.challengeSeedText() == null || state.challengeSeedText().isBlank()
                ? Long.toString(state.challengeSeed())
                : state.challengeSeedText();
    }

}
