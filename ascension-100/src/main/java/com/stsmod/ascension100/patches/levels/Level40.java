package com.stsmod.ascension100.patches.levels;

import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePrefixPatch;
import com.evacipated.cardcrawl.modthespire.lib.SpireReturn;
import com.megacrit.cardcrawl.actions.common.RollMoveAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.cards.CardGroup;
import com.megacrit.cardcrawl.cards.DamageInfo;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import com.megacrit.cardcrawl.monsters.beyond.WrithingMass;
import com.megacrit.cardcrawl.powers.AbstractPower;
import com.megacrit.cardcrawl.powers.ReactivePower;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.reflect.Method;

/**
 * Ascension Level 40: Enemy actions adapt to player situation
 * 적들의 행동이 때때로 당신의 상황에 맞춰 결정됩니다.
 *
 * 꿈틀대는 덩어리(Writhing Mass)의 반응 패턴이 플레이어의 손패 상황에 따라 변경됩니다:
 * - 손패에 공격카드가 없거나, 방어도가 20 이상이면 → 임플란트 패턴 강제
 * - 힘이 -2 보다 낮으면 → 다중타격 패턴 사용 안 함
 */
public class Level40 {
    private static final Logger logger = LogManager.getLogger(Level40.class.getName());

    /**
     * Patch WrithingMass ReactivePower to adapt pattern based on player state
     */
    @SpirePatch(
        clz = ReactivePower.class,
        method = "onAttacked"
    )
    public static class WrithingMassAdaptiveAI {
        @SpirePrefixPatch
        public static SpireReturn<Integer> Prefix(
            ReactivePower __instance,
            DamageInfo info,
            int damageAmount
        ) {
            // Only apply on ascension 40+
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 40) {
                return SpireReturn.Continue();
            }

            // Only apply to WrithingMass
            if (!(__instance.owner instanceof WrithingMass)) {
                return SpireReturn.Continue();
            }

            WrithingMass monster = (WrithingMass) __instance.owner;

            // Check original ReactivePower conditions
            if (info.owner != null &&
                info.type != DamageInfo.DamageType.HP_LOSS &&
                info.type != DamageInfo.DamageType.THORNS &&
                damageAmount > 0 &&
                damageAmount < monster.currentHealth) {

                __instance.flash();

                // Analyze player state
                boolean shouldUseImplant = checkImplantConditions(monster);
                boolean shouldAvoidMultiHit = checkAvoidMultiHit();

                int targetMove;

                if (shouldUseImplant) {
                    // Force MEGA_DEBUFF pattern (10-19 range)
                    targetMove = AbstractDungeon.aiRng.random(10, 19);
                    logger.info("[Asc40] WrithingMass: Forcing IMPLANT pattern");
                } else if (shouldAvoidMultiHit) {
                    // Avoid MULTI_HIT pattern (40-69 range)
                    // Use 0-39 or 70-99 instead
                    if (AbstractDungeon.aiRng.randomBoolean()) {
                        targetMove = AbstractDungeon.aiRng.random(0, 39);
                    } else {
                        targetMove = AbstractDungeon.aiRng.random(70, 99);
                    }
                    logger.info("[Asc40] WrithingMass: Avoiding MULTI_HIT pattern due to low strength");
                } else {
                    // Normal random
                    targetMove = AbstractDungeon.aiRng.random(0, 99);
                }

                // Call getMove with calculated number
                invokeGetMove(monster, targetMove);

                return SpireReturn.Return(damageAmount);
            }

            return SpireReturn.Continue();
        }

        /**
         * Check if IMPLANT pattern should be forced
         * Conditions:
         * 1. No attack cards in hand OR
         * 2. Monster has 20+ block (from Malleable power)
         */
        private static boolean checkImplantConditions(WrithingMass monster) {
            // Condition 1: No attack cards in hand
            CardGroup hand = AbstractDungeon.player.hand;
            int attackCount = 0;

            for (AbstractCard card : hand.group) {
                if (card.type == AbstractCard.CardType.ATTACK) {
                    attackCount++;
                }
            }

            boolean noAttackCards = (attackCount == 0);

            // Condition 2: High block (20+)
            boolean highBlock = (monster.currentBlock >= 20);

            if (noAttackCards) {
                logger.info(String.format(
                    "[Asc40] IMPLANT trigger: No attack cards in hand (hand size: %d)",
                    hand.group.size()
                ));
            }

            if (highBlock) {
                logger.info(String.format(
                    "[Asc40] IMPLANT trigger: High block (%d >= 20)",
                    monster.currentBlock
                ));
            }

            return noAttackCards || highBlock;
        }

        /**
         * Check if MULTI_HIT pattern should be avoided
         * Condition: Player strength < -2
         */
        private static boolean checkAvoidMultiHit() {
            AbstractPower strengthPower = AbstractDungeon.player.getPower("Strength");

            if (strengthPower != null && strengthPower.amount < -2) {
                logger.info(String.format(
                    "[Asc40] Avoiding MULTI_HIT: Player strength is %d (< -2)",
                    strengthPower.amount
                ));
                return true;
            }

            return false;
        }

        /**
         * Invoke getMove() using reflection
         */
        private static void invokeGetMove(AbstractMonster monster, int num) {
            try {
                Method getMove = monster.getClass()
                    .getDeclaredMethod("getMove", int.class);
                getMove.setAccessible(true);
                getMove.invoke(monster, num);

                logger.info(String.format(
                    "[Asc40] getMove(%d) invoked successfully",
                    num
                ));
            } catch (Exception e) {
                logger.error("[Asc40] Failed to invoke getMove: " + e.getMessage());
                // Fallback to original behavior
                AbstractDungeon.actionManager.addToBottom(
                    new RollMoveAction(monster)
                );
            }
        }
    }
}
