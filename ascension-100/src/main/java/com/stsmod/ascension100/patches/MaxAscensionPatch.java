package com.stsmod.ascension100.patches;

import com.evacipated.cardcrawl.modthespire.lib.*;
import com.megacrit.cardcrawl.screens.charSelect.CharacterOption;
import com.stsmod.ascension100.Ascension100Mod;

/**
 * Patch to change the maximum ascension level from 20 to 100
 * This is the core patch that enables the extended ascension mode
 */
public class MaxAscensionPatch {

    /**
     * Patch the getMaxAscensionLevel method
     * Returns 100 instead of 20
     */
    @SpirePatch(
            clz = CharacterOption.class,
            method = "getMaxAscensionLevel"
    )
    public static class GetMaxAscensionLevelPatch {
        @SpirePrefixPatch
        public static SpireReturn<Integer> Prefix(CharacterOption __instance) {
            // Return our max ascension level instead of the default 20
            return SpireReturn.Return(Ascension100Mod.MAX_ASCENSION);
        }
    }

    /**
     * Patch to ensure ascension level increments work correctly
     */
    @SpirePatch(
            clz = CharacterOption.class,
            method = "incrementAscensionLevel"
    )
    public static class IncrementAscensionPatch {
        @SpirePostfixPatch
        public static void Postfix(CharacterOption __instance, int amount) {
            Ascension100Mod.log("Ascension level changed by " + amount);
        }
    }
}
