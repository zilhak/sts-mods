# Shop Encounter (상점 조우 시스템)

Slay the Spire의 상점 시스템에 대한 완전한 분석 문서.

## 목차

1. [시스템 개요](#시스템-개요)
2. [호출 흐름](#호출-흐름)
3. [카드 풀 생성 시스템](#카드-풀-생성-시스템)
4. [유물 생성 시스템](#유물-생성-시스템)
5. [포션 생성 시스템](#포션-생성-시스템)
6. [가격 결정 시스템](#가격-결정-시스템)
7. [할인 시스템](#할인-시스템)
8. [카드 제거 (Purge) 시스템](#카드-제거-purge-시스템)
9. [The Courier 유물 효과](#the-courier-유물-효과)
10. [Ascension 16+ 변경사항](#ascension-16-변경사항)
11. [수정 방법](#수정-방법)
12. [관련 클래스](#관련-클래스)

---

## 시스템 개요

### 상점 룸 기본 구조

상점은 `ShopRoom` 클래스로 표현되며, 플레이어가 골드를 사용하여 카드, 유물, 포션을 구매하고 카드를 제거할 수 있는 특수 조우입니다.

**기본 설정:**
- **맵 심볼**: `$`
- **BGM**: `"SHOP"` (4막 제외)
- **Room Phase**: `RoomPhase.COMPLETE` (전투 없음)

### 상점 상품 구성

모든 상점은 다음 항목들을 판매합니다:

1. **캐릭터별 카드 (Colored Cards)**: 5장
   - 공격 (Attack) 2장
   - 스킬 (Skill) 2장
   - 파워 (Power) 1장
   - 그 중 1장은 50% 할인 (Sale Tag)

2. **무색 카드 (Colorless Cards)**: 2장
   - 언커먼 (Uncommon) 1장
   - 레어 (Rare) 1장

3. **유물 (Relics)**: 3개
   - 랜덤 등급 2개 (Common/Uncommon/Rare)
   - 상점 전용 유물 (Shop Tier) 1개

4. **포션 (Potions)**: 3개
   - 완전 랜덤

5. **카드 제거 서비스 (Purge)**
   - 기본 비용: 75 골드
   - 사용할 때마다 +25 골드 증가

### 핵심 RNG 시드

```java
// Merchant.java:70-108
AbstractDungeon.rollRarity()           // 카드 등급 결정 (merchantRng 사용)
AbstractDungeon.getCardFromPool()      // 특정 타입과 등급의 카드 획득
AbstractDungeon.getColorlessCardFromPool() // 무색 카드 획득
ShopScreen.rollRelicTier()            // 유물 등급 결정 (merchantRng 사용)
AbstractDungeon.returnRandomRelicEnd() // 유물 획득
AbstractDungeon.returnRandomPotion()   // 포션 획득
```

---

## 호출 흐름

### 1단계: ShopRoom 생성

맵 생성 시 ShopRoom이 배치됩니다.

```java
// AbstractDungeon.java:720
for (int i = 0; i < shopCount; i++) {
    roomList.add(new ShopRoom());
}
```

### 2단계: ShopRoom 진입

플레이어가 상점 노드에 진입하면 `onPlayerEntry()`가 호출됩니다.

```java
// ShopRoom.java:37-43
public void onPlayerEntry() {
    if (!AbstractDungeon.id.equals("TheEnding")) {
        playBGM("SHOP");  // 4막이 아니면 상점 BGM 재생
    }
    AbstractDungeon.overlayMenu.proceedButton.setLabel(TEXT[0]);
    setMerchant(new Merchant());  // 상인 생성
}
```

### 3단계: Merchant 초기화

Merchant 생성자가 호출되며 상품 풀을 생성합니다.

```java
// Merchant.java:57-122
public Merchant(float x, float y, int newShopScreen) {
    // 1. 캐릭터별 카드 5장 생성 (Attack 2, Skill 2, Power 1)
    AbstractCard c = AbstractDungeon.getCardFromPool(
        AbstractDungeon.rollRarity(),
        AbstractCard.CardType.ATTACK,
        true
    ).makeCopy();

    while (c.color == AbstractCard.CardColor.COLORLESS) {
        c = AbstractDungeon.getCardFromPool(
            AbstractDungeon.rollRarity(),
            AbstractCard.CardType.ATTACK,
            true
        ).makeCopy();
    }
    this.cards1.add(c);
    // ... 나머지 카드들도 동일 방식으로 생성

    // 2. 무색 카드 2장 생성
    this.cards2.add(AbstractDungeon.getColorlessCardFromPool(
        AbstractCard.CardRarity.UNCOMMON
    ).makeCopy());
    this.cards2.add(AbstractDungeon.getColorlessCardFromPool(
        AbstractCard.CardRarity.RARE
    ).makeCopy());

    // 3. ShopScreen 초기화 (유물, 포션 생성)
    AbstractDungeon.shopScreen.init(this.cards1, this.cards2);
}
```

### 4단계: ShopScreen 초기화

ShopScreen.init()이 호출되어 모든 상품을 최종 설정합니다.

```java
// ShopScreen.java:135-247
public void init(ArrayList<AbstractCard> coloredCards,
                 ArrayList<AbstractCard> colorlessCards) {
    this.coloredCards = coloredCards;
    this.colorlessCards = colorlessCards;

    initCards();      // 카드 가격 설정 및 배치
    initRelics();     // 유물 3개 생성 및 가격 설정
    initPotions();    // 포션 3개 생성 및 가격 설정

    this.purgeAvailable = true;
    actualPurgeCost = purgeCost;  // 카드 제거 비용 설정

    // Ascension 16+ 가격 10% 증가
    if (AbstractDungeon.ascensionLevel >= 16) {
        applyDiscount(1.1F, false);
    }

    // 유물 할인 적용
    if (AbstractDungeon.player.hasRelic("The Courier")) {
        applyDiscount(0.8F, true);  // 20% 할인
    }
    if (AbstractDungeon.player.hasRelic("Membership Card")) {
        applyDiscount(0.5F, true);  // 50% 할인
    }
    if (AbstractDungeon.player.hasRelic("Smiling Mask")) {
        actualPurgeCost = 50;  // 카드 제거 고정 50 골드
    }
}
```

---

## 카드 풀 생성 시스템

### 캐릭터별 카드 (Colored Cards) 생성

Merchant 생성자에서 5장의 카드를 특정 순서로 생성합니다:

**생성 순서:**
1. **공격 카드 1** (Attack #1)
2. **공격 카드 2** (Attack #2) - 첫 번째와 다른 카드
3. **스킬 카드 1** (Skill #1)
4. **스킬 카드 2** (Skill #2) - 세 번째와 다른 카드
5. **파워 카드 1** (Power #1)

**중복 방지 로직:**

```java
// Merchant.java:77-82
c = AbstractDungeon.getCardFromPool(
    AbstractDungeon.rollRarity(),
    AbstractCard.CardType.ATTACK,
    true
).makeCopy();

// 이전 카드와 같은 카드가 나오면 다시 뽑기
while (Objects.equals(c.cardID,
       ((AbstractCard)this.cards1.get(this.cards1.size() - 1)).cardID)
       || c.color == AbstractCard.CardColor.COLORLESS) {
    c = AbstractDungeon.getCardFromPool(
        AbstractDungeon.rollRarity(),
        AbstractCard.CardType.ATTACK,
        true
    ).makeCopy();
}
this.cards1.add(c);
```

**무색 카드 필터링:**

모든 캐릭터별 카드는 무색 카드(Colorless)를 제외하고 생성됩니다. 무색 카드가 나오면 다시 뽑습니다.

```java
while (c.color == AbstractCard.CardColor.COLORLESS) {
    c = AbstractDungeon.getCardFromPool(
        AbstractDungeon.rollRarity(),
        AbstractCard.CardType.ATTACK,
        true
    ).makeCopy();
}
```

### 무색 카드 (Colorless Cards) 생성

고정된 등급으로 2장이 생성됩니다:

```java
// Merchant.java:107-108
this.cards2.add(AbstractDungeon.getColorlessCardFromPool(
    AbstractCard.CardRarity.UNCOMMON
).makeCopy());

this.cards2.add(AbstractDungeon.getColorlessCardFromPool(
    AbstractCard.CardRarity.RARE
).makeCopy());
```

### 카드 등급 확률 (Card Rarity)

상점의 카드 등급은 `ShopRoom.getCardRarity()`를 통해 결정됩니다.

**ShopRoom 기본 확률:**

```java
// ShopRoom.java:28-29
this.baseRareCardChance = 9;       // 9%
this.baseUncommonCardChance = 37;  // 37%
// Common: 54%
```

**확률 테이블:**

| 등급 (Rarity) | 기본 확률 | 누적 범위 |
|--------------|---------|---------|
| Rare | 9% | 0-8 |
| Uncommon | 37% | 9-45 |
| Common | 54% | 46-99 |

**등급 결정 알고리즘:**

```java
// AbstractDungeon.java:2028-2029
public static AbstractCard.CardRarity rollRarity() {
    return rollRarity(cardRng);
}

// AbstractDungeon.java:2000-2007
public static AbstractCard.CardRarity rollRarity(Random rng) {
    int roll = cardRng.random(99);  // 0-99 범위
    roll += cardBlizzRandomizer;    // 카드 블리자드 시스템 (보상과 동일)

    if (currMapNode == null) {
        return getCardRarityFallback(roll);
    }
    return getCurrRoom().getCardRarity(roll);
}

// AbstractRoom.java:183-209 (ShopRoom은 useAlternation=false로 호출)
public AbstractCard.CardRarity getCardRarity(int roll, boolean useAlternation) {
    this.rareCardChance = this.baseRareCardChance;
    this.uncommonCardChance = this.baseUncommonCardChance;

    if (useAlternation) {
        alterCardRarityProbabilities();  // 유물 효과 적용
    }

    if (roll < this.rareCardChance) {
        return AbstractCard.CardRarity.RARE;
    }
    if (roll < this.rareCardChance + this.uncommonCardChance) {
        return AbstractCard.CardRarity.UNCOMMON;
    }
    return AbstractCard.CardRarity.COMMON;
}
```

**ShopRoom 특수 처리:**

```java
// ShopRoom.java:46-48
public AbstractCard.CardRarity getCardRarity(int roll) {
    return getCardRarity(roll, false);  // useAlternation = false
}
```

상점은 **유물의 등급 확률 변경 효과를 받지 않습니다**. 예를 들어, Prayer Wheel이나 Question Card 같은 유물이 있어도 상점 카드 등급 확률은 변하지 않습니다.

---

## 유물 생성 시스템

### 유물 슬롯 구성

상점은 항상 3개의 유물을 판매하며, 각 슬롯은 다른 방식으로 생성됩니다:

**슬롯별 생성 방식:**

```java
// ShopScreen.java:395-417
private void initRelics() {
    this.relics.clear();
    this.relics = new ArrayList<>();

    for (int i = 0; i < 3; i++) {
        AbstractRelic tempRelic = null;

        // 슬롯 0, 1: 랜덤 등급 (Common/Uncommon/Rare)
        if (i != 2) {
            tempRelic = AbstractDungeon.returnRandomRelicEnd(
                rollRelicTier()
            );
        }
        // 슬롯 2: 상점 전용 유물 (Shop Tier)
        else {
            tempRelic = AbstractDungeon.returnRandomRelicEnd(
                AbstractRelic.RelicTier.SHOP
            );
        }

        StoreRelic relic = new StoreRelic(tempRelic, i, this);

        // Daily Run이 아니면 가격 변동 ±5%
        if (!Settings.isDailyRun) {
            relic.price = MathUtils.round(
                relic.price * AbstractDungeon.merchantRng.random(0.95F, 1.05F)
            );
        }
        this.relics.add(relic);
    }
}
```

### 유물 등급 확률 (Relic Tier)

슬롯 0과 1의 유물 등급은 `rollRelicTier()`로 결정됩니다:

```java
// ShopScreen.java:487-500
public static AbstractRelic.RelicTier rollRelicTier() {
    int roll = AbstractDungeon.merchantRng.random(99);  // 0-99 범위
    logger.info("ROLL " + roll);

    if (roll < 48) {
        return AbstractRelic.RelicTier.COMMON;      // 48% (0-47)
    }
    if (roll < 82) {
        return AbstractRelic.RelicTier.UNCOMMON;    // 34% (48-81)
    }
    return AbstractRelic.RelicTier.RARE;            // 18% (82-99)
}
```

**유물 등급 확률 테이블:**

| 등급 (Tier) | 확률 | 누적 범위 | 슬롯 |
|------------|-----|---------|-----|
| Common | 48% | 0-47 | 0, 1 |
| Uncommon | 34% | 48-81 | 0, 1 |
| Rare | 18% | 82-99 | 0, 1 |
| **Shop** | **100%** | - | **2** |

### 상점 전용 유물 (Shop Tier Relics)

슬롯 2는 항상 Shop Tier 유물만 판매합니다. Shop Tier 유물 목록:

- **Courier** - 상점에서 구매 시 즉시 보충, Membership Card와 중복 가능
- **Frozen Egg 2.0** - 스킬 카드 한 장을 업그레이드 상태로 추가
- **Molten Egg 2.0** - 공격 카드 한 장을 업그레이드 상태로 추가
- **Toxic Egg 2.0** - 파워 카드 한 장을 업그레이드 상태로 추가
- **Cauldron** - 전투 시작 시 랜덤 포션 5개 생성
- **Toolbox** - 무색 카드 한 장 추가
- **Prismatic Shard** - 모든 캐릭터의 카드 획득 가능
- **Medical Kit** - 상태이상 카드 사용 가능하게 변경
- **Lee's Waffle** - 최대 HP +7 증가 및 즉시 HP 회복
- **Shuriken** - 공격 카드 3장 사용 시 힘 +1
- **Kunai** - 공격 카드 3장 사용 시 민첩 +1
- **Ornamental Fan** - 공격 카드 3장 사용 시 방어도 +4

---

## 포션 생성 시스템

### 포션 생성 로직

상점은 항상 3개의 포션을 판매하며, 완전 랜덤으로 생성됩니다:

```java
// ShopScreen.java:419-432
private void initPotions() {
    this.potions.clear();
    this.potions = new ArrayList<>();

    for (int i = 0; i < 3; i++) {
        StorePotion potion = new StorePotion(
            AbstractDungeon.returnRandomPotion(),
            i,
            this
        );

        // Daily Run이 아니면 가격 변동 ±5%
        if (!Settings.isDailyRun) {
            potion.price = MathUtils.round(
                potion.price * AbstractDungeon.merchantRng.random(0.95F, 1.05F)
            );
        }
        this.potions.add(potion);
    }
}
```

### 포션 등급 및 가격

포션은 등급에 따라 기본 가격이 다릅니다:

```java
// AbstractPotion.java:364-374
public int getPrice() {
    switch (this.rarity) {
        case COMMON:
            return 50;
        case UNCOMMON:
            return 75;
        case RARE:
            return 100;
    }
    return 999;  // 오류 시 (발생하지 않음)
}
```

**포션 가격 테이블:**

| 등급 (Rarity) | 기본 가격 (골드) |
|--------------|--------------|
| Common | 50 |
| Uncommon | 75 |
| Rare | 100 |

### Sozu 유물 효과

Sozu 유물을 소지한 경우 포션 구매가 불가능합니다:

```java
// StorePotion.java:87-91
public void purchasePotion() {
    if (AbstractDungeon.player.hasRelic("Sozu")) {
        AbstractDungeon.player.getRelic("Sozu").flash();
        return;  // 구매 즉시 취소
    }
    // ... 구매 로직
}
```

---

## 가격 결정 시스템

### 카드 가격

카드 가격은 등급에 따라 결정되며, ±10% 변동이 적용됩니다:

**기본 가격:**

```java
// AbstractCard.java:2678-2695
public static int getPrice(CardRarity rarity) {
    switch (rarity) {
        case BASIC:
            logger.info("ERROR: WHY WE SELLIN' BASIC");
            return 9999;
        case COMMON:
            return 50;
        case UNCOMMON:
            return 75;
        case RARE:
            return 150;
        case SPECIAL:
            logger.info("ERROR: WHY WE SELLIN' SPECIAL");
            return 9999;
    }
    logger.info("No rarity on this card?");
    return 0;
}
```

**가격 변동 적용:**

```java
// ShopScreen.java:260-272
for (i = 0; i < this.coloredCards.size(); i++) {
    // ±10% 변동 (0.9 ~ 1.1)
    float tmpPrice = AbstractCard.getPrice(
        ((AbstractCard)this.coloredCards.get(i)).rarity
    ) * AbstractDungeon.merchantRng.random(0.9F, 1.1F);

    AbstractCard c = this.coloredCards.get(i);
    c.price = (int)tmpPrice;

    // 유물 효과 적용
    for (AbstractRelic r : AbstractDungeon.player.relics) {
        r.onPreviewObtainCard(c);
    }
}
```

**무색 카드 가격:**

무색 카드는 기본 가격의 1.2배를 적용합니다:

```java
// ShopScreen.java:276-292
for (i = 0; i < this.colorlessCards.size(); i++) {
    float tmpPrice = AbstractCard.getPrice(
        ((AbstractCard)this.colorlessCards.get(i)).rarity
    ) * AbstractDungeon.merchantRng.random(0.9F, 1.1F);

    // 무색 카드는 1.2배 (20% 더 비쌈)
    tmpPrice *= 1.2F;

    AbstractCard c = this.colorlessCards.get(i);
    c.price = (int)tmpPrice;

    for (AbstractRelic r : AbstractDungeon.player.relics) {
        r.onPreviewObtainCard(c);
    }
}
```

**카드 가격 테이블 (할인 전):**

| 카드 종류 | 등급 | 기본 가격 | 변동 범위 | 최종 범위 |
|---------|------|---------|---------|---------|
| 캐릭터별 카드 | Common | 50 | ±10% | 45-55 |
| 캐릭터별 카드 | Uncommon | 75 | ±10% | 67-82 |
| 캐릭터별 카드 | Rare | 150 | ±10% | 135-165 |
| **무색 카드** | Uncommon | 75 | ±10%, ×1.2 | **81-99** |
| **무색 카드** | Rare | 150 | ±10%, ×1.2 | **162-198** |

**세일 카드 (On Sale):**

5장의 캐릭터별 카드 중 무작위 1장은 50% 할인됩니다:

```java
// ShopScreen.java:296-298
AbstractCard saleCard = this.coloredCards.get(
    AbstractDungeon.merchantRng.random(0, 4)
);
saleCard.price /= 2;  // 가격 절반
this.saleTag = new OnSaleTag(saleCard);
```

### 유물 가격

유물 가격은 등급에 따라 결정되며, ±5% 변동이 적용됩니다:

**기본 가격:**

```java
// AbstractRelic.java:171-186
public int getPrice() {
    switch (this.tier) {
        case COMMON:
            return 150;
        case UNCOMMON:
            return 250;
        case RARE:
            return 300;
        case SHOP:
            return 150;
        case BOSS:
            return 400;
        case STARTER:
            return 999;
        case DEPRECATED:
            return 999;
    }
}
```

**가격 변동 적용 (±5%):**

```java
// ShopScreen.java:411-414
if (!Settings.isDailyRun) {
    relic.price = MathUtils.round(
        relic.price * AbstractDungeon.merchantRng.random(0.95F, 1.05F)
    );
}
```

**유물 가격 테이블 (할인 전):**

| 등급 (Tier) | 기본 가격 | 변동 범위 | 최종 범위 |
|------------|---------|---------|---------|
| Common | 150 | ±5% | 142-157 |
| Uncommon | 250 | ±5% | 237-262 |
| Rare | 300 | ±5% | 285-315 |
| **Shop** | **150** | **±5%** | **142-157** |

---

## 할인 시스템

### The Courier (배송원)

**효과:** 상점 가격 20% 할인

```java
// ShopScreen.java:237-239
if (AbstractDungeon.player.hasRelic("The Courier")) {
    applyDiscount(0.8F, true);  // 80% 가격 = 20% 할인
}
```

**적용 범위:**
- 모든 카드
- 모든 유물
- 모든 포션
- 카드 제거 (Purge)

### Membership Card (회원 카드)

**효과:** 상점 가격 50% 할인

```java
// ShopScreen.java:240-242
if (AbstractDungeon.player.hasRelic("Membership Card")) {
    applyDiscount(0.5F, true);  // 50% 가격 = 50% 할인
}
```

**적용 범위:**
- 모든 카드
- 모든 유물
- 모든 포션
- 카드 제거 (Purge)

### 할인 중복 적용

The Courier와 Membership Card를 모두 소지한 경우, 할인이 **곱연산으로 중복 적용**됩니다:

**최종 가격 = 기본 가격 × 0.8 × 0.5 = 기본 가격 × 0.4 (60% 할인)**

```java
// ShopScreen.java:237-242
if (AbstractDungeon.player.hasRelic("The Courier")) {
    applyDiscount(0.8F, true);
}
if (AbstractDungeon.player.hasRelic("Membership Card")) {
    applyDiscount(0.5F, true);  // 이전 할인 후 가격에 다시 적용
}
```

**할인 적용 함수:**

```java
// ShopScreen.java:373-392
public void applyDiscount(float multiplier, boolean affectPurge) {
    for (StoreRelic r : this.relics) {
        r.price = MathUtils.round(r.price * multiplier);
    }
    for (StorePotion p : this.potions) {
        p.price = MathUtils.round(p.price * multiplier);
    }
    for (AbstractCard c : this.coloredCards) {
        c.price = MathUtils.round(c.price * multiplier);
    }
    for (AbstractCard c : this.colorlessCards) {
        c.price = MathUtils.round(c.price * multiplier);
    }

    if (AbstractDungeon.player.hasRelic("Smiling Mask")) {
        actualPurgeCost = 50;  // Smiling Mask는 고정 50 골드
    }
    else if (affectPurge) {
        actualPurgeCost = MathUtils.round(purgeCost * multiplier);
    }
}
```

### 할인 조합 가격표

| 유물 조합 | 할인율 | 최종 가격 배율 |
|---------|-------|-------------|
| 없음 | 0% | 1.0 |
| The Courier | 20% | 0.8 |
| Membership Card | 50% | 0.5 |
| **Courier + Membership** | **60%** | **0.4** |

**예시 (Common 유물 150 골드):**
- 할인 없음: 150 골드
- Courier: 120 골드
- Membership Card: 75 골드
- **Courier + Membership Card: 60 골드**

---

## 카드 제거 (Purge) 시스템

### 기본 시스템

상점에서는 골드를 지불하고 덱에서 카드 1장을 영구 제거할 수 있습니다.

**기본 메커니즘:**

```java
// ShopScreen.java:226-230, 249-251
this.purgeAvailable = true;
this.purgeCardY = -1000.0F;
this.purgeCardX = Settings.WIDTH * 0.73F * Settings.scale;
this.purgeCardScale = 0.7F;
actualPurgeCost = purgeCost;  // 초기 비용: 75 골드

// 초기화 함수
public static void resetPurgeCost() {
    purgeCost = 75;
    actualPurgeCost = 75;
}
```

**기본 비용:** 75 골드

### 비용 증가 시스템

카드를 제거할 때마다 **다음 제거 비용이 25 골드씩 증가**합니다:

```java
// ShopScreen.java:302-319
public static void purgeCard() {
    AbstractDungeon.player.loseGold(actualPurgeCost);
    CardCrawlGame.sound.play("SHOP_PURCHASE", 0.1F);

    // 다음 제거 비용 증가
    purgeCost += 25;
    actualPurgeCost = purgeCost;

    // Smiling Mask 소지 시 고정 50 골드
    if (AbstractDungeon.player.hasRelic("Smiling Mask")) {
        actualPurgeCost = 50;
        AbstractDungeon.player.getRelic("Smiling Mask").stopPulse();
    }
    // The Courier + Membership Card 조합 시 할인 재적용
    else if (AbstractDungeon.player.hasRelic("The Courier") &&
             AbstractDungeon.player.hasRelic("Membership Card")) {
        actualPurgeCost = MathUtils.round(purgeCost * 0.8F * 0.5F);
    }
    else if (AbstractDungeon.player.hasRelic("The Courier")) {
        actualPurgeCost = MathUtils.round(purgeCost * 0.8F);
    }
    else if (AbstractDungeon.player.hasRelic("Membership Card")) {
        actualPurgeCost = MathUtils.round(purgeCost * 0.5F);
    }
}
```

**제거 비용 증가 테이블 (할인 없음):**

| 제거 횟수 | 비용 (골드) |
|---------|-----------|
| 1회 | 75 |
| 2회 | 100 |
| 3회 | 125 |
| 4회 | 150 |
| 5회 | 175 |
| N회 | 75 + (N-1) × 25 |

### Smiling Mask (웃는 가면)

**효과:** 카드 제거 비용 고정 50 골드

```java
// ShopScreen.java:244-246
if (AbstractDungeon.player.hasRelic("Smiling Mask")) {
    actualPurgeCost = 50;
}
```

**중요:** Smiling Mask를 소지하면 제거 비용이 항상 50 골드로 고정되며, 제거 횟수에 관계없이 증가하지 않습니다.

```java
// ShopScreen.java:308-311
if (AbstractDungeon.player.hasRelic("Smiling Mask")) {
    actualPurgeCost = 50;  // 항상 50 골드로 재설정
    AbstractDungeon.player.getRelic("Smiling Mask").stopPulse();
}
```

### 제거 비용 조합표

**Smiling Mask 소지 시:**
- 항상 50 골드 (할인 무시, 횟수 무시)

**Smiling Mask 없을 때:**

| 제거 횟수 | 기본 비용 | Courier (0.8) | Membership (0.5) | 둘 다 (0.4) |
|---------|---------|-------------|----------------|-----------|
| 1회 | 75 | 60 | 37 | 30 |
| 2회 | 100 | 80 | 50 | 40 |
| 3회 | 125 | 100 | 62 | 50 |
| 4회 | 150 | 120 | 75 | 60 |
| 5회 | 175 | 140 | 87 | 70 |

---

## The Courier 유물 효과

### 기본 효과

The Courier는 상점에서 구매 시 즉시 새 상품으로 교체되는 특수 효과를 가지고 있습니다.

**적용 대상:**
- 유물
- 포션

**적용되지 않는 대상:**
- 카드 (한번 구매하면 사라짐)
- 카드 제거 서비스

### 유물 교체 시스템

The Courier를 소지하거나 상점에서 The Courier를 구매한 경우, 유물 구매 시 즉시 교체됩니다:

```java
// StoreRelic.java:122-134
if (this.relic.relicId.equals("The Courier") ||
    AbstractDungeon.player.hasRelic("The Courier")) {

    // 새 유물 생성
    AbstractRelic tempRelic = AbstractDungeon.returnRandomRelicEnd(
        ShopScreen.rollRelicTier()
    );

    // 상점 전용 유물 (OldCoin, SmilingMask, MawBank, Courier) 제외
    while (tempRelic instanceof com.megacrit.cardcrawl.relics.OldCoin ||
           tempRelic instanceof com.megacrit.cardcrawl.relics.SmilingMask ||
           tempRelic instanceof com.megacrit.cardcrawl.relics.MawBank ||
           tempRelic instanceof com.megacrit.cardcrawl.relics.Courier) {
        tempRelic = AbstractDungeon.returnRandomRelicEnd(
            ShopScreen.rollRelicTier()
        );
    }

    this.relic = tempRelic;
    this.price = this.relic.getPrice();
    this.shopScreen.getNewPrice(this);  // 가격 재계산
}
else {
    this.isPurchased = true;  // 일반 유물은 구매 후 사라짐
}
```

**교체 제외 유물:**
- Old Coin (구식 동전)
- Smiling Mask (웃는 가면)
- Maw Bank (입 은행)
- The Courier (배송원 자신)

**가격 재계산:**

```java
// ShopScreen.java:435-454
public void getNewPrice(StoreRelic r) {
    int retVal = r.price;

    // Daily Run이 아니면 ±5% 변동
    if (!Settings.isDailyRun) {
        retVal = MathUtils.round(
            retVal * AbstractDungeon.merchantRng.random(0.95F, 1.05F)
        );
    }

    // 할인 적용
    if (AbstractDungeon.player.hasRelic("The Courier")) {
        retVal = applyDiscountToRelic(retVal, 0.8F);
    }
    if (AbstractDungeon.player.hasRelic("Membership Card")) {
        retVal = applyDiscountToRelic(retVal, 0.5F);
    }

    r.price = retVal;
}
```

### 포션 교체 시스템

The Courier를 소지한 경우, 포션 구매 시 즉시 교체됩니다:

```java
// StorePotion.java:101-107
if (AbstractDungeon.player.hasRelic("The Courier")) {
    this.potion = AbstractDungeon.returnRandomPotion();
    this.price = this.potion.getPrice();
    this.shopScreen.getNewPrice(this);  // 가격 재계산
}
else {
    this.isPurchased = true;  // 일반 포션은 구매 후 사라짐
}
```

**가격 재계산:**

```java
// ShopScreen.java:457-476
public void getNewPrice(StorePotion r) {
    int retVal = r.price;

    // Daily Run이 아니면 ±5% 변동
    if (!Settings.isDailyRun) {
        retVal = MathUtils.round(
            retVal * AbstractDungeon.merchantRng.random(0.95F, 1.05F)
        );
    }

    // 할인 적용
    if (AbstractDungeon.player.hasRelic("The Courier")) {
        retVal = applyDiscountToRelic(retVal, 0.8F);
    }
    if (AbstractDungeon.player.hasRelic("Membership Card")) {
        retVal = applyDiscountToRelic(retVal, 0.5F);
    }

    r.price = retVal;
}
```

### The Courier 효과 요약

**유물:**
- 구매 시 즉시 새 랜덤 유물로 교체
- 등급은 `rollRelicTier()`로 재결정 (Common 48%, Uncommon 34%, Rare 18%)
- 상점 전용 유물 (OldCoin, SmilingMask, MawBank, Courier) 제외
- 가격 재계산 (±5% 변동 + 할인 적용)

**포션:**
- 구매 시 즉시 새 랜덤 포션으로 교체
- 등급 완전 랜덤
- 가격 재계산 (±5% 변동 + 할인 적용)

**카드:**
- 교체 없음 (일반 상점과 동일)

**카드 제거:**
- 영향 없음

---

## Ascension 16+ 변경사항

Ascension 16 이상에서는 상점 가격이 **10% 증가**합니다:

```java
// ShopScreen.java:233-235
if (AbstractDungeon.ascensionLevel >= 16) {
    applyDiscount(1.1F, false);  // 110% 가격 = 10% 증가
}
```

**중요:** `affectPurge = false`이므로 카드 제거 비용은 증가하지 않습니다.

**Ascension 16+ 가격 테이블 (할인 전, 변동 전):**

| 상품 | 등급 | 기본 가격 | A16+ 가격 |
|-----|------|---------|----------|
| 카드 | Common | 50 | 55 |
| 카드 | Uncommon | 75 | 82 |
| 카드 | Rare | 150 | 165 |
| 무색 카드 | Uncommon | 90 (75×1.2) | 99 |
| 무색 카드 | Rare | 180 (150×1.2) | 198 |
| 유물 | Common | 150 | 165 |
| 유물 | Uncommon | 250 | 275 |
| 유물 | Rare | 300 | 330 |
| 유물 | Shop | 150 | 165 |
| 포션 | Common | 50 | 55 |
| 포션 | Uncommon | 75 | 82 |
| 포션 | Rare | 100 | 110 |
| **카드 제거** | - | **75** | **75 (변화 없음)** |

**할인 적용 순서:**

1. Ascension 16+ 가격 증가 (×1.1)
2. The Courier 할인 (×0.8)
3. Membership Card 할인 (×0.5)

**최종 가격 계산 (Courier + Membership):**
- A15 이하: 기본 가격 × 0.8 × 0.5 = 기본 가격 × 0.4
- **A16+: 기본 가격 × 1.1 × 0.8 × 0.5 = 기본 가격 × 0.44**

---

## 수정 방법

### 1. 카드 가격 변경

카드 가격을 수정하려면 `AbstractCard.getPrice()`를 패치합니다:

```java
@SpirePatch(
    clz = AbstractCard.class,
    method = "getPrice"
)
public static class CardPricePatch {
    @SpirePostfixPatch
    public static int Postfix(int __result, AbstractCard.CardRarity rarity) {
        // 모든 카드 가격을 50% 할인
        return __result / 2;
    }
}
```

### 2. 유물 등급 확률 변경

유물 등급 확률을 수정하려면 `ShopScreen.rollRelicTier()`를 패치합니다:

```java
@SpirePatch(
    clz = ShopScreen.class,
    method = "rollRelicTier"
)
public static class RelicTierPatch {
    @SpirePrefixPatch
    public static SpireReturn<AbstractRelic.RelicTier> Prefix() {
        int roll = AbstractDungeon.merchantRng.random(99);

        // 수정된 확률: Rare 50%, Uncommon 30%, Common 20%
        if (roll < 50) {
            return SpireReturn.Return(AbstractRelic.RelicTier.RARE);
        }
        if (roll < 80) {
            return SpireReturn.Return(AbstractRelic.RelicTier.UNCOMMON);
        }
        return SpireReturn.Return(AbstractRelic.RelicTier.COMMON);
    }
}
```

### 3. 상점 상품 개수 변경

상점에 더 많은 카드나 유물을 추가하려면 `ShopScreen.init()`를 패치합니다:

```java
@SpirePatch(
    clz = ShopScreen.class,
    method = "initRelics"
)
public static class MoreRelicsPatch {
    @SpirePostfixPatch
    public static void Postfix(ShopScreen __instance) {
        // 유물 2개 추가 (총 5개)
        for (int i = 0; i < 2; i++) {
            AbstractRelic tempRelic = AbstractDungeon.returnRandomRelicEnd(
                ShopScreen.rollRelicTier()
            );
            StoreRelic relic = new StoreRelic(
                tempRelic,
                __instance.relics.size(),
                __instance
            );
            __instance.relics.add(relic);
        }
    }
}
```

### 4. 카드 제거 비용 고정

카드 제거 비용을 고정하려면 `ShopScreen.purgeCard()`를 패치합니다:

```java
@SpirePatch(
    clz = ShopScreen.class,
    method = "purgeCard"
)
public static class FixedPurgeCostPatch {
    @SpirePrefixPatch
    public static void Prefix() {
        // 카드 제거 비용을 항상 50 골드로 고정
        ShopScreen.purgeCost = 50;
        ShopScreen.actualPurgeCost = 50;
    }
}
```

### 5. 무료 상점 모드

모든 상품을 무료로 만들려면 `ShopScreen.init()`를 패치합니다:

```java
@SpirePatch(
    clz = ShopScreen.class,
    method = "init"
)
public static class FreeShopPatch {
    @SpirePostfixPatch
    public static void Postfix(ShopScreen __instance) {
        // 모든 카드 무료
        for (AbstractCard c : __instance.coloredCards) {
            c.price = 0;
        }
        for (AbstractCard c : __instance.colorlessCards) {
            c.price = 0;
        }

        // 모든 유물 무료
        for (StoreRelic r : __instance.relics) {
            r.price = 0;
        }

        // 모든 포션 무료
        for (StorePotion p : __instance.potions) {
            p.price = 0;
        }

        // 카드 제거 무료
        ShopScreen.actualPurgeCost = 0;
    }
}
```

### 6. 특정 카드만 상점에 표시

원하는 카드만 상점에 나오도록 하려면 `Merchant` 생성자를 패치합니다:

```java
@SpirePatch(
    clz = Merchant.class,
    method = SpirePatch.CONSTRUCTOR
)
public static class CustomCardPoolPatch {
    @SpirePostfixPatch
    public static void Postfix(Merchant __instance) {
        // cards1을 비우고 원하는 카드만 추가
        ArrayList<AbstractCard> cards1 =
            (ArrayList<AbstractCard>) ReflectionHacks.getPrivate(
                __instance,
                Merchant.class,
                "cards1"
            );
        cards1.clear();

        // 원하는 카드 추가
        cards1.add(new Strike_Red());
        cards1.add(new Defend_Red());
        cards1.add(new Bash());
        cards1.add(new Anger());
        cards1.add(new Armaments());
    }
}
```

### 7. The Courier 효과 확장

The Courier 효과를 카드에도 적용하려면 카드 구매 로직을 패치합니다:

```java
@SpirePatch(
    clz = ShopScreen.class,
    method = "purchaseCard"
)
public static class CourierCardRefreshPatch {
    @SpirePostfixPatch
    public static void Postfix(ShopScreen __instance, AbstractCard card) {
        if (AbstractDungeon.player.hasRelic("The Courier")) {
            // 구매한 카드를 새 카드로 교체
            AbstractCard newCard = AbstractDungeon.getCardFromPool(
                AbstractDungeon.rollRarity(),
                card.type,
                true
            ).makeCopy();

            int index = __instance.coloredCards.indexOf(card);
            if (index >= 0) {
                __instance.coloredCards.set(index, newCard);
                newCard.price = AbstractCard.getPrice(newCard.rarity);
            }
        }
    }
}
```

### 8. Ascension 16+ 가격 증가 비율 변경

가격 증가 비율을 수정하려면 `ShopScreen.init()`를 패치합니다:

```java
@SpirePatch(
    clz = ShopScreen.class,
    method = "init"
)
public static class CustomAscensionPricePatch {
    @SpireInsertPatch(
        locator = AscensionLocator.class
    )
    public static SpireReturn<Void> Insert(ShopScreen __instance) {
        if (AbstractDungeon.ascensionLevel >= 16) {
            // 10% 대신 25% 증가
            __instance.applyDiscount(1.25F, false);
            return SpireReturn.Return(null);
        }
        return SpireReturn.Continue();
    }

    private static class AscensionLocator extends SpireInsertLocator {
        @Override
        public int[] Locate(CtBehavior ctMethodToPatch) throws Exception {
            Matcher finalMatcher = new Matcher.FieldAccessMatcher(
                AbstractDungeon.class,
                "ascensionLevel"
            );
            return LineFinder.findInOrder(ctMethodToPatch, finalMatcher);
        }
    }
}
```

### 9. 카드 등급 확률 변경

상점 카드 등급 확률을 수정하려면 `ShopRoom.getCardRarity()`를 패치합니다:

```java
@SpirePatch(
    clz = ShopRoom.class,
    method = SpirePatch.CONSTRUCTOR
)
public static class ShopCardRarityPatch {
    @SpirePostfixPatch
    public static void Postfix(ShopRoom __instance) {
        // Rare 20%, Uncommon 40%, Common 40%
        __instance.baseRareCardChance = 20;
        __instance.baseUncommonCardChance = 40;
    }
}
```

### 10. 상점 진입 시 골드 추가

상점 진입 시 보너스 골드를 지급하려면 `ShopRoom.onPlayerEntry()`를 패치합니다:

```java
@SpirePatch(
    clz = ShopRoom.class,
    method = "onPlayerEntry"
)
public static class BonusGoldPatch {
    @SpirePostfixPatch
    public static void Postfix(ShopRoom __instance) {
        // 상점 진입 시 100 골드 추가
        AbstractDungeon.player.gainGold(100);

        // 플래시 효과
        AbstractDungeon.effectList.add(
            new GainGoldTextEffect(100)
        );
    }
}
```

---

## 관련 클래스

### 핵심 클래스

- **`ShopRoom`** (`com.megacrit.cardcrawl.rooms.ShopRoom`)
  - 상점 룸 클래스
  - 카드 등급 확률 설정
  - Merchant 생성 및 관리

- **`Merchant`** (`com.megacrit.cardcrawl.shop.Merchant`)
  - 상인 NPC 클래스
  - 카드 풀 생성 (cards1, cards2)
  - ShopScreen 초기화 호출

- **`ShopScreen`** (`com.megacrit.cardcrawl.shop.ShopScreen`)
  - 상점 UI 및 로직 클래스
  - 유물/포션 생성
  - 가격 설정 및 할인 적용
  - 카드 제거 시스템
  - 구매 로직

### 상품 클래스

- **`StoreRelic`** (`com.megacrit.cardcrawl.shop.StoreRelic`)
  - 상점 유물 래퍼 클래스
  - 유물 구매 로직
  - The Courier 교체 시스템
  - Membership Card/Smiling Mask 효과

- **`StorePotion`** (`com.megacrit.cardcrawl.shop.StorePotion`)
  - 상점 포션 래퍼 클래스
  - 포션 구매 로직
  - The Courier 교체 시스템
  - Sozu 유물 효과

### 가격 관련

- **`AbstractCard`** (`com.megacrit.cardcrawl.cards.AbstractCard`)
  - `getPrice(CardRarity)` - 카드 기본 가격 반환

- **`AbstractRelic`** (`com.megacrit.cardcrawl.relics.AbstractRelic`)
  - `getPrice()` - 유물 기본 가격 반환

- **`AbstractPotion`** (`com.megacrit.cardcrawl.potions.AbstractPotion`)
  - `getPrice()` - 포션 기본 가격 반환

### 유물 클래스

- **`Courier`** (`com.megacrit.cardcrawl.relics.Courier`)
  - 상점 가격 20% 할인
  - 유물/포션 구매 시 자동 교체

- **`MembershipCard`** (`com.megacrit.cardcrawl.relics.MembershipCard`)
  - 상점 가격 50% 할인

- **`SmilingMask`** (`com.megacrit.cardcrawl.relics.SmilingMask`)
  - 카드 제거 비용 고정 50 골드

- **`Sozu`** (`com.megacrit.cardcrawl.relics.Sozu`)
  - 포션 획득 불가 (상점 구매 포함)

### 던전 관련

- **`AbstractDungeon`** (`com.megacrit.cardcrawl.dungeons.AbstractDungeon`)
  - `rollRarity()` - 카드 등급 결정
  - `getCardFromPool()` - 카드 풀에서 카드 획득
  - `getColorlessCardFromPool()` - 무색 카드 획득
  - `returnRandomRelicEnd()` - 유물 획득
  - `returnRandomPotion()` - 포션 획득
  - `merchantRng` - 상점 전용 RNG

- **`AbstractRoom`** (`com.megacrit.cardcrawl.rooms.AbstractRoom`)
  - `getCardRarity(int, boolean)` - 등급 확률 계산
  - `baseRareCardChance` / `baseUncommonCardChance` - 기본 확률

### RNG

- **`merchantRng`** (`AbstractDungeon.merchantRng`)
  - 상점 모든 랜덤 요소에 사용
  - 카드 등급, 유물 등급, 가격 변동
  - 세일 카드 선택

- **`cardRng`** (`AbstractDungeon.cardRng`)
  - 카드 등급 결정 시 사용 (`rollRarity()`)
