# 허약 (Frail)

## 기본 정보

**클래스명**: `FrailPower`
**전체 경로**: `com.megacrit.cardcrawl.powers.FrailPower`
**ID**: `"Frail"`
**타입**: DEBUFF
**적용 대상**: 플레이어 / 몬스터 모두

---

## 효과

**기본 효과**: 얻는 방어도가 25% 감소
**수치 설명**: `amount`는 허약이 지속되는 턴 수
**적용 시점**: 방어도를 얻을 때 (modifyBlock)

**특수 상호작용**: 없음 (유물에 의한 효과 변경 없음)

---

## 코드 분석

### 생성자
```java
public FrailPower(AbstractCreature owner, int amount, boolean isSourceMonster) {
    this.name = NAME;
    this.ID = "Frail";
    this.owner = owner;
    this.amount = amount;
    this.priority = 10;  // 낮은 우선순위
    updateDescription();
    loadRegion("frail");

    // 몬스터가 적용한 허약은 첫 턴에 감소하지 않음
    if (isSourceMonster) {
        this.justApplied = true;
    }

    this.type = AbstractPower.PowerType.DEBUFF;
    this.isTurnBased = true;
}
```

**주요 파라미터**:
- `owner`: 허약 상태가 적용된 대상
- `amount`: 지속 턴 수
- `isSourceMonster`: 몬스터가 적용했는지 여부

**WeakPower와의 차이점**:
- `priority = 10` (낮음) vs WeakPower의 `priority = 99` (높음)
- WeakPower보다 늦게 계산됨

### 핵심 메서드

**modifyBlock()**: 방어도를 얻을 때 호출

```java
public float modifyBlock(float blockAmount) {
    return blockAmount * 0.75F;  // 25% 감소
}
```

**특징**:
- **DamageType 체크 없음**: 모든 방어도 획득에 적용
- **유물 체크 없음**: Weak/Vulnerable과 달리 효과 변경 유물 없음
- **단순 곱셈**: 받은 방어도 값에 0.75를 곱함

### 스택/감소 로직

**턴 종료 시 자동 감소**:

```java
public void atEndOfRound() {
    if (this.justApplied) {
        this.justApplied = false;
        return;  // 적용된 턴에는 감소하지 않음
    }

    if (this.amount == 0) {
        addToBot(new RemoveSpecificPowerAction(this.owner, this.owner, "Frail"));
    } else {
        addToBot(new ReducePowerAction(this.owner, this.owner, "Frail", 1));
    }
}
```

**감소 메커니즘**:
1. 몬스터가 적용한 허약(`isSourceMonster=true`)는 적용된 턴 종료 시 감소하지 않음
2. 그 다음 턴부터 매 턴 종료마다 1씩 감소
3. 0이 되면 자동으로 제거

---

## 수정 방법

### 효과 수치 변경

**변경 대상**: `modifyBlock()` 메서드의 곱셈 계수
**코드 위치**: 줄 57-59

**예시: 50% 감소로 변경**

```java
@SpirePatch(
    clz = FrailPower.class,
    method = "modifyBlock"
)
public static class FrailBlockReductionPatch {
    @SpireReplacePatch
    public static float Replace(FrailPower __instance, float blockAmount) {
        // 기존 0.75F를 0.5F로 변경 (50% 감소)
        return blockAmount * 0.5F;
    }
}
```

**예시: Dexterity 기반 동적 감소**

```java
@SpirePatch(
    clz = FrailPower.class,
    method = "modifyBlock"
)
public static class FrailDynamicReductionPatch {
    @SpireReplacePatch
    public static float Replace(FrailPower __instance, float blockAmount) {
        // Dexterity가 높을수록 허약 효과 감소
        int dexterity = __instance.owner.getPower("Dexterity").amount;
        float reduction = Math.max(0.5F, 0.75F - (dexterity * 0.05F));
        return blockAmount * reduction;
    }
}
```

### 지속 시간 변경

**방법 1: 생성 시 amount 조정**
```java
// 허약을 2배 지속시간으로 적용
new FrailPower(target, 6, true);  // 기존 3 → 6으로 변경
```

**방법 2: 턴당 감소량 변경**
```java
@SpirePatch(
    clz = FrailPower.class,
    method = "atEndOfRound"
)
public static class FrailDurationPatch {
    @SpireReplacePatch
    public static void Replace(FrailPower __instance) {
        if (__instance.justApplied) {
            __instance.justApplied = false;
            return;
        }

        // 2씩 감소하도록 수정
        if (__instance.amount <= 1) {
            AbstractDungeon.actionManager.addToBot(
                new RemoveSpecificPowerAction(__instance.owner, __instance.owner, "Frail")
            );
        } else {
            AbstractDungeon.actionManager.addToBot(
                new ReducePowerAction(__instance.owner, __instance.owner, "Frail", 2)
            );
        }
    }
}
```

### 특정 방어도 소스만 감소

**예시: 카드로 얻는 방어도만 감소**

```java
@SpirePatch(
    clz = AbstractPlayer.class,
    method = "addBlock"
)
public static class FrailCardBlockOnlyPatch {
    @SpirePrefixPatch
    public static void Prefix(AbstractPlayer __instance, int blockToGain) {
        // 카드에서 오는 방어도인지 체크 (Action 스택 확인 필요)
        if (__instance.hasPower("Frail")) {
            // 조건부 적용 로직
        }
    }
}
```

---

## 관련 파일

**적용하는 카드**:
- Power Through (아이언클래드) - 자신에게 허약 2 적용
- Wraith Form (사일런트) - 자신에게 허약 1 적용
- Biased Cognition (디펙트) - 자신에게 허약 1 적용
- Establishment (와쳐) - 허약을 활용하는 카드

**적용하는 몬스터**:
- Hexaghost (헥사고스트)
- Time Eater (시간 포식자)
- Spire Growth (첨탑 증식체)
- Darklings (어둠 정령)
- Spheric Guardian (구형 수호자)

**관련 유물**:
- **Turnip**: 허약 관련 직접 상호작용 없음
- **Tungsten Rod**: 방어도 감소와 무관 (대미지 감소 유물)

**관련 파워**:
- **Dexterity**: 턴마다 얻는 방어도에 허약 적용
- **Barricade**: 허약 효과를 받은 방어도도 유지됨
- **Plated Armor**: 허약 효과를 받지 않음 (modifyBlock 호출 안 함)

---

## 참고사항

1. **우선순위**: `priority = 10`으로 매우 낮음
   - 대부분의 다른 파워보다 나중에 계산됨
   - Dexterity 등으로 방어도가 증가한 **이후** 허약 적용
2. **모든 방어도에 적용**:
   - 카드, 유물, 파워 등 출처 무관하게 모든 방어도 획득에 적용
   - **예외**: `Plated Armor` 같이 `modifyBlock`을 거치지 않는 경우
3. **곱셈 적용**:
   - `blockAmount * 0.75F` (25% 감소)
   - 유물에 의한 효과 변경 없음
4. **justApplied 메커니즘**:
   - 플레이어가 적용 (`isSourceMonster=false`): 같은 턴 종료 시 바로 감소
   - 몬스터가 적용 (`isSourceMonster=true`): 다음 턴 종료부터 감소
5. **스택 가능**: 허약 2를 가진 상태에서 허약 1을 추가로 받으면 3이 됨
6. **Artifact로 무효화 가능**: Artifact 파워가 있으면 허약 적용을 막을 수 있음
7. **계산 순서 예시**:
   ```
   Defend 카드 (5 방어도)
   → Dexterity +3 적용 (8 방어도)
   → Frail 적용 (8 * 0.75 = 6 방어도)
   → 최종: 6 방어도 획득
   ```
8. **Plated Armor와의 차이**:
   - Plated Armor는 `atStartOfTurnPostDraw`에서 직접 방어도 추가
   - `modifyBlock`을 거치지 않아 허약 영향 없음
9. **자가 디버프 카드들**:
   - 강력한 효과를 얻는 대신 허약을 받는 패턴
   - Wraith Form: 무적 대신 허약
   - Biased Cognition: Focus +4 대신 허약
   - Power Through: 카드 드로우 + 방어도 + 허약
10. **Weak과의 차이**:
    - Weak: 주는 공격 대미지 25% 감소 (공격 능력 저하)
    - Frail: 얻는 방어도 25% 감소 (방어 능력 저하)
    - Weak은 priority 99 (높음), Frail은 priority 10 (낮음)
