# Plated Armor Power

## Overview
- **Power ID**: `"Plated Armor"`
- **Type**: `BUFF` (defensive)
- **Class**: `PlatedArmorPower`
- **Source**: `com.megacrit.cardcrawl.powers.PlatedArmorPower`

## Description
At the end of each turn, gain Block equal to the amount of Plated Armor. When the owner takes unblocked attack damage, lose 1 Plated Armor.

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
4. Triggers for both player and monster versions

### Degradation on Hit
```java
public void wasHPLost(DamageInfo info, int damageAmount) {
    if (info.owner != null &&
        info.owner != this.owner &&
        info.type != DamageInfo.DamageType.HP_LOSS &&
        info.type != DamageInfo.DamageType.THORNS &&
        damageAmount > 0) {

        flash();
        addToBot((AbstractGameAction)new ReducePowerAction(this.owner, this.owner, "Plated Armor", 1));
    }
}
```

**Degradation Conditions:**
- Only when HP is actually lost (not blocked)
- Damage must come from another creature (`info.owner != this.owner`)
- Not triggered by `HP_LOSS` type (direct HP reduction)
- Not triggered by `THORNS` damage (self-inflicted reflection)
- Damage amount must be > 0
- Reduces Plated Armor by exactly 1 (regardless of damage amount)

### Monster-Specific: Armor Break Animation
```java
public void onRemove() {
    if (!this.owner.isPlayer) {
        addToBot((AbstractGameAction)new ChangeStateAction((AbstractMonster)this.owner, "ARMOR_BREAK"));
    }
}
```

**Behavior:**
- When Plated Armor is removed from a monster, triggers "ARMOR_BREAK" animation
- Player doesn't have this animation state

## Implementation Details

### Constructor
```java
public PlatedArmorPower(AbstractCreature owner, int amt) {
    this.name = NAME;
    this.ID = "Plated Armor";
    this.owner = owner;
    this.amount = amt;
    updateDescription();
    loadRegion("platedarmor");
}
```

### Stack Management
```java
public void stackPower(int stackAmount) {
    this.fontScale = 8.0F;  // Visual feedback
    this.amount += stackAmount;
    if (this.amount > 999) {
        this.amount = 999;  // Hard cap at 999
    }
    updateDescription();
}
```

**Important:** Hard cap at 999 prevents overflow/display issues.

### Audio Feedback
```java
public void playApplyPowerSfx() {
    CardCrawlGame.sound.play("POWER_PLATED", 0.05F);
}
```

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

## Mod Implementation Notes

### Creating Plated Armor Power

**For Player:**
```java
addToBot(new ApplyPowerAction(
    AbstractDungeon.player,
    AbstractDungeon.player,
    new PlatedArmorPower(AbstractDungeon.player, 5),  // 5 Plated Armor
    5
));
```

**For Monsters:**
```java
addToBot(new ApplyPowerAction(
    monster,
    monster,
    new PlatedArmorPower(monster, 3),  // 3 Plated Armor
    3
));
```

### Important Interactions

**What Triggers Degradation:**
- Attack damage that pierces Block and reduces HP
- Only loses 1 stack per instance of HP loss, not per damage point

**What Does NOT Trigger Degradation:**
- Damage fully absorbed by Block (HP not lost)
- Self-inflicted damage (e.g., Hemokinesis, Brutality)
- HP_LOSS type damage (e.g., Rupture trigger cost)
- THORNS damage (reflected damage)
- Taking 0 damage

### Turn Timing

**End of Turn Sequence:**
1. `atEndOfTurnPreEndTurnCards()` - **Plated Armor grants Block here**
2. End-of-turn card effects (e.g., Burn, Wound)
3. `atEndOfTurn()`
4. Power decay effects
5. Block removed (start of next turn)

**Example:**
- Turn 3 ends: 4 Plated Armor → gain 4 Block
- Turn 4 starts: 4 Block available, Plated Armor still 4
- Take 10 damage: 4 blocked, 6 HP lost → Plated Armor becomes 3
- Turn 4 ends: gain 3 Block
- Turn 5 starts: 3 Block available

### Plated Armor vs Metallicize

| Aspect | Plated Armor | Metallicize |
|--------|--------------|-------------|
| Block Timing | End of turn (before card effects) | End of turn (before card effects) |
| Degradation | Loses 1 when HP lost | No degradation |
| Stability | Decreases over combat | Permanent amount |
| Typical Amount | Higher initial value | Lower permanent value |
| Strategic Use | Front-loaded defense | Consistent long-term |

Both use same hook (`atEndOfTurnPreEndTurnCards`) but different sustainability models.

### Testing Plated Armor

**Verify Block Generation:**
```java
// Give player 5 Plated Armor
addToBot(new ApplyPowerAction(
    player, player,
    new PlatedArmorPower(player, 5),
    5
));

// End turn
// Player should gain 5 Block at end of turn
```

**Verify Degradation on Hit:**
```java
// Setup: 3 Plated Armor, 0 Block
addToBot(new ApplyPowerAction(player, player, new PlatedArmorPower(player, 3), 3));

// Take 10 damage with 0 Block
addToBot(new DamageAction(
    player,
    new DamageInfo(enemy, 10, DamageInfo.DamageType.NORMAL),
    AttackEffect.SLASH_HEAVY
));

// Result: 10 HP lost, Plated Armor reduced to 2
```

**Verify NO Degradation When Blocked:**
```java
// Setup: 3 Plated Armor, 15 Block
player.addBlock(15);
addToBot(new ApplyPowerAction(player, player, new PlatedArmorPower(player, 3), 3));

// Take 10 damage (fully blocked)
addToBot(new DamageAction(
    player,
    new DamageInfo(enemy, 10, DamageInfo.DamageType.NORMAL),
    AttackEffect.SLASH_HEAVY
));

// Result: 0 HP lost, Block reduced to 5, Plated Armor still 3
```

**Verify Multi-Hit Degradation:**
```java
// Setup: 5 Plated Armor, 0 Block
addToBot(new ApplyPowerAction(player, player, new PlatedArmorPower(player, 5), 5));

// Multi-attack: 3 hits of 5 damage each
for (int i = 0; i < 3; i++) {
    addToBot(new DamageAction(
        player,
        new DamageInfo(enemy, 5, DamageInfo.DamageType.NORMAL),
        AttackEffect.SLASH_DIAGONAL
    ));
}

// Result: 15 HP lost total, Plated Armor reduced by 3 (once per hit), now 2
```

**Verify Self-Damage Immunity:**
```java
// Setup: 3 Plated Armor
addToBot(new ApplyPowerAction(player, player, new PlatedArmorPower(player, 3), 3));

// Self-damage (e.g., Hemokinesis)
addToBot(new DamageAction(
    player,
    new DamageInfo(player, 3, DamageInfo.DamageType.HP_LOSS),
    AttackEffect.NONE
));

// Result: 3 HP lost, Plated Armor still 3 (no degradation)
```

**Verify 999 Cap:**
```java
// Apply 1000 Plated Armor
addToBot(new ApplyPowerAction(player, player, new PlatedArmorPower(player, 1000), 1000));

// Result: Plated Armor capped at 999
```

## Common Pitfalls

1. **Degradation Trigger**: Only loses armor when HP is ACTUALLY LOST, not when damage is dealt. Block prevents degradation.

2. **Per-Hit vs Per-Damage**: Loses 1 armor per hit that deals damage, not 1 per damage point. A 50-damage hit only removes 1 armor.

3. **Self-Damage Immunity**: `info.owner != this.owner` check means self-inflicted damage doesn't degrade armor. Brutality, Hemokinesis, etc. won't reduce Plated Armor.

4. **HP_LOSS Type Immunity**: Direct HP loss effects bypass degradation. This includes some card costs and relic effects.

5. **Timing Window**: Block is granted at end of turn BEFORE card effects. Burn damage happens AFTER Plated Armor grants Block.

6. **Monster Armor Break**: When creating custom monsters with Plated Armor, ensure they have an "ARMOR_BREAK" animation state, or override `onRemove()`.

## Related Powers
- **Metallicize**: Similar Block generation, no degradation
- **Buffer**: Prevents damage instead of blocking
- **Barricade** (card effect): Preserves Block between turns (synergizes well)

## Monsters with Plated Armor
- **Bronze Automaton** (boss): Starts with high Plated Armor
- **Orb Walker** (elite): Gains Plated Armor during combat
- **Spheric Guardian** (elite): Permanent Plated Armor mechanic
