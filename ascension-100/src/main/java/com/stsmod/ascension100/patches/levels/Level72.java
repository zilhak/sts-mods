package com.stsmod.ascension100.patches.levels;

import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePrefixPatch;
import com.evacipated.cardcrawl.modthespire.lib.SpireReturn;
import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.actions.animations.AnimateSlowAttackAction;
import com.megacrit.cardcrawl.actions.common.DamageAction;
import com.megacrit.cardcrawl.actions.common.HealAction;
import com.megacrit.cardcrawl.actions.common.RollMoveAction;
import com.megacrit.cardcrawl.cards.DamageInfo;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import com.megacrit.cardcrawl.monsters.city.Healer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Ascension Level 72: Some enemy patterns are added
 *
 * 일부 적의 패턴이 추가됩니다.
 *
 * 신비주의자(Mystic/Healer) 는 백부장(Centurion)이 없는경우 11로 공격 및 자신의 체력을 5 회복하는 "흡혈 공격" 패턴을 사용합니다.
 */
public class Level72 {
    private static final Logger logger = LogManager.getLogger(Level72.class.getName());
    private static final byte VAMPIRE_ATTACK = 4;  // New move byte for vampire attack
    private static final int VAMPIRE_DAMAGE = 11;
    private static final int VAMPIRE_HEAL = 5;

    /**
     * Patch Healer's getMove to use Vampire Attack when alone (no Centurion)
     */
    @SpirePatch(
        clz = Healer.class,
        method = "getMove"
    )
    public static class HealerVampireGetMove {
        @SpirePrefixPatch
        public static SpireReturn<Void> Prefix(Healer __instance, int num) {
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 72) {
                return SpireReturn.Continue();
            }

            // Check if Centurion is present in the battle
            boolean hasCenturion = false;
            for (AbstractMonster m : AbstractDungeon.getMonsters().monsters) {
                if (!m.isDying && !m.isEscaping && m.id.equals("Centurion")) {
                    hasCenturion = true;
                    break;
                }
            }

            // If no Centurion, use Vampire Attack pattern
            if (!hasCenturion) {
                __instance.setMove(
                    VAMPIRE_ATTACK,
                    AbstractMonster.Intent.ATTACK_BUFF,
                    VAMPIRE_DAMAGE
                );
                logger.info("Ascension 72: Healer using Vampire Attack (no Centurion present)");
                return SpireReturn.Return(null);  // Skip original getMove logic
            }

            return SpireReturn.Continue();  // Continue with original logic
        }
    }

    /**
     * Patch Healer's takeTurn to execute Vampire Attack
     */
    @SpirePatch(
        clz = Healer.class,
        method = "takeTurn"
    )
    public static class HealerVampireTakeTurn {
        @SpirePrefixPatch
        public static SpireReturn<Void> Prefix(Healer __instance) {
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 72) {
                return SpireReturn.Continue();
            }

            // Check if this is Vampire Attack move
            if (__instance.nextMove == VAMPIRE_ATTACK) {
                // Animate attack
                AbstractDungeon.actionManager.addToBottom(
                    new AnimateSlowAttackAction((AbstractCreature)__instance)
                );

                // Deal damage
                DamageInfo vampireDamage = new DamageInfo(
                    (AbstractCreature)__instance,
                    VAMPIRE_DAMAGE,
                    DamageInfo.DamageType.NORMAL
                );
                AbstractDungeon.actionManager.addToBottom(
                    new DamageAction(
                        (AbstractCreature)AbstractDungeon.player,
                        vampireDamage,
                        AbstractGameAction.AttackEffect.SLASH_DIAGONAL
                    )
                );

                // Heal self
                AbstractDungeon.actionManager.addToBottom(
                    new HealAction(
                        (AbstractCreature)__instance,
                        (AbstractCreature)__instance,
                        VAMPIRE_HEAL
                    )
                );

                // Roll next move
                AbstractDungeon.actionManager.addToBottom(
                    new RollMoveAction(__instance)
                );

                logger.info(String.format(
                    "Ascension 72: Healer executed Vampire Attack (%d damage, heal %d HP)",
                    VAMPIRE_DAMAGE, VAMPIRE_HEAL
                ));

                // Skip original takeTurn logic
                return SpireReturn.Return(null);
            }

            return SpireReturn.Continue();
        }
    }
}
