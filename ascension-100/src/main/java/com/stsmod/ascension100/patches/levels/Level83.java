package com.stsmod.ascension100.patches.levels;

import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;
import com.megacrit.cardcrawl.actions.common.GainBlockAction;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Ascension Level 83: Enemy defensive patterns are strengthened
 *
 * 적들의 방어적인 패턴이 강화됩니다.
 *
 * 방어도를 얻는 적들의 패턴들의 방어도 수치가 10% 증가합니다.
 */
public class Level83 {
    private static final Logger logger = LogManager.getLogger(Level83.class.getName());

    /**
     * Patch all GainBlockAction constructors to increase block amount by 10% for monsters
     */
    @SpirePatch(
        clz = GainBlockAction.class,
        method = SpirePatch.CONSTRUCTOR,
        paramtypez = {AbstractCreature.class, int.class}
    )
    public static class IncreaseMonsterBlockAmount1 {
        @SpirePostfixPatch
        public static void Postfix(GainBlockAction __instance, AbstractCreature target, int amount) {
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 83) {
                return;
            }

            // Only increase block for monsters
            if (target instanceof AbstractMonster) {
                int originalAmount = __instance.amount;
                __instance.amount = (int) Math.ceil(originalAmount * 1.10f);

                logger.info(String.format(
                    "Ascension 83: Increased %s's block from %d to %d (+10%%)",
                    ((AbstractMonster) target).name,
                    originalAmount,
                    __instance.amount
                ));
            }
        }
    }

    @SpirePatch(
        clz = GainBlockAction.class,
        method = SpirePatch.CONSTRUCTOR,
        paramtypez = {AbstractCreature.class, AbstractCreature.class, int.class}
    )
    public static class IncreaseMonsterBlockAmount2 {
        @SpirePostfixPatch
        public static void Postfix(GainBlockAction __instance, AbstractCreature target, AbstractCreature source, int amount) {
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 83) {
                return;
            }

            // Only increase block for monsters
            if (target instanceof AbstractMonster) {
                int originalAmount = __instance.amount;
                __instance.amount = (int) Math.ceil(originalAmount * 1.10f);

                logger.info(String.format(
                    "Ascension 83: Increased %s's block from %d to %d (+10%%)",
                    ((AbstractMonster) target).name,
                    originalAmount,
                    __instance.amount
                ));
            }
        }
    }

    @SpirePatch(
        clz = GainBlockAction.class,
        method = SpirePatch.CONSTRUCTOR,
        paramtypez = {AbstractCreature.class, int.class, boolean.class}
    )
    public static class IncreaseMonsterBlockAmount3 {
        @SpirePostfixPatch
        public static void Postfix(GainBlockAction __instance, AbstractCreature target, int amount, boolean superFast) {
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 83) {
                return;
            }

            // Only increase block for monsters
            if (target instanceof AbstractMonster) {
                int originalAmount = __instance.amount;
                __instance.amount = (int) Math.ceil(originalAmount * 1.10f);

                logger.info(String.format(
                    "Ascension 83: Increased %s's block from %d to %d (+10%%)",
                    ((AbstractMonster) target).name,
                    originalAmount,
                    __instance.amount
                ));
            }
        }
    }

    @SpirePatch(
        clz = GainBlockAction.class,
        method = SpirePatch.CONSTRUCTOR,
        paramtypez = {AbstractCreature.class, AbstractCreature.class, int.class, boolean.class}
    )
    public static class IncreaseMonsterBlockAmount4 {
        @SpirePostfixPatch
        public static void Postfix(GainBlockAction __instance, AbstractCreature target, AbstractCreature source, int amount, boolean superFast) {
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 83) {
                return;
            }

            // Only increase block for monsters
            if (target instanceof AbstractMonster) {
                int originalAmount = __instance.amount;
                __instance.amount = (int) Math.ceil(originalAmount * 1.10f);

                logger.info(String.format(
                    "Ascension 83: Increased %s's block from %d to %d (+10%%)",
                    ((AbstractMonster) target).name,
                    originalAmount,
                    __instance.amount
                ));
            }
        }
    }
}
