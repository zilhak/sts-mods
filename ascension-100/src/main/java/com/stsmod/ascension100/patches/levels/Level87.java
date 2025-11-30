package com.stsmod.ascension100.patches.levels;

import com.evacipated.cardcrawl.modthespire.lib.SpireInsertPatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePrefixPatch;
import com.evacipated.cardcrawl.modthespire.lib.SpireReturn;
import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.actions.animations.VFXAction;
import com.megacrit.cardcrawl.actions.common.ApplyPowerAction;
import com.megacrit.cardcrawl.actions.common.DamageAction;
import com.megacrit.cardcrawl.actions.common.GainBlockAction;
import com.megacrit.cardcrawl.actions.common.HealAction;
import com.megacrit.cardcrawl.actions.common.MakeTempCardInDrawPileAction;
import com.megacrit.cardcrawl.actions.common.MakeTempCardInDiscardAction;
import com.megacrit.cardcrawl.actions.common.RemoveSpecificPowerAction;
import com.megacrit.cardcrawl.actions.common.RollMoveAction;
import com.megacrit.cardcrawl.actions.common.SetMoveAction;
import com.megacrit.cardcrawl.actions.common.SpawnMonsterAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.cards.DamageInfo;
import com.megacrit.cardcrawl.cards.status.VoidCard;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import com.megacrit.cardcrawl.monsters.MonsterGroup;
import com.megacrit.cardcrawl.monsters.beyond.Deca;
import com.megacrit.cardcrawl.monsters.beyond.Donu;
import com.megacrit.cardcrawl.monsters.beyond.AwakenedOne;
import com.megacrit.cardcrawl.monsters.beyond.TimeEater;
import com.megacrit.cardcrawl.monsters.city.BronzeAutomaton;
import com.megacrit.cardcrawl.monsters.city.BronzeOrb;
import com.megacrit.cardcrawl.monsters.city.Champ;
import com.megacrit.cardcrawl.monsters.city.Chosen;
import com.megacrit.cardcrawl.monsters.city.TheCollector;
import com.megacrit.cardcrawl.monsters.ending.CorruptHeart;
import com.megacrit.cardcrawl.monsters.exordium.Cultist;
import com.megacrit.cardcrawl.monsters.exordium.Hexaghost;
import com.megacrit.cardcrawl.monsters.exordium.SlimeBoss;
import com.megacrit.cardcrawl.monsters.exordium.TheGuardian;
import com.megacrit.cardcrawl.powers.AbstractPower;
import com.megacrit.cardcrawl.powers.BarricadePower;
import com.megacrit.cardcrawl.powers.MalleablePower;
import com.megacrit.cardcrawl.powers.ThornsPower;
import com.megacrit.cardcrawl.relics.AbstractRelic;
import com.megacrit.cardcrawl.vfx.combat.GoldenSlashEffect;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Ascension Level 87: Boss pattern enhancements
 *
 * 보스들의 패턴이 강화됩니다.
 *
 * 대왕 슬라임(Slime Boss): 강타(Slam) 패턴의 데미지가 40 증가합니다.
 * 수호자(The Guardian): 날카로운 껍질 대신 가시 5를 얻습니다. 기본적으로 바리케이드를 가집니다.
 * 육각령(Hexaghost): 첫턴에 30의 방어도를 얻습니다.
 * 청동 자동인형(Bronze Automaton): 구체 소환 패턴에서 청동 구체를 1기 추가로 소환합니다.
 * 투사(The Champ): 사형 집행(Execute) 패턴이 2회 공격에서 3회 공격으로 변경됩니다.
 * 수집가(The Collector): 메가 디버프(Mega Debuff) 패턴이 뽑을 카드 더미와 버린 카드 더미에 공허를 2장씩 섞어넣습니다.
 * 깨어난 자(Awakened One): 광신자(Cultist) 하나가 선택받은 자(Chosen)로 변경됩니다.
 * 시간 포식자(Time Eater): 잔물결(Ripple) 패턴에서 얻는 방어도가 180 증가합니다.
 * 타락한 심장(Corrupt Heart): 탄성(Malleable) 1을 추가로 얻습니다.
 * 도누와 데카(Donu and Deca): 생명 연결을 얻습니다.
 *   - 한쪽이 죽으면 반죽음 상태가 되고, 둘 다 죽어야 실제로 패배합니다.
 *   - 부활 시 죽을 때의 버프 상태를 복원합니다.
 */
public class Level87 {
    private static final Logger logger = LogManager.getLogger(Level87.class.getName());

    // Store powers when a boss enters half-dead state
    private static final Map<AbstractMonster, ArrayList<AbstractPower>> storedPowers = new HashMap<>();

    // ========================================
    // Slime Boss: Slam damage +40
    // ========================================

    /**
     * Increase Slime Boss Slam damage by 40
     */
    @SpirePatch(
        clz = SlimeBoss.class,
        method = SpirePatch.CONSTRUCTOR
    )
    public static class SlimeBossSlamDamageIncrease {
        @SpirePostfixPatch
        public static void Postfix(SlimeBoss __instance) {
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 87) {
                return;
            }

            try {
                // Access slamDmg field via reflection
                Field slamDmgField = SlimeBoss.class.getDeclaredField("slamDmg");
                slamDmgField.setAccessible(true);

                int originalSlamDmg = slamDmgField.getInt(__instance);
                int newSlamDmg = originalSlamDmg + 40;
                slamDmgField.setInt(__instance, newSlamDmg);

                // Update damage array (index 1 is Slam damage)
                if (__instance.damage.size() > 1) {
                    DamageInfo slamDamageInfo = __instance.damage.get(1);
                    slamDamageInfo.base = newSlamDmg;
                }

                logger.info(String.format(
                    "Ascension 87: Slime Boss Slam damage increased from %d to %d (+40)",
                    originalSlamDmg, newSlamDmg
                ));
            } catch (NoSuchFieldException | IllegalAccessException e) {
                logger.error("Ascension 87: Failed to modify Slime Boss Slam damage", e);
            }
        }
    }

    // ========================================
    // Guardian: Thorns instead of Sharp Hide, starts with Barricade
    // ========================================

    /**
     * Guardian starts with Barricade power
     */
    @SpirePatch(
        clz = TheGuardian.class,
        method = "usePreBattleAction"
    )
    public static class GuardianBarricade {
        @SpirePostfixPatch
        public static void Postfix(TheGuardian __instance) {
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 87) {
                return;
            }

            // Apply Barricade power
            AbstractDungeon.actionManager.addToBottom(
                new ApplyPowerAction(__instance, __instance,
                    new BarricadePower(__instance))
            );

            logger.info("Ascension 87: Guardian starts with Barricade");
        }
    }

    /**
     * Replace Sharp Hide with Thorns when Guardian uses Close Up (defensive mode)
     */
    @SpirePatch(
        clz = TheGuardian.class,
        method = "useCloseUp"
    )
    public static class GuardianThornsReplace {
        @SpirePostfixPatch
        public static void Postfix(TheGuardian __instance) {
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 87) {
                return;
            }

            // Remove Sharp Hide power
            AbstractDungeon.actionManager.addToBottom(
                new RemoveSpecificPowerAction(__instance, __instance, "Sharp Hide")
            );

            // Add Thorns 5 instead
            AbstractDungeon.actionManager.addToBottom(
                new ApplyPowerAction(__instance, __instance,
                    new ThornsPower(__instance, 5), 5)
            );

            logger.info("Ascension 87: Guardian gained Thorns 5 instead of Sharp Hide in defensive mode");
        }
    }

    /**
     * Remove Thorns when Guardian exits defensive mode (useTwinSmash transition)
     * This mirrors the vanilla behavior where Sharp Hide is removed in useTwinSmash
     */
    @SpirePatch(
        clz = TheGuardian.class,
        method = "useTwinSmash"
    )
    public static class GuardianRemoveThornsOnOffensive {
        @SpirePostfixPatch
        public static void Postfix(TheGuardian __instance) {
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 87) {
                return;
            }

            // Remove Thorns when exiting defensive mode
            // This happens after the offensive mode transition, just like Sharp Hide removal
            if (__instance.hasPower("Thorns")) {
                AbstractDungeon.actionManager.addToBottom(
                    new RemoveSpecificPowerAction(__instance, __instance, "Thorns")
                );
                logger.info("Ascension 87: Guardian removed Thorns when exiting defensive mode");
            }
        }
    }

    // ========================================
    // Hexaghost: Gains 30 block on first turn
    // ========================================

    /**
     * Hexaghost gains 30 block at the start of turn 1
     */
    @SpirePatch(
        clz = Hexaghost.class,
        method = "usePreBattleAction"
    )
    public static class HexaghostFirstTurnBlock {
        @SpirePostfixPatch
        public static void Postfix(Hexaghost __instance) {
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 87) {
                return;
            }

            // Apply 30 block at battle start
            AbstractDungeon.actionManager.addToBottom(
                new GainBlockAction(__instance, __instance, 30)
            );
            logger.info("Ascension 87: Hexaghost gained 30 block on first turn");
        }
    }

    // ========================================
    // Champ: Execute 3 hits instead of 2
    // ========================================

    /**
     * Champ Execute pattern changed from 2 hits to 3 hits
     * Intercepts setMove() call in getMove() and changes multiplier from 2 to 3
     */
    @SpirePatch(
        clz = AbstractMonster.class,
        method = "setMove",
        paramtypez = { String.class, byte.class, AbstractMonster.Intent.class, int.class, int.class, boolean.class }
    )
    public static class ChampExecuteIntentFix {
        @SpirePrefixPatch
        public static void Prefix(AbstractMonster __instance, String moveName, byte nextMove,
                                  AbstractMonster.Intent intent, int baseDamage, @com.evacipated.cardcrawl.modthespire.lib.ByRef int[] multiplier, boolean isMultiDamage) {
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 87) {
                return;
            }

            // Check if this is Champ's Execute move
            if (__instance instanceof Champ && nextMove == 3 && multiplier[0] == 2) {
                // Change multiplier from 2 to 3
                multiplier[0] = 3;
                logger.info("Ascension 87: Champ Execute intent multiplier changed from 2 to 3");
            }
        }
    }

    @SpirePatch(
        clz = Champ.class,
        method = "takeTurn"
    )
    public static class ChampExecuteThirdHit {
        private static final ThreadLocal<Byte> lastMove = new ThreadLocal<>();

        @SpirePrefixPatch
        public static void Prefix(Champ __instance) {
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 87) {
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
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 87) {
                return;
            }

            Byte move = lastMove.get();
            if (move != null && move == 3) { // EXECUTE move
                // Add third Execute hit
                float vfxSpeed = 0.2F;

                AbstractDungeon.actionManager.addToBottom(
                    new VFXAction(
                        new GoldenSlashEffect(
                            AbstractDungeon.player.hb.cX,
                            AbstractDungeon.player.hb.cY + 30.0F * Settings.scale,
                            true
                        ),
                        vfxSpeed
                    )
                );

                AbstractDungeon.actionManager.addToBottom(
                    new DamageAction(
                        AbstractDungeon.player,
                        __instance.damage.get(1),
                        AbstractGameAction.AttackEffect.NONE
                    )
                );

                logger.info("Ascension 87: Champ Execute gained third hit");
            }

            lastMove.remove();
        }
    }

    // ========================================
    // Collector: Mega Debuff adds Void cards
    // ========================================

    /**
     * Add Void cards to draw and discard piles when Collector uses Mega Debuff
     */
    @SpirePatch(
        clz = TheCollector.class,
        method = "takeTurn"
    )
    public static class CollectorMegaDebuffVoidCards {
        private static final ThreadLocal<Byte> lastMove = new ThreadLocal<>();

        @SpirePrefixPatch
        public static void Prefix(TheCollector __instance) {
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 87) {
                return;
            }

            try {
                java.lang.reflect.Field nextMoveField = AbstractMonster.class.getDeclaredField("nextMove");
                nextMoveField.setAccessible(true);
                byte move = nextMoveField.getByte(__instance);
                lastMove.set(move);
            } catch (Exception e) {
                logger.error("Failed to get Collector move", e);
            }
        }

        @SpirePostfixPatch
        public static void Postfix(TheCollector __instance) {
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 87) {
                return;
            }

            Byte move = lastMove.get();
            if (move != null && move == 4) { // MEGA_DEBUFF move
                // Add 2 Void cards to draw pile
                AbstractDungeon.actionManager.addToBottom(
                    new MakeTempCardInDrawPileAction(
                        (AbstractCard) new VoidCard(),
                        2,
                        true,
                        true
                    )
                );

                // Add 2 Void cards to discard pile
                AbstractDungeon.actionManager.addToBottom(
                    new MakeTempCardInDiscardAction(
                        (AbstractCard) new VoidCard(),
                        2
                    )
                );

                logger.info("Ascension 87: Collector Mega Debuff added 2 Void cards to draw pile and 2 to discard pile");
            }

            lastMove.remove();
        }
    }

    // ========================================
    // Donu and Deca: Life Link
    // ========================================

    /**
     * Mark that Life Link is enabled for Donu
     */
    @SpirePatch(
        clz = Donu.class,
        method = "usePreBattleAction"
    )
    public static class DonuLifeLink {
        @SpirePostfixPatch
        public static void Postfix(Donu __instance) {
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 87) {
                return;
            }

            logger.info("Ascension 87: Life Link enabled for Donu");
        }
    }

    /**
     * Mark that Life Link is enabled for Deca
     */
    @SpirePatch(
        clz = Deca.class,
        method = "usePreBattleAction"
    )
    public static class DecaLifeLink {
        @SpirePostfixPatch
        public static void Postfix(Deca __instance) {
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 87) {
                return;
            }

            logger.info("Ascension 87: Life Link enabled for Deca");
        }
    }

    /**
     * Enable cannotLose for Donu and Deca encounter
     * This prevents the battle from ending when one dies
     */
    @SpirePatch(
        clz = Donu.class,
        method = "usePreBattleAction"
    )
    public static class DonuEnableCannotLose {
        @SpirePostfixPatch
        public static void Postfix(Donu __instance) {
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 87) {
                return;
            }

            (AbstractDungeon.getCurrRoom()).cannotLose = true;
            logger.info("Ascension 87: Enabled cannotLose for Donu/Deca encounter");
        }
    }

    /**
     * Override Donu's damage() method to implement Life Link
     * Similar to Darkling's implementation
     */
    @SpirePatch(
        clz = Donu.class,
        method = "damage"
    )
    public static class DonuLifeLinkDamage {
        @SpirePostfixPatch
        public static void Postfix(Donu __instance, DamageInfo info) {
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 87) {
                return;
            }

            try {
                if (__instance.currentHealth <= 0 && !__instance.halfDead) {
                    __instance.halfDead = true;

                    // Trigger power and relic onDeath effects
                    for (AbstractPower p : __instance.powers) {
                        p.onDeath();
                    }
                    for (AbstractRelic r : AbstractDungeon.player.relics) {
                        r.onMonsterDeath(__instance);
                    }

                    // Store powers before clearing
                    ArrayList<AbstractPower> powersCopy = new ArrayList<>();
                    for (AbstractPower p : __instance.powers) {
                        powersCopy.add(p);
                    }
                    storedPowers.put(__instance, powersCopy);

                    // Clear powers
                    __instance.powers.clear();

                    logger.info("Ascension 87: Donu is now half dead");

                    // Check if Deca is also dead
                    AbstractMonster deca = null;
                    for (AbstractMonster m : (AbstractDungeon.getMonsters()).monsters) {
                        if (m.id.equals("Deca")) {
                            deca = m;
                            break;
                        }
                    }

                    boolean bothDead = (deca != null && deca.halfDead);

                    if (bothDead) {
                        // Both dead - allow battle to end
                        (AbstractDungeon.getCurrRoom()).cannotLose = false;
                        __instance.halfDead = false;
                        if (deca != null) {
                            deca.halfDead = false;
                        }

                        // Actually kill both
                        __instance.die();
                        if (deca != null) {
                            deca.die();
                        }

                        storedPowers.clear();
                        logger.info("Ascension 87: Both Donu and Deca are dead - ending battle");
                    } else {
                        // Set revival intent immediately (same as Darkling)
                        // Use UNKNOWN first, then BUFF on next turn via getMove()
                        __instance.setMove((byte)99, AbstractMonster.Intent.UNKNOWN);
                        __instance.createIntent();
                        AbstractDungeon.actionManager.addToBottom(
                            new SetMoveAction(__instance, (byte)99, AbstractMonster.Intent.UNKNOWN)
                        );
                        logger.info("Ascension 87: Donu half dead, set revival intent");
                    }
                }
            } catch (Exception e) {
                logger.error("Ascension 87: Error in Donu damage patch", e);
            }
        }
    }

    /**
     * Override Deca's damage() method to implement Life Link
     * Similar to Darkling's implementation
     */
    @SpirePatch(
        clz = Deca.class,
        method = "damage"
    )
    public static class DecaLifeLinkDamage {
        @SpirePostfixPatch
        public static void Postfix(Deca __instance, DamageInfo info) {
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 87) {
                return;
            }

            try {
                if (__instance.currentHealth <= 0 && !__instance.halfDead) {
                    __instance.halfDead = true;

                    // Trigger power and relic onDeath effects
                    for (AbstractPower p : __instance.powers) {
                        p.onDeath();
                    }
                    for (AbstractRelic r : AbstractDungeon.player.relics) {
                        r.onMonsterDeath(__instance);
                    }

                    // Store powers before clearing
                    ArrayList<AbstractPower> powersCopy = new ArrayList<>();
                    for (AbstractPower p : __instance.powers) {
                        powersCopy.add(p);
                    }
                    storedPowers.put(__instance, powersCopy);

                    // Clear powers
                    __instance.powers.clear();

                    logger.info("Ascension 87: Deca is now half dead");

                    // Check if Donu is also dead
                    AbstractMonster donu = null;
                    for (AbstractMonster m : (AbstractDungeon.getMonsters()).monsters) {
                        if (m.id.equals("Donu")) {
                            donu = m;
                            break;
                        }
                    }

                    boolean bothDead = (donu != null && donu.halfDead);

                    if (bothDead) {
                        // Both dead - allow battle to end
                        (AbstractDungeon.getCurrRoom()).cannotLose = false;
                        __instance.halfDead = false;
                        if (donu != null) {
                            donu.halfDead = false;
                        }

                        // Actually kill both
                        __instance.die();
                        if (donu != null) {
                            donu.die();
                        }

                        storedPowers.clear();
                        logger.info("Ascension 87: Both Donu and Deca are dead - ending battle");
                    } else {
                        // Set revival intent immediately (same as Darkling)
                        // Use UNKNOWN first, then BUFF on next turn via getMove()
                        __instance.setMove((byte)99, AbstractMonster.Intent.UNKNOWN);
                        __instance.createIntent();
                        AbstractDungeon.actionManager.addToBottom(
                            new SetMoveAction(__instance, (byte)99, AbstractMonster.Intent.UNKNOWN)
                        );
                        logger.info("Ascension 87: Deca half dead, set revival intent");
                    }
                }
            } catch (Exception e) {
                logger.error("Ascension 87: Error in Deca damage patch", e);
            }
        }
    }

    /**
     * Override die() to respect cannotLose flag, similar to Darkling
     * Only call super.die() if cannotLose is false
     */
    @SpirePatch(
        clz = Donu.class,
        method = "die"
    )
    public static class DonuCannotLoseDie {
        @SpirePrefixPatch
        public static SpireReturn<Void> Prefix(Donu __instance) {
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 87) {
                return SpireReturn.Continue();
            }

            // Only die if cannotLose is false (same logic as Darkling)
            if ((AbstractDungeon.getCurrRoom()).cannotLose) {
                logger.info("Ascension 87: Donu die() blocked by cannotLose");
                return SpireReturn.Return();
            }

            // Allow normal die() to proceed
            return SpireReturn.Continue();
        }
    }

    /**
     * Override die() to respect cannotLose flag, similar to Darkling
     * Only call super.die() if cannotLose is false
     */
    @SpirePatch(
        clz = Deca.class,
        method = "die"
    )
    public static class DecaCannotLoseDie {
        @SpirePrefixPatch
        public static SpireReturn<Void> Prefix(Deca __instance) {
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 87) {
                return SpireReturn.Continue();
            }

            // Only die if cannotLose is false (same logic as Darkling)
            if ((AbstractDungeon.getCurrRoom()).cannotLose) {
                logger.info("Ascension 87: Deca die() blocked by cannotLose");
                return SpireReturn.Return();
            }

            // Allow normal die() to proceed
            return SpireReturn.Continue();
        }
    }

    /**
     * Revive Donu at start of turn if half-dead
     * Use Prefix to intercept BEFORE switch statement
     */
    @SpirePatch(
        clz = Donu.class,
        method = "takeTurn"
    )
    public static class DonuRevive {
        @SpirePrefixPatch
        public static SpireReturn<Void> Prefix(Donu __instance) {
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 87) {
                return SpireReturn.Continue();
            }

            logger.info("Ascension 87: Donu takeTurn() called - checking halfDead status");
            logger.info("Ascension 87: Donu halfDead = " + __instance.halfDead);

            try {
                if (__instance.halfDead) {
                    // Revive with 50% HP
                    int healAmount = __instance.maxHealth / 2;

                    // Set currentHealth to 0 to trigger proper heal animation
                    __instance.currentHealth = 0;

                    AbstractDungeon.actionManager.addToBottom(
                        new HealAction((AbstractCreature)__instance, (AbstractCreature)__instance, healAmount)
                    );

                    // Restore stored powers
                    ArrayList<AbstractPower> storedPowerList = storedPowers.get(__instance);
                    if (storedPowerList != null) {
                        for (AbstractPower power : storedPowerList) {
                            AbstractDungeon.actionManager.addToBottom(
                                new ApplyPowerAction(
                                    (AbstractCreature)__instance,
                                    (AbstractCreature)__instance,
                                    power,
                                    power.amount
                                )
                            );
                        }
                        logger.info(String.format(
                            "Ascension 87: Restored %d powers to Donu",
                            storedPowerList.size()
                        ));
                    }

                    // Clear halfDead state
                    __instance.halfDead = false;
                    storedPowers.remove(__instance);

                    // Call RollMoveAction manually
                    // This will call getMove() which now sees halfDead = false
                    AbstractDungeon.actionManager.addToBottom(
                        new RollMoveAction(__instance)
                    );

                    logger.info(String.format(
                        "Ascension 87: Donu revived with %d HP",
                        healAmount
                    ));

                    // Block original takeTurn() since we handle everything here
                    return SpireReturn.Return();
                }
            } catch (Exception e) {
                logger.error("Ascension 87: Failed to revive Donu", e);
            }

            return SpireReturn.Continue();
        }
    }

    /**
     * Revive Deca at start of turn if half-dead
     * Use Prefix to intercept BEFORE switch statement
     */
    @SpirePatch(
        clz = Deca.class,
        method = "takeTurn"
    )
    public static class DecaRevive {
        @SpirePrefixPatch
        public static SpireReturn<Void> Prefix(Deca __instance) {
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 87) {
                return SpireReturn.Continue();
            }

            logger.info("Ascension 87: Deca takeTurn() called - checking halfDead status");
            logger.info("Ascension 87: Deca halfDead = " + __instance.halfDead);

            try {
                if (__instance.halfDead) {
                    // Revive with 50% HP
                    int healAmount = __instance.maxHealth / 2;

                    // Set currentHealth to 0 to trigger proper heal animation
                    __instance.currentHealth = 0;

                    AbstractDungeon.actionManager.addToBottom(
                        new HealAction((AbstractCreature)__instance, (AbstractCreature)__instance, healAmount)
                    );

                    // Restore stored powers
                    ArrayList<AbstractPower> storedPowerList = storedPowers.get(__instance);
                    if (storedPowerList != null) {
                        for (AbstractPower power : storedPowerList) {
                            AbstractDungeon.actionManager.addToBottom(
                                new ApplyPowerAction(
                                    (AbstractCreature)__instance,
                                    (AbstractCreature)__instance,
                                    power,
                                    power.amount
                                )
                            );
                        }
                        logger.info(String.format(
                            "Ascension 87: Restored %d powers to Deca",
                            storedPowerList.size()
                        ));
                    }

                    // Clear halfDead state
                    __instance.halfDead = false;
                    storedPowers.remove(__instance);

                    // Call RollMoveAction manually
                    // This will call getMove() which now sees halfDead = false
                    AbstractDungeon.actionManager.addToBottom(
                        new RollMoveAction(__instance)
                    );

                    logger.info(String.format(
                        "Ascension 87: Deca revived with %d HP",
                        healAmount
                    ));

                    // Block original takeTurn() since we handle everything here
                    return SpireReturn.Return();
                }
            } catch (Exception e) {
                logger.error("Ascension 87: Failed to revive Deca", e);
            }

            return SpireReturn.Continue();
        }
    }

    // ========================================
    // Time Eater: Ripple block +180
    // ========================================

    /**
     * Add +180 block when Time Eater uses Ripple pattern
     */
    @SpirePatch(
        clz = TimeEater.class,
        method = "takeTurn"
    )
    public static class TimeEaterRippleBlockBonus {
        private static final ThreadLocal<Byte> lastMove = new ThreadLocal<>();

        @SpirePrefixPatch
        public static void Prefix(TimeEater __instance) {
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 87) {
                return;
            }

            try {
                java.lang.reflect.Field nextMoveField = AbstractMonster.class.getDeclaredField("nextMove");
                nextMoveField.setAccessible(true);
                byte move = nextMoveField.getByte(__instance);
                lastMove.set(move);
            } catch (Exception e) {
                logger.error("Failed to get Time Eater move", e);
            }
        }

        @SpirePostfixPatch
        public static void Postfix(TimeEater __instance) {
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 87) {
                return;
            }

            Byte move = lastMove.get();
            if (move != null && move == 3) { // RIPPLE move
                // Add +180 additional block
                AbstractDungeon.actionManager.addToBottom(
                    new GainBlockAction(__instance, __instance, 180)
                );

                logger.info("Ascension 87: Time Eater Ripple gained +180 additional block");
            }

            lastMove.remove();
        }
    }

    // ========================================
    // Awakened One: Replace one Cultist with Chosen
    // ========================================

    /**
     * Replace one Cultist with Chosen in Awakened One encounter
     * IMPORTANT: Must be done in MonsterGroup.init() POSTFIX to avoid ConcurrentModificationException
     */
    @SpirePatch(
        clz = MonsterGroup.class,
        method = "init"
    )
    public static class AwakenedOneReplaceCultist {
        @SpirePostfixPatch
        public static void Postfix(MonsterGroup __instance) {
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 87) {
                return;
            }

            // Check if this is Awakened One encounter (has both AwakenedOne and Cultist)
            boolean hasAwakenedOne = false;
            boolean hasCultist = false;

            for (AbstractMonster m : __instance.monsters) {
                if (m instanceof AwakenedOne) {
                    hasAwakenedOne = true;
                }
                if (m instanceof Cultist) {
                    hasCultist = true;
                }
            }

            if (!hasAwakenedOne || !hasCultist) {
                return;
            }

            // Find and replace the first Cultist with Chosen
            // Original spawn positions from MonsterHelper.java:556:
            // new Cultist(-590.0F, 10.0F, false)  - left Cultist
            // new Cultist(-298.0F, -10.0F, false) - right Cultist
            // We replace the first one found (left Cultist) with Chosen at same position

            for (int i = 0; i < __instance.monsters.size(); i++) {
                AbstractMonster m = __instance.monsters.get(i);

                if (m instanceof Cultist) {
                    // Use original spawn coordinates for left Cultist
                    float x = -590.0F;
                    float y = 10.0F;

                    logger.info(String.format(
                        "Ascension 87: Replacing Cultist at index %d with Chosen at fixed position (%.1f, %.1f)",
                        i, x, y
                    ));

                    // Create and initialize Chosen at original Cultist position
                    Chosen chosen = new Chosen(x, y);
                    chosen.init();

                    // Replace Cultist with Chosen
                    __instance.monsters.set(i, chosen);

                    logger.info(String.format(
                        "Ascension 87: Successfully replaced Cultist with Chosen at (%.1f, %.1f)",
                        x, y
                    ));

                    // Only replace one Cultist
                    break;
                }
            }
        }
    }

    // ========================================
    // Bronze Automaton: Spawn +1 extra Bronze Orb
    // ========================================

    /**
     * Bronze Automaton spawns an additional Bronze Orb during SPAWN_ORBS pattern
     */
    @SpirePatch(
        clz = BronzeAutomaton.class,
        method = "takeTurn"
    )
    public static class BronzeAutomatonExtraOrb {
        @SpirePostfixPatch
        public static void Postfix(BronzeAutomaton __instance) {
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 87) {
                return;
            }

            try {
                // Access nextMove field via reflection
                Field nextMoveField = AbstractMonster.class.getDeclaredField("nextMove");
                nextMoveField.setAccessible(true);
                byte nextMove = nextMoveField.getByte(__instance);

                // SPAWN_ORBS pattern is move byte 4
                if (nextMove == 4) {
                    // Spawn third orb in center position (between the two default orbs)
                    // Default orbs: (-300.0F, 200.0F) and (200.0F, 130.0F)
                    // Third orb: center position
                    AbstractDungeon.actionManager.addToBottom(
                        new SpawnMonsterAction(new BronzeOrb(-50.0F, 165.0F, 2), true)
                    );

                    logger.info("Ascension 87: Bronze Automaton spawned third Bronze Orb");
                }
            } catch (NoSuchFieldException | IllegalAccessException e) {
                logger.error("Ascension 87: Failed to access BronzeAutomaton nextMove field", e);
            }
        }
    }

    // ========================================
    // Corrupt Heart: Malleable +1
    // ========================================

    /**
     * Corrupt Heart gains +1 Malleable at battle start
     */
    @SpirePatch(
        clz = CorruptHeart.class,
        method = "usePreBattleAction"
    )
    public static class CorruptHeartMalleable {
        @SpirePostfixPatch
        public static void Postfix(CorruptHeart __instance) {
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 87) {
                return;
            }

            // Apply Malleable 1
            AbstractDungeon.actionManager.addToBottom(
                new ApplyPowerAction(
                    (AbstractCreature)__instance,
                    (AbstractCreature)__instance,
                    new MalleablePower((AbstractCreature)__instance, 1),
                    1
                )
            );

            logger.info("Ascension 87: Corrupt Heart gained Malleable 1");
        }
    }
}
