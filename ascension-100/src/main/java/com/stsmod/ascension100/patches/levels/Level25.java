package com.stsmod.ascension100.patches.levels;

import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.potions.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Ascension Level 25: Potion slot increase, price decrease, and effects decrease
 *
 * 포션 슬롯이 1개 증가하고, 가격이 하락하고, 효과가 감소합니다.
 *
 * 포션 슬롯이 1개 증가합니다.
 * 상점에서 모든 포션의 가격이 40% 감소합니다.
 *
 * 강철의 정수(Essence of Steel): 판금 갑옷이 1 줄어듭니다. (4 → 3)
 * 청동 액체(Liquid Bronze): 가시 효과가 1 줄어듭니다. (3 → 2)
 * 정제된 혼돈(Distilled Chaos): 사용하는 카드가 1장 줄어듭니다. (3 → 2)
 * 재생 포션(Regen Potion): 재생 효과가 1 줄어듭니다. (5 → 4)
 * 수용의 포션(Potion of Capacity): 구체 슬롯이 1 줄어듭니다. (2 → 1)
 * 교활함 포션(Cunning Potion): 단도 개수가 1개 줄어듭니다. (3 → 2)
 * 신속 포션(Swift Potion): 뽑는 카드 개수가 1개 줄어듭니다. (3 → 2)
 * 속도 포션(Speed Potion): 얻는 민첩과 턴종료시 잃는 민첩이 1 줄어듭니다. (5 → 4)
 * 몸풀기용 포션(Steroid Potion): 얻는 힘과 턴종료시 잃는 힘이 1 줄어듭니다. (2 → 1)
 * 방어도 포션(Block Potion): 얻는 방어도가 3 줄어듭니다. (12 → 9)
 * 공포 포션(Fear Potion): 부여하는 취약이 1 줄어듭니다. (3 → 2)
 * 약화 포션(Weak Potion): 부여하는 약화가 1 줄어듭니다. (3 → 2)
 * 화염 포션(Fire Potion): 가하는 피해가 5 줄어듭니다. (20 → 15)
 * 폭발 포션(Explosive Potion): 가하는 피해가 2 줄어듭니다. (10 → 8)
 * 피 포션(Blood Potion): 회복하는 수치가 최대체력의 5%만큼 감소합니다. (20% → 15%)
 * 중독 포션(Poison Potion): 가하는 중독 수치가 1 줄어듭니다. (6 → 5)
 * 공격 포션(Attack Potion): 카드 선택지가 1개 줄어듭니다. (3 → 2)
 * 스킬 포션(Skill Potion): 카드 선택지가 1개 줄어듭니다. (3 → 2)
 * 파워 포션(Power Potion): 카드 선택지가 1개 줄어듭니다. (3 → 2)
 * 무색 포션(Colorless Potion): 카드 선택지가 1개 줄어듭니다. (3 → 2)
 * 무쇠의 심장(Heart of Iron): 금속화 수치가 2 줄어듭니다. (6 → 4)
 * 과일 주스(Fruit Juice): 증가하는 최대 체력이 2 줄어듭니다. (5 → 3)
 * 스네코 기름(Snecko Oil): 뽑는 카드의 개수가 1장 줄어듭니다. (5 → 4)
 * 병 속의 요정(Fairy in a Bottle): 부활 시 회복하는 최대 체력 수치가 10% 줄어듭니다. (30% → 20%)
 */
public class Level25 {
    private static final Logger logger = LogManager.getLogger(Level25.class.getName());

    /**
     * Potion slot restoration: Restore to base 3 slots
     *
     * Note: Ascension 11 reduces potion slots by 1 in the constructor (3 → 2).
     * Ascension 25 restores this penalty, bringing slots back to base value.
     *
     * IMPORTANT: We must also add a PotionSlot object because the constructor
     * creates PotionSlot objects based on potionSlots value BEFORE our patch runs.
     *
     * Final result: Base 3 - Asc11(1) + Asc25(1) = 3 slots (restored)
     */
    @SpirePatch(
        clz = AbstractPlayer.class,
        method = SpirePatch.CONSTRUCTOR
    )
    public static class PotionSlotIncrease {
        @SpirePostfixPatch
        public static void Postfix(AbstractPlayer __instance) {
            if (AbstractDungeon.isAscensionMode && AbstractDungeon.ascensionLevel >= 25) {
                // Add +1 to restore Ascension 11's -1 penalty
                int slotsToAdd = 1;
                int oldSlots = __instance.potionSlots;
                __instance.potionSlots += slotsToAdd;

                // Add PotionSlot object for the restored slot
                __instance.potions.add(new PotionSlot(oldSlots));

                logger.info(String.format(
                    "Ascension 25: Potion slots restored from %d to %d (Asc11 penalty removed)",
                    oldSlots, __instance.potionSlots
                ));
            }
        }
    }

    /**
     * Potion price reduction: -40%
     */
    @SpirePatch(
        clz = AbstractPotion.class,
        method = "getPrice"
    )
    public static class PotionPriceReduction {
        @SpirePostfixPatch
        public static int Postfix(int __result) {
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 25) {
                return __result;
            }

            // Reduce price by 40%
            int reducedPrice = (int) Math.ceil(__result * 0.6f);

            logger.debug(String.format(
                "Ascension 25: Potion price reduced from %d to %d (-40%%)",
                __result, reducedPrice
            ));

            return reducedPrice;
        }
    }

    // ========================================
    // Individual Potion Patches
    // ========================================

    /**
     * Essence of Steel: Plated Armor -1 (4 → 3)
     */
    @SpirePatch(
        clz = EssenceOfSteel.class,
        method = "getPotency",
        paramtypez = { int.class }
    )
    public static class EssenceOfSteelPatch {
        @SpirePostfixPatch
        public static int Postfix(int __result) {
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 25) {
                return __result;
            }
            return Math.max(0, __result - 1); // 4 → 3
        }
    }

    /**
     * Liquid Bronze: Thorns -1 (3 → 2)
     */
    @SpirePatch(
        clz = LiquidBronze.class,
        method = "getPotency",
        paramtypez = { int.class }
    )
    public static class LiquidBronzePatch {
        @SpirePostfixPatch
        public static int Postfix(int __result) {
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 25) {
                return __result;
            }
            return Math.max(0, __result - 1); // 3 → 2
        }
    }

    /**
     * Distilled Chaos: Cards -1 (3 → 2)
     */
    @SpirePatch(
        clz = DistilledChaosPotion.class,
        method = "getPotency",
        paramtypez = { int.class }
    )
    public static class DistilledChaosPatch {
        @SpirePostfixPatch
        public static int Postfix(int __result) {
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 25) {
                return __result;
            }
            return Math.max(0, __result - 1); // 3 → 2
        }
    }

    /**
     * Regen Potion: Regeneration -1 (5 → 4)
     */
    @SpirePatch(
        clz = RegenPotion.class,
        method = "getPotency",
        paramtypez = { int.class }
    )
    public static class RegenPotionPatch {
        @SpirePostfixPatch
        public static int Postfix(int __result) {
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 25) {
                return __result;
            }
            return Math.max(0, __result - 1); // 5 → 4
        }
    }

    /**
     * Potion of Capacity: Orb Slots -1 (2 → 1)
     */
    @SpirePatch(
        clz = PotionOfCapacity.class,
        method = "getPotency",
        paramtypez = { int.class }
    )
    public static class PotionOfCapacityPatch {
        @SpirePostfixPatch
        public static int Postfix(int __result) {
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 25) {
                return __result;
            }
            return Math.max(1, __result - 1); // 2 → 1 (minimum 1)
        }
    }

    /**
     * Cunning Potion: Shivs -1 (3 → 2)
     */
    @SpirePatch(
        clz = CunningPotion.class,
        method = "getPotency",
        paramtypez = { int.class }
    )
    public static class CunningPotionPatch {
        @SpirePostfixPatch
        public static int Postfix(int __result) {
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 25) {
                return __result;
            }
            return Math.max(0, __result - 1); // 3 → 2
        }
    }

    /**
     * Swift Potion: Draw Cards -1 (3 → 2)
     */
    @SpirePatch(
        clz = SwiftPotion.class,
        method = "getPotency",
        paramtypez = { int.class }
    )
    public static class SwiftPotionPatch {
        @SpirePostfixPatch
        public static int Postfix(int __result) {
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 25) {
                return __result;
            }
            return Math.max(0, __result - 1); // 3 → 2
        }
    }

    /**
     * Speed Potion: Dexterity -1 (5 → 4)
     */
    @SpirePatch(
        clz = SpeedPotion.class,
        method = "getPotency",
        paramtypez = { int.class }
    )
    public static class SpeedPotionPatch {
        @SpirePostfixPatch
        public static int Postfix(int __result) {
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 25) {
                return __result;
            }
            return Math.max(0, __result - 1); // 5 → 4
        }
    }

    /**
     * Steroid Potion: Strength -1 (2 → 1)
     */
    @SpirePatch(
        clz = SteroidPotion.class,
        method = "getPotency",
        paramtypez = { int.class }
    )
    public static class SteroidPotionPatch {
        @SpirePostfixPatch
        public static int Postfix(int __result) {
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 25) {
                return __result;
            }
            return Math.max(0, __result - 1); // 2 → 1
        }
    }

    /**
     * Block Potion: Block -3 (12 → 9)
     */
    @SpirePatch(
        clz = BlockPotion.class,
        method = "getPotency",
        paramtypez = { int.class }
    )
    public static class BlockPotionPatch {
        @SpirePostfixPatch
        public static int Postfix(int __result) {
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 25) {
                return __result;
            }
            return Math.max(0, __result - 3); // 12 → 9
        }
    }

    /**
     * Fear Potion: Vulnerable -1 (3 → 2)
     */
    @SpirePatch(
        clz = FearPotion.class,
        method = "getPotency",
        paramtypez = { int.class }
    )
    public static class FearPotionPatch {
        @SpirePostfixPatch
        public static int Postfix(int __result) {
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 25) {
                return __result;
            }
            return Math.max(0, __result - 1); // 3 → 2
        }
    }

    /**
     * Weak Potion: Weak -1 (3 → 2)
     */
    @SpirePatch(
        clz = WeakenPotion.class,
        method = "getPotency",
        paramtypez = { int.class }
    )
    public static class WeakPotionPatch {
        @SpirePostfixPatch
        public static int Postfix(int __result) {
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 25) {
                return __result;
            }
            return Math.max(0, __result - 1); // 3 → 2
        }
    }

    /**
     * Fire Potion: Damage -5 (20 → 15)
     */
    @SpirePatch(
        clz = FirePotion.class,
        method = "getPotency",
        paramtypez = { int.class }
    )
    public static class FirePotionPatch {
        @SpirePostfixPatch
        public static int Postfix(int __result) {
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 25) {
                return __result;
            }
            return Math.max(0, __result - 5); // 20 → 15
        }
    }

    /**
     * Explosive Potion: Damage -2 (10 → 8)
     */
    @SpirePatch(
        clz = ExplosivePotion.class,
        method = "getPotency",
        paramtypez = { int.class }
    )
    public static class ExplosivePotionPatch {
        @SpirePostfixPatch
        public static int Postfix(int __result) {
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 25) {
                return __result;
            }
            return Math.max(0, __result - 2); // 10 → 8
        }
    }

    /**
     * Blood Potion: Heal -5% of max HP (20% → 15%)
     */
    @SpirePatch(
        clz = BloodPotion.class,
        method = "getPotency",
        paramtypez = { int.class }
    )
    public static class BloodPotionPatch {
        @SpirePostfixPatch
        public static int Postfix(int __result) {
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 25) {
                return __result;
            }
            return Math.max(0, __result - 5); // 20% → 15%
        }
    }

    /**
     * Poison Potion: Poison -1 (6 → 5)
     */
    @SpirePatch(
        clz = PoisonPotion.class,
        method = "getPotency",
        paramtypez = { int.class }
    )
    public static class PoisonPotionPatch {
        @SpirePostfixPatch
        public static int Postfix(int __result) {
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 25) {
                return __result;
            }
            return Math.max(0, __result - 1); // 6 → 5
        }
    }

    // ========================================
    // Card Selection Potions (handled by separate patch files)
    // ========================================
    // Attack Potion: Card choices -1 (3 → 2) - see AttackPotionPatch.java
    // Skill Potion: Card choices -1 (3 → 2) - see SkillPotionPatch.java
    // Power Potion: Card choices -1 (3 → 2) - see PowerPotionPatch.java
    // Colorless Potion: Card choices -1 (3 → 2) - see ColorlessPotionPatch.java

    // ========================================
    // Rare Potions
    // ========================================

    /**
     * Heart of Iron: Metallicize -2 (6 → 4)
     */
    @SpirePatch(
        clz = HeartOfIron.class,
        method = "getPotency",
        paramtypez = { int.class }
    )
    public static class HeartOfIronPatch {
        @SpirePostfixPatch
        public static int Postfix(int __result) {
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 25) {
                return __result;
            }
            return Math.max(0, __result - 2); // 6 → 4
        }
    }

    /**
     * Fruit Juice: Max HP increase -2 (5 → 3)
     */
    @SpirePatch(
        clz = FruitJuice.class,
        method = "getPotency",
        paramtypez = { int.class }
    )
    public static class FruitJuicePatch {
        @SpirePostfixPatch
        public static int Postfix(int __result) {
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 25) {
                return __result;
            }
            return Math.max(0, __result - 2); // 5 → 3
        }
    }

    /**
     * Snecko Oil: Draw cards -1 (5 → 4)
     */
    @SpirePatch(
        clz = SneckoOil.class,
        method = "getPotency",
        paramtypez = { int.class }
    )
    public static class SneckoOilPatch {
        @SpirePostfixPatch
        public static int Postfix(int __result) {
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 25) {
                return __result;
            }
            return Math.max(0, __result - 1); // 5 → 4
        }
    }

    /**
     * Fairy Potion: Revival heal -10% of max HP (30% → 20%)
     */
    @SpirePatch(
        clz = FairyPotion.class,
        method = "getPotency",
        paramtypez = { int.class }
    )
    public static class FairyPotionPatch {
        @SpirePostfixPatch
        public static int Postfix(int __result) {
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 25) {
                return __result;
            }
            return Math.max(0, __result - 10); // 30% → 20%
        }
    }
}
