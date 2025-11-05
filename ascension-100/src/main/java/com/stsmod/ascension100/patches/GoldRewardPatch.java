package com.stsmod.ascension100.patches;

import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.rewards.RewardItem;
import javassist.CtBehavior;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Patches to modify gold rewards for extended ascension levels
 *
 * Ascension 28: Gold rewards from combat are reduced by 10%
 */
public class GoldRewardPatch {

    private static final Logger logger = LogManager.getLogger(GoldRewardPatch.class.getName());

    /**
     * Patch gold reward constructor to reduce gold at Ascension 28+
     */
    @SpirePatch(
        cls = "com.megacrit.cardcrawl.rewards.RewardItem",
        method = SpirePatch.CONSTRUCTOR,
        paramtypez = { int.class }
    )
    public static class ReduceGoldReward {
        @SpirePostfixPatch
        public static void Postfix(RewardItem __instance, int gold) {
            if (!AbstractDungeon.isAscensionMode) {
                return;
            }

            // Ascension 28: Reduce gold rewards by 10%
            if (AbstractDungeon.ascensionLevel >= 28) {
                int originalGold = __instance.goldAmt;
                __instance.goldAmt = (int) Math.ceil(originalGold * 0.9f);

                logger.info(String.format(
                    "Ascension %d: Gold reward reduced from %d to %d (-10%%)",
                    AbstractDungeon.ascensionLevel,
                    originalGold,
                    __instance.goldAmt
                ));
            }
        }
    }

    /**
     * Also patch stolen gold constructor
     */
    @SpirePatch(
        cls = "com.megacrit.cardcrawl.rewards.RewardItem",
        method = SpirePatch.CONSTRUCTOR,
        paramtypez = { int.class, boolean.class }
    )
    public static class ReduceStolenGoldReward {
        @SpirePostfixPatch
        public static void Postfix(RewardItem __instance, int gold, boolean theft) {
            if (!AbstractDungeon.isAscensionMode) {
                return;
            }

            // Ascension 28: Reduce stolen gold rewards by 10%
            if (AbstractDungeon.ascensionLevel >= 28) {
                int originalGold = __instance.goldAmt;
                __instance.goldAmt = (int) Math.ceil(originalGold * 0.9f);

                logger.info(String.format(
                    "Ascension %d: Stolen gold reward reduced from %d to %d (-10%%)",
                    AbstractDungeon.ascensionLevel,
                    originalGold,
                    __instance.goldAmt
                ));
            }
        }
    }
}
