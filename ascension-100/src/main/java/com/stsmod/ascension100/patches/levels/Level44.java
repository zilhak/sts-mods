package com.stsmod.ascension100.patches.levels;

import com.evacipated.cardcrawl.modthespire.lib.ByRef;
import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePrefixPatch;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Ascension Level 44: Thieves steal more gold
 *
 * 도둑들이 더 많은 돈을 강탈합니다.
 * 도둑들이 도둑질을 10 증가합니다.
 *
 * Both Looter and Mugger eventually call AbstractPlayer.loseGold()
 * when stealing from the player, so we patch that method to detect
 * thieves and increase the stolen amount.
 */
public class Level44 {
    private static final Logger logger = LogManager.getLogger(Level44.class.getName());

    @SpirePatch(
        clz = AbstractPlayer.class,
        method = "loseGold"
    )
    public static class ThiefGoldIncrease {
        @SpirePrefixPatch
        public static void Prefix(AbstractPlayer __instance, @ByRef int[] amount) {
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 44) {
                return;
            }

            // Only modify if gold is being stolen (amount > 0) and in combat
            if (amount[0] <= 0 || AbstractDungeon.getCurrRoom() == null ||
                AbstractDungeon.getCurrRoom().monsters == null) {
                return;
            }

            // Check if currently fighting a thief (Looter or Mugger)
            boolean hasThief = false;
            String thiefName = "";

            for (AbstractMonster m : AbstractDungeon.getCurrRoom().monsters.monsters) {
                String className = m.getClass().getSimpleName();
                if (className.equals("Looter") || className.equals("Mugger")) {
                    hasThief = true;
                    thiefName = className;
                    break;
                }
            }

            if (hasThief) {
                int originalAmount = amount[0];
                amount[0] += 10;

                logger.info(String.format(
                    "Ascension 44: %s theft increased from %d to %d",
                    thiefName, originalAmount, amount[0]
                ));
            }
        }
    }
}
