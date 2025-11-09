package com.stsmod.ascension100.patches.levels;

import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.dungeons.Exordium;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Ascension Level 31: Start with 5 less current HP
 *
 * 더 많은 피해를 입은 상태에서 시작합니다.
 * 게임 시작시 체력이 추가로 5 감소합니다.
 */
public class Level31 {
    private static final Logger logger = LogManager.getLogger(Level31.class.getName());

    @SpirePatch(
        clz = AbstractDungeon.class,
        method = "initializeLevelSpecificChances"
    )
    public static class StartingCurrentHPReduction {
        @SpirePostfixPatch
        public static void Postfix() {
            // Only apply at the START of the game (floor 1, Exordium)
            if (AbstractDungeon.floorNum <= 1 &&
                CardCrawlGame.dungeon instanceof Exordium &&
                AbstractDungeon.isAscensionMode &&
                AbstractDungeon.ascensionLevel >= 31) {

                int reduction = 5;
                AbstractDungeon.player.currentHealth = Math.max(1,
                    AbstractDungeon.player.currentHealth - reduction);

                logger.info(String.format(
                    "Ascension 31: Starting current HP reduced by %d (current: %d)",
                    reduction, AbstractDungeon.player.currentHealth
                ));
            }
        }
    }
}
