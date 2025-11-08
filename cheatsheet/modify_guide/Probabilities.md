# 확률 (Probabilities) 수정 가이드

## 1. 확률 시스템 개요

### Random 클래스
**위치:** `com.megacrit.cardcrawl.random.Random`

Slay the Spire의 모든 확률 계산에 사용되는 커스텀 난수 생성기입니다.
```java
public class Random {
    public RandomXS128 random;
    public int counter = 0;

    public int random(int range)              // 0 ~ range
    public int random(int start, int end)     // start ~ end
    public boolean randomBoolean()            // true/false
    public boolean randomBoolean(float chance) // 확률 기반 (0.0 ~ 1.0)
    public float random()                     // 0.0 ~ 1.0
    public float random(float range)          // 0.0 ~ range
}
```

### AbstractDungeon RNG 시드들
**위치:** `AbstractDungeon.java` 라인 200-211

게임의 모든 난수는 시드 기반으로 관리됩니다:
```java
public static Random monsterRng;      // 몬스터 조우
public static Random mapRng;          // 맵 생성
public static Random eventRng;        // 이벤트 선택
public static Random merchantRng;     // 상점 재고
public static Random cardRng;         // 카드 보상 레어리티
public static Random treasureRng;     // 보물, 골드
public static Random relicRng;        // 유물 선택
public static Random potionRng;       // 포션 드롭/선택
public static Random monsterHpRng;    // 몬스터 HP 랜덤화
public static Random aiRng;           // AI 행동 패턴
public static Random shuffleRng;      // 덱 섞기
public static Random cardRandomRng;   // 카드 랜덤 효과
public static Random miscRng;         // 기타
```

## 2. 주요 확률 요소

### 2.1 포션 드롭 확률
**위치:** `AbstractRoom.java` 라인 764-799

**기본값:** 40%
```java
int chance = 40;
chance += blizzardPotionMod; // 드롭 실패 시 +10%, 성공 시 -10%

if (AbstractDungeon.potionRng.random(0, 99) < chance) {
    // 포션 드롭 성공
    this.rewards.add(new RewardItem(AbstractDungeon.returnRandomPotion()));
    blizzardPotionMod -= 10;
} else {
    blizzardPotionMod += 10;
}
```

**Ascension 차이:** 없음 (40% 고정)

**상수 위치:** `Settings.java` 라인 203-204
```java
public static final int NORMAL_POTION_DROP_RATE = 40;
public static final int ELITE_POTION_DROP_RATE = 40;
```

**특수 조건:**
- **White Beast Statue 유물:** 100% 확률
- **보상 4개 이상:** 0% (드롭 안 됨)

### 2.2 카드 레어리티 확률
**위치:** `AbstractRoom.java` 라인 179-207

**기본 확률 (일반 전투):**
```java
baseRareCardChance = 3;         // 3% - RARE
baseUncommonCardChance = 37;    // 37% - UNCOMMON
// 나머지 60% - COMMON
```

**Blizzard 시스템:**
카드 획득 시마다 확률 조정:
```java
// AbstractDungeon.java 라인 1806-1820
switch (rarity) {
    case RARE:
        cardBlizzRandomizer = cardBlizzStartOffset;  // 리셋
        break;
    case UNCOMMON:
        cardBlizzRandomizer -= cardBlizzGrowth;      // -3
        if (cardBlizzRandomizer <= cardBlizzMaxOffset) {
            cardBlizzRandomizer = cardBlizzMaxOffset;  // 최소 -40
        }
        break;
}
```

**레어리티 판정:**
```java
// AbstractRoom.java 라인 189-206
int roll = cardRng.random(99);         // 0-99
roll += cardBlizzRandomizer;           // Blizzard 보정

if (roll < rareCardChance) {           // 3 + 보정
    return CardRarity.RARE;
} else if (roll < uncommonCardChance) { // 37 + 보정
    return CardRarity.UNCOMMON;
} else {
    return CardRarity.COMMON;
}
```

### 2.3 포션 레어리티 확률
**위치:** `PotionHelper.java` 라인 64-65

```java
public static int POTION_COMMON_CHANCE = 65;    // 65%
public static int POTION_UNCOMMON_CHANCE = 25;  // 25%
// 나머지 10% - RARE
```

### 2.4 엘리트 방 등장 확률
**위치:** 맵 생성 로직 (던전별 다름)

던전마다 엘리트 방 개수가 정해져 있으며, 랜덤하게 배치됩니다.
- **Exordium:** 총 2-3개
- **The City:** 총 3개
- **The Beyond:** 총 3개

### 2.5 이벤트 vs 적 확률
**위치:** ? 방 진입 시 결정

? 방에서는 이벤트와 적 조우 중 하나가 선택됩니다.

### 2.6 보물 상자 확률
**위치:** 맵 생성 시 결정

각 던전마다 보물 방이 정해진 층에 배치됩니다.

### 2.7 몬스터 조우 확률
**위치:** 몬스터 풀에서 선택

같은 몬스터가 연속으로 나오지 않도록 방지:
```java
// 이전 몬스터와 다른 몬스터 선택
```

## 3. 확률 수정 방법

### 3.1 포션 드롭 확률 증가

**목표:** 포션 드롭 40% → 100%

```java
@SpirePatch(
    clz = AbstractRoom.class,
    method = "addPotionToRewards",
    paramtypez = {}
)
public static class GuaranteedPotionDrop {
    @SpirePrefixPatch
    public static SpireReturn<Void> Prefix(AbstractRoom __instance) {
        // 원본 메서드 무시하고 100% 드롭
        if (__instance.rewards.size() < 4) {
            __instance.rewards.add(new RewardItem(AbstractDungeon.returnRandomPotion()));
        }
        return SpireReturn.Return(null);
    }
}
```

**또는 확률만 조정:**
```java
@SpirePatch(
    clz = AbstractRoom.class,
    method = "addPotionToRewards",
    paramtypez = {}
)
public static class IncreasePotionChance {
    @SpireInsertPatch(
        locator = ChanceCheckLocator.class
    )
    public static void Insert(@ByRef int[] chance) {
        // 확률을 100%로 변경
        chance[0] = 100;
    }

    private static class ChanceCheckLocator extends SpireInsertLocator {
        @Override
        public int[] Locate(CtBehavior ctMethodToPatch) throws Exception {
            Matcher finalMatcher = new Matcher.FieldAccessMatcher(
                AbstractDungeon.class, "potionRng"
            );
            return LineFinder.findInOrder(ctMethodToPatch, finalMatcher);
        }
    }
}
```

### 3.2 레어 카드 확률 대폭 증가

**목표:** 레어 카드 3% → 30%

```java
@SpirePatch(
    clz = AbstractRoom.class,
    method = SpirePatch.CLASS
)
public static class IncreaseRareCardChance {
    public static SpireField<Integer> customRareChance = new SpireField<>(() -> 30);
}

@SpirePatch(
    clz = AbstractRoom.class,
    method = "getCardRarity",
    paramtypez = {int.class, boolean.class}
)
public static class ModifyRareChance {
    @SpirePrefixPatch
    public static void Prefix(AbstractRoom __instance) {
        __instance.baseRareCardChance = 30;
        __instance.rareCardChance = 30;
    }
}
```

**또는 간단하게:**
```java
@SpirePatch(
    clz = AbstractRoom.class,
    method = "getCardRarity",
    paramtypez = {int.class, boolean.class}
)
public static class ForceRareCards {
    @SpirePostfixPatch
    public static AbstractCard.CardRarity Postfix(AbstractCard.CardRarity __result, int roll) {
        // 30% 확률로 강제 레어
        if (roll < 30) {
            return AbstractCard.CardRarity.RARE;
        }
        return __result;
    }
}
```

### 3.3 포션 레어리티 확률 조정

**목표:** 레어 포션 10% → 50%

```java
@SpirePatch(
    clz = PotionHelper.class,
    method = SpirePatch.CONSTRUCTOR
)
public static class ModifyPotionRarityChance {
    @SpirePostfixPatch
    public static void Postfix() {
        PotionHelper.POTION_COMMON_CHANCE = 30;    // 30%
        PotionHelper.POTION_UNCOMMON_CHANCE = 20;  // 20%
        // 나머지 50% = RARE
    }
}
```

### 3.4 엘리트 방 빈도 증가

엘리트 방 개수는 맵 생성 시 결정되므로, 맵 생성 로직을 수정해야 합니다.
(복잡하므로 여기서는 생략)

### 3.5 이벤트 우선 확률 조정

? 방에서 이벤트를 우선하도록 수정:
```java
@SpirePatch(
    clz = EventRoom.class,
    method = SpirePatch.CONSTRUCTOR
)
public static class IncreaseEventChance {
    // 이벤트 선택 로직 수정 필요
}
```

## 4. RNG 시드 제어

### 4.1 시드 고정으로 일관된 결과
```java
@SpirePatch(
    clz = AbstractDungeon.class,
    method = "generateSeeds"
)
public static class FixedSeed {
    @SpirePostfixPatch
    public static void Postfix() {
        // 고정된 시드로 변경
        AbstractDungeon.cardRng = new Random(12345L);
        AbstractDungeon.relicRng = new Random(67890L);
        // 디버깅 시 유용
    }
}
```

### 4.2 시드 저장 및 복원
```java
// 현재 RNG 상태 저장
Random savedCardRng = AbstractDungeon.cardRng.copy();

// 작업 수행
// ...

// RNG 상태 복원
AbstractDungeon.cardRng = savedCardRng;
```

## 5. Ascension별 확률 차이

### A13: 보스 골드 감소
```java
// AbstractRoom.java 라인 366-370
if (AbstractDungeon.ascensionLevel >= 13) {
    addGoldToRewards(MathUtils.round(tmp * 0.75F));  // 75% 감소
}
```

### A15: 보스 유물 스왑 확률
보스 유물 교환 이벤트 관련

### A18+: 엘리트 강화
엘리트 몬스터 체력 증가 등

## 6. 실용적인 수정 예시

### 예시 1: 모든 확률 2배
```java
@SpirePatch(
    clz = Random.class,
    method = "randomBoolean",
    paramtypez = {float.class}
)
public static class DoubleAllChances {
    @SpirePrefixPatch
    public static void Prefix(@ByRef float[] chance) {
        // 확률 2배 (최대 1.0)
        chance[0] = Math.min(chance[0] * 2.0f, 1.0f);
    }
}
```

### 예시 2: 보물 상자 확정 등장
```java
// 맵 생성 로직 수정 필요 (복잡)
// 대신 이벤트 방을 보물 방으로 교체하는 방법 사용
```

### 예시 3: 레어 카드만 등장
```java
@SpirePatch(
    clz = AbstractDungeon.class,
    method = "rollRarity",
    paramtypez = {Random.class}
)
public static class AlwaysRareCards {
    @SpirePostfixPatch
    public static AbstractCard.CardRarity Postfix(AbstractCard.CardRarity __result) {
        // 항상 레어 카드 반환
        return AbstractCard.CardRarity.RARE;
    }
}
```

### 예시 4: 포션 드롭 확률 층수에 비례
```java
@SpirePatch(
    clz = AbstractRoom.class,
    method = "addPotionToRewards",
    paramtypez = {}
)
public static class ScalingPotionChance {
    @SpireInsertPatch(
        locator = ChanceLocator.class
    )
    public static void Insert(@ByRef int[] chance) {
        // 층수당 +2% (최대 100%)
        int floorBonus = AbstractDungeon.floorNum * 2;
        chance[0] = Math.min(chance[0] + floorBonus, 100);
    }

    private static class ChanceLocator extends SpireInsertLocator {
        @Override
        public int[] Locate(CtBehavior ctMethodToPatch) throws Exception {
            Matcher finalMatcher = new Matcher.FieldAccessMatcher(
                AbstractDungeon.class, "potionRng"
            );
            return LineFinder.findInOrder(ctMethodToPatch, finalMatcher);
        }
    }
}
```

### 예시 5: 카드 업그레이드 확률 100%
```java
@SpirePatch(
    clz = AbstractDungeon.class,
    method = "getRewardCards"
)
public static class AlwaysUpgradedCards {
    @SpirePostfixPatch
    public static void Postfix(ArrayList<AbstractCard> __result) {
        // 모든 카드 업그레이드
        for (AbstractCard card : __result) {
            if (card.canUpgrade()) {
                card.upgrade();
            }
        }
    }
}
```

## 7. 관련 클래스

### 핵심 클래스
- **Random:** 난수 생성기
- **AbstractDungeon:** 모든 RNG 시드 관리
- **PotionHelper:** 포션 선택 및 레어리티
- **CardHelper:** 카드 선택 (제한적)
- **AbstractRoom:** 보상 확률 계산

### 주요 필드
- **AbstractDungeon.cardRng:** 카드 레어리티 결정
- **AbstractDungeon.potionRng:** 포션 드롭/선택
- **AbstractDungeon.relicRng:** 유물 선택
- **AbstractDungeon.treasureRng:** 골드 양
- **AbstractDungeon.monsterRng:** 몬스터 조우

## 8. 주의사항

### 8.1 확률 범위 준수
확률은 항상 0.0 ~ 1.0 범위를 유지해야 합니다:
```java
float chance = 0.4f;  // 40%
if (rng.randomBoolean(chance)) {
    // 성공
}
```

### 8.2 RNG 시드 관리
각 RNG는 독립적으로 관리됩니다. 하나를 수정해도 다른 RNG에는 영향이 없습니다:
```java
// cardRng만 수정
AbstractDungeon.cardRng = new Random(12345L);
// potionRng는 영향 없음
```

### 8.3 Counter 증가
RNG를 사용할 때마다 counter가 증가합니다:
```java
public int random(int range) {
    this.counter++;  // 항상 증가
    return this.random.nextInt(range + 1);
}
```

이는 시드 재현성을 위해 중요합니다.

### 8.4 Blizzard 시스템
카드와 포션 드롭은 Blizzard 시스템을 사용합니다:
- **실패 시:** 다음 확률 +10%
- **성공 시:** 확률 리셋 또는 -10%

이를 무시하고 확률을 고정하면 시스템이 깨집니다.

### 8.5 Daily Run 차이
Daily Run에서는 일부 확률이 고정값으로 변경됩니다:
```java
if (Settings.isDailyRun) {
    addGoldToRewards(15);  // 고정
} else {
    addGoldToRewards(treasureRng.random(10, 20));  // 랜덤
}
```

### 8.6 유물 효과 우선
일부 유물은 확률을 무시하고 결과를 강제합니다:
- **Question Card:** 카드 보상 +1
- **Busted Crown:** 카드 보상 없음
- **White Beast Statue:** 포션 100% 드롭

### 8.7 Save/Load 영향
RNG counter는 저장/로드 시 복원됩니다:
```java
// 저장
save.card_seed_count = AbstractDungeon.cardRng.counter;

// 로드
AbstractDungeon.cardRng = new Random(Settings.seed, save.card_seed_count);
```

### 8.8 멀티플레이어 비호환
RNG를 수정하면 Daily Run 리더보드나 멀티플레이어 모드에서 문제가 발생할 수 있습니다.

### 8.9 패치 충돌
여러 모드가 같은 확률을 수정하면 충돌할 수 있습니다. Prefix/Postfix 순서를 주의하세요.

## 9. 확률 디버깅

### 로그 출력
```java
logger.info("POTION CHANCE: " + chance);
logger.info("Card Rarity: " + rarity);
logger.info("RNG Counter: " + AbstractDungeon.cardRng.counter);
```

### 확률 추적
```java
@SpirePatch(
    clz = Random.class,
    method = "randomBoolean",
    paramtypez = {float.class}
)
public static class TrackProbability {
    @SpirePostfixPatch
    public static void Postfix(float chance, boolean __result) {
        logger.info("Probability: " + (chance * 100) + "%, Result: " + __result);
    }
}
```

## 10. 확률 공식 정리

### 포션 드롭
```
기본 확률 = 40%
Blizzard 보정 = ±10% (누적)
최종 확률 = 기본 확률 + Blizzard 보정
```

### 카드 레어리티
```
roll = random(0, 99)
roll += cardBlizzRandomizer

if roll < 3:
    RARE (3%)
elif roll < 37:
    UNCOMMON (34%)
else:
    COMMON (63%)
```

### 포션 레어리티
```
roll = random(0, 99)

if roll < 65:
    COMMON (65%)
elif roll < 90:
    UNCOMMON (25%)
else:
    RARE (10%)
```
