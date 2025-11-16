package com.stsmod.ascension100.patches.levels;

import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import com.megacrit.cardcrawl.monsters.ending.CorruptHeart;
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
 * Ascension Level 36: Boss stats adjusted
 *
 * 보스의 능력치가 조정됩니다.
 * 대왕 슬라임(Slime Boss): 체력 +25, 공격력 -10
 * 수호자(The Guardian): 체력 +20, 공격력 -1
 * 육각령(Hexaghost): 체력 +40
 * 청동 자동인형(Bronze Automaton): 체력 +25
 * 투사(The Champ): 공격력 +3
 * 수집가(The Collector): 체력 +60, 공격력 -2
 * 깨어난 자(Awakened One): 체력 +30
 * 시간 포식자(Time Eater): 체력 +60, 공격력 -2
 * 도누와 데카(Donu and Deca): 도누 체력 +50, 데카 공격력 +2
 */
public class Level36 {
    private static final Logger logger = LogManager.getLogger(Level36.class.getName());

    @SpirePatch(
        clz = AbstractMonster.class,
        method = "init"
    )
    public static class BossHealthAdjustment {
        @SpirePostfixPatch
        public static void Postfix(AbstractMonster __instance) {
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 36) {
                return;
            }

            // Only apply to Act 1-3 bosses, exclude Corrupt Heart
            if (__instance.type != AbstractMonster.EnemyType.BOSS ||
                __instance instanceof CorruptHeart) {
                return;
            }

            int hpChange = 0;

            // Determine HP change by boss
            if (__instance instanceof SlimeBoss) {
                hpChange = 25;
            } else if (__instance instanceof TheGuardian) {
                hpChange = 20;
            } else if (__instance instanceof Hexaghost) {
                hpChange = 40;
            } else if (__instance instanceof BronzeAutomaton) {
                hpChange = 25;
            } else if (__instance instanceof TheCollector) {
                hpChange = 60;
            } else if (__instance instanceof AwakenedOne) {
                hpChange = 30;
            } else if (__instance instanceof TimeEater) {
                hpChange = 60;
            } else if (__instance instanceof Donu) {
                hpChange = 50;
            }

            if (hpChange > 0) {
                int originalMaxHP = __instance.maxHealth;
                __instance.maxHealth += hpChange;
                __instance.currentHealth += hpChange;

                logger.info(String.format(
                    "Ascension 36: Boss %s HP increased from %d to %d (+%d)",
                    __instance.name, originalMaxHP, __instance.maxHealth, hpChange
                ));
            }
        }
    }

    @SpirePatch(
        clz = AbstractMonster.class,
        method = "usePreBattleAction"
    )
    public static class BossDamageAdjustment {
        @SpirePostfixPatch
        public static void Postfix(AbstractMonster __instance) {
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 36) {
                return;
            }

            // Only apply to Act 1-3 bosses, exclude Corrupt Heart
            if (__instance.type != AbstractMonster.EnemyType.BOSS ||
                __instance instanceof CorruptHeart) {
                return;
            }

            int damageChange = 0;

            // Determine damage change by boss
            if (__instance instanceof SlimeBoss) {
                damageChange = -10;
            } else if (__instance instanceof TheGuardian) {
                damageChange = -1;
            } else if (__instance instanceof Champ) {
                damageChange = 3;
            } else if (__instance instanceof TheCollector) {
                damageChange = -2;
            } else if (__instance instanceof TimeEater) {
                damageChange = -2;
            } else if (__instance instanceof Deca) {
                damageChange = 2;
            }

            if (damageChange != 0) {
                for (int i = 0; i < __instance.damage.size(); i++) {
                    if (__instance.damage.get(i) != null && __instance.damage.get(i).base > 0) {
                        __instance.damage.get(i).base += damageChange;
                        // Prevent negative damage
                        if (__instance.damage.get(i).base < 0) {
                            __instance.damage.get(i).base = 0;
                        }
                    }
                }

                logger.info(String.format(
                    "Ascension 36: Boss %s damage adjusted by %d",
                    __instance.name, damageChange
                ));
            }
        }
    }
}
