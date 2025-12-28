package com.stsmod.ascension100.patches.levels;

import com.badlogic.gdx.math.MathUtils;
import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePrefixPatch;
import com.megacrit.cardcrawl.cards.DamageInfo;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import com.megacrit.cardcrawl.monsters.beyond.AwakenedOne;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;

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
        method = "init"
    )
    public static class BossDamageByActIncrease {
        // Track which monsters have already been patched to prevent duplicate application
        private static final Set<AbstractMonster> patchedMonsters = Collections.newSetFromMap(new WeakHashMap<>());

        @SpirePrefixPatch
        public static void Prefix(AbstractMonster __instance) {
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 69) {
                return;
            }

            if (__instance.type == AbstractMonster.EnemyType.BOSS) {
                // Check if already patched
                if (patchedMonsters.contains(__instance)) {
                    logger.warn(String.format(
                        "Ascension 69: Skipping duplicate damage increase for %s (already patched)",
                        __instance.name
                    ));
                    return;
                }

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
                            damageInfo.output = damageInfo.base;  // Update output to match base
                        }
                    }

                    logger.info(String.format(
                        "Ascension 69: Boss %s damage increased by %d (Act %d)",
                        __instance.name, damageIncrease, actNum
                    ));

                    // Mark as patched
                    patchedMonsters.add(__instance);
                }
            }
        }
    }

    // ===== AwakenedOne Intent Fix Patches =====
    //
    // PROBLEM: getMove() uses hardcoded damage values in setMove() calls
    // - SLASH: setMove(1, ATTACK, 20) → hardcoded 20, actual damage[0] modified to 26
    // - SOUL_STRIKE: setMove(2, ATTACK, 6, 4) → hardcoded 6, actual damage[1] modified to 12
    // - DARK_ECHO: setMove(5, ATTACK, 40) → hardcoded 40, actual damage[2] modified to 46
    // - SLUDGE: setMove(6, ATTACK_DEBUFF, 18) → hardcoded 18, actual damage[3] modified to 24
    // - TACKLE: setMove(8, ATTACK, 10, 3) → hardcoded 10, actual damage[4] modified to 16
    //
    // Move byte → damage index mapping:
    // - byte 1 (SLASH) → damage[0]
    // - byte 2 (SOUL_STRIKE) → damage[1]
    // - byte 5 (DARK_ECHO) → damage[2]
    // - byte 6 (SLUDGE) → damage[3]
    // - byte 8 (TACKLE) → damage[4]
    // - byte 3 (REBIRTH) → Intent.UNKNOWN (no damage)
    //
    // SOLUTION: Same as GiantHead (Level68)
    // - Do NOT patch getMove() (this.move.baseDamage modification is useless)
    // - Patch createIntent() Postfix to fix intentDmg using damage[index].base
    // - Patch applyPowers() Postfix to fix intentDmg using damage[index].output

    /**
     * Patch 1: Fix Intent in createIntent() Postfix
     */
    @SpirePatch(
        clz = AbstractMonster.class,
        method = "createIntent"
    )
    public static class AwakenedOneCreateIntentFix {
        @SpirePostfixPatch
        public static void Postfix(AbstractMonster __instance) {
            // Only apply to AwakenedOne
            if (!(__instance instanceof AwakenedOne)) {
                return;
            }

            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 69) {
                return;
            }

            try {
                // Check if current move
                Field nextMoveField = AbstractMonster.class.getDeclaredField("nextMove");
                nextMoveField.setAccessible(true);
                byte nextMove = nextMoveField.getByte(__instance);

                // Map move byte to damage index
                int damageIndex = -1;
                switch (nextMove) {
                    case 1:  // SLASH
                        damageIndex = 0;
                        break;
                    case 2:  // SOUL_STRIKE
                        damageIndex = 1;
                        break;
                    case 5:  // DARK_ECHO
                        damageIndex = 2;
                        break;
                    case 6:  // SLUDGE
                        damageIndex = 3;
                        break;
                    case 8:  // TACKLE
                        damageIndex = 4;
                        break;
                    case 3:  // REBIRTH (no damage)
                        return;
                }

                if (damageIndex >= 0 && damageIndex < __instance.damage.size()) {
                    // CRITICAL: We must call calculateDamage() with correct baseDamage
                    // calculateDamage() is private, so we use reflection

                    int baseDamage = __instance.damage.get(damageIndex).base;

                    // Call private calculateDamage(int) method via reflection
                    java.lang.reflect.Method calculateDamageMethod = AbstractMonster.class.getDeclaredMethod("calculateDamage", int.class);
                    calculateDamageMethod.setAccessible(true);
                    calculateDamageMethod.invoke(__instance, baseDamage);

                    // Read the updated intentDmg
                    Field intentDmgField = AbstractMonster.class.getDeclaredField("intentDmg");
                    intentDmgField.setAccessible(true);
                    int actualDamage = intentDmgField.getInt(__instance);

                    logger.info(String.format(
                        "Ascension 69: AwakenedOne move %d Intent fixed in createIntent to %d (base=%d)",
                        nextMove, actualDamage, baseDamage
                    ));
                }
            } catch (Exception e) {
                logger.error("Failed to fix AwakenedOne Intent in createIntent", e);
            }
        }
    }

    /**
     * Patch 2: Fix Intent in applyPowers() Postfix
     */
    @SpirePatch(
        clz = AbstractMonster.class,
        method = "applyPowers"
    )
    public static class AwakenedOneApplyPowersFix {
        @SpirePostfixPatch
        public static void Postfix(AbstractMonster __instance) {
            // Only apply to AwakenedOne
            if (!(__instance instanceof AwakenedOne)) {
                return;
            }

            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 69) {
                return;
            }

            try {
                // CRITICAL: In applyPowers(), this.nextMove is NOT updated yet!
                // We must read this.move.nextMove instead
                // Reason: RollMoveAction calls setMove() which creates new EnemyMoveInfo,
                // but this.nextMove field is only updated in createIntent() (line 481)

                Field moveField = AbstractMonster.class.getDeclaredField("move");
                moveField.setAccessible(true);
                Object move = moveField.get(__instance);

                Field nextMoveField = move.getClass().getDeclaredField("nextMove");
                nextMoveField.setAccessible(true);
                byte nextMove = nextMoveField.getByte(move);

                // Map move byte to damage index
                int damageIndex = -1;
                switch (nextMove) {
                    case 1:  // SLASH
                        damageIndex = 0;
                        break;
                    case 2:  // SOUL_STRIKE
                        damageIndex = 1;
                        break;
                    case 5:  // DARK_ECHO
                        damageIndex = 2;
                        break;
                    case 6:  // SLUDGE
                        damageIndex = 3;
                        break;
                    case 8:  // TACKLE
                        damageIndex = 4;
                        break;
                    case 3:  // REBIRTH (no damage)
                        return;
                }

                if (damageIndex >= 0 && damageIndex < __instance.damage.size()) {
                    int actualDamage = __instance.damage.get(damageIndex).output;  // Use output after powers

                    // Update intentDmg field
                    Field intentDmgField = AbstractMonster.class.getDeclaredField("intentDmg");
                    intentDmgField.setAccessible(true);
                    int currentIntentDmg = intentDmgField.getInt(__instance);

                    if (currentIntentDmg != actualDamage) {
                        intentDmgField.setInt(__instance, actualDamage);
                        logger.info(String.format(
                            "Ascension 69: AwakenedOne move %d Intent fixed in applyPowers from %d to %d",
                            nextMove, currentIntentDmg, actualDamage
                        ));
                    }
                }
            } catch (Exception e) {
                logger.error("Failed to fix AwakenedOne Intent in applyPowers", e);
            }
        }
    }
}
