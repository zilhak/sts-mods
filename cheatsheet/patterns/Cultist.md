# 광신자 (Cultist)

## 기본 정보

**클래스명**: `Cultist`
**전체 경로**: `com.megacrit.cardcrawl.monsters.exordium.Cultist`
**ID**: `"Cultist"`
**타입**: 일반 적 (NORMAL)
**등장 지역**: 1막 (Exordium)

---

## HP 정보

| 난이도 | HP 범위 |
|--------|---------|
| 기본 (A0-A6) | 48-54 |
| A7+ | 50-56 |

**코드 위치**: 46-50줄

```java
if (AbstractDungeon.ascensionLevel >= 7) {
    setHp(50, 56);
} else {
    setHp(48, 54);
}
```

---

## 생성자 정보

### 주요 생성자
```java
public Cultist(float x, float y, boolean talk)
```

**파라미터**:
- `x`: X 좌표
- `y`: Y 좌표
- `talk`: 대사 출력 여부

### 보조 생성자
```java
public Cultist(float x, float y)
```
기본적으로 `talk = true`로 호출

---

## 패턴 정보

### 패턴 1: 주문 (Incantation)

**바이트 값**: `3`
**의도**: `BUFF`
**발동 조건**: 첫 번째 턴에 무조건 사용

**효과**:
- 자신에게 **Ritual** 파워 부여

**Ritual 수치**:
| 난이도 | Ritual 수치 |
|--------|------------|
| 기본 (A0-A1) | 3 |
| A2-A16 | 4 |
| A17+ | 5 (4 + 1) |

**코드 위치**: 55-59줄, 94-99줄

```java
// 생성자에서 설정
if (AbstractDungeon.ascensionLevel >= 2) {
    this.ritualAmount = 4;
} else {
    this.ritualAmount = 3;
}

// takeTurn에서 실행
case 3:
    if (AbstractDungeon.ascensionLevel >= 17) {
        AbstractDungeon.actionManager.addToBottom(
            new ApplyPowerAction(this, this,
                new RitualPower(this, this.ritualAmount + 1, false))
        );
    } else {
        AbstractDungeon.actionManager.addToBottom(
            new ApplyPowerAction(this, this,
                new RitualPower(this, this.ritualAmount, false))
        );
    }
    break;
```

**수정 포인트**:
- Ritual 수치 변경: `this.ritualAmount` 또는 A17 조건 수정
- 발동 조건 변경: `getMove()` 메서드의 155-159줄

---

### 패턴 2: 어둠의 일격 (Dark Strike)

**바이트 값**: `1`
**의도**: `ATTACK`
**데미지**: 6 (고정)
**발동 조건**: 두 번째 턴부터 계속 반복

**효과**:
- 플레이어에게 **6 데미지**

**코드 위치**: 33줄, 61줄, 102-106줄

```java
// 데미지 정의
private static final int ATTACK_DMG = 6;

// 생성자에서 데미지 등록
this.damage.add(new DamageInfo((AbstractCreature)this, 6));

// takeTurn에서 실행
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

**수정 포인트**:
- 데미지 변경:
  - `ATTACK_DMG` 상수 수정 (33줄)
  - 생성자의 데미지 등록 부분 수정 (61줄)
- 공격 효과 변경: `AttackEffect` 변경 가능

---

## AI 로직 (getMove)

**코드 위치**: 154-163줄

```java
protected void getMove(int num) {
    if (this.firstMove) {
        this.firstMove = false;
        setMove(INCANTATION_NAME, (byte)3, AbstractMonster.Intent.BUFF);
        return;
    }

    setMove((byte)1, AbstractMonster.Intent.ATTACK,
        ((DamageInfo)this.damage.get(0)).base);
}
```

**로직 설명**:
1. 첫 번째 턴: 무조건 Incantation (패턴 3)
2. 두 번째 턴 이후: 계속 Dark Strike (패턴 1)

**수정 포인트**:
- 패턴 순서 변경
- 랜덤 패턴 추가
- 조건부 패턴 변경

---

## 특수 동작

### 대사 시스템 (talky)

**변수**: `private boolean talky`
**설정 위치**: 63-66줄

```java
this.talky = talk;
if (Settings.FAST_MODE) {
    this.talky = false;
}
```

Incantation 사용 시 30% 확률로 대사 출력 (84-92줄)

### 사망 시 동작 (die)

**코드 위치**: 137-150줄

```java
public void die() {
    playDeathSfx();
    this.state.setTimeScale(0.1F);
    useShakeAnimation(5.0F);
    if (this.talky && this.saidPower) {
        AbstractDungeon.effectList.add(
            new SpeechBubble(this.hb.cX + this.dialogX,
                this.hb.cY + this.dialogY, 2.5F, DIALOG[2], false)
        );
        this.deathTimer += 1.5F;
    }
    super.die();
}
```

**특징**:
- 사망 시 음성 재생
- Incantation 사용했으면 대사 출력

---

## 수정 예시

### 1. Ritual 수치 증가

```java
// Level25Patches.java 참조
@SpirePatch(
    cls = "com.megacrit.cardcrawl.monsters.exordium.Cultist",
    method = SpirePatch.CONSTRUCTOR,
    paramtypez = { float.class, float.class, boolean.class }
)
public static class CultistRitualPatch {
    @SpirePostfixPatch
    public static void Postfix(Cultist __instance, float x, float y, boolean talk) {
        if (AbstractDungeon.isAscensionMode && AbstractDungeon.ascensionLevel >= 25) {
            // Ritual을 추가로 증가시키려면 ritualAmount 필드에 접근 필요
            // 또는 takeTurn 패치로 추가 Ritual 부여
        }
    }
}
```

### 2. 공격 데미지 감소

```java
@SpirePatch(
    cls = "com.megacrit.cardcrawl.monsters.exordium.Cultist",
    method = SpirePatch.CONSTRUCTOR,
    paramtypez = { float.class, float.class, boolean.class }
)
public static class CultistDamagePatch {
    @SpirePostfixPatch
    public static void Postfix(Cultist __instance, float x, float y, boolean talk) {
        if (AbstractDungeon.isAscensionMode && AbstractDungeon.ascensionLevel >= 25) {
            // 데미지 감소
            if (!__instance.damage.isEmpty()) {
                DamageInfo damageInfo = __instance.damage.get(0);
                int originalDamage = damageInfo.base;
                damageInfo.base = Math.max(1, originalDamage - 2);
            }
        }
    }
}
```

---

## 중요 필드

| 필드명 | 타입 | 설명 |
|--------|------|------|
| `firstMove` | boolean | 첫 턴 여부 |
| `saidPower` | boolean | Incantation 대사 출력 여부 |
| `ritualAmount` | int | Ritual 파워 수치 |
| `talky` | boolean | 대사 출력 여부 |

---

## 관련 파일

- **본 파일**: `com/megacrit/cardcrawl/monsters/exordium/Cultist.java`
- **파워**: `com.megacrit.cardcrawl.powers.RitualPower`
- **액션**:
  - `AnimateSlowAttackAction`
  - `DamageAction`
  - `ApplyPowerAction`
  - `TalkAction`

---

## 참고사항

1. **Ritual 파워**: 매 턴 시작 시 Ritual 수치만큼 힘(Strength) 획득
2. **멀티 인카운터**: "Murder of Cultists" 키로 3-4마리 등장
3. **대사 시스템**: 빠른 모드에서는 자동으로 비활성화
4. **첫 턴 고정**: 첫 턴은 항상 Incantation, 변경 시 주의
