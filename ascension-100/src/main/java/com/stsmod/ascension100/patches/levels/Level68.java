package com.stsmod.ascension100.patches.levels;

import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePrefixPatch;
import com.megacrit.cardcrawl.cards.DamageInfo;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import com.megacrit.cardcrawl.monsters.beyond.GiantHead;
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

            // Skip GiantHead (handled by separate Constructor patch for early timing)
            if (__instance instanceof GiantHead) {
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

    /**
     * GiantHead: Special handling in Constructor to ensure damage modification happens before first getMove()
     * GiantHead's first getMove() is called very early, so we need Constructor-level patching
     */
    @SpirePatch(
        clz = GiantHead.class,
        method = SpirePatch.CONSTRUCTOR
    )
    public static class GiantHeadConstructorPatch {
        @SpirePostfixPatch
        public static void Postfix(GiantHead __instance) {
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 68) {
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
                // Update all damage array entries (including COUNT at index 0)
                for (DamageInfo damageInfo : __instance.damage) {
                    if (damageInfo != null && damageInfo.base > 0) {
                        damageInfo.base += damageIncrease;
                        damageInfo.output = damageInfo.base;
                    }
                }

                // Update startingDeathDmg field for IT_IS_TIME Intent calculation
                try {
                    Field startingDeathDmgField = GiantHead.class.getDeclaredField("startingDeathDmg");
                    startingDeathDmgField.setAccessible(true);
                    int currentDeathDmg = startingDeathDmgField.getInt(__instance);
                    startingDeathDmgField.setInt(__instance, currentDeathDmg + damageIncrease);
                    logger.info(String.format(
                        "Ascension 68: GiantHead damage increased by %d (Act %d) - COUNT and IT_IS_TIME both updated",
                        damageIncrease, actNum
                    ));
                } catch (Exception e) {
                    logger.error("Failed to update GiantHead startingDeathDmg field", e);
                }
            }
        }
    }

    /**
     * GiantHead: Fix Count attack Intent in applyPowers()
     * applyPowers() recalculates Intent damage using this.move.baseDamage every turn,
     * so we must fix it after applyPowers() completes
     *
     * Patches AbstractMonster.applyPowers() since GiantHead doesn't override it
     */
    @SpirePatch(
        clz = AbstractMonster.class,
        method = "applyPowers"
    )
    public static class GiantHeadApplyPowersFix {
        @SpirePostfixPatch
        public static void Postfix(AbstractMonster __instance) {
            // Only apply to GiantHead
            if (!(__instance instanceof GiantHead)) {
                return;
            }

            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 68) {
                return;
            }

            try {
                // Check if current move is COUNT (byte 3)
                Field nextMoveField = AbstractMonster.class.getDeclaredField("nextMove");
                nextMoveField.setAccessible(true);
                byte nextMove = nextMoveField.getByte(__instance);

                if (nextMove == 3) {
                    // applyPowers() just recalculated Intent using this.move.baseDamage (13)
                    // We need to fix it to use damage.get(0).output instead
                    int actualDamage = __instance.damage.get(0).output;  // Use output after applyPowers

                    // Update intentBaseDmg field directly since calculateDamage was just called
                    Field intentBaseDmgField = AbstractMonster.class.getDeclaredField("intentBaseDmg");
                    intentBaseDmgField.setAccessible(true);
                    int currentIntentDmg = intentBaseDmgField.getInt(__instance);

                    if (currentIntentDmg != actualDamage) {
                        intentBaseDmgField.setInt(__instance, actualDamage);
                        logger.info(String.format(
                            "Ascension 68: GiantHead Count Intent fixed in applyPowers from %d to %d",
                            currentIntentDmg, actualDamage
                        ));
                    }
                }
            } catch (Exception e) {
                logger.error("Failed to fix GiantHead Count Intent in applyPowers", e);
            }
        }
    }
}
