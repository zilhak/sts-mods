package com.stsmod.ascension100.patches.levels;

import com.badlogic.gdx.math.MathUtils;
import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import com.stsmod.ascension100.util.EncounterHelper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Ascension Level 35: Normal enemy HP increase
 * 일반 적들의 체력이 증가합니다.
 * - Weak Enemies: HP +1
 * - Strong Enemies: HP +3%
 * - Act 2: Additional +2% HP
 * - Act 3: Additional +5% HP
 */
public class Level35 {
    private static final Logger logger = LogManager.getLogger(Level35.class.getName());

    @SpirePatch(
        clz = AbstractMonster.class,
        method = "init"
    )
    public static class NormalHealthIncrease {
        @SpirePostfixPatch
        public static void Postfix(AbstractMonster __instance) {
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 35) {
                return;
            }

            // Skip bosses and elites
            if (__instance.type == AbstractMonster.EnemyType.BOSS ||
                __instance.type == AbstractMonster.EnemyType.ELITE) {
                return;
            }

            // Skip minions in elite encounters (Gremlin Leader minions, Reptomancer daggers, etc.)
            if (AbstractDungeon.getMonsters() != null) {
                for (AbstractMonster m : AbstractDungeon.getMonsters().monsters) {
                    if (m.type == AbstractMonster.EnemyType.ELITE) {
                        // Elite encounter detected, skip all normal monsters
                        return;
                    }
                }
            }

            int originalMaxHP = __instance.maxHealth;
            float totalMultiplier = 1.0f;
            String logDetails = "";

            // Check if this is a Strong Enemy encounter
            if (EncounterHelper.isStrongEncounter()) {
                // Strong Enemies: +3% HP
                totalMultiplier *= 1.03f;
                logDetails = "+3% (Strong)";
            } else {
                // Weak Enemies: +1 HP (apply flat increase after multipliers)
                logDetails = "+1 (Weak)";
            }

            // Act-based additional HP increase
            int currentAct = AbstractDungeon.actNum;
            if (currentAct == 2) {
                // Act 2: Additional +2% HP
                totalMultiplier *= 1.02f;
                logDetails += " +2% (Act 2)";
            } else if (currentAct == 3) {
                // Act 3: Additional +5% HP
                totalMultiplier *= 1.05f;
                logDetails += " +5% (Act 3)";
            }

            // Apply multipliers first
            if (totalMultiplier > 1.0f) {
                __instance.maxHealth = MathUtils.ceil(__instance.maxHealth * totalMultiplier);
                __instance.currentHealth = MathUtils.ceil(__instance.currentHealth * totalMultiplier);
            }

            // Apply flat increase for Weak Enemies
            if (!EncounterHelper.isStrongEncounter()) {
                __instance.maxHealth += 1;
                __instance.currentHealth += 1;
            }

            logger.info(String.format(
                "Ascension 35: %s HP increased from %d to %d (%s)",
                __instance.name, originalMaxHP, __instance.maxHealth, logDetails
            ));
        }
    }
}
