package com.stsmod.ascension100.patches;

import com.badlogic.gdx.math.MathUtils;
import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Patches to modify monster health for extended ascension levels
 *
 * Ascension 21+: All enemies have 20% more HP
 */
public class MonsterHealthPatch {

    private static final Logger logger = LogManager.getLogger(MonsterHealthPatch.class.getName());

    /**
     * Patch monster initialization to increase HP at Ascension 21+
     */
    @SpirePatch(
        cls = "com.megacrit.cardcrawl.monsters.AbstractMonster",
        method = "init"
    )
    public static class HealthIncreasePatch {
        public static void Postfix(AbstractMonster __instance) {
            // Only apply if in ascension mode and level 21 or higher
            if (AbstractDungeon.isAscensionMode && AbstractDungeon.ascensionLevel >= 21) {
                // Calculate HP multiplier based on ascension level
                float hpMultiplier = getHPMultiplier(AbstractDungeon.ascensionLevel);

                if (hpMultiplier > 1.0f) {
                    // Store original HP for logging
                    int originalMaxHP = __instance.maxHealth;

                    // Apply HP multiplier
                    __instance.maxHealth = MathUtils.ceil(__instance.maxHealth * hpMultiplier);
                    __instance.currentHealth = MathUtils.ceil(__instance.currentHealth * hpMultiplier);

                    logger.info(String.format(
                        "Ascension %d: %s HP increased from %d to %d (x%.2f)",
                        AbstractDungeon.ascensionLevel,
                        __instance.name,
                        originalMaxHP,
                        __instance.maxHealth,
                        hpMultiplier
                    ));
                }
            }
        }
    }

    /**
     * Calculate HP multiplier based on ascension level
     *
     * @param ascensionLevel Current ascension level
     * @return HP multiplier (1.0 = no change)
     */
    private static float getHPMultiplier(int ascensionLevel) {
        if (ascensionLevel < 21) {
            return 1.0f;
        }

        // Ascension 21+: 20% HP increase (for testing)
        // Can be adjusted later for more complex scaling
        return 1.2f;
    }
}
