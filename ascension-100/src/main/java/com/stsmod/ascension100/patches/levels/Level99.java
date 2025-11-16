package com.stsmod.ascension100.patches.levels;

import com.badlogic.gdx.math.MathUtils;
import com.evacipated.cardcrawl.modthespire.lib.*;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.map.MapRoomNode;
import com.megacrit.cardcrawl.rewards.RewardItem;
import javassist.CtBehavior;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Ascension Level 99: Unfair events occur
 *
 * 불합리한 사건이 일어납니다.
 *
 * 게임 시작시 0~99 랜덤값 선택 후 해당 값에 따라 효과 발생:
 * 0~9: 시작시 30%의 체력을 잃은 채로 시작
 * 10~19: 전투 승리시 30% 확률로 골드를 얻지 못함
 * 20~29: 각 층을 오를때, 30% 확률로 최대체력이 1 감소
 * 30~99: 효과 없음
 */
public class Level99 {
    private static final Logger logger = LogManager.getLogger(Level99.class.getName());

    /**
     * Tracker for Level 99 unfair events
     */
    public static class UnfairEventTracker {
        public static int randomValue = -1;
        public static boolean isInitialized = false;

        public static void initialize() {
            if (!isInitialized) {
                randomValue = AbstractDungeon.miscRng.random(0, 99);
                isInitialized = true;
                logger.info(String.format("Ascension 99: Unfair event initialized with random value: %d", randomValue));
            }
        }

        public static void reset() {
            randomValue = -1;
            isInitialized = false;
        }

        public static boolean isEffect1Active() {
            return randomValue >= 0 && randomValue <= 9;
        }

        public static boolean isEffect2Active() {
            return randomValue >= 10 && randomValue <= 19;
        }

        public static boolean isEffect3Active() {
            return randomValue >= 20 && randomValue <= 29;
        }
    }

    /**
     * Initialize random value and apply Effect 1: Lose 30% HP at start
     */
    @SpirePatch(
        clz = AbstractPlayer.class,
        method = "initializeClass"
    )
    public static class GameStartHPLoss {
        @SpirePostfixPatch
        public static void Postfix(AbstractPlayer __instance) {
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 99) {
                return;
            }

            // Initialize random value
            UnfairEventTracker.initialize();

            // Effect 1 (0~9): Lose 30% HP at start
            if (UnfairEventTracker.isEffect1Active()) {
                int hpLoss = MathUtils.ceil(__instance.maxHealth * 0.3f);
                __instance.currentHealth = Math.max(1, __instance.currentHealth - hpLoss);

                logger.info(String.format(
                    "Ascension 99 (Effect 1): Starting HP reduced by %d (30%%) - Current: %d/%d",
                    hpLoss, __instance.currentHealth, __instance.maxHealth
                ));
            }
        }
    }

    /**
     * Effect 2: 30% chance to not gain gold from combat
     */
    @SpirePatch(
        clz = RewardItem.class,
        method = SpirePatch.CONSTRUCTOR,
        paramtypez = {int.class}
    )
    public static class BlockGoldReward {
        @SpireInsertPatch(
            locator = GoldConstructorLocator.class
        )
        public static void Insert(RewardItem __instance, @ByRef int[] goldAmount) {
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 99) {
                return;
            }

            // Effect 2 (10~19): 30% chance to not gain gold
            if (UnfairEventTracker.isEffect2Active()) {
                int roll = AbstractDungeon.miscRng.random(0, 99);
                if (roll < 30) {
                    int originalGold = goldAmount[0];
                    goldAmount[0] = 0;

                    logger.info(String.format(
                        "Ascension 99 (Effect 2): Combat gold blocked! (Roll: %d < 30, Original: %d)",
                        roll, originalGold
                    ));
                }
            }
        }

        private static class GoldConstructorLocator extends SpireInsertLocator {
            @Override
            public int[] Locate(CtBehavior ctBehavior) throws Exception {
                Matcher finalMatcher = new Matcher.FieldAccessMatcher(RewardItem.class, "goldAmt");
                return LineFinder.findInOrder(ctBehavior, finalMatcher);
            }
        }
    }

    /**
     * Effect 3: 30% chance to lose 1 max HP when moving to next floor
     */
    @SpirePatch(
        clz = AbstractDungeon.class,
        method = "setCurrMapNode"
    )
    public static class FloorTransitionMaxHPLoss {
        @SpirePostfixPatch
        public static void Postfix(MapRoomNode node) {
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 99) {
                return;
            }

            // Effect 3 (20~29): 30% chance to lose 1 max HP when moving floor
            if (UnfairEventTracker.isEffect3Active()) {
                // Only trigger when actually moving to a new floor (not initial map generation)
                if (AbstractDungeon.player != null && node != null) {
                    int roll = AbstractDungeon.miscRng.random(0, 99);
                    if (roll < 30) {
                        AbstractDungeon.player.decreaseMaxHealth(1);

                        logger.info(String.format(
                            "Ascension 99 (Effect 3): Max HP decreased by 1 when moving floor (Roll: %d < 30, Max HP: %d)",
                            roll, AbstractDungeon.player.maxHealth
                        ));
                    }
                }
            }
        }
    }

    /**
     * Reset tracker when new game starts
     */
    @SpirePatch(
        clz = CardCrawlGame.class,
        method = "create"
    )
    public static class ResetTracker {
        @SpirePostfixPatch
        public static void Postfix(CardCrawlGame __instance) {
            UnfairEventTracker.reset();
        }
    }
}
