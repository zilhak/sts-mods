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

    @SpirePatch(clz = AbstractMonster.class, method = "init")
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

            // Skip event combats (Colosseum, Masked Bandits, etc.)
            if (AbstractDungeon.getCurrRoom().combatEvent) {
                return;
            }

            // Only apply to normal monster rooms
            if (!(AbstractDungeon.getCurrRoom() instanceof com.megacrit.cardcrawl.rooms.MonsterRoom)) {
                return;
            }

            int originalMaxHP = __instance.maxHealth;
            int currentAct = AbstractDungeon.actNum;
            String logDetails = "";

            boolean isWeak = EncounterHelper.isWeakEncounter();
            boolean isStrong = EncounterHelper.isStrongEncounter();

            if (isWeak) {
                // Weak Enemies: +1 HP first, then apply act-based percentage
                __instance.maxHealth += 1;
                __instance.currentHealth += 1;
                logDetails = "+1 (Weak)";

                if (currentAct == 2) {
                    // Act 2: Additional +2% HP
                    __instance.maxHealth = MathUtils.ceil(__instance.maxHealth * 1.02f);
                    __instance.currentHealth = MathUtils.ceil(__instance.currentHealth * 1.02f);
                    logDetails += " +2% (Act 2)";
                } else if (currentAct == 3) {
                    // Act 3: Additional +5% HP
                    __instance.maxHealth = MathUtils.ceil(__instance.maxHealth * 1.05f);
                    __instance.currentHealth = MathUtils.ceil(__instance.currentHealth * 1.05f);
                    logDetails += " +5% (Act 3)";
                }
            } else if (isStrong) {
                // Strong Enemies: Add percentages together, not multiply
                float totalPercent = 0.03f; // Base 3%
                logDetails = "+3% (Strong)";

                if (currentAct == 2) {
                    // Act 2: +3% +2% = +5% total
                    totalPercent += 0.02f;
                    logDetails += " +2% (Act 2) = +5%";
                } else if (currentAct == 3) {
                    // Act 3: +3% +5% = +8% total
                    totalPercent += 0.05f;
                    logDetails += " +5% (Act 3) = +8%";
                }

                __instance.maxHealth = MathUtils.ceil(__instance.maxHealth * (1.0f + totalPercent));
                __instance.currentHealth = MathUtils.ceil(__instance.currentHealth * (1.0f + totalPercent));
            }

            if (isWeak || isStrong) {
                logger.info(String.format(
                        "Ascension 35: %s HP increased from %d to %d (%s)",
                        __instance.name, originalMaxHP, __instance.maxHealth, logDetails));
            }
        }
    }
}
