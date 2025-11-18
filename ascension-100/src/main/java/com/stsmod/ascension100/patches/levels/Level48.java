package com.stsmod.ascension100.patches.levels;

import com.evacipated.cardcrawl.modthespire.lib.*;
import com.megacrit.cardcrawl.cards.DamageInfo;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.neow.NeowReward;
import javassist.CtBehavior;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Ascension Level 48: Neow's rewards weakened
 *
 * 니오우의 선물이 약해집니다.
 *
 * 니오우의 선물 보상 일부 변경:
 * - TEN_PERCENT_HP_BONUS: 최대 HP +10% -> 최대 HP +8%
 * - HUNDRED_GOLD: 골드 100 -> 골드 80
 * - TEN_PERCENT_HP_LOSS: 최대 HP -10% -> 최대 HP -15%
 * - PERCENT_DAMAGE: 현재 HP의 30% 피해 -> 현재 HP의 40% 피해
 */
public class Level48 {
    private static final Logger logger = LogManager.getLogger(Level48.class.getName());

    /**
     * Modify hp_bonus calculation: 10% -> 8%
     */
    @SpirePatch(
        clz = NeowReward.class,
        method = SpirePatch.CONSTRUCTOR,
        paramtypez = {boolean.class}
    )
    public static class ModifyConstructor1 {
        @SpireInsertPatch(
            locator = HpBonusLocator.class
        )
        public static void Insert(NeowReward __instance) {
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 48) {
                return;
            }

            try {
                java.lang.reflect.Field hpBonusField = NeowReward.class.getDeclaredField("hp_bonus");
                hpBonusField.setAccessible(true);
                // Original: maxHealth * 0.1F
                // New: maxHealth * 0.08F
                hpBonusField.setInt(__instance, (int)(AbstractDungeon.player.maxHealth * 0.08F));
                logger.info("Ascension 48: Modified Neow hp_bonus to 8% (was 10%)");
            } catch (Exception e) {
                logger.error("Failed to modify Neow hp_bonus", e);
            }
        }

        private static class HpBonusLocator extends SpireInsertLocator {
            @Override
            public int[] Locate(CtBehavior ctBehavior) throws Exception {
                Matcher finalMatcher = new Matcher.FieldAccessMatcher(NeowReward.class, "hp_bonus");
                return LineFinder.findInOrder(ctBehavior, finalMatcher);
            }
        }
    }

    @SpirePatch(
        clz = NeowReward.class,
        method = SpirePatch.CONSTRUCTOR,
        paramtypez = {int.class}
    )
    public static class ModifyConstructor2 {
        @SpireInsertPatch(
            locator = HpBonusLocator.class
        )
        public static void Insert(NeowReward __instance) {
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 48) {
                return;
            }

            try {
                java.lang.reflect.Field hpBonusField = NeowReward.class.getDeclaredField("hp_bonus");
                hpBonusField.setAccessible(true);
                hpBonusField.setInt(__instance, (int)(AbstractDungeon.player.maxHealth * 0.08F));
            } catch (Exception e) {
                logger.error("Failed to modify Neow hp_bonus", e);
            }
        }

        private static class HpBonusLocator extends SpireInsertLocator {
            @Override
            public int[] Locate(CtBehavior ctBehavior) throws Exception {
                Matcher finalMatcher = new Matcher.FieldAccessMatcher(NeowReward.class, "hp_bonus");
                return LineFinder.findInOrder(ctBehavior, finalMatcher);
            }
        }
    }

    /**
     * Modify activate() method to change actual values
     */
    @SpirePatch(
        clz = NeowReward.class,
        method = "activate"
    )
    public static class ModifyActivate {
        @SpirePrefixPatch
        public static void Prefix(NeowReward __instance) {
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 48) {
                return;
            }

            try {
                java.lang.reflect.Field typeField = NeowReward.class.getDeclaredField("type");
                java.lang.reflect.Field drawbackField = NeowReward.class.getDeclaredField("drawback");
                java.lang.reflect.Field hpBonusField = NeowReward.class.getDeclaredField("hp_bonus");

                typeField.setAccessible(true);
                drawbackField.setAccessible(true);
                hpBonusField.setAccessible(true);

                Object type = typeField.get(__instance);
                Object drawback = drawbackField.get(__instance);
                int hpBonus = hpBonusField.getInt(__instance);

                // HUNDRED_GOLD: 100 -> 80
                if (type.toString().equals("HUNDRED_GOLD")) {
                    // Will be handled by bytecode instrumentation
                    logger.info("Ascension 48: HUNDRED_GOLD will give 80 gold (was 100)");
                }

                // TWO_FIFTY_GOLD: Keep 250
                if (type.toString().equals("TWO_FIFTY_GOLD")) {
                    logger.info("Ascension 48: TWO_FIFTY_GOLD remains 250 gold");
                }

                // TEN_PERCENT_HP_LOSS: -10% -> -15%
                if (drawback.toString().equals("TEN_PERCENT_HP_LOSS")) {
                    // Will decrease by hp_bonus * 1.5 in postfix
                    logger.info("Ascension 48: TEN_PERCENT_HP_LOSS will decrease max HP by 15% (was 10%)");
                }

                // PERCENT_DAMAGE: 30% -> 40%
                if (drawback.toString().equals("PERCENT_DAMAGE")) {
                    logger.info("Ascension 48: PERCENT_DAMAGE will deal 40% damage (was 30%)");
                }

            } catch (Exception e) {
                logger.error("Failed to check Neow reward type", e);
            }
        }

        @SpirePostfixPatch
        public static void Postfix(NeowReward __instance) {
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 48) {
                return;
            }

            // Drawbacks are already applied in activate()
            // We need to override some values that were applied
            try {
                java.lang.reflect.Field drawbackField = NeowReward.class.getDeclaredField("drawback");
                java.lang.reflect.Field hpBonusField = NeowReward.class.getDeclaredField("hp_bonus");

                drawbackField.setAccessible(true);
                hpBonusField.setAccessible(true);

                Object drawback = drawbackField.get(__instance);
                int hpBonus = hpBonusField.getInt(__instance);

                // TEN_PERCENT_HP_LOSS: Already decreased by hp_bonus, need to decrease more
                if (drawback.toString().equals("TEN_PERCENT_HP_LOSS")) {
                    // Original decreased by hp_bonus (8%)
                    // Need to decrease by 15% total, so decrease by 7% more
                    int additionalDecrease = (int)(AbstractDungeon.player.maxHealth * 0.07F);
                    AbstractDungeon.player.decreaseMaxHealth(additionalDecrease);
                    logger.info(String.format(
                        "Ascension 48: Applied additional -%d max HP for total -15%% (was -10%%)",
                        additionalDecrease
                    ));
                }

                // PERCENT_DAMAGE: Already applied /10*3, need to apply /10*1 more for total /10*4
                if (drawback.toString().equals("PERCENT_DAMAGE")) {
                    int additionalDamage = AbstractDungeon.player.currentHealth / 10;
                    AbstractDungeon.player.damage(new DamageInfo(null, additionalDamage, DamageInfo.DamageType.HP_LOSS));
                    logger.info(String.format(
                        "Ascension 48: Applied additional %d damage for total 40%% (was 30%%)",
                        additionalDamage
                    ));
                }

            } catch (Exception e) {
                logger.error("Failed to apply additional Neow penalties", e);
            }
        }
    }

    /**
     * Modify gold amount given: 100 -> 80
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
                    if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 48) {
                        return;
                    }

                    if (m.getMethodName().equals("gainGold")) {
                        // Change gainGold(100) to gainGold(80)
                        m.replace("{ " +
                            "if ($1 == 100) { " +
                            "  $_ = $proceed(80); " +
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
     * Modify option label text to show correct numbers
     */
    @SpirePatch(
        clz = NeowReward.class,
        method = "getRewardDrawbackOptions",
        paramtypez = {}
    )
    public static class ModifyDrawbackText {
        @SpirePostfixPatch
        public static void Postfix(NeowReward __instance, @ByRef Object[] __result) {
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 48) {
                return;
            }

            try {
                java.lang.reflect.Field hpBonusField = NeowReward.class.getDeclaredField("hp_bonus");
                hpBonusField.setAccessible(true);
                int hpBonus = hpBonusField.getInt(__instance);

                java.util.ArrayList<?> drawbackOptions = (java.util.ArrayList<?>) __result[0];

                for (Object def : drawbackOptions) {
                    java.lang.reflect.Field typeField = def.getClass().getDeclaredField("type");
                    java.lang.reflect.Field descField = def.getClass().getDeclaredField("desc");
                    typeField.setAccessible(true);
                    descField.setAccessible(true);

                    Object type = typeField.get(def);

                    if (type.toString().equals("TEN_PERCENT_HP_LOSS")) {
                        // Show -15% instead of -10%
                        int displayAmount = (int)(AbstractDungeon.player.maxHealth * 0.15F);
                        String[] TEXT = getNeowRewardText();
                        descField.set(def, TEXT[17] + displayAmount + TEXT[18]);
                        logger.info("Ascension 48: Modified TEN_PERCENT_HP_LOSS text to show 15%");
                    } else if (type.toString().equals("PERCENT_DAMAGE")) {
                        // Show 40% instead of 30%
                        int displayAmount = AbstractDungeon.player.currentHealth / 10 * 4;
                        String[] TEXT = getNeowRewardText();
                        descField.set(def, TEXT[21] + displayAmount + TEXT[29] + " ");
                        logger.info("Ascension 48: Modified PERCENT_DAMAGE text to show 40%");
                    }
                }

            } catch (Exception e) {
                logger.error("Failed to modify Neow drawback text", e);
            }
        }
    }

    /**
     * Modify reward option text to show correct numbers
     */
    @SpirePatch(
        clz = NeowReward.class,
        method = "getRewardOptions",
        paramtypez = {int.class}
    )
    public static class ModifyRewardText {
        @SpirePostfixPatch
        public static void Postfix(NeowReward __instance, int category, @ByRef Object[] __result) {
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 48) {
                return;
            }

            try {
                java.lang.reflect.Field hpBonusField = NeowReward.class.getDeclaredField("hp_bonus");
                hpBonusField.setAccessible(true);
                int hpBonus = hpBonusField.getInt(__instance);

                java.util.ArrayList<?> rewardOptions = (java.util.ArrayList<?>) __result[0];

                for (Object def : rewardOptions) {
                    java.lang.reflect.Field typeField = def.getClass().getDeclaredField("type");
                    java.lang.reflect.Field descField = def.getClass().getDeclaredField("desc");
                    typeField.setAccessible(true);
                    descField.setAccessible(true);

                    Object type = typeField.get(def);
                    String[] TEXT = getNeowRewardText();

                    if (type.toString().equals("TEN_PERCENT_HP_BONUS")) {
                        // Show +8 instead of original text
                        descField.set(def, TEXT[7] + hpBonus + " ]");
                        logger.info("Ascension 48: Modified TEN_PERCENT_HP_BONUS text to show 8%");
                    } else if (type.toString().equals("TWENTY_PERCENT_HP_BONUS")) {
                        // Show +16 (hp_bonus * 2 where hp_bonus is now 8%)
                        descField.set(def, TEXT[16] + (hpBonus * 2) + " ]");
                        logger.info("Ascension 48: Modified TWENTY_PERCENT_HP_BONUS text to show 16%");
                    } else if (type.toString().equals("HUNDRED_GOLD")) {
                        // Show 80 instead of 100
                        descField.set(def, TEXT[8] + 80 + TEXT[9]);
                        logger.info("Ascension 48: Modified HUNDRED_GOLD text to show 80");
                    }
                }

            } catch (Exception e) {
                logger.error("Failed to modify Neow reward text", e);
            }
        }
    }

    /**
     * Helper method to get Neow Reward text strings
     */
    private static String[] getNeowRewardText() {
        try {
            return CardCrawlGame.languagePack.getCharacterString("Neow Reward").TEXT;
        } catch (Exception e) {
            logger.error("Failed to get Neow Reward text", e);
            return new String[0];
        }
    }
}
