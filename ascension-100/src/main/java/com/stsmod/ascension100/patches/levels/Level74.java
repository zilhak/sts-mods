package com.stsmod.ascension100.patches.levels;

import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.rooms.AbstractRoom;
import com.megacrit.cardcrawl.rooms.MonsterRoom;
import com.megacrit.cardcrawl.rooms.MonsterRoomElite;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Ascension Level 74: Rare card drop rate decreased
 *
 * 희귀카드가 등장할 확률이 낮아집니다.
 *
 * 일반 전투 승리 카드 보상에서 희귀카드 등장할 확률이 약간 낮아지며, 언커먼 카드 등장할 확률이 약간 증가합니다.
 *
 * Base probabilities:
 * - Rare: 3% → 2%
 * - Uncommon: 37% → 38%
 * - Common: 60% → 60%
 */
public class Level74 {
    private static final Logger logger = LogManager.getLogger(Level74.class.getName());

    /**
     * Patch normal combat rooms to reduce rare card chance
     */
    @SpirePatch(
        clz = MonsterRoom.class,
        method = "onPlayerEntry"
    )
    public static class ReduceRareCardChanceNormal {
        @SpirePostfixPatch
        public static void Postfix(MonsterRoom __instance) {
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 74) {
                return;
            }

            // Reduce rare card chance from 3% to 2%
            __instance.baseRareCardChance = 2;

            // Increase uncommon card chance from 37% to 38%
            __instance.baseUncommonCardChance = 38;

            logger.info("Ascension 74: Adjusted card rarity chances for normal combat (Rare: 2%, Uncommon: 38%)");
        }
    }

    /**
     * Patch elite combat rooms to reduce rare card chance
     */
    @SpirePatch(
        clz = MonsterRoomElite.class,
        method = "onPlayerEntry"
    )
    public static class ReduceRareCardChanceElite {
        @SpirePostfixPatch
        public static void Postfix(MonsterRoomElite __instance) {
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 74) {
                return;
            }

            // Reduce rare card chance from 3% to 2%
            __instance.baseRareCardChance = 2;

            // Increase uncommon card chance from 37% to 38%
            __instance.baseUncommonCardChance = 38;

            logger.info("Ascension 74: Adjusted card rarity chances for elite combat (Rare: 2%, Uncommon: 38%)");
        }
    }
}
