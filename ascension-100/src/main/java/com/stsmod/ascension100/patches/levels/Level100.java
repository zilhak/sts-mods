package com.stsmod.ascension100.patches.levels;

import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;
import com.megacrit.cardcrawl.actions.common.ApplyPowerAction;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.monsters.ending.CorruptHeart;
import com.megacrit.cardcrawl.powers.TimeWarpPower;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Ascension Level 100: Corrupt Heart starts with Time Warp power
 *
 * 타락한 심장이 시간 포식 버프를 얻은채로 시작합니다.
 *
 * 타락한 심장(Corrupt Heart)이 전투 시작시 시간 포식(Time Warp) 파워를 가진 상태로 시작합니다.
 * 시간 포식: 플레이어가 카드를 12장 사용할 때마다 턴을 강제로 종료하고 모든 적이 힘 +2를 얻습니다.
 */
public class Level100 {
    private static final Logger logger = LogManager.getLogger(Level100.class.getName());

    /**
     * Apply Time Warp power to Corrupt Heart at battle start
     */
    @SpirePatch(
        clz = CorruptHeart.class,
        method = "usePreBattleAction"
    )
    public static class ApplyTimeWarpPower {
        @SpirePostfixPatch
        public static void Postfix(CorruptHeart __instance) {
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 100) {
                return;
            }

            // Apply Time Warp power to Corrupt Heart
            AbstractDungeon.actionManager.addToBottom(
                new ApplyPowerAction(
                    (AbstractCreature)__instance,
                    (AbstractCreature)__instance,
                    new TimeWarpPower((AbstractCreature)__instance),
                    0
                )
            );

            logger.info("Ascension 100: Applied Time Warp power to Corrupt Heart");
        }
    }
}
