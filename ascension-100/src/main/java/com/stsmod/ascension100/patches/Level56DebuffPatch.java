package com.stsmod.ascension100.patches;

import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.evacipated.cardcrawl.modthespire.lib.SpireReturn;
import com.megacrit.cardcrawl.cards.DamageInfo;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import com.megacrit.cardcrawl.powers.AbstractPower;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Ascension Level 56: Debuff effects are enhanced
 * - Vulnerable: +10% additional damage
 * - Weakened: +10% additional damage reduction
 * - Frail: +10% additional block reduction
 */
public class Level56DebuffPatch {

    private static final Logger logger = LogManager.getLogger(Level56DebuffPatch.class.getName());

    /**
     * Patch Vulnerable power to deal 10% more damage at Asc 56+
     */
    @SpirePatch(
        cls = "com.megacrit.cardcrawl.powers.VulnerablePower",
        method = "atDamageReceive"
    )
    public static class VulnerableEnhancePatch {
        public static float Postfix(float __result, AbstractPower __instance, float damage, DamageInfo.DamageType type) {
            if (AbstractDungeon.isAscensionMode && AbstractDungeon.ascensionLevel >= 56) {
                // Original vulnerable is 50%, add 10% more for total 60%
                return __result * 1.1f;
            }
            return __result;
        }
    }

    /**
     * Patch Weakened power to reduce 10% more damage at Asc 56+
     */
    @SpirePatch(
        cls = "com.megacrit.cardcrawl.powers.WeakPower",
        method = "atDamageGive"
    )
    public static class WeakenedEnhancePatch {
        public static float Postfix(float __result, AbstractPower __instance, float damage, DamageInfo.DamageType type) {
            if (AbstractDungeon.isAscensionMode && AbstractDungeon.ascensionLevel >= 56) {
                // Original weak is 25% reduction, make it 35% reduction
                // __result is already reduced by 25%, so multiply by 0.9 to reduce 10% more
                return __result * 0.9f;
            }
            return __result;
        }
    }

    /**
     * Patch Frail power to reduce 10% more block at Asc 56+
     */
    @SpirePatch(
        cls = "com.megacrit.cardcrawl.powers.FrailPower",
        method = "modifyBlock",
        paramtypez = {float.class}
    )
    public static class FrailEnhancePatch {
        public static float Postfix(float __result, AbstractPower __instance, float blockAmount) {
            if (AbstractDungeon.isAscensionMode && AbstractDungeon.ascensionLevel >= 56) {
                // Original frail is 25% reduction, make it 35% reduction
                // __result is already reduced by 25%, so multiply by 0.9 to reduce 10% more
                return __result * 0.9f;
            }
            return __result;
        }
    }
}
