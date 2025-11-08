# P2_Card_MakeTempCardInDiscardMissingElseBug

## Bug Classification
- **Priority**: P2 (Major - Missing functionality)
- **Category**: Card - Card generation logic
- **Affects**: Discard pile card generation when amount >= 6
- **Vanilla Impact**: Low (few effects create 6+ cards to discard)
- **Mod Impact**: Moderate (mods creating many cards to discard fail silently)

## Summary

`MakeTempCardInDiscardAction` has a **missing else clause** at line 42-46 that causes **zero cards to be generated** when `numCards >= 6`. The if-statement only handles `numCards < 6`, with no code path for the >= 6 case.

**Example**:
- Amount = 10
- Expected: 10 cards added to discard pile
- Actual: **0 cards generated**

## Root Cause Analysis

### Code Location: MakeTempCardInDiscardAction.update()

**File**: `com/megacrit/cardcrawl/actions/common/MakeTempCardInDiscardAction.java`
**Lines**: 39-51

```java
public void update() {
    if (this.duration == this.startDuration) {

        if (this.numCards < 6) {  // ⚠️ Only handles numCards < 6
            for (int i = 0; i < this.numCards; i++) {
                AbstractDungeon.effectList.add(new ShowCardAndAddToDiscardEffect(makeNewCard()));
            }
        }
        // ❌ NO ELSE CLAUSE!
        // If numCards >= 6, nothing happens!

        this.duration -= Gdx.graphics.getDeltaTime();
    }

    tickDuration();
}
```

### Comparison with Similar Actions

**MakeTempCardInDrawPileAction** (CORRECT implementation):
```java
// com/megacrit/cardcrawl/actions/common/MakeTempCardInDrawPileAction.java:59-81
if (this.amount < 6) {
    for (int i = 0; i < this.amount; i++) {
        // ... creates card with full animation
    }
} else {  // ✅ HAS ELSE CLAUSE!
    for (int i = 0; i < this.amount; i++) {
        // ... creates card with simplified animation
    }
}
```

**MakeTempCardInDiscardAction** (BUGGY implementation):
```java
if (this.numCards < 6) {
    for (int i = 0; i < this.numCards; i++) {
        // ... creates card
    }
}
// ❌ NO ELSE - amount >= 6 creates nothing!
```

### Intended vs Actual Behavior

**Intended Design Pattern** (inferred from MakeTempCardInDrawPileAction):
- `amount < 6`: Use detailed animation for each card
- `amount >= 6`: Use simplified animation for performance

**Actual Behavior**:
- `numCards < 6`: Cards created with animation ✓
- `numCards >= 6`: **No cards created at all** ❌

## Execution Flow Analysis

**Scenario 1: numCards = 3 (Normal case)**
```
1. if (this.numCards < 6)? YES (3 < 6)
2. for (i = 0; i < 3; i++)
   → ShowCardAndAddToDiscardEffect called 3 times
3. Result: 3 cards added to discard pile ✓
```

**Scenario 2: numCards = 10 (Bug case)**
```
1. if (this.numCards < 6)? NO (10 >= 6)
2. No else clause, nothing executes
3. Result: 0 cards added to discard pile ❌
```

**Scenario 3: numCards = 6 (Edge case)**
```
1. if (this.numCards < 6)? NO (6 >= 6)
2. No else clause, nothing executes
3. Result: 0 cards added to discard pile ❌
```

## Affected Game Elements

### Vanilla Cards Using This Action

Searching for vanilla usage:
```bash
grep -r "MakeTempCardInDiscardAction" com/megacrit/cardcrawl/cards/
```

**Vanilla cards that could be affected**:
- Most vanilla cards create 1-3 cards at a time
- Very few (if any) create 6+ cards to discard pile
- **Likely why bug went unnoticed**

### Mod Scenarios

**Scenario 1: Mass card generation mod**
```java
// Mod creates 10 Wounds to discard pile
addToBot(new MakeTempCardInDiscardAction(new Wound(), 10));
// Expected: 10 Wounds in discard
// Actual: 0 Wounds ❌ Silent failure!
```

**Scenario 2: X-cost effect to discard**
```java
// Card: "Add X random cards to discard pile"
int x = EnergyPanel.totalCount;  // Assume X = 8
addToBot(new MakeTempCardInDiscardAction(randomCard, x));
// Expected: 8 cards in discard
// Actual: 0 cards ❌ if X >= 6
```

## Technical Deep Dive

### Why This Bug Exists

**Theory 1: Copy-paste error**
- Developer copied from MakeTempCardInDrawPileAction
- Deleted else clause thinking it wasn't needed for discard
- Forgot that else clause actually generates the cards

**Theory 2: Performance optimization gone wrong**
- Intended to skip animation for 6+ cards
- But forgot to actually add cards without animation

**Theory 3: Incomplete implementation**
- Planned to add else clause later
- Never finished implementation

### Why Not Discovered

1. **Rare usage**: Very few vanilla effects create 6+ cards to discard
2. **Silent failure**: No error message, just no cards
3. **Not obviously broken**: Game doesn't crash, just behaves oddly
4. **Specific threshold**: Only affects amount >= 6, uncommon value

## Patch Options

### Option 1: Add Else Clause with Simplified Animation ⭐ **RECOMMENDED**

**Approach**: Match MakeTempCardInDrawPileAction pattern

**Pros**:
- Matches vanilla design pattern
- Fixes bug completely
- Performance-conscious for large amounts

**Cons**:
- None

**Implementation**:
```java
@SpirePatch(
    clz = MakeTempCardInDiscardAction.class,
    method = "update"
)
public class FixMissingElseClause {
    @SpirePrefixPatch
    public static SpireReturn<Void> ReplaceUpdate(MakeTempCardInDiscardAction __instance) {
        // Get private fields
        float duration = ReflectionHacks.getPrivate(__instance, AbstractGameAction.class, "duration");
        float startDuration = ReflectionHacks.getPrivate(__instance, AbstractGameAction.class, "startDuration");
        int numCards = ReflectionHacks.getPrivate(__instance, MakeTempCardInDiscardAction.class, "numCards");
        AbstractCard c = ReflectionHacks.getPrivate(__instance, MakeTempCardInDiscardAction.class, "c");
        boolean sameUUID = ReflectionHacks.getPrivate(__instance, MakeTempCardInDiscardAction.class, "sameUUID");

        if (duration == startDuration) {
            if (numCards < 6) {
                // Original behavior: detailed animation
                for (int i = 0; i < numCards; i++) {
                    AbstractCard newCard = sameUUID ?
                        c.makeSameInstanceOf() :
                        c.makeStatEquivalentCopy();
                    AbstractDungeon.effectList.add(
                        new ShowCardAndAddToDiscardEffect(newCard));
                }
            } else {
                // ✅ FIX: Add else clause for numCards >= 6
                for (int i = 0; i < numCards; i++) {
                    AbstractCard newCard = sameUUID ?
                        c.makeSameInstanceOf() :
                        c.makeStatEquivalentCopy();
                    // Use simplified constructor for performance
                    AbstractDungeon.effectList.add(
                        new ShowCardAndAddToDiscardEffect(newCard,
                            Settings.WIDTH / 2.0F, Settings.HEIGHT / 2.0F));
                }
            }

            duration -= Gdx.graphics.getDeltaTime();
            ReflectionHacks.setPrivate(__instance, AbstractGameAction.class, "duration", duration);
        }

        // Call tickDuration()
        ReflectionHacks.privateMethod(AbstractGameAction.class, "tickDuration")
            .invoke(__instance);

        return SpireReturn.Return(null);
    }
}
```

**Side Effects**:
- None - pure bug fix

**Verdict**: ⭐ **RECOMMENDED** - Clean fix with zero downsides

---

### Option 2: Remove Threshold, Always Use Loop

**Approach**: Remove `if (numCards < 6)` check, always create cards

**Pros**:
- Simpler code
- Works for all amounts

**Cons**:
- Potential performance impact for large amounts
- Doesn't match vanilla pattern

**Implementation**:
```java
@SpirePatch(
    clz = MakeTempCardInDiscardAction.class,
    method = "update"
)
public class RemoveThreshold {
    @SpirePrefixPatch
    public static SpireReturn<Void> ReplaceUpdate(MakeTempCardInDiscardAction __instance) {
        float duration = ReflectionHacks.getPrivate(__instance, AbstractGameAction.class, "duration");
        float startDuration = ReflectionHacks.getPrivate(__instance, AbstractGameAction.class, "startDuration");
        int numCards = ReflectionHacks.getPrivate(__instance, MakeTempCardInDiscardAction.class, "numCards");
        AbstractCard c = ReflectionHacks.getPrivate(__instance, MakeTempCardInDiscardAction.class, "c");
        boolean sameUUID = ReflectionHacks.getPrivate(__instance, MakeTempCardInDiscardAction.class, "sameUUID");

        if (duration == startDuration) {
            // Always create cards, no threshold check
            for (int i = 0; i < numCards; i++) {
                AbstractCard newCard = sameUUID ?
                    c.makeSameInstanceOf() :
                    c.makeStatEquivalentCopy();
                AbstractDungeon.effectList.add(
                    new ShowCardAndAddToDiscardEffect(newCard));
            }

            duration -= Gdx.graphics.getDeltaTime();
            ReflectionHacks.setPrivate(__instance, AbstractGameAction.class, "duration", duration);
        }

        ReflectionHacks.privateMethod(AbstractGameAction.class, "tickDuration")
            .invoke(__instance);

        return SpireReturn.Return(null);
    }
}
```

**Side Effects**:
- Slight performance degradation for 6+ cards
- Not noticeable in practice

**Verdict**: ⚠️ **ACCEPTABLE** - Works but doesn't match design pattern

---

### Option 3: Document Only (No Patch)

**Approach**: Warn mod developers about the limitation

**Pros**:
- No code changes
- No compatibility issues

**Cons**:
- Bug persists for all users
- Mods continue to fail silently

**Implementation**: Add to modding documentation:
```
WARNING: MakeTempCardInDiscardAction has a bug where numCards >= 6
creates zero cards. If you need to create 6+ cards to discard pile:
1. Use multiple actions with amount < 6
2. Use MakeTempCardInDrawPileAction instead
3. Manually add cards to AbstractDungeon.player.discardPile
```

**Verdict**: ❌ **NOT RECOMMENDED** - Bug should be fixed

## Verification Steps

1. **Create test mod**:
   ```java
   public void test() {
       logger.info("Discard pile size before: " +
           AbstractDungeon.player.discardPile.size());

       addToBot(new MakeTempCardInDiscardAction(new Strike_R(), 10));

       // Wait for action to complete
       addToBot(new AbstractGameAction() {
           public void update() {
               logger.info("Discard pile size after: " +
                   AbstractDungeon.player.discardPile.size());
               isDone = true;
           }
       });
   }
   ```
   - Expected (buggy): Discard pile size unchanged
   - Expected (fixed): Discard pile size +10

2. **Edge case test**:
   - Test with amount = 5: Should work
   - Test with amount = 6: Should fail (bug)
   - Test with amount = 7: Should fail (bug)

3. **Performance test** (after fix):
   - Create 100 cards to discard
   - Verify no frame drops
   - Confirm simplified animation is used

## Related Issues

- **MakeTempCardInDrawPileAction**: Correct implementation (has else clause)
- **MakeTempCardInHandAction**: Different bug (switch fall-through, not missing else)

## Additional Notes

### Severity Assessment

- **Priority P2** (not P1) because:
  1. Very rare in vanilla gameplay
  2. Only affects 6+ cards threshold
  3. Silent failure (no crash)
  4. Workarounds available

- **Would be P1 if**:
  1. Vanilla cards commonly created 6+ to discard
  2. Caused crashes instead of silent failure
  3. No workarounds existed

### Design Intent vs Implementation

**Likely intended behavior**:
```
amount < 6: Full animation for visual clarity
amount >= 6: Simplified animation for performance
```

**Actual buggy behavior**:
```
amount < 6: Full animation ✓
amount >= 6: No cards created ❌
```

## Conclusion

This is a **confirmed bug** with **certain fix**:
- Code clearly missing else clause
- Behavior is wrong (0 cards vs expected amount)
- Fix is straightforward (add else clause)
- No ambiguity like MakeTempCardInHandAction

**Patch Recommendation**: ⭐ **Option 1 - Add Else Clause**

**Priority**: P2 (Major bug, but rare occurrence)

**Confidence**: 100% (bug is certain, fix is simple)
