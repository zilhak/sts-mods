package com.stsmod.ascension100.patches.levels;

import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;
import com.megacrit.cardcrawl.actions.common.ApplyPowerAction;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.monsters.ending.CorruptHeart;
import com.megacrit.cardcrawl.powers.BeatOfDeathPower;
import com.megacrit.cardcrawl.powers.MetallicizePower;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Ascension Level 82: Heart becomes stronger
 *
 * 심장이 더 강해집니다.
 *
 * 심장은 금속화를 20 추가로 얻습니다.
 * 심장의 "죽음의 고동" 수치가 1 증가합니다.
 */
public class Level82 {
    private static final Logger logger = LogManager.getLogger(Level82.class.getName());

    @SpirePatch(
        clz = CorruptHeart.class,
        method = "usePreBattleAction"
    )
    public static class BuffCorruptHeart {
        @SpirePostfixPatch
        public static void Postfix(CorruptHeart __instance) {
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 82) {
                return;
            }

            // Add Metallicize 20
            AbstractDungeon.actionManager.addToBottom(
                new ApplyPowerAction((AbstractCreature)__instance, (AbstractCreature)__instance,
                    new MetallicizePower((AbstractCreature)__instance, 20), 20)
            );

            // Increase Beat of Death by 1
            AbstractDungeon.actionManager.addToBottom(
                new ApplyPowerAction((AbstractCreature)__instance, (AbstractCreature)__instance,
                    new BeatOfDeathPower((AbstractCreature)__instance, 1), 1)
            );

            logger.info("Ascension 82: Corrupt Heart gained Metallicize 20 and Beat of Death +1");
        }
    }
}
