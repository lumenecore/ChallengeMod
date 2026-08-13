package com.lumenechallenge.client.layout;

import com.lumenechallenge.client.LumeneChallengeClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

public final class ModSettingsScreen extends Screen {
    private static final int TAB_WIDTH = 120;
    private static final int TAB_GAP = 5;
    private static final int ROW_START_Y = 62;
    private static final int ROW_SPACING = 28;
    private static final int BUTTON_WIDTH = 110;
    private static final int LEFT_COLUMN_RATIO = 4;

    private final Screen parent;
    private int section = 0;
    private boolean waitingForSeedKey;
    private boolean waitingForRerollKey;

    public ModSettingsScreen(Screen parent) {
        super(Text.empty());
        this.parent = parent;
        ChallengeModSettings.load();
    }

    @Override
    public Text getTitle() {
        return ModI18n.text("screen.lumenechallenge.settings.title");
    }

    @Override
    protected void init() {
        clearChildren();
        int totalWidth = TAB_WIDTH * 4 + TAB_GAP * 3;
        int startX = Math.max(8, (width - totalWidth) / 2);

        addTab(startX, "screen.lumenechallenge.settings.main", 0);
        addTab(startX + (TAB_WIDTH + TAB_GAP), "screen.lumenechallenge.settings.customization", 1);
        addTab(startX + (TAB_WIDTH + TAB_GAP) * 2, "screen.lumenechallenge.settings.hotkeys", 2);
        addTab(startX + (TAB_WIDTH + TAB_GAP) * 3, "screen.lumenechallenge.settings.storage", 3);

        int rowY = ROW_START_Y;
        int rightX = Math.round(width * 0.75f) - BUTTON_WIDTH / 2;
        int leftX = leftX();

        switch (section) {
            case 0 -> {
                addRowButton(ModI18n.text("screen.lumenechallenge.settings.enable_challenge"),
                        ModI18n.text(ChallengeModSettings.isChallengeModEnabled()
                                ? "screen.lumenechallenge.settings.yes"
                                : "screen.lumenechallenge.settings.no"), rightX, rowY,
                        b -> { ChallengeModSettings.setChallengeModEnabled(!ChallengeModSettings.isChallengeModEnabled()); init(); });
                rowY += ROW_SPACING;
                addRowButton(ModI18n.text("screen.lumenechallenge.settings.language"),
                        Text.literal(ModI18n.displayName(ChallengeModSettings.getModLanguage())), rightX, rowY,
                        b -> changeLanguage());
            }
            case 1 -> {
                addRowButton(ModI18n.text("screen.lumenechallenge.settings.customization_entry"),
                        ModI18n.text("screen.lumenechallenge.settings.go"), rightX, rowY,
                        b -> client.setScreen(new LayoutSettingsScreen(this)));
            }
            case 2 -> {
                addRowButton(ModI18n.text("key.lumenechallenge.toggle_seed"),
                        keyText("toggle_seed", waitingForSeedKey), rightX, rowY,
                        b -> { waitingForSeedKey = true; init(); });
                rowY += ROW_SPACING;
                addRowButton(ModI18n.text("key.lumenechallenge.reroll"),
                        keyText("reroll", waitingForRerollKey), rightX, rowY,
                        b -> { waitingForRerollKey = true; init(); });
            }
            case 3 -> {
                int worldCount = WorldStorageUtil.getChallengeWorldCount();
                ButtonWidget clearButton = ButtonWidget.builder(
                                ModI18n.text("screen.lumenechallenge.settings.clear_button"),
                                b -> {
                                    int count = WorldStorageUtil.getChallengeWorldCount();
                                    long currentSizeMb = WorldStorageUtil.sizeInMegabytes(WorldStorageUtil.getChallengeWorldsSize());
                                    if (count > 0) client.setScreen(new ConfirmDeleteWorldsScreen(this, count, currentSizeMb));
                                })
                        .dimensions(rightX, rowY - 4, BUTTON_WIDTH, 20).build();
                clearButton.active = worldCount > 0;
                addDrawableChild(clearButton);
                rowY += ROW_SPACING;
                addRowButton(ModI18n.text("screen.lumenechallenge.settings.auto_delete"),
                        ModI18n.text(ChallengeModSettings.isAutoDeleteCompletedWorlds()
                                ? "screen.lumenechallenge.settings.yes" : "screen.lumenechallenge.settings.no"), rightX, rowY,
                        b -> { ChallengeModSettings.setAutoDeleteCompletedWorlds(!ChallengeModSettings.isAutoDeleteCompletedWorlds()); init(); });
            }
        }
    }

    private void addRowButton(Text label, Text buttonText, int rightX, int rowY, ButtonWidget.PressAction action) {
        addDrawableChild(ButtonWidget.builder(buttonText, action)
                .dimensions(rightX, rowY - 4, BUTTON_WIDTH, 20).build());
    }

    private void changeLanguage() {
        String current = ChallengeModSettings.getModLanguage();
        java.util.List<ModI18n.Language> languages = ModI18n.languages();
        int index = 0;
        for (int i = 0; i < languages.size(); i++) {
            if (languages.get(i).code().equals(current)) { index = i; break; }
        }
        ChallengeModSettings.setModLanguage(languages.get((index + 1) % languages.size()).code());
        init();
    }

    private void addTab(int x, String translationKey, int targetSection) {
        addDrawableChild(ButtonWidget.builder(ModI18n.text(translationKey), b -> {
            section = targetSection;
            waitingForSeedKey = false;
            waitingForRerollKey = false;
            init();
        }).dimensions(x, 18, TAB_WIDTH, 20).build());
    }

    private Text keyText(String id, boolean waiting) {
        if (waiting) return Text.literal("§e> §f? §e<");
        return Text.literal(LumeneChallengeClient.getKeyName(id));
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (waitingForSeedKey) {
            LumeneChallengeClient.rebindSeedKey(keyCode, scanCode);
            waitingForSeedKey = false;
            init();
            return true;
        }
        if (waitingForRerollKey) {
            LumeneChallengeClient.rebindRerollKey(keyCode, scanCode);
            waitingForRerollKey = false;
            init();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderInGameBackground(context);
        context.fill(0, 0, width, height, 0xCC0B0B0B);
        context.drawCenteredTextWithShadow(textRenderer, getTitle(), width / 2, 6, 0xFFFFFF);
        super.render(context, mouseX, mouseY, delta);

        int rowY = ROW_START_Y;
        switch (section) {
            case 0 -> {
                drawLabelAndTooltip(context, ModI18n.text("screen.lumenechallenge.settings.enable_challenge"),
                        ModI18n.text("screen.lumenechallenge.tooltip.enable_challenge"), leftX(), rowY, mouseX, mouseY);
                rowY += ROW_SPACING;
                drawLabelAndTooltip(context, ModI18n.text("screen.lumenechallenge.settings.language"),
                        ModI18n.text("screen.lumenechallenge.tooltip.language"), leftX(), rowY, mouseX, mouseY);
            }
            case 1 -> drawLabelAndTooltip(context, ModI18n.text("screen.lumenechallenge.settings.customization_entry"),
                    ModI18n.text("screen.lumenechallenge.tooltip.customization"), leftX(), rowY, mouseX, mouseY);
            case 2 -> {
                drawLabelAndTooltip(context, ModI18n.text("key.lumenechallenge.toggle_seed"),
                        ModI18n.text("screen.lumenechallenge.tooltip.toggle_seed"), leftX(), rowY, mouseX, mouseY);
                drawLabelAndTooltip(context, ModI18n.text("key.lumenechallenge.reroll"),
                        ModI18n.text("screen.lumenechallenge.tooltip.reroll"), leftX(), rowY + ROW_SPACING, mouseX, mouseY);
            }
            case 3 -> {
                drawLabelAndTooltip(context, ModI18n.text("screen.lumenechallenge.settings.clear_button"),
                        ModI18n.text("screen.lumenechallenge.tooltip.clear_worlds"), leftX(), rowY, mouseX, mouseY);
                drawLabelAndTooltip(context, ModI18n.text("screen.lumenechallenge.settings.auto_delete"),
                        ModI18n.text("screen.lumenechallenge.tooltip.auto_delete"), leftX(), rowY + ROW_SPACING, mouseX, mouseY);
            }
        }
    }

    private void drawLabelAndTooltip(DrawContext context, Text label, Text tooltip, int x, int y, int mouseX, int mouseY) {
        context.drawTextWithShadow(textRenderer, label, x, y, 0xFFFFFF);
        int textWidth = textRenderer.getWidth(label);
        if (mouseX >= x && mouseX < x + textWidth && mouseY >= y - 2 && mouseY <= y + textRenderer.fontHeight + 2) {
            context.drawTooltip(textRenderer, tooltip, mouseX, mouseY);
        }
    }

    private int leftX() {
        return Math.round(width * 0.25f);
    }

    @Override
    public void close() {
        client.setScreen(parent);
    }
}
