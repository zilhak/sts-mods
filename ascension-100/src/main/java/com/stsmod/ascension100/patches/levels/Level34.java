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
 * Ascension Level 34: Normal enemy HP increase
 * 일반 적들의 체력이 증가합니다.
 * - Weak Enemies: HP +1
 * - Strong Enemies: HP +3%
 */
public class Level34 {
    private static final Logger logger = LogManager.getLogger(Level34.class.getName());

    @SpirePatch(
        clz = AbstractMonster.class,
        method = "init"
    )
    public static class NormalHealthIncrease {
        @SpirePostfixPatch
        public static void Postfix(AbstractMonster __instance) {
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 34) {
                return;
            }

            // Skip bosses and elites
            if (__instance.type == AbstractMonster.EnemyType.BOSS ||
                __instance.type == AbstractMonster.EnemyType.ELITE) {
                return;
            }

            int originalMaxHP = __instance.maxHealth;

            // Check if this is a Strong Enemy encounter
            if (EncounterHelper.isStrongEncounter()) {
                // Strong Enemies: +3% HP
                __instance.maxHealth = MathUtils.ceil(__instance.maxHealth * 1.03f);
                __instance.currentHealth = MathUtils.ceil(__instance.currentHealth * 1.03f);

                logger.info(String.format(
                    "Ascension 34: Strong Enemy %s HP increased from %d to %d (+3%%)",
                    __instance.name, originalMaxHP, __instance.maxHealth
                ));
            } else {
                // Weak Enemies: +1 HP
                __instance.maxHealth += 1;
                __instance.currentHealth += 1;

                logger.info(String.format(
                    "Ascension 34: Weak Enemy %s HP increased from %d to %d (+1)",
                    __instance.name, originalMaxHP, __instance.maxHealth
                ));
            }
        }
    }
}
