# 가시 구체 (Spiker)

## 기본 정보

**클래스명**: `Spiker`
**전체 경로**: `com.megacrit.cardcrawl.monsters.beyond.Spiker`
**ID**: `"Spiker"`
**타입**: 일반 적 (NORMAL)
**등장 지역**: 3막 (The Beyond)
**인카운터명**: `"Ancient Shapes"` (공통 인카운터)

---

## HP 정보

| 난이도 | HP 범위 |
|--------|---------|
| 기본 (A0-A6) | 42-56 |
| A7+ | 44-60 |

**코드 위치**: 55-59줄

```java
if (AbstractDungeon.ascensionLevel >= 7) {
    setHp(44, 60);
} else {
    setHp(42, 56);
}
```

---

## 생성자 정보

### 주요 생성자
```java
public Spiker(float x, float y)
```

**파라미터**:
- `x`: X 좌표
- `y`: Y 좌표 (실제 표시는 y + 10.0F)

**특징**:
- 히트박스: (-8.0F, -10.0F, 150.0F, 150.0F)
- 애니메이션: `images/monsters/theForest/spiker/skeleton.*`
- 초기 애니메이션: "idle" (루프)

---

## 패턴 정보

### 패턴 1: 공격 (Attack)

**바이트 값**: `1`
**의도**: `ATTACK`
**발동 조건**: AI 로직에 따름 (50% 확률 또는 가시 카운터 > 5)

**데미지**:
| 난이도 | 데미지 |
|--------|--------|
| 기본 (A0-A1) | 7 |
| A2+ | 9 |

**코드 위치**: 38-40줄, 61-67줄, 87-89줄, 109-110줄

```java
// 데미지 상수
private static final int ATTACK_DMG = 7;
private static final int A_2_ATTACK_DMG = 9;

// 생성자에서 설정
if (AbstractDungeon.ascensionLevel >= 2) {
    this.attackDmg = 9;
} else {
    this.attackDmg = 7;
}

// 데미지 등록
this.damage.add(new DamageInfo((AbstractCreature)this, this.attackDmg));

// takeTurn 실행
case 1:
    AbstractDungeon.actionManager.addToBottom(
        new AnimateSlowAttackAction((AbstractCreature)this)
    );
    AbstractDungeon.actionManager.addToBottom(
        new DamageAction(
            (AbstractCreature)AbstractDungeon.player,
            this.damage.get(0),
            AbstractGameAction.AttackEffect.SLASH_HORIZONTAL
        )
    );
    break;
```

**특징**:
- 슬로우 공격 애니메이션
- 수평 참격 이펙트

**수정 포인트**:
- 데미지 변경: `attackDmg` 필드 또는 생성자 로직
- 이펙트 변경: `AttackEffect` 타입 수정

---

### 패턴 2: 가시 버프 (Buff Thorns)

**바이트 값**: `2`
**의도**: `BUFF`
**발동 조건**: 공격 패턴이 아닐 때 (50% 확률)

**효과**:
- 자신에게 **가시(Thorns) 2** 추가

**가시(Thorns) 수치**: 2 (고정)

**코드 위치**: 41-43줄, 92-93줄, 114줄

```java
// 상수
private static final int BUFF_AMT = 2;
private int thornsCount = 0;

// takeTurn 실행
case 2:
    this.thornsCount++;
    AbstractDungeon.actionManager.addToBottom(
        new ApplyPowerAction(
            (AbstractCreature)this,
            (AbstractCreature)this,
            (AbstractPower)new ThornsPower((AbstractCreature)this, 2),
            2
        )
    );
    break;
```

**특징**:
- 사용할 때마다 `thornsCount` 증가
- 카운터가 5를 초과하면 공격 패턴만 사용

**수정 포인트**:
- 가시 수치 변경: `BUFF_AMT` 상수
- 카운터 임계값 변경: `getMove()` 104줄의 `thornsCount > 5`

---

## 특수 동작

### 전투 시작 시 (usePreBattleAction)

**효과**: 초기 가시(Thorns) 파워 부여

**코드 위치**: 73-80줄

```java
public void usePreBattleAction() {
    if (AbstractDungeon.ascensionLevel >= 17) {
        AbstractDungeon.actionManager.addToBottom(
            new ApplyPowerAction(
                (AbstractCreature)this,
                (AbstractCreature)this,
                (AbstractPower)new ThornsPower((AbstractCreature)this, this.startingThorns + 3)
            )
        );
    } else {
        AbstractDungeon.actionManager.addToBottom(
            new ApplyPowerAction(
                (AbstractCreature)this,
                (AbstractCreature)this,
                (AbstractPower)new ThornsPower((AbstractCreature)this, this.startingThorns)
            )
        );
    }
}
```

**초기 가시 수치**:
| 난이도 | 가시 수치 |
|--------|----------|
| 기본 (A0-A1) | 3 |
| A2-A16 | 4 |
| A17+ | 7 (4 + 3) |

**코드 위치**: 34-36줄, 61-67줄

```java
// 상수
private static final int STARTING_THORNS = 3;
private static final int A_2_STARTING_THORNS = 4;

// 생성자에서 설정
if (AbstractDungeon.ascensionLevel >= 2) {
    this.startingThorns = 4;
} else {
    this.startingThorns = 3;
}
```

**특징**:
- A17에서 추가 3 가시 보너스
- 전투 시작부터 플레이어의 공격에 반격

---

## AI 로직 (getMove)

**코드 위치**: 103-115줄

```java
protected void getMove(int num) {
    // 가시 버프를 5번 초과 사용했으면 공격만 사용
    if (this.thornsCount > 5) {
        setMove((byte)1, AbstractMonster.Intent.ATTACK,
            ((DamageInfo)this.damage.get(0)).base);
        return;
    }

    // 50% 확률로 공격 (단, 직전에 공격하지 않았을 때)
    if (num < 50 && !lastMove((byte)1)) {
        setMove((byte)1, AbstractMonster.Intent.ATTACK,
            ((DamageInfo)this.damage.get(0)).base);
        return;
    }

    // 그 외: 가시 버프
    setMove((byte)2, AbstractMonster.Intent.BUFF);
}
```

**로직 설명**:

1. **가시 카운터 체크** (thornsCount > 5)
   - 조건: 가시 버프를 6번 이상 사용
   - 결과: 이후 공격만 계속 사용

2. **50% 확률 공격** (num < 50)
   - 조건: 랜덤 값 < 50 AND 직전에 공격하지 않음
   - 결과: 공격 패턴 사용

3. **기본 패턴**
   - 조건: 위 조건들에 해당하지 않음
   - 결과: 가시 버프 패턴 사용

**패턴 특징**:
- 가시 버프와 공격을 번갈아 사용하는 경향
- 가시 버프를 많이 사용하면 공격만 사용
- 공격을 2번 연속 사용하지 않음

**수정 포인트**:
- 가시 카운터 임계값 변경 (104줄)
- 공격 확률 변경 (109줄의 `num < 50`)
- 연속 공격 방지 제거 (`!lastMove((byte)1)`)

---

## 수정 예시

### 1. 초기 가시 증가 (Ascension 25)

```java
@SpirePatch(
    cls = "com.megacrit.cardcrawl.monsters.beyond.Spiker",
    method = "usePreBattleAction"
)
public static class SpikerThornsIncreasePatch {
    @SpirePostfixPatch
    public static void Postfix(Spiker __instance) {
        if (AbstractDungeon.isAscensionMode &&
            AbstractDungeon.ascensionLevel >= 25) {
            // 추가 가시 2
            AbstractDungeon.actionManager.addToBottom(
                new ApplyPowerAction(
                    __instance,
                    __instance,
                    new ThornsPower(__instance, 2),
                    2
                )
            );
        }
    }
}
```

### 2. 가시 버프량 증가

```java
@SpirePatch(
    cls = "com.megacrit.cardcrawl.monsters.beyond.Spiker",
    method = "takeTurn"
)
public static class SpikerBuffAmountPatch {
    @SpireInsertPatch(
        locator = BuffThornsLocator.class
    )
    public static void Insert(Spiker __instance) {
        if (AbstractDungeon.isAscensionMode &&
            AbstractDungeon.ascensionLevel >= 25) {
            // 버프 사용 시 추가 가시 1
            AbstractDungeon.actionManager.addToBottom(
                new ApplyPowerAction(
                    __instance,
                    __instance,
                    new ThornsPower(__instance, 1),
                    1
                )
            );
        }
    }

    private static class BuffThornsLocator extends SpireInsertLocator {
        @Override
        public int[] Locate(CtBehavior ctMethodToPatch) throws Exception {
            Matcher finalMatcher = new Matcher.MethodCallMatcher(
                ApplyPowerAction.class, "<init>"
            );
            return LineFinder.findInOrder(ctMethodToPatch, finalMatcher);
        }
    }
}
```

### 3. 데미지 증가

```java
@SpirePatch(
    cls = "com.megacrit.cardcrawl.monsters.beyond.Spiker",
    method = SpirePatch.CONSTRUCTOR
)
public static class SpikerDamagePatch {
    @SpirePostfixPatch
    public static void Postfix(Spiker __instance, float x, float y) {
        if (AbstractDungeon.isAscensionMode &&
            AbstractDungeon.ascensionLevel >= 25) {
            // 데미지 +2
            if (!__instance.damage.isEmpty()) {
                __instance.damage.get(0).base += 2;
            }
        }
    }
}
```

---

## 중요 필드

| 필드명 | 타입 | 설명 |
|--------|------|------|
| `startingThorns` | int | 시작 시 가시 수치 |
| `attackDmg` | int | 공격 데미지 |
| `thornsCount` | int | 가시 버프 사용 횟수 |

---

## 관련 파일

- **본 파일**: `com/megacrit/cardcrawl/monsters/beyond/Spiker.java`
- **파워**: `com.megacrit.cardcrawl.powers.ThornsPower`
- **액션**:
  - `AnimateSlowAttackAction`
  - `DamageAction`
  - `ApplyPowerAction`
  - `RollMoveAction`

---

## 참고사항

1. **Thorns 파워**: 플레이어가 공격할 때마다 가시 수치만큼 반격 데미지
2. **Ancient Shapes**: Spiker, Exploder, Repulsor가 함께 등장하는 인카운터
3. **가시 누적**: 버프를 사용할 때마다 가시가 누적됨 (2씩 증가)
4. **AI 제한**: 가시 버프를 6번 이상 사용하면 공격만 사용
5. **A17 보너스**: 시작 시 +3 가시 추가 (총 7)
