package com.stsmod.ascension100.patches.levels;

import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;
import com.megacrit.cardcrawl.actions.common.ApplyPowerAction;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import com.megacrit.cardcrawl.monsters.MonsterGroup;
import com.megacrit.cardcrawl.powers.BufferPower;
import com.megacrit.cardcrawl.powers.RitualPower;
import com.megacrit.cardcrawl.powers.TimeWarpPower;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Ascension Level 77: Special battle difficulty increases
 *
 * 특수전투의 난이도가 상승합니다.
 *
 * 특수 전투의 난이도가 상승합니다.
 * 1막 : 적들은 의식 버프 1을 얻습니다.
 * 2막 : 적들은 버퍼 버프 1을 얻습니다.
 * 3막 : 적들은 시간왜곡 버프를 얻습니다.
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
                            // Act 1: Ritual 1 (Gain 1 Strength at end of turn)
                            AbstractDungeon.actionManager.addToBottom(
                                new ApplyPowerAction(
                                    (AbstractCreature)m,
                                    (AbstractCreature)m,
                                    new RitualPower((AbstractCreature)m, 1, false),
                                    1
                                )
                            );
                            logger.info(String.format(
                                "Ascension 77: Applied Ritual 1 to %s (Act 1 Special Battle)",
                                m.name
                            ));
                            break;

                        case 2:
                            // Act 2: Buffer 1 (Prevent HP loss from next attack)
                            AbstractDungeon.actionManager.addToBottom(
                                new ApplyPowerAction(
                                    (AbstractCreature)m,
                                    (AbstractCreature)m,
                                    new BufferPower((AbstractCreature)m, 1),
                                    1
                                )
                            );
                            logger.info(String.format(
                                "Ascension 77: Applied Buffer 1 to %s (Act 2 Special Battle)",
                                m.name
                            ));
                            break;

                        case 3:
                            // Act 3: Time Warp (when player plays 12 cards in a turn, end turn and gain 1 Strength)
                            AbstractDungeon.actionManager.addToBottom(
                                new ApplyPowerAction(
                                    (AbstractCreature)m,
                                    (AbstractCreature)m,
                                    new TimeWarpPower((AbstractCreature)m),
                                    1
                                )
                            );
                            logger.info(String.format(
                                "Ascension 77: Applied Time Warp to %s (Act 3 Special Battle)",
                                m.name
                            ));
                            break;
                    }
                }
            }
        }
    }
}
