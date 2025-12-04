package com.stsmod.ascension100.patches.levels;

import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePrefixPatch;
import com.megacrit.cardcrawl.cards.DamageInfo;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import com.megacrit.cardcrawl.monsters.exordium.SlaverRed;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * Ascension Level 68: Enemy damage increased by act
 *
 * 적들의 공격이 강화됩니다.
 * 1막의 적들의 공격이 1 증가합니다.
 * 2막의 적들의 공격이 2 증가합니다. (byrd 제외)
 * 3막의 적들의 공격이 5 증가합니다.
 */
public class Level68 {
    private static final Logger logger = LogManager.getLogger(Level68.class.getName());

    @SpirePatch(
        clz = AbstractMonster.class,
        method = "init"
    )
    public static class DamageByActIncrease {
        // Track which monsters have already been patched to prevent duplicate application
        private static final Set<AbstractMonster> patchedMonsters = Collections.newSetFromMap(new WeakHashMap<>());

        @SpirePrefixPatch
        public static void Prefix(AbstractMonster __instance) {
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 68) {
                return;
            }

            // Skip byrd in Act 2 (handled by special Level 62 logic)
            if (AbstractDungeon.actNum == 2 && __instance.id != null && __instance.id.equals("Byrd")) {
                return;
            }

            // Check if already patched
            if (patchedMonsters.contains(__instance)) {
                logger.warn(String.format(
                    "Ascension 68: Skipping duplicate damage increase for %s (already patched)",
                    __instance.name
                ));
                return;
            }

            int actNum = AbstractDungeon.actNum;
            int damageIncrease = 0;

            if (actNum == 1) {
                damageIncrease = 1;
            } else if (actNum == 2) {
                damageIncrease = 2;
            } else if (actNum >= 3) {
                damageIncrease = 5;
            }

            if (damageIncrease > 0) {
                for (DamageInfo damageInfo : __instance.damage) {
                    if (damageInfo != null && damageInfo.base > 0) {
                        int originalDamage = damageInfo.base;
                        damageInfo.base += damageIncrease;
                        damageInfo.output = damageInfo.base;  // Update output to match base

                        logger.info(String.format(
                            "Ascension 68: %s (%s) damage increased from %d to %d (Act %d)",
                            __instance.name, __instance.type, originalDamage, damageInfo.base, actNum
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
                            "Ascension 68: SlaverRed stabDmg field updated from %d to %d (Act %d)",
                            currentStabDmg, currentStabDmg + damageIncrease, actNum
                        ));
                    } catch (Exception e) {
                        logger.error("Failed to update SlaverRed stabDmg field", e);
                    }
                }

                // Mark as patched
                patchedMonsters.add(__instance);
            }
        }
    }
}
