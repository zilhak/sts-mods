package com.stsmod.ascension100.patches.levels;

import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;
import com.megacrit.cardcrawl.actions.common.ApplyPowerAction;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import com.megacrit.cardcrawl.monsters.MonsterGroup;
import com.megacrit.cardcrawl.powers.RegenerateMonsterPower;
import com.megacrit.cardcrawl.powers.StrengthPower;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Ascension Level 77: Special battle difficulty increases
 *
 * 특수전투의 난이도가 상승합니다.
 *
 * 특수 전투의 난이도가 상승합니다.
 * 1막 : 적들은 힘을 2 얻습니다.
 * 2막 : 적들은 재생을 5 추가로 얻습니다.
 * 3막 : 적들은 힘을 5 얻습니다.
 */
public class Level77 {
    private static final Logger logger = LogManager.getLogger(Level77.class.getName());

    /**
     * Apply additional buffs to special battles
     */
    @SpirePatch(
        clz = MonsterGroup.class,
        method = "usePreBattleAction"
    )
    public static class ApplyAdditionalBuffs {
        @SpirePostfixPatch
        public static void Postfix(MonsterGroup __instance) {
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 77) {
                return;
            }

            if (!Level76.SpecialBattleTracker.isSpecialBattle) {
                return;
            }

            int actNum = AbstractDungeon.actNum;

            // Apply additional buffs based on act
            for (AbstractMonster m : __instance.monsters) {
                if (m != null && !m.isDying && !m.isDead) {
                    switch (actNum) {
                        case 1:
                            // Act 1: +2 Strength
                            AbstractDungeon.actionManager.addToBottom(
                                new ApplyPowerAction(
                                    (AbstractCreature)m,
                                    (AbstractCreature)m,
                                    new StrengthPower((AbstractCreature)m, 2),
                                    2
                                )
                            );
                            logger.info("Ascension 77: Applied +2 Strength (Act 1)");
                            break;

                        case 2:
                            // Act 2: +5 Regeneration (additional)
                            AbstractDungeon.actionManager.addToBottom(
                                new ApplyPowerAction(
                                    (AbstractCreature)m,
                                    (AbstractCreature)m,
                                    new RegenerateMonsterPower(m, 5),
                                    5
                                )
                            );
                            logger.info("Ascension 77: Applied +5 Regeneration (Act 2)");
                            break;

                        case 3:
                            // Act 3: +5 Strength
                            AbstractDungeon.actionManager.addToBottom(
                                new ApplyPowerAction(
                                    (AbstractCreature)m,
                                    (AbstractCreature)m,
                                    new StrengthPower((AbstractCreature)m, 5),
                                    5
                                )
                            );
                            logger.info("Ascension 77: Applied +5 Strength (Act 3)");
                            break;
                    }
                }
            }
        }
    }
}
