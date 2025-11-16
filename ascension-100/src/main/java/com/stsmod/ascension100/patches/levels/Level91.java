package com.stsmod.ascension100.patches.levels;

import com.evacipated.cardcrawl.modthespire.lib.*;
import com.megacrit.cardcrawl.actions.common.ApplyPowerAction;
import com.megacrit.cardcrawl.actions.common.MakeTempCardInDiscardAction;
import com.megacrit.cardcrawl.cards.curses.AscendersBane;
import com.megacrit.cardcrawl.cards.curses.Writhe;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.monsters.city.TorchHead;
import com.megacrit.cardcrawl.monsters.ending.CorruptHeart;
import com.megacrit.cardcrawl.powers.FrailPower;
import javassist.CtBehavior;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Ascension Level 91: Additional pattern effects
 *
 * 적의 일부 패턴에 부가효과가 추가됩니다.
 *
 * 횃불 머리(Torch Head) : 태클 패턴이 손상을 1 부여합니다.
 * 타락한 심장(Corrupt Heart) : 쇠약 패턴에서 등반자의 골칫거리, 고통을 추가로 집어넣습니다.
 */
public class Level91 {
    private static final Logger logger = LogManager.getLogger(Level91.class.getName());

    /**
     * TorchHead: Tackle pattern applies Frail 1
     */
    @SpirePatch(
        clz = TorchHead.class,
        method = "takeTurn"
    )
    public static class TorchHeadTackleAppliesFrail {
        @SpirePostfixPatch
        public static void Postfix(TorchHead __instance) {
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 91) {
                return;
            }

            // Check if current move is TACKLE (byte 1)
            try {
                java.lang.reflect.Field nextMoveField = TorchHead.class.getSuperclass().getDeclaredField("nextMove");
                nextMoveField.setAccessible(true);
                byte nextMove = nextMoveField.getByte(__instance);

                if (nextMove == 1) { // TACKLE
                    AbstractDungeon.actionManager.addToBottom(
                        new ApplyPowerAction(
                            (AbstractCreature)AbstractDungeon.player,
                            (AbstractCreature)__instance,
                            new FrailPower((AbstractCreature)AbstractDungeon.player, 1, true),
                            1
                        )
                    );

                    logger.info("Ascension 91: TorchHead TACKLE applied Frail 1");
                }
            } catch (Exception e) {
                logger.error("Failed to modify TorchHead TACKLE pattern", e);
            }
        }
    }

    /**
     * CorruptHeart: Debilitate pattern adds AscendersBane, Writhe
     */
    @SpirePatch(
        clz = CorruptHeart.class,
        method = "takeTurn"
    )
    public static class CorruptHeartDebilitateAddsCurses {
        @SpirePostfixPatch
        public static void Postfix(CorruptHeart __instance) {
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 91) {
                return;
            }

            // Check if current move is DEBILITATE (byte 3)
            try {
                java.lang.reflect.Field nextMoveField = CorruptHeart.class.getSuperclass().getDeclaredField("nextMove");
                nextMoveField.setAccessible(true);
                byte nextMove = nextMoveField.getByte(__instance);

                if (nextMove == 3) { // DEBILITATE
                    // Add AscendersBane
                    AbstractDungeon.actionManager.addToBottom(
                        new MakeTempCardInDiscardAction(new AscendersBane(), 1)
                    );

                    // Add Writhe
                    AbstractDungeon.actionManager.addToBottom(
                        new MakeTempCardInDiscardAction(new Writhe(), 1)
                    );

                    logger.info("Ascension 91: CorruptHeart DEBILITATE added AscendersBane, Writhe");
                }
            } catch (Exception e) {
                logger.error("Failed to modify CorruptHeart DEBILITATE pattern", e);
            }
        }
    }
}
