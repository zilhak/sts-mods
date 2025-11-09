package com.stsmod.ascension100.patches;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.evacipated.cardcrawl.modthespire.lib.*;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.helpers.FontHelper;
import com.megacrit.cardcrawl.helpers.Hitbox;
import com.megacrit.cardcrawl.helpers.input.InputHelper;
import com.megacrit.cardcrawl.screens.charSelect.CharacterSelectScreen;
import com.stsmod.ascension100.Ascension100Mod;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Patches to add +10/-10 buttons for ascension level selection
 */
public class AscensionButtonsPatch {

    private static final Logger logger = LogManager.getLogger(AscensionButtonsPatch.class.getName());

    // Button dimensions
    private static final float BUTTON_WIDTH = 70.0F;
    private static final float BUTTON_HEIGHT = 70.0F;

    // Custom hitboxes for +10/-10 buttons (stored as static since there's only one screen)
    private static Hitbox ascLeft10Hb = new Hitbox(BUTTON_WIDTH, BUTTON_HEIGHT);
    private static Hitbox ascRight10Hb = new Hitbox(BUTTON_WIDTH, BUTTON_HEIGHT);

    // Debug flag - set to true to enable detailed logging
    private static boolean debugLogging = true;
    private static int updateCount = 0;

    /**
     * Patch to update +10/-10 button positions and handle clicks
     */
    @SpirePatch(
        cls = "com.megacrit.cardcrawl.screens.charSelect.CharacterSelectScreen",
        method = "update"
    )
    public static class UpdatePatch {
        @SpirePostfixPatch
        public static void Postfix(CharacterSelectScreen __instance) {
            try {
                updateCount++;

                // Log every 60 frames (once per second at 60fps)
                if (debugLogging && updateCount % 60 == 0) {
                    logger.info("Update called: isAscensionMode=" + __instance.isAscensionMode +
                               ", ascensionLevel=" + __instance.ascensionLevel);
                }

                // Only update when ascension mode is active
                if (!__instance.isAscensionMode) {
                    return;
                }

                // Get the existing left/right hitboxes using reflection
                java.lang.reflect.Field ascLeftHbField = CharacterSelectScreen.class.getDeclaredField("ascLeftHb");
                java.lang.reflect.Field ascRightHbField = CharacterSelectScreen.class.getDeclaredField("ascRightHb");
                ascLeftHbField.setAccessible(true);
                ascRightHbField.setAccessible(true);

                Hitbox ascLeftHb = (Hitbox) ascLeftHbField.get(__instance);
                Hitbox ascRightHb = (Hitbox) ascRightHbField.get(__instance);

                if (ascLeftHb == null || ascRightHb == null) {
                    if (debugLogging && updateCount % 60 == 0) {
                        logger.warn("ascLeftHb or ascRightHb is null");
                    }
                    return;
                }

                // Get our +10/-10 hitboxes
                Hitbox left10Hb = ascLeft10Hb;
                Hitbox right10Hb = ascRight10Hb;

                // Position the -10 button to the left of the existing -1 button
                left10Hb.move(ascLeftHb.cX - BUTTON_WIDTH * 1.3f, ascLeftHb.cY);

                // Position the +10 button to the right of the existing +1 button
                right10Hb.move(ascRightHb.cX + BUTTON_WIDTH * 1.3f, ascRightHb.cY);

                // Update hitboxes
                left10Hb.update();
                right10Hb.update();

                // Debug: Log hitbox state when hovered
                if (debugLogging && (left10Hb.hovered || right10Hb.hovered)) {
                    logger.info("Hitbox hovered! left10=" + left10Hb.hovered +
                               ", right10=" + right10Hb.hovered +
                               ", justClicked=" + InputHelper.justClickedLeft +
                               ", justReleased=" + InputHelper.justReleasedClickLeft);
                }

                // Find the currently selected character option
                com.megacrit.cardcrawl.screens.charSelect.CharacterOption selectedOption = null;
                if (__instance.options != null) {
                    for (com.megacrit.cardcrawl.screens.charSelect.CharacterOption option : __instance.options) {
                        if (option.selected) {
                            selectedOption = option;
                            break;
                        }
                    }
                }

                if (selectedOption == null) {
                    if (debugLogging && updateCount % 60 == 0) {
                        logger.warn("No character option selected");
                    }
                    return;
                }

                // Handle -10 button click
                if (left10Hb.hovered && InputHelper.justClickedLeft) {
                    int currentLevel = __instance.ascensionLevel;
                    int newLevel = Math.max(0, currentLevel - 10);

                    logger.info("Left10 button clicked! Current level: " + currentLevel + ", new level: " + newLevel);

                    // Call decrementAscensionLevel with the new level value
                    selectedOption.decrementAscensionLevel(newLevel);

                    CardCrawlGame.sound.play("UI_CLICK_1");
                    logger.info("Ascension level decreased to: " + __instance.ascensionLevel);
                }

                // Handle +10 button click
                if (right10Hb.hovered && InputHelper.justClickedLeft) {
                    int currentLevel = __instance.ascensionLevel;
                    int newLevel = Math.min(Ascension100Mod.MAX_ASCENSION, currentLevel + 10);

                    logger.info("Right10 button clicked! Current level: " + currentLevel + ", new level: " + newLevel);

                    // Call incrementAscensionLevel with the new level value
                    selectedOption.incrementAscensionLevel(newLevel);

                    CardCrawlGame.sound.play("UI_CLICK_1");
                    logger.info("Ascension level increased to: " + __instance.ascensionLevel);
                }

            } catch (Exception e) {
                logger.error("Error updating +10/-10 buttons", e);
            }
        }
    }

    /**
     * Patch to render +10/-10 buttons
     */
    @SpirePatch(
        cls = "com.megacrit.cardcrawl.screens.charSelect.CharacterSelectScreen",
        method = "renderAscensionMode"
    )
    public static class RenderAscensionModePatch {
        @SpirePostfixPatch
        public static void Postfix(CharacterSelectScreen __instance, SpriteBatch sb) {
            try {
                // Only render when ascension mode is active
                if (!__instance.isAscensionMode) {
                    return;
                }

                // Check if a character is selected
                boolean hasSelectedCharacter = false;
                if (__instance.options != null) {
                    for (com.megacrit.cardcrawl.screens.charSelect.CharacterOption option : __instance.options) {
                        if (option.selected) {
                            hasSelectedCharacter = true;
                            break;
                        }
                    }
                }

                // Only render buttons when a character is selected
                if (!hasSelectedCharacter) {
                    return;
                }

                // Get our +10/-10 hitboxes
                Hitbox left10Hb = ascLeft10Hb;
                Hitbox right10Hb = ascRight10Hb;

                // Render -10 button
                Color color = left10Hb.hovered ? Color.YELLOW : Color.WHITE;
                FontHelper.renderFontCentered(sb, FontHelper.buttonLabelFont, "-10",
                    left10Hb.cX, left10Hb.cY, color);

                // Render +10 button
                color = right10Hb.hovered ? Color.YELLOW : Color.WHITE;
                FontHelper.renderFontCentered(sb, FontHelper.buttonLabelFont, "+10",
                    right10Hb.cX, right10Hb.cY, color);

                // Render hitboxes for debugging
                if (left10Hb.hovered || right10Hb.hovered) {
                    left10Hb.render(sb);
                    right10Hb.render(sb);
                }

            } catch (Exception e) {
                logger.error("Error rendering +10/-10 buttons", e);
            }
        }
    }
}
