package com.stsmod.ascension100.patches.levels;

import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePrefixPatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.monsters.city.Mugger;
import com.megacrit.cardcrawl.monsters.exordium.Looter;
import com.megacrit.cardcrawl.powers.AbstractPower;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.reflect.Field;

/**
 * Ascension Level 44: Thieves steal more gold
 *
 * 도둑들이 더 많은 돈을 강탈합니다.
 * 도둑들의 도둑질 수치가 25 증가합니다.
 *
 * Both Looter and Mugger's Thievery power increases by 25.
 */
public class Level44 {
    private static final Logger logger = LogManager.getLogger(Level44.class.getName());

    /**
     * Looter: Thievery +25
     *
     * IMPORTANT: Must modify goldAmt field BEFORE usePreBattleAction creates ThieveryPower
     * Original code: ApplyPowerAction(new ThieveryPower(this, this.goldAmt))
     * If we modify the power's amount after creation, it has no effect!
     */
    @SpirePatch(
        clz = Looter.class,
        method = "usePreBattleAction"
    )
    public static class LooterThieveryIncrease {
        @SpirePrefixPatch
        public static void Prefix(Looter __instance) {
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 44) {
                return;
            }

            try {
                // Increase goldAmt BEFORE ThieveryPower is created
                java.lang.reflect.Field goldAmtField = Looter.class.getDeclaredField("goldAmt");
                goldAmtField.setAccessible(true);
                int currentGoldAmt = goldAmtField.getInt(__instance);
                goldAmtField.setInt(__instance, currentGoldAmt + 25);

                logger.info(String.format(
                    "Ascension 44: Looter goldAmt increased from %d to %d (Thievery will be %d)",
                    currentGoldAmt, currentGoldAmt + 25, currentGoldAmt + 25
                ));
            } catch (Exception e) {
                logger.error("Failed to modify Looter goldAmt", e);
            }
        }

        @SpirePostfixPatch
        public static void Postfix(Looter __instance) {
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 44) {
                return;
            }

            try {
                // Reset goldAmt back to original after ThieveryPower is created
                java.lang.reflect.Field goldAmtField = Looter.class.getDeclaredField("goldAmt");
                goldAmtField.setAccessible(true);
                int currentGoldAmt = goldAmtField.getInt(__instance);
                goldAmtField.setInt(__instance, currentGoldAmt - 25);

                logger.info("Ascension 44: Looter goldAmt reset to original (Thievery amount remains increased)");
            } catch (Exception e) {
                logger.error("Failed to reset Looter goldAmt", e);
            }
        }
    }

    /**
     * Mugger: Thievery +25
     *
     * IMPORTANT: Must modify goldAmt field BEFORE usePreBattleAction creates ThieveryPower
     */
    @SpirePatch(
        clz = Mugger.class,
        method = "usePreBattleAction"
    )
    public static class MuggerThieveryIncrease {
        @SpirePrefixPatch
        public static void Prefix(Mugger __instance) {
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 44) {
                return;
            }

            try {
                // Increase goldAmt BEFORE ThieveryPower is created
                java.lang.reflect.Field goldAmtField = Mugger.class.getDeclaredField("goldAmt");
                goldAmtField.setAccessible(true);
                int currentGoldAmt = goldAmtField.getInt(__instance);
                goldAmtField.setInt(__instance, currentGoldAmt + 25);

                logger.info(String.format(
                    "Ascension 44: Mugger goldAmt increased from %d to %d (Thievery will be %d)",
                    currentGoldAmt, currentGoldAmt + 25, currentGoldAmt + 25
                ));
            } catch (Exception e) {
                logger.error("Failed to modify Mugger goldAmt", e);
            }
        }

        @SpirePostfixPatch
        public static void Postfix(Mugger __instance) {
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 44) {
                return;
            }

            try {
                // Reset goldAmt back to original after ThieveryPower is created
                java.lang.reflect.Field goldAmtField = Mugger.class.getDeclaredField("goldAmt");
                goldAmtField.setAccessible(true);
                int currentGoldAmt = goldAmtField.getInt(__instance);
                goldAmtField.setInt(__instance, currentGoldAmt - 25);

                logger.info("Ascension 44: Mugger goldAmt reset to original (Thievery amount remains increased)");
            } catch (Exception e) {
                logger.error("Failed to reset Mugger goldAmt", e);
            }
        }
    }
}
