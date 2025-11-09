package com.stsmod.ascension100.patches.levels;

import com.badlogic.gdx.math.MathUtils;
import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Ascension Level 63: Boss HP increased
 *
 * 보스의 체력이 증가합니다.
 * 보스의 체력이 20% 증가합니다.
 */
public class Level63 {
    private static final Logger logger = LogManager.getLogger(Level63.class.getName());

    @SpirePatch(
        clz = AbstractMonster.class,
        method = "init"
    )
    public static class BossHealthIncrease {
        @SpirePostfixPatch
        public static void Postfix(AbstractMonster __instance) {
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 63) {
                return;
            }

            if (__instance.type == AbstractMonster.EnemyType.BOSS) {
                int originalMaxHP = __instance.maxHealth;
                __instance.maxHealth = MathUtils.ceil(__instance.maxHealth * 1.2f);
                __instance.currentHealth = MathUtils.ceil(__instance.currentHealth * 1.2f);

                logger.info(String.format(
                    "Ascension 63: Boss %s HP increased from %d to %d",
                    __instance.name, originalMaxHP, __instance.maxHealth
                ));
            }
        }
    }
}
