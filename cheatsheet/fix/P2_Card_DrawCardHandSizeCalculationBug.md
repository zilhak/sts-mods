# P2_Card_DrawCardHandSizeCalculationBug

## Bug Classification
- **Priority**: P2 (Major - Logic Error)
- **Category**: Card - Draw calculation
- **Affects**: Draw effects when hand is near full (8-9 cards)
- **Vanilla Impact**: Moderate - Wrong calculation but accidentally works due to line 104 check
- **Mod Impact**: High - If hand size limit is modified, this breaks completely

## Summary

DrawCardAction has a **mathematically incorrect** hand size overflow calculation at lines 114-117. The formula `amount += (10 - amount + hand.size())` is wrong and should be `amount = 10 - hand.size()`.

However, this bug is **accidentally masked** by the hard limit check at line 104 that stops drawing when hand size reaches exactly 10. The buggy calculation only runs when `amount + hand.size() > 10` but hand hasn `t reached 10 yet, creating situations where the calculation is wrong but the line 104 check prevents the incorrect behavior from manifesting.

This becomes a **critical bug** for mods that:
1. Change the hand size limit (e.g., custom relics that increase max hand size)
2. Bypass the hard limit check
3. Rely on accurate draw amount calculations

## Root Cause Analysis

### Code Location: DrawCardAction.update()

**File**: `com/megacrit/cardcrawl/actions/common/DrawCardAction.java`
**Lines**: 104-118

```java
// Line 104-110: Hard limit check (executes BEFORE calculation)
if (AbstractDungeon.player.hand.size() == 10) {
    AbstractDungeon.player.createHandIsFullDialog();
    endActionWithFollowUp();
    return;  // ✅ Prevents drawing when hand is full
}

// Line 113-118: Overflow calculation (only runs if hand.size() < 10)
if (!this.shuffleCheck) {
    // ❌ BUG: Wrong calculation!
    if (this.amount + AbstractDungeon.player.hand.size() > 10) {
        int handSizeAndDraw = 10 - this.amount + AbstractDungeon.player.hand.size();
        this.amount += handSizeAndDraw;
        AbstractDungeon.player.createHandIsFullDialog();
    }

    // Shuffle handling...
    if (this.amount > deckSize) {
        // ...
    }
    this.shuffleCheck = true;
}
```

### Mathematical Analysis

**Given**:
- `H` = current hand size (0-9, since line 104 prevents H=10)
- `A` = requested draw amount
- `MAX` = 10 (hand size limit)

**Goal**: Calculate how many cards can actually be drawn
- Correct formula: `actualDraw = min(A, MAX - H) = min(A, 10 - H)`

**Current Code** (line 115-116):
```java
int handSizeAndDraw = 10 - this.amount + AbstractDungeon.player.hand.size();
this.amount += handSizeAndDraw;
```

**Expanding**:
```
handSizeAndDraw = 10 - A + H
newAmount = A + handSizeAndDraw
          = A + (10 - A + H)
          = 10 + H  // ❌ WRONG!
```

**Result**: `amount` becomes `10 + H`, which is **always greater than the correct value** `10 - H`.

### Example Scenarios

**Scenario 1: Hand has 8 cards, try to draw 5**
```
H = 8, A = 5
Condition: A + H = 13 > 10  ✓ (enters if-block)

Buggy calculation:
handSizeAndDraw = 10 - 5 + 8 = 13
amount = 5 + 13 = 18  ❌ (should be 2)

What actually happens:
- Line 114-117: Sets amount = 18
- Line 119-129: amount (18) > deckSize, triggers shuffle logic
- Recursive DrawCardAction created with wrong amount
- Line 104 check stops it when hand reaches 10
- Result: Draws 2 cards (correct, but by accident)
```

**Scenario 2: Hand has 9 cards, try to draw 3**
```
H = 9, A = 3
Condition: A + H = 12 > 10  ✓ (enters if-block)

Buggy calculation:
handSizeAndDraw = 10 - 3 + 9 = 16
amount = 3 + 16 = 19  ❌ (should be 1)

What actually happens:
- Sets amount = 19
- Triggers shuffle logic (if deckSize < 19)
- Line 104 check stops drawing at 1 card
- Result: Draws 1 card (correct, but by accident)
```

**Scenario 3: Hand has 5 cards, try to draw 7**
```
H = 5, A = 7
Condition: A + H = 12 > 10  ✓ (enters if-block)

Buggy calculation:
handSizeAndDraw = 10 - 7 + 5 = 8
amount = 7 + 8 = 15  ❌ (should be 5)

What actually happens:
- Sets amount = 15
- Triggers shuffle logic
- Line 104 check stops drawing at 5 cards
- Result: Draws 5 cards (correct, but by accident)
```

### Why The Bug Doesn't Break Vanilla

**Two safety mechanisms**:

1. **Line 104-110 Hard Limit**:
   ```java
   if (AbstractDungeon.player.hand.size() == 10) {
       // Stop immediately, don't even try to draw
       endActionWithFollowUp();
       return;
   }
   ```
   - Checked **every frame** in update()
   - Prevents hand from ever exceeding 10 cards
   - Doesn't rely on `amount` being correct

2. **Line 136-147 Individual Card Draw**:
   ```java
   if (this.amount != 0 && this.duration < 0.0F) {
       this.amount--;  // Decrement one at a time
       if (!AbstractDungeon.player.drawPile.isEmpty()) {
           drawnCards.add(AbstractDungeon.player.drawPile.getTopCard());
           AbstractDungeon.player.draw();  // Draws 1 card
           // Next frame: Line 104 check runs again
       }
   }
   ```
   - Draws one card at a time per frame
   - Line 104 check runs between each card
   - Even if `amount` is wrong (e.g., 18), drawing stops when hand reaches 10

**Result**: The buggy `amount` calculation creates unnecessary shuffle operations and wrong internal state, but the final card count is correct due to the safety checks.

## Affected Game Elements

### Vanilla Behavior
- **No observable bugs** due to safety mechanisms
- **Performance impact**: Unnecessary EmptyDeckShuffleAction operations
- **Internal state**: `amount` field contains wrong value during execution

### Mod Scenarios That Break

**Scenario 1: Mod increases hand size limit**
```java
// Mod increases max hand size to 15
@SpirePatch(clz = DrawCardAction.class, method = "update")
public static class IncreaseHandSize {
    @SpireInsertPatch(locator = Locator.class, localvars = {"handSize"})
    public static void ChangeHandSizeLimit(DrawCardAction __instance, @ByRef int[] handSize) {
        handSize[0] = 15;  // Change from 10 to 15
    }
}
```

**Problem**:
- Line 104 check now uses `hand.size() == 15`
- Line 115 calculation still hardcoded to 10: `10 - amount + hand.size()`
- Bug is no longer masked
- Cards drawn = wrong amount

**Scenario 2: Mod patches hand.size() check**
```java
// Mod removes the hard limit check
@SpirePatch(clz = DrawCardAction.class, method = "update")
public static class RemoveHardLimit {
    @SpirePrefixPatch
    public static SpireReturn Prefix(DrawCardAction __instance) {
        // Skip lines 104-110
        return SpireReturn.Continue();
    }
}
```

**Problem**:
- Safety mechanism removed
- Buggy calculation now controls actual draw amount
- Hand can exceed 10 cards

**Scenario 3: Mod reads `amount` field**
```java
// Mod checks draw amount for custom effects
@SpirePatch(clz = DrawCardAction.class, method = "update")
public static class TrackDrawAmount {
    @SpirePostfixPatch
    public static void Postfix(DrawCardAction __instance) {
        int drawAmount = __instance.amount;
        // ❌ Gets wrong value (18 instead of 2)
        triggerCustomEffect(drawAmount);
    }
}
```

**Problem**:
- Reads buggy `amount` value
- Custom effects trigger with wrong parameters

## Technical Deep Dive

### Correct Formula Derivation

**Goal**: Draw as many cards as possible without exceeding hand limit

```
Available space in hand = MAX - H
Cards we want to draw = A
Cards we can draw = min(A, MAX - H)
```

**If** `A + H > MAX`:
```
actualDraw = MAX - H
```

**Else**:
```
actualDraw = A
```

**Combined**:
```java
if (this.amount + AbstractDungeon.player.hand.size() > 10) {
    this.amount = 10 - AbstractDungeon.player.hand.size();  // ✅ Correct
    // NOT: this.amount += (10 - this.amount + hand.size());  ❌ Wrong
}
```

### Why Current Formula is Wrong

**Current**:
```java
int handSizeAndDraw = 10 - this.amount + AbstractDungeon.player.hand.size();
this.amount += handSizeAndDraw;
```

**This is trying to calculate**:
"How much should I adjust `amount` by?"

**But the formula is backwards**:
- It adds `hand.size()` instead of subtracting
- It adds the adjustment to `amount` instead of replacing it
- Result: `amount` becomes inflated

**Intended logic** (guessing original intent):
```java
// They might have meant:
int overflow = (this.amount + hand.size()) - 10;  // How many cards overflow?
this.amount -= overflow;  // Reduce draw by overflow amount

// Which simplifies to:
this.amount = 10 - hand.size();  // ✅ Correct
```

## Patch Options

### Option 1: Fix The Calculation ⭐ **RECOMMENDED**

**Approach**: Replace buggy formula with correct one

**Pros**:
- Mathematically correct
- Fixes internal state issues
- Reduces unnecessary shuffle operations
- Compatible with hand size mods

**Cons**:
- Changes internal behavior (though external behavior unchanged in vanilla)

**Implementation**:
```java
@SpirePatch(
    clz = DrawCardAction.class,
    method = "update"
)
public class FixHandSizeCalculation {
    @SpireInsertPatch(
        locator = Locator.class
    )
    public static void FixCalculation(DrawCardAction __instance) {
        // Replace lines 114-117
        int currentHandSize = AbstractDungeon.player.hand.size();

        if (__instance.amount + currentHandSize > 10) {
            // ✅ CORRECT: Set amount to available space
            __instance.amount = 10 - currentHandSize;
            AbstractDungeon.player.createHandIsFullDialog();
        }
    }

    private static class Locator extends SpireInsertLocator {
        @Override
        public int[] Locate(CtBehavior ctMethodToPatch) throws Exception {
            // Locate line 114 (if statement for overflow check)
            Matcher matcher = new Matcher.FieldAccessMatcher(
                AbstractDungeon.class, "player"
            );
            return LineFinder.findInOrder(ctMethodToPatch, matcher);
        }
    }
}
```

**Side Effects**:
- `amount` field will have correct values during execution
- Fewer unnecessary shuffle operations
- **No change** to final card count in vanilla (still correct)

**Verdict**: ⭐ **RECOMMENDED** - Fixes root cause without breaking anything

---

### Option 2: Remove Buggy Calculation, Rely on Hard Limit

**Approach**: Delete lines 114-117 entirely, rely on line 104 check

**Pros**:
- Simpler code
- Still works in vanilla

**Cons**:
- Loses overflow warning dialog
- More shuffle operations when drawing near hand limit
- Doesn't fix fundamental issue

**Implementation**:
```java
@SpirePatch(
    clz = DrawCardAction.class,
    method = "update"
)
public class RemoveBuggyCalculation {
    @SpireInsertPatch(locator = Locator.class)
    public static SpireReturn SkipBuggyCode(DrawCardAction __instance) {
        // Skip lines 114-117 entirely
        int currentHandSize = AbstractDungeon.player.hand.size();

        // Just keep the shuffle check logic (lines 119-131)
        // Hard limit (line 104) will handle stopping draw

        return SpireReturn.Continue();  // Skip to shuffle check
    }
}
```

**Side Effects**:
- No "hand is full" dialog when drawing near limit
- More EmptyDeckShuffleAction operations

**Verdict**: ⚠️ **NOT RECOMMENDED** - Loses functionality

---

### Option 3: Document Only (No Patch)

**Approach**: Warn mod developers about the bug

**Pros**:
- No compatibility issues
- No risk of breaking existing behavior

**Cons**:
- Bug persists
- Mods that modify hand size will break

**Implementation**: Add to mod development wiki:
```
WARNING: DrawCardAction has a buggy hand size calculation at line 115-116.
The calculation is mathematically wrong but masked by safety checks.

If your mod:
- Changes hand size limit
- Reads DrawCardAction.amount field
- Patches the hard limit check (line 104)

You MUST apply a fix for this bug or your mod will malfunction.
```

**Verdict**: ⚠️ **ACCEPTABLE** if not creating hand size mods

## Recommended Approach

**For Bug Fix Mod**: ⭐ **Option 1 - Fix The Calculation**

**Rationale**:
1. Mathematically correct formula
2. Fixes internal state issues
3. No change to vanilla behavior (external)
4. Enables hand size mods to work correctly
5. Low risk - hard limit is still active as backup

**Action Items**:
1. Implement Option 1 patch
2. Test with various scenarios:
   - Draw 10 cards with empty hand
   - Draw 5 cards with 8 cards in hand
   - Draw 20 cards with deck shuffle required
3. Verify "hand is full" dialog still appears
4. Test compatibility with hand size mods

## Verification Steps

1. **Vanilla Behavior Test**:
   - Hand with 9 cards, play Acrobatics (draw 3)
   - Expected: Draw 1 card, show "hand is full" dialog
   - Verify: amount = 1 (not 19)

2. **Shuffle Test**:
   - Empty deck except 1 card, discard pile has 20 cards
   - Hand has 8 cards, draw 5
   - Expected: Draw 2 cards (with shuffle in between)
   - Verify: Only 1 unnecessary shuffle (not multiple)

3. **Mod Compatibility Test**:
   - Create relic that increases hand size to 15
   - Hand with 14 cards, draw 5
   - Expected: Draw 1 card
   - Verify: Works correctly with patch, breaks without

4. **Edge Cases**:
   - Draw 0 cards (should do nothing)
   - Draw negative cards (undefined behavior)
   - Draw with No Draw Power (should be blocked)

## Related Issues

- **DrawCardAction line 119-129**: Shuffle logic may have efficiency issues
- **AbstractPlayer.draw()**: Actual card drawing implementation
- **EmptyDeckShuffleAction**: Triggered unnecessarily due to wrong `amount`

## Additional Notes

### Why This Wasn't Caught

1. **Safety checks hide the bug**: Line 104 prevents manifestation
2. **Works correctly externally**: Players see correct behavior
3. **Internal state not visible**: `amount` field is private
4. **No unit tests**: Internal calculations not validated

### Performance Impact

**Unnecessary Operations Due to Bug**:
```
Scenario: Hand 8, Draw 5
Buggy amount = 18
Deck size = 15

Line 119 check: 18 > 15  ✓
→ Creates DrawCardAction(3)  // amount - deckSize
→ Creates EmptyDeckShuffleAction()
→ Creates DrawCardAction(15)

Correct amount = 2
Deck size = 15

Line 119 check: 2 > 15  ✗
→ No shuffle needed
→ Draws 2 cards directly

Result: Buggy code does 1 unnecessary shuffle
```

This has minimal performance impact but shows the bug is real.

## Conclusion

This is a **clear mathematical error** in the code that is **accidentally masked** by safety mechanisms. The bug should be fixed to:
1. Correct internal state
2. Enable hand size mods
3. Reduce unnecessary operations
4. Improve code quality

**Patch Recommendation**: ⭐ **Option 1 - Fix The Calculation**

**Priority**: P2 (Major bug with low risk patch available)
