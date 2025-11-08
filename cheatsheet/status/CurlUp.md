# 웅크리기 (Curl Up)

## 기본 정보

**클래스명**: `CurlUpPower`
**전체 경로**: `com.megacrit.cardcrawl.powers.CurlUpPower`
**ID**: `"Curl Up"`
**타입**: 버프 (BUFF)
**사용 몬스터**: Louse (이)

---

## 생성자 정보

```java
public CurlUpPower(AbstractCreature owner, int amount)
```

**파라미터**:
- `owner`: 파워를 가질 크리처
- `amount`: 최초 피격 시 획득할 Block 수치

**코드 위치**: 21-28줄

```java
this.name = NAME;
this.ID = "Curl Up";
this.owner = owner;
this.amount = amount;
this.description = DESCRIPTIONS[0] + amount + DESCRIPTIONS[1];
loadRegion("closeUp");
```

---

## 효과 설명

### 최초 피격 시 Block 획득 후 소멸

**발동 타이밍**: 공격 받았을 때 (`onAttacked`)
**효과**:
1. `amount`만큼 Block 획득
2. "CLOSED" 상태로 변경
3. 이 파워 제거 (1회성)

**코드 위치**: 30-40줄

```java
public int onAttacked(DamageInfo info, int damageAmount) {
    if (!this.triggered && damageAmount < this.owner.currentHealth &&
        damageAmount > 0 && info.owner != null &&
        info.type == DamageInfo.DamageType.NORMAL) {

        flash();
        this.triggered = true;
        addToBot(new ChangeStateAction((AbstractMonster)this.owner, "CLOSED"));
        addToBot(new GainBlockAction(this.owner, this.owner, this.amount));
        addToBot(new RemoveSpecificPowerAction(this.owner, this.owner, "Curl Up"));
    }
    return damageAmount;
}
```

**발동 조건** (모두 충족 필요):
1. `!this.triggered`: 아직 발동 안 함 (최초 1회)
2. `damageAmount < this.owner.currentHealth`: 데미지가 현재 HP보다 작음 (즉사 방지)
3. `damageAmount > 0`: 실제 데미지가 0보다 큼
4. `info.owner != null`: 공격자가 존재
5. `info.type == DamageInfo.DamageType.NORMAL`: 일반 공격 데미지

**실행 순서**:
1. `ChangeStateAction`: "CLOSED" 상태로 변경 (애니메이션)
2. `GainBlockAction`: Block 획득
3. `RemoveSpecificPowerAction`: 이 파워 제거

---

## Louse 사용 예시

**일반 Louse (Normal)**:
- Curl Up: 9-12 Block

**방어형 Louse (Defensive)**:
- Curl Up: 11-15 Block

**코드 예시** (`LouseNormal.java`):
```java
if (AbstractDungeon.ascensionLevel >= 7) {
    addToBot(new ApplyPowerAction(this, this, new CurlUpPower(this, 11)));
} else {
    addToBot(new ApplyPowerAction(this, this, new CurlUpPower(this, 9)));
}
```

---

## 중요 필드

| 필드명 | 타입 | 설명 |
|--------|------|------|
| `triggered` | boolean | 발동 여부 (기본값: false) |
| `amount` | int | 최초 피격 시 획득할 Block 수치 |

---

## 수정 예시

### 1. Curl Up Block 수치 증가

```java
@SpirePatch(
    cls = "com.megacrit.cardcrawl.powers.CurlUpPower",
    method = SpirePatch.CONSTRUCTOR,
    paramtypez = { AbstractCreature.class, int.class }
)
public static class CurlUpAmountPatch {
    @SpirePostfixPatch
    public static void Postfix(CurlUpPower __instance, AbstractCreature owner, int amount) {
        if (AbstractDungeon.isAscensionMode && AbstractDungeon.ascensionLevel >= 25) {
            // Block 수치 1.5배로 증가
            int newAmount = (int)(amount * 1.5f);
            ReflectionHacks.setPrivate(__instance, AbstractPower.class, "amount", newAmount);
            __instance.updateDescription();
        }
    }
}
```

### 2. 여러 번 발동 가능하게 변경 (파워 제거 안 함)

```java
@SpirePatch(
    cls = "com.megacrit.cardcrawl.powers.CurlUpPower",
    method = "onAttacked"
)
public static class CurlUpReusablePatch {
    @SpireInsertPatch(locator = Locator.class)
    public static void Insert(CurlUpPower __instance, DamageInfo info, int damageAmount) {
        if (AbstractDungeon.isAscensionMode && AbstractDungeon.ascensionLevel >= 25) {
            // triggered를 false로 리셋 (다시 발동 가능)
            ReflectionHacks.setPrivate(__instance, CurlUpPower.class, "triggered", false);
        }
    }

    private static class Locator extends SpireInsertLocator {
        public int[] Locate(CtBehavior ctMethodToPatch) throws Exception {
            Matcher matcher = new Matcher.MethodCallMatcher(RemoveSpecificPowerAction.class, "<init>");
            return LineFinder.findInOrder(ctMethodToPatch, matcher);
        }
    }
}
```

### 3. 즉사 방지 조건 제거 (항상 발동)

```java
@SpirePatch(
    cls = "com.megacrit.cardcrawl.powers.CurlUpPower",
    method = "onAttacked"
)
public static class CurlUpAlwaysPatch {
    @SpirePrefixPatch
    public static SpireReturn<Integer> Prefix(CurlUpPower __instance, DamageInfo info, int damageAmount) {
        if (AbstractDungeon.isAscensionMode && AbstractDungeon.ascensionLevel >= 25) {
            boolean triggered = ReflectionHacks.getPrivate(__instance, CurlUpPower.class, "triggered");

            // 즉사 조건 무시하고 발동
            if (!triggered && damageAmount > 0 && info.owner != null &&
                info.type == DamageInfo.DamageType.NORMAL) {

                __instance.flash();
                ReflectionHacks.setPrivate(__instance, CurlUpPower.class, "triggered", true);

                AbstractDungeon.actionManager.addToBot(
                    new ChangeStateAction((AbstractMonster)__instance.owner, "CLOSED")
                );
                AbstractDungeon.actionManager.addToBot(
                    new GainBlockAction(__instance.owner, __instance.owner, __instance.amount)
                );
                AbstractDungeon.actionManager.addToBot(
                    new RemoveSpecificPowerAction(__instance.owner, __instance.owner, "Curl Up")
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

- **본 파일**: `com/megacrit/cardcrawl/powers/CurlUpPower.java`
- **액션**:
  - `ChangeStateAction`: 몬스터 상태 변경
  - `GainBlockAction`: Block 획득
  - `RemoveSpecificPowerAction`: 파워 제거
- **사용 몬스터**: Louse (Normal, Defensive)

---

## 참고사항

1. **1회성**: 최초 피격 시에만 발동 후 즉시 소멸
2. **즉사 방지**: `damageAmount < currentHealth` 조건으로 즉사 시 발동 안 함
3. **NORMAL 타입만**: HP_LOSS, THORNS는 발동 안 함
4. **ChangeStateAction**: Louse 전용 "CLOSED" 애니메이션 재생
5. **발동 순서**: 상태 변경 → Block 획득 → 파워 제거
6. **triggered 플래그**: 중복 발동 방지용 (이미 발동했으면 다시 안 함)
