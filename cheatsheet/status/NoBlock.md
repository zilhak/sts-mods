# 블록 봉쇄 (No Block)

## 기본 정보

**클래스명**: `NoBlockPower`
**전체 경로**: `com.megacrit.cardcrawl.powers.NoBlockPower`
**ID**: `"NoBlockPower"`
**타입**: DEBUFF
**적용 대상**: 플레이어 / 몬스터 모두

---

## 효과

**기본 효과**: 블록 획득이 불가능함 (모든 블록이 0으로 변환됨)
**수치 설명**: `amount`는 NoBlock이 지속되는 턴 수
**적용 시점**: 블록 계산 최종 단계 (modifyBlockLast)

**특수 상호작용**:
- **모든 블록 차단**: 카드, 유물, 파워 등 모든 블록 획득 원천 차단
- **justApplied 메커니즘**: 몬스터가 적용 시 첫 턴에 감소하지 않음
- **완전 무효화**: 블록을 0으로 강제 변환 (감소가 아님)

---

## 코드 분석

### 생성자
```java
public NoBlockPower(AbstractCreature owner, int amount, boolean isSourceMonster) {
    this.name = NAME;
    this.ID = "NoBlockPower";
    this.owner = owner;
    this.amount = amount;
    updateDescription();
    loadRegion("noBlock");

    // 몬스터가 적용한 NoBlock은 첫 턴에 감소하지 않음
    if (AbstractDungeon.actionManager.turnHasEnded && isSourceMonster) {
        this.justApplied = true;
    }

    this.type = AbstractPower.PowerType.DEBUFF;
    this.isTurnBased = true;
}

private boolean justApplied = false;
```

**주요 파라미터**:
- `owner`: NoBlock이 적용된 대상
- `amount`: 지속 턴 수
- `isSourceMonster`: 몬스터가 적용했는지 여부

### 핵심 메커니즘

**블록 완전 무효화**:

```java
public float modifyBlockLast(float blockAmount) {
    return 0.0F;  // 모든 블록을 0으로 강제 변환
}
```

**중요**:
- `modifyBlockLast`는 블록 계산의 최종 단계
- 다른 모든 블록 수정자가 적용된 후 마지막에 호출됨
- 0을 반환하여 어떤 블록도 획득하지 못하게 함

### 스택/감소 로직

**턴 종료 시 자동 감소**:

```java
public void atEndOfRound() {
    if (this.justApplied) {
        this.justApplied = false;
        return;  // 적용된 턴에는 감소하지 않음
    }
    if (this.amount == 0) {
        addToBot(new RemoveSpecificPowerAction(this.owner, this.owner, "NoBlockPower"));
    } else {
        addToBot(new ReducePowerAction(this.owner, this.owner, "NoBlockPower", 1));
    }
}
```

**감소 메커니즘**:
1. 몬스터가 적용(`isSourceMonster=true`) + 턴 종료 후(`turnHasEnded=true`) → `justApplied=true`
2. 첫 턴 종료 시 `justApplied=false`로만 설정, 감소 안 함
3. 다음 턴부터 매 턴 종료마다 amount 1씩 감소
4. 0이 되면 자동으로 제거

---

## 수정 방법

### 블록 감소율 조정

**변경 대상**: `modifyBlockLast()` 메서드
**코드 위치**: 줄 53-54

**예시 1: 블록 50% 감소**

```java
@SpirePatch(
    clz = NoBlockPower.class,
    method = "modifyBlockLast"
)
public static class PartialNoBlockPatch {
    @SpirePrefixPatch
    public static SpireReturn<Float> Prefix(NoBlockPower __instance, float blockAmount) {
        // 0 대신 50% 감소
        return SpireReturn.Return(blockAmount * 0.5F);
    }
}
```

**예시 2: 블록 상한선 적용**

```java
@SpirePatch(
    clz = NoBlockPower.class,
    method = "modifyBlockLast"
)
public static class BlockCapPatch {
    @SpirePrefixPatch
    public static SpireReturn<Float> Prefix(NoBlockPower __instance, float blockAmount) {
        // 최대 10 블록까지만 허용
        return SpireReturn.Return(Math.min(blockAmount, 10.0F));
    }
}
```

### 지속 시간 변경

**방법 1: 생성 시 amount 조정**
```java
// NoBlock을 3턴 동안 적용
new NoBlockPower(target, 3, true);
```

**방법 2: 영구 NoBlock**
```java
@SpirePatch(
    clz = NoBlockPower.class,
    method = "atEndOfRound"
)
public static class PermanentNoBlockPatch {
    @SpirePrefixPatch
    public static SpireReturn<Void> Prefix(NoBlockPower __instance) {
        // justApplied만 false로 설정하고 감소 방지
        __instance.justApplied = false;
        return SpireReturn.Return(null);
    }
}
```

### 조건부 블록 차단

**예시: 특정 카드 타입만 블록 차단**

```java
public class SelectiveNoBlockPower extends NoBlockPower {
    public SelectiveNoBlockPower(AbstractCreature owner, int amount, boolean isSourceMonster) {
        super(owner, amount, isSourceMonster);
    }

    @Override
    public float modifyBlockLast(float blockAmount) {
        // 현재 사용 중인 카드 확인
        AbstractCard currentCard = AbstractDungeon.actionManager.currentAction != null ?
            ((UseCardAction)AbstractDungeon.actionManager.currentAction).card : null;

        if (currentCard != null && currentCard.type == CardType.SKILL) {
            // 스킬 카드만 블록 차단
            return 0.0F;
        }
        return blockAmount;  // 다른 카드는 정상적으로 블록 획득
    }
}
```

---

## 관련 파일

**적용하는 카드**:
- 현재 베이스 게임에는 NoBlock을 직접 적용하는 카드 없음
- 주로 고난이도 모드나 이벤트에서 사용됨

**적용하는 몬스터**:
- **Spire Growth** (첨탑 성장체) - 성장 시 플레이어에게 NoBlock 적용
- **Corrupt Heart** (부패한 심장, A20) - 특정 패턴에서 NoBlock 적용 가능

**관련 파워**:
- **Frail** (허약): 블록 25% 감소 (완전 차단 아님)
- **Entangle** (속박): 공격 카드 사용 차단 (블록과 무관)
- **Draw Reduction**: 드로우 감소 (유사한 디버프 메커니즘)

---

## 참고사항

1. **modifyBlockLast 사용**: 블록 수정의 마지막 단계
   - 다른 블록 수정자: `modifyBlock()`, `modifyBlockFirst()`
   - 우선순위: modifyBlockFirst → modifyBlock → modifyBlockLast
   - NoBlock은 가장 마지막에 적용되어 모든 블록을 0으로 만듦

2. **완전 무효화**: 감소가 아닌 0으로 강제 변환
   - Frail: 블록 * 0.75 (25% 감소)
   - NoBlock: 블록 → 0 (100% 차단)

3. **justApplied 메커니즘**: WeakPower와 동일한 방식
   - 몬스터가 적용 시: 적용된 턴에는 감소하지 않음
   - 플레이어가 적용 시: 적용된 턴부터 바로 감소

4. **스택 가능**: 여러 NoBlock 동시 적용 시 amount 누적
   - NoBlock(2) + NoBlock(1) = NoBlock(3)
   - 효과는 동일 (블록 0), 지속 시간만 증가

5. **설명 텍스트**: 단일 설명 (amount와 무관)
   - "Cannot gain Block."

6. **방어 전략 완전 차단**:
   - 블록 기반 덱에 치명적
   - Silent의 Blur, IronClad의 Barricade 무력화
   - Defect의 Frost 오브 효과 없음

7. **대응 방법**:
   - 체력 회복 카드 사용
   - 적 공격력 감소 (Weak, Vulnerable)
   - 회피 능력 (Dodge, Blur는 블록 아님)
   - Intangible (무형화) 파워

8. **turnHasEnded 체크**: 정확한 justApplied 판단
   - 턴 진행 중: isSourceMonster와 무관하게 justApplied = false
   - 턴 종료 후: isSourceMonster=true면 justApplied = true

9. **Artifact 무효화**: Artifact 파워로 NoBlock 적용 자체를 막을 수 있음

10. **플레이어/몬스터 모두 적용 가능**:
    - 플레이어에게: 몬스터가 디버프로 적용
    - 몬스터에게: 특수 카드로 적용 가능 (베이스 게임에는 없음)

11. **블록 계산 파이프라인**:
    ```
    기본 블록 값
    → modifyBlockFirst() (우선 수정자)
    → modifyBlock() (일반 수정자, Frail 등)
    → modifyBlockLast() (최종 수정자, NoBlock)
    → 최종 블록 값
    ```
