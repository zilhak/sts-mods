package com.stsmod.ascension100.patches.levels;

import com.badlogic.gdx.math.MathUtils;
import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;
import com.megacrit.cardcrawl.cards.DamageInfo;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import com.megacrit.cardcrawl.monsters.ending.CorruptHeart;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Ascension Level 64: Boss damage increased
 *
 * 보스의 공격력이 증가합니다.
 * 보스(1~3막)의 공격력이 10% 증가합니다.
 * (타락한 심장 제외)
 */
public class Level64 {
    private static final Logger logger = LogManager.getLogger(Level64.class.getName());

    @SpirePatch(
        clz = AbstractMonster.class,
        method = "usePreBattleAction"
    )
    public static class BossDamageIncrease {
        @SpirePostfixPatch
        public static void Postfix(AbstractMonster __instance) {
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 64) {
                return;
            }

            // Only apply to Act 1-3 bosses, exclude Corrupt Heart
            if (__instance.type == AbstractMonster.EnemyType.BOSS &&
                !(__instance instanceof CorruptHeart)) {
                float multiplier = 1.1f;

                for (DamageInfo damageInfo : __instance.damage) {
                    if (damageInfo != null && damageInfo.base > 0) {
                        int originalDamage = damageInfo.base;
                        damageInfo.base = MathUtils.ceil(damageInfo.base * multiplier);

                        logger.info(String.format(
                            "Ascension 64: Boss %s damage increased from %d to %d",
                            __instance.name, originalDamage, damageInfo.base
                        ));
                    }
                }
            }
        }
    }
}
