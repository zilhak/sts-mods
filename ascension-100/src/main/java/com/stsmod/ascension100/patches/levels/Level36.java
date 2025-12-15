package com.stsmod.ascension100.patches.levels;

import com.badlogic.gdx.math.MathUtils;
import com.evacipated.cardcrawl.modthespire.lib.ByRef;
import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePrefixPatch;
import com.evacipated.cardcrawl.modthespire.lib.SpireReturn;
import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.actions.common.ApplyPowerAction;
import com.megacrit.cardcrawl.actions.common.MakeTempCardInDiscardAction;
import com.megacrit.cardcrawl.actions.common.MakeTempCardInDrawPileAction;
import com.megacrit.cardcrawl.actions.common.GainBlockAction;
import com.megacrit.cardcrawl.cards.DamageInfo;
import com.megacrit.cardcrawl.cards.status.Dazed;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import com.megacrit.cardcrawl.monsters.exordium.GremlinNob;
import com.megacrit.cardcrawl.monsters.exordium.Lagavulin;
import com.megacrit.cardcrawl.monsters.exordium.Sentry;
import com.megacrit.cardcrawl.monsters.city.BookOfStabbing;
import com.megacrit.cardcrawl.monsters.city.GremlinLeader;
import com.megacrit.cardcrawl.monsters.city.Taskmaster;
import com.megacrit.cardcrawl.monsters.beyond.GiantHead;
import com.megacrit.cardcrawl.monsters.beyond.Nemesis;
import com.megacrit.cardcrawl.monsters.beyond.Reptomancer;
import com.megacrit.cardcrawl.monsters.exordium.GremlinWarrior;
import com.megacrit.cardcrawl.monsters.exordium.GremlinFat;
import com.megacrit.cardcrawl.monsters.exordium.GremlinThief;
import com.megacrit.cardcrawl.monsters.exordium.GremlinWizard;
import com.megacrit.cardcrawl.monsters.exordium.GremlinTsundere;
import com.megacrit.cardcrawl.powers.AbstractPower;
import com.megacrit.cardcrawl.powers.VulnerablePower;
import com.megacrit.cardcrawl.powers.StrengthPower;
import com.megacrit.cardcrawl.powers.WeakPower;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.cards.status.Wound;
import com.megacrit.cardcrawl.cards.status.Burn;
import com.megacrit.cardcrawl.actions.unique.SummonGremlinAction;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.reflect.Field;

/**
 * Ascension Level 36: Elite monster enhancements
 *
 * 엘리트 몬스터들이 강화됩니다.
 * - Gremlin Nob: Skull Bash -5 dmg, Rush +3 dmg, Bellow applies Vulnerable 1 to self, +10% HP
 * - Lagavulin: Sleep +1 turn (3→4), +8% HP
 * - Sentry (CENTER ONLY): +15 HP, +3 attack
 * - Book of Stabbing: Multi-stab -2 dmg per hit, +1 hit count; Single Stab gains Strength 1
 * - Gremlin Leader: Rally gives +5 block to all allies, HP +5%, minion HP -25%, Rally summons to total of 3
 * - Taskmaster: Scouring Whip adds 1 extra Wound
 * - Giant Head: HP +20.1%, It Is Time damage has no limit
 * - Nemesis: Debuff pattern adds 1 extra Burn to draw pile
 * - Reptomancer: Snake Strike applies Weak 2 (instead of 1)
 */
public class Level36 {
    private static final Logger logger = LogManager.getLogger(Level36.class.getName());

    // ========================================
    // Gremlin Nob: Damage adjustments + Vulnerable on Bellow
    // ========================================

    /**
     * Gremlin Nob: Skull Bash -5 dmg, Rush +3 dmg, HP +10%
     * Uses constructor Prefix to ensure first turn applies correctly
     */
    @SpirePatch(
        clz = GremlinNob.class,
        method = SpirePatch.CONSTRUCTOR,
        paramtypez = {float.class, float.class}
    )
    public static class GremlinNobDamagePatch {
        @SpirePrefixPatch
        public static void Prefix(GremlinNob __instance, float x, float y) {
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 36) {
                return;
            }

            try {
                // damage[0] = rushDmg (BULL_RUSH = byte 1)
                // damage[1] = bashDmg (SKULL_BASH = byte 2)

                if (__instance.damage.size() >= 2) {
                    DamageInfo rushDmg = __instance.damage.get(0);
                    DamageInfo bashDmg = __instance.damage.get(1);

                    int originalRush = rushDmg.base;
                    int originalBash = bashDmg.base;

                    // Rush +3
                    rushDmg.base += 3;
                    // Skull Bash -5
                    bashDmg.base = Math.max(1, bashDmg.base - 5);

                    logger.info(String.format(
                        "Ascension 36: Gremlin Nob damage adjusted - Rush: %d→%d (+3), Skull Bash: %d→%d (-5)",
                        originalRush, rushDmg.base, originalBash, bashDmg.base
                    ));
                }
            } catch (Exception e) {
                logger.error("Failed to modify Gremlin Nob damage", e);
            }
        }

        @SpirePostfixPatch
        public static void Postfix(GremlinNob __instance) {
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 36) {
                return;
            }

            // HP +10%
            int originalHP = __instance.maxHealth;
            __instance.maxHealth = MathUtils.ceil(__instance.maxHealth * 1.10f);
            __instance.currentHealth = MathUtils.ceil(__instance.currentHealth * 1.10f);

            logger.info(String.format(
                "Ascension 36: Gremlin Nob HP increased from %d to %d (+10%%)",
                originalHP, __instance.maxHealth
            ));
        }
    }

    /**
     * Gremlin Nob: Bellow applies Vulnerable 1 to self
     */
    @SpirePatch(
        clz = GremlinNob.class,
        method = "takeTurn"
    )
    public static class GremlinNobBellowPatch {
        @SpirePostfixPatch
        public static void Postfix(GremlinNob __instance) {
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 36) {
                return;
            }

            // BELLOW = byte 3
            if (__instance.nextMove == 3) {
                AbstractDungeon.actionManager.addToBottom(
                    new ApplyPowerAction(
                        __instance, __instance,
                        new VulnerablePower(__instance, 1, true), 1
                    )
                );

                logger.info("Ascension 36: Gremlin Nob applied Vulnerable 1 to self on Bellow");
            }
        }
    }

    // ========================================
    // Lagavulin: Sleep +1 turn, HP +8%
    // ========================================

    /**
     * Lagavulin: HP +8%
     */
    @SpirePatch(
        clz = Lagavulin.class,
        method = SpirePatch.CONSTRUCTOR,
        paramtypez = {boolean.class}
    )
    public static class LagavulinHPPatch {
        @SpirePostfixPatch
        public static void Postfix(Lagavulin __instance, boolean setAsleep) {
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 36) {
                return;
            }

            int originalHP = __instance.maxHealth;
            __instance.maxHealth = MathUtils.ceil(__instance.maxHealth * 1.08f);
            __instance.currentHealth = MathUtils.ceil(__instance.currentHealth * 1.08f);

            logger.info(String.format(
                "Ascension 36: Lagavulin HP increased from %d to %d (+8%%)",
                originalHP, __instance.maxHealth
            ));
        }
    }

    /**
     * Lagavulin: Sleep duration +1 turn (3→4)
     * Uses Prefix to reset idleCount before takeTurn processes it
     */
    private static java.util.WeakHashMap<Lagavulin, Boolean> lagavulinSleepExtended = new java.util.WeakHashMap<>();

    @SpirePatch(
        clz = Lagavulin.class,
        method = "takeTurn"
    )
    public static class LagavulinSleepPatch {
        @SpirePrefixPatch
        public static void Prefix(Lagavulin __instance) {
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 36) {
                return;
            }

            try {
                // IDLE = byte 5
                if (__instance.nextMove == 5) {
                    // Access private idleCount field
                    Field idleCountField = Lagavulin.class.getDeclaredField("idleCount");
                    idleCountField.setAccessible(true);
                    int currentIdleCount = idleCountField.getInt(__instance);

                    // When idleCount == 2 (would become 3 this turn), reset to 1 (once only)
                    // This extends sleep by 1 turn: 0→1→2→(reset to 1)→2→3
                    if (currentIdleCount == 2 && !lagavulinSleepExtended.getOrDefault(__instance, false)) {
                        idleCountField.setInt(__instance, 1);
                        lagavulinSleepExtended.put(__instance, true);
                        logger.info("Ascension 36: Lagavulin sleep extended by 1 turn (idleCount reset 2→1 before takeTurn)");
                    }
                }
            } catch (Exception e) {
                logger.error("Failed to extend Lagavulin sleep", e);
            }
        }
    }

    // ========================================
    // Sentry (CENTER ONLY): HP +15, attack +3
    // ========================================

    /**
     * Sentry: Center sentry gets +15 HP and +3 attack damage
     */
    @SpirePatch(
        clz = Sentry.class,
        method = SpirePatch.CONSTRUCTOR,
        paramtypez = {float.class, float.class}
    )
    public static class SentryCenterStatsPatch {
        @SpirePostfixPatch
        public static void Postfix(Sentry __instance, float x, float y) {
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 36) {
                return;
            }

            // Check if this is the center Sentry by x coordinate
            // Center Sentry spawns at x = -85.0F
            if (Math.abs(x - (-85.0F)) < 1.0F) {
                // HP +15
                int originalHP = __instance.maxHealth;
                __instance.maxHealth += 15;
                __instance.currentHealth += 15;

                // Attack +3 (BEAM attack uses damage[0])
                if (!__instance.damage.isEmpty()) {
                    DamageInfo beamDmg = __instance.damage.get(0);
                    int originalDmg = beamDmg.base;
                    beamDmg.base += 3;

                    logger.info(String.format(
                        "Ascension 36: Center Sentry enhanced - HP: %d→%d (+15), Beam attack: %d→%d (+3)",
                        originalHP, __instance.maxHealth, originalDmg, beamDmg.base
                    ));
                }
            }
        }
    }

    // ========================================
    // Book of Stabbing: Multi-stab -2 dmg per hit, +1 hit count, Single Stab gains Strength 1
    // ========================================

    /**
     * Book of Stabbing: Stab damage -2, initial stab count +1
     */
    @SpirePatch(
        clz = BookOfStabbing.class,
        method = SpirePatch.CONSTRUCTOR
    )
    public static class BookOfStabbingPatch {
        @SpirePostfixPatch
        public static void Postfix(BookOfStabbing __instance) {
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 36) {
                return;
            }

            try {
                // Reduce stab damage by 2 (damage[0] is multi-stab)
                if (!__instance.damage.isEmpty()) {
                    DamageInfo stabDmg = __instance.damage.get(0);
                    int originalDmg = stabDmg.base;
                    stabDmg.base = Math.max(1, stabDmg.base - 2);
                    stabDmg.output = stabDmg.base; // Update output too

                    logger.info(String.format(
                        "Ascension 36: Book of Stabbing stab damage reduced from %d to %d (-2)",
                        originalDmg, stabDmg.base
                    ));
                }

                // Increase initial stabCount from 1 to 2
                Field stabCountField = BookOfStabbing.class.getDeclaredField("stabCount");
                stabCountField.setAccessible(true);
                stabCountField.setInt(__instance, 2);

                logger.info("Ascension 36: Book of Stabbing initial stab count set to 2 (+1)");
            } catch (Exception e) {
                logger.error("Failed to modify Book of Stabbing stats", e);
            }
        }
    }

    /**
     * Book of Stabbing: Single Stab (BIG_STAB) grants Strength 1
     */
    @SpirePatch(
        clz = BookOfStabbing.class,
        method = "takeTurn"
    )
    public static class BookOfStabbingSingleStabPatch {
        @SpirePostfixPatch
        public static void Postfix(BookOfStabbing __instance) {
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 36) {
                return;
            }

            // BIG_STAB = byte 2
            if (__instance.nextMove == 2) {
                AbstractDungeon.actionManager.addToBottom(
                    new ApplyPowerAction(
                        __instance, __instance,
                        new StrengthPower(__instance, 1), 1
                    )
                );

                logger.info("Ascension 36: Book of Stabbing Single Stab grants Strength 1");
            }
        }
    }

    /**
     * Book of Stabbing: Change Single Stab Intent to ATTACK_BUFF
     * Intercepts setMove calls and changes Intent.ATTACK to Intent.ATTACK_BUFF for move byte 2
     */
    @SpirePatch(
        clz = AbstractMonster.class,
        method = "setMove",
        paramtypez = {byte.class, AbstractMonster.Intent.class, int.class}
    )
    public static class BookOfStabbingIntentPatch {
        @SpirePrefixPatch
        public static void Prefix(AbstractMonster __instance, byte nextMove, @ByRef AbstractMonster.Intent[] intent, int baseDamage) {
            // Only apply to Book of Stabbing at A36+
            if (!(__instance instanceof BookOfStabbing)) {
                return;
            }

            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 36) {
                return;
            }

            // If this is BIG_STAB move (byte 2), change Intent to ATTACK_BUFF
            if (nextMove == 2 && intent[0] == AbstractMonster.Intent.ATTACK) {
                intent[0] = AbstractMonster.Intent.ATTACK_BUFF;
                logger.info("Ascension 36: Book of Stabbing Single Stab Intent changed to ATTACK_BUFF");
            }
        }
    }

    // ========================================
    // Gremlin Leader: Rally gives +5 block to all allies
    // ========================================

    /**
     * Gremlin Leader: Set blockAmt to 5
     */
    @SpirePatch(
        clz = GremlinLeader.class,
        method = SpirePatch.CONSTRUCTOR
    )
    public static class GremlinLeaderBlockPatch {
        @SpirePostfixPatch
        public static void Postfix(GremlinLeader __instance) {
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 36) {
                return;
            }

            try {
                // Set blockAmt to 5
                Field blockAmtField = GremlinLeader.class.getDeclaredField("blockAmt");
                blockAmtField.setAccessible(true);
                int originalBlock = blockAmtField.getInt(__instance);
                blockAmtField.setInt(__instance, 5);

                logger.info(String.format(
                    "Ascension 36: Gremlin Leader Rally block amount changed from %d to 5",
                    originalBlock
                ));
            } catch (Exception e) {
                logger.error("Failed to modify Gremlin Leader blockAmt", e);
            }
        }
    }

    /**
     * Gremlin Leader: Rally gives block to all allies including self
     * Adds block action for the leader during ENCOURAGE pattern
     */
    @SpirePatch(
        clz = GremlinLeader.class,
        method = "takeTurn"
    )
    public static class GremlinLeaderRallyPatch {
        @SpirePostfixPatch
        public static void Postfix(GremlinLeader __instance) {
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 36) {
                return;
            }

            try {
                // ENCOURAGE = byte 3
                if (__instance.nextMove == 3) {
                    // Get blockAmt field
                    Field blockAmtField = GremlinLeader.class.getDeclaredField("blockAmt");
                    blockAmtField.setAccessible(true);
                    int blockAmt = blockAmtField.getInt(__instance);

                    // Add block to the leader (in vanilla, leader only gets Strength)
                    AbstractDungeon.actionManager.addToBottom(
                        new GainBlockAction(__instance, __instance, blockAmt)
                    );

                    logger.info(String.format(
                        "Ascension 36: Gremlin Leader Rally now gives %d block to all allies (including self)",
                        blockAmt
                    ));
                }
            } catch (Exception e) {
                logger.error("Failed to add Rally block to Gremlin Leader", e);
            }
        }
    }

    /**
     * Gremlin Leader: HP +5%
     */
    @SpirePatch(
        clz = GremlinLeader.class,
        method = SpirePatch.CONSTRUCTOR
    )
    public static class GremlinLeaderHPPatch {
        @SpirePostfixPatch
        public static void Postfix(GremlinLeader __instance) {
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 36) {
                return;
            }

            int originalHP = __instance.maxHealth;
            __instance.maxHealth = MathUtils.ceil(__instance.maxHealth * 1.05f);
            __instance.currentHealth = MathUtils.ceil(__instance.currentHealth * 1.05f);

            logger.info(String.format(
                "Ascension 36: Gremlin Leader HP increased from %d to %d (+5%%)",
                originalHP, __instance.maxHealth
            ));
        }
    }

    /**
     * Gremlin minions: HP -25%
     */
    @SpirePatch(
        clz = AbstractMonster.class,
        method = "init"
    )
    public static class GremlinMinionHPPatch {
        @SpirePostfixPatch
        public static void Postfix(AbstractMonster __instance) {
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 36) {
                return;
            }

            // Check if this is a gremlin minion (not the leader)
            if (__instance instanceof GremlinWarrior ||
                __instance instanceof GremlinFat ||
                __instance instanceof GremlinThief ||
                __instance instanceof GremlinWizard ||
                __instance instanceof GremlinTsundere) {

                int originalHP = __instance.maxHealth;
                __instance.maxHealth = MathUtils.ceil(__instance.maxHealth * 0.75f);
                __instance.currentHealth = MathUtils.ceil(__instance.currentHealth * 0.75f);

                logger.info(String.format(
                    "Ascension 36: Gremlin minion %s HP decreased from %d to %d (-25%%)",
                    __instance.name, originalHP, __instance.maxHealth
                ));
            }
        }
    }

    /**
     * Gremlin Leader: Rally summons gremlins to make total of 3
     */
    @SpirePatch(
        clz = GremlinLeader.class,
        method = "takeTurn"
    )
    public static class GremlinLeaderRallySummonPatch {
        @SpirePostfixPatch
        public static void Postfix(GremlinLeader __instance) {
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 36) {
                return;
            }

            try {
                // RALLY = byte 2
                if (__instance.nextMove == 2) {
                    // Access gremlins array field
                    Field gremlinsField = GremlinLeader.class.getDeclaredField("gremlins");
                    gremlinsField.setAccessible(true);
                    AbstractMonster[] gremlins = (AbstractMonster[]) gremlinsField.get(__instance);

                    // Count available slots in gremlins array (null or isDying)
                    int availableSlots = 0;
                    for (AbstractMonster g : gremlins) {
                        if (g == null || g.isDying) {
                            availableSlots++;
                        }
                    }

                    // Count alive minions (excluding the leader)
                    int aliveMinions = 0;
                    for (AbstractMonster m : AbstractDungeon.getMonsters().monsters) {
                        if (m != __instance && !m.isDying && !m.isDead && !m.isEscaping) {
                            aliveMinions++;
                        }
                    }

                    // Calculate how many to summon (target: 3 total, limited by available slots)
                    int targetTotal = 3;
                    int toSummon = Math.min(availableSlots, targetTotal - aliveMinions);

                    // Summon only if we have both the need and available slots
                    if (toSummon > 0) {
                        for (int i = 0; i < toSummon; i++) {
                            AbstractDungeon.actionManager.addToBottom(
                                new SummonGremlinAction(gremlins)
                            );
                        }

                        logger.info(String.format(
                            "Ascension 36: Gremlin Leader Rally summoning %d gremlins (alive: %d, slots: %d, target: 3)",
                            toSummon, aliveMinions, availableSlots
                        ));
                    }
                }
            } catch (Exception e) {
                logger.error("Failed to summon gremlins for Rally", e);
            }
        }
    }

    // ========================================
    // Taskmaster: Scouring Whip adds extra Wound
    // ========================================

    /**
     * Taskmaster: Scouring Whip adds 1 extra Wound
     */
    @SpirePatch(
        clz = Taskmaster.class,
        method = "takeTurn"
    )
    public static class TaskmasterWoundPatch {
        @SpirePostfixPatch
        public static void Postfix(Taskmaster __instance) {
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 36) {
                return;
            }

            // SCOURING_WHIP = byte 2
            if (__instance.nextMove == 2) {
                AbstractDungeon.actionManager.addToBottom(
                    new MakeTempCardInDiscardAction(new Wound(), 1)
                );

                logger.info("Ascension 36: Taskmaster Scouring Whip added 1 extra Wound");
            }
        }
    }

    // ========================================
    // Giant Head: HP +20.1%
    // ========================================

    /**
     * Giant Head: HP +20.1%
     */
    @SpirePatch(
        clz = GiantHead.class,
        method = SpirePatch.CONSTRUCTOR
    )
    public static class GiantHeadHPPatch {
        @SpirePostfixPatch
        public static void Postfix(GiantHead __instance) {
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 36) {
                return;
            }

            int originalHP = __instance.maxHealth;
            __instance.maxHealth = MathUtils.ceil(__instance.maxHealth * 1.201f);
            __instance.currentHealth = MathUtils.ceil(__instance.currentHealth * 1.201f);

            logger.info(String.format(
                "Ascension 36: Giant Head HP increased from %d to %d (+20.1%%)",
                originalHP, __instance.maxHealth
            ));
        }
    }

    // ========================================
    // Nemesis: Debuff adds 1 extra Burn
    // ========================================

    /**
     * Nemesis: Debuff pattern adds 1 extra Burn card to draw pile
     */
    @SpirePatch(
        clz = Nemesis.class,
        method = "takeTurn"
    )
    public static class NemesisExtraBurnPatch {
        @SpirePostfixPatch
        public static void Postfix(Nemesis __instance) {
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 36) {
                return;
            }

            logger.info("Ascension 36: Nemesis takeTurn() called, nextMove = " + __instance.nextMove);

            // TRI_BURN = byte 4
            if (__instance.nextMove == 4) {
                // Add 1 extra Burn card to draw pile
                AbstractDungeon.actionManager.addToBottom(
                    new MakeTempCardInDrawPileAction(new Burn(), 1, true, true)
                );

                logger.info("Ascension 36: Nemesis Debuff added 1 extra Burn card to draw pile");
            }
        }
    }

    // ========================================
    // Reptomancer: Snake Strike applies Weak 2 instead of 1
    // ========================================

    /**
     * Reptomancer: Snake Strike applies Weak 2 (instead of original 1)
     * Intercepts ApplyPowerAction in takeTurn and modifies the weak amount
     */
    @SpirePatch(
        clz = Reptomancer.class,
        method = "takeTurn"
    )
    public static class ReptomancerWeakPatch {
        @SpirePostfixPatch
        public static void Postfix(Reptomancer __instance) {
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 36) {
                return;
            }

            // SNAKE_STRIKE = byte 1
            if (__instance.nextMove == 1) {
                // Find and modify the ApplyPowerAction that applies Weak
                for (int i = AbstractDungeon.actionManager.actions.size() - 1; i >= 0; i--) {
                    AbstractGameAction action = AbstractDungeon.actionManager.actions.get(i);
                    if (action instanceof ApplyPowerAction) {
                        ApplyPowerAction applyAction = (ApplyPowerAction) action;
                        // Check if it's applying WeakPower with amount 1
                        try {
                            Field powerField = ApplyPowerAction.class.getDeclaredField("powerToApply");
                            powerField.setAccessible(true);
                            AbstractPower power = (AbstractPower) powerField.get(applyAction);

                            if (power instanceof WeakPower && power.amount == 1) {
                                // Change the amount from 1 to 2
                                power.amount = 2;
                                logger.info("Ascension 36: Reptomancer Snake Strike Weak increased from 1 to 2");
                                break;
                            }
                        } catch (Exception e) {
                            logger.error("Failed to modify Reptomancer Weak amount", e);
                        }
                    }
                }
            }
        }
    }
}
