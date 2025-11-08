# 흐릿함 (Blur)

## 기본 정보

**클래스명**: `BlurPower`
**전체 경로**: `com.megacrit.cardcrawl.powers.BlurPower`
**ID**: `"Blur"`
**타입**: BUFF
**적용 대상**: 플레이어 전용

---

## 효과

**기본 효과**: 턴 종료 시 Block이 사라지지 않고 다음 턴에도 유지됨
**수치 설명**: `amount`는 Blur가 지속되는 턴 수
**적용 시점**: 턴 종료 시 Block 제거 방지 (암묵적)

**특수 상호작용**:
- **Block 유지**: 매 턴 종료 시 Block이 사라지지 않음
- **Barricade와 차이**: Blur는 턴 제한이 있음, Barricade는 영구 지속
- **스택 가능**: 여러 번 적용 시 지속 턴 수 증가

---

## 코드 분석

### 생성자
```java
public BlurPower(AbstractCreature owner, int amount) {
    this.name = NAME;
    this.ID = "Blur";
    this.owner = owner;
    this.amount = amount;
    this.description = DESCRIPTIONS[0];
    loadRegion("blur");
    this.isTurnBased = true;
}
```

**주요 파라미터**:
- `owner`: 항상 플레이어 (AbstractDungeon.player)
- `amount`: Blur가 지속되는 턴 수

### 핵심 메커니즘

**Block 유지 메커니즘**:

BlurPower 자체에는 Block 유지 코드가 없음. 대신 게임 엔진이 플레이어의 파워를 확인:

```java
// AbstractPlayer.java 또는 GameActionManager.java에서
if (player.hasPower("Blur") || player.hasPower("Barricade")) {
    // Block을 제거하지 않음
} else {
    player.loseBlock();  // 일반적으로 Block 제거
}
```

**중요**:
- BlurPower는 "마커" 역할 (존재 여부만 중요)
- 실제 Block 유지 로직은 게임 엔진에 구현되어 있음
- Barricade(영구)와 같은 방식으로 작동하지만 턴 제한 있음

### 스택/감소 로직

**턴 종료 시 자동 감소**:

```java
public void atEndOfRound() {
    if (this.amount == 0) {
        addToBot(new RemoveSpecificPowerAction(this.owner, this.owner, "Blur"));
    } else {
        addToBot(new ReducePowerAction(this.owner, this.owner, "Blur", 1));
    }
}
```

**감소 메커니즘**:
1. 매 턴 종료마다 amount 1씩 감소
2. amount가 0이 되면 다음 턴 종료 시 제거
3. justApplied 메커니즘 없음 (적용된 턴에도 바로 감소)

---

## 수정 방법

### 지속 시간 변경

**변경 대상**: 파워 생성 시 `amount` 파라미터
**코드 위치**: 파워 적용하는 카드

**예시 1: 더 긴 지속 시간**

```java
// Blur 카드에서 파워 적용 시
new BlurPower(AbstractDungeon.player, 2);  // 기존 1 → 2턴으로 변경
```

**예시 2: 영구 Blur (Barricade처럼)**

```java
@SpirePatch(
    clz = BlurPower.class,
    method = "atEndOfRound"
)
public static class PermanentBlurPatch {
    @SpirePrefixPatch
    public static SpireReturn<Void> Prefix(BlurPower __instance) {
        // 감소하지 않도록 차단
        return SpireReturn.Return(null);
    }
}
```

### 추가 효과 부여

**예시: Blur 적용 시 추가 Block 획득**

```java
public class EnhancedBlurPower extends BlurPower {
    public EnhancedBlurPower(AbstractCreature owner, int amount, int bonusBlock) {
        super(owner, amount);
        // Blur 적용 시 즉시 Block 추가
        owner.addBlock(bonusBlock);
    }

    @Override
    public void atEndOfRound() {
        // 매 턴 종료 시 추가 Block 획득
        this.owner.addBlock(5);
        super.atEndOfRound();  // 기본 감소 로직 유지
    }
}
```

### Block 유지 조건 변경

**예시: 일부 Block만 유지**

게임 엔진 수정 필요 (AbstractPlayer 또는 GameActionManager 패치):

```java
@SpirePatch(
    clz = AbstractPlayer.class,
    method = "applyEndOfTurnTriggers"
)
public static class PartialBlurPatch {
    @SpireInsertPatch(
        locator = Locator.class
    )
    public static void Insert(AbstractPlayer __instance) {
        if (__instance.hasPower("Blur")) {
            // Blur 있을 때 Block의 50%만 유지
            int currentBlock = __instance.currentBlock;
            __instance.loseBlock();  // 전체 제거
            __instance.addBlock(currentBlock / 2);  // 절반 복구
        }
    }

    private static class Locator extends SpireInsertLocator {
        @Override
        public int[] Locate(CtBehavior ctMethodToPatch) throws Exception {
            Matcher finalMatcher = new Matcher.MethodCallMatcher(
                AbstractCreature.class, "loseBlock"
            );
            return LineFinder.findInOrder(ctMethodToPatch, finalMatcher);
        }
    }
}
```

---

## 관련 파일

**적용하는 카드**:
- **Blur** (Silent - 블록 획득 + Blur 1턴)
- **Blur+** (Silent - 블록 획득 + Blur 1턴, 강화 시 블록량 증가)

**적용하는 유물**:
- **Orichalcum**: 블록이 0일 때 자동 블록 획득 (Blur와 시너지)
- **Calipers**: 턴 종료 시 블록 15 손실 (Blur 있으면 0 손실)

**관련 파워**:
- **Barricade** (IronClad): 영구 Block 유지 (Blur의 상위 호환)
- **Next Turn Block**: 다음 턴에 Block 추가 (Blur와 다름)

---

## 참고사항

1. **Barricade와의 차이**:
   | Blur | Barricade |
   |------|-----------|
   | 턴 제한 있음 (amount) | 영구 지속 |
   | Silent 전용 카드 | IronClad 전용 카드 |
   | 턴마다 amount 감소 | amount 감소 없음 |
   | isTurnBased = true | isTurnBased = false |

2. **마커 파워**: BlurPower 자체는 Block 유지 로직 없음
   - 게임 엔진이 "Blur" ID 존재 여부로 판단
   - `hasPower("Blur")` 체크로 Block 제거 방지

3. **justApplied 없음**: 적용된 턴에도 바로 감소
   - Blur(1) 적용 → 턴 종료 시 amount 0으로 감소 → 다음 턴 종료 시 제거
   - 실제로 2턴 동안 Block 유지됨

4. **스택 가능**: 여러 Blur 동시 적용 시 amount 누적
   - Blur(1) + Blur(1) = Blur(2)

5. **설명 텍스트 변화**:
   - amount == 1: "Block is not removed at the end of your turn."
   - amount > 1: "Block is not removed for the next N turns."

6. **Silent 전용**: Silent 클래스의 대표적인 방어 메커니즘
   - IronClad: Barricade (영구)
   - Silent: Blur (일시적)
   - Defect: Buffer (데미지 무효화)
   - Watcher: Stance 전환

7. **Block 누적 전략**: Blur로 Block을 여러 턴 쌓아서 큰 방어막 구축 가능

8. **Calipers 유물 시너지**:
   - Calipers 없음: 턴 종료 시 Block 완전 유지
   - Calipers 있음: 턴 종료 시 Block 15 감소 → Blur 있으면 0 감소

9. **우선순위**: 일반 파워 (특별한 priority 설정 없음)

10. **제거 조건**: amount가 0이 된 다음 턴 종료 시 제거
