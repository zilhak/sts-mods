package com.stsmod.ascension100.patches.levels;

import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.ui.campfire.RestOption;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Ascension Level 47: Resting reduces max HP
 *
 * 휴식 장소에서 쉴 때마다 최대체력이 감소합니다.
 * 휴식장소에서 휴식을 할 때마다 최대체력이 2 감소합니다.
 */
public class Level47 {
    private static final Logger logger = LogManager.getLogger(Level47.class.getName());

    @SpirePatch(
        clz = RestOption.class,
        method = "useOption"
    )
    public static class RestMaxHPPenalty {
        @SpirePostfixPatch
        public static void Postfix(Object __instance) {
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 47) {
                return;
            }

            // Decrease max HP by 2
            AbstractDungeon.player.decreaseMaxHealth(2);

            logger.info(String.format(
                "Ascension 47: Max HP reduced by 2 from resting (new max: %d)",
                AbstractDungeon.player.maxHealth
            ));
        }
    }
}
