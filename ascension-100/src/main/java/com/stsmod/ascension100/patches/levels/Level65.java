package com.stsmod.ascension100.patches.levels;

import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;
import com.megacrit.cardcrawl.actions.common.ApplyPowerAction;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.monsters.ending.SpireShield;
import com.megacrit.cardcrawl.monsters.ending.SpireSpear;
import com.megacrit.cardcrawl.monsters.exordium.Sentry;
import com.megacrit.cardcrawl.monsters.city.SphericGuardian;
import com.megacrit.cardcrawl.monsters.city.BronzeAutomaton;
import com.megacrit.cardcrawl.monsters.beyond.Donu;
import com.megacrit.cardcrawl.monsters.beyond.Deca;
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
     * Sentry: +1 Artifact (stacks with Level 31)
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

    /**
     * Spheric Guardian: +1 Artifact (stacks with Level 31)
     */
    @SpirePatch(
        clz = SphericGuardian.class,
        method = "usePreBattleAction"
    )
    public static class SphericGuardianArtifactPatch {
        @SpirePostfixPatch
        public static void Postfix(SphericGuardian __instance) {
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 65) {
                return;
            }

            // Add +1 Artifact to Spheric Guardian (stacks with Level 31)
            AbstractDungeon.actionManager.addToBottom(
                new ApplyPowerAction(__instance, __instance,
                    new ArtifactPower(__instance, 1), 1)
            );

            logger.info("Ascension 65: Spheric Guardian gained +1 Artifact (total: 5 at A65+)");
        }
    }

    /**
     * Bronze Automaton: +1 Artifact (stacks with Level 31)
     */
    @SpirePatch(
        clz = BronzeAutomaton.class,
        method = "usePreBattleAction"
    )
    public static class BronzeAutomatonArtifactPatch {
        @SpirePostfixPatch
        public static void Postfix(BronzeAutomaton __instance) {
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 65) {
                return;
            }

            // Add +1 Artifact to Bronze Automaton (stacks with Level 27 and Level 31)
            // Total: base 3 + L27:1 + L31:1 + L65:1 = 6
            AbstractDungeon.actionManager.addToBottom(
                new ApplyPowerAction(__instance, __instance,
                    new ArtifactPower(__instance, 1), 1)
            );

            logger.info("Ascension 65: Bronze Automaton gained +1 Artifact (total: 6 at A65+)");
        }
    }

    /**
     * Donu: +1 Artifact (stacks with Level 31)
     */
    @SpirePatch(
        clz = Donu.class,
        method = "usePreBattleAction"
    )
    public static class DonuArtifactPatch {
        @SpirePostfixPatch
        public static void Postfix(Donu __instance) {
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 65) {
                return;
            }

            // Add +1 Artifact to Donu (stacks with Level 31)
            AbstractDungeon.actionManager.addToBottom(
                new ApplyPowerAction(__instance, __instance,
                    new ArtifactPower(__instance, 1), 1)
            );

            logger.info("Ascension 65: Donu gained +1 Artifact (total: 5 at A65+)");
        }
    }

    /**
     * Deca: +1 Artifact (stacks with Level 31)
     */
    @SpirePatch(
        clz = Deca.class,
        method = "usePreBattleAction"
    )
    public static class DecaArtifactPatch {
        @SpirePostfixPatch
        public static void Postfix(Deca __instance) {
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 65) {
                return;
            }

            // Add +1 Artifact to Deca (stacks with Level 31)
            AbstractDungeon.actionManager.addToBottom(
                new ApplyPowerAction(__instance, __instance,
                    new ArtifactPower(__instance, 1), 1)
            );

            logger.info("Ascension 65: Deca gained +1 Artifact (total: 5 at A65+)");
        }
    }
}
