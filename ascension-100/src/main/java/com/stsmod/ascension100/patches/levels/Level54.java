package com.stsmod.ascension100.patches.levels;

import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePrefixPatch;
import com.megacrit.cardcrawl.actions.common.MakeTempCardInDiscardAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.cards.status.Burn;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import com.megacrit.cardcrawl.monsters.exordium.Hexaghost;
import com.megacrit.cardcrawl.monsters.exordium.TheGuardian;
import com.megacrit.cardcrawl.monsters.beyond.AwakenedOne;
import com.megacrit.cardcrawl.powers.AbstractPower;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Ascension Level 54: Some boss patterns enhanced
 *
 * 보스의 일부 패턴이 강화됩니다.
 *
 * Guardian: 수비모드에서 날카로운 껍질이 1 증가합니다.
 * - Base: 3
 * - A19+: 4
 * - A54+: 5
 *
 * Hexaghost: 지옥염 패턴에서 버린 카드더미에 화상을 추가로 2장 넣습니다.
 * - INFERNO move (6): Adds 2 upgraded Burns to discard pile
 *
 * Awakened One: 1페이즈의 체력이 25 증가합니다.
 */
public class Level54 {
    private static final Logger logger = LogManager.getLogger(Level54.class.getName());

    /**
     * Patch Guardian's defensive mode Sharp Hide power
     * Increase by 1 when ascension level >= 54
     */
    @SpirePatch(
        clz = TheGuardian.class,
        method = "useCloseUp"
    )
    public static class GuardianSharpHideIncrease {
        @SpirePostfixPatch
        public static void Postfix(TheGuardian __instance) {
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 54) {
                return;
            }

            // Find and increase Sharp Hide power by 1
            AbstractPower sharpHide = __instance.getPower("Sharp Hide");
            if (sharpHide != null) {
                int originalAmount = sharpHide.amount;
                sharpHide.amount += 1;
                sharpHide.updateDescription();

                logger.info(String.format(
                    "Ascension 54: Guardian Sharp Hide increased from %d to %d in defensive mode",
                    originalAmount, sharpHide.amount
                ));
            }
        }
    }

    /**
     * Hexaghost: Inferno pattern adds 2 extra Burns to discard pile
     * Base game: INFERNO move (6) adds 3 upgraded Burns via BurnIncreaseAction
     * A54+: Additional 2 upgraded Burns to discard pile
     */
    @SpirePatch(
        clz = Hexaghost.class,
        method = "takeTurn"
    )
    public static class HexaghostInfernoExtraBurns {
        private static final ThreadLocal<Byte> lastMove = new ThreadLocal<>();

        @SpirePrefixPatch
        public static void Prefix(Hexaghost __instance) {
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 54) {
                return;
            }

            try {
                java.lang.reflect.Field nextMoveField = AbstractMonster.class.getDeclaredField("nextMove");
                nextMoveField.setAccessible(true);
                byte move = nextMoveField.getByte(__instance);
                lastMove.set(move);
            } catch (Exception e) {
                logger.error("Failed to get Hexaghost move", e);
            }
        }

        @SpirePostfixPatch
        public static void Postfix(Hexaghost __instance) {
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 54) {
                return;
            }

            Byte move = lastMove.get();
            if (move != null && move == 6) { // INFERNO move
                // Add 2 upgraded Burns to discard pile
                Burn burn1 = new Burn();
                burn1.upgrade();
                AbstractDungeon.actionManager.addToBottom(
                    new MakeTempCardInDiscardAction((AbstractCard) burn1, 1)
                );

                Burn burn2 = new Burn();
                burn2.upgrade();
                AbstractDungeon.actionManager.addToBottom(
                    new MakeTempCardInDiscardAction((AbstractCard) burn2, 1)
                );

                logger.info("Ascension 54: Hexaghost Inferno added 2 extra Burns to discard pile");
            }

            lastMove.remove();
        }
    }

    /**
     * Awakened One: Phase 1 HP +25
     */
    @SpirePatch(
        clz = AwakenedOne.class,
        method = SpirePatch.CONSTRUCTOR
    )
    public static class AwakenedOnePhase1HPIncrease {
        @SpirePostfixPatch
        public static void Postfix(AwakenedOne __instance) {
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 54) {
                return;
            }

            // Phase 1 HP +25
            int originalHP = __instance.maxHealth;
            __instance.maxHealth += 25;
            __instance.currentHealth += 25;

            logger.info(String.format(
                "Ascension 54: AwakenedOne Phase 1 HP increased from %d to %d (+25)",
                originalHP,
                __instance.maxHealth
            ));
        }
    }
}
