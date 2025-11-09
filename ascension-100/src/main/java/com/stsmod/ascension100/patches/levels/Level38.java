package com.stsmod.ascension100.patches.levels;

import com.badlogic.gdx.math.MathUtils;
import com.evacipated.cardcrawl.modthespire.lib.ByRef;
import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePrefixPatch;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.rooms.VictoryRoom;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Ascension Level 38: Boss heal decreased
 *
 * 보스 전투 이후에 회복량이 감소합니다.
 * 보스 전투 이후에 회복량이 10% 감소합니다.
 */
public class Level38 {
    private static final Logger logger = LogManager.getLogger(Level38.class.getName());

    /**
     * Reduce boss victory heal by 10%
     */
    @SpirePatch(
        clz = AbstractCreature.class,
        method = "heal",
        paramtypez = {int.class, boolean.class}
    )
    public static class BossHealReduction {
        @SpirePrefixPatch
        public static void Prefix(AbstractCreature __instance, @ByRef int[] healAmount, boolean showEffect) {
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 38) {
                return;
            }

            // Only apply to player
            if (!(__instance instanceof AbstractPlayer)) {
                return;
            }

            // Check if we're in a VictoryRoom (boss defeated)
            // Use try-catch because getCurrRoom() can throw NPE during initialization
            try {
                if (AbstractDungeon.getCurrRoom() instanceof VictoryRoom) {
                    int originalAmount = healAmount[0];
                    healAmount[0] = MathUtils.floor(healAmount[0] * 0.9f);

                    logger.info(String.format(
                        "Ascension 38: Boss victory heal reduced from %d to %d (-10%%)",
                        originalAmount, healAmount[0]
                    ));
                }
            } catch (NullPointerException e) {
                // Silently ignore NPE during game initialization
                // This happens when heal() is called before dungeon is fully initialized
            }
        }
    }
}
