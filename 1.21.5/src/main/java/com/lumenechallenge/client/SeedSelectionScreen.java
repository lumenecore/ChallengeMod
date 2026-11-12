package com.lumenechallenge.client;

import com.lumenechallenge.client.layout.ModI18n;
import com.lumenechallenge.client.layout.ChallengeModSettings;

import com.lumenechallenge.challenge.ChallengeCategoryManager;
import com.lumenechallenge.challenge.ModifierType;
import com.lumenechallenge.challenge.RunDifficulty;
import com.lumenechallenge.network.SelectSeedPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class SeedSelectionScreen extends Screen {
    private static final int DESIGN_WIDTH = 960;
    private static final int DESIGN_HEIGHT = 540;

    private static final int CONTENT_X = 20;
    private static final int CONTENT_WIDTH = 920;
    private static final int CONTENT_TOP = 20;

    private static final int LABEL_WIDTH = 455;
    private static final int LABEL_HEIGHT = 40;
    private static final int FIELD_WIDTH = 455;
    private static final int FIELD_HEIGHT = 50;
    private static final int FIELD_GAP = 10;

    private static final int SECTION_TITLE_WIDTH = 920;
    private static final int SECTION_TITLE_HEIGHT = 40;

    private static final int DIFFICULTY_WIDTH = 300;
    private static final int DIFFICULTY_HEIGHT = 120;
    private static final int DIFFICULTY_GAP = 10;
    private static final int DIFFICULTY_X_1 = 20;
    private static final int DIFFICULTY_X_2 = DIFFICULTY_X_1 + DIFFICULTY_WIDTH + DIFFICULTY_GAP;
    private static final int DIFFICULTY_X_3 = DIFFICULTY_X_2 + DIFFICULTY_WIDTH + DIFFICULTY_GAP;

    private static final int MODIFIER_AREA_HEIGHT = 90;
    private static final int MODIFIER_ICON_SIZE = 64;
    private static final int MODIFIER_NAME_WIDTH = 160;
    private static final int MODIFIER_NAME_HEIGHT = 20;
    private static final int ARROW_SIZE = 32;
    private static final int SELECTED_GROUP_WIDTH = 160;
    private static final int SELECTED_GROUP_X = (DESIGN_WIDTH - SELECTED_GROUP_WIDTH) / 2;
    private static final int SELECTED_ICON_X = SELECTED_GROUP_X + ARROW_SIZE + 16;
    private static final int RIGHT_ARROW_X = SELECTED_ICON_X + MODIFIER_ICON_SIZE + 16;

    private static final int[] SIDE_MODIFIER_X = {
            80, 160, 240, 320,
            576, 656, 736, 816
    };

    private static final int START_BUTTON_WIDTH = 320;
    private static final int START_BUTTON_HEIGHT = 40;
    private static final int START_BUTTON_X = (DESIGN_WIDTH - START_BUTTON_WIDTH) / 2;

    private static final int MIN_CHALLENGE_COUNT = 1;
    private static final int MAX_CHALLENGE_COUNT = 100;
    private static final int DEFAULT_CHALLENGE_COUNT = 10;
    private static final int[] CHALLENGE_COUNT_OPTIONS = {1, 3, 5, 10, 25, 50, 75, 100};
    private static final int COUNT_BUTTON_SIZE = 46;
    private static final int COUNT_BUTTON_GAP = 8;
    // Base scale is unchanged. Hovered/selected values smoothly grow by ~2 screen pixels.
    private final float[] countButtonTextScales = {
            1.5f, 1.5f, 1.5f, 1.5f, 1.5f, 1.5f, 1.5f, 1.5f
    };
    private static final int SEED_MAX_LENGTH = 20;

    private static final Identifier BACKGROUND = Identifier.of(
            "lumenechallenge", "textures/gui/background.png");
    private static final Identifier START_FIELD = Identifier.of(
            "lumenechallenge", "textures/gui/start_field.png");
    private static final Identifier START_FIELD_FOCUSED = Identifier.of(
            "lumenechallenge", "textures/gui/start_field_focused.png");
    private static final Identifier START_ARROW_LEFT = Identifier.of(
            "lumenechallenge", "textures/gui/start_arrow_left.png");
    private static final Identifier START_ARROW_LEFT_HOVER = Identifier.of(
            "lumenechallenge", "textures/gui/start_arrow_left_hover.png");
    private static final Identifier START_ARROW_RIGHT = Identifier.of(
            "lumenechallenge", "textures/gui/start_arrow_right.png");
    private static final Identifier START_ARROW_RIGHT_HOVER = Identifier.of(
            "lumenechallenge", "textures/gui/start_arrow_right_hover.png");
    private static final Identifier START_BUTTON = Identifier.of(
            "lumenechallenge", "textures/gui/start_button.png");
    private static final Identifier START_BUTTON_HOVER = Identifier.of(
            "lumenechallenge", "textures/gui/start_button_hover.png");
    private static final Identifier START_TOOLTIP = Identifier.of(
            "lumenechallenge", "textures/gui/start_tooltip.png");
    private static final Identifier COUNT_BUTTON = Identifier.of(
            "lumenechallenge", "textures/gui/start_count_button.png");
    private static final Identifier COUNT_BUTTON_HOVER = Identifier.of(
            "lumenechallenge", "textures/gui/start_count_button_hover.png");
    private static final Identifier COUNT_BUTTON_SELECTED = Identifier.of(
            "lumenechallenge", "textures/gui/start_count_button_selected.png");


    private String seedInput = "";
    private int seedCursorPos = 0;
    private boolean seedFocused = false;

    private int selectedChallengeCount = DEFAULT_CHALLENGE_COUNT;

    private int blinkTicks = 0;

    private RunDifficulty selectedDifficulty = RunDifficulty.NORMAL;
    private ModifierType selectedModifier = ModifierType.NONE;

    public SeedSelectionScreen() {
        super(ModI18n.text("hud.lumenechallenge.title"));
    }

    @Override
    protected void init() {
        seedCursorPos = seedInput.length();
        seedFocused = false;
        blinkTicks = 0;
    }

    @Override
    public void tick() {
        blinkTicks++;
        ChallengeCategoryManager.get().warmupStep();
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        float scaleX = width / (float) DESIGN_WIDTH;
        float scaleY = height / (float) DESIGN_HEIGHT;
        int designMouseX = Math.round(mouseX / scaleX);
        int designMouseY = Math.round(mouseY / scaleY);

        var matrices = context.getMatrices();
        matrices.push();
        matrices.scale(scaleX, scaleY, 1.0f);

        drawTexture(context, BACKGROUND, 0, 0, DESIGN_WIDTH, DESIGN_HEIGHT, DESIGN_WIDTH, DESIGN_HEIGHT);
        renderContent(context, designMouseX, designMouseY);

        matrices.pop();
    }

    private void renderContent(DrawContext context, int mouseX, int mouseY) {
        int seedLabelY = CONTENT_TOP;
        int fieldY = seedLabelY + LABEL_HEIGHT + FIELD_GAP;
        int difficultyTitleY = fieldY + FIELD_HEIGHT + FIELD_GAP;
        int difficultyY = difficultyTitleY + SECTION_TITLE_HEIGHT + FIELD_GAP;
        int modifierTitleY = difficultyY + DIFFICULTY_HEIGHT + FIELD_GAP;
        int modifierY = modifierTitleY + SECTION_TITLE_HEIGHT + FIELD_GAP;
        int startButtonY = modifierY + MODIFIER_AREA_HEIGHT + FIELD_GAP;

        drawCenteredHeader(context, ModI18n.text("screen.lumenechallenge.seed.label"), CONTENT_X, seedLabelY, LABEL_WIDTH, LABEL_HEIGHT, 0xFFFFFFFF);
        drawCenteredHeader(context, ModI18n.text("screen.lumenechallenge.seed.challenges_label"), CONTENT_X + LABEL_WIDTH + FIELD_GAP,
                seedLabelY, LABEL_WIDTH, LABEL_HEIGHT, 0xFFFFFFFF);

        drawInputField(context, CONTENT_X, fieldY, FIELD_WIDTH, FIELD_HEIGHT, seedFocused);
        drawSeedInput(context, CONTENT_X, fieldY, FIELD_WIDTH, FIELD_HEIGHT);

        int countFieldX = CONTENT_X + FIELD_WIDTH + FIELD_GAP;
        drawCountSelector(context, countFieldX, fieldY, mouseX, mouseY);

        drawCenteredHeader(context, ModI18n.text("screen.lumenechallenge.seed.difficulty_label"), CONTENT_X, difficultyTitleY,
                SECTION_TITLE_WIDTH, SECTION_TITLE_HEIGHT, 0xFFFFFFFF);
        drawDifficulty(context, RunDifficulty.NORMAL, DIFFICULTY_X_1, difficultyY);
        drawDifficulty(context, RunDifficulty.HARD, DIFFICULTY_X_2, difficultyY);
        drawDifficulty(context, RunDifficulty.INSANE, DIFFICULTY_X_3, difficultyY);

        drawCenteredHeader(context, ModI18n.text("screen.lumenechallenge.seed.modifier_label"), CONTENT_X, modifierTitleY,
                SECTION_TITLE_WIDTH, SECTION_TITLE_HEIGHT, 0xFFFFFFFF);
        drawModifierCarousel(context, mouseX, mouseY, modifierY);

        boolean startHovered = isInside(mouseX, mouseY, START_BUTTON_X, startButtonY,
                START_BUTTON_WIDTH, START_BUTTON_HEIGHT);
        drawTexture(context, startHovered ? START_BUTTON_HOVER : START_BUTTON,
                START_BUTTON_X, startButtonY, START_BUTTON_WIDTH, START_BUTTON_HEIGHT, 320, 40);
        drawCenteredLine(context, ModI18n.text("screen.lumenechallenge.seed.start_game"), START_BUTTON_X, startButtonY,
                START_BUTTON_WIDTH, START_BUTTON_HEIGHT, 0xFFFFFFFF);
    }

    private void drawInputField(DrawContext context, int x, int y, int width, int height, boolean focused) {
        drawTexture(context, focused ? START_FIELD_FOCUSED : START_FIELD, x, y, width, height, 455, 50);
    }

    private void drawSeedInput(DrawContext context, int x, int y, int width, int height) {
        drawInputText(context, seedInput, seedCursorPos, seedFocused, x, y, width, height, SEED_MAX_LENGTH);
    }

    private void drawCountSelector(DrawContext context, int fieldX, int fieldY, int mouseX, int mouseY) {
        int totalWidth = CHALLENGE_COUNT_OPTIONS.length * COUNT_BUTTON_SIZE
                + (CHALLENGE_COUNT_OPTIONS.length - 1) * COUNT_BUTTON_GAP;
        int startX = fieldX + (FIELD_WIDTH - totalWidth) / 2;
        int buttonY = fieldY + (FIELD_HEIGHT - COUNT_BUTTON_SIZE) / 2;

        for (int i = 0; i < CHALLENGE_COUNT_OPTIONS.length; i++) {
            int value = CHALLENGE_COUNT_OPTIONS[i];
            int x = startX + i * (COUNT_BUTTON_SIZE + COUNT_BUTTON_GAP);
            boolean hovered = isInside(mouseX, mouseY, x, buttonY, COUNT_BUTTON_SIZE, COUNT_BUTTON_SIZE);
            Identifier texture = selectedChallengeCount == value
                    ? COUNT_BUTTON_SELECTED
                    : hovered ? COUNT_BUTTON_HOVER : COUNT_BUTTON;

            drawTexture(context, texture, x, buttonY, COUNT_BUTTON_SIZE, COUNT_BUTTON_SIZE,
                    COUNT_BUTTON_SIZE, COUNT_BUTTON_SIZE);

            // Keep the old size for inactive options; animate only the number on hover/selection.
            float targetScale = (selectedChallengeCount == value || hovered) ? 1.72f : 1.5f;
            float currentScale = countButtonTextScales[i];
            currentScale += (targetScale - currentScale) * 0.22f;
            if (Math.abs(targetScale - currentScale) < 0.004f) {
                currentScale = targetScale;
            }
            countButtonTextScales[i] = currentScale;

            drawCountButtonText(context, Text.literal(Integer.toString(value)), x, buttonY,
                    COUNT_BUTTON_SIZE, COUNT_BUTTON_SIZE, 0xFFFFFFFF, currentScale);
        }
    }

    private void drawCountButtonText(DrawContext context, Text text, int x, int y,
                                     int width, int height, int color, float scale) {
        var matrices = context.getMatrices();
        matrices.push();
        matrices.translate(x + width / 2.0f, y + height / 2.0f, 0.0f);
        matrices.scale(scale, scale, 1.0f);
        context.drawCenteredTextWithShadow(
                textRenderer,
                text,
                0,
                -textRenderer.fontHeight / 2,
                color
        );
        matrices.pop();
    }

    private void drawInputText(DrawContext context, String value, int cursorPos, boolean focused,
                               int x, int y, int width, int height, int maxLength) {
        int padding = 14;

        float scale = 1.5f;

        int maxWidth = (int) ((width - padding * 2) / scale);
        int safeCursor = Math.max(0, Math.min(cursorPos, value.length()));
        int startIndex = 0;

        if (textRenderer.getWidth(value) > maxWidth) {
            startIndex = safeCursor;
            while (startIndex > 0 &&
                    textRenderer.getWidth(value.substring(startIndex - 1, safeCursor)) <= maxWidth) {
                startIndex--;
            }
        }

        int endIndex = value.length();
        while (endIndex > startIndex &&
                textRenderer.getWidth(value.substring(startIndex, endIndex)) > maxWidth) {
            endIndex--;
        }

        String visible = value.substring(startIndex, endIndex);

        var matrices = context.getMatrices();
        matrices.push();

        float scaledTextX = x + padding;
        float centerY = y + height / 2.0f;

        matrices.translate(scaledTextX, centerY, 0.0f);
        matrices.scale(scale, scale, 1.0f);

        context.drawTextWithShadow(
                textRenderer,
                Text.literal(visible),
                0,
                -textRenderer.fontHeight / 2,
                0xFFFFFFFF
        );

        if (focused && (blinkTicks / 6) % 2 == 0) {
            int visibleCursor = Math.max(startIndex, Math.min(safeCursor, endIndex));
            String before = value.substring(startIndex, visibleCursor);

            int cursorX = textRenderer.getWidth(before);

            context.fill(
                    cursorX,
                    -textRenderer.fontHeight / 2 - 1,
                    cursorX + 1,
                    -textRenderer.fontHeight / 2 + textRenderer.fontHeight + 1,
                    0xFFFFFFFF
            );
        }

        matrices.pop();
    }

    private void drawDifficulty(DrawContext context, RunDifficulty difficulty, int x, int y) {
        // The *_hover asset is the selected-state artwork, not a mouse-hover state.
        // Only the currently selected difficulty uses it; cursor position is irrelevant.
        boolean selected = selectedDifficulty == difficulty;
        Identifier texture = resolveDifficultyTexture(difficulty, selected);
        drawTexture(context, texture, x, y, DIFFICULTY_WIDTH, DIFFICULTY_HEIGHT, 300, 120);
    }

    private Identifier resolveDifficultyTexture(RunDifficulty difficulty, boolean selected) {
        String textureName = switch (difficulty) {
            case NORMAL -> "normal";
            case HARD -> "hard";
            case INSANE -> "insane";
        };
        String suffix = selected ? "_hover" : "";
        List<String> folders = difficultyTextureFolders();

        // First try the selected ChallengeMod language.
        for (String folder : folders) {
            Identifier candidate = difficultyTexture(folder, textureName, suffix);
            if (hasTexture(candidate)) return candidate;
        }

        // Russian assets are the guaranteed fallback for incomplete translations.
        Identifier russianFallback = difficultyTexture("ru", textureName, suffix);
        if (hasTexture(russianFallback)) return russianFallback;

        // If the selected-state variant is missing even in the fallback set,
        // use the regular Russian texture. drawTexture() also guards missing resources.
        Identifier russianRegular = difficultyTexture("ru", textureName, "");
        if (hasTexture(russianRegular)) return russianRegular;

        return russianFallback;
    }

    private List<String> difficultyTextureFolders() {
        String language = ChallengeModSettings.getModLanguage();
        return switch (language) {
            case "zh_cn" -> List.of("cn");
            case "de_de" -> List.of("de");
            case "en_us" -> List.of("en");
            case "es_es" -> List.of("es");
            case "fr_fr" -> List.of("fr");
            case "ja_jp" -> List.of("jp");
            case "ko_kr" -> List.of("kr");
            case "pl_pl" -> List.of("pl");
            case "pt_br" -> List.of("pt");
            case "ru_ru" -> List.of("ru");
            case "tr_tr" -> List.of("tr");
            default -> List.of("ru");
        };
    }

    private Identifier difficultyTexture(String folder, String textureName, String suffix) {
        return Identifier.of("lumenechallenge",
                "textures/gui/levels/" + folder + "/" + textureName + suffix + ".png");
    }

    private boolean hasTexture(Identifier texture) {
        return client != null && GuiPanelRenderer.hasTexture(texture);
    }

    private void drawModifierCarousel(DrawContext context, int mouseX, int mouseY, int y) {
        List<ModifierType> modifiers = ModifierType.menuValues();
        int selectedIndex = modifiers.indexOf(selectedModifier);
        if (selectedIndex < 0) selectedIndex = 0;

        int selectedIconY = y + 2;
        int selectedArrowY = y + 18;
        int selectedNameY = y + 68;

        boolean leftHovered = isInside(mouseX, mouseY, SELECTED_GROUP_X, selectedArrowY, ARROW_SIZE, ARROW_SIZE);
        boolean rightHovered = isInside(mouseX, mouseY, RIGHT_ARROW_X, selectedArrowY, ARROW_SIZE, ARROW_SIZE);
        boolean selectedHovered = isInside(mouseX, mouseY, SELECTED_ICON_X, selectedIconY,
                MODIFIER_ICON_SIZE, MODIFIER_ICON_SIZE);

        drawTexture(context, leftHovered ? START_ARROW_LEFT_HOVER : START_ARROW_LEFT,
                SELECTED_GROUP_X, selectedArrowY, ARROW_SIZE, ARROW_SIZE, 32, 32);
        drawTexture(context, rightHovered ? START_ARROW_RIGHT_HOVER : START_ARROW_RIGHT,
                RIGHT_ARROW_X, selectedArrowY, ARROW_SIZE, ARROW_SIZE, 32, 32);
        drawModifierIcon(context, modifiers.get(selectedIndex), SELECTED_ICON_X, selectedIconY);
        drawCenteredLine(context, ModI18n.text(selectedModifier.translationKey()),
                SELECTED_GROUP_X, selectedNameY, MODIFIER_NAME_WIDTH, MODIFIER_NAME_HEIGHT, 0xFFFFFFFF);

        int sideY = y + 13;
        for (int offset = 1; offset <= 4; offset++) {
            ModifierType left = modifiers.get(Math.floorMod(selectedIndex - offset, modifiers.size()));
            ModifierType right = modifiers.get(Math.floorMod(selectedIndex + offset, modifiers.size()));
            drawModifierIcon(context, left, SIDE_MODIFIER_X[4 - offset], sideY);
            drawModifierIcon(context, right, SIDE_MODIFIER_X[4 + offset - 1], sideY);
        }

        if (selectedHovered) {
            drawStartTooltip(context, mouseX, mouseY, ModI18n.text(selectedModifier.tooltipKey()));
        } else {
            for (int offset = 1; offset <= 4; offset++) {
                ModifierType left = modifiers.get(Math.floorMod(selectedIndex - offset, modifiers.size()));
                ModifierType right = modifiers.get(Math.floorMod(selectedIndex + offset, modifiers.size()));

                int leftX = SIDE_MODIFIER_X[4 - offset];
                int rightX = SIDE_MODIFIER_X[4 + offset - 1];
                if (isInside(mouseX, mouseY, leftX, sideY, MODIFIER_ICON_SIZE, MODIFIER_ICON_SIZE)) {
                    drawStartTooltip(context, mouseX, mouseY, ModI18n.text(left.tooltipKey()));
                    return;
                }
                if (isInside(mouseX, mouseY, rightX, sideY, MODIFIER_ICON_SIZE, MODIFIER_ICON_SIZE)) {
                    drawStartTooltip(context, mouseX, mouseY, ModI18n.text(right.tooltipKey()));
                    return;
                }
            }
        }
    }

    private void drawModifierIcon(DrawContext context, ModifierType modifier, int x, int y) {
        var matrices = context.getMatrices();
        matrices.push();
        matrices.translate(x, y, 0.0f);
        matrices.scale(2.0f, 2.0f, 1.0f);
        drawTexture(context, modifier.icon(), 0, 0, 32, 32, 32, 32);
        matrices.pop();
    }

    private void drawStartTooltip(DrawContext context, int mouseX, int mouseY, Text line) {
        int tooltipWidth = 220;
        int tooltipHeight = 64;
        int x = mouseX + 12;
        if (x + tooltipWidth > DESIGN_WIDTH - 8) {
            x = mouseX - tooltipWidth - 12;
        }
        x = Math.max(8, Math.min(x, DESIGN_WIDTH - tooltipWidth - 8));

        int y = mouseY - tooltipHeight - 12;
        if (y < 8) y = mouseY + 12;
        y = Math.max(8, Math.min(y, DESIGN_HEIGHT - tooltipHeight - 8));

        drawTexture(context, START_TOOLTIP, x, y, tooltipWidth, tooltipHeight, 220, 64);

        // Render text larger than vanilla-sized tooltip text while keeping the
        // tooltip texture and line wrapping crisp at the fixed design scale.
        final float textScale = 1.30f;
        int logicalTextWidth = Math.max(1, (int) ((tooltipWidth - 20) / textScale));
        List<OrderedText> wrappedLines = new ArrayList<>(textRenderer.wrapLines(line, logicalTextWidth));
        if (wrappedLines.isEmpty()) wrappedLines.add(Text.literal("").asOrderedText());

        int lineHeight = textRenderer.fontHeight + 2;
        float renderedHeight = wrappedLines.size() * lineHeight * textScale - 1;
        float startY = y + Math.max(0, (tooltipHeight - renderedHeight) / 2.0f);
        int centerX = x + tooltipWidth / 2;

        var matrices = context.getMatrices();
        matrices.push();
        matrices.translate(centerX, startY, 0.0f);
        matrices.scale(textScale, textScale, 1.0f);
        for (int i = 0; i < wrappedLines.size(); i++) {
            context.drawCenteredTextWithShadow(textRenderer, wrappedLines.get(i), 0, i * lineHeight, 0xFFFFFFFF);
        }
        matrices.pop();
    }

    private void drawCenteredHeader(DrawContext context, Text text, int x, int y, int width, int height, int color) {
        var matrices = context.getMatrices();
        matrices.push();
        float scale = 1.5f;
        float centerX = x + width / 2.0f;
        float centerY = y + height / 2.0f;
        matrices.translate(centerX, centerY, 0.0f);
        matrices.scale(scale, scale, 1.0f);
        context.drawCenteredTextWithShadow(textRenderer, text, 0, -textRenderer.fontHeight / 2, color);
        matrices.pop();
    }

    private void drawCenteredLine(DrawContext context, Text text, int x, int y, int width, int height, int color) {
        var matrices = context.getMatrices();
        matrices.push();

        float scale = 1.5f;

        float centerX = x + width / 2.0f;
        float centerY = y + height / 2.0f;

        matrices.translate(centerX, centerY, 0.0f);
        matrices.scale(scale, scale, 1.0f);

        context.drawCenteredTextWithShadow(
                textRenderer,
                text,
                0,
                -textRenderer.fontHeight / 2,
                color
        );

        matrices.pop();
    }

    private boolean isInside(double mouseX, double mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) return true;

        float scaleX = width / (float) DESIGN_WIDTH;
        float scaleY = height / (float) DESIGN_HEIGHT;
        double x = mouseX / scaleX;
        double y = mouseY / scaleY;

        int fieldY = CONTENT_TOP + LABEL_HEIGHT + FIELD_GAP;
        int difficultyTitleY = fieldY + FIELD_HEIGHT + FIELD_GAP;
        int difficultyY = difficultyTitleY + SECTION_TITLE_HEIGHT + FIELD_GAP;
        int modifierTitleY = difficultyY + DIFFICULTY_HEIGHT + FIELD_GAP;
        int modifierY = modifierTitleY + SECTION_TITLE_HEIGHT + FIELD_GAP;
        int startButtonY = modifierY + MODIFIER_AREA_HEIGHT + FIELD_GAP;

        if (isInside(x, y, CONTENT_X, fieldY, FIELD_WIDTH, FIELD_HEIGHT)) {
            seedFocused = true;
            seedCursorPos = seedInput.length();
            return true;
        }

        int countFieldX = CONTENT_X + FIELD_WIDTH + FIELD_GAP;
        if (isInside(x, y, countFieldX, fieldY, FIELD_WIDTH, FIELD_HEIGHT)) {
            seedFocused = false;
            int totalWidth = CHALLENGE_COUNT_OPTIONS.length * COUNT_BUTTON_SIZE
                    + (CHALLENGE_COUNT_OPTIONS.length - 1) * COUNT_BUTTON_GAP;
            int startX = countFieldX + (FIELD_WIDTH - totalWidth) / 2;
            int buttonY = fieldY + (FIELD_HEIGHT - COUNT_BUTTON_SIZE) / 2;
            for (int i = 0; i < CHALLENGE_COUNT_OPTIONS.length; i++) {
                int buttonX = startX + i * (COUNT_BUTTON_SIZE + COUNT_BUTTON_GAP);
                if (isInside(x, y, buttonX, buttonY, COUNT_BUTTON_SIZE, COUNT_BUTTON_SIZE)) {
                    selectedChallengeCount = CHALLENGE_COUNT_OPTIONS[i];
                    return true;
                }
            }
            return true;
        }

        seedFocused = false;

        if (isInside(x, y, DIFFICULTY_X_1, difficultyY, DIFFICULTY_WIDTH, DIFFICULTY_HEIGHT)) {
            selectedDifficulty = RunDifficulty.NORMAL;
            return true;
        }
        if (isInside(x, y, DIFFICULTY_X_2, difficultyY, DIFFICULTY_WIDTH, DIFFICULTY_HEIGHT)) {
            selectedDifficulty = RunDifficulty.HARD;
            return true;
        }
        if (isInside(x, y, DIFFICULTY_X_3, difficultyY, DIFFICULTY_WIDTH, DIFFICULTY_HEIGHT)) {
            selectedDifficulty = RunDifficulty.INSANE;
            return true;
        }

        int selectedArrowY = modifierY + 18;
        if (isInside(x, y, SELECTED_GROUP_X, selectedArrowY, ARROW_SIZE, ARROW_SIZE)) {
            cycleModifier(-1);
            return true;
        }
        if (isInside(x, y, RIGHT_ARROW_X, selectedArrowY, ARROW_SIZE, ARROW_SIZE)) {
            cycleModifier(1);
            return true;
        }

        if (isInside(x, y, START_BUTTON_X, startButtonY, START_BUTTON_WIDTH, START_BUTTON_HEIGHT)) {
            submitSelection();
            return true;
        }

        return true;
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        if (seedFocused) {
            if (Character.isISOControl(chr) || seedInput.length() >= SEED_MAX_LENGTH) return true;
            seedInput = seedInput.substring(0, seedCursorPos) + chr + seedInput.substring(seedCursorPos);
            seedCursorPos++;
            return true;
        }


        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (seedFocused) {
            return handleSeedTextFieldKey(keyCode);
        }
        return true;
    }

    private boolean handleSeedTextFieldKey(int keyCode) {
        String value = seedInput;
        int cursor = seedCursorPos;

        switch (keyCode) {
            case GLFW.GLFW_KEY_V -> {
                if (GLFW.glfwGetKey(client.getWindow().getHandle(), GLFW.GLFW_KEY_LEFT_CONTROL) == GLFW.GLFW_PRESS
                        || GLFW.glfwGetKey(client.getWindow().getHandle(), GLFW.GLFW_KEY_RIGHT_CONTROL) == GLFW.GLFW_PRESS) {
                    String clipboard = client.keyboard.getClipboard();
                    if (clipboard != null && !clipboard.isEmpty()) {
                        value = clipboard.substring(0, Math.min(clipboard.length(), SEED_MAX_LENGTH));
                        cursor = value.length();
                    }
                }
            }
            case GLFW.GLFW_KEY_BACKSPACE -> {
                if (cursor > 0 && !value.isEmpty()) {
                    value = value.substring(0, cursor - 1) + value.substring(cursor);
                    cursor--;
                }
            }
            case GLFW.GLFW_KEY_DELETE -> {
                if (cursor < value.length()) {
                    value = value.substring(0, cursor) + value.substring(cursor + 1);
                }
            }
            case GLFW.GLFW_KEY_LEFT -> cursor = Math.max(0, cursor - 1);
            case GLFW.GLFW_KEY_RIGHT -> cursor = Math.min(value.length(), cursor + 1);
            case GLFW.GLFW_KEY_HOME -> cursor = 0;
            case GLFW.GLFW_KEY_END -> cursor = value.length();
            default -> {
                return true;
            }
        }

        seedInput = value;
        seedCursorPos = cursor;
        return true;
    }

    private void submitSelection() {
        if (client == null || client.getNetworkHandler() == null) return;

        String seedValue = seedInput.trim();
        if (seedValue.isEmpty()) {
            seedValue = Long.toString(ThreadLocalRandom.current().nextLong());
        }

        int challengeCount = parseChallengeCount();

        ClientPlayNetworking.send(new SelectSeedPayload(
                seedValue, selectedModifier.name(), selectedDifficulty.name(), challengeCount));
        client.setScreen(null);
    }

    private int parseChallengeCount() {
        return Math.max(MIN_CHALLENGE_COUNT, Math.min(MAX_CHALLENGE_COUNT, selectedChallengeCount));
    }

    private void cycleModifier(int delta) {
        List<ModifierType> modifiers = ModifierType.menuValues();
        int index = modifiers.indexOf(selectedModifier);
        if (index < 0) index = 0;
        selectedModifier = modifiers.get(Math.floorMod(index + delta, modifiers.size()));
    }

    private void drawTexture(DrawContext context, Identifier texture, int x, int y,
                             int width, int height, int textureWidth, int textureHeight) {
        GuiPanelRenderer.drawTextureIfAvailable(context, texture, x, y, width, height,
                textureWidth, textureHeight, 0xFF333333);
    }
}
