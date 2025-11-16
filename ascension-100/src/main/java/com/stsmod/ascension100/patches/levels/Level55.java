package com.stsmod.ascension100.patches.levels;

import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePrefixPatch;
import com.evacipated.cardcrawl.modthespire.lib.SpireReturn;
import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.actions.common.ApplyPowerAction;
// import com.megacrit.cardcrawl.actions.common.IncreaseMaxHpAction; // TODO: This class doesn't exist
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import com.megacrit.cardcrawl.powers.MetallicizePower;
import com.megacrit.cardcrawl.powers.RegenerateMonsterPower;
import com.megacrit.cardcrawl.powers.StrengthPower;
import com.megacrit.cardcrawl.rooms.MonsterRoomElite;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Ascension Level 55: Burning elites become stronger
 * 강화 엘리트가 더욱 강해집니다.
 *
 * 강화 엘리트의 무작위 효과는 다음과 같이 변경됩니다:
 * - 최대 체력: 최대 체력 추가로 15% 증가 (25% → 40%)
 * - 힘: 힘이 추가로 2 증가
 * - 금속화: 금속화 수치 추가로 4 증가
 * - 재생: 재생 수치 추가로 3 증가
 */
public class Level55 {
    private static final Logger logger = LogManager.getLogger(Level55.class.getName());

    /**
     * Patch Burning Elite buff application to enhance the bonuses
     */
    @SpirePatch(
        clz = MonsterRoomElite.class,
        method = "applyEmeraldEliteBuff"
    )
    public static class EnhancedBurningElite {
        @SpirePrefixPatch
        public static SpireReturn<Void> Prefix(MonsterRoomElite __instance) {
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 55) {
                return SpireReturn.Continue();
            }

            // Only apply if this is a Burning Elite room
            if (Settings.isFinalActAvailable &&
                AbstractDungeon.getCurrMapNode().hasEmeraldKey) {

                int randomBuff = AbstractDungeon.mapRng.random(0, 3);
                int actNum = AbstractDungeon.actNum;

                logger.info(String.format(
                    "[Asc55] Applying enhanced Burning Elite buff %d in Act %d",
                    randomBuff, actNum
                ));

                for (AbstractMonster m : __instance.monsters.monsters) {
                    switch (randomBuff) {
                        case 0:
                            // Strength: Base + 2 additional
                            int baseStr = actNum + 1;
                            int enhancedStr = baseStr + 2;
                            AbstractDungeon.actionManager.addToBottom(
                                new ApplyPowerAction(m, m,
                                    new StrengthPower(m, enhancedStr), enhancedStr)
                            );
                            logger.info(String.format(
                                "[Asc55] %s gained Strength %d (base: %d, +2 bonus)",
                                m.name, enhancedStr, baseStr
                            ));
                            break;

                        case 1:
                            // Max HP: 25% → 40% (15% additional)
                            // TODO: IncreaseMaxHpAction doesn't exist in base game
                            // Need to find proper way to increase max HP
                            int hpIncrease = (int)(m.maxHealth * 0.40F);
                            m.maxHealth += hpIncrease;
                            m.currentHealth += hpIncrease;
                            logger.info(String.format(
                                "[Asc55] %s max HP increased by %d (40%% increase)",
                                m.name, hpIncrease
                            ));
                            break;

                        case 2:
                            // Metallicize: Base + 4 additional
                            int baseMetallicize = actNum * 2 + 2;
                            int enhancedMetallicize = baseMetallicize + 4;
                            AbstractDungeon.actionManager.addToBottom(
                                new ApplyPowerAction(m, m,
                                    new MetallicizePower(m, enhancedMetallicize),
                                    enhancedMetallicize)
                            );
                            logger.info(String.format(
                                "[Asc55] %s gained Metallicize %d (base: %d, +4 bonus)",
                                m.name, enhancedMetallicize, baseMetallicize
                            ));
                            break;

                        case 3:
                            // Regeneration: Base + 3 additional
                            int baseRegen = 1 + actNum * 2;
                            int enhancedRegen = baseRegen + 3;
                            AbstractDungeon.actionManager.addToBottom(
                                new ApplyPowerAction(m, m,
                                    new RegenerateMonsterPower(m, enhancedRegen),
                                    enhancedRegen)
                            );
                            logger.info(String.format(
                                "[Asc55] %s gained Regeneration %d (base: %d, +3 bonus)",
                                m.name, enhancedRegen, baseRegen
                            ));
                            break;
                    }
                }

                // Return to prevent original method from running
                return SpireReturn.Return(null);
            }

            return SpireReturn.Continue();
        }
    }
}
