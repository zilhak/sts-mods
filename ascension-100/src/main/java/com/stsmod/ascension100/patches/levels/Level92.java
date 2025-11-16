package com.stsmod.ascension100.patches.levels;

import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePrefixPatch;
import com.evacipated.cardcrawl.modthespire.lib.SpireReturn;
import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.actions.common.ApplyPowerAction;
import com.megacrit.cardcrawl.actions.common.DamageAction;
import com.megacrit.cardcrawl.actions.common.MakeTempCardInDrawPileAction;
import com.megacrit.cardcrawl.actions.common.MakeTempCardInDiscardAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.cards.DamageInfo;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import com.megacrit.cardcrawl.powers.*;
import com.megacrit.cardcrawl.monsters.exordium.GremlinTsundere;
import com.megacrit.cardcrawl.monsters.exordium.GremlinFat;
import com.megacrit.cardcrawl.monsters.exordium.GremlinWarrior;
import com.megacrit.cardcrawl.monsters.exordium.JawWorm;
import com.megacrit.cardcrawl.monsters.exordium.FungiBeast;
import com.megacrit.cardcrawl.monsters.city.SnakePlant;
import com.megacrit.cardcrawl.monsters.beyond.Repulsor;
import com.megacrit.cardcrawl.monsters.beyond.Spiker;
import com.megacrit.cardcrawl.monsters.beyond.WrithingMass;
import com.megacrit.cardcrawl.monsters.beyond.Maw;
import com.megacrit.cardcrawl.monsters.beyond.OrbWalker;
import com.megacrit.cardcrawl.monsters.beyond.Darkling;
import com.megacrit.cardcrawl.monsters.city.Healer;
import com.megacrit.cardcrawl.monsters.city.Mugger;
import com.megacrit.cardcrawl.monsters.exordium.GremlinNob;
import com.megacrit.cardcrawl.monsters.exordium.Sentry;
import com.megacrit.cardcrawl.monsters.city.Taskmaster;
import com.megacrit.cardcrawl.monsters.beyond.Nemesis;
import com.megacrit.cardcrawl.monsters.city.BookOfStabbing;
import com.megacrit.cardcrawl.monsters.city.GremlinLeader;
import com.megacrit.cardcrawl.monsters.exordium.Lagavulin;
import com.megacrit.cardcrawl.monsters.city.Snecko;
import com.megacrit.cardcrawl.monsters.beyond.Reptomancer;
import com.megacrit.cardcrawl.monsters.beyond.SnakeDagger;
import com.megacrit.cardcrawl.monsters.beyond.AwakenedOne;
import com.megacrit.cardcrawl.monsters.exordium.SlaverRed;
import com.megacrit.cardcrawl.actions.utility.SFXAction;
import com.megacrit.cardcrawl.actions.common.ReducePowerAction;
import com.megacrit.cardcrawl.actions.common.SpawnMonsterAction;
import com.megacrit.cardcrawl.cards.status.Burn;
import com.megacrit.cardcrawl.cards.status.Dazed;
import com.megacrit.cardcrawl.cards.status.Wound;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.reflect.Field;

/**
 * Ascension Level 92: Enemy mechanics enhanced
 *
 * 적들의 기믹이 강화됩니다.
 *
 * NOTE: This is a MASSIVE level with enhancements to 40+ monsters
 * Due to complexity, this implementation focuses on:
 * 1. Slow Power compound calculation (Giant Head mechanic)
 * 2. Health and damage stat increases for various monsters
 * 3. Starting power bonuses
 * 4. Pattern-specific changes require individual monster patches (TODO for future implementation)
 *
 * Giant Head - Slow debuff: 15% compound interest per card played
 * Various monsters: HP, damage, and power bonuses as documented
 */
public class Level92 {
    private static final Logger logger = LogManager.getLogger(Level92.class.getName());
    private static final float COMPOUND_RATE = 1.15f;  // 15% compound interest for Slow

    /**
     * Patch SlowPower to use compound calculation at A92+
     * Giant Head mechanic: 15% compound damage increase per card played
     */
    @SpirePatch(
        clz = SlowPower.class,
        method = "atDamageReceive"
    )
    public static class SlowPowerCompoundDamage {
        @SpirePrefixPatch
        public static SpireReturn<Float> Prefix(SlowPower __instance, float damage, DamageInfo.DamageType type) {
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 92) {
                return SpireReturn.Continue();
            }

            if (type == DamageInfo.DamageType.NORMAL) {
                float multiplier = (float) Math.pow(COMPOUND_RATE, __instance.amount);
                float newDamage = damage * multiplier;

                logger.info(String.format(
                    "Ascension 92: Slow debuff (compound) - stacks: %d, multiplier: %.2f, damage: %.1f → %.1f",
                    __instance.amount, multiplier, damage, newDamage
                ));

                return SpireReturn.Return(newDamage);
            }

            return SpireReturn.Return(damage);
        }
    }

    /**
     * Common stat and power increases for various monsters at A92+
     * Applied at monster initialization
     */
    @SpirePatch(
        clz = AbstractMonster.class,
        method = "init"
    )
    public static class MonsterStatIncreases {
        @SpirePostfixPatch
        public static void Postfix(AbstractMonster __instance) {
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 92) {
                return;
            }

            String id = __instance.id;
            if (id == null) return;

            int hpBonus = 0;

            // Apply HP bonuses for specific monsters
            switch (id) {
                case "Louse": hpBonus = 2; break;
                case "Chosen": hpBonus = 20; break;
                case "Centurion": hpBonus = 15; break;
                case "Pointy": hpBonus = 5; break;
                case "Bear": hpBonus = 5; break;
                case "Exploder": hpBonus = 5; break;
                case "Spire Growth": hpBonus = 20; break;
                case "GiantHead":
                    // Giant Head gets 100% HP increase
                    int originalHP = __instance.maxHealth;
                    __instance.maxHealth *= 2;
                    __instance.currentHealth *= 2;
                    logger.info(String.format(
                        "Ascension 92: %s HP doubled from %d to %d",
                        __instance.name, originalHP, __instance.maxHealth
                    ));
                    return; // Skip regular HP bonus
            }

            if (hpBonus > 0) {
                int originalHP = __instance.maxHealth;
                __instance.maxHealth += hpBonus;
                __instance.currentHealth += hpBonus;

                logger.info(String.format(
                    "Ascension 92: %s HP increased from %d to %d (+%d)",
                    __instance.name, originalHP, __instance.maxHealth, hpBonus
                ));
            }
        }
    }

    /**
     * Damage increases for specific monsters at A92+
     * Applied before battle
     */
    @SpirePatch(
        clz = AbstractMonster.class,
        method = "usePreBattleAction"
    )
    public static class MonsterDamageIncreases {
        @SpirePostfixPatch
        public static void Postfix(AbstractMonster __instance) {
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 92) {
                return;
            }

            String id = __instance.id;
            if (id == null) return;

            int damageBonus = 0;

            // Apply damage bonuses for specific monsters
            switch (id) {
                case "Acid Slime (L)":
                case "Acid Slime (M)":
                case "Acid Slime (S)":
                case "Spike Slime (L)":
                case "Spike Slime (M)":
                case "Spike Slime (S)":
                    damageBonus = 1;
                    break;
                case "Romeo": damageBonus = 1; break;
                case "GremlinWarrior": damageBonus = 15; break; // Wizard Gremlin
                case "SneakyGremlin": damageBonus = 7; break; // Sneaky Gremlin
            }

            if (damageBonus > 0) {
                for (DamageInfo dmg : __instance.damage) {
                    if (dmg != null && dmg.base > 0) {
                        dmg.base += damageBonus;
                    }
                }

                logger.info(String.format(
                    "Ascension 92: %s damage increased by %d",
                    __instance.name, damageBonus
                ));
            }

            // Apply starting powers for specific monsters
            applyStartingPowers(__instance);
        }
    }

    /**
     * Helper method to apply starting powers to monsters
     */
    private static void applyStartingPowers(AbstractMonster monster) {
        String id = monster.id;
        if (id == null) return;

        // Spheric Guardian: Starting Plated Armor 5
        if (id.equals("SphericGuardian")) {
            AbstractDungeon.actionManager.addToBottom(
                new ApplyPowerAction(
                    (AbstractCreature)monster,
                    (AbstractCreature)monster,
                    new PlatedArmorPower((AbstractCreature)monster, 5),
                    5
                )
            );
            logger.info("Ascension 92: Spheric Guardian gained starting Plated Armor 5");
        }

        // Transient: Intangible 200
        if (id.equals("Transient")) {
            AbstractDungeon.actionManager.addToBottom(
                new ApplyPowerAction(
                    (AbstractCreature)monster,
                    (AbstractCreature)monster,
                    new IntangiblePlayerPower((AbstractCreature)monster, 200),
                    200
                )
            );
            logger.info("Ascension 92: Transient gained Intangible 200");
        }

        // Louse: Curl Up +10 (increase existing Curl Up power)
        if (id.equals("FuzzyLouseNormal") || id.equals("FuzzyLouseDefensive")) {
            // Curl Up power is applied in usePreBattleAction, we'll increase it after
            // This is handled in a separate patch below
        }

        // Shelled Parasite: Plated Armor +4 (increase existing Plated Armor)
        if (id.equals("Shelled Parasite")) {
            // Plated Armor is applied in usePreBattleAction, we'll increase it after
            // This is handled in a separate patch below
        }
    }

    /**
     * Increase Louse's Curl Up power by 10
     */
    @SpirePatch(
        clz = AbstractMonster.class,
        method = "usePreBattleAction"
    )
    public static class LouseCurlUpIncrease {
        @SpirePostfixPatch
        public static void Postfix(AbstractMonster __instance) {
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 92) {
                return;
            }

            String id = __instance.id;
            if (id == null) return;

            // Louse: Add +10 Curl Up using ApplyPowerAction
            if (id.equals("FuzzyLouseNormal") || id.equals("FuzzyLouseDefensive")) {
                AbstractDungeon.actionManager.addToBottom(
                    new ApplyPowerAction(__instance, __instance,
                        new com.megacrit.cardcrawl.powers.CurlUpPower(__instance, 10), 10)
                );
                logger.info(String.format(
                    "Ascension 92: %s gained +10 Curl Up",
                    __instance.name
                ));
            }

            // Shelled Parasite: Increase Plated Armor by 4
            if (id.equals("Shelled Parasite")) {
                AbstractPower platedArmor = __instance.getPower("Plated Armor");
                if (platedArmor != null) {
                    int originalAmount = platedArmor.amount;
                    platedArmor.amount += 4;
                    platedArmor.updateDescription();

                    logger.info(String.format(
                        "Ascension 92: %s Plated Armor increased from %d to %d (+4)",
                        __instance.name, originalAmount, platedArmor.amount
                    ));
                }
            }

            // Transient: Increase Fading by 1
            if (id.equals("Transient")) {
                AbstractPower fading = __instance.getPower("Fading");
                if (fading != null) {
                    int originalAmount = fading.amount;
                    fading.amount += 1;
                    fading.updateDescription();

                    logger.info(String.format(
                        "Ascension 92: %s Fading increased from %d to %d (+1)",
                        __instance.name, originalAmount, fading.amount
                    ));
                }
            }

            // Looter: Increase Thievery by 10 and escapeDef by 3
            if (id.equals("Looter")) {
                AbstractPower thieveryPower = __instance.getPower("Thievery");
                if (thieveryPower != null) {
                    thieveryPower.amount += 10;
                    thieveryPower.updateDescription();
                    logger.info(String.format(
                        "Ascension 92: Looter Thievery increased by 10 to %d",
                        thieveryPower.amount
                    ));
                }

                try {
                    Field escapeDefField = __instance.getClass().getDeclaredField("escapeDef");
                    escapeDefField.setAccessible(true);
                    int originalDef = escapeDefField.getInt(__instance);
                    escapeDefField.setInt(__instance, originalDef + 3);

                    logger.info(String.format(
                        "Ascension 92: Looter escapeDef increased from %d to %d (+3)",
                        originalDef, originalDef + 3
                    ));
                } catch (Exception e) {
                    logger.error("Failed to modify Looter escapeDef", e);
                }
            }

            // Mugger: Increase Thievery by 20
            if (id.equals("Mugger")) {
                AbstractPower thieveryPower = __instance.getPower("Thievery");
                if (thieveryPower != null) {
                    thieveryPower.amount += 20;
                    thieveryPower.updateDescription();
                    logger.info(String.format(
                        "Ascension 92: Mugger Thievery increased by 20 to %d",
                        thieveryPower.amount
                    ));
                }
            }

            // Shield Gremlin (GremlinTsundere): Increase blockAmt by 8
            if (id.equals("GremlinTsundere")) {
                try {
                    Field blockAmtField = __instance.getClass().getDeclaredField("blockAmt");
                    blockAmtField.setAccessible(true);
                    int originalBlock = blockAmtField.getInt(__instance);
                    blockAmtField.setInt(__instance, originalBlock + 8);

                    logger.info(String.format(
                        "Ascension 92: Shield Gremlin blockAmt increased from %d to %d (+8)",
                        originalBlock, originalBlock + 8
                    ));
                } catch (Exception e) {
                    logger.error("Failed to modify Shield Gremlin stats", e);
                }
            }

            // Mad Gremlin (GremlinWarrior): Increase AngryPower by 1
            if (id.equals("GremlinWarrior")) {
                AbstractPower angry = __instance.getPower("Anger");
                if (angry != null) {
                    int originalAmount = angry.amount;
                    angry.amount += 1;
                    angry.updateDescription();

                    logger.info(String.format(
                        "Ascension 92: Mad Gremlin Anger increased from %d to %d (+1)",
                        originalAmount, angry.amount
                    ));
                }
            }

            // Snake Plant: Increase Malleable by 2
            if (id.equals("SnakePlant")) {
                AbstractPower malleable = __instance.getPower("Malleable");
                if (malleable != null) {
                    int originalAmount = malleable.amount;
                    malleable.amount += 2;
                    malleable.updateDescription();

                    logger.info(String.format(
                        "Ascension 92: Snake Plant Malleable increased from %d to %d (+2)",
                        originalAmount, malleable.amount
                    ));
                }
            }

            // Repulsor: Increase dazeAmt by 1
            if (id.equals("Repulsor")) {
                try {
                    Field dazeAmtField = __instance.getClass().getDeclaredField("dazeAmt");
                    dazeAmtField.setAccessible(true);
                    int originalDaze = dazeAmtField.getInt(__instance);
                    dazeAmtField.setInt(__instance, originalDaze + 1);

                    logger.info(String.format(
                        "Ascension 92: Repulsor dazeAmt increased from %d to %d (+1)",
                        originalDaze, originalDaze + 1
                    ));
                } catch (Exception e) {
                    logger.error("Failed to modify Repulsor stats", e);
                }
            }

            // Writhing Mass: Increase Malleable by 5
            if (id.equals("WrithingMass")) {
                AbstractPower malleable = __instance.getPower("Malleable");
                if (malleable != null) {
                    int originalAmount = malleable.amount;
                    malleable.amount += 5;
                    malleable.updateDescription();

                    logger.info(String.format(
                        "Ascension 92: Writhing Mass Malleable increased from %d to %d (+5)",
                        originalAmount, malleable.amount
                    ));
                }
            }

            // Maw: Increase strUp by 3 for Drool pattern
            if (id.equals("Maw")) {
                try {
                    Field strUpField = __instance.getClass().getDeclaredField("strUp");
                    strUpField.setAccessible(true);
                    int originalStr = strUpField.getInt(__instance);
                    strUpField.setInt(__instance, originalStr + 3);

                    logger.info(String.format(
                        "Ascension 92: Maw strUp increased from %d to %d (+3)",
                        originalStr, originalStr + 3
                    ));
                } catch (Exception e) {
                    logger.error("Failed to modify Maw stats", e);
                }
            }
        }
    }

    /**
     * Fat Gremlin applies Vulnerable on attack at A92+
     * We use Prefix to remember the move, then Postfix to apply Vulnerable
     */
    @SpirePatch(
        clz = GremlinFat.class,
        method = "takeTurn"
    )
    public static class FatGremlinVulnerable {
        private static final ThreadLocal<Byte> lastMove = new ThreadLocal<>();

        @SpirePrefixPatch
        public static void Prefix(GremlinFat __instance) {
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 92) {
                return;
            }

            try {
                Field nextMoveField = AbstractMonster.class.getDeclaredField("nextMove");
                nextMoveField.setAccessible(true);
                byte move = nextMoveField.getByte(__instance);
                lastMove.set(move);
            } catch (Exception e) {
                logger.error("Failed to get Fat Gremlin move", e);
            }
        }

        @SpirePostfixPatch
        public static void Postfix(GremlinFat __instance) {
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 92) {
                return;
            }

            Byte move = lastMove.get();
            if (move != null && move == 2) { // BLUNT attack
                AbstractDungeon.actionManager.addToBottom(
                    new ApplyPowerAction(
                        (AbstractCreature)AbstractDungeon.player,
                        (AbstractCreature)__instance,
                        new VulnerablePower((AbstractCreature)AbstractDungeon.player, 1, true),
                        1
                    )
                );

                logger.info("Ascension 92: Fat Gremlin applied Vulnerable 1");
            }

            lastMove.remove();
        }
    }

    /**
     * Jaw Worm gains +1 extra Strength from Bellow pattern at A92+
     */
    @SpirePatch(
        clz = JawWorm.class,
        method = "takeTurn"
    )
    public static class JawWormExtraStrength {
        private static final ThreadLocal<Byte> lastMove = new ThreadLocal<>();

        @SpirePrefixPatch
        public static void Prefix(JawWorm __instance) {
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 92) {
                return;
            }

            try {
                Field nextMoveField = AbstractMonster.class.getDeclaredField("nextMove");
                nextMoveField.setAccessible(true);
                byte move = nextMoveField.getByte(__instance);
                lastMove.set(move);
            } catch (Exception e) {
                logger.error("Failed to get Jaw Worm move", e);
            }
        }

        @SpirePostfixPatch
        public static void Postfix(JawWorm __instance) {
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 92) {
                return;
            }

            Byte move = lastMove.get();
            if (move != null && move == 2) { // BELLOW move
                // Find and modify the last ApplyPowerAction with StrengthPower
                try {
                    for (int i = AbstractDungeon.actionManager.actions.size() - 1; i >= 0; i--) {
                        AbstractGameAction action = AbstractDungeon.actionManager.actions.get(i);

                        if (action instanceof ApplyPowerAction) {
                            Field powerToApplyField = ApplyPowerAction.class.getDeclaredField("powerToApply");
                            powerToApplyField.setAccessible(true);
                            AbstractPower power = (AbstractPower) powerToApplyField.get(action);

                            if (power instanceof StrengthPower && power.owner == __instance) {
                                power.amount += 1;

                                Field amountField = ApplyPowerAction.class.getDeclaredField("amount");
                                amountField.setAccessible(true);
                                int currentAmount = amountField.getInt(action);
                                amountField.setInt(action, currentAmount + 1);

                                logger.info(String.format(
                                    "Ascension 92: Jaw Worm Strength increased by +1 (total: %d)",
                                    power.amount
                                ));
                                break;
                            }
                        }
                    }
                } catch (Exception e) {
                    logger.error("Failed to modify Jaw Worm Strength amount", e);
                }
            }

            lastMove.remove();
        }
    }

    /**
     * Fungi Beast gains +3 extra Strength from Grow pattern at A92+
     * Base game: Strength 3 (A2: 4, A17: 5)
     * A92+: Strength 8 total on GROW move (A17 basis: 5+3)
     *
     * Uses Prefix to increase strAmt BEFORE takeTurn executes,
     * so all Strength is gained at once instead of 5+3 separately
     */
    @SpirePatch(
        clz = FungiBeast.class,
        method = "takeTurn"
    )
    public static class FungiBeastExtraStrength {
        private static boolean increased = false;

        @SpirePrefixPatch
        public static void Prefix(FungiBeast __instance) {
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 92) {
                return;
            }

            try {
                // Check if GROW move (2)
                Field nextMoveField = AbstractMonster.class.getDeclaredField("nextMove");
                nextMoveField.setAccessible(true);
                byte move = nextMoveField.getByte(__instance);

                if (move == 2 && !increased) { // GROW move
                    // Increase strAmt by 3 BEFORE takeTurn executes
                    Field strAmtField = FungiBeast.class.getDeclaredField("strAmt");
                    strAmtField.setAccessible(true);
                    int originalAmt = strAmtField.getInt(__instance);
                    strAmtField.setInt(__instance, originalAmt + 3);
                    increased = true;

                    logger.info(String.format(
                        "Ascension 92: Fungi Beast strAmt increased from %d to %d (A17+: will gain %d total)",
                        originalAmt, originalAmt + 3, originalAmt + 3 + 1
                    ));
                }
            } catch (Exception e) {
                logger.error("Failed to increase Fungi Beast strAmt", e);
            }
        }

        @SpirePostfixPatch
        public static void Postfix(FungiBeast __instance) {
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 92) {
                return;
            }

            try {
                // Reset strAmt back to original after GROW move
                Field nextMoveField = AbstractMonster.class.getDeclaredField("nextMove");
                nextMoveField.setAccessible(true);
                byte move = nextMoveField.getByte(__instance);

                if (move == 2 && increased) { // GROW move just executed
                    Field strAmtField = FungiBeast.class.getDeclaredField("strAmt");
                    strAmtField.setAccessible(true);
                    int currentAmt = strAmtField.getInt(__instance);
                    strAmtField.setInt(__instance, currentAmt - 3);
                    increased = false;

                    logger.info("Ascension 92: Fungi Beast strAmt reset to original");
                }
            } catch (Exception e) {
                logger.error("Failed to reset Fungi Beast strAmt", e);
            }
        }
    }

    /**
     * Spiker gains +4 extra Thorns from Buff pattern at A92+
     */
    @SpirePatch(
        clz = Spiker.class,
        method = "takeTurn"
    )
    public static class SpikerExtraThorns {
        private static final ThreadLocal<Byte> lastMove = new ThreadLocal<>();

        @SpirePrefixPatch
        public static void Prefix(Spiker __instance) {
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 92) {
                return;
            }

            try {
                Field nextMoveField = AbstractMonster.class.getDeclaredField("nextMove");
                nextMoveField.setAccessible(true);
                byte move = nextMoveField.getByte(__instance);
                lastMove.set(move);
            } catch (Exception e) {
                logger.error("Failed to get Spiker move", e);
            }
        }

        @SpirePostfixPatch
        public static void Postfix(Spiker __instance) {
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 92) {
                return;
            }

            Byte move = lastMove.get();
            if (move != null && move == 2) { // BUFF_THORNS move
                // Find and modify the last ApplyPowerAction with ThornsPower
                try {
                    for (int i = AbstractDungeon.actionManager.actions.size() - 1; i >= 0; i--) {
                        AbstractGameAction action = AbstractDungeon.actionManager.actions.get(i);

                        if (action instanceof ApplyPowerAction) {
                            Field powerToApplyField = ApplyPowerAction.class.getDeclaredField("powerToApply");
                            powerToApplyField.setAccessible(true);
                            AbstractPower power = (AbstractPower) powerToApplyField.get(action);

                            if (power instanceof ThornsPower && power.owner == __instance) {
                                power.amount += 4;

                                Field amountField = ApplyPowerAction.class.getDeclaredField("amount");
                                amountField.setAccessible(true);
                                int currentAmount = amountField.getInt(action);
                                amountField.setInt(action, currentAmount + 4);

                                logger.info(String.format(
                                    "Ascension 92: Spiker Thorns increased by +4 (total: %d)",
                                    power.amount
                                ));
                                break;
                            }
                        }
                    }
                } catch (Exception e) {
                    logger.error("Failed to modify Spiker Thorns amount", e);
                }
            }

            lastMove.remove();
        }
    }

    /**
     * Orb Walker adds extra Burns to draw pile and discard pile on Laser pattern at A92+
     * Base game: adds 1 Burn to discard AND draw pile
     * A92+: adds 1 MORE Burn to EACH pile (total 2 burns: 1 to draw, 1 to discard)
     */
    @SpirePatch(
        clz = OrbWalker.class,
        method = "takeTurn"
    )
    public static class OrbWalkerExtraBurns {
        private static final ThreadLocal<Byte> lastMove = new ThreadLocal<>();

        @SpirePrefixPatch
        public static void Prefix(OrbWalker __instance) {
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 92) {
                return;
            }

            try {
                Field nextMoveField = AbstractMonster.class.getDeclaredField("nextMove");
                nextMoveField.setAccessible(true);
                byte move = nextMoveField.getByte(__instance);
                lastMove.set(move);
            } catch (Exception e) {
                logger.error("Failed to get Orb Walker move", e);
            }
        }

        @SpirePostfixPatch
        public static void Postfix(OrbWalker __instance) {
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 92) {
                return;
            }

            Byte move = lastMove.get();
            if (move != null && move == 1) { // LASER move
                // Add 1 extra Burn to draw pile
                AbstractDungeon.actionManager.addToBottom(
                    new MakeTempCardInDrawPileAction(
                        (AbstractCard)new Burn(),
                        1,
                        true,
                        true
                    )
                );

                // Add 1 extra Burn to discard pile
                AbstractDungeon.actionManager.addToBottom(
                    new MakeTempCardInDiscardAction(
                        (AbstractCard)new Burn(),
                        1
                    )
                );

                logger.info("Ascension 92: Orb Walker added 2 extra Burns (1 to draw pile, 1 to discard pile)");
            }

            lastMove.remove();
        }
    }

    /**
     * Darkling gains Regeneration 2 when reviving (REINCARNATE move) at A92+
     * Base game: RegrowPower (Life Link) on revive
     * A92+: Additional RegenerateMonsterPower(2) on revive for HP regeneration
     */
    @SpirePatch(
        clz = Darkling.class,
        method = "takeTurn"
    )
    public static class DarklingRegeneration {
        private static final ThreadLocal<Byte> lastMove = new ThreadLocal<>();

        @SpirePrefixPatch
        public static void Prefix(Darkling __instance) {
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 92) {
                return;
            }

            try {
                Field nextMoveField = AbstractMonster.class.getDeclaredField("nextMove");
                nextMoveField.setAccessible(true);
                byte move = nextMoveField.getByte(__instance);
                lastMove.set(move);
            } catch (Exception e) {
                logger.error("Failed to get Darkling move", e);
            }
        }

        @SpirePostfixPatch
        public static void Postfix(Darkling __instance) {
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 92) {
                return;
            }

            Byte move = lastMove.get();
            if (move != null && move == 5) { // REINCARNATE move
                AbstractDungeon.actionManager.addToBottom(
                    new ApplyPowerAction(
                        (AbstractCreature)__instance,
                        (AbstractCreature)__instance,
                        new RegenerateMonsterPower(__instance, 2),
                        2
                    )
                );

                logger.info("Ascension 92: Darkling gained Regeneration 2 on revive");
            }

            lastMove.remove();
        }
    }

    /**
     * Healer (Mystic) applies additional Weak 2 on debuff attack pattern at A92+
     * Base game: ATTACK move (1) applies Frail 2
     * A92+: Additional WeakPower 2 on ATTACK move
     */
    @SpirePatch(
        clz = Healer.class,
        method = "takeTurn"
    )
    public static class HealerExtraWeak {
        private static final ThreadLocal<Byte> lastMove = new ThreadLocal<>();

        @SpirePrefixPatch
        public static void Prefix(Healer __instance) {
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 92) {
                return;
            }

            try {
                Field nextMoveField = AbstractMonster.class.getDeclaredField("nextMove");
                nextMoveField.setAccessible(true);
                byte move = nextMoveField.getByte(__instance);
                lastMove.set(move);
            } catch (Exception e) {
                logger.error("Failed to get Healer move", e);
            }
        }

        @SpirePostfixPatch
        public static void Postfix(Healer __instance) {
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 92) {
                return;
            }

            Byte move = lastMove.get();
            if (move != null && move == 1) { // ATTACK move
                AbstractDungeon.actionManager.addToBottom(
                    new ApplyPowerAction(
                        (AbstractCreature)AbstractDungeon.player,
                        (AbstractCreature)__instance,
                        new WeakPower((AbstractCreature)AbstractDungeon.player, 2, true),
                        2
                    )
                );

                logger.info("Ascension 92: Healer applied Weak 2 on debuff attack");
            }

            lastMove.remove();
        }
    }

    /**
     * Gremlin Nob gains +2 extra Anger on BELLOW pattern at A92+
     * Base game: Anger 2 (A18: Anger 3)
     * A92+: Total Anger 5 on BELLOW move (increases the existing ApplyPowerAction)
     */
    @SpirePatch(
        clz = GremlinNob.class,
        method = "takeTurn"
    )
    public static class GremlinNobExtraAnger {
        private static final ThreadLocal<Byte> lastMove = new ThreadLocal<>();

        @SpirePrefixPatch
        public static void Prefix(GremlinNob __instance) {
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 92) {
                return;
            }

            try {
                Field nextMoveField = AbstractMonster.class.getDeclaredField("nextMove");
                nextMoveField.setAccessible(true);
                byte move = nextMoveField.getByte(__instance);
                lastMove.set(move);
            } catch (Exception e) {
                logger.error("Failed to get Gremlin Nob move", e);
            }
        }

        @SpirePostfixPatch
        public static void Postfix(GremlinNob __instance) {
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 92) {
                return;
            }

            Byte move = lastMove.get();
            if (move != null && move == 3) { // BELLOW move
                // Find and modify the last ApplyPowerAction with AngerPower
                try {
                    for (int i = AbstractDungeon.actionManager.actions.size() - 1; i >= 0; i--) {
                        AbstractGameAction action = AbstractDungeon.actionManager.actions.get(i);

                        if (action instanceof ApplyPowerAction) {
                            Field powerToApplyField = ApplyPowerAction.class.getDeclaredField("powerToApply");
                            powerToApplyField.setAccessible(true);
                            AbstractPower power = (AbstractPower) powerToApplyField.get(action);

                            if (power instanceof AngerPower && power.owner == __instance) {
                                // Increase the amount by 2
                                power.amount += 2;

                                Field amountField = ApplyPowerAction.class.getDeclaredField("amount");
                                amountField.setAccessible(true);
                                int currentAmount = amountField.getInt(action);
                                amountField.setInt(action, currentAmount + 2);

                                logger.info(String.format(
                                    "Ascension 92: Gremlin Nob Anger increased by +2 (total: %d)",
                                    power.amount
                                ));
                                break;
                            }
                        }
                    }
                } catch (Exception e) {
                    logger.error("Failed to modify Gremlin Nob Anger amount", e);
                }
            }

            lastMove.remove();
        }
    }

    /**
     * Sentry adds +1 extra Dazed card on BOLT pattern at A92+
     * Base game: Dazed 2 (A18: Dazed 3)
     * A92+: Dazed 4 total on BOLT move
     *
     * Uses Prefix to increase dazedAmt BEFORE takeTurn executes,
     * so all 4 Dazed cards are added at once instead of 3+1 separately
     */
    @SpirePatch(
        clz = Sentry.class,
        method = "takeTurn"
    )
    public static class SentryExtraDazed {
        private static boolean increased = false;

        @SpirePrefixPatch
        public static void Prefix(Sentry __instance) {
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 92) {
                return;
            }

            try {
                // Check if BOLT move (3)
                Field nextMoveField = AbstractMonster.class.getDeclaredField("nextMove");
                nextMoveField.setAccessible(true);
                byte move = nextMoveField.getByte(__instance);

                if (move == 3 && !increased) { // BOLT move
                    // Increase dazedAmt by 1 BEFORE takeTurn executes
                    Field dazedAmtField = Sentry.class.getDeclaredField("dazedAmt");
                    dazedAmtField.setAccessible(true);
                    int originalAmt = dazedAmtField.getInt(__instance);
                    dazedAmtField.setInt(__instance, originalAmt + 1);
                    increased = true;

                    logger.info(String.format(
                        "Ascension 92: Sentry dazedAmt increased from %d to %d (will add all at once)",
                        originalAmt, originalAmt + 1
                    ));
                }
            } catch (Exception e) {
                logger.error("Failed to increase Sentry dazedAmt", e);
            }
        }

        @SpirePostfixPatch
        public static void Postfix(Sentry __instance) {
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 92) {
                return;
            }

            try {
                // Reset dazedAmt back to original after BOLT move
                Field nextMoveField = AbstractMonster.class.getDeclaredField("nextMove");
                nextMoveField.setAccessible(true);
                byte move = nextMoveField.getByte(__instance);

                if (move == 3 && increased) { // BOLT move just executed
                    Field dazedAmtField = Sentry.class.getDeclaredField("dazedAmt");
                    dazedAmtField.setAccessible(true);
                    int currentAmt = dazedAmtField.getInt(__instance);
                    dazedAmtField.setInt(__instance, currentAmt - 1);
                    increased = false;

                    logger.info("Ascension 92: Sentry dazedAmt reset to original");
                }
            } catch (Exception e) {
                logger.error("Failed to reset Sentry dazedAmt", e);
            }
        }
    }

    /**
     * Taskmaster adds +1 extra Wound card on SCOURING_WHIP attack at A92+
     * Base game: Wound 1 (A3: 2, A18: 3)
     * A92+: Additional Wound 1 on SCOURING_WHIP move
     */
    @SpirePatch(
        clz = Taskmaster.class,
        method = "takeTurn"
    )
    public static class TaskmasterExtraWound {
        private static final ThreadLocal<Byte> lastMove = new ThreadLocal<>();

        @SpirePrefixPatch
        public static void Prefix(Taskmaster __instance) {
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 92) {
                return;
            }

            try {
                Field nextMoveField = AbstractMonster.class.getDeclaredField("nextMove");
                nextMoveField.setAccessible(true);
                byte move = nextMoveField.getByte(__instance);
                lastMove.set(move);
            } catch (Exception e) {
                logger.error("Failed to get Taskmaster move", e);
            }
        }

        @SpirePostfixPatch
        public static void Postfix(Taskmaster __instance) {
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 92) {
                return;
            }

            Byte move = lastMove.get();
            if (move != null && move == 2) { // SCOURING_WHIP move
                AbstractDungeon.actionManager.addToBottom(
                    new MakeTempCardInDiscardAction(
                        (AbstractCard)new Wound(),
                        1
                    )
                );

                logger.info("Ascension 92: Taskmaster added 1 extra Wound from attack");
            }

            lastMove.remove();
        }
    }

    /**
     * Nemesis adds +2 extra Burn cards on TRI_BURN pattern at A92+
     * Base game: Burn 3 (A18: Burn 5)
     * A92+: Additional Burn 2 on TRI_BURN move
     */
    @SpirePatch(
        clz = Nemesis.class,
        method = "takeTurn"
    )
    public static class NemesisExtraBurns {
        private static final ThreadLocal<Byte> lastMove = new ThreadLocal<>();

        @SpirePrefixPatch
        public static void Prefix(Nemesis __instance) {
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 92) {
                return;
            }

            try {
                Field nextMoveField = AbstractMonster.class.getDeclaredField("nextMove");
                nextMoveField.setAccessible(true);
                byte move = nextMoveField.getByte(__instance);
                lastMove.set(move);
            } catch (Exception e) {
                logger.error("Failed to get Nemesis move", e);
            }
        }

        @SpirePostfixPatch
        public static void Postfix(Nemesis __instance) {
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 92) {
                return;
            }

            Byte move = lastMove.get();
            if (move != null && move == 4) { // TRI_BURN move
                AbstractDungeon.actionManager.addToBottom(
                    new MakeTempCardInDiscardAction(
                        (AbstractCard)new Burn(),
                        2
                    )
                );

                logger.info("Ascension 92: Nemesis added 2 extra Burns from debuff pattern");
            }

            lastMove.remove();
        }
    }

    /**
     * Book of Stabbing adds +1 extra stab attack on multi-stab pattern at A92+
     * Base game: Stabs based on stabCount (increases over time)
     * A92+: Additional 1 stab on STAB move
     *
     * Uses Prefix to increase stabCount BEFORE takeTurn executes,
     * so the original logic executes with +1 stabs automatically
     */
    @SpirePatch(
        clz = BookOfStabbing.class,
        method = "takeTurn"
    )
    public static class BookOfStabbingExtraStab {
        private static boolean increased = false;

        @SpirePrefixPatch
        public static void Prefix(BookOfStabbing __instance) {
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 92) {
                return;
            }

            try {
                // Check if STAB move (1)
                Field nextMoveField = AbstractMonster.class.getDeclaredField("nextMove");
                nextMoveField.setAccessible(true);
                byte move = nextMoveField.getByte(__instance);

                if (move == 1 && !increased) { // STAB move
                    // Increase stabCount by 1 BEFORE takeTurn executes
                    Field stabCountField = BookOfStabbing.class.getDeclaredField("stabCount");
                    stabCountField.setAccessible(true);
                    int originalCount = stabCountField.getInt(__instance);
                    stabCountField.setInt(__instance, originalCount + 1);
                    increased = true;

                    logger.info(String.format(
                        "Ascension 92: Book of Stabbing stabCount increased from %d to %d",
                        originalCount, originalCount + 1
                    ));
                }
            } catch (Exception e) {
                logger.error("Failed to increase Book of Stabbing stabCount", e);
            }
        }

        @SpirePostfixPatch
        public static void Postfix(BookOfStabbing __instance) {
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 92) {
                return;
            }

            try {
                // Reset stabCount back to original after STAB move
                Field nextMoveField = AbstractMonster.class.getDeclaredField("nextMove");
                nextMoveField.setAccessible(true);
                byte move = nextMoveField.getByte(__instance);

                if (move == 1 && increased) { // STAB move just executed
                    Field stabCountField = BookOfStabbing.class.getDeclaredField("stabCount");
                    stabCountField.setAccessible(true);
                    int currentCount = stabCountField.getInt(__instance);
                    stabCountField.setInt(__instance, currentCount - 1);
                    increased = false;

                    logger.info("Ascension 92: Book of Stabbing stabCount reset to original");
                }
            } catch (Exception e) {
                logger.error("Failed to reset Book of Stabbing stabCount", e);
            }
        }
    }

    /**
     * Gremlin Leader adds +4 extra Strength on ENCOURAGE pattern at A92+
     * Base game: Strength 3 (A3: 4, A18: 5) to all gremlins
     * A92+: Additional Strength 4 to all gremlins on ENCOURAGE move
     */
    @SpirePatch(
        clz = GremlinLeader.class,
        method = "takeTurn"
    )
    public static class GremlinLeaderExtraStrength {
        private static final ThreadLocal<Byte> lastMove = new ThreadLocal<>();

        @SpirePrefixPatch
        public static void Prefix(GremlinLeader __instance) {
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 92) {
                return;
            }

            try {
                Field nextMoveField = AbstractMonster.class.getDeclaredField("nextMove");
                nextMoveField.setAccessible(true);
                byte move = nextMoveField.getByte(__instance);
                lastMove.set(move);
            } catch (Exception e) {
                logger.error("Failed to get Gremlin Leader move", e);
            }
        }

        @SpirePostfixPatch
        public static void Postfix(GremlinLeader __instance) {
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 92) {
                return;
            }

            Byte move = lastMove.get();
            if (move != null && move == 3) { // ENCOURAGE move
                // Find and modify the last ApplyPowerAction with StrengthPower for each monster
                try {
                    // We need to modify strength for each gremlin
                    for (AbstractMonster m : (AbstractDungeon.getMonsters()).monsters) {
                        if (m.isDying) continue;

                        // Find the last StrengthPower action for this specific monster
                        for (int i = AbstractDungeon.actionManager.actions.size() - 1; i >= 0; i--) {
                            AbstractGameAction action = AbstractDungeon.actionManager.actions.get(i);

                            if (action instanceof ApplyPowerAction) {
                                Field powerToApplyField = ApplyPowerAction.class.getDeclaredField("powerToApply");
                                powerToApplyField.setAccessible(true);
                                AbstractPower power = (AbstractPower) powerToApplyField.get(action);

                                if (power instanceof StrengthPower && power.owner == m) {
                                    power.amount += 4;

                                    Field amountField = ApplyPowerAction.class.getDeclaredField("amount");
                                    amountField.setAccessible(true);
                                    int currentAmount = amountField.getInt(action);
                                    amountField.setInt(action, currentAmount + 4);

                                    logger.info(String.format(
                                        "Ascension 92: Gremlin Leader - %s Strength increased by +4 (total: %d)",
                                        m.name, power.amount
                                    ));
                                    break; // Found this monster's strength action, move to next monster
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    logger.error("Failed to modify Gremlin Leader Strength amount", e);
                }
            }

            lastMove.remove();
        }
    }

    /**
     * Lagavulin's Soul Sap (DEBUFF) pattern has enhanced debuffs at A92+
     * Base game: Dexterity -1/-2, Strength -1/-2
     * A92+: Additional Dexterity -3, Strength -3, Metallicize -2
     */
    @SpirePatch(
        clz = Lagavulin.class,
        method = "takeTurn"
    )
    public static class LagavulinEnhancedDebuff {
        private static final ThreadLocal<Byte> lastMove = new ThreadLocal<>();

        @SpirePrefixPatch
        public static void Prefix(Lagavulin __instance) {
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 92) {
                return;
            }

            try {
                Field nextMoveField = AbstractMonster.class.getDeclaredField("nextMove");
                nextMoveField.setAccessible(true);
                byte move = nextMoveField.getByte(__instance);
                lastMove.set(move);
            } catch (Exception e) {
                logger.error("Failed to get Lagavulin move", e);
            }
        }

        @SpirePostfixPatch
        public static void Postfix(Lagavulin __instance) {
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 92) {
                return;
            }

            Byte move = lastMove.get();
            if (move != null && move == 3) { // DEBUFF move (Soul Sap) is case 3, not 1
                // Find and modify the last ApplyPowerAction with DexterityPower and StrengthPower
                try {
                    boolean foundDex = false;
                    boolean foundStr = false;

                    for (int i = AbstractDungeon.actionManager.actions.size() - 1; i >= 0 && (!foundDex || !foundStr); i--) {
                        AbstractGameAction action = AbstractDungeon.actionManager.actions.get(i);

                        if (action instanceof ApplyPowerAction) {
                            Field powerToApplyField = ApplyPowerAction.class.getDeclaredField("powerToApply");
                            powerToApplyField.setAccessible(true);
                            AbstractPower power = (AbstractPower) powerToApplyField.get(action);

                            if (!foundDex && power instanceof DexterityPower && power.owner == AbstractDungeon.player) {
                                power.amount -= 3; // Make it more negative

                                Field amountField = ApplyPowerAction.class.getDeclaredField("amount");
                                amountField.setAccessible(true);
                                int currentAmount = amountField.getInt(action);
                                amountField.setInt(action, currentAmount - 3);

                                logger.info(String.format(
                                    "Ascension 92: Lagavulin Dexterity debuff increased by -3 (total: %d)",
                                    power.amount
                                ));
                                foundDex = true;
                            } else if (!foundStr && power instanceof StrengthPower && power.owner == AbstractDungeon.player) {
                                power.amount -= 3; // Make it more negative

                                Field amountField = ApplyPowerAction.class.getDeclaredField("amount");
                                amountField.setAccessible(true);
                                int currentAmount = amountField.getInt(action);
                                amountField.setInt(action, currentAmount - 3);

                                logger.info(String.format(
                                    "Ascension 92: Lagavulin Strength debuff increased by -3 (total: %d)",
                                    power.amount
                                ));
                                foundStr = true;
                            }
                        }
                    }

                    // Reduce Lagavulin's Metallicize by 2
                    if (__instance.hasPower("Metallicize")) {
                        AbstractDungeon.actionManager.addToBottom(
                            new ReducePowerAction(
                                (AbstractCreature)__instance,
                                (AbstractCreature)__instance,
                                "Metallicize",
                                2
                            )
                        );
                        logger.info("Ascension 92: Lagavulin Metallicize reduced by 2");
                    }
                } catch (Exception e) {
                    logger.error("Failed to modify Lagavulin debuff amount", e);
                }
            }

            lastMove.remove();
        }
    }

    /**
     * Snecko's Confusion bypasses Artifact at A92+
     * Base game: Confusion can be blocked by Artifact
     * A92+: Confusion is applied directly, bypassing Artifact
     */
    @SpirePatch(
        clz = Snecko.class,
        method = "takeTurn"
    )
    public static class SneckoConfusionBypassArtifact {
        private static final ThreadLocal<Byte> lastMove = new ThreadLocal<>();

        @SpirePrefixPatch
        public static void Prefix(Snecko __instance) {
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 92) {
                return;
            }

            try {
                Field nextMoveField = AbstractMonster.class.getDeclaredField("nextMove");
                nextMoveField.setAccessible(true);
                byte move = nextMoveField.getByte(__instance);
                lastMove.set(move);
            } catch (Exception e) {
                logger.error("Failed to get Snecko move", e);
            }
        }

        @SpirePostfixPatch
        public static void Postfix(Snecko __instance) {
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 92) {
                return;
            }

            Byte move = lastMove.get();
            if (move != null && move == 1) { // GLARE move
                // Force apply Confusion, bypassing Artifact
                // Apply directly to powers list instead of using ApplyPowerAction
                if (!AbstractDungeon.player.hasPower("Confusion")) {
                    ConfusionPower confusion = new ConfusionPower((AbstractCreature)AbstractDungeon.player);
                    AbstractDungeon.player.powers.add(confusion);
                    confusion.onInitialApplication();

                    logger.info("Ascension 92: Snecko applied Confusion bypassing Artifact");
                }
            }

            lastMove.remove();
        }
    }

    /**
     * Reptomancer: Summon pattern guarantees 4 daggers
     *
     * 소환 패턴에서 4마리 단검 보장
     */
    @SpirePatch(
        clz = Reptomancer.class,
        method = "takeTurn"
    )
    public static class ReptomancerGuaranteedDaggers {
        private static final ThreadLocal<Byte> lastMove = new ThreadLocal<>();

        @SpirePrefixPatch
        public static void Prefix(Reptomancer __instance) {
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 92) {
                return;
            }

            try {
                Field nextMoveField = AbstractMonster.class.getDeclaredField("nextMove");
                nextMoveField.setAccessible(true);
                byte move = nextMoveField.getByte(__instance);
                lastMove.set(move);
            } catch (Exception e) {
                logger.error("Failed to get Reptomancer move", e);
            }
        }

        @SpirePostfixPatch
        public static void Postfix(Reptomancer __instance) {
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 92) {
                return;
            }

            Byte move = lastMove.get();
            if (move != null && move == 2) { // SPAWN_DAGGER move
                try {
                    // Access private daggers array
                    Field daggersField = Reptomancer.class.getDeclaredField("daggers");
                    daggersField.setAccessible(true);
                    AbstractMonster[] daggers = (AbstractMonster[]) daggersField.get(__instance);

                    // Fill all empty dagger slots
                    for (int i = 0; i < daggers.length; i++) {
                        if (daggers[i] == null || daggers[i].isDeadOrEscaped()) {
                            SnakeDagger daggerToSpawn = new SnakeDagger(
                                Reptomancer.POSX[i],
                                Reptomancer.POSY[i]
                            );
                            daggers[i] = daggerToSpawn;
                            AbstractDungeon.actionManager.addToBottom(
                                new SpawnMonsterAction(daggerToSpawn, true)
                            );

                            logger.info(String.format(
                                "Ascension 92: Reptomancer spawned dagger at slot %d",
                                i
                            ));
                        }
                    }
                } catch (Exception e) {
                    logger.error("Failed to spawn additional daggers", e);
                }
            }

            lastMove.remove();
        }
    }

    /**
     * Slaver Red: Entangle probability increases with Taskmaster present
     *
     * Taskmaster가 있으면 Entangle 확률 증가 (25% → 50%)
     */
    @SpirePatch(
        clz = SlaverRed.class,
        method = "getMove"
    )
    public static class SlaverRedEnhancedEntangle {
        @SpirePrefixPatch
        public static SpireReturn<Void> Prefix(SlaverRed __instance, int num) {
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 92) {
                return SpireReturn.Continue();
            }

            try {
                // Check if Taskmaster is present
                boolean hasTaskmaster = false;
                for (AbstractMonster m : (AbstractDungeon.getMonsters()).monsters) {
                    if (m.id != null && m.id.equals("SlaverBoss") && !m.isDying) {
                        hasTaskmaster = true;
                        break;
                    }
                }

                if (!hasTaskmaster) {
                    return SpireReturn.Continue();
                }

                // Access private usedEntangle field
                Field usedEntangleField = SlaverRed.class.getDeclaredField("usedEntangle");
                usedEntangleField.setAccessible(true);
                boolean usedEntangle = usedEntangleField.getBoolean(__instance);

                // Access private firstTurn field
                Field firstTurnField = SlaverRed.class.getDeclaredField("firstTurn");
                firstTurnField.setAccessible(true);
                boolean firstTurn = firstTurnField.getBoolean(__instance);

                // If Taskmaster present and num >= 50 (instead of 75), force Entangle
                if (num >= 50 && num < 75 && !usedEntangle && !firstTurn) {
                    // Get ENTANGLE_NAME from MOVES array
                    Field movesField = SlaverRed.class.getDeclaredField("MOVES");
                    movesField.setAccessible(true);
                    String[] moves = (String[]) movesField.get(null);
                    String entangleName = moves[1]; // ENTANGLE_NAME = MOVES[1]

                    __instance.setMove(entangleName, (byte)2, AbstractMonster.Intent.STRONG_DEBUFF);

                    logger.info(String.format(
                        "Ascension 92: Slaver Red enhanced Entangle chance with Taskmaster (num=%d >= 50)",
                        num
                    ));

                    return SpireReturn.Return(null);
                }
            } catch (Exception e) {
                logger.error("Failed to enhance Slaver Red Entangle", e);
            }

            return SpireReturn.Continue();
        }
    }

    /**
     * Hexaghost: Inferno pattern adds 2 extra Burns to discard pile
     * Base game: INFERNO move (6) adds 3 upgraded Burns via BurnIncreaseAction
     * A92+: Additional 2 upgraded Burns to discard pile
     */
    @SpirePatch(
        clz = com.megacrit.cardcrawl.monsters.exordium.Hexaghost.class,
        method = "takeTurn"
    )
    public static class HexaghostInfernoExtraBurns {
        private static final ThreadLocal<Byte> lastMove = new ThreadLocal<>();

        @SpirePrefixPatch
        public static void Prefix(com.megacrit.cardcrawl.monsters.exordium.Hexaghost __instance) {
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 92) {
                return;
            }

            try {
                Field nextMoveField = AbstractMonster.class.getDeclaredField("nextMove");
                nextMoveField.setAccessible(true);
                byte move = nextMoveField.getByte(__instance);
                lastMove.set(move);
            } catch (Exception e) {
                logger.error("Failed to get Hexaghost move", e);
            }
        }

        @SpirePostfixPatch
        public static void Postfix(com.megacrit.cardcrawl.monsters.exordium.Hexaghost __instance) {
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 92) {
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

                logger.info("Ascension 92: Hexaghost Inferno added 2 extra Burns to discard pile");
            }

            lastMove.remove();
        }
    }

    /**
     * Awakened One: Curiosity +2
     * Base game: Curiosity 2 at Asc 19+
     * Level 27: +1 additional Curiosity (total 3)
     * Level 92: +2 additional Curiosity (total 5)
     */
    @SpirePatch(
        clz = AwakenedOne.class,
        method = "usePreBattleAction"
    )
    public static class AwakenedOneCuriosityPatch {
        @SpirePostfixPatch
        public static void Postfix(AwakenedOne __instance) {
            if (AbstractDungeon.isAscensionMode && AbstractDungeon.ascensionLevel >= 92) {
                // Add 2 Curiosity (base 2 + level 27 adds 1 = 3, then we add 2 more = 5 total)
                AbstractDungeon.actionManager.addToBottom(
                    new ApplyPowerAction(__instance, __instance,
                        new CuriosityPower(__instance, 2), 2)
                );
                logger.info("Ascension 92: AwakenedOne Curiosity +2 (total: 5)");
            }
        }
    }

    /**
     * Mugger: Smoke Bomb block +12
     */
    @SpirePatch(
        clz = Mugger.class,
        method = "takeTurn"
    )
    public static class MuggerSmokeBombBlockIncrease {
        @SpirePostfixPatch
        public static void Postfix(Mugger __instance) {
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 92) {
                return;
            }

            try {
                // Check if Smoke Bomb move (LUNGE move)
                Field nextMoveField = AbstractMonster.class.getDeclaredField("nextMove");
                nextMoveField.setAccessible(true);
                byte move = nextMoveField.getByte(__instance);

                if (move == 3) { // LUNGE (Smoke Bomb) move
                    // Increase escapeDef by 12 (in addition to Level53's +3)
                    Field escapeDefField = Mugger.class.getDeclaredField("escapeDef");
                    escapeDefField.setAccessible(true);
                    int currentEscapeDef = escapeDefField.getInt(__instance);
                    escapeDefField.setInt(__instance, currentEscapeDef + 12);

                    logger.info(String.format(
                        "Ascension 92: Mugger Smoke Bomb block increased by 12 to %d",
                        currentEscapeDef + 12
                    ));
                }
            } catch (Exception e) {
                logger.error("Failed to modify Mugger Smoke Bomb block", e);
            }
        }
    }

    // TODO: Pattern-specific changes require individual monster class patches:
    // - Gremlins: Shield +8, Mad +1 Anger, Fat vulnerability, etc.
    // - Shelled Parasite: Plated Armor +4
    // - Mugger: Lunge steal +20, Smoke Block +12
    // - Mystic: Debuff pattern Weak +2
    // - Snecko: Confusion bypasses artifact ✅
    // - Snake Plant: Malleable +2
    // - Repulsor: Dazed +1
    // - Spiker: Thorns +4
    // - Orb Walker: Burns to draw/discard pile ✅
    // - Darkling: Regrowth on revive 2 ✅
    // - Maw: Drool pattern Strength +3
    // - Writhing Mass: Malleable +5
    // - Reptomancer: Summon guarantees 4 daggers ✅
    // - Slaver Red: Enhanced Entangle with Taskmaster ✅
    // - Hexaghost: Inferno pattern Burns ✅
    // - Elite and Boss specific patterns (40+ different changes)
    //
    // These require extensive SpireInsertPatch with Locators for each monster's takeTurn()
    // or pattern-specific methods. Due to complexity, marked as TODO for future implementation.
}
