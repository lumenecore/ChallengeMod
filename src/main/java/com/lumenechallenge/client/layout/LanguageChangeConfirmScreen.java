package com.lumenechallenge.client.layout;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

public final class LanguageChangeConfirmScreen extends Screen {
    private final Screen parent;
    private final String targetLanguage;

    public LanguageChangeConfirmScreen(Screen parent, String targetLanguage) {
        super(Text.empty());
        this.parent = parent;
        this.targetLanguage = targetLanguage;
    }

    @Override
    protected void init() {
        int centerX = width / 2;
        int buttonWidth = 80;
        int gap = 8;
        int y = height / 2 + 22;
        addDrawableChild(ButtonWidget.builder(ModI18n.textInLanguage(targetLanguage, "screen.lumenechallenge.language_change.yes"), b -> {
            ChallengeModSettings.setModLanguage(ModI18n.normalizeMinecraftLanguage(targetLanguage));
            client.setScreen(parent);
        }).dimensions(centerX - buttonWidth - gap / 2, y, buttonWidth, 20).build());
        addDrawableChild(ButtonWidget.builder(ModI18n.textInLanguage(targetLanguage, "screen.lumenechallenge.language_change.no"), b -> client.setScreen(parent))
                .dimensions(centerX + gap / 2, y, buttonWidth, 20).build());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderInGameBackground(context);
        context.fill(0, 0, width, height, 0xCC0B0B0B);
        Text message = ModI18n.textInLanguage(targetLanguage, "screen.lumenechallenge.language_change.message",
                ModI18n.displayName(ModI18n.normalizeMinecraftLanguage(targetLanguage)));
        context.drawCenteredTextWithShadow(textRenderer, message, width / 2, height / 2 - 18, 0xFFFFFF);
        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public void close() {
        client.setScreen(parent);
    }
}
