# 의식 (Ritual)

## 기본 정보

**클래스명**: `RitualPower`
**전체 경로**: `com.megacrit.cardcrawl.powers.RitualPower`
**ID**: `"Ritual"`
**타입**: 버프 (BUFF)
**사용 몬스터**: Cultist (광신자)

---

## 생성자 정보

```java
public RitualPower(AbstractCreature owner, int strAmt, boolean playerControlled)
```

**파라미터**:
- `owner`: 파워를 가질 크리처
- `strAmt`: 매 턴 증가할 힘(Strength) 수치
- `playerControlled`: 플레이어가 제어하는지 여부

**코드 위치**: 17-25줄

```java
this.name = NAME;
this.ID = "Ritual";
this.owner = owner;
this.amount = strAmt;
this.onPlayer = playerControlled;
updateDescription();
loadRegion("ritual");
```

---

## 효과 설명

### 몬스터용 (onPlayer = false)

**발동 타이밍**: 라운드 종료 시 (`atEndOfRound`)
**효과**: 자신에게 `amount`만큼 힘(Strength) 부여

**코드 위치**: 45-53줄

```java
public void atEndOfRound() {
    if (!this.onPlayer)
        if (!this.skipFirst) {
            flash();
            addToBot(new ApplyPowerAction(this.owner, this.owner,
                new StrengthPower(this.owner, this.amount), this.amount));
        } else {
            this.skipFirst = false;
        }
}
```

**특징**:
- 첫 번째 라운드는 스킵 (`skipFirst = true`)
- 두 번째 라운드부터 발동
- 라운드 종료 시점에 발동

### 플레이어용 (onPlayer = true)

**발동 타이밍**: 턴 종료 시 (`atEndOfTurn`)
**효과**: 자신에게 `amount`만큼 힘(Strength) 부여

**코드 위치**: 37-42줄

```java
public void atEndOfTurn(boolean isPlayer) {
    if (isPlayer) {
        flash();
        addToBot(new ApplyPowerAction(this.owner, this.owner,
            new StrengthPower(this.owner, this.amount), this.amount));
    }
}
```

---

## 설명 텍스트

**코드 위치**: 28-34줄

```java
public void updateDescription() {
    if (!this.onPlayer) {
        this.description = DESCRIPTIONS[0] + this.amount + DESCRIPTIONS[1];
    } else {
        this.description = DESCRIPTIONS[2] + this.amount + DESCRIPTIONS[1];
    }
}
```

**설명**:
- 몬스터용: `DESCRIPTIONS[0] + amount + DESCRIPTIONS[1]`
- 플레이어용: `DESCRIPTIONS[2] + amount + DESCRIPTIONS[1]`

---

## 중요 필드

| 필드명 | 타입 | 설명 |
|--------|------|------|
| `skipFirst` | boolean | 첫 라운드 스킵 여부 (기본값: true) |
| `onPlayer` | boolean | 플레이어 제어 여부 |
| `amount` | int | 매 턴 증가할 Strength 수치 |

---

## Cultist 사용 예시

**코드 위치**: `Cultist.java` 94-99줄

```java
// A17+ 조건
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
```

**Ritual 수치**:
| 난이도 | Ritual 수치 |
|--------|------------|
| A0-A1 | 3 |
| A2-A16 | 4 |
| A17+ | 5 (4 + 1) |

---

## 수정 예시

### 1. Ritual 수치 증가

```java
@SpirePatch(
    cls = "com.megacrit.cardcrawl.powers.RitualPower",
    method = SpirePatch.CONSTRUCTOR,
    paramtypez = { AbstractCreature.class, int.class, boolean.class }
)
public static class RitualAmountPatch {
    @SpirePostfixPatch
    public static void Postfix(RitualPower __instance, AbstractCreature owner,
                               int strAmt, boolean playerControlled) {
        if (AbstractDungeon.isAscensionMode && AbstractDungeon.ascensionLevel >= 25) {
            // Ritual 수치 2배로 증가
            ReflectionHacks.setPrivate(__instance, AbstractPower.class, "amount", strAmt * 2);
        }
    }
}
```

### 2. 첫 턴부터 발동 (skipFirst 제거)

```java
@SpirePatch(
    cls = "com.megacrit.cardcrawl.powers.RitualPower",
    method = SpirePatch.CONSTRUCTOR,
    paramtypez = { AbstractCreature.class, int.class, boolean.class }
)
public static class RitualSkipFirstPatch {
    @SpirePostfixPatch
    public static void Postfix(RitualPower __instance, AbstractCreature owner,
                               int strAmt, boolean playerControlled) {
        if (AbstractDungeon.isAscensionMode && AbstractDungeon.ascensionLevel >= 25) {
            // 첫 턴부터 발동
            ReflectionHacks.setPrivate(__instance, RitualPower.class, "skipFirst", false);
        }
    }
}
```

---

## 관련 파일

- **본 파일**: `com/megacrit/cardcrawl/powers/RitualPower.java`
- **관련 파워**: `StrengthPower`
- **액션**: `ApplyPowerAction`
- **사용 몬스터**: `Cultist`

---

## 참고사항

1. **타이밍 차이**: 몬스터용은 라운드 종료, 플레이어용은 턴 종료
2. **skipFirst**: 몬스터용은 첫 라운드 스킵, 플레이어용은 즉시 발동
3. **중첩**: 여러 번 부여 시 amount가 누적됨
4. **영구 지속**: 제거되지 않는 한 계속 발동
