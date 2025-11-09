package com.stsmod.ascension100.patches.levels;

import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.potions.AbstractPotion;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Ascension Level 37: Potion price decreases and effect decreases
 *
 * 포션의 가격이 하락하고, 효과가 감소합니다.
 *
 * 포션의 가격이 40% 감소합니다.
 * 포션의 효과가 20% 감소합니다. (올림 계산)
 */
public class Level37 {
    private static final Logger logger = LogManager.getLogger(Level37.class.getName());

    /**
     * Potion price reduction: -40%
     */
    @SpirePatch(
        clz = AbstractPotion.class,
        method = "getPrice"
    )
    public static class PotionPriceReduction {
        @SpirePostfixPatch
        public static int Postfix(int __result) {
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 37) {
                return __result;
            }

            // Reduce price by 40%
            int reducedPrice = (int) Math.ceil(__result * 0.6f);

            logger.debug(String.format(
                "Ascension 37: Potion price reduced from %d to %d (-40%%)",
                __result, reducedPrice
            ));

            return reducedPrice;
        }
    }

    /**
     * Potion potency reduction: -20%
     *
     * We use paramtypez = {} (empty array) to explicitly specify
     * the no-parameter version of getPotency() to avoid overload ambiguity.
     */
    @SpirePatch(
        clz = AbstractPotion.class,
        method = "getPotency",
        paramtypez = {}
    )
    public static class PotionPotencyReduction {
        @SpirePostfixPatch
        public static int Postfix(int __result) {
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 37) {
                return __result;
            }

            // Reduce potency by 20% (round up)
            int reducedPotency = (int) Math.ceil(__result * 0.8f);

            logger.debug(String.format(
                "Ascension 37: Potion potency reduced from %d to %d (-20%%)",
                __result, reducedPotency
            ));

            return reducedPotency;
        }
    }
}
