# Relic & Potion System - 유물 및 포션 시스템

유물과 포션의 획득 확률, 발생 상황, 수정 방법에 대한 완전한 가이드입니다.

---

# 📿 유물 시스템 (Relic System)

## 🎯 유물 등급 (Relic Tiers)

```java
// AbstractRelic.java
public enum RelicTier {
    DEPRECATED,  // 사용되지 않음
    STARTER,     // 시작 유물 (클래스별 고유)
    COMMON,      // 일반 유물
    UNCOMMON,    // 고급 유물
    RARE,        // 희귀 유물
    SPECIAL,     // 특수 유물 (이벤트 전용)
    BOSS,        // 보스 유물
    SHOP         // 상점 전용 유물
}
```

**등급별 특징**:
| 등급 | 획득 방법 | 개수 | 특징 |
|------|----------|------|------|
| **STARTER** | 캐릭터 선택 | 클래스당 1개 | 게임 시작 시 보유 |
| **COMMON** | 엘리트, 상점 | 약 30개 | 가장 흔함 (50%) |
| **UNCOMMON** | 엘리트, 상점 | 약 25개 | 중간 확률 (33%) |
| **RARE** | 엘리트, 상점 | 약 20개 | 낮은 확률 (17%) |
| **BOSS** | 보스 보상 | 약 15개 | 보스 클리어 시만 |
| **SHOP** | 상점 구매 | 약 10개 | 상점에서만 구매 가능 |
| **SPECIAL** | 특정 이벤트 | 약 5개 | 이벤트 전용 (예: Neow's Lament) |

---

## 🎲 엘리트 전투 유물 확률

### 기본 확률 구조

```java
// MonsterRoomElite.java - returnRandomRelicTier()
int roll = AbstractDungeon.relicRng.random(0, 99);

if (roll < 50) {
    return AbstractRelic.RelicTier.COMMON;      // 50%
}
if (roll > 82) {
    return AbstractRelic.RelicTier.RARE;        // 17%
}
return AbstractRelic.RelicTier.UNCOMMON;        // 33%
```

**엘리트 유물 확률 정리**:
- **일반 (Common)**: 50% (roll: 0-49)
- **고급 (Uncommon)**: 33% (roll: 50-82)
- **희귀 (Rare)**: 17% (roll: 83-99)

### Elite Swarm 모드 영향

```java
if (ModHelper.isModEnabled("Elite Swarm")) {
    roll += 10;  // +10% 보너스
}
```

**Elite Swarm 모드 시 확률**:
- **일반**: 40% (roll: 0-49에서 10씩 증가)
- **고급**: 43%
- **희귀**: 17%

---

## 🎁 유물 획득 상황

### 1. 엘리트 전투 (Elite Battles)

**기본 보상**:
```java
// MonsterRoomElite.java - dropReward()
AbstractRelic.RelicTier tier = returnRandomRelicTier();  // 50/33/17 확률
addRelicToRewards(tier);

// Black Star 유물 보유 시
if (AbstractDungeon.player.hasRelic("Black Star")) {
    addNoncampRelicToRewards(returnRandomRelicTier());  // 유물 1개 추가
}
```

**특징**:
- 항상 유물 1개 보장
- **Black Star** 보유 시 유물 2개
- 캠프파이어 유물 제외 (Peace Pipe, Shovel, Girya)

### 2. 보스 전투 (Boss Battles)

**보스 유물 선택**:
```java
// 보스 클리어 시 3개 중 1개 선택
// 보스 유물 풀에서 순서대로 제공
```

**보스 유물 풀 관리**:
- 게임 시작 시 보스 유물 풀 섞기
- 순서대로 제공 (FIFO)
- 풀이 비면 "Red Circlet" 제공

### 3. 상점 (Shop)

**상점 유물**:
```java
// ShopScreen.java
// 기본 2-3개의 유물 판매
// SHOP, COMMON, UNCOMMON, RARE 등급 랜덤
```

**가격**:
- **일반**: 150골드
- **고급**: 250골드
- **희귀**: 300골드
- **상점 전용**: 150골드

### 4. 보물 방 (Treasure Rooms)

**일반 보물 방**:
- 보물 상자에서 유물 1개
- 보통 COMMON/UNCOMMON 등급

**보스 보물 방** (Boss Treasure):
- 보스 클리어 후 등장
- 레어 유물 확률 높음

### 5. 이벤트 (Events)

#### 1막 (Exordium)
- **Neow (게임 시작)**: 랜덤 유물, 보스 유물 교환 등
- **Golden Shrine**: 골드로 유물 구매 (275골드)
- **Scrap Ooze**: 유물 1개 선택
- **Wing Statue**: 유물 제거 또는 획득

#### 2막 (The City)
- **The Mausoleum**: 유물 1개 (대신 저주)
- **Vampires**: 최대체력 -5, 유물 1개
- **Knowing Skull**: 유물 선택지
- **We Meet Again**: 보스 유물 3개 중 1개 (체력 손실)

#### 3막 (The Beyond)
- **Falling**: 유물 선택
- **Mind Bloom**: 보스 유물 획득 (Act 4 스킵)
- **Secret Portal**: 보스 유물

### 6. 니오 보상 (Neow Rewards)

```java
// NeowReward.java
RANDOM_COMMON_RELIC,     // 랜덤 일반 유물
RANDOM_RARE_RELIC,       // 랜덤 희귀 유물 (최대체력 -7)
BOSS_RELIC,              // 랜덤 보스 유물
REMOVE_CARD_AND_RELIC,   // 카드 제거 + 유물
SWAP_BOSS_RELIC,         // 보스 유물 교환
```

---

## 🔧 유물 수정 방법

### 1. 엘리트 유물 확률 변경

```java
@SpirePatch(
    cls = "com.megacrit.cardcrawl.rooms.MonsterRoomElite",
    method = "returnRandomRelicTier"
)
public static class EliteRelicTierPatch {
    @SpirePrefixPatch
    public static SpireReturn<AbstractRelic.RelicTier> Prefix(MonsterRoomElite __instance) {
        if (AbstractDungeon.ascensionLevel >= 50) {
            int roll = AbstractDungeon.relicRng.random(0, 99);

            // 승천 50+: 레어 확률 감소, 커먼 확률 증가
            if (roll < 60) {
                return SpireReturn.Return(AbstractRelic.RelicTier.COMMON);     // 60%
            }
            if (roll > 90) {
                return SpireReturn.Return(AbstractRelic.RelicTier.RARE);       // 10%
            }
            return SpireReturn.Return(AbstractRelic.RelicTier.UNCOMMON);       // 30%
        }
        return SpireReturn.Continue();
    }
}
```

### 2. 엘리트 유물 개수 변경

```java
@SpirePatch(
    cls = "com.megacrit.cardcrawl.rooms.MonsterRoomElite",
    method = "dropReward"
)
public static class EliteRelicCountPatch {
    @SpirePostfixPatch
    public static void Postfix(MonsterRoomElite __instance) {
        if (AbstractDungeon.ascensionLevel >= 60) {
            // 승천 60+: 엘리트 유물 1개 제거
            __instance.removeOneRelicFromRewards();
        }
    }
}
```

### 3. 보스 유물 풀 수정

```java
@SpirePatch(
    cls = "com.megacrit.cardcrawl.dungeons.AbstractDungeon",
    method = "initializeBossRelicPool"
)
public static class BossRelicPoolPatch {
    @SpirePostfixPatch
    public static void Postfix() {
        if (AbstractDungeon.ascensionLevel >= 70) {
            // 특정 보스 유물 제거 (약한 유물 제거)
            AbstractDungeon.bossRelicPool.remove("Busted Crown");
            AbstractDungeon.bossRelicPool.remove("Runic Dome");
        }
    }
}
```

### 4. 유물 획득 차단

```java
@SpirePatch(
    cls = "com.megacrit.cardcrawl.rooms.AbstractRoom",
    method = "addRelicToRewards"
)
public static class BlockRelicRewardPatch {
    @SpirePrefixPatch
    public static SpireReturn Prefix(AbstractRoom __instance, AbstractRelic.RelicTier tier) {
        if (AbstractDungeon.ascensionLevel >= 80) {
            // 승천 80+: 엘리트 전투에서 유물 보상 없음
            if (__instance instanceof MonsterRoomElite) {
                return SpireReturn.Return(null);
            }
        }
        return SpireReturn.Continue();
    }
}
```

### 5. 특정 유물 강제 제공

```java
@SpirePatch(
    cls = "com.megacrit.cardcrawl.rooms.MonsterRoomElite",
    method = "dropReward"
)
public static class ForceRelicPatch {
    @SpirePostfixPatch
    public static void Postfix(MonsterRoomElite __instance) {
        if (AbstractDungeon.floorNum == 10) {
            // 10층에서 특정 유물 강제 추가
            AbstractRelic relic = new Girya();
            __instance.addRelicToRewards(relic);
        }
    }
}
```

---

# 🧪 포션 시스템 (Potion System)

## 🎯 포션 등급 (Potion Rarities)

```java
// AbstractPotion.java
public enum PotionRarity {
    PLACEHOLDER,  // 빈 슬롯
    COMMON,       // 일반 포션
    UNCOMMON,     // 고급 포션
    RARE          // 희귀 포션
}

public enum PotionSize {
    ANVIL,    // 특대 (Entropic Brew)
    SPHERE,   // 구형
    BOTTLE,   // 병형
    MOON,     // 초승달형
    SPIKY,    // 뾰족한 형태
    CARD,     // 카드형 (Fairy in a Bottle)
    JAR,      // 항아리형
    SNECKO,   // 스네코형
    TINY,     // 소형
    EYE,      // 눈알형
    BOLT,     // 번개형
    GHOST,    // 유령형
    HEART,    // 하트형
    M         // M자형 (Smoke Bomb)
}
```

---

## 🎲 포션 획득 확률

### 기본 확률 구조

```java
// PotionHelper.java
public static int POTION_COMMON_CHANCE = 65;    // 65%
public static int POTION_UNCOMMON_CHANCE = 25;  // 25%
// RARE: 10% (나머지)
```

**포션 등급 확률**:
- **일반 (Common)**: 65%
- **고급 (Uncommon)**: 25%
- **희귀 (Rare)**: 10%

### 포션 드롭 확률 (Potion Drop Rate)

```java
// AbstractRoom.java - addPotionToRewards()
int chance = 0;

if (this instanceof MonsterRoomElite) {
    chance = 40;                        // 엘리트: 40%
    chance += blizzardPotionMod;        // 블리자드 보정
} else if (this instanceof MonsterRoom) {
    if (!monsters.haveMonstersEscaped()) {
        chance = 40;                    // 일반 전투: 40%
        chance += blizzardPotionMod;
    }
} else if (this instanceof EventRoom) {
    chance = 40;                        // 이벤트: 40%
    chance += blizzardPotionMod;
}

// White Beast Statue 유물 보유 시
if (AbstractDungeon.player.hasRelic("White Beast Statue")) {
    chance = 100;                       // 100% 포션 드롭
}

// 보상이 4개 이상이면 포션 드롭 차단
if (rewards.size() >= 4) {
    chance = 0;
}
```

**포션 드롭 확률 정리**:
| 상황 | 기본 확률 | 블리자드 보정 | 최종 확률 |
|------|-----------|---------------|----------|
| 일반 전투 | 40% | ±10% | 30-50% |
| 엘리트 전투 | 40% | ±10% | 30-50% |
| 이벤트 | 40% | ±10% | 30-50% |
| **White Beast Statue** | 100% | - | 100% |

---

## 📊 포션 블리자드 시스템 (Potion Blizzard)

카드 블리자드와 유사하게, 포션 드롭을 조절하는 시스템입니다.

```java
// AbstractRoom.java
public static int blizzardPotionMod = 0;  // 초기값: 0
private static final int BLIZZARD_POTION_MOD_AMT = 10;
```

**작동 방식**:
```java
if (potionDropped) {
    blizzardPotionMod -= 10;  // 포션 드롭 시 -10%
} else {
    blizzardPotionMod += 10;  // 포션 미드롭 시 +10%
}
```

**예시**:
```
초기: 40% + 0% = 40%
미드롭 → 40% + 10% = 50%
미드롭 → 40% + 20% = 60%
드롭 → 40% + 10% = 50% (드롭 후 -10%)
```

---

## 🎒 포션 슬롯 (Potion Slots)

### 기본 슬롯 수

```java
// AbstractPlayer.java
public int potionSlots = 3;  // 기본 3개
```

**슬롯 증가 유물**:
| 유물 | 추가 슬롯 | 최종 슬롯 |
|------|----------|----------|
| **Potion Belt** | +2 | 5개 |
| **Sozu** | -2 (포션 드롭 차단) | 1개 |
| **White Beast Statue** | 0 (드롭 확률 100%) | 3개 |

### 포션 슬롯 변경 예제

```java
// PotionBelt.java
public void onEquip() {
    AbstractDungeon.player.potionSlots += 2;  // +2 슬롯

    // 빈 슬롯 추가
    AbstractDungeon.player.potions.add(new PotionSlot(potionSlots - 2));
    AbstractDungeon.player.potions.add(new PotionSlot(potionSlots - 1));
}
```

---

## 🎁 포션 획득 상황

### 1. 전투 후 보상

**자동 드롭**:
- 일반 전투: 40% (±블리자드)
- 엘리트 전투: 40% (±블리자드)
- 이벤트 전투: 40% (±블리자드)

### 2. 상점 (Shop)

**상점 포션**:
- 기본 3개 판매
- 가격: 50골드 (일반), 75골드 (고급), 100골드 (희귀)

### 3. 이벤트

#### 1막 (Exordium)
- **Shining Light**: 포션 2개 획득
- **Augmenter**: 특정 포션 제공
- **Living Wall**: 포션 선택지

#### 2막 (The City)
- **Drug Dealer**: 포션 구매 (저렴)
- **The Nest**: 포션 선택

#### 3막 (The Beyond)
- **SecretPortal**: 포션 획득

### 4. 유물 효과

**포션 생성 유물**:
| 유물 | 효과 |
|------|------|
| **White Beast Statue** | 전투 후 100% 포션 드롭 |
| **Toy Ornithopter** | 전투 후 포션 힐링 증가 |
| **Sacred Bark** | 포션 효과 2배 |
| **Entropic Brew** | 보스 클리어 시 Entropic Brew 획득 |

---

## 🔧 포션 수정 방법

### 1. 포션 드롭 확률 변경

```java
@SpirePatch(
    cls = "com.megacrit.cardcrawl.rooms.AbstractRoom",
    method = "addPotionToRewards"
)
public static class PotionDropPatch {
    @SpireInsertPatch(loc = 765)  // chance 변수 설정 후
    public static void Insert(AbstractRoom __instance, @ByRef int[] chance) {
        if (AbstractDungeon.ascensionLevel >= 84) {
            // 승천 84: 포션 드롭 확률 절반
            chance[0] /= 2;  // 40% → 20%
        }
    }
}
```

### 2. 포션 등급 확률 변경

```java
@SpirePatch(
    cls = "com.megacrit.cardcrawl.helpers.PotionHelper",
    method = SpirePatch.STATICINIT
)
public static class PotionRarityPatch {
    @SpirePostfixPatch
    public static void Postfix() {
        if (AbstractDungeon.ascensionLevel >= 70) {
            // 승천 70+: 레어 포션 확률 감소
            PotionHelper.POTION_COMMON_CHANCE = 75;    // 65% → 75%
            PotionHelper.POTION_UNCOMMON_CHANCE = 20;  // 25% → 20%
            // RARE: 5% (나머지)
        }
    }
}
```

### 3. 포션 블리자드 시스템 조정

```java
@SpirePatch(
    cls = "com.megacrit.cardcrawl.rooms.AbstractRoom",
    method = "addPotionToRewards"
)
public static class PotionBlizzardPatch {
    @SpirePostfixPatch
    public static void Postfix(AbstractRoom __instance) {
        if (AbstractDungeon.ascensionLevel >= 90) {
            // 블리자드 보정 무효화 (항상 40% 고정)
            AbstractRoom.blizzardPotionMod = 0;
        }
    }
}
```

### 4. 포션 슬롯 제한

```java
@SpirePatch(
    cls = "com.megacrit.cardcrawl.characters.AbstractPlayer",
    method = SpirePatch.CONSTRUCTOR
)
public static class PotionSlotPatch {
    @SpirePostfixPatch
    public static void Postfix(AbstractPlayer __instance) {
        if (AbstractDungeon.ascensionLevel >= 95) {
            // 승천 95: 포션 슬롯 1개만
            __instance.potionSlots = 1;
        }
    }
}
```

### 5. 특정 포션 차단

```java
@SpirePatch(
    cls = "com.megacrit.cardcrawl.dungeons.AbstractDungeon",
    method = "returnRandomPotion",
    paramtypez = { AbstractPotion.PotionRarity.class, boolean.class }
)
public static class BlockPotionPatch {
    @SpirePostfixPatch
    public static AbstractPotion Postfix(AbstractPotion result) {
        if (AbstractDungeon.ascensionLevel >= 75) {
            // Fairy in a Bottle 차단
            while (result.ID.equals("FairyPotion")) {
                result = PotionHelper.getRandomPotion();
            }
        }
        return result;
    }
}
```

---

## 📝 실전 예제

### 예제 1: 승천 레벨별 차등 적용

```java
@SpirePatch(
    cls = "com.megacrit.cardcrawl.rooms.MonsterRoomElite",
    method = "returnRandomRelicTier"
)
public static class AscensionRelicPatch {
    @SpirePrefixPatch
    public static SpireReturn<AbstractRelic.RelicTier> Prefix() {
        int level = AbstractDungeon.ascensionLevel;
        int roll = AbstractDungeon.relicRng.random(0, 99);

        if (level >= 21 && level < 40) {
            // 레벨 21-39: 약간 어렵게
            if (roll < 55) return SpireReturn.Return(AbstractRelic.RelicTier.COMMON);
            if (roll > 85) return SpireReturn.Return(AbstractRelic.RelicTier.RARE);
            return SpireReturn.Return(AbstractRelic.RelicTier.UNCOMMON);

        } else if (level >= 40 && level < 60) {
            // 레벨 40-59: 중간 난이도
            if (roll < 60) return SpireReturn.Return(AbstractRelic.RelicTier.COMMON);
            if (roll > 90) return SpireReturn.Return(AbstractRelic.RelicTier.RARE);
            return SpireReturn.Return(AbstractRelic.RelicTier.UNCOMMON);

        } else if (level >= 60) {
            // 레벨 60+: 매우 어렵게
            if (roll < 70) return SpireReturn.Return(AbstractRelic.RelicTier.COMMON);
            if (roll > 95) return SpireReturn.Return(AbstractRelic.RelicTier.RARE);
            return SpireReturn.Return(AbstractRelic.RelicTier.UNCOMMON);
        }

        return SpireReturn.Continue();
    }
}
```

### 예제 2: Act별 다른 포션 드롭

```java
@SpirePatch(
    cls = "com.megacrit.cardcrawl.rooms.AbstractRoom",
    method = "addPotionToRewards"
)
public static class ActBasedPotionPatch {
    @SpireInsertPatch(loc = 765)
    public static void Insert(@ByRef int[] chance) {
        int actNum = AbstractDungeon.actNum;

        switch (actNum) {
            case 1:
                // 1막: 포션 드롭 확률 증가 (초보자 친화)
                chance[0] += 20;  // 40% → 60%
                break;
            case 2:
                // 2막: 기본 확률 유지
                break;
            case 3:
                // 3막: 포션 드롭 확률 감소 (난이도 상승)
                chance[0] -= 20;  // 40% → 20%
                break;
        }
    }
}
```

### 예제 3: 유물-포션 복합 시스템

```java
@SpirePatch(
    cls = "com.megacrit.cardcrawl.rooms.MonsterRoomElite",
    method = "dropReward"
)
public static class RelicPotionBalancePatch {
    @SpirePostfixPatch
    public static void Postfix(MonsterRoomElite __instance) {
        if (AbstractDungeon.ascensionLevel >= 50) {
            // 승천 50+: 유물 대신 포션 2개 제공
            __instance.removeOneRelicFromRewards();
            __instance.addPotionToRewards(AbstractDungeon.returnRandomPotion());
            __instance.addPotionToRewards(AbstractDungeon.returnRandomPotion());
        }
    }
}
```

---

## 🔍 디버깅 팁

### 1. 유물 풀 상태 로깅

```java
@SpirePatch(
    cls = "com.megacrit.cardcrawl.dungeons.AbstractDungeon",
    method = "returnRandomRelicKey"
)
public static class RelicPoolLogPatch {
    @SpirePrefixPatch
    public static void Prefix(AbstractRelic.RelicTier tier) {
        logger.info("Relic Pool (" + tier + "): ");
        switch (tier) {
            case COMMON:
                logger.info("  Size: " + AbstractDungeon.commonRelicPool.size());
                break;
            case UNCOMMON:
                logger.info("  Size: " + AbstractDungeon.uncommonRelicPool.size());
                break;
            case RARE:
                logger.info("  Size: " + AbstractDungeon.rareRelicPool.size());
                break;
        }
    }
}
```

### 2. 포션 드롭 확률 추적

```java
@SpirePatch(
    cls = "com.megacrit.cardcrawl.rooms.AbstractRoom",
    method = "addPotionToRewards"
)
public static class PotionChanceLogPatch {
    @SpireInsertPatch(loc = 789)  // "POTION CHANCE" 로그 직후
    public static void Insert(int chance) {
        logger.info("  Blizzard Mod: " + AbstractRoom.blizzardPotionMod);
        logger.info("  Final Chance: " + chance);
    }
}
```

---

## ⚠️ 주의사항

### 1. 유물 풀은 게임 시작 시 초기화

```java
// ❌ 잘못된 예 (런타임에 풀 수정)
@SpirePatch(cls = "...", method = "returnRandomRelic")
public static void Postfix() {
    AbstractDungeon.commonRelicPool.remove("Lantern");  // 효과 없음
}

// ✅ 올바른 예 (초기화 시점에 수정)
@SpirePatch(cls = "...", method = "initializeRelicList")
public static void Postfix() {
    AbstractDungeon.commonRelicPool.remove("Lantern");  // 정상 작동
}
```

### 2. 포션 슬롯은 음수가 될 수 없음

```java
// ❌ 잘못된 예
__instance.potionSlots = -1;  // 크래시 발생

// ✅ 올바른 예
__instance.potionSlots = Math.max(0, __instance.potionSlots - 1);
```

### 3. null 체크 필수

```java
@SpirePostfixPatch
public static void Postfix(AbstractRoom __instance) {
    // ❌ rewards가 null일 수 있음
    if (__instance.rewards.size() > 0) {
        // ...
    }

    // ✅ null 체크 추가
    if (__instance.rewards != null && __instance.rewards.size() > 0) {
        // ...
    }
}
```

### 4. 유물 ID는 정확히 입력

```java
// ❌ 잘못된 예
if (AbstractDungeon.player.hasRelic("potion_belt")) {  // 소문자 사용

// ✅ 올바른 예
if (AbstractDungeon.player.hasRelic("Potion Belt")) {  // 정확한 ID
```

---

## 📚 관련 클래스 참조

### 유물 관련
| 클래스 | 경로 | 역할 |
|--------|------|------|
| **AbstractRelic** | `com.megacrit.cardcrawl.relics` | 유물 기본 클래스 |
| **RelicLibrary** | `com.megacrit.cardcrawl.helpers` | 유물 풀 관리 |
| **MonsterRoomElite** | `com.megacrit.cardcrawl.rooms` | 엘리트 유물 보상 |
| **AbstractDungeon** | `com.megacrit.cardcrawl.dungeons` | 유물 풀 초기화 |

### 포션 관련
| 클래스 | 경로 | 역할 |
|--------|------|------|
| **AbstractPotion** | `com.megacrit.cardcrawl.potions` | 포션 기본 클래스 |
| **PotionHelper** | `com.megacrit.cardcrawl.helpers` | 포션 확률 관리 |
| **AbstractRoom** | `com.megacrit.cardcrawl.rooms` | 포션 드롭 로직 |
| **AbstractPlayer** | `com.megacrit.cardcrawl.characters` | 포션 슬롯 관리 |

---

## 🎓 추가 학습

1. **유물 효과**: `AbstractRelic` 클래스의 다양한 콜백 메서드
2. **포션 사용**: `AbstractPotion.use()` 메서드
3. **유물 풀 관리**: `RelicLibrary.initialize()` 메서드
4. **상점 시스템**: `ShopScreen` 클래스

---

**작성일**: 2025-11-15
**기반 소스**: E:\workspace\sts-decompile\
**검증**: 디컴파일 소스 직접 분석
**게임 버전**: 01-23-2019
