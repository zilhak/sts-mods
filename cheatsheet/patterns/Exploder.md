# 폭발하는 것 (Exploder)

## 기본 정보

**클래스명**: `Exploder`
**전체 경로**: `com.megacrit.cardcrawl.monsters.beyond.Exploder`
**ID**: `"Exploder"`
**타입**: 일반 적 (NORMAL)
**등장 지역**: 3막 (The Beyond)
**인카운터명**: `"Ancient Shapes"` (공통 인카운터)

---

## HP 정보

| 난이도 | HP 범위 |
|--------|---------|
| 기본 (A0-A6) | 30 |
| A7+ | 30-35 |

**코드 위치**: 52-56줄

```java
if (AbstractDungeon.ascensionLevel >= 7) {
    setHp(30, 35);
} else {
    setHp(30, 30);
}
```

**특징**:
- 기본 난이도에서는 HP가 고정 (30)
- A7+에서 HP 범위가 생김 (30-35)

---

## 생성자 정보

### 주요 생성자
```java
public Exploder(float x, float y)
```

**파라미터**:
- `x`: X 좌표
- `y`: Y 좌표 (실제 표시는 y + 10.0F)

**특징**:
- 히트박스: (-8.0F, -10.0F, 150.0F, 150.0F)
- 애니메이션: `images/monsters/theForest/exploder/skeleton.*`
- 초기 애니메이션: "idle" (루프)

---

## 패턴 정보

### 패턴 1: 공격 (Attack)

**바이트 값**: `1`
**의도**: `ATTACK`
**발동 조건**: 처음 2턴 동안만 사용

**데미지**:
| 난이도 | 데미지 |
|--------|--------|
| 기본 (A0-A1) | 9 |
| A2+ | 11 |

**코드 위치**: 36-38줄, 58-62줄, 78-80줄, 95줄

```java
// 데미지 상수
private static final int ATTACK_DMG = 9;
private static final int A_2_ATTACK_DMG = 11;

// 생성자에서 설정
if (AbstractDungeon.ascensionLevel >= 2) {
    this.attackDmg = 11;
} else {
    this.attackDmg = 9;
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
            AbstractGameAction.AttackEffect.FIRE
        )
    );
    break;
```

**특징**:
- 슬로우 공격 애니메이션
- 화염 공격 이펙트 (FIRE)
- 최대 2턴까지만 사용

**수정 포인트**:
- 데미지 변경: `attackDmg` 필드 또는 생성자 로직
- 이펙트 변경: `AttackEffect.FIRE` 수정
- 공격 턴 수 변경: `getMove()` 94줄의 `turnCount < 2`

---

### 패턴 2: 자폭 대기 (Block/Wait)

**바이트 값**: `2`
**의도**: `UNKNOWN`
**발동 조건**: 3턴째부터 사용

**효과**:
- **아무 행동도 하지 않음**
- **Explosive 파워가 작동하여 자폭**

**코드 위치**: 97줄

```java
// getMove에서 설정만 됨 (실제 행동 없음)
else {
    setMove((byte)2, AbstractMonster.Intent.UNKNOWN);
}
```

**특징**:
- takeTurn에 case 2가 없음 (아무 행동 안 함)
- 턴이 끝나면 Explosive 파워가 몬스터를 죽이고 플레이어에게 데미지

**수정 포인트**:
- 자폭 시점 변경: `getMove()` 94줄 수정
- 자폭 데미지 변경: ExplosivePower의 EXPLODE_BASE 수정

---

## 특수 동작

### 전투 시작 시 (usePreBattleAction)

**효과**: **Explosive** 파워 부여

**코드 위치**: 68-70줄

```java
public void usePreBattleAction() {
    AbstractDungeon.actionManager.addToBottom(
        new ApplyPowerAction(
            (AbstractCreature)this,
            (AbstractCreature)this,
            (AbstractPower)new ExplosivePower((AbstractCreature)this, 3)
        )
    );
}
```

**Explosive 파워 설명**:
- 매 턴 종료 시 카운터 감소
- 카운터가 0이 되면 자폭
- 자폭 시 플레이어에게 큰 데미지

**자폭 기본 데미지**: 3 (코드 40줄 `EXPLODE_BASE = 3`)

**Explosive 파워 동작**:
1. 턴 종료 시 카운터 -1
2. 카운터 0 도달 시:
   - 몬스터 즉시 사망
   - 플레이어에게 `currentBlock + EXPLODE_BASE` 데미지
   - currentBlock = 몬스터가 가진 방어도

**특징**:
- 타이머가 있는 자폭형 몬스터
- 2턴 공격 후 3턴째 자폭
- 방어도를 쌓으면 자폭 데미지 증가

---

### 턴 카운터 (takeTurn)

**코드 위치**: 75줄

```java
public void takeTurn() {
    this.turnCount++;  // 매 턴마다 증가
    // ...
}
```

**특징**:
- AI가 턴 수를 추적하여 자폭 타이밍 결정
- 2턴 동안만 공격, 3턴째부터 대기

---

## AI 로직 (getMove)

**코드 위치**: 93-99줄

```java
protected void getMove(int num) {
    if (this.turnCount < 2) {
        setMove((byte)1, AbstractMonster.Intent.ATTACK,
            ((DamageInfo)this.damage.get(0)).base);
    } else {
        setMove((byte)2, AbstractMonster.Intent.UNKNOWN);
    }
}
```

**로직 설명**:

1. **턴 1-2** (turnCount < 2)
   - 결과: 공격 패턴 사용
   - 랜덤 요소 없음 (무조건 공격)

2. **턴 3+** (turnCount >= 2)
   - 결과: UNKNOWN 의도 (아무 행동 안 함)
   - Explosive 파워가 카운트다운 종료 → 자폭

**타임라인**:
- **턴 1**: 공격 (Explosive 카운터 3 → 2)
- **턴 2**: 공격 (Explosive 카운터 2 → 1)
- **턴 3**: 대기 (Explosive 카운터 1 → 0 → 자폭)

**수정 포인트**:
- 공격 턴 수 변경: `turnCount < 2`를 다른 값으로
- UNKNOWN 의도 변경: 다른 의도로 변경 가능
- 자폭 전 행동 추가: case 2에 행동 추가

---

## 수정 예시

### 1. 자폭 데미지 증가 (Ascension 25)

```java
@SpirePatch(
    cls = "com.megacrit.cardcrawl.monsters.beyond.Exploder",
    method = "usePreBattleAction"
)
public static class ExploderExplosivePatch {
    @SpirePostfixPatch
    public static void Postfix(Exploder __instance) {
        if (AbstractDungeon.isAscensionMode &&
            AbstractDungeon.ascensionLevel >= 25) {
            // Explosive 데미지 베이스 증가 (ExplosivePower 수정 필요)
            // 또는 추가 Explosive 파워 부여
            AbstractDungeon.actionManager.addToBottom(
                new ApplyPowerAction(
                    __instance,
                    __instance,
                    new ExplosivePower(__instance, 2),  // 추가 2 데미지
                    2
                )
            );
        }
    }
}
```

### 2. 공격 턴 수 증가

```java
@SpirePatch(
    cls = "com.megacrit.cardcrawl.monsters.beyond.Exploder",
    method = "getMove"
)
public static class ExploderAttackTurnsPatch {
    @SpirePrefixPatch
    public static SpireReturn<Void> Prefix(Exploder __instance, int num) {
        if (AbstractDungeon.isAscensionMode &&
            AbstractDungeon.ascensionLevel >= 25) {
            // 3턴 동안 공격 (기존 2턴)
            int turnCount = ReflectionHacks.getPrivate(__instance, Exploder.class, "turnCount");
            if (turnCount < 3) {
                ReflectionHacks.privateMethod(AbstractMonster.class, "setMove", byte.class, AbstractMonster.Intent.class, int.class)
                    .invoke(__instance, (byte)1, AbstractMonster.Intent.ATTACK,
                        __instance.damage.get(0).base);
            } else {
                ReflectionHacks.privateMethod(AbstractMonster.class, "setMove", byte.class, AbstractMonster.Intent.class)
                    .invoke(__instance, (byte)2, AbstractMonster.Intent.UNKNOWN);
            }
            return SpireReturn.Return(null);
        }
        return SpireReturn.Continue();
    }
}
```

### 3. 자폭 전 방어도 획득

```java
@SpirePatch(
    cls = "com.megacrit.cardcrawl.monsters.beyond.Exploder",
    method = "takeTurn"
)
public static class ExploderBlockBeforeExplodePatch {
    @SpirePostfixPatch
    public static void Postfix(Exploder __instance) {
        if (AbstractDungeon.isAscensionMode &&
            AbstractDungeon.ascensionLevel >= 25) {
            int turnCount = ReflectionHacks.getPrivate(__instance, Exploder.class, "turnCount");
            // 3턴째 (자폭 턴)에 방어도 획득
            if (turnCount >= 2 && __instance.nextMove == 2) {
                AbstractDungeon.actionManager.addToBottom(
                    new GainBlockAction(__instance, __instance, 15)
                );
            }
        }
    }
}
```

---

## 중요 필드

| 필드명 | 타입 | 설명 |
|--------|------|------|
| `turnCount` | int | 현재 턴 수 (0부터 시작) |
| `attackDmg` | int | 공격 데미지 |

---

## 관련 파일

- **본 파일**: `com/megacrit/cardcrawl/monsters/beyond/Exploder.java`
- **파워**: `com.megacrit.cardcrawl.powers.ExplosivePower`
- **액션**:
  - `AnimateSlowAttackAction`
  - `DamageAction`
  - `ApplyPowerAction`
  - `RollMoveAction`

---

## 참고사항

1. **ExplosivePower**: 카운트다운 타이머, 0이 되면 자폭
2. **Ancient Shapes**: Spiker, Exploder, Repulsor가 함께 등장하는 인카운터
3. **자폭 데미지**: `currentBlock + EXPLODE_BASE` (방어도 + 기본값 3)
4. **턴 제한**: 2턴 동안만 공격 가능
5. **타임라인**: 3턴 후 무조건 자폭 (Explosive 카운터 3)
6. **방어도 전략**: 몬스터에게 방어도를 주면 자폭 데미지 증가
7. **UNKNOWN 의도**: 3턴째부터 의도가 물음표로 표시됨
