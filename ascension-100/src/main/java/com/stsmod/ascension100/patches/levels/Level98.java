package com.stsmod.ascension100.patches.levels;

import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.vfx.campfire.CampfireSleepEffect;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Ascension Level 98: Campfire rest healing reduced
 *
 * 모닥불의 회복효과가 줄어듭니다.
 *
 * 모닥불에서 휴식시 회복 수치가 5 감소합니다.
 */
public class Level98 {
    private static final Logger logger = LogManager.getLogger(Level98.class.getName());

    @SpirePatch(
        clz = CampfireSleepEffect.class,
        method = SpirePatch.CONSTRUCTOR
    )
    public static class ReduceRestHealing {
        @SpirePostfixPatch
        public static void Postfix(CampfireSleepEffect __instance) {
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 98) {
                return;
            }

            try {
                // Access private healAmount field using reflection
                java.lang.reflect.Field healAmountField = CampfireSleepEffect.class.getDeclaredField("healAmount");
                healAmountField.setAccessible(true);

                int originalHeal = healAmountField.getInt(__instance);
                int newHeal = Math.max(1, originalHeal - 5);
                healAmountField.setInt(__instance, newHeal);

                logger.info(String.format(
                    "Ascension 98: Campfire rest healing reduced from %d to %d (-5)",
                    originalHeal, newHeal
                ));
            } catch (Exception e) {
                logger.error("Failed to modify campfire healing", e);
            }
        }
    }
}
