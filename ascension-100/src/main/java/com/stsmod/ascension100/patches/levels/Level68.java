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

            // Skip bosses (Level 68 is for normal enemies only, not bosses)
            if (__instance.type == AbstractMonster.EnemyType.BOSS) {
                return;
            }

            // Skip elite minions (they should not get normal enemy bonuses)
            if (__instance.id != null && __instance.id.equals("Dagger")) {
                logger.info("Ascension 68: Skipping Dagger (elite minion, not a normal enemy)");
                return;
            }

            // Skip Byrd completely (handled by Level 62 Flight increase and Level 58 Headbutt increase)
            if (__instance.id != null && __instance.id.equals("Byrd")) {
                logger.info("Ascension 68: Skipping Byrd (handled by other levels)");
                return;
            }

            // Alternative check using instanceof
            if (__instance instanceof com.megacrit.cardcrawl.monsters.city.Byrd) {
                logger.info("Ascension 68: Skipping Byrd via instanceof check");
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

                // Special handling for GremlinThief: update thiefDamage field for SetMoveAction
                if (__instance.id != null && __instance.id.equals("GremlinThief")) {
                    try {
                        Field thiefDamageField = __instance.getClass().getDeclaredField("thiefDamage");
                        thiefDamageField.setAccessible(true);
                        int currentThiefDamage = thiefDamageField.getInt(__instance);
                        thiefDamageField.setInt(__instance, currentThiefDamage + damageIncrease);
                        logger.info(String.format(
                            "Ascension 68: GremlinThief thiefDamage field updated from %d to %d (Act %d)",
                            currentThiefDamage, currentThiefDamage + damageIncrease, actNum
                        ));
                    } catch (Exception e) {
                        logger.error("Failed to update GremlinThief thiefDamage field", e);
                    }
                }

                // Special handling for GremlinLeader: update STAB_DMG field for getMove() intent calculation
                if (__instance.id != null && __instance.id.equals("GremlinLeader")) {
                    try {
                        Field stabDmgField = __instance.getClass().getDeclaredField("STAB_DMG");
                        stabDmgField.setAccessible(true);
                        int currentStabDmg = stabDmgField.getInt(__instance);
                        stabDmgField.setInt(__instance, currentStabDmg + damageIncrease);
                        logger.info(String.format(
                            "Ascension 68: GremlinLeader STAB_DMG field updated from %d to %d (Act %d)",
                            currentStabDmg, currentStabDmg + damageIncrease, actNum
                        ));
                    } catch (Exception e) {
                        logger.error("Failed to update GremlinLeader STAB_DMG field", e);
                    }
                }

                // Mark as patched
                patchedMonsters.add(__instance);
            }
        }
    }

    /**
     * Generic Intent Fix for all monsters using hardcoded damage in setMove()
     *
     * This fixes Intent for normal enemies (not bosses) that use hardcoded damage values
     * in getMove()/setMove() instead of reading from damage array.
     *
     * Level 68 applies to normal enemies only, not bosses.
     */
    @SpirePatch(
        clz = AbstractMonster.class,
        method = "createIntent"
    )
    public static class GenericCreateIntentFix {
        @SpirePostfixPatch
        public static void Postfix(AbstractMonster __instance) {
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 68) {
                return;
            }

            // Skip bosses (Level 68 applies to normal enemies only)
            if (__instance.type == AbstractMonster.EnemyType.BOSS) {
                return;
            }

            try {
                // Only fix attack intents
                Field intentField = AbstractMonster.class.getDeclaredField("intent");
                intentField.setAccessible(true);
                AbstractMonster.Intent intent = (AbstractMonster.Intent) intentField.get(__instance);

                if (intent != AbstractMonster.Intent.ATTACK &&
                    intent != AbstractMonster.Intent.ATTACK_BUFF &&
                    intent != AbstractMonster.Intent.ATTACK_DEFEND &&
                    intent != AbstractMonster.Intent.ATTACK_DEBUFF) {
                    return;
                }

                // Get intentBaseDmg (what setMove() hardcoded)
                Field intentBaseDmgField = AbstractMonster.class.getDeclaredField("intentBaseDmg");
                intentBaseDmgField.setAccessible(true);
                int intentBaseDmg = intentBaseDmgField.getInt(__instance);

                if (intentBaseDmg <= 0) {
                    return;  // No damage intent
                }

                // Find matching damage in damage array
                for (int i = 0; i < __instance.damage.size(); i++) {
                    DamageInfo dmg = __instance.damage.get(i);
                    if (dmg != null && dmg.base > 0) {
                        // Check if this damage matches the hardcoded base
                        // Level 68: normal enemies only (+1/+2/+5)
                        // This patch only applies to normal enemies (bosses are excluded above)
                        int actNum = AbstractDungeon.actNum;
                        int expectedIncrease = 0;
                        if (actNum == 1) {
                            expectedIncrease = 1;
                        } else if (actNum == 2) {
                            expectedIncrease = 2;
                        } else if (actNum >= 3) {
                            expectedIncrease = 5;
                        }

                        // If dmg.base - expectedIncrease == intentBaseDmg, this is the right damage
                        if (dmg.base - expectedIncrease == intentBaseDmg) {
                            // Update intentDmg to use actual damage
                            Field intentDmgField = AbstractMonster.class.getDeclaredField("intentDmg");
                            intentDmgField.setAccessible(true);
                            intentDmgField.setInt(__instance, dmg.base);

                            logger.info(String.format(
                                "Ascension 68: %s Intent fixed in createIntent from %d to %d (index %d)",
                                __instance.name, intentBaseDmg, dmg.base, i
                            ));
                            break;  // Found the match, stop
                        }
                    }
                }
            } catch (Exception e) {
                logger.error("Failed to fix generic Intent in createIntent", e);
            }
        }
    }

    /**
     * Generic Intent Fix for applyPowers() - all monsters using hardcoded damage
     *
     * This is called every turn after player ends turn
     * Level 68 applies to normal enemies only, not bosses.
     */
    @SpirePatch(
        clz = AbstractMonster.class,
        method = "applyPowers"
    )
    public static class GenericApplyPowersFix {
        @SpirePostfixPatch
        public static void Postfix(AbstractMonster __instance) {
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 68) {
                return;
            }

            // Skip bosses (Level 68 applies to normal enemies only)
            if (__instance.type == AbstractMonster.EnemyType.BOSS) {
                return;
            }

            try {
                // Only fix attack intents
                Field intentField = AbstractMonster.class.getDeclaredField("intent");
                intentField.setAccessible(true);
                AbstractMonster.Intent intent = (AbstractMonster.Intent) intentField.get(__instance);

                if (intent != AbstractMonster.Intent.ATTACK &&
                    intent != AbstractMonster.Intent.ATTACK_BUFF &&
                    intent != AbstractMonster.Intent.ATTACK_DEFEND &&
                    intent != AbstractMonster.Intent.ATTACK_DEBUFF) {
                    return;
                }

                // Get intentBaseDmg (what setMove() hardcoded)
                Field intentBaseDmgField = AbstractMonster.class.getDeclaredField("intentBaseDmg");
                intentBaseDmgField.setAccessible(true);
                int intentBaseDmg = intentBaseDmgField.getInt(__instance);

                if (intentBaseDmg <= 0) {
                    return;  // No damage intent
                }

                // Find matching damage in damage array
                for (int i = 0; i < __instance.damage.size(); i++) {
                    DamageInfo dmg = __instance.damage.get(i);
                    if (dmg != null && dmg.base > 0) {
                        // Check if this damage matches the hardcoded base
                        int actNum = AbstractDungeon.actNum;
                        int expectedIncrease = 0;
                        if (actNum == 1) {
                            expectedIncrease = 1;
                        } else if (actNum == 2) {
                            expectedIncrease = 2;
                        } else if (actNum >= 3) {
                            expectedIncrease = 5;
                        }

                        // Add Level 69 boss damage increase if applicable
                        if (AbstractDungeon.ascensionLevel >= 69 && __instance.type == AbstractMonster.EnemyType.BOSS) {
                            if (actNum == 1) {
                                expectedIncrease += 1;  // Level 69: +1 for Act 1 bosses
                            } else if (actNum == 2) {
                                expectedIncrease += 3;  // Level 69: +3 for Act 2 bosses
                            } else if (actNum == 3) {
                                expectedIncrease += 6;  // Level 69: +6 for Act 3 bosses
                            }
                        }

                        // If dmg.base - expectedIncrease == intentBaseDmg, this is the right damage
                        if (dmg.base - expectedIncrease == intentBaseDmg) {
                            // Update intentDmg to use actual damage (after powers)
                            Field intentDmgField = AbstractMonster.class.getDeclaredField("intentDmg");
                            intentDmgField.setAccessible(true);
                            intentDmgField.setInt(__instance, dmg.output);

                            logger.info(String.format(
                                "Ascension 68: %s Intent fixed in applyPowers from %d to %d (index %d)",
                                __instance.name, intentBaseDmg, dmg.output, i
                            ));
                            break;  // Found the match, stop
                        }
                    }
                }
            } catch (Exception e) {
                logger.error("Failed to fix generic Intent in applyPowers", e);
            }
        }
    }

}
