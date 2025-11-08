package com.stsmod.ascension100.patches;

import com.badlogic.gdx.math.MathUtils;
import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Patches for Ascension Levels 31-39
 *
 * Level 31: Start with 5 less current HP
 * Level 32: Elite HP +10%
 * Level 33: Normal HP +10%
 * Level 34: Elite damage +2
 * Level 35: Normal damage +1
 * Level 36: Boss HP +15%, damage -1
 * Level 37: Potion price -40%, effect -20%
 * Level 38: Boss heal -10%
 * Level 39: Start with 5 less max HP
 */
public class Level31to39Patches {

    private static final Logger logger = LogManager.getLogger(Level31to39Patches.class.getName());

    // NOTE: Level 31 & 39 (starting HP reduction) is now handled by PlayerStartingHPPatch.java
    // to ensure it only applies once at game start, not every battle

    /**
     * Levels 32-36: Additional HP increases
     */
    @SpirePatch(
        cls = "com.megacrit.cardcrawl.monsters.AbstractMonster",
        method = "init"
    )
    public static class AdditionalHPPatch {
        @SpirePostfixPatch
        public static void Postfix(AbstractMonster __instance) {
            if (!AbstractDungeon.isAscensionMode) {
                return;
            }

            int level = AbstractDungeon.ascensionLevel;
            float hpMultiplier = 1.0f;
            int originalMaxHP = __instance.maxHealth;

            // Level 32: Elite +10% HP
            if (level >= 32 && __instance.type == AbstractMonster.EnemyType.ELITE) {
                hpMultiplier *= 1.1f;
            }

            // Level 33: Normal +10% HP
            if (level >= 33 && __instance.type == AbstractMonster.EnemyType.NORMAL) {
                hpMultiplier *= 1.1f;
            }

            // Level 36: Boss +15% HP
            if (level >= 36 && __instance.type == AbstractMonster.EnemyType.BOSS) {
                hpMultiplier *= 1.15f;
            }

            if (hpMultiplier > 1.0f) {
                __instance.maxHealth = MathUtils.ceil(__instance.maxHealth * hpMultiplier);
                __instance.currentHealth = MathUtils.ceil(__instance.currentHealth * hpMultiplier);

                logger.info(String.format(
                    "Ascension %d: %s (%s) HP increased from %d to %d (x%.2f)",
                    level, __instance.name, __instance.type,
                    originalMaxHP, __instance.maxHealth, hpMultiplier
                ));
            }
        }
    }

    /**
     * Levels 34-36: Additional damage modifications
     */
    @SpirePatch(
        cls = "com.megacrit.cardcrawl.monsters.AbstractMonster",
        method = "usePreBattleAction"
    )
    public static class AdditionalDamagePatch {
        @SpirePostfixPatch
        public static void Postfix(AbstractMonster __instance) {
            if (!AbstractDungeon.isAscensionMode) {
                return;
            }

            int level = AbstractDungeon.ascensionLevel;
            int damageIncrease = 0;

            // Level 34: Elite +2 damage
            if (level >= 34 && __instance.type == AbstractMonster.EnemyType.ELITE) {
                damageIncrease += 2;
            }

            // Level 35: Normal +1 damage
            if (level >= 35 && __instance.type == AbstractMonster.EnemyType.NORMAL) {
                damageIncrease += 1;
            }

            // Level 36: Boss -1 damage
            if (level >= 36 && __instance.type == AbstractMonster.EnemyType.BOSS) {
                damageIncrease -= 1;
            }

            if (damageIncrease != 0) {
                for (int i = 0; i < __instance.damage.size(); i++) {
                    if (__instance.damage.get(i) != null && __instance.damage.get(i).base > 0) {
                        __instance.damage.get(i).base += damageIncrease;
                    }
                }

                logger.info(String.format(
                    "Ascension %d: %s (%s) damage modified by %d",
                    level, __instance.name, __instance.type, damageIncrease
                ));
            }
        }
    }
}
