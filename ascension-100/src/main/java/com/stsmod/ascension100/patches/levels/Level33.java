package com.stsmod.ascension100.patches.levels;

import com.badlogic.gdx.math.MathUtils;
import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Ascension Level 33: Normal enemies have additional 10% more HP
 *
 * 일반 적들의 체력이 추가로 10% 증가합니다.
 */
public class Level33 {
    private static final Logger logger = LogManager.getLogger(Level33.class.getName());

    @SpirePatch(
        clz = AbstractMonster.class,
        method = "init"
    )
    public static class NormalHealthIncrease {
        @SpirePostfixPatch
        public static void Postfix(AbstractMonster __instance) {
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 33) {
                return;
            }

            if (__instance.type == AbstractMonster.EnemyType.NORMAL) {
                int originalMaxHP = __instance.maxHealth;
                __instance.maxHealth = MathUtils.ceil(__instance.maxHealth * 1.1f);
                __instance.currentHealth = MathUtils.ceil(__instance.currentHealth * 1.1f);

                logger.info(String.format(
                    "Ascension 33: Normal %s HP increased from %d to %d",
                    __instance.name, originalMaxHP, __instance.maxHealth
                ));
            }
        }
    }
}
