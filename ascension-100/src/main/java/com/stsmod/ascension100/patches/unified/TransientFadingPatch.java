package com.stsmod.ascension100.patches.unified;

import com.evacipated.cardcrawl.modthespire.lib.*;
import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.actions.common.ApplyPowerAction;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.monsters.beyond.Transient;
import com.megacrit.cardcrawl.powers.FadingPower;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.reflect.Field;

/**
 * Unified patch for Transient's Fading power across multiple ascension levels
 *
 * Intercepts FadingPower creation in Transient.usePreBattleAction() and modifies
 * the initial amount parameter before it's applied.
 *
 * Ascension Level Bonuses:
 * - Level 26: +1 Fading (Base 5/6 → 6/7)
 * - Level 53: No change
 * - Level 86: +1 Fading (cumulative, total +2)
 * - Level 92: +1 Fading (cumulative, total +3)
 *
 * Implementation Strategy:
 * Uses SpirePrefixPatch on ApplyPowerAction constructor to intercept FadingPower
 * application from Transient and modify the amount parameter.
 */
public class TransientFadingPatch {
    private static final Logger logger = LogManager.getLogger(TransientFadingPatch.class.getName());

    /**
     * Intercept ApplyPowerAction constructor when Transient applies FadingPower
     * and increase the amount based on ascension level
     */
    @SpirePatch(
        clz = ApplyPowerAction.class,
        method = SpirePatch.CONSTRUCTOR,
        paramtypez = {AbstractCreature.class, AbstractCreature.class, com.megacrit.cardcrawl.powers.AbstractPower.class, int.class}
    )
    public static class InterceptFadingApplication {
        @SpirePostfixPatch
        public static void Postfix(
            ApplyPowerAction __instance,
            AbstractCreature target,
            AbstractCreature source,
            com.megacrit.cardcrawl.powers.AbstractPower powerToApply,
            int stackAmount
        ) {
            // Only apply in Ascension mode
            if (!AbstractDungeon.isAscensionMode) {
                return;
            }

            // Check if this is FadingPower being applied to Transient
            if (!(powerToApply instanceof FadingPower)) {
                return;
            }

            if (!(target instanceof Transient)) {
                return;
            }

            // Calculate bonus based on ascension level
            int bonus = 0;

            if (AbstractDungeon.ascensionLevel >= 92) {
                bonus = 3; // Level 26 (+1) + Level 86 (+1) + Level 92 (+1)
            } else if (AbstractDungeon.ascensionLevel >= 86) {
                bonus = 2; // Level 26 (+1) + Level 86 (+1)
            } else if (AbstractDungeon.ascensionLevel >= 26) {
                bonus = 1; // Level 26 only
            }

            if (bonus > 0) {
                try {
                    // Modify the power's amount
                    powerToApply.amount += bonus;
                    powerToApply.updateDescription();

                    // Modify the action's amount field
                    Field amountField = ApplyPowerAction.class.getDeclaredField("amount");
                    amountField.setAccessible(true);
                    int currentAmount = amountField.getInt(__instance);
                    amountField.setInt(__instance, currentAmount + bonus);

                    logger.info(String.format(
                        "Ascension %d: Transient Fading increased by +%d to %d turns (Base: %d)",
                        AbstractDungeon.ascensionLevel,
                        bonus,
                        powerToApply.amount,
                        powerToApply.amount - bonus
                    ));
                } catch (Exception e) {
                    logger.error("Failed to modify Transient Fading amount", e);
                }
            }
        }
    }
}
