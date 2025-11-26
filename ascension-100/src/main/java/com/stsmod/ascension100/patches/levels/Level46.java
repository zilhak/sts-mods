package com.stsmod.ascension100.patches.levels;

import com.badlogic.gdx.math.MathUtils;
import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.relics.AbstractRelic;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Ascension Level 46: Higher relic price
 *
 * 상점의 유물 가격이 증가합니다.
 * 상점의 유물 가격이 10% 증가합니다.
 */
public class Level46 {
    private static final Logger logger = LogManager.getLogger(Level46.class.getName());

    @SpirePatch(
        clz = AbstractRelic.class,
        method = "getPrice"
    )
    public static class RelicPriceIncrease {
        @SpirePostfixPatch
        public static int Postfix(int __result) {
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 46) {
                return __result;
            }

            int originalPrice = __result;
            int newPrice = MathUtils.ceil(__result * 1.10f);

            logger.debug(String.format(
                "Ascension 46: Relic price increased from %d to %d (+10%%)",
                originalPrice, newPrice
            ));

            return newPrice;
        }
    }
}
