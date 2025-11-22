package com.stsmod.ascension100.patches;

import com.evacipated.cardcrawl.modthespire.lib.*;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.helpers.Prefs;
import com.megacrit.cardcrawl.screens.charSelect.CharacterOption;
import com.stsmod.ascension100.Ascension100Mod;
import javassist.CtBehavior;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Patches to allow maxAscensionLevel to exceed 20 and reach 100
 */
public class MaxAscensionLevelPatch {

    private static final Logger logger = LogManager.getLogger(MaxAscensionLevelPatch.class.getName());

    /**
     * Patch updateHitbox to fix maxAscensionLevel cap at 20
     * This runs continuously during character selection
     */
    @SpirePatch(
        clz = CharacterOption.class,
        method = "updateHitbox"
    )
    public static class UpdateHitboxMaxCapPatch {
        @SpirePrefixPatch
        public static void Prefix(CharacterOption __instance) {
            try {
                // Get maxAscensionLevel field
                java.lang.reflect.Field maxAscensionField = CharacterOption.class.getDeclaredField("maxAscensionLevel");
                maxAscensionField.setAccessible(true);
                int currentMax = maxAscensionField.getInt(__instance);

                // Get the character field to access prefs
                java.lang.reflect.Field charField = CharacterOption.class.getDeclaredField("c");
                charField.setAccessible(true);
                com.megacrit.cardcrawl.characters.AbstractPlayer player =
                    (com.megacrit.cardcrawl.characters.AbstractPlayer) charField.get(__instance);

                if (player != null) {
                    Prefs pref = player.getPrefs();
                    int actualMax = pref.getInteger("ASCENSION_LEVEL", 1);

                    // Cap at 100 instead of 20
                    if (actualMax > Ascension100Mod.MAX_ASCENSION) {
                        actualMax = Ascension100Mod.MAX_ASCENSION;
                    }

                    // Only update if it's different (and not already correct)
                    if (currentMax != actualMax) {
                        maxAscensionField.set(__instance, actualMax);
                        logger.info("Updated maxAscensionLevel from " + currentMax + " to " + actualMax);
                    }
                }
            } catch (Exception e) {
                // Silently fail - this runs every frame
            }
        }
    }

    /**
     * Patch incrementAscensionLevel to respect the new max
     */
    @SpirePatch(
        clz = CharacterOption.class,
        method = "incrementAscensionLevel",
        paramtypez = {int.class}
    )
    public static class IncrementWithNewMaxPatch {
        @SpirePrefixPatch
        public static SpireReturn<Void> Prefix(CharacterOption __instance, int level) {
            try {
                // Get maxAscensionLevel field
                java.lang.reflect.Field maxAscensionField = CharacterOption.class.getDeclaredField("maxAscensionLevel");
                maxAscensionField.setAccessible(true);
                int maxLevel = maxAscensionField.getInt(__instance);

                // Check against actual max (up to 100)
                if (level > maxLevel) {
                    logger.info("Cannot increment to " + level + ", max is " + maxLevel);
                    return SpireReturn.Return(null);
                }

                // Allow vanilla code to proceed
                return SpireReturn.Continue();

            } catch (Exception e) {
                logger.error("Failed to check max level in incrementAscensionLevel", e);
                return SpireReturn.Continue();
            }
        }
    }
}
