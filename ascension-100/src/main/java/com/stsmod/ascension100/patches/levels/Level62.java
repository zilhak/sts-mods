package com.stsmod.ascension100.patches.levels;

import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;
import com.megacrit.cardcrawl.cards.DamageInfo;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import com.megacrit.cardcrawl.monsters.city.Byrd;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.reflect.Field;

/**
 * Ascension Level 62: Enemies deal more damage (with Byrd special handling)
 *
 * 적들의 공격이 강화됩니다.
 * 일반 적들의 공격력이 1 증가합니다 (섀(byrd) 제외)
 * byrd는 대신, 비행 수치가 1 증가합니다.
 */
public class Level62 {
    private static final Logger logger = LogManager.getLogger(Level62.class.getName());

    /**
     * Increase normal enemies' damage by +1, skip Byrd
     */
    @SpirePatch(
        clz = AbstractMonster.class,
        method = "usePreBattleAction"
    )
    public static class NormalEnemiesDamageIncrease {
        @SpirePostfixPatch
        public static void Postfix(AbstractMonster __instance) {
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 62) {
                return;
            }

            // Skip bosses, elites, and Byrd (handled separately)
            if (__instance.type == AbstractMonster.EnemyType.BOSS ||
                __instance.type == AbstractMonster.EnemyType.ELITE ||
                __instance instanceof Byrd) {
                return;
            }

            int damageIncrease = 1;

            for (DamageInfo damageInfo : __instance.damage) {
                if (damageInfo != null && damageInfo.base > 0) {
                    damageInfo.base += damageIncrease;
                }
            }

            logger.info(String.format(
                "Ascension 62: %s (%s) damage increased by %d",
                __instance.name, __instance.type, damageIncrease
            ));
        }
    }

    /**
     * Byrd special handling: Flight +1
     */
    @SpirePatch(
        clz = Byrd.class,
        method = "usePreBattleAction"
    )
    public static class ByrdFlightIncrease {
        @SpirePostfixPatch
        public static void Postfix(Byrd __instance) {
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 62) {
                return;
            }

            // Increase existing Flight power by 1
            com.megacrit.cardcrawl.powers.AbstractPower flightPower = __instance.getPower("Flight");
            if (flightPower != null) {
                int originalAmount = flightPower.amount;
                flightPower.amount += 1;
                flightPower.updateDescription();

                logger.info(String.format(
                    "Ascension 62: Byrd Flight increased from %d to %d (+1)",
                    originalAmount, flightPower.amount
                ));
            }
        }
    }
}
