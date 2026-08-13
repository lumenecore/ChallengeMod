package com.lumenechallenge.client.layout;

import com.lumenechallenge.client.layout.ModI18n;

import com.lumenechallenge.challenge.ModifierType;
import com.lumenechallenge.client.GuiPanelRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

public class LayoutSettingsScreen extends Screen {
    private static final int TITLE_Y = 12;
    private static final int BUTTON_WIDTH = 96;
    private static final int BUTTON_HEIGHT = 20;
    private static final int BUTTON_GAP = 8;
    private static final int BUTTON_BOTTOM_MARGIN = 12;

    private final Screen parentScreen;

    private HudLayoutConfig config;
    private List<EditablePanel> panels;
    private EditablePanel activePanel;
    private int dragOffsetX;
    private int dragOffsetY;

    public LayoutSettingsScreen(Screen parentScreen) {
        super(ModI18n.text("screen.lumenechallenge.settings.customization_title"));
        this.parentScreen = parentScreen;
    }

    @Override
    protected void init() {
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

        context.drawCenteredTextWithShadow(textRenderer, ModI18n.text("screen.lumenechallenge.settings.customization_title"), width / 2, TITLE_Y, 0xFFFFFF);

        for (EditablePanel panel : panels) {
            panel.draw(context, isHovered(panel, mouseX, mouseY) || panel == activePanel);
        }

        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) {
            return super.mouseClicked(mouseX, mouseY, button);
        }

        for (EditablePanel panel : panels) {
            if (panel.contains(mouseX, mouseY)) {
                activePanel = panel;
                dragOffsetX = (int) Math.round(mouseX) - panel.x();
                dragOffsetY = (int) Math.round(mouseY) - panel.y();
                return true;
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (button != 0 || activePanel == null) {
            return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
        }

        activePanel.move((int) Math.round(mouseX) - dragOffsetX, (int) Math.round(mouseY) - dragOffsetY);
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

    private boolean isHovered(EditablePanel panel, int mouseX, int mouseY) {
        return panel.contains(mouseX, mouseY);
    }

    private List<EditablePanel> buildPanels() {
        List<EditablePanel> panels = new ArrayList<>();
        panels.add(new EditablePanel(
                "screen.lumenechallenge.layout.objective",
                GuiPanelRenderer.OBJECTIVE_PANEL_WIDTH,
                GuiPanelRenderer.OBJECTIVE_PANEL_HEIGHT,
                config.objectivePanel,
                GuiPanelRenderer::drawObjectivePanel
        ));
        panels.add(new EditablePanel(
                "screen.lumenechallenge.layout.timer",
                GuiPanelRenderer.TIMER_PANEL_WIDTH,
                GuiPanelRenderer.TIMER_PANEL_HEIGHT,
                config.timerPanel,
                GuiPanelRenderer::drawTimerPanel
        ));
        panels.add(new EditablePanel(
                "screen.lumenechallenge.layout.reroll",
                GuiPanelRenderer.COMPLETE_REROLL_PANEL_WIDTH,
                GuiPanelRenderer.COMPLETE_REROLL_PANEL_HEIGHT,
                config.rerollPanel,
                GuiPanelRenderer::drawCompleteRerollPanel
        ));
        panels.add(new EditablePanel(
                "screen.lumenechallenge.layout.modifier",
                GuiPanelRenderer.MODIFIER_ICON_SIZE,
                GuiPanelRenderer.MODIFIER_ICON_SIZE,
                config.modifierPanel,
                (context, x, y) -> {
                    context.drawTexture(
                            net.minecraft.client.render.RenderLayer::getGuiTextured,
                            ModifierType.POCKETS.icon(),
                            x, y, 0, 0,
                            GuiPanelRenderer.MODIFIER_ICON_SIZE, GuiPanelRenderer.MODIFIER_ICON_SIZE,
                            32, 32, 32, 32
                    );
                    context.drawCenteredTextWithShadow(textRenderer, ModI18n.text("screen.lumenechallenge.layout.modifier"), x + GuiPanelRenderer.MODIFIER_ICON_SIZE / 2, y + GuiPanelRenderer.MODIFIER_ICON_SIZE + 4, 0xFFFFFF);
                }
        ));
        panels.add(new EditablePanel(
                "screen.lumenechallenge.layout.seed",
                HudLayoutConfig.SEED_PANEL_WIDTH,
                HudLayoutConfig.SEED_PANEL_HEIGHT,
                config.seedPanel,
                (context, x, y) -> context.fill(x, y, x + HudLayoutConfig.SEED_PANEL_WIDTH, y + HudLayoutConfig.SEED_PANEL_HEIGHT, 0x33000000)
        ));
        panels.add(new EditablePanel(
                "screen.lumenechallenge.layout.notification",
                100,
                20,
                config.notificationPanel,
                (context, x, y) -> {
                    GuiPanelRenderer.drawNotificationPanel(context, x, y, 100, 20, 1.0f);
                    context.drawCenteredTextWithShadow(textRenderer, ModI18n.text("screen.lumenechallenge.layout.notification"), x + 50, y + 6, 0xFFFFFF);
                }
        ));
        return panels;
    }

    private final class EditablePanel {
        private final String name;
        private final int width;
        private final int height;
        private final HudLayoutConfig.PanelPlacement placement;
        private final PanelDrawer drawer;

        private EditablePanel(String name, int width, int height, HudLayoutConfig.PanelPlacement placement, PanelDrawer drawer) {
            this.name = name;
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

        private void draw(DrawContext context, boolean hovered) {
            int x = x();
            int y = y();
            drawer.draw(context, x, y);
            if (hovered) {
                context.fill(x, y, x + width, y + height, 0x22FFFFFF);
                context.fill(x - 1, y - 1, x + width + 1, y, 0xFFFFFFFF);
                context.fill(x - 1, y + height, x + width + 1, y + height + 1, 0xFFFFFFFF);
                context.fill(x - 1, y, x, y + height, 0xFFFFFFFF);
                context.fill(x + width, y, x + width + 1, y + height, 0xFFFFFFFF);
            }
            context.drawTextWithShadow(textRenderer, ModI18n.text(name), x + 4, y - 10, 0xFFFFFF);
        }
    }

    @FunctionalInterface
    private interface PanelDrawer {
        void draw(DrawContext context, int x, int y);
    }
}
