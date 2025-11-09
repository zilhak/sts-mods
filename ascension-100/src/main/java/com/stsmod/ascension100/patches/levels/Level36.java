package com.stsmod.ascension100.patches.levels;

import com.badlogic.gdx.math.MathUtils;
import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Ascension Level 36: Boss stats adjusted
 *
 * 보스의 능력치가 조정됩니다.
 * 보스의 체력이 추가로 15% 증가합니다.
 * 보스의 공격력이 1 감소합니다.
 */
public class Level36 {
    private static final Logger logger = LogManager.getLogger(Level36.class.getName());

    @SpirePatch(
        clz = AbstractMonster.class,
        method = "init"
    )
    public static class BossHealthIncrease {
        @SpirePostfixPatch
        public static void Postfix(AbstractMonster __instance) {
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 36) {
                return;
            }

            if (__instance.type == AbstractMonster.EnemyType.BOSS) {
                int originalMaxHP = __instance.maxHealth;
                __instance.maxHealth = MathUtils.ceil(__instance.maxHealth * 1.15f);
                __instance.currentHealth = MathUtils.ceil(__instance.currentHealth * 1.15f);

                logger.info(String.format(
                    "Ascension 36: Boss %s HP increased from %d to %d",
                    __instance.name, originalMaxHP, __instance.maxHealth
                ));
            }
        }
    }

    @SpirePatch(
        clz = AbstractMonster.class,
        method = "usePreBattleAction"
    )
    public static class BossDamageDecrease {
        @SpirePostfixPatch
        public static void Postfix(AbstractMonster __instance) {
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 36) {
                return;
            }

            if (__instance.type == AbstractMonster.EnemyType.BOSS) {
                int damageDecrease = -1;

                for (int i = 0; i < __instance.damage.size(); i++) {
                    if (__instance.damage.get(i) != null && __instance.damage.get(i).base > 0) {
                        __instance.damage.get(i).base += damageDecrease;
                    }
                }

                logger.info(String.format(
                    "Ascension 36: Boss %s damage decreased by 1",
                    __instance.name
                ));
            }
        }
    }
}
