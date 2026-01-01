package com.stsmod.ascension100.patches.levels;

import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePrefixPatch;
import com.megacrit.cardcrawl.cards.DamageInfo;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import com.megacrit.cardcrawl.monsters.city.Byrd;
import com.megacrit.cardcrawl.monsters.exordium.SlaverRed;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.reflect.Field;

/**
 * Ascension Level 58: Normal enemies deal +1 damage (except Byrd)
 *
 * 일반 적들의 공격력이 1 증가합니다 (섀(Byrd) 제외)
 * Byrd는 대신, Headbutt 패턴에서 공격력이 2 증가합니다.
 */
public class Level58 {
    private static final Logger logger = LogManager.getLogger(Level58.class.getName());

    /**
     * Normal enemies (except Byrd) get +1 damage
     */
    @SpirePatch(
        clz = AbstractMonster.class,
        method = "init",
        paramtypez = {}
    )
    public static class NormalDamageIncrease {
        @SpirePrefixPatch
        public static void Prefix(AbstractMonster __instance) {
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 58) {
                return;
            }

            // Only apply to NORMAL enemies, exclude Byrd and elite minions
            if (__instance.type == AbstractMonster.EnemyType.NORMAL && !(__instance instanceof Byrd)) {
                // Skip elite minions (they should not get normal enemy bonuses)
                if (__instance.id != null && __instance.id.equals("Dagger")) {
                    return;
                }

                int damageIncrease = 1;

                for (DamageInfo damageInfo : __instance.damage) {
                    if (damageInfo != null && damageInfo.base > 0) {
                        damageInfo.base += damageIncrease;
                        damageInfo.output = damageInfo.base;  // Update output to match base
                    }
                }

                // Special handling for SlaverRed: update stabDmg field for first turn Intent
                if (__instance instanceof SlaverRed) {
                    try {
                        Field stabDmgField = SlaverRed.class.getDeclaredField("stabDmg");
                        stabDmgField.setAccessible(true);
                        int currentStabDmg = stabDmgField.getInt(__instance);
                        stabDmgField.setInt(__instance, currentStabDmg + damageIncrease);
                        logger.info(String.format(
                            "Ascension 58: SlaverRed stabDmg field updated from %d to %d",
                            currentStabDmg, currentStabDmg + damageIncrease
                        ));
                    } catch (Exception e) {
                        logger.error("Failed to update SlaverRed stabDmg field", e);
                    }
                }

                // Special handling for GremlinThief: update thiefDamage field for SetMoveAction
                if (__instance.id != null && __instance.id.equals("GremlinThief")) {
                    try {
                        Field thiefDamageField = __instance.getClass().getDeclaredField("thiefDamage");
                        thiefDamageField.setAccessible(true);
                        int currentThiefDamage = thiefDamageField.getInt(__instance);
                        thiefDamageField.setInt(__instance, currentThiefDamage + damageIncrease);
                        logger.info(String.format(
                            "Ascension 58: GremlinThief thiefDamage field updated from %d to %d",
                            currentThiefDamage, currentThiefDamage + damageIncrease
                        ));
                    } catch (Exception e) {
                        logger.error("Failed to update GremlinThief thiefDamage field", e);
                    }
                }

                logger.info(String.format(
                    "Ascension 58: Normal %s damage increased by %d [init prefix]",
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
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 58) {
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
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 58) {
                return;
            }

            try {
                Byte move = lastMove.get();
                if (move != null && move == 5) { // HEADBUTT move (pattern 5)
                    // Apply +2 damage to Headbutt pattern (damage index 2)
                    if (__instance.damage.size() > 2 && __instance.damage.get(2) != null) {
                        __instance.damage.get(2).base += 2;
                        logger.info("Ascension 58: Byrd Headbutt damage +2");
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
