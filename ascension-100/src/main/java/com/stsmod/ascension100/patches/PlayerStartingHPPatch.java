package com.stsmod.ascension100.patches;

import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Patches for starting HP reduction (Level 31 & 39)
 *
 * These patches apply ONCE at the start of the game when the player character is initialized.
 */
public class PlayerStartingHPPatch {

    private static final Logger logger = LogManager.getLogger(PlayerStartingHPPatch.class.getName());

    /**
     * Level 31 & 39: Reduce starting HP ONCE at game start
     *
     * This patch is applied in AbstractPlayer.initializeClass() which is called
     * when starting a new run (same location as Ascension 14's HP loss).
     */
    @SpirePatch(
        clz = AbstractPlayer.class,
        method = "initializeClass"
    )
    public static class GameStartHPReduction {
        @SpirePostfixPatch
        public static void Postfix(AbstractPlayer __instance) {
            if (!AbstractDungeon.isAscensionMode) {
                return;
            }

            int level = AbstractDungeon.ascensionLevel;

            // Level 31: -5 current HP (damage taken at start)
            if (level >= 31) {
                int reduction = 5;
                __instance.currentHealth = Math.max(1, __instance.currentHealth - reduction);

                logger.info(String.format(
                    "Ascension 31: Starting current HP reduced by %d (current: %d/%d)",
                    reduction, __instance.currentHealth, __instance.maxHealth
                ));
            }

            // Level 39: -5 max HP (permanent reduction)
            if (level >= 39) {
                int reduction = 5;
                __instance.decreaseMaxHealth(reduction);

                logger.info(String.format(
                    "Ascension 39: Starting max HP reduced by %d (max: %d)",
                    reduction, __instance.maxHealth
                ));
            }
        }
    }
}
