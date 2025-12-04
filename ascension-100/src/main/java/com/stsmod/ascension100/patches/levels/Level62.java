package com.stsmod.ascension100.patches.levels;

import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePrefixPatch;
import com.megacrit.cardcrawl.cards.DamageInfo;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import com.megacrit.cardcrawl.monsters.city.Byrd;
import com.megacrit.cardcrawl.monsters.exordium.SlaverRed;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;

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
        method = "init"
    )
    public static class NormalEnemiesDamageIncrease {
        // Track which monsters have already been patched to prevent duplicate application
        private static final Set<AbstractMonster> patchedMonsters = Collections.newSetFromMap(new WeakHashMap<>());

        @SpirePrefixPatch
        public static void Prefix(AbstractMonster __instance) {
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 62) {
                return;
            }

            // Skip bosses, elites, and Byrd (handled separately)
            if (__instance.type == AbstractMonster.EnemyType.BOSS ||
                __instance.type == AbstractMonster.EnemyType.ELITE ||
                __instance instanceof Byrd) {
                return;
            }

            // Check if already patched
            if (patchedMonsters.contains(__instance)) {
                logger.warn(String.format(
                    "Ascension 62: Skipping duplicate damage increase for %s (already patched)",
                    __instance.name
                ));
                return;
            }

            int damageIncrease = 1;

            for (DamageInfo damageInfo : __instance.damage) {
                if (damageInfo != null && damageInfo.base > 0) {
                    int originalDamage = damageInfo.base;
                    damageInfo.base += damageIncrease;
                    damageInfo.output = damageInfo.base;  // Update output to match base

                    logger.info(String.format(
                        "Ascension 62: %s (%s) damage increased from %d to %d",
                        __instance.name, __instance.type, originalDamage, damageInfo.base
                    ));
                }
            }

            // Special handling for SlaverRed: update stabDmg field for first turn Intent
            if (__instance instanceof SlaverRed) {
                try {
                    Field stabDmgField = SlaverRed.class.getDeclaredField("stabDmg");
                    stabDmgField.setAccessible(true);
                    int currentStabDmg = stabDmgField.getInt(__instance);
                    stabDmgField.setInt(__instance, currentStabDmg + damageIncrease);
                    logger.info(String.format(
                        "Ascension 62: SlaverRed stabDmg field updated from %d to %d",
                        currentStabDmg, currentStabDmg + damageIncrease
                    ));
                } catch (Exception e) {
                    logger.error("Failed to update SlaverRed stabDmg field", e);
                }
            }

            // Mark as patched
            patchedMonsters.add(__instance);
        }
    }

    /**
     * Byrd special handling: Flight +1
     * Increases the flightAmt field so it applies to both initial flight and re-flight (GO_AIRBORNE move)
     */
    @SpirePatch(
        clz = Byrd.class,
        method = SpirePatch.CONSTRUCTOR
    )
    public static class ByrdFlightIncrease {
        @SpirePostfixPatch
        public static void Postfix(Byrd __instance) {
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 62) {
                return;
            }

            try {
                // Access flightAmt field via reflection
                Field flightAmtField = Byrd.class.getDeclaredField("flightAmt");
                flightAmtField.setAccessible(true);

                int originalFlightAmt = flightAmtField.getInt(__instance);
                int newFlightAmt = originalFlightAmt + 1;
                flightAmtField.setInt(__instance, newFlightAmt);

                logger.info(String.format(
                    "Ascension 62: Byrd flightAmt increased from %d to %d (+1)",
                    originalFlightAmt, newFlightAmt
                ));
            } catch (NoSuchFieldException | IllegalAccessException e) {
                logger.error("Ascension 62: Failed to modify Byrd flightAmt", e);
            }
        }
    }
}
