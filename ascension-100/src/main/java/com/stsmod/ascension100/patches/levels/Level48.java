package com.stsmod.ascension100.patches.levels;

import com.evacipated.cardcrawl.modthespire.lib.*;
import com.megacrit.cardcrawl.cards.DamageInfo;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.neow.NeowReward;
import javassist.expr.ExprEditor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Ascension Level 48: Neow's rewards weakened
 *
 * 니오우의 선물이 약해집니다.
 *
 * 니오우의 선물 보상 일부 변경:
 * - TEN_PERCENT_HP_BONUS: 최대 HP +10% -> 최대 HP +8%
 * - TWENTY_PERCENT_HP_BONUS: 최대 HP +20% -> 최대 HP +16%
 * - HUNDRED_GOLD: 골드 100 -> 골드 80
 * - TEN_PERCENT_HP_LOSS: 최대 HP -10% -> 최대 HP -15%
 * - PERCENT_DAMAGE: 현재 HP의 30% 피해 -> 현재 HP의 40% 피해
 */
public class Level48 {
    private static final Logger logger = LogManager.getLogger(Level48.class.getName());

    /**
     * Patch constructor NeowReward(boolean firstMini)
     * - Recalculate hp_bonus: 10% -> 8%
     * - Update optionLabel text to reflect new values
     */
    @SpirePatch(
        clz = NeowReward.class,
        method = SpirePatch.CONSTRUCTOR,
        paramtypez = {boolean.class}
    )
    public static class ModifyConstructor1 {
        @SpirePostfixPatch
        public static void Postfix(NeowReward __instance) {
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 48) {
                return;
            }

            try {
                // Recalculate hp_bonus: 10% -> 8%
                java.lang.reflect.Field hpBonusField = NeowReward.class.getDeclaredField("hp_bonus");
                hpBonusField.setAccessible(true);
                int newHpBonus = (int)(AbstractDungeon.player.maxHealth * 0.08F);
                hpBonusField.setInt(__instance, newHpBonus);

                // Update optionLabel text
                java.lang.reflect.Field optionLabelField = NeowReward.class.getDeclaredField("optionLabel");
                optionLabelField.setAccessible(true);
                String optionLabel = (String) optionLabelField.get(__instance);

                // Replace hp_bonus value in text (10% value -> 8% value)
                int oldHpBonus = (int)(AbstractDungeon.player.maxHealth * 0.1F);
                optionLabel = optionLabel.replace(" " + oldHpBonus + " ]", " " + newHpBonus + " ]");

                optionLabelField.set(__instance, optionLabel);

                logger.info(String.format(
                    "Ascension 48: Modified Constructor1 - hp_bonus: %d -> %d, optionLabel updated",
                    oldHpBonus, newHpBonus
                ));
            } catch (Exception e) {
                logger.error("Failed to modify NeowReward constructor (boolean)", e);
            }
        }
    }

    /**
     * Patch constructor NeowReward(int category)
     * - Recalculate hp_bonus: 10% -> 8%
     * - Update optionLabel text to reflect new values
     */
    @SpirePatch(
        clz = NeowReward.class,
        method = SpirePatch.CONSTRUCTOR,
        paramtypez = {int.class}
    )
    public static class ModifyConstructor2 {
        @SpirePostfixPatch
        public static void Postfix(NeowReward __instance, int category) {
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 48) {
                return;
            }

            try {
                // Recalculate hp_bonus: 10% -> 8%
                java.lang.reflect.Field hpBonusField = NeowReward.class.getDeclaredField("hp_bonus");
                hpBonusField.setAccessible(true);
                int newHpBonus = (int)(AbstractDungeon.player.maxHealth * 0.08F);
                int oldHpBonus = (int)(AbstractDungeon.player.maxHealth * 0.1F);
                hpBonusField.setInt(__instance, newHpBonus);

                // Update optionLabel text
                java.lang.reflect.Field optionLabelField = NeowReward.class.getDeclaredField("optionLabel");
                optionLabelField.setAccessible(true);
                String optionLabel = (String) optionLabelField.get(__instance);

                // For category 2 (drawbacks), optionLabel contains both drawback and reward
                // We need to update both parts

                // 1. Update hp_bonus values (10% -> 8%, 20% -> 16%)
                // TEN_PERCENT_HP_BONUS: " {oldHpBonus} ]" -> " {newHpBonus} ]"
                optionLabel = optionLabel.replace(" " + oldHpBonus + " ]", " " + newHpBonus + " ]");

                // TWENTY_PERCENT_HP_BONUS: " {oldHpBonus*2} ]" -> " {newHpBonus*2} ]"
                optionLabel = optionLabel.replace(" " + (oldHpBonus * 2) + " ]", " " + (newHpBonus * 2) + " ]");

                // 2. Update TEN_PERCENT_HP_LOSS drawback: -10% -> -15%
                // Original: "Lose {oldHpBonus} Max HP"
                // New: "Lose {15% of max HP} Max HP"
                int hp15Percent = (int)(AbstractDungeon.player.maxHealth * 0.15F);
                // Replace the old hp_bonus value in drawback text with 15% value
                // Pattern: "[Lose Max HP: {value}]" or similar - need to find the exact pattern
                String oldDrawbackText = String.valueOf(oldHpBonus);
                String newDrawbackText = String.valueOf(hp15Percent);

                // Only replace if this is a drawback option (category 2)
                if (category == 2) {
                    // Check if this reward has TEN_PERCENT_HP_LOSS drawback
                    java.lang.reflect.Field drawbackField = NeowReward.class.getDeclaredField("drawback");
                    drawbackField.setAccessible(true);
                    Object drawback = drawbackField.get(__instance);

                    if (drawback.toString().equals("TEN_PERCENT_HP_LOSS")) {
                        // Replace first occurrence (drawback part) with 15% value
                        optionLabel = optionLabel.replaceFirst(oldDrawbackText, newDrawbackText);
                        logger.info(String.format(
                            "Ascension 48: Updated TEN_PERCENT_HP_LOSS drawback text: %d -> %d",
                            oldHpBonus, hp15Percent
                        ));
                    }

                    // 3. Update PERCENT_DAMAGE drawback: 30% -> 40%
                    if (drawback.toString().equals("PERCENT_DAMAGE")) {
                        int old30Percent = AbstractDungeon.player.currentHealth / 10 * 3;
                        int new40Percent = AbstractDungeon.player.currentHealth / 10 * 4;
                        optionLabel = optionLabel.replace(String.valueOf(old30Percent), String.valueOf(new40Percent));
                        logger.info(String.format(
                            "Ascension 48: Updated PERCENT_DAMAGE drawback text: %d -> %d",
                            old30Percent, new40Percent
                        ));
                    }
                }

                // 4. Update HUNDRED_GOLD: 100 -> 80
                // Original text uses special character 'd' for 100
                // Pattern: "[Obtain {100} Gold]" - represented as TEXT[8] + 'd' + TEXT[9]
                // We need to replace 'd' with '80'
                optionLabel = optionLabel.replace("d Gold", "80 Gold");
                optionLabel = optionLabel.replace("골드 d", "골드 80");  // Korean
                optionLabel = optionLabel.replace("d 金币", "80 金币");  // Chinese

                optionLabelField.set(__instance, optionLabel);

                logger.info(String.format(
                    "Ascension 48: Modified Constructor2(category=%d) - hp_bonus: %d -> %d, optionLabel updated",
                    category, oldHpBonus, newHpBonus
                ));
            } catch (Exception e) {
                logger.error("Failed to modify NeowReward constructor (int)", e);
            }
        }
    }

    /**
     * Patch activate() method to modify actual gold given: 100 -> 80
     */
    @SpirePatch(
        clz = NeowReward.class,
        method = "activate"
    )
    public static class ModifyGoldReward {
        @SpireInstrumentPatch
        public static ExprEditor Instrument() {
            return new ExprEditor() {
                @Override
                public void edit(javassist.expr.MethodCall m) throws javassist.CannotCompileException {
                    if (m.getMethodName().equals("gainGold")) {
                        // Change gainGold(100) to gainGold(80)
                        m.replace("{ " +
                            "if (!com.megacrit.cardcrawl.dungeons.AbstractDungeon.isAscensionMode || " +
                            "    com.megacrit.cardcrawl.dungeons.AbstractDungeon.ascensionLevel < 48) { " +
                            "  $_ = $proceed($$); " +
                            "} else if ($1 == 100) { " +
                            "  $_ = $proceed(80); " +
                            "  org.apache.logging.log4j.LogManager.getLogger(\"Ascension100Mod\").info(\"Ascension 48: Gave 80 gold instead of 100\"); " +
                            "} else { " +
                            "  $_ = $proceed($$); " +
                            "} " +
                            "}");
                    }
                }
            };
        }
    }

    /**
     * Patch activate() method to apply additional penalties for drawbacks
     */
    @SpirePatch(
        clz = NeowReward.class,
        method = "activate"
    )
    public static class ModifyDrawbackPenalties {
        @SpirePostfixPatch
        public static void Postfix(NeowReward __instance) {
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 48) {
                return;
            }

            try {
                java.lang.reflect.Field drawbackField = NeowReward.class.getDeclaredField("drawback");
                drawbackField.setAccessible(true);
                Object drawback = drawbackField.get(__instance);

                // TEN_PERCENT_HP_LOSS: Already decreased by hp_bonus (8%)
                // Need to decrease by 15% total, so apply additional 7%
                if (drawback.toString().equals("TEN_PERCENT_HP_LOSS")) {
                    int additionalDecrease = (int)(AbstractDungeon.player.maxHealth * 0.07F);
                    AbstractDungeon.player.decreaseMaxHealth(additionalDecrease);
                    logger.info(String.format(
                        "Ascension 48: Applied additional -%d max HP for TEN_PERCENT_HP_LOSS (total -15%%)",
                        additionalDecrease
                    ));
                }

                // PERCENT_DAMAGE: Already applied 30% damage (currentHealth / 10 * 3)
                // Need to apply 40% total, so apply additional 10%
                if (drawback.toString().equals("PERCENT_DAMAGE")) {
                    int additionalDamage = AbstractDungeon.player.currentHealth / 10;
                    AbstractDungeon.player.damage(new DamageInfo(null, additionalDamage, DamageInfo.DamageType.HP_LOSS));
                    logger.info(String.format(
                        "Ascension 48: Applied additional %d HP damage for PERCENT_DAMAGE (total 40%%)",
                        additionalDamage
                    ));
                }

            } catch (Exception e) {
                logger.error("Failed to apply additional Neow penalties", e);
            }
        }
    }
}
