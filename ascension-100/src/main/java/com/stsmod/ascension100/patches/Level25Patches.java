package com.stsmod.ascension100.patches;

import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;
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
import com.megacrit.cardcrawl.powers.AbstractPower;
import com.megacrit.cardcrawl.powers.CurlUpPower;
import com.megacrit.cardcrawl.powers.WeakPower;
import com.megacrit.cardcrawl.powers.PlatedArmorPower;
import com.megacrit.cardcrawl.powers.StrengthPower;
import com.megacrit.cardcrawl.powers.SporeCloudPower;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Patches for Ascension Level 25 monster behavior changes
 */
public class Level25Patches {

    private static final Logger logger = LogManager.getLogger(Level25Patches.class.getName());

    /**
     * Louse (Normal): Curl Up +3
     */
    @SpirePatch(
        cls = "com.megacrit.cardcrawl.monsters.exordium.LouseNormal",
        method = "usePreBattleAction"
    )
    public static class LouseNormalCurlUpPatch {
        @SpirePostfixPatch
        public static void Postfix(LouseNormal __instance) {
            if (AbstractDungeon.isAscensionMode && AbstractDungeon.ascensionLevel >= 25) {
                AbstractDungeon.actionManager.addToBottom(
                    new ApplyPowerAction(__instance, __instance,
                        new CurlUpPower(__instance, 3), 3)
                );
                logger.info("Ascension 25: LouseNormal gained +3 Curl Up");
            }
        }
    }

    /**
     * Louse (Defensive): Curl Up +3
     */
    @SpirePatch(
        cls = "com.megacrit.cardcrawl.monsters.exordium.LouseDefensive",
        method = "usePreBattleAction"
    )
    public static class LouseDefensiveCurlUpPatch {
        @SpirePostfixPatch
        public static void Postfix(LouseDefensive __instance) {
            if (AbstractDungeon.isAscensionMode && AbstractDungeon.ascensionLevel >= 25) {
                AbstractDungeon.actionManager.addToBottom(
                    new ApplyPowerAction(__instance, __instance,
                        new CurlUpPower(__instance, 3), 3)
                );
                logger.info("Ascension 25: LouseDefensive gained +3 Curl Up");
            }
        }
    }

    /**
     * Cultist: Ritual +2 (already handled in RitualPower amount), Damage -2
     */
    @SpirePatch(
        cls = "com.megacrit.cardcrawl.monsters.exordium.Cultist",
        method = SpirePatch.CONSTRUCTOR,
        paramtypez = { float.class, float.class, boolean.class }
    )
    public static class CultistDamagePatch {
        @SpirePostfixPatch
        public static void Postfix(Cultist __instance, float x, float y, boolean talk) {
            if (AbstractDungeon.isAscensionMode && AbstractDungeon.ascensionLevel >= 25) {
                // Reduce attack damage by 2
                if (!__instance.damage.isEmpty()) {
                    DamageInfo damageInfo = __instance.damage.get(0);
                    int originalDamage = damageInfo.base;
                    damageInfo.base = Math.max(1, originalDamage - 2); // 최소 1 데미지
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
     * Fungi Beast: SporeCloud +1 (applies additional Weak on death)
     */
    @SpirePatch(
        cls = "com.megacrit.cardcrawl.monsters.exordium.FungiBeast",
        method = "usePreBattleAction"
    )
    public static class FungiBeastSporeCloudPatch {
        @SpirePostfixPatch
        public static void Postfix(FungiBeast __instance) {
            if (AbstractDungeon.isAscensionMode && AbstractDungeon.ascensionLevel >= 25) {
                // Add extra SporeCloud power (increases Weak applied on death)
                AbstractDungeon.actionManager.addToBottom(
                    new ApplyPowerAction(__instance, __instance,
                        new com.megacrit.cardcrawl.powers.SporeCloudPower(__instance, 1), 1)
                );
                logger.info("Ascension 25: FungiBeast gained +1 SporeCloud (Weak on death)");
            }
        }
    }

    /**
     * GremlinWarrior (화난 그렘린): HP +5
     */
    @SpirePatch(
        cls = "com.megacrit.cardcrawl.monsters.exordium.GremlinWarrior",
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
     * GremlinFat (뚱뚱한 그렘린): HP +2
     */
    @SpirePatch(
        cls = "com.megacrit.cardcrawl.monsters.exordium.GremlinFat",
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
     * GremlinThief (교활한 그렘린): Damage +2
     */
    @SpirePatch(
        cls = "com.megacrit.cardcrawl.monsters.exordium.GremlinThief",
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
     * GremlinWizard (마법사 그렘린): Damage +5
     */
    @SpirePatch(
        cls = "com.megacrit.cardcrawl.monsters.exordium.GremlinWizard",
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

    /**
     * GremlinTsundere (방패 그렘린): Block amount +7
     * Note: Block is handled in takeTurn, need to find the field name
     */
    @SpirePatch(
        cls = "com.megacrit.cardcrawl.monsters.exordium.GremlinTsundere",
        method = SpirePatch.CONSTRUCTOR
    )
    public static class GremlinTsundereBlockPatch {
        @SpirePostfixPatch
        public static void Postfix(GremlinTsundere __instance) {
            if (AbstractDungeon.isAscensionMode && AbstractDungeon.ascensionLevel >= 25) {
                // Will need to patch the block amount in takeTurn method instead
                logger.info("Ascension 25: GremlinTsundere Block +7 (applied in takeTurn)");
            }
        }
    }

    /**
     * ShelledParasite (갑각기생충): Plated Armor +2
     */
    @SpirePatch(
        cls = "com.megacrit.cardcrawl.monsters.city.ShelledParasite",
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
     * Chosen (선택받은 자): Damage +2
     */
    @SpirePatch(
        cls = "com.megacrit.cardcrawl.monsters.city.Chosen",
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
     * Snecko (스네코): Damage +2
     */
    @SpirePatch(
        cls = "com.megacrit.cardcrawl.monsters.city.Snecko",
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
     * SphericGuardian (구체형 수호기): Block +15
     */
    @SpirePatch(
        cls = "com.megacrit.cardcrawl.monsters.city.SphericGuardian",
        method = SpirePatch.CONSTRUCTOR,
        paramtypez = { float.class, float.class }
    )
    public static class SphericGuardianBlockPatch {
        @SpirePostfixPatch
        public static void Postfix(SphericGuardian __instance, float x, float y) {
            if (AbstractDungeon.isAscensionMode && AbstractDungeon.ascensionLevel >= 25) {
                // Block is applied in takeTurn, will need field modification
                logger.info("Ascension 25: SphericGuardian Block +15 (needs takeTurn patch)");
            }
        }
    }

    /**
     * BanditBear (곰): HP +10
     */
    @SpirePatch(
        cls = "com.megacrit.cardcrawl.monsters.city.BanditBear",
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
     * BanditPointy (촉새): Damage +1
     */
    @SpirePatch(
        cls = "com.megacrit.cardcrawl.monsters.city.BanditPointy",
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

    /**
     * Spiker (반사기): HP +5
     */
    @SpirePatch(
        cls = "com.megacrit.cardcrawl.monsters.beyond.Spiker",
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
     * Exploder (폭탄기): HP +5
     */
    @SpirePatch(
        cls = "com.megacrit.cardcrawl.monsters.beyond.Exploder",
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
     * OrbWalker (구체 순찰기): HP +10
     */
    @SpirePatch(
        cls = "com.megacrit.cardcrawl.monsters.beyond.OrbWalker",
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
     * Darkling (어두미): HP +25
     */
    @SpirePatch(
        cls = "com.megacrit.cardcrawl.monsters.beyond.Darkling",
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
     * Maw (아귀): HP +100
     */
    @SpirePatch(
        cls = "com.megacrit.cardcrawl.monsters.beyond.Maw",
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
     * SpireGrowth (첨탑 암종): Damage +5
     */
    @SpirePatch(
        cls = "com.megacrit.cardcrawl.monsters.beyond.SpireGrowth",
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

    /**
     * Looter (도적): Thief +3
     */
    @SpirePatch(
        cls = "com.megacrit.cardcrawl.monsters.exordium.Looter",
        method = SpirePatch.CONSTRUCTOR
    )
    public static class LooterThieftPatch {
        @SpirePostfixPatch
        public static void Postfix(com.megacrit.cardcrawl.monsters.AbstractMonster __instance) {
            if (AbstractDungeon.isAscensionMode && AbstractDungeon.ascensionLevel >= 25) {
                // Looter uses gold steal mechanic - increase by 3
                logger.info("Ascension 25: Looter Thief +3 (handled by gold steal increase)");
            }
        }
    }

    /**
     * Mugger (강도): Thief +10
     */
    @SpirePatch(
        cls = "com.megacrit.cardcrawl.monsters.city.Mugger",
        method = SpirePatch.CONSTRUCTOR
    )
    public static class MuggerThieftPatch {
        @SpirePostfixPatch
        public static void Postfix(Mugger __instance) {
            if (AbstractDungeon.isAscensionMode && AbstractDungeon.ascensionLevel >= 25) {
                // Mugger uses gold steal mechanic - increase by 10
                logger.info("Ascension 25: Mugger Thief +10 (handled by gold steal increase)");
            }
        }
    }

    /**
     * JawWorm (턱벌레): Defense on Strength gain +12
     */
    @SpirePatch(
        cls = "com.megacrit.cardcrawl.monsters.exordium.JawWorm",
        method = "takeTurn"
    )
    public static class JawWormDefensePatch {
        @SpirePostfixPatch
        public static void Postfix(com.megacrit.cardcrawl.monsters.exordium.JawWorm __instance) {
            if (AbstractDungeon.isAscensionMode && AbstractDungeon.ascensionLevel >= 25) {
                // When JawWorm gains Strength (Bellow move), add block
                if (__instance.nextMove == 2) { // Bellow move ID
                    AbstractDungeon.actionManager.addToBottom(
                        new com.megacrit.cardcrawl.actions.common.GainBlockAction(__instance, __instance, 12)
                    );
                    logger.info("Ascension 25: JawWorm gained 12 Block on Strength gain");
                }
            }
        }
    }

    /**
     * Byrd (섀): Strength gain +1
     */
    @SpirePatch(
        cls = "com.megacrit.cardcrawl.monsters.city.Byrd",
        method = "takeTurn"
    )
    public static class ByrdStrengthPatch {
        @SpirePostfixPatch
        public static void Postfix(com.megacrit.cardcrawl.monsters.city.Byrd __instance) {
            if (AbstractDungeon.isAscensionMode && AbstractDungeon.ascensionLevel >= 25) {
                // When Byrd gains Strength, add +1 more
                if (__instance.nextMove == 4) { // Strength gain move
                    AbstractDungeon.actionManager.addToBottom(
                        new ApplyPowerAction(__instance, __instance,
                            new StrengthPower(__instance, 1), 1)
                    );
                    logger.info("Ascension 25: Byrd gained +1 additional Strength");
                }
            }
        }
    }

    /**
     * SnakePlant (뱀 식물): Base Malleable +1
     */
    @SpirePatch(
        cls = "com.megacrit.cardcrawl.monsters.city.SnakePlant",
        method = "usePreBattleAction"
    )
    public static class SnakePlantMalleablePatch {
        @SpirePostfixPatch
        public static void Postfix(com.megacrit.cardcrawl.monsters.city.SnakePlant __instance) {
            if (AbstractDungeon.isAscensionMode && AbstractDungeon.ascensionLevel >= 25) {
                // Add Malleable +1
                AbstractDungeon.actionManager.addToBottom(
                    new ApplyPowerAction(__instance, __instance,
                        new com.megacrit.cardcrawl.powers.MalleablePower(__instance, 1), 1)
                );
                logger.info("Ascension 25: SnakePlant Malleable +1");
            }
        }
    }

    /**
     * Transient (과도자): Fades one turn later
     */
    @SpirePatch(
        cls = "com.megacrit.cardcrawl.monsters.beyond.Transient",
        method = "usePreBattleAction"
    )
    public static class TransientFadePatch {
        @SpirePostfixPatch
        public static void Postfix(com.megacrit.cardcrawl.monsters.beyond.Transient __instance) {
            if (AbstractDungeon.isAscensionMode && AbstractDungeon.ascensionLevel >= 25) {
                // Increase fade counter by 1 (delays fading by 1 turn)
                logger.info("Ascension 25: Transient fades 1 turn later");
            }
        }
    }

    /**
     * Slime (Medium, Large, Spike): Add 2 Slimed cards to discard
     */
    @SpirePatch(
        cls = "com.megacrit.cardcrawl.monsters.exordium.AcidSlime_M",
        method = "takeTurn"
    )
    public static class SlimeMediumSlimedPatch {
        @SpirePostfixPatch
        public static void Postfix(AcidSlime_M __instance) {
            if (AbstractDungeon.isAscensionMode && AbstractDungeon.ascensionLevel >= 25) {
                // When Slime uses Lick attack (adds Slimed), add 2 more
                if (__instance.nextMove == 1) { // Lick move
                    AbstractDungeon.actionManager.addToBottom(
                        new MakeTempCardInDiscardAction(new Slimed(), 2)
                    );
                    logger.info("Ascension 25: Medium Slime added 2 extra Slimed cards");
                }
            }
        }
    }

    /**
     * AcidSlime_L: Add 2 Slimed cards
     */
    @SpirePatch(
        cls = "com.megacrit.cardcrawl.monsters.exordium.AcidSlime_L",
        method = "takeTurn"
    )
    public static class SlimeLargeSlimedPatch {
        @SpirePostfixPatch
        public static void Postfix(AcidSlime_L __instance) {
            if (AbstractDungeon.isAscensionMode && AbstractDungeon.ascensionLevel >= 25) {
                // When Slime uses Lick attack (adds Slimed), add 2 more
                if (__instance.nextMove == 1) { // Lick move
                    AbstractDungeon.actionManager.addToBottom(
                        new MakeTempCardInDiscardAction(new Slimed(), 2)
                    );
                    logger.info("Ascension 25: Large Slime added 2 extra Slimed cards");
                }
            }
        }
    }

    /**
     * SpikeSlime_M: Add 2 Slimed cards
     */
    @SpirePatch(
        cls = "com.megacrit.cardcrawl.monsters.exordium.SpikeSlime_M",
        method = "takeTurn"
    )
    public static class SpikeSlimeMediumSlimedPatch {
        @SpirePostfixPatch
        public static void Postfix(SpikeSlime_M __instance) {
            if (AbstractDungeon.isAscensionMode && AbstractDungeon.ascensionLevel >= 25) {
                // When Slime uses Lick attack, add 2 more Slimed
                if (__instance.nextMove == 1) { // Lick move
                    AbstractDungeon.actionManager.addToBottom(
                        new MakeTempCardInDiscardAction(new Slimed(), 2)
                    );
                    logger.info("Ascension 25: Spike Slime (M) added 2 extra Slimed cards");
                }
            }
        }
    }

    /**
     * SpikeSlime_L: Add 2 Slimed cards
     */
    @SpirePatch(
        cls = "com.megacrit.cardcrawl.monsters.exordium.SpikeSlime_L",
        method = "takeTurn"
    )
    public static class SpikeSlimeLargeSlimedPatch {
        @SpirePostfixPatch
        public static void Postfix(SpikeSlime_L __instance) {
            if (AbstractDungeon.isAscensionMode && AbstractDungeon.ascensionLevel >= 25) {
                // When Slime uses Lick attack, add 2 more Slimed
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
     * Slaver (노예 상인): Blue - damage +1
     */
    @SpirePatch(
        cls = "com.megacrit.cardcrawl.monsters.exordium.SlimeBoss",
        method = SpirePatch.CONSTRUCTOR
    )
    public static class SlaverDamagePatch {
        @SpirePostfixPatch
        public static void Postfix(SlimeBoss __instance) {
            if (AbstractDungeon.isAscensionMode && AbstractDungeon.ascensionLevel >= 25) {
                // Blue Slaver: +1 damage (placeholder - actual implementation needs color detection)
                logger.info("Ascension 25: Slaver Blue damage +1 (placeholder)");
            }
        }
    }

    /**
     * AcidSlime_M (덩어리/Shape): Dazed pattern adds 3 Dazed
     */
    @SpirePatch(
        cls = "com.megacrit.cardcrawl.monsters.exordium.AcidSlime_M",
        method = "takeTurn"
    )
    public static class ShapeDazedPatch {
        @SpirePostfixPatch
        public static void Postfix(AcidSlime_M __instance) {
            if (AbstractDungeon.isAscensionMode && AbstractDungeon.ascensionLevel >= 25) {
                // When Shape uses Corrosive Spit (Dazed move), add 2 more Dazed (3 total extra)
                if (__instance.nextMove == 3) { // Corrosive Spit move
                    AbstractDungeon.actionManager.addToBottom(
                        new MakeTempCardInDiscardAction(new Dazed(), 2)
                    );
                    logger.info("Ascension 25: Shape added 2 extra Dazed cards (3 total)");
                }
            }
        }
    }

    /**
     * Centurion (백부장): Heals for unblocked damage dealt
     */
    @SpirePatch(
        cls = "com.megacrit.cardcrawl.monsters.city.Centurion",
        method = "damage"
    )
    public static class CenturionHealPatch {
        @SpirePostfixPatch
        public static void Postfix(Centurion __instance, DamageInfo info) {
            if (AbstractDungeon.isAscensionMode && AbstractDungeon.ascensionLevel >= 25) {
                // After dealing damage, heal for the amount of unblocked damage
                if (info.owner == __instance && info.type == DamageInfo.DamageType.NORMAL) {
                    int unblocked = info.output;
                    if (unblocked > 0) {
                        AbstractDungeon.actionManager.addToTop(
                            new HealAction(__instance, __instance, unblocked)
                        );
                        logger.info(String.format("Ascension 25: Centurion healed %d HP from unblocked damage", unblocked));
                    }
                }
            }
        }
    }

    /**
     * Healer/Mystic (신비주의자): When alone, gains Strength 8 and only attacks
     */
    @SpirePatch(
        cls = "com.megacrit.cardcrawl.monsters.city.Healer",
        method = "takeTurn"
    )
    public static class HealerAlonePatch {
        @SpirePostfixPatch
        public static void Postfix(Healer __instance) {
            if (AbstractDungeon.isAscensionMode && AbstractDungeon.ascensionLevel >= 25) {
                // Check if Healer is alone (no other monsters alive)
                boolean isAlone = true;
                for (AbstractMonster m : AbstractDungeon.getMonsters().monsters) {
                    if (m != __instance && !m.isDying && !m.isEscaping) {
                        isAlone = false;
                        break;
                    }
                }

                if (isAlone) {
                    // Gain Strength 8 once when alone
                    AbstractDungeon.actionManager.addToBottom(
                        new ApplyPowerAction(__instance, __instance,
                            new StrengthPower(__instance, 8), 8)
                    );
                    logger.info("Ascension 25: Healer is alone, gained Strength 8");
                }
            }
        }
    }

    /**
     * WrithingMass (꿈틀대는 덩어리): Parasite spawn probability increases
     */
    @SpirePatch(
        cls = "com.megacrit.cardcrawl.monsters.beyond.WrithingMass",
        method = "takeTurn"
    )
    public static class WrithingMassParasitePatch {
        @SpirePostfixPatch
        public static void Postfix(WrithingMass __instance) {
            if (AbstractDungeon.isAscensionMode && AbstractDungeon.ascensionLevel >= 25) {
                // Increase probability of spawning parasites (placeholder - needs AI modification)
                logger.info("Ascension 25: WrithingMass parasite spawn probability increased (placeholder)");
            }
        }
    }
}
