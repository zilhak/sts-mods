package com.stsmod.ascension100.patches;

import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;
import com.megacrit.cardcrawl.actions.common.ApplyPowerAction;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import com.megacrit.cardcrawl.powers.ArtifactPower;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Ascension Level 30: Construct monsters are more resistant to debuffs
 * Monsters that start with Artifact gain +1 additional Artifact
 */
public class Level30ArtifactPatch {

    private static final Logger logger = LogManager.getLogger(Level30ArtifactPatch.class.getName());

    /**
     * Patch monster pre-battle action to add extra artifact
     */
    @SpirePatch(
        cls = "com.megacrit.cardcrawl.monsters.AbstractMonster",
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
}
