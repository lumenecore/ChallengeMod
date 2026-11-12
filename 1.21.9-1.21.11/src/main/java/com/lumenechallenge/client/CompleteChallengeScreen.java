package com.lumenechallenge.client;

import com.lumenechallenge.LumeneChallengeMod;
import com.lumenechallenge.challenge.ChallengeState;
import com.lumenechallenge.challenge.RunDifficulty;
import com.lumenechallenge.client.layout.HudLayoutConfig;
import com.lumenechallenge.client.layout.ModI18n;
import com.lumenechallenge.client.layout.NotificationManager;
import com.lumenechallenge.util.TimeUtil;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.input.KeyInput;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

/** Compact run summary shown after the final challenge is completed. */
public class CompleteChallengeScreen extends Screen {
    private static final int PANEL_WIDTH = GuiPanelRenderer.COMPLETE_PANEL_WIDTH;
    private static final int PANEL_HEIGHT = GuiPanelRenderer.COMPLETE_PANEL_HEIGHT;

    // Overall panel layout: 10px outer padding on every side.
    private static final int OUTER_PADDING = 10;
    private static final int HEADER_HEIGHT = 25;
    private static final int SECTION_GAP = 5;
    private static final int BUTTON_AREA_HEIGHT = 25;

    // The stat grid is inset 14px from the 260px content area.
    private static final int STATS_SIDE_INSET = 14;
    private static final int TILE_MARGIN = 2;
    private static final int TILE_GAP = 4;
    private static final int TILE_WIDTH = 112;
    private static final int WIDE_TILE_WIDTH = TILE_WIDTH * 2 + TILE_GAP;
    private static final int TILE_HEIGHT = 46;
    private static final int TILE_PADDING = 5;
    private static final int TILE_CONTENT_WIDTH = 102;
    private static final int WIDE_TILE_CONTENT_WIDTH = WIDE_TILE_WIDTH - TILE_PADDING * 2;
    private static final int TILE_HEADER_HEIGHT = 12;
    private static final int TILE_TEXT_GAP = 4;
    private static final int TILE_VALUE_HEIGHT = 20;

    private static final int CLOSE_BUTTON_WIDTH = 100;
    private static final int CLOSE_BUTTON_HEIGHT = 16;

    private static final float HEADER_TEXT_SCALE = 1.3f;
    private static final float TILE_HEADER_TEXT_SCALE = 1.0f;
    private static final float TILE_VALUE_TEXT_SCALE = 1.0f;

    private static final Identifier CLOSE_BUTTON =
            Identifier.of(LumeneChallengeMod.MOD_ID, "textures/gui/complete_close_button.png");
    private static final Identifier CLOSE_BUTTON_HOVER =
            Identifier.of(LumeneChallengeMod.MOD_ID, "textures/gui/complete_close_button_hover.png");

    private final ChallengeState state;

    private int panelX;
    private int panelY;
    private Bounds seedValueBounds = new Bounds(0, 0, 0, 0);

    public CompleteChallengeScreen(ChallengeState state) {
        super(ModI18n.text("screen.lumenechallenge.complete.congratulations"));
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
        // The run has already ended. Keep the integrated server running behind
        // the summary so closing this screen does not trigger a pause/resume spike.
        return false;
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return true;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        context.fill(0, 0, width, height, 0xA0000000);
        GuiPanelRenderer.drawCompletePanel(context, panelX, panelY);

        drawHeader(context);
        drawStats(context, mouseX, mouseY);
        drawCloseButton(context, mouseX, mouseY);
        NotificationManager.render(context, textRenderer, width, height);
    }

    private void drawHeader(DrawContext context) {
        int x = panelX + OUTER_PADDING;
        int y = panelY + OUTER_PADDING;
        drawCenteredFittedText(
                context,
                ModI18n.text("screen.lumenechallenge.complete.congratulations"),
                x,
                y,
                PANEL_WIDTH - OUTER_PADDING * 2,
                HEADER_HEIGHT,
                HEADER_TEXT_SCALE,
                0xFFFFFFFF
        );
    }

    private void drawStats(DrawContext context, int mouseX, int mouseY) {
        int contentX = panelX + OUTER_PADDING;
        int contentY = panelY + OUTER_PADDING + HEADER_HEIGHT + SECTION_GAP;
        int gridX = contentX + STATS_SIDE_INSET + TILE_MARGIN;
        int gridY = contentY + TILE_MARGIN;
        int secondColumnX = gridX + TILE_WIDTH + TILE_GAP;
        int secondRowY = gridY + TILE_HEIGHT + TILE_GAP;

        drawStatTile(
                context,
                gridX,
                gridY,
                TILE_CONTENT_WIDTH,
                ModI18n.text("screen.lumenechallenge.complete.stat.time"),
                Text.literal(runTimeText()),
                false,
                mouseX,
                mouseY
        );
        drawStatTile(
                context,
                secondColumnX,
                gridY,
                TILE_CONTENT_WIDTH,
                ModI18n.text("screen.lumenechallenge.complete.stat.difficulty"),
                difficultyText(),
                false,
                mouseX,
                mouseY
        );
        // The bottom row is one wide seed tile spanning the space of the former two tiles.
        drawStatTile(
                context,
                gridX,
                secondRowY,
                WIDE_TILE_CONTENT_WIDTH,
                ModI18n.text("screen.lumenechallenge.complete.stat.seed"),
                Text.literal(displaySeedText()),
                true,
                mouseX,
                mouseY
        );
    }

    private void drawStatTile(
            DrawContext context,
            int tileX,
            int tileY,
            int tileContentWidth,
            Text heading,
            Text value,
            boolean seedValue,
            int mouseX,
            int mouseY
    ) {
        int textX = tileX + TILE_PADDING;
        int headingY = tileY + TILE_PADDING;
        int valueY = headingY + TILE_HEADER_HEIGHT + TILE_TEXT_GAP;

        drawCenteredFittedText(
                context,
                heading,
                textX,
                headingY,
                tileContentWidth,
                TILE_HEADER_HEIGHT,
                TILE_HEADER_TEXT_SCALE,
                0xFFFFFFFF
        );

        // Move only the seed value itself slightly upward. Its heading and tile
        // geometry stay unchanged so the visual layout remains balanced.
        int actualValueY = seedValue ? valueY - 2 : valueY;
        TextBounds valueBounds = drawCenteredFittedText(
                context,
                value,
                textX,
                actualValueY,
                tileContentWidth,
                TILE_VALUE_HEIGHT,
                TILE_VALUE_TEXT_SCALE,
                0xFFFFFFFF
        );

        if (seedValue) {
            // Hover/click bounds follow the rendered glyphs, not the full reserved
            // value area. This keeps the underline and copy action on the number only.
            seedValueBounds = new Bounds(
                    valueBounds.x(),
                    valueBounds.y(),
                    valueBounds.width(),
                    valueBounds.height()
            );
            if (seedValueBounds.contains(mouseX, mouseY)) {
                int underlineY = valueBounds.y() + valueBounds.height() + 1;
                context.fill(valueBounds.x(), underlineY, valueBounds.x() + valueBounds.width(), underlineY + 1, 0xFFFFFFFF);
            }
        }
    }

    private void drawCloseButton(DrawContext context, int mouseX, int mouseY) {
        Bounds bounds = closeButtonBounds();
        boolean hovered = bounds.contains(mouseX, mouseY);
        drawTexture(
                context,
                hovered ? CLOSE_BUTTON_HOVER : CLOSE_BUTTON,
                bounds.x(),
                bounds.y(),
                bounds.width(),
                bounds.height()
        );

        context.drawCenteredTextWithShadow(
                textRenderer,
                ModI18n.text("screen.lumenechallenge.complete.button"),
                bounds.x() + bounds.width() / 2,
                bounds.y() + (bounds.height() - textRenderer.fontHeight) / 2,
                0xFFFFFFFF
        );
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        double mouseX = click.x();
        double mouseY = click.y();
        if (click.button() != 0) {
            return super.mouseClicked(click, doubled);
        }

        if (seedValueBounds.contains(mouseX, mouseY)) {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client != null) {
                client.keyboard.setClipboard(displaySeedText());
                NotificationManager.show(ModI18n.text("notification.lumenechallenge.seed_copied"));
            }
            return true;
        }

        if (closeButtonBounds().contains(mouseX, mouseY)) {
            close();
            return true;
        }

        return super.mouseClicked(click, doubled);
    }

    @Override
    public boolean keyPressed(KeyInput input) {
        int keyCode = input.key();
        if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER || keyCode == GLFW.GLFW_KEY_ESCAPE) {
            closeOrDeleteWorld();
            return true;
        }
        return super.keyPressed(input);
    }

    private void closeOrDeleteWorld() {
        if (LumeneChallengeClient.shouldAutoDeleteCompletedWorlds()) {
            LumeneChallengeClient.finishCompletedWorldAndReturnToWorldList();
            return;
        }
        close();
    }

    private String runTimeText() {
        if (state == null) {
            return TimeUtil.formatTicks(0L);
        }
        return TimeUtil.formatTicks(Math.max(0L, state.finishTime() - state.startTime()));
    }

    private Text difficultyText() {
        RunDifficulty difficulty = state == null ? RunDifficulty.NORMAL : state.runDifficulty();
        String key = switch (difficulty) {
            case NORMAL -> "screen.lumenechallenge.complete.difficulty.normal";
            case HARD -> "screen.lumenechallenge.complete.difficulty.hard";
            case INSANE -> "screen.lumenechallenge.complete.difficulty.insane";
        };
        return ModI18n.text(key);
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

    private Bounds closeButtonBounds() {
        int areaY = panelY + PANEL_HEIGHT - OUTER_PADDING - BUTTON_AREA_HEIGHT;
        int x = panelX + (PANEL_WIDTH - CLOSE_BUTTON_WIDTH) / 2;
        int y = areaY + (BUTTON_AREA_HEIGHT - CLOSE_BUTTON_HEIGHT) / 2;
        return new Bounds(x, y, CLOSE_BUTTON_WIDTH, CLOSE_BUTTON_HEIGHT);
    }

    private TextBounds drawCenteredFittedText(
            DrawContext context,
            Text text,
            int x,
            int y,
            int boxWidth,
            int boxHeight,
            float preferredScale,
            int color
    ) {
        int naturalWidth = Math.max(1, textRenderer.getWidth(text));
        float scale = Math.min(preferredScale, (float) boxWidth / naturalWidth);
        float scaledWidth = naturalWidth * scale;
        float scaledHeight = textRenderer.fontHeight * scale;
        float drawX = x + (boxWidth - scaledWidth) / 2.0f;
        float drawY = y + (boxHeight - scaledHeight) / 2.0f;

        var matrices = context.getMatrices();
        matrices.pushMatrix();
        matrices.scale(scale, scale);
        context.drawTextWithShadow(
                textRenderer,
                text,
                Math.round(drawX / scale),
                Math.round(drawY / scale),
                color
        );
        matrices.popMatrix();

        return new TextBounds(
                Math.round(drawX),
                Math.round(drawY),
                Math.max(1, Math.round(scaledWidth)),
                Math.max(1, Math.round(scaledHeight))
        );
    }

    private void drawTexture(DrawContext context, Identifier texture, int x, int y, int drawWidth, int drawHeight) {
        GuiPanelRenderer.drawTextureIfAvailable(context, texture, x, y, drawWidth, drawHeight,
                drawWidth, drawHeight, 0xFFFFFFFF);
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

    private record TextBounds(int x, int y, int width, int height) {
    }
}
