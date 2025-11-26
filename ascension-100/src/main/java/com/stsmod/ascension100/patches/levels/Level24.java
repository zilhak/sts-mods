package com.stsmod.ascension100.patches.levels;

import com.badlogic.gdx.math.MathUtils;
import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePrefixPatch;
import com.megacrit.cardcrawl.cards.DamageInfo;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import com.megacrit.cardcrawl.monsters.city.Byrd;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * Ascension Level 24: Normal enemies deal 3% more damage (except Byrd)
 *
 * 일반 적들의 공격력이 3% 증가합니다. (섀 제외)
 */
public class Level24 {
    private static final Logger logger = LogManager.getLogger(Level24.class.getName());

    @SpirePatch(
        clz = AbstractMonster.class,
        method = "init"
    )
    public static class NormalDamageIncrease {
        // Track which monsters have already been patched to prevent duplicate application
        private static final Set<AbstractMonster> patchedMonsters = Collections.newSetFromMap(new WeakHashMap<>());

        @SpirePrefixPatch
        public static void Prefix(AbstractMonster __instance) {
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 24) {
                return;
            }

            // Exclude Byrd from damage increase
            if (__instance.type == AbstractMonster.EnemyType.NORMAL && !(__instance instanceof Byrd)) {
                // Check if already patched
                if (patchedMonsters.contains(__instance)) {
                    logger.warn(String.format(
                        "Ascension 24: Skipping duplicate damage increase for %s (already patched)",
                        __instance.name
                    ));
                    return;
                }

                float multiplier = 1.03f;

                for (DamageInfo damageInfo : __instance.damage) {
                    if (damageInfo != null && damageInfo.base > 0) {
                        int originalDamage = damageInfo.base;
                        damageInfo.base = MathUtils.ceil(damageInfo.base * multiplier);

                        logger.info(String.format(
                            "Ascension 24: Normal %s damage increased from %d to %d",
                            __instance.name, originalDamage, damageInfo.base
                        ));
                    }
                }

                // Mark as patched
                patchedMonsters.add(__instance);
            }
        }
    }

}
