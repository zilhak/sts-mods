package com.stsmod.ascension100.patches.levels;

import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.monsters.city.Mugger;
import com.megacrit.cardcrawl.monsters.exordium.Looter;
import com.megacrit.cardcrawl.powers.AbstractPower;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Ascension Level 44: Thieves steal more gold
 *
 * 도둑들이 더 많은 돈을 강탈합니다.
 * 도둑들의 도둑질 수치가 10 증가합니다.
 *
 * Both Looter and Mugger's Thievery power increases by 10.
 */
public class Level44 {
    private static final Logger logger = LogManager.getLogger(Level44.class.getName());

    /**
     * Looter: Thievery +10
     */
    @SpirePatch(
        clz = Looter.class,
        method = "usePreBattleAction"
    )
    public static class LooterThieveryIncrease {
        @SpirePostfixPatch
        public static void Postfix(Looter __instance) {
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 44) {
                return;
            }

            // Increase Thievery power by 10
            AbstractPower thieveryPower = __instance.getPower("Thievery");
            if (thieveryPower != null) {
                thieveryPower.amount += 10;
                thieveryPower.updateDescription();
                logger.info(String.format(
                    "Ascension 44: Looter Thievery increased by 10 to %d",
                    thieveryPower.amount
                ));
            }
        }
    }

    /**
     * Mugger: Thievery +10
     */
    @SpirePatch(
        clz = Mugger.class,
        method = "usePreBattleAction"
    )
    public static class MuggerThieveryIncrease {
        @SpirePostfixPatch
        public static void Postfix(Mugger __instance) {
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 44) {
                return;
            }

            // Increase Thievery power by 10
            AbstractPower thieveryPower = __instance.getPower("Thievery");
            if (thieveryPower != null) {
                thieveryPower.amount += 10;
                thieveryPower.updateDescription();
                logger.info(String.format(
                    "Ascension 44: Mugger Thievery increased by 10 to %d",
                    thieveryPower.amount
                ));
            }
        }
    }
}
