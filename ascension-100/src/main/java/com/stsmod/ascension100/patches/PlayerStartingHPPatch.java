package com.stsmod.ascension100.patches;

import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.dungeons.Exordium;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Patches for starting HP reduction (Level 31 & 39)
 *
 * These patches apply ONCE at the start of the game, NOT every battle.
 * Patched into AbstractDungeon.initializeLevelSpecificChances() which is called
 * only when starting a new run at floor 1.
 */
public class PlayerStartingHPPatch {

    private static final Logger logger = LogManager.getLogger(PlayerStartingHPPatch.class.getName());

    /**
     * Level 31 & 39: Reduce starting HP ONCE at game start
     *
     * This patch is applied in the same location as Ascension 14's HP loss
     * (AbstractDungeon.initializeLevelSpecificChances, when floorNum <= 1)
     */
    @SpirePatch(
        cls = "com.megacrit.cardcrawl.dungeons.AbstractDungeon",
        method = "initializeLevelSpecificChances"
    )
    public static class GameStartHPReduction {
        @SpirePostfixPatch
        public static void Postfix() {
            // Only apply at the START of the game (floor 1, Exordium)
            if (AbstractDungeon.floorNum <= 1 &&
                CardCrawlGame.dungeon instanceof Exordium &&
                AbstractDungeon.isAscensionMode) {

                int level = AbstractDungeon.ascensionLevel;

                // Level 31: -5 current HP (damage taken at start)
                if (level >= 31) {
                    int reduction = 5;
                    AbstractDungeon.player.currentHealth = Math.max(1,
                        AbstractDungeon.player.currentHealth - reduction);

                    logger.info(String.format(
                        "Ascension 31: Starting current HP reduced by %d (current: %d)",
                        reduction, AbstractDungeon.player.currentHealth
                    ));
                }

                // Level 39: -5 max HP (permanent reduction)
                if (level >= 39) {
                    int reduction = 5;
                    AbstractDungeon.player.decreaseMaxHealth(reduction);

                    logger.info(String.format(
                        "Ascension 39: Starting max HP reduced by %d (max: %d)",
                        reduction, AbstractDungeon.player.maxHealth
                    ));
                }
            }
        }
    }
}
