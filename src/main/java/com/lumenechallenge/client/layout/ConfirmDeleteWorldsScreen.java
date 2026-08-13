package com.lumenechallenge.client.layout;

import com.lumenechallenge.util.WorldMarkerUtil;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

public final class ConfirmDeleteWorldsScreen extends Screen {
    private final Screen parent;
    private final int count;
    private final long sizeMb;

    public ConfirmDeleteWorldsScreen(Screen parent, int count, long sizeMb) {
        super(Text.empty());
        this.parent = parent;
        this.count = count;
        this.sizeMb = sizeMb;
    }

    @Override
    public Text getTitle() {
        return ModI18n.text("screen.lumenechallenge.delete.title");
    }

    @Override
    protected void init() {
        int centerX = width / 2;
        int buttonWidth = 120;
        int gap = 8;
        int y = height / 2 + 24;

        addDrawableChild(ButtonWidget.builder(ModI18n.text("screen.lumenechallenge.delete.confirm"), button -> {
            WorldMarkerUtil.deleteAllMarkedWorlds(WorldStorageUtil.getSavesDirectory());
            client.setScreen(parent);
        }).dimensions(centerX - buttonWidth - gap / 2, y, buttonWidth, 20).build());

        addDrawableChild(ButtonWidget.builder(ModI18n.text("screen.lumenechallenge.delete.cancel"), button -> client.setScreen(parent))
                .dimensions(centerX + gap / 2, y, buttonWidth, 20).build());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderInGameBackground(context);
        context.fill(0, 0, width, height, 0xCC0B0B0B);
        context.drawCenteredTextWithShadow(textRenderer, getTitle(), width / 2, height / 2 - 50, 0xFFFFFF);
        context.drawCenteredTextWithShadow(
                textRenderer,
                ModI18n.text(messageKey(), count, sizeMb),
                width / 2,
                height / 2 - 18,
                0xFFFFFF
        );
        super.render(context, mouseX, mouseY, delta);
    }

    private String messageKey() {
        int mod100 = count % 100;
        int mod10 = count % 10;
        if ("ru_ru".equals(ChallengeModSettings.getModLanguage())) {
            if (mod100 >= 11 && mod100 <= 14) return "screen.lumenechallenge.delete.message.many";
            return switch (mod10) {
                case 1 -> "screen.lumenechallenge.delete.message.one";
                case 2, 3, 4 -> "screen.lumenechallenge.delete.message.few";
                default -> "screen.lumenechallenge.delete.message.many";
            };
        }
        if (count == 1) return "screen.lumenechallenge.delete.message.one";
        return "screen.lumenechallenge.delete.message.many";
    }

    @Override
    public void close() {
        client.setScreen(parent);
    }
}
