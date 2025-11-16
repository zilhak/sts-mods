package com.stsmod.ascension100.patches.levels;

import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.monsters.MonsterGroup;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Ascension Level 79: Special battles can occur in ? rooms
 *
 * ?에서 특수전투가 발생할 수 있습니다.
 *
 * ?에서 발생하는 전투에서도 특수 전투가 발생할 수 있습니다.
 */
public class Level79 {
    private static final Logger logger = LogManager.getLogger(Level79.class.getName());

    /**
     * Enable special battles in ? room (EventRoom) combats
     */
    @SpirePatch(
        clz = MonsterGroup.class,
        method = "init"
    )
    public static class EnableSpecialBattleInEventRoom {
        @SpirePostfixPatch
        public static void Postfix(MonsterGroup __instance) {
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 79) {
                return;
            }

            // Check if we're in an EventRoom (? room)
            if (AbstractDungeon.getCurrRoom() instanceof com.megacrit.cardcrawl.rooms.EventRoom) {
                // Check if special battle hasn't been triggered yet
                if (!Level76.SpecialBattleTracker.isSpecialBattle) {
                    // Roll for special battle with the current probability
                    int roll = AbstractDungeon.miscRng.random(0, 99);
                    if (roll < Level76.SpecialBattleTracker.specialBattleChance) {
                        Level76.SpecialBattleTracker.isSpecialBattle = true;
                        logger.info(String.format("Ascension 79: Special Battle triggered in ? room! (Roll: %d < %d)",
                            roll, Level76.SpecialBattleTracker.specialBattleChance));
                    }
                }
            }
        }
    }
}
