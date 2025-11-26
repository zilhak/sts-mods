package com.stsmod.ascension100.patches.levels;

import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePrefixPatch;
import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.actions.animations.VFXAction;
import com.megacrit.cardcrawl.actions.common.ApplyPowerAction;
import com.megacrit.cardcrawl.actions.common.DamageAction;
import com.megacrit.cardcrawl.actions.common.GainBlockAction;
import com.megacrit.cardcrawl.actions.common.HealAction;
import com.megacrit.cardcrawl.actions.common.MakeTempCardInDrawPileAction;
import com.megacrit.cardcrawl.actions.common.MakeTempCardInDiscardAction;
import com.megacrit.cardcrawl.actions.common.RemoveSpecificPowerAction;
import com.megacrit.cardcrawl.actions.common.SpawnMonsterAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.cards.DamageInfo;
import com.megacrit.cardcrawl.cards.status.VoidCard;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
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
import com.megacrit.cardcrawl.powers.RegrowPower;
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
     * Remove Thorns when Guardian exits defensive mode (rollMove transition)
     */
    @SpirePatch(
        clz = TheGuardian.class,
        method = "rollMove"
    )
    public static class GuardianRemoveThornsOnOffensive {
        @SpirePostfixPatch
        public static void Postfix(TheGuardian __instance) {
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 87) {
                return;
            }

            try {
                // Check if Guardian is switching to offensive mode
                Field closeDefenseField = TheGuardian.class.getDeclaredField("closeDefense");
                closeDefenseField.setAccessible(true);
                boolean isClosedDefense = closeDefenseField.getBoolean(__instance);

                // If not in defensive mode, remove Thorns (equivalent to Sharp Hide removal)
                if (!isClosedDefense && __instance.hasPower("Thorns")) {
                    AbstractDungeon.actionManager.addToBottom(
                        new RemoveSpecificPowerAction(__instance, __instance, "Thorns")
                    );
                    logger.info("Ascension 87: Guardian removed Thorns when exiting defensive mode");
                }
            } catch (NoSuchFieldException | IllegalAccessException e) {
                logger.error("Ascension 87: Failed to check Guardian defensive mode", e);
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
     * Add third Execute hit when Champ uses Execute pattern
     */
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
     * Apply Life Link (RegrowPower) to both Donu and Deca at battle start
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

            // Enable cannotLose for the room
            (AbstractDungeon.getCurrRoom()).cannotLose = true;

            // Apply Life Link power
            AbstractDungeon.actionManager.addToBottom(
                new ApplyPowerAction(
                    (AbstractCreature)__instance,
                    (AbstractCreature)__instance,
                    new RegrowPower((AbstractCreature)__instance)
                )
            );

            logger.info("Ascension 87: Applied Life Link to Donu");
        }
    }

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

            // Apply Life Link power
            AbstractDungeon.actionManager.addToBottom(
                new ApplyPowerAction(
                    (AbstractCreature)__instance,
                    (AbstractCreature)__instance,
                    new RegrowPower((AbstractCreature)__instance)
                )
            );

            logger.info("Ascension 87: Applied Life Link to Deca");
        }
    }

    /**
     * Handle Donu's death - enter half-dead state and store powers
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

            handleLifeLinkDamage(__instance, "Donu");
        }
    }

    /**
     * Handle Deca's death - enter half-dead state and store powers
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

            handleLifeLinkDamage(__instance, "Deca");
        }
    }

    /**
     * Common Life Link damage handling for both bosses
     */
    private static void handleLifeLinkDamage(AbstractMonster instance, String bossName) {
        try {
            // Access halfDead field via reflection
            Field halfDeadField = AbstractMonster.class.getDeclaredField("halfDead");
            halfDeadField.setAccessible(true);
            boolean halfDead = halfDeadField.getBoolean(instance);

            if (instance.currentHealth <= 0 && !halfDead) {
                // Set halfDead state
                halfDeadField.setBoolean(instance, true);

                // Store current powers (make a deep copy)
                ArrayList<AbstractPower> powersCopy = new ArrayList<>();
                for (AbstractPower p : instance.powers) {
                    // Trigger onDeath for powers
                    p.onDeath();

                    // Store power for later restoration (skip RegrowPower)
                    if (!p.ID.equals("Life Link")) {
                        powersCopy.add(p);
                    }
                }
                storedPowers.put(instance, powersCopy);

                // Trigger relic effects
                for (AbstractRelic r : AbstractDungeon.player.relics) {
                    r.onMonsterDeath(instance);
                }

                // Clear powers
                instance.powers.clear();

                logger.info(String.format(
                    "Ascension 87: %s entered half-dead state. Stored %d powers.",
                    bossName, powersCopy.size()
                ));

                // Check if both are half-dead
                AbstractMonster donu = null;
                AbstractMonster deca = null;

                for (AbstractMonster m : (AbstractDungeon.getMonsters()).monsters) {
                    if (m.id.equals("Donu")) {
                        donu = m;
                    } else if (m.id.equals("Deca")) {
                        deca = m;
                    }
                }

                boolean bothDead = false;
                if (donu != null && deca != null) {
                    boolean donuHalfDead = halfDeadField.getBoolean(donu);
                    boolean decaHalfDead = halfDeadField.getBoolean(deca);
                    bothDead = donuHalfDead && decaHalfDead;
                }

                if (bothDead) {
                    // Both are dead - allow room to end
                    (AbstractDungeon.getCurrRoom()).cannotLose = false;

                    // Reset halfDead and actually kill both
                    if (donu != null) {
                        halfDeadField.setBoolean(donu, false);
                        donu.die();
                    }
                    if (deca != null) {
                        halfDeadField.setBoolean(deca, false);
                        deca.die();
                    }

                    // Clear stored powers
                    storedPowers.clear();

                    logger.info("Ascension 87: Both Donu and Deca are dead. Ending battle.");
                } else {
                    // One is still alive - revive the dead one on their next turn
                    logger.info(String.format(
                        "Ascension 87: %s is half-dead but partner is alive. Will revive.",
                        bossName
                    ));
                }
            }
        } catch (NoSuchFieldException | IllegalAccessException e) {
            logger.error("Ascension 87: Failed to access halfDead field", e);
        }
    }

    /**
     * Revive Donu at start of turn if half-dead
     */
    @SpirePatch(
        clz = Donu.class,
        method = "takeTurn"
    )
    public static class DonuRevive {
        @SpirePostfixPatch
        public static void Postfix(Donu __instance) {
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 87) {
                return;
            }

            reviveIfHalfDead(__instance, "Donu");
        }
    }

    /**
     * Revive Deca at start of turn if half-dead
     */
    @SpirePatch(
        clz = Deca.class,
        method = "takeTurn"
    )
    public static class DecaRevive {
        @SpirePostfixPatch
        public static void Postfix(Deca __instance) {
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 87) {
                return;
            }

            reviveIfHalfDead(__instance, "Deca");
        }
    }

    /**
     * Common revival logic for both bosses
     */
    private static void reviveIfHalfDead(AbstractMonster instance, String bossName) {
        try {
            Field halfDeadField = AbstractMonster.class.getDeclaredField("halfDead");
            halfDeadField.setAccessible(true);
            boolean halfDead = halfDeadField.getBoolean(instance);

            if (halfDead) {
                // Revive with 50% HP
                int healAmount = instance.maxHealth / 2;
                AbstractDungeon.actionManager.addToBottom(
                    new HealAction((AbstractCreature)instance, (AbstractCreature)instance, healAmount)
                );

                // Restore Life Link
                AbstractDungeon.actionManager.addToBottom(
                    new ApplyPowerAction(
                        (AbstractCreature)instance,
                        (AbstractCreature)instance,
                        new RegrowPower((AbstractCreature)instance),
                        1
                    )
                );

                // Restore stored powers
                ArrayList<AbstractPower> storedPowerList = storedPowers.get(instance);
                if (storedPowerList != null) {
                    for (AbstractPower power : storedPowerList) {
                        AbstractDungeon.actionManager.addToBottom(
                            new ApplyPowerAction(
                                (AbstractCreature)instance,
                                (AbstractCreature)instance,
                                power,
                                power.amount
                            )
                        );
                    }
                    logger.info(String.format(
                        "Ascension 87: Restored %d powers to %s",
                        storedPowerList.size(), bossName
                    ));
                }

                // Trigger relic effects
                for (AbstractRelic r : AbstractDungeon.player.relics) {
                    r.onSpawnMonster(instance);
                }

                // Clear halfDead state
                halfDeadField.setBoolean(instance, false);

                logger.info(String.format(
                    "Ascension 87: %s revived with %d HP",
                    bossName, healAmount
                ));
            }
        } catch (NoSuchFieldException | IllegalAccessException e) {
            logger.error("Ascension 87: Failed to revive " + bossName, e);
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
     */
    @SpirePatch(
        clz = AwakenedOne.class,
        method = "usePreBattleAction"
    )
    public static class AwakenedOneReplaceCultist {
        @SpirePostfixPatch
        public static void Postfix(AwakenedOne __instance) {
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 87) {
                return;
            }

            // Find and replace the first Cultist with Chosen
            for (int i = 0; i < (AbstractDungeon.getMonsters()).monsters.size(); i++) {
                AbstractMonster m = (AbstractDungeon.getMonsters()).monsters.get(i);

                if (m instanceof Cultist) {
                    // Store Cultist position
                    float x = m.drawX;
                    float y = m.drawY;

                    // Remove Cultist
                    (AbstractDungeon.getMonsters()).monsters.remove(i);

                    // Add Chosen at same position
                    Chosen chosen = new Chosen(x, y);
                    (AbstractDungeon.getMonsters()).monsters.add(i, chosen);

                    logger.info(String.format(
                        "Ascension 87: Replaced Cultist at (%.1f, %.1f) with Chosen",
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
