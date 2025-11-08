# 방벽 (Barricade)

## 기본 정보

**클래스명**: `BarricadePower`
**전체 경로**: `com.megacrit.cardcrawl.powers.BarricadePower`
**ID**: `"Barricade"`
**타입**: 버프
**적용 대상**: 플레이어 / 몬스터 모두

---

## 효과

**기본 효과**: 턴 종료 시 Block이 소멸되지 않음
**수치 설명**: `amount = -1` (영구 지속, 수치 표시 없음)
**적용 시점**: 턴 종료 시 Block 소멸 방지

**중요**:
- 일반적으로 Block은 턴 종료 시 0으로 리셋됨
- Barricade가 있으면 이 리셋이 일어나지 않음
- Block을 무한정 축적할 수 있음

---

## 코드 분석

### 생성자
```java
public BarricadePower(AbstractCreature owner) {
    this.name = NAME;
    this.ID = "Barricade";
    this.owner = owner;
    this.amount = -1;  // 영구 지속
    updateDescription();
    loadRegion("barricade");
}
```

**주요 특징**:
- `amount = -1`: 영구 지속 파워 (턴 종료 시 감소하지 않음)
- 별도의 생성자 파라미터 없음 (항상 영구)

### 핵심 메커니즘

**Block 유지 로직**:

Barricade 자체는 코드가 매우 간단합니다. 실제 Block 소멸 방지는 게임 엔진의 턴 종료 로직에서 Barricade 파워 존재 여부를 확인하여 처리됩니다.

```java
// AbstractPlayer.java의 턴 종료 로직 (참고용)
public void applyEndOfTurnTriggers() {
    // Barricade가 없으면 Block을 0으로 설정
    if (!this.hasPower("Barricade")) {
        this.loseBlock();
    }
}
```

### 설명 업데이트

```java
public void updateDescription() {
    if (this.owner.isPlayer) {
        this.description = DESCRIPTIONS[0];  // 플레이어용 설명
    } else {
        this.description = DESCRIPTIONS[1];  // 몬스터용 설명
    }
}
```

---

## 수정 방법

### 제한적 Barricade (N턴 지속)

**변경 대상**: 생성자 및 턴 종료 로직 추가
**목적**: 영구가 아닌 N턴 동안만 유지

```java
@SpirePatch(
    clz = BarricadePower.class,
    method = SpirePatch.CONSTRUCTOR
)
public static class LimitedBarricadePatch {
    @SpirePrefixPatch
    public static void Prefix(BarricadePower __instance, AbstractCreature owner) {
        // amount를 3으로 설정하여 3턴 지속
        __instance.amount = 3;
        __instance.isTurnBased = true;
    }
}

// 턴 종료 시 감소 로직 추가
@SpirePatch(
    clz = BarricadePower.class,
    method = "atEndOfRound"
)
public static class BarricadeDurationPatch {
    @SpireInsertPatch(locator = Locator.class)
    public static void Insert(BarricadePower __instance) {
        if (__instance.amount > 0) {
            __instance.amount--;
            if (__instance.amount == 0) {
                addToBot(new RemoveSpecificPowerAction(__instance.owner, __instance.owner, "Barricade"));
            }
        }
    }
}
```

### Block 보존율 조정

**목적**: Block의 일부만 유지 (예: 50% 유지)

```java
@SpirePatch(
    clz = AbstractPlayer.class,
    method = "applyEndOfTurnTriggers"
)
public static class PartialBarricadePatch {
    @SpirePrefixPatch
    public static void Prefix(AbstractPlayer __instance) {
        if (__instance.hasPower("Barricade")) {
            // 50%만 유지
            int currentBlock = __instance.currentBlock;
            __instance.loseBlock();
            __instance.addBlock((int)(currentBlock * 0.5f));
        }
    }
}
```

### Block 상한선 추가

**목적**: 무한 축적을 방지하고 최대 Block 제한

```java
@SpirePatch(
    clz = AbstractCreature.class,
    method = "addBlock"
)
public static class BarricadeCapPatch {
    @SpirePrefixPatch
    public static void Prefix(AbstractCreature __instance, @ByRef int[] blockAmount) {
        if (__instance.hasPower("Barricade")) {
            int maxBlock = 999;  // 최대 Block 상한
            int futureBlock = __instance.currentBlock + blockAmount[0];

            if (futureBlock > maxBlock) {
                blockAmount[0] = maxBlock - __instance.currentBlock;
            }
        }
    }
}
```

---

## 관련 파일

**적용하는 카드**:
- Barricade (아이언클래드 - 파워 카드)
- Barricade+ (업그레이드 버전, 코스트 감소)

**관련 유물**:
- **Calipers**: Barricade 없이도 턴당 15 Block 유지 (Barricade와 중첩 가능)
- **Blur** (파워 카드): 다음 턴까지만 Block 유지 (임시 Barricade)

**시너지 카드**:
- Entrench: Block을 2배로 만듦 (Barricade와 조합 시 강력)
- Body Slam: Block만큼 대미지 (축적된 Block 활용)
- Metallicize: 매 턴 Block 획득 (지속 축적)
- Juggernaut: Block 획득 시 대미지 (Block 축적마다 대미지)

---

## 참고사항

1. **amount = -1**: 영구 지속 파워의 표준 표시 방법
2. **Block 축적 전략**:
   - Entrench로 Block 배증
   - Body Slam으로 축적된 Block을 대미지로 전환
   - Metallicize, Flame Barrier 등으로 지속적 Block 획득
3. **Calipers와의 차이**:
   - Barricade: 모든 Block 유지
   - Calipers: 턴당 15 Block만 유지
   - 둘 다 있으면 Barricade가 우선 (모든 Block 유지)
4. **제거 불가**: 일반적인 방법으로는 Barricade를 제거할 수 없음 (영구 버프)
5. **몬스터도 사용 가능**: Bronze Automaton 등 일부 보스가 사용
6. **코드 단순성**: Barricade 자체는 단순하며, 실제 Block 유지 로직은 게임 엔진에서 처리
