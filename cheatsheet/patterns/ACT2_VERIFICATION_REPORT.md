# Act 2 Monster Pattern Verification Report

**Date**: 2025-11-08
**Verification Method**: Direct comparison of markdown documents against decompiled source code
**Focus**: getMove() logic and takeTurn() implementation

---

## Summary

Out of Act 2 monsters verified, significant dummy data and errors were found in the documentation.

### Critical Issues Found

## 1. TorchHead - CONFIRMED 100% DUMMY DATA

**Document Claims**:
- Pattern 1: Tackle (7 dmg) - 70% probability
- Pattern 2: Burning Attack (12 dmg + Burn cards) - 30% probability
- Complex AI logic with probability distribution

**Actual Source Code** (TorchHead.java):
```java
// Constructor (line 37)
setMove((byte)1, AbstractMonster.Intent.ATTACK, 7);

// getMove() (lines 84-86)
protected void getMove(int num) {
    setMove((byte)1, AbstractMonster.Intent.ATTACK, 7);
}

// takeTurn() (lines 56-65) - Only case 1 exists
```

**Verdict**: Pattern 2 does NOT exist. There is NO Burn mechanism, NO probability logic, NO second move. The monster ONLY does basic 7 damage attack every turn.

**Action Required**: DELETE entire Pattern 2 section, AI logic probability claims, all Burn-related content.

---

## 2. Byrd - MAJOR ERRORS IN PATTERN DATA

**Document Claims**:
- Pattern 1: Peck (1 x 3 hits)
- Pattern 2: Swoop (12 dmg)
- Pattern 3: Fly (Weak debuff)

**Actual Source Code** (Byrd.java):
```java
// Constructor (lines 67-75)
if (AbstractDungeon.ascensionLevel >= 2) {
    this.peckDmg = 1;
    this.peckCount = 6;  // NOT 3!
    this.swoopDmg = 14;  // NOT 12!
} else {
    this.peckDmg = 1;
    this.peckCount = 5;  // NOT 3!
    this.swoopDmg = 12;
}

// Move 6 (CAW) - lines 115-118
case 6:
    // SFXAction("BYRD_DEATH")
    // TalkAction
    // ApplyPowerAction(StrengthPower(this, 1))  // NOT WeakPower!
```

**Errors Found**:
1. **Peck count wrong**: Document says 3, source shows 5 (A0-A1) or 6 (A2+)
2. **Swoop damage wrong**: Document says 12, source shows 12 (A0-A1) or 14 (A2+) - missing A2 variant
3. **Pattern 3 completely wrong**: Document claims "Weak debuff", source shows it's CAW which gives **StrengthPower to SELF**, not Weak to player
4. **Missing patterns**: HEADBUTT (move 5), STUNNED (move 4), GO_AIRBORNE (move 2) exist in source but not documented
5. **Missing Flight mechanic**: FlightPower (3-4 stacks) applied in usePreBattleAction() not mentioned
6. **Missing grounded state**: Complex state machine with FLYING/GROUNDED states not documented

**Action Required**: Complete rewrite of Byrd.md required.

---

## 3. Chosen - MAJOR ERRORS IN PATTERN DATA

**Document Claims**:
- Pattern 1: Poke (5 x 2 hits) - "first turn 40%"
- Pattern 2: Slash (18 dmg) - "first turn 60%"
- Pattern 3: Chosen (Strength 3/4 buff) - "HP 50% threshold"
- Pattern 4: Hex (Hex debuff) - "15% probability"

**Actual Source Code** (Chosen.java):
```java
// Constructor (lines 69-77)
if (AbstractDungeon.ascensionLevel >= 2) {
    this.zapDmg = 21;        // NOT 18!
    this.debilitateDmg = 12; // NOT documented!
    this.pokeDmg = 6;        // NOT 5!
} else {
    this.zapDmg = 18;
    this.debilitateDmg = 10;
    this.pokeDmg = 5;
}

// getMove() (lines 154-210)
// First turn logic (lines 182-186):
if (this.firstTurn) {
    this.firstTurn = false;
    setMove((byte)5, Intent.ATTACK, pokeDmg, 2, true); // POKE is ALWAYS first!
    return;
}

// Move 4 (HEX) applies HexPower, NOT "Hex card"!
```

**Errors Found**:
1. **Pattern names wrong**: "Slash" is actually ZAP (move 1), "Poke" is move 5
2. **Damage values wrong**:
   - ZAP: 18 (A0-A1) or 21 (A2+), not just "18"
   - POKE: 5 (A0-A1) or 6 (A2+), not just "5"
3. **Missing DEBILITATE pattern**: Move 3 (10/12 dmg + Vulnerable 2) completely undocumented
4. **Missing DRAIN pattern**: Move 2 (Weak 3 to player + Strength 3 to self) completely undocumented
5. **First turn logic wrong**: POKE (move 5) is ALWAYS first turn, not "40% probability"
6. **Hex mechanism wrong**: Applies HexPower (cannot draw status cards), not "Hex card to deck"
7. **AI logic completely wrong**: Complex state machine with A17+ variant not documented

**Action Required**: Complete rewrite of Chosen.md required.

---

## 4. Snecko - MODERATE ERRORS

**Document Claims**:
- Pattern 1: Tail Whip (8 x 2 hits)
- Pattern 2: Bite (15 dmg)
- Pattern 3: Perplexing Gaze (Confusion)

**Actual Source Code** (Snecko.java):
```java
// Constructor (lines 68-74)
if (AbstractDungeon.ascensionLevel >= 2) {
    this.biteDmg = 18;   // A2+ is 18, not 15
    this.tailDmg = 10;   // A2+ is 10, not 8
} else {
    this.biteDmg = 15;
    this.tailDmg = 8;
}

// getMove() (lines 155-174)
if (this.firstTurn) {
    this.firstTurn = false;
    setMove(MOVES[0], (byte)1, Intent.STRONG_DEBUFF); // GLARE is always first!
    return;
}
```

**Errors Found**:
1. **Move names wrong**: TAIL is move 3, BITE is move 2, GLARE is move 1
2. **First turn wrong**: Document says "Tail Whip 60% / Bite 40%", but source shows GLARE (Confusion) is ALWAYS first turn
3. **Pattern order confusing**: Document lists patterns in wrong execution order
4. **Ascension variants missing**: A2+ damage increases not clearly documented
5. **Tail is NOT multi-hit**: Source shows move 3 applies Vulnerable 2 (A17: + Weak 2), it's ATTACK_DEBUFF, not multi-attack

**Action Required**: Rewrite Snecko.md with correct move order and first-turn behavior.

---

## Verification Methodology

For each monster, the following was checked:

1. **getMove() method**: What moves are actually set by AI logic
2. **takeTurn() method**: What case statements exist and what they do
3. **Constructor**: HP values, damage values, ascension variants
4. **Special mechanics**: Powers, state machines, conditional logic

### Key Principles

- **If a pattern is not in getMove()**: It's dummy data
- **If a case exists in takeTurn() but never reached**: It's dead code
- **If damage values differ by ascension**: Both must be documented
- **If first-turn logic differs**: Must be explicitly stated

---

## Remaining Monsters to Verify

- Centurion
- Healer
- SnakePlant
- SphericGuardian
- Mugger
- ShelledParasite
- Bandits (3 types)
- Act 2 Elites (Book of Stabbing, Gremlin Leader, Taskmaster)
- Bosses (Bronze Automaton, Champ, The Collector)

---

## Recommendations

1. **Delete all dummy data immediately** - especially TorchHead Pattern 2
2. **Verify every document against source** - don't trust existing documentation
3. **Document ascension variants explicitly** - A0-A1 vs A2+ vs A7+ vs A17+
4. **Include actual move names** - use the byte constants from source
5. **Document AI logic accurately** - include all conditionals and state checks
6. **Note special mechanics** - powers, state machines, flags, phase transitions

---

## Next Steps

1. Fix TorchHead.md - remove Pattern 2 entirely
2. Rewrite Byrd.md completely
3. Rewrite Chosen.md completely
4. Fix Snecko.md - correct first turn and move order
5. Continue verification for remaining monsters
