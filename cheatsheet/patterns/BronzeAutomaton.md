# 청동 자동인형 (Bronze Automaton)

## 기본 정보

**클래스명**: `BronzeAutomaton`
**전체 경로**: `com.megacrit.cardcrawl.monsters.city.BronzeAutomaton`
**ID**: `"BronzeAutomaton"`
**타입**: 보스 (BOSS)
**등장 지역**: 2막 (The City)

---

## HP 정보

| 난이도 | HP |
|--------|-----|
| 기본 (A0-A8) | 300 |
| A9+ | 320 |

**특징**:
- 2막 보스 중 최고 HP
- **페이즈 시스템 없음** (HP 기반 전환 없음)
- 턴 카운트 기반 패턴 시스템

---

## 핵심 메커니즘

### 턴 카운트 시스템

**Hyper Beam 4턴 주기**:
- `numTurns` 변수로 추적 (초기값: 0)
- `numTurns == 4`일 때 **Hyper Beam** 발동
- Hyper Beam 후 `numTurns = 0` 리셋
- 이후 1씩 증가하며 다시 카운트

**타임라인 예시**:
```
턴 1: SPAWN_ORBS (첫턴 전용)
턴 2: FLAIL/BOOST → numTurns = 1
턴 3: BOOST/FLAIL → numTurns = 2
턴 4: FLAIL/BOOST → numTurns = 3
턴 5: BOOST/FLAIL → numTurns = 4
턴 6: HYPER_BEAM → numTurns = 0 (리셋)
턴 7: STUNNED (A0-A18) 또는 BOOST (A19+)
턴 8: FLAIL → numTurns = 1
...
```

**중요**: 페이즈 시스템이 아닌 **순수 턴 카운트 기반**

---

## 패턴 정보

### 패턴 1: 구체 소환 (SPAWN_ORBS, byte 4)

**의도**: `UNKNOWN`
**발동 조건**: **첫 턴 전용** (`firstTurn == true`)

**효과**:
- Bronze Orb 2개 소환
  - 위치 1: `(-300.0F, 200.0F, 0)`
  - 위치 2: `(200.0F, 130.0F, 1)`
- 소환 시 사운드 효과 (랜덤):
  - `AUTOMATON_ORB_SPAWN` 또는 `MONSTER_AUTOMATON_SUMMON`

**코드 분석**:
```java
// getMove() - 첫 턴 체크
if (this.firstTurn) {
    setMove((byte)4, Intent.UNKNOWN);
    this.firstTurn = false;
    return;
}

// takeTurn() - 구체 소환
case 4:
    // Bronze Orb 1
    SpawnMonsterAction(new BronzeOrb(-300.0F, 200.0F, 0), true);
    // Bronze Orb 2
    SpawnMonsterAction(new BronzeOrb(200.0F, 130.0F, 1), true);
```

**특징**:
- 전투 시작 시 **단 1회** 발동
- 이후 다시 사용하지 않음
- `firstTurn` 플래그로 제어

---

### 패턴 2: 극강타 (FLAIL, byte 1)

**의도**: `ATTACK`
**데미지**: 7 x 2회 (A4+: 8 x 2회)

| 난이도 | 데미지 | 총 데미지 |
|--------|--------|----------|
| 기본 (A0-A3) | 7 x 2 | 14 |
| A4+ | 8 x 2 | 16 |

**발동 조건**:
- 이전 패턴이 `STUNNED(3)` 또는 `BOOST(5)` 또는 `SPAWN_ORBS(4)`
- `numTurns != 4` (Hyper Beam 아닐 때)
- 이전 패턴이 `HYPER_BEAM(2)`가 아닐 때

**효과**:
- 플레이어에게 **2회 연속 공격**
- 공격 이펙트: `SLASH_DIAGONAL`

**코드 분석**:
```java
// takeTurn()
case 1:
    AnimateFastAttackAction(this);
    DamageAction(player, damage.get(0), SLASH_DIAGONAL); // 1타
    DamageAction(player, damage.get(0), SLASH_DIAGONAL); // 2타
    break;

// getMove() - 발동 조건
if (lastMove((byte)3) || lastMove((byte)5) || lastMove((byte)4)) {
    setMove((byte)1, Intent.ATTACK, damage.get(0).base, 2, true);
}
```

---

### 패턴 3: 강화 (BOOST, byte 5)

**의도**: `DEFEND_BUFF`

**효과**:
- 자신에게 **Block 9** (A9+: **Block 12**)
- 자신에게 **Strength +3** (A4+: **Strength +4**)

| 난이도 | Block | Strength |
|--------|-------|----------|
| A0-A3 | 9 | 3 |
| A4-A8 | 9 | 4 |
| A9+ | 12 | 4 |

**발동 조건**:
- 이전 패턴이 `FLAIL(1)`
- `numTurns != 4` (Hyper Beam 아닐 때)
- 이전 패턴이 `HYPER_BEAM(2)`가 아닐 때

**코드 분석**:
```java
// Constructor
if (AbstractDungeon.ascensionLevel >= 9) {
    this.blockAmt = 12;
} else {
    this.blockAmt = 9;
}

if (AbstractDungeon.ascensionLevel >= 4) {
    this.strAmt = 4;
} else {
    this.strAmt = 3;
}

// takeTurn()
case 5:
    GainBlockAction(this, this, this.blockAmt);
    ApplyPowerAction(this, this, new StrengthPower(this, this.strAmt));
    break;

// getMove() - 발동 조건
else {
    setMove((byte)5, Intent.DEFEND_BUFF);
}
```

---

### 패턴 4: 하이퍼 빔 (HYPER_BEAM, byte 2)

**의도**: `ATTACK`
**데미지**: 45 (A4+: 50)

| 난이도 | 데미지 |
|--------|--------|
| 기본 (A0-A3) | 45 |
| A4+ | 50 |

**발동 조건**: **`numTurns == 4`** (매 5번째 턴)

**효과**:
- 레이저 빔 시각 효과
- 플레이어에게 **고데미지 단타**
- 공격 이펙트: `NONE` (레이저 효과만)

**코드 분석**:
```java
// Constructor
if (AbstractDungeon.ascensionLevel >= 4) {
    this.beamDmg = 50;
} else {
    this.beamDmg = 45;
}
damage.add(new DamageInfo(this, this.beamDmg)); // index 1

// getMove() - 4턴마다 발동
if (this.numTurns == 4) {
    setMove(BEAM_NAME, (byte)2, Intent.ATTACK, damage.get(1).base);
    this.numTurns = 0; // 리셋!
    return;
}

// takeTurn()
case 2:
    VFXAction(new LaserBeamEffect(hb.cX, hb.cY + 60.0F), 1.5F);
    DamageAction(player, damage.get(1), AttackEffect.NONE);
    break;
```

**중요**:
- 정확히 **4턴 주기**로 발동 (매 5번째 턴)
- 사용 후 `numTurns = 0` 리셋
- 이후 다시 4턴 카운트 시작

---

### 패턴 5: 기절 (STUNNED, byte 3)

**의도**: `STUN`
**발동 조건**:
- 이전 패턴이 `HYPER_BEAM(2)`
- **A0-A18 전용** (A19+에서는 `BOOST`로 대체)

**효과**:
- **아무 행동도 하지 않음**
- "STUNNED" 텍스트 표시

**코드 분석**:
```java
// getMove() - Hyper Beam 후 처리
if (lastMove((byte)2)) {
    if (AbstractDungeon.ascensionLevel >= 19) {
        setMove((byte)5, Intent.DEFEND_BUFF); // A19+: BOOST
        return;
    }
    setMove((byte)3, Intent.STUN); // A0-A18: STUNNED
    return;
}

// takeTurn()
case 3:
    TextAboveCreatureAction(this, TextType.STUNNED);
    break;
```

**중요**:
- **A19 이상에서는 STUNNED 대신 BOOST 사용**
- Hyper Beam의 페널티 제거 (A19+ 강화)

---

## AI 로직 상세 분석

### getMove() 전체 흐름

```java
protected void getMove(int num) {
    // 1단계: 첫 턴 체크
    if (this.firstTurn) {
        setMove((byte)4, Intent.UNKNOWN); // SPAWN_ORBS
        this.firstTurn = false;
        return;
    }

    // 2단계: 4턴 카운트 체크 (Hyper Beam)
    if (this.numTurns == 4) {
        setMove(BEAM_NAME, (byte)2, Intent.ATTACK, damage.get(1).base);
        this.numTurns = 0; // 리셋!
        return;
    }

    // 3단계: Hyper Beam 후 처리
    if (lastMove((byte)2)) {
        if (AbstractDungeon.ascensionLevel >= 19) {
            setMove((byte)5, Intent.DEFEND_BUFF); // BOOST
        } else {
            setMove((byte)3, Intent.STUN); // STUNNED
        }
        return;
    }

    // 4단계: FLAIL ↔ BOOST 교대
    if (lastMove((byte)3) || lastMove((byte)5) || lastMove((byte)4)) {
        // 이전이 STUNNED/BOOST/SPAWN_ORBS → FLAIL
        setMove((byte)1, Intent.ATTACK, damage.get(0).base, 2, true);
    } else {
        // 이전이 FLAIL → BOOST
        setMove((byte)5, Intent.DEFEND_BUFF);
    }

    // 5단계: 턴 카운트 증가
    this.numTurns++; // 다음 턴을 위한 카운트
}
```

---

## 패턴 순서 시뮬레이션

### A0-A18 (STUNNED 있음)

```
턴 1: SPAWN_ORBS (첫턴)
턴 2: FLAIL (7x2 = 14) → numTurns = 1
턴 3: BOOST (Block 9, Str +3) → numTurns = 2
턴 4: FLAIL (7x2 = 14) → numTurns = 3
턴 5: BOOST (Block 9, Str +3) → numTurns = 4
턴 6: HYPER_BEAM (45) → numTurns = 0 (리셋)
턴 7: STUNNED (아무것도 안함)
턴 8: FLAIL (7x2 = 14) → numTurns = 1
턴 9: BOOST (Block 9, Str +3) → numTurns = 2
턴 10: FLAIL (7x2 = 14) → numTurns = 3
턴 11: BOOST (Block 9, Str +3) → numTurns = 4
턴 12: HYPER_BEAM (45) → numTurns = 0
턴 13: STUNNED
...
```

**패턴**: SPAWN → (FLAIL → BOOST)x2 → HYPER_BEAM → STUNNED → 반복

---

### A19+ (BOOST로 대체)

```
턴 1: SPAWN_ORBS (첫턴)
턴 2: FLAIL (8x2 = 16) → numTurns = 1
턴 3: BOOST (Block 12, Str +4) → numTurns = 2
턴 4: FLAIL (8x2 = 16) → numTurns = 3
턴 5: BOOST (Block 12, Str +4) → numTurns = 4
턴 6: HYPER_BEAM (50) → numTurns = 0 (리셋)
턴 7: BOOST (Block 12, Str +4) ← STUNNED 대신!
턴 8: FLAIL (8x2 = 16) → numTurns = 1
턴 9: BOOST (Block 12, Str +4) → numTurns = 2
턴 10: FLAIL (8x2 = 16) → numTurns = 3
턴 11: BOOST (Block 12, Str +4) → numTurns = 4
턴 12: HYPER_BEAM (50) → numTurns = 0
턴 13: BOOST (Block 12, Str +4)
...
```

**패턴**: SPAWN → (FLAIL → BOOST)x2 → HYPER_BEAM → BOOST → 반복

**중요**: A19+에서는 **STUNNED 턴이 없어 더 위협적**

---

## 구체 (Bronze Orb) 정보

### 소환 정보

**소환 시점**: 첫 턴 전용
**소환 개수**: 2개 (고정)
**위치**:
- Orb 1: `(-300.0F, 200.0F)` - 왼쪽 위
- Orb 2: `(200.0F, 130.0F)` - 오른쪽 중간

### 특징

- **재소환 없음**: 첫 턴 이후 추가 소환 없음
- **독립 행동**: 본체와 별개로 턴 진행
- **Boost 무효**: 본체만 Strength 증가 (구체는 버프 없음)
- **사망 시**: 제거되어도 재소환 안됨

**중요**: 구체에게 **Boost가 적용되지 않음** (문서 오류 수정)

---

## 전투 전략

### 턴별 대응 전략

**초반 (턴 1-5)**:
- 턴 1: 구체 소환 → 구체 제거 우선 또는 무시
- 턴 2-5: FLAIL(14-16) / BOOST 교대
  - FLAIL: 방어력 14-16 확보
  - BOOST: 다음 턴 Strength 증가 주의

**Hyper Beam 턴 (턴 6)**:
- **45-50 데미지 대비 필수**
- 최우선 방어 (Block 50+ 권장)
- 실패 시 즉시 치명타

**Hyper Beam 후 (턴 7)**:
- A0-A18: STUNNED → **공격 찬스!**
- A19+: BOOST → 방어 지속

### 난이도별 핵심 차이

| 난이도 | FLAIL | HYPER_BEAM | Str | Block | Beam 후 |
|--------|-------|------------|-----|-------|---------|
| A0-A3 | 7x2 | 45 | +3 | 9 | STUNNED |
| A4-A8 | 8x2 | 50 | +4 | 9 | STUNNED |
| A9-A18 | 8x2 | 50 | +4 | 12 | STUNNED |
| A19+ | 8x2 | 50 | +4 | 12 | **BOOST** |

**결론**: A19+가 가장 어려움 (STUNNED 제거)

---

### 구체 관리 전략

**제거 우선 전략**:
- 구체 2개 → 턴당 16-20 추가 데미지
- AOE 카드로 빠르게 제거
- 본체 집중 전 처리 권장

**무시 전략**:
- 본체만 집중 공격
- 구체 데미지 감수 (방어 +16-20 필요)
- 빠른 킬 가능 시 유효

**추천**: 초반 구체 제거 → 본체 집중

---

### 추천 카드

**방어** (Hyper Beam 대비):
- Shrug It Off (Block 8 + 드로우)
- Flame Barrier (Block 12 + 반격)
- Barricade (Block 유지)

**AOE** (구체 제거):
- Cleave (8 데미지 AOE)
- Immolate (21 데미지 AOE)
- Whirlwind (변동 AOE)

**단타** (본체):
- Heavy Blade (Strength 비례)
- Bludgeon (32 데미지)
- Perfected Strike (고데미지)

**디버프**:
- Disarm (Strength -2, BOOST 카운터)
- Shockwave (Weak + Vulnerable)
- Clothesline (Weak)

---

### 위험 요소

**Hyper Beam 주기**:
- **턴 6, 12, 18, 24...**에 반드시 발동
- 방어 실패 시 50 데미지 → 즉사 위험

**Strength 누적**:
- BOOST로 Str +3~4 누적
- 5턴마다 반복 → 누적 시 FLAIL 위협 증가
- Disarm으로 제거 권장

**A19+ 강화**:
- STUNNED 제거 → 휴식 턴 없음
- BOOST 연속 → Block + Str 급증
- 난이도 급상승

---

## 수정 예시

### 1. Hyper Beam 주기 단축 (A25+)

```java
@SpirePatch(
    clz = BronzeAutomaton.class,
    method = "getMove"
)
public static class HyperBeamIntervalPatch {
    @SpirePrefixPatch
    public static SpireReturn<Void> Prefix(BronzeAutomaton __instance, int num) {
        if (AbstractDungeon.ascensionLevel >= 25) {
            // numTurns == 4 → numTurns == 2
            // Hyper Beam 5턴마다 → 3턴마다
            if (ReflectionHacks.getPrivate(__instance, BronzeAutomaton.class, "numTurns").equals(2)) {
                ReflectionHacks.setPrivate(__instance, BronzeAutomaton.class, "numTurns", 0);
                // setMove(HYPER_BEAM)
            }
        }
        return SpireReturn.Continue();
    }
}
```

---

### 2. STUNNED 완전 제거 (모든 난이도)

```java
@SpirePatch(
    clz = BronzeAutomaton.class,
    method = "getMove"
)
public static class RemoveStunnedPatch {
    @SpirePrefixPatch
    public static SpireReturn<Void> Prefix(BronzeAutomaton __instance, int num) {
        // lastMove(2) == HYPER_BEAM 후 항상 BOOST
        if (ReflectionHacks.invoke(__instance, AbstractMonster.class, "lastMove", (byte)2)) {
            ReflectionHacks.invoke(__instance, AbstractMonster.class, "setMove",
                (byte)5, AbstractMonster.Intent.DEFEND_BUFF);
            return SpireReturn.Return(null);
        }
        return SpireReturn.Continue();
    }
}
```

---

### 3. 구체 재소환 시스템 추가

```java
@SpirePatch(
    clz = BronzeAutomaton.class,
    method = "getMove"
)
public static class RespawnOrbsPatch {
    @SpirePrefixPatch
    public static SpireReturn<Void> Prefix(BronzeAutomaton __instance, int num) {
        if (AbstractDungeon.ascensionLevel >= 25) {
            // 구체 전멸 시 재소환
            int aliveOrbs = countAliveOrbs();
            if (aliveOrbs == 0 && ReflectionHacks.getPrivate(__instance, BronzeAutomaton.class, "numTurns") != 4) {
                ReflectionHacks.invoke(__instance, AbstractMonster.class, "setMove",
                    (byte)4, AbstractMonster.Intent.UNKNOWN);
                return SpireReturn.Return(null);
            }
        }
        return SpireReturn.Continue();
    }

    private static int countAliveOrbs() {
        int count = 0;
        for (AbstractMonster m : AbstractDungeon.getMonsters().monsters) {
            if (m instanceof BronzeOrb && !m.isDeadOrEscaped()) {
                count++;
            }
        }
        return count;
    }
}
```

---

## 중요 필드

| 필드명 | 타입 | 초기값 | 설명 |
|--------|------|--------|------|
| `numTurns` | int | 0 | Hyper Beam 카운터 (4일 때 발동) |
| `firstTurn` | boolean | true | 첫 턴 여부 (구체 소환) |
| `flailDmg` | int | 7/8 | FLAIL 데미지 (A4: +1) |
| `beamDmg` | int | 45/50 | HYPER_BEAM 데미지 (A4: +5) |
| `strAmt` | int | 3/4 | BOOST Strength (A4: +1) |
| `blockAmt` | int | 9/12 | BOOST Block (A9: +3) |

---

## 관련 파일

- **본 파일**: `com/megacrit/cardcrawl/monsters/city/BronzeAutomaton.java`
- **구체**: `com/megacrit/cardcrawl/monsters/city/BronzeOrb.java`
- **파워**:
  - `com.megacrit.cardcrawl.powers.StrengthPower`
  - `com.megacrit.cardcrawl.powers.ArtifactPower` (3중첩, 사전 버프)
- **액션**:
  - `DamageAction`
  - `ApplyPowerAction`
  - `SpawnMonsterAction`
  - `GainBlockAction`
  - `TextAboveCreatureAction`
  - `VFXAction`

---

## 참고사항

1. **페이즈 시스템 없음**: HP 기반 전환 없음, 순수 턴 카운트 시스템
2. **Hyper Beam 주기**: 정확히 **4턴마다** 발동 (매 5번째 턴)
3. **STUNNED 메커니즘**: A19+에서 BOOST로 대체 (강화)
4. **구체 소환**: 첫 턴 단 1회, 재소환 없음
5. **FLAIL ↔ BOOST**: 교대 패턴 (Hyper Beam 전까지)
6. **Artifact 3**: 사전 버프로 디버프 3회 무효화
7. **턴 카운트 증가 타이밍**: `getMove()` 마지막 (다음 턴 준비)
8. **A19+ 난이도 급증**: STUNNED 제거로 공격 빈도 증가
9. **Block 누적**: BOOST 반복으로 방어력 상승
10. **Strength 누적**: BOOST 반복으로 FLAIL 위협 증가
