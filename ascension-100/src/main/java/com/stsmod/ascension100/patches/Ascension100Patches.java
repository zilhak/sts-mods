package com.stsmod.ascension100.patches;

import com.evacipated.cardcrawl.modthespire.lib.ByRef;
import com.evacipated.cardcrawl.modthespire.lib.SpireInsertPatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.evacipated.cardcrawl.modthespire.lib.SpireReturn;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.localization.UIStrings;
import com.megacrit.cardcrawl.screens.charSelect.CharacterOption;
import com.stsmod.ascension100.Ascension100Mod;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Patches to extend ascension mode from 20 to 100 levels
 */
public class Ascension100Patches {

    private static final Logger logger = LogManager.getLogger(Ascension100Patches.class.getName());

    // Load UI strings for ascension descriptions
    private static UIStrings getAscensionStrings() {
        try {
            return CardCrawlGame.languagePack.getUIString("AscensionModeDescriptions");
        } catch (Exception e) {
            logger.error("Failed to load ascension strings", e);
            return null;
        }
    }

    /**
     * Patch to allow incrementing ascension level beyond 20 up to 100
     */
    @SpirePatch(
        cls = "com.megacrit.cardcrawl.screens.charSelect.CharacterOption",
        method = "incrementAscensionLevel",
        paramtypes = {"int"}
    )
    public static class IncrementPatch {
        @SpireInsertPatch(rloc = 0)
        public static SpireReturn<?> Insert(CharacterOption __instance, @ByRef int[] level) {
            // Cap at 100
            if (level[0] > Ascension100Mod.MAX_ASCENSION) {
                level[0] = Ascension100Mod.MAX_ASCENSION;
            }

            // Store the current ascension level
            Ascension100Mod.currentAscensionLevel = level[0];
            CardCrawlGame.mainMenuScreen.charSelectScreen.ascensionLevel = level[0];

            // For levels >= 20, handle description ourselves
            if (level[0] >= 20) {
                UIStrings ascStrings = getAscensionStrings();
                if (ascStrings != null && level[0] > 0 && level[0] <= ascStrings.TEXT.length) {
                    CardCrawlGame.mainMenuScreen.charSelectScreen.ascLevelInfoString =
                        ascStrings.TEXT[level[0] - 1];
                }
                logger.info("Ascension level set to: " + level[0]);
                return SpireReturn.Return(null);
            }

            // Let vanilla code handle levels < 20
            return SpireReturn.Continue();
        }
    }

    /**
     * Patch to allow decrementing ascension level from extended levels
     */
    @SpirePatch(
        cls = "com.megacrit.cardcrawl.screens.charSelect.CharacterOption",
        method = "decrementAscensionLevel",
        paramtypes = {"int"}
    )
    public static class DecrementPatch {
        @SpireInsertPatch(rloc = 0)
        public static SpireReturn<?> Insert(CharacterOption __instance, @ByRef int[] level) {
            // Ensure we don't go below 0
            if (level[0] < 0) {
                level[0] = 0;
            }

            // Store the current ascension level
            Ascension100Mod.currentAscensionLevel = level[0];
            CardCrawlGame.mainMenuScreen.charSelectScreen.ascensionLevel = level[0];

            // For levels >= 20, handle description ourselves
            if (level[0] >= 20) {
                UIStrings ascStrings = getAscensionStrings();
                if (ascStrings != null && level[0] > 0 && level[0] <= ascStrings.TEXT.length) {
                    CardCrawlGame.mainMenuScreen.charSelectScreen.ascLevelInfoString =
                        ascStrings.TEXT[level[0] - 1];
                }
                logger.info("Ascension level set to: " + level[0]);
                return SpireReturn.Return(null);
            } else if (level[0] == 0) {
                // Handle level 0 - no description
                CardCrawlGame.mainMenuScreen.charSelectScreen.ascLevelInfoString = "";
                return SpireReturn.Return(null);
            }

            // Let vanilla code handle levels 1-19
            return SpireReturn.Continue();
        }
    }

    /**
     * Patch to update hitbox and display for extended ascension levels
     */
    @SpirePatch(
        cls = "com.megacrit.cardcrawl.screens.charSelect.CharacterOption",
        method = "updateHitbox",
        paramtypes = {}
    )
    public static class UpdateHitboxPatch {
        @SpireInsertPatch(rloc = 62)
        public static SpireReturn<?> Insert(CharacterOption __instance) {
            // Cancel original hitbox logic, we'll handle it in Postfix
            return SpireReturn.Return(null);
        }

        public static void Postfix(CharacterOption __instance) {
            // Ensure level stays within bounds
            if (CardCrawlGame.mainMenuScreen.charSelectScreen.ascensionLevel > Ascension100Mod.MAX_ASCENSION) {
                CardCrawlGame.mainMenuScreen.charSelectScreen.ascensionLevel = Ascension100Mod.MAX_ASCENSION;
            }
            if (CardCrawlGame.mainMenuScreen.charSelectScreen.ascensionLevel < 0) {
                CardCrawlGame.mainMenuScreen.charSelectScreen.ascensionLevel = 0;
            }

            // Update stored level
            Ascension100Mod.currentAscensionLevel = CardCrawlGame.mainMenuScreen.charSelectScreen.ascensionLevel;

            // Update description string for levels >= 20
            int level = CardCrawlGame.mainMenuScreen.charSelectScreen.ascensionLevel;
            if (level > 0) {
                UIStrings ascStrings = getAscensionStrings();
                if (ascStrings != null && level <= ascStrings.TEXT.length) {
                    CardCrawlGame.mainMenuScreen.charSelectScreen.ascLevelInfoString =
                        ascStrings.TEXT[level - 1];
                }
            } else {
                CardCrawlGame.mainMenuScreen.charSelectScreen.ascLevelInfoString = "";
            }
        }
    }
}
