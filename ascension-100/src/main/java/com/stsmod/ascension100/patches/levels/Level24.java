package com.stsmod.ascension100.patches.levels;

import com.badlogic.gdx.math.MathUtils;
import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;
import com.megacrit.cardcrawl.cards.DamageInfo;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Ascension Level 24: Normal enemies deal 10% more damage
 *
 * 일반 적들의 공격력이 10% 증가합니다.
 */
public class Level24 {
    private static final Logger logger = LogManager.getLogger(Level24.class.getName());

    @SpirePatch(
        clz = AbstractMonster.class,
        method = "usePreBattleAction"
    )
    public static class NormalDamageIncrease {
        @SpirePostfixPatch
        public static void Postfix(AbstractMonster __instance) {
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 24) {
                return;
            }

            if (__instance.type == AbstractMonster.EnemyType.NORMAL) {
                float multiplier = 1.1f;

                for (DamageInfo damageInfo : __instance.damage) {
                    if (damageInfo != null && damageInfo.base > 0) {
                        int originalDamage = damageInfo.base;
                        damageInfo.base = MathUtils.ceil(damageInfo.base * multiplier);

                        logger.info(String.format(
                            "Ascension 24: Normal %s damage increased from %d to %d",
                            __instance.name, originalDamage, damageInfo.base
                        ));
                    }
                }
            }
        }
    }
}
