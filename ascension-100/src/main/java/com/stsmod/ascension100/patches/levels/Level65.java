package com.stsmod.ascension100.patches.levels;

import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;
import com.megacrit.cardcrawl.actions.common.ApplyPowerAction;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import com.megacrit.cardcrawl.monsters.ending.SpireShield;
import com.megacrit.cardcrawl.monsters.ending.SpireSpear;
import com.megacrit.cardcrawl.monsters.exordium.Sentry;
import com.megacrit.cardcrawl.powers.ArtifactPower;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Ascension Level 65: Artifact monsters stronger
 *
 * 인공 몬스터는 디버프에 더욱 강해집니다.
 * 시작시 인공물을 얻는 몬스터들은 인공물을 1씩 더 얻습니다.
 *
 * This effect stacks with Level 31, so at Ascension 65+:
 * - Level 31 adds +1 Artifact
 * - Level 65 adds +1 Artifact
 * - Total: +2 Artifact for monsters that have it
 */
public class Level65 {
    private static final Logger logger = LogManager.getLogger(Level65.class.getName());

    /**
     * Add +1 Artifact to monsters that have Artifact power
     * Stacks with Level 30 effect
     */
    @SpirePatch(
        clz = AbstractMonster.class,
        method = "usePreBattleAction"
    )
    public static class ExtraArtifactPatch {
        @SpirePostfixPatch
        public static void Postfix(AbstractMonster __instance) {
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 65) {
                return;
            }

            // Check if monster already has Artifact power
            if (__instance.hasPower("Artifact")) {
                // Add 1 more Artifact (stacks with Level 30)
                AbstractDungeon.actionManager.addToBottom(
                    new ApplyPowerAction(__instance, __instance,
                        new ArtifactPower(__instance, 1), 1)
                );

                logger.info(String.format(
                    "Ascension 65: %s gained +1 Artifact (stacks with Level 31)",
                    __instance.name
                ));
            }
        }
    }

    /**
     * Sentry: Direct usePreBattleAction patch to ensure +1 Artifact
     * Stacks with Level 31
     */
    @SpirePatch(
        clz = Sentry.class,
        method = "usePreBattleAction"
    )
    public static class SentryArtifactPatch {
        @SpirePostfixPatch
        public static void Postfix(Sentry __instance) {
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 65) {
                return;
            }

            // Sentry applies Artifact 1 in its own usePreBattleAction
            // Add 1 more (stacks with Level 31 for total +2)
            AbstractDungeon.actionManager.addToBottom(
                new ApplyPowerAction(__instance, __instance,
                    new ArtifactPower(__instance, 1), 1)
            );

            logger.info("Ascension 65: Sentry gained +1 Artifact (stacks with Level 31)");
        }
    }

    /**
     * Spire Spear: +1 Artifact (stacks with Level 31)
     */
    @SpirePatch(
        clz = SpireSpear.class,
        method = "usePreBattleAction"
    )
    public static class SpireSpearArtifactPatch {
        @SpirePostfixPatch
        public static void Postfix(SpireSpear __instance) {
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 65) {
                return;
            }

            // Add +1 Artifact to Spire Spear (stacks with Level 30)
            AbstractDungeon.actionManager.addToBottom(
                new ApplyPowerAction(__instance, __instance,
                    new ArtifactPower(__instance, 1), 1)
            );

            logger.info("Ascension 65: Spire Spear gained +1 Artifact (stacks with Level 31)");
        }
    }

    /**
     * Spire Shield: +1 Artifact (stacks with Level 31)
     */
    @SpirePatch(
        clz = SpireShield.class,
        method = "usePreBattleAction"
    )
    public static class SpireShieldArtifactPatch {
        @SpirePostfixPatch
        public static void Postfix(SpireShield __instance) {
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 65) {
                return;
            }

            // Add +1 Artifact to Spire Shield (stacks with Level 30)
            AbstractDungeon.actionManager.addToBottom(
                new ApplyPowerAction(__instance, __instance,
                    new ArtifactPower(__instance, 1), 1)
            );

            logger.info("Ascension 65: Spire Shield gained +1 Artifact (stacks with Level 31)");
        }
    }
}
