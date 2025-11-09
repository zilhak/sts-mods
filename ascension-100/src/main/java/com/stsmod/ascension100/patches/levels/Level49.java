package com.stsmod.ascension100.patches.levels;

import com.badlogic.gdx.math.MathUtils;
import com.evacipated.cardcrawl.modthespire.lib.*;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.rewards.RewardItem;
import javassist.CtBehavior;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Ascension Level 49: More gold, higher card price
 *
 * 얻는 돈이 늘어나며, 상점의 카드 가격이 증가합니다.
 * 얻는 돈이 30% 증가합니다.
 * 상점의 카드가격이 50% 증가합니다.
 */
public class Level49 {
    private static final Logger logger = LogManager.getLogger(Level49.class.getName());

    @SpirePatch(
        clz = RewardItem.class,
        method = SpirePatch.CONSTRUCTOR,
        paramtypez = {int.class}
    )
    public static class GoldRewardIncrease {
        @SpireInsertPatch(
            locator = GoldConstructorLocator.class
        )
        public static void Insert(RewardItem __instance, @ByRef int[] goldAmount) {
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 49) {
                return;
            }

            int originalGold = goldAmount[0];
            goldAmount[0] = MathUtils.ceil(goldAmount[0] * 1.30f);

            logger.info(String.format(
                "Ascension 49: Combat gold increased from %d to %d",
                originalGold, goldAmount[0]
            ));
        }

        private static class GoldConstructorLocator extends SpireInsertLocator {
            @Override
            public int[] Locate(CtBehavior ctBehavior) throws Exception {
                Matcher finalMatcher = new Matcher.FieldAccessMatcher(RewardItem.class, "goldAmt");
                return LineFinder.findInOrder(ctBehavior, finalMatcher);
            }
        }
    }

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
            int newPrice = MathUtils.ceil(__result * 1.5f);

            logger.debug(String.format(
                "Ascension 49: Card price increased from %d to %d",
                originalPrice, newPrice
            ));

            return newPrice;
        }
    }
}
