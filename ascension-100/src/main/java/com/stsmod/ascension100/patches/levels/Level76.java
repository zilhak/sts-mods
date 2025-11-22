package com.stsmod.ascension100.patches.levels;

import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;
import com.megacrit.cardcrawl.actions.common.ApplyPowerAction;
import com.megacrit.cardcrawl.actions.common.GainBlockAction;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import com.megacrit.cardcrawl.monsters.MonsterGroup;
import com.megacrit.cardcrawl.powers.PlatedArmorPower;
import com.megacrit.cardcrawl.powers.RegenerateMonsterPower;
import com.megacrit.cardcrawl.relics.AbstractRelic;
import com.megacrit.cardcrawl.rooms.AbstractRoom;
import com.megacrit.cardcrawl.rooms.MonsterRoom;
import com.megacrit.cardcrawl.rooms.MonsterRoomElite;
import com.stsmod.ascension100.util.EncounterHelper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Ascension Level 76: Special battles occur
 *
 * 특수전투가 발생합니다.
 *
 * 지도에서 적 인카운터 심볼의 Strong Enemy 전투에서, 15% 확률로 특수 전투가 발생할 수 있습니다.
 * 특수 전투는 엘리트 전투와 동일한 보상을 얻습니다.
 * 특수 전투는 시작시 적들이 버프와 함께, 시작시 방어도를 얻은 상태에서 시작합니다.
 * 특수 전투시에는 일반 BGM 대신 엘리트 BGM(BOSS_BOTTOM)을 재생합니다.
 *
 * 1막 : 적들은 금속화를 2, 재생을 2얻습니다. 시작시 방어도를 6 얻습니다.
 * 2막 : 적들은 금속화를 5, 재생을 3얻습니다. 시작시 방어도를 10 얻습니다.
 * 3막 : 적들은 금속화를 8, 재생을 5얻습니다. 시작시 방어도를 25 얻습니다.
 */
public class Level76 {
    private static final Logger logger = LogManager.getLogger(Level76.class.getName());

    /**
     * Special battle tracker
     */
    public static class SpecialBattleTracker {
        public static boolean isSpecialBattle = false;
        public static int specialBattleChance = 15; // Can be modified by Level 78
        public static int lastSpecialBattleFloor = -999; // Last floor where special battle occurred
        public static final int SPECIAL_BATTLE_COOLDOWN = 10; // Floors between special battles
    }

    /**
     * Determine if current battle should be a special battle
     * Check AFTER monster group is initialized to ensure lastCombatMetricKey is set
     */
    @SpirePatch(
        clz = MonsterGroup.class,
        method = "init"
    )
    public static class TriggerSpecialBattle {
        @SpirePostfixPatch
        public static void Postfix(MonsterGroup __instance) {
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 76) {
                return;
            }

            // Only trigger in MonsterRoom (not elite/boss rooms)
            if (!(AbstractDungeon.getCurrRoom() instanceof MonsterRoom) ||
                AbstractDungeon.getCurrRoom() instanceof MonsterRoomElite) {
                return;
            }

            // Check if this is a Strong Enemy encounter
            // lastCombatMetricKey is now properly set after init()
            if (!EncounterHelper.isStrongEncounter()) {
                logger.info(String.format("Ascension 76: Not a strong encounter (%s), skipping special battle check",
                    AbstractDungeon.lastCombatMetricKey));
                return;
            }

            // Special battles cannot occur before floor 10
            int currentFloor = AbstractDungeon.floorNum;
            if (currentFloor < 10) {
                logger.info(String.format("Ascension 76: Special Battle cannot occur before floor 10 (Current floor: %d)",
                    currentFloor));
                return;
            }

            // Check cooldown - special battles cannot occur within 10 floors of each other
            int floorsSinceLastSpecial = currentFloor - SpecialBattleTracker.lastSpecialBattleFloor;

            if (floorsSinceLastSpecial < SpecialBattleTracker.SPECIAL_BATTLE_COOLDOWN) {
                logger.info(String.format("Ascension 76: Special Battle on cooldown (Floor %d, last was %d, need %d floors apart)",
                    currentFloor, SpecialBattleTracker.lastSpecialBattleFloor, SpecialBattleTracker.SPECIAL_BATTLE_COOLDOWN));
                return;
            }

            // Roll for special battle
            int roll = AbstractDungeon.miscRng.random(0, 99);
            if (roll < SpecialBattleTracker.specialBattleChance) {
                SpecialBattleTracker.isSpecialBattle = true;
                SpecialBattleTracker.lastSpecialBattleFloor = currentFloor;
                logger.info(String.format("Ascension 76: Special Battle triggered! (Floor: %d, Roll: %d < %d, Encounter: %s)",
                    currentFloor, roll, SpecialBattleTracker.specialBattleChance, AbstractDungeon.lastCombatMetricKey));
            } else {
                logger.info(String.format("Ascension 76: Special Battle NOT triggered (Floor: %d, Roll: %d >= %d, Encounter: %s)",
                    currentFloor, roll, SpecialBattleTracker.specialBattleChance, AbstractDungeon.lastCombatMetricKey));
            }
        }
    }

    /**
     * Play elite BGM for special battles
     * Intercept playBGM call to change BGM key
     */
    @SpirePatch(
        clz = com.megacrit.cardcrawl.rooms.AbstractRoom.class,
        method = "playBGM",
        paramtypez = {String.class}
    )
    public static class PlayEliteBGM {
        @com.evacipated.cardcrawl.modthespire.lib.SpirePrefixPatch
        public static void Prefix(com.megacrit.cardcrawl.rooms.AbstractRoom __instance, @com.evacipated.cardcrawl.modthespire.lib.ByRef String[] key) {
            // Only apply in special battles
            if (!SpecialBattleTracker.isSpecialBattle) {
                return;
            }

            // Only apply in MonsterRoom (not elite/boss rooms)
            if (!(__instance instanceof MonsterRoom) ||
                __instance instanceof MonsterRoomElite) {
                return;
            }

            // Change BGM key to elite BGM (ELITE - STS_EliteBoss_NewMix_v1.ogg)
            key[0] = "ELITE";
            logger.info("Ascension 76: Changed BGM to ELITE for Special Battle");
        }
    }

    /**
     * Apply buffs to monsters at battle start
     */
    @SpirePatch(
        clz = MonsterGroup.class,
        method = "usePreBattleAction"
    )
    public static class ApplySpecialBattleBuffs {
        @SpirePostfixPatch
        public static void Postfix(MonsterGroup __instance) {
            if (!SpecialBattleTracker.isSpecialBattle) {
                return;
            }

            int actNum = AbstractDungeon.actNum;
            int plating, regen, block;

            // Determine buff amounts by act
            switch (actNum) {
                case 1:
                    plating = 2;
                    regen = 2;
                    block = 6;
                    break;
                case 2:
                    plating = 5;
                    regen = 3;
                    block = 10;
                    break;
                case 3:
                default:
                    plating = 8;
                    regen = 5;
                    block = 25;
                    break;
            }

            // Apply buffs to all monsters
            for (AbstractMonster m : __instance.monsters) {
                if (m != null && !m.isDying && !m.isDead) {
                    // Apply Plated Armor (금속화)
                    AbstractDungeon.actionManager.addToBottom(
                        new ApplyPowerAction(
                            (AbstractCreature)m,
                            (AbstractCreature)m,
                            new PlatedArmorPower((AbstractCreature)m, plating),
                            plating
                        )
                    );

                    // Apply Regeneration (재생)
                    AbstractDungeon.actionManager.addToBottom(
                        new ApplyPowerAction(
                            (AbstractCreature)m,
                            (AbstractCreature)m,
                            new RegenerateMonsterPower(m, regen),
                            regen
                        )
                    );

                    // Apply starting block (시작시 방어도)
                    AbstractDungeon.actionManager.addToBottom(
                        new GainBlockAction(m, m, block)
                    );
                }
            }

            logger.info(String.format("Ascension 76: Applied Special Battle buffs - Plating: %d, Regen: %d, Block: %d (Act %d)",
                plating, regen, block, actNum));
        }
    }

    /**
     * Add elite-tier relic reward to special battles
     */
    @SpirePatch(
        clz = MonsterRoom.class,
        method = "dropReward"
    )
    public static class AddEliteRelicReward {
        @SpirePostfixPatch
        public static void Postfix(MonsterRoom __instance) {
            if (!SpecialBattleTracker.isSpecialBattle) {
                return;
            }

            // Add relic reward like elite battles
            AbstractRelic.RelicTier tier = AbstractDungeon.returnRandomRelicTier();
            __instance.addRelicToRewards(tier);

            logger.info(String.format("Ascension 76: Added relic reward (%s tier) to Special Battle", tier));
        }
    }

    /**
     * Reset special battle cooldown when starting a new game
     */
    @SpirePatch(
        clz = com.megacrit.cardcrawl.dungeons.AbstractDungeon.class,
        method = "generateMap"
    )
    public static class ResetCooldownOnNewGame {
        @SpirePostfixPatch
        public static void Postfix() {
            // Reset cooldown at start of each act/run
            SpecialBattleTracker.lastSpecialBattleFloor = -999;
            logger.info("Ascension 76: Reset special battle cooldown for new game/act");
        }
    }

    /**
     * Reset special battle flag after combat ends
     */
    @SpirePatch(
        clz = AbstractRoom.class,
        method = "endBattle"
    )
    public static class ResetSpecialBattleFlag {
        @SpirePostfixPatch
        public static void Postfix(AbstractRoom __instance) {
            if (SpecialBattleTracker.isSpecialBattle) {
                logger.info("Ascension 76: Resetting Special Battle flag");
                SpecialBattleTracker.isSpecialBattle = false;
            }
        }
    }
}
