package com.stsmod.ascension100.patches;

import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.monsters.exordium.SlimeBoss;
import com.megacrit.cardcrawl.monsters.exordium.TheGuardian;
import com.megacrit.cardcrawl.monsters.exordium.Hexaghost;
import com.megacrit.cardcrawl.monsters.city.BronzeAutomaton;
import com.megacrit.cardcrawl.monsters.city.Champ;
import com.megacrit.cardcrawl.monsters.city.TheCollector;
import com.megacrit.cardcrawl.monsters.beyond.AwakenedOne;
import com.megacrit.cardcrawl.monsters.beyond.TimeEater;
import com.megacrit.cardcrawl.monsters.beyond.Donu;
import com.megacrit.cardcrawl.actions.common.ApplyPowerAction;
import com.megacrit.cardcrawl.actions.common.MakeTempCardInDiscardAction;
import com.megacrit.cardcrawl.cards.status.Slimed;
import com.megacrit.cardcrawl.cards.status.Dazed;
import com.megacrit.cardcrawl.powers.CuriosityPower;
import com.megacrit.cardcrawl.powers.WeakPower;
import com.megacrit.cardcrawl.powers.VulnerablePower;
import com.megacrit.cardcrawl.powers.FrailPower;
import com.megacrit.cardcrawl.powers.StrengthPower;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Patches for Ascension Level 27 boss behavior pattern changes
 */
public class Level27BossPatch {

    private static final Logger logger = LogManager.getLogger(Level27BossPatch.class.getName());

    /**
     * Hexaghost: +1 Damage
     */
    @SpirePatch(
        cls = "com.megacrit.cardcrawl.monsters.exordium.Hexaghost",
        method = SpirePatch.CONSTRUCTOR
    )
    public static class HexaghostDamagePatch {
        @SpirePostfixPatch
        public static void Postfix(Hexaghost __instance) {
            if (AbstractDungeon.isAscensionMode && AbstractDungeon.ascensionLevel >= 27) {
                // Increase damage
                __instance.damage.forEach(damageInfo -> {
                    if (damageInfo != null && damageInfo.base > 0) {
                        damageInfo.base += 1;
                    }
                });

                logger.info("Ascension 27: Hexaghost damage +1");
            }
        }
    }

    /**
     * Awakened One: Curiosity = 3
     */
    @SpirePatch(
        cls = "com.megacrit.cardcrawl.monsters.beyond.AwakenedOne",
        method = "usePreBattleAction"
    )
    public static class AwakenedOneCuriosityPatch {
        @SpirePostfixPatch
        public static void Postfix(AwakenedOne __instance) {
            if (AbstractDungeon.isAscensionMode && AbstractDungeon.ascensionLevel >= 27) {
                // Apply additional Curiosity to reach 3 total
                // Base game gives 1 at Asc 18+, so we add 2 more to reach 3
                AbstractDungeon.actionManager.addToBottom(
                    new ApplyPowerAction(__instance, __instance,
                        new CuriosityPower(__instance, 2), 2)
                );
                logger.info("Ascension 27: AwakenedOne Curiosity increased to 3");
            }
        }
    }

    /**
     * Slime Boss: Add 1 more Slimed card
     * Patches the Slimed card adding action
     */
    @SpirePatch(
        cls = "com.megacrit.cardcrawl.monsters.exordium.SlimeBoss",
        method = "takeTurn"
    )
    public static class SlimeBossSlimePatch {
        @SpirePostfixPatch
        public static void Postfix(SlimeBoss __instance) {
            if (AbstractDungeon.isAscensionMode && AbstractDungeon.ascensionLevel >= 27) {
                // Check if Slime Boss just used the Slime attack (which adds Slimed cards)
                // We add 1 additional Slimed card after any attack that adds Slimed
                if (__instance.nextMove == 1) { // Slime attack move ID
                    AbstractDungeon.actionManager.addToBottom(
                        new MakeTempCardInDiscardAction(new Slimed(), 1)
                    );
                    logger.info("Ascension 27: SlimeBoss added 1 extra Slimed card");
                }
            }
        }
    }

    /**
     * The Guardian: Vent pattern adds +1 Weak and +1 Vulnerable
     */
    @SpirePatch(
        cls = "com.megacrit.cardcrawl.monsters.exordium.TheGuardian",
        method = "takeTurn"
    )
    public static class GuardianVentPatch {
        @SpirePostfixPatch
        public static void Postfix(TheGuardian __instance) {
            if (AbstractDungeon.isAscensionMode && AbstractDungeon.ascensionLevel >= 27) {
                // Check if Guardian just used Vent attack
                if (__instance.nextMove == 4) { // Vent move ID
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
            }
        }
    }

    /**
     * The Champ: Strength gain pattern gains +1 more Strength
     */
    @SpirePatch(
        cls = "com.megacrit.cardcrawl.monsters.city.Champ",
        method = "takeTurn"
    )
    public static class ChampStrengthPatch {
        @SpirePostfixPatch
        public static void Postfix(Champ __instance) {
            if (AbstractDungeon.isAscensionMode && AbstractDungeon.ascensionLevel >= 27) {
                // Check if Champ just used a Strength-gaining move
                if (__instance.nextMove == 2) { // Buff move ID (gains Strength)
                    AbstractDungeon.actionManager.addToBottom(
                        new ApplyPowerAction(__instance, __instance,
                            new StrengthPower(__instance, 1), 1)
                    );
                    logger.info("Ascension 27: Champ gained +1 additional Strength");
                }
            }
        }
    }

    /**
     * The Collector: Debuff pattern applies Weak/Vulnerable/Frail 3 each, twice
     */
    @SpirePatch(
        cls = "com.megacrit.cardcrawl.monsters.city.TheCollector",
        method = "takeTurn"
    )
    public static class CollectorDebuffPatch {
        @SpirePostfixPatch
        public static void Postfix(TheCollector __instance) {
            if (AbstractDungeon.isAscensionMode && AbstractDungeon.ascensionLevel >= 27) {
                // Check if Collector just used the mega debuff move
                if (__instance.nextMove == 2) { // Mega Debuff move ID
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
            }
        }
    }

    /**
     * Donu: Attack adds 1 more Dazed card
     */
    @SpirePatch(
        cls = "com.megacrit.cardcrawl.monsters.beyond.Donu",
        method = "takeTurn"
    )
    public static class DonuDazedPatch {
        @SpirePostfixPatch
        public static void Postfix(Donu __instance) {
            if (AbstractDungeon.isAscensionMode && AbstractDungeon.ascensionLevel >= 27) {
                // Check if Donu just used an attack that adds Dazed
                if (__instance.nextMove == 2 || __instance.nextMove == 3) { // Circle of Power or attack moves
                    AbstractDungeon.actionManager.addToBottom(
                        new MakeTempCardInDiscardAction(new Dazed(), 1)
                    );
                    logger.info("Ascension 27: Donu added 1 extra Dazed card");
                }
            }
        }
    }

    /**
     * Bronze Automaton: Bronze Orb minions have +20 HP
     * Note: This patches the Bronze Orb that spawns during the fight
     */
    @SpirePatch(
        cls = "com.megacrit.cardcrawl.monsters.city.BronzeOrb",
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
     * This modifies the heal amount when Time Eater uses its heal ability
     */
    @SpirePatch(
        cls = "com.megacrit.cardcrawl.monsters.beyond.TimeEater",
        method = "takeTurn"
    )
    public static class TimeEaterHealPatch {
        @SpirePostfixPatch
        public static void Postfix(TimeEater __instance) {
            if (AbstractDungeon.isAscensionMode && AbstractDungeon.ascensionLevel >= 27) {
                // Check if Time Eater just used heal move
                // If it healed to 50%, we need to add additional healing to reach 70%
                if (__instance.nextMove == 5) { // Heal move ID (need to verify)
                    // Calculate additional healing needed (from 50% to 70% = 20% of max HP)
                    int additionalHeal = (int) (__instance.maxHealth * 0.2f);
                    __instance.currentHealth = Math.min(__instance.currentHealth + additionalHeal, __instance.maxHealth);

                    logger.info(String.format(
                        "Ascension 27: TimeEater healed additional %d HP (to 70%% instead of 50%%)",
                        additionalHeal
                    ));
                }
            }
        }
    }
}
