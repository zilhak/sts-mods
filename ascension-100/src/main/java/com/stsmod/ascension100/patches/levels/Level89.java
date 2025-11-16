package com.stsmod.ascension100.patches.levels;

import com.badlogic.gdx.math.MathUtils;
import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import com.megacrit.cardcrawl.rooms.MonsterRoomElite;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Ascension Level 89: Burning elites gain additional HP per act
 *
 * 강화 엘리트가 더욱 강해집니다.
 *
 * 강화 엘리트는 각 막당 체력이 10%/20%/30% 추가로 증가합니다.
 */
public class Level89 {
    private static final Logger logger = LogManager.getLogger(Level89.class.getName());

    @SpirePatch(
        clz = MonsterRoomElite.class,
        method = "applyEmeraldEliteBuff"
    )
    public static class BurningEliteExtraHP {
        @SpirePostfixPatch
        public static void Postfix(MonsterRoomElite __instance) {
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 89) {
                return;
            }

            // Only apply if this is a Burning Elite room
            if (Settings.isFinalActAvailable &&
                AbstractDungeon.getCurrMapNode() != null &&
                AbstractDungeon.getCurrMapNode().hasEmeraldKey) {

                int actNum = AbstractDungeon.actNum;
                float hpBonus = 0.0f;

                // Determine HP bonus based on act
                switch (actNum) {
                    case 1:
                        hpBonus = 0.10f; // 10%
                        break;
                    case 2:
                        hpBonus = 0.20f; // 20%
                        break;
                    case 3:
                    case 4: // Act 4 (Heart)
                        hpBonus = 0.30f; // 30%
                        break;
                }

                if (hpBonus > 0.0f) {
                    for (AbstractMonster m : __instance.monsters.monsters) {
                        int hpIncrease = MathUtils.ceil(m.maxHealth * hpBonus);
                        m.maxHealth += hpIncrease;
                        m.currentHealth += hpIncrease;

                        logger.info(String.format(
                            "Ascension 89: %s gained %d max HP (%.0f%% bonus) in Act %d - Total: %d HP",
                            m.name, hpIncrease, hpBonus * 100, actNum, m.maxHealth
                        ));
                    }
                }
            }
        }
    }
}
