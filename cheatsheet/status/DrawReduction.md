# 드로우 감소 (Draw Reduction)

## 기본 정보

**클래스명**: `DrawReductionPower`
**전체 경로**: `com.megacrit.cardcrawl.powers.DrawReductionPower`
**ID**: `"Draw Reduction"`
**타입**: DEBUFF
**적용 대상**: 플레이어 전용

---

## 효과

**기본 효과**: 매 턴 드로우 카드 수 감소
**수치 설명**: `amount`는 감소하는 턴 수 (드로우 감소량 아님)
**적용 시점**: 파워 적용 즉시 `gameHandSize` 1 감소

**특수 상호작용**:
- **즉시 적용**: 파워 적용 시 `gameHandSize` 즉시 -1
- **턴 기반**: 매 턴 종료마다 amount 1씩 감소
- **justApplied**: 적용된 턴에는 감소하지 않음

---

## 코드 분석

### 생성자
```java
public DrawReductionPower(AbstractCreature owner, int amount) {
    this.name = NAME;
    this.ID = "Draw Reduction";
    this.owner = owner;
    this.amount = amount;  // 지속 턴 수
    updateDescription();
    loadRegion("lessdraw");
    this.type = AbstractPower.PowerType.DEBUFF;
    this.isTurnBased = true;
}

private boolean justApplied = true;
```

**주요 파라미터**:
- `owner`: 항상 플레이어 (AbstractDungeon.player)
- `amount`: 지속 턴 수 (드로우 감소량이 아님!)

### 핵심 메커니즘

**즉시 gameHandSize 감소**:

```java
public void onInitialApplication() {
    AbstractDungeon.player.gameHandSize--;  // -1 고정
}
```

**중요**:
- `amount`는 지속 턴 수이지 감소량이 아님
- 드로우 감소는 항상 -1로 고정
- DrawPower와 달리 amount가 감소량을 의미하지 않음

**제거 시 복구**:

```java
public void onRemove() {
    AbstractDungeon.player.gameHandSize++;  // +1 복구
}
```

### 스택/감소 로직

**턴 종료 시 자동 감소**:

```java
public void atEndOfRound() {
    if (this.justApplied) {
        this.justApplied = false;
        return;  // 적용된 턴에는 감소하지 않음
    }
    addToBot(new ReducePowerAction(this.owner, this.owner, "Draw Reduction", 1));
}
```

**감소 메커니즘**:
1. 파워 적용 시 `justApplied = true`
2. 첫 턴 종료 시 `justApplied = false`로만 설정, 감소 안 함
3. 다음 턴부터 매 턴 종료마다 amount 1씩 감소
4. amount가 0이 되면 자동으로 제거 (ReducePowerAction이 처리)

---

## 수정 방법

### 드로우 감소량 변경

**변경 대상**: `onInitialApplication()` 메서드의 감소량
**코드 위치**: 줄 30

**예시: 2장 감소로 변경**

```java
@SpirePatch(
    clz = DrawReductionPower.class,
    method = "onInitialApplication"
)
public static class HarshDrawReductionPatch {
    @SpirePrefixPatch
    public static SpireReturn<Void> Prefix(DrawReductionPower __instance) {
        // 기존 -1 대신 -2 적용
        AbstractDungeon.player.gameHandSize -= 2;
        return SpireReturn.Return(null);
    }
}

// onRemove도 같이 패치해야 복구가 정확함
@SpirePatch(
    clz = DrawReductionPower.class,
    method = "onRemove"
)
public static class HarshDrawReductionRemovePatch {
    @SpirePrefixPatch
    public static SpireReturn<Void> Prefix(DrawReductionPower __instance) {
        AbstractDungeon.player.gameHandSize += 2;  // +2 복구
        return SpireReturn.Return(null);
    }
}
```

### amount를 감소량으로 사용

**방법: amount를 드로우 감소량으로 재해석**

```java
public class VariableDrawReductionPower extends DrawReductionPower {
    private int reductionAmount;

    public VariableDrawReductionPower(AbstractCreature owner, int reduction, int turns) {
        super(owner, turns);
        this.reductionAmount = reduction;
    }

    @Override
    public void onInitialApplication() {
        AbstractDungeon.player.gameHandSize -= this.reductionAmount;
    }

    @Override
    public void onRemove() {
        AbstractDungeon.player.gameHandSize += this.reductionAmount;
    }

    @Override
    public void updateDescription() {
        if (this.amount == 1) {
            this.description = "Draw " + this.reductionAmount + " fewer card for 1 turn.";
        } else {
            this.description = "Draw " + this.reductionAmount + " fewer cards for " +
                             this.amount + " turns.";
        }
    }
}
```

### 지속 시간 변경

**방법 1: 생성 시 amount 조정**
```java
// 드로우 감소를 3턴 동안 적용
new DrawReductionPower(player, 3);  // amount = 지속 턴 수
```

**방법 2: 영구 지속**
```java
@SpirePatch(
    clz = DrawReductionPower.class,
    method = "atEndOfRound"
)
public static class PermanentDrawReductionPatch {
    @SpirePrefixPatch
    public static SpireReturn<Void> Prefix(DrawReductionPower __instance) {
        // justApplied만 false로 설정하고 감소 방지
        if (__instance.justApplied) {
            __instance.justApplied = false;
        }
        return SpireReturn.Return(null);
    }
}
```

---

## 관련 파일

**적용하는 카드**:
- Doubt (Watcher - 의심 카드, 턴 종료 시 드로우 -1)
- Writhe (Curse - 저주 카드, 드로우 감소 가능성)

**적용하는 몬스터**:
- Hexaghost (헥사고스트) - Inferno 패턴에서 드로우 감소
- Time Eater (시간 포식자) - 12장 이상 사용 시 드로우 감소

**관련 파워**:
- **DrawPower**: 드로우 증가/감소 (영구, amount가 감소량)
- **No Draw**: 드로우 완전 차단 (존재 시)

---

## 참고사항

1. **amount의 의미**: 지속 턴 수 (감소량이 아님!)
   - DrawReductionPower(3) = 3턴 동안 드로우 -1
   - DrawPower(-1)과 다름!
2. **고정 감소량**: 항상 -1 드로우 (변경 불가, 패치 필요)
3. **즉시 적용**: `onInitialApplication()`에서 즉시 gameHandSize 감소
4. **justApplied 메커니즘**: 적용된 턴에는 amount 감소 안 함
5. **설명 텍스트 변화**:
   - amount == 1: "Draw 1 less card next turn."
   - amount > 1: "Draw 1 less card for the next N turns."
6. **스택 가능**: 여러 DrawReductionPower 중첩 가능
   - DrawReductionPower(2) 적용 시 gameHandSize -1
   - 다시 DrawReductionPower(1) 적용 시 gameHandSize -1 추가 (총 -2)
7. **DrawPower와의 차이**:
   | DrawReductionPower | DrawPower |
   |-------------------|-----------|
   | amount = 지속 턴 수 | amount = 드로우 증감량 |
   | 감소량 -1 고정 | amount만큼 증감 |
   | 턴 종료 시 amount 감소 | amount 감소 안 함 (영구) |
   | justApplied 있음 | justApplied 없음 |
8. **제거 시 복구**: `onRemove()`에서 gameHandSize 정확히 +1 복구
9. **ReducePowerAction**: 0이 되면 자동 제거 (명시적 RemoveSpecificPowerAction 불필요)
10. **플레이어 전용**: 몬스터에게 적용되지 않음 (gameHandSize는 플레이어 필드)
