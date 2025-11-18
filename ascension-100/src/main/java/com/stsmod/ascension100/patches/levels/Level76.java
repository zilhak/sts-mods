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
    }

    /**
     * Determine if current battle should be a special battle
     */
    @SpirePatch(
        clz = MonsterRoom.class,
        method = "onPlayerEntry"
    )
    public static class TriggerSpecialBattle {
        @SpirePostfixPatch
        public static void Postfix(MonsterRoom __instance) {
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 76) {
                return;
            }

            // Only trigger in Strong Enemy battles (floor 3+)
            // Exclude elite rooms
            if (__instance instanceof MonsterRoomElite) {
                return;
            }

            // Check if this is a Strong Enemy encounter
            if (!EncounterHelper.isStrongEncounter()) {
                return;
            }

            if (AbstractDungeon.getCurrMapNode() != null && AbstractDungeon.getCurrMapNode().y >= 3) {
                int roll = AbstractDungeon.miscRng.random(0, 99);
                if (roll < SpecialBattleTracker.specialBattleChance) {
                    SpecialBattleTracker.isSpecialBattle = true;
                    logger.info(String.format("Ascension 76: Special Battle triggered! (Roll: %d < %d)",
                        roll, SpecialBattleTracker.specialBattleChance));
                }
            }
        }
    }

    /**
     * Play elite BGM for special battles
     */
    @SpirePatch(
        clz = MonsterRoom.class,
        method = "onPlayerEntry"
    )
    public static class PlayEliteBGM {
        @SpirePostfixPatch
        public static void Postfix(MonsterRoom __instance) {
            if (!SpecialBattleTracker.isSpecialBattle) {
                return;
            }

            // Play elite BGM (BOSS_BOTTOM - Act 1 boss BGM)
            CardCrawlGame.music.unsilenceBGM();
            CardCrawlGame.music.playTempBgmInstantly("BOSS_BOTTOM", true);

            logger.info("Ascension 76: Playing elite BGM (BOSS_BOTTOM) for Special Battle");
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
