package com.stsmod.ascension100.patches.levels;

import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;
import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.actions.common.ApplyPowerAction;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import com.megacrit.cardcrawl.monsters.beyond.Darkling;
import com.megacrit.cardcrawl.monsters.beyond.SpireGrowth;
import com.megacrit.cardcrawl.monsters.city.BanditLeader;
import com.megacrit.cardcrawl.powers.ConstrictedPower;
import com.megacrit.cardcrawl.powers.VulnerablePower;
import com.megacrit.cardcrawl.powers.WeakPower;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Ascension Level 71: Some enemy attacks have additional effects
 *
 * 일부 적의 공격이 부가적인 효과를 갖습니다.
 *
 * 로미오(Romeo/BanditLeader) 의 십자 베기 패턴은 취약을 1 부여합니다.
 * 어두미(Darkling) 의 물고 늘어지기 패턴은 약화를 1 부여합니다.
 * 첨탑 암종(SpireGrowth)의 신속 태클 (Quick Tackle) 패턴은 이미 포박이 있는 상태에서 포박을 2 추가로 부여합니다.
 */
public class Level71 {
    private static final Logger logger = LogManager.getLogger(Level71.class.getName());

    /**
     * Patch BanditLeader (Romeo) - Cross Slash adds Vulnerable
     * Cross Slash is move byte 1 (CROSS_SLASH)
     */
    @SpirePatch(
        clz = BanditLeader.class,
        method = "takeTurn"
    )
    public static class RomeoCrossSlashVulnerable {
        @SpirePostfixPatch
        public static void Postfix(BanditLeader __instance) {
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 71) {
                return;
            }

            // Check if this is Cross Slash move (byte 1)
            // Cross Slash: damage.get(0), SLASH_DIAGONAL attack
            // We check nextMove which is set by getMove()
            byte nextMove = __instance.nextMove;

            // CROSS_SLASH = 1
            if (nextMove == 1) {
                // Add Vulnerable 1 to player when Cross Slash is used
                AbstractDungeon.actionManager.addToBottom(
                    new ApplyPowerAction(
                        (AbstractCreature)AbstractDungeon.player,
                        (AbstractCreature)__instance,
                        new VulnerablePower((AbstractCreature)AbstractDungeon.player, 1, true),
                        1
                    )
                );

                logger.info("Ascension 71: Romeo's Cross Slash applied Vulnerable 1");
            }
        }
    }

    /**
     * Patch Darkling - Chomp adds Weak
     * Chomp is move byte 1 (CHOMP)
     */
    @SpirePatch(
        clz = Darkling.class,
        method = "takeTurn"
    )
    public static class DarklingChompWeak {
        @SpirePostfixPatch
        public static void Postfix(Darkling __instance) {
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 71) {
                return;
            }

            byte nextMove = __instance.nextMove;

            // CHOMP = 1
            if (nextMove == 1) {
                // Add Weak 1 to player when Chomp is used
                AbstractDungeon.actionManager.addToBottom(
                    new ApplyPowerAction(
                        (AbstractCreature)AbstractDungeon.player,
                        (AbstractCreature)__instance,
                        new WeakPower((AbstractCreature)AbstractDungeon.player, 1, true),
                        1
                    )
                );

                logger.info("Ascension 71: Darkling's Chomp applied Weak 1");
            }
        }
    }

    /**
     * Patch SpireGrowth - Quick Tackle adds Constricted if already constricted
     * Quick Tackle is move byte 1 (QUICK_TACKLE)
     */
    @SpirePatch(
        clz = SpireGrowth.class,
        method = "takeTurn"
    )
    public static class SpireGrowthQuickTackleConstrict {
        @SpirePostfixPatch
        public static void Postfix(SpireGrowth __instance) {
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 71) {
                return;
            }

            byte nextMove = __instance.nextMove;

            // QUICK_TACKLE = 1
            if (nextMove == 1) {
                // If player already has Constricted, add 2 more
                if (AbstractDungeon.player.hasPower("Constricted")) {
                    AbstractDungeon.actionManager.addToBottom(
                        new ApplyPowerAction(
                            (AbstractCreature)AbstractDungeon.player,
                            (AbstractCreature)__instance,
                            new ConstrictedPower((AbstractCreature)AbstractDungeon.player, (AbstractCreature)__instance, 2),
                            2
                        )
                    );

                    logger.info("Ascension 71: SpireGrowth's Quick Tackle applied Constricted 2 (player already constricted)");
                }
            }
        }
    }
}
