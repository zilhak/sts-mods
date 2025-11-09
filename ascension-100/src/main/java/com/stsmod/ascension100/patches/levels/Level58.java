package com.stsmod.ascension100.patches.levels;

import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;
import com.megacrit.cardcrawl.cards.DamageInfo;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Ascension Level 58: Enemies deal more damage
 *
 * 적들의 공격이 강화됩니다.
 * 적들의 공격이 1 증가합니다.
 */
public class Level58 {
    private static final Logger logger = LogManager.getLogger(Level58.class.getName());

    @SpirePatch(
        clz = AbstractMonster.class,
        method = "usePreBattleAction"
    )
    public static class AllEnemiesDamageIncrease {
        @SpirePostfixPatch
        public static void Postfix(AbstractMonster __instance) {
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 58) {
                return;
            }

            int damageIncrease = 1;

            for (DamageInfo damageInfo : __instance.damage) {
                if (damageInfo != null && damageInfo.base > 0) {
                    damageInfo.base += damageIncrease;
                }
            }

            logger.info(String.format(
                "Ascension 58: %s (%s) damage increased by %d",
                __instance.name, __instance.type, damageIncrease
            ));
        }
    }
}
