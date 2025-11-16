package com.stsmod.ascension100.patches.levels;

import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;
import com.megacrit.cardcrawl.cards.DamageInfo;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Ascension Level 68: Enemy damage increased by act
 *
 * 적들의 공격이 강화됩니다.
 * 1막의 적들의 공격이 1 증가합니다.
 * 2막의 적들의 공격이 2 증가합니다. (byrd 제외)
 * 3막의 적들의 공격이 5 증가합니다.
 */
public class Level68 {
    private static final Logger logger = LogManager.getLogger(Level68.class.getName());

    @SpirePatch(
        clz = AbstractMonster.class,
        method = "usePreBattleAction"
    )
    public static class DamageByActIncrease {
        @SpirePostfixPatch
        public static void Postfix(AbstractMonster __instance) {
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 68) {
                return;
            }

            // Skip byrd in Act 2 (handled by special Level 62 logic)
            if (AbstractDungeon.actNum == 2 && __instance.id != null && __instance.id.equals("Byrd")) {
                return;
            }

            int actNum = AbstractDungeon.actNum;
            int damageIncrease = 0;

            if (actNum == 1) {
                damageIncrease = 1;
            } else if (actNum == 2) {
                damageIncrease = 2;
            } else if (actNum >= 3) {
                damageIncrease = 5;
            }

            if (damageIncrease > 0) {
                for (DamageInfo damageInfo : __instance.damage) {
                    if (damageInfo != null && damageInfo.base > 0) {
                        damageInfo.base += damageIncrease;
                    }
                }

                logger.info(String.format(
                    "Ascension 68: %s (%s) damage increased by %d (Act %d)",
                    __instance.name, __instance.type, damageIncrease, actNum
                ));
            }
        }
    }
}
