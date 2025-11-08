# 분노 (Angry)

## 기본 정보

**클래스명**: `AngryPower`
**전체 경로**: `com.megacrit.cardcrawl.powers.AngryPower`
**ID**: `"Angry"`
**타입**: 버프 (BUFF)
**사용 몬스터**: Gremlin Warrior (그렘린 전사), 기타

---

## 생성자 정보

```java
public AngryPower(AbstractCreature owner, int attackAmount)
```

**파라미터**:
- `owner`: 파워를 가질 크리처
- `attackAmount`: 피격 시 증가할 힘(Strength) 수치

**코드 위치**: 16-24줄

```java
this.name = NAME;
this.ID = "Angry";
this.owner = owner;
this.amount = attackAmount;
updateDescription();
this.isPostActionPower = true;
loadRegion("anger");
```

**특징**:
- `isPostActionPower = true`: 액션 후 처리되는 파워

---

## 효과 설명

### 피격 시 힘 증가

**발동 타이밍**: 공격 받았을 때 (`onAttacked`)
**효과**: 자신에게 `amount`만큼 힘(Strength) 부여

**코드 위치**: 27-34줄

```java
public int onAttacked(DamageInfo info, int damageAmount) {
    if (info.owner != null && damageAmount > 0 &&
        info.type != DamageInfo.DamageType.HP_LOSS &&
        info.type != DamageInfo.DamageType.THORNS) {

        addToTop(new ApplyPowerAction(this.owner, this.owner,
            new StrengthPower(this.owner, this.amount), this.amount));
        flash();
    }
    return damageAmount;
}
```

**발동 조건**:
1. `info.owner != null`: 공격자가 존재
2. `damageAmount > 0`: 실제 데미지가 0보다 큼
3. `info.type != HP_LOSS`: HP 손실이 아님 (독, 출혈 등 제외)
4. `info.type != THORNS`: 가시 데미지가 아님

**특징**:
- `addToTop`: 즉시 실행 (현재 액션 큐 최상단 추가)
- 공격 받을 때마다 발동 (횟수 제한 없음)
- 데미지 감소 없이 힘만 증가

---

## 설명 텍스트

**코드 위치**: 37-39줄

```java
public void updateDescription() {
    this.description = DESCRIPTIONS[1] + this.amount + DESCRIPTIONS[2];
}
```

**설명**: `DESCRIPTIONS[1] + amount + DESCRIPTIONS[2]`
예시: "피격 시 힘 1 증가"

---

## 중요 필드

| 필드명 | 타입 | 설명 |
|--------|------|------|
| `amount` | int | 피격 시 증가할 Strength 수치 |
| `isPostActionPower` | boolean | 액션 후 처리 여부 (true) |

---

## 수정 예시

### 1. Angry 수치 증가

```java
@SpirePatch(
    cls = "com.megacrit.cardcrawl.powers.AngryPower",
    method = SpirePatch.CONSTRUCTOR,
    paramtypez = { AbstractCreature.class, int.class }
)
public static class AngryAmountPatch {
    @SpirePostfixPatch
    public static void Postfix(AngryPower __instance, AbstractCreature owner,
                               int attackAmount) {
        if (AbstractDungeon.isAscensionMode && AbstractDungeon.ascensionLevel >= 25) {
            // Angry 수치 2배로 증가
            ReflectionHacks.setPrivate(__instance, AbstractPower.class, "amount", attackAmount * 2);
        }
    }
}
```

### 2. 피격 시 추가 효과 (Block 획득)

```java
@SpirePatch(
    cls = "com.megacrit.cardcrawl.powers.AngryPower",
    method = "onAttacked"
)
public static class AngryBlockPatch {
    @SpireInsertPatch(locator = Locator.class)
    public static void Insert(AngryPower __instance, DamageInfo info, int damageAmount) {
        if (AbstractDungeon.isAscensionMode && AbstractDungeon.ascensionLevel >= 25) {
            // 피격 시 추가로 Block 3 획득
            AbstractDungeon.actionManager.addToTop(
                new GainBlockAction(__instance.owner, __instance.owner, 3)
            );
        }
    }

    private static class Locator extends SpireInsertLocator {
        public int[] Locate(CtBehavior ctMethodToPatch) throws Exception {
            Matcher matcher = new Matcher.MethodCallMatcher(ApplyPowerAction.class, "<init>");
            return LineFinder.findInOrder(ctMethodToPatch, matcher);
        }
    }
}
```

### 3. 피격 횟수 제한 추가

```java
@SpirePatch(
    cls = "com.megacrit.cardcrawl.powers.AngryPower",
    method = "onAttacked"
)
public static class AngryLimitPatch {
    private static Map<AngryPower, Integer> hitCounts = new HashMap<>();
    private static final int MAX_TRIGGERS = 3; // 최대 3회까지만 발동

    @SpirePrefixPatch
    public static SpireReturn<Integer> Prefix(AngryPower __instance, DamageInfo info, int damageAmount) {
        if (AbstractDungeon.isAscensionMode && AbstractDungeon.ascensionLevel >= 25) {
            int count = hitCounts.getOrDefault(__instance, 0);
            if (count >= MAX_TRIGGERS) {
                // 최대 횟수 도달 시 발동 안 함
                return SpireReturn.Return(damageAmount);
            }
            hitCounts.put(__instance, count + 1);
        }
        return SpireReturn.Continue();
    }
}
```

---

## 관련 파일

- **본 파일**: `com/megacrit/cardcrawl/powers/AngryPower.java`
- **관련 파워**: `StrengthPower`
- **액션**: `ApplyPowerAction`
- **사용 몬스터**: Gremlin Warrior

---

## 참고사항

1. **HP_LOSS vs NORMAL**: 독, 출혈 등은 HP_LOSS이므로 Angry 발동 안 함
2. **THORNS**: 가시 데미지는 발동 안 함 (무한 루프 방지)
3. **addToTop**: 즉시 실행되므로 같은 턴에 힘 증가 효과 적용
4. **중첩**: 여러 번 피격 시 계속 누적됨
5. **영구 지속**: 제거되지 않는 한 계속 발동
6. **isPostActionPower**: 액션 후 처리로 데미지 계산에는 영향 안 줌
