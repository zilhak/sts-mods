package com.stsmod.ascension100.patches.ui;

import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePrefixPatch;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.helpers.Prefs;
import com.megacrit.cardcrawl.screens.charSelect.CharacterOption;
import com.megacrit.cardcrawl.screens.charSelect.CharacterSelectScreen;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Ascension Level Persistence Patch
 *
 * 승천 레벨 선택 유지 기능
 *
 * Problem:
 * - 원본 게임은 승천 최대 20까지만 지원
 * - 승천 100을 선택해도 게임 재시작 시 승천 20으로 제한됨
 *
 * Solution:
 * - CharacterOption.updateHitbox() 패치: 캐릭터 선택 시 승천 20 제한 제거
 * - CharacterSelectScreen.updateButtons() 패치: 게임 시작 시 선택된 승천 레벨 저장
 */
public class AscensionLevelPersistencePatch {
    private static final Logger logger = LogManager.getLogger(AscensionLevelPersistencePatch.class.getName());
    private static final int MAX_ASCENSION_LEVEL = 100;

    /**
     * Patch 1: Remove ascension 20 cap when loading character
     *
     * Original code (CharacterOption.java:173-220):
     * - if (this.hb.clicked) { ... if (!this.selected) { ... } }
     * - Loads LAST_ASCENSION_LEVEL from preferences
     * - Limits to 20: if (20 < ascensionLevel) ascensionLevel = 20;
     *
     * Our fix:
     * - Trigger ONLY when hitbox is clicked (not on every frame)
     * - Allow up to 100 ascension levels
     * - Update maxAscensionLevel field to 100
     *
     * IMPORTANT: This must run ONLY when character is FIRST clicked,
     * not every frame while selected. Otherwise it resets user's manual changes.
     */
    @SpirePatch(
        clz = CharacterOption.class,
        method = "updateHitbox"
    )
    public static class RemoveAscensionCapOnLoadPatch {
        private static boolean hasBeenProcessedThisClick = false;

        @SpirePostfixPatch
        public static void Postfix(CharacterOption __instance) {
            // CRITICAL: Only run when hitbox is CLICKED, not when selected
            // Original game code structure:
            //   if (this.hb.clicked) {
            //     this.hb.clicked = false;  <- Reset immediately
            //     if (!this.selected) { ... load ascension ... }
            //   }
            //
            // We use hb.clicked as trigger because:
            // 1. It's set to true when user clicks
            // 2. It's reset to false immediately by original code
            // 3. This ensures we run exactly once per click

            if (!__instance.hb.clicked || __instance.selected) {
                // Reset flag when hitbox is not clicked anymore
                if (!__instance.hb.clicked) {
                    hasBeenProcessedThisClick = false;
                }
                return;
            }

            // Prevent running multiple times for the same click
            if (hasBeenProcessedThisClick) {
                return;
            }
            hasBeenProcessedThisClick = true;

            try {
                Prefs pref = __instance.c.getPrefs();
                if (pref == null) {
                    return;
                }

                // Load saved ascension level (original code limits to 20)
                int lastAscension = pref.getInteger("LAST_ASCENSION_LEVEL", 1);

                // Allow up to 100 instead of 20
                if (lastAscension > 20) {
                    CardCrawlGame.mainMenuScreen.charSelectScreen.ascensionLevel =
                        Math.min(lastAscension, MAX_ASCENSION_LEVEL);

                    logger.info(String.format(
                        "Loaded ascension level %d for %s (bypassing 20 cap)",
                        CardCrawlGame.mainMenuScreen.charSelectScreen.ascensionLevel,
                        __instance.c.chosenClass.name()
                    ));
                }

                // Also update maxAscensionLevel field to allow 100
                java.lang.reflect.Field maxAscField = CharacterOption.class.getDeclaredField("maxAscensionLevel");
                maxAscField.setAccessible(true);
                int maxAsc = pref.getInteger("ASCENSION_LEVEL", 1);
                int newMaxAsc = Math.min(maxAsc, MAX_ASCENSION_LEVEL);

                // Only update if it's higher than 20 (avoid interfering with low-ascension players)
                int currentMaxAsc = maxAscField.getInt(__instance);
                if (newMaxAsc > currentMaxAsc) {
                    maxAscField.setInt(__instance, newMaxAsc);
                    logger.info(String.format(
                        "Updated max ascension level to %d for %s",
                        newMaxAsc,
                        __instance.c.chosenClass.name()
                    ));
                }

            } catch (Exception e) {
                logger.error("Failed to remove ascension cap on character selection", e);
            }
        }
    }

    /**
     * Patch 2: Save selected ascension level when starting game
     *
     * Original code (CharacterSelectScreen.java:297-354):
     * - Confirm button clicked → start game
     * - Does NOT save currently selected ascension level
     *
     * Our fix:
     * - Save ascensionLevel to LAST_ASCENSION_LEVEL before starting game
     * - Ensures selection persists to next session
     */
    @SpirePatch(
        clz = CharacterSelectScreen.class,
        method = "updateButtons"
    )
    public static class SaveAscensionOnGameStartPatch {
        @SpirePrefixPatch
        public static void Prefix(CharacterSelectScreen __instance) {
            // Check if confirm button was clicked and ascension mode is enabled
            if (!__instance.confirmButton.hb.clicked || !__instance.isAscensionMode) {
                return;
            }

            try {
                // Find selected character
                for (CharacterOption o : __instance.options) {
                    if (o.selected) {
                        // Save current ascension level
                        int currentLevel = __instance.ascensionLevel;
                        o.saveChosenAscensionLevel(currentLevel);

                        logger.info(String.format(
                            "Saved ascension level %d for %s on game start",
                            currentLevel,
                            o.c.chosenClass.name()
                        ));
                        break;
                    }
                }
            } catch (Exception e) {
                logger.error("Failed to save ascension level on game start", e);
            }
        }
    }

    /**
     * Patch 3: Ensure ascension level increment/decrement works above 20
     *
     * Original code (CharacterSelectScreen.java:253-271):
     * - Left/right buttons to change ascension level
     * - Checks maxAscensionLevel (limited to 20 in original)
     *
     * Our fix:
     * - Already handled by Patch 1 (maxAscensionLevel updated to 100)
     * - This patch ensures the UI updates correctly for levels > 20
     */
    @SpirePatch(
        clz = CharacterSelectScreen.class,
        method = "updateAscensionToggle"
    )
    public static class ExtendAscensionUIRangePatch {
        @SpirePostfixPatch
        public static void Postfix(CharacterSelectScreen __instance) {
            try {
                // Access private fields using reflection
                java.lang.reflect.Field isAscensionModeUnlockedField =
                    CharacterSelectScreen.class.getDeclaredField("isAscensionModeUnlocked");
                isAscensionModeUnlockedField.setAccessible(true);
                boolean isAscensionModeUnlocked = isAscensionModeUnlockedField.getBoolean(__instance);

                java.lang.reflect.Field anySelectedField =
                    CharacterSelectScreen.class.getDeclaredField("anySelected");
                anySelectedField.setAccessible(true);
                boolean anySelected = anySelectedField.getBoolean(__instance);

                // Ensure ascLevelInfoString is set correctly even for levels > 20
                if (!isAscensionModeUnlocked || !anySelected) {
                    return;
                }

                int currentLevel = __instance.ascensionLevel;

                // Original game only has A_TEXT[0-19] for ascension 1-20
                // For levels > 20, we need to handle this gracefully
                if (currentLevel > 20 && currentLevel <= MAX_ASCENSION_LEVEL) {
                    // Check if A_TEXT array is large enough
                    if (CharacterSelectScreen.A_TEXT.length < currentLevel) {
                        // Fallback to showing "Ascension X" text
                        __instance.ascLevelInfoString = "Ascension " + currentLevel;
                    } else if (currentLevel > 0) {
                        __instance.ascLevelInfoString = CharacterSelectScreen.A_TEXT[currentLevel - 1];
                    }
                }
            } catch (Exception e) {
                logger.error("Failed to update ascension UI for high levels", e);
            }
        }
    }
}
