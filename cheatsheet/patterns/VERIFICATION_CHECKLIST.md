# Monster Pattern Verification Checklist

Quick reference for verifying monster documentation against source code.

---

## Verification Steps

### For Each Monster:

#### 1. Source Code Analysis (E:\workspace\sts-decompile\com\megacrit\cardcrawl\monsters\city\)

```
[ ] Open {Monster}.java
[ ] Check constructor:
    [ ] HP values (HP_MIN, HP_MAX, A_2_HP_MIN, A_2_HP_MAX, A_7+, etc.)
    [ ] Damage values for each move
    [ ] Ascension level conditionals
    [ ] Initial state/flags

[ ] Check getMove(int num):
    [ ] List all setMove() calls - these are REAL patterns
    [ ] Note move bytes (e.g., (byte)1, (byte)2)
    [ ] Note probability ranges (num < X)
    [ ] Check first turn logic (firstTurn flag)
    [ ] Check lastMove() / lastTwoMoves() calls
    [ ] Note ascension level conditionals

[ ] Check takeTurn():
    [ ] For each case in switch(nextMove)
    [ ] What actions are executed?
    [ ] What damage is dealt?
    [ ] What powers/debuffs applied?
    [ ] Any special mechanics?

[ ] Check special methods:
    [ ] usePreBattleAction() - initial powers/setup
    [ ] changeState() - state machine logic
    [ ] Special conditionals - HP thresholds, flags, etc.
```

#### 2. Document Comparison (E:\workspace\sts-mods\cheatsheet\patterns\)

```
[ ] Open {Monster}.md
[ ] Verify HP ranges match source (all ascension levels)
[ ] For each pattern in document:
    [ ] Does it exist in getMove()? (if not, it's DUMMY DATA)
    [ ] Is the move byte correct?
    [ ] Is the damage value correct (all ascension variants)?
    [ ] Are the effects correct (match takeTurn())?
    [ ] Is the probability correct (match getMove() ranges)?

[ ] Check for MISSING patterns:
    [ ] Are all setMove() calls from getMove() documented?

[ ] Verify AI Logic section:
    [ ] Does it match getMove() implementation exactly?
    [ ] Are all conditionals documented?
    [ ] Is first turn logic correct?
    [ ] Are ascension variants noted?
```

#### 3. Common Error Patterns to Check

```
[ ] Dummy Data:
    [ ] Patterns documented but never set in getMove()
    [ ] Case statements in takeTurn() never reached

[ ] Wrong First Turn:
    [ ] Document claims probability, code shows deterministic
    [ ] Check for "firstTurn" or "firstMove" flag

[ ] Missing Ascension Variants:
    [ ] A2+ damage increases
    [ ] A7+ HP increases
    [ ] A17+ additional effects
    [ ] A9+ specific changes

[ ] Wrong Move Names:
    [ ] Confusion between internal names and display names
    [ ] Check MOVES string array vs byte constants

[ ] Incomplete Pattern Lists:
    [ ] Missing moves that exist in getMove()
    [ ] Missing state-dependent moves

[ ] Wrong Effect Attribution:
    [ ] Powers applied to wrong target (self vs player)
    [ ] Wrong power types (Weak vs Vulnerable, etc.)
```

---

## Act 2 Monster Checklist

### Normal Monsters

- [x] ✅ TorchHead - VERIFIED & FIXED (dummy data removed)
- [ ] ⚠️ Byrd - ERRORS FOUND (requires complete rewrite)
- [ ] ⚠️ Chosen - ERRORS FOUND (requires complete rewrite)
- [ ] ⚠️ Snecko - ERRORS FOUND (requires rewrite)
- [ ] Centurion
- [ ] Healer
- [ ] SnakePlant
- [ ] SphericGuardian
- [ ] Mugger
- [ ] ShelledParasite

### Bandit Group (Bandits_Complete.md)

- [ ] BanditBear
- [ ] BanditLeader
- [ ] BanditPointy

### Elite Monsters (Act2_Elites_Complete.md)

- [ ] Book of Stabbing
- [ ] Gremlin Leader
- [ ] Taskmaster

### Boss Monsters

- [ ] BronzeAutomaton (+ BronzeOrb minion)
- [ ] Champ
- [ ] TheCollector

---

## Quick Verification Command Reference

### Reading Source Files

```bash
# Read specific monster source
cat E:\workspace\sts-decompile\com\megacrit\cardcrawl\monsters\city\{Monster}.java

# Search for move definitions
grep -n "setMove" {Monster}.java

# Find damage constants
grep -n "DMG\|DAMAGE" {Monster}.java

# Find HP constants
grep -n "HP_MIN\|HP_MAX" {Monster}.java
```

### Common Source Patterns

```java
// HP with ascension scaling
if (AbstractDungeon.ascensionLevel >= 7) {
    setHp(A_2_HP_MIN, A_2_HP_MAX);
} else {
    setHp(HP_MIN, HP_MAX);
}

// Damage with ascension scaling
if (AbstractDungeon.ascensionLevel >= 2) {
    this.damage = A_2_DAMAGE;
} else {
    this.damage = BASE_DAMAGE;
}

// First turn logic
if (this.firstTurn) {
    this.firstTurn = false;
    setMove(...);
    return;
}

// Probability-based move selection
if (num < 40) {
    setMove(MOVE_A);
} else if (num < 70) {
    setMove(MOVE_B);
} else {
    setMove(MOVE_C);
}

// Prevent move repetition
if (lastMove((byte)1)) {
    setMove((byte)2, ...);
} else {
    setMove((byte)1, ...);
}
```

---

## Error Severity Classification

### Critical (Immediate Fix Required):
- Dummy data (patterns that don't exist)
- Wrong effects (different powers/debuffs)
- Wrong first turn logic (deterministic vs probability)
- Missing moves that significantly impact strategy

### High (Fix Soon):
- Wrong damage values
- Missing ascension variants
- Wrong probability distributions
- Incorrect move names

### Medium (Fix When Possible):
- Minor inaccuracies in descriptions
- Missing optimization details
- Incomplete strategy sections

### Low (Nice to Have):
- Typos in Korean text
- Formatting inconsistencies
- Missing lore/flavor text

---

## Template for Verified Monster Document

```markdown
# {Monster Name} ({Korean Name})

## 기본 정보
**클래스명**: `{ClassName}`
**ID**: `"{ID}"`
**타입**: {NORMAL|ELITE|BOSS}
**등장 지역**: 2막 (The City)

## HP 정보
| 난이도 | HP 범위 |
|--------|---------|
| A0-A1 | {min}-{max} |
| A2-A6 | {min}-{max} |
| A7-A8 | {min}-{max} |
| A9+ | {min}-{max} |

## 패턴 정보

### 패턴 {N}: {Name} (Move Byte: {X})
**의도**: `{Intent}`
**데미지**: {damage}

| 난이도 | 데미지 |
|--------|--------|
| A0-A1 | {dmg} |
| A2+ | {dmg} |

**효과**:
- {Actual effects from takeTurn()}

**발동 조건**: {From getMove() logic}

## AI 로직 (getMove)

**실제 소스코드**:
```java
// Paste relevant getMove() code
```

**로직 설명**:
{Line-by-line explanation matching source}

## 특수 동작
{Powers, state machines, conditionals from source}

## 검증 정보
- ✅ Verified against source: {Date}
- ✅ All patterns confirmed in getMove()
- ✅ Damage values match constructor
- ✅ AI logic matches implementation
- ✅ Effects match takeTurn()
```

---

## Notes

- Always verify against latest decompiled source
- Document all ascension variants explicitly
- Include actual source code snippets where helpful
- Note verification date for future reference
- If uncertain, mark with ⚠️ and note what needs clarification
