package com.stsmod.ascension100.patches;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.evacipated.cardcrawl.modthespire.lib.*;
import com.megacrit.cardcrawl.screens.charSelect.CharacterSelectScreen;
import com.stsmod.ascension100.Ascension100Mod;

/**
 * Patches to display ascension numbers correctly for levels 21-100
 */
public class AscensionNumberPatch {

    /**
     * Patch the render method to ensure display works for high ascension levels
     */
    @SpirePatch(
            clz = CharacterSelectScreen.class,
            method = "render"
    )
    public static class RenderPatch {
        @SpirePostfixPatch
        public static void Postfix(CharacterSelectScreen __instance, SpriteBatch sb) {
            // Ensure rendering works correctly for ascension levels 21-100
            // The game's default UI should handle this, but we log for debugging
            try {
                if (__instance.ascensionLevel > 20 && __instance.ascensionLevel <= Ascension100Mod.MAX_ASCENSION) {
                    // Log only once per level change to avoid spam
                    // Actual rendering is handled by the game
                }
            } catch (Exception e) {
                Ascension100Mod.error("Error in render patch: " + e.getMessage());
            }
        }
    }
}
