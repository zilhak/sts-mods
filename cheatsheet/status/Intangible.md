# Intangible Power

## Overview
- **Power ID**: `"Intangible"` (monsters), `"IntangiblePlayer"` (player)
- **Type**: `BUFF` (defensive)
- **Classes**:
  - `IntangiblePower` - for monsters
  - `IntangiblePlayerPower` - for player
- **Source**:
  - `com.megacrit.cardcrawl.powers.IntangiblePower`
  - `com.megacrit.cardcrawl.powers.IntangiblePlayerPower`

## Description
Reduces all incoming damage to 1. Lasts for a certain number of turns, decaying at different times depending on whether it's player or monster version.

## Key Mechanics

### Damage Reduction Mechanism
Both versions use the same hook:

```java
public float atDamageFinalReceive(float damage, DamageInfo.DamageType type) {
    if (damage > 1.0F) {
        damage = 1.0F;  // Cap damage at 1
    }
    return damage;
}
```

**Critical Points:**
1. Hook: `atDamageFinalReceive()` - final damage calculation hook
2. Priority: 75 (high priority, executes late in damage pipeline)
3. Doesn't negate damage completely, reduces to exactly 1
4. Works on all damage types (NORMAL, THORNS, HP_LOSS)
5. If incoming damage is already ≤1, no change occurs

### Turn-Based Decay: Player Version

```java
// IntangiblePlayerPower.java
public void atEndOfRound() {
    flash();

    if (this.amount == 0) {
        addToBot((AbstractGameAction)new RemoveSpecificPowerAction(this.owner, this.owner, "IntangiblePlayer"));
    } else {
        addToBot((AbstractGameAction)new ReducePowerAction(this.owner, this.owner, "IntangiblePlayer", 1));
    }
}
```

**Player Behavior:**
- Decrements at **end of round** (after both player and all monsters have taken turns)
- No "just applied" grace period
- Lasts through the entire round it's applied

### Turn-Based Decay: Monster Version

```java
// IntangiblePower.java
private boolean justApplied;

public IntangiblePower(AbstractCreature owner, int turns) {
    // ...
    this.justApplied = true;  // Set on creation
}

public void atEndOfTurn(boolean isPlayer) {
    if (this.justApplied) {
        this.justApplied = false;
        return;  // Don't decrement on first turn
    }
    flash();

    if (this.amount == 0) {
        addToBot((AbstractGameAction)new RemoveSpecificPowerAction(this.owner, this.owner, "Intangible"));
    } else {
        addToBot((AbstractGameAction)new ReducePowerAction(this.owner, this.owner, "Intangible", 1));
    }
}
```

**Monster Behavior:**
- Decrements at **end of turn** (specific creature's turn)
- Has `justApplied` flag - doesn't decrement on the turn it's applied
- Effective for at least 1 full turn cycle

## Implementation Details

### Constructor Differences

**Player Version:**
```java
public IntangiblePlayerPower(AbstractCreature owner, int turns) {
    this.name = NAME;
    this.ID = "IntangiblePlayer";
    this.owner = owner;
    this.amount = turns;
    updateDescription();
    loadRegion("intangible");  // Same sprite as monster version
    this.priority = 75;        // High priority
}
```

**Monster Version:**
```java
public IntangiblePower(AbstractCreature owner, int turns) {
    this.name = NAME;
    this.ID = "Intangible";
    this.owner = owner;
    this.amount = turns;
    updateDescription();
    loadRegion("intangible");
    this.priority = 75;
    this.justApplied = true;  // Additional field
}
```

### Audio Feedback
Both versions:
```java
public void playApplyPowerSfx() {
    CardCrawlGame.sound.play("POWER_INTANGIBLE", 0.05F);
}
```

### Description
Both versions:
```java
public void updateDescription() {
    this.description = DESCRIPTIONS[0];  // Static description, doesn't show turn count
}
```

## Mod Implementation Notes

### Creating Intangible Power

**For Player:**
```java
addToBot(new ApplyPowerAction(
    AbstractDungeon.player,
    AbstractDungeon.player,
    new IntangiblePlayerPower(AbstractDungeon.player, 1),  // 1 turn
    1
));
```

**For Monsters:**
```java
addToBot(new ApplyPowerAction(
    monster,
    monster,
    new IntangiblePower(monster, 2),  // 2 turns
    2
));
```

### Important Interactions

**Damage Pipeline Position:**
1. `atDamageReceive()` - early damage modifications (Vulnerable)
2. Block reduction
3. `onAttackedToChangeDamage()` - mid-pipeline (Buffer)
4. `atDamageFinalReceive()` - **Intangible executes here** (final adjustment)

**Key Behaviors:**
- Intangible runs AFTER Buffer
- If Buffer prevents damage (returns 0), Intangible doesn't need to reduce it
- If damage somehow becomes ≤1 before Intangible, no further reduction

### Intangible vs Buffer

| Aspect | Intangible | Buffer |
|--------|-----------|--------|
| Damage Result | Always 1 (if > 1) | Always 0 |
| Duration | Turn-based | Hit-based |
| Multi-Hit | Takes 1 damage per hit | Negates 1 hit per stack |
| Decay | End of turn/round | When hit |
| Priority | 75 (late) | Default (mid) |

### Testing Intangible

**Verify Damage Reduction:**
```java
// Give player Intangible
addToBot(new ApplyPowerAction(
    player,
    player,
    new IntangiblePlayerPower(player, 1),
    1
));

// Deal large damage
addToBot(new DamageAction(
    player,
    new DamageInfo(enemy, 100, DamageInfo.DamageType.NORMAL),
    AttackEffect.BLUNT_HEAVY
));

// Player should take exactly 1 damage
```

**Verify Turn Decay (Player):**
```java
// Apply at start of player turn
addToBot(new ApplyPowerAction(
    player, player,
    new IntangiblePlayerPower(player, 1),
    1
));

// Intangible active during player turn AND enemy turn
// Decrements at end of round (after all enemies act)
// Next player turn: Intangible is gone
```

**Verify justApplied (Monster):**
```java
// Apply during monster's turn
addToBot(new ApplyPowerAction(
    monster, monster,
    new IntangiblePower(monster, 1),
    1
));

// Monster turn ends: justApplied = false, amount still 1
// Next monster turn: amount decrements to 0, power removed
// Effectively lasts for 2 turn-end checks
```

**Verify Multi-Hit Interaction:**
```java
// Give player Intangible (1 turn)
addToBot(new ApplyPowerAction(
    player, player,
    new IntangiblePlayerPower(player, 1),
    1
));

// Multi-attack: 5 hits of 10 damage each
for (int i = 0; i < 5; i++) {
    addToBot(new DamageAction(
        player,
        new DamageInfo(enemy, 10, DamageInfo.DamageType.NORMAL),
        AttackEffect.SLASH_DIAGONAL
    ));
}

// Player takes 5 damage total (1 per hit)
// Intangible still active (doesn't consume per hit, only per turn)
```

## Common Pitfalls

1. **Wrong Power ID for Player**: Use `"IntangiblePlayer"`, not `"Intangible"` for player. Wrong ID will cause turn decay to not work correctly.

2. **Turn Timing Confusion**:
   - Player version: Lasts through entire round (player + all enemy turns)
   - Monster version: Has grace period on application turn

3. **Not True Damage Immunity**: Intangible still allows 1 damage per hit. Against multi-hit attacks, this can accumulate quickly.

4. **Priority Matters**: Priority 75 means Intangible runs late. Other powers that reduce damage execute first.

5. **Doesn't Prevent HP Loss**: Effects that cause HP loss without dealing damage (e.g., some relics, powers) bypass Intangible.

6. **justApplied Flag**: Only monster version has this. Custom implementations for players should NOT include `justApplied` logic.

## Related Powers
- **Buffer**: Negates damage completely (per hit)
- **Plated Armor**: Generates Block instead of reducing damage
- **Wraith Form** (Silent card): Grants IntangiblePlayer for 2 turns, adds Dexterity loss

## Cards/Relics Granting Intangible
- **Wraith Form** (Silent): IntangiblePlayer for 2-3 turns
- **Apparition** (colorless event card): IntangiblePlayer for 1 turn
- **Incense Burner** (relic): IntangiblePlayer for 1 turn every 6 turns
- Monsters: Heart (boss), Nemesis (elite)
