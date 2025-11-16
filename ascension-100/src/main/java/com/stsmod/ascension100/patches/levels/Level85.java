package com.stsmod.ascension100.patches.levels;

import com.evacipated.cardcrawl.modthespire.lib.*;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.events.exordium.ScrapOoze;
import com.megacrit.cardcrawl.events.shrines.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;

/**
 * Ascension Level 85: Some events become more hostile
 *
 * 일부 이벤트가 적대적으로 변경됩니다.
 *
 * - "맞추고 가져가!": 카드 구성 변경 (일반, 특별, 레어, 무색, 저주x2)
 * - "얼굴 상인": HP 손실 15%로 증가
 * - "모닥불 정령들": 특별 카드 회복량 80%로 감소
 * - "파란 옷의 여자": 모든 가격 +10
 * - "날붙이 수액": 초기 데미지 +1, 유물 획득 확률 +10%
 */
public class Level85 {
    private static final Logger logger = LogManager.getLogger(Level85.class.getName());

    /**
     * Patch GremlinMatchGame card composition
     */
    @SpirePatch(
        clz = GremlinMatchGame.class,
        method = "initializeCards"
    )
    public static class ModifyMatchAndKeepCards {
        @SpirePrefixPatch
        public static SpireReturn<ArrayList<AbstractCard>> Prefix(GremlinMatchGame __instance) {
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 85) {
                return SpireReturn.Continue();
            }

            ArrayList<AbstractCard> retVal = new ArrayList<>();
            ArrayList<AbstractCard> retVal2 = new ArrayList<>();

            // Modified card pool for Ascension 85+
            retVal.add(AbstractDungeon.getCard(AbstractCard.CardRarity.COMMON).makeCopy());  // Common character card
            retVal.add(AbstractDungeon.getCard(AbstractCard.CardRarity.UNCOMMON).makeCopy()); // Uncommon character card
            retVal.add(AbstractDungeon.getCard(AbstractCard.CardRarity.RARE).makeCopy());     // Rare character card
            retVal.add(AbstractDungeon.returnColorlessCard(AbstractCard.CardRarity.UNCOMMON).makeCopy()); // Colorless card
            retVal.add(AbstractDungeon.returnRandomCurse()); // Curse 1
            retVal.add(AbstractDungeon.returnRandomCurse()); // Curse 2

            retVal.add(AbstractDungeon.player.getStartCardForEvent());

            // Create duplicates
            for (AbstractCard c : retVal) {
                for (com.megacrit.cardcrawl.relics.AbstractRelic r : AbstractDungeon.player.relics) {
                    r.onPreviewObtainCard(c);
                }
                retVal2.add(c.makeStatEquivalentCopy());
            }

            retVal.addAll(retVal2);

            // Position cards
            for (AbstractCard c : retVal) {
                c.current_x = com.megacrit.cardcrawl.core.Settings.WIDTH / 2.0F;
                c.target_x = c.current_x;
                c.current_y = -300.0F * com.megacrit.cardcrawl.core.Settings.scale;
                c.target_y = c.current_y;
            }

            logger.info("Ascension 85: Modified Match and Keep card pool");
            return SpireReturn.Return(retVal);
        }
    }

    /**
     * Patch FaceTrader HP loss from 10% to 15%
     */
    @SpirePatch(
        clz = FaceTrader.class,
        method = SpirePatch.CONSTRUCTOR
    )
    public static class IncreaseFaceTraderDamage {
        @SpirePostfixPatch
        public static void Postfix(FaceTrader __instance) {
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 85) {
                return;
            }

            try {
                // Access private damage field using reflection
                java.lang.reflect.Field damageField = FaceTrader.class.getDeclaredField("damage");
                damageField.setAccessible(true);

                // Recalculate damage to 15% (was 10%)
                int newDamage = AbstractDungeon.player.maxHealth * 15 / 100;
                if (newDamage == 0) {
                    newDamage = 1;
                }
                damageField.setInt(__instance, newDamage);

                logger.info(String.format("Ascension 85: Increased FaceTrader damage to %d (15%% of max HP)", newDamage));
            } catch (Exception e) {
                logger.error("Failed to modify FaceTrader damage", e);
            }
        }
    }

    /**
     * Patch Bonfire Elementals to reduce Special card healing from 100% to 80%
     */
    @SpirePatch(
        clz = Bonfire.class,
        method = "setReward"
    )
    public static class ReduceBonfireSpecialHealing {
        @SpirePrefixPatch
        public static SpireReturn<Void> Prefix(Bonfire __instance, AbstractCard.CardRarity rarity) {
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 85) {
                return SpireReturn.Continue();
            }

            // Only modify UNCOMMON (Special) rarity healing
            if (rarity == AbstractCard.CardRarity.UNCOMMON) {
                try {
                    // Heal 80% of missing HP instead of 100%
                    int missingHP = AbstractDungeon.player.maxHealth - AbstractDungeon.player.currentHealth;
                    int healAmount = (int)(missingHP * 0.8F);

                    AbstractDungeon.player.heal(healAmount);

                    // Update dialog
                    java.lang.reflect.Field descField = com.megacrit.cardcrawl.events.shrines.Bonfire.class.getDeclaredField("DESCRIPTIONS");
                    descField.setAccessible(true);
                    String[] descriptions = (String[]) descField.get(null);

                    String dialog = descriptions[2] + descriptions[6]; // DIALOG_3 + DESCRIPTIONS[6]

                    // Update image event text
                    __instance.imageEventText.updateBodyText(dialog);
                    __instance.imageEventText.updateDialogOption(0,
                        ((String[])com.megacrit.cardcrawl.events.shrines.Bonfire.class.getDeclaredField("OPTIONS").get(null))[1]);

                    logger.info(String.format("Ascension 85: Bonfire Special healing reduced to %d (80%% of missing HP)", healAmount));

                    return SpireReturn.Return(null);
                } catch (Exception e) {
                    logger.error("Failed to modify Bonfire healing", e);
                    return SpireReturn.Continue();
                }
            }

            return SpireReturn.Continue();
        }
    }

    /**
     * Patch WomanInBlue to increase all costs by 10
     */
    @SpirePatch(
        clz = WomanInBlue.class,
        method = SpirePatch.CONSTRUCTOR
    )
    public static class IncreaseWomanInBlueCosts {
        @SpirePostfixPatch
        public static void Postfix(WomanInBlue __instance) {
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 85) {
                return;
            }

            try {
                // Modify the constant costs using reflection
                java.lang.reflect.Field cost1Field = WomanInBlue.class.getDeclaredField("cost1");
                java.lang.reflect.Field cost2Field = WomanInBlue.class.getDeclaredField("cost2");
                java.lang.reflect.Field cost3Field = WomanInBlue.class.getDeclaredField("cost3");

                cost1Field.setAccessible(true);
                cost2Field.setAccessible(true);
                cost3Field.setAccessible(true);

                // Increase each by 10 (20→30, 30→40, 40→50)
                cost1Field.setInt(null, 30);
                cost2Field.setInt(null, 40);
                cost3Field.setInt(null, 50);

                logger.info("Ascension 85: Increased WomanInBlue costs (+10 each)");
            } catch (Exception e) {
                logger.error("Failed to modify WomanInBlue costs", e);
            }
        }
    }

    /**
     * Patch ScrapOoze to increase initial damage and relic chance
     */
    @SpirePatch(
        clz = ScrapOoze.class,
        method = SpirePatch.CONSTRUCTOR
    )
    public static class ModifyScrapOoze {
        @SpirePostfixPatch
        public static void Postfix(ScrapOoze __instance) {
            if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 85) {
                return;
            }

            try {
                // Increase damage by 1
                java.lang.reflect.Field dmgField = ScrapOoze.class.getDeclaredField("dmg");
                dmgField.setAccessible(true);
                int currentDmg = dmgField.getInt(__instance);
                dmgField.setInt(__instance, currentDmg + 1);

                // Increase relic obtain chance by 10%
                java.lang.reflect.Field relicChanceField = ScrapOoze.class.getDeclaredField("relicObtainChance");
                relicChanceField.setAccessible(true);
                int currentChance = relicChanceField.getInt(__instance);
                relicChanceField.setInt(__instance, currentChance + 10);

                logger.info(String.format(
                    "Ascension 85: Modified ScrapOoze - Damage: %d (+1), Relic chance: %d%% (+10%%)",
                    currentDmg + 1, currentChance + 10
                ));
            } catch (Exception e) {
                logger.error("Failed to modify ScrapOoze", e);
            }
        }
    }
}
