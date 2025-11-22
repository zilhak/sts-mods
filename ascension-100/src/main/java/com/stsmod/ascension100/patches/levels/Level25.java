package com.stsmod.ascension100.patches.levels;

import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePrefixPatch;
import com.evacipated.cardcrawl.modthespire.lib.SpireReturn;
import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.actions.common.ApplyPowerAction;
import com.megacrit.cardcrawl.actions.common.MakeTempCardInDiscardAction;
import com.megacrit.cardcrawl.actions.common.HealAction;
import com.megacrit.cardcrawl.cards.DamageInfo;
import com.megacrit.cardcrawl.cards.status.Slimed;
import com.megacrit.cardcrawl.cards.status.Dazed;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import com.megacrit.cardcrawl.monsters.exordium.*;
import com.megacrit.cardcrawl.monsters.city.*;
import com.megacrit.cardcrawl.monsters.beyond.*;
import com.megacrit.cardcrawl.powers.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Ascension Level 25: Enemy pattern enhancements
 *
 * 적들의 행동 패턴이 강화됩니다.
 */
public class Level25 {
    private static final Logger logger = LogManager.getLogger(Level25.class.getName());
    private static final float COMPOUND_RATE = 1.25f;  // 25% compound interest for Slow (Giant Head)

    // ========================================
    // 1막 적들
    // ========================================

    // Louse (Normal & Defensive): Curl Up +3
    // MOVED TO: LouseCurlUpPatch.java (unified patch)

    /**
     * Cultist: Damage -4 (rituals stronger, attacks weaker)
     */
    @SpirePatch(
        clz = Cultist.class,
        method = SpirePatch.CONSTRUCTOR,
        paramtypez = { float.class, float.class, boolean.class }
    )
    public static class CultistDamagePatch {
        @SpirePostfixPatch
        public static void Postfix(Cultist __instance, float x, float y, boolean talk) {
            if (AbstractDungeon.isAscensionMode && AbstractDungeon.ascensionLevel >= 25) {
                if (!__instance.damage.isEmpty()) {
                    DamageInfo damageInfo = __instance.damage.get(0);
                    int originalDamage = damageInfo.base;
                    damageInfo.base = Math.max(1, originalDamage - 4);
                    logger.info(String.format(
                        "Ascension 25: Cultist damage reduced from %d to %d",
                        originalDamage,
                        damageInfo.base
                    ));
                }
            }
        }
    }

    /**
     * Cultist: Incantation pattern grants +2 additional Ritual
     */
    @SpirePatch(
        clz = Cultist.class,
        method = "takeTurn"
    )
    public static class CultistRitualPatch {
        @SpirePostfixPatch
        public static void Postfix(Cultist __instance) {
            if (AbstractDungeon.isAscensionMode && AbstractDungeon.ascensionLevel >= 25) {
                // Check if Cultist just used Incantation (move 3)
                if (__instance.nextMove == 3) {
                    AbstractDungeon.actionManager.addToBottom(
                        new ApplyPowerAction(__instance, __instance,
                            new RitualPower(__instance, 2, false), 2)
                    );
                    logger.info("Ascension 25: Cultist gained +2 additional Ritual from Incantation");
                }
            }
        }
    }

    /**
     * Fungi Beast: SporeCloud +1 (applies additional Weak on death)
     */
    @SpirePatch(
        clz = FungiBeast.class,
        method = "usePreBattleAction"
    )
    public static class FungiBeastSporeCloudPatch {
        @SpirePostfixPatch
        public static void Postfix(FungiBeast __instance) {
            if (AbstractDungeon.isAscensionMode && AbstractDungeon.ascensionLevel >= 25) {
                AbstractDungeon.actionManager.addToBottom(
                    new ApplyPowerAction(__instance, __instance,
                        new SporeCloudPower(__instance, 1), 1)
                );
                logger.info("Ascension 25: FungiBeast gained +1 SporeCloud");
            }
        }
    }

    /**
     * Jaw Worm: Defense +3 on Strength gain
     */
    @SpirePatch(
        clz = JawWorm.class,
        method = "takeTurn"
    )
    public static class JawWormDefensePatch {
        @SpirePostfixPatch
        public static void Postfix(JawWorm __instance) {
            if (AbstractDungeon.isAscensionMode && AbstractDungeon.ascensionLevel >= 25) {
                if (__instance.nextMove == 2) { // Bellow move
                    AbstractDungeon.actionManager.addToBottom(
                        new com.megacrit.cardcrawl.actions.common.GainBlockAction(__instance, __instance, 3)
                    );
                    logger.info("Ascension 25: JawWorm gained 3 Block on Strength gain");
                }
            }
        }
    }

    // ========================================
    // Gremlins
    // ========================================

    /**
     * Gremlin Warrior: HP +3
     */
    @SpirePatch(
        clz = GremlinWarrior.class,
        method = SpirePatch.CONSTRUCTOR
    )
    public static class GremlinWarriorHPPatch {
        @SpirePostfixPatch
        public static void Postfix(GremlinWarrior __instance) {
            if (AbstractDungeon.isAscensionMode && AbstractDungeon.ascensionLevel >= 25) {
                __instance.maxHealth += 3;
                __instance.currentHealth += 3;
                logger.info("Ascension 25: GremlinWarrior HP +3");
            }
        }
    }

    /**
     * Gremlin Fat: HP +2
     */
    @SpirePatch(
        clz = GremlinFat.class,
        method = SpirePatch.CONSTRUCTOR
    )
    public static class GremlinFatHPPatch {
        @SpirePostfixPatch
        public static void Postfix(GremlinFat __instance) {
            if (AbstractDungeon.isAscensionMode && AbstractDungeon.ascensionLevel >= 25) {
                __instance.maxHealth += 2;
                __instance.currentHealth += 2;
                logger.info("Ascension 25: GremlinFat HP +2");
            }
        }
    }

    // GremlinThief patch moved to line 738 (GremlinThiefDamagePatch25) to avoid duplication

    /**
     * Gremlin Wizard: Damage +3
     */
    @SpirePatch(
        clz = GremlinWizard.class,
        method = SpirePatch.CONSTRUCTOR
    )
    public static class GremlinWizardDamagePatch {
        @SpirePostfixPatch
        public static void Postfix(GremlinWizard __instance) {
            if (AbstractDungeon.isAscensionMode && AbstractDungeon.ascensionLevel >= 25) {
                __instance.damage.forEach(damageInfo -> {
                    if (damageInfo != null && damageInfo.base > 0) {
                        damageInfo.base += 3;
                    }
                });
                logger.info("Ascension 25: GremlinWizard Damage +3");
            }
        }
    }

    // ========================================
    // Slimes
    // ========================================

    /**
     * SpikeSlime_M (가시슬라임 중): Flame Tackle adds 1 extra Slimed
     */
    @SpirePatch(
        clz = SpikeSlime_M.class,
        method = "takeTurn"
    )
    public static class SpikeSlimeMediumSlimedPatch {
        @SpirePostfixPatch
        public static void Postfix(SpikeSlime_M __instance) {
            if (AbstractDungeon.isAscensionMode && AbstractDungeon.ascensionLevel >= 25) {
                if (__instance.nextMove == 1) { // Flame Tackle (불꽃 태클)
                    AbstractDungeon.actionManager.addToBottom(
                        new MakeTempCardInDiscardAction(new Slimed(), 1)
                    );
                    logger.info("Ascension 25: Spike Slime (M) Flame Tackle added 1 extra Slimed card");
                }
            }
        }
    }

    /**
     * SpikeSlime_L (가시슬라임 대): Flame Tackle adds 1 extra Slimed
     */
    @SpirePatch(
        clz = SpikeSlime_L.class,
        method = "takeTurn"
    )
    public static class SpikeSlimeLargeSlimedPatch {
        @SpirePostfixPatch
        public static void Postfix(SpikeSlime_L __instance) {
            if (AbstractDungeon.isAscensionMode && AbstractDungeon.ascensionLevel >= 25) {
                if (__instance.nextMove == 1) { // Flame Tackle (불꽃 태클)
                    AbstractDungeon.actionManager.addToBottom(
                        new MakeTempCardInDiscardAction(new Slimed(), 1)
                    );
                    logger.info("Ascension 25: Spike Slime (L) Flame Tackle added 1 extra Slimed card");
                }
            }
        }
    }

    /**
     * AcidSlime_M (산성슬라임 중): Corrosive Spit adds 1 extra Slimed
     */
    @SpirePatch(
        clz = AcidSlime_M.class,
        method = "takeTurn"
    )
    public static class AcidSlimeMediumSlimedPatch {
        @SpirePostfixPatch
        public static void Postfix(AcidSlime_M __instance) {
            if (AbstractDungeon.isAscensionMode && AbstractDungeon.ascensionLevel >= 25) {
                if (__instance.nextMove == 1) { // Corrosive Spit (부식의 침)
                    AbstractDungeon.actionManager.addToBottom(
                        new MakeTempCardInDiscardAction(new Slimed(), 1)
                    );
                    logger.info("Ascension 25: Acid Slime (M) Corrosive Spit added 1 extra Slimed card");
                }
            }
        }
    }

    /**
     * AcidSlime_L (산성슬라임 대): Corrosive Spit adds 1 extra Slimed
     */
    @SpirePatch(
        clz = AcidSlime_L.class,
        method = "takeTurn"
    )
    public static class AcidSlimeLargeSlimedPatch {
        @SpirePostfixPatch
        public static void Postfix(AcidSlime_L __instance) {
            if (AbstractDungeon.isAscensionMode && AbstractDungeon.ascensionLevel >= 25) {
                if (__instance.nextMove == 1) { // Corrosive Spit (부식의 침)
                    AbstractDungeon.actionManager.addToBottom(
                        new MakeTempCardInDiscardAction(new Slimed(), 1)
                    );
                    logger.info("Ascension 25: Acid Slime (L) Corrosive Spit added 1 extra Slimed card");
                }
            }
        }
    }

    /**
     * Shape (AcidSlime_M): Dazed pattern adds 1 extra Dazed
     */
    @SpirePatch(
        clz = AcidSlime_M.class,
        method = "takeTurn"
    )
    public static class ShapeDazedPatch {
        @SpirePostfixPatch
        public static void Postfix(AcidSlime_M __instance) {
            if (AbstractDungeon.isAscensionMode && AbstractDungeon.ascensionLevel >= 25) {
                if (__instance.nextMove == 3) { // Corrosive Spit move
                    AbstractDungeon.actionManager.addToBottom(
                        new MakeTempCardInDiscardAction(new Dazed(), 1)
                    );
                    logger.info("Ascension 25: Shape added 1 extra Dazed card");
                }
            }
        }
    }

    // ========================================
    // 2막 적들
    // ========================================

    /**
     * Shelled Parasite: Plated Armor +2
     */
    @SpirePatch(
        clz = ShelledParasite.class,
        method = "usePreBattleAction"
    )
    public static class ShelledParasitePatch {
        @SpirePostfixPatch
        public static void Postfix(ShelledParasite __instance) {
            if (AbstractDungeon.isAscensionMode && AbstractDungeon.ascensionLevel >= 25) {
                AbstractDungeon.actionManager.addToBottom(
                    new ApplyPowerAction(__instance, __instance,
                        new PlatedArmorPower(__instance, 2), 2)
                );
                logger.info("Ascension 25: ShelledParasite Plated Armor +2");
            }
        }
    }

    /**
     * Chosen: Damage +2
     */
    @SpirePatch(
        clz = Chosen.class,
        method = SpirePatch.CONSTRUCTOR,
        paramtypez = { float.class, float.class }
    )
    public static class ChosenDamagePatch {
        @SpirePostfixPatch
        public static void Postfix(Chosen __instance, float x, float y) {
            if (AbstractDungeon.isAscensionMode && AbstractDungeon.ascensionLevel >= 25) {
                __instance.damage.forEach(damageInfo -> {
                    if (damageInfo != null && damageInfo.base > 0) {
                        damageInfo.base += 2;
                    }
                });
                logger.info("Ascension 25: Chosen Damage +2");
            }
        }
    }

    /**
     * Byrd: Strength gain +1
     */
    @SpirePatch(
        clz = Byrd.class,
        method = "takeTurn"
    )
    public static class ByrdStrengthPatch {
        private static final ThreadLocal<Byte> lastMove = new ThreadLocal<>();

        @SpirePrefixPatch
        public static void Prefix(Byrd __instance) {
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 25) {
                return;
            }

            try {
                // Store the current move before takeTurn executes
                java.lang.reflect.Field nextMoveField = AbstractMonster.class.getDeclaredField("nextMove");
                nextMoveField.setAccessible(true);
                byte move = nextMoveField.getByte(__instance);
                lastMove.set(move);
            } catch (Exception e) {
                logger.error("Failed to get Byrd move", e);
            }
        }

        @SpirePostfixPatch
        public static void Postfix(Byrd __instance) {
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 25) {
                return;
            }

            Byte move = lastMove.get();
            if (move != null && move == 6) { // Strength gain move (Caw pattern)
                // Find and modify the last ApplyPowerAction with StrengthPower
                try {
                    for (int i = AbstractDungeon.actionManager.actions.size() - 1; i >= 0; i--) {
                        AbstractGameAction action = AbstractDungeon.actionManager.actions.get(i);

                        if (action instanceof ApplyPowerAction) {
                            java.lang.reflect.Field powerToApplyField = ApplyPowerAction.class.getDeclaredField("powerToApply");
                            powerToApplyField.setAccessible(true);
                            AbstractPower power = (AbstractPower) powerToApplyField.get(action);

                            if (power instanceof StrengthPower && power.owner == __instance) {
                                power.amount += 1;

                                java.lang.reflect.Field amountField = ApplyPowerAction.class.getDeclaredField("amount");
                                amountField.setAccessible(true);
                                int currentAmount = amountField.getInt(action);
                                amountField.setInt(action, currentAmount + 1);

                                logger.info(String.format(
                                    "Ascension 25: Byrd Strength increased by +1 (total: %d)",
                                    power.amount
                                ));
                                break;
                            }
                        }
                    }
                } catch (Exception e) {
                    logger.error("Failed to modify Byrd Strength amount", e);
                }
            }

            lastMove.remove();
        }
    }

    /**
     * Centurion: Defend pattern block +5
     */
    @SpirePatch(
        clz = Centurion.class,
        method = SpirePatch.CONSTRUCTOR
    )
    public static class CenturionDefendPatch {
        @SpirePostfixPatch
        public static void Postfix(Centurion __instance, float x, float y) {
            if (AbstractDungeon.isAscensionMode && AbstractDungeon.ascensionLevel >= 25) {
                try {
                    // Increase blockAmount field by 5
                    java.lang.reflect.Field blockAmountField = Centurion.class.getDeclaredField("blockAmount");
                    blockAmountField.setAccessible(true);
                    int currentBlock = blockAmountField.getInt(__instance);
                    blockAmountField.setInt(__instance, currentBlock + 5);

                    logger.info(String.format(
                        "Ascension 25: Centurion defend block increased from %d to %d",
                        currentBlock, currentBlock + 5
                    ));
                } catch (Exception e) {
                    logger.error("Failed to modify Centurion blockAmount", e);
                }
            }
        }
    }

    /**
     * Healer: When alone, gains Strength +2 and heals 10% HP each turn
     */
    @SpirePatch(
        clz = Healer.class,
        method = "takeTurn"
    )
    public static class HealerAlonePatch {
        @SpirePostfixPatch
        public static void Postfix(Healer __instance) {
            if (AbstractDungeon.isAscensionMode && AbstractDungeon.ascensionLevel >= 25) {
                boolean isAlone = true;
                for (AbstractMonster m : AbstractDungeon.getMonsters().monsters) {
                    if (m != __instance && !m.isDying && !m.isEscaping) {
                        isAlone = false;
                        break;
                    }
                }

                if (isAlone) {
                    // Gain Strength +2
                    AbstractDungeon.actionManager.addToBottom(
                        new ApplyPowerAction(__instance, __instance,
                            new StrengthPower(__instance, 2), 2)
                    );

                    // Heal 10% max HP
                    int healAmount = (int) (__instance.maxHealth * 0.1f);
                    AbstractDungeon.actionManager.addToBottom(
                        new HealAction(__instance, __instance, healAmount)
                    );

                    logger.info(String.format(
                        "Ascension 25: Healer is alone, gained Strength +2 and healed %d HP (10%%)",
                        healAmount
                    ));
                }
            }
        }
    }

    /**
     * Snecko: Damage +2
     */
    @SpirePatch(
        clz = Snecko.class,
        method = SpirePatch.CONSTRUCTOR,
        paramtypez = { float.class, float.class }
    )
    public static class SneckoDamagePatch {
        @SpirePostfixPatch
        public static void Postfix(Snecko __instance, float x, float y) {
            if (AbstractDungeon.isAscensionMode && AbstractDungeon.ascensionLevel >= 25) {
                __instance.damage.forEach(damageInfo -> {
                    if (damageInfo != null && damageInfo.base > 0) {
                        damageInfo.base += 2;
                    }
                });
                logger.info("Ascension 25: Snecko Damage +2");
            }
        }
    }

    /**
     * Snake Plant: Base Malleable +1
     */
    @SpirePatch(
        clz = SnakePlant.class,
        method = "usePreBattleAction"
    )
    public static class SnakePlantMalleablePatch {
        @SpirePostfixPatch
        public static void Postfix(SnakePlant __instance) {
            if (AbstractDungeon.isAscensionMode && AbstractDungeon.ascensionLevel >= 25) {
                // Increase existing Malleable power by 1
                AbstractPower malleablePower = __instance.getPower("Malleable");
                if (malleablePower != null) {
                    malleablePower.amount += 1;
                    malleablePower.updateDescription();
                    logger.info(String.format(
                        "Ascension 25: SnakePlant Malleable increased by 1 to %d",
                        malleablePower.amount
                    ));
                }
            }
        }
    }

    // ========================================
    // Bandits
    // ========================================

    /**
     * Bandit Bear: HP +6
     */
    @SpirePatch(
        clz = BanditBear.class,
        method = SpirePatch.CONSTRUCTOR
    )
    public static class BanditBearHPPatch {
        @SpirePostfixPatch
        public static void Postfix(BanditBear __instance) {
            if (AbstractDungeon.isAscensionMode && AbstractDungeon.ascensionLevel >= 25) {
                __instance.maxHealth += 6;
                __instance.currentHealth += 6;
                logger.info("Ascension 25: BanditBear HP +6");
            }
        }
    }

    /**
     * Bandit Pointy: Damage +1
     */
    @SpirePatch(
        clz = BanditPointy.class,
        method = SpirePatch.CONSTRUCTOR
    )
    public static class BanditPointyDamagePatch {
        @SpirePostfixPatch
        public static void Postfix(BanditPointy __instance) {
            if (AbstractDungeon.isAscensionMode && AbstractDungeon.ascensionLevel >= 25) {
                __instance.damage.forEach(damageInfo -> {
                    if (damageInfo != null && damageInfo.base > 0) {
                        damageInfo.base += 1;
                    }
                });
                logger.info("Ascension 25: BanditPointy Damage +1");
            }
        }
    }

    // ========================================
    // 3막 적들
    // ========================================

    /**
     * Spiker: HP +5
     */
    @SpirePatch(
        clz = Spiker.class,
        method = SpirePatch.CONSTRUCTOR
    )
    public static class SpikerHPPatch {
        @SpirePostfixPatch
        public static void Postfix(Spiker __instance) {
            if (AbstractDungeon.isAscensionMode && AbstractDungeon.ascensionLevel >= 25) {
                __instance.maxHealth += 5;
                __instance.currentHealth += 5;
                logger.info("Ascension 25: Spiker HP +5");
            }
        }
    }

    /**
     * Exploder: HP +5
     */
    @SpirePatch(
        clz = Exploder.class,
        method = SpirePatch.CONSTRUCTOR
    )
    public static class ExploderHPPatch {
        @SpirePostfixPatch
        public static void Postfix(Exploder __instance) {
            if (AbstractDungeon.isAscensionMode && AbstractDungeon.ascensionLevel >= 25) {
                __instance.maxHealth += 5;
                __instance.currentHealth += 5;
                logger.info("Ascension 25: Exploder HP +5");
            }
        }
    }

    /**
     * Orb Walker: HP +6
     */
    @SpirePatch(
        clz = OrbWalker.class,
        method = SpirePatch.CONSTRUCTOR
    )
    public static class OrbWalkerHPPatch {
        @SpirePostfixPatch
        public static void Postfix(OrbWalker __instance) {
            if (AbstractDungeon.isAscensionMode && AbstractDungeon.ascensionLevel >= 25) {
                __instance.maxHealth += 6;
                __instance.currentHealth += 6;
                logger.info("Ascension 25: OrbWalker HP +6");
            }
        }
    }

    /**
     * Darkling: HP +10
     */
    @SpirePatch(
        clz = Darkling.class,
        method = SpirePatch.CONSTRUCTOR
    )
    public static class DarklingHPPatch {
        @SpirePostfixPatch
        public static void Postfix(Darkling __instance) {
            if (AbstractDungeon.isAscensionMode && AbstractDungeon.ascensionLevel >= 25) {
                __instance.maxHealth += 10;
                __instance.currentHealth += 10;
                logger.info("Ascension 25: Darkling HP +10");
            }
        }
    }

    /**
     * Maw: HP +50
     */
    @SpirePatch(
        clz = Maw.class,
        method = SpirePatch.CONSTRUCTOR
    )
    public static class MawHPPatch {
        @SpirePostfixPatch
        public static void Postfix(Maw __instance) {
            if (AbstractDungeon.isAscensionMode && AbstractDungeon.ascensionLevel >= 25) {
                __instance.maxHealth += 50;
                __instance.currentHealth += 50;
                logger.info("Ascension 25: Maw HP +50");
            }
        }
    }

    /**
     * Spire Growth: Damage +5
     */
    @SpirePatch(
        clz = SpireGrowth.class,
        method = SpirePatch.CONSTRUCTOR
    )
    public static class SpireGrowthDamagePatch {
        @SpirePostfixPatch
        public static void Postfix(SpireGrowth __instance) {
            if (AbstractDungeon.isAscensionMode && AbstractDungeon.ascensionLevel >= 25) {
                __instance.damage.forEach(damageInfo -> {
                    if (damageInfo != null && damageInfo.base > 0) {
                        damageInfo.base += 5;
                    }
                });
                logger.info("Ascension 25: SpireGrowth Damage +5");
            }
        }
    }

    // ========================================
    // Additional Act 1 Enemies
    // ========================================

    /**
     * GremlinTsundere (방패 그렘린): Block amount +5
     */
    @SpirePatch(
        clz = GremlinTsundere.class,
        method = SpirePatch.CONSTRUCTOR
    )
    public static class GremlinTsundereBlockPatch {
        @SpirePostfixPatch
        public static void Postfix(GremlinTsundere __instance) {
            if (AbstractDungeon.isAscensionMode && AbstractDungeon.ascensionLevel >= 25) {
                try {
                    // Increase blockAmt field by 5
                    java.lang.reflect.Field blockAmtField = GremlinTsundere.class.getDeclaredField("blockAmt");
                    blockAmtField.setAccessible(true);
                    int currentBlockAmt = blockAmtField.getInt(__instance);
                    blockAmtField.setInt(__instance, currentBlockAmt + 5);

                    logger.info(String.format(
                        "Ascension 25: GremlinTsundere blockAmt increased from %d to %d",
                        currentBlockAmt, currentBlockAmt + 5
                    ));
                } catch (Exception e) {
                    logger.error("Failed to modify GremlinTsundere blockAmt", e);
                }
            }
        }
    }

    /**
     * GremlinThief (교활한 그렘린): Damage +1
     */
    @SpirePatch(
        clz = GremlinThief.class,
        method = SpirePatch.CONSTRUCTOR
    )
    public static class GremlinThiefDamagePatch25 {
        @SpirePostfixPatch
        public static void Postfix(GremlinThief __instance) {
            if (AbstractDungeon.isAscensionMode && AbstractDungeon.ascensionLevel >= 25) {
                try {
                    // Increase thiefDamage field by 1
                    java.lang.reflect.Field thiefDamageField = GremlinThief.class.getDeclaredField("thiefDamage");
                    thiefDamageField.setAccessible(true);
                    int currentDamage = thiefDamageField.getInt(__instance);
                    thiefDamageField.setInt(__instance, currentDamage + 1);

                    // Update damage info
                    if (!__instance.damage.isEmpty()) {
                        __instance.damage.get(0).base += 1;
                    }

                    logger.info(String.format(
                        "Ascension 25: GremlinThief damage increased from %d to %d",
                        currentDamage, currentDamage + 1
                    ));
                } catch (Exception e) {
                    logger.error("Failed to modify GremlinThief damage", e);
                }
            }
        }
    }

    /**
     * Looter (도적): Thief +5
     */
    @SpirePatch(
        clz = Looter.class,
        method = SpirePatch.CONSTRUCTOR
    )
    public static class LooterThieftPatch {
        @SpirePostfixPatch
        public static void Postfix(Looter __instance, float x, float y) {
            if (AbstractDungeon.isAscensionMode && AbstractDungeon.ascensionLevel >= 25) {
                // Increase goldAmt by 5 (this determines Thievery power amount)
                try {
                    java.lang.reflect.Field goldAmtField = Looter.class.getDeclaredField("goldAmt");
                    goldAmtField.setAccessible(true);
                    int currentGoldAmt = goldAmtField.getInt(__instance);
                    goldAmtField.setInt(__instance, currentGoldAmt + 5);
                    logger.info(String.format(
                        "Ascension 25: Looter goldAmt (Thievery) increased from %d to %d (+5)",
                        currentGoldAmt, currentGoldAmt + 5
                    ));
                } catch (Exception e) {
                    logger.error("Failed to modify Looter goldAmt", e);
                }
            }
        }
    }

    /**
     * Slaver Blue (푸른색 노예 상인): Damage +1
     */
    @SpirePatch(
        clz = SlaverBlue.class,
        method = SpirePatch.CONSTRUCTOR
    )
    public static class SlaverBlueDamagePatch {
        @SpirePostfixPatch
        public static void Postfix(SlaverBlue __instance, float x, float y) {
            if (AbstractDungeon.isAscensionMode && AbstractDungeon.ascensionLevel >= 25) {
                // Increase Stab damage (+1)
                if (!__instance.damage.isEmpty()) {
                    DamageInfo stabDamage = __instance.damage.get(0); // Stab attack
                    int originalDamage = stabDamage.base;
                    stabDamage.base += 1;

                    logger.info(String.format(
                        "Ascension 25: Slaver Blue Stab damage increased from %d to %d",
                        originalDamage, stabDamage.base
                    ));
                }
            }
        }
    }

    /**
     * Slaver Red (붉은색 노예 상인): Unlimited Entangle usage
     */
    @SpirePatch(
        clz = SlaverRed.class,
        method = "takeTurn"
    )
    public static class SlaverRedEntanglePatch {
        @SpirePostfixPatch
        public static void Postfix(SlaverRed __instance) {
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 25) {
                return;
            }

            try {
                // Check if Entangle move (byte 2) was just used
                java.lang.reflect.Field nextMoveField = AbstractMonster.class.getDeclaredField("nextMove");
                nextMoveField.setAccessible(true);
                byte nextMove = nextMoveField.getByte(__instance);

                if (nextMove == 2) { // Entangle move
                    // Reset usedEntangle flag to allow unlimited use
                    java.lang.reflect.Field usedEntangleField = SlaverRed.class.getDeclaredField("usedEntangle");
                    usedEntangleField.setAccessible(true);
                    usedEntangleField.setBoolean(__instance, false);

                    logger.info("Ascension 25: Slaver Red Entangle flag reset - unlimited usage enabled");
                }
            } catch (Exception e) {
                logger.error("Failed to reset Slaver Red usedEntangle flag", e);
            }
        }
    }

    // ========================================
    // Additional Act 2 Enemies
    // ========================================

    /**
     * SphericGuardian (구체형 수호기): Block +15, HP -5
     */
    @SpirePatch(
        clz = SphericGuardian.class,
        method = SpirePatch.CONSTRUCTOR,
        paramtypez = {float.class, float.class}
    )
    public static class SphericGuardianStatsPatch {
        @SpirePostfixPatch
        public static void Postfix(SphericGuardian __instance, float x, float y) {
            if (AbstractDungeon.isAscensionMode && AbstractDungeon.ascensionLevel >= 25) {
                // Reduce HP by 5
                __instance.maxHealth -= 5;
                __instance.currentHealth -= 5;
                logger.info("Ascension 25: SphericGuardian HP -5");
            }
        }
    }

    @SpirePatch(
        clz = SphericGuardian.class,
        method = "usePreBattleAction"
    )
    public static class SphericGuardianBlockPatch {
        @SpirePostfixPatch
        public static void Postfix(SphericGuardian __instance) {
            if (AbstractDungeon.isAscensionMode && AbstractDungeon.ascensionLevel >= 25) {
                // Add extra 15 block to the initial 40 block (total 55)
                AbstractDungeon.actionManager.addToBottom(
                    new com.megacrit.cardcrawl.actions.common.GainBlockAction(__instance, __instance, 15)
                );
                logger.info("Ascension 25: SphericGuardian gained +15 initial block (total 55)");
            }
        }
    }

    /**
     * Mugger (강도): Thief +10
     */
    @SpirePatch(
        clz = Mugger.class,
        method = SpirePatch.CONSTRUCTOR
    )
    public static class MuggerThieftPatch {
        @SpirePostfixPatch
        public static void Postfix(Mugger __instance, float x, float y) {
            if (AbstractDungeon.isAscensionMode && AbstractDungeon.ascensionLevel >= 25) {
                // Increase goldAmt by 10 (this determines Thievery power amount)
                try {
                    java.lang.reflect.Field goldAmtField = Mugger.class.getDeclaredField("goldAmt");
                    goldAmtField.setAccessible(true);
                    int currentGoldAmt = goldAmtField.getInt(__instance);
                    goldAmtField.setInt(__instance, currentGoldAmt + 10);
                    logger.info(String.format(
                        "Ascension 25: Mugger goldAmt (Thievery) increased from %d to %d (+10)",
                        currentGoldAmt, currentGoldAmt + 10
                    ));
                } catch (Exception e) {
                    logger.error("Failed to modify Mugger goldAmt", e);
                }
            }
        }
    }

    // ========================================
    // Additional Act 3 Enemies
    // ========================================

    /**
     * Transient (과도자): Fades one turn later
     */
    @SpirePatch(
        clz = Transient.class,
        method = "usePreBattleAction"
    )
    public static class TransientFadePatch {
        @SpirePostfixPatch
        public static void Postfix(Transient __instance) {
            if (AbstractDungeon.isAscensionMode && AbstractDungeon.ascensionLevel >= 25) {
                // Increase FadingPower amount by 1 (delays fading by 1 turn)
                AbstractPower fadingPower = __instance.getPower("Fading");
                if (fadingPower != null) {
                    fadingPower.amount += 1;
                    fadingPower.updateDescription();
                    logger.info(String.format(
                        "Ascension 25: Transient Fading increased by 1 to %d turns",
                        fadingPower.amount
                    ));
                }
            }
        }
    }

    /**
     * WrithingMass (꿈틀대는 덩어리): Parasite spawn probability increases
     * Increases parasite spawn chance by modifying getMove probability range from 10-19 (10%) to 10-34 (25%)
     */
    @SpirePatch(
        clz = WrithingMass.class,
        method = "getMove",
        paramtypez = { int.class }
    )
    public static class WrithingMassParasitePatch {
        @SpirePrefixPatch
        public static void Prefix(WrithingMass __instance, @com.evacipated.cardcrawl.modthespire.lib.ByRef int[] num) {
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 25) {
                return;
            }

            try {
                // Check if firstMove is false (not the first turn)
                java.lang.reflect.Field firstMoveField = WrithingMass.class.getDeclaredField("firstMove");
                firstMoveField.setAccessible(true);
                boolean firstMove = firstMoveField.getBoolean(__instance);

                if (!firstMove && num[0] >= 20 && num[0] < 35) {
                    // 60% chance to shift num into parasite spawn range (10-19)
                    if (com.badlogic.gdx.math.MathUtils.randomBoolean(0.6f)) {
                        num[0] = com.badlogic.gdx.math.MathUtils.random(10, 19);
                        logger.info(String.format(
                            "Ascension 25: WrithingMass getMove adjusted to %d (increased parasite probability)",
                            num[0]
                        ));
                    }
                }
            } catch (Exception e) {
                logger.error("Failed to modify WrithingMass parasite probability", e);
            }
        }
    }

    // ========================================
    // Giant Head (거인의 머리) - A92 mechanics moved to A25
    // ========================================

    /**
     * Giant Head: Slow debuff uses compound calculation (25% per card played)
     * 거인의 머리(Giant Head): 둔화 디버프의 효과가 "카드 사용시마다 데미지 25% 증가"로 변경
     */
    @SpirePatch(
        clz = SlowPower.class,
        method = "atDamageReceive"
    )
    public static class GiantHeadSlowPowerCompound {
        @SpirePrefixPatch
        public static SpireReturn<Float> Prefix(SlowPower __instance, float damage, DamageInfo.DamageType type) {
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 25) {
                return SpireReturn.Continue();
            }

            if (type == DamageInfo.DamageType.NORMAL) {
                float multiplier = (float) Math.pow(COMPOUND_RATE, __instance.amount);
                float newDamage = damage * multiplier;

                logger.info(String.format(
                    "Ascension 25: Giant Head Slow debuff (compound) - stacks: %d, multiplier: %.2f, damage: %.1f → %.1f",
                    __instance.amount, multiplier, damage, newDamage
                ));

                return SpireReturn.Return(newDamage);
            }

            return SpireReturn.Return(damage);
        }
    }

    /**
     * Giant Head: HP increased by 100%
     * 거인의 머리(Giant Head): 체력이 100% 증가
     */
    @SpirePatch(
        clz = AbstractMonster.class,
        method = "init"
    )
    public static class GiantHeadHPIncrease {
        @SpirePostfixPatch
        public static void Postfix(AbstractMonster __instance) {
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 25) {
                return;
            }

            if (__instance.id != null && __instance.id.equals("GiantHead")) {
                // Giant Head gets 100% HP increase
                int originalHP = __instance.maxHealth;
                __instance.maxHealth *= 2;
                __instance.currentHealth *= 2;

                logger.info(String.format(
                    "Ascension 25: %s HP doubled from %d to %d",
                    __instance.name, originalHP, __instance.maxHealth
                ));
            }
        }
    }
}
