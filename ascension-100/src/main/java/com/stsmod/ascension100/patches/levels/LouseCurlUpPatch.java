package com.stsmod.ascension100.patches.levels;

import com.evacipated.cardcrawl.modthespire.lib.*;
import com.megacrit.cardcrawl.actions.common.ApplyPowerAction;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.powers.AbstractPower;
import com.megacrit.cardcrawl.powers.CurlUpPower;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Unified Louse Curl Up patch for all Ascension levels
 *
 * Increases Curl Up amount based on Ascension level:
 * - Level 25+: +3
 * - Level 86+: +6 (total +9)
 * - Level 92+: +10 (total +19)
 */
public class LouseCurlUpPatch {
    private static final Logger logger = LogManager.getLogger(LouseCurlUpPatch.class.getName());

    @SpirePatch(
        clz = ApplyPowerAction.class,
        method = SpirePatch.CONSTRUCTOR,
        paramtypez = {AbstractCreature.class, AbstractCreature.class, AbstractPower.class}
    )
    public static class IncreaseLouseCurlUp {
        @SpirePostfixPatch
        public static void Postfix(ApplyPowerAction __instance, AbstractCreature target, AbstractCreature source, AbstractPower powerToApply) {
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 25) {
                return;
            }

            // Only apply to Louse's Curl Up power
            if (!(powerToApply instanceof CurlUpPower)) {
                return;
            }

            if (target == null || target.id == null) {
                return;
            }

            String id = target.id;
            if (!id.equals("FuzzyLouseNormal") && !id.equals("FuzzyLouseDefensive")) {
                return;
            }

            // Calculate total bonus based on Ascension level
            int bonus = 0;
            if (AbstractDungeon.ascensionLevel >= 92) {
                bonus = 3 + 6 + 10; // Level 25 + 86 + 92
            } else if (AbstractDungeon.ascensionLevel >= 86) {
                bonus = 3 + 6; // Level 25 + 86
            } else if (AbstractDungeon.ascensionLevel >= 25) {
                bonus = 3; // Level 25
            }

            if (bonus > 0) {
                int originalAmount = powerToApply.amount;
                powerToApply.amount += bonus;
                powerToApply.updateDescription();

                logger.info(String.format(
                    "Ascension %d: %s Curl Up increased from %d to %d (+%d)",
                    AbstractDungeon.ascensionLevel, target.name, originalAmount, powerToApply.amount, bonus
                ));
            }
        }
    }
}
