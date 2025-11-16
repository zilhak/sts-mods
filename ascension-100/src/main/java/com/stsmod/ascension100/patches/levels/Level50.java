package com.stsmod.ascension100.patches.levels;

import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;
import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.actions.common.DamageAction;
import com.megacrit.cardcrawl.cards.DamageInfo;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
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

    /**
     * Patch Act 3 boss room to punish players who don't have all keys
     */
    @SpirePatch(
        clz = MonsterRoomBoss.class,
        method = "onPlayerEntry"
    )
    public static class ForceAct4Punishment {
        @SpirePostfixPatch
        public static void Postfix(MonsterRoomBoss __instance) {
            // Only apply on ascension 50+
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 50) {
                return;
            }

            // Only apply in Act 3
            if (AbstractDungeon.actNum != 3) {
                return;
            }

            AbstractPlayer player = AbstractDungeon.player;

            // Check if player has all 3 keys (stored as relics)
            boolean hasRubyKey = player.hasRelic("Ruby Key");
            boolean hasEmeraldKey = player.hasRelic("Emerald Key");
            boolean hasSapphireKey = player.hasRelic("Sapphire Key");

            boolean hasAllKeys = hasRubyKey && hasEmeraldKey && hasSapphireKey;

            if (!hasAllKeys) {
                logger.warn(String.format(
                    "[Asc50] Act 3 Boss without all keys! Ruby: %b, Emerald: %b, Sapphire: %b",
                    hasRubyKey, hasEmeraldKey, hasSapphireKey
                ));

                // Punishment 1: Set HP to 1
                int hpLoss = player.currentHealth - 1;
                if (hpLoss > 0) {
                    player.currentHealth = 1;
                    logger.info(String.format(
                        "[Asc50] Set player HP to 1 (lost %d HP)",
                        hpLoss
                    ));
                }

                // Punishment 2: Set energy to 0
                player.energy.energy = 0;
                logger.info("[Asc50] Set player energy to 0");

                // Punishment 3: Deal 100 damage 5 times (500 total)
                // This will kill the player immediately since HP is 1
                for (int i = 0; i < 5; i++) {
                    AbstractDungeon.actionManager.addToBottom(
                        new DamageAction(
                            (AbstractCreature) player,
                            new DamageInfo(
                                (AbstractCreature) player,
                                100,
                                DamageInfo.DamageType.HP_LOSS
                            ),
                            AbstractGameAction.AttackEffect.FIRE,
                            true
                        )
                    );
                }

                logger.warn("[Asc50] Applied 500 HP LOSS damage (100 x 5) - Player will die");
            } else {
                logger.info("[Asc50] Player has all 3 keys - no punishment");
            }
        }
    }
}
