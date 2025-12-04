package com.stsmod.ascension100.patches.levels;

import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePrefixPatch;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import com.megacrit.cardcrawl.monsters.city.Byrd;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.reflect.Field;

/**
 * Ascension Level 36: Normal enemies deal +1 damage (except Byrd)
 *
 * 일반 적들의 공격력이 1 증가합니다 (섀(Byrd) 제외)
 * Byrd는 대신, Headbutt 패턴에서 공격력이 2 증가합니다.
 */
public class Level36 {
    private static final Logger logger = LogManager.getLogger(Level36.class.getName());

    /**
     * Normal enemies (except Byrd) get +1 damage
     */
    @SpirePatch(
        clz = AbstractMonster.class,
        method = "init"
    )
    public static class NormalDamageIncrease {
        @SpirePrefixPatch
        public static void Prefix(AbstractMonster __instance) {
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 36) {
                return;
            }

            // Exclude Byrd - it has special handling
            if (__instance.type == AbstractMonster.EnemyType.NORMAL && !(__instance instanceof Byrd)) {
                int damageIncrease = 1;

                for (int i = 0; i < __instance.damage.size(); i++) {
                    if (__instance.damage.get(i) != null && __instance.damage.get(i).base > 0) {
                        __instance.damage.get(i).base += damageIncrease;
                        __instance.damage.get(i).output = __instance.damage.get(i).base;  // Update output to match base
                    }
                }

                logger.info(String.format(
                    "Ascension 36: Normal %s damage increased by %d",
                    __instance.name, damageIncrease
                ));
            }
        }
    }

    /**
     * Byrd: Headbutt pattern deals +2 damage
     * HEADBUTT uses damage.get(2) - fixed damage of 3
     * We modify it in Constructor to avoid stacking issues
     */
    @SpirePatch(
        clz = Byrd.class,
        method = SpirePatch.CONSTRUCTOR,
        paramtypez = {float.class, float.class}
    )
    public static class ByrdHeadbuttEnhancement {
        @SpirePostfixPatch
        public static void Postfix(Byrd __instance, float x, float y) {
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 36) {
                return;
            }

            // Byrd damage array structure:
            // damage.get(0) = PECK (1 damage, multi-hit)
            // damage.get(1) = SWOOP (12/14 damage)
            // damage.get(2) = HEADBUTT (3 damage) ← This one!

            if (__instance.damage.size() > 2 && __instance.damage.get(2) != null) {
                int originalDamage = __instance.damage.get(2).base;
                __instance.damage.get(2).base += 2;

                logger.info(String.format(
                    "Ascension 36: Byrd Headbutt damage increased from %d to %d",
                    originalDamage, __instance.damage.get(2).base
                ));
            }
        }
    }
}
