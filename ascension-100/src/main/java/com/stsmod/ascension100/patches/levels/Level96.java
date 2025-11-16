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
 * Ascension Level 96: Rest site probability decreased
 *
 * 휴식 장소가 줄어듭니다.
 *
 * 휴식 장소의 등장 확률이 감소합니다.
 */
public class Level96 {
    private static final Logger logger = LogManager.getLogger(Level96.class.getName());
    private static final float REST_CHANCE_DECREASE = 0.04F;

    /**
     * Patch Exordium (Act 1) rest room chance
     */
    @SpirePatch(
        clz = Exordium.class,
        method = "initializeLevelSpecificChances"
    )
    public static class DecreaseExordiumRestChance {
        @SpirePostfixPatch
        public static void Postfix(Exordium __instance) {
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 96) {
                return;
            }

            // Decrease rest room chance
            float originalChance = getRestRoomChance();
            setRestRoomChance(Math.max(0.02F, originalChance - REST_CHANCE_DECREASE));

            logger.info(String.format(
                "Ascension 96: Decreased Exordium rest chance from %.1f%% to %.1f%%",
                originalChance * 100,
                getRestRoomChance() * 100
            ));
        }
    }

    /**
     * Patch TheCity (Act 2) rest room chance
     */
    @SpirePatch(
        clz = TheCity.class,
        method = "initializeLevelSpecificChances"
    )
    public static class DecreaseCityRestChance {
        @SpirePostfixPatch
        public static void Postfix(TheCity __instance) {
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 96) {
                return;
            }

            // Decrease rest room chance
            float originalChance = getRestRoomChance();
            setRestRoomChance(Math.max(0.02F, originalChance - REST_CHANCE_DECREASE));

            logger.info(String.format(
                "Ascension 96: Decreased TheCity rest chance from %.1f%% to %.1f%%",
                originalChance * 100,
                getRestRoomChance() * 100
            ));
        }
    }

    /**
     * Patch TheBeyond (Act 3) rest room chance
     */
    @SpirePatch(
        clz = TheBeyond.class,
        method = "initializeLevelSpecificChances"
    )
    public static class DecreaseBeyondRestChance {
        @SpirePostfixPatch
        public static void Postfix(TheBeyond __instance) {
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 96) {
                return;
            }

            // Decrease rest room chance
            float originalChance = getRestRoomChance();
            setRestRoomChance(Math.max(0.02F, originalChance - REST_CHANCE_DECREASE));

            logger.info(String.format(
                "Ascension 96: Decreased TheBeyond rest chance from %.1f%% to %.1f%%",
                originalChance * 100,
                getRestRoomChance() * 100
            ));
        }
    }

    // Helper methods to access protected static fields in AbstractDungeon
    private static float getRestRoomChance() {
        try {
            java.lang.reflect.Field field = AbstractDungeon.class.getDeclaredField("restRoomChance");
            field.setAccessible(true);
            return field.getFloat(null);
        } catch (Exception e) {
            logger.error("Failed to get restRoomChance", e);
            return 0.12F;
        }
    }

    private static void setRestRoomChance(float value) {
        try {
            java.lang.reflect.Field field = AbstractDungeon.class.getDeclaredField("restRoomChance");
            field.setAccessible(true);
            field.setFloat(null, value);
        } catch (Exception e) {
            logger.error("Failed to set restRoomChance", e);
        }
    }
}
