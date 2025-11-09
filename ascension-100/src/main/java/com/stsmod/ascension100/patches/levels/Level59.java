package com.stsmod.ascension100.patches.levels;

import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Ascension Level 59: Elite HP increased even more
 *
 * 엘리트의 체력이 더더욱 증가합니다.
 * 모든 엘리트의 체력이 10 증가합니다.
 */
public class Level59 {
    private static final Logger logger = LogManager.getLogger(Level59.class.getName());

    @SpirePatch(
        clz = AbstractMonster.class,
        method = "init"
    )
    public static class EliteHealthBonus {
        @SpirePostfixPatch
        public static void Postfix(AbstractMonster __instance) {
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 59) {
                return;
            }

            if (__instance.type == AbstractMonster.EnemyType.ELITE) {
                int hpBonus = 10;
                int originalMaxHP = __instance.maxHealth;

                __instance.maxHealth += hpBonus;
                __instance.currentHealth += hpBonus;

                logger.info(String.format(
                    "Ascension 59: Elite %s HP increased from %d to %d (+%d flat)",
                    __instance.name, originalMaxHP, __instance.maxHealth, hpBonus
                ));
            }
        }
    }
}
