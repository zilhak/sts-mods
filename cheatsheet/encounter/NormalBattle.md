# 일반 전투 (Normal Battle) Encounter 시스템

지도상 "일반 적" 심볼로 진입했을 때, 적 구성이 결정되는 전체 시스템 분석

---

## 📑 목차

1. [시스템 개요](#시스템-개요)
2. [호출 흐름](#호출-흐름)
3. [몬스터 풀 정의](#몬스터-풀-정의)
4. [확률 시스템](#확률-시스템)
5. [몬스터 선택 로직](#몬스터-선택-로직)
6. [수정 방법](#수정-방법)
7. [관련 클래스](#관련-클래스)

---

## 시스템 개요

일반 전투는 **사전 생성된 몬스터 리스트**에서 순차적으로 꺼내오는 방식으로 동작합니다.

### 핵심 특징

1. **사전 생성**: 던전 시작 시 모든 일반 전투 몬스터가 미리 결정됨
2. **순차 소비**: 전투마다 리스트의 첫 번째 요소를 꺼내어 사용
3. **계층 구조**: Weak (1-3층) → Strong (4-15층) 자동 전환
4. **중복 방지**: 연속으로 같은 몬스터가 나오지 않도록 필터링
5. **제외 규칙**: 특정 몬스터 조합은 연속 등장 방지

---

## 호출 흐름

### 전체 프로세스

```
던전 시작 (Exordium 생성자)
    ↓
generateMonsters() 호출
    ↓
generateWeakEnemies(3) + generateStrongEnemies(12)
    ↓
monsterList에 15개 몬스터 사전 생성
    ↓
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
플레이어가 일반 전투 방 진입
    ↓
MonsterRoom.onPlayerEntry() 호출 (Line 57)
    ↓
CardCrawlGame.dungeon.getMonsterForRoomCreation() 호출 (Line 60)
    ↓
monsterList.get(0) 꺼내기 (Line 2342-2344)
    ↓
MonsterHelper.getEncounter(key) 호출 (Line 2344)
    ↓
실제 MonsterGroup 생성 (Line 368-572)
    ↓
monsters.init() 호출 (Line 61)
    ↓
전투 시작
```

### 1단계: 던전 초기화

**파일**: `Exordium.java`

```java
public Exordium(AbstractPlayer p, ArrayList<String> emptyList) {
    super(NAME, "Exordium", p, emptyList);
    // ...
    generateMap();  // 맵 생성 시 generateMonsters() 자동 호출
}
```

**코드 위치**: `Exordium.java:49-93`

### 2단계: 몬스터 풀 생성

**파일**: `Exordium.java`

```java
protected void generateMonsters() {
    generateWeakEnemies(3);      // 약한 적 3개
    generateStrongEnemies(12);   // 강한 적 12개
    generateElites(10);          // 엘리트 10개
}
```

**코드 위치**: `Exordium.java:144-148`

### 3단계: 방 진입 시 몬스터 할당

**파일**: `MonsterRoom.java`

```java
public void onPlayerEntry() {
    playBGM(null);
    if (this.monsters == null) {
        // 몬스터가 아직 할당되지 않았으면 가져오기
        this.monsters = CardCrawlGame.dungeon.getMonsterForRoomCreation();
        this.monsters.init();
    }
    waitTimer = 0.1F;
}
```

**코드 위치**: `MonsterRoom.java:57-64`

### 4단계: 몬스터 선택

**파일**: `AbstractDungeon.java`

```java
public MonsterGroup getMonsterForRoomCreation() {
    if (monsterList.isEmpty()) {
        generateStrongEnemies(12);  // 리스트 소진 시 재생성
    }
    logger.info("MONSTER: " + monsterList.get(0));
    lastCombatMetricKey = monsterList.get(0);
    return MonsterHelper.getEncounter(monsterList.get(0));  // 문자열 키로 몬스터 생성
}
```

**코드 위치**: `AbstractDungeon.java:2338-2345`

**중요**: `monsterList.get(0)`을 꺼낸 후 리스트에서 제거되므로, 다음 전투는 다음 요소를 사용

### 5단계: 실제 MonsterGroup 생성

**파일**: `MonsterHelper.java`

```java
public static MonsterGroup getEncounter(String key) {
    switch (key) {
        case "Cultist":
            return new MonsterGroup(new Cultist(0.0F, -10.0F));
        case "Jaw Worm":
            return new MonsterGroup(new JawWorm(0.0F, 25.0F));
        case "2 Louse":
            return new MonsterGroup(new AbstractMonster[] {
                getLouse(-200.0F, 10.0F),
                getLouse(80.0F, 30.0F)
            });
        // ... (모든 encounter 정의)
    }
}
```

**코드 위치**: `MonsterHelper.java:368-572`

---

## 몬스터 풀 정의

### Act 1 (Exordium) 몬스터 풀

#### Weak Enemies (1-3층)

**파일**: `Exordium.java:153-161`

```java
protected void generateWeakEnemies(int count) {
    ArrayList<MonsterInfo> monsters = new ArrayList<>();
    monsters.add(new MonsterInfo("Cultist", 2.0F));
    monsters.add(new MonsterInfo("Jaw Worm", 2.0F));
    monsters.add(new MonsterInfo("2 Louse", 2.0F));
    monsters.add(new MonsterInfo("Small Slimes", 2.0F));
    MonsterInfo.normalizeWeights(monsters);
    populateMonsterList(monsters, count, false);
}
```

| 몬스터 ID | 가중치 | 정규화 확률 | 설명 |
|-----------|--------|-------------|------|
| "Cultist" | 2.0 | 25% | 광신자 1마리 |
| "Jaw Worm" | 2.0 | 25% | 턱벌레 1마리 |
| "2 Louse" | 2.0 | 25% | 공벌레 2마리 |
| "Small Slimes" | 2.0 | 25% | 작은 슬라임 조합 |

**특징**:
- 모든 몬스터 동일 확률 (25%)
- **3개만 생성** (1-3층용)
- 중복 가능 (연속 제외 규칙만 적용)

#### Strong Enemies (4-15층)

**파일**: `Exordium.java:163-178`

```java
protected void generateStrongEnemies(int count) {
    ArrayList<MonsterInfo> monsters = new ArrayList<>();
    monsters.add(new MonsterInfo("Blue Slaver", 2.0F));
    monsters.add(new MonsterInfo("Gremlin Gang", 1.0F));
    monsters.add(new MonsterInfo("Looter", 2.0F));
    monsters.add(new MonsterInfo("Large Slime", 2.0F));
    monsters.add(new MonsterInfo("Lots of Slimes", 1.0F));
    monsters.add(new MonsterInfo("Exordium Thugs", 1.5F));
    monsters.add(new MonsterInfo("Exordium Wildlife", 1.5F));
    monsters.add(new MonsterInfo("Red Slaver", 1.0F));
    monsters.add(new MonsterInfo("3 Louse", 2.0F));
    monsters.add(new MonsterInfo("2 Fungi Beasts", 2.0F));
    MonsterInfo.normalizeWeights(monsters);
    populateFirstStrongEnemy(monsters, generateExclusions());
    populateMonsterList(monsters, count, false);
}
```

| 몬스터 ID | 가중치 | 정규화 확률 | 설명 |
|-----------|--------|-------------|------|
| "Blue Slaver" | 2.0 | ~12.5% | 파란 노예상인 |
| "Gremlin Gang" | 1.0 | ~6.25% | 그렘린 4마리 |
| "Looter" | 2.0 | ~12.5% | 도적 |
| "Large Slime" | 2.0 | ~12.5% | 큰 슬라임 (Acid/Spike 랜덤) |
| "Lots of Slimes" | 1.0 | ~6.25% | 작은 슬라임 5마리 |
| "Exordium Thugs" | 1.5 | ~9.4% | 인간형 적 조합 |
| "Exordium Wildlife" | 1.5 | ~9.4% | 야생 적 조합 |
| "Red Slaver" | 1.0 | ~6.25% | 빨간 노예상인 |
| "3 Louse" | 2.0 | ~12.5% | 공벌레 3마리 |
| "2 Fungi Beasts" | 2.0 | ~12.5% | 동물하초 2마리 |

**특징**:
- 총 가중치: 16.0
- **12개 생성** (4-15층용, 실제로는 첫 번째 강한 적 포함 13개)
- `populateFirstStrongEnemy()` 호출로 첫 번째 강한 적 특별 처리

---

## 확률 시스템

### MonsterInfo 클래스

**파일**: `MonsterInfo.java`

```java
public class MonsterInfo implements Comparable<MonsterInfo> {
    public String name;
    public float weight;

    public MonsterInfo(String name, float weight) {
        this.name = name;
        this.weight = weight;
    }
}
```

**코드 위치**: `MonsterInfo.java:9-19`

### 가중치 정규화

**파일**: `MonsterInfo.java:21-34`

```java
public static void normalizeWeights(ArrayList<MonsterInfo> list) {
    Collections.sort(list);  // 가중치 오름차순 정렬
    float total = 0.0F;

    // 총합 계산
    for (MonsterInfo i : list) {
        total += i.weight;
    }

    // 정규화 (각 가중치 / 총합 = 확률)
    for (MonsterInfo i : list) {
        i.weight /= total;
        if (Settings.isInfo) {
            logger.info(i.name + ": " + i.weight + "%");
        }
    }
}
```

**예시**: Strong Enemies 정규화
```
총 가중치 = 2+1+2+2+1+1.5+1.5+1+2+2 = 16.0

Blue Slaver:  2.0 / 16.0 = 0.125 (12.5%)
Gremlin Gang: 1.0 / 16.0 = 0.0625 (6.25%)
Looter:       2.0 / 16.0 = 0.125 (12.5%)
...
```

### 확률 기반 선택 (Roll)

**파일**: `MonsterInfo.java:43-52`

```java
public static String roll(ArrayList<MonsterInfo> list, float roll) {
    float currentWeight = 0.0F;
    for (MonsterInfo i : list) {
        currentWeight += i.weight;
        if (roll < currentWeight) {
            return i.name;
        }
    }
    return "ERROR";
}
```

**동작 방식**:
```
roll = 0.37 (37%)인 경우:

Blue Slaver:  0.0 ~ 0.125  (X)
Gremlin Gang: 0.125 ~ 0.1875 (X)
Looter:       0.1875 ~ 0.3125 (X)
Large Slime:  0.3125 ~ 0.4375 (O) ← 선택됨!
```

---

## 몬스터 선택 로직

### populateMonsterList 메서드

**파일**: `AbstractDungeon.java:1324-1355`

```java
public void populateMonsterList(ArrayList<MonsterInfo> monsters, int numMonsters, boolean elites) {
    if (elites) {
        // 엘리트 처리 (생략)
    } else {
        for (int i = 0; i < numMonsters; i++) {
            if (monsterList.isEmpty()) {
                // 첫 번째 몬스터: 무조건 추가
                monsterList.add(MonsterInfo.roll(monsters, monsterRng.random()));
            } else {
                String toAdd = MonsterInfo.roll(monsters, monsterRng.random());

                // 연속 중복 방지
                if (!toAdd.equals(monsterList.get(monsterList.size() - 1))) {
                    monsterList.add(toAdd);
                } else {
                    i--;  // 재시도
                }
            }
        }
    }
}
```

**중요 특징**:
1. **연속 중복 방지**: 이전 몬스터와 같으면 재선택
2. **비연속 중복 허용**: A → B → A 같은 패턴은 가능
3. **무한 루프 방지 없음**: 이론적으로 같은 몬스터가 계속 나올 수 있음 (실제로는 확률적으로 매우 낮음)

### populateFirstStrongEnemy 메서드

**파일**: `AbstractDungeon.java:1313-1321`

```java
public void populateFirstStrongEnemy(ArrayList<MonsterInfo> monsters, ArrayList<String> exclusions) {
    while (true) {
        String m = MonsterInfo.roll(monsters, monsterRng.random());
        if (!exclusions.contains(m)) {
            monsterList.add(m);
            return;
        }
        // 제외 목록에 있으면 재선택
    }
}
```

**역할**:
- 약한 적 (Weak Enemies) 마지막 몬스터를 기준으로 **첫 번째 강한 적 특별 처리**
- `generateExclusions()`로 제외 목록 생성

### generateExclusions 메서드

**파일**: `Exordium.java:190-215`

```java
protected ArrayList<String> generateExclusions() {
    ArrayList<String> retVal = new ArrayList<>();

    // 마지막 Weak Enemy에 따라 제외 목록 결정
    switch (monsterList.get(monsterList.size() - 1)) {
        case "Looter":
            retVal.add("Exordium Thugs");  // Looter가 Thugs에 포함되므로 중복 방지
            break;

        case "Blue Slaver":
            retVal.add("Red Slaver");      // Slaver 연속 방지
            retVal.add("Exordium Thugs");  // Slaver가 Thugs에 포함
            break;

        case "2 Louse":
            retVal.add("3 Louse");         // Louse 연속 방지
            break;

        case "Small Slimes":
            retVal.add("Large Slime");     // 슬라임 연속 방지
            retVal.add("Lots of Slimes");
            break;
    }

    return retVal;
}
```

**제외 규칙 요약**:

| 마지막 Weak Enemy | 제외되는 첫 Strong Enemy |
|-------------------|--------------------------|
| Looter | Exordium Thugs |
| Blue Slaver | Red Slaver, Exordium Thugs |
| 2 Louse | 3 Louse |
| Small Slimes | Large Slime, Lots of Slimes |
| Cultist | (제외 없음) |
| Jaw Worm | (제외 없음) |

---

## 특수 Encounter 생성 로직

### Exordium Thugs

**파일**: `MonsterHelper.java:829-835`

```java
private static MonsterGroup bottomHumanoid() {
    AbstractMonster[] monsters = new AbstractMonster[2];
    monsters[0] = bottomGetWeakWildlife(randomXOffset(-160.0F), randomYOffset(20.0F));
    monsters[1] = bottomGetStrongHumanoid(randomXOffset(130.0F), randomYOffset(20.0F));
    return new MonsterGroup(monsters);
}
```

**구성**:
- 약한 야생 적 1 + 강한 인간형 적 1

**bottomGetStrongHumanoid** (Line 867-875):
```java
private static AbstractMonster bottomGetStrongHumanoid(float x, float y) {
    ArrayList<AbstractMonster> monsters = new ArrayList<>();
    monsters.add(new Cultist(x, y));
    monsters.add(getSlaver(x, y));   // Red/Blue Slaver 50:50
    monsters.add(new Looter(x, y));

    // 1/3 확률로 선택
    return monsters.get(AbstractDungeon.miscRng.random(0, monsters.size() - 1));
}
```

**bottomGetWeakWildlife** (Line 900-907):
```java
private static AbstractMonster bottomGetWeakWildlife(float x, float y) {
    ArrayList<AbstractMonster> monsters = new ArrayList<>();
    monsters.add(getLouse(x, y));        // Normal/Defensive Louse 50:50
    monsters.add(new SpikeSlime_M(x, y));
    monsters.add(new AcidSlime_M(x, y));

    // 1/3 확률로 선택
    return monsters.get(AbstractDungeon.miscRng.random(0, monsters.size() - 1));
}
```

**가능한 조합**:
- Louse + Cultist
- Louse + Slaver (Red/Blue)
- Louse + Looter
- SpikeSlime_M + Cultist
- SpikeSlime_M + Slaver
- SpikeSlime_M + Looter
- AcidSlime_M + Cultist
- AcidSlime_M + Slaver
- AcidSlime_M + Looter

**총 경우의 수**: 3 × 3 = 9가지 (Louse 타입 2종 × Slaver 타입 2종 고려 시 18가지)

### Exordium Wildlife

**파일**: `MonsterHelper.java:842-858`

```java
private static MonsterGroup bottomWildlife() {
    int numMonster = 2;  // 항상 2마리
    AbstractMonster[] monsters = new AbstractMonster[numMonster];

    if (numMonster == 2) {
        monsters[0] = bottomGetStrongWildlife(randomXOffset(-150.0F), randomYOffset(20.0F));
        monsters[1] = bottomGetWeakWildlife(randomXOffset(150.0F), randomYOffset(20.0F));
    }
    // 3마리 코드는 주석 처리됨 (사용 안 함)

    return new MonsterGroup(monsters);
}
```

**bottomGetStrongWildlife** (Line 884-891):
```java
private static AbstractMonster bottomGetStrongWildlife(float x, float y) {
    ArrayList<AbstractMonster> monsters = new ArrayList<>();
    monsters.add(new FungiBeast(x, y));
    monsters.add(new JawWorm(x, y));

    // 50:50 확률
    return monsters.get(AbstractDungeon.miscRng.random(0, monsters.size() - 1));
}
```

**가능한 조합**:
- FungiBeast + Louse
- FungiBeast + SpikeSlime_M
- FungiBeast + AcidSlime_M
- JawWorm + Louse
- JawWorm + SpikeSlime_M
- JawWorm + AcidSlime_M

**총 경우의 수**: 2 × 3 = 6가지

### Gremlin Gang

**파일**: `MonsterHelper.java:770-808`

```java
private static MonsterGroup spawnGremlins() {
    ArrayList<String> gremlinPool = new ArrayList<>();
    gremlinPool.add("GremlinWarrior");
    gremlinPool.add("GremlinWarrior");   // 2개 (가중치 2)
    gremlinPool.add("GremlinThief");
    gremlinPool.add("GremlinThief");     // 2개 (가중치 2)
    gremlinPool.add("GremlinFat");
    gremlinPool.add("GremlinFat");       // 2개 (가중치 2)
    gremlinPool.add("GremlinTsundere");  // 1개 (가중치 1)
    gremlinPool.add("GremlinWizard");    // 1개 (가중치 1)

    AbstractMonster[] retVal = new AbstractMonster[4];

    // 4마리를 중복 없이 뽑기
    for (int i = 0; i < 4; i++) {
        int index = AbstractDungeon.miscRng.random(gremlinPool.size() - 1);
        String key = gremlinPool.get(index);
        gremlinPool.remove(index);  // 뽑은 그렘린 제거 (중복 방지)
        retVal[i] = getGremlin(key, POSITIONS[i]);
    }

    return new MonsterGroup(retVal);
}
```

**확률 분석**:
- Warrior: 2/8 = 25%
- Thief: 2/8 = 25%
- Fat: 2/8 = 25%
- Tsundere: 1/8 = 12.5%
- Wizard: 1/8 = 12.5%

**특징**:
- **중복 없음**: 같은 그렘린이 2번 나올 수 없음
- **랜덤 위치**: 4개 고정 위치에 랜덤 배치

### Small Slimes

**파일**: `MonsterHelper.java:690-703`

```java
private static MonsterGroup spawnSmallSlimes() {
    AbstractMonster[] retVal = new AbstractMonster[2];

    // 50:50 확률로 2가지 조합
    if (AbstractDungeon.miscRng.randomBoolean()) {
        retVal[0] = new SpikeSlime_S(-230.0F, 32.0F, 0);
        retVal[1] = new AcidSlime_M(35.0F, 8.0F);
    } else {
        retVal[0] = new AcidSlime_S(-230.0F, 32.0F, 0);
        retVal[1] = new SpikeSlime_M(35.0F, 8.0F);
    }

    return new MonsterGroup(retVal);
}
```

**가능한 조합**:
1. SpikeSlime_S + AcidSlime_M (50%)
2. AcidSlime_S + SpikeSlime_M (50%)

### Large Slime

**파일**: `MonsterHelper.java:382-386`

```java
case "Large Slime":
    if (AbstractDungeon.miscRng.randomBoolean()) {
        return new MonsterGroup(new AcidSlime_L(0.0F, 0.0F));
    }
    return new MonsterGroup(new SpikeSlime_L(0.0F, 0.0F));
```

**가능한 조합**:
1. AcidSlime_L (50%)
2. SpikeSlime_L (50%)

### Lots of Slimes

**파일**: `MonsterHelper.java:706-768`

```java
private static MonsterGroup spawnManySmallSlimes() {
    ArrayList<String> slimePool = new ArrayList<>();
    slimePool.add("SpikeSlime_S");
    slimePool.add("SpikeSlime_S");
    slimePool.add("SpikeSlime_S");  // 3개 (60%)
    slimePool.add("AcidSlime_S");
    slimePool.add("AcidSlime_S");   // 2개 (40%)

    AbstractMonster[] retVal = new AbstractMonster[5];

    // 5마리를 중복 허용하여 뽑기
    for (int i = 0; i < 5; i++) {
        int index = AbstractDungeon.miscRng.random(slimePool.size() - 1);
        String key = slimePool.get(index);
        slimePool.remove(index);  // 뽑은 슬라임 제거

        if (key.equals("SpikeSlime_S")) {
            retVal[i] = new SpikeSlime_S(POSITIONS[i], 0);
        } else {
            retVal[i] = new AcidSlime_S(POSITIONS[i], 0);
        }
    }

    return new MonsterGroup(retVal);
}
```

**확률 분석**:
- SpikeSlime_S: 3/5 = 60%
- AcidSlime_S: 2/5 = 40%

**가능한 조합**:
- 최소 Acid: 0마리 (Spike 5마리)
- 최대 Acid: 2마리 (Spike 3마리)
- 예: SSSAA, SSSSA, SASAS, ASSSA 등 다양한 순서

---

## 수정 방법

### 1. 몬스터 풀 확률 변경

**목표**: "Gremlin Gang" 확률을 증가시키기

**파일**: `Exordium.java`

```java
@SpirePatch(
    cls = "com.megacrit.cardcrawl.dungeons.Exordium",
    method = "generateStrongEnemies",
    paramtypez = { int.class }
)
public static class GremlinGangWeightPatch {
    @SpireInsertPatch(rloc = 2)  // "Gremlin Gang" 추가 직후
    public static void Insert(Exordium __instance, int count) {
        // 가중치를 1.0에서 3.0으로 변경하려면
        // MonsterInfo 리스트에 직접 접근하거나
        // Prefix로 가로채서 커스텀 리스트 사용
    }
}
```

**더 나은 방법**: Prefix로 전체 메서드 대체

```java
@SpirePatch(
    cls = "com.megacrit.cardcrawl.dungeons.Exordium",
    method = "generateStrongEnemies",
    paramtypez = { int.class }
)
public static class CustomStrongEnemiesPatch {
    @SpirePrefixPatch
    public static SpireReturn<Void> Prefix(Exordium __instance, int count) {
        ArrayList<MonsterInfo> monsters = new ArrayList<>();
        monsters.add(new MonsterInfo("Blue Slaver", 2.0F));
        monsters.add(new MonsterInfo("Gremlin Gang", 5.0F));  // 증가!
        monsters.add(new MonsterInfo("Looter", 2.0F));
        // ... (나머지 몬스터)

        MonsterInfo.normalizeWeights(monsters);

        // populateFirstStrongEnemy와 populateMonsterList 호출
        // (Reflection으로 접근 필요)

        return SpireReturn.Return(null);  // 원본 메서드 스킵
    }
}
```

### 2. 특정 몬스터 강제 추가

**목표**: 첫 전투를 "Cultist"로 고정

**파일**: `AbstractDungeon.java`

```java
@SpirePatch(
    cls = "com.megacrit.cardcrawl.dungeons.AbstractDungeon",
    method = "populateMonsterList",
    paramtypez = { ArrayList.class, int.class, boolean.class }
)
public static class ForceFirstMonsterPatch {
    @SpirePrefixPatch
    public static void Prefix(AbstractDungeon __instance,
                              ArrayList<MonsterInfo> monsters,
                              int numMonsters,
                              boolean elites) {
        if (!elites && __instance.monsterList.isEmpty()) {
            // 첫 번째 몬스터 강제 설정
            __instance.monsterList.add("Cultist");
        }
    }
}
```

### 3. 제외 규칙 추가

**목표**: "Looter" 다음에 "Red Slaver" 금지

**파일**: `Exordium.java`

```java
@SpirePatch(
    cls = "com.megacrit.cardcrawl.dungeons.Exordium",
    method = "generateExclusions"
)
public static class CustomExclusionPatch {
    @SpirePostfixPatch
    public static ArrayList<String> Postfix(ArrayList<String> retVal, Exordium __instance) {
        String lastMonster = __instance.monsterList.get(__instance.monsterList.size() - 1);

        if (lastMonster.equals("Looter")) {
            retVal.add("Red Slaver");
        }

        return retVal;
    }
}
```

### 4. 새로운 몬스터 조합 추가

**목표**: "Giant Jaw Worms" 추가 (JawWorm 3마리)

**Step 1**: MonsterHelper에 encounter 추가

```java
@SpirePatch(
    cls = "com.megacrit.cardcrawl.helpers.MonsterHelper",
    method = "getEncounter",
    paramtypez = { String.class }
)
public static class NewEncounterPatch {
    @SpirePrefixPatch
    public static SpireReturn<MonsterGroup> Prefix(String key) {
        if (key.equals("Giant Jaw Worms")) {
            return SpireReturn.Return(new MonsterGroup(new AbstractMonster[] {
                new JawWorm(-350.0F, 25.0F),
                new JawWorm(-125.0F, 10.0F),
                new JawWorm(80.0F, 30.0F)
            }));
        }
        return SpireReturn.Continue();
    }
}
```

**Step 2**: 몬스터 풀에 추가

```java
@SpirePatch(
    cls = "com.megacrit.cardcrawl.dungeons.Exordium",
    method = "generateStrongEnemies",
    paramtypez = { int.class }
)
public static class AddGiantJawWormsPatch {
    @SpirePrefixPatch
    public static SpireReturn<Void> Prefix(Exordium __instance, int count) {
        ArrayList<MonsterInfo> monsters = new ArrayList<>();
        // ... (기존 몬스터들)
        monsters.add(new MonsterInfo("Giant Jaw Worms", 1.5F));  // 추가!

        MonsterInfo.normalizeWeights(monsters);
        // ... (populateMonsterList 호출)

        return SpireReturn.Return(null);
    }
}
```

### 5. 연속 중복 허용

**목표**: 같은 몬스터가 연속으로 나올 수 있도록 변경

**파일**: `AbstractDungeon.java`

```java
@SpirePatch(
    cls = "com.megacrit.cardcrawl.dungeons.AbstractDungeon",
    method = "populateMonsterList",
    paramtypez = { ArrayList.class, int.class, boolean.class }
)
public static class AllowDuplicatesPatch {
    @SpirePrefixPatch
    public static SpireReturn<Void> Prefix(AbstractDungeon __instance,
                                           ArrayList<MonsterInfo> monsters,
                                           int numMonsters,
                                           boolean elites) {
        // 연속 중복 체크를 건너뛰는 커스텀 로직
        for (int i = 0; i < numMonsters; i++) {
            String toAdd = MonsterInfo.roll(monsters, __instance.monsterRng.random());
            __instance.monsterList.add(toAdd);  // 중복 체크 없이 추가
        }

        return SpireReturn.Return(null);  // 원본 메서드 스킵
    }
}
```

---

## 관련 클래스

### 핵심 클래스

| 클래스 | 경로 | 역할 |
|--------|------|------|
| **MonsterRoom** | `com.megacrit.cardcrawl.rooms.MonsterRoom` | 일반 전투 방 |
| **AbstractDungeon** | `com.megacrit.cardcrawl.dungeons.AbstractDungeon` | 던전 베이스 클래스 |
| **Exordium** | `com.megacrit.cardcrawl.dungeons.Exordium` | 1막 던전 |
| **MonsterHelper** | `com.megacrit.cardcrawl.helpers.MonsterHelper` | 몬스터 생성 헬퍼 |
| **MonsterInfo** | `com.megacrit.cardcrawl.monsters.MonsterInfo` | 몬스터 확률 정보 |
| **MonsterGroup** | `com.megacrit.cardcrawl.monsters.MonsterGroup` | 몬스터 그룹 |

### 주요 메서드

#### MonsterRoom

| 메서드 | 코드 위치 | 설명 |
|--------|----------|------|
| `onPlayerEntry()` | Line 57-64 | 방 진입 시 몬스터 할당 |
| `dropReward()` | Line 29-38 | 전투 보상 (Vintage 모드) |

#### AbstractDungeon

| 메서드 | 코드 위치 | 설명 |
|--------|----------|------|
| `getMonsterForRoomCreation()` | Line 2338-2345 | 일반 전투 몬스터 가져오기 |
| `populateMonsterList()` | Line 1324-1355 | 몬스터 리스트 채우기 |
| `populateFirstStrongEnemy()` | Line 1313-1321 | 첫 강한 적 특별 처리 |

#### Exordium

| 메서드 | 코드 위치 | 설명 |
|--------|----------|------|
| `generateMonsters()` | Line 144-148 | 전체 몬스터 풀 생성 |
| `generateWeakEnemies()` | Line 153-161 | 약한 적 풀 생성 |
| `generateStrongEnemies()` | Line 163-178 | 강한 적 풀 생성 |
| `generateExclusions()` | Line 190-215 | 제외 규칙 생성 |

#### MonsterHelper

| 메서드 | 코드 위치 | 설명 |
|--------|----------|------|
| `getEncounter()` | Line 368-572 | Key로 MonsterGroup 생성 |
| `spawnGremlins()` | Line 770-808 | 그렘린 4마리 생성 |
| `spawnSmallSlimes()` | Line 690-703 | 작은 슬라임 조합 |
| `spawnManySmallSlimes()` | Line 706-768 | 작은 슬라임 5마리 |
| `bottomHumanoid()` | Line 829-835 | 인간형 조합 |
| `bottomWildlife()` | Line 842-858 | 야생 조합 |

#### MonsterInfo

| 메서드 | 코드 위치 | 설명 |
|--------|----------|------|
| `normalizeWeights()` | Line 21-34 | 가중치 정규화 |
| `roll()` | Line 43-52 | 확률 기반 선택 |

### RNG 사용

| RNG 객체 | 용도 | 초기화 |
|----------|------|--------|
| `monsterRng` | 몬스터 리스트 생성 | `seed + actNum` |
| `miscRng` | 런타임 랜덤 (위치, 타입 등) | `seed + floorNum` |
| `aiRng` | AI 행동 결정 | (별도) |

**중요**: `monsterRng`는 던전 생성 시 고정 시드로 초기화되므로, 같은 시드로 시작하면 **항상 같은 순서**로 몬스터가 나옴

---

## 추가 호출 시점

### monsters.init()

**파일**: `MonsterGroup.java`

**역할**:
- 각 몬스터의 `usePreBattleAction()` 호출
- 몬스터 위치 초기화
- 체력, 버프 등 초기 상태 설정

**예시**: Lagavulin의 경우
```java
public void usePreBattleAction() {
    // 8턴 동안 잠자기
    addToBot(new ApplyPowerAction(this, this,
        new MetallicizePower(this, 8), 8));
}
```

### 전투 시작 후 호출 함수

1. **MonsterGroup.init()** → 각 몬스터 초기화
2. **AbstractMonster.usePreBattleAction()** → 전투 시작 시 버프/디버프
3. **AbstractMonster.takeTurn()** → 각 턴마다 호출
4. **AbstractMonster.getMove(int num)** → AI 패턴 결정
5. **AbstractMonster.damage(DamageInfo info)** → 데미지 받을 때
6. **AbstractMonster.die()** → 사망 시

---

## 참고사항

### Seed 영향

- **몬스터 순서**: 동일 시드 = 동일 순서
- **몬스터 타입**: `miscRng` 사용 (Louse Normal/Defensive, Slaver Red/Blue 등)
- **몬스터 위치**: `randomXOffset`, `randomYOffset` 사용

### 디버깅 로그

**파일**: `AbstractDungeon.java:2342`

```java
logger.info("MONSTER: " + monsterList.get(0));
```

게임 로그에서 다음 몬스터를 미리 확인 가능:
```
INFO: MONSTER: Cultist
INFO: MONSTER: Jaw Worm
INFO: MONSTER: 2 Louse
...
```

### 성능 최적화

- 몬스터 리스트는 **던전 시작 시 한 번만 생성**
- 전투마다 `getEncounter()`로 **실제 객체 생성**
- 메모리 효율적: 문자열 키 → 필요 시 객체화

### 멀티플레이어 고려사항

- 시드 기반 RNG로 **재현 가능**
- `lastCombatMetricKey`로 전투 기록
- BotDataUploader로 통계 수집

---

## 작성 정보

- **작성일**: 2025-11-08
- **대상 버전**: Slay the Spire 01-23-2019 빌드
- **분석 범위**: Act 1 (Exordium) 일반 전투 시스템 전체
