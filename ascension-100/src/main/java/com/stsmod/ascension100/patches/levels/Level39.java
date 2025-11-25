package com.stsmod.ascension100.patches.levels;

import com.evacipated.cardcrawl.modthespire.lib.ByRef;
import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.vfx.campfire.CampfireSleepEffect;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Ascension Level 39: Reduced rest healing
 *
 * 휴식 시 회복량이 감소합니다.
 *
 * 휴식 시 체력 회복량이 3% 감소합니다. (30% → 27%)
 */
public class Level39 {
    private static final Logger logger = LogManager.getLogger(Level39.class.getName());

    // Original rest heal is 30% of max HP
    // At Ascension 39, reduce by 3% (30% → 27%)
    private static final float HEAL_REDUCTION = 0.03f;

    /**
     * Patch CampfireSleepEffect constructor to reduce heal amount at Ascension 39+
     */
    @SpirePatch(
        clz = CampfireSleepEffect.class,
        method = SpirePatch.CONSTRUCTOR
    )
    public static class RestHealReduction {
        @SpirePostfixPatch
        public static void Postfix(CampfireSleepEffect __instance, @ByRef int[] ___healAmount) {
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 39) {
                return;
            }

            int originalHeal = ___healAmount[0];
            int reduction = (int)(AbstractDungeon.player.maxHealth * HEAL_REDUCTION);
            ___healAmount[0] = Math.max(1, originalHeal - reduction);

            logger.info(String.format(
                "Ascension 39: Rest heal reduced from %d to %d (-%d, -3%%)",
                originalHeal, ___healAmount[0], reduction
            ));
        }
    }
}
