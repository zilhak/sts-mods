package com.stsmod.ascension100.patches.levels;

import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.shop.ShopScreen;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Ascension Level 42: Higher card removal cost
 *
 * 상점의 카드 제거 비용이 증가합니다.
 * 상점의 카드 제거 초기 비용이 25 증가합니다.
 */
public class Level42 {
    private static final Logger logger = LogManager.getLogger(Level42.class.getName());

    @SpirePatch(
        clz = ShopScreen.class,
        method = "init"
    )
    public static class CardRemovalCostIncrease {
        @SpirePostfixPatch
        public static void Postfix(ShopScreen __instance) {
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 42) {
                return;
            }

            int originalCost = ShopScreen.purgeCost;
            ShopScreen.purgeCost += 25;

            logger.info(String.format(
                "Ascension 42: Card removal cost increased from %d to %d",
                originalCost, ShopScreen.purgeCost
            ));
        }
    }
}
