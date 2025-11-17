package com.stsmod.ascension100.patches.levels;

import com.evacipated.cardcrawl.modthespire.lib.*;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import javassist.CtBehavior;
import com.megacrit.cardcrawl.monsters.exordium.SlimeBoss;
import com.megacrit.cardcrawl.monsters.exordium.TheGuardian;
import com.megacrit.cardcrawl.monsters.exordium.Hexaghost;
import com.megacrit.cardcrawl.monsters.city.Champ;
import com.megacrit.cardcrawl.monsters.city.TheCollector;
import com.megacrit.cardcrawl.monsters.beyond.AwakenedOne;
import com.megacrit.cardcrawl.monsters.beyond.TimeEater;
import com.megacrit.cardcrawl.monsters.beyond.Donu;
import com.megacrit.cardcrawl.monsters.ending.CorruptHeart;
import com.megacrit.cardcrawl.actions.common.ApplyPowerAction;
import com.megacrit.cardcrawl.actions.common.MakeTempCardInDiscardAction;
import com.megacrit.cardcrawl.cards.status.Slimed;
import com.megacrit.cardcrawl.cards.status.Dazed;
import com.megacrit.cardcrawl.powers.CuriosityPower;
import com.megacrit.cardcrawl.powers.WeakPower;
import com.megacrit.cardcrawl.powers.VulnerablePower;
import com.megacrit.cardcrawl.powers.FrailPower;
import com.megacrit.cardcrawl.powers.StrengthPower;
import com.megacrit.cardcrawl.powers.InvinciblePower;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Ascension Level 27: Boss behavior patterns enhanced
 * 보스의 행동 패턴이 강화됩니다.
 */
public class Level27 {

    private static final Logger logger = LogManager.getLogger(Level27.class.getName());

    /**
     * Hexaghost: Inflame pattern grants +1 additional Strength
     * Base game: Inflame pattern (move 3) grants Strength based on strAmount field
     * A27+: Additional Strength +1 on Inflame pattern (merged into single effect)
     */
    @SpirePatch(
        clz = Hexaghost.class,
        method = "takeTurn"
    )
    public static class HexaghostInflamePatch {
        private static final ThreadLocal<Byte> lastMove = new ThreadLocal<>();

        @SpirePrefixPatch
        public static void Prefix(Hexaghost __instance) {
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 27) {
                return;
            }

            try {
                java.lang.reflect.Field nextMoveField = com.megacrit.cardcrawl.monsters.AbstractMonster.class.getDeclaredField("nextMove");
                nextMoveField.setAccessible(true);
                byte move = nextMoveField.getByte(__instance);
                lastMove.set(move);
            } catch (Exception e) {
                logger.error("Failed to get Hexaghost move", e);
            }
        }

        @SpirePostfixPatch
        public static void Postfix(Hexaghost __instance) {
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 27) {
                return;
            }

            Byte move = lastMove.get();
            if (move != null && move == 3) { // INFLAME move
                // Find and modify the last ApplyPowerAction with StrengthPower
                try {
                    for (int i = AbstractDungeon.actionManager.actions.size() - 1; i >= 0; i--) {
                        com.megacrit.cardcrawl.actions.AbstractGameAction action = AbstractDungeon.actionManager.actions.get(i);

                        if (action instanceof ApplyPowerAction) {
                            java.lang.reflect.Field powerToApplyField = ApplyPowerAction.class.getDeclaredField("powerToApply");
                            powerToApplyField.setAccessible(true);
                            com.megacrit.cardcrawl.powers.AbstractPower power = (com.megacrit.cardcrawl.powers.AbstractPower) powerToApplyField.get(action);

                            if (power instanceof StrengthPower && power.owner == __instance) {
                                power.amount += 1;

                                java.lang.reflect.Field amountField = ApplyPowerAction.class.getDeclaredField("amount");
                                amountField.setAccessible(true);
                                int currentAmount = amountField.getInt(action);
                                amountField.setInt(action, currentAmount + 1);

                                logger.info(String.format(
                                    "Ascension 27: Hexaghost Inflame Strength increased by +1 (total: %d)",
                                    power.amount
                                ));
                                break;
                            }
                        }
                    }
                } catch (Exception e) {
                    logger.error("Failed to modify Hexaghost Inflame Strength amount", e);
                }
            }

            lastMove.remove();
        }
    }

    /**
     * Awakened One: Curiosity +1
     * Base game: Curiosity 2 at Asc 19+
     * Level 27: +1 additional Curiosity (total 3)
     */
    @SpirePatch(
        clz = AwakenedOne.class,
        method = "usePreBattleAction"
    )
    public static class AwakenedOneCuriosityPatch {
        @SpirePostfixPatch
        public static void Postfix(AwakenedOne __instance) {
            if (AbstractDungeon.isAscensionMode && AbstractDungeon.ascensionLevel >= 27) {
                // Add 1 Curiosity (base game gives 2 at Asc 19+, so total becomes 3)
                AbstractDungeon.actionManager.addToBottom(
                    new ApplyPowerAction(__instance, __instance,
                        new CuriosityPower(__instance, 1), 1)
                );
                logger.info("Ascension 27: AwakenedOne Curiosity +1 (total: 3)");
            }
        }
    }

    /**
     * Slime Boss: Add 1 more Slimed card
     */
    @SpirePatch(
        clz = SlimeBoss.class,
        method = "takeTurn"
    )
    public static class SlimeBossSlimePatch {
        private static final ThreadLocal<Byte> lastMove = new ThreadLocal<>();

        @SpirePrefixPatch
        public static void Prefix(SlimeBoss __instance) {
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 27) {
                return;
            }

            try {
                java.lang.reflect.Field nextMoveField = com.megacrit.cardcrawl.monsters.AbstractMonster.class.getDeclaredField("nextMove");
                nextMoveField.setAccessible(true);
                byte move = nextMoveField.getByte(__instance);
                lastMove.set(move);
            } catch (Exception e) {
                logger.error("Failed to get SlimeBoss move", e);
            }
        }

        @SpirePostfixPatch
        public static void Postfix(SlimeBoss __instance) {
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 27) {
                return;
            }

            Byte move = lastMove.get();
            if (move != null && move == 1) { // Slime attack move ID
                AbstractDungeon.actionManager.addToBottom(
                    new MakeTempCardInDiscardAction(new Slimed(), 1)
                );
                logger.info("Ascension 27: SlimeBoss added 1 extra Slimed card");
            }

            lastMove.remove();
        }
    }

    /**
     * The Guardian: Vent pattern adds +1 Weak and +1 Vulnerable
     */
    @SpirePatch(
        clz = TheGuardian.class,
        method = "takeTurn"
    )
    public static class GuardianVentPatch {
        private static final ThreadLocal<Byte> lastMove = new ThreadLocal<>();

        @SpirePrefixPatch
        public static void Prefix(TheGuardian __instance) {
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 27) {
                return;
            }

            try {
                java.lang.reflect.Field nextMoveField = com.megacrit.cardcrawl.monsters.AbstractMonster.class.getDeclaredField("nextMove");
                nextMoveField.setAccessible(true);
                byte move = nextMoveField.getByte(__instance);
                lastMove.set(move);
            } catch (Exception e) {
                logger.error("Failed to get Guardian move", e);
            }
        }

        @SpirePostfixPatch
        public static void Postfix(TheGuardian __instance) {
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 27) {
                return;
            }

            Byte move = lastMove.get();
            if (move != null && move == 4) { // Vent move ID
                AbstractDungeon.actionManager.addToBottom(
                    new ApplyPowerAction(AbstractDungeon.player, __instance,
                        new WeakPower(AbstractDungeon.player, 1, true), 1)
                );
                AbstractDungeon.actionManager.addToBottom(
                    new ApplyPowerAction(AbstractDungeon.player, __instance,
                        new VulnerablePower(AbstractDungeon.player, 1, true), 1)
                );
                logger.info("Ascension 27: TheGuardian Vent added +1 Weak and +1 Vulnerable");
            }

            lastMove.remove();
        }
    }

    /**
     * The Champ: Strength gain pattern gains +1 more Strength
     */
    @SpirePatch(
        clz = Champ.class,
        method = "takeTurn"
    )
    public static class ChampStrengthPatch {
        private static final ThreadLocal<Byte> lastMove = new ThreadLocal<>();

        @SpirePrefixPatch
        public static void Prefix(Champ __instance) {
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 27) {
                return;
            }

            try {
                java.lang.reflect.Field nextMoveField = com.megacrit.cardcrawl.monsters.AbstractMonster.class.getDeclaredField("nextMove");
                nextMoveField.setAccessible(true);
                byte move = nextMoveField.getByte(__instance);
                lastMove.set(move);
            } catch (Exception e) {
                logger.error("Failed to get Champ move", e);
            }
        }

        @SpirePostfixPatch
        public static void Postfix(Champ __instance) {
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 27) {
                return;
            }

            Byte move = lastMove.get();
            if (move != null && move == 2) { // Buff move ID (gains Strength)
                AbstractDungeon.actionManager.addToBottom(
                    new ApplyPowerAction(__instance, __instance,
                        new StrengthPower(__instance, 1), 1)
                );
                logger.info("Ascension 27: Champ gained +1 additional Strength");
            }

            lastMove.remove();
        }
    }

    /**
     * The Collector: Debuff pattern applies Weak/Vulnerable/Frail 3 each, twice
     */
    @SpirePatch(
        clz = TheCollector.class,
        method = "takeTurn"
    )
    public static class CollectorDebuffPatch {
        private static final ThreadLocal<Byte> lastMove = new ThreadLocal<>();

        @SpirePrefixPatch
        public static void Prefix(TheCollector __instance) {
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 27) {
                return;
            }

            try {
                java.lang.reflect.Field nextMoveField = com.megacrit.cardcrawl.monsters.AbstractMonster.class.getDeclaredField("nextMove");
                nextMoveField.setAccessible(true);
                byte move = nextMoveField.getByte(__instance);
                lastMove.set(move);
            } catch (Exception e) {
                logger.error("Failed to get TheCollector move", e);
            }
        }

        @SpirePostfixPatch
        public static void Postfix(TheCollector __instance) {
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 27) {
                return;
            }

            Byte move = lastMove.get();
            if (move != null && move == 2) { // Mega Debuff move ID
                // Apply Weak 3, Vulnerable 3, Frail 3 twice
                AbstractDungeon.actionManager.addToBottom(
                    new ApplyPowerAction(AbstractDungeon.player, __instance,
                        new WeakPower(AbstractDungeon.player, 3, true), 3)
                );
                AbstractDungeon.actionManager.addToBottom(
                    new ApplyPowerAction(AbstractDungeon.player, __instance,
                        new VulnerablePower(AbstractDungeon.player, 3, true), 3)
                );
                AbstractDungeon.actionManager.addToBottom(
                    new ApplyPowerAction(AbstractDungeon.player, __instance,
                        new FrailPower(AbstractDungeon.player, 3, true), 3)
                );
                logger.info("Ascension 27: TheCollector applied additional debuffs (Weak/Vulnerable/Frail 3 each)");
            }

            lastMove.remove();
        }
    }

    /**
     * Donu: Attack adds 1 more Dazed card
     */
    @SpirePatch(
        clz = Donu.class,
        method = "takeTurn"
    )
    public static class DonuDazedPatch {
        private static final ThreadLocal<Byte> lastMove = new ThreadLocal<>();

        @SpirePrefixPatch
        public static void Prefix(Donu __instance) {
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 27) {
                return;
            }

            try {
                java.lang.reflect.Field nextMoveField = com.megacrit.cardcrawl.monsters.AbstractMonster.class.getDeclaredField("nextMove");
                nextMoveField.setAccessible(true);
                byte move = nextMoveField.getByte(__instance);
                lastMove.set(move);
            } catch (Exception e) {
                logger.error("Failed to get Donu move", e);
            }
        }

        @SpirePostfixPatch
        public static void Postfix(Donu __instance) {
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 27) {
                return;
            }

            Byte move = lastMove.get();
            if (move != null && (move == 2 || move == 3)) { // Circle of Power or attack moves
                AbstractDungeon.actionManager.addToBottom(
                    new MakeTempCardInDiscardAction(new Dazed(), 1)
                );
                logger.info("Ascension 27: Donu added 1 extra Dazed card");
            }

            lastMove.remove();
        }
    }

    /**
     * Bronze Automaton: Bronze Orb minions have +20 HP
     */
    @SpirePatch(
        clz = com.megacrit.cardcrawl.monsters.city.BronzeOrb.class,
        method = SpirePatch.CONSTRUCTOR
    )
    public static class BronzeOrbHPPatch {
        @SpirePostfixPatch
        public static void Postfix(com.megacrit.cardcrawl.monsters.AbstractMonster __instance) {
            if (AbstractDungeon.isAscensionMode && AbstractDungeon.ascensionLevel >= 27) {
                int originalHP = __instance.maxHealth;
                __instance.maxHealth += 20;
                __instance.currentHealth += 20;

                logger.info(String.format(
                    "Ascension 27: BronzeOrb HP increased from %d to %d (+20)",
                    originalHP,
                    __instance.maxHealth
                ));
            }
        }
    }

    /**
     * Time Eater: Heal pattern heals to 70% HP instead of 50%
     */
    @SpirePatch(
        clz = TimeEater.class,
        method = "takeTurn"
    )
    public static class TimeEaterHealPatch {
        @SpireInsertPatch(
            locator = TimeEaterHealLocator.class
        )
        public static void Insert(TimeEater __instance) {
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 27) {
                return;
            }

            // Add additional 20% healing (from 50% to 70%)
            int additionalHeal = (int) (__instance.maxHealth * 0.2f);
            AbstractDungeon.actionManager.addToBottom(
                new com.megacrit.cardcrawl.actions.common.HealAction(
                    __instance, __instance, additionalHeal
                )
            );

            logger.info(String.format(
                "Ascension 27: TimeEater will heal additional %d HP (to 70%% instead of 50%%)",
                additionalHeal
            ));
        }

        private static class TimeEaterHealLocator extends SpireInsertLocator {
            @Override
            public int[] Locate(CtBehavior ctBehavior) throws Exception {
                // Find HealAction in case 5 (HASTE move)
                Matcher healActionMatcher = new Matcher.NewExprMatcher(
                    com.megacrit.cardcrawl.actions.common.HealAction.class
                );
                return LineFinder.findInOrder(ctBehavior, healActionMatcher);
            }
        }
    }

    /**
     * Corrupt Heart: Invincible = 180 (reduced from 200)
     */
    @SpirePatch(
        clz = CorruptHeart.class,
        method = "usePreBattleAction"
    )
    public static class CorruptHeartInvinciblePatch {
        @SpirePostfixPatch
        public static void Postfix(CorruptHeart __instance) {
            if (AbstractDungeon.isAscensionMode && AbstractDungeon.ascensionLevel >= 27) {
                // At A19+, Corrupt Heart has 200 Invincible (300 - 100)
                // We reduce it by 20 to make it 180
                com.megacrit.cardcrawl.powers.AbstractPower invinciblePower = __instance.getPower("Invincible");
                if (invinciblePower != null) {
                    invinciblePower.amount -= 20;
                    invinciblePower.updateDescription();
                    logger.info(String.format(
                        "Ascension 27: CorruptHeart Invincible reduced to %d (from 200)",
                        invinciblePower.amount
                    ));
                }
            }
        }
    }
}
