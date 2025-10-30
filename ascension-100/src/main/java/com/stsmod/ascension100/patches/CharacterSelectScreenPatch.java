package com.stsmod.ascension100.patches;

import com.evacipated.cardcrawl.modthespire.lib.*;
import com.megacrit.cardcrawl.screens.charSelect.CharacterSelectScreen;

/**
 * Patches the character select screen to allow ascension levels up to 100
 */
public class CharacterSelectScreenPatch {

    /**
     * Patch the ascension mode update to support levels 1-100
     */
    @SpirePatch(
            clz = CharacterSelectScreen.class,
            method = "update"
    )
    public static class UpdatePatch {
        @SpirePostfixPatch
        public static void Postfix(CharacterSelectScreen __instance) {
            // This allows the UI to handle higher ascension levels
            // The actual level selection is handled by CharacterOption patches
        }
    }
}
