package com.lumenechallenge.mixin;

import com.lumenechallenge.LumeneChallengeMod;
import com.lumenechallenge.util.WorldMarkerUtil;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.world.WorldListWidget;
import net.minecraft.util.Identifier;
import net.minecraft.world.level.storage.LevelSummary;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(WorldListWidget.WorldEntry.class)
public abstract class WorldListEntryMixin {
    private static final Identifier MODDED = Identifier.of(LumeneChallengeMod.MOD_ID, "textures/gui/world_moded.png");
    private static final int TEXTURE_WIDTH = 1024;
    private static final int TEXTURE_HEIGHT = 128;

    @Shadow @Final LevelSummary level;

    @Inject(method = "render(Lnet/minecraft/client/gui/DrawContext;IIIIIIIZF)V", at = @At("HEAD"))
    private void lumenechallenge$drawMarker(
            DrawContext context,
            int index,
            int y,
            int x,
            int entryWidth,
            int entryHeight,
            int mouseX,
            int mouseY,
            boolean hovered,
            float tickProgress,
            CallbackInfo ci
    ) {
        if (!WorldMarkerUtil.isMarked(level.getIconPath())) return;

        // Draw the marked-world artwork behind the vanilla entry contents.
        context.drawTexture(
                net.minecraft.client.gl.RenderPipelines.GUI_TEXTURED,
                MODDED,
                x, y, 0, 0,
                entryWidth, entryHeight,
                TEXTURE_WIDTH, TEXTURE_HEIGHT,
                TEXTURE_WIDTH, TEXTURE_HEIGHT
        );
    }
}
