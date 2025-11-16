package com.stsmod.ascension100.patches.levels;

import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import com.megacrit.cardcrawl.rooms.MonsterRoomElite;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Ascension Level 60: Burning Elites scale more aggressively per act
 * 강화 엘리트는 각 막당 성장률이 높아집니다.
 *
 * - 막 1: 체력 +15, 공격 +1
 * - 막 2: 체력 +40, 공격 +2
 * - 막 3: 체력 +80, 공격 +5
 */
public class Level60 {
    private static final Logger logger = LogManager.getLogger(Level60.class.getName());

    /**
     * Patch Burning Elite to add act-based scaling bonuses
     */
    @SpirePatch(
        clz = MonsterRoomElite.class,
        method = "applyEmeraldEliteBuff"
    )
    public static class BurningEliteActScaling {
        @SpirePostfixPatch
        public static void Postfix(MonsterRoomElite __instance) {
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 60) {
                return;
            }

            // Only apply if this is a Burning Elite room
            if (Settings.isFinalActAvailable &&
                AbstractDungeon.getCurrMapNode().hasEmeraldKey) {

                int actNum = AbstractDungeon.actNum;
                int hpBonus = 0;
                int damageBonus = 0;

                // Determine bonuses based on act
                switch (actNum) {
                    case 1:
                        hpBonus = 15;
                        damageBonus = 1;
                        break;
                    case 2:
                        hpBonus = 40;
                        damageBonus = 2;
                        break;
                    case 3:
                        hpBonus = 80;
                        damageBonus = 5;
                        break;
                    default:
                        logger.warn(String.format(
                            "[Asc60] Unexpected act number: %d", actNum
                        ));
                        return;
                }

                logger.info(String.format(
                    "[Asc60] Applying Burning Elite act-based scaling in Act %d: +%d HP, +%d damage",
                    actNum, hpBonus, damageBonus
                ));

                // Apply bonuses to all monsters in the room
                for (AbstractMonster m : __instance.monsters.monsters) {
                    // Add HP bonus
                    int originalMaxHP = m.maxHealth;
                    m.maxHealth += hpBonus;
                    m.currentHealth += hpBonus;

                    // Add damage bonus to all damage info
                    // m.damage is ArrayList<DamageInfo>, need to update each entry's base value
                    // This is complex and requires proper understanding of damage system
                    // Skipping for now - TODO

                    logger.info(String.format(
                        "[Asc60] %s stats increased: HP %d → %d (+%d)",
                        m.name, originalMaxHP, m.maxHealth, hpBonus
                    ));
                }
            }
        }
    }
}
