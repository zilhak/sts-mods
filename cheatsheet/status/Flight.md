# 비행 (Flight)

## 기본 정보

**클래스명**: `FlightPower`
**전체 경로**: `com.megacrit.cardcrawl.powers.FlightPower`
**ID**: `"Flight"`
**타입**: 버프 (BUFF)
**사용 몬스터**: Byrd (버드)

---

## 생성자 정보

```java
public FlightPower(AbstractCreature owner, int amount)
```

**파라미터**:
- `owner`: 파워를 가질 크리처
- `amount`: 비행 카운터 수치

**코드 위치**: 21-30줄

```java
this.name = NAME;
this.ID = "Flight";
this.owner = owner;
this.amount = amount;
this.storedAmount = amount;
updateDescription();
loadRegion("flight");
this.priority = 50;
```

**특징**:
- `priority = 50`: 높은 우선순위 (데미지 감소를 먼저 적용)
- `storedAmount`: 초기 수치 저장 (턴 시작 시 복구용)

---

## 효과 설명

### 1. 데미지 50% 감소

**발동 타이밍**: 데미지 받을 때 (`atDamageFinalReceive`)
**효과**: 받는 데미지 50% 감소

**코드 위치**: 49-58줄

```java
public float atDamageFinalReceive(float damage, DamageInfo.DamageType type) {
    return calculateDamageTakenAmount(damage, type);
}

private float calculateDamageTakenAmount(float damage, DamageInfo.DamageType type) {
    if (type != DamageInfo.DamageType.HP_LOSS && type != DamageInfo.DamageType.THORNS) {
        return damage / 2.0F;
    }
    return damage;
}
```

**감소 조건**:
- `type != HP_LOSS`: HP 손실이 아님 (독, 출혈 등 제외)
- `type != THORNS`: 가시 데미지가 아님

**감소 공식**: `damage / 2.0F` (정확히 50%)

---

### 2. 피격 시 카운터 감소

**발동 타이밍**: 공격 받았을 때 (`onAttacked`)
**효과**: 비행 카운터 1 감소

**코드 위치**: 62-70줄

```java
public int onAttacked(DamageInfo info, int damageAmount) {
    Boolean willLive = calculateDamageTakenAmount(damageAmount, info.type) < this.owner.currentHealth;

    if (info.owner != null && info.type != DamageInfo.DamageType.HP_LOSS &&
        info.type != DamageInfo.DamageType.THORNS && damageAmount > 0 &&
        willLive.booleanValue()) {

        flash();
        addToBot(new ReducePowerAction(this.owner, this.owner, "Flight", 1));
    }
    return damageAmount;
}
```

**감소 조건**:
1. `info.owner != null`: 공격자가 존재
2. `info.type != HP_LOSS`: HP 손실이 아님
3. `info.type != THORNS`: 가시 데미지가 아님
4. `damageAmount > 0`: 실제 데미지가 0보다 큼
5. `willLive == true`: 공격 받아도 생존

**특징**:
- 데미지 감소 후에도 생존하는 경우에만 카운터 감소
- 즉사 시에는 카운터 감소 안 함

---

### 3. 턴 시작 시 카운터 복구

**발동 타이밍**: 턴 시작 시 (`atStartOfTurn`)
**효과**: 비행 카운터를 초기값으로 복구

**코드 위치**: 43-46줄

```java
public void atStartOfTurn() {
    this.amount = this.storedAmount;
    updateDescription();
}
```

**특징**:
- 매 턴 시작 시 카운터가 초기값으로 리셋됨
- 이전 턴에 감소한 카운터가 복구됨

---

### 4. 파워 제거 시 착지

**발동 타이밍**: 파워 제거 시 (`onRemove`)
**효과**: "GROUNDED" 상태로 변경

**코드 위치**: 73-75줄

```java
public void onRemove() {
    addToBot(new ChangeStateAction((AbstractMonster)this.owner, "GROUNDED"));
}
```

**특징**:
- Byrd 전용 "GROUNDED" 애니메이션 재생
- 카운터가 0이 되어 파워가 제거되면 착지

---

## Byrd 사용 예시

**Flight 카운터**:
| 난이도 | Flight 카운터 |
|--------|--------------|
| A0-A6 | 3 |
| A7+ | 4 |

**코드 예시** (`Byrd.java`):
```java
if (AbstractDungeon.ascensionLevel >= 7) {
    addToBot(new ApplyPowerAction(this, this, new FlightPower(this, 4)));
} else {
    addToBot(new ApplyPowerAction(this, this, new FlightPower(this, 3)));
}
```

**의미**:
- A0-A6: 3회 피격까지 비행 유지
- A7+: 4회 피격까지 비행 유지

---

## 설명 텍스트

**코드 위치**: 38-40줄

```java
public void updateDescription() {
    this.description = DESCRIPTIONS[0] + this.amount + DESCRIPTIONS[1];
}
```

**설명**: `DESCRIPTIONS[0] + amount + DESCRIPTIONS[1]`
예시: "받는 데미지 50% 감소. 3회 피격 시 착지."

---

## 중요 필드

| 필드명 | 타입 | 설명 |
|--------|------|------|
| `amount` | int | 현재 비행 카운터 |
| `storedAmount` | int | 초기 비행 카운터 (턴 시작 시 복구용) |
| `priority` | int | 우선순위 (50, 높은 편) |

---

## 수정 예시

### 1. Flight 카운터 증가

```java
@SpirePatch(
    cls = "com.megacrit.cardcrawl.powers.FlightPower",
    method = SpirePatch.CONSTRUCTOR,
    paramtypez = { AbstractCreature.class, int.class }
)
public static class FlightAmountPatch {
    @SpirePostfixPatch
    public static void Postfix(FlightPower __instance, AbstractCreature owner, int amount) {
        if (AbstractDungeon.isAscensionMode && AbstractDungeon.ascensionLevel >= 25) {
            // Flight 카운터 2배로 증가
            int newAmount = amount * 2;
            ReflectionHacks.setPrivate(__instance, AbstractPower.class, "amount", newAmount);
            ReflectionHacks.setPrivate(__instance, FlightPower.class, "storedAmount", newAmount);
            __instance.updateDescription();
        }
    }
}
```

### 2. 데미지 감소율 변경 (75% 감소)

```java
@SpirePatch(
    cls = "com.megacrit.cardcrawl.powers.FlightPower",
    method = "calculateDamageTakenAmount",
    paramtypez = { float.class, DamageInfo.DamageType.class }
)
public static class FlightReductionPatch {
    @SpirePrefixPatch
    public static SpireReturn<Float> Prefix(FlightPower __instance, float damage,
                                            DamageInfo.DamageType type) {
        if (AbstractDungeon.isAscensionMode && AbstractDungeon.ascensionLevel >= 25) {
            // 75% 감소 (1/4만 받음)
            if (type != DamageInfo.DamageType.HP_LOSS && type != DamageInfo.DamageType.THORNS) {
                return SpireReturn.Return(damage / 4.0F);
            }
            return SpireReturn.Return(damage);
        }
        return SpireReturn.Continue();
    }
}
```

### 3. 카운터 감소 안 함 (무한 비행)

```java
@SpirePatch(
    cls = "com.megacrit.cardcrawl.powers.FlightPower",
    method = "onAttacked"
)
public static class FlightInfinitePatch {
    @SpirePrefixPatch
    public static SpireReturn<Integer> Prefix(FlightPower __instance, DamageInfo info,
                                              int damageAmount) {
        if (AbstractDungeon.isAscensionMode && AbstractDungeon.ascensionLevel >= 25) {
            // 카운터 감소 없이 데미지만 감소
            return SpireReturn.Return(damageAmount);
        }
        return SpireReturn.Continue();
    }
}
```

### 4. 즉사 시에도 카운터 감소

```java
@SpirePatch(
    cls = "com.megacrit.cardcrawl.powers.FlightPower",
    method = "onAttacked"
)
public static class FlightAlwaysDecreasePatch {
    @SpirePrefixPatch
    public static SpireReturn<Integer> Prefix(FlightPower __instance, DamageInfo info,
                                              int damageAmount) {
        if (AbstractDungeon.isAscensionMode && AbstractDungeon.ascensionLevel >= 25) {
            // willLive 조건 제거하고 항상 카운터 감소
            if (info.owner != null && info.type != DamageInfo.DamageType.HP_LOSS &&
                info.type != DamageInfo.DamageType.THORNS && damageAmount > 0) {

                __instance.flash();
                AbstractDungeon.actionManager.addToBot(
                    new ReducePowerAction(__instance.owner, __instance.owner, "Flight", 1)
                );
            }
            return SpireReturn.Return(damageAmount);
        }
        return SpireReturn.Continue();
    }
}
```

---

## 관련 파일

- **본 파일**: `com/megacrit/cardcrawl/powers/FlightPower.java`
- **액션**:
  - `ReducePowerAction`: 파워 수치 감소
  - `ChangeStateAction`: 몬스터 상태 변경
- **사용 몬스터**: Byrd

---

## 참고사항

1. **데미지 감소**: 50% 고정 (1/2만 받음)
2. **카운터 시스템**: 피격 시 1씩 감소, 턴 시작 시 복구
3. **생존 조건**: 공격 받아도 생존하는 경우에만 카운터 감소
4. **즉사 방지**: willLive 체크로 즉사 시 카운터 감소 안 함
5. **HP_LOSS/THORNS**: 데미지 감소 안 됨, 카운터 감소 안 됨
6. **착지**: 카운터가 0이 되면 파워 제거 → "GROUNDED" 상태
7. **priority 50**: 높은 우선순위로 먼저 데미지 감소 적용
8. **음향 효과**: `playApplyPowerSfx()` 메서드로 "POWER_FLIGHT" 사운드 재생
