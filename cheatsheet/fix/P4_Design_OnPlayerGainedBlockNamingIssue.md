# P4_Design_OnPlayerGainedBlockNamingIssue

## Bug Classification
- **Priority**: P4 (Design Issue)
- **Category**: Design - Misleading API naming
- **Affects**: Mod developers creating Powers or using block-related hooks
- **Vanilla Impact**: None (unused hook in vanilla Powers)
- **Mod Impact**: Moderate (confusing semantics, unexpected behavior)

## Summary

The `onPlayerGainedBlock()` hook in `AbstractPower` has a misleading name and semantics. Despite its name suggesting it only triggers when the **Player** gains block, it actually triggers when **any creature** (Player or Monster) gains block. This is because `AbstractCreature.addBlock()` calls this hook on all Monster Powers regardless of who is gaining block.

## Root Cause Analysis

### Code Location: AbstractCreature.addBlock()

**File**: `com/megacrit/cardcrawl/core/AbstractCreature.java`
**Lines**: 478-509

```java
public void addBlock(int blockAmount) {
    float tmp = blockAmount;

    // When Player gains block: trigger Player's Relics and Powers
    if (this.isPlayer) {
        // Call onPlayerGainedBlock on Player's Relics
        for (AbstractRelic r : AbstractDungeon.player.relics) {
            tmp = r.onPlayerGainedBlock(tmp);  // ✅ Makes sense for Relics
        }

        // Call onGainedBlock on Player's Powers
        if (tmp > 0.0F) {
            for (AbstractPower p : this.powers) {
                p.onGainedBlock(tmp);  // ✅ Correct hook for Player Powers
            }
        }
    }

    boolean effect = false;
    if (this.currentBlock == 0) {
        effect = true;
    }

    // ⚠️ DESIGN ISSUE: Called on Monster Powers regardless of who is gaining block
    for (AbstractMonster m : (AbstractDungeon.getCurrRoom()).monsters.monsters) {
        for (AbstractPower p : m.powers) {
            tmp = p.onPlayerGainedBlock(tmp);  // ❌ Misleading name
        }
    }

    this.currentBlock += MathUtils.floor(tmp);
    // ... rest of method
}
```

### The Problem

**When Player gains block**:
1. Player's Relics get `onPlayerGainedBlock(tmp)` called ✅ Correct
2. Player's Powers get `onGainedBlock(tmp)` called ✅ Correct
3. Monster's Powers get `onPlayerGainedBlock(tmp)` called ✅ Makes sense (Monsters react to Player blocking)

**When Monster gains block** (e.g., JawWorm's Bellow attack):
1. Monster's Powers get `onPlayerGainedBlock(tmp)` called ❌ **Semantically incorrect**
   - Method name says "PlayerGained" but Player didn't gain block
   - Confusing for mod developers
   - No way to distinguish between Player block and Monster block

### Why This Exists

**Vanilla Usage**:
- `AbstractPower.onPlayerGainedBlock()`: Default implementation, **no vanilla Powers override this**
- `AbstractRelic.onPlayerGainedBlock()`: Default implementation, **only Orichalcum overrides this**

**Orichalcum Relic** (only vanilla user):
```java
// Orichalcum.java:42-48
public int onPlayerGainedBlock(float blockAmount) {
    if (blockAmount > 0.0F) {
        stopPulse();  // Stop pulsing when Player gains block
    }
    return MathUtils.floor(blockAmount);
}
```

The hook appears to be **designed for Relics**, not Powers. The fact that it's called on Monster Powers seems like either:
1. **Future-proofing** for Powers that might need to react to any block gains
2. **Design oversight** where the hook was copied from Relics without adjusting semantics
3. **Intentional global monitoring** where Monster Powers can observe all block gains

## Affected Game Elements

### Vanilla Code
- **Monsters with block**: JawWorm, TheGuardian, Hexaghost, GremlinTsundere, SphericGuardian, SpireShield, Looter, Mugger, TheCollector
- **Relics using hook**: Orichalcum (only one)
- **Powers using hook**: None in vanilla

### Mod Scenarios

**Scenario 1: Mod creates Power for Monsters**
```java
// Mod Power that gives Monsters a reactive effect
public class CounterPower extends AbstractPower {
    @Override
    public int onPlayerGainedBlock(float blockAmount) {
        // Intended: Trigger when Player blocks
        // Actual: ALSO triggers when this Monster blocks!
        addToTop(new DamageAction(AbstractDungeon.player,
            new DamageInfo(owner, 5, DamageType.THORNS)));
        return MathUtils.floor(blockAmount);
    }
}
```

**Problem**: If this Power is given to a Monster that can block (e.g., JawWorm), it will trigger when the Monster itself blocks, not just when the Player blocks.

**Scenario 2: Mod uses hook to modify block amount**
```java
public class BlockReductionPower extends AbstractPower {
    @Override
    public int onPlayerGainedBlock(float blockAmount) {
        // Intended: Reduce Player's block gain
        // Actual: Also reduces Monster block gains!
        return MathUtils.floor(blockAmount * 0.5f);  // 50% block
    }
}
```

**Problem**: If applied to a Monster, it will reduce that Monster's own block gains.

## Technical Deep Dive

### Hook Comparison

| Hook Name | Defined In | Called On | Semantics |
|-----------|-----------|-----------|-----------|
| `onGainedBlock(float)` | AbstractPower | Owner's Powers only | "I gained block" |
| `onPlayerGainedBlock(float)` | AbstractPower | All Monster Powers | "Someone gained block" (misleading name) |
| `onPlayerGainedBlock(float)` | AbstractRelic | Player's Relics only | "Player gained block" (correct) |

### Correct Design (if following naming convention)

```java
// AbstractCreature.addBlock() - How it SHOULD work if name matches semantics
public void addBlock(int blockAmount) {
    float tmp = blockAmount;

    if (this.isPlayer) {
        // Player-specific triggers
        for (AbstractRelic r : AbstractDungeon.player.relics) {
            tmp = r.onPlayerGainedBlock(tmp);
        }
        if (tmp > 0.0F) {
            for (AbstractPower p : this.powers) {
                p.onGainedBlock(tmp);
            }
        }

        // Monster Powers react to PLAYER gaining block
        for (AbstractMonster m : (AbstractDungeon.getCurrRoom()).monsters.monsters) {
            for (AbstractPower p : m.powers) {
                tmp = p.onPlayerGainedBlock(tmp);  // ✅ Makes sense here
            }
        }
    } else {
        // Monster gaining block - should have separate hook
        for (AbstractPower p : this.powers) {
            tmp = p.onGainedBlock(tmp);  // Or onMonsterGainedBlock?
        }
        // Monster Powers should NOT call onPlayerGainedBlock here
    }

    this.currentBlock += MathUtils.floor(tmp);
}
```

## Patch Options

### Option 1: No Patch (Document Only) ⭐ **RECOMMENDED**

**Approach**: Document this design quirk for mod developers

**Pros**:
- No compatibility issues
- No risk of breaking existing mods
- Simple to implement

**Cons**:
- Issue persists for future mod developers
- Name remains misleading

**Implementation**: Add to mod documentation/wiki
```
IMPORTANT: onPlayerGainedBlock() is called on Monster Powers
whenever ANY creature gains block, not just when Player gains block.

If you override this method in a Power applied to Monsters:
1. It will trigger when the Monster itself blocks
2. You must check if you want Player-only behavior:
   if (AbstractDungeon.player != this.owner) {
       // Player gained block
   } else {
       // Monster (this.owner) gained block
   }
```

**Patch Code**: N/A (documentation only)

---

### Option 2: Add isPlayer Parameter

**Approach**: Patch `addBlock()` to pass context about who is gaining block

**Pros**:
- Provides clear context to Power implementations
- Backward compatible (can use parameter to filter)

**Cons**:
- Changes hook signature (mods must update)
- Requires patching AbstractPower and all implementations

**Implementation**:
```java
// Patch AbstractCreature.addBlock()
@SpirePatch(
    clz = AbstractCreature.class,
    method = "addBlock",
    paramtypez = {int.class}
)
public class AddBlockPatch {
    @SpireInsertPatch(
        locator = Locator.class
    )
    public static void Insert(AbstractCreature __instance, int blockAmount) {
        // Replace lines 494-498
        if (!__instance.isPlayer) {
            return;  // Don't call onPlayerGainedBlock for Monster block
        }
    }

    private static class Locator extends SpireInsertLocator {
        @Override
        public int[] Locate(CtBehavior ctMethodToPatch) throws Exception {
            // Locate line 494 (Monster Power loop)
            Matcher matcher = new Matcher.MethodCallMatcher(
                AbstractMonster.class, "monsters"
            );
            return LineFinder.findInOrder(ctMethodToPatch, matcher);
        }
    }
}
```

**Side Effects**:
- Monster Powers will no longer be notified when Player gains block
- **BREAKS intended design if Monster Powers are supposed to react to Player blocking**

**Verdict**: ❌ **NOT RECOMMENDED** - Likely breaks intended functionality

---

### Option 3: Add Separate onAnyCreatureGainedBlock Hook

**Approach**: Create new hook with clear semantics, deprecate misleading name

**Pros**:
- Clear semantics
- Provides both specific and general hooks
- Mod developers can choose appropriate hook

**Cons**:
- Requires BaseMod support
- Can't patch vanilla AbstractPower (would need wrapper)
- High complexity for low benefit

**Implementation**:
```java
// In custom BaseMod extension
public abstract class AbstractPowerExtended extends AbstractPower {
    // New hooks with clear semantics
    public int onAnyCreatureGainedBlock(AbstractCreature creature, float blockAmount) {
        return MathUtils.floor(blockAmount);
    }

    public int onSpecificCreatureGainedBlock(AbstractCreature creature, float blockAmount) {
        if (creature.isPlayer) {
            return onPlayerGainedBlock(blockAmount);
        } else {
            return onMonsterGainedBlock(creature, blockAmount);
        }
    }

    public int onMonsterGainedBlock(AbstractCreature monster, float blockAmount) {
        return MathUtils.floor(blockAmount);
    }

    // Deprecated: kept for compatibility
    @Deprecated
    public int onPlayerGainedBlock(float blockAmount) {
        return MathUtils.floor(blockAmount);
    }
}
```

**Side Effects**:
- Only works for Powers extending custom class
- Doesn't fix vanilla hook confusion

**Verdict**: ⚠️ **POSSIBLE** - Only if BaseMod adds official support

---

### Option 4: Conditional Skip for Monster Self-Block

**Approach**: Skip hook when Monster is gaining block on itself

**Pros**:
- Preserves cross-creature reactivity (Monsters react to Player blocking)
- Prevents confusing self-triggers

**Cons**:
- Still doesn't fix misleading name
- Assumes self-block triggers are never intended

**Implementation**:
```java
@SpirePatch(
    clz = AbstractCreature.class,
    method = "addBlock"
)
public class FixMonsterSelfBlockTrigger {
    @SpireInsertPatch(locator = Locator.class)
    public static SpireReturn<Void> Insert(AbstractCreature __instance) {
        // Skip Monster Power loop if Monster is gaining block on itself
        if (!__instance.isPlayer) {
            return SpireReturn.Return(null);  // Skip lines 494-498
        }
        return SpireReturn.Continue();  // Continue normally for Player
    }

    private static class Locator extends SpireInsertLocator {
        @Override
        public int[] Locate(CtBehavior ctMethodToPatch) throws Exception {
            Matcher matcher = new Matcher.FieldAccessMatcher(
                AbstractDungeon.class, "monsters"
            );
            return LineFinder.findInOrder(ctMethodToPatch, matcher);
        }
    }
}
```

**Side Effects**:
- **BREAKS Monster Powers that need to react to Monster block gains**
- Changes global behavior - untested implications

**Verdict**: ⚠️ **RISKY** - Only if confident Monster Powers shouldn't see Monster blocks

## Recommended Approach

### For Mod Developers

**If creating Powers for Monsters**:
```java
@Override
public int onPlayerGainedBlock(float blockAmount) {
    // Filter to only react when PLAYER gains block, not when this Monster blocks
    if (AbstractDungeon.player.currentBlock != AbstractDungeon.player.currentBlock + blockAmount) {
        // Unreliable - currentBlock already updated
    }

    // Better: Check if we're in a Monster's turn
    if (!GameActionManager.turn) {
        // Player's turn - Player likely gained block
    }

    // Best: Accept that this hook triggers for all block gains
    // and design Powers accordingly
}
```

**Best Practice**:
- **Avoid using `onPlayerGainedBlock` in Monster Powers** unless you want global block monitoring
- Use `onGainedBlock` for self-block reactions (only triggers for owner)
- Document clearly if your Power reacts to all block gains

### For Bug Fix Mod Authors

**Recommendation**: **Option 1 - Document Only**

**Rationale**:
1. No vanilla Powers use this hook (zero impact on vanilla)
2. Unknown if mods rely on current behavior
3. Changing behavior risks breaking existing mods
4. Issue is primarily about naming, not functionality
5. Extremely low severity (P4)

**Action Items**:
1. Add warning to mod development guides
2. Document in BaseMod wiki
3. Consider proposing official hook addition in BaseMod API

## Verification Steps

If you choose to patch this:

1. **Test with vanilla Monsters that block**:
   - JawWorm's Bellow attack (gains 6 block + 3 Strength)
   - TheGuardian's defensive mode
   - Verify Monster Powers don't trigger incorrectly

2. **Test cross-creature reactivity**:
   - Create Monster Power that should react to Player blocking
   - Verify it still triggers when Player gains block
   - Ensure it doesn't break intended mechanics

3. **Test mod compatibility**:
   - Search Steam Workshop for mods using onPlayerGainedBlock
   - Test with Minty Spire, Hubris, Downfall (large mods with custom Powers)
   - Verify no behavioral regressions

4. **Test edge cases**:
   - Multiple Monsters blocking simultaneously (AoE block effects)
   - Block amount modifications (does modified amount propagate correctly?)
   - Frail reducing block (should still trigger hooks)

## Related Issues

- **P2_Combat_MonsterWasHPLostTimingBug**: Similar inconsistency between Monster and Player hooks
- **AbstractPower.onGainedBlock**: Correct hook for self-block (only triggers for owner)
- **AbstractRelic.onPlayerGainedBlock**: Same name but only called for Player Relics (consistent)

## Additional Notes

### Why This Isn't Higher Priority

- **No vanilla impact**: No vanilla Powers use this hook
- **Mod impact is moderate**: Mods can work around it with documentation
- **Risk > benefit**: Patching could break more than it fixes
- **Workarounds exist**: Mod developers can check context in their implementations

### Future Considerations

If BaseMod adds official support for extended hooks:
```java
// Proposed BaseMod API additions
public interface BlockGainSubscriber {
    void receiveOnBlockGained(AbstractCreature creature, int amount);
}

// Separated hooks for clarity
public interface PlayerBlockGainSubscriber {
    void receiveOnPlayerBlockGained(int amount);
}

public interface MonsterBlockGainSubscriber {
    void receiveOnMonsterBlockGained(AbstractMonster monster, int amount);
}
```

This would provide clean, well-named hooks without modifying vanilla behavior.

## Conclusion

This is a **low-priority design issue** best addressed through **documentation** rather than code patches. The misleading name can confuse mod developers, but the functionality itself may be intentional for global block monitoring. Recommend documenting this quirk prominently in mod development guides and considering official BaseMod API additions for clearer semantics.

**Patch Recommendation**: ⭐ **Option 1 - Document Only** (no code changes)
