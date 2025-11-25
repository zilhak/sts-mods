package com.stsmod.ascension100.patches.levels;

import com.badlogic.gdx.math.MathUtils;
import com.evacipated.cardcrawl.modthespire.lib.*;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.rewards.RewardItem;
import com.megacrit.cardcrawl.shop.ShopScreen;
import javassist.CtBehavior;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Ascension Level 42: More gold, higher card removal cost
 *
 * 얻는 돈이 늘어나며, 상점의 카드 제거 비용이 증가합니다.
 * 전투 종료시 얻는 돈이 8% 증가합니다.
 * 상점의 카드 제거 초기 비용이 25 증가합니다.
 */
public class Level42 {
    private static final Logger logger = LogManager.getLogger(Level42.class.getName());

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
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 42) {
                return;
            }

            int originalGold = goldAmount[0];
            goldAmount[0] = MathUtils.ceil(goldAmount[0] * 1.08f);

            logger.info(String.format(
                "Ascension 42: Combat gold increased from %d to %d (+8%%)",
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
