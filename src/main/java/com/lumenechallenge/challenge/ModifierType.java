
package com.lumenechallenge.challenge;

import com.lumenechallenge.LumeneChallengeMod;
import net.minecraft.util.Identifier;

import java.util.List;
import java.util.Random;

public enum ModifierType {
    NONE("modifier.lumenechallenge.none", "modifier.lumenechallenge.none.desc", "textures/gui/modifier_none.png", 1.0f),
    RANDOM("modifier.lumenechallenge.random", "modifier.lumenechallenge.random.desc", "textures/gui/modifier_random.png", 1.0f),
    GIANT("modifier.lumenechallenge.giant", "modifier.lumenechallenge.giant.desc", "textures/gui/modifier_giant.png", 2.0f),
    DWARF("modifier.lumenechallenge.dwarf", "modifier.lumenechallenge.dwarf.desc", "textures/gui/modifier_dwarf.png", 0.5f),
    CHAOS("modifier.lumenechallenge.chaos", "modifier.lumenechallenge.chaos.desc", "textures/gui/modifier_chaos.png", 1.0f),
    HANGOVER("modifier.lumenechallenge.hangover", "modifier.lumenechallenge.hangover.desc", "textures/gui/modifier_hangover.png", 1.0f),
    POCKETS("modifier.lumenechallenge.pockets", "modifier.lumenechallenge.pockets.desc", "textures/gui/modifier_pockets.png", 1.0f),
    SPEEDRUN("modifier.lumenechallenge.speedrun", "modifier.lumenechallenge.speedrun.desc", "textures/gui/modifier_speedrun.png", 1.0f),
    PACIFIST("modifier.lumenechallenge.pacifist", "modifier.lumenechallenge.pacifist.desc", "textures/gui/modifier_pacifist.png", 1.0f),
    OBESITY("modifier.lumenechallenge.obesity", "modifier.lumenechallenge.obesity.desc", "textures/gui/modifier_obesity.png", 1.0f),
    CATACLYSM("modifier.lumenechallenge.cataclysm", "modifier.lumenechallenge.cataclysm.desc", "textures/gui/modifier_cataclysm.png", 1.0f);

    private static final ModifierType[] RANDOM_POOL = {GIANT, DWARF, CHAOS, HANGOVER, POCKETS, SPEEDRUN, PACIFIST, OBESITY, CATACLYSM};
    private static final ModifierType[] MENU_ORDER = {NONE, RANDOM, GIANT, DWARF, CHAOS, HANGOVER, POCKETS, SPEEDRUN, PACIFIST, OBESITY, CATACLYSM};

    private final String translationKey;
    private final String tooltipKey;
    private final Identifier icon;
    private final float scale;

    ModifierType(String translationKey, String tooltipKey, String iconPath, float scale) {
        this.translationKey = translationKey;
        this.tooltipKey = tooltipKey;
        this.icon = Identifier.of(LumeneChallengeMod.MOD_ID, iconPath);
        this.scale = scale;
    }

    public String translationKey() {
        return translationKey;
    }

    public String tooltipKey() {
        return tooltipKey;
    }

    public Identifier icon() {
        return icon;
    }

    public float scale() {
        return scale;
    }

    public boolean isVisible() {
        return this != NONE;
    }

    public boolean hasMenuInfo() {
        return this != NONE;
    }

    public static List<ModifierType> menuValues() {
        return List.of(MENU_ORDER);
    }

    public static ModifierType random(long seed) {
        Random random = new Random(seed ^ 0x6A09E667F3BCC909L);
        return RANDOM_POOL[random.nextInt(RANDOM_POOL.length)];
    }

    public static ModifierType fromId(String id) {
        if (id == null || id.isBlank()) {
            return NONE;
        }
        for (ModifierType type : values()) {
            if (type.name().equalsIgnoreCase(id)) {
                return type;
            }
        }
        return NONE;
    }
}
