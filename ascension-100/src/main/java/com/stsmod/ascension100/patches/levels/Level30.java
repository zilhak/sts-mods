package com.stsmod.ascension100.patches.levels;

import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.dungeons.Exordium;
import com.megacrit.cardcrawl.dungeons.TheCity;
import com.megacrit.cardcrawl.dungeons.TheBeyond;
import com.megacrit.cardcrawl.dungeons.TheEnding;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Ascension Level 30: Luck decreased - Relic probability adjusted
 * 운이 감소합니다.
 *
 * 상세효과:
 * 유물 확률 조정
 * - 일반 50% (변화 없음)
 * - 고급 35% (33% → 35%)
 * - 희귀 15% (17% → 15%)
 *
 * 숨겨진 효과:
 * - 와처의 "추월(Rushdown)" 카드가 카드풀에서 제거됩니다.
 */
public class Level30 {

    private static final Logger logger = LogManager.getLogger(Level30.class.getName());
    // Rushdown card's actual ID in the game code is "Adaptation" (not "Rushdown")
    private static final String RUSHDOWN_CARD_ID = "Adaptation";

    /**
     * Remove Rushdown card from card pools
     * This is a hidden change not documented publicly
     *
     * Note: The card's display name is "Rushdown", but the internal ID is "Adaptation"
     */
    private static void removeRushdownFromCardPools() {
        if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 30) {
            return;
        }

        try {
            // Remove from both source and current uncommon card pools
            boolean removedFromSrc = AbstractDungeon.srcUncommonCardPool.removeCard(RUSHDOWN_CARD_ID);
            boolean removedFromCurrent = AbstractDungeon.uncommonCardPool.removeCard(RUSHDOWN_CARD_ID);

            if (removedFromSrc || removedFromCurrent) {
                logger.info("Ascension 30: Rushdown card (ID: " + RUSHDOWN_CARD_ID + ") removed from card pools (hidden change)");
            } else {
                logger.warn("Ascension 30: Rushdown card not found in pools");
            }
        } catch (Exception e) {
            logger.error("Failed to remove Rushdown card from pools", e);
        }
    }

    /**
     * Adjust relic probabilities for all dungeons
     */
    private static void adjustRelicProbabilities() {
        if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 30) {
            return;
        }

        try {
            // Access the protected static fields via reflection
            java.lang.reflect.Field commonField = AbstractDungeon.class.getDeclaredField("commonRelicChance");
            java.lang.reflect.Field uncommonField = AbstractDungeon.class.getDeclaredField("uncommonRelicChance");
            java.lang.reflect.Field rareField = AbstractDungeon.class.getDeclaredField("rareRelicChance");

            commonField.setAccessible(true);
            uncommonField.setAccessible(true);
            rareField.setAccessible(true);

            // Set new probabilities
            // Common: 50% (unchanged)
            // Uncommon: 35% (from 33%)
            // Rare: 15% (from 17%)
            commonField.setInt(null, 50);
            uncommonField.setInt(null, 35);
            rareField.setInt(null, 15);

            logger.info("Ascension 30: Relic probabilities adjusted to Common 50%, Uncommon 35%, Rare 15%");
        } catch (Exception e) {
            logger.error("Failed to adjust relic probabilities", e);
        }
    }

    /**
     * Patch AbstractDungeon.initializeCardPools() to remove Rushdown card
     */
    @SpirePatch(
        clz = AbstractDungeon.class,
        method = "initializeCardPools"
    )
    public static class RemoveRushdownPatch {
        @SpirePostfixPatch
        public static void Postfix() {
            removeRushdownFromCardPools();
        }
    }

    /**
     * Patch Exordium dungeon initialization
     */
    @SpirePatch(
        clz = Exordium.class,
        method = "initializeLevelSpecificChances"
    )
    public static class ExordiumRelicProbabilityPatch {
        @SpirePostfixPatch
        public static void Postfix(Exordium __instance) {
            adjustRelicProbabilities();
        }
    }

    /**
     * Patch TheCity dungeon initialization
     */
    @SpirePatch(
        clz = TheCity.class,
        method = "initializeLevelSpecificChances"
    )
    public static class TheCityRelicProbabilityPatch {
        @SpirePostfixPatch
        public static void Postfix(TheCity __instance) {
            adjustRelicProbabilities();
        }
    }

    /**
     * Patch TheBeyond dungeon initialization
     */
    @SpirePatch(
        clz = TheBeyond.class,
        method = "initializeLevelSpecificChances"
    )
    public static class TheBeyondRelicProbabilityPatch {
        @SpirePostfixPatch
        public static void Postfix(TheBeyond __instance) {
            adjustRelicProbabilities();
        }
    }

    /**
     * Patch TheEnding dungeon initialization
     */
    @SpirePatch(
        clz = TheEnding.class,
        method = "initializeLevelSpecificChances"
    )
    public static class TheEndingRelicProbabilityPatch {
        @SpirePostfixPatch
        public static void Postfix(TheEnding __instance) {
            adjustRelicProbabilities();
        }
    }
}
