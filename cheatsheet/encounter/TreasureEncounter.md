# 보물 상자 시스템 (Treasure Encounter)

## 목차

1. [시스템 개요](#시스템-개요)
2. [호출 흐름](#호출-흐름)
3. [상자 타입 선택 시스템](#상자-타입-선택-시스템)
4. [일반 보물 상자 (Normal Chest)](#일반-보물-상자-normal-chest)
5. [보스 보물 상자 (Boss Chest)](#보스-보물-상자-boss-chest)
6. [보상 생성 메커니즘](#보상-생성-메커니즘)
7. [유물 상호작용](#유물-상호작용)
8. [금화 보상 시스템](#금화-보상-시스템)
9. [상자 타입별 확률 테이블](#상자-타입별-확률-테이블)
10. [수정 방법](#수정-방법)
11. [관련 클래스](#관련-클래스)

---

## 시스템 개요

Slay the Spire의 보물 상자 시스템은 **일반 보물방(TreasureRoom)**과 **보스 보물방(TreasureRoomBoss)**으로 구성됩니다.

### 핵심 특징

- **3가지 일반 상자 타입**: Small, Medium, Large - 각각 다른 유물 등급과 금화 보상 확률
- **보스 상자**: 3개의 Boss Relic 중 1개 선택
- **확률적 타입 선택**: treasureRng를 사용한 결정론적 상자 타입 선택
- **유물 상호작용**: Matryoshka (추가 유물), TinyChest (전투 후 상자 출현) 등
- **금화 보상**: 각 상자 타입마다 고정 금액 + ±10% 변동
- **Cursed Chest**: 특정 확률로 저주 카드 획득

---

## 호출 흐름

### 일반 보물방 진입

```
TreasureRoom.onPlayerEntry()                          // Line 32-36
    └─> AbstractDungeon.getRandomChest()              // Line 607-616 (AbstractDungeon.java)
        ├─> Roll (0-99) using treasureRng
        ├─> [0-49]: new SmallChest()                  // 50%
        ├─> [50-82]: new MediumChest()                // 33%
        └─> [83-99]: new LargeChest()                 // 17%
```

### 보스 보물방 진입

```
TreasureRoomBoss.onPlayerEntry()                     // Line 58-65
    └─> new BossChest()
        └─> BossChest Constructor                     // Line 24-46
            ├─> Act 1-3: Generate 3 Boss Relics       // Line 34-36
            └─> Act 4 + Blight Chests Mod: Generate Blights  // Line 40-44
```

### 상자 열기 (일반)

```
AbstractChest.update()                                // Line 140-147
    └─> Click Detection
        └─> keyRequirement()                          // Line 51-54 (AbstractChest.java)
            └─> open(false)                           // Line 78-128
                ├─> Trigger Relic.onChestOpen(false)  // Line 80-82
                ├─> Add Gold Reward                    // Line 85-92
                ├─> Add Cursed Card (if cursed)       // Line 94-97
                ├─> Add Relic Based on relicReward     // Line 103-116
                ├─> Add Sapphire Key (if conditions)   // Line 118-121
                ├─> Trigger Relic.onChestOpenAfter()   // Line 123-125
                └─> combatRewardScreen.open()          // Line 127
```

### 상자 열기 (보스)

```
BossChest.open(true)                                  // Line 50-65
    └─> Act 1-3 Path:
        ├─> Trigger Relic.onChestOpen(true)           // Line 52-56 (except Matryoshka)
        ├─> Play CHEST_OPEN sound                     // Line 59
        └─> AbstractDungeon.bossRelicScreen.open(relics)  // Line 60
    └─> Act 4 + Blight Chests Path:
        ├─> Play CHEST_OPEN sound                     // Line 62
        └─> AbstractDungeon.bossRelicScreen.openBlight(blights)  // Line 63
```

---

## 상자 타입 선택 시스템

### AbstractDungeon.getRandomChest() (AbstractDungeon.java:607-616)

```java
public static AbstractChest getRandomChest() {
  int roll = treasureRng.random(0, 99);              // 0-99 롤

  if (roll < smallChestChance)                       // 0-49 (50%)
    return (AbstractChest)new SmallChest();
  if (roll < mediumChestChance + smallChestChance) { // 50-82 (33%)
    return (AbstractChest)new MediumChest();
  }
  return (AbstractChest)new LargeChest();           // 83-99 (17%)
}
```

### 상자 타입 확률 설정

**모든 막(Exordium, TheCity, TheBeyond) 공통**:

```java
// Exordium.java:130-131
smallChestChance = 50;
mediumChestChance = 33;

// TheCity.java:100-101
smallChestChance = 50;
mediumChestChance = 33;

// TheBeyond.java:91-92
smallChestChance = 50;
mediumChestChance = 33;
```

**확률 분포**:
- Small Chest: **50%** (roll < 50)
- Medium Chest: **33%** (roll < 83, roll >= 50)
- Large Chest: **17%** (roll >= 83)

### RNG 시드 활용

- `AbstractDungeon.treasureRng`: 상자 타입 선택용 시드
- 게임 시작 시 고정된 시드로 초기화
- 세이브/로드 시에도 동일한 시드 사용 → 재현 가능

---

## 일반 보물 상자 (Normal Chest)

### Small Chest (SmallChest.java:9-23)

```java
public SmallChest() {
  this.img = ImageMaster.S_CHEST;
  this.openedImg = ImageMaster.S_CHEST_OPEN;

  this.hb = new Hitbox(256.0F * Settings.scale, 200.0F * Settings.scale);
  this.hb.move(CHEST_LOC_X, CHEST_LOC_Y - 150.0F * Settings.scale);

  this.COMMON_CHANCE = 75;         // 75% Common
  this.UNCOMMON_CHANCE = 25;       // 25% Uncommon
  this.RARE_CHANCE = 0;            // 0% Rare
  this.GOLD_CHANCE = 50;           // 50% 금화 획득
  this.GOLD_AMT = 25;              // 25 Gold

  randomizeReward();
}
```

### Medium Chest (MediumChest.java:9-23)

```java
public MediumChest() {
  this.img = ImageMaster.M_CHEST;
  this.openedImg = ImageMaster.M_CHEST_OPEN;

  this.hb = new Hitbox(256.0F * Settings.scale, 270.0F * Settings.scale);
  this.hb.move(CHEST_LOC_X, CHEST_LOC_Y - 90.0F * Settings.scale);

  this.COMMON_CHANCE = 35;         // 35% Common
  this.UNCOMMON_CHANCE = 50;       // 50% Uncommon
  this.RARE_CHANCE = 15;           // 15% Rare
  this.GOLD_CHANCE = 35;           // 35% 금화 획득
  this.GOLD_AMT = 50;              // 50 Gold

  randomizeReward();
}
```

### Large Chest (LargeChest.java:9-23)

```java
public LargeChest() {
  this.img = ImageMaster.L_CHEST;
  this.openedImg = ImageMaster.L_CHEST_OPEN;

  this.hb = new Hitbox(340.0F * Settings.scale, 200.0F * Settings.scale);
  this.hb.move(CHEST_LOC_X, CHEST_LOC_Y - 120.0F * Settings.scale);

  this.COMMON_CHANCE = 0;          // 0% Common
  this.UNCOMMON_CHANCE = 75;       // 75% Uncommon
  this.RARE_CHANCE = 25;           // 25% Rare
  this.GOLD_CHANCE = 50;           // 50% 금화 획득
  this.GOLD_AMT = 75;              // 75 Gold

  randomizeReward();
}
```

---

## 보스 보물 상자 (Boss Chest)

### BossChest 생성 (BossChest.java:24-46)

```java
public BossChest() {
  this.img = ImageMaster.BOSS_CHEST;
  this.openedImg = ImageMaster.BOSS_CHEST_OPEN;

  this.hb = new Hitbox(256.0F * Settings.scale, 200.0F * Settings.scale);
  this.hb.move(CHEST_LOC_X, CHEST_LOC_Y - 100.0F * Settings.scale);

  if (AbstractDungeon.actNum < 4 || !AbstractPlayer.customMods.contains("Blight Chests")) {

    this.relics.clear();
    for (int i = 0; i < 3; i++) {
      this.relics.add(AbstractDungeon.returnRandomRelic(AbstractRelic.RelicTier.BOSS));
    }
  }
  else {

    this.blights.clear();
    this.blights.add(BlightHelper.getRandomBlight());
    ArrayList<String> exclusion = new ArrayList<>();
    exclusion.add(((AbstractBlight)this.blights.get(0)).blightID);
    this.blights.add(BlightHelper.getRandomChestBlight(exclusion));
  }
}
```

### Boss Relic 선택 화면

- **3개의 Boss Relic** 생성 (중복 없음, relicRng 사용)
- **선택식 보상**: 1개만 선택 가능
- **Matryoshka 제외**: 보스 상자에서는 Matryoshka 효과 발동 안 됨 (Line 53)

### Blight Chests 모드 (Act 4)

- Act 4 + "Blight Chests" 모드 활성화 시
- Boss Relic 대신 **2개의 Blight** 제공
- 첫 번째는 무작위, 두 번째는 첫 번째와 다른 Blight

---

## 보상 생성 메커니즘

### AbstractChest.randomizeReward() (AbstractChest.java:57-73)

```java
public void randomizeReward() {
  int roll = AbstractDungeon.treasureRng.random(0, 99);


  if (roll < this.GOLD_CHANCE) {
    this.goldReward = true;
  }


  if (roll < this.COMMON_CHANCE) {
    this.relicReward = RelicReward.COMMON_RELIC;
  } else if (roll < this.UNCOMMON_CHANCE + this.COMMON_CHANCE) {
    this.relicReward = RelicReward.UNCOMMON_RELIC;
  } else {
    this.relicReward = RelicReward.RARE_RELIC;
  }
}
```

### 보상 유형 결정 알고리즘

**1단계: 금화 보상 결정**
- Roll < GOLD_CHANCE → 금화 보상 활성화

**2단계: 유물 등급 결정**
- Roll < COMMON_CHANCE → Common Relic
- Roll < COMMON_CHANCE + UNCOMMON_CHANCE → Uncommon Relic
- Else → Rare Relic

**예시 (MediumChest)**:
```
Roll = 30:
  - goldReward = true (30 < 35)
  - relicReward = COMMON_RELIC (30 < 35)
  → 금화 + Common Relic

Roll = 50:
  - goldReward = false (50 >= 35)
  - relicReward = UNCOMMON_RELIC (50 < 35 + 50)
  → Uncommon Relic만

Roll = 90:
  - goldReward = false (90 >= 35)
  - relicReward = RARE_RELIC (90 >= 85)
  → Rare Relic만
```

### 보상 추가 (AbstractChest.open:103-116)

```java
switch (this.relicReward) {
  case COMMON_RELIC:
    AbstractDungeon.getCurrRoom().addRelicToRewards(AbstractRelic.RelicTier.COMMON);
    break;
  case UNCOMMON_RELIC:
    AbstractDungeon.getCurrRoom().addRelicToRewards(AbstractRelic.RelicTier.UNCOMMON);
    break;
  case RARE_RELIC:
    AbstractDungeon.getCurrRoom().addRelicToRewards(AbstractRelic.RelicTier.RARE);
    break;
  default:
    logger.info("ERROR: Unspecified reward: " + this.relicReward.name());
    break;
}
```

---

## 유물 상호작용

### Matryoshka (마트료시카) (Matryoshka.java:21-40)

**효과**: 일반 상자를 열 때 추가 유물 획득 (2회)

```java
public void onChestOpen(boolean bossChest) {
  if (!bossChest &&                              // 보스 상자 제외
    this.counter > 0) {
    this.counter--;

    flash();
    addToTop((AbstractGameAction)new RelicAboveCreatureAction((AbstractCreature)AbstractDungeon.player, this));

    if (AbstractDungeon.relicRng.randomBoolean(0.75F)) {
      AbstractDungeon.getCurrRoom().addRelicToRewards(AbstractRelic.RelicTier.COMMON);
    } else {
      AbstractDungeon.getCurrRoom().addRelicToRewards(AbstractRelic.RelicTier.UNCOMMON);
    }
    if (this.counter == 0) {
      setCounter(-2);
      this.description = this.DESCRIPTIONS[2];
    } else {
      this.description = this.DESCRIPTIONS[1];
    }
  }
}
```

**발동 조건**:
- 일반 상자 (Small, Medium, Large)만 해당
- 보스 상자에서는 발동 안 됨 (BossChest.java:53 참조)

**추가 유물 확률**:
- Common: 75%
- Uncommon: 25%

**카운터 관리**:
- 초기값: 2
- 상자 열 때마다 -1
- 0이 되면 효과 소진 (counter = -2로 설정)

### TinyChest (작은 보물상자) (TinyChest.java:10-33)

**효과**: 4전투마다 ? 방이 보물방으로 변경

```java
public static final int ROOM_COUNT = 4;

public TinyChest() {
  super("Tiny Chest", "tinyChest.png", AbstractRelic.RelicTier.COMMON, AbstractRelic.LandingSound.SOLID);
  this.counter = -1;
}

public void onEquip() {
  this.counter = 0;
}

public boolean canSpawn() {
  return (Settings.isEndless || AbstractDungeon.floorNum <= 35);
}
```

**작동 방식**:
- 전투 승리 시 counter 증가
- counter가 4에 도달하면 다음 ? 방이 보물방으로 생성
- counter는 MapGenerator 단계에서 체크됨

**출현 제한**:
- 35층 이하에서만 출현 (Endless 모드 제외)
- 이후에는 의미가 없어지기 때문

### Cursed Chest (저주 상자)

**AbstractChest.cursed 플래그** (AbstractChest.java:94-97):

```java
if (this.cursed) {
  AbstractDungeon.topLevelEffects.add(new ShowCardAndObtainEffect(
        AbstractDungeon.returnRandomCurse(), this.hb.cX, this.hb.cY));
}
```

**발동 조건**:
- 특정 이벤트나 모드에서 `cursed = true` 설정
- 상자를 열면 무작위 저주 카드 획득

---

## 금화 보상 시스템

### 금화 생성 (AbstractChest.open:85-92)

```java
if (this.goldReward) {
  if (Settings.isDailyRun) {
    AbstractDungeon.getCurrRoom().addGoldToRewards(this.GOLD_AMT);
  } else {
    AbstractDungeon.getCurrRoom().addGoldToRewards(
        Math.round(AbstractDungeon.treasureRng.random(this.GOLD_AMT * 0.9F, this.GOLD_AMT * 1.1F)));
  }
}
```

### 금화 변동폭

**Daily Run**:
- 고정 금액 (GOLD_AMT)

**Normal Run**:
- 기본 금액의 ±10% 변동
- `treasureRng.random(GOLD_AMT * 0.9F, GOLD_AMT * 1.1F)`

### 상자별 금화 보상

| 상자 타입 | 기본 금액 | Normal Run 범위 | 획득 확률 |
|-----------|-----------|-----------------|-----------|
| Small     | 25        | 22-28           | 50%       |
| Medium    | 50        | 45-55           | 35%       |
| Large     | 75        | 67-83           | 50%       |
| Boss      | 0         | -               | 0%        |

---

## 상자 타입별 확률 테이블

### 일반 상자 타입 확률

| 타입   | 출현 확률 | Roll 범위 |
|--------|-----------|-----------|
| Small  | 50%       | 0-49      |
| Medium | 33%       | 50-82     |
| Large  | 17%       | 83-99     |

### Small Chest 보상 확률

| 보상 타입        | 확률 | Roll 범위 |
|------------------|------|-----------|
| Common Relic     | 75%  | 0-74      |
| Uncommon Relic   | 25%  | 75-99     |
| Rare Relic       | 0%   | -         |
| 금화 (25G ±10%) | 50%  | 0-49      |

### Medium Chest 보상 확률

| 보상 타입        | 확률 | Roll 범위 |
|------------------|------|-----------|
| Common Relic     | 35%  | 0-34      |
| Uncommon Relic   | 50%  | 35-84     |
| Rare Relic       | 15%  | 85-99     |
| 금화 (50G ±10%) | 35%  | 0-34      |

### Large Chest 보상 확률

| 보상 타입        | 확률 | Roll 범위 |
|------------------|------|-----------|
| Common Relic     | 0%   | -         |
| Uncommon Relic   | 75%  | 0-74      |
| Rare Relic       | 25%  | 75-99     |
| 금화 (75G ±10%) | 50%  | 0-49      |

### Boss Chest 보상

| 보상 타입           | 개수 | 선택 방식   |
|---------------------|------|-------------|
| Boss Relic          | 3개  | 1개 선택    |
| Blight (Act 4 모드) | 2개  | 1개 선택    |

---

## 수정 방법

### 1. 상자 타입 확률 변경

**패치 위치**: `AbstractDungeon.getRandomChest()`

```java
@SpirePatch(
    clz = AbstractDungeon.class,
    method = "getRandomChest"
)
public static class ChestTypeChancePatch {
    @SpirePostfixPatch
    public static AbstractChest Postfix(AbstractChest __result) {
        int roll = AbstractDungeon.treasureRng.random(0, 99);

        // 커스텀 확률 (Large 50%, Medium 30%, Small 20%)
        if (roll < 20) {
            return new SmallChest();
        } else if (roll < 50) {
            return new MediumChest();
        } else {
            return new LargeChest();
        }
    }
}
```

### 2. 유물 등급 확률 수정 (SmallChest 예시)

**패치 위치**: `SmallChest` 생성자

```java
@SpirePatch(
    clz = SmallChest.class,
    method = SpirePatch.CONSTRUCTOR
)
public static class SmallChestRewardPatch {
    @SpirePostfixPatch
    public static void Postfix(SmallChest __instance) {
        // Small Chest에서도 Rare 유물 획득 가능하게 변경
        __instance.COMMON_CHANCE = 50;      // 50%
        __instance.UNCOMMON_CHANCE = 30;    // 30%
        __instance.RARE_CHANCE = 20;        // 20%

        // 금화 획득 확률 증가
        __instance.GOLD_CHANCE = 75;        // 75%
        __instance.GOLD_AMT = 40;           // 40 Gold

        // 보상 재생성
        __instance.randomizeReward();
    }
}
```

### 3. Boss Chest에서 유물 개수 변경

**패치 위치**: `BossChest` 생성자

```java
@SpirePatch(
    clz = BossChest.class,
    method = SpirePatch.CONSTRUCTOR
)
public static class BossChestRelicCountPatch {
    @SpirePostfixPatch
    public static void Postfix(BossChest __instance) {
        if (AbstractDungeon.actNum < 4) {
            __instance.relics.clear();

            // 5개의 Boss Relic 제공
            for (int i = 0; i < 5; i++) {
                __instance.relics.add(
                    AbstractDungeon.returnRandomRelic(AbstractRelic.RelicTier.BOSS)
                );
            }
        }
    }
}
```

### 4. 상자 오픈 시 추가 보상

**패치 위치**: `AbstractChest.open()`

```java
@SpirePatch(
    clz = AbstractChest.class,
    method = "open",
    paramtypez = {boolean.class}
)
public static class ChestExtraRewardPatch {
    @SpirePostfixPatch
    public static void Postfix(AbstractChest __instance, boolean bossChest) {
        // 모든 상자에서 포션 추가
        AbstractDungeon.getCurrRoom().addPotionToRewards(
            AbstractDungeon.returnRandomPotion(true)
        );

        // 50% 확률로 카드 보상 추가
        if (AbstractDungeon.treasureRng.randomBoolean(0.5f)) {
            AbstractDungeon.getCurrRoom().addCardToRewards();
        }
    }
}
```

### 5. Matryoshka 횟수 및 확률 변경

**패치 위치**: `Matryoshka.onChestOpen()`

```java
@SpirePatch(
    clz = Matryoshka.class,
    method = "onChestOpen",
    paramtypez = {boolean.class}
)
public static class MatryoshkaBuffPatch {
    @SpirePrefixPatch
    public static SpireReturn<Void> Prefix(Matryoshka __instance, boolean bossChest) {
        if (!bossChest && __instance.counter > 0) {
            __instance.counter--;
            __instance.flash();

            // 항상 Uncommon 이상 보장
            if (AbstractDungeon.relicRng.randomBoolean(0.5f)) {
                AbstractDungeon.getCurrRoom().addRelicToRewards(AbstractRelic.RelicTier.UNCOMMON);
            } else {
                AbstractDungeon.getCurrRoom().addRelicToRewards(AbstractRelic.RelicTier.RARE);
            }

            if (__instance.counter == 0) {
                __instance.setCounter(-2);
            }

            return SpireReturn.Return(null);
        }
        return SpireReturn.Continue();
    }
}

@SpirePatch(
    clz = Matryoshka.class,
    method = SpirePatch.CONSTRUCTOR
)
public static class MatryoshkaCounterPatch {
    @SpirePostfixPatch
    public static void Postfix(Matryoshka __instance) {
        // 5회 사용 가능하게 변경
        __instance.counter = 5;
    }
}
```

### 6. TinyChest 카운터 수정

**패치 위치**: `TinyChest` 클래스

```java
@SpirePatch(
    clz = TinyChest.class,
    method = SpirePatch.CLASS
)
public static class TinyChestRoomCountPatch {
    public static SpireField<Integer> newRoomCount = new SpireField<>(() -> 3);
}

@SpirePatch(
    clz = TinyChest.class,
    method = "onEquip"
)
public static class TinyChestEquipPatch {
    @SpirePostfixPatch
    public static void Postfix(TinyChest __instance) {
        // 3전투마다 보물방 출현
        TinyChestRoomCountPatch.newRoomCount.set(__instance, 3);
    }
}
```

### 7. 금화 보상 배율 변경

**패치 위치**: `AbstractChest.open()`

```java
@SpirePatch(
    clz = AbstractChest.class,
    method = "open",
    paramtypez = {boolean.class}
)
public static class ChestGoldMultiplierPatch {
    @SpireInsertPatch(
        locator = GoldRewardLocator.class
    )
    public static SpireReturn<Void> Insert(AbstractChest __instance, boolean bossChest) {
        if (__instance.goldReward) {
            int baseGold = __instance.GOLD_AMT;

            // 3배 금화 획득
            int multipliedGold = baseGold * 3;

            if (!Settings.isDailyRun) {
                multipliedGold = Math.round(
                    AbstractDungeon.treasureRng.random(
                        multipliedGold * 0.9F,
                        multipliedGold * 1.1F
                    )
                );
            }

            AbstractDungeon.getCurrRoom().addGoldToRewards(multipliedGold);
            return SpireReturn.Return(null);
        }
        return SpireReturn.Continue();
    }
}

private static class GoldRewardLocator extends SpireInsertLocator {
    @Override
    public int[] Locate(CtBehavior ctMethodToPatch) throws Exception {
        Matcher matcher = new Matcher.FieldAccessMatcher(
            AbstractChest.class, "goldReward"
        );
        return LineFinder.findInOrder(ctMethodToPatch, matcher);
    }
}
```

### 8. 커스텀 상자 타입 추가

**새로운 상자 클래스 생성**:

```java
public class MegaChest extends AbstractChest {
    public MegaChest() {
        this.img = ImageMaster.loadImage("modResources/images/chests/megaChest.png");
        this.openedImg = ImageMaster.loadImage("modResources/images/chests/megaChestOpen.png");

        this.hb = new Hitbox(400.0F * Settings.scale, 300.0F * Settings.scale);
        this.hb.move(CHEST_LOC_X, CHEST_LOC_Y - 100.0F * Settings.scale);

        // 초강력 보상
        this.COMMON_CHANCE = 0;          // 0% Common
        this.UNCOMMON_CHANCE = 50;       // 50% Uncommon
        this.RARE_CHANCE = 50;           // 50% Rare
        this.GOLD_CHANCE = 100;          // 100% 금화
        this.GOLD_AMT = 150;             // 150 Gold

        randomizeReward();
    }
}
```

**AbstractDungeon.getRandomChest() 패치**:

```java
@SpirePatch(
    clz = AbstractDungeon.class,
    method = "getRandomChest"
)
public static class CustomChestPatch {
    @SpirePostfixPatch
    public static AbstractChest Postfix(AbstractChest __result) {
        int roll = AbstractDungeon.treasureRng.random(0, 99);

        // 5% 확률로 MegaChest 출현
        if (roll >= 95) {
            return new MegaChest();
        }

        return __result;
    }
}
```

### 9. Cursed Chest 확률 조정

**패치 위치**: 상자 생성 시점

```java
@SpirePatch(
    clz = AbstractChest.class,
    method = "randomizeReward"
)
public static class CursedChestChancePatch {
    @SpirePostfixPatch
    public static void Postfix(AbstractChest __instance) {
        // 20% 확률로 저주 상자
        if (AbstractDungeon.treasureRng.randomBoolean(0.2f)) {
            __instance.cursed = true;
        }
    }
}
```

### 10. Sapphire Key 추가 조건 변경

**패치 위치**: `AbstractChest.open()` (Line 118-121)

```java
@SpirePatch(
    clz = AbstractChest.class,
    method = "open",
    paramtypez = {boolean.class}
)
public static class SapphireKeyConditionPatch {
    @SpireInsertPatch(
        locator = SapphireKeyLocator.class,
        localvars = {}
    )
    public static SpireReturn<Void> Insert(AbstractChest __instance, boolean bossChest) {
        // 커스텀 조건: Large Chest 이상에서만 Sapphire Key 출현
        if (Settings.isFinalActAvailable && !Settings.hasSapphireKey) {
            if (__instance instanceof LargeChest || __instance instanceof BossChest) {
                AbstractDungeon.getCurrRoom().addSapphireKey(
                    (AbstractDungeon.getCurrRoom()).rewards.get(
                        (AbstractDungeon.getCurrRoom()).rewards.size() - 1
                    )
                );
                return SpireReturn.Return(null);
            }
        }
        return SpireReturn.Continue();
    }
}

private static class SapphireKeyLocator extends SpireInsertLocator {
    @Override
    public int[] Locate(CtBehavior ctMethodToPatch) throws Exception {
        Matcher matcher = new Matcher.FieldAccessMatcher(
            Settings.class, "hasSapphireKey"
        );
        return LineFinder.findInOrder(ctMethodToPatch, matcher);
    }
}
```

---

## 관련 클래스

### 핵심 클래스

| 클래스 | 경로 | 설명 |
|--------|------|------|
| **TreasureRoom** | `com.megacrit.cardcrawl.rooms.TreasureRoom` | 일반 보물방 |
| **TreasureRoomBoss** | `com.megacrit.cardcrawl.rooms.TreasureRoomBoss` | 보스 보물방 |
| **AbstractChest** | `com.megacrit.cardcrawl.rewards.chests.AbstractChest` | 상자 기본 클래스 |
| **SmallChest** | `com.megacrit.cardcrawl.rewards.chests.SmallChest` | 작은 상자 (50% 출현) |
| **MediumChest** | `com.megacrit.cardcrawl.rewards.chests.MediumChest` | 중간 상자 (33% 출현) |
| **LargeChest** | `com.megacrit.cardcrawl.rewards.chests.LargeChest` | 큰 상자 (17% 출현) |
| **BossChest** | `com.megacrit.cardcrawl.rewards.chests.BossChest` | 보스 상자 (3 Boss Relics) |

### 유물 클래스

| 클래스 | 경로 | 설명 |
|--------|------|------|
| **Matryoshka** | `com.megacrit.cardcrawl.relics.Matryoshka` | 상자에서 추가 유물 획득 (2회) |
| **TinyChest** | `com.megacrit.cardcrawl.relics.TinyChest` | 4전투마다 보물방 출현 |

### RNG 관련

| 필드 | 클래스 | 설명 |
|------|--------|------|
| **treasureRng** | `AbstractDungeon` | 상자 타입 선택, 금화 변동, 보상 생성용 |
| **relicRng** | `AbstractDungeon` | 유물 선택용 |

### 보상 시스템

| 클래스 | 경로 | 설명 |
|--------|------|------|
| **AbstractRoom** | `com.megacrit.cardcrawl.rooms.AbstractRoom` | addRelicToRewards(), addGoldToRewards() |
| **RewardItem** | `com.megacrit.cardcrawl.rewards.RewardItem` | 보상 아이템 표시 |

### 효과 클래스

| 클래스 | 경로 | 설명 |
|--------|------|------|
| **ChestShineEffect** | `com.megacrit.cardcrawl.vfx.ChestShineEffect` | 상자 반짝임 효과 |
| **SpookyChestEffect** | `com.megacrit.cardcrawl.vfx.scene.SpookyChestEffect` | 일반 상자 이펙트 |
| **SpookierChestEffect** | `com.megacrit.cardcrawl.vfx.scene.SpookierChestEffect` | 보스 상자 이펙트 |
| **BossChestShineEffect** | `com.megacrit.cardcrawl.vfx.BossChestShineEffect` | 보스 상자 반짝임 |

---

## 추가 참고사항

### 상자 위치 계산

```java
// AbstractChest.java:25-26
public static final float CHEST_LOC_X = Settings.WIDTH / 2.0F + 348.0F * Settings.scale;
public static final float CHEST_LOC_Y = AbstractDungeon.floorY + 192.0F * Settings.scale;
```

- 모든 상자는 동일한 X 좌표 사용
- Y 좌표는 상자 크기에 따라 오프셋 조정

### 상자 클릭 감지

```java
// AbstractChest.update():140-147
public void update() {
  this.hb.update();
  if (((this.hb.hovered && InputHelper.justClickedLeft) || CInputActionSet.select.isJustPressed())
      && !AbstractDungeon.isScreenUp && !this.isOpen &&
      keyRequirement()) {
    InputHelper.justClickedLeft = false;
    open(false);
  }
}
```

- Hitbox 호버 + 클릭 감지
- 화면이 열려있지 않을 때만 작동
- keyRequirement() 체크 (기본적으로 항상 true)

### 보스 상자 특수 처리

- Matryoshka 효과 발동 안 됨 (BossChest.java:53)
- bossChest 파라미터로 구분
- bossRelicScreen을 통한 선택식 보상

### Act 4 Blight Chests

- "Blight Chests" 모드 + Act 4 조건
- Boss Relic 대신 2개의 Blight 제공
- 첫 번째와 다른 Blight 보장 (exclusion list)

### treasureRng 일관성

- 모든 상자 관련 확률에 treasureRng 사용
- 세이브/로드 시 동일한 시드로 재현 가능
- 금화 변동도 treasureRng로 처리
