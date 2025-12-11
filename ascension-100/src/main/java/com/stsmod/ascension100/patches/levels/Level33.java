package com.stsmod.ascension100.patches.levels;

import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePrefixPatch;
import com.evacipated.cardcrawl.modthespire.lib.SpireReturn;
import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.actions.common.ApplyPowerAction;
import com.megacrit.cardcrawl.actions.common.DamageAction;
import com.megacrit.cardcrawl.actions.common.MakeTempCardInDiscardAction;
import com.megacrit.cardcrawl.actions.common.MakeTempCardInDrawPileAction;
import com.megacrit.cardcrawl.actions.common.RollMoveAction;
import com.megacrit.cardcrawl.actions.utility.SFXAction;
import com.megacrit.cardcrawl.actions.animations.VFXAction;
import com.megacrit.cardcrawl.cards.DamageInfo;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import com.megacrit.cardcrawl.monsters.beyond.GiantHead;
import com.megacrit.cardcrawl.monsters.beyond.Nemesis;
import com.megacrit.cardcrawl.monsters.beyond.Reptomancer;
import com.megacrit.cardcrawl.monsters.city.BookOfStabbing;
import com.megacrit.cardcrawl.monsters.city.GremlinLeader;
import com.megacrit.cardcrawl.monsters.city.Taskmaster;
import com.megacrit.cardcrawl.monsters.exordium.GremlinNob;
import com.megacrit.cardcrawl.monsters.exordium.Lagavulin;
import com.megacrit.cardcrawl.monsters.exordium.Sentry;
import com.megacrit.cardcrawl.monsters.exordium.SlaverBlue;
import com.megacrit.cardcrawl.monsters.exordium.SlaverRed;
import com.megacrit.cardcrawl.powers.*;
import com.megacrit.cardcrawl.rooms.MonsterRoomElite;
import com.megacrit.cardcrawl.vfx.combat.ShockWaveEffect;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Ascension Level 33: Burning Elites (Emerald Key Elites) enhancements
 *
 * 강화 엘리트가 더욱 강해집니다.
 *
 * 귀족 그렘린(Gremlin Nob): 외침 (Bellow) 패턴에서 격분(Tantrum)을 추가로 2 얻습니다.
 * 라가불린(Lagavulin): 수면시 금속화 수치가 3으로 감소합니다. 영혼 흡수 (Siphon Soul) 패턴이 힘과 민첩을 추가로 1 감소시킵니다. 밀집을 1 감소시킵니다.
 * 보초기(Sentry): 인공물 수치가 3 증가합니다.
 * 칼부림의 책(Book of Stabbing): 단일 찌르기 (Single Stab) 패턴이 약화를 1 부여합니다.
 * 그렘린 리더(Gremlin Leader): 격려 (Encourage) 패턴이 추가로 재생을 2 부여합니다.
 * 노예 관리자(Taskmaster): 모든 노예상인의 체력이 5% 증가합니다.
 * 거인의 머리(Giant Head): 매 턴 금속화를 2 얻습니다.
 * 네메시스(Nemesis): 디버프 (Debuff) 패턴에서 화상 대신 화상+를 집어넣습니다.
 * 파충류 주술사 (Reptomancer): 단검(Dagger)은 폭발 (Explode) 패턴에서 스스로 죽지 않습니다.
 */
public class Level33 {
    private static final Logger logger = LogManager.getLogger(Level33.class.getName());

    /**
     * Check if current room is a Burning Elite room
     */
    private static boolean isBurningElite() {
        return AbstractDungeon.getCurrRoom() instanceof MonsterRoomElite &&
               AbstractDungeon.getCurrMapNode() != null &&
               AbstractDungeon.getCurrMapNode().hasEmeraldKey;
    }

    /**
     * Gremlin Nob: Bellow pattern gives +2 additional Anger (Tantrum)
     * Case 3 in takeTurn is the Bellow pattern
     */
    @SpirePatch(
        clz = GremlinNob.class,
        method = "takeTurn"
    )
    public static class GremlinNobBellowPatch {
        @SpirePostfixPatch
        public static void Postfix(GremlinNob __instance) {
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 33) {
                return;
            }

            if (!isBurningElite()) {
                return;
            }

            // Check if Bellow pattern was just used (nextMove == 3)
            if (__instance.nextMove == 3) {
                // Add 2 additional Anger power
                AbstractDungeon.actionManager.addToBottom(
                    new ApplyPowerAction(__instance, __instance,
                        new AngerPower(__instance, 2), 2)
                );
                logger.info("Ascension 33: Burning Elite Gremlin Nob gained +2 Anger from Bellow");
            }
        }
    }

    /**
     * Lagavulin: When asleep, Metallicize reduced to 3
     * Siphon Soul reduces Strength and Dexterity by additional 1, reduces Plated Armor by 1
     */
    @SpirePatch(
        clz = Lagavulin.class,
        method = "takeTurn"
    )
    public static class LagavulinEnhancementPatch {
        @SpirePostfixPatch
        public static void Postfix(Lagavulin __instance) {
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 33) {
                return;
            }

            if (!isBurningElite()) {
                return;
            }

            // Siphon Soul is move 1 (DEBUFF)
            if (__instance.nextMove == 1) {
                // Additional -1 Strength, -1 Dexterity, -1 Focus to player
                AbstractDungeon.actionManager.addToBottom(
                    new ApplyPowerAction(AbstractDungeon.player, __instance,
                        new StrengthPower(AbstractDungeon.player, -1), -1)
                );
                AbstractDungeon.actionManager.addToBottom(
                    new ApplyPowerAction(AbstractDungeon.player, __instance,
                        new DexterityPower(AbstractDungeon.player, -1), -1)
                );
                AbstractDungeon.actionManager.addToBottom(
                    new ApplyPowerAction(AbstractDungeon.player, __instance,
                        new FocusPower(AbstractDungeon.player, -1), -1)
                );

                logger.info("Ascension 33: Burning Elite Lagavulin Siphon Soul enhanced - Str/Dex/Focus -1");
            }
        }
    }

    /**
     * Lagavulin: Reduce sleeping Metallicize to 3
     */
    @SpirePatch(
        clz = Lagavulin.class,
        method = "usePreBattleAction"
    )
    public static class LagavulinMetallicizePatch {
        @SpirePrefixPatch
        public static SpireReturn<Void> Prefix(Lagavulin __instance) {
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 33) {
                return SpireReturn.Continue();
            }

            if (!isBurningElite()) {
                return SpireReturn.Continue();
            }

            try {
                // Check if Lagavulin is asleep using reflection
                java.lang.reflect.Field asleepField = Lagavulin.class.getDeclaredField("asleep");
                asleepField.setAccessible(true);
                boolean asleep = asleepField.getBoolean(__instance);

                if (asleep) {
                    // Replace original logic: use Metallicize 3 instead of 8
                    com.megacrit.cardcrawl.core.CardCrawlGame.music.precacheTempBgm("ELITE");
                    AbstractDungeon.actionManager.addToBottom(
                        new com.megacrit.cardcrawl.actions.common.GainBlockAction(__instance, __instance, 8)
                    );
                    AbstractDungeon.actionManager.addToBottom(
                        new ApplyPowerAction(__instance, __instance,
                            new MetallicizePower(__instance, 3), 3)
                    );
                    logger.info("Ascension 33: Burning Elite Lagavulin starts with Metallicize 3 (reduced from 8)");
                    return SpireReturn.Return(null); // Skip original method
                }
            } catch (Exception e) {
                logger.error("Failed to patch Lagavulin Metallicize", e);
            }

            return SpireReturn.Continue(); // Not asleep or error, run original
        }
    }

    /**
     * Sentry: +3 Artifact
     */
    @SpirePatch(
        clz = Sentry.class,
        method = "usePreBattleAction"
    )
    public static class SentryArtifactPatch {
        @SpirePostfixPatch
        public static void Postfix(Sentry __instance) {
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 33) {
                return;
            }

            if (!isBurningElite()) {
                return;
            }

            // Add 3 additional Artifact (base game already adds 1)
            AbstractDungeon.actionManager.addToBottom(
                new ApplyPowerAction(__instance, __instance,
                    new ArtifactPower(__instance, 3), 3)
            );
            logger.info("Ascension 33: Burning Elite Sentry gained +3 Artifact");
        }
    }

    /**
     * Book of Stabbing: Big Stab applies Weak 1
     */
    @SpirePatch(
        clz = BookOfStabbing.class,
        method = "takeTurn"
    )
    public static class BookOfStabbingWeakPatch {
        @SpirePostfixPatch
        public static void Postfix(BookOfStabbing __instance) {
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 33) {
                return;
            }

            if (!isBurningElite()) {
                return;
            }

            // Big Stab is move 2
            if (__instance.nextMove == 2) {
                AbstractDungeon.actionManager.addToBottom(
                    new ApplyPowerAction(AbstractDungeon.player, __instance,
                        new WeakPower(AbstractDungeon.player, 1, true), 1)
                );
                logger.info("Ascension 33: Burning Elite Book of Stabbing Big Stab applied Weak");
            }
        }
    }

    /**
     * Book of Stabbing: Change Intent to ATTACK_DEBUFF when Big Stab applies Weak
     * Patches getMove to intercept setMove calls for move 2 (Big Stab)
     */
    @SpirePatch(
        clz = BookOfStabbing.class,
        method = "getMove"
    )
    public static class BookOfStabbingIntentPatch {
        @SpirePrefixPatch
        public static SpireReturn<Void> Prefix(BookOfStabbing __instance, int num) {
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 33) {
                return SpireReturn.Continue();
            }

            if (!isBurningElite()) {
                return SpireReturn.Continue();
            }

            // Replicate getMove logic but use ATTACK_DEBUFF for Big Stab (move 2)
            try {
                // Access private stabCount field
                java.lang.reflect.Field stabCountField = BookOfStabbing.class.getDeclaredField("stabCount");
                stabCountField.setAccessible(true);
                int stabCount = stabCountField.getInt(__instance);

                // Access protected lastMove method
                java.lang.reflect.Method lastMoveMethod = AbstractMonster.class.getDeclaredMethod("lastMove", byte.class);
                lastMoveMethod.setAccessible(true);

                // Access protected lastTwoMoves method
                java.lang.reflect.Method lastTwoMovesMethod = AbstractMonster.class.getDeclaredMethod("lastTwoMoves", byte.class);
                lastTwoMovesMethod.setAccessible(true);

                if (num < 15) {
                    boolean lastMoveWasTwo = (Boolean) lastMoveMethod.invoke(__instance, (byte)2);
                    if (lastMoveWasTwo) {
                        stabCount++;
                        stabCountField.setInt(__instance, stabCount);
                        __instance.setMove((byte)1, AbstractMonster.Intent.ATTACK,
                            __instance.damage.get(0).base, stabCount, true);
                    } else {
                        // Big Stab - use ATTACK_DEBUFF instead of ATTACK
                        __instance.setMove((byte)2, AbstractMonster.Intent.ATTACK_DEBUFF,
                            __instance.damage.get(1).base);
                        if (AbstractDungeon.ascensionLevel >= 18) {
                            stabCount++;
                            stabCountField.setInt(__instance, stabCount);
                        }
                        logger.info("Ascension 33: Burning Elite Book of Stabbing Big Stab Intent set to ATTACK_DEBUFF");
                    }
                } else {
                    boolean lastTwoMovesWereOne = (Boolean) lastTwoMovesMethod.invoke(__instance, (byte)1);
                    if (lastTwoMovesWereOne) {
                        // Big Stab - use ATTACK_DEBUFF instead of ATTACK
                        __instance.setMove((byte)2, AbstractMonster.Intent.ATTACK_DEBUFF,
                            __instance.damage.get(1).base);
                        if (AbstractDungeon.ascensionLevel >= 18) {
                            stabCount++;
                            stabCountField.setInt(__instance, stabCount);
                        }
                        logger.info("Ascension 33: Burning Elite Book of Stabbing Big Stab Intent set to ATTACK_DEBUFF");
                    } else {
                        stabCount++;
                        stabCountField.setInt(__instance, stabCount);
                        __instance.setMove((byte)1, AbstractMonster.Intent.ATTACK,
                            __instance.damage.get(0).base, stabCount, true);
                    }
                }

                return SpireReturn.Return(null); // Skip original getMove
            } catch (Exception e) {
                logger.error("Failed to patch Book of Stabbing Intent", e);
                return SpireReturn.Continue();
            }
        }
    }

    /**
     * Gremlin Leader: Encourage gives +2 Regeneration to minions
     */
    @SpirePatch(
        clz = GremlinLeader.class,
        method = "takeTurn"
    )
    public static class GremlinLeaderRegenPatch {
        @SpirePostfixPatch
        public static void Postfix(GremlinLeader __instance) {
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 33) {
                return;
            }

            if (!isBurningElite()) {
                return;
            }

            // Encourage is move 3
            if (__instance.nextMove == 3) {
                // Apply Regeneration to all monsters including itself
                for (AbstractMonster m : AbstractDungeon.getMonsters().monsters) {
                    if (!m.isDying && !m.isEscaping) {
                        AbstractDungeon.actionManager.addToBottom(
                            new ApplyPowerAction(m, __instance,
                                new RegenerateMonsterPower(m, 2), 2)
                        );
                    }
                }
                logger.info("Ascension 33: Burning Elite Gremlin Leader Encourage gave +2 Regeneration to all monsters");
            }
        }
    }

    /**
     * Taskmaster: All Slavers gain +5% HP
     * Patch AbstractMonster.init() to modify Slaver HP in Burning Elite encounters
     */
    @SpirePatch(
        clz = AbstractMonster.class,
        method = "init"
    )
    public static class SlaverHPPatch {
        @SpirePostfixPatch
        public static void Postfix(AbstractMonster __instance) {
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 33) {
                return;
            }

            // Check if this is a Slaver in a Burning Elite encounter
            if (__instance instanceof SlaverBlue || __instance instanceof SlaverRed) {
                if (!isBurningElite()) {
                    return;
                }

                // Increase HP by 5%
                int originalHP = __instance.maxHealth;
                __instance.maxHealth = (int)(__instance.maxHealth * 1.05f);
                __instance.currentHealth = (int)(__instance.currentHealth * 1.05f);
                logger.info(String.format(
                    "Ascension 33: Burning Elite %s HP %d -> %d (+5%%)",
                    __instance.name, originalHP, __instance.maxHealth
                ));
            }
        }
    }

    /**
     * Giant Head: Gains 2 Metallicize per turn
     */
    @SpirePatch(
        clz = GiantHead.class,
        method = "takeTurn"
    )
    public static class GiantHeadMetallicizePatch {
        @SpirePostfixPatch
        public static void Postfix(GiantHead __instance) {
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 33) {
                return;
            }

            if (!isBurningElite()) {
                return;
            }

            // Gain 2 Metallicize every turn
            AbstractDungeon.actionManager.addToBottom(
                new ApplyPowerAction(__instance, __instance,
                    new MetallicizePower(__instance, 2), 2)
            );
            logger.info("Ascension 33: Burning Elite Giant Head gained +2 Metallicize");
        }
    }

    /**
     * Nemesis: Debuff pattern adds Burn+ instead of Burn
     */
    @SpirePatch(
        clz = Nemesis.class,
        method = "takeTurn"
    )
    public static class NemesisBurnPlusPatch {
        @SpirePrefixPatch
        public static SpireReturn<Void> Prefix(Nemesis __instance) {
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 33) {
                return SpireReturn.Continue();
            }

            if (!isBurningElite()) {
                return SpireReturn.Continue();
            }

            // Check if Debuff pattern is being used (nextMove == 4)
            if (__instance.nextMove == 4) {
                // Override the entire takeTurn for case 4 to use Burn+ instead
                AbstractDungeon.actionManager.addToBottom(new SFXAction("VO_NEMESIS_1C"));
                AbstractDungeon.actionManager.addToBottom(
                    new VFXAction(__instance,
                        new ShockWaveEffect(__instance.hb.cX, __instance.hb.cY,
                            Settings.GREEN_TEXT_COLOR, ShockWaveEffect.ShockWaveType.CHAOTIC),
                        1.5F)
                );

                // Add regular Burn cards to draw pile
                int burnCount = (AbstractDungeon.ascensionLevel >= 18) ? 5 : 3;

                // Create one Burn card and add to draw pile (not upgraded, and in draw pile instead of discard)
                com.megacrit.cardcrawl.cards.status.Burn burn = new com.megacrit.cardcrawl.cards.status.Burn();
                AbstractDungeon.actionManager.addToBottom(
                    new MakeTempCardInDrawPileAction(burn, burnCount, true, true)
                );

                // Add Intangible if not already present (from base game logic)
                if (!__instance.hasPower("Intangible")) {
                    AbstractDungeon.actionManager.addToBottom(
                        new ApplyPowerAction(__instance, __instance,
                            new IntangiblePower(__instance, 1))
                    );
                }

                // Roll next move
                AbstractDungeon.actionManager.addToBottom(new RollMoveAction(__instance));

                logger.info("Ascension 33: Burning Elite Nemesis added " + burnCount + " Burn cards to draw pile");

                // Return early to skip the base game's takeTurn logic for this move
                return SpireReturn.Return(null);
            }

            return SpireReturn.Continue();
        }
    }

    /**
     * Reptomancer Dagger: Prevent suicide from Explode pattern
     */
    @SpirePatch(
        clz = com.megacrit.cardcrawl.monsters.beyond.SnakeDagger.class,
        method = "takeTurn"
    )
    public static class ReptomancerDaggerExplodePatch {
        @SpirePrefixPatch
        public static SpireReturn<Void> Prefix(com.megacrit.cardcrawl.monsters.beyond.SnakeDagger __instance) {
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 33) {
                return SpireReturn.Continue();
            }

            if (!isBurningElite()) {
                return SpireReturn.Continue();
            }

            // Check if Explode pattern is being used (nextMove == 2)
            if (__instance.nextMove == 2) {
                // Override the entire takeTurn for case 2 to prevent suicide
                // Use "ATTACK" animation instead of "SUICIDE" to prevent transparency
                AbstractDungeon.actionManager.addToBottom(
                    new com.megacrit.cardcrawl.actions.common.ChangeStateAction(__instance, "ATTACK")
                );
                AbstractDungeon.actionManager.addToBottom(
                    new com.megacrit.cardcrawl.actions.utility.WaitAction(0.3F)
                );
                AbstractDungeon.actionManager.addToBottom(
                    new DamageAction(AbstractDungeon.player,
                        __instance.damage.get(1),
                        AbstractGameAction.AttackEffect.SLASH_HEAVY)
                );
                // NOTE: Removed the LoseHPAction that would kill the dagger
                // This allows the dagger to survive the Explode pattern

                // Roll next move
                AbstractDungeon.actionManager.addToBottom(new RollMoveAction(__instance));

                logger.info("Ascension 33: Burning Elite Dagger Explode - prevented suicide");

                // Return early to skip the base game's takeTurn logic for this move
                return SpireReturn.Return(null);
            }

            return SpireReturn.Continue();
        }
    }
}
