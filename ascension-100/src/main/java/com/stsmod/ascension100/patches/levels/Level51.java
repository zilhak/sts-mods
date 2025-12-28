package com.stsmod.ascension100.patches.levels;

import com.evacipated.cardcrawl.modthespire.lib.*;
import com.megacrit.cardcrawl.cards.DamageInfo;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import com.megacrit.cardcrawl.monsters.beyond.WrithingMass;
import com.megacrit.cardcrawl.monsters.city.Mugger;
import com.megacrit.cardcrawl.monsters.city.SnakePlant;
import com.megacrit.cardcrawl.monsters.exordium.SlaverBlue;
import com.megacrit.cardcrawl.monsters.exordium.SlaverRed;
import com.megacrit.cardcrawl.monsters.exordium.LouseNormal;
import com.megacrit.cardcrawl.monsters.exordium.LouseDefensive;
import com.megacrit.cardcrawl.monsters.exordium.GremlinTsundere;
import com.megacrit.cardcrawl.monsters.exordium.GremlinThief;
import com.megacrit.cardcrawl.monsters.exordium.GremlinWizard;
import com.megacrit.cardcrawl.monsters.exordium.GremlinWarrior;
import com.megacrit.cardcrawl.monsters.exordium.GremlinFat;
import com.megacrit.cardcrawl.monsters.exordium.JawWorm;
import com.megacrit.cardcrawl.monsters.city.ShelledParasite;
import com.megacrit.cardcrawl.monsters.city.Chosen;
import com.megacrit.cardcrawl.monsters.city.Centurion;
import com.megacrit.cardcrawl.monsters.city.Snecko;
import com.megacrit.cardcrawl.monsters.city.SphericGuardian;
import com.megacrit.cardcrawl.monsters.city.BanditPointy;
import com.megacrit.cardcrawl.monsters.city.BanditLeader;
import com.megacrit.cardcrawl.monsters.beyond.Repulsor;
import com.megacrit.cardcrawl.monsters.beyond.OrbWalker;
import com.megacrit.cardcrawl.monsters.beyond.Transient;
import com.megacrit.cardcrawl.monsters.beyond.SpireGrowth;
import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.actions.common.ApplyPowerAction;
import com.megacrit.cardcrawl.actions.common.GainBlockAction;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.powers.AbstractPower;
import com.megacrit.cardcrawl.powers.ExplosivePower;
import com.megacrit.cardcrawl.powers.StrengthPower;
import com.megacrit.cardcrawl.powers.PlatedArmorPower;
import com.megacrit.cardcrawl.powers.SporeCloudPower;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.reflect.Field;

/**
 * Ascension Level 51: Enemy action patterns enhanced
 * 모든 적의 행동 패턴이 강화됩니다.
 *
 * 상세효과:
 * - 공벌레(Louse): 힘 증가 패턴에서 추가적으로 힘을 1 증가
 * - WrithingMass의 기생충 추가 패턴(MEGA_DEBUFF)을 2회 사용 가능
 * - 폭탄기(Exploder): 폭발 패턴 데미지를 50으로 증가 (기본 30)
 * - 강도(Mugger): 도둑질 수치가 5 증가
 * - 뱀 식물(Snake Plant): 기본 탄성 수치가 1 증가
 */
public class Level51 {
    private static final Logger logger = LogManager.getLogger(Level51.class.getName());

    /**
     * Allow WrithingMass to use IMPLANT pattern twice instead of once
     */
    @SpirePatch(clz = WrithingMass.class, method = "takeTurn")
    public static class AllowTwoImplants {
        private static boolean hasResetOnce = false;

        @SpirePostfixPatch
        public static void Postfix(WrithingMass __instance) {
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 51) {
                return;
            }

            // MEGA_DEBUFF (case 4) 사용 직후
            if (__instance.nextMove == 4 && !hasResetOnce) {
                try {
                    // usedMegaDebuff 필드 접근
                    Field usedMegaDebuff = WrithingMass.class
                            .getDeclaredField("usedMegaDebuff");
                    usedMegaDebuff.setAccessible(true);

                    // 플래그 리셋 (재사용 허용)
                    usedMegaDebuff.setBoolean(__instance, false);

                    hasResetOnce = true;

                    logger.info(String.format(
                            "[Asc51] WrithingMass IMPLANT flag reset - can use one more time (1/2 used)"));
                } catch (Exception e) {
                    logger.error("[Asc51] Failed to reset usedMegaDebuff flag: " + e.getMessage());
                }
            } else if (__instance.nextMove == 4 && hasResetOnce) {
                logger.info("[Asc51] WrithingMass IMPLANT used 2/2 times - no more uses");
            }
        }
    }

    /**
     * Reset the flag counter at the start of each battle
     */
    @SpirePatch(clz = WrithingMass.class, method = "usePreBattleAction")
    public static class ResetBattleCounter {
        @SpirePostfixPatch
        public static void Postfix(WrithingMass __instance) {
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 51) {
                return;
            }

            // 전투 시작 시 카운터 초기화
            AllowTwoImplants.hasResetOnce = false;

            logger.info("[Asc51] WrithingMass battle started - IMPLANT counter reset (0/2 used)");
        }
    }

    /**
     * Exploder: Explosion damage increased
     * 폭탄기: 폭발 패턴 데미지 증가
     * - Level 51-85: 30 → 50
     * - Level 86+: 30 → 60
     */
    @SpirePatch(clz = com.megacrit.cardcrawl.actions.common.DamageAction.class, method = SpirePatch.CONSTRUCTOR, paramtypez = {
            com.megacrit.cardcrawl.core.AbstractCreature.class,
            DamageInfo.class,
            com.megacrit.cardcrawl.actions.AbstractGameAction.AttackEffect.class,
            boolean.class
    })
    public static class ExploderExplosionDamage {
        @SpirePostfixPatch
        public static void Postfix(
                com.megacrit.cardcrawl.actions.common.DamageAction __instance,
                com.megacrit.cardcrawl.core.AbstractCreature target,
                DamageInfo info,
                com.megacrit.cardcrawl.actions.AbstractGameAction.AttackEffect effect,
                boolean superFast) {
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 51) {
                return;
            }

            // Only modify ExplosivePower explosion damage (THORNS type, 30 base damage,
            // target is player)
            if (info.type == DamageInfo.DamageType.THORNS &&
                    info.base == 30 &&
                    target != null && target.isPlayer &&
                    info.owner != null && !info.owner.isPlayer) {

                if (AbstractDungeon.ascensionLevel >= 86) {
                    // Level 86+: 60 damage
                    info.base = 60;
                    info.output = 60;
                    logger.info("Ascension 86: ExplosivePower explosion damage increased from 30 to 60");
                } else {
                    // Level 53-85: 50 damage
                    info.base = 50;
                    info.output = 50;
                    logger.info("Ascension 51: ExplosivePower explosion damage increased from 30 to 50");
                }
            }
        }
    }

    /**
     * ExplosivePower: Update tooltip to show correct damage based on ascension
     * level
     * 폭발성 파워: 승천 레벨에 맞는 데미지를 툴팁에 표시
     * - Level 51-85: 50 damage
     * - Level 86+: 60 damage
     */
    @SpirePatch(clz = ExplosivePower.class, method = "updateDescription")
    public static class ExplosivePowerTooltipFix {
        @SpirePostfixPatch
        public static void Postfix(ExplosivePower __instance) {
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 51) {
                return;
            }

            try {
                // Get the current description
                Field descriptionField = AbstractPower.class.getDeclaredField("description");
                descriptionField.setAccessible(true);
                String currentDescription = (String) descriptionField.get(__instance);

                // Replace damage value in tooltip
                String newDescription;
                if (AbstractDungeon.ascensionLevel >= 86) {
                    // Level 86+: Replace "30" with "60"
                    newDescription = currentDescription.replace(" 30 ", " 60 ");
                    logger.info("Ascension 86: ExplosivePower tooltip updated to show 60 damage");
                } else {
                    // Level 53-85: Replace "30" with "50"
                    newDescription = currentDescription.replace(" 30 ", " 50 ");
                    logger.info("Ascension 51: ExplosivePower tooltip updated to show 50 damage");
                }

                descriptionField.set(__instance, newDescription);
            } catch (Exception e) {
                logger.error("Failed to update ExplosivePower tooltip", e);
            }
        }
    }

    /**
     * Slaver Blue (푸른색 노예 상인): Damage +1
     */
    @SpirePatch(clz = SlaverBlue.class, method = SpirePatch.CONSTRUCTOR, paramtypez = { float.class, float.class })
    public static class SlaverBlueDamagePatch {
        @SpirePostfixPatch
        public static void Postfix(SlaverBlue __instance, float x, float y) {
            if (AbstractDungeon.isAscensionMode && AbstractDungeon.ascensionLevel >= 51) {
                try {
                    // Increase stabDmg field (+1) - used in getMove() line 124
                    Field stabDmgField = SlaverBlue.class.getDeclaredField("stabDmg");
                    stabDmgField.setAccessible(true);
                    int currentStabDmg = stabDmgField.getInt(__instance);
                    stabDmgField.setInt(__instance, currentStabDmg + 1);

                    // Also increase damage.get(0).base for consistency
                    if (!__instance.damage.isEmpty()) {
                        DamageInfo stabDamage = __instance.damage.get(0);
                        stabDamage.base += 1;
                    }

                    logger.info(String.format(
                            "Ascension 51: Slaver Blue Stab damage increased from %d to %d",
                            currentStabDmg, currentStabDmg + 1));
                } catch (Exception e) {
                    logger.error("Failed to modify Slaver Blue damage", e);
                }
            }
        }
    }

    /**
     * Slaver Red (붉은색 노예 상인): Damage +1
     */
    @SpirePatch(clz = SlaverRed.class, method = SpirePatch.CONSTRUCTOR, paramtypez = { float.class, float.class })
    public static class SlaverRedDamagePatch {
        @SpirePostfixPatch
        public static void Postfix(SlaverRed __instance, float x, float y) {
            if (AbstractDungeon.isAscensionMode && AbstractDungeon.ascensionLevel >= 51) {
                try {
                    // Increase stabDmg field (+1) - used in getMove() line 155
                    Field stabDmgField = SlaverRed.class.getDeclaredField("stabDmg");
                    stabDmgField.setAccessible(true);
                    int currentStabDmg = stabDmgField.getInt(__instance);
                    stabDmgField.setInt(__instance, currentStabDmg + 1);

                    // Also increase damage.get(0).base for consistency
                    if (!__instance.damage.isEmpty()) {
                        DamageInfo stabDamage = __instance.damage.get(0);
                        stabDamage.base += 1;
                    }

                    logger.info(String.format(
                            "Ascension 51: Slaver Red Stab damage increased from %d to %d",
                            currentStabDmg, currentStabDmg + 1));
                } catch (Exception e) {
                    logger.error("Failed to modify Slaver Red damage", e);
                }
            }
        }
    }

    /**
     * Mugger (강도): Thievery +5
     *
     * IMPORTANT: Must modify goldAmt field BEFORE usePreBattleAction creates
     * ThieveryPower
     */
    @SpirePatch(clz = Mugger.class, method = "usePreBattleAction")
    public static class MuggerThieveryIncrease {
        @SpirePrefixPatch
        public static void Prefix(Mugger __instance) {
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 51) {
                return;
            }

            try {
                // Increase goldAmt BEFORE ThieveryPower is created
                java.lang.reflect.Field goldAmtField = Mugger.class.getDeclaredField("goldAmt");
                goldAmtField.setAccessible(true);
                int currentGoldAmt = goldAmtField.getInt(__instance);
                goldAmtField.setInt(__instance, currentGoldAmt + 5);

                logger.info(String.format(
                        "Ascension 51: Mugger goldAmt increased from %d to %d (Thievery will be %d)",
                        currentGoldAmt, currentGoldAmt + 5, currentGoldAmt + 5));
            } catch (Exception e) {
                logger.error("Failed to modify Mugger goldAmt", e);
            }
        }

        @SpirePostfixPatch
        public static void Postfix(Mugger __instance) {
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 51) {
                return;
            }

            try {
                // Reset goldAmt back to original after ThieveryPower is created
                java.lang.reflect.Field goldAmtField = Mugger.class.getDeclaredField("goldAmt");
                goldAmtField.setAccessible(true);
                int currentGoldAmt = goldAmtField.getInt(__instance);
                goldAmtField.setInt(__instance, currentGoldAmt - 5);

                logger.info("Ascension 51: Mugger goldAmt reset to original (Thievery amount remains increased)");
            } catch (Exception e) {
                logger.error("Failed to reset Mugger goldAmt", e);
            }
        }
    }

    /**
     * Snake Plant (뱀 식물): Malleable +1
     * Intercepts MalleablePower creation to increase both amount and basePower
     */
    @SpirePatch(clz = com.megacrit.cardcrawl.powers.MalleablePower.class, method = SpirePatch.CONSTRUCTOR, paramtypez = {
            com.megacrit.cardcrawl.core.AbstractCreature.class })
    public static class SnakePlantMalleableIncrease {
        @SpirePostfixPatch
        public static void Postfix(com.megacrit.cardcrawl.powers.MalleablePower __instance,
                com.megacrit.cardcrawl.core.AbstractCreature owner) {
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 51) {
                return;
            }

            // Check if owner is SnakePlant
            if (owner instanceof SnakePlant) {
                // Increase both amount and basePower by 1
                __instance.amount += 1;
                try {
                    java.lang.reflect.Field basePowerField = com.megacrit.cardcrawl.powers.MalleablePower.class
                            .getDeclaredField("basePower");
                    basePowerField.setAccessible(true);
                    int currentBasePower = basePowerField.getInt(__instance);
                    basePowerField.setInt(__instance, currentBasePower + 1);
                } catch (Exception e) {
                    logger.error("Failed to modify MalleablePower basePower for Level 51", e);
                }
                __instance.updateDescription();
                logger.info(String.format(
                        "Ascension 51: Snake Plant Malleable increased to %d (base: %d)",
                        __instance.amount, __instance.amount));
            }
        }
    }

    /**
     * Looter (도적): Smoke Bomb block +4
     */
    @SpirePatch(clz = com.megacrit.cardcrawl.monsters.exordium.Looter.class, method = "takeTurn")
    public static class LooterSmokeBombBlockIncrease {
        @SpirePostfixPatch
        public static void Postfix(com.megacrit.cardcrawl.monsters.exordium.Looter __instance) {
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 51) {
                return;
            }

            try {
                // Check if Smoke Bomb move (ESCAPE move)
                java.lang.reflect.Field nextMoveField = AbstractMonster.class.getDeclaredField("nextMove");
                nextMoveField.setAccessible(true);
                byte move = nextMoveField.getByte(__instance);

                if (move == 2) { // ESCAPE (Smoke Bomb) move
                    // Increase escapeDef by 4
                    java.lang.reflect.Field escapeDefField = com.megacrit.cardcrawl.monsters.exordium.Looter.class
                            .getDeclaredField("escapeDef");
                    escapeDefField.setAccessible(true);
                    int currentEscapeDef = escapeDefField.getInt(__instance);
                    escapeDefField.setInt(__instance, currentEscapeDef + 4);

                    logger.info(String.format(
                            "Ascension 51: Looter Smoke Bomb block increased by 4 to %d",
                            currentEscapeDef + 4));
                }
            } catch (Exception e) {
                logger.error("Failed to modify Looter Smoke Bomb block", e);
            }
        }
    }

    /**
     * Mugger (강도): Smoke Bomb block +3
     */
    @SpirePatch(clz = Mugger.class, method = "takeTurn")
    public static class MuggerSmokeBombBlockIncrease {
        @SpirePostfixPatch
        public static void Postfix(Mugger __instance) {
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 51) {
                return;
            }

            try {
                // Check if Smoke Bomb move (LUNGE move)
                java.lang.reflect.Field nextMoveField = AbstractMonster.class.getDeclaredField("nextMove");
                nextMoveField.setAccessible(true);
                byte move = nextMoveField.getByte(__instance);

                if (move == 3) { // LUNGE (Smoke Bomb) move
                    // Increase escapeDef by 3
                    java.lang.reflect.Field escapeDefField = Mugger.class.getDeclaredField("escapeDef");
                    escapeDefField.setAccessible(true);
                    int currentEscapeDef = escapeDefField.getInt(__instance);
                    escapeDefField.setInt(__instance, currentEscapeDef + 3);

                    logger.info(String.format(
                            "Ascension 51: Mugger Smoke Bomb block increased by 3 to %d",
                            currentEscapeDef + 3));
                }
            } catch (Exception e) {
                logger.error("Failed to modify Mugger Smoke Bomb block", e);
            }
        }
    }

    /**
     * Healer (신비주의자/Mystic): Heal pattern +5 heal amount
     */
    @SpirePatch(clz = com.megacrit.cardcrawl.monsters.city.Healer.class, method = "takeTurn")
    public static class HealerHealAmountIncrease {
        private static final ThreadLocal<Byte> lastMove = new ThreadLocal<>();

        @SpirePrefixPatch
        public static void Prefix(com.megacrit.cardcrawl.monsters.city.Healer __instance) {
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 51) {
                return;
            }

            try {
                java.lang.reflect.Field nextMoveField = AbstractMonster.class.getDeclaredField("nextMove");
                nextMoveField.setAccessible(true);
                byte move = nextMoveField.getByte(__instance);
                lastMove.set(move);

                if (move == 2) { // HEAL move
                    // Increase healAmt by 5 BEFORE takeTurn executes
                    java.lang.reflect.Field healAmtField = com.megacrit.cardcrawl.monsters.city.Healer.class
                            .getDeclaredField("healAmt");
                    healAmtField.setAccessible(true);
                    int currentHealAmt = healAmtField.getInt(__instance);
                    healAmtField.setInt(__instance, currentHealAmt + 5);

                    logger.info(String.format(
                            "Ascension 51: Healer heal amount increased by 5 to %d",
                            currentHealAmt + 5));
                }
            } catch (Exception e) {
                logger.error("Failed to increase Healer heal amount", e);
            }
        }

        @SpirePostfixPatch
        public static void Postfix(com.megacrit.cardcrawl.monsters.city.Healer __instance) {
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 51) {
                return;
            }

            Byte move = lastMove.get();
            if (move != null && move == 2) { // HEAL move
                try {
                    // Reset healAmt back to original after HEAL
                    java.lang.reflect.Field healAmtField = com.megacrit.cardcrawl.monsters.city.Healer.class
                            .getDeclaredField("healAmt");
                    healAmtField.setAccessible(true);
                    int currentHealAmt = healAmtField.getInt(__instance);
                    healAmtField.setInt(__instance, currentHealAmt - 5);

                    logger.info("Ascension 51: Healer heal amount reset to original");
                } catch (Exception e) {
                    logger.error("Failed to reset Healer heal amount", e);
                }
            }
            lastMove.remove();
        }
    }

    /**
     * BanditBear (곰): Bear Hug dexterity reduction +1
     */
    @SpirePatch(clz = com.megacrit.cardcrawl.monsters.city.BanditBear.class, method = "takeTurn")
    public static class BanditBearBearHugDebuffIncrease {
        @SpirePostfixPatch
        public static void Postfix(com.megacrit.cardcrawl.monsters.city.BanditBear __instance) {
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 51) {
                return;
            }

            try {
                // Check if Bear Hug move
                java.lang.reflect.Field nextMoveField = AbstractMonster.class.getDeclaredField("nextMove");
                nextMoveField.setAccessible(true);
                byte move = nextMoveField.getByte(__instance);

                if (move == 2) { // BEAR_HUG move
                    // Apply additional -1 Dexterity
                    AbstractDungeon.actionManager.addToBottom(
                            new com.megacrit.cardcrawl.actions.common.ApplyPowerAction(
                                    AbstractDungeon.player,
                                    __instance,
                                    new com.megacrit.cardcrawl.powers.DexterityPower(AbstractDungeon.player, -1),
                                    -1));

                    logger.info("Ascension 51: BanditBear Bear Hug applied -1 additional Dexterity");
                }
            } catch (Exception e) {
                logger.error("Failed to modify BanditBear Bear Hug", e);
            }
        }
    }

    /**
     * Spiker (반사기): Power pattern Thorns +1
     */
    @SpirePatch(clz = com.megacrit.cardcrawl.monsters.beyond.Spiker.class, method = "takeTurn")
    public static class SpikerThornsPowerIncrease {
        private static final ThreadLocal<Byte> lastMove = new ThreadLocal<>();

        @com.evacipated.cardcrawl.modthespire.lib.SpirePrefixPatch
        public static void Prefix(com.megacrit.cardcrawl.monsters.beyond.Spiker __instance) {
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 51) {
                return;
            }

            try {
                // Store current move before it changes
                java.lang.reflect.Field nextMoveField = AbstractMonster.class.getDeclaredField("nextMove");
                nextMoveField.setAccessible(true);
                byte move = nextMoveField.getByte(__instance);
                lastMove.set(move);
            } catch (Exception e) {
                logger.error("Failed to get Spiker move", e);
            }
        }

        @com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch
        public static void Postfix(com.megacrit.cardcrawl.monsters.beyond.Spiker __instance) {
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 51) {
                return;
            }

            Byte move = lastMove.get();
            if (move != null && move == 2) { // BUFF_THORNS move
                // Find and modify the last ApplyPowerAction with ThornsPower
                try {
                    for (int i = AbstractDungeon.actionManager.actions.size() - 1; i >= 0; i--) {
                        com.megacrit.cardcrawl.actions.AbstractGameAction action = AbstractDungeon.actionManager.actions
                                .get(i);

                        if (action instanceof com.megacrit.cardcrawl.actions.common.ApplyPowerAction) {
                            java.lang.reflect.Field powerToApplyField = com.megacrit.cardcrawl.actions.common.ApplyPowerAction.class
                                    .getDeclaredField("powerToApply");
                            powerToApplyField.setAccessible(true);
                            com.megacrit.cardcrawl.powers.AbstractPower power = (com.megacrit.cardcrawl.powers.AbstractPower) powerToApplyField
                                    .get(action);

                            if (power instanceof com.megacrit.cardcrawl.powers.ThornsPower
                                    && power.owner == __instance) {
                                power.amount += 1;

                                java.lang.reflect.Field amountField = com.megacrit.cardcrawl.actions.common.ApplyPowerAction.class
                                        .getDeclaredField("amount");
                                amountField.setAccessible(true);
                                int currentAmount = amountField.getInt(action);
                                amountField.setInt(action, currentAmount + 1);

                                logger.info(String.format(
                                        "Ascension 51: Spiker Thorns increased by +1 (total: %d)",
                                        power.amount));
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
     * Darkling (어두미): Revive grants Strength +2
     */
    @SpirePatch(clz = com.megacrit.cardcrawl.monsters.beyond.Darkling.class, method = "takeTurn")
    public static class DarklingReviveStrengthGain {
        @SpirePostfixPatch
        public static void Postfix(com.megacrit.cardcrawl.monsters.beyond.Darkling __instance) {
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 51) {
                return;
            }

            try {
                // Check if Revive move
                java.lang.reflect.Field nextMoveField = AbstractMonster.class.getDeclaredField("nextMove");
                nextMoveField.setAccessible(true);
                byte move = nextMoveField.getByte(__instance);

                if (move == 5) { // REINCARNATE move
                    // Apply +2 Strength on revive
                    AbstractDungeon.actionManager.addToBottom(
                            new com.megacrit.cardcrawl.actions.common.ApplyPowerAction(
                                    __instance,
                                    __instance,
                                    new com.megacrit.cardcrawl.powers.StrengthPower(__instance, 2),
                                    2));

                    logger.info("Ascension 51: Darkling gained +2 Strength on revive");
                }
            } catch (Exception e) {
                logger.error("Failed to modify Darkling revive", e);
            }
        }
    }

    /**
     * Maw (아귀): Roar pattern Weak and Frail +1
     */
    @SpirePatch(clz = com.megacrit.cardcrawl.monsters.beyond.Maw.class, method = "takeTurn")
    public static class MawRoarDebuffIncrease {
        @SpirePostfixPatch
        public static void Postfix(com.megacrit.cardcrawl.monsters.beyond.Maw __instance) {
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 51) {
                return;
            }

            try {
                // Check if Roar move
                java.lang.reflect.Field nextMoveField = AbstractMonster.class.getDeclaredField("nextMove");
                nextMoveField.setAccessible(true);
                byte move = nextMoveField.getByte(__instance);

                if (move == 3) { // ROAR move
                    // Apply additional Weak +1 and Frail +1
                    AbstractDungeon.actionManager.addToBottom(
                            new com.megacrit.cardcrawl.actions.common.ApplyPowerAction(
                                    AbstractDungeon.player,
                                    __instance,
                                    new com.megacrit.cardcrawl.powers.WeakPower(AbstractDungeon.player, 1, true),
                                    1));
                    AbstractDungeon.actionManager.addToBottom(
                            new com.megacrit.cardcrawl.actions.common.ApplyPowerAction(
                                    AbstractDungeon.player,
                                    __instance,
                                    new com.megacrit.cardcrawl.powers.FrailPower(AbstractDungeon.player, 1, true),
                                    1));

                    logger.info("Ascension 51: Maw Roar applied +1 additional Weak and Frail");
                }
            } catch (Exception e) {
                logger.error("Failed to modify Maw Roar", e);
            }
        }
    }

    /**
     * Louse Normal: Grow pattern +1 additional Strength
     * 공벌레(일반): 힘 증가 패턴에서 추가적으로 힘을 1 증가
     */
    @SpirePatch(clz = LouseNormal.class, method = "takeTurn")
    public static class LouseNormalGrowStrengthBonus {
        private static final ThreadLocal<Byte> lastMove = new ThreadLocal<>();

        @SpirePrefixPatch
        public static void Prefix(LouseNormal __instance) {
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 51) {
                return;
            }

            try {
                Field nextMoveField = AbstractMonster.class.getDeclaredField("nextMove");
                nextMoveField.setAccessible(true);
                byte move = nextMoveField.getByte(__instance);
                lastMove.set(move);
            } catch (Exception e) {
                logger.error("Failed to get Louse Normal move", e);
            }
        }

        @SpirePostfixPatch
        public static void Postfix(LouseNormal __instance) {
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 51) {
                return;
            }

            Byte move = lastMove.get();
            if (move != null && move == 3) { // GROW move (byte 3)
                // Find and modify the StrengthPower in action queue
                try {
                    for (int i = AbstractDungeon.actionManager.actions.size() - 1; i >= 0; i--) {
                        AbstractGameAction action = AbstractDungeon.actionManager.actions.get(i);

                        if (action instanceof ApplyPowerAction) {
                            Field powerToApplyField = ApplyPowerAction.class.getDeclaredField("powerToApply");
                            powerToApplyField.setAccessible(true);
                            AbstractPower power = (AbstractPower) powerToApplyField.get(action);

                            if (power instanceof StrengthPower && power.owner == __instance) {
                                power.amount += 1;
                                logger.info(String.format(
                                        "Ascension 51: Louse Normal Grow pattern strength increased by +1 to %d",
                                        power.amount));
                                break;
                            }
                        }
                    }
                } catch (Exception e) {
                    logger.error("Failed to modify Louse Normal Grow strength", e);
                }
            }

            lastMove.remove();
        }
    }

    /**
     * Louse Defensive: Grow pattern +1 additional Strength
     * 공벌레(방어): 힘 증가 패턴에서 추가적으로 힘을 1 증가
     */
    @SpirePatch(clz = LouseDefensive.class, method = "takeTurn")
    public static class LouseDefensiveGrowStrengthBonus {
        private static final ThreadLocal<Byte> lastMove = new ThreadLocal<>();

        @SpirePrefixPatch
        public static void Prefix(LouseDefensive __instance) {
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 51) {
                return;
            }

            try {
                Field nextMoveField = AbstractMonster.class.getDeclaredField("nextMove");
                nextMoveField.setAccessible(true);
                byte move = nextMoveField.getByte(__instance);
                lastMove.set(move);
            } catch (Exception e) {
                logger.error("Failed to get Louse Defensive move", e);
            }
        }

        @SpirePostfixPatch
        public static void Postfix(LouseDefensive __instance) {
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 51) {
                return;
            }

            Byte move = lastMove.get();
            if (move != null && move == 3) { // GROW move (byte 3)
                // Find and modify the StrengthPower in action queue
                try {
                    for (int i = AbstractDungeon.actionManager.actions.size() - 1; i >= 0; i--) {
                        AbstractGameAction action = AbstractDungeon.actionManager.actions.get(i);

                        if (action instanceof ApplyPowerAction) {
                            Field powerToApplyField = ApplyPowerAction.class.getDeclaredField("powerToApply");
                            powerToApplyField.setAccessible(true);
                            AbstractPower power = (AbstractPower) powerToApplyField.get(action);

                            if (power instanceof StrengthPower && power.owner == __instance) {
                                power.amount += 1;
                                logger.info(String.format(
                                        "Ascension 51: Louse Defensive Grow pattern strength increased by +1 to %d",
                                        power.amount));
                                break;
                            }
                        }
                    }
                } catch (Exception e) {
                    logger.error("Failed to modify Louse Defensive Grow strength", e);
                }
            }

            lastMove.remove();
        }
    }

    /**
     * GremlinTsundere (방패 그렘린): Block amount +3
     */
    @SpirePatch(clz = GremlinTsundere.class, method = SpirePatch.CONSTRUCTOR, paramtypez = { float.class, float.class })
    public static class GremlinTsundereBlockPatch {
        @SpirePostfixPatch
        public static void Postfix(GremlinTsundere __instance, float x, float y) {
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 51) {
                return;
            }

            try {
                // Increase blockAmt field by 3
                Field blockAmtField = GremlinTsundere.class.getDeclaredField("blockAmt");
                blockAmtField.setAccessible(true);
                int currentBlockAmt = blockAmtField.getInt(__instance);
                blockAmtField.setInt(__instance, currentBlockAmt + 3);

                logger.info(String.format(
                        "Ascension 51: GremlinTsundere blockAmt increased from %d to %d (+3)",
                        currentBlockAmt, currentBlockAmt + 3));
            } catch (Exception e) {
                logger.error("Failed to modify GremlinTsundere blockAmt", e);
            }
        }
    }

    /**
     * GremlinThief (교활한 그렘린): Damage +2
     */
    @SpirePatch(clz = GremlinThief.class, method = SpirePatch.CONSTRUCTOR, paramtypez = { float.class, float.class })
    public static class GremlinThiefDamagePatch51 {
        @SpirePostfixPatch
        public static void Postfix(GremlinThief __instance, float x, float y) {
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 51) {
                return;
            }

            try {
                // Increase thiefDamage field by 2
                Field thiefDamageField = GremlinThief.class.getDeclaredField("thiefDamage");
                thiefDamageField.setAccessible(true);
                int currentDamage = thiefDamageField.getInt(__instance);
                thiefDamageField.setInt(__instance, currentDamage + 2);

                // Update damage info
                if (!__instance.damage.isEmpty()) {
                    DamageInfo damageInfo = __instance.damage.get(0);
                    damageInfo.base += 2;
                }

                logger.info(String.format(
                        "Ascension 51: GremlinThief damage increased from %d to %d (+2)",
                        currentDamage, currentDamage + 2));
            } catch (Exception e) {
                logger.error("Failed to modify GremlinThief damage", e);
            }
        }
    }

    /**
     * GremlinWizard (마법사 그렘린): Damage +5
     */
    @SpirePatch(clz = GremlinWizard.class, method = SpirePatch.CONSTRUCTOR, paramtypez = { float.class, float.class })
    public static class GremlinWizardDamagePatch {
        @SpirePostfixPatch
        public static void Postfix(GremlinWizard __instance, float x, float y) {
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 51) {
                return;
            }

            // Increase damage by 5
            if (!__instance.damage.isEmpty()) {
                int originalDamage = __instance.damage.get(0).base;
                __instance.damage.get(0).base += 5;

                logger.info(String.format(
                        "Ascension 51: GremlinWizard damage increased from %d to %d (+5)",
                        originalDamage, originalDamage + 5));
            }
        }
    }

    /**
     * GremlinWarrior (화난 그렘린): HP +10
     */
    @SpirePatch(clz = GremlinWarrior.class, method = SpirePatch.CONSTRUCTOR, paramtypez = { float.class, float.class })
    public static class GremlinWarriorHPPatch {
        @SpirePostfixPatch
        public static void Postfix(GremlinWarrior __instance, float x, float y) {
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 51) {
                return;
            }

            int originalHP = __instance.maxHealth;
            __instance.maxHealth += 10;
            __instance.currentHealth += 10;

            logger.info(String.format(
                    "Ascension 51: GremlinWarrior HP increased from %d to %d (+10)",
                    originalHP, __instance.maxHealth));
        }
    }

    /**
     * GremlinFat (뚱뚱한 그렘린): HP +2
     */
    @SpirePatch(clz = GremlinFat.class, method = SpirePatch.CONSTRUCTOR, paramtypez = { float.class, float.class })
    public static class GremlinFatHPPatch {
        @SpirePostfixPatch
        public static void Postfix(GremlinFat __instance, float x, float y) {
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 51) {
                return;
            }

            int originalHP = __instance.maxHealth;
            __instance.maxHealth += 2;
            __instance.currentHealth += 2;

            logger.info(String.format(
                    "Ascension 51: GremlinFat HP increased from %d to %d (+2)",
                    originalHP, __instance.maxHealth));
        }
    }

    /**
     * JawWorm (턱벌레): First turn block +3
     * 첫턴에서 방어도를 3 얻은상태로 시작
     */
    @SpirePatch(clz = JawWorm.class, method = "usePreBattleAction")
    public static class JawWormFirstTurnBlock {
        @SpirePostfixPatch
        public static void Postfix(JawWorm __instance) {
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 51) {
                return;
            }

            AbstractDungeon.actionManager.addToBottom(
                    new GainBlockAction(__instance, __instance, 3));

            logger.info("Ascension 51: JawWorm gained 3 block at battle start");
        }
    }

    /**
     * FungiBeast (동물하초): SporeCloud -1 and adds Frail on death
     * 포자 구름 수치 -1, 죽을 때 취약과 손상을 같이 부여
     */
    @SpirePatch(clz = SporeCloudPower.class, method = SpirePatch.CONSTRUCTOR)
    public static class FungiBeastSporeCloudReduction {
        @SpirePostfixPatch
        public static void Postfix(SporeCloudPower __instance, AbstractCreature owner, int amount) {
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 51) {
                return;
            }

            // Check if owner is FungiBeast
            if (owner instanceof com.megacrit.cardcrawl.monsters.exordium.FungiBeast) {
                __instance.amount = Math.max(1, __instance.amount - 1);
                __instance.updateDescription();
                logger.info(String.format(
                        "Ascension 51: FungiBeast SporeCloud reduced from %d to %d",
                        amount, __instance.amount));
            }
        }
    }

    /**
     * FungiBeast: Add Frail debuff on death (in addition to Vulnerable)
     */
    @SpirePatch(clz = SporeCloudPower.class, method = "onDeath")
    public static class FungiBeastSporeCloudFrail {
        @SpirePostfixPatch
        public static void Postfix(SporeCloudPower __instance) {
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 51) {
                return;
            }

            // Check if owner is FungiBeast
            if (__instance.owner instanceof com.megacrit.cardcrawl.monsters.exordium.FungiBeast) {
                // Add Frail debuff (same amount as Vulnerable)
                AbstractDungeon.actionManager.addToTop(
                        new ApplyPowerAction(
                                AbstractDungeon.player,
                                null,
                                new com.megacrit.cardcrawl.powers.FrailPower(
                                        AbstractDungeon.player,
                                        __instance.amount,
                                        true),
                                __instance.amount));
                logger.info(String.format(
                        "Ascension 51: FungiBeast death also applies %d Frail (in addition to Vulnerable)",
                        __instance.amount));
            }
        }
    }

    /**
     * SporeCloudPower: Update description for Ascension 51+ (adds Frail info)
     */
    @SpirePatch(clz = SporeCloudPower.class, method = "updateDescription")
    public static class SporeCloudPowerDescriptionPatch {
        @SpirePostfixPatch
        public static void Postfix(SporeCloudPower __instance) {
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 51) {
                return;
            }

            // Check if owner is FungiBeast
            if (__instance.owner instanceof com.megacrit.cardcrawl.monsters.exordium.FungiBeast) {
                // Update description to mention both Vulnerable and Frail
                if (com.megacrit.cardcrawl.core.Settings.language == com.megacrit.cardcrawl.core.Settings.GameLanguage.KOR) {
                    __instance.description = "사망 시, 당신에게 #y취약 과 #y손상 을 #b" + __instance.amount + " 부여합니다.";
                } else {
                    __instance.description = "On death, applies #b" + __instance.amount + " #yVulnerable and #yFrail.";
                }
            }
        }
    }

    /**
     * ShelledParasite (갑각기생충): Plated Armor +2
     */
    @SpirePatch(clz = ShelledParasite.class, method = "usePreBattleAction")
    public static class ShelledParasitePlatedArmorIncrease {
        @SpirePostfixPatch
        public static void Postfix(ShelledParasite __instance) {
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 51) {
                return;
            }

            AbstractDungeon.actionManager.addToBottom(
                    new ApplyPowerAction(__instance, __instance,
                            new PlatedArmorPower(__instance, 2), 2));

            logger.info("Ascension 51: ShelledParasite gained +2 Plated Armor");
        }
    }

    /**
     * Chosen (선택받은 자): Damage +2
     */
    @SpirePatch(clz = Chosen.class, method = SpirePatch.CONSTRUCTOR, paramtypez = { float.class, float.class })
    public static class ChosenDamagePatch {
        @SpirePostfixPatch
        public static void Postfix(Chosen __instance) {
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 51) {
                return;
            }

            // Increase all damage by 2
            for (DamageInfo damage : __instance.damage) {
                damage.base += 2;
            }

            logger.info("Ascension 51: Chosen damage increased by +2");
        }
    }

    /**
     * Centurion (백부장): HP +6
     */
    @SpirePatch(clz = Centurion.class, method = SpirePatch.CONSTRUCTOR, paramtypez = { float.class, float.class })
    public static class CenturionHPPatch {
        @SpirePostfixPatch
        public static void Postfix(Centurion __instance) {
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 51) {
                return;
            }

            int originalHP = __instance.maxHealth;
            __instance.maxHealth += 6;
            __instance.currentHealth += 6;

            logger.info(String.format(
                    "Ascension 51: Centurion HP increased from %d to %d (+6)",
                    originalHP, __instance.maxHealth));
        }
    }

    /**
     * Snecko (스네코): Damage +2
     */
    @SpirePatch(clz = Snecko.class, method = SpirePatch.CONSTRUCTOR, paramtypez = { float.class, float.class })
    public static class SneckoDamagePatch {
        @SpirePostfixPatch
        public static void Postfix(Snecko __instance) {
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 51) {
                return;
            }

            // Increase all damage by 2
            for (DamageInfo damage : __instance.damage) {
                damage.base += 2;
            }

            logger.info("Ascension 51: Snecko damage increased by +2");
        }
    }

    /**
     * SphericGuardian (구체형 수호기): Block +15
     */
    @SpirePatch(clz = SphericGuardian.class, method = "usePreBattleAction")
    public static class SphericGuardianBlockIncrease {
        @SpirePostfixPatch
        public static void Postfix(SphericGuardian __instance) {
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 51) {
                return;
            }

            AbstractDungeon.actionManager.addToBottom(
                    new GainBlockAction(__instance, __instance, 15));

            logger.info("Ascension 51: SphericGuardian gained +15 block at battle start");
        }
    }

    /**
     * BanditPointy (촉새): Damage +1
     */
    @SpirePatch(clz = BanditPointy.class, method = SpirePatch.CONSTRUCTOR, paramtypez = { float.class, float.class })
    public static class BanditPointyDamagePatch {
        @SpirePostfixPatch
        public static void Postfix(BanditPointy __instance) {
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 51) {
                return;
            }

            // Increase all damage by 1
            for (DamageInfo damage : __instance.damage) {
                damage.base += 1;
            }

            logger.info("Ascension 51: BanditPointy damage increased by +1");
        }
    }

    /**
     * BanditLeader (로미오): HP +5
     */
    @SpirePatch(clz = BanditLeader.class, method = SpirePatch.CONSTRUCTOR, paramtypez = { float.class, float.class })
    public static class BanditLeaderHPPatch {
        @SpirePostfixPatch
        public static void Postfix(BanditLeader __instance) {
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 51) {
                return;
            }

            int originalHP = __instance.maxHealth;
            __instance.maxHealth += 5;
            __instance.currentHealth += 5;

            logger.info(String.format(
                    "Ascension 51: BanditLeader HP increased from %d to %d (+5)",
                    originalHP, __instance.maxHealth));
        }
    }

    /**
     * Repulsor (현혹기): Damage +2
     */
    @SpirePatch(clz = Repulsor.class, method = SpirePatch.CONSTRUCTOR, paramtypez = { float.class, float.class })
    public static class RepulsorDamagePatch {
        @SpirePostfixPatch
        public static void Postfix(Repulsor __instance) {
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 51) {
                return;
            }

            // Increase all damage by 2
            for (DamageInfo damage : __instance.damage) {
                damage.base += 2;
            }

            logger.info("Ascension 51: Repulsor damage increased by +2");
        }
    }

    /**
     * OrbWalker (구체 순찰기): HP +10
     */
    @SpirePatch(clz = OrbWalker.class, method = SpirePatch.CONSTRUCTOR, paramtypez = { float.class, float.class })
    public static class OrbWalkerHPPatch {
        @SpirePostfixPatch
        public static void Postfix(OrbWalker __instance) {
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 51) {
                return;
            }

            int originalHP = __instance.maxHealth;
            __instance.maxHealth += 10;
            __instance.currentHealth += 10;

            logger.info(String.format(
                    "Ascension 51: OrbWalker HP increased from %d to %d (+10)",
                    originalHP, __instance.maxHealth));
        }
    }

    /**
     * Transient (과도자): Damage +5
     */
    @SpirePatch(clz = Transient.class, method = SpirePatch.CONSTRUCTOR, paramtypez = {})
    public static class TransientDamagePatch {
        @SpirePostfixPatch
        public static void Postfix(Transient __instance) {
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 51) {
                return;
            }

            // Increase all damage by 5
            for (DamageInfo damage : __instance.damage) {
                damage.base += 5;
            }

            logger.info("Ascension 51: Transient damage increased by +5");
        }
    }

    /**
     * SpireGrowth (첨탑 암종): HP +15
     */
    @SpirePatch(clz = SpireGrowth.class, method = SpirePatch.CONSTRUCTOR)
    public static class SpireGrowthHPPatch {
        @SpirePostfixPatch
        public static void Postfix(SpireGrowth __instance) {
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 51) {
                return;
            }

            int originalHP = __instance.maxHealth;
            __instance.maxHealth += 15;
            __instance.currentHealth += 15;

            logger.info(String.format(
                    "Ascension 51: SpireGrowth HP increased from %d to %d (+15)",
                    originalHP, __instance.maxHealth));
        }
    }
}
