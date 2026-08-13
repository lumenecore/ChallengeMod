package com.lumenechallenge.mixin;

import com.lumenechallenge.LumeneChallengeMod;
import com.lumenechallenge.util.WorldMarkerUtil;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.world.WorldListWidget;
import net.minecraft.util.Identifier;
import net.minecraft.world.level.storage.LevelSummary;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(WorldListWidget.WorldEntry.class)
public abstract class WorldListEntryMixin {
    private static final Identifier MODDED = Identifier.of(LumeneChallengeMod.MOD_ID, "textures/gui/world_moded.png");
    @Shadow private LevelSummary level;

    @Inject(method = "render", at = @At("TAIL"))
    private void lumenechallenge$drawMarker(DrawContext context, int index, int y, int x, int entryWidth, int entryHeight,
                                             int mouseX, int mouseY, boolean hovered, float delta, CallbackInfo ci) {
        if (!WorldMarkerUtil.isMarked(level.getIconPath())) return;
        int markerWidth = 40;
        int markerHeight = 10;
        int markerX = x + entryWidth - markerWidth - 2;
        int markerY = y + Math.max(0, (entryHeight - markerHeight) / 2) - 1;
        context.drawTexture(net.minecraft.client.render.RenderLayer::getGuiTextured, MODDED,
                markerX, markerY, 0, 0, markerWidth, markerHeight, 200, 50, 200, 50);
    }
}
