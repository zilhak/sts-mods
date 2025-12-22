package com.stsmod.ascension100.patches.levels;

import com.badlogic.gdx.math.MathUtils;
import com.evacipated.cardcrawl.modthespire.lib.*;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import javassist.CtBehavior;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;

/**
 * Ascension Level 38: Boss heal decreased
 *
 * 보스 전투 이후에 회복량이 감소합니다.
 *
 * Vanilla: Ascension 5+ heals 75% of missing HP after boss
 * Level 38: Reduces heal multiplier from 0.75 to 0.70 (70% of missing HP)
 *
 * Implementation: Patches AbstractDungeon.dungeonTransitionSetup() where boss heal occurs
 */
public class Level38 {
    private static final Logger logger = LogManager.getLogger(Level38.class.getName());

    /**
     * Reduce boss victory heal from 75% to 70% (additional -5%)
     *
     * Vanilla code at AbstractDungeon.dungeonTransitionSetup():
     * if (ascensionLevel >= 5) {
     *     player.heal(MathUtils.round((player.maxHealth - player.currentHealth) * 0.75F), false);
     * }
     *
     * This patch intercepts the heal amount calculation and reduces it further
     */
    @SpirePatch(
        clz = AbstractDungeon.class,
        method = "dungeonTransitionSetup"
    )
    public static class BossHealReduction {
        @SpireInsertPatch(
            locator = HealLocator.class
        )
        public static void Insert() {
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 38) {
                return;
            }

            // Calculate the additional -5% reduction
            // Vanilla: 75% heal
            // Level 38: 70% heal (multiply by 0.70/0.75 = 0.9333...)
            int vanillaHeal = MathUtils.round(
                (AbstractDungeon.player.maxHealth - AbstractDungeon.player.currentHealth) * 0.75F
            );

            int reducedHeal = MathUtils.round(
                (AbstractDungeon.player.maxHealth - AbstractDungeon.player.currentHealth) * 0.70F
            );

            int reduction = vanillaHeal - reducedHeal;

            // Apply the reduction by damaging the player
            // (heal will happen right after this insertion, so we pre-damage)
            if (reduction > 0) {
                AbstractDungeon.player.currentHealth -= reduction;
                if (AbstractDungeon.player.currentHealth < 1) {
                    AbstractDungeon.player.currentHealth = 1;
                }

                logger.info(String.format(
                    "Ascension 38: Boss heal reduced by %d HP (75%% → 70%%). Will heal %d instead of %d",
                    reduction, reducedHeal, vanillaHeal
                ));
            }
        }
    }

    /**
     * Locator to find the exact line where boss heal occurs
     * Target: player.heal(MathUtils.round((player.maxHealth - player.currentHealth) * 0.75F), false);
     */
    private static class HealLocator extends SpireInsertLocator {
        @Override
        public int[] Locate(CtBehavior ctBehavior) throws Exception {
            Matcher finalMatcher = new Matcher.MethodCallMatcher(
                com.megacrit.cardcrawl.characters.AbstractPlayer.class,
                "heal"
            );
            return LineFinder.findInOrder(ctBehavior, new ArrayList<>(), finalMatcher);
        }
    }
}
