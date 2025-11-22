package com.stsmod.ascension100.patches;

import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.evacipated.cardcrawl.modthespire.lib.SpireReturn;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.helpers.Prefs;
import com.megacrit.cardcrawl.screens.stats.CharStat;
import com.stsmod.ascension100.Ascension100Mod;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Patch CharStat.incrementAscension() to extend beyond level 20
 * Unlocks ascension levels incrementally (one at a time)
 * Manual level adjustment available in mod config
 */
public class IncrementAscensionPatch {

    private static final Logger logger = LogManager.getLogger(IncrementAscensionPatch.class.getName());

    @SpirePatch(
        clz = CharStat.class,
        method = "incrementAscension"
    )
    public static class IncrementAscensionPatchClass {
        public static SpireReturn<Void> Prefix(CharStat __instance) {
            // Don't modify ascension in trial mode
            if (Settings.isTrial) {
                return SpireReturn.Return(null);
            }

            try {
                // Get preferences using reflection
                java.lang.reflect.Field prefField = CharStat.class.getDeclaredField("pref");
                prefField.setAccessible(true);
                Prefs pref = (Prefs) prefField.get(__instance);

                int currentMax = pref.getInteger("ASCENSION_LEVEL", 1);
                int playedLevel = AbstractDungeon.ascensionLevel;

                logger.info("=== ASCENSION UNLOCK CHECK ===");
                logger.info("Current max ascension: " + currentMax);
                logger.info("Played level: " + playedLevel);

                // Only increment if player beat their current max level
                if (playedLevel < currentMax) {
                    logger.info("Played level (" + playedLevel + ") is below current max (" + currentMax + "), no unlock");
                    return SpireReturn.Return(null);
                }

                // Incremental unlock: Always unlock next level only
                int newLevel;
                if (playedLevel == currentMax) {
                    newLevel = currentMax + 1;
                    if (newLevel > Ascension100Mod.MAX_ASCENSION) {
                        newLevel = Ascension100Mod.MAX_ASCENSION;
                        logger.info("Already at max ascension level: " + Ascension100Mod.MAX_ASCENSION);
                    } else {
                        logger.info("Unlocking ascension level " + newLevel + " (cleared " + currentMax + ")");
                    }
                } else {
                    // Played higher than current max (manual adjustment in config)
                    newLevel = playedLevel + 1;
                    logger.info("Unlocking ascension level " + newLevel + " (cleared " + playedLevel + ", was " + currentMax + ")");
                }

                // Save new ascension level
                pref.putInteger("ASCENSION_LEVEL", newLevel);
                pref.putInteger("LAST_ASCENSION_LEVEL", newLevel);
                pref.flush();

                // Save actual cleared level to progress config (for tracking real progress)
                if (Ascension100Mod.progressConfig != null && playedLevel >= currentMax) {
                    // Get character class name
                    String characterKey = AbstractDungeon.player.chosenClass.name() + "_ACTUAL_MAX";
                    int actualMax = Ascension100Mod.progressConfig.getInt(characterKey);

                    // Update if this is higher than recorded
                    if (playedLevel > actualMax) {
                        Ascension100Mod.progressConfig.setInt(characterKey, playedLevel);
                        Ascension100Mod.progressConfig.save();
                        logger.info("Saved actual clear progress: " + characterKey + " = " + playedLevel);
                    }
                }

                logger.info("Ascension level updated to: " + newLevel);

            } catch (Exception e) {
                logger.error("Failed to increment ascension level", e);
            }

            // Return early to skip vanilla logic
            return SpireReturn.Return(null);
        }
    }
}
