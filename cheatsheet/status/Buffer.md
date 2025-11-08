# Buffer Power

## Overview
- **Power ID**: `"Buffer"`
- **Type**: `BUFF` (defensive)
- **Class**: `BufferPower`
- **Source**: `com.megacrit.cardcrawl.powers.BufferPower`

## Description
Prevents the next instance(s) of damage from reducing HP below 1. Each hit that would deal damage consumes 1 stack of Buffer and negates all damage from that hit.

## Key Mechanics

### Damage Negation Mechanism
```java
public int onAttackedToChangeDamage(DamageInfo info, int damageAmount) {
    if (damageAmount > 0) {
        addToTop((AbstractGameAction)new ReducePowerAction(this.owner, this.owner, this.ID, 1));
    }
    return 0;  // All damage reduced to 0
}
```

**Critical Points:**
1. Hook: `onAttackedToChangeDamage()` - called when owner is attacked
2. Checks if incoming damage > 0
3. Returns 0, negating ALL damage from that hit
4. Consumes 1 stack via `ReducePowerAction`

**Important:** Buffer doesn't prevent damage based on current HP. It negates damage regardless of whether it would be fatal.

### Stack Management
```java
public void stackPower(int stackAmount) {
    this.fontScale = 8.0F;  // Visual feedback
    this.amount += stackAmount;
}
```

**Behavior:**
- Multiple Buffer stacks can accumulate
- Each hit consumes exactly 1 stack (not variable)
- Power is automatically removed when amount reaches 0

## Implementation Details

### Constructor
```java
public BufferPower(AbstractCreature owner, int bufferAmt) {
    this.name = NAME;
    this.ID = "Buffer";
    this.owner = owner;
    this.amount = bufferAmt;
    updateDescription();
    loadRegion("buffer");
}
```

**Note:** No explicit `type` set in constructor. Uses default `PowerType.BUFF` from AbstractPower.

### Description Updates
```java
public void updateDescription() {
    if (this.amount <= 1) {
        this.description = DESCRIPTIONS[0];  // Single stack description
    } else {
        this.description = DESCRIPTIONS[1] + this.amount + DESCRIPTIONS[2];  // Multiple stacks
    }
}
```

## Mod Implementation Notes

### Creating Buffer Power
```java
// Grant Buffer to player
addToBot(new ApplyPowerAction(
    AbstractDungeon.player,  // target
    AbstractDungeon.player,  // source
    new BufferPower(AbstractDungeon.player, 1),  // 1 stack
    1
));
```

### Important Interactions

**What Buffer Prevents:**
- All incoming damage from attacks (damageAmount > 0)
- Multi-hit attacks consume multiple Buffer stacks (1 per hit)
- Works against any damage type (NORMAL, THORNS, HP_LOSS)

**What Buffer Does NOT Prevent:**
- Attacks that deal 0 damage (no stack consumed)
- HP loss from non-damage sources (powers, relics, events)
- Death from effects that set HP to 0 directly

### Damage Reduction Hooks

**Hook Priority:** `onAttackedToChangeDamage()` is one of several damage modification hooks:
1. `atDamageReceive()` - initial damage calculation
2. `onAttackedToChangeDamage()` - **Buffer executes here**
3. `atDamageFinalReceive()` - final damage adjustment (Intangible)

Buffer converts damage to 0 after initial calculations but before final adjustments.

### Testing Buffer

**Verify Damage Negation:**
```java
// Give player Buffer
addToBot(new ApplyPowerAction(player, player, new BufferPower(player, 1), 1));

// Deal damage (should be negated)
addToBot(new DamageAction(
    player,
    new DamageInfo(enemy, 50, DamageInfo.DamageType.NORMAL),
    AttackEffect.BLUNT_HEAVY
));

// Player HP should remain unchanged, Buffer consumed
```

**Verify Multi-Hit Behavior:**
```java
// Give player 2 Buffer stacks
addToBot(new ApplyPowerAction(player, player, new BufferPower(player, 2), 2));

// Multi-hit attack (3 hits of 10 damage each)
for (int i = 0; i < 3; i++) {
    addToBot(new DamageAction(
        player,
        new DamageInfo(enemy, 10, DamageInfo.DamageType.NORMAL),
        AttackEffect.SLASH_HORIZONTAL
    ));
}

// First 2 hits negated (consume Buffer), 3rd hit deals damage
```

**Verify Stack Accumulation:**
```java
// Apply Buffer twice
addToBot(new ApplyPowerAction(player, player, new BufferPower(player, 1), 1));
addToBot(new ApplyPowerAction(player, player, new BufferPower(player, 1), 1));

// Result: 2 stacks of Buffer (stacks additively)
```

## Common Pitfalls

1. **Not a "Death Prevention" Power**: Buffer prevents damage, not death. If an effect sets HP to 0 directly (without dealing damage), Buffer won't help.

2. **Multi-Hit Attacks**: Each hit in a multi-attack consumes 1 Buffer stack. A 5-hit attack will remove up to 5 stacks.

3. **0 Damage Attacks**: If an attack would deal 0 damage (due to Block, Intangible, etc.), Buffer is NOT consumed because `damageAmount > 0` check fails.

4. **Automatic Removal**: When Buffer reaches 0 stacks, the power is automatically removed by the game engine (no explicit `RemoveSpecificPowerAction` needed in the power class).

5. **Visual Feedback**: Font scale animation (`fontScale = 8.0F`) triggers when stacking, providing visual confirmation.

## Comparison with Similar Powers

| Power | Mechanism | Consumption | Effect |
|-------|-----------|-------------|--------|
| **Buffer** | Negates all damage from a hit | 1 per hit that deals damage | Damage = 0 |
| **Intangible** | Reduces damage to 1 | 1 per turn | Damage = 1 if > 1 |
| **Plated Armor** | Grants Block | Loses 1 when hit | Passive Block generation |

## Related Powers
- **Intangible**: Reduces damage instead of negating
- **Plated Armor**: Generates Block instead of damage negation
- Cards that grant Buffer: Fossilized Helix+ (relic), Self-Repair (Defect card)
