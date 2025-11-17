package com.stsmod.ascension100.patches.levels;

import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import com.megacrit.cardcrawl.monsters.MonsterGroup;
import com.megacrit.cardcrawl.monsters.exordium.*;
import com.megacrit.cardcrawl.monsters.city.*;
import com.megacrit.cardcrawl.monsters.beyond.*;
import com.stsmod.ascension100.util.EncounterHelper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Ascension Level 45: Enemy composition changes
 *
 * 적의 구성이 일부 변경됩니다.
 *
 * Strong Enemies 전투는 20% 확률로, 다음 적을 추가합니다:
 * - 1막: 그렘린 또는 소형 슬라임들 중 한마리 추가
 * - 2막: 중형 슬라임들 중 한마리 추가
 * - 3막: 뚱뚱한 그렘린, 섀, 폭탄기 중 하나
 */
public class Level45 {
    private static final Logger logger = LogManager.getLogger(Level45.class.getName());

    @SpirePatch(
        clz = MonsterGroup.class,
        method = "init"
    )
    public static class AddExtraMonsterToStrongEncounters {
        @SpirePostfixPatch
        public static void Postfix(MonsterGroup __instance) {
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 45) {
                return;
            }

            // Only apply to non-elite, non-boss encounters
            if (__instance.monsters == null || __instance.monsters.isEmpty()) {
                return;
            }

            // Skip elite and boss encounters
            for (AbstractMonster m : __instance.monsters) {
                if (m != null && (m.type == AbstractMonster.EnemyType.ELITE ||
                                  m.type == AbstractMonster.EnemyType.BOSS)) {
                    return;
                }
            }

            // Check if this is a Strong Enemy encounter
            if (!EncounterHelper.isStrongEncounter()) {
                return;
            }

            int actNum = AbstractDungeon.actNum;
            int roll = AbstractDungeon.monsterRng.random(0, 99);

            // 20% chance to add extra monster
            if (roll < 20) {
                AbstractMonster extraMonster = null;
                float xPos = 200.0f;

                switch (actNum) {
                    case 1:
                        // Act 1: Add Gremlin or Small Slime (random choice)
                        int act1Choice = AbstractDungeon.monsterRng.random(0, 5);
                        switch (act1Choice) {
                            case 0:
                                extraMonster = new GremlinWarrior(xPos, 0.0f);
                                break;
                            case 1:
                                extraMonster = new GremlinThief(xPos, 0.0f);
                                break;
                            case 2:
                                extraMonster = new GremlinFat(xPos, 0.0f);
                                break;
                            case 3:
                                extraMonster = new AcidSlime_S(xPos, 0.0f, 0);
                                break;
                            case 4:
                                extraMonster = new SpikeSlime_S(xPos, 0.0f, 0);
                                break;
                            case 5:
                                extraMonster = new FungiBeast(xPos, 0.0f);
                                break;
                        }
                        logger.info(String.format(
                            "Ascension 45: Adding %s to Act 1 battle (Roll: %d < 20)",
                            extraMonster != null ? extraMonster.name : "unknown", roll
                        ));
                        break;

                    case 2:
                        // Act 2: Add Medium Slime (random choice)
                        int act2Choice = AbstractDungeon.monsterRng.random(0, 2);
                        switch (act2Choice) {
                            case 0:
                                extraMonster = new AcidSlime_M(xPos, 0.0f);
                                break;
                            case 1:
                                extraMonster = new SpikeSlime_M(xPos, 0.0f);
                                break;
                            case 2:
                                extraMonster = new SlaverBlue(xPos, 0.0f);
                                break;
                        }
                        logger.info(String.format(
                            "Ascension 45: Adding %s to Act 2 battle (Roll: %d < 20)",
                            extraMonster != null ? extraMonster.name : "unknown", roll
                        ));
                        break;

                    case 3:
                        // Act 3: Add Fat Gremlin, Shelled Parasite, or Exploder (random choice)
                        int act3Choice = AbstractDungeon.monsterRng.random(0, 2);
                        switch (act3Choice) {
                            case 0:
                                extraMonster = new GremlinFat(xPos, 0.0f);
                                break;
                            case 1:
                                extraMonster = new ShelledParasite(xPos, 0.0f);
                                break;
                            case 2:
                                extraMonster = new Exploder(xPos, 0.0f);
                                break;
                        }
                        logger.info(String.format(
                            "Ascension 45: Adding %s to Act 3 battle (Roll: %d < 20)",
                            extraMonster != null ? extraMonster.name : "unknown", roll
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
