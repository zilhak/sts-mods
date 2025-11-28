package com.stsmod.ascension100.patches.levels;

import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;
import com.megacrit.cardcrawl.actions.common.ApplyPowerAction;
import com.megacrit.cardcrawl.cards.DamageInfo;
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
import com.megacrit.cardcrawl.powers.StrengthPower;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Ascension Level 35: Elite enemies attack enhancements
 *
 * 엘리트 적들의 공격이 강화됩니다.
 *
 * 귀족 그렘린(Gremlin Nob): 공격력 +1
 * 라가불린(Lagavulin): 공격력 +1
 * 보초기(Sentry): 공격력 +1
 * 칼부림의 책(Book of Stabbing): 공격력 +1
 * 그렘린 리더(Gremlin Leader): 격려 패턴 힘 +1
 * 노예 관리자(Taskmaster): 공격력 +2
 * 거인의 머리(Giant Head): It Is Time 데미지 +5
 * 네메시스(Nemesis): 공격력 +2
 * 파충류 주술사(Reptomancer): 공격력 +2
 */
public class Level35 {
    private static final Logger logger = LogManager.getLogger(Level35.class.getName());

    /**
     * Gremlin Nob: +1 damage to all attacks
     */
    @SpirePatch(
        clz = GremlinNob.class,
        paramtypez = {float.class, float.class},
        method = SpirePatch.CONSTRUCTOR
    )
    public static class GremlinNobDamagePatch {
        @SpirePostfixPatch
        public static void Postfix(GremlinNob __instance) {
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 35) {
                return;
            }

            for (DamageInfo damageInfo : __instance.damage) {
                if (damageInfo != null && damageInfo.base > 0) {
                    damageInfo.base += 1;
                }
            }
            logger.info("Ascension 35: Gremlin Nob damage +1");
        }
    }

    /**
     * Lagavulin: +1 damage
     */
    @SpirePatch(
        clz = Lagavulin.class,
        paramtypez = {boolean.class},
        method = SpirePatch.CONSTRUCTOR
    )
    public static class LagavulinDamagePatch {
        @SpirePostfixPatch
        public static void Postfix(Lagavulin __instance) {
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 35) {
                return;
            }

            for (DamageInfo damageInfo : __instance.damage) {
                if (damageInfo != null && damageInfo.base > 0) {
                    damageInfo.base += 1;
                }
            }
            logger.info("Ascension 35: Lagavulin damage +1");
        }
    }

    /**
     * Sentry: +1 damage
     */
    @SpirePatch(
        clz = Sentry.class,
        paramtypez = {float.class, float.class},
        method = SpirePatch.CONSTRUCTOR
    )
    public static class SentryDamagePatch {
        @SpirePostfixPatch
        public static void Postfix(Sentry __instance) {
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 35) {
                return;
            }

            for (DamageInfo damageInfo : __instance.damage) {
                if (damageInfo != null && damageInfo.base > 0) {
                    damageInfo.base += 1;
                }
            }
            logger.info("Ascension 35: Sentry damage +1");
        }
    }

    /**
     * Book of Stabbing: +1 damage
     */
    @SpirePatch(
        clz = BookOfStabbing.class,
        paramtypez = {},
        method = SpirePatch.CONSTRUCTOR
    )
    public static class BookOfStabbingDamagePatch {
        @SpirePostfixPatch
        public static void Postfix(BookOfStabbing __instance) {
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 35) {
                return;
            }

            for (DamageInfo damageInfo : __instance.damage) {
                if (damageInfo != null && damageInfo.base > 0) {
                    damageInfo.base += 1;
                }
            }
            logger.info("Ascension 35: Book of Stabbing damage +1");
        }
    }

    /**
     * Taskmaster: +2 damage
     */
    @SpirePatch(
        clz = Taskmaster.class,
        paramtypez = {float.class, float.class},
        method = SpirePatch.CONSTRUCTOR
    )
    public static class TaskmasterDamagePatch {
        @SpirePostfixPatch
        public static void Postfix(Taskmaster __instance) {
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 35) {
                return;
            }

            for (DamageInfo damageInfo : __instance.damage) {
                if (damageInfo != null && damageInfo.base > 0) {
                    damageInfo.base += 2;
                }
            }
            logger.info("Ascension 35: Taskmaster damage +2");
        }
    }

    /**
     * Giant Head: +5 damage (It Is Time attack)
     */
    @SpirePatch(
        clz = GiantHead.class,
        paramtypez = {},
        method = SpirePatch.CONSTRUCTOR
    )
    public static class GiantHeadDamagePatch {
        @SpirePostfixPatch
        public static void Postfix(GiantHead __instance) {
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 35) {
                return;
            }

            for (DamageInfo damageInfo : __instance.damage) {
                if (damageInfo != null && damageInfo.base > 0) {
                    damageInfo.base += 5;
                }
            }
            logger.info("Ascension 35: Giant Head damage +5");
        }
    }

    /**
     * Nemesis: +2 damage
     */
    @SpirePatch(
        clz = Nemesis.class,
        paramtypez = {},
        method = SpirePatch.CONSTRUCTOR
    )
    public static class NemesisDamagePatch {
        @SpirePostfixPatch
        public static void Postfix(Nemesis __instance) {
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 35) {
                return;
            }

            for (DamageInfo damageInfo : __instance.damage) {
                if (damageInfo != null && damageInfo.base > 0) {
                    damageInfo.base += 2;
                }
            }

            // Also update fireDmg field (used in Burns attack - move 2)
            try {
                java.lang.reflect.Field fireDmgField = Nemesis.class.getDeclaredField("fireDmg");
                fireDmgField.setAccessible(true);
                int currentFireDmg = fireDmgField.getInt(__instance);
                fireDmgField.setInt(__instance, currentFireDmg + 2);
                logger.info(String.format("Ascension 35: Nemesis damage +2 (Scythe: %d, Burns: %d)",
                    __instance.damage.get(0).base, currentFireDmg + 2));
            } catch (Exception e) {
                logger.error("Failed to modify Nemesis fireDmg field", e);
            }
        }
    }

    /**
     * Reptomancer: +2 damage
     */
    @SpirePatch(
        clz = Reptomancer.class,
        paramtypez = {},
        method = SpirePatch.CONSTRUCTOR
    )
    public static class ReptomancerDamagePatch {
        @SpirePostfixPatch
        public static void Postfix(Reptomancer __instance) {
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 35) {
                return;
            }

            for (DamageInfo damageInfo : __instance.damage) {
                if (damageInfo != null && damageInfo.base > 0) {
                    damageInfo.base += 2;
                }
            }
            logger.info("Ascension 35: Reptomancer damage +2");
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
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 35) {
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

                    logger.info("Ascension 35: Gremlin Leader Encourage +1 Strength to minions");
                }
            } catch (Exception e) {
                logger.error("Failed to modify Gremlin Leader Encourage pattern", e);
            }
        }
    }
}
