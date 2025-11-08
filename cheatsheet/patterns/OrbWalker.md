# 오브 워커 (Orb Walker)

## 기본 정보

**클래스명**: `OrbWalker`
**전체 경로**: `com.megacrit.cardcrawl.monsters.beyond.OrbWalker`
**ID**: `"Orb Walker"`
**타입**: 일반 적 (NORMAL)
**등장 지역**: 3막 (The Beyond)
**인카운터명**: `"Double Orb Walker"` (2마리 동시 등장)

---

## HP 정보

| 난이도 | HP 범위 |
|--------|---------|
| 기본 (A0-A6) | 90-96 |
| A7+ | 92-102 |

**코드 위치**: 41-45줄

```java
if (AbstractDungeon.ascensionLevel >= 7) {
    setHp(92, 102);
} else {
    setHp(90, 96);
}
```

---

## 생성자 정보

### 주요 생성자
```java
public OrbWalker(float x, float y)
```

**파라미터**:
- `x`: X 좌표
- `y`: Y 좌표

**특징**:
- 히트박스: (-20.0F, -14.0F, 280.0F, 250.0F)
- 애니메이션: `images/monsters/theForest/orbWalker/skeleton.*`
- 초기 애니메이션: "Idle" (루프, 대문자 I)

---

## 패턴 정보

### 패턴 1: 레이저 (Laser)

**바이트 값**: `1`
**의도**: `ATTACK_DEBUFF`
**발동 조건**: AI 로직에 따름 (60% 확률)

**데미지**:
| 난이도 | 데미지 |
|--------|--------|
| 기본 (A0-A1) | 10 |
| A2+ | 11 |

**효과**:
- 플레이어에게 데미지
- 덱과 버리는 더미에 **화상(Burn) 1장** 추가

**코드 위치**: 35줄, 49줄, 55줄, 86-90줄, 105-106줄

```java
// 데미지 상수
public static final int LASER_DMG = 10;
public static final int A_2_LASER_DMG = 11;

// 생성자에서 설정
if (AbstractDungeon.ascensionLevel >= 2) {
    this.laserDmg = 11;
} else {
    this.laserDmg = 10;
}

// 데미지 등록 (인덱스 0)
this.damage.add(new DamageInfo((AbstractCreature)this, this.laserDmg));

// takeTurn 실행
case 1:
    AbstractDungeon.actionManager.addToBottom(
        new ChangeStateAction(this, "ATTACK")
    );
    AbstractDungeon.actionManager.addToBottom(
        new WaitAction(0.4F)
    );
    AbstractDungeon.actionManager.addToBottom(
        new DamageAction(
            (AbstractCreature)AbstractDungeon.player,
            this.damage.get(0),
            AbstractGameAction.AttackEffect.FIRE
        )
    );
    AbstractDungeon.actionManager.addToBottom(
        new MakeTempCardInDiscardAndDeckAction((AbstractCard)new Burn())
    );
    break;
```

**특징**:
- 공격 애니메이션 ("ATTACK" 상태 변경)
- 화염 공격 이펙트
- 화상 카드가 덱과 버리는 더미에 무작위로 추가

**수정 포인트**:
- 데미지 변경: `laserDmg` 필드
- 화상 개수 변경: `MakeTempCardInDiscardAndDeckAction` 호출 추가
- 이펙트 변경: `AttackEffect.FIRE` 수정

---

### 패턴 2: 발톱 (Claw)

**바이트 값**: `2`
**의도**: `ATTACK`
**발동 조건**: AI 로직에 따름 (40% 확률)

**데미지**:
| 난이도 | 데미지 |
|--------|--------|
| 기본 (A0-A1) | 15 |
| A2+ | 16 |

**효과**:
- 플레이어에게 **강력한 데미지**

**코드 위치**: 30줄, 48줄, 56줄, 81-83줄, 99-100줄

```java
// 데미지 상수
public static final int CLAW_DMG = 15;
public static final int A_2_CLAW_DMG = 16;

// 생성자에서 설정
if (AbstractDungeon.ascensionLevel >= 2) {
    this.clawDmg = 16;
} else {
    this.clawDmg = 15;
}

// 데미지 등록 (인덱스 1)
this.damage.add(new DamageInfo((AbstractCreature)this, this.clawDmg));

// takeTurn 실행
case 2:
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

**특징**:
- 슬로우 공격 애니메이션
- 무거운 참격 이펙트 (SLASH_HEAVY)
- 레이저보다 높은 데미지

**수정 포인트**:
- 데미지 변경: `clawDmg` 필드
- 이펙트 변경: `AttackEffect.SLASH_HEAVY` 수정

---

## 특수 동작

### 전투 시작 시 (usePreBattleAction)

**효과**: **일반 힘 상승(GenericStrengthUpPower)** 부여

**코드 위치**: 67-74줄

```java
public void usePreBattleAction() {
    if (AbstractDungeon.ascensionLevel >= 17) {
        AbstractDungeon.actionManager.addToBottom(
            new ApplyPowerAction(
                (AbstractCreature)this,
                (AbstractCreature)this,
                (AbstractPower)new GenericStrengthUpPower((AbstractCreature)this, MOVES[0], 5)
            )
        );
    } else {
        AbstractDungeon.actionManager.addToBottom(
            new ApplyPowerAction(
                (AbstractCreature)this,
                (AbstractCreature)this,
                (AbstractPower)new GenericStrengthUpPower((AbstractCreature)this, MOVES[0], 3)
            )
        );
    }
}
```

**GenericStrengthUpPower 설명**:
- 일반 힘(Strength) 파워와 유사
- 매 턴 공격 데미지에 추가

**초기 힘 수치**:
| 난이도 | 힘 |
|--------|----|
| 기본 (A0-A16) | 3 |
| A17+ | 5 |

**특징**:
- 전투 시작부터 높은 힘 보유
- A17에서 +2 추가 힘

---

### 피격 애니메이션 (damage)

**코드 위치**: 114-120줄

```java
public void damage(DamageInfo info) {
    super.damage(info);
    if (info.owner != null &&
        info.type != DamageInfo.DamageType.THORNS &&
        info.output > 0) {
        this.state.setAnimation(0, "Hit", false);
        this.state.addAnimation(0, "Idle", true, 0.0F);
    }
}
```

**특징**:
- 가시 데미지가 아닐 때만 피격 애니메이션
- 피격 후 Idle 애니메이션으로 복귀

---

### 상태 변경 (changeState)

**코드 위치**: 123-130줄

```java
public void changeState(String key) {
    switch (key) {
        case "ATTACK":
            this.state.setAnimation(0, "Attack", false);
            this.state.addAnimation(0, "Idle", true, 0.0F);
            break;
    }
}
```

**특징**:
- 레이저 공격 시 "Attack" 애니메이션 재생
- 공격 후 Idle 애니메이션으로 복귀

---

## AI 로직 (getMove)

**코드 위치**: 97-110줄

```java
protected void getMove(int num) {
    if (num < 40) {
        // 40% 확률
        if (!lastTwoMoves((byte)2)) {
            setMove((byte)2, AbstractMonster.Intent.ATTACK,
                ((DamageInfo)this.damage.get(1)).base);
        } else {
            setMove((byte)1, AbstractMonster.Intent.ATTACK_DEBUFF,
                ((DamageInfo)this.damage.get(0)).base);
        }
    } else {
        // 60% 확률
        if (!lastTwoMoves((byte)1)) {
            setMove((byte)1, AbstractMonster.Intent.ATTACK_DEBUFF,
                ((DamageInfo)this.damage.get(0)).base);
        } else {
            setMove((byte)2, AbstractMonster.Intent.ATTACK,
                ((DamageInfo)this.damage.get(1)).base);
        }
    }
}
```

**로직 설명**:

1. **40% 확률** (num < 40)
   - 조건: 직전 2턴에 발톱을 사용하지 않음
   - 결과: **발톱(Claw)** 사용
   - 예외: 직전 2턴 모두 발톱 → **레이저(Laser)** 사용

2. **60% 확률** (num >= 40)
   - 조건: 직전 2턴에 레이저를 사용하지 않음
   - 결과: **레이저(Laser)** 사용
   - 예외: 직전 2턴 모두 레이저 → **발톱(Claw)** 사용

**패턴 특징**:
- 레이저가 더 자주 사용됨 (60% vs 40%)
- 같은 패턴을 3번 연속 사용하지 않음
- `lastTwoMoves()` 체크로 패턴 변화 강제

**예시 시퀀스**:
- 레이저 → 레이저 → 발톱 (강제)
- 발톱 → 발톱 → 레이저 (강제)
- 레이저 → 발톱 → 레이저 (확률)

**수정 포인트**:
- 확률 변경: `num < 40`을 다른 값으로
- 연속 사용 허용: `lastTwoMoves()` 체크 제거
- 패턴 고정: 랜덤 요소 제거

---

## 수정 예시

### 1. 초기 힘 증가 (Ascension 25)

```java
@SpirePatch(
    cls = "com.megacrit.cardcrawl.monsters.beyond.OrbWalker",
    method = "usePreBattleAction"
)
public static class OrbWalkerStrengthIncreasePatch {
    @SpirePostfixPatch
    public static void Postfix(OrbWalker __instance) {
        if (AbstractDungeon.isAscensionMode &&
            AbstractDungeon.ascensionLevel >= 25) {
            // 추가 힘 2
            AbstractDungeon.actionManager.addToBottom(
                new ApplyPowerAction(
                    __instance,
                    __instance,
                    new GenericStrengthUpPower(__instance, "Strength", 2)
                )
            );
        }
    }
}
```

### 2. 레이저 화상 개수 증가

```java
@SpirePatch(
    cls = "com.megacrit.cardcrawl.monsters.beyond.OrbWalker",
    method = "takeTurn"
)
public static class OrbWalkerBurnIncreasePatch {
    @SpirePostfixPatch
    public static void Postfix(OrbWalker __instance) {
        if (AbstractDungeon.isAscensionMode &&
            AbstractDungeon.ascensionLevel >= 25) {
            // 레이저 사용 시 (nextMove == 1) 추가 화상
            if (__instance.nextMove == 1) {
                AbstractDungeon.actionManager.addToBottom(
                    new MakeTempCardInDiscardAndDeckAction(new Burn())
                );
            }
        }
    }
}
```

### 3. 데미지 증가

```java
@SpirePatch(
    cls = "com.megacrit.cardcrawl.monsters.beyond.OrbWalker",
    method = SpirePatch.CONSTRUCTOR
)
public static class OrbWalkerDamagePatch {
    @SpirePostfixPatch
    public static void Postfix(OrbWalker __instance, float x, float y) {
        if (AbstractDungeon.isAscensionMode &&
            AbstractDungeon.ascensionLevel >= 25) {
            // 레이저 데미지 +2 (인덱스 0)
            if (!__instance.damage.isEmpty()) {
                __instance.damage.get(0).base += 2;
            }
            // 발톱 데미지 +3 (인덱스 1)
            if (__instance.damage.size() > 1) {
                __instance.damage.get(1).base += 3;
            }
        }
    }
}
```

### 4. 패턴 확률 변경

```java
@SpirePatch(
    cls = "com.megacrit.cardcrawl.monsters.beyond.OrbWalker",
    method = "getMove"
)
public static class OrbWalkerMovePatternPatch {
    @SpirePrefixPatch
    public static SpireReturn<Void> Prefix(OrbWalker __instance, int num) {
        if (AbstractDungeon.isAscensionMode &&
            AbstractDungeon.ascensionLevel >= 25) {
            // 50% 균등 확률로 변경
            if (num < 50) {
                if (!ReflectionHacks.privateMethod(AbstractMonster.class, "lastTwoMoves", byte.class)
                        .invoke(__instance, (byte)2)) {
                    ReflectionHacks.privateMethod(AbstractMonster.class, "setMove", byte.class, AbstractMonster.Intent.class, int.class)
                        .invoke(__instance, (byte)2, AbstractMonster.Intent.ATTACK,
                            __instance.damage.get(1).base);
                } else {
                    ReflectionHacks.privateMethod(AbstractMonster.class, "setMove", byte.class, AbstractMonster.Intent.class, int.class)
                        .invoke(__instance, (byte)1, AbstractMonster.Intent.ATTACK_DEBUFF,
                            __instance.damage.get(0).base);
                }
            } else {
                if (!ReflectionHacks.privateMethod(AbstractMonster.class, "lastTwoMoves", byte.class)
                        .invoke(__instance, (byte)1)) {
                    ReflectionHacks.privateMethod(AbstractMonster.class, "setMove", byte.class, AbstractMonster.Intent.class, int.class)
                        .invoke(__instance, (byte)1, AbstractMonster.Intent.ATTACK_DEBUFF,
                            __instance.damage.get(0).base);
                } else {
                    ReflectionHacks.privateMethod(AbstractMonster.class, "setMove", byte.class, AbstractMonster.Intent.class, int.class)
                        .invoke(__instance, (byte)2, AbstractMonster.Intent.ATTACK,
                            __instance.damage.get(1).base);
                }
            }
            return SpireReturn.Return(null);
        }
        return SpireReturn.Continue();
    }
}
```

---

## 중요 필드

| 필드명 | 타입 | 설명 |
|--------|------|------|
| `clawDmg` | int | 발톱 공격 데미지 |
| `laserDmg` | int | 레이저 공격 데미지 |

---

## 관련 파일

- **본 파일**: `com/megacrit/cardcrawl/monsters/beyond/OrbWalker.java`
- **파워**: `com.megacrit.cardcrawl.powers.GenericStrengthUpPower`
- **카드**: `com.megacrit.cardcrawl.cards.status.Burn`
- **액션**:
  - `AnimateSlowAttackAction`
  - `DamageAction`
  - `ApplyPowerAction`
  - `ChangeStateAction`
  - `WaitAction`
  - `MakeTempCardInDiscardAndDeckAction`
  - `RollMoveAction`

---

## 참고사항

1. **GenericStrengthUpPower**: 일반 힘 파워, 공격 데미지에 추가
2. **Double Orb Walker**: 2마리가 동시에 등장하는 인카운터
3. **화상 카드**: 덱과 버리는 더미에 무작위로 추가되는 상태이상 카드
4. **패턴 확률**: 레이저(60%) > 발톱(40%)
5. **연속 패턴 제한**: 같은 패턴 3번 연속 사용 불가
6. **A17 보너스**: 초기 힘 +2 (총 5)
7. **상태 변경 시스템**: 레이저 사용 시 "ATTACK" 애니메이션
8. **피격 애니메이션**: 가시 데미지가 아닐 때만 "Hit" 애니메이션
