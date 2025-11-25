package com.stsmod.ascension100.patches.levels;

import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.map.MapRoomNode;
import com.megacrit.cardcrawl.rooms.EventRoom;
import com.megacrit.cardcrawl.rooms.MonsterRoomElite;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.reflect.Field;
import java.util.ArrayList;

/**
 * Ascension Level 41: More elites appear
 * 엘리트가 더 많이 발생합니다.
 *
 * 지도 생성시 ?지역은 10% 확률로 엘리트 몬스터로 변경됩니다.
 * When the map is generated, ? rooms have a 10% chance to become Elite rooms.
 */
public class Level41 {
    private static final Logger logger = LogManager.getLogger(Level41.class.getName());
    private static final float EVENT_TO_ELITE_CHANCE = 0.10f; // 10% chance

    /**
     * After map generation, convert some EventRooms to MonsterRoomElite
     */
    @SpirePatch(
        clz = AbstractDungeon.class,
        method = "generateMap"
    )
    public static class ConvertEventRoomsToElite {
        @SpirePostfixPatch
        public static void Postfix() {
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 41) {
                return;
            }

            try {
                // Access the map field via reflection
                Field mapField = AbstractDungeon.class.getDeclaredField("map");
                mapField.setAccessible(true);
                @SuppressWarnings("unchecked")
                ArrayList<ArrayList<MapRoomNode>> map = (ArrayList<ArrayList<MapRoomNode>>) mapField.get(null);

                if (map == null) {
                    logger.warn("Ascension 41: Map is null, cannot convert EventRooms");
                    return;
                }

                int totalEventRooms = 0;
                int convertedRooms = 0;

                // Iterate through all nodes in the map
                for (ArrayList<MapRoomNode> row : map) {
                    for (MapRoomNode node : row) {
                        // Check if this node is an EventRoom
                        if (node != null && node.room instanceof EventRoom) {
                            totalEventRooms++;

                            // 10% chance to convert to Elite room
                            // Use mapRng for consistency with map generation
                            if (AbstractDungeon.mapRng.randomBoolean(EVENT_TO_ELITE_CHANCE)) {
                                // Replace EventRoom with MonsterRoomElite
                                node.room = new MonsterRoomElite();
                                convertedRooms++;

                                logger.info(String.format(
                                    "Ascension 41: Converted EventRoom to Elite at position (x=%d, y=%d)",
                                    node.x, node.y
                                ));
                            }
                        }
                    }
                }

                logger.info(String.format(
                    "Ascension 41: Map generation complete - Converted %d/%d EventRooms to Elite (%.1f%%)",
                    convertedRooms, totalEventRooms, (totalEventRooms > 0 ? (convertedRooms * 100.0 / totalEventRooms) : 0.0)
                ));

            } catch (NoSuchFieldException e) {
                logger.error("Ascension 41: Could not find 'map' field in AbstractDungeon", e);
            } catch (IllegalAccessException e) {
                logger.error("Ascension 41: Could not access 'map' field in AbstractDungeon", e);
            } catch (Exception e) {
                logger.error("Ascension 41: Unexpected error during EventRoom to Elite conversion", e);
            }
        }
    }
}
