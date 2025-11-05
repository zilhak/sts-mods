package com.stsmod.ascension100.patches;

import com.badlogic.gdx.math.MathUtils;
import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Patches to modify monster health and damage for extended ascension levels
 *
 * Ascension 21: Elite enemies have 10% more HP
 * Ascension 22: Normal enemies have 10% more HP
 * Ascension 23: Elite enemies deal 10% more damage
 * Ascension 24: Normal enemies deal 10% more damage
 */
public class MonsterHealthPatch {

    private static final Logger logger = LogManager.getLogger(MonsterHealthPatch.class.getName());

    /**
     * Patch monster initialization to increase HP at Ascension 21-22
     */
    @SpirePatch(
        cls = "com.megacrit.cardcrawl.monsters.AbstractMonster",
        method = "init"
    )
    public static class HealthIncreasePatch {
        public static void Postfix(AbstractMonster __instance) {
            if (!AbstractDungeon.isAscensionMode) {
                return;
            }

            float hpMultiplier = getHPMultiplier(__instance);

            if (hpMultiplier > 1.0f) {
                int originalMaxHP = __instance.maxHealth;

                // Apply HP multiplier
                __instance.maxHealth = MathUtils.ceil(__instance.maxHealth * hpMultiplier);
                __instance.currentHealth = MathUtils.ceil(__instance.currentHealth * hpMultiplier);

                logger.info(String.format(
                    "Ascension %d: %s (%s) HP increased from %d to %d (x%.2f)",
                    AbstractDungeon.ascensionLevel,
                    __instance.name,
                    __instance.type,
                    originalMaxHP,
                    __instance.maxHealth,
                    hpMultiplier
                ));
            }
        }
    }

    /**
     * Calculate HP multiplier based on ascension level and monster type
     *
     * @param monster The monster instance
     * @return HP multiplier (1.0 = no change)
     */
    private static float getHPMultiplier(AbstractMonster monster) {
        int level = AbstractDungeon.ascensionLevel;
        float multiplier = 1.0f;

        // Ascension 21: Elite enemies +10% HP
        if (level >= 21 && monster.type == AbstractMonster.EnemyType.ELITE) {
            multiplier *= 1.1f;
        }

        // Ascension 22: Normal enemies +10% HP
        if (level >= 22 && monster.type == AbstractMonster.EnemyType.NORMAL) {
            multiplier *= 1.1f;
        }

        return multiplier;
    }
}
