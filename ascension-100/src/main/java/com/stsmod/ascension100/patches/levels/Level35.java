package com.stsmod.ascension100.patches.levels;

import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;
import com.megacrit.cardcrawl.actions.common.MakeTempCardInDiscardAction;
import com.megacrit.cardcrawl.cards.curses.AscendersBane;
import com.megacrit.cardcrawl.cards.curses.Clumsy;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.localization.CardStrings;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Ascension Level 35: Starting curse card modification
 *
 * 시작시 얻는 저주카드가 변경됩니다 (Level 35-79):
 * - 등반자의 골칫거리 (AscendersBane) 원래 속성 유지 (휘발성)
 * - 소멸시 버린 카드더미에 서투름 (Clumsy) 을 한장 넣습니다
 *
 * NOTE: Level 80에서 AscendersBane이 다시 변경됨 (소멸 속성으로)
 */
public class Level35 {
    private static final Logger logger = LogManager.getLogger(Level35.class.getName());

    /**
     * AscendersBane: Update description to show Clumsy effect
     * Keep original Ethereal property
     * Applied from Level 35-79 (Level 80+ changes AscendersBane differently)
     */
    @SpirePatch(
        clz = AscendersBane.class,
        method = SpirePatch.CONSTRUCTOR
    )
    public static class ModifyAscendersBane {
        @SpirePostfixPatch
        public static void Postfix(AscendersBane __instance) {
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 35 || AbstractDungeon.ascensionLevel >= 80) {
                return;
            }

            // Load localized description from CardStrings
            CardStrings cardStrings = CardCrawlGame.languagePack.getCardStrings("Ascension100:AscendersBaneLevel30");
            if (cardStrings != null && cardStrings.DESCRIPTION != null) {
                __instance.rawDescription = cardStrings.DESCRIPTION;
                __instance.initializeDescription();
                logger.info("Ascension 35: AscendersBane description updated (adds Clumsy on exhaust)");
            } else {
                logger.error("Failed to load CardStrings for AscendersBane Level 35");
            }
        }
    }

    /**
     * AscendersBane: Add Clumsy to discard pile on exhaust
     * Applied from Level 35-79 (Level 80+ changes AscendersBane differently)
     */
    @SpirePatch(
        clz = AscendersBane.class,
        method = "triggerOnExhaust"
    )
    public static class AddClumsyOnExhaust {
        @SpirePostfixPatch
        public static void Postfix(AscendersBane __instance) {
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 35 || AbstractDungeon.ascensionLevel >= 80) {
                return;
            }

            // Add one Clumsy to discard pile
            AbstractDungeon.actionManager.addToBottom(
                new MakeTempCardInDiscardAction(new Clumsy(), 1)
            );

            logger.info("Ascension 35: Added Clumsy to discard pile (AscendersBane exhausted)");
        }
    }
}
