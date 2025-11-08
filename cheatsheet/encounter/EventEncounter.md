# 이벤트 조우 (Event Encounter) 시스템

지도상 "?" 심볼로 진입했을 때의 이벤트 시스템 전체 분석

---

## 📑 목차

1. [시스템 개요](#시스템-개요)
2. [호출 흐름](#호출-흐름)
3. [이벤트 방 확률 시스템](#이벤트-방-확률-시스템)
4. [이벤트 vs Shrine 선택](#이벤트-vs-shrine-선택)
5. [일반 이벤트 선택](#일반-이벤트-선택)
6. [Shrine 이벤트 선택](#shrine-이벤트-선택)
7. [Act별 이벤트 풀](#act별-이벤트-풀)
8. [Special One-Time Events](#special-one-time-events)
9. [조건부 이벤트](#조건부-이벤트)
10. [이벤트-전투 하이브리드](#이벤트-전투-하이브리드)
11. [누적 확률 시스템](#누적-확률-시스템)
12. [유물 영향](#유물-영향)
13. [수정 방법](#수정-방법)
14. [관련 클래스](#관련-클래스)

---

## 시스템 개요

"?" 방은 **2단계 RNG**를 통해 결정됩니다:

1. **1단계: 방 타입 결정** - Event vs Elite vs Monster vs Shop vs Treasure
2. **2단계: 이벤트 내용 결정** - Shrine vs Normal Event → 구체적 이벤트 선택

### 핵심 특징

1. **누적 확률**: 나오지 않을수록 확률 증가
2. **조건부 등장**: 골드, HP, 유물 등 조건 체크
3. **One-Time Events**: 한 번만 등장 (특수 조건)
4. **하이브리드 이벤트**: 이벤트 + 전투 조합
5. **Shrine 우선**: shrineChance에 따라 Shrine 먼저 체크
6. **리스트 소진**: 선택된 이벤트는 리스트에서 제거

---

## 호출 흐름

### 전체 프로세스

```
맵에서 "?" 방 선택
    ↓
EventHelper.roll() 호출 (방 타입 결정)
    ↓
RoomResult 반환: EVENT | ELITE | MONSTER | SHOP | TREASURE
    ↓
RoomResult == EVENT인 경우:
    ↓
EventRoom 생성
    ↓
EventRoom.onPlayerEntry() 호출 (Line 19)
    ↓
AbstractDungeon.generateEvent(eventRng) 호출 (Line 22)
    ↓
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
Shrine vs Event 선택:
    ↓
shrineChance 확률로 분기 (Line 2358)
    ↓
true → getShrine(rng) 호출
false → getEvent(rng) 호출
    ↓
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
조건 필터링:
    ↓
eventList 또는 shrineList에서 조건 충족하는 이벤트만 추출
    ↓
specialOneTimeEventList 조건 체크 (getShrine만)
    ↓
랜덤 선택 후 리스트에서 제거
    ↓
EventHelper.getEvent(key) 호출 (Line 294)
    ↓
AbstractEvent 객체 생성 및 반환
    ↓
event.onEnterRoom() 호출 (Line 23)
```

### 1단계: 방 타입 결정 (EventHelper.roll)

**파일**: `EventHelper.java:106-251`

**호출 시점**: 맵 생성 시 "?" 노드의 실제 타입 결정

```java
public static RoomResult roll(Random eventRng) {
    saveFilePreviousChances = getChances();
    float roll = eventRng.random();  // 0.0 ~ 1.0 랜덤

    // Tiny Chest 유물 체크
    boolean forceChest = false;
    if (AbstractDungeon.player.hasRelic("Tiny Chest")) {
        AbstractRelic r = AbstractDungeon.player.getRelic("Tiny Chest");
        r.counter++;
        if (r.counter == 4) {
            r.counter = 0;
            r.flash();
            forceChest = true;  // 4번째마다 무조건 보물
        }
    }

    // 확률 크기 계산
    int eliteSize = 0;
    if (ModHelper.isModEnabled("DeadlyEvents")) {
        eliteSize = (int)(ELITE_CHANCE * 100.0F);
    }
    if (AbstractDungeon.floorNum < 6) {
        eliteSize = 0;  // 6층 미만에서는 엘리트 0%
    }

    int monsterSize = (int)(MONSTER_CHANCE * 100.0F);
    int shopSize = (int)(SHOP_CHANCE * 100.0F);
    if (AbstractDungeon.getCurrRoom() instanceof ShopRoom) {
        shopSize = 0;  // 이전 방이 상점이면 0%
    }
    int treasureSize = (int)(TREASURE_CHANCE * 100.0F);

    // 100칸 배열 생성 (0-99 인덱스)
    RoomResult[] possibleResults = new RoomResult[100];
    Arrays.fill(possibleResults, RoomResult.EVENT);  // 기본값: EVENT

    int fillIndex = 0;

    // DeadlyEvents 모드: 엘리트 2번 추가
    if (ModHelper.isModEnabled("DeadlyEvents")) {
        Arrays.fill(possibleResults, Math.min(99, fillIndex),
                    Math.min(100, fillIndex + eliteSize), RoomResult.ELITE);
        fillIndex += eliteSize;
        Arrays.fill(possibleResults, Math.min(99, fillIndex),
                    Math.min(100, fillIndex + eliteSize), RoomResult.ELITE);
        fillIndex += eliteSize;
    }

    // 몬스터 구간 채우기
    Arrays.fill(possibleResults, Math.min(99, fillIndex),
                Math.min(100, fillIndex + monsterSize), RoomResult.MONSTER);
    fillIndex += monsterSize;

    // 상점 구간 채우기
    Arrays.fill(possibleResults, Math.min(99, fillIndex),
                Math.min(100, fillIndex + shopSize), RoomResult.SHOP);
    fillIndex += shopSize;

    // 보물 구간 채우기
    Arrays.fill(possibleResults, Math.min(99, fillIndex),
                Math.min(100, fillIndex + treasureSize), RoomResult.TREASURE);

    // 최종 선택
    RoomResult choice = possibleResults[(int)(roll * 100.0F)];

    // Tiny Chest 강제
    if (forceChest) {
        choice = RoomResult.TREASURE;
    }

    // 확률 업데이트 (누적 시스템)
    if (choice == RoomResult.ELITE) {
        ELITE_CHANCE = 0.0F;
        if (ModHelper.isModEnabled("DeadlyEvents")) {
            ELITE_CHANCE = 0.1F;
        }
    } else {
        ELITE_CHANCE += 0.1F;  // 10% 증가
    }

    if (choice == RoomResult.MONSTER) {
        // Juzu Bracelet 체크
        if (AbstractDungeon.player.hasRelic("Juzu Bracelet")) {
            AbstractDungeon.player.getRelic("Juzu Bracelet").flash();
            choice = RoomResult.EVENT;  // 몬스터 → 이벤트 변환
        }
        MONSTER_CHANCE = 0.1F;
    } else {
        MONSTER_CHANCE += 0.1F;  // 10% 증가
    }

    if (choice == RoomResult.SHOP) {
        SHOP_CHANCE = 0.03F;
    } else {
        SHOP_CHANCE += 0.03F;  // 3% 증가
    }

    // Mimic Infestation Blight (Endless 모드)
    if (Settings.isEndless && AbstractDungeon.player.hasBlight("MimicInfestation")) {
        if (choice == RoomResult.TREASURE) {
            if (AbstractDungeon.player.hasRelic("Juzu Bracelet")) {
                AbstractDungeon.player.getRelic("Juzu Bracelet").flash();
                choice = RoomResult.EVENT;
            } else {
                choice = RoomResult.ELITE;  // 보물 → 엘리트 변환
            }
            TREASURE_CHANCE = 0.02F;
            if (ModHelper.isModEnabled("DeadlyEvents")) {
                TREASURE_CHANCE += 0.02F;
            }
        }
    } else if (choice == RoomResult.TREASURE) {
        TREASURE_CHANCE = 0.02F;
    } else {
        TREASURE_CHANCE += 0.02F;  // 2% 증가
        if (ModHelper.isModEnabled("DeadlyEvents")) {
            TREASURE_CHANCE += 0.02F;  // 4% 증가
        }
    }

    return choice;
}
```

**중요 포인트**:
- **100칸 배열 방식**: 0-99 인덱스에 확률에 따라 RoomResult 배정
- **누적 확률**: 나오지 않으면 다음 "?" 방에서 확률 증가
- **유물 영향**: Tiny Chest, Juzu Bracelet이 결과 변경 가능

### 2단계: EventRoom 진입

**파일**: `EventRoom.java:19-24`

```java
public void onPlayerEntry() {
    AbstractDungeon.overlayMenu.proceedButton.hide();
    Random eventRngDuplicate = new Random(Settings.seed, AbstractDungeon.eventRng.counter);
    this.event = AbstractDungeon.generateEvent(eventRngDuplicate);
    this.event.onEnterRoom();
}
```

**특징**:
- **eventRngDuplicate**: 시드 + counter로 재현 가능
- **generateEvent()**: 2단계 이벤트 선택 시작
- **onEnterRoom()**: 이벤트 UI 초기화

---

## 이벤트 방 확률 시스템

### 기본 확률 상수

**파일**: `EventHelper.java:74-98`

```java
private static final float BASE_ELITE_CHANCE = 0.1F;
private static final float BASE_MONSTER_CHANCE = 0.1F;
private static final float BASE_SHOP_CHANCE = 0.03F;
private static final float BASE_TREASURE_CHANCE = 0.02F;

private static final float RAMP_ELITE_CHANCE = 0.1F;
private static final float RAMP_MONSTER_CHANCE = 0.1F;
private static final float RAMP_SHOP_CHANCE = 0.03F;
private static final float RAMP_TREASURE_CHANCE = 0.02F;

private static final float RESET_ELITE_CHANCE = 0.0F;
private static final float RESET_MONSTER_CHANCE = 0.1F;
private static final float RESET_SHOP_CHANCE = 0.03F;
private static final float RESET_TREASURE_CHANCE = 0.02F;

private static float ELITE_CHANCE = 0.1F;
private static float MONSTER_CHANCE = 0.1F;
private static float SHOP_CHANCE = 0.03F;
public static float TREASURE_CHANCE = 0.02F;
```

### 초기 확률

| 타입 | 초기 확률 | 증가량 | 리셋 값 |
|------|----------|--------|---------|
| ELITE | 10% | +10% | 0% (DeadlyEvents: 10%) |
| MONSTER | 10% | +10% | 10% |
| SHOP | 3% | +3% | 3% |
| TREASURE | 2% | +2% | 2% (DeadlyEvents: +4%) |
| **EVENT** | **75%** | - | - |

**EVENT 확률 계산**:
```
EVENT% = 100% - (ELITE% + MONSTER% + SHOP% + TREASURE%)
초기: 100% - (10% + 10% + 3% + 2%) = 75%
```

### 누적 확률 예시

| "?" 횟수 | ELITE | MONSTER | SHOP | TREASURE | EVENT |
|---------|-------|---------|------|----------|-------|
| 1회차 | 10% | 10% | 3% | 2% | 75% |
| 2회차 (EVENT 나옴) | 20% | 20% | 6% | 4% | 50% |
| 3회차 (EVENT 나옴) | 30% | 30% | 9% | 6% | 25% |
| 4회차 (MONSTER 나옴) | 40% | 10% (리셋) | 12% | 8% | 30% |
| 5회차 (ELITE 나옴) | 0% (리셋) | 20% | 15% | 10% | 55% |

**특수 조건**:
- **6층 미만**: ELITE_CHANCE = 0% 고정
- **DeadlyEvents 모드**: ELITE가 배열에 2번 들어감 (2배 확률)

---

## 이벤트 vs Shrine 선택

### generateEvent() 메서드

**파일**: `AbstractDungeon.java:2357-2374`

```java
public static AbstractEvent generateEvent(Random rng) {
    if (rng.random(1.0F) < shrineChance) {
        // Shrine 우선 체크
        if (!shrineList.isEmpty() || !specialOneTimeEventList.isEmpty())
            return getShrine(rng);
        if (!eventList.isEmpty()) {
            return getEvent(rng);
        }
        logger.info("No events or shrines left");
        return null;
    }

    // 일반 이벤트 체크
    AbstractEvent retVal = getEvent(rng);
    if (retVal == null) {
        return getShrine(rng);  // eventList 비었으면 shrine으로 폴백
    }
    return retVal;
}
```

### shrineChance 값

**설정 위치**: `AbstractDungeon`의 `initializeLevelSpecificChances()` 호출 시

**기본값**:
- **Act 1**: 0.0F (0%)
- **Act 2**: 0.0F (0%)
- **Act 3**: 0.0F (0%)

**중요**: 기본 게임에서는 shrineChance가 0이므로, **항상 일반 이벤트 먼저 체크**합니다.

### 선택 흐름

```
shrineChance < random()?
    ↓
YES (shrineChance가 더 작음 = 일반 확률)
    → getEvent(rng) 호출
    → eventList에서 선택
    → 비어있으면 getShrine(rng) 폴백

NO (random이 더 작음 = shrine 확률)
    → getShrine(rng) 호출
    → shrineList + specialOneTimeEventList에서 선택
    → 비어있으면 getEvent(rng) 폴백
```

**실제 동작** (shrineChance = 0):
```
0.0 < random(0.0-1.0) → 항상 true
→ 항상 getEvent() 먼저 호출
→ eventList 비었을 때만 getShrine() 호출
```

---

## 일반 이벤트 선택

### getEvent() 메서드

**파일**: `AbstractDungeon.java:2444-2496`

```java
public static AbstractEvent getEvent(Random rng) {
    ArrayList<String> tmp = new ArrayList<>();

    // 조건 필터링
    for (String e : eventList) {
        switch (e) {
            case "Dead Adventurer":
                if (floorNum > 6) {
                    tmp.add(e);
                }
                continue;

            case "Mushrooms":
                if (floorNum > 6) {
                    tmp.add(e);
                }
                continue;

            case "The Moai Head":
                if (!player.hasRelic("Golden Idol") && player.currentHealth / player.maxHealth > 0.5F) {
                    continue;  // 조건 미충족 시 skip
                }
                tmp.add(e);
                continue;

            case "The Cleric":
                if (player.gold >= 35) {
                    tmp.add(e);
                }
                continue;

            case "Beggar":
                if (player.gold >= 75) {
                    tmp.add(e);
                }
                continue;

            case "Colosseum":
                if (currMapNode != null && currMapNode.y > map.size() / 2) {
                    tmp.add(e);
                }
                continue;
        }
        // 조건 없는 이벤트는 무조건 추가
        tmp.add(e);
    }

    // eventList가 모두 조건 미충족인 경우
    if (tmp.isEmpty()) {
        return getShrine(rng);
    }

    // 랜덤 선택
    String tmpKey = tmp.get(rng.random(tmp.size() - 1));
    eventList.remove(tmpKey);
    logger.info("Removed event: " + tmpKey + " from pool.");

    return EventHelper.getEvent(tmpKey);
}
```

### 조건부 이벤트 필터링

| 이벤트 ID | 조건 | 코드 위치 |
|-----------|------|----------|
| "Dead Adventurer" | floorNum > 6 | Line 2448-2451 |
| "Mushrooms" | floorNum > 6 | Line 2453-2456 |
| "The Moai Head" | hasRelic("Golden Idol") OR currentHealth/maxHealth > 0.5 | Line 2458-2463 |
| "The Cleric" | gold >= 35 | Line 2466-2469 |
| "Beggar" | gold >= 75 | Line 2471-2474 |
| "Colosseum" | currMapNode.y > map.size() / 2 (후반부) | Line 2476-2479 |

**중요**:
- 조건 미충족 시 **해당 이벤트만 제외**, 다른 이벤트는 여전히 가능
- **tmp.isEmpty()**: 모든 이벤트가 조건 미충족 시 Shrine으로 폴백

---

## Shrine 이벤트 선택

### getShrine() 메서드

**파일**: `AbstractDungeon.java:2378-2442`

```java
public static AbstractEvent getShrine(Random rng) {
    ArrayList<String> tmp = new ArrayList<>();
    tmp.addAll(shrineList);  // 일반 shrine 먼저 추가

    // Special one-time events 조건 체크
    for (String e : specialOneTimeEventList) {
        switch (e) {
            case "Fountain of Cleansing":
                if (player.isCursed()) {
                    tmp.add(e);
                }
                continue;

            case "Designer":
                if ((id.equals("TheCity") || id.equals("TheBeyond")) && player.gold >= 75) {
                    tmp.add(e);
                }
                continue;

            case "Duplicator":
                if (id.equals("TheCity") || id.equals("TheBeyond")) {
                    tmp.add(e);
                }
                continue;

            case "FaceTrader":
                if (id.equals("TheCity") || id.equals("Exordium")) {
                    tmp.add(e);
                }
                continue;

            case "Knowing Skull":
                if (id.equals("TheCity") && player.currentHealth > 12) {
                    tmp.add(e);
                }
                continue;

            case "N'loth":
                if ((id.equals("TheCity") || id.equals("TheCity")) && player.relics.size() >= 2) {
                    tmp.add(e);
                }
                continue;

            case "The Joust":
                if (id.equals("TheCity") && player.gold >= 50) {
                    tmp.add(e);
                }
                continue;

            case "The Woman in Blue":
                if (player.gold >= 50) {
                    tmp.add(e);
                }
                continue;

            case "SecretPortal":
                if (CardCrawlGame.playtime >= 800.0F && id.equals("TheBeyond")) {
                    tmp.add(e);
                }
                continue;
        }
        // 조건 없는 special event는 무조건 추가
        tmp.add(e);
    }

    // 랜덤 선택
    String tmpKey = tmp.get(rng.random(tmp.size() - 1));
    shrineList.remove(tmpKey);
    specialOneTimeEventList.remove(tmpKey);
    logger.info("Removed event: " + tmpKey + " from pool.");

    return EventHelper.getEvent(tmpKey);
}
```

**중요**:
- **shrineList와 specialOneTimeEventList 모두 체크**
- **조건부 필터링**: special one-time만 조건 존재
- **2개 리스트에서 제거**: 중복 등장 방지

---

## Act별 이벤트 풀

### Act 1 (Exordium) 이벤트

**파일**: `Exordium.java:261-273`

```java
protected void initializeEventList() {
    eventList.add("Big Fish");
    eventList.add("The Cleric");
    eventList.add("Dead Adventurer");
    eventList.add("Golden Idol");
    eventList.add("Golden Wing");
    eventList.add("World of Goop");
    eventList.add("Liars Game");
    eventList.add("Living Wall");
    eventList.add("Mushrooms");
    eventList.add("Scrap Ooze");
    eventList.add("Shining Light");
}
```

| 이벤트 ID | 클래스 | 조건 | 특징 |
|-----------|--------|------|------|
| "Big Fish" | BigFish | 없음 | Donut, Banana 선택 |
| "The Cleric" | Cleric | gold >= 35 | 골드로 회복 구매 |
| "Dead Adventurer" | DeadAdventurer | floorNum > 6 | 전투 or 탐색 선택 |
| "Golden Idol" | GoldenIdolEvent | 없음 | 유물 vs 저주 |
| "Golden Wing" | GoldenWing | 없음 | 카드 제거 기회 |
| "World of Goop" | GoopPuddle | 없음 | 골드 획득 |
| "Liars Game" | Sssserpent | 없음 | 도박 게임 |
| "Living Wall" | LivingWall | 없음 | 카드 제거 (체력 소모) |
| "Mushrooms" | Mushrooms | floorNum > 6 | 공격/회복 버섯 선택 |
| "Scrap Ooze" | ScrapOoze | 없음 | 전투 or 유물 |
| "Shining Light" | ShiningLight | 없음 | 카드 업그레이드 or 제거 |

### Act 1 Shrine 리스트

**파일**: `Exordium.java:276-283`

```java
protected void initializeShrineList() {
    shrineList.add("Match and Keep!");
    shrineList.add("Golden Shrine");
    shrineList.add("Transmorgrifier");
    shrineList.add("Purifier");
    shrineList.add("Upgrade Shrine");
    shrineList.add("Wheel of Change");
}
```

| Shrine ID | 클래스 | 효과 |
|-----------|--------|------|
| "Match and Keep!" | GremlinMatchGame | 카드 매칭 게임 |
| "Golden Shrine" | GoldShrine | 골드로 저주 or 카드 |
| "Transmorgrifier" | Transmogrifier | 카드 변환 |
| "Purifier" | PurificationShrine | 카드 제거 |
| "Upgrade Shrine" | UpgradeShrine | 카드 업그레이드 |
| "Wheel of Change" | GremlinWheelGame | 룰렛 게임 |

### Act 2 (TheCity) 이벤트

**파일**: `TheCity.java:218-232`

```java
protected void initializeEventList() {
    eventList.add("Addict");
    eventList.add("Back to Basics");
    eventList.add("Beggar");
    eventList.add("Colosseum");
    eventList.add("Cursed Tome");
    eventList.add("Drug Dealer");
    eventList.add("Forgotten Altar");
    eventList.add("Ghosts");
    eventList.add("Masked Bandits");
    eventList.add("Nest");
    eventList.add("The Library");
    eventList.add("The Mausoleum");
    eventList.add("Vampires");
}
```

| 이벤트 ID | 클래스 | 조건 | 특징 |
|-----------|--------|------|------|
| "Addict" | Addict | 없음 | 유물 매매 |
| "Back to Basics" | BackToBasics | 없음 | 카드 제거 (전부 or 일부) |
| "Beggar" | Beggar | gold >= 75 | 골드로 유물 구매 |
| "Colosseum" | Colosseum | y > map.size()/2 | **전투 이벤트** |
| "Cursed Tome" | CursedTome | 없음 | 유물 + 저주 |
| "Drug Dealer" | DrugDealer | 없음 | 포션 관련 |
| "Forgotten Altar" | ForgottenAltar | 없음 | (Act 2에도 있음) |
| "Ghosts" | Ghosts | 없음 | 유령과 선택 |
| "Masked Bandits" | MaskedBandits | 없음 | **전투 이벤트** |
| "Nest" | Nest | 없음 | 유물 or 저주 |
| "The Library" | TheLibrary | 없음 | 카드 선택 |
| "The Mausoleum" | TheMausoleum | 없음 | 유물 탐색 |
| "Vampires" | Vampires | 없음 | **전투 이벤트** |

### Act 2 Shrine 리스트

**파일**: `TheCity.java:244-251`

```java
protected void initializeShrineList() {
    shrineList.add("Match and Keep!");
    shrineList.add("Wheel of Change");
    shrineList.add("Golden Shrine");
    shrineList.add("Transmorgrifier");
    shrineList.add("Purifier");
    shrineList.add("Upgrade Shrine");
}
```

**Act 1과 동일** (순서만 다름)

### Act 3 (TheBeyond) 이벤트

**파일**: `TheBeyond.java:207-215`

```java
protected void initializeEventList() {
    eventList.add("Falling");
    eventList.add("MindBloom");
    eventList.add("The Moai Head");
    eventList.add("Mysterious Sphere");
    eventList.add("SensoryStone");
    eventList.add("Tomb of Lord Red Mask");
    eventList.add("Winding Halls");
}
```

| 이벤트 ID | 클래스 | 조건 | 특징 |
|-----------|--------|------|------|
| "Falling" | Falling | 없음 | 위험한 선택들 |
| "MindBloom" | MindBloom | 없음 | **전투 이벤트** (보스 선택) |
| "The Moai Head" | MoaiHead | hasRelic("Golden Idol") OR HP > 50% | Golden Idol 교환 |
| "Mysterious Sphere" | MysteriousSphere | 없음 | 구체 선택 |
| "SensoryStone" | SensoryStone | 없음 | 기억 선택 (캐릭터별) |
| "Tomb of Lord Red Mask" | TombRedMask | 없음 | 유물 획득 + 저주 |
| "Winding Halls" | WindingHalls | 없음 | 미로 선택 |

### Act 3 Shrine 리스트

**파일**: `TheBeyond.java:227-234`

```java
protected void initializeShrineList() {
    shrineList.add("Match and Keep!");
    shrineList.add("Wheel of Change");
    shrineList.add("Golden Shrine");
    shrineList.add("Transmorgrifier");
    shrineList.add("Purifier");
    shrineList.add("Upgrade Shrine");
}
```

**Act 1, 2와 동일**

---

## Special One-Time Events

### 정의 및 초기화

**파일**: `AbstractDungeon.java`

**초기화 위치**: `initializeSpecialOneTimeEventList()` (각 던전마다 호출)

**특징**:
- **한 런에 1회만 등장**
- **조건부 활성화**
- **Shrine 선택 시에만 체크**

### 공통 Special Events

| Event ID | 조건 | Act | 효과 |
|----------|------|-----|------|
| "Fountain of Cleansing" | player.isCursed() | 전체 | 저주 제거 |
| "Designer" | (TheCity OR TheBeyond) AND gold >= 75 | 2, 3 | 카드 디자인 |
| "Duplicator" | TheCity OR TheBeyond | 2, 3 | 카드 복제 |
| "FaceTrader" | TheCity OR Exordium | 1, 2 | 유물 교환 |
| "Knowing Skull" | TheCity AND currentHealth > 12 | 2 | HP로 유물 교환 |
| "N'loth" | (TheCity OR TheCity) AND relics.size() >= 2 | 2 | 유물 교환 |
| "The Joust" | TheCity AND gold >= 50 | 2 | 도박 |
| "The Woman in Blue" | gold >= 50 | 전체 | 포션 구매 |
| "SecretPortal" | playtime >= 800 AND TheBeyond | 3 | Act 4 포털 |

### SecretPortal 특이사항

**파일**: `AbstractDungeon.java:2425-2428`

```java
case "SecretPortal":
    if (CardCrawlGame.playtime >= 800.0F && id.equals("TheBeyond")) {
        tmp.add(e);
    }
    continue;
```

**조건**:
- **playtime >= 800초** (13분 20초)
- **TheBeyond** (Act 3)

**효과**:
- Act 4 (TheEnding)로 진입할 수 있는 포털
- **Act 4 언락 필수 조건**

---

## 조건부 이벤트

### 층수 조건

| 이벤트 | 조건 | 이유 |
|--------|------|------|
| "Dead Adventurer" | floorNum > 6 | 초반에는 너무 어려움 |
| "Mushrooms" | floorNum > 6 | 초반 밸런스 |

### 골드 조건

| 이벤트 | 최소 골드 | 용도 |
|--------|----------|------|
| "The Cleric" | 35 | 회복 구매 |
| "Beggar" | 75 | 유물 구매 |
| "Designer" | 75 | 카드 디자인 |
| "The Joust" | 50 | 도박 참가 |
| "The Woman in Blue" | 50 | 포션 구매 |

### HP 조건

| 이벤트 | 조건 | 이유 |
|--------|------|------|
| "The Moai Head" | currentHealth/maxHealth > 0.5 OR hasRelic("Golden Idol") | HP 낮으면 위험 |
| "Knowing Skull" | currentHealth > 12 | HP 교환 최소치 |

### 맵 위치 조건

| 이벤트 | 조건 | 이유 |
|--------|------|------|
| "Colosseum" | currMapNode.y > map.size() / 2 | 후반부에만 |

### 유물 조건

| 이벤트 | 조건 | 이유 |
|--------|------|------|
| "The Moai Head" | hasRelic("Golden Idol") | Idol 교환 이벤트 |
| "N'loth" | relics.size() >= 2 | 유물 교환에 필요 |

### 상태 조건

| 이벤트 | 조건 | 이유 |
|--------|------|------|
| "Fountain of Cleansing" | player.isCursed() | 저주 제거용 |

---

## 이벤트-전투 하이브리드

특정 이벤트는 **전투로 이어질 수 있거나 전투가 필수**입니다.

### Masked Bandits

**파일**: `MaskedBandits.java:34-43`

```java
public MaskedBandits() {
    this.body = DESCRIPTIONS[4];

    this.roomEventText.addDialogOption(OPTIONS[0]);  // 골드 지불
    this.roomEventText.addDialogOption(OPTIONS[1]);  // 싸우기

    this.hasDialog = true;
    this.hasFocus = true;
    (AbstractDungeon.getCurrRoom()).monsters = MonsterHelper.getEncounter("Masked Bandits");
}
```

**선택지**:
1. **골드 지불**: 모든 골드 소실, 전투 회피
2. **싸우기**: 전투 시작, 승리 시 Red Mask 유물 + 골드

**전투 보상**:
```java
if (AbstractDungeon.player.hasRelic("Red Mask")) {
    AbstractDungeon.getCurrRoom().addRelicToRewards(new Circlet());
} else {
    AbstractDungeon.getCurrRoom().addRelicToRewards(new RedMask());
}
AbstractDungeon.getCurrRoom().addGoldToRewards(AbstractDungeon.miscRng.random(25, 35));
```

**코드 위치**: `MaskedBandits.java:68-82`

### Colosseum

**특징**:
- **3연속 전투** (Nob, 2 Slavers, Lagavulin)
- 각 전투 승리 시 보상 없음
- **3연속 승리 시만 최종 보상**

### Vampires

**선택지**:
1. **거부**: 전투 (5 Vampires)
2. **수락**: 체력 -30%, Bite 카드 획득

### MindBloom

**특징**:
- **보스 전투** 선택 가능
- Act 3 보스 중 하나와 싸움
- 보상 없음, 체험용

### Scrap Ooze

**선택지**:
1. **유물 획득**: 전투 없음
2. **돈 획득**: 전투 (Scrap Ooze)

### Dead Adventurer

**선택지에 따라**:
- 전투 발생 가능 (조건부)

---

## 누적 확률 시스템

### 확률 리셋 조건

**파일**: `EventHelper.java:201-247`

```java
if (choice == RoomResult.ELITE) {
    ELITE_CHANCE = 0.0F;  // 리셋
    if (ModHelper.isModEnabled("DeadlyEvents")) {
        ELITE_CHANCE = 0.1F;
    }
} else {
    ELITE_CHANCE += 0.1F;  // 증가
}
```

| 결과 | ELITE | MONSTER | SHOP | TREASURE |
|------|-------|---------|------|----------|
| ELITE 나옴 | 0% (리셋) | +10% | +3% | +2% |
| MONSTER 나옴 | +10% | 10% (리셋) | +3% | +2% |
| SHOP 나옴 | +10% | +10% | 3% (리셋) | +2% |
| TREASURE 나옴 | +10% | +10% | +3% | 2% (리셋) |
| **EVENT 나옴** | **+10%** | **+10%** | **+3%** | **+2%** |

**중요**:
- **EVENT는 확률 증가만 시킴** (리셋 없음)
- 계속 EVENT만 나오면 다른 것들의 확률이 계속 증가
- 결국 **EVENT 확률 감소** (100% - 나머지)

### 시뮬레이션 예시

```
"?" 1회차:
  ELITE 10%, MONSTER 10%, SHOP 3%, TREASURE 2%, EVENT 75%
  → EVENT 나옴

"?" 2회차:
  ELITE 20%, MONSTER 20%, SHOP 6%, TREASURE 4%, EVENT 50%
  → EVENT 나옴

"?" 3회차:
  ELITE 30%, MONSTER 30%, SHOP 9%, TREASURE 6%, EVENT 25%
  → MONSTER 나옴

"?" 4회차:
  ELITE 40%, MONSTER 10%, SHOP 12%, TREASURE 8%, EVENT 30%
  → ELITE 나옴

"?" 5회차:
  ELITE 0%, MONSTER 20%, SHOP 15%, TREASURE 10%, EVENT 55%
```

---

## 유물 영향

### Tiny Chest

**효과**: 4번째 "?" 방마다 **무조건 보물**

**코드 위치**: `EventHelper.java:116-124`

```java
boolean forceChest = false;
if (AbstractDungeon.player.hasRelic("Tiny Chest")) {
    AbstractRelic r = AbstractDungeon.player.getRelic("Tiny Chest");
    r.counter++;
    if (r.counter == 4) {
        r.counter = 0;
        r.flash();
        forceChest = true;
    }
}
```

**동작**:
- counter: 0 → 1 → 2 → 3 → **0 (trigger)**
- **forceChest = true**: 확률 무시하고 TREASURE 강제

### Juzu Bracelet

**효과**: MONSTER 결과를 **EVENT로 변경**

**코드 위치**: `EventHelper.java:211-214`

```java
if (choice == RoomResult.MONSTER) {
    if (AbstractDungeon.player.hasRelic("Juzu Bracelet")) {
        AbstractDungeon.player.getRelic("Juzu Bracelet").flash();
        choice = RoomResult.EVENT;
    }
    MONSTER_CHANCE = 0.1F;
}
```

**중요**:
- MONSTER_CHANCE는 **여전히 리셋됨**
- EVENT로 바뀌어도 **EVENT 확률 증가 없음**

### Golden Idol

**효과**: "The Moai Head" 이벤트 조건 완화

**코드 위치**: `AbstractDungeon.java:2458-2463`

```java
case "The Moai Head":
    if (!player.hasRelic("Golden Idol") && player.currentHealth / player.maxHealth > 0.5F) {
        continue;  // HP > 50% 필요
    }
    tmp.add(e);  // Idol 있으면 HP 조건 무시
    continue;
```

### Red Mask

**효과**: Masked Bandits 전투 보상 변경

**코드 위치**: `MaskedBandits.java:75-79`

```java
if (AbstractDungeon.player.hasRelic("Red Mask")) {
    AbstractDungeon.getCurrRoom().addRelicToRewards(new Circlet());
} else {
    AbstractDungeon.getCurrRoom().addRelicToRewards(new RedMask());
}
```

---

## 수정 방법

### 1. 특정 이벤트 확률 증가

**목표**: "Big Fish"가 더 자주 나오게 하기

```java
@SpirePatch(
    cls = "com.megacrit.cardcrawl.dungeons.Exordium",
    method = "initializeEventList"
)
public static class BigFishBoostPatch {
    @SpirePostfixPatch
    public static void Postfix(Exordium __instance) {
        // "Big Fish"를 여러 번 추가
        AbstractDungeon.eventList.add("Big Fish");
        AbstractDungeon.eventList.add("Big Fish");
        AbstractDungeon.eventList.add("Big Fish");
    }
}
```

**효과**: 리스트에 4개 존재 → 4배 높은 확률

### 2. 새로운 이벤트 추가

**Step 1**: 이벤트 클래스 작성

```java
public class MyCustomEvent extends AbstractEvent {
    public static final String ID = "MyCustomEvent";
    public static final String NAME = "My Custom Event";

    public MyCustomEvent() {
        this.body = "Event description here";
        this.roomEventText.addDialogOption("Option 1");
        this.roomEventText.addDialogOption("Option 2");
        this.hasDialog = true;
        this.hasFocus = true;
    }

    protected void buttonEffect(int buttonPressed) {
        switch (buttonPressed) {
            case 0:
                // Option 1 logic
                break;
            case 1:
                // Option 2 logic
                break;
        }
    }
}
```

**Step 2**: EventHelper.getEvent 패치

```java
@SpirePatch(
    cls = "com.megacrit.cardcrawl.helpers.EventHelper",
    method = "getEvent",
    paramtypez = { String.class }
)
public static class CustomEventHelperPatch {
    @SpirePrefixPatch
    public static SpireReturn<AbstractEvent> Prefix(String key) {
        if (key.equals("MyCustomEvent")) {
            return SpireReturn.Return(new MyCustomEvent());
        }
        return SpireReturn.Continue();
    }
}
```

**Step 3**: eventList에 추가

```java
@SpirePatch(
    cls = "com.megacrit.cardcrawl.dungeons.Exordium",
    method = "initializeEventList"
)
public static class AddCustomEventPatch {
    @SpirePostfixPatch
    public static void Postfix(Exordium __instance) {
        AbstractDungeon.eventList.add("MyCustomEvent");
    }
}
```

### 3. 이벤트 조건 변경

**목표**: "The Cleric" 골드 조건 제거

```java
@SpirePatch(
    cls = "com.megacrit.cardcrawl.dungeons.AbstractDungeon",
    method = "getEvent",
    paramtypez = { Random.class }
)
public static class RemoveClericConditionPatch {
    @SpireInsertPatch(locator = ClericConditionLocator.class)
    public static SpireReturn<Void> Insert(Random rng, @ByRef ArrayList<String>[] tmp) {
        // "The Cleric" 조건 무시하고 무조건 추가
        boolean hasCleric = false;
        for (String e : AbstractDungeon.eventList) {
            if (e.equals("The Cleric")) {
                tmp[0].add(e);
                hasCleric = true;
                break;
            }
        }

        // 조건 체크 스킵
        if (hasCleric) {
            return SpireReturn.Return(null);
        }

        return SpireReturn.Continue();
    }
}

private static class ClericConditionLocator extends SpireInsertLocator {
    public int[] Locate(CtBehavior ctMethodToPatch) throws Exception {
        Matcher matcher = new Matcher.MethodCallMatcher(
            AbstractPlayer.class, "gold"
        );
        return LineFinder.findInOrder(ctMethodToPatch, matcher);
    }
}
```

### 4. shrineChance 변경

**목표**: Shrine이 50% 확률로 나오게 하기

```java
@SpirePatch(
    cls = "com.megacrit.cardcrawl.dungeons.Exordium",
    method = "initializeLevelSpecificChances"
)
public static class IncreaseShrineChancePatch {
    @SpirePostfixPatch
    public static void Postfix(Exordium __instance) {
        AbstractDungeon.shrineChance = 0.5F;  // 50%
    }
}
```

### 5. 누적 확률 변경

**목표**: MONSTER 확률을 더 빠르게 증가시키기

```java
@SpirePatch(
    cls = "com.megacrit.cardcrawl.helpers.EventHelper",
    method = "roll",
    paramtypez = { Random.class }
)
public static class FastMonsterRampPatch {
    @SpireInsertPatch(locator = MonsterRampLocator.class)
    public static void Insert(Random eventRng) {
        // 원래 +0.1F → +0.2F로 변경
        try {
            Field field = EventHelper.class.getDeclaredField("MONSTER_CHANCE");
            field.setAccessible(true);
            float current = field.getFloat(null);
            field.setFloat(null, current + 0.1F);  // 추가 0.1F
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

private static class MonsterRampLocator extends SpireInsertLocator {
    public int[] Locate(CtBehavior ctMethodToPatch) throws Exception {
        Matcher matcher = new Matcher.FieldAccessMatcher(
            EventHelper.class, "MONSTER_CHANCE"
        );
        return LineFinder.findInOrder(ctMethodToPatch, matcher);
    }
}
```

### 6. 이벤트-전투 하이브리드 수정

**목표**: Masked Bandits 보상 개선

```java
@SpirePatch(
    cls = "com.megacrit.cardcrawl.events.city.MaskedBandits",
    method = "buttonEffect",
    paramtypez = { int.class }
)
public static class BetterMaskedBanditsRewardPatch {
    @SpireInsertPatch(locator = MaskedBanditsRewardLocator.class)
    public static void Insert(MaskedBandits __instance, int buttonPressed) {
        // 추가 보상
        AbstractDungeon.getCurrRoom().addCardReward(new RewardItem());
        AbstractDungeon.getCurrRoom().addPotionToRewards(
            AbstractDungeon.returnRandomPotion()
        );
    }
}
```

---

## 관련 클래스

### 핵심 클래스

| 클래스 | 경로 | 역할 |
|--------|------|------|
| **EventRoom** | `com.megacrit.cardcrawl.rooms.EventRoom` | 이벤트 방 |
| **EventHelper** | `com.megacrit.cardcrawl.helpers.EventHelper` | 이벤트 확률 및 선택 |
| **AbstractEvent** | `com.megacrit.cardcrawl.events.AbstractEvent` | 이벤트 베이스 클래스 |
| **RoomEventDialog** | `com.megacrit.cardcrawl.events.RoomEventDialog` | 이벤트 UI |

### 주요 메서드

#### EventHelper

| 메서드 | 코드 위치 | 설명 |
|--------|----------|------|
| `roll(Random)` | Line 110-251 | "?" 방 타입 결정 |
| `getEvent(String)` | Line 294-415 | key로 이벤트 객체 생성 |
| `resetProbabilities()` | Line 256-262 | 확률 초기화 |
| `getChances()` | Line 271-278 | 현재 확률 조회 |

#### AbstractDungeon

| 메서드 | 코드 위치 | 설명 |
|--------|----------|------|
| `generateEvent(Random)` | Line 2357-2374 | Shrine vs Event 선택 |
| `getEvent(Random)` | Line 2444-2496 | 일반 이벤트 선택 |
| `getShrine(Random)` | Line 2378-2442 | Shrine 이벤트 선택 |
| `initializeEventList()` | (각 Act) | 이벤트 리스트 초기화 |
| `initializeShrineList()` | (각 Act) | Shrine 리스트 초기화 |

#### EventRoom

| 메서드 | 코드 위치 | 설명 |
|--------|----------|------|
| `onPlayerEntry()` | Line 19-24 | 방 진입 시 이벤트 생성 |
| `update()` | Line 27-39 | 이벤트 업데이트 |

### 이벤트 클래스 (Act 1 예시)

| 클래스 | 경로 | 특징 |
|--------|------|------|
| **BigFish** | `com.megacrit.cardcrawl.events.exordium.BigFish` | Donut vs Banana |
| **Cleric** | `com.megacrit.cardcrawl.events.exordium.Cleric` | 골드로 회복 |
| **MaskedBandits** | `com.megacrit.cardcrawl.events.city.MaskedBandits` | 전투 이벤트 |
| **Colosseum** | `com.megacrit.cardcrawl.events.city.Colosseum` | 3연속 전투 |

### Enum

| Enum | 값 | 설명 |
|------|-----|------|
| **RoomResult** | EVENT, ELITE, MONSTER, SHOP, TREASURE | "?" 방 타입 |

---

## 참고사항

### 디버깅 로그

**파일**: `EventHelper.java:113, 133-137`

```
INFO: Rolling for room type... EVENT_RNG_COUNTER: X
INFO: ROLL: 0.xx
INFO: ELIT: 0.xx
INFO: MNST: 0.xx
INFO: SHOP: 0.xx
INFO: TRSR: 0.xx
```

**파일**: `AbstractDungeon.java:2439, 2493`

```
INFO: Removed event: [Event Name] from pool.
```

### 이벤트 리스트 소진

- **eventList 비었을 때**: `getShrine(rng)` 폴백
- **shrineList 비었을 때**: `getEvent(rng)` 폴백
- **둘 다 비었을 때**: null 반환 → "No events or shrines left"

**코드 위치**: `AbstractDungeon.java:2364-2365, 2487-2488`

### Endless 모드

**Mimic Infestation Blight**:
- TREASURE → ELITE 변환 (Juzu Bracelet 있으면 EVENT)

**코드 위치**: `EventHelper.java:226-239`

### DeadlyEvents 모드

**효과**:
- ELITE 확률 2배 (배열에 2번 추가)
- TREASURE 증가량 2배 (+4%)

**코드 위치**: `EventHelper.java:140-142, 160-174, 203-205, 235-237, 244-246`

---

## 작성 정보

- **작성일**: 2025-11-08
- **대상 버전**: Slay the Spire 01-23-2019 빌드
- **분석 범위**: 이벤트 조우 시스템 전체, 확률 시스템, 조건부 이벤트, 하이브리드 이벤트
