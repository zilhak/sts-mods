package com.stsmod.ascension100.patches;

import com.badlogic.gdx.math.MathUtils;
import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;
import com.megacrit.cardcrawl.cards.DamageInfo;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Patches to modify monster damage for extended ascension levels
 *
 * Ascension 23: Elite enemies deal 10% more damage
 * Ascension 24: Normal enemies deal 10% more damage
 */
public class MonsterDamagePatch {

    private static final Logger logger = LogManager.getLogger(MonsterDamagePatch.class.getName());

    /**
     * Patch monster pre-battle action to increase damage at Ascension 23-24
     */
    @SpirePatch(
        cls = "com.megacrit.cardcrawl.monsters.AbstractMonster",
        method = "usePreBattleAction"
    )
    public static class DamageIncreasePatch {
        @SpirePostfixPatch
        public static void Postfix(AbstractMonster __instance) {
            if (!AbstractDungeon.isAscensionMode) {
                return;
            }

            float damageMultiplier = getDamageMultiplier(__instance);

            if (damageMultiplier > 1.0f) {
                // Apply damage multiplier to all damage values
                for (DamageInfo damageInfo : __instance.damage) {
                    if (damageInfo != null && damageInfo.base > 0) {
                        int originalDamage = damageInfo.base;
                        damageInfo.base = MathUtils.ceil(damageInfo.base * damageMultiplier);

                        logger.info(String.format(
                            "Ascension %d: %s (%s) damage increased from %d to %d (x%.2f)",
                            AbstractDungeon.ascensionLevel,
                            __instance.name,
                            __instance.type,
                            originalDamage,
                            damageInfo.base,
                            damageMultiplier
                        ));
                    }
                }
            }
        }
    }

    /**
     * Calculate damage multiplier based on ascension level and monster type
     *
     * @param monster The monster instance
     * @return Damage multiplier (1.0 = no change)
     */
    private static float getDamageMultiplier(AbstractMonster monster) {
        int level = AbstractDungeon.ascensionLevel;
        float multiplier = 1.0f;

        // Ascension 23: Elite enemies +10% damage
        if (level >= 23 && monster.type == AbstractMonster.EnemyType.ELITE) {
            multiplier *= 1.1f;
        }

        // Ascension 24: Normal enemies +10% damage
        if (level >= 24 && monster.type == AbstractMonster.EnemyType.NORMAL) {
            multiplier *= 1.1f;
        }

        return multiplier;
    }
}
