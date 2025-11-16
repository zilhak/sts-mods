package com.stsmod.ascension100.patches.levels;

import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;
import com.megacrit.cardcrawl.actions.common.ApplyPowerAction;
import com.megacrit.cardcrawl.cards.DamageInfo;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
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
 * 칼부림의 책(Book of Stabbing): 공격력 +1
 * 그렘린 리더(Gremlin Leader): 격려 패턴 힘 +1
 * 노예 관리자(Taskmaster): 공격력 +3
 * 거인의 머리(Giant Head): It Is Time 데미지 +10
 * 네메시스(Nemesis): 공격력 +5
 * 파충류 주술사(Reptomancer): 공격력 +2
 */
public class Level23 {
    private static final Logger logger = LogManager.getLogger(Level23.class.getName());

    /**
     * Elite damage increases by monster ID
     */
    @SpirePatch(
        clz = AbstractMonster.class,
        method = "usePreBattleAction"
    )
    public static class EliteDamageIncrease {
        @SpirePostfixPatch
        public static void Postfix(AbstractMonster __instance) {
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 23) {
                return;
            }

            if (__instance.type != AbstractMonster.EnemyType.ELITE) {
                return;
            }

            String id = __instance.id;
            if (id == null) return;

            int damageIncrease = 0;

            // Determine damage increase by elite ID
            switch (id) {
                case "GremlinNob":      // 귀족 그렘린
                    damageIncrease = 1;
                    break;
                case "Lagavulin":       // 라가불린
                    damageIncrease = 2;
                    break;
                case "Sentry":          // 보초기
                    damageIncrease = 1;
                    break;
                case "BookOfStabbing":  // 칼부림의 책
                    damageIncrease = 1;
                    break;
                case "SlaverBoss":      // 노예 관리자 (Taskmaster)
                    damageIncrease = 3;
                    break;
                case "GiantHead":       // 거인의 머리
                    damageIncrease = 10;
                    break;
                case "Nemesis":         // 네메시스
                    damageIncrease = 5;
                    break;
                case "Reptomancer":     // 파충류 주술사
                    damageIncrease = 2;
                    break;
                default:
                    return; // No damage increase for this elite
            }

            // Apply damage increase to all damage values
            for (DamageInfo damageInfo : __instance.damage) {
                if (damageInfo != null && damageInfo.base > 0) {
                    damageInfo.base += damageIncrease;
                }
            }

            logger.info(String.format(
                "Ascension 23: Elite %s damage increased by %d",
                __instance.name, damageIncrease
            ));
        }
    }

    /**
     * Gremlin Leader: Encourage pattern grants +1 additional Strength
     */
    @SpirePatch(
        clz = com.megacrit.cardcrawl.monsters.city.GremlinLeader.class,
        method = "takeTurn"
    )
    public static class GremlinLeaderEncourageBuff {
        @SpirePostfixPatch
        public static void Postfix(com.megacrit.cardcrawl.monsters.city.GremlinLeader __instance) {
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

                    logger.info("Ascension 23: Gremlin Leader Encourage gave +1 additional Strength to minions");
                }
            } catch (Exception e) {
                logger.error("Failed to modify Gremlin Leader Encourage pattern", e);
            }
        }
    }
}
