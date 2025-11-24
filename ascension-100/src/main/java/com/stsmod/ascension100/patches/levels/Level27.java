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
 * Ascension Level 27: Boss stats enhanced
 * 보스의 능력치가 강화됩니다.
 */
public class Level27 {

    private static final Logger logger = LogManager.getLogger(Level27.class.getName());

    /**
     * Slime Boss: +10 HP
     */
    @SpirePatch(
        clz = SlimeBoss.class,
        method = SpirePatch.CONSTRUCTOR
    )
    public static class SlimeBossHPPatch {
        @SpirePostfixPatch
        public static void Postfix(SlimeBoss __instance) {
            if (AbstractDungeon.isAscensionMode && AbstractDungeon.ascensionLevel >= 27) {
                int originalHP = __instance.maxHealth;
                __instance.maxHealth += 10;
                __instance.currentHealth += 10;
                logger.info(String.format(
                    "Ascension 27: SlimeBoss HP increased from %d to %d (+10)",
                    originalHP,
                    __instance.maxHealth
                ));
            }
        }
    }

    /**
     * The Guardian: +20 HP
     */
    @SpirePatch(
        clz = TheGuardian.class,
        method = SpirePatch.CONSTRUCTOR
    )
    public static class GuardianHPPatch {
        @SpirePostfixPatch
        public static void Postfix(TheGuardian __instance) {
            if (AbstractDungeon.isAscensionMode && AbstractDungeon.ascensionLevel >= 27) {
                int originalHP = __instance.maxHealth;
                __instance.maxHealth += 20;
                __instance.currentHealth += 20;
                logger.info(String.format(
                    "Ascension 27: TheGuardian HP increased from %d to %d (+20)",
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
            if (AbstractDungeon.isAscensionMode && AbstractDungeon.ascensionLevel >= 27) {
                int originalHP = __instance.maxHealth;
                __instance.maxHealth += 20;
                __instance.currentHealth += 20;
                logger.info(String.format(
                    "Ascension 27: Hexaghost HP increased from %d to %d (+20)",
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
            if (AbstractDungeon.isAscensionMode && AbstractDungeon.ascensionLevel >= 27) {
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
                    "Ascension 27: BronzeAutomaton HP increased from %d to %d (+20), damage +2",
                    originalHP,
                    __instance.maxHealth
                ));
            }
        }
    }

    /**
     * Bronze Automaton: +1 Artifact
     */
    @SpirePatch(
        clz = BronzeAutomaton.class,
        method = "usePreBattleAction"
    )
    public static class BronzeAutomatonArtifactPatch {
        @SpirePostfixPatch
        public static void Postfix(BronzeAutomaton __instance) {
            if (AbstractDungeon.isAscensionMode && AbstractDungeon.ascensionLevel >= 27) {
                AbstractDungeon.actionManager.addToBottom(
                    new com.megacrit.cardcrawl.actions.common.ApplyPowerAction(
                        __instance, __instance,
                        new com.megacrit.cardcrawl.powers.ArtifactPower(__instance, 1), 1
                    )
                );
                logger.info("Ascension 27: BronzeAutomaton gained +1 Artifact (total 4)");
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
            if (AbstractDungeon.isAscensionMode && AbstractDungeon.ascensionLevel >= 27) {
                // Increase damage
                __instance.damage.forEach(damageInfo -> {
                    if (damageInfo != null && damageInfo.base > 0) {
                        damageInfo.base += 2;
                    }
                });

                logger.info("Ascension 27: Champ damage +2");
            }
        }
    }

    /**
     * The Collector: +40 HP
     */
    @SpirePatch(
        clz = TheCollector.class,
        method = SpirePatch.CONSTRUCTOR
    )
    public static class CollectorHPPatch {
        @SpirePostfixPatch
        public static void Postfix(TheCollector __instance) {
            if (AbstractDungeon.isAscensionMode && AbstractDungeon.ascensionLevel >= 27) {
                int originalHP = __instance.maxHealth;
                __instance.maxHealth += 40;
                __instance.currentHealth += 40;

                logger.info(String.format(
                    "Ascension 27: TheCollector HP increased from %d to %d (+40)",
                    originalHP,
                    __instance.maxHealth
                ));
            }
        }
    }

    /**
     * Awakened One: +30 HP (Phase 1 only)
     */
    @SpirePatch(
        clz = AwakenedOne.class,
        method = SpirePatch.CONSTRUCTOR
    )
    public static class AwakenedOneHPPatch {
        @SpirePostfixPatch
        public static void Postfix(AwakenedOne __instance) {
            if (AbstractDungeon.isAscensionMode && AbstractDungeon.ascensionLevel >= 27) {
                // Phase 1 HP +30
                int originalHP = __instance.maxHealth;
                __instance.maxHealth += 30;
                __instance.currentHealth += 30;

                logger.info(String.format(
                    "Ascension 27: AwakenedOne Phase 1 HP increased from %d to %d (+30)",
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
        clz = TimeEater.class,
        method = SpirePatch.CONSTRUCTOR
    )
    public static class TimeEaterHPPatch {
        @SpirePostfixPatch
        public static void Postfix(TimeEater __instance) {
            if (AbstractDungeon.isAscensionMode && AbstractDungeon.ascensionLevel >= 27) {
                int originalHP = __instance.maxHealth;
                __instance.maxHealth += 40;
                __instance.currentHealth += 40;

                logger.info(String.format(
                    "Ascension 27: TimeEater HP increased from %d to %d (+40)",
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
            if (AbstractDungeon.isAscensionMode && AbstractDungeon.ascensionLevel >= 27) {
                int originalHP = __instance.maxHealth;
                __instance.maxHealth += 35;
                __instance.currentHealth += 35;

                logger.info(String.format(
                    "Ascension 27: Donu HP increased from %d to %d (+35)",
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
            if (AbstractDungeon.isAscensionMode && AbstractDungeon.ascensionLevel >= 27) {
                int originalHP = __instance.maxHealth;
                __instance.maxHealth += 35;
                __instance.currentHealth += 35;

                logger.info(String.format(
                    "Ascension 27: Deca HP increased from %d to %d (+35)",
                    originalHP,
                    __instance.maxHealth
                ));
            }
        }
    }

    /**
     * Corrupt Heart: +50 HP
     */
    @SpirePatch(
        clz = CorruptHeart.class,
        method = SpirePatch.CONSTRUCTOR
    )
    public static class CorruptHeartHPPatch {
        @SpirePostfixPatch
        public static void Postfix(CorruptHeart __instance) {
            if (AbstractDungeon.isAscensionMode && AbstractDungeon.ascensionLevel >= 27) {
                int originalHP = __instance.maxHealth;
                __instance.maxHealth += 50;
                __instance.currentHealth += 50;

                logger.info(String.format(
                    "Ascension 27: CorruptHeart HP increased from %d to %d (+50)",
                    originalHP,
                    __instance.maxHealth
                ));
            }
        }
    }
}
