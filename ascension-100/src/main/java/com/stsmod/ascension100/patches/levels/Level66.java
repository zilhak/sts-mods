package com.stsmod.ascension100.patches.levels;

import com.badlogic.gdx.math.MathUtils;
import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;
import com.megacrit.cardcrawl.actions.common.ApplyPowerAction;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import com.megacrit.cardcrawl.powers.MetallicizePower;
import com.megacrit.cardcrawl.powers.RegenerateMonsterPower;
import com.megacrit.cardcrawl.powers.StrengthPower;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Ascension Level 66: Some enemies start with buffs
 *
 * 일부 적이 버프를 가진채로 등장합니다.
 *
 * 1막의 모든 적은 5% 확률로 생성시 힘1, 금속화2 중 하나를 무작위로 부여받습니다.
 * 2막의 모든 적은 5% 확률로 생성시 금속화4, 재생3 중 하나를 무작위로 부여받습니다.
 * 3막의 모든 적은 5% 확률로 생성시 힘6, 금속화8, 재생6 중 하나를 무작위로 부여받습니다.
 */
public class Level66 {
    private static final Logger logger = LogManager.getLogger(Level66.class.getName());

    @SpirePatch(
        clz = AbstractMonster.class,
        method = "usePreBattleAction"
    )
    public static class RandomStartingBuffs {
        @SpirePostfixPatch
        public static void Postfix(AbstractMonster __instance) {
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 66) {
                return;
            }

            // 5% chance to get a random buff
            if (MathUtils.randomBoolean(0.05f)) {
                int actNum = AbstractDungeon.actNum;
                int randomBuff = MathUtils.random(2); // 0, 1, or 2

                if (actNum == 1) {
                    int act1Buff = MathUtils.random(1); // 0 or 1
                    switch (act1Buff) {
                        case 0: // Strength 1
                            AbstractDungeon.actionManager.addToBottom(
                                new ApplyPowerAction(__instance, __instance,
                                    new StrengthPower(__instance, 1), 1)
                            );
                            logger.info(String.format(
                                "Ascension 66: %s started with Strength 1 (Act 1)",
                                __instance.name
                            ));
                            break;
                        case 1: // Metallicize 2
                            AbstractDungeon.actionManager.addToBottom(
                                new ApplyPowerAction(__instance, __instance,
                                    new MetallicizePower(__instance, 2), 2)
                            );
                            logger.info(String.format(
                                "Ascension 66: %s started with Metallicize 2 (Act 1)",
                                __instance.name
                            ));
                            break;
                    }
                } else if (actNum == 2) {
                    int act2Buff = MathUtils.random(1); // 0 or 1
                    switch (act2Buff) {
                        case 0: // Metallicize 4
                            AbstractDungeon.actionManager.addToBottom(
                                new ApplyPowerAction(__instance, __instance,
                                    new MetallicizePower(__instance, 4), 4)
                            );
                            logger.info(String.format(
                                "Ascension 66: %s started with Metallicize 4 (Act 2)",
                                __instance.name
                            ));
                            break;
                        case 1: // Regeneration 3
                            AbstractDungeon.actionManager.addToBottom(
                                new ApplyPowerAction(__instance, __instance,
                                    new RegenerateMonsterPower(__instance, 3), 3)
                            );
                            logger.info(String.format(
                                "Ascension 66: %s started with Regeneration 3 (Act 2)",
                                __instance.name
                            ));
                            break;
                    }
                } else if (actNum >= 3) {
                    switch (randomBuff) {
                        case 0: // Strength 6
                            AbstractDungeon.actionManager.addToBottom(
                                new ApplyPowerAction(__instance, __instance,
                                    new StrengthPower(__instance, 6), 6)
                            );
                            logger.info(String.format(
                                "Ascension 66: %s started with Strength 6 (Act 3)",
                                __instance.name
                            ));
                            break;
                        case 1: // Metallicize 8
                            AbstractDungeon.actionManager.addToBottom(
                                new ApplyPowerAction(__instance, __instance,
                                    new MetallicizePower(__instance, 8), 8)
                            );
                            logger.info(String.format(
                                "Ascension 66: %s started with Metallicize 8 (Act 3)",
                                __instance.name
                            ));
                            break;
                        case 2: // Regeneration 6
                            AbstractDungeon.actionManager.addToBottom(
                                new ApplyPowerAction(__instance, __instance,
                                    new RegenerateMonsterPower(__instance, 6), 6)
                            );
                            logger.info(String.format(
                                "Ascension 66: %s started with Regeneration 6 (Act 3)",
                                __instance.name
                            ));
                            break;
                    }
                }
            }
        }
    }
}
