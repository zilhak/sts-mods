package com.stsmod.ascension100.patches.levels;

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
import com.megacrit.cardcrawl.monsters.ending.CorruptHeart;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Ascension Level 26: Boss stats enhanced
 * 보스의 능력치가 강화됩니다.
 */
public class Level26 {

    private static final Logger logger = LogManager.getLogger(Level26.class.getName());

    /**
     * Slime Boss: +15 HP
     */
    @SpirePatch(
        clz = SlimeBoss.class,
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
        clz = TheGuardian.class,
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
        clz = Hexaghost.class,
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
        clz = BronzeAutomaton.class,
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
        clz = Champ.class,
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
        clz = TheCollector.class,
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
     * Awakened One: +30 HP (Phase 1), +40 HP (Phase 2)
     */
    @SpirePatch(
        clz = AwakenedOne.class,
        method = SpirePatch.CONSTRUCTOR
    )
    public static class AwakenedOneHPPatch {
        @SpirePostfixPatch
        public static void Postfix(AwakenedOne __instance) {
            if (AbstractDungeon.isAscensionMode && AbstractDungeon.ascensionLevel >= 26) {
                // Phase 1 HP +30
                int originalHP = __instance.maxHealth;
                __instance.maxHealth += 30;
                __instance.currentHealth += 30;

                // Phase 2 HP +40
                try {
                    java.lang.reflect.Field form2HPField = AwakenedOne.class.getDeclaredField("form2HP");
                    form2HPField.setAccessible(true);
                    int originalForm2HP = form2HPField.getInt(__instance);
                    form2HPField.setInt(__instance, originalForm2HP + 40);

                    logger.info(String.format(
                        "Ascension 26: AwakenedOne Phase 1 HP increased from %d to %d (+30), Phase 2 HP increased from %d to %d (+40)",
                        originalHP,
                        __instance.maxHealth,
                        originalForm2HP,
                        originalForm2HP + 40
                    ));
                } catch (Exception e) {
                    logger.error("Failed to modify AwakenedOne Phase 2 HP", e);
                    logger.info(String.format(
                        "Ascension 26: AwakenedOne Phase 1 HP increased from %d to %d (+30)",
                        originalHP,
                        __instance.maxHealth
                    ));
                }
            }
        }
    }

    /**
     * Time Eater: +40 HP
     */
    @SpirePatch(
        clz = TimeEater.class,
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
        clz = Donu.class,
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
        clz = Deca.class,
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

    /**
     * Corrupt Heart: +100 HP
     */
    @SpirePatch(
        clz = CorruptHeart.class,
        method = SpirePatch.CONSTRUCTOR
    )
    public static class CorruptHeartHPPatch {
        @SpirePostfixPatch
        public static void Postfix(CorruptHeart __instance) {
            if (AbstractDungeon.isAscensionMode && AbstractDungeon.ascensionLevel >= 26) {
                int originalHP = __instance.maxHealth;
                __instance.maxHealth += 100;
                __instance.currentHealth += 100;

                logger.info(String.format(
                    "Ascension 26: CorruptHeart HP increased from %d to %d (+100)",
                    originalHP,
                    __instance.maxHealth
                ));
            }
        }
    }
}
