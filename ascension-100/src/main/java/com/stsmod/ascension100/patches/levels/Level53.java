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
import com.megacrit.cardcrawl.powers.AbstractPower;
import com.megacrit.cardcrawl.powers.ExplosivePower;
import javassist.CtBehavior;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.reflect.Field;

/**
 * Ascension Level 53: Enemy action patterns enhanced
 * 모든 적의 행동 패턴이 강화됩니다.
 *
 * 상세효과:
 * - WrithingMass의 기생충 추가 패턴(MEGA_DEBUFF)을 2회 사용 가능
 * - 폭탄기(Exploder): 폭발 패턴 데미지를 50으로 증가 (기본 30)
 * - 강도(Mugger): 도둑질 수치가 5 증가
 * - 뱀 식물(Snake Plant): 기본 탄성 수치가 1 증가
 */
public class Level53 {
    private static final Logger logger = LogManager.getLogger(Level53.class.getName());

    /**
     * Allow WrithingMass to use IMPLANT pattern twice instead of once
     */
    @SpirePatch(
        clz = WrithingMass.class,
        method = "takeTurn"
    )
    public static class AllowTwoImplants {
        private static boolean hasResetOnce = false;

        @SpirePostfixPatch
        public static void Postfix(WrithingMass __instance) {
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 53) {
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
                        "[Asc53] WrithingMass IMPLANT flag reset - can use one more time (1/2 used)"
                    ));
                } catch (Exception e) {
                    logger.error("[Asc53] Failed to reset usedMegaDebuff flag: " + e.getMessage());
                }
            } else if (__instance.nextMove == 4 && hasResetOnce) {
                logger.info("[Asc53] WrithingMass IMPLANT used 2/2 times - no more uses");
            }
        }
    }

    /**
     * Reset the flag counter at the start of each battle
     */
    @SpirePatch(
        clz = WrithingMass.class,
        method = "usePreBattleAction"
    )
    public static class ResetBattleCounter {
        @SpirePostfixPatch
        public static void Postfix(WrithingMass __instance) {
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 53) {
                return;
            }

            // 전투 시작 시 카운터 초기화
            AllowTwoImplants.hasResetOnce = false;

            logger.info("[Asc53] WrithingMass battle started - IMPLANT counter reset (0/2 used)");
        }
    }

    /**
     * Exploder: Explosion damage increased
     * 폭탄기: 폭발 패턴 데미지 증가
     * - Level 53-85: 30 → 50
     * - Level 86+: 30 → 60
     */
    @SpirePatch(
        clz = com.megacrit.cardcrawl.actions.common.DamageAction.class,
        method = SpirePatch.CONSTRUCTOR,
        paramtypez = {
            com.megacrit.cardcrawl.core.AbstractCreature.class,
            DamageInfo.class,
            com.megacrit.cardcrawl.actions.AbstractGameAction.AttackEffect.class,
            boolean.class
        }
    )
    public static class ExploderExplosionDamage {
        @SpirePostfixPatch
        public static void Postfix(
            com.megacrit.cardcrawl.actions.common.DamageAction __instance,
            com.megacrit.cardcrawl.core.AbstractCreature target,
            DamageInfo info,
            com.megacrit.cardcrawl.actions.AbstractGameAction.AttackEffect effect,
            boolean superFast
        ) {
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 53) {
                return;
            }

            // Only modify ExplosivePower explosion damage (THORNS type, 30 base damage, target is player)
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
                    logger.info("Ascension 53: ExplosivePower explosion damage increased from 30 to 50");
                }
            }
        }
    }

    /**
     * ExplosivePower: Update tooltip to show correct damage based on ascension level
     * 폭발성 파워: 승천 레벨에 맞는 데미지를 툴팁에 표시
     * - Level 53-85: 50 damage
     * - Level 86+: 60 damage
     */
    @SpirePatch(
        clz = ExplosivePower.class,
        method = "updateDescription"
    )
    public static class ExplosivePowerTooltipFix {
        @SpirePostfixPatch
        public static void Postfix(ExplosivePower __instance) {
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 53) {
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
                    logger.info("Ascension 53: ExplosivePower tooltip updated to show 50 damage");
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
    @SpirePatch(
        clz = SlaverBlue.class,
        method = SpirePatch.CONSTRUCTOR
    )
    public static class SlaverBlueDamagePatch {
        @SpirePostfixPatch
        public static void Postfix(SlaverBlue __instance, float x, float y) {
            if (AbstractDungeon.isAscensionMode && AbstractDungeon.ascensionLevel >= 53) {
                // Increase Stab damage (+1)
                if (!__instance.damage.isEmpty()) {
                    DamageInfo stabDamage = __instance.damage.get(0); // Stab attack
                    int originalDamage = stabDamage.base;
                    stabDamage.base += 1;

                    logger.info(String.format(
                        "Ascension 53: Slaver Blue Stab damage increased from %d to %d",
                        originalDamage, stabDamage.base
                    ));
                }
            }
        }
    }

    /**
     * Slaver Red (붉은색 노예 상인): Damage +1
     */
    @SpirePatch(
        clz = SlaverRed.class,
        method = SpirePatch.CONSTRUCTOR
    )
    public static class SlaverRedDamagePatch {
        @SpirePostfixPatch
        public static void Postfix(SlaverRed __instance, float x, float y) {
            if (AbstractDungeon.isAscensionMode && AbstractDungeon.ascensionLevel >= 53) {
                // Increase Stab damage (+1)
                if (!__instance.damage.isEmpty()) {
                    DamageInfo stabDamage = __instance.damage.get(0); // Stab attack
                    int originalDamage = stabDamage.base;
                    stabDamage.base += 1;

                    logger.info(String.format(
                        "Ascension 53: Slaver Red Stab damage increased from %d to %d",
                        originalDamage, stabDamage.base
                    ));
                }
            }
        }
    }

    /**
     * Mugger (강도): Thievery +5
     */
    @SpirePatch(
        clz = Mugger.class,
        method = "usePreBattleAction"
    )
    public static class MuggerThieveryIncrease {
        @SpirePostfixPatch
        public static void Postfix(Mugger __instance) {
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 53) {
                return;
            }

            // Increase Thievery power by 5
            AbstractPower thieveryPower = __instance.getPower("Thievery");
            if (thieveryPower != null) {
                thieveryPower.amount += 5;
                thieveryPower.updateDescription();
                logger.info(String.format(
                    "Ascension 53: Mugger Thievery increased by 5 to %d",
                    thieveryPower.amount
                ));
            }
        }
    }

    /**
     * Snake Plant (뱀 식물): Malleable +1
     */
    @SpirePatch(
        clz = SnakePlant.class,
        method = "usePreBattleAction"
    )
    public static class SnakePlantMalleableIncrease {
        @SpirePostfixPatch
        public static void Postfix(SnakePlant __instance) {
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 53) {
                return;
            }

            // Increase Malleable power by 1
            AbstractPower malleablePower = __instance.getPower("Malleable");
            if (malleablePower != null) {
                malleablePower.amount += 1;
                malleablePower.updateDescription();
                logger.info(String.format(
                    "Ascension 53: Snake Plant Malleable increased by 1 to %d",
                    malleablePower.amount
                ));
            }
        }
    }

    /**
     * Looter (도적): Smoke Bomb block +4
     */
    @SpirePatch(
        clz = com.megacrit.cardcrawl.monsters.exordium.Looter.class,
        method = "takeTurn"
    )
    public static class LooterSmokeBombBlockIncrease {
        @SpirePostfixPatch
        public static void Postfix(com.megacrit.cardcrawl.monsters.exordium.Looter __instance) {
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 53) {
                return;
            }

            try {
                // Check if Smoke Bomb move (ESCAPE move)
                java.lang.reflect.Field nextMoveField = AbstractMonster.class.getDeclaredField("nextMove");
                nextMoveField.setAccessible(true);
                byte move = nextMoveField.getByte(__instance);

                if (move == 2) { // ESCAPE (Smoke Bomb) move
                    // Increase escapeDef by 4
                    java.lang.reflect.Field escapeDefField = com.megacrit.cardcrawl.monsters.exordium.Looter.class.getDeclaredField("escapeDef");
                    escapeDefField.setAccessible(true);
                    int currentEscapeDef = escapeDefField.getInt(__instance);
                    escapeDefField.setInt(__instance, currentEscapeDef + 4);

                    logger.info(String.format(
                        "Ascension 53: Looter Smoke Bomb block increased by 4 to %d",
                        currentEscapeDef + 4
                    ));
                }
            } catch (Exception e) {
                logger.error("Failed to modify Looter Smoke Bomb block", e);
            }
        }
    }

    /**
     * Mugger (강도): Smoke Bomb block +3
     */
    @SpirePatch(
        clz = Mugger.class,
        method = "takeTurn"
    )
    public static class MuggerSmokeBombBlockIncrease {
        @SpirePostfixPatch
        public static void Postfix(Mugger __instance) {
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 53) {
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
                        "Ascension 53: Mugger Smoke Bomb block increased by 3 to %d",
                        currentEscapeDef + 3
                    ));
                }
            } catch (Exception e) {
                logger.error("Failed to modify Mugger Smoke Bomb block", e);
            }
        }
    }

    /**
     * Healer (신비주의자/Mystic): Heal pattern +5 heal amount
     */
    @SpirePatch(
        clz = com.megacrit.cardcrawl.monsters.city.Healer.class,
        method = "takeTurn"
    )
    public static class HealerHealAmountIncrease {
        private static final ThreadLocal<Byte> lastMove = new ThreadLocal<>();

        @SpirePrefixPatch
        public static void Prefix(com.megacrit.cardcrawl.monsters.city.Healer __instance) {
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 53) {
                return;
            }

            try {
                java.lang.reflect.Field nextMoveField = AbstractMonster.class.getDeclaredField("nextMove");
                nextMoveField.setAccessible(true);
                byte move = nextMoveField.getByte(__instance);
                lastMove.set(move);

                if (move == 2) { // HEAL move
                    // Increase healAmt by 5 BEFORE takeTurn executes
                    java.lang.reflect.Field healAmtField = com.megacrit.cardcrawl.monsters.city.Healer.class.getDeclaredField("healAmt");
                    healAmtField.setAccessible(true);
                    int currentHealAmt = healAmtField.getInt(__instance);
                    healAmtField.setInt(__instance, currentHealAmt + 5);

                    logger.info(String.format(
                        "Ascension 53: Healer heal amount increased by 5 to %d",
                        currentHealAmt + 5
                    ));
                }
            } catch (Exception e) {
                logger.error("Failed to increase Healer heal amount", e);
            }
        }

        @SpirePostfixPatch
        public static void Postfix(com.megacrit.cardcrawl.monsters.city.Healer __instance) {
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 53) {
                return;
            }

            Byte move = lastMove.get();
            if (move != null && move == 2) { // HEAL move
                try {
                    // Reset healAmt back to original after HEAL
                    java.lang.reflect.Field healAmtField = com.megacrit.cardcrawl.monsters.city.Healer.class.getDeclaredField("healAmt");
                    healAmtField.setAccessible(true);
                    int currentHealAmt = healAmtField.getInt(__instance);
                    healAmtField.setInt(__instance, currentHealAmt - 5);

                    logger.info("Ascension 53: Healer heal amount reset to original");
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
    @SpirePatch(
        clz = com.megacrit.cardcrawl.monsters.city.BanditBear.class,
        method = "takeTurn"
    )
    public static class BanditBearBearHugDebuffIncrease {
        @SpirePostfixPatch
        public static void Postfix(com.megacrit.cardcrawl.monsters.city.BanditBear __instance) {
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 53) {
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
                            -1
                        )
                    );

                    logger.info("Ascension 53: BanditBear Bear Hug applied -1 additional Dexterity");
                }
            } catch (Exception e) {
                logger.error("Failed to modify BanditBear Bear Hug", e);
            }
        }
    }

    /**
     * Spiker (반사기): Power pattern Thorns +1
     */
    @SpirePatch(
        clz = com.megacrit.cardcrawl.monsters.beyond.Spiker.class,
        method = "takeTurn"
    )
    public static class SpikerThornsPowerIncrease {
        @SpirePostfixPatch
        public static void Postfix(com.megacrit.cardcrawl.monsters.beyond.Spiker __instance) {
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 53) {
                return;
            }

            try {
                // Check if Power move
                java.lang.reflect.Field nextMoveField = AbstractMonster.class.getDeclaredField("nextMove");
                nextMoveField.setAccessible(true);
                byte move = nextMoveField.getByte(__instance);

                if (move == 2) { // POWER move
                    // Apply additional +1 Thorns
                    AbstractDungeon.actionManager.addToBottom(
                        new com.megacrit.cardcrawl.actions.common.ApplyPowerAction(
                            __instance,
                            __instance,
                            new com.megacrit.cardcrawl.powers.ThornsPower(__instance, 1),
                            1
                        )
                    );

                    logger.info("Ascension 53: Spiker gained +1 additional Thorns");
                }
            } catch (Exception e) {
                logger.error("Failed to modify Spiker Thorns", e);
            }
        }
    }

    /**
     * Darkling (어두미): Revive grants Strength +2
     */
    @SpirePatch(
        clz = com.megacrit.cardcrawl.monsters.beyond.Darkling.class,
        method = "takeTurn"
    )
    public static class DarklingReviveStrengthGain {
        @SpirePostfixPatch
        public static void Postfix(com.megacrit.cardcrawl.monsters.beyond.Darkling __instance) {
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 53) {
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
                            2
                        )
                    );

                    logger.info("Ascension 53: Darkling gained +2 Strength on revive");
                }
            } catch (Exception e) {
                logger.error("Failed to modify Darkling revive", e);
            }
        }
    }

    /**
     * Maw (아귀): Roar pattern Weak and Frail +1
     */
    @SpirePatch(
        clz = com.megacrit.cardcrawl.monsters.beyond.Maw.class,
        method = "takeTurn"
    )
    public static class MawRoarDebuffIncrease {
        @SpirePostfixPatch
        public static void Postfix(com.megacrit.cardcrawl.monsters.beyond.Maw __instance) {
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 53) {
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
                            1
                        )
                    );
                    AbstractDungeon.actionManager.addToBottom(
                        new com.megacrit.cardcrawl.actions.common.ApplyPowerAction(
                            AbstractDungeon.player,
                            __instance,
                            new com.megacrit.cardcrawl.powers.FrailPower(AbstractDungeon.player, 1, true),
                            1
                        )
                    );

                    logger.info("Ascension 53: Maw Roar applied +1 additional Weak and Frail");
                }
            } catch (Exception e) {
                logger.error("Failed to modify Maw Roar", e);
            }
        }
    }
}
