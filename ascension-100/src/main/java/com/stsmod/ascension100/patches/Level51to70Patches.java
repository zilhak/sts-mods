package com.stsmod.ascension100.patches;

import com.badlogic.gdx.math.MathUtils;
import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;
import com.megacrit.cardcrawl.actions.common.ApplyPowerAction;
import com.megacrit.cardcrawl.cards.DamageInfo;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import com.megacrit.cardcrawl.powers.*;
import com.megacrit.cardcrawl.powers.RegenerateMonsterPower;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Patches for Ascension Levels 51-70
 *
 * Level 51: All enemies +20% HP
 * Level 52: All enemies +1 damage
 * Level 57: All enemies +10% HP
 * Level 58: All enemies +1 damage
 * Level 59: All elites +10 HP
 * Level 61: All enemies +10% HP
 * Level 62: All enemies +1 damage
 * Level 63: All bosses +20% HP
 * Level 64: All bosses +10% damage
 * Level 67: All enemies +15% HP
 * Level 68: Enemies +1/2/5 damage by act
 * Level 69: Bosses HP +10%/20%/30%, damage +1/4/10 by act
 */
public class Level51to70Patches {

    private static final Logger logger = LogManager.getLogger(Level51to70Patches.class.getName());

    /**
     * Additional HP increases for levels 51-70
     */
    @SpirePatch(
        cls = "com.megacrit.cardcrawl.monsters.AbstractMonster",
        method = "init"
    )
    public static class HighLevelHPPatch {
        @SpirePostfixPatch
        public static void Postfix(AbstractMonster __instance) {
            if (!AbstractDungeon.isAscensionMode) {
                return;
            }

            int level = AbstractDungeon.ascensionLevel;
            float hpMultiplier = 1.0f;
            int hpBonus = 0;
            int originalMaxHP = __instance.maxHealth;

            // Level 51: All enemies +20% HP
            if (level >= 51) {
                hpMultiplier *= 1.2f;
            }

            // Level 57: All enemies +10% HP
            if (level >= 57) {
                hpMultiplier *= 1.1f;
            }

            // Level 59: Elites +10 HP
            if (level >= 59 && __instance.type == AbstractMonster.EnemyType.ELITE) {
                hpBonus += 10;
            }

            // Level 61: All enemies +10% HP
            if (level >= 61) {
                hpMultiplier *= 1.1f;
            }

            // Level 63: Bosses +20% HP
            if (level >= 63 && __instance.type == AbstractMonster.EnemyType.BOSS) {
                hpMultiplier *= 1.2f;
            }

            // Level 67: All enemies +15% HP
            if (level >= 67) {
                hpMultiplier *= 1.15f;
            }

            // Level 69: Bosses HP by act (+10%/+20%/+30%)
            if (level >= 69 && __instance.type == AbstractMonster.EnemyType.BOSS) {
                int actNum = AbstractDungeon.actNum;
                if (actNum == 1) {
                    hpMultiplier *= 1.1f;
                } else if (actNum == 2) {
                    hpMultiplier *= 1.2f;
                } else if (actNum >= 3) {
                    hpMultiplier *= 1.3f;
                }
            }

            if (hpMultiplier > 1.0f) {
                __instance.maxHealth = MathUtils.ceil(__instance.maxHealth * hpMultiplier);
                __instance.currentHealth = MathUtils.ceil(__instance.currentHealth * hpMultiplier);
            }

            if (hpBonus > 0) {
                __instance.maxHealth += hpBonus;
                __instance.currentHealth += hpBonus;
            }

            if (hpMultiplier > 1.0f || hpBonus > 0) {
                logger.info(String.format(
                    "Ascension %d: %s (%s) HP increased from %d to %d (x%.2f, +%d)",
                    level, __instance.name, __instance.type,
                    originalMaxHP, __instance.maxHealth, hpMultiplier, hpBonus
                ));
            }
        }
    }

    /**
     * Additional damage increases for levels 51-70
     */
    @SpirePatch(
        cls = "com.megacrit.cardcrawl.monsters.AbstractMonster",
        method = "usePreBattleAction"
    )
    public static class HighLevelDamagePatch {
        @SpirePostfixPatch
        public static void Postfix(AbstractMonster __instance) {
            if (!AbstractDungeon.isAscensionMode) {
                return;
            }

            int level = AbstractDungeon.ascensionLevel;
            int damageIncrease = 0;
            float damageMultiplier = 1.0f;

            // Level 52: All enemies +1 damage
            if (level >= 52) {
                damageIncrease += 1;
            }

            // Level 58: All enemies +1 damage
            if (level >= 58) {
                damageIncrease += 1;
            }

            // Level 62: All enemies +1 damage
            if (level >= 62) {
                damageIncrease += 1;
            }

            // Level 64: Bosses +10% damage
            if (level >= 64 && __instance.type == AbstractMonster.EnemyType.BOSS) {
                damageMultiplier *= 1.1f;
            }

            // Level 68: Damage by act (+1/+2/+5)
            if (level >= 68) {
                int actNum = AbstractDungeon.actNum;
                if (actNum == 1) {
                    damageIncrease += 1;
                } else if (actNum == 2) {
                    damageIncrease += 2;
                } else if (actNum >= 3) {
                    damageIncrease += 5;
                }
            }

            // Level 69: Boss damage by act (+1/+4/+10)
            if (level >= 69 && __instance.type == AbstractMonster.EnemyType.BOSS) {
                int actNum = AbstractDungeon.actNum;
                if (actNum == 1) {
                    damageIncrease += 1;
                } else if (actNum == 2) {
                    damageIncrease += 4;
                } else if (actNum >= 3) {
                    damageIncrease += 10;
                }
            }

            if (damageIncrease > 0 || damageMultiplier > 1.0f) {
                for (DamageInfo damageInfo : __instance.damage) {
                    if (damageInfo != null && damageInfo.base > 0) {
                        damageInfo.base = MathUtils.ceil(damageInfo.base * damageMultiplier) + damageIncrease;
                    }
                }

                logger.info(String.format(
                    "Ascension %d: %s (%s) damage modified (x%.2f, +%d)",
                    level, __instance.name, __instance.type, damageMultiplier, damageIncrease
                ));
            }
        }
    }

    /**
     * Level 66: Some enemies start with buffs (15% chance)
     * Level 70: Elites gain special advantages by act
     */
    @SpirePatch(
        cls = "com.megacrit.cardcrawl.monsters.AbstractMonster",
        method = "usePreBattleAction"
    )
    public static class StartingBuffsPatch {
        @SpirePostfixPatch
        public static void Postfix(AbstractMonster __instance) {
            if (!AbstractDungeon.isAscensionMode) {
                return;
            }

            int level = AbstractDungeon.ascensionLevel;
            int actNum = AbstractDungeon.actNum;

            // Level 66: Random buffs (15% chance)
            if (level >= 66 && MathUtils.randomBoolean(0.15f)) {
                int randomBuff = MathUtils.random(2); // 0, 1, or 2

                if (actNum == 1) {
                    switch (randomBuff) {
                        case 0: // Strength 2
                            AbstractDungeon.actionManager.addToBottom(
                                new ApplyPowerAction(__instance, __instance,
                                    new StrengthPower(__instance, 2), 2)
                            );
                            break;
                        case 1: // Metallicize 2
                            AbstractDungeon.actionManager.addToBottom(
                                new ApplyPowerAction(__instance, __instance,
                                    new MetallicizePower(__instance, 2), 2)
                            );
                            break;
                        case 2: // Regeneration 4
                            AbstractDungeon.actionManager.addToBottom(
                                new ApplyPowerAction(__instance, __instance,
                                    new RegenerateMonsterPower(__instance, 4), 4)
                            );
                            break;
                    }
                    logger.info(String.format("Ascension 66: %s started with random buff (Act 1)", __instance.name));
                } else if (actNum == 2) {
                    switch (randomBuff) {
                        case 0: // Strength 3
                            AbstractDungeon.actionManager.addToBottom(
                                new ApplyPowerAction(__instance, __instance,
                                    new StrengthPower(__instance, 3), 3)
                            );
                            break;
                        case 1: // Metallicize 5
                            AbstractDungeon.actionManager.addToBottom(
                                new ApplyPowerAction(__instance, __instance,
                                    new MetallicizePower(__instance, 5), 5)
                            );
                            break;
                        case 2: // Regeneration 8
                            AbstractDungeon.actionManager.addToBottom(
                                new ApplyPowerAction(__instance, __instance,
                                    new RegenerateMonsterPower(__instance, 8), 8)
                            );
                            break;
                    }
                    logger.info(String.format("Ascension 66: %s started with random buff (Act 2)", __instance.name));
                } else if (actNum >= 3) {
                    switch (randomBuff) {
                        case 0: // Strength 6
                            AbstractDungeon.actionManager.addToBottom(
                                new ApplyPowerAction(__instance, __instance,
                                    new StrengthPower(__instance, 6), 6)
                            );
                            break;
                        case 1: // Metallicize 8
                            AbstractDungeon.actionManager.addToBottom(
                                new ApplyPowerAction(__instance, __instance,
                                    new MetallicizePower(__instance, 8), 8)
                            );
                            break;
                        case 2: // Regeneration 15
                            AbstractDungeon.actionManager.addToBottom(
                                new ApplyPowerAction(__instance, __instance,
                                    new RegenerateMonsterPower(__instance, 15), 15)
                            );
                            break;
                    }
                    logger.info(String.format("Ascension 66: %s started with random buff (Act 3)", __instance.name));
                }
            }

            // Level 70: Elites gain special advantages by act
            if (level >= 70 && __instance.type == AbstractMonster.EnemyType.ELITE) {
                if (actNum == 1) {
                    // Act 1: Metallicize 4
                    AbstractDungeon.actionManager.addToBottom(
                        new ApplyPowerAction(__instance, __instance,
                            new MetallicizePower(__instance, 4), 4)
                    );
                    logger.info(String.format("Ascension 70: Elite %s gained Metallicize 4 (Act 1)", __instance.name));
                } else if (actNum == 2) {
                    // Act 2: Strength 2
                    AbstractDungeon.actionManager.addToBottom(
                        new ApplyPowerAction(__instance, __instance,
                            new StrengthPower(__instance, 2), 2)
                    );
                    logger.info(String.format("Ascension 70: Elite %s gained Strength 2 (Act 2)", __instance.name));
                } else if (actNum >= 3) {
                    // Act 3: Intangible 2
                    AbstractDungeon.actionManager.addToBottom(
                        new ApplyPowerAction(__instance, __instance,
                            new IntangiblePower(__instance, 2), 2)
                    );
                    logger.info(String.format("Ascension 70: Elite %s gained Intangible 2 (Act 3)", __instance.name));
                }
            }
        }
    }
}
