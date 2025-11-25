package com.stsmod.ascension100.actions;

import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.screens.CardRewardScreen;
import com.megacrit.cardcrawl.vfx.cardManip.ShowCardAndAddToDiscardEffect;
import com.megacrit.cardcrawl.vfx.cardManip.ShowCardAndAddToHandEffect;

import java.util.ArrayList;

/**
 * Modified Discovery Action with flexible card choice count
 * Based on vanilla DiscoveryAction but allows specifying number of cards to choose from
 */
public class ReducedDiscoveryAction extends AbstractGameAction {
    private boolean retrieveCard = false;
    private boolean returnColorless = false;
    private AbstractCard.CardType cardType = null;
    private int numCards; // Number of cards to choose from

    /**
     * Create a discovery action with custom card count
     * @param type Card type filter (null for any)
     * @param amount Number of cards to add to hand
     * @param numCards Number of cards to choose from (2, 3, 4, etc.)
     */
    public ReducedDiscoveryAction(AbstractCard.CardType type, int amount, int numCards) {
        this.actionType = ActionType.CARD_MANIPULATION;
        this.duration = Settings.ACTION_DUR_FAST;
        this.amount = amount;
        this.cardType = type;
        this.numCards = numCards;
    }

    /**
     * Create a discovery action for colorless cards with custom card count
     * @param colorless True to generate colorless cards
     * @param amount Number of cards to add to hand
     * @param numCards Number of cards to choose from
     */
    public ReducedDiscoveryAction(boolean colorless, int amount, int numCards) {
        this.actionType = ActionType.CARD_MANIPULATION;
        this.duration = Settings.ACTION_DUR_FAST;
        this.amount = amount;
        this.returnColorless = colorless;
        this.numCards = numCards;
    }

    @Override
    public void update() {
        ArrayList<AbstractCard> generatedCards;
        if (this.returnColorless) {
            generatedCards = generateColorlessCardChoices();
        } else {
            generatedCards = generateCardChoices(this.cardType);
        }

        if (this.duration == Settings.ACTION_DUR_FAST) {
            AbstractDungeon.cardRewardScreen.customCombatOpen(
                generatedCards,
                CardRewardScreen.TEXT[1],
                (this.cardType != null)
            );

            tickDuration();
            return;
        }

        if (!this.retrieveCard) {
            if (AbstractDungeon.cardRewardScreen.discoveryCard != null) {
                AbstractCard disCard = AbstractDungeon.cardRewardScreen.discoveryCard.makeStatEquivalentCopy();
                AbstractCard disCard2 = AbstractDungeon.cardRewardScreen.discoveryCard.makeStatEquivalentCopy();

                // Master Reality power upgrades discovered cards
                if (AbstractDungeon.player.hasPower("MasterRealityPower")) {
                    disCard.upgrade();
                    disCard2.upgrade();
                }

                disCard.setCostForTurn(0);
                disCard2.setCostForTurn(0);

                disCard.current_x = -1000.0F * Settings.xScale;
                disCard2.current_x = -1000.0F * Settings.xScale + AbstractCard.IMG_HEIGHT_S;

                if (this.amount == 1) {
                    if (AbstractDungeon.player.hand.size() < 10) {
                        AbstractDungeon.effectList.add(new ShowCardAndAddToHandEffect(
                            disCard,
                            Settings.WIDTH / 2.0F,
                            Settings.HEIGHT / 2.0F
                        ));
                    } else {
                        AbstractDungeon.effectList.add(new ShowCardAndAddToDiscardEffect(
                            disCard,
                            Settings.WIDTH / 2.0F,
                            Settings.HEIGHT / 2.0F
                        ));
                    }
                    disCard2 = null;
                } else if (AbstractDungeon.player.hand.size() + this.amount <= 10) {
                    AbstractDungeon.effectList.add(new ShowCardAndAddToHandEffect(
                        disCard,
                        Settings.WIDTH / 2.0F - AbstractCard.IMG_WIDTH / 2.0F,
                        Settings.HEIGHT / 2.0F
                    ));
                    AbstractDungeon.effectList.add(new ShowCardAndAddToHandEffect(
                        disCard2,
                        Settings.WIDTH / 2.0F + AbstractCard.IMG_WIDTH / 2.0F,
                        Settings.HEIGHT / 2.0F
                    ));
                } else if (AbstractDungeon.player.hand.size() == 9) {
                    AbstractDungeon.effectList.add(new ShowCardAndAddToHandEffect(
                        disCard,
                        Settings.WIDTH / 2.0F - AbstractCard.IMG_WIDTH / 2.0F,
                        Settings.HEIGHT / 2.0F
                    ));
                    AbstractDungeon.effectList.add(new ShowCardAndAddToDiscardEffect(
                        disCard2,
                        Settings.WIDTH / 2.0F + AbstractCard.IMG_WIDTH / 2.0F,
                        Settings.HEIGHT / 2.0F
                    ));
                } else {
                    AbstractDungeon.effectList.add(new ShowCardAndAddToDiscardEffect(
                        disCard,
                        Settings.WIDTH / 2.0F - AbstractCard.IMG_WIDTH / 2.0F,
                        Settings.HEIGHT / 2.0F
                    ));
                    AbstractDungeon.effectList.add(new ShowCardAndAddToDiscardEffect(
                        disCard2,
                        Settings.WIDTH / 2.0F + AbstractCard.IMG_WIDTH / 2.0F,
                        Settings.HEIGHT / 2.0F
                    ));
                }

                AbstractDungeon.cardRewardScreen.discoveryCard = null;
            }
            this.retrieveCard = true;
        }

        tickDuration();
    }

    /**
     * Generate colorless card choices (with custom count)
     */
    private ArrayList<AbstractCard> generateColorlessCardChoices() {
        ArrayList<AbstractCard> cards = new ArrayList<>();

        while (cards.size() != this.numCards) {
            boolean dupe = false;

            AbstractCard tmp = AbstractDungeon.returnTrulyRandomColorlessCardInCombat();
            for (AbstractCard c : cards) {
                if (c.cardID.equals(tmp.cardID)) {
                    dupe = true;
                    break;
                }
            }
            if (!dupe) {
                cards.add(tmp.makeCopy());
            }
        }

        return cards;
    }

    /**
     * Generate card choices by type (with custom count)
     */
    private ArrayList<AbstractCard> generateCardChoices(AbstractCard.CardType type) {
        ArrayList<AbstractCard> cards = new ArrayList<>();

        while (cards.size() != this.numCards) {
            boolean dupe = false;
            AbstractCard tmp = null;
            if (type == null) {
                tmp = AbstractDungeon.returnTrulyRandomCardInCombat();
            } else {
                tmp = AbstractDungeon.returnTrulyRandomCardInCombat(type);
            }
            for (AbstractCard c : cards) {
                if (c.cardID.equals(tmp.cardID)) {
                    dupe = true;
                    break;
                }
            }
            if (!dupe) {
                cards.add(tmp.makeCopy());
            }
        }

        return cards;
    }
}
