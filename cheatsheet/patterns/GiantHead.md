# 거대한 머리 (Giant Head)

## 기본 정보

**클래스명**: `GiantHead`
**전체 경로**: `com.megacrit.cardcrawl.monsters.beyond.GiantHead`
**ID**: `"GiantHead"`
**타입**: 엘리트 (ELITE)
**등장 지역**: 3막 (Beyond)

---

## HP 정보

| 난이도 | HP |
|--------|-----|
| 기본 (A0-A7) | 500 |
| A8+ | 520 |

**코드 위치**: 53-57줄

```java
if (AbstractDungeon.ascensionLevel >= 8) {
    setHp(520, 520);
} else {
    setHp(500, 500);
}
```

---

## 생성자 정보

### 주요 생성자
```java
public GiantHead()
```

**특징**:
- X: -70.0F, Y: 40.0F
- 히트박스: 0.0F, -40.0F, 460.0F, 300.0F
- 애니메이션: `images/monsters/theForest/head/skeleton.*`

---

## 특수 메커니즘: COUNT 시스템

**핵심 필드**: `private int count = 5`

**동작 원리**:
- 전투 시작 시 COUNT = 5
- A18+에서는 COUNT = 4로 시작 (usePreBattleAction에서 -1)
- COUNT가 1 이하가 되면 "IT IS TIME" 패턴 시작
- 각 턴마다 COUNT가 감소하며 데미지 증가

**코드 위치**: 37줄, 76-81줄, 157-163줄

```java
// 생성자
private int count = 5;

// usePreBattleAction
public void usePreBattleAction() {
    AbstractDungeon.actionManager.addToBottom(
        new ApplyPowerAction(this, this,
            new SlowPower(this, 0))
    );
    if (AbstractDungeon.ascensionLevel >= 18) {
        this.count--;  // A18+에서는 4부터 시작
    }
}

// getMove에서 COUNT 체크
if (this.count <= 1) {
    if (this.count > -6) {
        this.count--;
    }
    setMove((byte)2, AbstractMonster.Intent.ATTACK,
        this.startingDeathDmg - this.count * 5);
    return;
}
this.count--;
```

---

## 패턴 정보

### 패턴 1: 응시 (Glare)

**바이트 값**: `1`
**의도**: `DEBUFF`
**발동 조건**: AI 로직, COUNT > 1일 때

**효과**:
- 플레이어에게 **약화(Weak) 1턴** 부여
- COUNT 감소 표시 (`~COUNT...~`)

**코드 위치**: 87-91줄

```java
case 1:
    playSfx();
    AbstractDungeon.actionManager.addToBottom(
        new ShoutAction(this, "#r~" +
            Integer.toString(this.count) + "...~", 1.7F, 1.7F)
    );
    AbstractDungeon.actionManager.addToBottom(
        new ApplyPowerAction(AbstractDungeon.player, this,
            new WeakPower(AbstractDungeon.player, 1, true), 1)
    );
    break;
```

**특징**:
- 음성 효과 랜덤 재생 (VO_GIANTHEAD_1A/1B/1C)
- 빨간색 텍스트로 COUNT 표시

**수정 포인트**:
- 약화 턴 수 변경: `new WeakPower(..., 1, ...)` 수정
- COUNT 표시 제거: `ShoutAction` 제거

---

### 패턴 2: 때가 되었다 (It Is Time)

**바이트 값**: `2`
**의도**: `ATTACK`
**발동 조건**: COUNT <= 1

**데미지 계산**:
```
데미지 = startingDeathDmg - count * 5
```

**데미지 진행**:
| COUNT | 계산식 | A0-A2 데미지 | A3+ 데미지 |
|-------|--------|--------------|------------|
| 1 | 30 - 1*5 | 25 | 35 |
| 0 | 30 - 0*5 | 30 | 40 |
| -1 | 30 - (-1)*5 | 35 | 45 |
| -2 | 30 - (-2)*5 | 40 | 50 |
| -3 | 30 - (-3)*5 | 45 | 55 |
| -4 | 30 - (-4)*5 | 50 | 60 |
| -5 | 30 - (-5)*5 | 55 | 65 |
| -6 | 30 - (-6)*5 | 60 | 70 |

**startingDeathDmg 설정**:
| 난이도 | startingDeathDmg |
|--------|------------------|
| A0-A2 | 30 |
| A3+ | 40 |

**코드 위치**: 59-63줄, 104-113줄, 157-163줄

```java
// 생성자에서 설정
if (AbstractDungeon.ascensionLevel >= 3) {
    this.startingDeathDmg = 40;
} else {
    this.startingDeathDmg = 30;
}

// 데미지 인덱스 등록 (총 8개)
this.damage.add(new DamageInfo(this, this.startingDeathDmg));       // index 1
this.damage.add(new DamageInfo(this, this.startingDeathDmg + 5));   // index 2
this.damage.add(new DamageInfo(this, this.startingDeathDmg + 10));  // index 3
this.damage.add(new DamageInfo(this, this.startingDeathDmg + 15));  // index 4
this.damage.add(new DamageInfo(this, this.startingDeathDmg + 20));  // index 5
this.damage.add(new DamageInfo(this, this.startingDeathDmg + 25));  // index 6
this.damage.add(new DamageInfo(this, this.startingDeathDmg + 30));  // index 7

// takeTurn 실행
case 2:
    playSfx();
    AbstractDungeon.actionManager.addToBottom(
        new ShoutAction(this, getTimeQuote(), 1.7F, 2.0F)
    );
    index = 1 - this.count;  // COUNT가 낮을수록 index 증가
    if (index > 7) {
        index = 7;  // 최대 인덱스 제한
    }
    AbstractDungeon.actionManager.addToBottom(
        new DamageAction(AbstractDungeon.player,
            this.damage.get(index),
            AbstractGameAction.AttackEffect.SMASH)
    );
    break;

// getMove에서 패턴 선택
if (this.count <= 1) {
    if (this.count > -6) {
        this.count--;
    }
    setMove((byte)2, AbstractMonster.Intent.ATTACK,
        this.startingDeathDmg - this.count * 5);
    return;
}
```

**특징**:
- COUNT가 -6 이하로 내려가지 않음
- 랜덤 대사 출력 (DIALOG[0-3])
- 음성 효과 랜덤 재생
- 공격 이펙트: SMASH

**수정 포인트**:
- 시작 데미지 변경: `startingDeathDmg` 필드
- 데미지 증가량 변경: `this.count * 5` 계산식
- COUNT 최소값 변경: `if (this.count > -6)` 조건

---

### 패턴 3: 계산 (Count)

**바이트 값**: `3`
**의도**: `ATTACK`
**발동 조건**: AI 로직, COUNT > 1일 때

**데미지**: 13 (모든 난이도 동일)

**효과**:
- 플레이어에게 **13 데미지**
- COUNT 감소 표시 (`~COUNT...~`)

**코드 위치**: 36줄, 65줄, 98-103줄

```java
// 데미지 상수
private static final int COUNT_DMG = 13;

// 생성자에서 데미지 등록 (index 0)
this.damage.add(new DamageInfo(this, 13));

// takeTurn 실행
case 3:
    playSfx();
    AbstractDungeon.actionManager.addToBottom(
        new ShoutAction(this, "#r~" +
            Integer.toString(this.count) + "...~", 1.7F, 1.7F)
    );
    AbstractDungeon.actionManager.addToBottom(
        new DamageAction(AbstractDungeon.player,
            this.damage.get(0),
            AbstractGameAction.AttackEffect.FIRE)
    );
    break;
```

**특징**:
- 공격 이펙트: FIRE
- 음성 효과 랜덤 재생
- COUNT 표시

**수정 포인트**:
- 데미지 변경: `COUNT_DMG` 상수 또는 damage.get(0) 수정

---

## 특수 동작

### 전투 시작 (usePreBattleAction)

**효과**:
- 자신에게 **느림(Slow)** 파워 부여 (수치 0, 시각 효과만)
- A18+에서 COUNT를 1 감소 (5 → 4)

**코드 위치**: 76-81줄

```java
public void usePreBattleAction() {
    AbstractDungeon.actionManager.addToBottom(
        new ApplyPowerAction(this, this,
            new SlowPower(this, 0))
    );
    if (AbstractDungeon.ascensionLevel >= 18) {
        this.count--;
    }
}
```

**특징**:
- Slow 파워는 실제 효과 없음 (0)
- 시각적으로 느린 몬스터임을 표현

### 사망 시 (die)

**코드 위치**: 150-153줄

```java
public void die() {
    super.die();
    playDeathSfx();
}
```

**특징**:
- 랜덤 사망 음성 (VO_GIANTHEAD_2A/2B/2C)

---

## AI 로직 (getMove)

**코드 위치**: 156-182줄

### COUNT <= 1: "It Is Time" 모드

```java
if (this.count <= 1) {
    if (this.count > -6) {
        this.count--;
    }
    setMove((byte)2, AbstractMonster.Intent.ATTACK,
        this.startingDeathDmg - this.count * 5);
    return;
}
this.count--;
```

**동작**:
- COUNT가 1 이하가 되면 계속 패턴 2 사용
- 매 턴 COUNT 감소로 데미지 증가
- COUNT는 -6까지만 감소

### COUNT > 1: 정상 패턴

**num < 50 (50% 확률)**:
```java
if (num < 50) {
    if (!lastTwoMoves((byte)1)) {
        setMove((byte)1, AbstractMonster.Intent.DEBUFF);  // Glare
    } else {
        setMove((byte)3, AbstractMonster.Intent.ATTACK, 13);  // Count
    }
}
```

**로직**:
- 이전 2턴 동안 Glare를 사용하지 않았으면 → Glare (패턴 1)
- 이전 2턴 동안 Glare를 2번 사용했으면 → Count (패턴 3)

**num >= 50 (50% 확률)**:
```java
else if (!lastTwoMoves((byte)3)) {
    setMove((byte)3, AbstractMonster.Intent.ATTACK, 13);  // Count
} else {
    setMove((byte)1, AbstractMonster.Intent.DEBUFF);  // Glare
}
```

**로직**:
- 이전 2턴 동안 Count를 사용하지 않았으면 → Count (패턴 3)
- 이전 2턴 동안 Count를 2번 사용했으면 → Glare (패턴 1)

**수정 포인트**:
- 확률 변경: `if (num < 50)` 조건 수정
- COUNT 임계값 변경: `if (this.count <= 1)` 조건
- 패턴 순서 변경

---

## 수정 예시

### 1. COUNT 시작값 변경

```java
@SpirePatch(
    cls = "com.megacrit.cardcrawl.monsters.beyond.GiantHead",
    method = SpirePatch.CONSTRUCTOR
)
public static class GiantHeadCountPatch {
    @SpirePostfixPatch
    public static void Postfix(GiantHead __instance) {
        if (AbstractDungeon.isAscensionMode &&
            AbstractDungeon.ascensionLevel >= 25) {
            // 리플렉션으로 count 필드 접근 필요
            try {
                Field countField = GiantHead.class.getDeclaredField("count");
                countField.setAccessible(true);
                countField.set(__instance, 3);  // COUNT 3으로 시작
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
```

### 2. "It Is Time" 데미지 증가

```java
@SpirePatch(
    cls = "com.megacrit.cardcrawl.monsters.beyond.GiantHead",
    method = SpirePatch.CONSTRUCTOR
)
public static class GiantHeadDeathDamagePatch {
    @SpirePostfixPatch
    public static void Postfix(GiantHead __instance) {
        if (AbstractDungeon.isAscensionMode &&
            AbstractDungeon.ascensionLevel >= 25) {
            // startingDeathDmg 필드 변경
            try {
                Field dmgField = GiantHead.class
                    .getDeclaredField("startingDeathDmg");
                dmgField.setAccessible(true);
                int currentDmg = dmgField.getInt(__instance);
                dmgField.set(__instance, currentDmg + 10);  // +10 데미지

                // damage 리스트도 다시 생성 필요
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
```

### 3. Count 패턴 데미지 증가

```java
@SpirePatch(
    cls = "com.megacrit.cardcrawl.monsters.beyond.GiantHead",
    method = SpirePatch.CONSTRUCTOR
)
public static class GiantHeadCountDamagePatch {
    @SpirePostfixPatch
    public static void Postfix(GiantHead __instance) {
        if (AbstractDungeon.isAscensionMode &&
            AbstractDungeon.ascensionLevel >= 25) {
            // damage.get(0) 수정 (Count 패턴)
            if (!__instance.damage.isEmpty()) {
                __instance.damage.get(0).base += 3;  // 13 → 16
            }
        }
    }
}
```

---

## 중요 필드

| 필드명 | 타입 | 설명 |
|--------|------|------|
| `count` | int | 카운트다운 값 (5부터 시작) |
| `startingDeathDmg` | int | "It Is Time" 시작 데미지 |

---

## 관련 파일

- **본 파일**: `com/megacrit/cardcrawl/monsters/beyond/GiantHead.java`
- **파워**:
  - `com.megacrit.cardcrawl.powers.SlowPower`
  - `com.megacrit.cardcrawl.powers.WeakPower`
- **액션**:
  - `ShoutAction`
  - `ApplyPowerAction`
  - `DamageAction`
  - `RollMoveAction`
  - `SFXAction`

---

## 참고사항

1. **COUNT 시스템**: 핵심 메커니즘, 턴이 지날수록 강해짐
2. **A18 변경점**: COUNT 4부터 시작
3. **데미지 증가**: "It Is Time" 모드에서 매 턴 5씩 증가
4. **최대 데미지**: A3+에서 70 (count = -6일 때)
5. **음성 효과**: 공격/사망 시 랜덤 재생
6. **Slow 파워**: 시각 효과만, 실제 효과 없음
7. **복잡한 AI**: 이전 2턴 패턴 체크
8. **데미지 인덱스**: 8개의 데미지 정보 사전 등록
