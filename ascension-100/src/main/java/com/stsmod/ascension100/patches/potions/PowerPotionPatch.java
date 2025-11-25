package com.stsmod.ascension100.patches.potions;

import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;
import com.evacipated.cardcrawl.modthespire.lib.SpireReturn;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.potions.PowerPotion;
import com.stsmod.ascension100.actions.ReducedDiscoveryAction;

/**
 * Patch for PowerPotion to reduce card choices from 3 to 2 at Ascension 25+
 * Part of Level 25 effect: Card selection potions show 1 fewer option
 */
public class PowerPotionPatch {

    /**
     * Replace the original use() method with our custom implementation
     * Only applies at Ascension 25+
     */
    @SpirePatch(
        clz = PowerPotion.class,
        method = "use"
    )
    public static class UseHook {
        public static SpireReturn<Void> Prefix(PowerPotion __instance, AbstractCreature target) {
            // Only apply at Ascension 25+
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 25) {
                return SpireReturn.Continue();
            }

            // Get potency (1 normally, 2 with Sacred Bark)
            int potency = __instance.getPotency();

            // Add our custom ReducedDiscoveryAction with 2 card choices instead of 3
            AbstractDungeon.actionManager.addToBottom(
                new ReducedDiscoveryAction(AbstractCard.CardType.POWER, potency, 2)
            );

            // Return early to skip the original method
            return SpireReturn.Return(null);
        }
    }

    /**
     * Update description text to show "2" instead of "3" at Ascension 25+
     * Handles both English ("#b3" -> "#b2") and Korean ("셋" -> "둘")
     */
    @SpirePatch(
        clz = PowerPotion.class,
        method = "initializeData"
    )
    public static class DescriptionHook {
        @SpirePostfixPatch
        public static void Postfix(PowerPotion __instance) {
            // Only apply at Ascension 25+
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 25) {
                return;
            }

            // Replace "3" with "2" in the description (English)
            // Replace "셋" with "둘" (Korean)
            if (__instance.description != null) {
                __instance.description = __instance.description
                    .replace("#b3", "#b2")
                    .replace("셋", "둘");
            }
        }
    }
}
