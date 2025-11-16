package com.stsmod.ascension100.patches.levels;

import com.evacipated.cardcrawl.modthespire.lib.SpireField;
import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import com.megacrit.cardcrawl.rooms.AbstractRoom;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Ascension Level 84: Potion drop rate decreased
 *
 * 포션이 더 적은 확률로 드롭됩니다.
 *
 * 포션 드롭 확률이 10% 감소합니다.
 * 기본 확률: 40% → 30%
 */
public class Level84 {
    private static final Logger logger = LogManager.getLogger(Level84.class.getName());

    /**
     * Modify base potion drop chance in AbstractRoom
     * We'll override the addPotionToRewards to reduce the base chance from 40 to 30
     */
    @SpirePatch(
        clz = AbstractRoom.class,
        method = "addPotionToRewards",
        paramtypez = {}
    )
    public static class ReducePotionDropRate {
        // We need to use a different approach since we can't directly modify local variables
        // Let's patch blizzardPotionMod to always subtract 10 on ascension 84+
    }

    /**
     * Alternative approach: Patch the monster defeat to adjust potion blizzard mod
     */
    @SpirePatch(
        clz = AbstractMonster.class,
        method = "die",
        paramtypez = {boolean.class}
    )
    public static class AdjustPotionBlizzardMod {
        @SpirePostfixPatch
        public static void Postfix(AbstractMonster __instance, boolean triggerRelics) {
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 84) {
                return;
            }

            // Reduce the blizzard potion modifier by 10 to effectively lower base chance
            // This creates a persistent -10% penalty to potion drops
            // Since base is 40% and we want 30%, we need -10%
            // We do this once per combat
            if (__instance.type == AbstractMonster.EnemyType.BOSS ||
                __instance.type == AbstractMonster.EnemyType.ELITE ||
                __instance.type == AbstractMonster.EnemyType.NORMAL) {

                // Check if all monsters are dead/dying
                boolean allDead = true;
                for (AbstractMonster m : AbstractDungeon.getMonsters().monsters) {
                    if (!m.isDying && !m.isDead) {
                        allDead = false;
                        break;
                    }
                }

                if (allDead) {
                    // Subtract 10 from blizzard mod to reduce potion chance by 10%
                    // This will be applied before the potion drop roll
                    com.megacrit.cardcrawl.rooms.AbstractRoom.blizzardPotionMod -= 10;

                    logger.info("Ascension 84: Reduced potion drop chance by 10% (blizzardPotionMod adjusted)");
                }
            }
        }
    }
}
