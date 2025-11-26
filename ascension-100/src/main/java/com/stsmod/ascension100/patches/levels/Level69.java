package com.stsmod.ascension100.patches.levels;

import com.badlogic.gdx.math.MathUtils;
import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;
import com.megacrit.cardcrawl.cards.DamageInfo;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Ascension Level 69: Bosses enhanced by act
 *
 * 보스가 강화됩니다.
 * 1막의 보스의 체력이 5% 증가하고, 공격력이 1 증가합니다. (수호자 제외)
 * 2막의 보스의 체력이 10% 증가하고, 공격력이 3 증가합니다.
 * 3막의 보스의 체력이 15% 증가하고, 공격력이 6 증가합니다.
 * (타락한 심장 제외)
 */
public class Level69 {
    private static final Logger logger = LogManager.getLogger(Level69.class.getName());

    @SpirePatch(
        clz = AbstractMonster.class,
        method = "init"
    )
    public static class BossHPByActIncrease {
        @SpirePostfixPatch
        public static void Postfix(AbstractMonster __instance) {
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 69) {
                return;
            }

            if (__instance.type == AbstractMonster.EnemyType.BOSS) {
                int actNum = AbstractDungeon.actNum;
                float hpMultiplier = 1.0f;

                // Exclude Guardian from Act 1 buffs
                if (actNum == 1 && __instance.id.equals("TheGuardian")) {
                    logger.info(String.format(
                        "Ascension 69: Guardian excluded from Act 1 HP buff"
                    ));
                    return;
                }

                if (actNum == 1) {
                    hpMultiplier = 1.05f;
                } else if (actNum == 2) {
                    hpMultiplier = 1.10f;
                } else if (actNum == 3) {
                    // Only Act 3, not Act 4 (Corrupt Heart)
                    hpMultiplier = 1.15f;
                }

                if (hpMultiplier > 1.0f) {
                    int originalMaxHP = __instance.maxHealth;
                    __instance.maxHealth = MathUtils.ceil(__instance.maxHealth * hpMultiplier);
                    __instance.currentHealth = MathUtils.ceil(__instance.currentHealth * hpMultiplier);

                    logger.info(String.format(
                        "Ascension 69: Boss %s HP increased from %d to %d (Act %d)",
                        __instance.name, originalMaxHP, __instance.maxHealth, actNum
                    ));
                }
            }
        }
    }

    @SpirePatch(
        clz = AbstractMonster.class,
        method = "usePreBattleAction"
    )
    public static class BossDamageByActIncrease {
        @SpirePostfixPatch
        public static void Postfix(AbstractMonster __instance) {
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 69) {
                return;
            }

            if (__instance.type == AbstractMonster.EnemyType.BOSS) {
                int actNum = AbstractDungeon.actNum;
                int damageIncrease = 0;

                // Exclude Guardian from Act 1 buffs
                if (actNum == 1 && __instance.id.equals("TheGuardian")) {
                    logger.info(String.format(
                        "Ascension 69: Guardian excluded from Act 1 damage buff"
                    ));
                    return;
                }

                if (actNum == 1) {
                    damageIncrease = 1;
                } else if (actNum == 2) {
                    damageIncrease = 3;
                } else if (actNum == 3) {
                    // Only Act 3, not Act 4 (Corrupt Heart)
                    damageIncrease = 6;
                }

                if (damageIncrease > 0) {
                    for (DamageInfo damageInfo : __instance.damage) {
                        if (damageInfo != null && damageInfo.base > 0) {
                            damageInfo.base += damageIncrease;
                        }
                    }

                    logger.info(String.format(
                        "Ascension 69: Boss %s damage increased by %d (Act %d)",
                        __instance.name, damageIncrease, actNum
                    ));
                }
            }
        }
    }
}
