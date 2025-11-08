# 에너지 (Energy) 수정 가이드

## 1. 에너지 시스템 개요

### 핵심 클래스: `EnergyManager`
```java
public class EnergyManager {
    public int energy;        // 현재 에너지
    public int energyMaster;  // 기본 에너지 (턴마다 회복되는 양)
}
```

### 에너지 시스템 작동 원리
- **energyMaster**: 플레이어의 기본 에너지. 턴 시작 시 이 값으로 회복
- **energy**: 현재 사용 가능한 에너지
- **EnergyPanel.totalCount**: UI에 표시되는 에너지 카운터

## 2. 에너지가 변경되는 시점

### 2.1 턴 시작 (기본 에너지 회복)
**메서드: `EnergyManager.recharge()`**

```java
public void recharge() {
    if (AbstractDungeon.player.hasRelic("Ice Cream")) {
        // Ice Cream 유물: 남은 에너지 유지
        if (EnergyPanel.totalCount > 0) {
            AbstractDungeon.player.getRelic("Ice Cream").flash();
            // 유물 효과 표시
        }
        EnergyPanel.addEnergy(this.energy);  // 기존 에너지에 추가
    } else if (AbstractDungeon.player.hasPower("Conserve")) {
        // Conserve 파워: 남은 에너지 유지 (1회용)
        if (EnergyPanel.totalCount > 0) {
            // Conserve 파워 1 감소
        }
        EnergyPanel.addEnergy(this.energy);
    } else {
        // 일반적인 경우: 에너지를 energyMaster 값으로 설정
        EnergyPanel.setEnergy(this.energy);
    }
    AbstractDungeon.actionManager.updateEnergyGain(this.energy);
}
```

**작동 순서:**
1. Ice Cream 유물 확인 → 있으면 에너지 추가
2. Conserve 파워 확인 → 있으면 에너지 추가 후 파워 감소
3. 둘 다 없으면 → 에너지를 energyMaster로 재설정

### 2.2 카드 사용 (에너지 소비)
**메서드: `EnergyManager.use(int e)`**

```java
public void use(int e) {
    EnergyPanel.useEnergy(e);
}
```

카드를 사용할 때 `card.energyCost` 만큼 에너지 소비

### 2.3 유물에 의한 에너지 변화
- **Ice Cream**: 턴 종료 시 에너지 유지
- **Lantern**: 전투 시작 시 에너지 +1
- **Art of War**: 공격 카드를 사용하지 않으면 다음 턴 에너지 +1

### 2.4 포션
- **Energy Potion**: 즉시 에너지 +2

### 2.5 파워
- **Energized**: 다음 턴 에너지 +1 또는 +2
- **Conserve**: Ice Cream과 유사, 에너지 유지 (1회용)

## 3. 에너지 수정 방법

### 3.1 시작 에너지 증가
**목표:** 시작 에너지 3 → 4

**방법 1: EnergyManager 생성자 패치**
```java
@SpirePatch(
    clz = EnergyManager.class,
    method = SpirePatch.CONSTRUCTOR
)
public static class IncreaseStartingEnergy {
    @SpirePostfixPatch
    public static void Postfix(EnergyManager __instance, int e) {
        // 기본 에너지를 1 증가
        __instance.energyMaster = e + 1;
    }
}
```

**방법 2: 플레이어 초기화 시 수정**
```java
@SpirePatch(
    clz = AbstractPlayer.class,
    method = "applyStartOfCombatLogic"
)
public static class StartCombatEnergyBoost {
    @SpirePostfixPatch
    public static void Postfix(AbstractPlayer __instance) {
        // 전투 시작 시 에너지 +1
        __instance.energy.energyMaster++;
    }
}
```

### 3.2 특정 캐릭터 에너지 변경
**목표:** Ironclad만 시작 에너지 +1

```java
@SpirePatch(
    clz = EnergyManager.class,
    method = SpirePatch.CONSTRUCTOR
)
public static class IroncladEnergyBoost {
    @SpirePostfixPatch
    public static void Postfix(EnergyManager __instance, int e) {
        if (AbstractDungeon.player instanceof Ironclad) {
            __instance.energyMaster++;
        }
    }
}
```

### 3.3 턴마다 에너지 추가
**목표:** 매 턴 시작 시 에너지 +1 추가

```java
@SpirePatch(
    clz = EnergyManager.class,
    method = "recharge"
)
public static class BonusEnergyPerTurn {
    @SpireInsertPatch(
        locator = Locator.class
    )
    public static void Insert(EnergyManager __instance) {
        // energyMaster에 1 추가 (이 값으로 회복됨)
        __instance.energy = __instance.energyMaster + 1;
    }

    private static class Locator extends SpireInsertLocator {
        @Override
        public int[] Locate(CtBehavior ctMethodToPatch) throws Exception {
            Matcher finalMatcher = new Matcher.MethodCallMatcher(
                EnergyPanel.class, "setEnergy"
            );
            return LineFinder.findInOrder(ctMethodToPatch, finalMatcher);
        }
    }
}
```

**더 간단한 방법 - Postfix 사용:**
```java
@SpirePatch(
    clz = EnergyManager.class,
    method = "recharge"
)
public static class BonusEnergyPerTurnSimple {
    @SpirePostfixPatch
    public static void Postfix(EnergyManager __instance) {
        // recharge 후에 에너지 1 추가
        EnergyPanel.addEnergy(1);
    }
}
```

### 3.4 특정 난이도에서 에너지 감소
**목표:** Ascension 20+ 시작 에너지 -1

```java
@SpirePatch(
    clz = EnergyManager.class,
    method = SpirePatch.CONSTRUCTOR
)
public static class HighAscensionEnergyPenalty {
    @SpirePostfixPatch
    public static void Postfix(EnergyManager __instance, int e) {
        if (AbstractDungeon.ascensionLevel >= 20) {
            __instance.energyMaster--;
            // 음수 방지
            if (__instance.energyMaster < 0) {
                __instance.energyMaster = 0;
            }
        }
    }
}
```

## 4. 에너지 관련 유물/파워

### Ice Cream (유물)
- 효과: 턴 종료 시 남은 에너지 유지
- 구현: `recharge()` 메서드에서 `EnergyPanel.setEnergy()` 대신 `addEnergy()` 사용

### Energized 파워
- 효과: 다음 턴 에너지 +1 또는 +2 (일회성)
- 구현: `atStartOfTurn()` 훅에서 `EnergyPanel.addEnergy()` 호출 후 파워 제거

## 5. 실용적인 수정 예시

### 예시 1: 무한 에너지
**목표:** 에너지를 사용해도 감소하지 않음

```java
@SpirePatch(
    clz = EnergyManager.class,
    method = "use"
)
public static class InfiniteEnergy {
    @SpirePrefixPatch
    public static SpireReturn<Void> Prefix(EnergyManager __instance, int e) {
        // 에너지를 사용하지 않도록 차단
        return SpireReturn.Return();
    }
}
```

**더 나은 방법 - UI만 업데이트:**
```java
@SpirePatch(
    clz = EnergyPanel.class,
    method = "useEnergy"
)
public static class InfiniteEnergyUI {
    @SpirePrefixPatch
    public static SpireReturn<Void> Prefix(int e) {
        // 에너지 소비 무시
        // UI는 그대로 유지
        return SpireReturn.Return();
    }
}
```

### 예시 2: 에너지 코스트 모두 절반
**목표:** 모든 카드의 에너지 코스트를 절반으로 감소

```java
@SpirePatch(
    clz = AbstractCard.class,
    method = "applyPowers"
)
public static class HalfEnergyCost {
    @SpirePostfixPatch
    public static void Postfix(AbstractCard __instance) {
        // costForTurn 필드를 수정
        if (__instance.costForTurn > 0) {
            __instance.costForTurn = __instance.costForTurn / 2;
            // 0.5 → 0으로 변환 방지
            if (__instance.costForTurn == 0 && __instance.cost > 0) {
                __instance.costForTurn = 1;
            }
        }
        // X 코스트는 그대로 유지
        if (__instance.costForTurn == -1) {
            __instance.isCostModified = false;
        }
    }
}
```

### 예시 3: A25+ 시작 에너지 -1
**목표:** Ascension 25 이상에서 시작 에너지 1 감소

```java
@SpirePatch(
    clz = AbstractPlayer.class,
    method = "applyStartOfCombatLogic"
)
public static class A25EnergyPenalty {
    @SpirePostfixPatch
    public static void Postfix(AbstractPlayer __instance) {
        if (AbstractDungeon.ascensionLevel >= 25) {
            __instance.energy.energyMaster--;
            // 음수 방지
            if (__instance.energy.energyMaster < 0) {
                __instance.energy.energyMaster = 0;
            }
            // 현재 에너지도 조정
            __instance.energy.energy = __instance.energy.energyMaster;
        }
    }
}
```

## 6. 관련 클래스

### EnergyManager
- **필드:**
  - `int energy`: 현재 에너지
  - `int energyMaster`: 기본 에너지
- **메서드:**
  - `prep()`: 전투 시작 시 에너지 초기화
  - `recharge()`: 턴 시작 시 에너지 회복
  - `use(int e)`: 에너지 사용

### EnergyPanel
- **정적 메서드:**
  - `setEnergy(int e)`: 에너지를 특정 값으로 설정
  - `addEnergy(int e)`: 에너지 추가
  - `useEnergy(int e)`: 에너지 사용
- **정적 필드:**
  - `int totalCount`: 현재 에너지 양 (UI 표시용)

### AbstractCard
- **필드:**
  - `int cost`: 카드의 기본 에너지 코스트
  - `int costForTurn`: 현재 턴에서의 실제 에너지 코스트
  - `boolean isCostModified`: 코스트가 수정되었는지 여부

## 7. 주의사항

### 7.1 음수 에너지 방지
```java
if (__instance.energyMaster < 0) {
    __instance.energyMaster = 0;
}
```
에너지를 감소시키는 패치를 작성할 때는 항상 음수 체크 필요

### 7.2 EnergyPanel 동기화
`EnergyManager`의 값을 직접 수정할 경우 `EnergyPanel`도 함께 업데이트해야 함:
```java
__instance.energy.energyMaster = 5;
EnergyPanel.setEnergy(__instance.energy.energyMaster);
```

### 7.3 Ice Cream 유물과의 충돌
Ice Cream이 있을 때 에너지 시스템을 수정하면 예상치 못한 동작 발생 가능
- `recharge()` 메서드를 패치할 때는 Ice Cream 로직 이전/이후를 고려

### 7.4 X 코스트 카드 처리
X 코스트 카드는 `cost == -1`로 표현됨:
```java
if (__instance.cost == -1) {
    // X 코스트 카드는 특별 처리 필요
    return;
}
```

### 7.5 전투 시작 vs 턴 시작
- **전투 시작**: `AbstractPlayer.applyStartOfCombatLogic()`
  - 첫 턴에만 적용되는 에너지 수정
- **턴 시작**: `EnergyManager.recharge()`
  - 매 턴 적용되는 에너지 수정

### 7.6 energyMaster vs energy
- `energyMaster`: 기본 에너지 (턴 시작 시 회복되는 양)
- `energy`: 현재 에너지 (실시간으로 변화)
- 대부분의 경우 `energyMaster`를 수정해야 영구적인 변화

### 7.7 패치 적용 시점
```java
@SpirePatch(
    clz = EnergyManager.class,
    method = "prep"  // 전투 시작
)
// vs
@SpirePatch(
    clz = EnergyManager.class,
    method = "recharge"  // 턴 시작
)
```
- `prep()`: 전투당 1회 호출 (초기화)
- `recharge()`: 턴마다 호출 (에너지 회복)
