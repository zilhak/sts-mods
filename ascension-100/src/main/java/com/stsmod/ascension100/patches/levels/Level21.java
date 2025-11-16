package com.stsmod.ascension100.patches.levels;

import com.badlogic.gdx.math.MathUtils;
import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Ascension Level 21: Elite enemies have 5% more HP
 * 엘리트 적들의 체력이 5% 증가합니다.
 */
public class Level21 {
    private static final Logger logger = LogManager.getLogger(Level21.class.getName());

    @SpirePatch(
        clz = AbstractMonster.class,
        method = "init"
    )
    public static class EliteHealthIncrease {
        @SpirePostfixPatch
        public static void Postfix(AbstractMonster __instance) {
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 21) {
                return;
            }

            if (__instance.type == AbstractMonster.EnemyType.ELITE) {
                int originalMaxHP = __instance.maxHealth;
                __instance.maxHealth = MathUtils.ceil(__instance.maxHealth * 1.05f);
                __instance.currentHealth = MathUtils.ceil(__instance.currentHealth * 1.05f);

                logger.info(String.format(
                    "Ascension 21: Elite %s HP increased from %d to %d (+5%%)",
                    __instance.name, originalMaxHP, __instance.maxHealth
                ));
            }
        }
    }
}
