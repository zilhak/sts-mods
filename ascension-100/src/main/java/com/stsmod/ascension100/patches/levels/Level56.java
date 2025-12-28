package com.stsmod.ascension100.patches.levels;

import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePrefixPatch;
import com.evacipated.cardcrawl.modthespire.lib.SpireReturn;
import com.megacrit.cardcrawl.cards.DamageInfo;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.powers.FrailPower;
import com.megacrit.cardcrawl.powers.VulnerablePower;
import com.megacrit.cardcrawl.powers.WeakPower;

/**
 * Ascension Level 56: Debuff effects enhanced
 * 디버프의 효과가 더욱 강화됩니다.
 *
 * - 취약: 50% → 60% (10% 추가)
 * * Odd Mushroom, Paper Frog 유물 효과는 원본 수치 유지
 * - 약화: 25% → 35% (10% 추가)
 * * Paper Crane 유물 효과는 원본 수치 유지
 * - 손상: 25% → 35% (10% 추가)
 */
public class Level56 {
    /**
     * Patch VulnerablePower: 50% → 60% damage increase
     */
    @SpirePatch(clz = VulnerablePower.class, method = "atDamageReceive")
    public static class EnhancedVulnerable {
        @SpirePrefixPatch
        public static SpireReturn<Float> Prefix(VulnerablePower __instance, float damage, DamageInfo.DamageType type) {
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 56) {
                return SpireReturn.Continue();
            }

            if (type == DamageInfo.DamageType.NORMAL) {
                // Check for relics (same logic as original)
                if (__instance.owner.isPlayer && AbstractDungeon.player.hasRelic("Odd Mushroom")) {
                    return SpireReturn.Return(damage * 1.25F);
                }
                if (__instance.owner != null && !__instance.owner.isPlayer
                        && AbstractDungeon.player.hasRelic("Paper Frog")) {
                    return SpireReturn.Return(damage * 1.75F);
                }

                // Enhanced: 1.5F → 1.6F (50% → 60%)
                return SpireReturn.Return(damage * 1.6F);
            }

            return SpireReturn.Return(damage);
        }
    }

    /**
     * Patch WeakPower: 25% → 35% damage reduction
     */
    @SpirePatch(clz = WeakPower.class, method = "atDamageGive")
    public static class EnhancedWeak {
        @SpirePrefixPatch
        public static SpireReturn<Float> Prefix(WeakPower __instance, float damage, DamageInfo.DamageType type) {
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 56) {
                return SpireReturn.Continue();
            }

            if (type == DamageInfo.DamageType.NORMAL) {
                // Check for Paper Crane relic (same logic as original)
                if (__instance.owner != null && !__instance.owner.isPlayer &&
                        AbstractDungeon.player.hasRelic("Paper Crane")) {
                    return SpireReturn.Return(damage * 0.6F);
                }

                // Enhanced: 0.75F → 0.65F (25% → 35% reduction)
                return SpireReturn.Return(damage * 0.65F);
            }

            return SpireReturn.Return(damage);
        }
    }

    /**
     * Patch FrailPower: 25% → 35% block reduction
     */
    @SpirePatch(clz = FrailPower.class, method = "modifyBlock")
    public static class EnhancedFrail {
        @SpirePrefixPatch
        public static SpireReturn<Float> Prefix(FrailPower __instance, float blockAmount) {
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 56) {
                return SpireReturn.Continue();
            }

            // Enhanced: 0.75F → 0.65F (25% → 35% reduction)
            return SpireReturn.Return(blockAmount * 0.65F);
        }
    }
}
