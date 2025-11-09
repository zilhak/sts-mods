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
 * 1막의 모든 적은 15% 확률로 생성시 힘2, 금속화2, 재생4 중 하나를 무작위로 부여받습니다.
 * 2막의 모든 적은 15% 확률로 생성시 힘3, 금속화5, 재생8 중 하나를 무작위로 부여받습니다.
 * 3막의 모든 적은 15% 확률로 생성시 힘6, 금속화8, 재생15 중 하나를 무작위로 부여받습니다.
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

            // 15% chance to get a random buff
            if (MathUtils.randomBoolean(0.15f)) {
                int actNum = AbstractDungeon.actNum;
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
                    logger.info(String.format(
                        "Ascension 66: %s started with random buff (Act 1)",
                        __instance.name
                    ));
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
                    logger.info(String.format(
                        "Ascension 66: %s started with random buff (Act 2)",
                        __instance.name
                    ));
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
                    logger.info(String.format(
                        "Ascension 66: %s started with random buff (Act 3)",
                        __instance.name
                    ));
                }
            }
        }
    }
}
