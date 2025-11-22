package com.stsmod.ascension100.patches.levels;

import com.badlogic.gdx.math.MathUtils;
import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Ascension Level 88: Max HP decreased
 *
 * 최대 체력이 감소합니다.
 *
 * 시작시 최대 체력이 10% 감소합니다.
 */
public class Level88 {
    private static final Logger logger = LogManager.getLogger(Level88.class.getName());

    @SpirePatch(
        clz = AbstractPlayer.class,
        method = "initializeClass"
    )
    public static class MaxHPReduction {
        @SpirePostfixPatch
        public static void Postfix(AbstractPlayer __instance) {
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 88) {
                return;
            }

            // Reduce max HP by 10% directly without triggering BaseMod events
            // This prevents conflicts with other mods during initialization
            int originalMaxHP = __instance.maxHealth;
            int reduction = MathUtils.ceil(originalMaxHP * 0.1f);
            __instance.maxHealth -= reduction;
            __instance.currentHealth = __instance.maxHealth;

            logger.info(String.format(
                "Ascension 88: Starting max HP reduced from %d to %d (-%d, -10%%)",
                originalMaxHP, __instance.maxHealth, reduction
            ));
        }
    }
}
