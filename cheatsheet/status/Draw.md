# 추가 드로우 (Draw)

## 기본 정보

**클래스명**: `DrawPower`
**전체 경로**: `com.megacrit.cardcrawl.powers.DrawPower`
**ID**: `"Draw"`
**타입**: BUFF (양수) / DEBUFF (음수)
**적용 대상**: 플레이어 전용

---

## 효과

**기본 효과**: 매 턴 시작 시 추가로 카드 드로우 (또는 감소)
**수치 설명**: `amount`는 추가/감소되는 드로우 카드 수
**적용 시점**: 파워 적용 즉시 `gameHandSize` 변경

**특수 상호작용**:
- **양수(+)**: 매 턴 추가 드로우 (BUFF)
- **음수(-)**: 매 턴 드로우 감소 (DEBUFF)
- **즉시 적용**: 파워 적용 시 `AbstractDungeon.player.gameHandSize` 즉시 변경
- **영구 지속**: 제거되기 전까지 계속 유지됨

---

## 코드 분석

### 생성자
```java
public DrawPower(AbstractCreature owner, int amount) {
    this.name = NAME;
    this.ID = "Draw";
    this.owner = owner;
    this.amount = amount;
    updateDescription();
    loadRegion("draw");

    // 음수면 DEBUFF, 양수면 BUFF
    if (amount < 0) {
        this.type = AbstractPower.PowerType.DEBUFF;
        loadRegion("draw2");  // 다른 이미지
    } else {
        this.type = AbstractPower.PowerType.BUFF;
        loadRegion("draw");
    }

    this.isTurnBased = false;  // 턴 종료 시 감소하지 않음
    AbstractDungeon.player.gameHandSize += amount;  // 즉시 적용
}
```

**주요 파라미터**:
- `owner`: 항상 플레이어 (AbstractDungeon.player)
- `amount`: 추가/감소 드로우 수 (양수=BUFF, 음수=DEBUFF)

### 핵심 메커니즘

**즉시 gameHandSize 변경**:

```java
// 생성자에서 즉시 적용
AbstractDungeon.player.gameHandSize += amount;
```

**중요**:
- `gameHandSize`는 매 턴 드로우할 카드 수를 결정하는 플레이어 필드
- 파워 적용 즉시 변경됨 (다음 턴부터가 아님)
- 기본 `gameHandSize`는 5

**제거 시 복구**:

```java
public void onRemove() {
    AbstractDungeon.player.gameHandSize -= this.amount;
}
```

### 스택 관리

**reducePower()**: 파워 감소 시 호출

```java
public void reducePower(int reduceAmount) {
    this.fontScale = 8.0F;  // 시각 효과
    this.amount -= reduceAmount;

    if (this.amount == 0) {
        addToTop(new RemoveSpecificPowerAction(this.owner, this.owner, "Draw"));
    }
}
```

**스택 메커니즘**:
1. `reducePower`로 amount 감소 가능
2. 0이 되면 자동으로 파워 제거
3. 제거 시 `gameHandSize`도 원래대로 복구됨

---

## 수정 방법

### 드로우 수 변경

**변경 대상**: 파워 생성 시 `amount` 파라미터
**코드 위치**: 파워 적용하는 카드/유물

**예시 1: 항상 2장 추가 드로우**

```java
// 카드나 유물에서 파워 적용 시
new DrawPower(AbstractDungeon.player, 2);  // 기존 1 → 2로 변경
```

**예시 2: DrawPower 효과 배율 증가**

```java
@SpirePatch(
    clz = DrawPower.class,
    method = SpirePatch.CONSTRUCTOR
)
public static class DoubleDr awPowerPatch {
    @SpirePostfixPatch
    public static void Postfix(DrawPower __instance, AbstractCreature owner, int amount) {
        // 원래 amount 제거하고 2배 적용
        AbstractDungeon.player.gameHandSize -= amount;
        __instance.amount = amount * 2;
        AbstractDungeon.player.gameHandSize += __instance.amount;
        __instance.updateDescription();
    }
}
```

### 지속 시간 추가

**기본적으로 영구 지속**: `isTurnBased = false`이므로 턴 종료 시 감소하지 않음

**방법: 턴 기반으로 변경**

```java
public class TemporaryDrawPower extends DrawPower {
    private int turnsRemaining;

    public TemporaryDrawPower(AbstractCreature owner, int amount, int turns) {
        super(owner, amount);
        this.turnsRemaining = turns;
        this.isTurnBased = true;
    }

    @Override
    public void atEndOfRound() {
        this.turnsRemaining--;
        if (this.turnsRemaining <= 0) {
            addToBot(new RemoveSpecificPowerAction(this.owner, this.owner, "Draw"));
        }
    }
}
```

### 음수/양수 동작 변경

**방법: DEBUFF 시 더 큰 페널티**

```java
@SpirePatch(
    clz = DrawPower.class,
    method = SpirePatch.CONSTRUCTOR
)
public static class HarshDrawDebuffPatch {
    @SpirePostfixPatch
    public static void Postfix(DrawPower __instance, AbstractCreature owner, int amount) {
        if (amount < 0) {
            // DEBUFF 시 2배 페널티
            AbstractDungeon.player.gameHandSize -= amount;  // 기존 제거
            int harshPenalty = amount * 2;
            __instance.amount = harshPenalty;
            AbstractDungeon.player.gameHandSize += harshPenalty;
            __instance.updateDescription();
        }
    }
}
```

---

## 관련 파일

**적용하는 카드**:
- Acrobatics (Silent - +3 드로우, 1턴)
- Backflip (Silent - +2 드로우, 1턴)
- Deep Breath (Watcher - +2 드로우)
- Evaluate (Watcher - +2 드로우, Insight 적용)
- Concentrate (Defect - +3 드로우)
- Quick Slash (Silent - +1 드로우)

**적용하는 유물**:
- **Bag of Preparation**: 전투 시작 시 +2 드로우 (첫 턴만)
- **Snecko Eye**: 무작위 코스트 대신 +2 드로우
- **Ring of the Snake**: 전투 시작 시 +2 드로우

**관련 파워**:
- **DrawReductionPower**: 드로우 감소 (별도 파워)
- **Next Turn Block**: 다음 턴 효과 지연

---

## 참고사항

1. **즉시 적용**: 파워 생성 시 `gameHandSize` 즉시 변경
2. **영구 지속**: `isTurnBased = false`로 턴 종료 시 감소하지 않음
3. **제거 시 복구**: `onRemove()`에서 `gameHandSize` 원래대로 복구
4. **양수/음수 구분**:
   - 양수: BUFF, "draw" 이미지
   - 음수: DEBUFF, "draw2" 이미지
5. **스택 가능**: 여러 DrawPower 동시 적용 가능
   - DrawPower(+2) + DrawPower(+1) = 총 +3 드로우
6. **설명 텍스트 변화**:
   - amount == 1: "Draw 1 additional card"
   - amount > 1: "Draw N additional cards"
   - amount == -1: "Draw 1 fewer card"
   - amount < -1: "Draw N fewer cards"
7. **플레이어 전용**: 몬스터에게 적용되지 않음
8. **gameHandSize 직접 수정**: 다른 드로우 수정 효과와 직접 상호작용
9. **reducePower 가능**: amount를 동적으로 감소시킬 수 있음
10. **fontScale 효과**: reducePower 호출 시 시각적 피드백 (8.0F)
