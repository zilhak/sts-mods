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
 * Ascension Level 67: Enemies have more HP by act and encounter type
 *
 * 적들의 체력이 증가합니다.
 * 1막의 strong enemies 전투에서 적들의 체력이 5% 증가합니다.
 * 2막의 weak enemies 전투에서 적들의 체력이 5% 증가합니다. strong enemies 전투에서 적들의 체력이 8% 증가합니다.
 * 3막의 weak enemies 전투에서 적들의 체력이 8% 증가합니다. strong enemies 전투에서 적들의 체력이 10% 증가합니다.
 */
public class Level67 {
    private static final Logger logger = LogManager.getLogger(Level67.class.getName());

    @SpirePatch(
        clz = AbstractMonster.class,
        method = "init"
    )
    public static class EnemiesHealthByActIncrease {
        @SpirePostfixPatch
        public static void Postfix(AbstractMonster __instance) {
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 67) {
                return;
            }

            // Skip bosses and elites
            if (__instance.type == AbstractMonster.EnemyType.BOSS ||
                __instance.type == AbstractMonster.EnemyType.ELITE) {
                return;
            }

            int actNum = AbstractDungeon.actNum;
            float multiplier = 1.0f;

            boolean isWeak = EncounterHelper.isWeakEncounter();
            boolean isStrong = EncounterHelper.isStrongEncounter();

            if (actNum == 1) {
                // Act 1: Only strong enemies get +5%
                if (isStrong) {
                    multiplier = 1.05f;
                }
            } else if (actNum == 2) {
                // Act 2: Weak +5%, Strong +8%
                if (isWeak) {
                    multiplier = 1.05f;
                } else if (isStrong) {
                    multiplier = 1.08f;
                }
            } else if (actNum >= 3) {
                // Act 3: Weak +8%, Strong +10%
                if (isWeak) {
                    multiplier = 1.08f;
                } else if (isStrong) {
                    multiplier = 1.10f;
                }
            }

            if (multiplier > 1.0f) {
                int originalMaxHP = __instance.maxHealth;
                __instance.maxHealth = MathUtils.ceil(__instance.maxHealth * multiplier);
                __instance.currentHealth = MathUtils.ceil(__instance.currentHealth * multiplier);

                logger.info(String.format(
                    "Ascension 67: %s (Act %d, %s, encounter: %s) HP increased from %d to %d (%.0f%%)",
                    __instance.name, actNum, EncounterHelper.getEncounterType(),
                    AbstractDungeon.lastCombatMetricKey,
                    originalMaxHP, __instance.maxHealth, (multiplier - 1.0f) * 100
                ));
            }
        }
    }
}
