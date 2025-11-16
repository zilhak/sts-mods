package com.stsmod.ascension100.patches.levels;

import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;
import com.megacrit.cardcrawl.actions.common.ApplyPowerAction;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import com.megacrit.cardcrawl.monsters.ending.SpireShield;
import com.megacrit.cardcrawl.monsters.ending.SpireSpear;
import com.megacrit.cardcrawl.powers.ArtifactPower;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Ascension Level 30: Construct monsters more resistant to debuffs
 * 인공 몬스터가 디버프에 더욱 강해집니다.
 *
 * Monsters that start with Artifact gain +1 additional Artifact
 */
public class Level30 {

    private static final Logger logger = LogManager.getLogger(Level30.class.getName());

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
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 30) {
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
                    "Ascension 30: %s gained +1 Artifact",
                    __instance.name
                ));
            }
        }
    }

    /**
     * Spire Spear: +1 Artifact (spawned by Corrupt Heart)
     */
    @SpirePatch(
        clz = SpireSpear.class,
        method = SpirePatch.CONSTRUCTOR
    )
    public static class SpireSpearArtifactPatch {
        @SpirePostfixPatch
        public static void Postfix(SpireSpear __instance) {
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 30) {
                return;
            }

            // Add +1 Artifact to Spire Spear
            AbstractDungeon.actionManager.addToBottom(
                new ApplyPowerAction(__instance, __instance,
                    new ArtifactPower(__instance, 1), 1)
            );

            logger.info("Ascension 30: Spire Spear gained +1 Artifact");
        }
    }

    /**
     * Spire Shield: +1 Artifact (spawned by Corrupt Heart)
     */
    @SpirePatch(
        clz = SpireShield.class,
        method = SpirePatch.CONSTRUCTOR
    )
    public static class SpireShieldArtifactPatch {
        @SpirePostfixPatch
        public static void Postfix(SpireShield __instance) {
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 30) {
                return;
            }

            // Add +1 Artifact to Spire Shield
            AbstractDungeon.actionManager.addToBottom(
                new ApplyPowerAction(__instance, __instance,
                    new ArtifactPower(__instance, 1), 1)
            );

            logger.info("Ascension 30: Spire Shield gained +1 Artifact");
        }
    }
}
