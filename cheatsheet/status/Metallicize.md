# Metallicize Power

## Overview
- **Power ID**: `"Metallicize"`
- **Type**: `BUFF` (defensive)
- **Class**: `MetallicizePower`
- **Source**: `com.megacrit.cardcrawl.powers.MetallicizePower`

## Description
At the end of each turn, gain Block equal to the amount of Metallicize. Unlike Plated Armor, Metallicize does NOT degrade when taking damage.

## Key Mechanics

### Block Generation
```java
public void atEndOfTurnPreEndTurnCards(boolean isPlayer) {
    flash();
    addToBot((AbstractGameAction)new GainBlockAction(this.owner, this.owner, this.amount));
}
```

**Critical Points:**
1. Hook: `atEndOfTurnPreEndTurnCards()` - triggers at end of turn BEFORE end-of-turn card effects
2. Grants Block equal to current `amount` value
3. Block persists into next turn (standard Block behavior)
4. **No degradation mechanism** - amount never decreases
5. Triggers for both player and monster versions

### No Degradation
**Key Difference from Plated Armor:** Metallicize has NO `wasHPLost()` hook. The amount remains constant throughout combat unless:
- Explicitly reduced by a card/power effect
- Removed entirely by power removal effects
- Modified by Strength/Dexterity loss effects (if implemented)

## Implementation Details

### Constructor
```java
public MetallicizePower(AbstractCreature owner, int armorAmt) {
    this.name = NAME;
    this.ID = "Metallicize";
    this.owner = owner;
    this.amount = armorAmt;
    updateDescription();
    loadRegion("armor");  // Uses "armor" sprite, not "metallicize"
}
```

**Note:** Uses `loadRegion("armor")` - shares sprite with generic armor visual.

### Audio Feedback
```java
public void playApplyPowerSfx() {
    CardCrawlGame.sound.play("POWER_METALLICIZE", 0.05F);
}
```

Unique sound effect distinguishes it from Plated Armor.

### Description Updates
```java
public void updateDescription() {
    if (this.owner.isPlayer) {
        this.description = DESCRIPTIONS[0] + this.amount + DESCRIPTIONS[1];
    } else {
        this.description = DESCRIPTIONS[2] + this.amount + DESCRIPTIONS[3];
    }
}
```

Different descriptions for player vs. monster (same mechanics, different wording).

### No Stack Cap
Unlike Plated Armor, Metallicize does NOT have a 999 cap in its `stackPower()` method. Standard stacking applies:
```java
// Inherited from AbstractPower - no explicit override in MetallicizePower
public void stackPower(int stackAmount) {
    this.fontScale = 8.0F;
    this.amount += stackAmount;
}
```

## Mod Implementation Notes

### Creating Metallicize Power

**For Player:**
```java
addToBot(new ApplyPowerAction(
    AbstractDungeon.player,
    AbstractDungeon.player,
    new MetallicizePower(AbstractDungeon.player, 3),  // 3 Metallicize
    3
));
```

**For Monsters:**
```java
addToBot(new ApplyPowerAction(
    monster,
    monster,
    new MetallicizePower(monster, 2),  // 2 Metallicize
    2
));
```

### Important Interactions

**What Affects Metallicize:**
- Power removal effects (Artifact, debuff cleanse)
- Direct power manipulation (ReducePowerAction, RemoveSpecificPowerAction)
- Card effects that modify power amounts

**What Does NOT Affect Metallicize:**
- Taking damage (no degradation)
- HP loss of any kind
- Number of attacks received
- Self-damage or HP_LOSS type damage

### Turn Timing

**End of Turn Sequence:**
1. `atEndOfTurnPreEndTurnCards()` - **Metallicize grants Block here**
2. End-of-turn card effects (e.g., Burn, Wound, Decay)
3. `atEndOfTurn()`
4. Power decay effects
5. Block removed (start of next turn)

**Example:**
- Turn 1: Apply 3 Metallicize
- Turn 1 ends: Gain 3 Block
- Turn 2 starts: 3 Block available, Metallicize still 3
- Turn 2: Take 50 damage → Metallicize still 3 (no degradation)
- Turn 2 ends: Gain 3 Block again
- Turn 3 starts: 3 Block available, Metallicize still 3
- This continues indefinitely until combat ends

### Metallicize vs Plated Armor

| Aspect | Metallicize | Plated Armor |
|--------|-------------|--------------|
| Block Timing | End of turn (before card effects) | End of turn (before card effects) |
| Degradation | **None** | Loses 1 per unblocked hit |
| Stability | **Permanent amount** | Decreases over combat |
| Typical Amount | Lower (2-4) | Higher (5-15) |
| Strategic Use | **Long-term defense** | Front-loaded defense |
| Stack Cap | None (can go >999) | Hard cap at 999 |
| Sound Effect | POWER_METALLICIZE | POWER_PLATED |
| Sprite | "armor" | "platedarmor" |

**Design Philosophy:**
- Metallicize: Small but reliable, rewards long combats
- Plated Armor: Large but degrading, rewards avoiding damage

### Synergies

**Strong with:**
- **Barricade**: Block persists between turns, multiplying Metallicize value
- **Calipers** (relic): Retains 15 Block between turns, stacks with Metallicize
- **Long combats**: Value increases with combat duration
- **Block-scaling effects**: Gaining Block triggers effects multiple times

**Weak against:**
- **Power removal**: Orange Pellets, Artifact (if debuff version), Apparition
- **Short combats**: Less total Block generation
- **Overwhelming damage**: Small amounts can't keep up with high DPS

### Testing Metallicize

**Verify Block Generation:**
```java
// Give player 3 Metallicize
addToBot(new ApplyPowerAction(
    player, player,
    new MetallicizePower(player, 3),
    3
));

// End turn
// Player should gain 3 Block at end of turn
```

**Verify No Degradation:**
```java
// Setup: 3 Metallicize, 0 Block
addToBot(new ApplyPowerAction(player, player, new MetallicizePower(player, 3), 3));

// Take 10 damage with 0 Block
addToBot(new DamageAction(
    player,
    new DamageInfo(enemy, 10, DamageInfo.DamageType.NORMAL),
    AttackEffect.SLASH_HEAVY
));

// Result: 10 HP lost, Metallicize still 3 (no degradation)
// End turn: gain 3 Block again
```

**Verify Multi-Turn Consistency:**
```java
// Setup: 4 Metallicize
addToBot(new ApplyPowerAction(player, player, new MetallicizePower(player, 4), 4));

// Simulate 5 turns of combat with damage
for (int turn = 0; turn < 5; turn++) {
    // End turn: gain 4 Block
    // Start turn: take damage
    addToBot(new DamageAction(
        player,
        new DamageInfo(enemy, 20, DamageInfo.DamageType.NORMAL),
        AttackEffect.SLASH_HEAVY
    ));
}

// After 5 turns: Metallicize still 4, gained 20 total Block (4 per turn)
```

**Verify Stacking:**
```java
// Apply Metallicize twice
addToBot(new ApplyPowerAction(player, player, new MetallicizePower(player, 2), 2));
addToBot(new ApplyPowerAction(player, player, new MetallicizePower(player, 3), 3));

// Result: 5 Metallicize total (stacks additively)
// End turn: gain 5 Block
```

**Verify Barricade Synergy:**
```java
// Apply Barricade (Block doesn't reset between turns)
addToBot(new ApplyPowerAction(player, player, new BarricadePower(player), 1));

// Apply 3 Metallicize
addToBot(new ApplyPowerAction(player, player, new MetallicizePower(player, 3), 3));

// Turn 1 ends: gain 3 Block (total: 3)
// Turn 2 starts: still 3 Block (Barricade preserves)
// Turn 2 ends: gain 3 Block (total: 6)
// Turn 3 starts: still 6 Block
// Block accumulates indefinitely
```

**Verify No Stack Cap:**
```java
// Apply 1000 Metallicize (no cap unlike Plated Armor)
addToBot(new ApplyPowerAction(player, player, new MetallicizePower(player, 1000), 1000));

// Result: Metallicize = 1000 (no cap)
// End turn: gain 1000 Block
```

## Common Pitfalls

1. **Expecting Degradation**: Metallicize does NOT degrade. It's permanent for the combat duration unless explicitly removed.

2. **Confusing with Plated Armor**: Despite similar mechanics, they're completely different powers with different IDs and behaviors.

3. **Block Doesn't Carry Over**: The BLOCK granted carries over if Barricade is active, but Metallicize itself always grants fresh Block each turn.

4. **Low Initial Value**: Typically 2-4 Metallicize. Don't expect instant high Block - value comes from consistency.

5. **Power Removal**: Can be removed by Orange Pellets, Artifact (if applied as debuff somehow), or specific power removal effects.

6. **Timing Confusion**: Grants Block BEFORE end-of-turn cards, so Burn damage happens AFTER Block is gained.

## Related Powers
- **Plated Armor**: Similar Block generation, but degrades on hit
- **Buffer**: Prevents damage instead of blocking
- **Barricade**: Synergizes by preserving Metallicize's Block

## Cards Granting Metallicize
- **Metallicize** (Ironclad Rare Power): 3(4) Metallicize
- Limited sources - typically requires Power card investment

## Monsters with Metallicize
- **Jaw Worm**: Gains small Metallicize amounts
- **Transient**: Has high Metallicize in some encounters
- **Spire Shield/Spire Spear**: Permanent Metallicize as core mechanic

## Design Notes for Modders

Metallicize is a **scaling defense** power - weak early, strong in long combats. When designing cards/powers:

- **Small amounts** (2-4): Balanced for player use
- **Large amounts** (8+): Boss-level threat, provides insurmountable defense in long combats
- **Synergy consideration**: Extremely powerful with Block retention effects
- **Counter design**: Power removal, overwhelming burst damage, or combat time limits

**Balance Guideline:**
- 3 Metallicize ≈ 3 Block/turn ≈ 30-60 total Block in typical combat
- Compare to: Body Slam value, Barricade synergy, Calipers interaction
