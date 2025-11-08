# 재생 (Regeneration)

## 기본 정보

**클래스명**: `RegenerateMonsterPower`
**전체 경로**: `com.megacrit.cardcrawl.powers.RegenerateMonsterPower`
**ID**: `"Regenerate"`
**타입**: 버프 (BUFF)
**사용 몬스터**: Awakened One (각성한 자), 기타

---

## 생성자 정보

```java
public RegenerateMonsterPower(AbstractMonster owner, int regenAmt)
```

**파라미터**:
- `owner`: 파워를 가질 몬스터
- `regenAmt`: 턴 종료 시 회복할 HP 수치

**코드 위치**: 14-22줄

```java
this.name = NAME;
this.ID = "Regenerate";
this.owner = (AbstractCreature)owner;
this.amount = regenAmt;
updateDescription();
loadRegion("regen");
this.type = AbstractPower.PowerType.BUFF;
```

**특징**:
- 몬스터 전용 (생성자에서 `AbstractMonster` 타입 요구)
- 명시적으로 `PowerType.BUFF` 설정

---

## 효과 설명

### 턴 종료 시 HP 회복

**발동 타이밍**: 턴 종료 시 (`atEndOfTurn`)
**효과**: `amount`만큼 HP 회복

**코드 위치**: 30-34줄

```java
public void atEndOfTurn(boolean isPlayer) {
    flash();
    if (!this.owner.halfDead && !this.owner.isDying && !this.owner.isDead)
        addToBot(new HealAction(this.owner, this.owner, this.amount));
}
```

**회복 조건**:
1. `!halfDead`: Half Dead 상태가 아님
2. `!isDying`: 죽는 중이 아님
3. `!isDead`: 이미 죽지 않음

**특징**:
- 턴 종료 시 무조건 발동 (isPlayer 파라미터 미사용)
- flash() 효과로 파워 발동 시각화
- 조건 충족 시 HealAction으로 회복 실행

---

## Awakened One 사용 예시

**재생 수치**:
| 난이도 | Phase 1 | Phase 2 (각성 후) |
|--------|---------|------------------|
| A0-A18 | - | 15 |
| A19+ | - | 20 |

**코드 예시** (`AwakenedOne.java`):
```java
// Phase 2 진입 시
if (AbstractDungeon.ascensionLevel >= 19) {
    addToBot(new ApplyPowerAction(this, this,
        new RegenerateMonsterPower(this, 20)));
} else {
    addToBot(new ApplyPowerAction(this, this,
        new RegenerateMonsterPower(this, 15)));
}
```

**의미**:
- Phase 2에서만 재생 파워 획득
- A19+ 난이도에서 회복량 증가 (15 → 20)
- 매 턴 종료 시 상당량의 HP 회복

---

## 설명 텍스트

**코드 위치**: 25-27줄

```java
public void updateDescription() {
    this.description = DESCRIPTIONS[0] + this.amount + DESCRIPTIONS[1];
}
```

**설명**: `DESCRIPTIONS[0] + amount + DESCRIPTIONS[1]`
예시: "턴 종료 시 HP 15 회복"

---

## 중요 필드

| 필드명 | 타입 | 설명 |
|--------|------|------|
| `amount` | int | 턴 종료 시 회복할 HP 수치 |
| `type` | PowerType | 파워 타입 (BUFF) |

---

## 수정 예시

### 1. Regeneration 수치 증가

```java
@SpirePatch(
    cls = "com.megacrit.cardcrawl.powers.RegenerateMonsterPower",
    method = SpirePatch.CONSTRUCTOR,
    paramtypez = { AbstractMonster.class, int.class }
)
public static class RegenerationAmountPatch {
    @SpirePostfixPatch
    public static void Postfix(RegenerateMonsterPower __instance, AbstractMonster owner,
                               int regenAmt) {
        if (AbstractDungeon.isAscensionMode && AbstractDungeon.ascensionLevel >= 25) {
            // 회복량 1.5배로 증가
            int newAmount = (int)(regenAmt * 1.5f);
            ReflectionHacks.setPrivate(__instance, AbstractPower.class, "amount", newAmount);
            __instance.updateDescription();
        }
    }
}
```

### 2. 회복 횟수 제한 추가

```java
@SpirePatch(
    cls = "com.megacrit.cardcrawl.powers.RegenerateMonsterPower",
    method = "atEndOfTurn"
)
public static class RegenerationLimitPatch {
    private static Map<RegenerateMonsterPower, Integer> healCounts = new HashMap<>();
    private static final int MAX_HEALS = 5; // 최대 5턴까지만 회복

    @SpirePrefixPatch
    public static SpireReturn<Void> Prefix(RegenerateMonsterPower __instance, boolean isPlayer) {
        if (AbstractDungeon.isAscensionMode && AbstractDungeon.ascensionLevel >= 25) {
            int count = healCounts.getOrDefault(__instance, 0);
            if (count >= MAX_HEALS) {
                // 최대 횟수 도달 시 파워 제거
                __instance.flash();
                AbstractDungeon.actionManager.addToBot(
                    new RemoveSpecificPowerAction(__instance.owner, __instance.owner, "Regenerate")
                );
                healCounts.remove(__instance);
                return SpireReturn.Return();
            }
            healCounts.put(__instance, count + 1);
        }
        return SpireReturn.Continue();
    }
}
```

### 3. 회복 시 추가 효과 (힘 획득)

```java
@SpirePatch(
    cls = "com.megacrit.cardcrawl.powers.RegenerateMonsterPower",
    method = "atEndOfTurn"
)
public static class RegenerationBonusPatch {
    @SpireInsertPatch(locator = Locator.class)
    public static void Insert(RegenerateMonsterPower __instance, boolean isPlayer) {
        if (AbstractDungeon.isAscensionMode && AbstractDungeon.ascensionLevel >= 25) {
            // 회복 시 추가로 힘 1 획득
            AbstractDungeon.actionManager.addToBot(
                new ApplyPowerAction(__instance.owner, __instance.owner,
                    new StrengthPower(__instance.owner, 1), 1)
            );
        }
    }

    private static class Locator extends SpireInsertLocator {
        public int[] Locate(CtBehavior ctMethodToPatch) throws Exception {
            Matcher matcher = new Matcher.MethodCallMatcher(HealAction.class, "<init>");
            return LineFinder.findInOrder(ctMethodToPatch, matcher);
        }
    }
}
```

### 4. 조건부 회복 (HP 50% 이하일 때만)

```java
@SpirePatch(
    cls = "com.megacrit.cardcrawl.powers.RegenerateMonsterPower",
    method = "atEndOfTurn"
)
public static class RegenerationConditionalPatch {
    @SpirePrefixPatch
    public static SpireReturn<Void> Prefix(RegenerateMonsterPower __instance, boolean isPlayer) {
        if (AbstractDungeon.isAscensionMode && AbstractDungeon.ascensionLevel >= 25) {
            __instance.flash();

            // HP 50% 이하일 때만 회복
            if (!__instance.owner.halfDead && !__instance.owner.isDying && !__instance.owner.isDead) {
                float hpPercent = (float)__instance.owner.currentHealth / __instance.owner.maxHealth;
                if (hpPercent <= 0.5f) {
                    AbstractDungeon.actionManager.addToBot(
                        new HealAction(__instance.owner, __instance.owner, __instance.amount)
                    );
                }
            }
            return SpireReturn.Return();
        }
        return SpireReturn.Continue();
    }
}
```

### 5. 회복량을 HP 비율로 변경

```java
@SpirePatch(
    cls = "com.megacrit.cardcrawl.powers.RegenerateMonsterPower",
    method = "atEndOfTurn"
)
public static class RegenerationPercentPatch {
    @SpirePrefixPatch
    public static SpireReturn<Void> Prefix(RegenerateMonsterPower __instance, boolean isPlayer) {
        if (AbstractDungeon.isAscensionMode && AbstractDungeon.ascensionLevel >= 25) {
            __instance.flash();

            if (!__instance.owner.halfDead && !__instance.owner.isDying && !__instance.owner.isDead) {
                // amount%만큼 회복 (예: amount=5 → 최대 HP의 5%)
                int healAmount = (int)(__instance.owner.maxHealth * __instance.amount / 100f);
                AbstractDungeon.actionManager.addToBot(
                    new HealAction(__instance.owner, __instance.owner, healAmount)
                );
            }
            return SpireReturn.Return();
        }
        return SpireReturn.Continue();
    }
}
```

---

## 플레이어용 Regeneration

**참고**: 플레이어용 재생은 별도 클래스 `RegeneratePower` 사용
- 경로: `com.megacrit.cardcrawl.powers.RegeneratePower`
- 발동 타이밍: 턴 시작 시 (`atStartOfTurn`)
- 감소 시스템: 회복 후 amount 1씩 감소

---

## 관련 파일

- **본 파일**: `com/megacrit/cardcrawl/powers/RegenerateMonsterPower.java`
- **플레이어용**: `com.megacrit.cardcrawl.powers.RegeneratePower`
- **액션**: `HealAction`
- **사용 몬스터**: Awakened One (Phase 2)

---

## 참고사항

1. **몬스터 전용**: 생성자에서 `AbstractMonster` 타입 요구
2. **턴 종료**: 플레이어용은 턴 시작, 몬스터용은 턴 종료
3. **영구 지속**: 제거되지 않는 한 계속 회복 (플레이어용은 감소)
4. **조건 체크**: halfDead, isDying, isDead 체크로 안전하게 회복
5. **flash() 효과**: 회복 발동 시 시각적 효과
6. **최대 HP 초과 불가**: HealAction이 자동으로 최대 HP 제한
7. **AwakenedOne 전용**: Phase 2 진입 시에만 부여되는 강력한 회복 효과
