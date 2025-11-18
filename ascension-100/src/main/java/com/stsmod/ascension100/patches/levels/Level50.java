package com.stsmod.ascension100.patches.levels;

import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;
import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.actions.common.DamageAction;
import com.megacrit.cardcrawl.cards.DamageInfo;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.monsters.MonsterGroup;
import com.megacrit.cardcrawl.rooms.MonsterRoomBoss;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Ascension Level 50: Act 4 forced
 * ???막이 강제됩니다.
 *
 * 3막 보스 전투에서, 세개의 열쇠를 얻지 못했을 시:
 * - 체력은 1로 고정
 * - 에너지가 0으로 고정
 * - 전투 시작시 100 데미지를 5번 입음
 *
 * This effectively forces players to collect all 3 keys to survive Act 3 boss fight.
 */
public class Level50 {
    private static final Logger logger = LogManager.getLogger(Level50.class.getName());

    private static boolean punishmentApplied = false;

    /**
     * Apply punishment during pre-battle phase when action manager is ready
     */
    @SpirePatch(
        clz = MonsterGroup.class,
        method = "usePreBattleAction"
    )
    public static class ApplyPreBattlePunishment {
        @SpirePostfixPatch
        public static void Postfix(MonsterGroup __instance) {
            // Only apply on ascension 50+
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 50) {
                return;
            }

            // Only apply in Act 3 boss room
            if (AbstractDungeon.actNum != 3 || !(AbstractDungeon.getCurrRoom() instanceof MonsterRoomBoss)) {
                return;
            }

            // Only apply once per boss fight
            if (punishmentApplied) {
                return;
            }

            AbstractPlayer player = AbstractDungeon.player;

            // Check if player has all 3 keys (stored in Settings flags)
            boolean hasRubyKey = Settings.hasRubyKey;
            boolean hasEmeraldKey = Settings.hasEmeraldKey;
            boolean hasSapphireKey = Settings.hasSapphireKey;

            boolean hasAllKeys = hasRubyKey && hasEmeraldKey && hasSapphireKey;

            if (!hasAllKeys) {
                logger.warn(String.format(
                    "[Asc50] Act 3 Boss without all keys! Ruby: %b, Emerald: %b, Sapphire: %b",
                    hasRubyKey, hasEmeraldKey, hasSapphireKey
                ));

                // Punishment 0: Remove revival items first
                // Remove all Fairy Potions
                for (int i = player.potions.size() - 1; i >= 0; i--) {
                    com.megacrit.cardcrawl.potions.AbstractPotion potion = player.potions.get(i);
                    if (potion.ID != null && potion.ID.equals("FairyPotion")) {
                        player.removePotion(potion);
                        logger.info("[Asc50] Removed Fairy Potion from slot " + i);
                    }
                }

                // Remove Lizard Tail if present
                if (player.hasRelic("Lizard Tail")) {
                    player.loseRelic("Lizard Tail");
                    logger.info("[Asc50] Removed Lizard Tail relic");
                }

                // Punishment 1: Set HP to 1
                int hpLoss = player.currentHealth - 1;
                if (hpLoss > 0) {
                    player.currentHealth = 1;
                    player.healthBarUpdatedEvent();
                    logger.info(String.format(
                        "[Asc50] Set player HP to 1 (lost %d HP)",
                        hpLoss
                    ));
                }

                // Punishment 2: Set energy to 0
                player.energy.energy = 0;
                logger.info("[Asc50] Set player energy to 0");

                // Punishment 3: Deal massive damage to guarantee death
                // Use 9999 damage to ensure death even with any protection
                AbstractDungeon.actionManager.addToBottom(
                    new DamageAction(
                        (AbstractCreature) player,
                        new DamageInfo(
                            (AbstractCreature) player,
                            9999,
                            DamageInfo.DamageType.HP_LOSS
                        ),
                        AbstractGameAction.AttackEffect.FIRE,
                        true
                    )
                );

                logger.warn("[Asc50] Applied 9999 HP LOSS damage - Player will die");
                punishmentApplied = true;
            } else {
                logger.info("[Asc50] Player has all 3 keys - no punishment");
            }
        }
    }

    /**
     * Reset punishment flag when combat ends
     */
    @SpirePatch(
        clz = MonsterRoomBoss.class,
        method = "onPlayerEntry"
    )
    public static class ResetPunishmentFlag {
        @SpirePostfixPatch
        public static void Postfix(MonsterRoomBoss __instance) {
            if (AbstractDungeon.actNum == 3) {
                punishmentApplied = false;
            }
        }
    }
}
