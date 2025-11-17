package com.stsmod.ascension100.patches.levels;

import com.badlogic.gdx.math.MathUtils;
import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;
import com.megacrit.cardcrawl.actions.common.ApplyPowerAction;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import com.megacrit.cardcrawl.monsters.MonsterGroup;
import com.megacrit.cardcrawl.powers.MetallicizePower;
import com.megacrit.cardcrawl.powers.RegenerateMonsterPower;
import com.megacrit.cardcrawl.powers.StrengthPower;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Ascension Level 81: Some enemies start with buffs
 *
 * 일부 적이 버프를 가진채로 등장합니다.
 *
 * 모든 일반 적은 15% 확률로 생성시 힘1, 금속화2, 재생1 중 하나를 무작위로 부여받습니다.
 * (한 전투당 하나의 적만 버프를 얻을 수 있습니다.)
 */
public class Level81 {
    private static final Logger logger = LogManager.getLogger(Level81.class.getName());
    private static boolean buffAppliedThisCombat = false;

    @SpirePatch(
        clz = AbstractMonster.class,
        method = "usePreBattleAction"
    )
    public static class RandomStartingBuffsForNormalEnemies {
        @SpirePostfixPatch
        public static void Postfix(AbstractMonster __instance) {
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 81) {
                return;
            }

            // Only apply to normal enemies (not elites or bosses)
            if (__instance.type != AbstractMonster.EnemyType.NORMAL) {
                return;
            }

            // Only one enemy per combat can receive a buff
            if (buffAppliedThisCombat) {
                return;
            }

            // 15% chance to get a random buff
            if (MathUtils.randomBoolean(0.15f)) {
                buffAppliedThisCombat = true; // Mark that a buff has been applied this combat
                int randomBuff = MathUtils.random(2); // 0, 1, or 2

                switch (randomBuff) {
                    case 0: // Strength 1
                        AbstractDungeon.actionManager.addToBottom(
                            new ApplyPowerAction(__instance, __instance,
                                new StrengthPower(__instance, 1), 1)
                        );
                        logger.info(String.format(
                            "Ascension 81: Normal enemy %s gained Strength 1",
                            __instance.name
                        ));
                        break;
                    case 1: // Metallicize 2
                        AbstractDungeon.actionManager.addToBottom(
                            new ApplyPowerAction(__instance, __instance,
                                new MetallicizePower(__instance, 2), 2)
                        );
                        logger.info(String.format(
                            "Ascension 81: Normal enemy %s gained Metallicize 2",
                            __instance.name
                        ));
                        break;
                    case 2: // Regeneration 1
                        AbstractDungeon.actionManager.addToBottom(
                            new ApplyPowerAction(__instance, __instance,
                                new RegenerateMonsterPower(__instance, 1), 1)
                        );
                        logger.info(String.format(
                            "Ascension 81: Normal enemy %s gained Regeneration 1",
                            __instance.name
                        ));
                        break;
                }
            }
        }
    }

    /**
     * Reset the buff flag when a new combat starts
     */
    @SpirePatch(
        clz = MonsterGroup.class,
        method = "init"
    )
    public static class ResetBuffFlagOnNewCombat {
        @SpirePostfixPatch
        public static void Postfix(MonsterGroup __instance) {
            // Reset the flag at the start of each new combat
            buffAppliedThisCombat = false;
            logger.debug("Ascension 81: Reset buff flag for new combat");
        }
    }
}
