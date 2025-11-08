# Artifact Power

## Overview
- **Power ID**: `"Artifact"`
- **Type**: `BUFF` (defensive)
- **Class**: `ArtifactPower`
- **Source**: `com.megacrit.cardcrawl.powers.ArtifactPower`

## Description
Negates the next debuff(s) applied to the owner. Each debuff blocked consumes 1 stack of Artifact.

## Key Mechanics

### Debuff Blocking Mechanism
Artifact doesn't directly implement debuff blocking in its own class. The blocking logic is implemented in `ApplyPowerAction.update()`:

```java
// ApplyPowerAction.java:196-206
if (this.target.hasPower("Artifact") &&
    this.powerToApply.type == AbstractPower.PowerType.DEBUFF) {
    addToTop((AbstractGameAction)new TextAboveCreatureAction(this.target, TEXT[0]));
    this.duration -= Gdx.graphics.getDeltaTime();
    CardCrawlGame.sound.play("NULLIFY_SFX");
    this.target.getPower("Artifact").flashWithoutSound();
    this.target.getPower("Artifact").onSpecificTrigger();
    return;  // Debuff application is cancelled
}
```

**Critical Points:**
1. Check occurs during `ApplyPowerAction` before the debuff is applied
2. Any power with `type == PowerType.DEBUFF` is blocked
3. The debuff is completely prevented (action returns early)
4. Artifact consumes 1 stack via `onSpecificTrigger()`

### Stack Reduction
```java
public void onSpecificTrigger() {
    if (this.amount <= 0) {
        addToTop((AbstractGameAction)new RemoveSpecificPowerAction(this.owner, this.owner, "Artifact"));
    } else {
        addToTop((AbstractGameAction)new ReducePowerAction(this.owner, this.owner, "Artifact", 1));
    }
}
```

**Behavior:**
- Called when a debuff is blocked
- Reduces Artifact by 1 stack
- If amount reaches 0, removes the power entirely

## Implementation Details

### Constructor
```java
public ArtifactPower(AbstractCreature owner, int amount) {
    this.name = NAME;
    this.ID = "Artifact";
    this.owner = owner;
    this.amount = amount;
    updateDescription();
    loadRegion("artifact");
    this.type = AbstractPower.PowerType.BUFF;
}
```

### Description Updates
```java
public void updateDescription() {
    if (this.amount == 1) {
        this.description = DESCRIPTIONS[0] + this.amount + DESCRIPTIONS[1];  // singular
    } else {
        this.description = DESCRIPTIONS[0] + this.amount + DESCRIPTIONS[2];  // plural
    }
}
```

## Mod Implementation Notes

### Creating Artifact Power
```java
// Grant Artifact to player
addToBot(new ApplyPowerAction(
    AbstractDungeon.player,  // target
    AbstractDungeon.player,  // source
    new ArtifactPower(AbstractDungeon.player, 2),  // 2 stacks
    2
));
```

### Important Interactions

**What Artifact Blocks:**
- Any power with `type = PowerType.DEBUFF`
- Examples: Weak, Vulnerable, Frail, Poison, Entangled, Bias

**What Artifact Does NOT Block:**
- Powers with `type = PowerType.BUFF`
- Direct damage (not a debuff application)
- HP loss effects (not power-based)
- Stat reductions that aren't debuffs (e.g., Strength loss from Flex wearing off)

### Relic Interactions
From `ApplyPowerAction`:
- **Champion Belt**: Checks `!target.hasPower("Artifact")` before triggering (line 160)
- This ensures Artifact can protect from debuffs that have bonus effects

### Testing Artifact

**Verify Debuff Blocking:**
```java
// Apply Artifact first
addToBot(new ApplyPowerAction(player, player, new ArtifactPower(player, 1), 1));

// Try to apply debuff - should be blocked
addToBot(new ApplyPowerAction(player, enemy, new WeakPower(player, 2, false), 2));

// Artifact should be consumed, debuff should not be applied
```

**Verify Stack Consumption:**
```java
// Apply 3 stacks
addToBot(new ApplyPowerAction(player, player, new ArtifactPower(player, 3), 3));

// Apply 3 debuffs
addToBot(new ApplyPowerAction(player, enemy, new WeakPower(player, 2, false), 2));
addToBot(new ApplyPowerAction(player, enemy, new VulnerablePower(player, 2, false), 2));
addToBot(new ApplyPowerAction(player, enemy, new FrailPower(player, 2, false), 2));

// All debuffs blocked, Artifact should be removed
// 4th debuff would apply normally
```

## Common Pitfalls

1. **Debuff Type Classification**: Only powers marked as `PowerType.DEBUFF` are blocked. Custom debuffs must set `type = PowerType.DEBUFF` in constructor.

2. **Timing**: Artifact check happens in `ApplyPowerAction`, not in the power class itself. Direct power manipulation bypassing `ApplyPowerAction` won't trigger Artifact.

3. **Order of Operations**: The debuff is prevented before `onApplyPower` hooks are called on the debuff itself.

4. **Visual Feedback**: When Artifact blocks a debuff:
   - "NULLIFY" text appears above creature
   - "NULLIFY_SFX" sound plays
   - Artifact icon flashes
   - Artifact stack decrements

## Related Powers
- **Buffer**: Prevents fatal damage
- **Intangible**: Reduces all damage to 1
- Powers that grant Artifact: Incense Burner (relic), Panacea (card)
