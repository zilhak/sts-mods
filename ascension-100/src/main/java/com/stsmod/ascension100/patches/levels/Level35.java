package com.stsmod.ascension100.patches.levels;

import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePrefixPatch;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import com.megacrit.cardcrawl.monsters.city.Byrd;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.reflect.Field;

/**
 * Ascension Level 35: Normal enemies deal +1 damage (except Byrd)
 *
 * 일반 적들의 공격력이 1 증가합니다 (섀(Byrd) 제외)
 * Byrd는 대신, Headbutt 패턴에서 공격력이 2 증가합니다.
 */
public class Level35 {
    private static final Logger logger = LogManager.getLogger(Level35.class.getName());

    /**
     * Normal enemies (except Byrd) get +1 damage
     */
    @SpirePatch(
        clz = AbstractMonster.class,
        method = "usePreBattleAction"
    )
    public static class NormalDamageIncrease {
        @SpirePostfixPatch
        public static void Postfix(AbstractMonster __instance) {
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 35) {
                return;
            }

            // Exclude Byrd - it has special handling
            if (__instance.type == AbstractMonster.EnemyType.NORMAL && !(__instance instanceof Byrd)) {
                int damageIncrease = 1;

                for (int i = 0; i < __instance.damage.size(); i++) {
                    if (__instance.damage.get(i) != null && __instance.damage.get(i).base > 0) {
                        __instance.damage.get(i).base += damageIncrease;
                    }
                }

                logger.info(String.format(
                    "Ascension 35: Normal %s damage increased by %d",
                    __instance.name, damageIncrease
                ));
            }
        }
    }

    /**
     * Byrd: Headbutt pattern deals +2 damage
     */
    @SpirePatch(
        clz = Byrd.class,
        method = "takeTurn"
    )
    public static class ByrdHeadbuttEnhancement {
        private static final ThreadLocal<Byte> lastMove = new ThreadLocal<>();

        @SpirePrefixPatch
        public static void Prefix(Byrd __instance) {
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 35) {
                return;
            }

            try {
                Field nextMoveField = AbstractMonster.class.getDeclaredField("nextMove");
                nextMoveField.setAccessible(true);
                byte move = nextMoveField.getByte(__instance);
                lastMove.set(move);
            } catch (Exception e) {
                logger.error("Failed to get Byrd move", e);
            }
        }

        @SpirePostfixPatch
        public static void Postfix(Byrd __instance) {
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 35) {
                return;
            }

            try {
                Byte move = lastMove.get();
                if (move != null && move == 2) { // HEADBUTT move
                    // Apply +2 damage to Headbutt pattern
                    if (__instance.damage.size() > 0 && __instance.damage.get(0) != null) {
                        __instance.damage.get(0).base += 2;
                        logger.info("Ascension 35: Byrd Headbutt damage +2");
                    }
                }
            } catch (Exception e) {
                logger.error("Failed to enhance Byrd Headbutt", e);
            } finally {
                lastMove.remove();
            }
        }
    }
}
