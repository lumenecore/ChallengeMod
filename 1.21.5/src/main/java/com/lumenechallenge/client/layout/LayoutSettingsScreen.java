package com.lumenechallenge.client.layout;

import com.lumenechallenge.client.GuiPanelRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class LayoutSettingsScreen extends Screen {
    private static final int BUTTON_WIDTH = 96;
    private static final int BUTTON_HEIGHT = 20;
    private static final int BUTTON_GAP = 8;
    private static final int BUTTON_BOTTOM_MARGIN = 12;

    private static final String[] OBJECTIVE_PREVIEW_TASKS = {
            "Убейте корову",
            "Добудьте железо",
            "Съешьте сладкие ягоды",
            "Переплавьте золото",
            "Порыбачьте 2 раза",
            "Посадите зомби в лодку",
            "Найдите деревню",
            "Получите достижение",
            "Привяжите на поводок курицу",
            "Умрите, утонув",
            "Попадите трезубцем в жителя"
    };

    private final Screen parentScreen;

    private HudLayoutConfig config;
    private List<EditablePanel> panels;
    private EditablePanel activePanel;
    private int dragOffsetX;
    private int dragOffsetY;
    private long previewOpenedAtMs;

    public LayoutSettingsScreen(Screen parentScreen) {
        super(Text.empty());
        this.parentScreen = parentScreen;
    }

    @Override
    protected void init() {
        previewOpenedAtMs = System.currentTimeMillis();
        config = HudLayoutConfig.get(width, height);
        panels = buildPanels();
        activePanel = null;

        int totalButtonsWidth = BUTTON_WIDTH * 2 + BUTTON_GAP;
        int buttonStartX = Math.max(8, (width - totalButtonsWidth) / 2);
        int buttonY = Math.max(8, height - BUTTON_HEIGHT - BUTTON_BOTTOM_MARGIN);

        addDrawableChild(ButtonWidget.builder(ModI18n.text("screen.lumenechallenge.layout.reset"), button -> resetLayout())
                .dimensions(buttonStartX, buttonY, BUTTON_WIDTH, BUTTON_HEIGHT)
                .build());
        addDrawableChild(ButtonWidget.builder(ModI18n.text("screen.lumenechallenge.layout.save"), button -> saveAndClose())
                .dimensions(buttonStartX + BUTTON_WIDTH + BUTTON_GAP, buttonY, BUTTON_WIDTH, BUTTON_HEIGHT)
                .build());
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
        renderInGameBackground(context);
        context.fill(0, 0, width, height, 0xCC0B0B0B);

        // Only one panel can display its hover preview at a time. While dragging,
        // keep it on the dragged panel even when the cursor passes over another one.
        EditablePanel hoveredPanel = activePanel != null
                ? activePanel
                : findPanelAt(mouseX, mouseY);

        long elapsedSeconds = Math.max(0L, (System.currentTimeMillis() - previewOpenedAtMs) / 1000L);
        for (EditablePanel panel : panels) {
            panel.draw(context, panel == hoveredPanel, elapsedSeconds);
        }

        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) {
            return super.mouseClicked(mouseX, mouseY, button);
        }

        EditablePanel clickedPanel = findPanelAt(mouseX, mouseY);
        if (clickedPanel != null) {
            activePanel = clickedPanel;
            dragOffsetX = (int) Math.round(mouseX) - clickedPanel.x();
            dragOffsetY = (int) Math.round(mouseY) - clickedPanel.y();
            return true;
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (button != 0 || activePanel == null) {
            return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
        }

        // Use the normal Minecraft cursor. Panels may overlap freely; only
        // keep the dragged panel within the editor's visible screen bounds.
        int targetX = (int) Math.round(mouseX) - dragOffsetX;
        int targetY = (int) Math.round(mouseY) - dragOffsetY;
        targetX = Math.max(0, Math.min(targetX, Math.max(0, width - activePanel.width)));
        targetY = Math.max(0, Math.min(targetY, Math.max(0, height - activePanel.height)));
        activePanel.move(targetX, targetY);
        return true;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0 && activePanel != null) {
            HudLayoutConfig.save();
            activePanel = null;
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public void close() {
        HudLayoutConfig.save();
        NotificationManager.show(ModI18n.text("notification.lumenechallenge.changes_saved"));
        if (client != null) {
            client.setScreen(parentScreen);
        }
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            close();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private void resetLayout() {
        config.resetVisiblePanels(width, height);
    }

    private void saveAndClose() {
        close();
    }

    /** Returns the visually topmost panel under the cursor. */
    private EditablePanel findPanelAt(double mouseX, double mouseY) {
        for (int i = panels.size() - 1; i >= 0; i--) {
            EditablePanel panel = panels.get(i);
            if (panel.contains(mouseX, mouseY)) {
                return panel;
            }
        }
        return null;
    }

    private List<EditablePanel> buildPanels() {
        List<EditablePanel> panels = new ArrayList<>();
        panels.add(new EditablePanel(
                PreviewKind.OBJECTIVE,
                null,
                GuiPanelRenderer.OBJECTIVE_PANEL_WIDTH,
                GuiPanelRenderer.OBJECTIVE_PANEL_HEIGHT,
                config.objectivePanel,
                (context, x, y, hovered) -> GuiPanelRenderer.drawObjectivePanelPreview(context, x, y, hovered)
        ));
        panels.add(new EditablePanel(
                PreviewKind.TIMER,
                null,
                GuiPanelRenderer.TIMER_PANEL_WIDTH,
                GuiPanelRenderer.TIMER_PANEL_HEIGHT,
                config.timerPanel,
                (context, x, y, hovered) -> GuiPanelRenderer.drawTimerPanelPreview(context, x, y, hovered)
        ));
        panels.add(new EditablePanel(
                PreviewKind.REROLL,
                null,
                GuiPanelRenderer.COMPLETE_REROLL_PANEL_WIDTH,
                GuiPanelRenderer.COMPLETE_REROLL_PANEL_HEIGHT,
                config.rerollPanel,
                (context, x, y, hovered) -> GuiPanelRenderer.drawRerollPanelPreview(context, x, y, hovered)
        ));
        panels.add(new EditablePanel(
                PreviewKind.MODIFIER,
                null,
                GuiPanelRenderer.MODIFIER_ICON_SIZE,
                GuiPanelRenderer.MODIFIER_ICON_SIZE,
                config.modifierPanel,
                (context, x, y, hovered) -> GuiPanelRenderer.drawModifierPreview(context, x, y, hovered)
        ));
        panels.add(new EditablePanel(
                PreviewKind.SEED,
                null,
                HudLayoutConfig.SEED_PANEL_WIDTH,
                HudLayoutConfig.SEED_PANEL_HEIGHT,
                config.seedPanel,
                (context, x, y, hovered) -> GuiPanelRenderer.drawSeedPanelPreview(context, x, y, hovered)
        ));
        panels.add(new EditablePanel(
                PreviewKind.NOTIFICATION,
                "Оповещение",
                100,
                20,
                config.notificationPanel,
                (context, x, y, hovered) -> GuiPanelRenderer.drawNotificationPanelPreview(context, x, y, hovered)
        ));
        return panels;
    }

    private enum PreviewKind {
        OBJECTIVE,
        TIMER,
        REROLL,
        MODIFIER,
        SEED,
        NOTIFICATION
    }

    private final class EditablePanel {
        private final PreviewKind kind;
        private final String displayName;
        private final int width;
        private final int height;
        private final HudLayoutConfig.PanelPlacement placement;
        private final PanelDrawer drawer;

        private EditablePanel(PreviewKind kind, String displayName, int width, int height,
                              HudLayoutConfig.PanelPlacement placement, PanelDrawer drawer) {
            this.kind = kind;
            this.displayName = displayName;
            this.width = width;
            this.height = height;
            this.placement = placement;
            this.drawer = drawer;
        }

        private int x() {
            return placement.x(LayoutSettingsScreen.this.width, width);
        }

        private int y() {
            return placement.y(LayoutSettingsScreen.this.height, height);
        }

        private boolean contains(double mouseX, double mouseY) {
            int x = x();
            int y = y();
            return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
        }

        private void move(int x, int y) {
            placement.setPixels(
                    x,
                    y,
                    LayoutSettingsScreen.this.width,
                    LayoutSettingsScreen.this.height,
                    width,
                    height
            );
        }

        private void draw(DrawContext context, boolean hovered, long elapsedSeconds) {
            int x = x();
            int y = y();
            drawer.draw(context, x, y, hovered);

            switch (kind) {
                case OBJECTIVE -> drawObjectiveSample(context, x, y, elapsedSeconds);
                case TIMER -> drawTimerSample(context, x, y, elapsedSeconds);
                case REROLL -> drawRerollSample(context, x, y, elapsedSeconds);
                case NOTIFICATION -> {
                    int labelY = y + (height - textRenderer.fontHeight) / 2;
                    context.drawCenteredTextWithShadow(textRenderer, Text.literal(displayName), x + width / 2, labelY, 0xFFFFFFFF);
                }
                case SEED -> drawSeedSample(context, x, y);
                case MODIFIER -> {
                    // The modifier preview intentionally has no software-rendered text.
                }
            }
        }
    }

    private void drawObjectiveSample(DrawContext context, int x, int y, long elapsedSeconds) {
        String objective = OBJECTIVE_PREVIEW_TASKS[(int) (elapsedSeconds % OBJECTIVE_PREVIEW_TASKS.length)];
        drawCenteredWrappedText(
                context,
                Text.literal(objective),
                x + 5,
                y + 30,
                140,
                40
        );
        int progressPercent = (int) (elapsedSeconds % OBJECTIVE_PREVIEW_TASKS.length) * 10;
        drawCenteredWrappedText(
                context,
                Text.literal("Процесс: " + progressPercent + "%"),
                x,
                y + 75,
                GuiPanelRenderer.OBJECTIVE_PANEL_WIDTH,
                21
        );
    }

    private void drawTimerSample(DrawContext context, int x, int y, long elapsedSeconds) {
        long hours = elapsedSeconds / 3600L;
        long minutes = (elapsedSeconds % 3600L) / 60L;
        long seconds = elapsedSeconds % 60L;
        String timer = String.format(Locale.ROOT, "%02d.%02d.%02d", hours, minutes, seconds);

        int textY = y
                + (GuiPanelRenderer.TIMER_PANEL_HEIGHT - GuiPanelRenderer.TIMER_TEXT_HEIGHT) / 2
                + (GuiPanelRenderer.TIMER_TEXT_HEIGHT - textRenderer.fontHeight) / 2
                + 1;
        int centerX = x + GuiPanelRenderer.TIMER_TEXT_LEFT + GuiPanelRenderer.TIMER_TEXT_WIDTH / 2;
        context.drawCenteredTextWithShadow(textRenderer, Text.literal(timer), centerX, textY, 0xFFFFFFFF);
    }

    private void drawRerollSample(DrawContext context, int x, int y, long elapsedSeconds) {
        int rerolls = 10 - (int) (elapsedSeconds % 10L);
        int textY = y
                + (GuiPanelRenderer.COMPLETE_REROLL_PANEL_HEIGHT - GuiPanelRenderer.REROLL_TEXT_HEIGHT) / 2
                + (GuiPanelRenderer.REROLL_TEXT_HEIGHT - textRenderer.fontHeight) / 2
                + 1;
        int centerX = x + GuiPanelRenderer.REROLL_TEXT_LEFT + GuiPanelRenderer.REROLL_TEXT_WIDTH / 2 + 1;
        context.drawCenteredTextWithShadow(textRenderer, Text.literal(Integer.toString(rerolls)), centerX, textY, 0xFFFFFFFF);
    }


    private void drawSeedSample(DrawContext context, int x, int y) {
        Text seed = Text.literal("Сид: 3031071795166257310");
        int seedTextWidth = textRenderer.getWidth(seed);
        int textX = x + HudLayoutConfig.SEED_TEXT_MARGIN;
        int textY = y + HudLayoutConfig.SEED_TEXT_MARGIN
                + (HudLayoutConfig.SEED_TEXT_HEIGHT - textRenderer.fontHeight) / 2
                + 1;
        context.drawTextWithShadow(textRenderer, seed, textX, textY, 0xFFFFFFFF);
    }

    private void drawCenteredWrappedText(DrawContext context, Text text, int x, int y, int width, int height) {
        List<OrderedText> lines = textRenderer.wrapLines(text, width);
        int lineHeight = textRenderer.fontHeight + 1;
        int totalHeight = Math.max(textRenderer.fontHeight, lines.size() * lineHeight - 1);
        int startY = y + Math.max(0, (height - totalHeight) / 2);

        for (int i = 0; i < lines.size(); i++) {
            context.drawCenteredTextWithShadow(textRenderer, lines.get(i), x + width / 2, startY + i * lineHeight, 0xFFFFFFFF);
        }
    }

    @FunctionalInterface
    private interface PanelDrawer {
        void draw(DrawContext context, int x, int y, boolean hovered);
    }
}
