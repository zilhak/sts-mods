package com.stsmod.ascension100.patches.levels;

import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.rooms.MonsterRoom;
import com.megacrit.cardcrawl.rooms.MonsterRoomElite;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Ascension Level 78: Special battle probability increases
 *
 * 특수전투의 확률이 높아집니다.
 *
 * 특수전투의 발생확률이 20%가 됩니다.
 */
public class Level78 {
    private static final Logger logger = LogManager.getLogger(Level78.class.getName());

    /**
     * Increase special battle probability from 15% to 20%
     */
    @SpirePatch(
        clz = MonsterRoom.class,
        method = "onPlayerEntry"
    )
    public static class IncreaseSpecialBattleProbability {
        @SpirePostfixPatch
        public static void Postfix(MonsterRoom __instance) {
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 78) {
                return;
            }

            // Increase probability to 20%
            if (!(__instance instanceof MonsterRoomElite)) {
                Level76.SpecialBattleTracker.specialBattleChance = 20;
                logger.info("Ascension 78: Special Battle probability increased to 20%");
            }
        }
    }
}
