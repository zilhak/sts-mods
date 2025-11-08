# 보상 (Rewards) 수정 가이드

## 1. 보상 시스템 개요

### RewardItem 클래스
**위치:** `com.megacrit.cardcrawl.rewards.RewardItem`

전투 승리 후 획득하는 모든 보상을 관리하는 핵심 클래스입니다.

### 보상 타입 (RewardType)
```java
public enum RewardType {
    CARD,           // 카드 선택 보상
    GOLD,           // 골드
    RELIC,          // 유물
    POTION,         // 포션
    STOLEN_GOLD,    // 도난당한 골드 (특정 이벤트)
    EMERALD_KEY,    // 에메랄드 키
    SAPPHIRE_KEY    // 사파이어 키
}
```

## 2. 보상이 생성되는 시점

### 2.1 전투 승리 후 (AbstractRoom.java)

#### 일반 전투 (MonsterRoom)
**골드:** 10-20 골드 (랜덤, Daily Run은 15 고정)
```java
// AbstractRoom.java 라인 411-415
if (Settings.isDailyRun) {
    addGoldToRewards(15);
} else {
    addGoldToRewards(AbstractDungeon.treasureRng.random(10, 20));
}
```

**카드:** 기본 3장
```java
// AbstractDungeon.java 라인 1794
int numCards = 3;
```

**포션:** 40% 확률
```java
// AbstractRoom.java 라인 767-772
chance = 40;
chance += blizzardPotionMod;
```

#### 엘리트 전투 (MonsterRoomElite)
**골드:** 25-35 골드 (랜덤, Daily Run은 30 고정)
```java
// AbstractRoom.java 라인 400-404
if (Settings.isDailyRun) {
    addGoldToRewards(30);
} else {
    addGoldToRewards(AbstractDungeon.treasureRng.random(25, 35));
}
```

**유물:** 확정 1개
```java
// MonsterRoomElite.java 라인 98
addRelicToRewards(tier);
```

**Black Star 유물 소지 시:** 유물 2개
```java
// MonsterRoomElite.java 라인 99-101
if (AbstractDungeon.player.hasRelic("Black Star")) {
    addNoncampRelicToRewards(returnRandomRelicTier());
}
```

**포션:** 40% 확률

#### 보스 전투 (MonsterRoomBoss)
**골드:** 100 ± 5 골드
- Ascension 13+: 75% 감소 (75골드)
```java
// AbstractRoom.java 라인 359-370
if (Settings.isDailyRun) {
    addGoldToRewards(100);
} else {
    int tmp = 100 + AbstractDungeon.miscRng.random(-5, 5);
    if (AbstractDungeon.ascensionLevel >= 13) {
        addGoldToRewards(MathUtils.round(tmp * 0.75F));
    } else {
        addGoldToRewards(tmp);
    }
}
```

**보스 유물:** 3개 중 1개 선택

### 2.2 이벤트
이벤트마다 다르며, `addPotionToRewards()` 호출 시 40% 확률로 포션 획득

### 2.3 보물 상자
던전 맵에서 자동 생성되며, 보상은 상자 타입별로 다름

### 2.4 상점
골드로 직접 구매

## 3. 보상 구성 수정

### 3.1 보상 개수 증가

**예시 1: 카드 보상 3장 → 5장**
```java
@SpirePatch(
    clz = AbstractDungeon.class,
    method = "getRewardCards"
)
public static class IncreaseCardRewards {
    @SpirePrefixPatch
    public static void Prefix() {
        // numCards는 로컬 변수이므로 직접 수정 불가
        // 대신 유물의 changeNumberOfCardsInReward를 활용
    }

    // 또는 Postfix로 카드 추가
    @SpirePostfixPatch
    public static void Postfix(ArrayList<AbstractCard> __result) {
        // 카드 2장 더 추가
        for (int i = 0; i < 2; i++) {
            AbstractCard.CardRarity rarity = AbstractDungeon.rollRarity();
            AbstractCard card = AbstractDungeon.getCard(rarity);
            __result.add(card.makeCopy());
        }
    }
}
```

**예시 2: 엘리트 유물 1개 → 2개**
```java
@SpirePatch(
    clz = MonsterRoomElite.class,
    method = "dropReward"
)
public static class DoubleEliteRelics {
    @SpirePostfixPatch
    public static void Postfix(MonsterRoomElite __instance) {
        // 유물 1개 더 추가
        AbstractRelic.RelicTier tier = AbstractDungeon.returnRandomRelicTier();
        __instance.addRelicToRewards(tier);
    }
}
```

### 3.2 보상 타입 추가

**예시: 일반 전투에서도 유물 획득 가능 (10% 확률)**
```java
@SpirePatch(
    clz = MonsterRoom.class,
    method = "dropReward"
)
public static class AddRelicToNormalCombat {
    @SpirePostfixPatch
    public static void Postfix(MonsterRoom __instance) {
        // 10% 확률로 유물 추가
        if (AbstractDungeon.miscRng.randomBoolean(0.1f)) {
            AbstractRelic.RelicTier tier;
            int roll = AbstractDungeon.relicRng.random(0, 99);
            if (roll < 50) {
                tier = AbstractRelic.RelicTier.COMMON;
            } else if (roll < 83) {
                tier = AbstractRelic.RelicTier.UNCOMMON;
            } else {
                tier = AbstractRelic.RelicTier.RARE;
            }
            __instance.addRelicToRewards(tier);
        }
    }
}
```

### 3.3 보상 품질 향상

**예시: 레어 카드 출현 확률 증가**
```java
@SpirePatch(
    clz = AbstractRoom.class,
    method = "getCardRarity",
    paramtypez = {int.class, boolean.class}
)
public static class IncreaseRareCardChance {
    @SpirePostfixPatch
    public static AbstractCard.CardRarity Postfix(AbstractCard.CardRarity __result, int roll) {
        // 레어 카드 확률 3배 증가 (3% → 9%)
        // roll < 9면 RARE로 변경
        if (roll < 9 && __result == AbstractCard.CardRarity.COMMON) {
            return AbstractCard.CardRarity.RARE;
        }
        return __result;
    }
}
```

### 3.4 특정 조건 시 보너스 보상

**예시: 완벽한 전투 시 추가 골드 (피해 받지 않음)**
```java
@SpirePatch(
    clz = AbstractRoom.class,
    method = "endBattle"
)
public static class PerfectBattleBonus {
    @SpirePostfixPatch
    public static void Postfix(AbstractRoom __instance) {
        // 체력이 전투 시작 시와 동일하면 보너스 골드
        if (AbstractDungeon.player.currentHealth == AbstractDungeon.player.maxHealth) {
            __instance.addGoldToRewards(50);
        }
    }
}
```

## 4. 전투별 보상 차별화

### 4.1 일반 전투 보상
- **골드 범위:** 10-20
- **카드 개수:** 3장
- **포션 확률:** 40%
- **유물:** 없음 (기본)

### 4.2 엘리트 전투 보상
- **골드 범위:** 25-35
- **카드 개수:** 3장
- **포션 확률:** 40%
- **유물:** 확정 1개 (Black Star 소지 시 2개)

### 4.3 보스 전투 보상
- **골드:** 100 ± 5 (A13+는 75% = 75골드)
- **카드:** 없음
- **포션:** 40%
- **보스 유물:** 3개 중 1개 선택

## 5. 실용적인 수정 예시

### 예시 1: 모든 전투에서 포션 확정 드롭
```java
@SpirePatch(
    clz = AbstractRoom.class,
    method = "addPotionToRewards",
    paramtypez = {}
)
public static class GuaranteedPotionDrop {
    @SpirePrefixPatch
    public static SpireReturn<Void> Prefix(AbstractRoom __instance) {
        // 원본 메서드 실행 안 함
        if (__instance.rewards.size() < 4) {
            __instance.rewards.add(new RewardItem(AbstractDungeon.returnRandomPotion()));
        }
        return SpireReturn.Return(null);
    }
}
```

### 예시 2: 엘리트 유물 2개로 증가
```java
@SpirePatch(
    clz = MonsterRoomElite.class,
    method = "dropReward"
)
public static class DoubleEliteRelics {
    @SpireInsertPatch(
        locator = RelicAddLocator.class
    )
    public static void Insert(MonsterRoomElite __instance) {
        // 유물 추가 직후 1개 더 추가
        AbstractRelic.RelicTier tier = __instance.returnRandomRelicTier();
        __instance.addRelicToRewards(tier);
    }

    private static class RelicAddLocator extends SpireInsertLocator {
        @Override
        public int[] Locate(CtBehavior ctMethodToPatch) throws Exception {
            Matcher finalMatcher = new Matcher.MethodCallMatcher(
                AbstractRoom.class, "addRelicToRewards"
            );
            return LineFinder.findInOrder(ctMethodToPatch, finalMatcher);
        }
    }
}
```

### 예시 3: 보스 전투 후 카드 보상 추가
```java
@SpirePatch(
    clz = MonsterRoomBoss.class,
    method = "dropReward"
)
public static class BossCardReward {
    @SpirePostfixPatch
    public static void Postfix(MonsterRoomBoss __instance) {
        // 보스 전투 후 레어 카드 보상 추가
        __instance.addCardToRewards();
    }
}
```

### 예시 4: Ascension 난이도별 보상 감소
```java
@SpirePatch(
    clz = AbstractRoom.class,
    method = "addGoldToRewards",
    paramtypez = {int.class}
)
public static class AscensionGoldReduction {
    @SpirePrefixPatch
    public static void Prefix(int[] gold) {
        // A20에서 골드 50% 감소
        if (AbstractDungeon.ascensionLevel >= 20) {
            gold[0] = MathUtils.round(gold[0] * 0.5f);
        }
    }
}
```

## 6. 관련 클래스

### 핵심 클래스
- **RewardItem:** 개별 보상 아이템
- **AbstractRoom:** 방 추상 클래스 (보상 생성 로직)
- **MonsterRoom:** 일반 전투 방
- **MonsterRoomElite:** 엘리트 전투 방
- **MonsterRoomBoss:** 보스 전투 방
- **CombatRewardScreen:** 보상 선택 화면

### 주요 메서드
- `addGoldToRewards(int gold)` - 골드 보상 추가
- `addRelicToRewards(RelicTier tier)` - 유물 보상 추가
- `addPotionToRewards()` - 포션 보상 추가 (확률적)
- `addCardToRewards()` - 카드 보상 추가
- `dropReward()` - 전투 종료 시 보상 드롭

## 7. 보상 생성 흐름도

```
전투 승리
    ↓
AbstractRoom.endBattle()
    ↓
각 방 타입의 dropReward() 호출
    ↓
MonsterRoom.dropReward()        → 골드(10-20) + 카드(3장) + 포션(40%)
MonsterRoomElite.dropReward()   → 골드(25-35) + 유물(1개) + 카드(3장) + 포션(40%)
MonsterRoomBoss.dropReward()    → 골드(100±5) + 보스유물(3→1)
    ↓
AbstractRoom.addPotionToRewards()
    ↓
CombatRewardScreen.open()
    ↓
플레이어가 보상 선택
    ↓
RewardItem.claimReward()
```

## 8. 주의사항

### 8.1 보상 개수 제한
포션 드롭 로직에서 보상이 4개 이상이면 포션을 추가하지 않습니다:
```java
// AbstractRoom.java 라인 785-787
if (this.rewards.size() >= 4) {
    chance = 0;
}
```

### 8.2 유물 효과 고려
일부 유물은 보상에 영향을 줍니다:
- **Question Card:** 카드 보상 개수 +1
- **Busted Crown:** 카드 보상 없음
- **Prayer Wheel:** 카드 보상 개수 +1
- **Black Star:** 엘리트 유물 2개
- **Golden Idol:** 골드 25% 추가
- **White Beast Statue:** 포션 확정 드롭

### 8.3 Ascension 효과
- **A13:** 보스 골드 75% 감소
- **A14+:** 엘리트 유물 품질 감소 가능성

### 8.4 Daily Run 차이
Daily Run에서는 골드가 고정값입니다 (랜덤 없음)

### 8.5 보상 중복 방지
카드 보상은 자동으로 중복을 방지합니다:
```java
// AbstractDungeon.java 라인 1837-1841
for (AbstractCard c : retVal) {
    if (c.cardID.equals(card.cardID)) {
        containsDupe = true;
    }
}
```

### 8.6 Blizzard 포션 시스템
포션 드롭 실패 시 다음 전투에서 확률이 10%씩 증가:
```java
// AbstractRoom.java 라인 792-798
if (AbstractDungeon.potionRng.random(0, 99) < chance) {
    // 포션 드롭
    blizzardPotionMod -= 10;
} else {
    blizzardPotionMod += 10;
}
```
