package com.stsmod.ascension100.patches.levels;

import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Ascension Level 34: Elite enemies deal +2 damage
 *
 * 엘리트 적들의 공격이 추가로 2 증가합니다.
 */
public class Level34 {
    private static final Logger logger = LogManager.getLogger(Level34.class.getName());

    @SpirePatch(
        clz = AbstractMonster.class,
        method = "usePreBattleAction"
    )
    public static class EliteDamageIncrease {
        @SpirePostfixPatch
        public static void Postfix(AbstractMonster __instance) {
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 34) {
                return;
            }

            if (__instance.type == AbstractMonster.EnemyType.ELITE) {
                int damageIncrease = 2;

                for (int i = 0; i < __instance.damage.size(); i++) {
                    if (__instance.damage.get(i) != null && __instance.damage.get(i).base > 0) {
                        __instance.damage.get(i).base += damageIncrease;
                    }
                }

                logger.info(String.format(
                    "Ascension 34: Elite %s damage increased by %d",
                    __instance.name, damageIncrease
                ));
            }
        }
    }
}
