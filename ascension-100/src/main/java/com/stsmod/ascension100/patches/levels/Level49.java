package com.stsmod.ascension100.patches.levels;

import com.badlogic.gdx.math.MathUtils;
import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Ascension Level 49: Higher card price
 *
 * 상점의 카드 가격이 증가합니다.
 * 상점의 카드가격이 10% 증가합니다.
 */
public class Level49 {
    private static final Logger logger = LogManager.getLogger(Level49.class.getName());

    @SpirePatch(
        clz = AbstractCard.class,
        method = "getPrice"
    )
    public static class CardPriceIncrease {
        @SpirePostfixPatch
        public static int Postfix(int __result) {
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 49) {
                return __result;
            }

            int originalPrice = __result;
            int newPrice = MathUtils.ceil(__result * 1.10f);

            logger.debug(String.format(
                "Ascension 49: Card price increased from %d to %d (+10%%)",
                originalPrice, newPrice
            ));

            return newPrice;
        }
    }
}
