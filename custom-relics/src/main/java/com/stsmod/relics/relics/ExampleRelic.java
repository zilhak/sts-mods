package com.stsmod.relics.relics;

import basemod.abstracts.CustomRelic;
import com.badlogic.gdx.graphics.Texture;
import com.megacrit.cardcrawl.actions.common.DrawCardAction;
import com.megacrit.cardcrawl.actions.common.GainEnergyAction;
import com.megacrit.cardcrawl.actions.common.RelicAboveCreatureAction;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.relics.AbstractRelic;
import com.stsmod.relics.CustomRelicsMod;

/**
 * Example Relic: Lucky Coin
 *
 * Effect: At the start of each combat, gain [E] and draw 1 card.
 *
 * This is a simple example relic demonstrating basic relic functionality.
 * Rarity: Common
 */
public class ExampleRelic extends CustomRelic {

    // Relic ID
    public static final String ID = CustomRelicsMod.makeID("LuckyCoin");

    // Image paths
    private static final String IMG = "lucky_coin.png";
    private static final String OUTLINE = "lucky_coin_outline.png";

    // Relic stats
    private static final RelicTier TIER = RelicTier.COMMON;
    private static final LandingSound SOUND = LandingSound.CLINK;

    /**
     * Constructor
     */
    public ExampleRelic() {
        super(
                ID,
                CustomRelicsMod.getRelicTexture(IMG),
                TIER,
                SOUND
        );
    }

    /**
     * Get description text
     * This will be populated from localization files
     */
    @Override
    public String getUpdatedDescription() {
        return DESCRIPTIONS[0];
    }

    /**
     * Called at the start of each combat
     * This is where the relic's main effect triggers
     */
    @Override
    public void atBattleStart() {
        // Flash the relic to indicate it's activating
        flash();

        // Show the relic above the player
        addToBot(new RelicAboveCreatureAction(AbstractDungeon.player, this));

        // Gain 1 energy
        addToBot(new GainEnergyAction(1));

        // Draw 1 card
        addToBot(new DrawCardAction(1));
    }

    /**
     * Create a copy of this relic
     * Required for relic functionality
     */
    @Override
    public AbstractRelic makeCopy() {
        return new ExampleRelic();
    }
}
