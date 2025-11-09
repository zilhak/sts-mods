package com.stsmod.ascension100.patches.levels;

import com.badlogic.gdx.math.MathUtils;
import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Ascension Level 32: Elite enemies have additional 10% more HP
 *
 * 엘리트 적들의 체력이 추가로 10% 증가합니다.
 */
public class Level32 {
    private static final Logger logger = LogManager.getLogger(Level32.class.getName());

    @SpirePatch(
        clz = AbstractMonster.class,
        method = "init"
    )
    public static class EliteHealthIncrease {
        @SpirePostfixPatch
        public static void Postfix(AbstractMonster __instance) {
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 32) {
                return;
            }

            if (__instance.type == AbstractMonster.EnemyType.ELITE) {
                int originalMaxHP = __instance.maxHealth;
                __instance.maxHealth = MathUtils.ceil(__instance.maxHealth * 1.1f);
                __instance.currentHealth = MathUtils.ceil(__instance.currentHealth * 1.1f);

                logger.info(String.format(
                    "Ascension 32: Elite %s HP increased from %d to %d",
                    __instance.name, originalMaxHP, __instance.maxHealth
                ));
            }
        }
    }
}
