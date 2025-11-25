package com.stsmod.ascension100.patches.levels;

import com.badlogic.gdx.math.MathUtils;
import com.evacipated.cardcrawl.modthespire.lib.*;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.relics.AbstractRelic;
import com.megacrit.cardcrawl.rewards.RewardItem;
import javassist.CtBehavior;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Ascension Level 46: More gold, higher relic price
 *
 * 얻는 돈이 늘어나며, 상점의 유물 가격이 증가합니다.
 * 전투 종료시 얻는 돈이 20% 증가합니다. (올림 계산)
 * 상점의 유물 가격이 20% 증가합니다.
 */
public class Level46 {
    private static final Logger logger = LogManager.getLogger(Level46.class.getName());

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
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 46) {
                return;
            }

            int originalGold = goldAmount[0];
            goldAmount[0] = MathUtils.ceil(goldAmount[0] * 1.20f);

            logger.info(String.format(
                "Ascension 46: Combat gold increased from %d to %d",
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
            int newPrice = MathUtils.ceil(__result * 1.20f);

            logger.debug(String.format(
                "Ascension 46: Relic price increased from %d to %d (+20%%)",
                originalPrice, newPrice
            ));

            return newPrice;
        }
    }
}
