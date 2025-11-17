package com.stsmod.ascension100.patches.levels;

import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePrefixPatch;
import com.evacipated.cardcrawl.modthespire.lib.SpireReturn;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.dungeons.TheCity;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import com.megacrit.cardcrawl.monsters.MonsterGroup;
import com.megacrit.cardcrawl.monsters.MonsterInfo;
import com.megacrit.cardcrawl.monsters.city.Chosen;
import com.megacrit.cardcrawl.monsters.city.Mugger;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;

/**
 * Ascension Level 75: Battle composition changes
 *
 * 일부 전투구성이 변경됩니다.
 *
 * 2막의 strong enemies 전투 구성에 강도(Mugger) + 선택받은자(Chosen) 를 추가합니다.
 */
public class Level75 {
    private static final Logger logger = LogManager.getLogger(Level75.class.getName());
    public static final String ENCOUNTER_NAME = "Mugger and Chosen";

    /**
     * Patch TheCity's generateStrongEnemies to add Mugger + Chosen encounter
     */
    @SpirePatch(
        clz = TheCity.class,
        method = "generateStrongEnemies"
    )
    public static class AddMuggerChosenEncounter {
        @SpirePrefixPatch
        public static SpireReturn<Void> Prefix(TheCity __instance, int count) {
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 75) {
                return SpireReturn.Continue();
            }

            // Call original logic first by using reflection
            try {
                ArrayList<MonsterInfo> monsters = new ArrayList<>();
                monsters.add(new MonsterInfo("Chosen and Byrds", 2.0F));
                monsters.add(new MonsterInfo("Sentry and Sphere", 2.0F));
                monsters.add(new MonsterInfo("Snake Plant", 6.0F));
                monsters.add(new MonsterInfo("Snecko", 4.0F));
                monsters.add(new MonsterInfo("Centurion and Healer", 6.0F));
                monsters.add(new MonsterInfo("Cultist and Chosen", 3.0F));
                monsters.add(new MonsterInfo("3 Cultists", 3.0F));
                monsters.add(new MonsterInfo("Shelled Parasite and Fungi", 3.0F));

                // Add new Mugger and Chosen encounter (Ascension 75+)
                monsters.add(new MonsterInfo(ENCOUNTER_NAME, 3.0F));

                MonsterInfo.normalizeWeights(monsters);

                // Use reflection to call protected methods
                java.lang.reflect.Method populateFirstStrongEnemy = TheCity.class.getDeclaredMethod(
                    "populateFirstStrongEnemy", ArrayList.class, ArrayList.class
                );
                populateFirstStrongEnemy.setAccessible(true);

                java.lang.reflect.Method generateExclusions = TheCity.class.getDeclaredMethod("generateExclusions");
                generateExclusions.setAccessible(true);
                ArrayList<String> exclusions = (ArrayList<String>) generateExclusions.invoke(__instance);

                populateFirstStrongEnemy.invoke(__instance, monsters, exclusions);

                java.lang.reflect.Method populateMonsterList = AbstractDungeon.class.getDeclaredMethod(
                    "populateMonsterList", ArrayList.class, int.class, boolean.class
                );
                populateMonsterList.setAccessible(true);
                populateMonsterList.invoke(__instance, monsters, count, false);

                logger.info("Ascension 75: Added 'Mugger and Chosen' encounter to TheCity strong enemies");

                return SpireReturn.Return(null);
            } catch (Exception e) {
                logger.error("Failed to add Mugger and Chosen encounter", e);
                return SpireReturn.Continue();
            }
        }
    }

    /**
     * Patch MonsterHelper.getEncounter to handle "Mugger and Chosen"
     */
    @SpirePatch(
        clz = com.megacrit.cardcrawl.helpers.MonsterHelper.class,
        method = "getEncounter"
    )
    public static class GetMuggerChosenEncounter {
        @SpirePrefixPatch
        public static SpireReturn<MonsterGroup> Prefix(String key) {
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 75) {
                return SpireReturn.Continue();
            }

            if (ENCOUNTER_NAME.equals(key)) {
                logger.info("Ascension 75: Creating Mugger and Chosen encounter");
                return SpireReturn.Return(
                    new MonsterGroup(new AbstractMonster[] {
                        new Mugger(-200.0F, 0.0F),
                        new Chosen(120.0F, 0.0F)
                    })
                );
            }

            return SpireReturn.Continue();
        }
    }

    /**
     * Patch MonsterHelper.getEncounterName to handle "Mugger and Chosen"
     */
    @SpirePatch(
        clz = com.megacrit.cardcrawl.helpers.MonsterHelper.class,
        method = "getEncounterName"
    )
    public static class GetMuggerChosenEncounterName {
        @SpirePrefixPatch
        public static SpireReturn<String> Prefix(String key) {
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 75) {
                return SpireReturn.Continue();
            }

            if (ENCOUNTER_NAME.equals(key)) {
                // Return a display name for the encounter
                return SpireReturn.Return("Mugger and Chosen");
            }

            return SpireReturn.Continue();
        }
    }

}
