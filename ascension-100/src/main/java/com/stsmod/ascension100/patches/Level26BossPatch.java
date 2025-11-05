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
import com.megacrit.cardcrawl.monsters.beyond.Deca;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Patches for Ascension Level 26 boss stat increases
 */
public class Level26BossPatch {

    private static final Logger logger = LogManager.getLogger(Level26BossPatch.class.getName());

    /**
     * Slime Boss: +15 HP
     */
    @SpirePatch(
        cls = "com.megacrit.cardcrawl.monsters.exordium.SlimeBoss",
        method = SpirePatch.CONSTRUCTOR
    )
    public static class SlimeBossHPPatch {
        @SpirePostfixPatch
        public static void Postfix(SlimeBoss __instance) {
            if (AbstractDungeon.isAscensionMode && AbstractDungeon.ascensionLevel >= 26) {
                int originalHP = __instance.maxHealth;
                __instance.maxHealth += 15;
                __instance.currentHealth += 15;
                logger.info(String.format(
                    "Ascension 26: SlimeBoss HP increased from %d to %d (+15)",
                    originalHP,
                    __instance.maxHealth
                ));
            }
        }
    }

    /**
     * The Guardian: +50 HP
     */
    @SpirePatch(
        cls = "com.megacrit.cardcrawl.monsters.exordium.TheGuardian",
        method = SpirePatch.CONSTRUCTOR
    )
    public static class GuardianHPPatch {
        @SpirePostfixPatch
        public static void Postfix(TheGuardian __instance) {
            if (AbstractDungeon.isAscensionMode && AbstractDungeon.ascensionLevel >= 26) {
                int originalHP = __instance.maxHealth;
                __instance.maxHealth += 50;
                __instance.currentHealth += 50;
                logger.info(String.format(
                    "Ascension 26: TheGuardian HP increased from %d to %d (+50)",
                    originalHP,
                    __instance.maxHealth
                ));
            }
        }
    }

    /**
     * Hexaghost: +20 HP
     */
    @SpirePatch(
        cls = "com.megacrit.cardcrawl.monsters.exordium.Hexaghost",
        method = SpirePatch.CONSTRUCTOR
    )
    public static class HexaghostHPPatch {
        @SpirePostfixPatch
        public static void Postfix(Hexaghost __instance) {
            if (AbstractDungeon.isAscensionMode && AbstractDungeon.ascensionLevel >= 26) {
                int originalHP = __instance.maxHealth;
                __instance.maxHealth += 20;
                __instance.currentHealth += 20;
                logger.info(String.format(
                    "Ascension 26: Hexaghost HP increased from %d to %d (+20)",
                    originalHP,
                    __instance.maxHealth
                ));
            }
        }
    }

    /**
     * Bronze Automaton: +20 HP, +2 Damage
     */
    @SpirePatch(
        cls = "com.megacrit.cardcrawl.monsters.city.BronzeAutomaton",
        method = SpirePatch.CONSTRUCTOR
    )
    public static class BronzeAutomatonPatch {
        @SpirePostfixPatch
        public static void Postfix(BronzeAutomaton __instance) {
            if (AbstractDungeon.isAscensionMode && AbstractDungeon.ascensionLevel >= 26) {
                int originalHP = __instance.maxHealth;
                __instance.maxHealth += 20;
                __instance.currentHealth += 20;

                // Increase damage
                __instance.damage.forEach(damageInfo -> {
                    if (damageInfo != null && damageInfo.base > 0) {
                        damageInfo.base += 2;
                    }
                });

                logger.info(String.format(
                    "Ascension 26: BronzeAutomaton HP increased from %d to %d (+20), damage +2",
                    originalHP,
                    __instance.maxHealth
                ));
            }
        }
    }

    /**
     * The Champ: +2 Damage
     */
    @SpirePatch(
        cls = "com.megacrit.cardcrawl.monsters.city.Champ",
        method = SpirePatch.CONSTRUCTOR
    )
    public static class ChampDamagePatch {
        @SpirePostfixPatch
        public static void Postfix(Champ __instance) {
            if (AbstractDungeon.isAscensionMode && AbstractDungeon.ascensionLevel >= 26) {
                // Increase damage
                __instance.damage.forEach(damageInfo -> {
                    if (damageInfo != null && damageInfo.base > 0) {
                        damageInfo.base += 2;
                    }
                });

                logger.info("Ascension 26: Champ damage +2");
            }
        }
    }

    /**
     * The Collector: +60 HP
     */
    @SpirePatch(
        cls = "com.megacrit.cardcrawl.monsters.city.TheCollector",
        method = SpirePatch.CONSTRUCTOR
    )
    public static class CollectorHPPatch {
        @SpirePostfixPatch
        public static void Postfix(TheCollector __instance) {
            if (AbstractDungeon.isAscensionMode && AbstractDungeon.ascensionLevel >= 26) {
                int originalHP = __instance.maxHealth;
                __instance.maxHealth += 60;
                __instance.currentHealth += 60;

                logger.info(String.format(
                    "Ascension 26: TheCollector HP increased from %d to %d (+60)",
                    originalHP,
                    __instance.maxHealth
                ));
            }
        }
    }

    /**
     * Awakened One: +30 HP
     */
    @SpirePatch(
        cls = "com.megacrit.cardcrawl.monsters.beyond.AwakenedOne",
        method = SpirePatch.CONSTRUCTOR
    )
    public static class AwakenedOneHPPatch {
        @SpirePostfixPatch
        public static void Postfix(AwakenedOne __instance) {
            if (AbstractDungeon.isAscensionMode && AbstractDungeon.ascensionLevel >= 26) {
                int originalHP = __instance.maxHealth;
                __instance.maxHealth += 30;
                __instance.currentHealth += 30;

                logger.info(String.format(
                    "Ascension 26: AwakenedOne HP increased from %d to %d (+30)",
                    originalHP,
                    __instance.maxHealth
                ));
            }
        }
    }

    /**
     * Time Eater: +40 HP
     */
    @SpirePatch(
        cls = "com.megacrit.cardcrawl.monsters.beyond.TimeEater",
        method = SpirePatch.CONSTRUCTOR
    )
    public static class TimeEaterHPPatch {
        @SpirePostfixPatch
        public static void Postfix(TimeEater __instance) {
            if (AbstractDungeon.isAscensionMode && AbstractDungeon.ascensionLevel >= 26) {
                int originalHP = __instance.maxHealth;
                __instance.maxHealth += 40;
                __instance.currentHealth += 40;

                logger.info(String.format(
                    "Ascension 26: TimeEater HP increased from %d to %d (+40)",
                    originalHP,
                    __instance.maxHealth
                ));
            }
        }
    }

    /**
     * Donu: +35 HP
     */
    @SpirePatch(
        cls = "com.megacrit.cardcrawl.monsters.beyond.Donu",
        method = SpirePatch.CONSTRUCTOR
    )
    public static class DonuHPPatch {
        @SpirePostfixPatch
        public static void Postfix(Donu __instance) {
            if (AbstractDungeon.isAscensionMode && AbstractDungeon.ascensionLevel >= 26) {
                int originalHP = __instance.maxHealth;
                __instance.maxHealth += 35;
                __instance.currentHealth += 35;

                logger.info(String.format(
                    "Ascension 26: Donu HP increased from %d to %d (+35)",
                    originalHP,
                    __instance.maxHealth
                ));
            }
        }
    }

    /**
     * Deca: +35 HP
     */
    @SpirePatch(
        cls = "com.megacrit.cardcrawl.monsters.beyond.Deca",
        method = SpirePatch.CONSTRUCTOR
    )
    public static class DecaHPPatch {
        @SpirePostfixPatch
        public static void Postfix(Deca __instance) {
            if (AbstractDungeon.isAscensionMode && AbstractDungeon.ascensionLevel >= 26) {
                int originalHP = __instance.maxHealth;
                __instance.maxHealth += 35;
                __instance.currentHealth += 35;

                logger.info(String.format(
                    "Ascension 26: Deca HP increased from %d to %d (+35)",
                    originalHP,
                    __instance.maxHealth
                ));
            }
        }
    }
}
