package com.stsmod.ascension100.patches.levels;

import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePrefixPatch;
import com.megacrit.cardcrawl.actions.common.MakeTempCardInDiscardAction;
import com.megacrit.cardcrawl.actions.common.HealAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.cards.DamageInfo;
import com.megacrit.cardcrawl.cards.status.Burn;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import com.megacrit.cardcrawl.monsters.exordium.Hexaghost;
import com.megacrit.cardcrawl.monsters.exordium.SlimeBoss;
import com.megacrit.cardcrawl.monsters.exordium.TheGuardian;
import com.megacrit.cardcrawl.monsters.city.BronzeAutomaton;
import com.megacrit.cardcrawl.monsters.city.BronzeOrb;
import com.megacrit.cardcrawl.monsters.city.Champ;
import com.megacrit.cardcrawl.monsters.city.TheCollector;
import com.megacrit.cardcrawl.monsters.beyond.AwakenedOne;
import com.megacrit.cardcrawl.monsters.beyond.Deca;
import com.megacrit.cardcrawl.monsters.beyond.TimeEater;
import com.megacrit.cardcrawl.monsters.ending.CorruptHeart;
import com.megacrit.cardcrawl.powers.AbstractPower;
import com.megacrit.cardcrawl.powers.ArtifactPower;
import com.megacrit.cardcrawl.powers.MetallicizePower;
import com.megacrit.cardcrawl.powers.PlatedArmorPower;
import com.megacrit.cardcrawl.powers.SharpHidePower;
import com.megacrit.cardcrawl.actions.common.ApplyPowerAction;
import com.megacrit.cardcrawl.actions.common.SpawnMonsterAction;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Ascension Level 54: Boss patterns enhanced
 *
 * 보스의 패턴이 강화됩니다.
 *
 * 1. Slime Boss: 강타 패턴 데미지 +17
 * 2. Guardian: 수비모드 날카로운 껍질 +1
 * 3. Hexaghost: 지옥염 패턴 화상 +2장
 * 4. Bronze Automaton: 청동 구체 소환시 인공물 +2
 * 5. The Champ: 분노 패턴 체력 10% 회복
 * 6. The Collector: 횃불 머리 소환 방식 변경 (둘이 되도록 → 두마리 소환)
 * 7. Awakened One: 1페이즈 체력 +25
 * 8. Time Eater: 잔물결(Ripple) 패턴에서 약화 +1, 손상 +1 (총 2턴씩)
 * 9. Donu and Deca: 데카 버프 판금 갑옷 +1
 * 10. Corrupt Heart: 금속화 +20
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

            // Add +1 Sharp Hide after useCloseUp applies base Sharp Hide
            AbstractDungeon.actionManager.addToBottom(
                new ApplyPowerAction(__instance, __instance,
                    new SharpHidePower(__instance, 1), 1)
            );

            logger.info("Ascension 54: Guardian gained +1 additional Sharp Hide in defensive mode");
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

    /**
     * Slime Boss: 강타(Slam) 패턴 데미지 +17
     */
    @SpirePatch(
        clz = SlimeBoss.class,
        method = SpirePatch.CONSTRUCTOR,
        paramtypez = {}
    )
    public static class SlimeBossSlamDamageIncrease {
        @SpirePostfixPatch
        public static void Postfix(SlimeBoss __instance) {
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 54) {
                return;
            }

            // Increase all damage values by 17
            for (DamageInfo damageInfo : __instance.damage) {
                if (damageInfo != null && damageInfo.base > 0) {
                    damageInfo.base += 17;
                }
            }

            logger.info("Ascension 54: Slime Boss Slam damage increased by +17");
        }
    }

    /**
     * Bronze Automaton: 청동 구체 소환시 인공물 +2
     */
    @SpirePatch(
        clz = BronzeOrb.class,
        method = SpirePatch.CONSTRUCTOR
    )
    public static class BronzeOrbArtifactBonus {
        @SpirePostfixPatch
        public static void Postfix(BronzeOrb __instance, float x, float y, int count) {
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 54) {
                return;
            }

            // Add 2 Artifact to Bronze Orb
            AbstractDungeon.actionManager.addToBottom(
                new ApplyPowerAction(__instance, __instance,
                    new ArtifactPower(__instance, 2), 2)
            );

            logger.info("Ascension 54: Bronze Orb spawned with +2 Artifact");
        }
    }

    /**
     * The Champ: 분노(Anger) 패턴 체력 10% 회복
     */
    @SpirePatch(
        clz = Champ.class,
        method = "takeTurn"
    )
    public static class ChampAngerHeal {
        private static final ThreadLocal<Byte> lastMove = new ThreadLocal<>();

        @SpirePrefixPatch
        public static void Prefix(Champ __instance) {
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 54) {
                return;
            }

            try {
                java.lang.reflect.Field nextMoveField = AbstractMonster.class.getDeclaredField("nextMove");
                nextMoveField.setAccessible(true);
                byte move = nextMoveField.getByte(__instance);
                lastMove.set(move);
            } catch (Exception e) {
                logger.error("Failed to get Champ move", e);
            }
        }

        @SpirePostfixPatch
        public static void Postfix(Champ __instance) {
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 54) {
                return;
            }

            Byte move = lastMove.get();
            // Move 3 is the Anger pattern
            if (move != null && move == 3) {
                int healAmount = (int) (__instance.maxHealth * 0.10f);
                AbstractDungeon.actionManager.addToBottom(
                    new HealAction(__instance, __instance, healAmount)
                );

                logger.info(String.format(
                    "Ascension 54: Champ Anger pattern healed for %d (10%% max HP)",
                    healAmount
                ));
            }

            lastMove.remove();
        }
    }

    /**
     * The Collector: 횃불 머리 소환 패턴 변경 (둘이 되도록 → 두마리 소환)
     */
    @SpirePatch(
        clz = TheCollector.class,
        method = "takeTurn"
    )
    public static class CollectorTorchHeadSpawn {
        private static final ThreadLocal<Byte> lastMove = new ThreadLocal<>();
        private static final ThreadLocal<Integer> torchCount = new ThreadLocal<>();

        @SpirePrefixPatch
        public static void Prefix(TheCollector __instance) {
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 54) {
                return;
            }

            try {
                java.lang.reflect.Field nextMoveField = AbstractMonster.class.getDeclaredField("nextMove");
                nextMoveField.setAccessible(true);
                byte move = nextMoveField.getByte(__instance);
                lastMove.set(move);

                // Count current TorchHeads
                int count = 0;
                for (AbstractMonster m : AbstractDungeon.getCurrRoom().monsters.monsters) {
                    if (m.id != null && m.id.equals("TorchHead") && !m.isDying && !m.isDead) {
                        count++;
                    }
                }
                torchCount.set(count);
            } catch (Exception e) {
                logger.error("Failed to get Collector move", e);
            }
        }

        @SpirePostfixPatch
        public static void Postfix(TheCollector __instance) {
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 54) {
                return;
            }

            Byte move = lastMove.get();
            Integer currentTorchCount = torchCount.get();

            // Move 2 is summon TorchHead pattern
            if (move != null && move == 2 && currentTorchCount != null && currentTorchCount == 1) {
                // Spawn one additional TorchHead (base game spawns 1, we add 1 more)
                float xPos = -370.0f;
                AbstractDungeon.actionManager.addToBottom(
                    new SpawnMonsterAction(
                        new com.megacrit.cardcrawl.monsters.city.TorchHead(xPos, 0.0f),
                        true
                    )
                );

                logger.info("Ascension 54: Collector spawned additional TorchHead (2 total instead of making it 2)");
            }

            lastMove.remove();
            torchCount.remove();
        }
    }

    /**
     * Time Eater: Ripple 패턴에서 약화(Weak) +1, 손상(Frail) +1 추가
     *
     * 원본 게임 Ripple 패턴 (move 3):
     * - 방어도 20
     * - 취약(Vulnerable) 1턴
     * - 약화(Weak) 1턴
     * - (A19+) 손상(Frail) 1턴
     *
     * Level 54 추가 효과:
     * - 약화(Weak) +1 (총 2턴)
     * - 손상(Frail) +1 (총 2턴, A19+ 기준)
     */
    @SpirePatch(
        clz = TimeEater.class,
        method = "takeTurn"
    )
    public static class TimeEaterRippleDebuffIncrease {
        private static final ThreadLocal<Byte> lastMove = new ThreadLocal<>();

        @SpirePrefixPatch
        public static void Prefix(TimeEater __instance) {
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 54) {
                return;
            }

            try {
                java.lang.reflect.Field nextMoveField = AbstractMonster.class.getDeclaredField("nextMove");
                nextMoveField.setAccessible(true);
                byte move = nextMoveField.getByte(__instance);
                lastMove.set(move);
            } catch (Exception e) {
                logger.error("Failed to get TimeEater move", e);
            }
        }

        @SpirePostfixPatch
        public static void Postfix(TimeEater __instance) {
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 54) {
                return;
            }

            Byte move = lastMove.get();
            // Move 3 is Ripple pattern
            if (move != null && move == 3) {
                // Add +1 Weak (base game applies 1, so total becomes 2)
                AbstractDungeon.actionManager.addToBottom(
                    new ApplyPowerAction(
                        AbstractDungeon.player,
                        __instance,
                        new com.megacrit.cardcrawl.powers.WeakPower(AbstractDungeon.player, 1, true),
                        1
                    )
                );

                // Add +1 Frail (base game applies 1 at A19+, so total becomes 2)
                AbstractDungeon.actionManager.addToBottom(
                    new ApplyPowerAction(
                        AbstractDungeon.player,
                        __instance,
                        new com.megacrit.cardcrawl.powers.FrailPower(AbstractDungeon.player, 1, true),
                        1
                    )
                );

                logger.info("Ascension 54: Time Eater Ripple pattern added +1 Weak and +1 Frail (total 2 each)");
            }

            lastMove.remove();
        }
    }

    /**
     * Deca: 버프 패턴에서 판금 갑옷 +1
     */
    @SpirePatch(
        clz = Deca.class,
        method = "takeTurn"
    )
    public static class DecaPlatedArmorBonus {
        private static final ThreadLocal<Byte> lastMove = new ThreadLocal<>();

        @SpirePrefixPatch
        public static void Prefix(Deca __instance) {
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 54) {
                return;
            }

            try {
                java.lang.reflect.Field nextMoveField = AbstractMonster.class.getDeclaredField("nextMove");
                nextMoveField.setAccessible(true);
                byte move = nextMoveField.getByte(__instance);
                lastMove.set(move);
            } catch (Exception e) {
                logger.error("Failed to get Deca move", e);
            }
        }

        @SpirePostfixPatch
        public static void Postfix(Deca __instance) {
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 54) {
                return;
            }

            Byte move = lastMove.get();
            // Move 1 is buff pattern
            if (move != null && move == 1) {
                AbstractDungeon.actionManager.addToBottom(
                    new ApplyPowerAction(__instance, __instance,
                        new PlatedArmorPower(__instance, 1), 1)
                );

                logger.info("Ascension 54: Deca buff pattern added +1 Plated Armor");
            }

            lastMove.remove();
        }
    }

    /**
     * Corrupt Heart: 금속화 +20
     */
    @SpirePatch(
        clz = CorruptHeart.class,
        method = "usePreBattleAction"
    )
    public static class CorruptHeartMetallicize {
        @SpirePostfixPatch
        public static void Postfix(CorruptHeart __instance) {
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 54) {
                return;
            }

            AbstractDungeon.actionManager.addToBottom(
                new ApplyPowerAction(__instance, __instance,
                    new MetallicizePower(__instance, 20), 20)
            );

            logger.info("Ascension 54: Corrupt Heart gained +20 Metallicize");
        }
    }
}
