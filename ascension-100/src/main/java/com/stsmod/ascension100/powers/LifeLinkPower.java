package com.stsmod.ascension100.powers;

import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.localization.PowerStrings;
import com.megacrit.cardcrawl.powers.AbstractPower;

/**
 * Life Link power - visual indicator for Donu/Deca at Ascension 87+
 * Shows that the boss will revive if the other is still alive
 */
public class LifeLinkPower extends AbstractPower {
    public static final String POWER_ID = "ascension100:LifeLink";
    private static final PowerStrings powerStrings = CardCrawlGame.languagePack.getPowerStrings(POWER_ID);
    public static final String NAME = powerStrings.NAME;
    public static final String[] DESCRIPTIONS = powerStrings.DESCRIPTIONS;

    public LifeLinkPower(AbstractCreature owner) {
        this.name = NAME;
        this.ID = POWER_ID;
        this.owner = owner;
        this.amount = -1; // No amount display
        this.type = PowerType.BUFF;

        updateDescription();

        // Load power image - MUST have valid region128 to avoid crash in FlashPowerEffect
        loadRegion("regrow");

        // CRITICAL: If loadRegion failed (region128 is null), we MUST set a fallback
        // FlashPowerEffect.java:29 will crash if both img and region128 are null
        if (this.region128 == null) {
            // Fallback to strength icon which definitely exists in base game
            this.region128 = AbstractPower.atlas.findRegion("128/strength");
            this.region48 = AbstractPower.atlas.findRegion("48/strength");
        }
    }

    @Override
    public void updateDescription() {
        this.description = DESCRIPTIONS[0];
    }
}
