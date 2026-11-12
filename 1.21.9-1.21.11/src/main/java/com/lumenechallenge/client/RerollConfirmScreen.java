package com.lumenechallenge.client;

import com.lumenechallenge.challenge.ChallengeState;
import com.lumenechallenge.client.layout.ModI18n;
import com.lumenechallenge.client.layout.HudLayoutConfig;
import com.lumenechallenge.network.RerollChallengePayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Click;
import net.minecraft.client.input.KeyInput;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

public class RerollConfirmScreen extends Screen {
    private static final int PANEL_WIDTH = GuiPanelRenderer.REROLL_PANEL_WIDTH;
    private static final int PANEL_HEIGHT = GuiPanelRenderer.REROLL_PANEL_HEIGHT;
    private static final int BUTTON_WIDTH = GuiPanelRenderer.REROLL_BUTTON_WIDTH;
    private static final int BUTTON_HEIGHT = GuiPanelRenderer.REROLL_BUTTON_HEIGHT;

    private static final int TEXT_BLOCK_HEIGHT = 50;
    private static final int BUTTON_BLOCK_HEIGHT = 25;
    private static final int BUTTON_LEFT_PADDING = 10;
    private static final int BUTTON_RIGHT_PADDING = 10;
    private static final int BUTTON_GAP = 10;

    private int panelX;
    private int panelY;
    private int confirmButtonX;
    private int confirmButtonY;
    private int cancelButtonX;
    private int cancelButtonY;

    public RerollConfirmScreen() {
        super(ModI18n.text("screen.lumenechallenge.reroll.title"));
    }

    @Override
    protected void init() {
        HudLayoutConfig layout = HudLayoutConfig.get(width, height);
        panelX = layout.rerollConfirmPanel.x(width, PANEL_WIDTH);
        panelY = layout.rerollConfirmPanel.y(height, PANEL_HEIGHT);

        int rowY = panelY + TEXT_BLOCK_HEIGHT;
        confirmButtonX = panelX + BUTTON_LEFT_PADDING;
        cancelButtonX = panelX + PANEL_WIDTH - BUTTON_RIGHT_PADDING - BUTTON_WIDTH;
        confirmButtonY = rowY;
        cancelButtonY = rowY;
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
        GuiPanelRenderer.drawRerollPanel(context, panelX, panelY);

        ChallengeState visibleState = ClientChallengeBridge.getVisibleState();
        int rerollsLeft = visibleState == null ? 0 : visibleState.rerollsRemaining();

        drawTwoLineBlock(context, ModI18n.text("screen.lumenechallenge.reroll.warning"), ModI18n.text("screen.lumenechallenge.reroll.remaining", rerollsLeft), panelY + 12, 30);

        boolean confirmHovered = isInside(mouseX, mouseY, confirmButtonX, confirmButtonY, BUTTON_WIDTH, BUTTON_HEIGHT);
        boolean cancelHovered = isInside(mouseX, mouseY, cancelButtonX, cancelButtonY, BUTTON_WIDTH, BUTTON_HEIGHT);

        GuiPanelRenderer.drawRerollButton(context, confirmButtonX, confirmButtonY, confirmHovered);
        GuiPanelRenderer.drawRerollCancelButton(context, cancelButtonX, cancelButtonY, cancelHovered);

        drawButtonLabel(context, ModI18n.text("screen.lumenechallenge.reroll.confirm"), confirmButtonX, confirmButtonY);
        drawButtonLabel(context, ModI18n.text("screen.lumenechallenge.reroll.close"), cancelButtonX, cancelButtonY);

        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        double mouseX = click.x();
        double mouseY = click.y();
        int button = click.button();
        if (button != 0) {
            return super.mouseClicked(click, doubled);
        }

        ChallengeState visibleState = ClientChallengeBridge.getVisibleState();
        if (visibleState != null && visibleState.rerollsRemaining() > 0
                && isInside(mouseX, mouseY, confirmButtonX, confirmButtonY, BUTTON_WIDTH, BUTTON_HEIGHT)) {
            ClientPlayNetworking.send(new RerollChallengePayload(0));
            MinecraftClient.getInstance().setScreen(null);
            return true;
        }

        if (isInside(mouseX, mouseY, cancelButtonX, cancelButtonY, BUTTON_WIDTH, BUTTON_HEIGHT)) {
            MinecraftClient.getInstance().setScreen(null);
            return true;
        }

        return super.mouseClicked(click, doubled);
    }

    @Override
    public boolean keyPressed(KeyInput input) {
        int keyCode = input.key();
        int scanCode = input.scancode();
        int modifiers = input.modifiers();
        if (keyCode == GLFW.GLFW_KEY_Y) {
            ChallengeState visibleState = ClientChallengeBridge.getVisibleState();
            if (visibleState != null && visibleState.rerollsRemaining() > 0) {
                ClientPlayNetworking.send(new RerollChallengePayload(0));
                MinecraftClient.getInstance().setScreen(null);
                return true;
            }
        }

        if (keyCode == GLFW.GLFW_KEY_N || keyCode == GLFW.GLFW_KEY_ESCAPE) {
            MinecraftClient.getInstance().setScreen(null);
            return true;
        }

        return super.keyPressed(input);
    }

    private void drawTwoLineBlock(DrawContext context, Text first, Text second, int blockTopY, int blockHeight) {
        drawTwoLineBlock(context, first, second, blockTopY, blockHeight, false);
    }

    private void drawTwoLineBlock(DrawContext context, Text first, Text second, int blockTopY, int blockHeight, boolean compact) {
        int lineHeight = textRenderer.fontHeight + 3;
        int totalHeight = (lineHeight * 2) - 3;
        int startY = blockTopY + Math.max(0, (blockHeight - totalHeight) / 2);
        int color = 0xFFFFFFFF;
        int x = panelX + PANEL_WIDTH / 2;
        context.drawCenteredTextWithShadow(textRenderer, first, x, startY + (compact ? -2 : 0), color);
        context.drawCenteredTextWithShadow(textRenderer, second, x, startY + lineHeight + (compact ? -2 : 0), color);
    }

    private void drawButtonLabel(DrawContext context, Text text, int x, int y) {
        // Strip legacy shortcut suffixes as a safeguard for older/customized language files.
        String label = text.getString().replaceFirst("(?i)\\s*\\[[yn]\\]\\s*$", "").trim();
        Text visibleLabel = Text.literal(label).setStyle(text.getStyle());
        drawCenteredScaledText(context, visibleLabel, x + BUTTON_WIDTH / 2, y + (BUTTON_HEIGHT / 2), 0.9f, 0xFFFFFFFF);
    }

    private void drawCenteredScaledText(DrawContext context, Text text, float centerX, float centerY, float scale, int color) {
        var matrices = context.getMatrices();
        matrices.pushMatrix();
        matrices.scale(scale, scale);
        context.drawCenteredTextWithShadow(textRenderer, text, Math.round(centerX / scale), Math.round((centerY - (textRenderer.fontHeight * scale / 2.0f)) / scale), color);
        matrices.popMatrix();
    }

    private boolean isInside(double mouseX, double mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }
}
