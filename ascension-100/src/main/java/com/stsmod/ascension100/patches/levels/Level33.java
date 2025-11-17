package com.stsmod.ascension100.patches.levels;

import com.badlogic.gdx.math.MathUtils;
import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import com.stsmod.ascension100.util.EncounterHelper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Ascension Level 33: Strong enemies have additional 5% more HP
 * Strong Enemies 전투에서 적들의 체력이 추가로 5% 증가합니다.
 */
public class Level33 {
    private static final Logger logger = LogManager.getLogger(Level33.class.getName());

    @SpirePatch(
        clz = AbstractMonster.class,
        method = "init"
    )
    public static class NormalHealthIncrease {
        @SpirePostfixPatch
        public static void Postfix(AbstractMonster __instance) {
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 33) {
                return;
            }

            // Skip bosses and elites
            if (__instance.type == AbstractMonster.EnemyType.BOSS ||
                __instance.type == AbstractMonster.EnemyType.ELITE) {
                return;
            }

            // Check if this is a Strong Enemy encounter
            if (!EncounterHelper.isStrongEncounter()) {
                return;
            }

            int originalMaxHP = __instance.maxHealth;
            __instance.maxHealth = MathUtils.ceil(__instance.maxHealth * 1.05f);
            __instance.currentHealth = MathUtils.ceil(__instance.currentHealth * 1.05f);

            logger.info(String.format(
                "Ascension 33: Strong Enemy %s HP increased from %d to %d (+5%%)",
                __instance.name, originalMaxHP, __instance.maxHealth
            ));
        }
    }
}
