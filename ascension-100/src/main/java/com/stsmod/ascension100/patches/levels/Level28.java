package com.stsmod.ascension100.patches.levels;

import com.badlogic.gdx.math.MathUtils;
import com.evacipated.cardcrawl.modthespire.lib.*;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.rewards.RewardItem;
import javassist.CtBehavior;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Ascension Level 28: Gold from combat decreased
 *
 * 전투에서 획득하는 금액이 10% 감소합니다.
 */
public class Level28 {
    private static final Logger logger = LogManager.getLogger(Level28.class.getName());

    /**
     * Reduce combat gold rewards by 10%
     */
    @SpirePatch(
        clz = RewardItem.class,
        method = SpirePatch.CONSTRUCTOR,
        paramtypez = {int.class}
    )
    public static class GoldRewardDecrease {
        @SpireInsertPatch(
            locator = GoldConstructorLocator.class
        )
        public static void Insert(RewardItem __instance, @ByRef int[] goldAmount) {
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 28) {
                return;
            }

            int originalGold = goldAmount[0];
            goldAmount[0] = MathUtils.floor(goldAmount[0] * 0.9f);

            logger.info(String.format(
                "Ascension 28: Combat gold reduced from %d to %d (-10%%)",
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
}
