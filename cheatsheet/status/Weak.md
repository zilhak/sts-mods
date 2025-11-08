# 약화 (Weak)

## 기본 정보

**클래스명**: `WeakPower`
**전체 경로**: `com.megacrit.cardcrawl.powers.WeakPower`
**ID**: `"Weakened"`
**타입**: DEBUFF
**적용 대상**: 플레이어 / 몬스터 모두

---

## 효과

**기본 효과**: 공격 대미지가 25% 감소
**수치 설명**: `amount`는 약화가 지속되는 턴 수
**적용 시점**: 대미지를 **줄 때** (atDamageGive)

**특수 상호작용**:
- **Paper Crane 유물 소지 시**: 적의 약화 효과가 40% 감소로 증가 (0.6배)
- 일반적으로는 75% 대미지 (0.75배)

---

## 코드 분석

### 생성자
```java
public WeakPower(AbstractCreature owner, int amount, boolean isSourceMonster) {
    this.name = NAME;
    this.ID = "Weakened";
    this.owner = owner;
    this.amount = amount;
    updateDescription();
    loadRegion("weak");

    // 몬스터가 적용한 약화는 첫 턴에 감소하지 않음
    if (isSourceMonster) {
        this.justApplied = true;
    }

    this.type = AbstractPower.PowerType.DEBUFF;
    this.isTurnBased = true;
    this.priority = 99;  // 높은 우선순위
}
```

**주요 파라미터**:
- `owner`: 약화 상태가 적용된 대상
- `amount`: 지속 턴 수
- `isSourceMonster`: 몬스터가 적용했는지 여부 (플레이어가 적용하면 false)

### 핵심 메서드

**atDamageGive()**: 대미지 계산 시 호출

```java
public float atDamageGive(float damage, DamageInfo.DamageType type) {
    if (type == DamageInfo.DamageType.NORMAL) {
        // Paper Crane 유물 체크 (플레이어가 소지, 약화 대상이 몬스터인 경우)
        if (!this.owner.isPlayer && AbstractDungeon.player.hasRelic("Paper Crane")) {
            return damage * 0.6F;  // 40% 감소
        }
        return damage * 0.75F;  // 25% 감소
    }
    return damage;  // NORMAL 타입이 아니면 그대로
}
```

**중요**: `DamageInfo.DamageType.NORMAL`만 영향을 받음 (THORNS, HP_LOSS 등은 영향 없음)

### 스택/감소 로직

**턴 종료 시 자동 감소**:

```java
public void atEndOfRound() {
    if (this.justApplied) {
        this.justApplied = false;
        return;  // 적용된 턴에는 감소하지 않음
    }

    if (this.amount == 0) {
        addToBot(new RemoveSpecificPowerAction(this.owner, this.owner, "Weakened"));
    } else {
        addToBot(new ReducePowerAction(this.owner, this.owner, "Weakened", 1));
    }
}
```

**감소 메커니즘**:
1. 몬스터가 적용한 약화(`isSourceMonster=true`)는 적용된 턴 종료 시 감소하지 않음
2. 그 다음 턴부터 매 턴 종료마다 1씩 감소
3. 0이 되면 자동으로 제거

---

## 수정 방법

### 효과 수치 변경

**변경 대상**: `atDamageGive()` 메서드의 곱셈 계수
**코드 위치**: 줄 76-79

**예시: 50% 감소로 변경**

```java
@SpirePatch(
    clz = WeakPower.class,
    method = "atDamageGive"
)
public static class WeakDamageReductionPatch {
    @SpireInsertPatch(
        locator = Locator.class
    )
    public static SpireReturn<Float> Insert(WeakPower __instance, float damage, DamageInfo.DamageType type) {
        if (type == DamageInfo.DamageType.NORMAL) {
            // 기존 0.75F를 0.5F로 변경
            if (!__instance.owner.isPlayer && AbstractDungeon.player.hasRelic("Paper Crane")) {
                return SpireReturn.Return(damage * 0.4F);  // Paper Crane: 60% 감소
            }
            return SpireReturn.Return(damage * 0.5F);  // 일반: 50% 감소
        }
        return SpireReturn.Continue();
    }

    private static class Locator extends SpireInsertLocator {
        @Override
        public int[] Locate(CtBehavior ctMethodToPatch) throws Exception {
            Matcher finalMatcher = new Matcher.FieldAccessMatcher(
                DamageInfo.DamageType.class, "NORMAL"
            );
            return LineFinder.findInOrder(ctMethodToPatch, finalMatcher);
        }
    }
}
```

### 지속 시간 변경

**방법 1: 생성 시 amount 조정**
```java
// 약화를 2배 지속시간으로 적용
new WeakPower(target, 4, true);  // 기존 2 → 4로 변경
```

**방법 2: 턴당 감소량 변경**
```java
@SpirePatch(
    clz = WeakPower.class,
    method = "atEndOfRound"
)
public static class WeakDurationPatch {
    @SpirePrefixPatch
    public static void Prefix(WeakPower __instance) {
        // 2턴에 1번씩만 감소하도록 수정 (필드 추가 필요)
    }
}
```

---

## 관련 파일

**적용하는 카드**:
- Bash (아이언클래드 기본 공격)
- Clothesline (아이언클래드)
- Uppercut (아이언클래드)
- Sucker Punch (사일런트)
- Neutralize (사일런트 기본 공격)
- Malaise (사일런트)
- Crippling Cloud (사일런트)
- Melter (디펙트)
- Bowling Bash (와쳐)
- Wave of the Hand (와쳐)

**적용하는 몬스터**:
- Hexaghost (헥사고스트)
- Bronze Automaton (청동 자동인형)
- Taskmaster (책임자)
- Chosen (선택받은 자)
- Snake Plant (뱀 식물)

**관련 유물**:
- **Paper Crane**: 적에게 적용된 약화 효과 증가 (25% → 40% 감소)
- **Champion Belt**: 엘리트에게 약화 적용 시 추가 에너지

---

## 참고사항

1. **우선순위**: `priority = 99`로 매우 높음 → 대부분의 다른 파워보다 먼저 계산됨
2. **NORMAL 대미지만 적용**: THORNS, HP_LOSS 같은 특수 대미지는 영향받지 않음
3. **곱셈 적용**:
   - 기본: `damage * 0.75F` (25% 감소)
   - Paper Crane: `damage * 0.6F` (40% 감소)
4. **justApplied 메커니즘**: 몬스터가 적용한 약화는 즉시 감소하지 않음
   - 플레이어가 적용 (`isSourceMonster=false`): 같은 턴 종료 시 바로 감소
   - 몬스터가 적용 (`isSourceMonster=true`): 다음 턴 종료부터 감소
5. **스택 가능**: 약화 2를 가진 상태에서 약화 1을 추가로 받으면 3이 됨
6. **Artifact로 무효화 가능**: Artifact 파워가 있으면 약화 적용을 막을 수 있음
