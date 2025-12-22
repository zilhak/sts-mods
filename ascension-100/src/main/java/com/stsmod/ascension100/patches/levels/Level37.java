package com.stsmod.ascension100.patches.levels;

import com.badlogic.gdx.math.MathUtils;
import com.evacipated.cardcrawl.modthespire.lib.*;
import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.actions.common.ApplyPowerAction;
import com.megacrit.cardcrawl.actions.common.HealAction;
import com.megacrit.cardcrawl.actions.common.RemoveAllBlockAction;
import com.megacrit.cardcrawl.actions.common.RemoveSpecificPowerAction;
import com.megacrit.cardcrawl.actions.common.RollMoveAction;
import com.megacrit.cardcrawl.actions.common.SetMoveAction;
import com.megacrit.cardcrawl.cards.DamageInfo;
import com.megacrit.cardcrawl.relics.AbstractRelic;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import com.megacrit.cardcrawl.monsters.beyond.Deca;
import com.megacrit.cardcrawl.monsters.beyond.Donu;
import com.megacrit.cardcrawl.monsters.beyond.TimeEater;
import com.megacrit.cardcrawl.monsters.city.BronzeAutomaton;
import com.megacrit.cardcrawl.monsters.city.BronzeOrb;
import com.megacrit.cardcrawl.monsters.city.Champ;
import com.megacrit.cardcrawl.monsters.city.TorchHead;
import com.megacrit.cardcrawl.monsters.exordium.Cultist;
import com.megacrit.cardcrawl.monsters.exordium.Hexaghost;
import com.megacrit.cardcrawl.monsters.exordium.SlimeBoss;
import com.megacrit.cardcrawl.monsters.exordium.TheGuardian;
import com.megacrit.cardcrawl.powers.*;
import com.megacrit.cardcrawl.powers.MalleablePower;
import javassist.CtBehavior;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.reflect.Field;
import java.util.ArrayList;

/**
 * Ascension Level 37: Boss ability adjustments
 * 보스의 능력치가 조정됩니다.
 */
public class Level37 {
    private static final Logger logger = LogManager.getLogger(Level37.class.getName());

    // ============= SLIME BOSS =============

    @SpirePatch(clz = SlimeBoss.class, method = SpirePatch.CONSTRUCTOR)
    public static class SlimeBossStatsPatch {
        @SpirePostfixPatch
        public static void Postfix(SlimeBoss __instance) throws Exception {
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 37) {
                return;
            }

            // HP +10
            int originalMaxHP = __instance.maxHealth;
            __instance.maxHealth += 10;
            __instance.currentHealth += 10;

            logger.info(String.format(
                "Ascension 37: Slime Boss HP increased from %d to %d (+10)",
                originalMaxHP, __instance.maxHealth
            ));

            // Damage -12
            Field tackleDmgField = SlimeBoss.class.getDeclaredField("tackleDmg");
            tackleDmgField.setAccessible(true);
            int tackleDmg = tackleDmgField.getInt(__instance);
            tackleDmgField.setInt(__instance, Math.max(1, tackleDmg - 12));

            Field slamDmgField = SlimeBoss.class.getDeclaredField("slamDmg");
            slamDmgField.setAccessible(true);
            int slamDmg = slamDmgField.getInt(__instance);
            slamDmgField.setInt(__instance, Math.max(1, slamDmg - 12));

            // Update damage info
            if (__instance.damage.size() >= 2) {
                __instance.damage.get(0).base = Math.max(1, __instance.damage.get(0).base - 12);
                __instance.damage.get(1).base = Math.max(1, __instance.damage.get(1).base - 12);
            }

            logger.info("Ascension 37: Slime Boss damage decreased by 12");
        }
    }

    @SpirePatch(clz = SlimeBoss.class, method = "takeTurn")
    public static class SlimeBossVulnerablePatch {
        @SpirePostfixPatch
        public static void Postfix(SlimeBoss __instance) {
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 37) {
                return;
            }

            if (__instance.nextMove == 4) { // STICKY = byte 4
                AbstractDungeon.actionManager.addToBottom(new ApplyPowerAction(
                    AbstractDungeon.player, __instance,
                    new VulnerablePower(AbstractDungeon.player, 2, true), 2
                ));

                logger.info("Ascension 37: Slime Boss Goop Spray applies Vulnerable 2");
            }
        }
    }

    // ============= THE GUARDIAN =============

    @SpirePatch(clz = TheGuardian.class, method = SpirePatch.CONSTRUCTOR)
    public static class GuardianStatsPatch {
        @SpirePostfixPatch
        public static void Postfix(TheGuardian __instance) throws Exception {
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 37) {
                return;
            }

            // HP +10
            int originalMaxHP = __instance.maxHealth;
            __instance.maxHealth += 10;
            __instance.currentHealth += 10;

            logger.info(String.format(
                "Ascension 37: Guardian HP increased from %d to %d (+10)",
                originalMaxHP, __instance.maxHealth
            ));

            // Mode shift increase: 10 → 15
            Field dmgThresholdIncreaseField = TheGuardian.class.getDeclaredField("dmgThresholdIncrease");
            dmgThresholdIncreaseField.setAccessible(true);
            dmgThresholdIncreaseField.setInt(__instance, 15);

            logger.info("Ascension 37: Guardian mode shift increase changed from 10 to 15");
        }
    }

    @SpirePatch(clz = TheGuardian.class, method = "usePreBattleAction")
    public static class GuardianBarricadePatch {
        @SpirePostfixPatch
        public static void Postfix(TheGuardian __instance) {
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 37) {
                return;
            }

            AbstractDungeon.actionManager.addToBottom(new ApplyPowerAction(
                __instance, __instance,
                new BarricadePower(__instance)
            ));

            logger.info("Ascension 37: Guardian gains Barricade");
        }
    }

    // ============= HEXAGHOST =============

    @SpirePatch(clz = Hexaghost.class, method = SpirePatch.CONSTRUCTOR)
    public static class HexaghostStatsPatch {
        @SpirePostfixPatch
        public static void Postfix(Hexaghost __instance) throws Exception {
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 37) {
                return;
            }

            // HP +16
            int originalMaxHP = __instance.maxHealth;
            __instance.maxHealth += 16;
            __instance.currentHealth += 16;

            logger.info(String.format(
                "Ascension 37: Hexaghost HP increased from %d to %d (+16)",
                originalMaxHP, __instance.maxHealth
            ));

            // Inferno damage +1
            Field infernoDmgField = Hexaghost.class.getDeclaredField("infernoDmg");
            infernoDmgField.setAccessible(true);
            int infernoDmg = infernoDmgField.getInt(__instance);
            infernoDmgField.setInt(__instance, infernoDmg + 1);

            // Update damage info (damage[3] is inferno damage)
            if (__instance.damage.size() > 3) {
                __instance.damage.get(3).base += 1;
            }

            logger.info("Ascension 37: Hexaghost Inferno damage +1");
        }
    }

    // ============= BRONZE ORB =============

    @SpirePatch(clz = BronzeOrb.class, method = SpirePatch.CONSTRUCTOR)
    public static class BronzeOrbHPPatch {
        @SpirePostfixPatch
        public static void Postfix(BronzeOrb __instance) {
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 37) {
                return;
            }

            int originalMaxHP = __instance.maxHealth;
            __instance.maxHealth = MathUtils.ceil(__instance.maxHealth * 1.30f);
            __instance.currentHealth = MathUtils.ceil(__instance.currentHealth * 1.30f);

            logger.info(String.format(
                "Ascension 37: Bronze Orb HP increased from %d to %d (+30%%)",
                originalMaxHP, __instance.maxHealth
            ));
        }
    }

    @SpirePatch(clz = BronzeOrb.class, method = SpirePatch.CONSTRUCTOR, paramtypez = {float.class, float.class, int.class})
    public static class BronzeOrbDamagePatch {
        @SpirePostfixPatch
        public static void Postfix(BronzeOrb __instance, float x, float y, int count) {
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 37) {
                return;
            }

            // Reduce Bronze Orb damage by 2
            if (__instance.damage != null && !__instance.damage.isEmpty()) {
                int originalDmg = __instance.damage.get(0).base;
                __instance.damage.get(0).base = Math.max(1, originalDmg - 2);
                __instance.damage.get(0).output = __instance.damage.get(0).base;

                logger.info(String.format(
                    "Ascension 37: Bronze Orb attack damage reduced from %d to %d (-2)",
                    originalDmg, __instance.damage.get(0).base
                ));
            }
        }
    }

    @SpirePatch(clz = BronzeOrb.class, method = SpirePatch.CONSTRUCTOR, paramtypez = {float.class, float.class, int.class})
    public static class BronzeOrbArtifactPatch {
        @SpirePostfixPatch
        public static void Postfix(BronzeOrb __instance, float x, float y, int count) {
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 37) {
                return;
            }

            // Bronze Orb gains 2 Artifact when spawned
            AbstractDungeon.actionManager.addToBottom(new ApplyPowerAction(
                __instance, __instance,
                new ArtifactPower(__instance, 2), 2
            ));

            logger.info("Ascension 37: Bronze Orb gains 2 Artifact on spawn");
        }
    }

    /**
     * Bronze Orb: Fix hardcoded damage in setMove() calls
     * getMove() calls setMove((byte)1, Intent.ATTACK, 8) with hardcoded 8
     * We intercept setMove calls and replace 8 with actual damage.base value
     */
    @SpirePatch(
        clz = AbstractMonster.class,
        method = "setMove",
        paramtypez = {byte.class, AbstractMonster.Intent.class, int.class}
    )
    public static class BronzeOrbSetMovePatch {
        @SpirePrefixPatch
        public static void Prefix(AbstractMonster __instance, byte nextMove, AbstractMonster.Intent intent, @ByRef int[] damage) {
            // Only apply to Bronze Orb at A37+
            if (!(__instance instanceof BronzeOrb)) {
                return;
            }

            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 37) {
                return;
            }

            // If this is the BEAM move (byte 1) with hardcoded damage 8, correct it
            if (nextMove == 1 && damage[0] == 8 && !__instance.damage.isEmpty()) {
                int correctedDamage = __instance.damage.get(0).base;
                logger.info(String.format(
                    "Ascension 37: Bronze Orb setMove intercepted - correcting damage %d → %d",
                    damage[0], correctedDamage
                ));
                damage[0] = correctedDamage;
            }
        }
    }

    // ============= CHAMP =============

    @SpirePatch(clz = Champ.class, method = "getMove", paramtypez = {int.class})
    public static class ChampAngerThresholdPatch {
        @SpirePrefixPatch
        public static SpireReturn<Void> Prefix(Champ __instance, int num) throws Exception {
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 37) {
                return SpireReturn.Continue();
            }

            // Check if health is below 60% instead of 50%
            Field thresholdReachedField = Champ.class.getDeclaredField("thresholdReached");
            thresholdReachedField.setAccessible(true);
            boolean thresholdReached = thresholdReachedField.getBoolean(__instance);

            if (!thresholdReached && __instance.currentHealth < __instance.maxHealth * 0.6f) {
                thresholdReachedField.setBoolean(__instance, true);
                __instance.setMove((byte)7, AbstractMonster.Intent.BUFF);
                __instance.createIntent();

                logger.info("Ascension 37: Champ Anger threshold changed to 60%");

                // Return early to prevent getMove from continuing (like original code)
                return SpireReturn.Return(null);
            }

            return SpireReturn.Continue();
        }
    }

    // ============= TORCH HEAD =============

    @SpirePatch(clz = TorchHead.class, method = SpirePatch.CONSTRUCTOR, paramtypez = {float.class, float.class})
    public static class TorchHeadStatsPatch {
        @SpirePostfixPatch
        public static void Postfix(TorchHead __instance) {
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 37) {
                return;
            }

            // HP +5
            int originalMaxHP = __instance.maxHealth;
            __instance.maxHealth += 5;
            __instance.currentHealth += 5;

            // Tackle damage +2 (7 → 9)
            if (__instance.damage != null && !__instance.damage.isEmpty()) {
                int originalDmg = __instance.damage.get(0).base;
                __instance.damage.get(0).base += 2;
                __instance.damage.get(0).output = __instance.damage.get(0).base;

                logger.info(String.format(
                    "Ascension 37: Torch Head stats increased - HP: %d→%d (+5), Tackle damage: %d→%d (+2)",
                    originalMaxHP, __instance.maxHealth, originalDmg, __instance.damage.get(0).base
                ));
            }
        }
    }

    /**
     * Torch Head: Fix hardcoded damage in getMove() (line 85: setMove(..., 7))
     */
    @SpirePatch(clz = TorchHead.class, method = "getMove", paramtypez = {int.class})
    public static class TorchHeadGetMovePatch {
        @SpirePostfixPatch
        public static void Postfix(TorchHead __instance, int num) {
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 37) {
                return;
            }

            // TorchHead's getMove always sets damage to hardcoded 7
            // We need to correct it to actual damage value
            int correctedDamage = __instance.damage.get(0).base;
            __instance.setMove((byte)1, AbstractMonster.Intent.ATTACK, correctedDamage);
            __instance.createIntent();

            logger.info(String.format(
                "Ascension 37: Torch Head getMove intent corrected to %d damage",
                correctedDamage
            ));
        }
    }

    /**
     * Torch Head: Fix hardcoded damage in takeTurn() (line 62: SetMoveAction(..., 7))
     * This runs every turn AFTER the monster attacks, so we only fix the Intent
     */
    @SpirePatch(clz = TorchHead.class, method = "takeTurn")
    public static class TorchHeadTakeTurnPatch {
        @SpirePostfixPatch
        public static void Postfix(TorchHead __instance) {
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 37) {
                return;
            }

            // Find and modify the SetMoveAction that was just added
            // takeTurn() adds: new SetMoveAction(this, (byte)1, Intent.ATTACK, 7)
            // We need to correct the hardcoded 7 to actual damage
            for (int i = AbstractDungeon.actionManager.actions.size() - 1; i >= 0; i--) {
                AbstractGameAction action = AbstractDungeon.actionManager.actions.get(i);

                if (action instanceof com.megacrit.cardcrawl.actions.common.SetMoveAction) {
                    try {
                        // Access private fields to modify the SetMoveAction
                        java.lang.reflect.Field theNextDamageField = com.megacrit.cardcrawl.actions.common.SetMoveAction.class.getDeclaredField("theNextDamage");
                        theNextDamageField.setAccessible(true);

                        // Get current damage value
                        int currentDamage = theNextDamageField.getInt(action);

                        // Only modify if it's the hardcoded 7
                        if (currentDamage == 7) {
                            int correctedDamage = __instance.damage.get(0).base;
                            theNextDamageField.setInt(action, correctedDamage);

                            logger.info(String.format(
                                "Ascension 37: Torch Head SetMoveAction theNextDamage corrected from 7 to %d",
                                correctedDamage
                            ));
                            break;
                        }
                    } catch (Exception e) {
                        logger.error("Failed to modify TorchHead SetMoveAction", e);
                    }
                }
            }
        }
    }

    // ============= CULTIST (for Awakened One) =============

    /**
     * Cultist damage() patch - set halfDead when HP reaches 0
     * Similar to Donu/Deca in Level87
     * Patch AbstractMonster.damage and filter by instanceof
     */
    @SpirePatch(clz = AbstractMonster.class, method = "damage")
    public static class CultistHalfDeadPatch {
        @SpirePostfixPatch
        public static void Postfix(AbstractMonster __instance, DamageInfo info) {
            // Only apply to Cultist
            if (!(__instance instanceof Cultist)) {
                return;
            }

            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 37) {
                return;
            }

            // Check if Awakened One is present
            boolean awakenedPresent = false;
            for (AbstractMonster m : AbstractDungeon.getMonsters().monsters) {
                if (m.id != null && m.id.equals("AwakenedOne") && !m.isDead && !m.isDying) {
                    awakenedPresent = true;
                    break;
                }
            }

            if (!awakenedPresent) {
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

                    // Clear all powers
                    __instance.powers.clear();

                    // Set ??? intent (UNKNOWN, do nothing this turn)
                    __instance.setMove((byte)99, AbstractMonster.Intent.UNKNOWN);
                    __instance.createIntent();
                    AbstractDungeon.actionManager.addToBottom(
                        new SetMoveAction(__instance, (byte)99, AbstractMonster.Intent.UNKNOWN)
                    );

                    logger.info("Ascension 37: Cultist is now half dead, set ??? intent");
                }
            } catch (Exception e) {
                logger.error("Ascension 37: Error in Cultist damage patch", e);
            }
        }
    }

    /**
     * Cultist die() patch - prevent actual death when Awakened One is alive
     */
    @SpirePatch(clz = Cultist.class, method = "die")
    public static class CultistCannotDie {
        @SpirePrefixPatch
        public static SpireReturn<Void> Prefix(Cultist __instance) {
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 37) {
                return SpireReturn.Continue();
            }

            // Check if Awakened One is alive
            boolean awakenedAlive = false;
            for (AbstractMonster m : AbstractDungeon.getMonsters().monsters) {
                if (m.id != null && m.id.equals("AwakenedOne") && !m.isDead && !m.isDying) {
                    awakenedAlive = true;
                    break;
                }
            }

            // If Awakened One is alive, prevent Cultist death
            if (awakenedAlive) {
                logger.info("Ascension 37: Cultist die() blocked - Awakened One is alive");
                return SpireReturn.Return();
            }

            // If Awakened One is dead, allow Cultist to die normally
            return SpireReturn.Continue();
        }
    }

    /**
     * Cultist takeTurn() patch - handle death turn (99) and revival turn (98)
     */
    @SpirePatch(clz = Cultist.class, method = "takeTurn")
    public static class CultistReviveTakeTurn {
        @SpirePrefixPatch
        public static SpireReturn<Void> Prefix(Cultist __instance) {
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 37) {
                return SpireReturn.Continue();
            }

            logger.info("Ascension 37: Cultist takeTurn() called, nextMove = " + __instance.nextMove);

            // Case 99: Death turn - do nothing, just wait
            if (__instance.nextMove == 99) {
                logger.info("Ascension 37: Cultist death turn - doing nothing");
                AbstractDungeon.actionManager.addToBottom(
                    new RollMoveAction(__instance)
                );
                return SpireReturn.Return();
            }

            // Case 98: Revival turn - actually revive
            if (__instance.nextMove == 98) {
                logger.info("Ascension 37: Cultist revival turn - reviving now");

                // Revive with full HP
                __instance.currentHealth = 0;
                AbstractDungeon.actionManager.addToBottom(
                    new HealAction(__instance, __instance, __instance.maxHealth)
                );

                // Clear halfDead state
                __instance.halfDead = false;

                // Reset firstMove flag to start with Incantation
                try {
                    Field firstMoveField = Cultist.class.getDeclaredField("firstMove");
                    firstMoveField.setAccessible(true);
                    firstMoveField.setBoolean(__instance, true);
                } catch (Exception e) {
                    logger.error("Ascension 37: Failed to reset Cultist firstMove", e);
                }

                // Call RollMoveAction to determine next move (should be Incantation)
                AbstractDungeon.actionManager.addToBottom(
                    new RollMoveAction(__instance)
                );

                logger.info("Ascension 37: Cultist revived with full HP, starting with Incantation");
                return SpireReturn.Return();
            }

            return SpireReturn.Continue();
        }
    }

    /**
     * Cultist getMove() patch - set revival intent when halfDead
     */
    @SpirePatch(clz = Cultist.class, method = "getMove")
    public static class CultistReviveGetMove {
        @SpirePrefixPatch
        public static SpireReturn<Void> Prefix(Cultist __instance, int num) {
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 37) {
                return SpireReturn.Continue();
            }

            if (__instance.halfDead) {
                logger.info("Ascension 37: Cultist getMove() while halfDead - setting revival intent");
                __instance.setMove((byte)98, AbstractMonster.Intent.BUFF);
                return SpireReturn.Return();
            }

            return SpireReturn.Continue();
        }
    }

    // ============= TIME EATER =============

    @SpirePatch(clz = TimeEater.class, method = SpirePatch.CONSTRUCTOR)
    public static class TimeEaterHPPatch {
        @SpirePostfixPatch
        public static void Postfix(TimeEater __instance) {
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 37) {
                return;
            }

            int originalMaxHP = __instance.maxHealth;
            __instance.maxHealth += 30;
            __instance.currentHealth += 30;

            logger.info(String.format(
                "Ascension 37: Time Eater HP increased from %d to %d (+30)",
                originalMaxHP, __instance.maxHealth
            ));
        }
    }

    // ============= DONU =============

    @SpirePatch(clz = Donu.class, method = "usePreBattleAction")
    public static class DonuMalleablePatch {
        @SpirePostfixPatch
        public static void Postfix(Donu __instance) {
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 37) {
                return;
            }

            // Donu gains 2 Malleable (탄성) at battle start
            AbstractDungeon.actionManager.addToBottom(new ApplyPowerAction(
                __instance, __instance,
                new MalleablePower(__instance, 2), 2
            ));

            logger.info("Ascension 37: Donu gains 2 Malleable at battle start");
        }
    }

    // ============= DECA =============

    @SpirePatch(clz = Deca.class, method = "takeTurn")
    public static class DecaPlatedArmorPatch {
        @SpirePostfixPatch
        public static void Postfix(Deca __instance) {
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 37) {
                return;
            }

            if (__instance.nextMove == 2) { // SQUARE_OF_PROTECTION = byte 2
                // Add +2 to existing Plated Armor application
                for (AbstractMonster m : AbstractDungeon.getMonsters().monsters) {
                    AbstractDungeon.actionManager.addToBottom(new ApplyPowerAction(
                        m, __instance,
                        new PlatedArmorPower(m, 2), 2
                    ));
                }

                logger.info("Ascension 37: Deca Square of Protection grants +2 extra Plated Armor");
            }
        }
    }
}
