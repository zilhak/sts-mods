package com.stsmod.ascension100.patches.levels;

import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.rooms.MonsterRoom;
import com.megacrit.cardcrawl.rooms.MonsterRoomElite;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Ascension Level 97: Luck decreased (rare card probability further reduced)
 *
 * 운이 감소합니다.
 *
 * 카드 보상에서 레어 카드 등장 확률이 감소합니다.
 *
 * Card probabilities:
 * - Rare: 2% → 1%
 * - Uncommon: 38% → 39%
 * - Common: 60% → 60%
 */
public class Level97 {
    private static final Logger logger = LogManager.getLogger(Level97.class.getName());

    /**
     * Patch normal combat rooms to further reduce rare card chance
     */
    @SpirePatch(
        clz = MonsterRoom.class,
        method = "onPlayerEntry"
    )
    public static class ReduceRareCardChanceNormal {
        @SpirePostfixPatch
        public static void Postfix(MonsterRoom __instance) {
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 97) {
                return;
            }

            // Further reduce rare card chance from 2% to 1%
            __instance.baseRareCardChance = 1;

            // Increase uncommon card chance from 38% to 39%
            __instance.baseUncommonCardChance = 39;

            logger.info("Ascension 97: Further adjusted card rarity chances for normal combat (Rare: 1%, Uncommon: 39%)");
        }
    }

    /**
     * Patch elite combat rooms to further reduce rare card chance
     */
    @SpirePatch(
        clz = MonsterRoomElite.class,
        method = "onPlayerEntry"
    )
    public static class ReduceRareCardChanceElite {
        @SpirePostfixPatch
        public static void Postfix(MonsterRoomElite __instance) {
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 97) {
                return;
            }

            // Further reduce rare card chance from 2% to 1%
            __instance.baseRareCardChance = 1;

            // Increase uncommon card chance from 38% to 39%
            __instance.baseUncommonCardChance = 39;

            logger.info("Ascension 97: Further adjusted card rarity chances for elite combat (Rare: 1%, Uncommon: 39%)");
        }
    }
}
