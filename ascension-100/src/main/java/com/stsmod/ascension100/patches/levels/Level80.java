package com.stsmod.ascension100.patches.levels;

import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.cards.curses.AscendersBane;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Ascension Level 80: Starting curse is modified
 *
 * 시작 시 얻는 저주카드가 변경됩니다.
 *
 * 시작시 얻는 "등반자의 골칫거리" 가 강화됩니다.
 * 강화된 등반자의 골칫거리는 1코스트, 사용가능 저주카드입니다. (사용시 소멸)
 */
public class Level80 {
    private static final Logger logger = LogManager.getLogger(Level80.class.getName());

    /**
     * Patch AscendersBane constructor to modify it at Ascension 80+
     */
    @SpirePatch(
        clz = AscendersBane.class,
        method = SpirePatch.CONSTRUCTOR
    )
    public static class ModifyAscendersBane {
        @SpirePostfixPatch
        public static void Postfix(AscendersBane __instance) {
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 80) {
                return;
            }

            // Change cost from -2 (unplayable) to 1
            __instance.cost = 1;
            __instance.costForTurn = 1;

            // Remove Ethereal (so it doesn't disappear at end of turn)
            __instance.isEthereal = false;

            // Make it Exhaust when played (only disappears when used)
            __instance.exhaust = true;

            // Reinitialize description - exhaust keyword will be added automatically
            __instance.initializeDescription();

            logger.info("Ascension 80: Modified AscendersBane - Cost: 1, Exhaust (removed Ethereal)");
        }
    }
}
