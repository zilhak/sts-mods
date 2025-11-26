package com.stsmod.ascension100.patches.levels;

import com.badlogic.gdx.math.MathUtils;
import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePrefixPatch;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.events.shrines.GremlinWheelGame;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.reflect.Field;

/**
 * Ascension Level 43: Wheel event adapts to player HP
 *
 * 일부 이벤트가 당신의 상황에 맞춰 변경됩니다.
 * 돌림판 이벤트에서 체력이 15% 미만인 경우, 피해를 입는 이벤트가 선택됩니다.
 * 돌림판 이벤트에서 체력이 80% 이상인경우, 체력 회복 이벤트가 선택됩니다.
 *
 * Wheel results:
 * - 0: Gold
 * - 1: Random Relic
 * - 2: Full Heal (forced when HP >= 80%)
 * - 3: Curse
 * - 4: Card Removal
 * - 5: Take Damage (forced when HP < 15%)
 */
public class Level43 {
    private static final Logger logger = LogManager.getLogger(Level43.class.getName());

    /**
     * Patch the wheel event to force specific outcomes based on player HP
     */
    @SpirePatch(
        clz = GremlinWheelGame.class,
        method = "buttonEffect",
        paramtypez = {int.class}
    )
    public static class ForceResultByHP {
        @SpirePrefixPatch
        public static void Prefix(GremlinWheelGame __instance, int buttonPressed) {
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 43) {
                return;
            }

            try {
                // Get screen field to check if we're in INTRO state
                Field screenField = GremlinWheelGame.class.getDeclaredField("screen");
                screenField.setAccessible(true);
                Object screen = screenField.get(__instance);

                // Only apply when player presses the spin button (INTRO screen, button 0)
                if (screen.toString().equals("INTRO") && buttonPressed == 0) {
                    // Calculate HP percentage
                    float hpPercent = (float) AbstractDungeon.player.currentHealth /
                                     AbstractDungeon.player.maxHealth;

                    // Get result field
                    Field resultField = GremlinWheelGame.class.getDeclaredField("result");
                    resultField.setAccessible(true);

                    int forcedResult = -1;

                    if (hpPercent < 0.15f) {
                        // HP < 15%: Force damage result (5)
                        forcedResult = 5;
                        logger.info(String.format(
                            "Ascension 43: Wheel forced to DAMAGE (HP: %d/%d = %.1f%% < 15%%)",
                            AbstractDungeon.player.currentHealth,
                            AbstractDungeon.player.maxHealth,
                            hpPercent * 100
                        ));
                    } else if (hpPercent >= 0.80f) {
                        // HP >= 80%: Force heal result (2)
                        forcedResult = 2;
                        logger.info(String.format(
                            "Ascension 43: Wheel forced to HEAL (HP: %d/%d = %.1f%% >= 80%%)",
                            AbstractDungeon.player.currentHealth,
                            AbstractDungeon.player.maxHealth,
                            hpPercent * 100
                        ));
                    } else {
                        // Normal range: Let random work normally
                        logger.info(String.format(
                            "Ascension 43: Wheel using normal random (HP: %d/%d = %.1f%%)",
                            AbstractDungeon.player.currentHealth,
                            AbstractDungeon.player.maxHealth,
                            hpPercent * 100
                        ));
                        return;
                    }

                    // Set the forced result
                    resultField.setInt(__instance, forcedResult);

                    // Also set the result angle to match the forced result
                    // Each result is 60 degrees apart (360 / 6 = 60)
                    Field resultAngleField = GremlinWheelGame.class.getDeclaredField("resultAngle");
                    resultAngleField.setAccessible(true);
                    float angle = forcedResult * 60.0F + MathUtils.random(-10.0F, 10.0F);
                    resultAngleField.setFloat(__instance, angle);

                    logger.info(String.format(
                        "Ascension 43: Set wheel result to %d, angle to %.1f degrees",
                        forcedResult, angle
                    ));
                }
            } catch (NoSuchFieldException e) {
                logger.error("Ascension 43: Could not find field in GremlinWheelGame", e);
            } catch (IllegalAccessException e) {
                logger.error("Ascension 43: Could not access field in GremlinWheelGame", e);
            } catch (Exception e) {
                logger.error("Ascension 43: Unexpected error modifying wheel result", e);
            }
        }
    }
}
