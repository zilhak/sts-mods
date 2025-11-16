package com.stsmod.ascension100.util;

import com.megacrit.cardcrawl.dungeons.AbstractDungeon;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Helper class for determining encounter difficulty (weak vs strong)
 * Based on game's internal encounter generation logic
 */
public class EncounterHelper {

    // Act 1 (Exordium) Weak Encounters
    private static final Set<String> ACT1_WEAK = new HashSet<>(Arrays.asList(
        "Cultist",
        "Jaw Worm",
        "2 Louse",
        "Small Slimes"
    ));

    // Act 1 (Exordium) Strong Encounters
    private static final Set<String> ACT1_STRONG = new HashSet<>(Arrays.asList(
        "Blue Slaver",
        "Gremlin Gang",
        "Looter",
        "Large Slime",
        "Lots of Slimes",
        "Exordium Thugs",
        "Exordium Wildlife",
        "Red Slaver",
        "3 Louse",
        "2 Fungi Beasts"
    ));

    // Act 2 (The City) Weak Encounters
    private static final Set<String> ACT2_WEAK = new HashSet<>(Arrays.asList(
        "Spheric Guardian",
        "Chosen",
        "Shell Parasite",
        "3 Byrds",
        "2 Thieves"
    ));

    // Act 2 (The City) Strong Encounters
    private static final Set<String> ACT2_STRONG = new HashSet<>(Arrays.asList(
        "Chosen and Byrds",
        "Sentry and Sphere",
        "Snecko",
        "Looter",
        "Mugger",
        "Shelled Parasite and Fungus Beast",
        "Snake Plant",
        "Spheric Guardian",
        "Centurion and Healer",
        "Cultist and Chosen",
        "3 Cultists",
        "Shelled Parasite and Cultist"
    ));

    // Act 3 (The Beyond) Weak Encounters
    private static final Set<String> ACT3_WEAK = new HashSet<>(Arrays.asList(
        "Jaw Worm Horde",
        "3 Darklings",
        "Orb Walker"
    ));

    // Act 3 (The Beyond) Strong Encounters
    private static final Set<String> ACT3_STRONG = new HashSet<>(Arrays.asList(
        "Spheric Guardian",
        "Maw",
        "Shapes",
        "Spire Growth",
        "Transient",
        "4 Shapes",
        "Spire Growth and Transient",
        "Writhing Mass",
        "Giant Head",
        "Nemesis",
        "Repulsor"
    ));

    /**
     * Check if current encounter is a "weak" encounter
     * @return true if weak encounter, false if strong or unknown
     */
    public static boolean isWeakEncounter() {
        String encounterKey = AbstractDungeon.lastCombatMetricKey;
        if (encounterKey == null || encounterKey.isEmpty()) {
            return false;
        }

        int actNum = AbstractDungeon.actNum;
        switch (actNum) {
            case 1:
                return ACT1_WEAK.contains(encounterKey);
            case 2:
                return ACT2_WEAK.contains(encounterKey);
            case 3:
                return ACT3_WEAK.contains(encounterKey);
            default:
                return false;
        }
    }

    /**
     * Check if current encounter is a "strong" encounter
     * @return true if strong encounter, false if weak or unknown
     */
    public static boolean isStrongEncounter() {
        String encounterKey = AbstractDungeon.lastCombatMetricKey;
        if (encounterKey == null || encounterKey.isEmpty()) {
            return false;
        }

        int actNum = AbstractDungeon.actNum;
        switch (actNum) {
            case 1:
                return ACT1_STRONG.contains(encounterKey);
            case 2:
                return ACT2_STRONG.contains(encounterKey);
            case 3:
                return ACT3_STRONG.contains(encounterKey);
            default:
                return false;
        }
    }

    /**
     * Get encounter type for logging
     * @return "weak", "strong", or "unknown"
     */
    public static String getEncounterType() {
        if (isWeakEncounter()) {
            return "weak";
        } else if (isStrongEncounter()) {
            return "strong";
        } else {
            return "unknown";
        }
    }
}
