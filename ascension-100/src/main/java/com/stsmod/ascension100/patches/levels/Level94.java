package com.stsmod.ascension100.patches.levels;

import com.badlogic.gdx.math.MathUtils;
import com.evacipated.cardcrawl.modthespire.lib.*;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.events.shrines.Designer;
import com.megacrit.cardcrawl.events.shrines.GoldShrine;
import com.megacrit.cardcrawl.events.shrines.PurificationShrine;
import com.megacrit.cardcrawl.events.shrines.WomanInBlue;
import com.megacrit.cardcrawl.vfx.RainingGoldEffect;
import javassist.CtBehavior;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Ascension Level 94: Some events become more hostile
 *
 * 일부 이벤트가 불리해집니다.
 *
 * - 파란 옷의 여자: "사지 않는다" 선택시 현재 체력과 최대 체력을 50% 감소
 * - 황금 성소: 기도 선택시 50% 확률로 아무것도 얻지 못함
 * - 신성한 샘: 저주카드가 없더라도 발생 가능
 * - 탑-클래스 디자이너: "때린다" 선택시 최대체력이 10 감소
 */
public class Level94 {
    private static final Logger logger = LogManager.getLogger(Level94.class.getName());

    /**
     * WomanInBlue: "Leave" option now reduces current HP and max HP by 50%
     */
    @SpirePatch(
        clz = WomanInBlue.class,
        method = "buttonEffect"
    )
    public static class WomanInBlueLeaveHarsh {
        @SpirePostfixPatch
        public static void Postfix(WomanInBlue __instance, int buttonPressed) {
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 94) {
                return;
            }

            // Only apply to "Leave" option (case 3)
            // Check screen is INTRO and buttonPressed is 3
            try {
                java.lang.reflect.Field screenField = WomanInBlue.class.getDeclaredField("screen");
                screenField.setAccessible(true);
                Object screen = screenField.get(__instance);

                if (screen.toString().equals("INTRO") && buttonPressed == 3) {
                    // Reduce current HP by 50%
                    int currentHPLoss = MathUtils.ceil(AbstractDungeon.player.currentHealth * 0.5f);
                    AbstractDungeon.player.currentHealth = Math.max(1, AbstractDungeon.player.currentHealth - currentHPLoss);

                    // Reduce max HP by 50%
                    int maxHPLoss = MathUtils.ceil(AbstractDungeon.player.maxHealth * 0.5f);
                    AbstractDungeon.player.decreaseMaxHealth(maxHPLoss);

                    logger.info(String.format(
                        "Ascension 94: Woman in Blue 'Leave' - Lost %d current HP and %d max HP (50%%)",
                        currentHPLoss, maxHPLoss
                    ));
                }
            } catch (Exception e) {
                logger.error("Failed to modify Woman in Blue event", e);
            }
        }
    }

    /**
     * GoldShrine: "Pray" option has 50% chance to give nothing
     */
    @SpirePatch(
        clz = GoldShrine.class,
        method = "buttonEffect"
    )
    public static class GoldShrinePrayFail {
        @SpirePrefixPatch
        public static SpireReturn<Void> Prefix(GoldShrine __instance, int buttonPressed) {
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 94) {
                return SpireReturn.Continue();
            }

            // Only apply to "Pray" option (case 0)
            try {
                java.lang.reflect.Field screenField = GoldShrine.class.getDeclaredField("screen");
                screenField.setAccessible(true);
                Object screen = screenField.get(__instance);

                if (screen.toString().equals("INTRO") && buttonPressed == 0) {
                    // 50% chance to fail
                    int roll = AbstractDungeon.miscRng.random(0, 99);
                    if (roll < 50) {
                        // Failed - give nothing
                        try {
                            // Set screen to COMPLETE
                            Class<?> screenClass = Class.forName("com.megacrit.cardcrawl.events.shrines.GoldShrine$CUR_SCREEN");
                            Object completeScreen = Enum.valueOf((Class<Enum>)screenClass, "COMPLETE");
                            screenField.set(__instance, completeScreen);

                            // Update dialog
                            java.lang.reflect.Field imageEventTextField = __instance.getClass().getSuperclass().getDeclaredField("imageEventText");
                            imageEventTextField.setAccessible(true);
                            Object imageEventText = imageEventTextField.get(__instance);

                            java.lang.reflect.Method updateBodyText = imageEventText.getClass().getMethod("updateBodyText", String.class);
                            updateBodyText.invoke(imageEventText, "The shrine does not respond to your prayer.");

                            java.lang.reflect.Field optionsField = GoldShrine.class.getDeclaredField("OPTIONS");
                            optionsField.setAccessible(true);
                            String[] options = (String[]) optionsField.get(null);

                            java.lang.reflect.Method updateDialogOption = imageEventText.getClass().getMethod("updateDialogOption", int.class, String.class);
                            updateDialogOption.invoke(imageEventText, 0, options[3]);

                            java.lang.reflect.Method clearRemainingOptions = imageEventText.getClass().getMethod("clearRemainingOptions");
                            clearRemainingOptions.invoke(imageEventText);

                            logger.info(String.format("Ascension 94: Golden Shrine 'Pray' failed (Roll: %d < 50)", roll));

                            return SpireReturn.Return(null);
                        } catch (Exception e2) {
                            logger.error("Failed to update Golden Shrine dialog", e2);
                        }
                    }
                }
            } catch (Exception e) {
                logger.error("Failed to modify Golden Shrine event", e);
            }

            return SpireReturn.Continue();
        }
    }

    /**
     * Designer: "Punch" option now decreases max HP by 10 instead of damaging
     */
    @SpirePatch(
        clz = Designer.class,
        method = "buttonEffect"
    )
    public static class DesignerPunchMaxHPLoss {
        @SpirePostfixPatch
        public static void Postfix(Designer __instance, int buttonPressed) {
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 94) {
                return;
            }

            // Only apply to "Punch" option (case 3)
            try {
                java.lang.reflect.Field screenField = Designer.class.getDeclaredField("curScreen");
                screenField.setAccessible(true);
                Object screen = screenField.get(__instance);

                if (screen.toString().equals("INTRO") && buttonPressed == 3) {
                    // Decrease max HP by 10 instead of damaging
                    AbstractDungeon.player.decreaseMaxHealth(10);

                    logger.info("Ascension 94: Designer 'Punch' - Decreased max HP by 10");
                }
            } catch (Exception e) {
                logger.error("Failed to modify Designer event", e);
            }
        }
    }

    /**
     * Fountain of Cleansing: Can appear even without curse cards
     * (신성한 샘: 저주카드가 없더라도 발생 가능)
     *
     * Patches AbstractDungeon.getShrine() to bypass the isCursed() check
     * for "Fountain of Cleansing" event at Ascension 94+
     */
    @SpirePatch(
        clz = AbstractDungeon.class,
        method = "getShrine"
    )
    public static class FountainOfCleansingAlwaysSpawn {
        @SpirePrefixPatch
        public static void Prefix(com.megacrit.cardcrawl.random.Random rng) {
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 94) {
                return;
            }

            // At Ascension 94+, temporarily add "Fountain of Cleansing" to shrineList
            // if it's in specialOneTimeEventList and player doesn't have curses
            if (AbstractDungeon.specialOneTimeEventList.contains("Fountain of Cleansing") &&
                !AbstractDungeon.player.isCursed() &&
                !AbstractDungeon.shrineList.contains("Fountain of Cleansing")) {

                // Temporarily move it to shrineList so it bypasses the isCursed check
                AbstractDungeon.shrineList.add("Fountain of Cleansing");
                logger.info("Ascension 94: Fountain of Cleansing added to shrine pool without curse cards");
            }
        }

        @SpirePostfixPatch
        public static void Postfix(com.megacrit.cardcrawl.random.Random rng) {
            // Clean up: if we added it temporarily, move it back
            // The getShrine method already handles removal from lists
        }
    }
}
