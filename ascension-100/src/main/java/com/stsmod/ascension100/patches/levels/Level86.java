package com.stsmod.ascension100.patches.levels;

import com.evacipated.cardcrawl.modthespire.lib.*;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import javassist.CtBehavior;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Ascension Level 86: Enemy patterns enhanced
 *
 * 적의 패턴이 강화됩니다.
 *
 * 공벌레(Louse) : 몸 말기 (Curl Up) 수치가 6 증가
 * 도적(Looter) : 연막탄 패턴의 방어도가 5 증가, 도둑질 수치가 10 증가
 * 동물하초(Fungi Beast) : 죽을 때 가하는 취약과 약화가 1 증가
 * 그렘린(Gremlin) :
 *   방패 그렘린 : 시전하는 보호의 수치가 3 증가
 *   마법사 그렘린 : 공격력이 5 증가
 *   화난 그렘린 : 체력이 2 증가
 *   뚱뚱한 그렘린 : 체력이 2 증가
 *   교활한 그렘린 : 공격력이 1 증가
 * 갑각기생충(Shelled Parasite) : 판금갑옷을 추가로 2 얻습니다
 * 강도(Mugger) : 도둑질 수치가 15 증가, 연막탄 패턴의 방어도가 3 증가
 * 신비주의자(Mystic/Healer) : 회복(Heal) 패턴의 체력 회복량이 3 증가
 * 뱀 식물(Snake Plant) : 기본 탄성 수치가 2 증가
 * 구체형 수호기(Spheric Guardian) : 경화 패턴에서 얻는 방어도가 10 증가
 * 곰(Bear) : 베어허그 패턴의 민첩 감소수치가 1 증가
 * 반사기(Spiker) : 파워 패턴의 가시 수치가 1 증가
 * 폭탄기(Exploder) : 폭발 패턴은 데미지를 60 가합니다
 * 아귀(The Maw) : 포효 패턴의 약화, 손상이 1 증가
 * 과도자(Transient) : 과도자의 희미해짐 수치가 1 증가
 */
public class Level86 {
    private static final Logger logger = LogManager.getLogger(Level86.class.getName());

    /**
     * Louse (LouseNormal, LouseDefensive): Curl Up +6
     * MOVED TO: LouseCurlUpPatch.java (unified patch)
     */

    /**
     * Looter: Thievery +10, Smoke Bomb defense +5
     *
     * IMPORTANT: Must modify goldAmt field BEFORE usePreBattleAction creates ThieveryPower
     */
    @SpirePatch(
        clz = com.megacrit.cardcrawl.monsters.exordium.Looter.class,
        method = "usePreBattleAction"
    )
    public static class LooterThieveryBoost {
        @SpirePrefixPatch
        public static void Prefix(com.megacrit.cardcrawl.monsters.exordium.Looter __instance) {
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 86) {
                return;
            }

            try {
                // Increase goldAmt BEFORE ThieveryPower is created
                java.lang.reflect.Field goldAmtField = com.megacrit.cardcrawl.monsters.exordium.Looter.class.getDeclaredField("goldAmt");
                goldAmtField.setAccessible(true);
                int currentGoldAmt = goldAmtField.getInt(__instance);
                goldAmtField.setInt(__instance, currentGoldAmt + 10);

                logger.info(String.format(
                    "Ascension 86: Looter goldAmt increased from %d to %d (Thievery will be %d)",
                    currentGoldAmt, currentGoldAmt + 10, currentGoldAmt + 10
                ));
            } catch (Exception e) {
                logger.error("Failed to modify Looter goldAmt", e);
            }
        }

        @SpirePostfixPatch
        public static void Postfix(com.megacrit.cardcrawl.monsters.exordium.Looter __instance) {
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 86) {
                return;
            }

            try {
                // Reset goldAmt back to original after ThieveryPower is created
                java.lang.reflect.Field goldAmtField = com.megacrit.cardcrawl.monsters.exordium.Looter.class.getDeclaredField("goldAmt");
                goldAmtField.setAccessible(true);
                int currentGoldAmt = goldAmtField.getInt(__instance);
                goldAmtField.setInt(__instance, currentGoldAmt - 10);

                logger.info("Ascension 86: Looter goldAmt reset to original (Thievery amount remains increased)");
            } catch (Exception e) {
                logger.error("Failed to reset Looter goldAmt", e);
            }
        }
    }

    @SpirePatch(
        clz = com.megacrit.cardcrawl.monsters.exordium.Looter.class,
        method = "takeTurn"
    )
    public static class LooterSmokeBombDefenseBoost {
        @SpirePostfixPatch
        public static void Postfix(com.megacrit.cardcrawl.monsters.exordium.Looter __instance) {
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 86) {
                return;
            }

            try {
                // Check if Smoke Bomb move (ESCAPE move)
                java.lang.reflect.Field nextMoveField = com.megacrit.cardcrawl.monsters.AbstractMonster.class.getDeclaredField("nextMove");
                nextMoveField.setAccessible(true);
                byte move = nextMoveField.getByte(__instance);

                if (move == 2) { // ESCAPE (Smoke Bomb) move
                    // Increase escapeDef by 5 (in addition to Level53's +4)
                    java.lang.reflect.Field escapeDefField = com.megacrit.cardcrawl.monsters.exordium.Looter.class.getDeclaredField("escapeDef");
                    escapeDefField.setAccessible(true);
                    int escapeDef = escapeDefField.getInt(__instance);
                    escapeDefField.setInt(__instance, escapeDef + 5);

                    logger.info(String.format(
                        "Ascension 86: Looter Smoke Bomb defense increased by 5 to %d",
                        escapeDef + 5
                    ));
                }
            } catch (Exception e) {
                logger.error("Failed to modify Looter escapeDef", e);
            }
        }
    }

    /**
     * FungiBeast: Spore Cloud +1 vulnerable, +1 weak on death (via constructor intercept)
     */
    @SpirePatch(
        clz = com.megacrit.cardcrawl.powers.SporeCloudPower.class,
        method = SpirePatch.CONSTRUCTOR
    )
    public static class FungiBeastSporeCloudBoost {
        @SpirePostfixPatch
        public static void Postfix(com.megacrit.cardcrawl.powers.SporeCloudPower __instance,
                                    com.megacrit.cardcrawl.core.AbstractCreature owner, int amount) {
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 86) {
                return;
            }

            // Check if owner is FungiBeast
            if (owner instanceof com.megacrit.cardcrawl.monsters.exordium.FungiBeast) {
                __instance.amount += 1;
                __instance.updateDescription();
                logger.info(String.format(
                    "Ascension 86: FungiBeast SporeCloud constructor intercepted, increased from %d to %d",
                    amount, __instance.amount
                ));
            }

            // Note: Weak debuff is added in die() method
        }
    }

    @SpirePatch(
        clz = com.megacrit.cardcrawl.monsters.AbstractMonster.class,
        method = "die",
        paramtypez = {boolean.class}
    )
    public static class FungiBeastWeakOnDeath {
        @SpirePostfixPatch
        public static void Postfix(com.megacrit.cardcrawl.monsters.AbstractMonster __instance, boolean triggerRelics) {
            // Only apply to FungiBeast
            if (!(__instance instanceof com.megacrit.cardcrawl.monsters.exordium.FungiBeast)) {
                return;
            }

            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 86) {
                return;
            }

            if (AbstractDungeon.getCurrRoom().isBattleEnding()) {
                return;
            }

            // Apply Weak 1 to player on death (in addition to Vulnerable from Spore Cloud)
            AbstractDungeon.actionManager.addToTop(
                new com.megacrit.cardcrawl.actions.common.ApplyPowerAction(
                    AbstractDungeon.player,
                    null,
                    new com.megacrit.cardcrawl.powers.WeakPower(AbstractDungeon.player, 1, true),
                    1
                )
            );

            logger.info("Ascension 86: FungiBeast applied Weak 1 on death");
        }
    }

    /**
     * GremlinTsundere (Shield Gremlin): Block amount +3
     */
    @SpirePatch(
        clz = com.megacrit.cardcrawl.monsters.exordium.GremlinTsundere.class,
        method = SpirePatch.CONSTRUCTOR
    )
    public static class GremlinTsundereBlockBoost {
        @SpirePostfixPatch
        public static void Postfix(com.megacrit.cardcrawl.monsters.exordium.GremlinTsundere __instance, float x, float y) {
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 86) {
                return;
            }

            try {
                // Increase blockAmt field by 3
                java.lang.reflect.Field blockAmtField = com.megacrit.cardcrawl.monsters.exordium.GremlinTsundere.class.getDeclaredField("blockAmt");
                blockAmtField.setAccessible(true);
                int blockAmt = blockAmtField.getInt(__instance);
                blockAmtField.setInt(__instance, blockAmt + 3);

                logger.info(String.format(
                    "Ascension 86: GremlinTsundere block amount increased by 3 to %d",
                    blockAmt + 3
                ));
            } catch (Exception e) {
                logger.error("Failed to modify GremlinTsundere blockAmt", e);
            }
        }
    }

    /**
     * GremlinWizard: Attack damage +5
     */
    @SpirePatch(
        clz = com.megacrit.cardcrawl.monsters.exordium.GremlinWizard.class,
        method = SpirePatch.CONSTRUCTOR
    )
    public static class GremlinWizardDamageBoost {
        @SpirePostfixPatch
        public static void Postfix(com.megacrit.cardcrawl.monsters.exordium.GremlinWizard __instance, float x, float y) {
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 86) {
                return;
            }

            // Increase damage by 5
            if (!__instance.damage.isEmpty()) {
                com.megacrit.cardcrawl.cards.DamageInfo damageInfo = __instance.damage.get(0);
                damageInfo.base += 5;
                damageInfo.output += 5;

                logger.info(String.format(
                    "Ascension 86: GremlinWizard damage increased by 5 to %d",
                    damageInfo.base
                ));
            }
        }
    }

    /**
     * GremlinWarrior (Angry Gremlin): HP +2
     */
    @SpirePatch(
        clz = com.megacrit.cardcrawl.monsters.exordium.GremlinWarrior.class,
        method = SpirePatch.CONSTRUCTOR
    )
    public static class GremlinWarriorHPBoost {
        @SpirePostfixPatch
        public static void Postfix(com.megacrit.cardcrawl.monsters.exordium.GremlinWarrior __instance, float x, float y) {
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 86) {
                return;
            }

            // Increase max HP by 2
            __instance.maxHealth += 2;
            __instance.currentHealth = __instance.maxHealth;
            __instance.healthBarUpdatedEvent();

            logger.info(String.format(
                "Ascension 86: GremlinWarrior HP increased by 2 to %d",
                __instance.maxHealth
            ));
        }
    }

    /**
     * GremlinFat: HP +2
     */
    @SpirePatch(
        clz = com.megacrit.cardcrawl.monsters.exordium.GremlinFat.class,
        method = SpirePatch.CONSTRUCTOR
    )
    public static class GremlinFatHPBoost {
        @SpirePostfixPatch
        public static void Postfix(com.megacrit.cardcrawl.monsters.exordium.GremlinFat __instance, float x, float y) {
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 86) {
                return;
            }

            // Increase max HP by 2
            __instance.maxHealth += 2;
            __instance.currentHealth = __instance.maxHealth;
            __instance.healthBarUpdatedEvent();

            logger.info(String.format(
                "Ascension 86: GremlinFat HP increased by 2 to %d",
                __instance.maxHealth
            ));
        }
    }

    /**
     * GremlinThief (Sneaky Gremlin): Attack damage +1
     */
    @SpirePatch(
        clz = com.megacrit.cardcrawl.monsters.exordium.GremlinThief.class,
        method = SpirePatch.CONSTRUCTOR
    )
    public static class GremlinThiefDamageBoost {
        @SpirePostfixPatch
        public static void Postfix(com.megacrit.cardcrawl.monsters.exordium.GremlinThief __instance, float x, float y) {
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 86) {
                return;
            }

            // Increase damage by 1
            if (!__instance.damage.isEmpty()) {
                com.megacrit.cardcrawl.cards.DamageInfo damageInfo = __instance.damage.get(0);
                damageInfo.base += 1;
                damageInfo.output += 1;

                logger.info(String.format(
                    "Ascension 86: GremlinThief damage increased by 1 to %d",
                    damageInfo.base
                ));
            }
        }
    }

    /**
     * ShelledParasite: Plated Armor +2
     */
    @SpirePatch(
        clz = com.megacrit.cardcrawl.monsters.city.ShelledParasite.class,
        method = "usePreBattleAction"
    )
    public static class ShelledParasitePlatedArmorBoost {
        @SpirePostfixPatch
        public static void Postfix(com.megacrit.cardcrawl.monsters.city.ShelledParasite __instance) {
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 86) {
                return;
            }

            // Add +2 to Plated Armor power
            com.megacrit.cardcrawl.powers.AbstractPower platedArmorPower = __instance.getPower("Plated Armor");
            if (platedArmorPower != null) {
                platedArmorPower.amount += 2;
                platedArmorPower.updateDescription();
                logger.info(String.format(
                    "Ascension 86: ShelledParasite Plated Armor increased by 2 to %d",
                    platedArmorPower.amount
                ));
            }
        }
    }

    /**
     * Mugger: Thievery +15, Smoke Bomb defense +3
     *
     * IMPORTANT: Must modify goldAmt field BEFORE usePreBattleAction creates ThieveryPower
     */
    @SpirePatch(
        clz = com.megacrit.cardcrawl.monsters.city.Mugger.class,
        method = "usePreBattleAction"
    )
    public static class MuggerThieveryBoost {
        @SpirePrefixPatch
        public static void Prefix(com.megacrit.cardcrawl.monsters.city.Mugger __instance) {
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 86) {
                return;
            }

            try {
                // Increase goldAmt BEFORE ThieveryPower is created
                java.lang.reflect.Field goldAmtField = com.megacrit.cardcrawl.monsters.city.Mugger.class.getDeclaredField("goldAmt");
                goldAmtField.setAccessible(true);
                int currentGoldAmt = goldAmtField.getInt(__instance);
                goldAmtField.setInt(__instance, currentGoldAmt + 15);

                logger.info(String.format(
                    "Ascension 86: Mugger goldAmt increased from %d to %d (Thievery will be %d)",
                    currentGoldAmt, currentGoldAmt + 15, currentGoldAmt + 15
                ));
            } catch (Exception e) {
                logger.error("Failed to modify Mugger goldAmt", e);
            }
        }

        @SpirePostfixPatch
        public static void Postfix(com.megacrit.cardcrawl.monsters.city.Mugger __instance) {
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 86) {
                return;
            }

            try {
                // Reset goldAmt back to original after ThieveryPower is created
                java.lang.reflect.Field goldAmtField = com.megacrit.cardcrawl.monsters.city.Mugger.class.getDeclaredField("goldAmt");
                goldAmtField.setAccessible(true);
                int currentGoldAmt = goldAmtField.getInt(__instance);
                goldAmtField.setInt(__instance, currentGoldAmt - 15);

                logger.info("Ascension 86: Mugger goldAmt reset to original (Thievery amount remains increased)");
            } catch (Exception e) {
                logger.error("Failed to reset Mugger goldAmt", e);
            }
        }
    }

    @SpirePatch(
        clz = com.megacrit.cardcrawl.monsters.city.Mugger.class,
        method = SpirePatch.CONSTRUCTOR
    )
    public static class MuggerSmokeBombDefenseBoost {
        @SpirePostfixPatch
        public static void Postfix(com.megacrit.cardcrawl.monsters.city.Mugger __instance, float x, float y) {
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 86) {
                return;
            }

            try {
                // Increase escapeDef field by 3
                java.lang.reflect.Field escapeDefField = com.megacrit.cardcrawl.monsters.city.Mugger.class.getDeclaredField("escapeDef");
                escapeDefField.setAccessible(true);
                int escapeDef = escapeDefField.getInt(__instance);
                escapeDefField.setInt(__instance, escapeDef + 3);

                logger.info(String.format(
                    "Ascension 86: Mugger Smoke Bomb defense increased by 3 to %d",
                    escapeDef + 3
                ));
            } catch (Exception e) {
                logger.error("Failed to modify Mugger escapeDef", e);
            }
        }
    }

    /**
     * Healer (Mystic): Heal amount +3
     */
    @SpirePatch(
        clz = com.megacrit.cardcrawl.monsters.city.Healer.class,
        method = SpirePatch.CONSTRUCTOR
    )
    public static class HealerHealAmountBoost {
        @SpirePostfixPatch
        public static void Postfix(com.megacrit.cardcrawl.monsters.city.Healer __instance, float x, float y) {
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 86) {
                return;
            }

            try {
                // Increase healAmt field by 3
                java.lang.reflect.Field healAmtField = com.megacrit.cardcrawl.monsters.city.Healer.class.getDeclaredField("healAmt");
                healAmtField.setAccessible(true);
                int healAmt = healAmtField.getInt(__instance);
                healAmtField.setInt(__instance, healAmt + 3);

                logger.info(String.format(
                    "Ascension 86: Healer heal amount increased by 3 to %d",
                    healAmt + 3
                ));
            } catch (Exception e) {
                logger.error("Failed to modify Healer healAmt", e);
            }
        }
    }

    /**
     * SnakePlant: Malleable +2
     */
    @SpirePatch(
        clz = com.megacrit.cardcrawl.monsters.city.SnakePlant.class,
        method = "usePreBattleAction"
    )
    public static class SnakePlantMalleableBoost {
        @SpirePostfixPatch
        public static void Postfix(com.megacrit.cardcrawl.monsters.city.SnakePlant __instance) {
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 86) {
                return;
            }

            // Add +2 to Malleable power
            com.megacrit.cardcrawl.powers.AbstractPower malleablePower = __instance.getPower("Malleable");
            if (malleablePower != null) {
                malleablePower.amount += 2;
                malleablePower.updateDescription();
                logger.info(String.format(
                    "Ascension 86: SnakePlant Malleable increased by 2 to %d",
                    malleablePower.amount
                ));
            }
        }
    }

    /**
     * SphericGuardian: Harden block +10
     * Override case 3 (HARDEN) to use 25 block instead of 15
     */
    @SpirePatch(
        clz = com.megacrit.cardcrawl.monsters.city.SphericGuardian.class,
        method = "takeTurn"
    )
    public static class SphericGuardianHardenBoost {
        @SpirePrefixPatch
        public static SpireReturn<Void> Prefix(com.megacrit.cardcrawl.monsters.city.SphericGuardian __instance) {
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 86) {
                return SpireReturn.Continue();
            }

            try {
                // Check if Harden move (byte 3) is being executed
                java.lang.reflect.Field nextMoveField = com.megacrit.cardcrawl.monsters.city.SphericGuardian.class.getSuperclass().getDeclaredField("nextMove");
                nextMoveField.setAccessible(true);
                byte nextMove = nextMoveField.getByte(__instance);

                if (nextMove == 3) { // HARDEN - override entire move with increased block
                    // Original: 15 block + attack
                    // Level 86: 25 block + attack (unified action)
                    AbstractDungeon.actionManager.addToBottom(
                        new com.megacrit.cardcrawl.actions.common.GainBlockAction(
                            __instance,
                            __instance,
                            25  // 15 base + 10 bonus
                        )
                    );
                    AbstractDungeon.actionManager.addToBottom(
                        new com.megacrit.cardcrawl.actions.animations.AnimateFastAttackAction(__instance)
                    );
                    AbstractDungeon.actionManager.addToBottom(
                        new com.megacrit.cardcrawl.actions.common.DamageAction(
                            AbstractDungeon.player,
                            __instance.damage.get(0),
                            com.megacrit.cardcrawl.actions.AbstractGameAction.AttackEffect.BLUNT_HEAVY
                        )
                    );
                    AbstractDungeon.actionManager.addToBottom(
                        new com.megacrit.cardcrawl.actions.common.RollMoveAction(__instance)
                    );

                    logger.info("Ascension 86: SphericGuardian Harden executed with 25 block (15 base + 10 bonus)");

                    // Skip original takeTurn for this move
                    return SpireReturn.Return(null);
                }
            } catch (Exception e) {
                logger.error("Failed to override SphericGuardian Harden", e);
            }

            return SpireReturn.Continue();
        }
    }

    /**
     * BanditBear: Bear Hug dexterity loss +1
     */
    @SpirePatch(
        clz = com.megacrit.cardcrawl.monsters.city.BanditBear.class,
        method = "takeTurn"
    )
    public static class BanditBearBearHugBoost {
        @SpirePostfixPatch
        public static void Postfix(com.megacrit.cardcrawl.monsters.city.BanditBear __instance) {
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 86) {
                return;
            }

            try {
                // Check if Bear Hug move (byte 2) is being executed
                java.lang.reflect.Field nextMoveField = com.megacrit.cardcrawl.monsters.city.BanditBear.class.getSuperclass().getDeclaredField("nextMove");
                nextMoveField.setAccessible(true);
                byte nextMove = nextMoveField.getByte(__instance);

                if (nextMove == 2) { // BEAR_HUG
                    // Add extra dexterity loss
                    AbstractDungeon.actionManager.addToBottom(
                        new com.megacrit.cardcrawl.actions.common.ApplyPowerAction(
                            AbstractDungeon.player,
                            __instance,
                            new com.megacrit.cardcrawl.powers.DexterityPower(AbstractDungeon.player, -1),
                            -1
                        )
                    );

                    logger.info("Ascension 86: BanditBear Bear Hug applied -1 extra dexterity");
                }
            } catch (Exception e) {
                logger.error("Failed to modify BanditBear Bear Hug", e);
            }
        }
    }

    /**
     * Spiker: Thorns power +1
     */
    @SpirePatch(
        clz = com.megacrit.cardcrawl.monsters.beyond.Spiker.class,
        method = "takeTurn"
    )
    public static class SpikerThornsBoost {
        private static final ThreadLocal<Byte> lastMove = new ThreadLocal<>();

        @com.evacipated.cardcrawl.modthespire.lib.SpirePrefixPatch
        public static void Prefix(com.megacrit.cardcrawl.monsters.beyond.Spiker __instance) {
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 86) {
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
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 86) {
                return;
            }

            Byte move = lastMove.get();
            if (move != null && move == 2) { // BUFF_THORNS move
                // Find and modify the last ApplyPowerAction with ThornsPower
                try {
                    for (int i = AbstractDungeon.actionManager.actions.size() - 1; i >= 0; i--) {
                        com.megacrit.cardcrawl.actions.AbstractGameAction action = AbstractDungeon.actionManager.actions.get(i);

                        if (action instanceof com.megacrit.cardcrawl.actions.common.ApplyPowerAction) {
                            java.lang.reflect.Field powerToApplyField = com.megacrit.cardcrawl.actions.common.ApplyPowerAction.class.getDeclaredField("powerToApply");
                            powerToApplyField.setAccessible(true);
                            com.megacrit.cardcrawl.powers.AbstractPower power = (com.megacrit.cardcrawl.powers.AbstractPower) powerToApplyField.get(action);

                            if (power instanceof com.megacrit.cardcrawl.powers.ThornsPower && power.owner == __instance) {
                                power.amount += 1;

                                java.lang.reflect.Field amountField = com.megacrit.cardcrawl.actions.common.ApplyPowerAction.class.getDeclaredField("amount");
                                amountField.setAccessible(true);
                                int currentAmount = amountField.getInt(action);
                                amountField.setInt(action, currentAmount + 1);

                                logger.info(String.format(
                                    "Ascension 86: Spiker Thorns increased by +1 (total: %d)",
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

    /*
     * 폭탄기(Exploder): 폭발 패턴은 데미지를 60 가합니다
     *
     * NOTE: 이 패치는 Level53.java에 구현되어 있습니다.
     * Level53.ExploderExplosionDamage가 DamageAction 생성자를 패치하여:
     * - Level 53-85: 폭발 데미지 30 → 50
     * - Level 86+: 폭발 데미지 30 → 60
     *
     * ExplosivePower가 생성하는 THORNS 타입 30 데미지를 감지하여 수정합니다.
     */

    /**
     * Maw: Roar weak and frail +1
     */
    @SpirePatch(
        clz = com.megacrit.cardcrawl.monsters.beyond.Maw.class,
        method = "takeTurn"
    )
    public static class MawRoarBoost {
        @SpirePostfixPatch
        public static void Postfix(com.megacrit.cardcrawl.monsters.beyond.Maw __instance) {
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 86) {
                return;
            }

            try {
                // Check if Roar move (byte 3) is being executed
                java.lang.reflect.Field nextMoveField = com.megacrit.cardcrawl.monsters.beyond.Maw.class.getSuperclass().getDeclaredField("nextMove");
                nextMoveField.setAccessible(true);
                byte nextMove = nextMoveField.getByte(__instance);

                if (nextMove == 3) { // ROAR
                    // Add +1 extra Weak and Frail
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

                    logger.info("Ascension 86: Maw Roar applied +1 extra Weak and Frail");
                }
            } catch (Exception e) {
                logger.error("Failed to modify Maw Roar", e);
            }
        }
    }

    // Transient: Fading +1
    // MOVED TO: TransientFadingPatch.java (unified patch)
}
