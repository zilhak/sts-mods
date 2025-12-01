package com.stsmod.ascension100.patches.levels;

import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;
import com.megacrit.cardcrawl.actions.common.ApplyPowerAction;
import com.megacrit.cardcrawl.cards.DamageInfo;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import com.megacrit.cardcrawl.monsters.exordium.GremlinNob;
import com.megacrit.cardcrawl.monsters.exordium.Sentry;
import com.megacrit.cardcrawl.monsters.exordium.Lagavulin;
import com.megacrit.cardcrawl.monsters.city.BookOfStabbing;
import com.megacrit.cardcrawl.monsters.city.GremlinLeader;
import com.megacrit.cardcrawl.monsters.city.Taskmaster;
import com.megacrit.cardcrawl.monsters.beyond.GiantHead;
import com.megacrit.cardcrawl.monsters.beyond.Nemesis;
import com.megacrit.cardcrawl.monsters.beyond.Reptomancer;
import com.megacrit.cardcrawl.powers.StrengthPower;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Ascension Level 23: Elite enemies attack enhancements
 *
 * 엘리트 적들의 공격이 강화됩니다.
 *
 * 귀족 그렘린(Gremlin Nob): 공격력 +1
 * 라가불린(Lagavulin): 공격력 +2
 * 보초기(Sentry): 공격력 +1
 * 그렘린 리더(Gremlin Leader): 격려 패턴 힘 +1
 * 노예 관리자(Taskmaster): 공격력 +3
 * 거인의 머리(Giant Head): It Is Time 데미지 +10 (MOVED TO LEVEL25)
 * 네메시스(Nemesis): 공격력 +5
 * 파충류 주술사(Reptomancer): 공격력 +2
 *
 * NOTE: Book of Stabbing damage increase removed (too strong)
 */
public class Level23 {
    private static final Logger logger = LogManager.getLogger(Level23.class.getName());

    /**
     * Gremlin Nob: +1 damage to all attacks
     */
    @SpirePatch(
        clz = GremlinNob.class,
        method = SpirePatch.CONSTRUCTOR,
        paramtypez = {float.class, float.class}
    )
    public static class GremlinNobDamagePatch {
        @SpirePostfixPatch
        public static void Postfix(GremlinNob __instance) {
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 23) {
                return;
            }

            for (DamageInfo damageInfo : __instance.damage) {
                if (damageInfo != null && damageInfo.base > 0) {
                    damageInfo.base += 1;
                }
            }
            logger.info("Ascension 23: Gremlin Nob damage +1");
        }
    }

    /**
     * Lagavulin: +2 damage
     */
    @SpirePatch(
        clz = Lagavulin.class,
        method = SpirePatch.CONSTRUCTOR,
        paramtypez = {boolean.class}
    )
    public static class LagavulinDamagePatch {
        @SpirePostfixPatch
        public static void Postfix(Lagavulin __instance) {
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 23) {
                return;
            }

            for (DamageInfo damageInfo : __instance.damage) {
                if (damageInfo != null && damageInfo.base > 0) {
                    damageInfo.base += 2;
                }
            }
            logger.info("Ascension 23: Lagavulin damage +2");
        }
    }

    /**
     * Sentry: +1 damage
     */
    @SpirePatch(
        clz = Sentry.class,
        method = SpirePatch.CONSTRUCTOR,
        paramtypez = {float.class, float.class}
    )
    public static class SentryDamagePatch {
        @SpirePostfixPatch
        public static void Postfix(Sentry __instance) {
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 23) {
                return;
            }

            for (DamageInfo damageInfo : __instance.damage) {
                if (damageInfo != null && damageInfo.base > 0) {
                    damageInfo.base += 1;
                }
            }
            logger.info("Ascension 23: Sentry damage +1");
        }
    }

    // Book of Stabbing: Damage increase removed (too strong)
    // REMOVED: BookOfStabbingDamagePatch

    /**
     * Taskmaster: +3 damage
     */
    @SpirePatch(
        clz = Taskmaster.class,
        method = SpirePatch.CONSTRUCTOR,
        paramtypez = {float.class, float.class}
    )
    public static class TaskmasterDamagePatch {
        @SpirePostfixPatch
        public static void Postfix(Taskmaster __instance) {
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 23) {
                return;
            }

            for (DamageInfo damageInfo : __instance.damage) {
                if (damageInfo != null && damageInfo.base > 0) {
                    damageInfo.base += 3;
                }
            }
            logger.info("Ascension 23: Taskmaster damage +3");
        }
    }

    /**
     * Taskmaster getMove() fix: Use actual damage.get(1).base instead of hardcoded 7
     */
    @SpirePatch(
        clz = Taskmaster.class,
        method = "getMove"
    )
    public static class TaskmasterGetMoveFix {
        @SpirePostfixPatch
        public static void Postfix(Taskmaster __instance, int num) {
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 23) {
                return;
            }

            // Taskmaster always uses move byte 2 (Scouring Whip) with damage.get(1)
            // But getMove() hardcodes damage value as 7
            // We need to override it with the actual modified damage value
            int actualDamage = __instance.damage.get(1).base;  // This is 10 after our +3 patch
            __instance.setMove((byte)2, AbstractMonster.Intent.ATTACK_DEBUFF, actualDamage);

            logger.info(String.format("Ascension 23: Taskmaster getMove fixed to use damage %d", actualDamage));
        }
    }

    /**
     * Giant Head: +10 damage (It Is Time attack only)
     *
     * Pattern bytes:
     * - GLARE = 1 (debuff only)
     * - IT_IS_TIME = 2 (uses damage[1~7])
     * - COUNT = 3 (uses damage[0])
     *
     * damage array:
     * - damage[0] = 13 (COUNT attack) - NOT modified
     * - damage[1~7] = startingDeathDmg + (0~30) (IT IS TIME) - +10 to all
     */
    @SpirePatch(
        clz = GiantHead.class,
        method = SpirePatch.CONSTRUCTOR,
        paramtypez = {}
    )
    public static class GiantHeadDamagePatch {
        @SpirePostfixPatch
        public static void Postfix(GiantHead __instance) {
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 23) {
                return;
            }

            // Only modify IT IS TIME attacks (damage indices 1~7)
            // Skip damage[0] which is COUNT attack
            for (int i = 1; i < __instance.damage.size(); i++) {
                DamageInfo damageInfo = __instance.damage.get(i);
                if (damageInfo != null && damageInfo.base > 0) {
                    damageInfo.base += 10;
                }
            }
            logger.info("Ascension 23: Giant Head IT IS TIME damage +10 (damage[1-7])");
        }
    }

    /**
     * Giant Head getMove() fix: IT IS TIME damage display
     *
     * Original getMove() line 161:
     * setMove((byte)2, Intent.ATTACK, startingDeathDmg - count * 5)
     *
     * This hardcodes the damage calculation instead of using damage array.
     * We intercept after getMove() and recalculate using modified damage[index].
     */
    @SpirePatch(
        clz = GiantHead.class,
        method = "getMove"
    )
    public static class GiantHeadGetMoveFix {
        @SpirePostfixPatch
        public static void Postfix(GiantHead __instance, int num) {
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 23) {
                return;
            }

            try {
                // Check if IT IS TIME move was set
                java.lang.reflect.Field nextMoveField = AbstractMonster.class.getDeclaredField("nextMove");
                nextMoveField.setAccessible(true);
                byte nextMove = nextMoveField.getByte(__instance);

                if (nextMove == 2) { // IT_IS_TIME
                    // Get count to calculate damage index
                    java.lang.reflect.Field countField = GiantHead.class.getDeclaredField("count");
                    countField.setAccessible(true);
                    int count = countField.getInt(__instance);

                    // takeTurn() uses: index = 1 - count
                    int index = 1 - count;
                    if (index > 7) {
                        index = 7;
                    }

                    // Use modified damage array value
                    int actualDamage = __instance.damage.get(index).base;
                    __instance.setMove((byte)2, AbstractMonster.Intent.ATTACK, actualDamage);

                    logger.info(String.format("Ascension 23: Giant Head IT IS TIME Intent updated to %d (count=%d, index=%d)",
                        actualDamage, count, index));
                }
            } catch (Exception e) {
                logger.error("Failed to fix Giant Head IT IS TIME Intent", e);
            }
        }
    }

    /**
     * Nemesis: +5 damage
     */
    @SpirePatch(
        clz = Nemesis.class,
        method = SpirePatch.CONSTRUCTOR,
        paramtypez = {}
    )
    public static class NemesisDamagePatch {
        @SpirePostfixPatch
        public static void Postfix(Nemesis __instance) {
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 23) {
                return;
            }

            for (DamageInfo damageInfo : __instance.damage) {
                if (damageInfo != null && damageInfo.base > 0) {
                    damageInfo.base += 5;
                }
            }

            // Also update fireDmg field (used in Burns attack - move 2)
            try {
                java.lang.reflect.Field fireDmgField = Nemesis.class.getDeclaredField("fireDmg");
                fireDmgField.setAccessible(true);
                int currentFireDmg = fireDmgField.getInt(__instance);
                fireDmgField.setInt(__instance, currentFireDmg + 5);
                logger.info(String.format("Ascension 23: Nemesis damage +5 (Scythe: %d, Burns: %d)",
                    __instance.damage.get(0).base, currentFireDmg + 5));
            } catch (Exception e) {
                logger.error("Failed to modify Nemesis fireDmg field", e);
            }
        }
    }

    /**
     * Nemesis getMove() fix: Use actual damage values instead of hardcoded values
     * - Move 3 (Scythe): hardcoded 45 → use damage.get(0).base
     * - Move 2 (Burns): uses fireDmg field → already fixed in constructor
     */
    @SpirePatch(
        clz = Nemesis.class,
        method = "getMove"
    )
    public static class NemesisGetMoveFix {
        @SpirePostfixPatch
        public static void Postfix(Nemesis __instance, int num) {
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 23) {
                return;
            }

            // Nemesis uses move byte 3 (Scythe) with damage.get(0)
            // But getMove() hardcodes damage value as 45 (lines 168, 193, 205)
            // We need to override it with the actual modified damage value
            try {
                java.lang.reflect.Field nextMoveField = AbstractMonster.class.getDeclaredField("nextMove");
                nextMoveField.setAccessible(true);
                byte nextMove = nextMoveField.getByte(__instance);

                if (nextMove == 3) { // Scythe attack
                    int actualDamage = __instance.damage.get(0).base;  // This is 50 after our +5 patch
                    __instance.setMove((byte)3, AbstractMonster.Intent.ATTACK, actualDamage);
                    logger.info(String.format("Ascension 23: Nemesis Scythe getMove fixed to use damage %d", actualDamage));
                }
                // Move 2 (Burns) uses fireDmg field which is already fixed in constructor, no need to fix here
            } catch (Exception e) {
                logger.error("Failed to fix Nemesis getMove", e);
            }
        }
    }

    /**
     * Reptomancer: +2 damage
     */
    @SpirePatch(
        clz = Reptomancer.class,
        method = SpirePatch.CONSTRUCTOR,
        paramtypez = {}
    )
    public static class ReptomancerDamagePatch {
        @SpirePostfixPatch
        public static void Postfix(Reptomancer __instance) {
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 23) {
                return;
            }

            for (DamageInfo damageInfo : __instance.damage) {
                if (damageInfo != null && damageInfo.base > 0) {
                    damageInfo.base += 2;
                }
            }
            logger.info("Ascension 23: Reptomancer damage +2");
        }
    }

    /**
     * Gremlin Leader: Encourage pattern grants +1 additional Strength
     */
    @SpirePatch(
        clz = GremlinLeader.class,
        method = "takeTurn"
    )
    public static class GremlinLeaderEncourageBuff {
        @SpirePostfixPatch
        public static void Postfix(GremlinLeader __instance) {
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 23) {
                return;
            }

            // Check if Gremlin Leader just used Encourage move (byte 3)
            try {
                java.lang.reflect.Field nextMoveField = AbstractMonster.class.getDeclaredField("nextMove");
                nextMoveField.setAccessible(true);
                byte nextMove = nextMoveField.getByte(__instance);

                if (nextMove == 3) { // Encourage move
                    // Apply +1 Strength to all non-dead minions
                    for (AbstractMonster m : AbstractDungeon.getMonsters().monsters) {
                        if (m != __instance && !m.isDying && !m.isDead) {
                            AbstractDungeon.actionManager.addToBottom(
                                new ApplyPowerAction(m, __instance,
                                    new StrengthPower(m, 1), 1)
                            );
                        }
                    }

                    logger.info("Ascension 23: Gremlin Leader Encourage +1 Strength to minions");
                }
            } catch (Exception e) {
                logger.error("Failed to modify Gremlin Leader Encourage pattern", e);
            }
        }
    }
}
