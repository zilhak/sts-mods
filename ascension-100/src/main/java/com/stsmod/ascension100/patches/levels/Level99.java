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
 * 10~19: 전투 승리시 20% 확률로 골드를 얻지 못함
 * 20~29: 각 층을 오를때, 30% 확률로 최대체력이 1 감소
 * 30~39: 회복시 1 덜 회복
 * 40~49: 전투 보상에 물약이 포함될시, 50% 확률로 물약 보상이 제거
 * 50~59: 10% 확률로 엘리트가 유물을 주지 않음
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

        public static boolean isEffect4Active() {
            return randomValue >= 30 && randomValue <= 39;
        }

        public static boolean isEffect5Active() {
            return randomValue >= 40 && randomValue <= 49;
        }

        public static boolean isEffect6Active() {
            return randomValue >= 50 && randomValue <= 59;
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

            // Effect 2 (10~19): 20% chance to not gain gold
            if (UnfairEventTracker.isEffect2Active()) {
                int roll = AbstractDungeon.miscRng.random(0, 99);
                if (roll < 20) {
                    int originalGold = goldAmount[0];
                    goldAmount[0] = 0;

                    logger.info(String.format(
                        "Ascension 99 (Effect 2): Combat gold blocked! (Roll: %d < 20, Original: %d)",
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
     * Effect 4: Heal 1 less when healing
     */
    @SpirePatch(
        clz = AbstractPlayer.class,
        method = "heal",
        paramtypez = {int.class, boolean.class}
    )
    public static class ReduceHealing {
        @SpirePrefixPatch
        public static void Prefix(AbstractPlayer __instance, @ByRef int[] healAmount) {
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 99) {
                return;
            }

            // Effect 4 (30~39): Heal 1 less
            if (UnfairEventTracker.isEffect4Active() && healAmount[0] > 0) {
                int original = healAmount[0];
                healAmount[0] = Math.max(0, healAmount[0] - 1);

                logger.info(String.format(
                    "Ascension 99 (Effect 4): Heal reduced from %d to %d (-1)",
                    original, healAmount[0]
                ));
            }
        }
    }

    /**
     * Effect 5: 50% chance to remove potion rewards from combat
     */
    @SpirePatch(
        clz = AbstractDungeon.class,
        method = "getCurrRoom"
    )
    public static class RemovePotionReward {
        @SpirePostfixPatch
        public static void Postfix() {
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 99) {
                return;
            }

            // Effect 5 (40~49): 50% chance to remove potion from rewards
            if (UnfairEventTracker.isEffect5Active() && AbstractDungeon.getCurrRoom() != null) {
                if (AbstractDungeon.getCurrRoom().rewards != null) {
                    // Check after combat rewards are generated
                    for (int i = AbstractDungeon.getCurrRoom().rewards.size() - 1; i >= 0; i--) {
                        RewardItem reward = AbstractDungeon.getCurrRoom().rewards.get(i);
                        if (reward.type == RewardItem.RewardType.POTION) {
                            int roll = AbstractDungeon.miscRng.random(0, 99);
                            if (roll < 50) {
                                AbstractDungeon.getCurrRoom().rewards.remove(i);
                                logger.info(String.format(
                                    "Ascension 99 (Effect 5): Potion reward removed (Roll: %d < 50)",
                                    roll
                                ));
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Effect 6: 10% chance for elites to not give relic
     */
    @SpirePatch(
        clz = AbstractDungeon.class,
        method = "getCurrRoom"
    )
    public static class BlockEliteRelic {
        @SpirePostfixPatch
        public static void Postfix() {
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 99) {
                return;
            }

            // Effect 6 (50~59): 10% chance for elite to not give relic
            if (UnfairEventTracker.isEffect6Active() && AbstractDungeon.getCurrRoom() != null) {
                if (AbstractDungeon.getCurrRoom().rewards != null) {
                    // Check after combat rewards are generated
                    for (int i = AbstractDungeon.getCurrRoom().rewards.size() - 1; i >= 0; i--) {
                        RewardItem reward = AbstractDungeon.getCurrRoom().rewards.get(i);
                        if (reward.type == RewardItem.RewardType.RELIC) {
                            int roll = AbstractDungeon.miscRng.random(0, 99);
                            if (roll < 10) {
                                AbstractDungeon.getCurrRoom().rewards.remove(i);
                                logger.info(String.format(
                                    "Ascension 99 (Effect 6): Elite relic reward removed (Roll: %d < 10)",
                                    roll
                                ));
                            }
                        }
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
