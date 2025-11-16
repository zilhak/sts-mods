package com.stsmod.ascension100.patches.levels;

import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.dungeons.Exordium;
import com.megacrit.cardcrawl.dungeons.TheCity;
import com.megacrit.cardcrawl.dungeons.TheBeyond;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Ascension Level 73: Shop rooms appear more frequently
 *
 * 상점이 더 자주 등장합니다.
 *
 * 상점 등장 확률이 4% 증가합니다. (5% → 9%, 약 2개 더 증가)
 */
public class Level73 {
    private static final Logger logger = LogManager.getLogger(Level73.class.getName());
    private static final float SHOP_CHANCE_INCREASE = 0.04F;

    /**
     * Patch Exordium (Act 1) shop room chance
     */
    @SpirePatch(
        clz = Exordium.class,
        method = "initializeLevelSpecificChances"
    )
    public static class IncreaseExordiumShopChance {
        @SpirePostfixPatch
        public static void Postfix(Exordium __instance) {
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 73) {
                return;
            }

            // Increase shop room chance from 5% to 9%
            float originalChance = getShopRoomChance();
            setShopRoomChance(originalChance + SHOP_CHANCE_INCREASE);

            logger.info(String.format(
                "Ascension 73: Increased Exordium shop chance from %.1f%% to %.1f%%",
                originalChance * 100,
                getShopRoomChance() * 100
            ));
        }
    }

    /**
     * Patch TheCity (Act 2) shop room chance
     */
    @SpirePatch(
        clz = TheCity.class,
        method = "initializeLevelSpecificChances"
    )
    public static class IncreaseCityShopChance {
        @SpirePostfixPatch
        public static void Postfix(TheCity __instance) {
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 73) {
                return;
            }

            // Increase shop room chance from 5% to 9%
            float originalChance = getShopRoomChance();
            setShopRoomChance(originalChance + SHOP_CHANCE_INCREASE);

            logger.info(String.format(
                "Ascension 73: Increased TheCity shop chance from %.1f%% to %.1f%%",
                originalChance * 100,
                getShopRoomChance() * 100
            ));
        }
    }

    /**
     * Patch TheBeyond (Act 3) shop room chance
     */
    @SpirePatch(
        clz = TheBeyond.class,
        method = "initializeLevelSpecificChances"
    )
    public static class IncreaseBeyondShopChance {
        @SpirePostfixPatch
        public static void Postfix(TheBeyond __instance) {
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 73) {
                return;
            }

            // Increase shop room chance from 5% to 9%
            float originalChance = getShopRoomChance();
            setShopRoomChance(originalChance + SHOP_CHANCE_INCREASE);

            logger.info(String.format(
                "Ascension 73: Increased TheBeyond shop chance from %.1f%% to %.1f%%",
                originalChance * 100,
                getShopRoomChance() * 100
            ));
        }
    }

    // Helper methods to access protected static fields in AbstractDungeon
    private static float getShopRoomChance() {
        try {
            java.lang.reflect.Field field = AbstractDungeon.class.getDeclaredField("shopRoomChance");
            field.setAccessible(true);
            return field.getFloat(null);
        } catch (Exception e) {
            logger.error("Failed to get shopRoomChance", e);
            return 0.05F;
        }
    }

    private static void setShopRoomChance(float value) {
        try {
            java.lang.reflect.Field field = AbstractDungeon.class.getDeclaredField("shopRoomChance");
            field.setAccessible(true);
            field.setFloat(null, value);
        } catch (Exception e) {
            logger.error("Failed to set shopRoomChance", e);
        }
    }
}
