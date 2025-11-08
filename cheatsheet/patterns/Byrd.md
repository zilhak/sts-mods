# 버드 (Byrd)

## 기본 정보

**클래스명**: `Byrd`
**전체 경로**: `com.megacrit.cardcrawl.monsters.city.Byrd`
**ID**: `"Byrd"`
**타입**: 일반 적 (NORMAL)
**등장 지역**: 2막 (The City)

---

## HP 정보

| 난이도 | HP 범위 |
|--------|---------|
| 기본 (A0-A6) | 25-31 |
| A7+ | 26-33 |

**코드 위치**: 55-59줄

```java
if (AbstractDungeon.ascensionLevel >= 7) {
    setHp(26, 33);
} else {
    setHp(25, 31);
}
```

---

## Flight 메커니즘

Byrd의 핵심 메커니즘은 **비행(FLYING)** 상태와 **착지(GROUNDED)** 상태를 전환하는 것입니다.

### Flight 파워

**전투 시작 시 자동 부여**

| 난이도 | Flight 수치 |
|--------|------------|
| 기본 (A0-A16) | 3 |
| A17+ | 4 |

**코드 위치**: 61-65줄, 87-89줄

```java
// 생성자에서 설정
if (AbstractDungeon.ascensionLevel >= 17) {
    this.flightAmt = 4;
} else {
    this.flightAmt = 3;
}

// usePreBattleAction에서 부여
public void usePreBattleAction() {
    AbstractDungeon.actionManager.addToBottom(
        new ApplyPowerAction((AbstractCreature)this, (AbstractCreature)this,
            new FlightPower((AbstractCreature)this, this.flightAmt))
    );
}
```

### 상태 전환 메커니즘

**FLYING 상태**:
- Flight 파워가 0이 되면 자동으로 GROUNDED 상태로 전환
- 애니메이션: `flying.atlas`, `flying.json`
- 히트박스: `(0.0F, 50.0F, 240.0F, 180.0F)`
- 사용 가능 패턴: Peck, Swoop, Caw

**GROUNDED 상태**:
- Flight 파워가 없을 때
- 애니메이션: `grounded.atlas`, `grounded.json`
- 히트박스: `(10.0F, -50.0F, 240.0F, 180.0F)`
- 다음 턴: 무조건 STUNNED (기절)
- 그 다음 턴: 무조건 Headbutt + GO_AIRBORNE (공중으로 복귀)

**코드 위치**: 138-163줄

```java
public void changeState(String stateName) {
    switch (stateName) {
        case "FLYING":
            loadAnimation("images/monsters/theCity/byrd/flying.atlas",
                "images/monsters/theCity/byrd/flying.json", 1.0F);
            AnimationState.TrackEntry e = this.state.setAnimation(0, "idle_flap", true);
            e.setTime(e.getEndTime() * MathUtils.random());
            updateHitbox(0.0F, 50.0F, 240.0F, 180.0F);
            break;
        case "GROUNDED":
            setMove((byte)4, AbstractMonster.Intent.STUN);
            createIntent();
            this.isFlying = false;
            loadAnimation("images/monsters/theCity/byrd/grounded.atlas",
                "images/monsters/theCity/byrd/grounded.json", 1.0F);
            e = this.state.setAnimation(0, "idle", true);
            e.setTime(e.getEndTime() * MathUtils.random());
            updateHitbox(10.0F, -50.0F, 240.0F, 180.0F);
            break;
    }
}
```

---

## 패턴 정보

### 패턴 1: 쪼기 (Peck)

**바이트 값**: `1`
**의도**: `ATTACK` (다중 공격)
**발동 조건**: FLYING 상태에서만 사용

**데미지**:
| 난이도 | 횟수 | 총 데미지 |
|--------|------|-----------|
| 기본 (A0-A1) | 5회 | 5 (1×5) |
| A2+ | 6회 | 6 (1×6) |

**코드 위치**: 47-50줄, 67-75줄, 94-101줄

```java
// 상수 정의
private static final int PECK_DMG = 1;
private static final int PECK_COUNT = 5;
private static final int A_2_PECK_COUNT = 6;

// 생성자에서 설정
if (AbstractDungeon.ascensionLevel >= 2) {
    this.peckDmg = 1;
    this.peckCount = 6;
} else {
    this.peckDmg = 1;
    this.peckCount = 5;
}

// 데미지 등록
this.damage.add(new DamageInfo((AbstractCreature)this, this.peckDmg));

// takeTurn 실행
case 1:
    AbstractDungeon.actionManager.addToBottom(
        new AnimateFastAttackAction((AbstractCreature)this)
    );
    for (i = 0; i < this.peckCount; i++) {
        playRandomBirdSFx();  // 랜덤 새 소리
        AbstractDungeon.actionManager.addToBottom(
            new DamageAction(
                (AbstractCreature)AbstractDungeon.player,
                this.damage.get(0),
                AbstractGameAction.AttackEffect.BLUNT_LIGHT,
                true  // 빠른 공격 모드
            )
        );
    }
    break;
```

**특징**:
- 5-6회 연속 공격
- 랜덤 새 소리 효과 (`MONSTER_BYRD_ATTACK_0` ~ `MONSTER_BYRD_ATTACK_5`)

**수정 포인트**:
- 횟수 변경: `peckCount` 필드 수정
- 데미지 변경: `peckDmg` 필드 수정

---

### 패턴 2: 급강하 (Swoop)

**바이트 값**: `3`
**의도**: `ATTACK`
**발동 조건**: FLYING 상태에서만 사용
**데미지**: 12 (A0-A1) / 14 (A2+)

**코드 위치**: 49줄, 70-75줄, 120-124줄

```java
// 상수 정의
private static final int SWOOP_DMG = 12;
private static final int A_2_SWOOP_DMG = 14;

// 생성자에서 설정
if (AbstractDungeon.ascensionLevel >= 2) {
    this.swoopDmg = 14;
} else {
    this.swoopDmg = 12;
}

// 데미지 등록
this.damage.add(new DamageInfo((AbstractCreature)this, this.swoopDmg));

// takeTurn 실행
case 3:
    AbstractDungeon.actionManager.addToBottom(
        new AnimateSlowAttackAction((AbstractCreature)this)
    );
    AbstractDungeon.actionManager.addToBottom(
        new DamageAction(
            (AbstractCreature)AbstractDungeon.player,
            this.damage.get(1),
            AbstractGameAction.AttackEffect.SLASH_HEAVY
        )
    );
    break;
```

**수정 포인트**:
- 데미지 변경: `swoopDmg` 필드 수정

---

### 패턴 3: 울부짖음 (Caw)

**바이트 값**: `6`
**의도**: `BUFF`
**발동 조건**: FLYING 상태에서만 사용

**효과**:
- **자신에게 Strength 1 부여** (모든 난이도 동일)
- 대사 출력: `DIALOG[0]`
- 사망 소리 효과: `BYRD_DEATH`

**코드 위치**: 114-118줄

```java
case 6:
    AbstractDungeon.actionManager.addToBottom(
        new SFXAction("BYRD_DEATH")
    );
    AbstractDungeon.actionManager.addToBottom(
        new TalkAction((AbstractCreature)this, DIALOG[0], 1.2F, 1.2F)
    );
    AbstractDungeon.actionManager.addToBottom(
        new ApplyPowerAction(
            (AbstractCreature)this, (AbstractCreature)this,
            new StrengthPower((AbstractCreature)this, 1), 1
        )
    );
    break;
```

**중요**: 이전 문서의 "플레이어에게 Weak" 설명은 **완전히 틀린 정보**입니다.
실제로는 **자신에게 Strength 1**을 부여합니다.

**수정 포인트**:
- Strength 수치 변경: 상수 `1` 부분 수정
- 효과 변경: ApplyPowerAction 수정

---

### 패턴 4: 기절 (Stunned)

**바이트 값**: `4`
**의도**: `STUN`
**발동 조건**: GROUNDED 상태로 전환된 직후 다음 턴

**효과**:
- 아무 행동도 하지 않음
- "STUNNED" 텍스트 표시
- 다음 턴은 무조건 Headbutt

**코드 위치**: 125-128줄

```java
case 4:
    AbstractDungeon.actionManager.addToBottom(
        new SetAnimationAction((AbstractCreature)this, "head_lift")
    );
    AbstractDungeon.actionManager.addToBottom(
        new TextAboveCreatureAction(
            (AbstractCreature)this,
            TextAboveCreatureAction.TextType.STUNNED
        )
    );
    break;
```

---

### 패턴 5: 박치기 (Headbutt)

**바이트 값**: `5`
**의도**: `ATTACK`
**발동 조건**: STUNNED 다음 턴
**데미지**: 3 (고정)

**효과**:
- 플레이어에게 3 데미지
- **다음 턴 무조건 GO_AIRBORNE (패턴 2)**

**코드 위치**: 79줄, 102-107줄

```java
// 데미지 등록
this.damage.add(new DamageInfo((AbstractCreature)this, 3));

// takeTurn 실행
case 5:
    AbstractDungeon.actionManager.addToBottom(
        new AnimateSlowAttackAction((AbstractCreature)this)
    );
    AbstractDungeon.actionManager.addToBottom(
        new DamageAction(
            (AbstractCreature)AbstractDungeon.player,
            this.damage.get(2),
            AbstractGameAction.AttackEffect.BLUNT_HEAVY
        )
    );
    setMove((byte)2, AbstractMonster.Intent.UNKNOWN);
    return;  // 주의: RollMoveAction 없음
```

**중요**: Headbutt 후에는 `RollMoveAction`을 실행하지 않고 즉시 `return`합니다.
다음 패턴이 무조건 GO_AIRBORNE이기 때문입니다.

---

### 패턴 6: 공중으로 (Go Airborne)

**바이트 값**: `2`
**의도**: `UNKNOWN`
**발동 조건**: Headbutt 다음 턴

**효과**:
- FLYING 상태로 복귀
- Flight 파워 재부여 (3 또는 4)

**코드 위치**: 108-112줄

```java
case 2:
    this.isFlying = true;
    AbstractDungeon.actionManager.addToBottom(
        new ChangeStateAction(this, "FLYING")
    );
    AbstractDungeon.actionManager.addToBottom(
        new ApplyPowerAction(
            (AbstractCreature)this, (AbstractCreature)this,
            new FlightPower((AbstractCreature)this, this.flightAmt)
        )
    );
    break;
```

---

## AI 로직 (getMove)

**코드 위치**: 171-226줄

### 첫 번째 턴

```java
if (this.firstMove) {
    this.firstMove = false;
    if (AbstractDungeon.aiRng.randomBoolean(0.375F)) {
        setMove((byte)6, AbstractMonster.Intent.BUFF);  // Caw (37.5%)
    } else {
        setMove((byte)1, AbstractMonster.Intent.ATTACK,
            ((DamageInfo)this.damage.get(0)).base,
            this.peckCount, true);  // Peck (62.5%)
    }
    return;
}
```

**확률**:
- Caw: 37.5%
- Peck: 62.5%

---

### FLYING 상태 (isFlying = true)

**num < 50 (50% 확률)**:
```java
if (num < 50) {
    if (lastTwoMoves((byte)1)) {
        // 직전 2턴이 모두 Peck이면
        if (AbstractDungeon.aiRng.randomBoolean(0.4F)) {
            setMove((byte)3, ...);  // Swoop (40%)
        } else {
            setMove((byte)6, ...);  // Caw (60%)
        }
    } else {
        setMove((byte)1, ...);  // Peck
    }
}
```

**num 50~69 (20% 확률)**:
```java
else if (num < 70) {
    if (lastMove((byte)3)) {
        // 직전 턴이 Swoop이면
        if (AbstractDungeon.aiRng.randomBoolean(0.375F)) {
            setMove((byte)6, ...);  // Caw (37.5%)
        } else {
            setMove((byte)1, ...);  // Peck (62.5%)
        }
    } else {
        setMove((byte)3, ...);  // Swoop
    }
}
```

**num 70~99 (30% 확률)**:
```java
else {
    if (lastMove((byte)6)) {
        // 직전 턴이 Caw이면
        if (AbstractDungeon.aiRng.randomBoolean(0.2857F)) {
            setMove((byte)3, ...);  // Swoop (28.57%)
        } else {
            setMove((byte)1, ...);  // Peck (71.43%)
        }
    } else {
        setMove((byte)6, ...);  // Caw
    }
}
```

---

### GROUNDED 상태 (isFlying = false)

```java
if (!this.isFlying) {
    setMove((byte)5, AbstractMonster.Intent.ATTACK,
        ((DamageInfo)this.damage.get(2)).base);  // Headbutt
}
```

**로직**:
- GROUNDED 상태에서는 무조건 Headbutt (3 데미지)
- Headbutt 후에는 GO_AIRBORNE이 자동 설정됨
- GO_AIRBORNE으로 FLYING 상태 복귀

---

## 전체 패턴 순서 예시

### 정상적인 FLYING 상태 유지 시

1. **첫 턴**: Peck (62.5%) or Caw (37.5%)
2. **이후**: Peck ↔ Swoop ↔ Caw (복잡한 확률 기반)

### Flight 파워가 0이 되어 GROUNDED 상태가 된 경우

1. **GROUNDED 전환**: changeState("GROUNDED") 호출
2. **다음 턴**: STUNNED (패턴 4) - 아무 행동 없음
3. **그 다음 턴**: Headbutt (패턴 5) - 3 데미지
4. **그 다음 턴**: GO_AIRBORNE (패턴 2) - FLYING 복귀, Flight 파워 재부여
5. **이후**: 정상 FLYING 패턴 반복

---

## 수정 예시

### 1. Peck 횟수 증가

```java
@SpirePatch(
    cls = "com.megacrit.cardcrawl.monsters.city.Byrd",
    method = SpirePatch.CONSTRUCTOR
)
public static class ByrdPeckPatch {
    @SpirePostfixPatch
    public static void Postfix(Byrd __instance, float x, float y) {
        if (AbstractDungeon.ascensionLevel >= 25) {
            // Peck 횟수를 8회로 증가
            // peckCount 필드에 접근 필요 (Reflection 사용)
        }
    }
}
```

### 2. Flight 수치 감소

```java
@SpirePatch(
    cls = "com.megacrit.cardcrawl.monsters.city.Byrd",
    method = "usePreBattleAction"
)
public static class ByrdFlightPatch {
    @SpirePrefixPatch
    public static void Prefix(Byrd __instance) {
        if (AbstractDungeon.ascensionLevel >= 25) {
            // Flight 수치를 2로 감소
            // flightAmt 필드에 접근 필요
        }
    }
}
```

### 3. Caw 효과 변경 (플레이어에게 Weak 부여)

```java
@SpirePatch(
    cls = "com.megacrit.cardcrawl.monsters.city.Byrd",
    method = "takeTurn"
)
public static class ByrdCawPatch {
    @SpirePostfixPatch
    public static void Postfix(Byrd __instance) {
        if (__instance.nextMove == 6 && AbstractDungeon.ascensionLevel >= 25) {
            // Caw 사용 시 플레이어에게 Weak 2 추가
            AbstractDungeon.actionManager.addToBottom(
                new ApplyPowerAction(
                    AbstractDungeon.player,
                    __instance,
                    new WeakPower(AbstractDungeon.player, 2, true),
                    2
                )
            );
        }
    }
}
```

---

## 중요 필드

| 필드명 | 타입 | 설명 |
|--------|------|------|
| `firstMove` | boolean | 첫 턴 여부 |
| `isFlying` | boolean | 비행 상태 여부 |
| `peckDmg` | int | Peck 데미지 |
| `peckCount` | int | Peck 횟수 |
| `swoopDmg` | int | Swoop 데미지 |
| `flightAmt` | int | Flight 파워 수치 |

---

## 관련 파일

- **본 파일**: `com/megacrit/cardcrawl/monsters/city/Byrd.java`
- **파워**:
  - `com.megacrit.cardcrawl.powers.FlightPower`
  - `com.megacrit.cardcrawl.powers.StrengthPower`
- **액션**:
  - `AnimateFastAttackAction`
  - `AnimateSlowAttackAction`
  - `DamageAction`
  - `ApplyPowerAction`
  - `ChangeStateAction`
  - `SetAnimationAction`
  - `TextAboveCreatureAction`
  - `SFXAction`
  - `TalkAction`

---

## 참고사항

1. **Flight 메커니즘**: 핵심 메커니즘, Flight 파워가 0이 되면 GROUNDED 상태로 전환
2. **GROUNDED 패턴**: STUNNED → Headbutt → GO_AIRBORNE (고정 순서)
3. **Peck 횟수**: 5-6회 (이전 문서의 3회는 오류)
4. **Caw 효과**: 자신에게 Strength 1 (이전 문서의 "플레이어에게 Weak"는 오류)
5. **상태별 히트박스**: FLYING과 GROUNDED에서 히트박스 위치가 다름
6. **애니메이션**: 2개의 별도 애니메이션 세트 사용 (flying, grounded)
7. **복수 등장**: "3_Byrds", "4_Byrds" 인카운터로 여러 마리 등장 가능
