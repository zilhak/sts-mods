package com.stsmod.ascension100.patches.levels;

import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import com.megacrit.cardcrawl.monsters.MonsterGroup;
import com.megacrit.cardcrawl.monsters.exordium.GremlinFat;
import com.megacrit.cardcrawl.monsters.exordium.Looter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Ascension Level 90: Additional enemy composition
 *
 * 적의 구성이 추가됩니다.
 *
 * 2막에서 10% 확률로 뚱뚱한 그렘린이 전투에 추가될 수 있습니다.
 * 3막에서 10% 확률로 도적이 전투에 추가될 수 있습니다.
 */
public class Level90 {
    private static final Logger logger = LogManager.getLogger(Level90.class.getName());

    @SpirePatch(
        clz = MonsterGroup.class,
        method = "init"
    )
    public static class AddExtraMonster {
        @SpirePostfixPatch
        public static void Postfix(MonsterGroup __instance) {
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 90) {
                return;
            }

            int actNum = AbstractDungeon.actNum;
            int roll = AbstractDungeon.monsterRng.random(0, 99);

            // 10% chance to add extra monster
            if (roll < 10) {
                AbstractMonster extraMonster = null;
                float xPos = 0.0f;

                switch (actNum) {
                    case 2:
                        // Add Fat Gremlin in Act 2
                        // Position: Place to the right of existing monsters
                        xPos = 200.0f;
                        extraMonster = new GremlinFat(xPos, 0.0f);
                        logger.info(String.format(
                            "Ascension 90: Adding Fat Gremlin to Act 2 battle (Roll: %d < 10)",
                            roll
                        ));
                        break;

                    case 3:
                        // Add Looter in Act 3
                        // Position: Place to the right of existing monsters
                        xPos = 200.0f;
                        extraMonster = new Looter(xPos, 0.0f);
                        logger.info(String.format(
                            "Ascension 90: Adding Looter to Act 3 battle (Roll: %d < 10)",
                            roll
                        ));
                        break;
                }

                if (extraMonster != null) {
                    __instance.addMonster(extraMonster);
                    extraMonster.init();
                    extraMonster.applyPowers();
                }
            }
        }
    }
}
