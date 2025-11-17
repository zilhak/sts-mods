package com.stsmod.ascension100.patches.levels;

import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;
import com.megacrit.cardcrawl.actions.common.ApplyPowerAction;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.monsters.MonsterGroup;
import com.stsmod.ascension100.powers.TurnLimitPower;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Ascension Level 95: Die on 10th turn end
 *
 * 10번째 턴 종료시 사망합니다.
 *
 * 전투 시작시 제한 시간 10을 얻습니다.
 * (제한 시간: 턴이 끝날 때마다 1 감소. 1이 되면 999 데미지, 계속 유지)
 */
public class Level95 {
    private static final Logger logger = LogManager.getLogger(Level95.class.getName());

    @SpirePatch(
        clz = MonsterGroup.class,
        method = "usePreBattleAction"
    )
    public static class ApplyTurnLimitPower {
        @SpirePostfixPatch
        public static void Postfix(MonsterGroup __instance) {
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 95) {
                return;
            }

            // Apply Turn Limit 10 to player at battle start
            if (AbstractDungeon.player != null) {
                AbstractDungeon.actionManager.addToBottom(
                    new ApplyPowerAction(
                        (AbstractCreature)AbstractDungeon.player,
                        (AbstractCreature)AbstractDungeon.player,
                        new TurnLimitPower((AbstractCreature)AbstractDungeon.player, 10),
                        10
                    )
                );

                logger.info("Ascension 95: Applied Turn Limit 10 to player (die on turn 10)");
            }
        }
    }
}
