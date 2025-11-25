package com.stsmod.ascension100.patches.levels;

import com.badlogic.gdx.math.MathUtils;
import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.dungeons.Exordium;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Unified Player HP initialization patch for Ascension 24, 32, and 88
 *
 * This patch executes AFTER vanilla ascension penalties in AbstractDungeon.dungeonTransitionSetup()
 * to ensure correct HP values.
 *
 * Vanilla penalties (applied in dungeonTransitionSetup at floorNum <= 1):
 * - Ascension 14+: Max HP reduction (getAscensionMaxHPLoss())
 * - Ascension 6+: Current HP = Max HP * 0.9
 *
 * Our modifications (applied after vanilla penalties):
 * 1. Level 24: Max HP +5% (restores HP lost from A14 penalty)
 * 2. Level 32: Current HP -5
 * 3. Level 88: Max HP -10%, Current HP = Max HP
 *
 * Note: Level 39 (rest heal reduction) is implemented separately in Level39.java
 */
@SpirePatch(
    clz = AbstractDungeon.class,
    method = "dungeonTransitionSetup"
)
public class PlayerHPInitPatch {
    private static final Logger logger = LogManager.getLogger(PlayerHPInitPatch.class.getName());

    @SpirePostfixPatch
    public static void Postfix() {
        // Only apply on first floor
        if (AbstractDungeon.floorNum > 1) {
            return;
        }

        // Only apply in Exordium (first act)
        if (!(CardCrawlGame.dungeon instanceof Exordium)) {
            return;
        }

        if (!AbstractDungeon.isAscensionMode) {
            return;
        }

        int level = AbstractDungeon.ascensionLevel;
        AbstractPlayer p = AbstractDungeon.player;
        int originalMaxHP = p.maxHealth;
        int originalCurrentHP = p.currentHealth;

        // Level 24: Max HP +5% (restores HP lost from A14 penalty)
        if (level >= 24) {
            int increase = MathUtils.ceil(originalMaxHP * 0.05f);
            p.maxHealth += increase;
            p.currentHealth += increase;

            logger.info(String.format(
                "Ascension 24: Max HP increased from %d to %d (+%d, +5%%)",
                originalMaxHP, p.maxHealth, increase
            ));
        }

        // Level 32: Current HP -5 (after Level 24 increase)
        if (level >= 32) {
            int beforeReduction = p.currentHealth;
            p.currentHealth -= 5;

            // Ensure HP doesn't go below 1
            if (p.currentHealth < 1) {
                p.currentHealth = 1;
            }

            logger.info(String.format(
                "Ascension 32: Starting HP reduced from %d to %d (-5)",
                beforeReduction, p.currentHealth
            ));
        }

        // Level 88: Max HP -10%, Current HP = Max HP (full heal)
        if (level >= 88) {
            int beforeMaxHP = p.maxHealth;
            int reduction = MathUtils.ceil(beforeMaxHP * 0.1f);

            p.maxHealth -= reduction;
            p.currentHealth = p.maxHealth;

            // Ensure HP doesn't go below 1
            if (p.maxHealth < 1) {
                p.maxHealth = 1;
                p.currentHealth = 1;
            }

            logger.info(String.format(
                "Ascension 88: Max HP reduced from %d to %d (-%d, -10%%), Current HP set to Max HP",
                beforeMaxHP, p.maxHealth, reduction
            ));
        }

        // Log final result
        if (level >= 24 || level >= 32 || level >= 88) {
            logger.info(String.format(
                "Final HP: %d/%d (from %d/%d)",
                p.currentHealth, p.maxHealth,
                originalCurrentHP, originalMaxHP
            ));
        }
    }
}
