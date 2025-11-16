package com.stsmod.ascension100.patches.levels;

import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;
import com.megacrit.cardcrawl.actions.common.ApplyPowerAction;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import com.megacrit.cardcrawl.powers.IntangiblePower;
import com.megacrit.cardcrawl.powers.MetallicizePower;
import com.megacrit.cardcrawl.powers.StrengthPower;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Ascension Level 70: Normal elites gain special advantages
 *
 * 일반 엘리트가 특수한 이점을 얻습니다.
 * 1막의 엘리트는 금속화를 2 얻습니다.
 * 2막의 엘리트는 힘을 2 얻습니다.
 * 3막의 엘리트는 불가침을 1 얻습니다.
 */
public class Level70 {
    private static final Logger logger = LogManager.getLogger(Level70.class.getName());

    @SpirePatch(
        clz = AbstractMonster.class,
        method = "usePreBattleAction"
    )
    public static class EliteAdvantagesByAct {
        @SpirePostfixPatch
        public static void Postfix(AbstractMonster __instance) {
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 70) {
                return;
            }

            if (__instance.type == AbstractMonster.EnemyType.ELITE) {
                int actNum = AbstractDungeon.actNum;

                if (actNum == 1) {
                    // Act 1: Metallicize 2
                    AbstractDungeon.actionManager.addToBottom(
                        new ApplyPowerAction(__instance, __instance,
                            new MetallicizePower(__instance, 2), 2)
                    );
                    logger.info(String.format(
                        "Ascension 70: Elite %s gained Metallicize 2 (Act 1)",
                        __instance.name
                    ));
                } else if (actNum == 2) {
                    // Act 2: Strength 2
                    AbstractDungeon.actionManager.addToBottom(
                        new ApplyPowerAction(__instance, __instance,
                            new StrengthPower(__instance, 2), 2)
                    );
                    logger.info(String.format(
                        "Ascension 70: Elite %s gained Strength 2 (Act 2)",
                        __instance.name
                    ));
                } else if (actNum >= 3) {
                    // Act 3: Intangible 1
                    AbstractDungeon.actionManager.addToBottom(
                        new ApplyPowerAction(__instance, __instance,
                            new IntangiblePower(__instance, 1), 1)
                    );
                    logger.info(String.format(
                        "Ascension 70: Elite %s gained Intangible 1 (Act 3)",
                        __instance.name
                    ));
                }
            }
        }
    }
}
