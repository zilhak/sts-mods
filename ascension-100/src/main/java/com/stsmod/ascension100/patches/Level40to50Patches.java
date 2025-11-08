package com.stsmod.ascension100.patches;

import com.badlogic.gdx.math.MathUtils;
import com.evacipated.cardcrawl.modthespire.lib.*;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.relics.AbstractRelic;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.rewards.RewardItem;
import com.megacrit.cardcrawl.shop.ShopScreen;
import javassist.CtBehavior;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Patches for Ascension Levels 40-50
 *
 * Level 40: No effect (intentionally empty)
 * Level 41: 10% of ? rooms become elite encounters (NOT IMPLEMENTED - requires map generation research)
 * Level 42: +25% gold from combat, +25 card removal cost
 * Level 43: Wheel event condition changes (NOT IMPLEMENTED - requires event research)
 * Level 44: Looter/Mugger steal +10 more gold (NOT IMPLEMENTED - requires monster class access)
 * Level 45: Enemy composition changes (NOT IMPLEMENTED - requires encounter research)
 * Level 46: +20% gold from combat, +15% relic price
 * Level 47: Rest reduces max HP by 2 (NOT IMPLEMENTED - requires campfire research)
 * Level 48: Neow penalty +10% (NOT IMPLEMENTED - requires event research)
 * Level 49: +30% gold from combat, +50% card price
 * Level 50: Act 4 forced with penalties (NOT IMPLEMENTED - requires game flow research)
 */
public class Level40to50Patches {

    private static final Logger logger = LogManager.getLogger(Level40to50Patches.class.getName());

    // ========================================
    // LEVEL 40: NO EFFECT (INTENTIONALLY EMPTY)
    // ========================================

    // ========================================
    // LEVEL 42, 46, 49: GOLD INCREASES
    // ========================================

    /**
     * Level 42: +25% combat gold
     * Level 46: +20% combat gold (stacks with 42)
     * Level 49: +30% combat gold (stacks with 42 and 46)
     *
     * Total at level 49: x1.25 x1.20 x1.30 = x1.95 (95% increase)
     */
    @SpirePatch(
            clz = RewardItem.class,
            method = SpirePatch.CONSTRUCTOR,
            paramtypez = {int.class}
    )
    public static class GoldRewardIncreasePatch {
        @SpireInsertPatch(
                locator = GoldConstructorLocator.class
        )
        public static void Insert(RewardItem __instance, @ByRef int[] goldAmount) {
            if (!AbstractDungeon.isAscensionMode) {
                return;
            }

            int level = AbstractDungeon.ascensionLevel;
            float multiplier = 1.0f;

            // Level 42: +25%
            if (level >= 42) {
                multiplier *= 1.25f;
            }

            // Level 46: +20%
            if (level >= 46) {
                multiplier *= 1.20f;
            }

            // Level 49: +30%
            if (level >= 49) {
                multiplier *= 1.30f;
            }

            if (multiplier > 1.0f) {
                int originalGold = goldAmount[0];
                goldAmount[0] = MathUtils.ceil(goldAmount[0] * multiplier);

                logger.info(String.format(
                        "Ascension %d: Combat gold increased from %d to %d (x%.2f)",
                        level, originalGold, goldAmount[0], multiplier
                ));
            }
        }

        private static class GoldConstructorLocator extends SpireInsertLocator {
            @Override
            public int[] Locate(CtBehavior ctBehavior) throws Exception {
                Matcher finalMatcher = new Matcher.FieldAccessMatcher(RewardItem.class, "goldAmt");
                return LineFinder.findInOrder(ctBehavior, finalMatcher);
            }
        }
    }

    // ========================================
    // LEVEL 42: CARD REMOVAL COST +25
    // ========================================

    /**
     * Level 42: Card removal cost increased by 25 gold
     */
    @SpirePatch(
            clz = ShopScreen.class,
            method = "init"
    )
    public static class CardRemovalCostPatch {
        @SpirePostfixPatch
        public static void Postfix(ShopScreen __instance) {
            if (!AbstractDungeon.isAscensionMode) {
                return;
            }

            int level = AbstractDungeon.ascensionLevel;

            if (level >= 42) {
                // ShopScreen.purgeCost is static
                int originalCost = ShopScreen.purgeCost;
                ShopScreen.purgeCost += 25;

                logger.info(String.format(
                        "Ascension %d: Card removal cost increased from %d to %d",
                        level, originalCost, ShopScreen.purgeCost
                ));
            }
        }
    }

    // ========================================
    // LEVEL 44: LOOTER/MUGGER STEAL +10 GOLD
    // ========================================
    // TODO: Requires access to monster classes or alternative implementation
    // Options:
    // 1. Patch monster constructors using string class names (requires @SpirePatch2)
    // 2. Patch AbstractPlayer.loseGold() and detect thief context
    // 3. Patch DamageAction with goldAmt parameter
    // Currently skipped - implement in future update

    // ========================================
    // LEVEL 47: REST REDUCES MAX HP BY 2
    // ========================================

    /**
     * Level 47: Resting reduces max HP by 2
     */
    @SpirePatch(
            clz = com.megacrit.cardcrawl.ui.campfire.RestOption.class,
            method = "useOption"
    )
    public static class RestMaxHPPenaltyPatch {
        @SpirePostfixPatch
        public static void Postfix(Object __instance) {
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 47) {
                return;
            }

            // Decrease max HP by 2
            AbstractDungeon.player.decreaseMaxHealth(2);

            logger.info(String.format(
                    "Ascension %d: Max HP reduced by 2 from resting (new max: %d)",
                    AbstractDungeon.ascensionLevel, AbstractDungeon.player.maxHealth
            ));
        }
    }

    // ========================================
    // LEVEL 46: RELIC PRICE +15%
    // ========================================

    /**
     * Level 46: Relic price increased by 15%
     */
    @SpirePatch(
            clz = AbstractRelic.class,
            method = "getPrice"
    )
    public static class RelicPriceIncreasePatch {
        @SpirePostfixPatch
        public static int Postfix(int __result) {
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 46) {
                return __result;
            }

            int originalPrice = __result;
            int newPrice = MathUtils.ceil(__result * 1.15f);

            logger.debug(String.format(
                    "Ascension %d: Relic price increased from %d to %d",
                    AbstractDungeon.ascensionLevel, originalPrice, newPrice
            ));

            return newPrice;
        }
    }

    // ========================================
    // LEVEL 49: CARD PRICE +50%
    // ========================================

    /**
     * Level 49: Card price increased by 50%
     */
    @SpirePatch(
            clz = AbstractCard.class,
            method = "getPrice"
    )
    public static class CardPriceIncreasePatch {
        @SpirePostfixPatch
        public static int Postfix(int __result) {
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 49) {
                return __result;
            }

            int originalPrice = __result;
            int newPrice = MathUtils.ceil(__result * 1.5f);

            logger.debug(String.format(
                    "Ascension %d: Card price increased from %d to %d",
                    AbstractDungeon.ascensionLevel, originalPrice, newPrice
            ));

            return newPrice;
        }
    }
}
