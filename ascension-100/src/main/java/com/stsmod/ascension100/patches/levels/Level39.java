package com.stsmod.ascension100.patches.levels;

import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.dungeons.Exordium;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Ascension Level 39: Start with 5 less max HP
 *
 * 최대 체력이 감소합니다.
 * 게임 시작시 최대 체력이 추가로 5 감소합니다.
 */
public class Level39 {
    private static final Logger logger = LogManager.getLogger(Level39.class.getName());

    @SpirePatch(
        clz = AbstractDungeon.class,
        method = "initializeLevelSpecificChances"
    )
    public static class StartingMaxHPReduction {
        @SpirePostfixPatch
        public static void Postfix() {
            // Only apply at the START of the game (floor 1, Exordium)
            if (AbstractDungeon.floorNum <= 1 &&
                CardCrawlGame.dungeon instanceof Exordium &&
                AbstractDungeon.isAscensionMode &&
                AbstractDungeon.ascensionLevel >= 39) {

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
