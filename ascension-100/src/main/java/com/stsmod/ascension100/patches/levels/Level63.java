package com.stsmod.ascension100.patches.levels;

import com.badlogic.gdx.math.MathUtils;
import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import com.megacrit.cardcrawl.monsters.ending.CorruptHeart;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Ascension Level 63: Boss HP increased
 *
 * 보스의 체력이 증가합니다.
 * 모든 보스(1~3막)의 체력이 8% 증가합니다.
 * (타락한 심장 제외)
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

            // Only apply to Act 1-3 bosses, exclude Corrupt Heart
            if (__instance.type == AbstractMonster.EnemyType.BOSS &&
                !(__instance instanceof CorruptHeart)) {
                int originalMaxHP = __instance.maxHealth;
                __instance.maxHealth = MathUtils.ceil(__instance.maxHealth * 1.08f);
                __instance.currentHealth = MathUtils.ceil(__instance.currentHealth * 1.08f);

                logger.info(String.format(
                    "Ascension 63: Boss %s HP increased from %d to %d (+8%%)",
                    __instance.name, originalMaxHP, __instance.maxHealth
                ));
            }
        }
    }
}
