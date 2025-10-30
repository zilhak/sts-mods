package com.stsmod.ascension100.patches;

import com.evacipated.cardcrawl.modthespire.lib.*;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.stsmod.ascension100.Ascension100Mod;

/**
 * Patches to extend ascension level from 20 to 100
 * Validates that ascension levels don't exceed the maximum
 */
public class AscensionLevelPatch {

    /**
     * Patch ascension level validation
     * Ensure levels don't exceed 100
     */
    @SpirePatch(
            clz = AbstractPlayer.class,
            method = "getAscensionLevel"
    )
    public static class GetAscensionLevelPatch {
        @SpirePostfixPatch
        public static int Postfix(int __result, AbstractPlayer __instance) {
            // Ensure ascension level doesn't exceed our maximum
            if (__result > Ascension100Mod.MAX_ASCENSION) {
                Ascension100Mod.log("Capping ascension level from " + __result + " to " + Ascension100Mod.MAX_ASCENSION);
                return Ascension100Mod.MAX_ASCENSION;
            }
            return __result;
        }
    }
}
