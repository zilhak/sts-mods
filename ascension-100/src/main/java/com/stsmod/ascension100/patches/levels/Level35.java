package com.stsmod.ascension100.patches.levels;

import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Ascension Level 35: Normal enemies deal +1 damage
 *
 * 일반 적들의 공격이 추가로 1 증가합니다.
 */
public class Level35 {
    private static final Logger logger = LogManager.getLogger(Level35.class.getName());

    @SpirePatch(
        clz = AbstractMonster.class,
        method = "usePreBattleAction"
    )
    public static class NormalDamageIncrease {
        @SpirePostfixPatch
        public static void Postfix(AbstractMonster __instance) {
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 35) {
                return;
            }

            if (__instance.type == AbstractMonster.EnemyType.NORMAL) {
                int damageIncrease = 1;

                for (int i = 0; i < __instance.damage.size(); i++) {
                    if (__instance.damage.get(i) != null && __instance.damage.get(i).base > 0) {
                        __instance.damage.get(i).base += damageIncrease;
                    }
                }

                logger.info(String.format(
                    "Ascension 35: Normal %s damage increased by %d",
                    __instance.name, damageIncrease
                ));
            }
        }
    }
}
