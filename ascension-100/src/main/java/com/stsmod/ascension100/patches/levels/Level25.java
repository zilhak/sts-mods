package com.stsmod.ascension100.patches.levels;

import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePrefixPatch;
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

    // ========================================
    // 1막 적들
    // ========================================

    /**
     * Louse (Normal & Defensive): Curl Up +3
     * Directly increases existing Curl Up power to ensure stacking
     */
    @SpirePatch(
        clz = AbstractMonster.class,
        method = "usePreBattleAction"
    )
    public static class LouseCurlUpPatch25 {
        @SpirePostfixPatch
        public static void Postfix(AbstractMonster __instance) {
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 25) {
                return;
            }

            String id = __instance.id;
            if (id == null) return;

            // Louse: Increase existing Curl Up power by 3
            if (id.equals("FuzzyLouseNormal") || id.equals("FuzzyLouseDefensive")) {
                AbstractPower curlUp = __instance.getPower("Curl Up");
                if (curlUp != null) {
                    int originalAmount = curlUp.amount;
                    curlUp.amount += 3;
                    curlUp.updateDescription();
                    logger.info(String.format(
                        "Ascension 25: %s Curl Up increased from %d to %d (+3)",
                        __instance.name, originalAmount, curlUp.amount
                    ));
                }
            }
        }
    }

    /**
     * Cultist: Damage -2 (rituals stronger, attacks weaker)
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
                    damageInfo.base = Math.max(1, originalDamage - 2);
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
     * Jaw Worm: Defense +12 on Strength gain
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
                        new com.megacrit.cardcrawl.actions.common.GainBlockAction(__instance, __instance, 12)
                    );
                    logger.info("Ascension 25: JawWorm gained 12 Block on Strength gain");
                }
            }
        }
    }

    // ========================================
    // Gremlins
    // ========================================

    /**
     * Gremlin Warrior: HP +5
     */
    @SpirePatch(
        clz = GremlinWarrior.class,
        method = SpirePatch.CONSTRUCTOR
    )
    public static class GremlinWarriorHPPatch {
        @SpirePostfixPatch
        public static void Postfix(GremlinWarrior __instance) {
            if (AbstractDungeon.isAscensionMode && AbstractDungeon.ascensionLevel >= 25) {
                __instance.maxHealth += 5;
                __instance.currentHealth += 5;
                logger.info("Ascension 25: GremlinWarrior HP +5");
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

    /**
     * Gremlin Thief: Damage +2
     */
    @SpirePatch(
        clz = GremlinThief.class,
        method = SpirePatch.CONSTRUCTOR
    )
    public static class GremlinThiefDamagePatch {
        @SpirePostfixPatch
        public static void Postfix(GremlinThief __instance) {
            if (AbstractDungeon.isAscensionMode && AbstractDungeon.ascensionLevel >= 25) {
                __instance.damage.forEach(damageInfo -> {
                    if (damageInfo != null && damageInfo.base > 0) {
                        damageInfo.base += 2;
                    }
                });
                logger.info("Ascension 25: GremlinThief Damage +2");
            }
        }
    }

    /**
     * Gremlin Wizard: Damage +5
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
                        damageInfo.base += 5;
                    }
                });
                logger.info("Ascension 25: GremlinWizard Damage +5");
            }
        }
    }

    // ========================================
    // Slimes
    // ========================================

    /**
     * Slimes: Add 2 extra Slimed cards
     */
    @SpirePatch(
        clz = AcidSlime_M.class,
        method = "takeTurn"
    )
    public static class SlimeMediumSlimedPatch {
        @SpirePostfixPatch
        public static void Postfix(AcidSlime_M __instance) {
            if (AbstractDungeon.isAscensionMode && AbstractDungeon.ascensionLevel >= 25) {
                if (__instance.nextMove == 1) { // Lick move
                    AbstractDungeon.actionManager.addToBottom(
                        new MakeTempCardInDiscardAction(new Slimed(), 2)
                    );
                    logger.info("Ascension 25: Medium Slime added 2 extra Slimed cards");
                }
            }
        }
    }

    @SpirePatch(
        clz = AcidSlime_L.class,
        method = "takeTurn"
    )
    public static class SlimeLargeSlimedPatch {
        @SpirePostfixPatch
        public static void Postfix(AcidSlime_L __instance) {
            if (AbstractDungeon.isAscensionMode && AbstractDungeon.ascensionLevel >= 25) {
                if (__instance.nextMove == 1) { // Lick move
                    AbstractDungeon.actionManager.addToBottom(
                        new MakeTempCardInDiscardAction(new Slimed(), 2)
                    );
                    logger.info("Ascension 25: Large Slime added 2 extra Slimed cards");
                }
            }
        }
    }

    @SpirePatch(
        clz = SpikeSlime_M.class,
        method = "takeTurn"
    )
    public static class SpikeSlimeMediumSlimedPatch {
        @SpirePostfixPatch
        public static void Postfix(SpikeSlime_M __instance) {
            if (AbstractDungeon.isAscensionMode && AbstractDungeon.ascensionLevel >= 25) {
                if (__instance.nextMove == 1) { // Lick move
                    AbstractDungeon.actionManager.addToBottom(
                        new MakeTempCardInDiscardAction(new Slimed(), 2)
                    );
                    logger.info("Ascension 25: Spike Slime (M) added 2 extra Slimed cards");
                }
            }
        }
    }

    @SpirePatch(
        clz = SpikeSlime_L.class,
        method = "takeTurn"
    )
    public static class SpikeSlimeLargeSlimedPatch {
        @SpirePostfixPatch
        public static void Postfix(SpikeSlime_L __instance) {
            if (AbstractDungeon.isAscensionMode && AbstractDungeon.ascensionLevel >= 25) {
                if (__instance.nextMove == 1) { // Lick move
                    AbstractDungeon.actionManager.addToBottom(
                        new MakeTempCardInDiscardAction(new Slimed(), 2)
                    );
                    logger.info("Ascension 25: Spike Slime (L) added 2 extra Slimed cards");
                }
            }
        }
    }

    /**
     * Shape (AcidSlime_M): Dazed pattern adds 3 Dazed total
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
                        new MakeTempCardInDiscardAction(new Dazed(), 2)
                    );
                    logger.info("Ascension 25: Shape added 2 extra Dazed cards");
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
            if (move != null && move == 4) { // Strength gain move (FLY pattern)
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
     * Centurion: Heals for unblocked damage dealt
     */
    @SpirePatch(
        clz = Centurion.class,
        method = "damage"
    )
    public static class CenturionHealPatch {
        @SpirePostfixPatch
        public static void Postfix(Centurion __instance, DamageInfo info) {
            if (AbstractDungeon.isAscensionMode && AbstractDungeon.ascensionLevel >= 25) {
                if (info.owner == __instance && info.type == DamageInfo.DamageType.NORMAL) {
                    int unblocked = info.output;
                    if (unblocked > 0) {
                        AbstractDungeon.actionManager.addToTop(
                            new HealAction(__instance, __instance, unblocked)
                        );
                        logger.info(String.format("Ascension 25: Centurion healed %d HP", unblocked));
                    }
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
     * Bandit Bear: HP +10
     */
    @SpirePatch(
        clz = BanditBear.class,
        method = SpirePatch.CONSTRUCTOR
    )
    public static class BanditBearHPPatch {
        @SpirePostfixPatch
        public static void Postfix(BanditBear __instance) {
            if (AbstractDungeon.isAscensionMode && AbstractDungeon.ascensionLevel >= 25) {
                __instance.maxHealth += 10;
                __instance.currentHealth += 10;
                logger.info("Ascension 25: BanditBear HP +10");
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
     * Orb Walker: HP +10
     */
    @SpirePatch(
        clz = OrbWalker.class,
        method = SpirePatch.CONSTRUCTOR
    )
    public static class OrbWalkerHPPatch {
        @SpirePostfixPatch
        public static void Postfix(OrbWalker __instance) {
            if (AbstractDungeon.isAscensionMode && AbstractDungeon.ascensionLevel >= 25) {
                __instance.maxHealth += 10;
                __instance.currentHealth += 10;
                logger.info("Ascension 25: OrbWalker HP +10");
            }
        }
    }

    /**
     * Darkling: HP +25
     */
    @SpirePatch(
        clz = Darkling.class,
        method = SpirePatch.CONSTRUCTOR
    )
    public static class DarklingHPPatch {
        @SpirePostfixPatch
        public static void Postfix(Darkling __instance) {
            if (AbstractDungeon.isAscensionMode && AbstractDungeon.ascensionLevel >= 25) {
                __instance.maxHealth += 25;
                __instance.currentHealth += 25;
                logger.info("Ascension 25: Darkling HP +25");
            }
        }
    }

    /**
     * Maw: HP +100
     */
    @SpirePatch(
        clz = Maw.class,
        method = SpirePatch.CONSTRUCTOR
    )
    public static class MawHPPatch {
        @SpirePostfixPatch
        public static void Postfix(Maw __instance) {
            if (AbstractDungeon.isAscensionMode && AbstractDungeon.ascensionLevel >= 25) {
                __instance.maxHealth += 100;
                __instance.currentHealth += 100;
                logger.info("Ascension 25: Maw HP +100");
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
     * GremlinTsundere (방패 그렘린): Block amount +7
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
                    // Increase blockAmt field by 7
                    java.lang.reflect.Field blockAmtField = GremlinTsundere.class.getDeclaredField("blockAmt");
                    blockAmtField.setAccessible(true);
                    int currentBlockAmt = blockAmtField.getInt(__instance);
                    blockAmtField.setInt(__instance, currentBlockAmt + 7);

                    logger.info(String.format(
                        "Ascension 25: GremlinTsundere blockAmt increased from %d to %d",
                        currentBlockAmt, currentBlockAmt + 7
                    ));
                } catch (Exception e) {
                    logger.error("Failed to modify GremlinTsundere blockAmt", e);
                }
            }
        }
    }

    /**
     * GremlinThief (교활한 그렘린): Damage +2
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
                    // Increase thiefDamage field by 2
                    java.lang.reflect.Field thiefDamageField = GremlinThief.class.getDeclaredField("thiefDamage");
                    thiefDamageField.setAccessible(true);
                    int currentDamage = thiefDamageField.getInt(__instance);
                    thiefDamageField.setInt(__instance, currentDamage + 2);

                    // Update damage info
                    if (!__instance.damage.isEmpty()) {
                        __instance.damage.get(0).base += 2;
                    }

                    logger.info(String.format(
                        "Ascension 25: GremlinThief damage increased from %d to %d",
                        currentDamage, currentDamage + 2
                    ));
                } catch (Exception e) {
                    logger.error("Failed to modify GremlinThief damage", e);
                }
            }
        }
    }

    /**
     * Looter (도적): Thief +3
     */
    @SpirePatch(
        clz = Looter.class,
        method = "usePreBattleAction"
    )
    public static class LooterThieftPatch {
        @SpirePostfixPatch
        public static void Postfix(Looter __instance) {
            if (AbstractDungeon.isAscensionMode && AbstractDungeon.ascensionLevel >= 25) {
                // Increase Thievery power by 3
                AbstractPower thieveryPower = __instance.getPower("Thievery");
                if (thieveryPower != null) {
                    thieveryPower.amount += 3;
                    thieveryPower.updateDescription();
                    logger.info(String.format(
                        "Ascension 25: Looter Thievery increased by 3 to %d",
                        thieveryPower.amount
                    ));
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
     * SphericGuardian (구체형 수호기): Block +15
     */
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
        method = "usePreBattleAction"
    )
    public static class MuggerThieftPatch {
        @SpirePostfixPatch
        public static void Postfix(Mugger __instance) {
            if (AbstractDungeon.isAscensionMode && AbstractDungeon.ascensionLevel >= 25) {
                // Increase Thievery power by 10
                AbstractPower thieveryPower = __instance.getPower("Thievery");
                if (thieveryPower != null) {
                    thieveryPower.amount += 10;
                    thieveryPower.updateDescription();
                    logger.info(String.format(
                        "Ascension 25: Mugger Thievery increased by 10 to %d",
                        thieveryPower.amount
                    ));
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
}
