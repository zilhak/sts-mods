# Card Reward System - 카드 보상 시스템

카드 보상 확률, 발생 상황, 수정 방법에 대한 완전한 가이드입니다.

## 📊 기본 확률 구조

### 1. 희귀도별 기본 확률

```java
// AbstractRoom.java
public int baseRareCardChance = 3;        // 레어: 3%
public int baseUncommonCardChance = 37;   // 언커먼: 37%
// 커먼: 60% (나머지)
```

**확률 계산 방식**:
1. 0~99 사이 랜덤 숫자 생성 (`roll`)
2. `cardBlizzRandomizer` 값 추가 (희귀도 조정 시스템)
3. 희귀도 판정:
   - `roll < 3`: **레어** (3%)
   - `roll < 40`: **언커먼** (37%)
   - `roll >= 40`: **커먼** (60%)

### 2. 카드 블리자드 시스템 (Card Blizzard)

게임이 희귀카드 획득을 조절하는 내부 시스템입니다.

```java
// AbstractDungeon.java
public static int cardBlizzStartOffset = 5;     // 시작 보너스: +5%
public static int cardBlizzRandomizer = 5;      // 현재 보너스
public static int cardBlizzGrowth = 1;          // 감소량: -1%
public static int cardBlizzMaxOffset = -10;     // 최소값: -10%
```

**작동 방식**:
- **게임 시작**: `+5%` 보너스 (레어 확률 3% → 8%)
- **레어 카드 획득**: 보너스 리셋 → `+5%`
- **언커먼/커먼 획득**: 보너스 `-1%` (최소 -10%까지)

**예시**:
```
초기: 레어 8% (3% + 5%)
언커먼 획득 → 레어 7% (3% + 4%)
언커먼 획득 → 레어 6% (3% + 3%)
레어 획득 → 레어 8% (3% + 5%) 리셋
```

### 3. 방 타입별 확률

```java
// 일반 전투 (MonsterRoom)
public AbstractCard.CardRarity getCardRarity(int roll) {
    if (roll < rareCardChance) return RARE;       // 3% (+ 블리자드)
    if (roll < rare + uncommon) return UNCOMMON;   // 37%
    return COMMON;                                 // 60%
}

// 엘리트 전투 (MonsterRoomElite)
// 일반 전투와 동일

// 보스 전투 (MonsterRoomBoss)
public AbstractCard.CardRarity getCardRarity(int roll) {
    return AbstractCard.CardRarity.RARE;  // 항상 레어 (100%)
}
```

**정리**:
| 방 타입 | 레어 | 언커먼 | 커먼 |
|---------|------|--------|------|
| 일반 전투 | 3% (+블리자드) | 37% | 60% |
| 엘리트 전투 | 3% (+블리자드) | 37% | 60% |
| **보스 전투** | **100%** | - | - |

---

## 🎁 카드 보상이 발생하는 상황

### 1. 전투 후 보상 (Combat Rewards)

**발생 조건**:
- 일반 전투 클리어
- 엘리트 전투 클리어
- 보스 전투 클리어 (승천 12 미만)

**코드**:
```java
// RewardItem.java - 생성자
public RewardItem() {
    this.type = RewardType.CARD;
    this.isBoss = AbstractDungeon.getCurrRoom() instanceof MonsterRoomBoss;
    this.cards = AbstractDungeon.getRewardCards();  // 카드 3장 생성
    this.text = TEXT[2];  // "카드"
}
```

**특징**:
- 기본 3장 제공
- Singing Bowl이 있으면 Skip 가능 (최대 체력 +2)
- 보스전은 항상 레어 카드만 제공

### 2. 이벤트 보상

**카드 보상을 주는 주요 이벤트**:

#### 1막 (Exordium)
- **Neow (게임 시작)**: 특정 선택지에서 카드 획득
- **Wing Statue**: 레어 카드 선택
- **Big Fish**: 레어 카드 보상 (체력 회복 선택지)
- **Golden Idol**: 저주 대신 카드 획득 가능
- **Living Wall**: 카드 제거 또는 추가 선택

#### 2막 (The City)
- **The Library**: 카드 선택 (클래스 카드 20장 중 1장)
- **The Colosseum**: 전투 승리 시 카드 보상
- **Knowing Skull**: 카드 보상 선택지
- **Masked Bandits**: 전투 후 일반 보상
- **The Mausoleum**: 레어 카드 획득

#### 3막 (The Beyond)
- **Sensory Stone**: 클래스별 특정 카드 선택
- **Tomb of Lord Red Mask**: 전투 후 보상
- **Winding Halls**: 카드 선택 (Madness 효과)

**예시 코드 (Sensory Stone)**:
```java
// SensoryStone.java
AbstractDungeon.cardRewardScreen.open(this.memories, null, TEXT[1]);
```

### 3. 니오 보상 (Neow Rewards)

게임 시작 시 선택할 수 있는 보상입니다.

```java
// NeowReward.java
public enum NeowRewardType {
    THREE_CARDS,      // 카드 3장 중 1장 선택
    ONE_RANDOM_RARE_CARD,  // 랜덤 레어 카드 1장
    REMOVE_CARD,      // 카드 제거
    UPGRADE_CARD,     // 카드 업그레이드
    TRANSFORM_CARD,   // 카드 변환
    RANDOM_COLORLESS, // 무색 카드
    // ... 기타 보상
}
```

**카드 관련 니오 보상**:
| 보상 | 내용 | 조건 |
|------|------|------|
| THREE_CARDS | 카드 3장 중 1장 | 페널티 없음 |
| ONE_RANDOM_RARE_CARD | 랜덤 레어 1장 | 최대 체력 -7 |
| RANDOM_COLORLESS | 무색 카드 1장 | 다양 |
| REMOVE_CARD | 카드 제거 | 골드 손실 등 |
| UPGRADE_CARD | 카드 업그레이드 | - |
| TRANSFORM_CARD | 카드 변환 | - |

### 4. 카드 생성 액션 (Card Generation Actions)

전투 중 특정 카드/유물이 카드 선택 화면을 띄웁니다.

#### Discovery 계열
```java
// DiscoveryAction.java
public class DiscoveryAction extends AbstractGameAction {
    // 클래스 카드 3장 중 1장 선택
    // 카드들은 임시로 덱에 추가됨
}
```

**사용 카드**:
- **Discovery** (Ironclad): 클래스 카드 3장 중 1장
- **Transmutation** (Watcher): 무색 카드 3장 중 1장
- **Foreign Influence** (Watcher): 다른 클래스 카드 3장 중 1장

#### Colorless 선택
```java
// ChooseOneColorless.java
// 무색 카드 3장 중 1장 선택
```

**사용 카드**:
- **Prismatic Shard** (유물): 모든 클래스 카드 획득 가능

#### Codex
```java
// CodexAction.java
// 스킬 카드 3장 중 1장 선택 (임시)
```

**사용 카드**:
- **Nilry's Codex** (유물)

### 5. 휴식 장소 (Rest Site)

직접적인 카드 보상은 없지만, 카드 관련 선택지가 있습니다:
- **카드 업그레이드** (기본 선택지)
- **휴식** (체력 회복)
- **유물 이벤트** (특정 유물 보유 시)

---

## 🔢 카드 보상 개수

### 기본 개수

```java
// AbstractDungeon.java - getRewardCards()
int numCards = 3;  // 기본 3장

for (AbstractRelic r : player.relics) {
    numCards = r.changeNumberOfCardsInReward(numCards);
}

if (ModHelper.isModEnabled("Binary")) {
    numCards--;  // Binary 모드: -1장
}
```

### 카드 개수를 변경하는 유물

| 유물 | 효과 | 코드 위치 |
|------|------|----------|
| **Question Card** | +1장 (4장) | `QuestionCard.java` |
| **Busted Crown** | -1장 (2장) | `BustedCrown.java` |
| **Prayer Wheel** | +1장 (4장) | `PrayerWheel.java` |
| **Empty Cage** (이벤트) | +2장 (5장) | 일회성 |

**예시**:
- 기본: 3장
- Question Card 보유: 4장
- Question Card + Prayer Wheel: 5장
- Busted Crown 보유: 2장 (대신 에너지 +1)

---

## 🎯 확률 수정 방법

### 1. 기본 확률 변경

```java
@SpirePatch(
    cls = "com.megacrit.cardcrawl.rooms.AbstractRoom",
    method = SpirePatch.CONSTRUCTOR
)
public static class BaseRarityPatch {
    @SpirePostfixPatch
    public static void Postfix(AbstractRoom __instance) {
        if (AbstractDungeon.ascensionLevel >= 74) {
            // 승천 74: 레어 확률 감소
            __instance.baseRareCardChance = 1;        // 3% → 1%
            __instance.baseUncommonCardChance = 29;   // 37% → 29%
            // 커먼: 60% → 70%
        }
    }
}
```

### 2. 블리자드 시스템 조정

```java
@SpirePatch(
    cls = "com.megacrit.cardcrawl.dungeons.AbstractDungeon",
    method = SpirePatch.STATICINIT
)
public static class BlizzardPatch {
    @SpirePostfixPatch
    public static void Postfix() {
        if (AbstractDungeon.ascensionLevel >= 80) {
            // 블리자드 보너스 감소
            AbstractDungeon.cardBlizzStartOffset = 2;  // +5% → +2%
            AbstractDungeon.cardBlizzGrowth = 2;       // -1% → -2% (빠르게 감소)
        }
    }
}
```

### 3. 방 타입별 확률 오버라이드

```java
@SpirePatch(
    cls = "com.megacrit.cardcrawl.rooms.MonsterRoomElite",
    method = "getCardRarity"
)
public static class EliteRarityPatch {
    @SpirePrefixPatch
    public static SpireReturn<AbstractCard.CardRarity> Prefix(
        MonsterRoomElite __instance,
        int roll
    ) {
        if (AbstractDungeon.ascensionLevel >= 50) {
            // 엘리트도 보스처럼 항상 레어
            return SpireReturn.Return(AbstractCard.CardRarity.RARE);
        }
        return SpireReturn.Continue();
    }
}
```

### 4. 카드 개수 변경

```java
@SpirePatch(
    cls = "com.megacrit.cardcrawl.dungeons.AbstractDungeon",
    method = "getRewardCards"
)
public static class CardCountPatch {
    @SpireInsertPatch(loc = 1797)  // numCards 초기화 직후
    public static void Insert() {
        if (AbstractDungeon.ascensionLevel >= 90) {
            // 승천 90: 카드 선택지 1장 감소
            numCards--;  // 3장 → 2장 (유물 효과 적용 전)
        }
    }
}
```

### 5. 보스 보상 제거 (바닐라 승천 12)

```java
@SpirePatch(
    cls = "com.megacrit.cardcrawl.rooms.MonsterRoomBoss",
    method = "dropReward"
)
public static class BossRewardPatch {
    @SpirePostfixPatch
    public static void Postfix(MonsterRoomBoss __instance) {
        if (AbstractDungeon.ascensionLevel >= 12) {
            // 승천 12+: 카드 보상 제거
            Iterator<RewardItem> iterator = AbstractDungeon.getCurrRoom().rewards.iterator();
            while (iterator.hasNext()) {
                RewardItem item = iterator.next();
                if (item.type == RewardItem.RewardType.CARD) {
                    iterator.remove();
                }
            }
        }
    }
}
```

---

## 📝 실전 예제

### 예제 1: 승천 레벨별 차등 적용

```java
@SpirePatch(
    cls = "com.megacrit.cardcrawl.rooms.AbstractRoom",
    method = "getCardRarity",
    paramtypez = { int.class }
)
public static class AscensionRarityPatch {
    @SpirePrefixPatch
    public static void Prefix(AbstractRoom __instance) {
        int level = AbstractDungeon.ascensionLevel;

        if (level >= 21 && level < 30) {
            // 레어 확률 소폭 감소
            __instance.baseRareCardChance = 2;  // 3% → 2%
        } else if (level >= 30 && level < 50) {
            // 레어 확률 중간 감소
            __instance.baseRareCardChance = 1;  // 3% → 1%
        } else if (level >= 50) {
            // 레어 확률 대폭 감소
            __instance.baseRareCardChance = 0;  // 레어 불가능
            __instance.baseUncommonCardChance = 30;  // 언커먼도 감소
        }
    }
}
```

### 예제 2: 엘리트 전투 레어 확률 증가

```java
@SpirePatch(
    cls = "com.megacrit.cardcrawl.rooms.MonsterRoomElite",
    method = "getCardRarity"
)
public static class EliteRareBoostPatch {
    @SpirePostfixPatch
    public static AbstractCard.CardRarity Postfix(
        AbstractCard.CardRarity result,
        MonsterRoomElite __instance,
        int roll
    ) {
        if (AbstractDungeon.ascensionLevel >= 25) {
            // 승천 25+: 엘리트 레어 확률 10%로 증가
            if (roll < 10) {
                return AbstractCard.CardRarity.RARE;
            }
        }
        return result;
    }
}
```

### 예제 3: Act별 다른 확률

```java
@SpirePatch(
    cls = "com.megacrit.cardcrawl.rooms.AbstractRoom",
    method = "getCardRarity",
    paramtypez = { int.class, boolean.class }
)
public static class ActBasedRarityPatch {
    @SpirePrefixPatch
    public static void Prefix(AbstractRoom __instance) {
        int actNum = AbstractDungeon.actNum;

        switch (actNum) {
            case 1:
                // 1막: 레어 확률 증가 (초반 강화)
                __instance.baseRareCardChance = 5;  // 3% → 5%
                break;
            case 2:
                // 2막: 기본 확률 유지
                break;
            case 3:
                // 3막: 레어 확률 감소 (난이도 상승)
                __instance.baseRareCardChance = 2;  // 3% → 2%
                break;
        }
    }
}
```

---

## 🔍 디버깅 팁

### 1. 현재 확률 로깅

```java
@SpirePatch(
    cls = "com.megacrit.cardcrawl.dungeons.AbstractDungeon",
    method = "rollRarity",
    paramtypez = { Random.class }
)
public static class RarityLogPatch {
    @SpirePostfixPatch
    public static void Postfix(AbstractCard.CardRarity result) {
        int blizz = AbstractDungeon.cardBlizzRandomizer;
        logger.info("Card Rarity: " + result +
                    " (Blizzard: +" + blizz + "%)");
    }
}
```

### 2. 보상 생성 추적

```java
@SpirePatch(
    cls = "com.megacrit.cardcrawl.rewards.RewardItem",
    method = SpirePatch.CONSTRUCTOR
)
public static class RewardLogPatch {
    @SpirePostfixPatch
    public static void Postfix(RewardItem __instance) {
        if (__instance.type == RewardItem.RewardType.CARD) {
            logger.info("Card Reward Created: " + __instance.cards.size() + " cards");
            for (AbstractCard c : __instance.cards) {
                logger.info("  - " + c.name + " (" + c.rarity + ")");
            }
        }
    }
}
```

---

## ⚠️ 주의사항

### 1. 확률은 0-100 범위로 제한

```java
// 잘못된 예
__instance.baseRareCardChance = 150;  // ❌ 100 초과 불가능

// 올바른 예
__instance.baseRareCardChance = Math.min(100, 50);  // ✅
```

### 2. 블리자드 시스템은 전역 변수

```java
// ❌ 잘못된 수정 (매번 리셋됨)
@SpirePatch(cls = "...", method = "getRewardCards")
public static void Postfix() {
    AbstractDungeon.cardBlizzRandomizer = 10;  // 효과 없음
}

// ✅ 올바른 수정 (초기화 시점에 변경)
@SpirePatch(cls = "...", method = SpirePatch.STATICINIT)
public static void Postfix() {
    AbstractDungeon.cardBlizzStartOffset = 10;  // 시작값 변경
}
```

### 3. null 체크 필수

```java
@SpirePostfixPatch
public static void Postfix(AbstractRoom __instance) {
    // ❌ 게임 시작 시 currMapNode가 null일 수 있음
    if (AbstractDungeon.getCurrRoom() instanceof MonsterRoomBoss) {
        // ...
    }

    // ✅ null 체크 추가
    if (AbstractDungeon.currMapNode != null &&
        AbstractDungeon.getCurrRoom() instanceof MonsterRoomBoss) {
        // ...
    }
}
```

---

## 📚 관련 클래스 참조

| 클래스 | 경로 | 역할 |
|--------|------|------|
| **AbstractDungeon** | `com.megacrit.cardcrawl.dungeons` | 전역 카드 생성 |
| **AbstractRoom** | `com.megacrit.cardcrawl.rooms` | 방별 확률 설정 |
| **MonsterRoomBoss** | `com.megacrit.cardcrawl.rooms` | 보스 보상 (레어 100%) |
| **MonsterRoomElite** | `com.megacrit.cardcrawl.rooms` | 엘리트 보상 |
| **RewardItem** | `com.megacrit.cardcrawl.rewards` | 보상 아이템 |
| **CardRewardScreen** | `com.megacrit.cardcrawl.screens` | 카드 선택 화면 |
| **NeowReward** | `com.megacrit.cardcrawl.neow` | 니오 보상 |

---

## 🎓 추가 학습

1. **유물 효과**: `AbstractRelic.changeRareCardRewardChance()` 메서드
2. **이벤트 카드**: `AbstractEvent` 클래스의 이벤트별 구현
3. **카드 풀 관리**: `CardLibrary.getCard()` 메서드
4. **중복 방지**: `getRewardCards()`의 중복 체크 로직

---

**작성일**: 2025-11-15
**기반 소스**: E:\workspace\sts-decompile\
**검증**: 디컴파일 소스 직접 분석
**게임 버전**: 01-23-2019
