# 보스 전투 (Boss Battle) Encounter 시스템

지도상 "보스" 심볼로 진입했을 때의 전투 시스템 전체 분석

---

## 📑 목차

1. [시스템 개요](#시스템-개요)
2. [호출 흐름](#호출-흐름)
3. [보스 선택 메커니즘](#보스-선택-메커니즘)
4. [Act별 보스 풀](#act별-보스-풀)
5. [UnlockTracker 시스템](#unlocktracker-시스템)
6. [보스 키 관리](#보스-키-관리)
7. [보스 방 진입](#보스-방-진입)
8. [보상 시스템](#보상-시스템)
9. [Act 4 특수 구조](#act-4-특수-구조)
10. [수정 방법](#수정-방법)
11. [관련 클래스](#관련-클래스)

---

## 시스템 개요

보스 전투는 **던전 시작 시 미리 결정**되며, **UnlockTracker를 통한 unlock 순서** 또는 **Daily Run 랜덤**으로 선택됩니다.

### 핵심 특징

1. **사전 결정**: 던전 생성 시 `bossList`에 보스 3개 추가
2. **Unlock 우선순위**: 처음 보는 보스를 우선적으로 배치
3. **Daily Run 예외**: 모든 보스 셔플하여 랜덤 선택
4. **단일 선택**: bossList[0]만 사용, 나머지는 보스 아이콘용
5. **카드 보상**: 모든 카드가 **RARE 등급** 고정
6. **BGM 침묵**: 보스 방 진입 시 음악 중단

---

## 호출 흐름

### 전체 프로세스

```
던전 시작 (Exordium 생성자)
    ↓
initializeBoss() 호출 (Line 355)
    ↓
bossList에 보스 3개 추가 (unlock 우선 or 랜덤)
    ↓
setBoss(bossList.get(0)) 호출 (Line 356)
    ↓
bossKey에 보스 이름 저장 + 보스 아이콘 설정
    ↓
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
플레이어가 보스 방 진입
    ↓
MonsterRoomBoss.onPlayerEntry() 호출 (Line 19)
    ↓
CardCrawlGame.dungeon.getBoss() 호출 (Line 20)
    ↓
MonsterHelper.getEncounter(bossKey) 호출 (Line 2501)
    ↓
bossList.remove(0) 실행 (Line 24)
    ↓
CardCrawlGame.metricData.path_taken.add("BOSS") (Line 22)
    ↓
CardCrawlGame.music.silenceBGM() (Line 23)
    ↓
monsters.init() → 전투 시작 (Line 27)
```

### 단계별 상세 분석

#### 1단계: 던전 초기화

**파일**: `AbstractDungeon.java:346-365`

```java
public AbstractDungeon(String name, String levelId, AbstractPlayer p, ArrayList<String> newSpecialOneTimeEventList) {
    // ... 초기화 코드 ...

    dungeonTransitionSetup();
    generateMonsters();           // 일반/엘리트 몬스터 생성
    initializeBoss();             // ← 보스 풀 생성
    setBoss(bossList.get(0));     // ← 첫 번째 보스를 bossKey에 설정
    initializeEventList();
    initializeEventImg();
    initializeShrineList();
    initializeCardPools();
    // ...
}
```

**특징**:
- `generateMonsters()` **이후** `initializeBoss()` 호출
- `bossList.get(0)`을 **즉시** bossKey에 할당
- 맵 생성 전에 보스가 결정됨

#### 2단계: setBoss() 메서드

**파일**: `AbstractDungeon.java:420-476`

```java
private void setBoss(String key) {
    bossKey = key;  // 전역 static 변수에 저장

    // 기존 보스 아이콘 dispose
    if (DungeonMap.boss != null && DungeonMap.bossOutline != null) {
        DungeonMap.boss.dispose();
        DungeonMap.bossOutline.dispose();
    }

    // 보스별 아이콘 이미지 로드
    if (key.equals("The Guardian")) {
        DungeonMap.boss = ImageMaster.loadImage("images/ui/map/boss/guardian.png");
        DungeonMap.bossOutline = ImageMaster.loadImage("images/ui/map/bossOutline/guardian.png");
    } else if (key.equals("Hexaghost")) {
        DungeonMap.boss = ImageMaster.loadImage("images/ui/map/boss/hexaghost.png");
        DungeonMap.bossOutline = ImageMaster.loadImage("images/ui/map/bossOutline/hexaghost.png");
    } else if (key.equals("Slime Boss")) {
        DungeonMap.boss = ImageMaster.loadImage("images/ui/map/boss/slimeboss.png");
        DungeonMap.bossOutline = ImageMaster.loadImage("images/ui/map/bossOutline/slimeboss.png");
    }
    // ... Act 2, 3, 4 보스들도 동일 패턴 ...
}
```

**중요**:
- `bossKey`는 **static 변수**로 전역적으로 접근 가능
- 맵에 표시될 보스 아이콘도 이 시점에 결정
- 플레이어는 맵을 통해 어떤 보스가 나올지 **미리 알 수 있음**

#### 3단계: 보스 방 진입

**파일**: `MonsterRoomBoss.java:19-31`

```java
public void onPlayerEntry() {
    this.monsters = CardCrawlGame.dungeon.getBoss();
    logger.info("BOSSES: " + AbstractDungeon.bossList.size());
    CardCrawlGame.metricData.path_taken.add("BOSS");
    CardCrawlGame.music.silenceBGM();
    AbstractDungeon.bossList.remove(0);  // ← 첫 번째 보스 제거

    if (this.monsters != null) {
        this.monsters.init();
    }

    waitTimer = 0.1F;
}
```

**중요 사항**:
1. **bossList.remove(0)**: 사용한 보스를 리스트에서 제거
2. **BGM 침묵**: 보스 전용 음악 준비 (각 보스는 고유 BGM 보유)
3. **메트릭 기록**: path_taken에 "BOSS" 추가 (통계용)

#### 4단계: getBoss() 메서드

**파일**: `AbstractDungeon.java:2498-2502`

```java
public MonsterGroup getBoss() {
    lastCombatMetricKey = bossKey;
    dungeonMapScreen.map.atBoss = true;
    return MonsterHelper.getEncounter(bossKey);
}
```

**역할**:
- `lastCombatMetricKey` 업데이트 (전투 통계용)
- `atBoss` 플래그 활성화 (UI 표시용)
- `bossKey`로 MonsterHelper에서 실제 MonsterGroup 생성

---

## 보스 선택 메커니즘

### 선택 알고리즘

각 Act의 `initializeBoss()` 메서드는 **동일한 로직**을 따릅니다:

```
1. bossList.clear() - 기존 리스트 초기화

2. Daily Run 체크:
   - Settings.isDailyRun == true
     → 모든 보스 3개 추가 → Collections.shuffle(bossList, monsterRng)

   - Settings.isDailyRun == false
     → 3단계 unlock 체크 실행

3. Unlock 우선순위 체크:
   - if (!UnlockTracker.isBossSeen("BOSS_1"))
       → bossList.add("Boss 1")
   - else if (!UnlockTracker.isBossSeen("BOSS_2"))
       → bossList.add("Boss 2")
   - else if (!UnlockTracker.isBossSeen("BOSS_3"))
       → bossList.add("Boss 3")
   - else
       → 모든 보스 3개 추가 → Collections.shuffle(bossList, monsterRng)

4. 안전장치:
   - if (bossList.size() == 1)
       → bossList.add(bossList.get(0)) - 복제하여 2개로
   - else if (bossList.isEmpty())
       → 에러 로그 + 모든 보스 추가 + 셔플

5. 특수 모드 체크:
   - if (Settings.isDemo)
       → bossList.clear() → 특정 보스만 추가 (Act 1: Hexaghost)
```

### 선택 시나리오

#### 시나리오 1: 첫 플레이 (모든 보스 미확인)

```
1단계: !isBossSeen("GUARDIAN") == true
       → bossList.add("The Guardian")

2단계: bossList.size() == 1
       → bossList.add("The Guardian") - 복제

결과: ["The Guardian", "The Guardian"]
      → 보스는 The Guardian 확정, 맵 아이콘도 Guardian
```

#### 시나리오 2: Guardian만 본 상태

```
1단계: !isBossSeen("GUARDIAN") == false → 통과
       !isBossSeen("GHOST") == true
       → bossList.add("Hexaghost")

2단계: bossList.size() == 1
       → bossList.add("Hexaghost")

결과: ["Hexaghost", "Hexaghost"]
```

#### 시나리오 3: 모든 보스를 본 상태

```
1단계: 모든 isBossSeen() == true
       → else 블록 실행
       → bossList.add("The Guardian")
       → bossList.add("Hexaghost")
       → bossList.add("Slime Boss")

2단계: Collections.shuffle(bossList, new Random(monsterRng.randomLong()))

결과: ["Slime Boss", "The Guardian", "Hexaghost"] (예시)
      → 첫 번째가 실제 보스, 나머지 2개는 맵 아이콘 pool
```

#### 시나리오 4: Daily Run

```
1단계: Settings.isDailyRun == true
       → bossList.add("The Guardian")
       → bossList.add("Hexaghost")
       → bossList.add("Slime Boss")

2단계: Collections.shuffle(bossList, new Random(monsterRng.randomLong()))

결과: ["Hexaghost", "Slime Boss", "The Guardian"] (시드 기반)
      → 완전 랜덤
```

### 중요 알고리즘 특징

1. **bossList는 항상 2개 이상**:
   - 단 1개일 경우 복제
   - 0개일 경우 에러 후 전체 추가

2. **unlock 체크는 순서대로**:
   - else-if 구조로 인해 첫 번째 미확인 보스만 선택
   - 동시에 2명 이상 미확인 불가

3. **monsterRng 사용**:
   - 시드 기반 RNG로 **재현 가능**
   - 같은 시드 = 같은 보스 순서

---

## Act별 보스 풀

### Act 1 (Exordium) 보스

**파일**: `Exordium.java:218-257`

```java
protected void initializeBoss() {
    bossList.clear();

    if (Settings.isDailyRun) {
        bossList.add("The Guardian");
        bossList.add("Hexaghost");
        bossList.add("Slime Boss");
        Collections.shuffle(bossList, new Random(monsterRng.randomLong()));
    }
    else if (!UnlockTracker.isBossSeen("GUARDIAN")) {
        bossList.add("The Guardian");
    } else if (!UnlockTracker.isBossSeen("GHOST")) {
        bossList.add("Hexaghost");
    } else if (!UnlockTracker.isBossSeen("SLIME")) {
        bossList.add("Slime Boss");
    } else {
        bossList.add("The Guardian");
        bossList.add("Hexaghost");
        bossList.add("Slime Boss");
        Collections.shuffle(bossList, new Random(monsterRng.randomLong()));
    }

    // 안전장치
    if (bossList.size() == 1) {
        bossList.add(bossList.get(0));
    } else if (bossList.isEmpty()) {
        logger.warn("Boss list was empty. How?");
        bossList.add("The Guardian");
        bossList.add("Hexaghost");
        bossList.add("Slime Boss");
        Collections.shuffle(bossList, new Random(monsterRng.randomLong()));
    }

    // Demo 모드 (튜토리얼)
    if (Settings.isDemo) {
        bossList.clear();
        bossList.add("Hexaghost");
    }
}
```

| 보스 ID | UnlockTracker Key | 우선순위 | 특징 |
|---------|-------------------|----------|------|
| "The Guardian" | "GUARDIAN" | 1순위 | 방어 중심, 여러 페이즈 |
| "Hexaghost" | "GHOST" | 2순위 | 화염 공격, Demo 모드 전용 |
| "Slime Boss" | "SLIME" | 3순위 | 분열 메커니즘 |

**Demo 모드 특이사항**:
- `Settings.isDemo == true`일 경우 **Hexaghost만 고정**
- 튜토리얼/데모 플레이에서 사용

### Act 2 (TheCity) 보스

**파일**: `TheCity.java:181-215`

```java
protected void initializeBoss() {
    bossList.clear();

    if (Settings.isDailyRun) {
        bossList.add("Automaton");
        bossList.add("Collector");
        bossList.add("Champ");
        Collections.shuffle(bossList, new Random(monsterRng.randomLong()));
    }
    else if (!UnlockTracker.isBossSeen("CHAMP")) {
        bossList.add("Champ");
    } else if (!UnlockTracker.isBossSeen("AUTOMATON")) {
        bossList.add("Automaton");
    } else if (!UnlockTracker.isBossSeen("COLLECTOR")) {
        bossList.add("Collector");
    } else {
        bossList.add("Automaton");
        bossList.add("Collector");
        bossList.add("Champ");
        Collections.shuffle(bossList, new Random(monsterRng.randomLong()));
    }

    // 안전장치 (동일)
    if (bossList.size() == 1) {
        bossList.add(bossList.get(0));
    } else if (bossList.isEmpty()) {
        logger.warn("Boss list was empty. How?");
        bossList.add("Automaton");
        bossList.add("Collector");
        bossList.add("Champ");
        Collections.shuffle(bossList, new Random(monsterRng.randomLong()));
    }
}
```

| 보스 ID | UnlockTracker Key | 우선순위 | 특징 |
|---------|-------------------|----------|------|
| "Champ" | "CHAMP" | 1순위 | 분노 모드, 고 HP |
| "Automaton" | "AUTOMATON" | 2순위 | Orb 생성, 3페이즈 |
| "Collector" | "COLLECTOR" | 3순위 | 하수인 소환 |

**unlock 순서 특이사항**:
- Champ가 1순위 (Act 1과 달리 알파벳 순서 아님)

### Act 3 (TheBeyond) 보스

**파일**: `TheBeyond.java:169-203`

```java
protected void initializeBoss() {
    bossList.clear();

    if (Settings.isDailyRun) {
        bossList.add("Awakened One");
        bossList.add("Time Eater");
        bossList.add("Donu and Deca");
        Collections.shuffle(bossList, new Random(monsterRng.randomLong()));
    }
    else if (!UnlockTracker.isBossSeen("CROW")) {
        bossList.add("Awakened One");
    } else if (!UnlockTracker.isBossSeen("DONUT")) {
        bossList.add("Donu and Deca");
    } else if (!UnlockTracker.isBossSeen("WIZARD")) {
        bossList.add("Time Eater");
    } else {
        bossList.add("Awakened One");
        bossList.add("Time Eater");
        bossList.add("Donu and Deca");
        Collections.shuffle(bossList, new Random(monsterRng.randomLong()));
    }

    // 안전장치 (동일)
    if (bossList.size() == 1) {
        bossList.add(bossList.get(0));
    } else if (bossList.isEmpty()) {
        logger.warn("Boss list was empty. How?");
        bossList.add("Awakened One");
        bossList.add("Time Eater");
        bossList.add("Donu and Deca");
        Collections.shuffle(bossList, new Random(monsterRng.randomLong()));
    }
}
```

| 보스 ID | UnlockTracker Key | 우선순위 | 특징 |
|---------|-------------------|----------|------|
| "Awakened One" | "CROW" | 1순위 | Powers 페널티, 부활 |
| "Donu and Deca" | "DONUT" | 2순위 | 2체 보스, 상호 버프 |
| "Time Eater" | "WIZARD" | 3순위 | 카드 제한, 시간 메커니즘 |

**UnlockTracker Key 특이사항**:
- "Awakened One" → "CROW" (까마귀 형상)
- "Donu and Deca" → "DONUT" (도넛 모양)
- "Time Eater" → "WIZARD" (마법사처럼 생김)

### Act 4 (TheEnding) 보스

**파일**: `TheEnding.java:235-239`

```java
protected void initializeBoss() {
    bossList.add("The Heart");
    bossList.add("The Heart");
    bossList.add("The Heart");
}
```

| 보스 ID | 확률 | 특징 |
|---------|------|------|
| "The Heart" | 100% | 고정, Act 4 최종 보스 |

**특징**:
- **unlock 체크 없음**: 무조건 "The Heart"
- **3개 중복 추가**: 일관성 유지 (bossList는 항상 여러 개)
- **셔플 없음**: 어차피 모두 같은 보스

---

## UnlockTracker 시스템

### isBossSeen() 메서드

**파일**: `UnlockTracker.java:875-880`

```java
public static boolean isBossSeen(String key) {
    if (bossSeenPref.getInteger(key, 0) == 1) {
        return true;
    }
    return false;
}
```

**동작 방식**:
- `bossSeenPref`: Preferences 객체 (파일 기반 저장소)
- `key`: 보스 ID ("GUARDIAN", "GHOST" 등)
- `getInteger(key, 0)`: key에 해당하는 값 조회, 없으면 0 반환
- `== 1`: 1이면 본 적 있음, 0이면 처음

### markBossAsSeen() 메서드

**파일**: `UnlockTracker.java:868-873`

```java
public static void markBossAsSeen(String originalName) {
    if (bossSeenPref.getInteger(originalName) != 1) {
        bossSeenPref.putInteger(originalName, 1);
        bossSeenPref.flush();
    }
}
```

**호출 시점**:
- 보스 전투 승리 후 자동 호출 (정확한 위치는 추가 조사 필요)
- `flush()`: 즉시 파일에 저장 (게임 종료 전 저장 보장)

### UnlockTracker Key 매핑

| Act | 보스 이름 | 게임 내 Key | UnlockTracker Key |
|-----|----------|-------------|-------------------|
| 1 | The Guardian | "The Guardian" | "GUARDIAN" |
| 1 | Hexaghost | "Hexaghost" | "GHOST" |
| 1 | Slime Boss | "Slime Boss" | "SLIME" |
| 2 | Champ | "Champ" | "CHAMP" |
| 2 | Automaton | "Automaton" | "AUTOMATON" |
| 2 | The Collector | "Collector" | "COLLECTOR" |
| 3 | Awakened One | "Awakened One" | "CROW" |
| 3 | Time Eater | "Time Eater" | "WIZARD" |
| 3 | Donu and Deca | "Donu and Deca" | "DONUT" |
| 4 | The Heart | "The Heart" | (체크 안 함) |

**중요**:
- 게임 내 Key (MonsterHelper 사용)와 UnlockTracker Key는 **다를 수 있음**
- 모드 제작 시 반드시 올바른 Key 사용 필요

---

## 보스 키 관리

### bossKey 변수

**파일**: `AbstractDungeon.java:237`

```java
public static String bossKey;
```

**특징**:
- **static 변수**: 전역 접근 가능
- **던전 생성 시 설정**: `setBoss(bossList.get(0))`
- **변경 불가**: 한 번 설정되면 해당 Act 종료까지 유지

### bossKey 사용처

1. **setBoss()**: 초기 설정 (Line 421)
2. **getBoss()**: MonsterGroup 생성 시 참조 (Line 2499, 2501)
3. **DungeonMap**: 보스 아이콘 이미지 설정
4. **메트릭**: `lastCombatMetricKey` 업데이트

### bossList의 역할

```
bossList[0]: 실제 싸울 보스 (bossKey에 저장)
bossList[1]: 맵 아이콘 표시용 (미사용)
bossList[2]: 맵 아이콘 표시용 (미사용)
```

**중요**:
- bossList가 3개인 이유: **UI 일관성**
- 일부 코드에서 bossList.size()를 체크하므로, 최소 2개 이상 필요
- 실제로는 **첫 번째 요소만 사용**

---

## 보스 방 진입

### MonsterRoomBoss 클래스

**파일**: `MonsterRoomBoss.java`

```java
public class MonsterRoomBoss extends MonsterRoom {
    private static final Logger logger = LogManager.getLogger(MonsterRoomBoss.class.getName());

    public void onPlayerEntry() {
        this.monsters = CardCrawlGame.dungeon.getBoss();
        logger.info("BOSSES: " + AbstractDungeon.bossList.size());
        CardCrawlGame.metricData.path_taken.add("BOSS");
        CardCrawlGame.music.silenceBGM();
        AbstractDungeon.bossList.remove(0);

        if (this.monsters != null) {
            this.monsters.init();
        }

        waitTimer = 0.1F;
    }

    public AbstractCard.CardRarity getCardRarity(int roll) {
        return AbstractCard.CardRarity.RARE;  // ← 모든 카드 보상 RARE
    }
}
```

### 특수 동작

#### 1. BGM 침묵

```java
CardCrawlGame.music.silenceBGM();
```

**이유**:
- 보스마다 고유 BGM 보유
- 보스 MonsterGroup.init() 시 자체 BGM 재생
- 기존 던전 BGM 미리 중단

#### 2. bossList 변경

```java
AbstractDungeon.bossList.remove(0);
```

**효과**:
- 사용한 보스 제거
- **Endless 모드 대비**: bossList가 비면 재생성 로직 작동
- **디버그 로그**: bossList.size()를 로그로 출력

**Endless 모드에서의 동작**:
```
1차 보스 후: bossList = [boss2, boss3]
2차 보스 후: bossList = [boss3]
3차 보스 후: bossList = []
4차 보스 전: generateBoss() 재호출 → 새로운 bossList 생성
```

#### 3. 메트릭 기록

```java
CardCrawlGame.metricData.path_taken.add("BOSS");
```

**용도**:
- 플레이 데이터 수집
- 통계 분석용
- Run history 기록

---

## 보상 시스템

### 카드 보상

**파일**: `MonsterRoomBoss.java:34-36`

```java
public AbstractCard.CardRarity getCardRarity(int roll) {
    return AbstractCard.CardRarity.RARE;
}
```

**특징**:
- **roll 파라미터 무시**: 항상 RARE 반환
- **일반/엘리트와 다름**: 일반은 roll 기반 확률 계산
- **3장 카드 선택**: AbstractRoom의 보상 로직에 의해 RARE 3장 제공

### 유물 보상

보스 전투는 **Boss Relic 전용 풀** 사용:

**파일**: `AbstractDungeon.java:229`

```java
public static ArrayList<String> bossRelicPool = new ArrayList<>();
```

**보상 생성**:
- 보스 처치 후 자동으로 Boss Relic 1개 제공
- `bossRelicPool`에서 랜덤 선택
- 한 번 획득한 보스 유물은 풀에서 제거

**Boss Relic 특징**:
- 강력한 효과 + 단점 동반 (대부분)
- 일반 유물보다 훨씬 강력
- 캐릭터별 Boss Relic 존재

---

## Act 4 특수 구조

### 고정 맵 구조

**파일**: `TheEnding.java:73-168`

```java
private void generateSpecialMap() {
    long startTime = System.currentTimeMillis();

    map = new ArrayList<>();

    // 노드 생성
    ArrayList<MapRoomNode> row1 = new ArrayList<>();
    MapRoomNode restNode = new MapRoomNode(3, 0);
    restNode.room = new RestRoom();

    MapRoomNode shopNode = new MapRoomNode(3, 1);
    shopNode.room = new ShopRoom();

    MapRoomNode enemyNode = new MapRoomNode(3, 2);
    enemyNode.room = new MonsterRoomElite();  // ← Shield and Spear

    MapRoomNode bossNode = new MapRoomNode(3, 3);
    bossNode.room = new MonsterRoomBoss();    // ← The Heart

    MapRoomNode victoryNode = new MapRoomNode(3, 4);
    victoryNode.room = new TrueVictoryRoom();

    // 연결
    connectNode(restNode, shopNode);
    connectNode(shopNode, enemyNode);
    enemyNode.addEdge(new MapEdge(..., bossNode, ...));
    // victoryNode는 자동 연결

    // 5x7 맵 생성 (실제 경로는 중앙 1줄만)
    // ...
}
```

### Act 4 고정 경로

```
Floor 1 (0): Rest Site (휴식처)
      ↓
Floor 2 (1): Shop (상점)
      ↓
Floor 3 (2): Elite (Shield and Spear)
      ↓
Floor 4 (3): BOSS (The Heart)
      ↓
Floor 5 (4): True Victory Room (엔딩)
```

**특징**:
1. **선택지 없음**: 직선 경로만 존재
2. **고정 조우**: 모든 방이 미리 결정됨
3. **엘리트 필수**: 보스 전 반드시 Shield and Spear 전투
4. **The Heart 확정**: 100% The Heart와 싸움

### Act 4 보스 메커니즘

**파일**: `TheEnding.java:204-214, 235-239`

```java
protected void generateMonsters() {
    monsterList = new ArrayList<>();
    monsterList.add("Shield and Spear");
    monsterList.add("Shield and Spear");
    monsterList.add("Shield and Spear");

    eliteMonsterList = new ArrayList<>();
    eliteMonsterList.add("Shield and Spear");
    eliteMonsterList.add("Shield and Spear");
    eliteMonsterList.add("Shield and Spear");
}

protected void initializeBoss() {
    bossList.add("The Heart");
    bossList.add("The Heart");
    bossList.add("The Heart");
}
```

**특징**:
- `generateWeakEnemies`, `generateStrongEnemies`, `generateElites` 모두 **비어있음**
- monsterList와 eliteMonsterList를 **직접 할당**
- The Heart는 **unlock 체크 없음**

---

## 수정 방법

### 1. 보스 선택 확률 변경

**목표**: Slime Boss를 더 자주 나오게 하기

**파일**: `Exordium.java`

```java
@SpirePatch(
    cls = "com.megacrit.cardcrawl.dungeons.Exordium",
    method = "initializeBoss"
)
public static class SlimeBossBoostPatch {
    @SpirePrefixPatch
    public static SpireReturn<Void> Prefix(Exordium __instance) {
        AbstractDungeon.bossList.clear();

        if (Settings.isDailyRun) {
            // Daily Run은 그대로 유지
            AbstractDungeon.bossList.add("The Guardian");
            AbstractDungeon.bossList.add("Hexaghost");
            AbstractDungeon.bossList.add("Slime Boss");
            Collections.shuffle(AbstractDungeon.bossList,
                new Random(AbstractDungeon.monsterRng.randomLong()));
        } else {
            // Slime Boss를 항상 1순위로
            if (!UnlockTracker.isBossSeen("SLIME")) {
                AbstractDungeon.bossList.add("Slime Boss");
            } else if (!UnlockTracker.isBossSeen("GUARDIAN")) {
                AbstractDungeon.bossList.add("The Guardian");
            } else if (!UnlockTracker.isBossSeen("GHOST")) {
                AbstractDungeon.bossList.add("Hexaghost");
            } else {
                // 모두 본 경우: Slime Boss 가중치 증가
                AbstractDungeon.bossList.add("Slime Boss");
                AbstractDungeon.bossList.add("Slime Boss");  // 2번 추가
                AbstractDungeon.bossList.add("The Guardian");
                AbstractDungeon.bossList.add("Hexaghost");
                Collections.shuffle(AbstractDungeon.bossList,
                    new Random(AbstractDungeon.monsterRng.randomLong()));
            }
        }

        // 안전장치
        if (AbstractDungeon.bossList.size() == 1) {
            AbstractDungeon.bossList.add(AbstractDungeon.bossList.get(0));
        }

        return SpireReturn.Return(null);
    }
}
```

**결과**: Slime Boss가 4/6 = 66.7% 확률로 등장

### 2. 새로운 보스 추가

**목표**: Act 1에 "Super Slime" 보스 추가

**Step 1**: MonsterHelper에 encounter 추가

```java
@SpirePatch(
    cls = "com.megacrit.cardcrawl.helpers.MonsterHelper",
    method = "getEncounter",
    paramtypez = { String.class }
)
public static class SuperSlimeEncounterPatch {
    @SpirePrefixPatch
    public static SpireReturn<MonsterGroup> Prefix(String key) {
        if (key.equals("Super Slime")) {
            return SpireReturn.Return(new MonsterGroup(
                new SuperSlimeBoss(0.0F, 0.0F)  // 커스텀 보스 클래스
            ));
        }
        return SpireReturn.Continue();
    }
}
```

**Step 2**: initializeBoss 패치

```java
@SpirePatch(
    cls = "com.megacrit.cardcrawl.dungeons.Exordium",
    method = "initializeBoss"
)
public static class AddSuperSlimePatch {
    @SpirePostfixPatch
    public static void Postfix(Exordium __instance) {
        // 기존 보스 리스트에 추가
        AbstractDungeon.bossList.add("Super Slime");

        // 4개가 되었으므로 셔플
        Collections.shuffle(AbstractDungeon.bossList,
            new Random(AbstractDungeon.monsterRng.randomLong()));
    }
}
```

**Step 3**: setBoss에 아이콘 추가

```java
@SpirePatch(
    cls = "com.megacrit.cardcrawl.dungeons.AbstractDungeon",
    method = "setBoss",
    paramtypez = { String.class }
)
public static class SuperSlimeIconPatch {
    @SpirePostfixPatch
    public static void Postfix(AbstractDungeon __instance, String key) {
        if (key.equals("Super Slime")) {
            DungeonMap.boss = ImageMaster.loadImage(
                "mymod/images/boss/superslime.png");
            DungeonMap.bossOutline = ImageMaster.loadImage(
                "mymod/images/bossOutline/superslime.png");
        }
    }
}
```

### 3. 보스 강제 설정

**목표**: 특정 조건에서 항상 특정 보스 나오게 하기

```java
@SpirePatch(
    cls = "com.megacrit.cardcrawl.dungeons.AbstractDungeon",
    method = "setBoss",
    paramtypez = { String.class }
)
public static class ForceBossPatch {
    @SpirePrefixPatch
    public static SpireReturn<Void> Prefix(AbstractDungeon __instance, String key) {
        // Ascension 20 이상이면 항상 가장 어려운 보스
        if (AbstractDungeon.ascensionLevel >= 20) {
            String forcedBoss = null;

            if (__instance instanceof Exordium) {
                forcedBoss = "The Guardian";  // 가장 어려운 Act 1 보스
            } else if (__instance instanceof TheCity) {
                forcedBoss = "Champ";
            } else if (__instance instanceof TheBeyond) {
                forcedBoss = "Awakened One";
            }

            if (forcedBoss != null) {
                // Reflection으로 private setBoss 직접 호출
                // 또는 bossKey 직접 설정
                try {
                    Field bossKeyField = AbstractDungeon.class.getDeclaredField("bossKey");
                    bossKeyField.setAccessible(true);
                    bossKeyField.set(null, forcedBoss);

                    // 보스 아이콘 업데이트는 원본 메서드가 처리
                    return SpireReturn.Continue();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        return SpireReturn.Continue();
    }
}
```

### 4. UnlockTracker 우회

**목표**: unlock 상관없이 항상 랜덤 선택

```java
@SpirePatch(
    cls = "com.megacrit.cardcrawl.dungeons.Exordium",
    method = "initializeBoss"
)
public static class AlwaysRandomBossPatch {
    @SpirePrefixPatch
    public static SpireReturn<Void> Prefix(Exordium __instance) {
        AbstractDungeon.bossList.clear();

        // unlock 체크 무시하고 항상 3개 추가 + 셔플
        AbstractDungeon.bossList.add("The Guardian");
        AbstractDungeon.bossList.add("Hexaghost");
        AbstractDungeon.bossList.add("Slime Boss");

        Collections.shuffle(AbstractDungeon.bossList,
            new Random(AbstractDungeon.monsterRng.randomLong()));

        return SpireReturn.Return(null);  // 원본 메서드 스킵
    }
}
```

### 5. Act 4 보스 변경

**목표**: The Heart 대신 다른 보스 등장

```java
@SpirePatch(
    cls = "com.megacrit.cardcrawl.dungeons.TheEnding",
    method = "initializeBoss"
)
public static class CustomAct4BossPatch {
    @SpirePrefixPatch
    public static SpireReturn<Void> Prefix(TheEnding __instance) {
        AbstractDungeon.bossList.clear();

        // 랜덤 최종 보스 선택
        ArrayList<String> finalBosses = new ArrayList<>();
        finalBosses.add("The Heart");
        finalBosses.add("Corrupted Heart");  // 커스텀 보스
        finalBosses.add("True Heart");       // 커스텀 보스

        String chosenBoss = finalBosses.get(
            AbstractDungeon.miscRng.random(finalBosses.size() - 1));

        AbstractDungeon.bossList.add(chosenBoss);
        AbstractDungeon.bossList.add(chosenBoss);
        AbstractDungeon.bossList.add(chosenBoss);

        return SpireReturn.Return(null);
    }
}
```

---

## 관련 클래스

### 핵심 클래스

| 클래스 | 경로 | 역할 |
|--------|------|------|
| **MonsterRoomBoss** | `com.megacrit.cardcrawl.rooms.MonsterRoomBoss` | 보스 방 |
| **AbstractDungeon** | `com.megacrit.cardcrawl.dungeons.AbstractDungeon` | 보스 리스트 및 키 관리 |
| **Exordium** | `com.megacrit.cardcrawl.dungeons.Exordium` | 1막 보스 풀 |
| **TheCity** | `com.megacrit.cardcrawl.dungeons.TheCity` | 2막 보스 풀 |
| **TheBeyond** | `com.megacrit.cardcrawl.dungeons.TheBeyond` | 3막 보스 풀 |
| **TheEnding** | `com.megacrit.cardcrawl.dungeons.TheEnding` | 4막 보스 및 고정 맵 |
| **UnlockTracker** | `com.megacrit.cardcrawl.unlock.UnlockTracker` | 보스 unlock 추적 |
| **MonsterHelper** | `com.megacrit.cardcrawl.helpers.MonsterHelper` | 보스 MonsterGroup 생성 |
| **DungeonMap** | `com.megacrit.cardcrawl.dungeons.DungeonMap` | 보스 아이콘 표시 |

### 주요 메서드

#### AbstractDungeon

| 메서드 | 코드 위치 | 설명 |
|--------|----------|------|
| `initializeBoss()` | (각 던전 클래스) | 보스 풀 생성 (abstract) |
| `setBoss(String key)` | Line 420-476 | bossKey 설정 및 아이콘 로드 |
| `getBoss()` | Line 2498-2502 | MonsterGroup 반환 |

#### MonsterRoomBoss

| 메서드 | 코드 위치 | 설명 |
|--------|----------|------|
| `onPlayerEntry()` | Line 19-31 | 보스 방 진입 시 초기화 |
| `getCardRarity(int roll)` | Line 34-36 | RARE 카드 고정 반환 |

#### UnlockTracker

| 메서드 | 코드 위치 | 설명 |
|--------|----------|------|
| `isBossSeen(String key)` | Line 875-880 | 보스 본 적 있는지 확인 |
| `markBossAsSeen(String originalName)` | Line 868-873 | 보스를 본 것으로 기록 |

### 관련 static 변수

#### AbstractDungeon

| 변수 | 타입 | 설명 |
|------|------|------|
| `bossKey` | String | 현재 Act의 보스 키 |
| `bossList` | ArrayList\<String\> | 보스 리스트 (3개) |
| `lastCombatMetricKey` | String | 마지막 전투 통계 키 |
| `bossRelicPool` | ArrayList\<String\> | 보스 유물 풀 |

---

## 참고사항

### Seed 영향

- **보스 선택**: 동일 시드 = 동일 보스
- **보스 순서**: monsterRng.randomLong() 사용
- **재현 가능**: 같은 시드 + unlock 상태 = 같은 보스

### Daily Run

- **완전 랜덤**: unlock 무시
- **시드 기반**: 모든 플레이어가 같은 보스
- **통계 수집**: 전체 플레이어 비교 가능

### Endless 모드

- **bossList 재생성**: 비면 자동으로 새로 생성
- **무한 반복**: 계속 보스와 싸울 수 있음
- **unlock 누적**: 모든 보스를 보면 계속 랜덤

### Demo 모드

- **Hexaghost 고정** (Act 1 only)
- **튜토리얼 전용**
- **unlock 무시**

### 보스 BGM

각 보스는 고유 BGM 보유:

| 보스 | BGM 파일 |
|------|---------|
| The Guardian | `BOSS_BOTTOM` |
| Hexaghost | `BOSS_BOTTOM` |
| Slime Boss | `BOSS_BOTTOM` |
| Champ | `BOSS_CITY` |
| Automaton | `BOSS_CITY` |
| Collector | `BOSS_CITY` |
| Awakened One | `BOSS_BEYOND` |
| Time Eater | `BOSS_BEYOND` |
| Donu and Deca | `BOSS_BEYOND` |
| The Heart | `BOSS_ENDING` |

**호출 시점**: MonsterGroup.init() 내부에서 각 보스가 자체 BGM 재생

---

## 디버깅 정보

### 로그 메시지

**파일**: `MonsterRoomBoss.java:21`

```
INFO: BOSSES: 2
```

- bossList.remove(0) 직후 크기 출력
- 정상: 3 → 2 (제거 후)
- 비정상: 0 또는 1 (버그 가능성)

**파일**: `AbstractDungeon.java:246, 209, 197`

```
WARN: Boss list was empty. How?
```

- bossList가 비어있을 때 출력
- 이론상 발생하지 않아야 함
- 발생 시 자동 복구 (모든 보스 추가 + 셔플)

### 보스 Key 확인

게임 로그에서 bossKey 확인:
```
(setBoss 호출 시 로그는 없지만, DungeonMap 이미지 로드 로그로 유추 가능)
```

실제 보스 Key는 `AbstractDungeon.bossKey` 직접 조회 필요

---

## 작성 정보

- **작성일**: 2025-11-08
- **대상 버전**: Slay the Spire 01-23-2019 빌드
- **분석 범위**: Act 1-4 보스 전투 시스템 전체, UnlockTracker 시스템, 보스 선택 알고리즘
