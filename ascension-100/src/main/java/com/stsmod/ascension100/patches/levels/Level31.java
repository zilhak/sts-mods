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
 * Ascension Level 31: Construct monsters more resistant to debuffs
 * 인공 몬스터가 디버프에 더욱 강해집니다.
 *
 * Monsters that start with Artifact gain +1 additional Artifact
 */
public class Level31 {

    private static final Logger logger = LogManager.getLogger(Level31.class.getName());

    /**
     * Patch monster pre-battle action to add extra artifact
     */
    @SpirePatch(
        clz = AbstractMonster.class,
        method = "usePreBattleAction"
    )
    public static class ExtraArtifactPatch {
        @SpirePostfixPatch
        public static void Postfix(AbstractMonster __instance) {
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 31) {
                return;
            }

            // Check if monster already has Artifact power
            if (__instance.hasPower("Artifact")) {
                // Add 1 more Artifact
                AbstractDungeon.actionManager.addToBottom(
                    new ApplyPowerAction(__instance, __instance,
                        new ArtifactPower(__instance, 1), 1)
                );

                logger.info(String.format(
                    "Ascension 31: %s gained +1 Artifact",
                    __instance.name
                ));
            }
        }
    }

    /**
     * Sentry: Direct usePreBattleAction patch to ensure +1 Artifact
     */
    @SpirePatch(
        clz = Sentry.class,
        method = "usePreBattleAction"
    )
    public static class SentryArtifactPatch {
        @SpirePostfixPatch
        public static void Postfix(Sentry __instance) {
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 31) {
                return;
            }

            // Sentry applies Artifact 1 in its own usePreBattleAction
            // Add 1 more to make it 2 total
            AbstractDungeon.actionManager.addToBottom(
                new ApplyPowerAction(__instance, __instance,
                    new ArtifactPower(__instance, 1), 1)
            );

            logger.info("Ascension 31: Sentry gained +1 Artifact (total: 2)");
        }
    }

    /**
     * Spire Spear: +1 Artifact (spawned by Corrupt Heart)
     */
    @SpirePatch(
        clz = SpireSpear.class,
        method = "usePreBattleAction"
    )
    public static class SpireSpearArtifactPatch {
        @SpirePostfixPatch
        public static void Postfix(SpireSpear __instance) {
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 31) {
                return;
            }

            // Add +1 Artifact to Spire Spear
            AbstractDungeon.actionManager.addToBottom(
                new ApplyPowerAction(__instance, __instance,
                    new ArtifactPower(__instance, 1), 1)
            );

            logger.info("Ascension 31: Spire Spear gained +1 Artifact");
        }
    }

    /**
     * Spire Shield: +1 Artifact (spawned by Corrupt Heart)
     */
    @SpirePatch(
        clz = SpireShield.class,
        method = "usePreBattleAction"
    )
    public static class SpireShieldArtifactPatch {
        @SpirePostfixPatch
        public static void Postfix(SpireShield __instance) {
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 31) {
                return;
            }

            // Add +1 Artifact to Spire Shield
            AbstractDungeon.actionManager.addToBottom(
                new ApplyPowerAction(__instance, __instance,
                    new ArtifactPower(__instance, 1), 1)
            );

            logger.info("Ascension 31: Spire Shield gained +1 Artifact");
        }
    }
}
