# 반발자 (Repulsor)

## 기본 정보

**클래스명**: `Repulsor`
**전체 경로**: `com.megacrit.cardcrawl.monsters.beyond.Repulsor`
**ID**: `"Repulsor"`
**타입**: 일반 적 (NORMAL)
**등장 지역**: 3막 (The Beyond)
**인카운터 이름**:
- `"Ancient Shapes Weak"` (약한 버전)
- `"Ancient Shapes"` (일반 버전)

---

## HP 정보

| 난이도 | HP 범위 |
|--------|---------|
| 기본 (A0-A6) | 29-35 |
| A7+ | 31-38 |

**코드 위치**: 45-49줄

```java
if (AbstractDungeon.ascensionLevel >= 7) {
    setHp(31, 38);
} else {
    setHp(29, 35);
}
```

**특징**:
- 낮은 HP (3막 기준)
- 일반적으로 여러 마리가 함께 등장

---

## 생성자 정보

### 주요 생성자
```java
public Repulsor(float x, float y)
```

**파라미터**:
- `x`: X 좌표
- `y`: Y 좌표 (생성자에서 +10.0F 보정)

**특징**:
- 크기: 150.0F x 150.0F (작은 크기)
- 히트박스 오프셋: -8.0F, -10.0F
- Y 좌표 자동 보정: `y + 10.0F`

**애니메이션 설정**:
```java
loadAnimation("images/monsters/theForest/repulser/skeleton.atlas",
              "images/monsters/theForest/repulser/skeleton.json", 1.0F);

AnimationState.TrackEntry e = this.state.setAnimation(0, "idle", true);
e.setTime(e.getEndTime() * MathUtils.random());
```

---

## 패턴 정보

### 패턴 1: 혼란 (Daze)

**바이트 값**: `1`
**의도**: `DEBUFF`
**발동 조건**: AI 로직에 따름 (기본 패턴)

**효과**:
- 플레이어 덱에 **Dazed** 카드 추가

**Dazed 수치**:
| 난이도 | Dazed 카드 개수 |
|--------|----------------|
| 모든 난이도 | 2 |

**코드 위치**: 43줄, 68-70줄

```java
// 생성자에서 설정
this.dazeAmt = 2;

// takeTurn 실행
case 1:
    AbstractDungeon.actionManager.addToBottom(
        new MakeTempCardInDrawPileAction(
            (AbstractCard)new Dazed(),
            this.dazeAmt,
            true,  // randomSpot
            true   // autoPosition
        )
    );
    break;
```

**Dazed 카드**:
- 상태이상 카드
- 허비 (Ethereal)
- 카드를 버리면 사라짐

**특징**:
- 랜덤한 위치에 덱에 추가
- 자동으로 위치 조정

**수정 포인트**:
- Dazed 개수 변경: `dazeAmt` 필드
- 추가할 카드 변경: `new Dazed()` 부분

---

### 패턴 2: 공격 (Attack)

**바이트 값**: `2`
**의도**: `ATTACK`
**발동 조건**: AI 로직에 따름 (20% 확률)

**데미지**:
| 난이도 | 데미지 |
|--------|--------|
| 기본 (A0-A1) | 11 |
| A2+ | 13 |

**코드 위치**: 51-55줄, 57줄, 63-67줄

```java
// 생성자에서 설정
if (AbstractDungeon.ascensionLevel >= 2) {
    this.attackDmg = 13;
} else {
    this.attackDmg = 11;
}

// 데미지 등록
this.damage.add(new DamageInfo((AbstractCreature)this, this.attackDmg));

// takeTurn 실행
case 2:
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
- 느린 공격 애니메이션
- SLASH_HORIZONTAL 효과
- 낮은 빈도로 사용

**수정 포인트**:
- 데미지 변경: `attackDmg` 필드
- 발동 확률 변경: `getMove()` 메서드

---

## AI 로직 (getMove)

**코드 위치**: 80-87줄

```java
protected void getMove(int num) {
    // 20% 확률, 직전에 Attack 안 했으면
    if (num < 20 && !lastMove((byte)2)) {
        setMove((byte)2, AbstractMonster.Intent.ATTACK,
            ((DamageInfo)this.damage.get(0)).base);
        return;
    }

    // 기본: Daze
    setMove((byte)1, AbstractMonster.Intent.DEBUFF);
}
```

### AI 우선순위

1. **20% 확률**: `num < 20` && 직전에 Attack 안 했으면 → **Attack**

2. **기본 패턴**: → **Daze**

**특징**:
- 매우 단순한 AI
- 대부분 Daze 사용
- Attack은 가끔만 사용

**수정 포인트**:
- Attack 확률 변경: `num < 20` 부분 수정
- 새로운 패턴 추가 가능

---

## 인카운터 정보

### Ancient Shapes Weak
- 약한 버전의 인카운터
- 보통 2-3마리 등장

### Ancient Shapes
- 일반 버전의 인카운터
- 보통 3-4마리 등장
- 다른 몬스터와 함께 등장 가능

**특징**:
- 낮은 HP로 개별적으로는 약함
- 다수로 등장하여 지속적인 Dazed 카드 추가
- 덱을 오염시키는 전략

---

## 수정 예시

### 1. Dazed 카드 개수 증가

```java
@SpirePatch(
    cls = "com.megacrit.cardcrawl.monsters.beyond.Repulsor",
    method = SpirePatch.CONSTRUCTOR,
    paramtypez = { float.class, float.class }
)
public static class RepulsorDazePatch {
    @SpirePostfixPatch
    public static void Postfix(Repulsor __instance, float x, float y) {
        if (AbstractDungeon.ascensionLevel >= 25) {
            // dazeAmt 필드 변경
            try {
                Field dazeField = Repulsor.class.getDeclaredField("dazeAmt");
                dazeField.setAccessible(true);
                dazeField.setInt(__instance, 3);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
```

### 2. Attack 확률 증가

```java
@SpirePatch(
    cls = "com.megacrit.cardcrawl.monsters.beyond.Repulsor",
    method = "getMove"
)
public static class RepulsorAttackPatch {
    @SpirePrefixPatch
    public static SpireReturn<Void> Prefix(Repulsor __instance, int num) {
        if (AbstractDungeon.ascensionLevel >= 25) {
            // 40% 확률로 Attack
            if (num < 40 && !__instance.lastMove((byte)2)) {
                __instance.setMove((byte)2, AbstractMonster.Intent.ATTACK,
                    ((DamageInfo)__instance.damage.get(0)).base);
                return SpireReturn.Return();
            }
            __instance.setMove((byte)1, AbstractMonster.Intent.DEBUFF);
            return SpireReturn.Return();
        }
        return SpireReturn.Continue();
    }
}
```

### 3. 데미지 증가

```java
@SpirePatch(
    cls = "com.megacrit.cardcrawl.monsters.beyond.Repulsor",
    method = SpirePatch.CONSTRUCTOR,
    paramtypez = { float.class, float.class }
)
public static class RepulsorDamagePatch {
    @SpirePostfixPatch
    public static void Postfix(Repulsor __instance, float x, float y) {
        if (AbstractDungeon.ascensionLevel >= 25) {
            // 데미지 증가
            if (!__instance.damage.isEmpty()) {
                __instance.damage.get(0).base += 3;
            }
        }
    }
}
```

### 4. 새로운 디버프 카드 추가

```java
@SpirePatch(
    cls = "com.megacrit.cardcrawl.monsters.beyond.Repulsor",
    method = "takeTurn"
)
public static class RepulsorNewDebuffPatch {
    @SpirePostfixPatch
    public static void Postfix(Repulsor __instance) {
        if (AbstractDungeon.ascensionLevel >= 25 &&
            __instance.nextMove == 1) {
            // Dazed 외에 Slimed도 추가
            AbstractDungeon.actionManager.addToBottom(
                new MakeTempCardInDrawPileAction(
                    new Slimed(), 1, true, true
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
| `attackDmg` | int | Attack 데미지 |
| `dazeAmt` | int | Dazed 카드 개수 (2) |

---

## 관련 파일

- **본 파일**: `com/megacrit/cardcrawl/monsters/beyond/Repulsor.java`
- **카드**: `com.megacrit.cardcrawl.cards.status.Dazed`
- **액션**:
  - `AnimateSlowAttackAction`
  - `DamageAction`
  - `MakeTempCardInDrawPileAction`
  - `RollMoveAction`

---

## 참고사항

1. **다수 등장**: 보통 2-4마리가 함께 등장
2. **단순한 AI**: 매우 단순한 패턴 (Daze 중심)
3. **낮은 HP**: 개별적으로는 약하지만 다수로 위협적
4. **덱 오염 전략**: Dazed 카드로 플레이어 덱 방해
5. **작은 크기**: 150x150으로 화면에서 작게 보임
6. **인카운터 명칭**: "Ancient Shapes"로 여러 마리 등장
7. **Y 좌표 보정**: 생성자에서 자동으로 +10.0F 적용
