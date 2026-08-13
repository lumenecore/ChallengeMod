package com.lumenechallenge.client.layout;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.lumenechallenge.client.GuiPanelRenderer;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public final class HudLayoutConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH = FabricLoader.getInstance().getConfigDir().resolve("lumenechallenge-layout.json");

    private static HudLayoutConfig instance;

    public PanelPlacement objectivePanel = new PanelPlacement(0.0f, 0.0f);
    public PanelPlacement timerPanel = new PanelPlacement(1.0f, 0.0f);
    public PanelPlacement rerollPanel = new PanelPlacement(1.0f, 0.0f);
    public PanelPlacement modifierPanel = new PanelPlacement(1.0f, 0.14f);
    public PanelPlacement notificationPanel = new PanelPlacement(1.0f, 1.0f);
    public PanelPlacement seedPanel = new PanelPlacement(0.0f, 1.0f);
    public PanelPlacement completePanel = new PanelPlacement(0.5f, 0.5f);
    public PanelPlacement rerollConfirmPanel = new PanelPlacement(0.5f, 0.5f);

    public static final int SEED_PANEL_WIDTH = 160;
    public static final int SEED_PANEL_HEIGHT = 9;

    public static HudLayoutConfig get(int screenWidth, int screenHeight) {
        if (instance == null) {
            instance = loadInternal(screenWidth, screenHeight);
        }
        return instance;
    }

    public static void reload(int screenWidth, int screenHeight) {
        instance = loadInternal(screenWidth, screenHeight);
    }

    public static void save() {
        if (instance == null) {
            return;
        }
        try {
            Path parent = CONFIG_PATH.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Files.writeString(CONFIG_PATH, GSON.toJson(instance), StandardCharsets.UTF_8);
        } catch (IOException ignored) {
        }
    }

    private static HudLayoutConfig loadInternal(int screenWidth, int screenHeight) {
        HudLayoutConfig config = null;
        if (Files.isRegularFile(CONFIG_PATH)) {
            try {
                String json = Files.readString(CONFIG_PATH, StandardCharsets.UTF_8);
                config = GSON.fromJson(json, HudLayoutConfig.class);
            } catch (IOException ignored) {
                config = null;
            }
        }

        if (config == null) {
            config = new HudLayoutConfig();
            config.applyDefaults(screenWidth, screenHeight);
            saveToDisk(config);
            return config;
        }

        config.normalize();
        if (screenWidth > 0 && screenHeight > 0) {
            config.applyMissingDefaults(screenWidth, screenHeight);
        }
        return config;
    }

    private static void saveToDisk(HudLayoutConfig config) {
        try {
            Path parent = CONFIG_PATH.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Files.writeString(CONFIG_PATH, GSON.toJson(config), StandardCharsets.UTF_8);
        } catch (IOException ignored) {
        }
    }

    private void applyDefaults(int screenWidth, int screenHeight) {
        objectivePanel.setPixels(8, 8, screenWidth, screenHeight, GuiPanelRenderer.OBJECTIVE_PANEL_WIDTH, GuiPanelRenderer.OBJECTIVE_PANEL_HEIGHT);
        timerPanel.setPixels(Math.max(0, screenWidth - GuiPanelRenderer.TIMER_PANEL_WIDTH - 8), 8, screenWidth, screenHeight, GuiPanelRenderer.TIMER_PANEL_WIDTH, GuiPanelRenderer.TIMER_PANEL_HEIGHT);
        rerollPanel.setPixels(
                Math.max(0, screenWidth - GuiPanelRenderer.TIMER_PANEL_WIDTH - GuiPanelRenderer.COMPLETE_REROLL_PANEL_WIDTH - 12),
                8,
                screenWidth,
                screenHeight,
                GuiPanelRenderer.COMPLETE_REROLL_PANEL_WIDTH,
                GuiPanelRenderer.COMPLETE_REROLL_PANEL_HEIGHT
        );
        modifierPanel.setPixels(
                Math.max(0, screenWidth - GuiPanelRenderer.MODIFIER_ICON_SIZE - 8),
                8 + GuiPanelRenderer.TIMER_PANEL_HEIGHT + 4,
                screenWidth,
                screenHeight,
                GuiPanelRenderer.MODIFIER_ICON_SIZE,
                GuiPanelRenderer.MODIFIER_ICON_SIZE
        );
        notificationPanel.setPixels(
                Math.max(0, screenWidth - 100 - 8),
                Math.max(0, screenHeight - 20 - 8),
                screenWidth,
                screenHeight,
                100,
                20
        );
        seedPanel.setPixels(
                0,
                Math.max(0, screenHeight - SEED_PANEL_HEIGHT),
                screenWidth,
                screenHeight,
                SEED_PANEL_WIDTH,
                SEED_PANEL_HEIGHT
        );
        completePanel.setPixels(
                Math.max(0, (screenWidth - GuiPanelRenderer.COMPLETE_PANEL_WIDTH) / 2),
                Math.max(0, (screenHeight - GuiPanelRenderer.COMPLETE_PANEL_HEIGHT) / 2),
                screenWidth,
                screenHeight,
                GuiPanelRenderer.COMPLETE_PANEL_WIDTH,
                GuiPanelRenderer.COMPLETE_PANEL_HEIGHT
        );
        rerollConfirmPanel.setPixels(
                Math.max(0, (screenWidth - GuiPanelRenderer.REROLL_PANEL_WIDTH) / 2),
                Math.max(0, (screenHeight - GuiPanelRenderer.REROLL_PANEL_HEIGHT) / 2),
                screenWidth,
                screenHeight,
                GuiPanelRenderer.REROLL_PANEL_WIDTH,
                GuiPanelRenderer.REROLL_PANEL_HEIGHT
        );
        normalize();
    }

    private void applyMissingDefaults(int screenWidth, int screenHeight) {
        if (objectivePanel == null) {
            objectivePanel = new PanelPlacement();
            objectivePanel.setPixels(8, 8, screenWidth, screenHeight, GuiPanelRenderer.OBJECTIVE_PANEL_WIDTH, GuiPanelRenderer.OBJECTIVE_PANEL_HEIGHT);
        }
        if (timerPanel == null) {
            timerPanel = new PanelPlacement();
            timerPanel.setPixels(Math.max(0, screenWidth - GuiPanelRenderer.TIMER_PANEL_WIDTH - 8), 8, screenWidth, screenHeight, GuiPanelRenderer.TIMER_PANEL_WIDTH, GuiPanelRenderer.TIMER_PANEL_HEIGHT);
        }
        if (rerollPanel == null) {
            rerollPanel = new PanelPlacement();
            rerollPanel.setPixels(
                    Math.max(0, screenWidth - GuiPanelRenderer.TIMER_PANEL_WIDTH - GuiPanelRenderer.COMPLETE_REROLL_PANEL_WIDTH - 12),
                    8,
                    screenWidth,
                    screenHeight,
                    GuiPanelRenderer.COMPLETE_REROLL_PANEL_WIDTH,
                    GuiPanelRenderer.COMPLETE_REROLL_PANEL_HEIGHT
            );
        }
        if (modifierPanel == null) {
            modifierPanel = new PanelPlacement();
            modifierPanel.setPixels(
                    Math.max(0, screenWidth - GuiPanelRenderer.MODIFIER_ICON_SIZE - 8),
                    8 + GuiPanelRenderer.TIMER_PANEL_HEIGHT + 4,
                    screenWidth,
                    screenHeight,
                    GuiPanelRenderer.MODIFIER_ICON_SIZE,
                    GuiPanelRenderer.MODIFIER_ICON_SIZE
            );
        }
        if (notificationPanel == null) {
            notificationPanel = new PanelPlacement();
            notificationPanel.setPixels(
                    Math.max(0, screenWidth - 100 - 8),
                    Math.max(0, screenHeight - 20 - 8),
                    screenWidth,
                    screenHeight,
                    100,
                    20
            );
        }
        if (seedPanel == null) {
            seedPanel = new PanelPlacement();
            seedPanel.setPixels(
                    0,
                    Math.max(0, screenHeight - SEED_PANEL_HEIGHT),
                    screenWidth,
                    screenHeight,
                    SEED_PANEL_WIDTH,
                    SEED_PANEL_HEIGHT
            );
        }
        if (completePanel == null) {
            completePanel = new PanelPlacement();
            completePanel.setPixels(
                    Math.max(0, (screenWidth - GuiPanelRenderer.COMPLETE_PANEL_WIDTH) / 2),
                    Math.max(0, (screenHeight - GuiPanelRenderer.COMPLETE_PANEL_HEIGHT) / 2),
                    screenWidth,
                    screenHeight,
                    GuiPanelRenderer.COMPLETE_PANEL_WIDTH,
                    GuiPanelRenderer.COMPLETE_PANEL_HEIGHT
            );
        }
        if (rerollConfirmPanel == null) {
            rerollConfirmPanel = new PanelPlacement();
            rerollConfirmPanel.setPixels(
                    Math.max(0, (screenWidth - GuiPanelRenderer.REROLL_PANEL_WIDTH) / 2),
                    Math.max(0, (screenHeight - GuiPanelRenderer.REROLL_PANEL_HEIGHT) / 2),
                    screenWidth,
                    screenHeight,
                    GuiPanelRenderer.REROLL_PANEL_WIDTH,
                    GuiPanelRenderer.REROLL_PANEL_HEIGHT
            );
        }
        normalize();
    }


    public void resetVisiblePanels(int screenWidth, int screenHeight) {
        if (objectivePanel == null) {
            objectivePanel = new PanelPlacement();
        }
        if (timerPanel == null) {
            timerPanel = new PanelPlacement();
        }
        if (rerollPanel == null) {
            rerollPanel = new PanelPlacement();
        }
        if (modifierPanel == null) {
            modifierPanel = new PanelPlacement();
        }
        if (notificationPanel == null) {
            notificationPanel = new PanelPlacement();
        }
        if (seedPanel == null) {
            seedPanel = new PanelPlacement();
        }

        objectivePanel.setPixels(8, 8, screenWidth, screenHeight, GuiPanelRenderer.OBJECTIVE_PANEL_WIDTH, GuiPanelRenderer.OBJECTIVE_PANEL_HEIGHT);
        timerPanel.setPixels(Math.max(0, screenWidth - GuiPanelRenderer.TIMER_PANEL_WIDTH - 8), 8, screenWidth, screenHeight, GuiPanelRenderer.TIMER_PANEL_WIDTH, GuiPanelRenderer.TIMER_PANEL_HEIGHT);
        rerollPanel.setPixels(
                Math.max(0, screenWidth - GuiPanelRenderer.TIMER_PANEL_WIDTH - GuiPanelRenderer.COMPLETE_REROLL_PANEL_WIDTH - 12),
                8,
                screenWidth,
                screenHeight,
                GuiPanelRenderer.COMPLETE_REROLL_PANEL_WIDTH,
                GuiPanelRenderer.COMPLETE_REROLL_PANEL_HEIGHT
        );
        modifierPanel.setPixels(
                Math.max(0, screenWidth - GuiPanelRenderer.MODIFIER_ICON_SIZE - 8),
                8 + GuiPanelRenderer.TIMER_PANEL_HEIGHT + 4,
                screenWidth,
                screenHeight,
                GuiPanelRenderer.MODIFIER_ICON_SIZE,
                GuiPanelRenderer.MODIFIER_ICON_SIZE
        );
        notificationPanel.setPixels(
                Math.max(0, screenWidth - 100 - 8),
                Math.max(0, screenHeight - 20 - 8),
                screenWidth,
                screenHeight,
                100,
                20
        );
        seedPanel.setPixels(
                0,
                Math.max(0, screenHeight - SEED_PANEL_HEIGHT),
                screenWidth,
                screenHeight,
                SEED_PANEL_WIDTH,
                SEED_PANEL_HEIGHT
        );
        save();
    }

    private void normalize() {
        if (objectivePanel != null) objectivePanel.clamp();
        if (timerPanel != null) timerPanel.clamp();
        if (rerollPanel != null) rerollPanel.clamp();
        if (modifierPanel != null) modifierPanel.clamp();
        if (notificationPanel != null) notificationPanel.clamp();
        if (seedPanel != null) seedPanel.clamp();
        if (completePanel != null) completePanel.clamp();
        if (rerollConfirmPanel != null) rerollConfirmPanel.clamp();
    }

    public PanelPlacement panel(PanelKind kind) {
        return switch (kind) {
            case OBJECTIVE -> objectivePanel;
            case TIMER -> timerPanel;
            case REROLL -> rerollPanel;
            case MODIFIER -> modifierPanel;
            case NOTIFICATION -> notificationPanel;
            case SEED -> seedPanel;
            case COMPLETE -> completePanel;
            case REROLL_CONFIRM -> rerollConfirmPanel;
        };
    }

    public enum PanelKind {
        OBJECTIVE,
        TIMER,
        REROLL,
        MODIFIER,
        NOTIFICATION,
        SEED,
        COMPLETE,
        REROLL_CONFIRM
    }

    public static final class PanelPlacement {
        public float x = 0.0f;
        public float y = 0.0f;

        public PanelPlacement() {
        }

        public PanelPlacement(float x, float y) {
            this.x = x;
            this.y = y;
        }

        public int x(int screenWidth, int panelWidth) {
            if (screenWidth <= panelWidth) {
                return 0;
            }
            return Math.round(clamp01(x) * (screenWidth - panelWidth));
        }

        public int y(int screenHeight, int panelHeight) {
            if (screenHeight <= panelHeight) {
                return 0;
            }
            return Math.round(clamp01(y) * (screenHeight - panelHeight));
        }

        public void setPixels(int pixelX, int pixelY, int screenWidth, int screenHeight, int panelWidth, int panelHeight) {
            this.x = toFactor(pixelX, screenWidth, panelWidth);
            this.y = toFactor(pixelY, screenHeight, panelHeight);
            clamp();
        }

        public void clamp() {
            this.x = clamp01(this.x);
            this.y = clamp01(this.y);
        }

        private static float toFactor(int pixelValue, int screenSize, int panelSize) {
            int range = Math.max(1, screenSize - panelSize);
            return clamp01(pixelValue / (float) range);
        }

        private static float clamp01(float value) {
            if (Float.isNaN(value) || Float.isInfinite(value)) {
                return 0.0f;
            }
            return Math.max(0.0f, Math.min(1.0f, value));
        }
    }
}
