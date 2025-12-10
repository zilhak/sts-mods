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
 * Ascension Level 31: Construct monsters more resistant to debuffs
 * 인공 몬스터가 디버프에 더욱 강해집니다.
 *
 * Monsters that start with Artifact gain +1 additional Artifact
 */
public class Level31 {

    private static final Logger logger = LogManager.getLogger(Level31.class.getName());

    /**
     * Sentry: +1 Artifact
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

    /**
     * Spheric Guardian: +1 Artifact
     */
    @SpirePatch(
        clz = SphericGuardian.class,
        method = "usePreBattleAction"
    )
    public static class SphericGuardianArtifactPatch {
        @SpirePostfixPatch
        public static void Postfix(SphericGuardian __instance) {
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 31) {
                return;
            }

            // Add +1 Artifact to Spheric Guardian
            AbstractDungeon.actionManager.addToBottom(
                new ApplyPowerAction(__instance, __instance,
                    new ArtifactPower(__instance, 1), 1)
            );

            logger.info("Ascension 31: Spheric Guardian gained +1 Artifact (total: 4)");
        }
    }

    /**
     * Bronze Automaton: +1 Artifact
     */
    @SpirePatch(
        clz = BronzeAutomaton.class,
        method = "usePreBattleAction"
    )
    public static class BronzeAutomatonArtifactPatch {
        @SpirePostfixPatch
        public static void Postfix(BronzeAutomaton __instance) {
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 31) {
                return;
            }

            // Add +1 Artifact to Bronze Automaton
            // Note: Level 27 already adds +1, so this makes total 5 (base 3 + L27:1 + L31:1)
            AbstractDungeon.actionManager.addToBottom(
                new ApplyPowerAction(__instance, __instance,
                    new ArtifactPower(__instance, 1), 1)
            );

            logger.info("Ascension 31: Bronze Automaton gained +1 Artifact (total: 5 at A31+)");
        }
    }

    /**
     * Donu: +1 Artifact
     */
    @SpirePatch(
        clz = Donu.class,
        method = "usePreBattleAction"
    )
    public static class DonuArtifactPatch {
        @SpirePostfixPatch
        public static void Postfix(Donu __instance) {
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 31) {
                return;
            }

            // Add +1 Artifact to Donu
            AbstractDungeon.actionManager.addToBottom(
                new ApplyPowerAction(__instance, __instance,
                    new ArtifactPower(__instance, 1), 1)
            );

            logger.info("Ascension 31: Donu gained +1 Artifact (total: 3 at A19-30, 4 at A31+)");
        }
    }

    /**
     * Deca: +1 Artifact
     */
    @SpirePatch(
        clz = Deca.class,
        method = "usePreBattleAction"
    )
    public static class DecaArtifactPatch {
        @SpirePostfixPatch
        public static void Postfix(Deca __instance) {
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 31) {
                return;
            }

            // Add +1 Artifact to Deca
            AbstractDungeon.actionManager.addToBottom(
                new ApplyPowerAction(__instance, __instance,
                    new ArtifactPower(__instance, 1), 1)
            );

            logger.info("Ascension 31: Deca gained +1 Artifact (total: 3 at A19-30, 4 at A31+)");
        }
    }
}
