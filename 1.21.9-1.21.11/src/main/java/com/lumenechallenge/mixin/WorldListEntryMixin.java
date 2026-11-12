package com.lumenechallenge.mixin;

import com.lumenechallenge.LumeneChallengeMod;
import com.lumenechallenge.util.WorldMarkerUtil;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.world.WorldListWidget;
import net.minecraft.client.gui.widget.Widget;
import net.minecraft.util.Identifier;
import net.minecraft.world.level.storage.LevelSummary;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(WorldListWidget.WorldEntry.class)
public abstract class WorldListEntryMixin {
    private static final Identifier MODDED = Identifier.of(LumeneChallengeMod.MOD_ID, "textures/gui/world_moded.png");
    private static final int TEXTURE_WIDTH = 1024;
    private static final int TEXTURE_HEIGHT = 128;

    @Inject(method = "render", at = @At("HEAD"))
    private void lumenechallenge$drawMarker(DrawContext context, int mouseX, int mouseY, boolean hovered, float delta, CallbackInfo ci) {
        LevelSummary level = ((WorldListWidget.WorldEntry)(Object)this).getLevel();
        if (!WorldMarkerUtil.isMarked(level.getIconPath())) return;

        Widget widget = (Widget) (Object) this;
        int x = widget.getX();
        int y = widget.getY();
        int entryWidth = widget.getWidth();
        int entryHeight = widget.getHeight();

        // Draw the modded-world artwork as the full entry background.
        // The vanilla entry content is rendered after this injection and stays visible on top.
        context.drawTexture(net.minecraft.client.gl.RenderPipelines.GUI_TEXTURED, MODDED,
                x, y, 0, 0,
                entryWidth, entryHeight,
                TEXTURE_WIDTH, TEXTURE_HEIGHT,
                TEXTURE_WIDTH, TEXTURE_HEIGHT);
    }
}
