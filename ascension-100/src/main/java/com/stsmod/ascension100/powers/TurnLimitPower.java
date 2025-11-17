package com.stsmod.ascension100.powers;

import com.megacrit.cardcrawl.actions.common.DamageAction;
import com.megacrit.cardcrawl.actions.common.ReducePowerAction;
import com.megacrit.cardcrawl.cards.DamageInfo;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.localization.PowerStrings;
import com.megacrit.cardcrawl.powers.AbstractPower;

/**
 * Turn Limit Power - Custom power for Ascension 95+
 *
 * Decreases by 1 each turn.
 * When it reaches 1, deals 999 damage at end of turn and stays at 1.
 * If player survives, continues to deal 999 damage every turn.
 */
public class TurnLimitPower extends AbstractPower {
    public static final String POWER_ID = "ascension100:TurnLimit";
    private static final PowerStrings powerStrings = CardCrawlGame.languagePack.getPowerStrings(POWER_ID);
    public static final String NAME = powerStrings.NAME;
    public static final String[] DESCRIPTIONS = powerStrings.DESCRIPTIONS;

    private static final int LETHAL_DAMAGE = 999;

    public TurnLimitPower(AbstractCreature owner, int amount) {
        this.name = NAME;
        this.ID = POWER_ID;
        this.owner = owner;
        this.amount = amount;
        this.type = PowerType.DEBUFF;
        updateDescription();
        loadRegion("lessdraw"); // Using less draw icon as placeholder
    }

    @Override
    public void updateDescription() {
        if (this.amount == 1) {
            this.description = DESCRIPTIONS[1] + LETHAL_DAMAGE + DESCRIPTIONS[2];
        } else {
            this.description = DESCRIPTIONS[0] + this.amount + DESCRIPTIONS[3];
        }
    }

    @Override
    public void atEndOfTurn(boolean isPlayer) {
        if (this.amount > 1) {
            // Decrease by 1 if amount > 1
            AbstractDungeon.actionManager.addToBottom(
                new ReducePowerAction(this.owner, this.owner, this.ID, 1)
            );
            updateDescription();
        } else if (this.amount == 1) {
            // Deal 999 damage and stay at 1
            AbstractDungeon.actionManager.addToBottom(
                new DamageAction(
                    this.owner,
                    new DamageInfo(this.owner, LETHAL_DAMAGE, DamageInfo.DamageType.HP_LOSS),
                    com.megacrit.cardcrawl.actions.AbstractGameAction.AttackEffect.FIRE
                )
            );
            // Don't reduce amount - it stays at 1
            updateDescription();
        }
    }
}
