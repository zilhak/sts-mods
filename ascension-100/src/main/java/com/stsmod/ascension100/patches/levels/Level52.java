package com.stsmod.ascension100.patches.levels;

import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePrefixPatch;
import com.megacrit.cardcrawl.cards.DamageInfo;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import com.megacrit.cardcrawl.monsters.city.Byrd;
import com.stsmod.ascension100.util.EncounterHelper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Ascension Level 52: Enemy damage increased by act and encounter type
 *
 * 적들의 공격이 강화됩니다. (섀 제외)
 * 1막의 strong enemies 전투에서 적들의 공격력이 1 증가합니다.
 * 2막의 weak enemies 전투에서 적들의 공격력이 1 증가합니다. strong enemies 전투에서 적들의 공격력이 2 증가합니다.
 * 3막의 weak enemies 전투에서 적들의 공격력이 1 증가합니다. strong enemies 전투에서 적들의 공격력이 5 증가합니다.
 */
public class Level52 {
    private static final Logger logger = LogManager.getLogger(Level52.class.getName());

    @SpirePatch(
        clz = AbstractMonster.class,
        method = "init"
    )
    public static class EnemiesDamageByActIncrease {
        @SpirePrefixPatch
        public static void Prefix(AbstractMonster __instance) {
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 52) {
                return;
            }

            // Skip bosses and elites
            if (__instance.type == AbstractMonster.EnemyType.BOSS ||
                __instance.type == AbstractMonster.EnemyType.ELITE) {
                return;
            }

            // Skip Byrd (섀) - should not get basic damage increases
            if (__instance instanceof Byrd) {
                return;
            }

            int actNum = AbstractDungeon.actNum;
            int damageIncrease = 0;

            boolean isWeak = EncounterHelper.isWeakEncounter();
            boolean isStrong = EncounterHelper.isStrongEncounter();

            if (actNum == 1) {
                // Act 1: Only strong enemies get +1
                if (isStrong) {
                    damageIncrease = 1;
                }
            } else if (actNum == 2) {
                // Act 2: Weak +1, Strong +2
                if (isWeak) {
                    damageIncrease = 1;
                } else if (isStrong) {
                    damageIncrease = 2;
                }
            } else if (actNum >= 3) {
                // Act 3: Weak +1, Strong +5
                if (isWeak) {
                    damageIncrease = 1;
                } else if (isStrong) {
                    damageIncrease = 5;
                }
            }

            if (damageIncrease > 0) {
                for (DamageInfo damageInfo : __instance.damage) {
                    if (damageInfo != null && damageInfo.base > 0) {
                        damageInfo.base += damageIncrease;
                        damageInfo.output = damageInfo.base;  // Update output to match base
                    }
                }

                logger.info(String.format(
                    "Ascension 52: %s (Act %d, %s) damage increased by %d",
                    __instance.name, actNum, EncounterHelper.getEncounterType(), damageIncrease
                ));
            }
        }
    }
}
