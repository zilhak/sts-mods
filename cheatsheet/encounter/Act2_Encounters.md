# Act 2 (TheCity) - 2막 몬스터 풀 상세 문서

## 목차
1. [시스템 개요](#시스템-개요)
2. [호출 흐름](#호출-흐름)
3. [약한 적 (Weak Enemies) 풀](#약한-적-weak-enemies-풀)
4. [강한 적 (Strong Enemies) 풀](#강한-적-strong-enemies-풀)
5. [엘리트 적 (Elite Enemies) 풀](#엘리트-적-elite-enemies-풀)
6. [보스 (Boss) 풀](#보스-boss-풀)
7. [몬스터 상세 정보](#몬스터-상세-정보)
8. [수정 방법](#수정-방법)
9. [관련 클래스](#관련-클래스)

---

## 시스템 개요

### 2막 던전 특징

**던전 ID**: `TheCity`
**던전 이름**: "도시" (The City)
**층수 범위**: Floor 17~33 (Act 2)
**배경**: 도시 환경 (TheCityScene)

### 방 생성 확률 (TheCity.java:92-107)

```java
// 방 타입별 생성 확률
shopRoomChance = 0.05F;      // 상점: 5%
restRoomChance = 0.12F;      // 휴식처: 12%
treasureRoomChance = 0.0F;   // 보물방: 0% (전투 후 보상으로만)
eventRoomChance = 0.22F;     // 이벤트: 22%
eliteRoomChance = 0.08F;     // 엘리트: 8%

// 상자/유물 확률 (1막과 동일)
smallChestChance = 50;       // 작은 상자: 50%
mediumChestChance = 33;      // 중간 상자: 33%
largeChestChance = 17;       // 큰 상자: 17%

commonRelicChance = 50;      // 일반 유물: 50%
uncommonRelicChance = 33;    // 고급 유물: 33%
rareRelicChance = 17;        // 희귀 유물: 17%

// 카드 업그레이드 확률
if (AbstractDungeon.ascensionLevel >= 12) {
    cardUpgradedChance = 0.125F;  // A12+: 12.5%
} else {
    cardUpgradedChance = 0.25F;   // Normal: 25%
}
```

### 몬스터 풀 구조 (TheCity.java:119-123)

```java
protected void generateMonsters() {
    generateWeakEnemies(2);    // 약한 적 2개
    generateStrongEnemies(12); // 강한 적 12개
    generateElites(10);        // 엘리트 10개
}
```

**중요**: 2막은 **Weak 2개**, **Strong 12개** 구조로 1막(Weak 3개, Strong 9개)와 다름!

---

## 호출 흐름

### 1. 던전 초기화

```
TheCity 생성자
  ↓
initializeLevelSpecificChances() - 확률 설정
  ↓
generateMap() - 맵 생성
  ↓
generateMonsters() - 몬스터 풀 생성
  ├─ generateWeakEnemies(2)
  ├─ generateStrongEnemies(12)
  └─ generateElites(10)
```

### 2. 전투 진입 시

```
MonsterRoomBoss/MonsterRoomElite/MonsterRoom
  ↓
AbstractDungeon.getMonsterForRoom() 또는 getEliteMonsterForRoom()
  ↓
monsterList에서 다음 인덱스 몬스터 선택
  ↓
MonsterHelper.getEncounter(key)
  ↓
몬스터 그룹 생성 및 배치
```

### 3. 제외 규칙 (generateExclusions)

**TheCity.java:161-178**:
```java
protected ArrayList<String> generateExclusions() {
    ArrayList<String> retVal = new ArrayList<>();
    switch ((String)monsterList.get(monsterList.size() - 1)) {
        case "Spheric Guardian":
            retVal.add("Sentry and Sphere");
            break;
        case "3 Byrds":
            retVal.add("Chosen and Byrds");
            break;
        case "Chosen":
            retVal.add("Chosen and Byrds");
            retVal.add("Cultist and Chosen");
            break;
    }
    return retVal;
}
```

**제외 로직**:
- Weak에서 **Spheric Guardian** 출현 → Strong에서 **Sentry and Sphere** 제외
- Weak에서 **3 Byrds** 출현 → Strong에서 **Chosen and Byrds** 제외
- Weak에서 **Chosen** 출현 → Strong에서 **Chosen and Byrds**, **Cultist and Chosen** 제외

이는 동일 몬스터가 연속으로 등장하지 않도록 방지하기 위함.

---

## 약한 적 (Weak Enemies) 풀

### 몬스터 풀 구성 (TheCity.java:125-134)

| 몬스터 ID | 이름 | 가중치 | 정규화 확률 |
|----------|------|--------|------------|
| Spheric Guardian | 구체 수호자 | 2.0F | 20% |
| Chosen | 선택받은 자 | 2.0F | 20% |
| Shell Parasite | 껍질 기생충 | 2.0F | 20% |
| 3 Byrds | 새 3마리 | 2.0F | 20% |
| 2 Thieves | 도적 2명 | 2.0F | 20% |

**총 가중치**: 10.0F
**정규화**: 각 몬스터 2.0/10.0 = 20% 동일 확률

```java
protected void generateWeakEnemies(int count) {
    ArrayList<MonsterInfo> monsters = new ArrayList<>();
    monsters.add(new MonsterInfo("Spheric Guardian", 2.0F));
    monsters.add(new MonsterInfo("Chosen", 2.0F));
    monsters.add(new MonsterInfo("Shell Parasite", 2.0F));
    monsters.add(new MonsterInfo("3 Byrds", 2.0F));
    monsters.add(new MonsterInfo("2 Thieves", 2.0F));
    MonsterInfo.normalizeWeights(monsters);
    populateMonsterList(monsters, count, false);
}
```

### 1. Spheric Guardian (구체 수호자)

**HP**: 20 (고정)
**Ascension 7+**: 20 (변화 없음)

**패턴 (SphericGuardian.java:144-162)**:
- **First Turn**: DEFEND (25 or 35 Block - A17+)
- **Second Turn**: ATTACK_DEBUFF (10 or 11 damage + Frail 5)
- **이후 패턴**:
  - 이전 행동이 "Big Attack(1)"이었으면 → ATTACK_DEFEND (10 or 11 damage + 15 Block)
  - 그 외 → Big Attack (10 or 11 damage × 2회)

**특수 능력 (usePreBattleAction - Line 68-73)**:
```java
// 전투 시작 시
- Barricade (방어도 턴 종료 시 유지)
- Artifact 3
- Block 40
```

**Ascension 보정**:
- A2+: 데미지 10 → 11
- A17+: 첫 턴 방어도 25 → 35, Flight 3 → 4

### 2. Chosen (선택받은 자)

**HP**: 95~99
**Ascension 7+**: 98~103

**Ascension 2+ 데미지 (Chosen.java:69-77)**:
- Zap: 18 → 21
- Debilitate: 10 → 12
- Poke: 5 → 6

**패턴 (Chosen.java:154-210)**:

**Ascension 17+ 패턴**:
- **First Turn**: HEX (Hex 1 디버프)
- **이후**:
  - Debilitate(3) 또는 Drain(2) 미사용 시 (50% 확률로 선택)
  - 그 외: Zap(1) 40% / Poke(5) 60%

**Normal 패턴**:
- **First Turn**: Poke (5 or 6 damage × 2회)
- **Second Turn**: HEX (Hex 1 디버프)
- **이후**: Ascension 17+와 동일

**스킬 상세**:
1. **Poke (5)**: 5 or 6 데미지 × 2회
2. **Zap (1)**: 18 or 21 데미지 (Fire 속성)
3. **Drain (2)**: 플레이어에게 Weak 3, 자신에게 Strength 3
4. **Debilitate (3)**: 10 or 12 데미지 + Vulnerable 2
5. **Hex (4)**: Hex 1 (드로우 카드 -1)

### 3. Shell Parasite (껍질 기생충)

**MonsterHelper.java:444-445**:
```java
case "Shell Parasite":
    return new MonsterGroup((AbstractMonster)new ShelledParasite());
```

단일 몬스터 조우.

### 4. 3 Byrds (새 3마리)

**MonsterHelper.java:430-434**:
```java
case "3 Byrds":
    return new MonsterGroup(new AbstractMonster[] {
        (AbstractMonster)new Byrd(-360.0F, MathUtils.random(25.0F, 70.0F)),
        (AbstractMonster)new Byrd(-80.0F, MathUtils.random(25.0F, 70.0F)),
        (AbstractMonster)new Byrd(200.0F, MathUtils.random(25.0F, 70.0F))
    });
```

**개별 Byrd 정보 (Byrd.java:52-84)**:

**HP**: 25~31
**Ascension 7+**: 26~33

**Ascension 2+ 데미지**:
- Peck: 1 damage × 5회 → 1 damage × 6회
- Swoop: 12 → 14
- Headbutt: 3 (고정)

**전투 시작 시 (usePreBattleAction - Line 87-89)**:
```java
// Flight 3 or 4 (A17+)
AbstractDungeon.actionManager.addToBottom(
    new ApplyPowerAction(this, this, new FlightPower(this, this.flightAmt))
);
```

**패턴 (Byrd.java:171-226)**:
- **First Turn**: 37.5% Caw (Strength +1), 62.5% Peck (1×5 or 1×6)
- **Flying 상태**:
  - 50%: Peck 연속 2회면 Swoop(14) 또는 Caw / 그 외 Peck
  - 20%: Swoop 직후면 Peck 또는 Caw / 그 외 Swoop
  - 30%: Caw 직후면 Peck 또는 Swoop / 그 외 Caw
- **Grounded 상태** (Flight 소진 시):
  - Headbutt (3 damage) → 다시 Flying으로 전환

**Y 좌표 랜덤화**: 각 Byrd는 25.0F~70.0F 사이 랜덤 높이에 배치됨.

### 5. 2 Thieves (도적 2명)

**MonsterHelper.java:428-429**:
```java
case "2 Thieves":
    return new MonsterGroup(new AbstractMonster[] {
        (AbstractMonster)new Looter(-200.0F, 15.0F),
        (AbstractMonster)new Mugger(80.0F, 0.0F)
    });
```

**조합**: Looter 1명 + Mugger 1명

---

## 강한 적 (Strong Enemies) 풀

### 몬스터 풀 구성 (TheCity.java:136-149)

| 몬스터 ID | 이름 | 가중치 | 정규화 확률 |
|----------|------|--------|------------|
| Chosen and Byrds | 선택받은 자 + 새 | 2.0F | 6.25% |
| Sentry and Sphere | 파수꾼 + 구체 | 2.0F | 6.25% |
| Snake Plant | 뱀 식물 | 6.0F | 18.75% |
| Snecko | 스네코 | 4.0F | 12.5% |
| Centurion and Healer | 백부장 + 치료사 | 6.0F | 18.75% |
| Cultist and Chosen | 광신도 + 선택받은 자 | 3.0F | 9.375% |
| 3 Cultists | 광신도 3명 | 3.0F | 9.375% |
| Shelled Parasite and Fungi | 기생충 + 곰팡이 | 3.0F | 9.375% |

**총 가중치**: 32.0F

```java
protected void generateStrongEnemies(int count) {
    ArrayList<MonsterInfo> monsters = new ArrayList<>();
    monsters.add(new MonsterInfo("Chosen and Byrds", 2.0F));
    monsters.add(new MonsterInfo("Sentry and Sphere", 2.0F));
    monsters.add(new MonsterInfo("Snake Plant", 6.0F));
    monsters.add(new MonsterInfo("Snecko", 4.0F));
    monsters.add(new MonsterInfo("Centurion and Healer", 6.0F));
    monsters.add(new MonsterInfo("Cultist and Chosen", 3.0F));
    monsters.add(new MonsterInfo("3 Cultists", 3.0F));
    monsters.add(new MonsterInfo("Shelled Parasite and Fungi", 3.0F));
    MonsterInfo.normalizeWeights(monsters);
    populateFirstStrongEnemy(monsters, generateExclusions());
    populateMonsterList(monsters, count, false);
}
```

**중요 차이점**:
- `populateFirstStrongEnemy` 호출: 첫 번째 Strong 적은 Weak 적과의 제외 규칙 적용
- 12개 생성 (1막은 9개)

### 1. Chosen and Byrds (선택받은 자 + 새)

**MonsterHelper.java:455-457**:
```java
case "Chosen and Byrds":
    return new MonsterGroup(new AbstractMonster[] {
        (AbstractMonster)new Byrd(-170.0F, MathUtils.random(25.0F, 70.0F)),
        (AbstractMonster)new Chosen(80.0F, 0.0F)
    });
```

**조합**: Byrd 1마리 + Chosen 1명

**제외 규칙**: Weak에서 "3 Byrds" 또는 "Chosen" 출현 시 제외됨.

### 2. Sentry and Sphere (파수꾼 + 구체)

**MonsterHelper.java:458-459**:
```java
case "Sentry and Sphere":
    return new MonsterGroup(new AbstractMonster[] {
        (AbstractMonster)new Sentry(-305.0F, 30.0F),
        (AbstractMonster)new SphericGuardian()
    });
```

**조합**: Sentry 1기 + SphericGuardian 1기

**제외 규칙**: Weak에서 "Spheric Guardian" 출현 시 제외됨.

**주의**: Sentry는 1막 Elite 몬스터지만, 2막 Strong 전투에서는 일반 조합으로 등장!

### 3. Snake Plant (뱀 식물)

**MonsterHelper.java:460-461**:
```java
case "Snake Plant":
    return new MonsterGroup((AbstractMonster)new SnakePlant(-30.0F, -30.0F));
```

단일 몬스터. **가중치 6.0 = 18.75% 확률**로 가장 흔한 Strong 조우 중 하나.

### 4. Snecko (스네코)

**MonsterHelper.java:462-463**:
```java
case "Snecko":
    return new MonsterGroup((AbstractMonster)new Snecko());
```

단일 몬스터. **가중치 4.0 = 12.5% 확률**.

### 5. Centurion and Healer (백부장 + 치료사)

**MonsterHelper.java:464-465**:
```java
case "Centurion and Healer":
    return new MonsterGroup(new AbstractMonster[] {
        (AbstractMonster)new Centurion(-200.0F, 15.0F),
        (AbstractMonster)new Healer(120.0F, 0.0F)
    });
```

**조합**: Centurion 1명 + Healer 1명

**가중치 6.0 = 18.75%**로 가장 흔한 Strong 조우.

**전략적 중요도**: Healer를 먼저 처치하지 않으면 Centurion이 계속 회복됨.

### 6. Cultist and Chosen (광신도 + 선택받은 자)

**MonsterHelper.java:448-449**:
```java
case "Cultist and Chosen":
    return new MonsterGroup(new AbstractMonster[] {
        (AbstractMonster)new Cultist(-230.0F, 15.0F, false),
        (AbstractMonster)new Chosen(100.0F, 25.0F)
    });
```

**조합**: Cultist 1명 + Chosen 1명

**제외 규칙**: Weak에서 "Chosen" 출현 시 제외됨.

**Cultist 매개변수**: `new Cultist(x, y, setMoves)` - 세 번째 매개변수 `false`는 일반 패턴 사용.

### 7. 3 Cultists (광신도 3명)

**MonsterHelper.java:450-451**:
```java
case "3 Cultists":
    return new MonsterGroup(new AbstractMonster[] {
        (AbstractMonster)new Cultist(-465.0F, -20.0F, false),
        (AbstractMonster)new Cultist(-130.0F, 15.0F, false),
        (AbstractMonster)new Cultist(200.0F, -5.0F)
    });
```

**조합**: Cultist 3명

**배치**: 3명이 X축 및 Y축 다르게 배치 (-465, -130, 200 / -20, 15, -5).

### 8. Shelled Parasite and Fungi (껍질 기생충 + 곰팡이 짐승)

**MonsterHelper.java:466-467**:
```java
case "Shelled Parasite and Fungi":
    return new MonsterGroup(new AbstractMonster[] {
        (AbstractMonster)new ShelledParasite(-260.0F, 15.0F),
        (AbstractMonster)new FungiBeast(120.0F, 0.0F)
    });
```

**조합**: ShelledParasite 1마리 + FungiBeast 1마리

---

## 엘리트 적 (Elite Enemies) 풀

### 엘리트 풀 구성 (TheCity.java:151-158)

| 몬스터 ID | 이름 | 가중치 | 정규화 확률 |
|----------|------|--------|------------|
| Gremlin Leader | 그렘린 리더 | 1.0F | 33.33% |
| Slavers | 노예상들 | 1.0F | 33.33% |
| Book of Stabbing | 찌르기의 책 | 1.0F | 33.33% |

**총 가중치**: 3.0F

```java
protected void generateElites(int count) {
    ArrayList<MonsterInfo> monsters = new ArrayList<>();
    monsters.add(new MonsterInfo("Gremlin Leader", 1.0F));
    monsters.add(new MonsterInfo("Slavers", 1.0F));
    monsters.add(new MonsterInfo("Book of Stabbing", 1.0F));
    MonsterInfo.normalizeWeights(monsters);
    populateMonsterList(monsters, count, true);
}
```

**완전 균등 분포**: 각 엘리트 33.33% 확률.

### 1. Gremlin Leader (그렘린 리더)

**MonsterHelper.java:471-474**:
```java
case "Gremlin Leader":
    return new MonsterGroup(new AbstractMonster[] {
        spawnGremlin(GremlinLeader.POSX[0], GremlinLeader.POSY[0]),
        spawnGremlin(GremlinLeader.POSX[1], GremlinLeader.POSY[1]),
        (AbstractMonster)new GremlinLeader()
    });
```

**조합**: 랜덤 Gremlin 2마리 + GremlinLeader 1마리

**spawnGremlin 로직 (MonsterHelper.java:810-822)**:
```java
private static AbstractMonster spawnGremlin(float x, float y) {
    ArrayList<String> gremlinPool = new ArrayList<>();
    gremlinPool.add("GremlinWarrior");  // 2개
    gremlinPool.add("GremlinWarrior");
    gremlinPool.add("GremlinThief");    // 2개
    gremlinPool.add("GremlinThief");
    gremlinPool.add("GremlinFat");      // 2개
    gremlinPool.add("GremlinFat");
    gremlinPool.add("GremlinTsundere"); // 1개
    gremlinPool.add("GremlinWizard");   // 1개

    return getGremlin(gremlinPool.get(
        AbstractDungeon.miscRng.random(0, gremlinPool.size() - 1)
    ), x, y);
}
```

**그렘린 종류별 확률**:
- GremlinWarrior: 25% (2/8)
- GremlinThief: 25% (2/8)
- GremlinFat: 25% (2/8)
- GremlinTsundere: 12.5% (1/8)
- GremlinWizard: 12.5% (1/8)

**전략적 고려사항**: GremlinLeader는 그렘린들에게 버프를 주므로 우선 처치 필요.

### 2. Slavers (노예상들)

**MonsterHelper.java:476-477**:
```java
case "Slavers":
    return new MonsterGroup(new AbstractMonster[] {
        (AbstractMonster)new SlaverBlue(-385.0F, -15.0F),
        (AbstractMonster)new Taskmaster(-133.0F, 0.0F),
        (AbstractMonster)new SlaverRed(125.0F, -30.0F)
    });
```

**조합**: SlaverBlue + Taskmaster + SlaverRed

**배치**: 3명이 다른 위치에 배치 (-385/-133/125).

**주의**: Taskmaster는 **보스급 이름**을 가진 몬스터로, Slaver들을 강화함.

### 3. Book of Stabbing (찌르기의 책)

**MonsterHelper.java:469-470**:
```java
case "Book of Stabbing":
    return new MonsterGroup((AbstractMonster)new BookOfStabbing());
```

단일 엘리트 몬스터.

---

## 보스 (Boss) 풀

### 보스 선택 로직 (TheCity.java:181-215)

```java
protected void initializeBoss() {
    bossList.clear();

    // Daily Run인 경우
    if (Settings.isDailyRun) {
        bossList.add("Automaton");
        bossList.add("Collector");
        bossList.add("Champ");
        Collections.shuffle(bossList, new Random(monsterRng.randomLong()));
    }
    // 처음 보는 보스 우선 배정
    else if (!UnlockTracker.isBossSeen("CHAMP")) {
        bossList.add("Champ");
    } else if (!UnlockTracker.isBossSeen("AUTOMATON")) {
        bossList.add("Automaton");
    } else if (!UnlockTracker.isBossSeen("COLLECTOR")) {
        bossList.add("Collector");
    } else {
        // 모두 본 경우 랜덤
        bossList.add("Automaton");
        bossList.add("Collector");
        bossList.add("Champ");
        Collections.shuffle(bossList, new Random(monsterRng.randomLong()));
    }

    // 안전 장치: bossList가 1개면 복제, 0개면 모두 추가 후 셔플
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

### 보스 목록

| 보스 ID | 이름 | 우선순위 |
|---------|------|---------|
| Champ | 챔피언 | 1순위 (미발견 시) |
| Automaton | 자동기계 | 2순위 (미발견 시) |
| Collector | 수집가 | 3순위 (미발견 시) |

**선택 메커니즘**:
1. **Daily Run**: 3개 모두 추가 후 랜덤 셔플
2. **일반 플레이**: 미발견 보스 우선, 모두 본 경우 랜덤 셔플
3. **안전 장치**:
   - bossList 크기가 1이면 같은 보스 추가 (2개로 만듦)
   - 비어있으면 3개 모두 추가 후 셔플

**monsterRng 사용**: `new Random(monsterRng.randomLong())`로 시드 기반 셔플 (일관성 보장).

---

## 몬스터 상세 정보

### Chosen (선택받은 자) - 상세 분석

**클래스**: `com.megacrit.cardcrawl.monsters.city.Chosen`

**기본 스탯 (Chosen.java:34-57)**:
```java
// HP
HP_MIN = 95;
HP_MAX = 99;
A_2_HP_MIN = 98;
A_2_HP_MAX = 103;

// 데미지 (Normal)
ZAP_DMG = 18;        // 불 공격
DEBILITATE_DMG = 10; // 약화 공격
POKE_DMG = 5;        // 찌르기

// 데미지 (A2+)
A_2_ZAP_DMG = 21;
A_2_DEBILITATE_DMG = 12;
A_2_POKE_DMG = 6;

// 디버프 수치
DEBILITATE_VULN = 2; // Vulnerable 2
DRAIN_STR = 3;       // Strength +3 (자신)
DRAIN_WEAK = 3;      // Weak 3 (플레이어)
HEX_AMT = 1;         // Hex 1 (드로우 -1)
```

**패턴 트리 (Chosen.java:154-210)**:

**Ascension 17+ 패턴**:
```
Turn 1: HEX (usedHex = true)
  ↓
Turn 2~:
  - lastMove가 DEBILITATE(3) 또는 DRAIN(2)가 아니면:
    - 50%: DEBILITATE (12 damage + Vuln 2)
    - 50%: DRAIN (Weak 3 → 플레이어, Str 3 → 자신)
  - 그 외:
    - 40%: ZAP (21 damage, Fire)
    - 60%: POKE (6 damage × 2)
```

**일반 패턴**:
```
Turn 1: POKE (6 × 2) (firstTurn = false)
  ↓
Turn 2: HEX (usedHex = true)
  ↓
Turn 3~: (Ascension 17+와 동일)
```

**takeTurn 로직 (Chosen.java:95-139)**:
```java
public void takeTurn() {
    switch (this.nextMove) {
        case 5: // POKE
            AbstractDungeon.actionManager.addToBottom(
                new AnimateSlowAttackAction(this)
            );
            AbstractDungeon.actionManager.addToBottom(
                new DamageAction(AbstractDungeon.player,
                    this.damage.get(2), AttackEffect.SLASH_HORIZONTAL)
            );
            AbstractDungeon.actionManager.addToBottom(
                new DamageAction(AbstractDungeon.player,
                    this.damage.get(2), AttackEffect.SLASH_VERTICAL)
            );
            break;

        case 1: // ZAP
            AbstractDungeon.actionManager.addToBottom(
                new FastShakeAction(this, 0.3F, 0.5F)
            );
            AbstractDungeon.actionManager.addToBottom(
                new DamageAction(AbstractDungeon.player,
                    this.damage.get(0), AttackEffect.FIRE)
            );
            break;

        case 2: // DRAIN
            AbstractDungeon.actionManager.addToBottom(
                new ApplyPowerAction(AbstractDungeon.player, this,
                    new WeakPower(AbstractDungeon.player, 3, true), 3)
            );
            AbstractDungeon.actionManager.addToBottom(
                new ApplyPowerAction(this, this,
                    new StrengthPower(this, 3), 3)
            );
            break;

        case 3: // DEBILITATE
            AbstractDungeon.actionManager.addToBottom(
                new AnimateSlowAttackAction(this)
            );
            AbstractDungeon.actionManager.addToBottom(
                new DamageAction(AbstractDungeon.player,
                    this.damage.get(1), AttackEffect.SLASH_HEAVY)
            );
            AbstractDungeon.actionManager.addToBottom(
                new ApplyPowerAction(AbstractDungeon.player, this,
                    new VulnerablePower(AbstractDungeon.player, 2, true), 2)
            );
            break;

        case 4: // HEX
            AbstractDungeon.actionManager.addToBottom(
                new TalkAction(this, DIALOG[0])
            );
            AbstractDungeon.actionManager.addToBottom(
                new ChangeStateAction(this, "ATTACK")
            );
            AbstractDungeon.actionManager.addToBottom(
                new WaitAction(0.2F)
            );
            AbstractDungeon.actionManager.addToBottom(
                new ApplyPowerAction(AbstractDungeon.player, this,
                    new HexPower(AbstractDungeon.player, 1))
            );
            break;
    }

    AbstractDungeon.actionManager.addToBottom(new RollMoveAction(this));
}
```

**애니메이션 (Chosen.java:142-149)**:
```java
public void changeState(String key) {
    switch (key) {
        case "ATTACK":
            this.state.setAnimation(0, "Attack", false);
            this.state.addAnimation(0, "Idle", true, 0.0F);
            break;
    }
}
```

**피격 애니메이션 (Chosen.java:218-225)**:
```java
public void damage(DamageInfo info) {
    super.damage(info);
    if (info.owner != null && info.type != DamageInfo.DamageType.THORNS
        && info.output > 0) {
        this.state.setAnimation(0, "Hit", false);
        this.state.setTimeScale(0.8F);
        this.state.addAnimation(0, "Idle", true, 0.0F);
    }
}
```

**사망 시 (Chosen.java:228-231)**:
```java
public void die() {
    super.die();
    CardCrawlGame.sound.play("CHOSEN_DEATH");
}
```

---

### SphericGuardian (구체 수호자) - 상세 분석

**클래스**: `com.megacrit.cardcrawl.monsters.city.SphericGuardian`

**기본 스탯 (SphericGuardian.java:35-43)**:
```java
HP = 20 (고정)

DMG = 10;
A_2_DMG = 11;

SLAM_AMT = 2;         // 공격 횟수
HARDEN_BLOCK = 15;    // 방어도 + 공격 시 블록
FRAIL_AMT = 5;        // Frail 지속 턴
ACTIVATE_BLOCK = 25;  // 첫 턴 블록 (Normal)
ARTIFACT_AMT = 3;     // Artifact 개수
STARTING_BLOCK_AMT = 40; // 전투 시작 블록
```

**전투 전 준비 (SphericGuardian.java:68-73)**:
```java
public void usePreBattleAction() {
    // Barricade: 방어도가 턴 종료 시 사라지지 않음
    AbstractDungeon.actionManager.addToBottom(
        new ApplyPowerAction(this, this, new BarricadePower(this))
    );

    // Artifact 3: 디버프 3회 무시
    AbstractDungeon.actionManager.addToBottom(
        new ApplyPowerAction(this, this, new ArtifactPower(this, 3))
    );

    // 시작 블록 40
    AbstractDungeon.actionManager.addToBottom(
        new GainBlockAction(this, this, 40)
    );
}
```

**패턴 로직 (SphericGuardian.java:144-162)**:
```java
protected void getMove(int num) {
    if (this.firstMove) {
        this.firstMove = false;
        setMove((byte)2, Intent.DEFEND); // ACTIVATE_BLOCK
        return;
    }

    if (this.secondMove) {
        this.secondMove = false;
        setMove((byte)4, Intent.ATTACK_DEBUFF,
            this.damage.get(0).base); // FRAIL_ATTACK
        return;
    }

    if (lastMove((byte)1)) {
        // BIG_ATTACK 직후
        setMove((byte)3, Intent.ATTACK_DEFEND,
            this.damage.get(0).base); // BLOCK_ATTACK
    } else {
        // 그 외
        setMove((byte)1, Intent.ATTACK,
            this.damage.get(0).base, 2, true); // BIG_ATTACK × 2
    }
}
```

**패턴 흐름**:
```
Turn 1: DEFEND (25 or 35 Block) - A17+: 35
  ↓
Turn 2: ATTACK_DEBUFF (10 or 11 damage + Frail 5)
  ↓
Turn 3~:
  - 이전이 BIG_ATTACK이면: ATTACK_DEFEND (11 damage + 15 Block)
  - 그 외: BIG_ATTACK (11 damage × 2)
```

**행동 실행 (SphericGuardian.java:76-118)**:
```java
public void takeTurn() {
    switch (this.nextMove) {
        case 1: // BIG_ATTACK
            AbstractDungeon.actionManager.addToBottom(
                new ChangeStateAction(this, "ATTACK")
            );
            AbstractDungeon.actionManager.addToBottom(
                new WaitAction(0.4F)
            );
            // 첫 번째 공격 (true = fast)
            AbstractDungeon.actionManager.addToBottom(
                new DamageAction(AbstractDungeon.player,
                    this.damage.get(0), AttackEffect.BLUNT_HEAVY, true)
            );
            // 두 번째 공격
            AbstractDungeon.actionManager.addToBottom(
                new DamageAction(AbstractDungeon.player,
                    this.damage.get(0), AttackEffect.BLUNT_HEAVY)
            );
            break;

        case 2: // INITIAL_BLOCK_GAIN
            if (AbstractDungeon.ascensionLevel >= 17) {
                AbstractDungeon.actionManager.addToBottom(
                    new GainBlockAction(this, this, 35)
                );
            } else {
                AbstractDungeon.actionManager.addToBottom(
                    new GainBlockAction(this, this, 25)
                );
            }
            AbstractDungeon.actionManager.addToBottom(
                new WaitAction(0.2F)
            );
            // 효과음
            if (MathUtils.randomBoolean()) {
                AbstractDungeon.actionManager.addToBottom(
                    new SFXAction("SPHERE_DETECT_VO_1")
                );
            } else {
                AbstractDungeon.actionManager.addToBottom(
                    new SFXAction("SPHERE_DETECT_VO_2")
                );
            }
            break;

        case 3: // BLOCK_ATTACK
            AbstractDungeon.actionManager.addToBottom(
                new GainBlockAction(this, this, 15)
            );
            AbstractDungeon.actionManager.addToBottom(
                new AnimateFastAttackAction(this)
            );
            AbstractDungeon.actionManager.addToBottom(
                new DamageAction(AbstractDungeon.player,
                    this.damage.get(0), AttackEffect.BLUNT_HEAVY)
            );
            break;

        case 4: // FRAIL_ATTACK
            AbstractDungeon.actionManager.addToBottom(
                new AnimateSlowAttackAction(this)
            );
            AbstractDungeon.actionManager.addToBottom(
                new DamageAction(AbstractDungeon.player,
                    this.damage.get(0), AttackEffect.BLUNT_LIGHT)
            );
            AbstractDungeon.actionManager.addToBottom(
                new ApplyPowerAction(AbstractDungeon.player, this,
                    new FrailPower(AbstractDungeon.player, 5, true), 5)
            );
            break;
    }

    AbstractDungeon.actionManager.addToBottom(new RollMoveAction(this));
}
```

**중요 포인트**:
- **Barricade 파워**: 방어도가 턴 종료 시 사라지지 않으므로, 계속 누적됨
- **Artifact 3**: 디버프가 3회까지 무시되므로 디버프 의존 덱은 불리
- **낮은 HP (20)**: 빠른 처치가 가능하지만 높은 방어도로 방어
- **Ascension 17+**: 첫 턴 방어도 25 → 35 증가

---

### Byrd (새) - 상세 분석

**클래스**: `com.megacrit.cardcrawl.monsters.city.Byrd`

**기본 스탯 (Byrd.java:37-85)**:
```java
// HP
HP_MIN = 25;
HP_MAX = 31;
A_2_HP_MIN = 26;
A_2_HP_MAX = 33;

// 데미지 (Normal)
PECK_DMG = 1;
PECK_COUNT = 5;
SWOOP_DMG = 12;
HEADBUTT_DMG = 3;

// 데미지 (A2+)
A_2_PECK_COUNT = 6;
A_2_SWOOP_DMG = 14;

// 버프
CAW_STR = 1; // Strength +1

// Flight
flightAmt = 3 (Normal), 4 (A17+)
```

**전투 전 준비 (Byrd.java:87-89)**:
```java
public void usePreBattleAction() {
    AbstractDungeon.actionManager.addToBottom(
        new ApplyPowerAction(this, this,
            new FlightPower(this, this.flightAmt))
    );
}
```

**Flight 시스템**:
- **Flight**: 플레이어의 공격을 일정 횟수 반감 (3 or 4)
- **소진 시**: "GROUNDED" 상태로 전환 → Headbutt 공격 후 다시 "FLYING"

**패턴 로직 (Byrd.java:171-226)**:
```java
protected void getMove(int num) {
    if (this.firstMove) {
        this.firstMove = false;
        if (AbstractDungeon.aiRng.randomBoolean(0.375F)) {
            setMove((byte)6, Intent.BUFF); // CAW
        } else {
            setMove((byte)1, Intent.ATTACK,
                this.damage.get(0).base, this.peckCount, true); // PECK
        }
        return;
    }

    if (this.isFlying) {
        // Flying 상태
        if (num < 50) {
            // 50% 확률
            if (lastTwoMoves((byte)1)) {
                // Peck 연속 2회 사용 시
                if (AbstractDungeon.aiRng.randomBoolean(0.4F)) {
                    setMove((byte)3, Intent.ATTACK,
                        this.damage.get(1).base); // SWOOP
                } else {
                    setMove((byte)6, Intent.BUFF); // CAW
                }
            } else {
                setMove((byte)1, Intent.ATTACK,
                    this.damage.get(0).base, this.peckCount, true); // PECK
            }
        }
        else if (num < 70) {
            // 20% 확률
            if (lastMove((byte)3)) {
                // Swoop 직후
                if (AbstractDungeon.aiRng.randomBoolean(0.375F)) {
                    setMove((byte)6, Intent.BUFF); // CAW
                } else {
                    setMove((byte)1, Intent.ATTACK,
                        this.damage.get(0).base, this.peckCount, true); // PECK
                }
            } else {
                setMove((byte)3, Intent.ATTACK,
                    this.damage.get(1).base); // SWOOP
            }
        }
        else {
            // 30% 확률
            if (lastMove((byte)6)) {
                // Caw 직후
                if (AbstractDungeon.aiRng.randomBoolean(0.2857F)) {
                    setMove((byte)3, Intent.ATTACK,
                        this.damage.get(1).base); // SWOOP
                } else {
                    setMove((byte)1, Intent.ATTACK,
                        this.damage.get(0).base, this.peckCount, true); // PECK
                }
            } else {
                setMove((byte)6, Intent.BUFF); // CAW
            }
        }
    } else {
        // Grounded 상태: Headbutt만
        setMove((byte)5, Intent.ATTACK, this.damage.get(2).base);
    }
}
```

**패턴 요약**:
```
First Turn:
  - 37.5%: CAW (Strength +1)
  - 62.5%: PECK (1×5 or 1×6)

Flying 상태 (num 0~99 랜덤):
  num 0~49 (50%):
    - Peck 연속 2회면: 40% SWOOP(14), 60% CAW
    - 그 외: PECK

  num 50~69 (20%):
    - Swoop 직후면: 37.5% CAW, 62.5% PECK
    - 그 외: SWOOP

  num 70~99 (30%):
    - Caw 직후면: 28.57% SWOOP, 71.43% PECK
    - 그 외: CAW

Grounded 상태:
  - HEADBUTT (3 damage) → GO_AIRBORNE
```

**상태 전환 (Byrd.java:138-163)**:
```java
public void changeState(String stateName) {
    AnimationState.TrackEntry e;
    switch (stateName) {
        case "FLYING":
            loadAnimation(
                "images/monsters/theCity/byrd/flying.atlas",
                "images/monsters/theCity/byrd/flying.json", 1.0F
            );
            e = this.state.setAnimation(0, "idle_flap", true);
            e.setTime(e.getEndTime() * MathUtils.random());
            updateHitbox(0.0F, 50.0F, 240.0F, 180.0F);
            break;

        case "GROUNDED":
            setMove((byte)4, Intent.STUN);
            createIntent();
            this.isFlying = false;
            loadAnimation(
                "images/monsters/theCity/byrd/grounded.atlas",
                "images/monsters/theCity/byrd/grounded.json", 1.0F
            );
            e = this.state.setAnimation(0, "idle", true);
            e.setTime(e.getEndTime() * MathUtils.random());
            updateHitbox(10.0F, -50.0F, 240.0F, 180.0F);
            break;
    }
}
```

**행동 실행 (Byrd.java:91-131)**:
```java
public void takeTurn() {
    int i;
    switch (this.nextMove) {
        case 1: // PECK
            AbstractDungeon.actionManager.addToBottom(
                new AnimateFastAttackAction(this)
            );
            for (i = 0; i < this.peckCount; i++) {
                playRandomBirdSFx(); // "MONSTER_BYRD_ATTACK_0" ~ "MONSTER_BYRD_ATTACK_5"
                AbstractDungeon.actionManager.addToBottom(
                    new DamageAction(AbstractDungeon.player,
                        this.damage.get(0), AttackEffect.BLUNT_LIGHT, true)
                );
            }
            break;

        case 5: // HEADBUTT
            AbstractDungeon.actionManager.addToBottom(
                new AnimateSlowAttackAction(this)
            );
            AbstractDungeon.actionManager.addToBottom(
                new DamageAction(AbstractDungeon.player,
                    this.damage.get(2), AttackEffect.BLUNT_HEAVY)
            );
            setMove((byte)2, Intent.UNKNOWN); // GO_AIRBORNE
            return; // RollMoveAction 생략

        case 2: // GO_AIRBORNE
            this.isFlying = true;
            AbstractDungeon.actionManager.addToBottom(
                new ChangeStateAction(this, "FLYING")
            );
            AbstractDungeon.actionManager.addToBottom(
                new ApplyPowerAction(this, this,
                    new FlightPower(this, this.flightAmt))
            );
            break;

        case 6: // CAW
            AbstractDungeon.actionManager.addToBottom(
                new SFXAction("BYRD_DEATH")
            );
            AbstractDungeon.actionManager.addToBottom(
                new TalkAction(this, DIALOG[0], 1.2F, 1.2F)
            );
            AbstractDungeon.actionManager.addToBottom(
                new ApplyPowerAction(this, this,
                    new StrengthPower(this, 1), 1)
            );
            break;

        case 3: // SWOOP
            AbstractDungeon.actionManager.addToBottom(
                new AnimateSlowAttackAction(this)
            );
            AbstractDungeon.actionManager.addToBottom(
                new DamageAction(AbstractDungeon.player,
                    this.damage.get(1), AttackEffect.SLASH_HEAVY)
            );
            break;

        case 4: // STUNNED
            AbstractDungeon.actionManager.addToBottom(
                new SetAnimationAction(this, "head_lift")
            );
            AbstractDungeon.actionManager.addToBottom(
                new TextAboveCreatureAction(this, TextType.STUNNED)
            );
            break;
    }
    AbstractDungeon.actionManager.addToBottom(new RollMoveAction(this));
}
```

**사망 시 (Byrd.java:229-232)**:
```java
public void die() {
    super.die();
    CardCrawlGame.sound.play("BYRD_DEATH");
}
```

**전략적 고려사항**:
- **Flight 파워**: 공격이 3~4회 반감되므로 약한 공격 여러 번보다 강한 공격 한 방이 유리
- **Grounded 상태**: Flight 소진 시 Headbutt(3) 후 다시 Flight 재충전 → 연속 타격으로 Flight 소진 유도 필요
- **Caw 버프**: Strength +1씩 누적되므로 빠른 처치 필요
- **다수 등장**: "3 Byrds"는 3마리가 동시 출현하므로 AoE 공격 유리

---

## 수정 방법

### 1. 몬스터 풀 확률 변경

**목표**: Snake Plant의 출현 확률을 18.75% → 30%로 증가

```java
@SpirePatch(
    clz = TheCity.class,
    method = "generateStrongEnemies"
)
public class IncreaseSnakePlantChancePatch {
    @SpirePostfixPatch
    public static void Postfix(TheCity __instance, int count) {
        // 원본 풀을 가져와서 수정
        ArrayList<MonsterInfo> monsters = new ArrayList<>();
        monsters.add(new MonsterInfo("Chosen and Byrds", 2.0F));
        monsters.add(new MonsterInfo("Sentry and Sphere", 2.0F));
        monsters.add(new MonsterInfo("Snake Plant", 10.0F)); // 6.0F → 10.0F
        monsters.add(new MonsterInfo("Snecko", 4.0F));
        monsters.add(new MonsterInfo("Centurion and Healer", 6.0F));
        monsters.add(new MonsterInfo("Cultist and Chosen", 3.0F));
        monsters.add(new MonsterInfo("3 Cultists", 3.0F));
        monsters.add(new MonsterInfo("Shelled Parasite and Fungi", 3.0F));

        MonsterInfo.normalizeWeights(monsters);

        // monsterList 필드에 접근하여 수정
        Field monsterListField = TheCity.class
            .getSuperclass()
            .getDeclaredField("monsterList");
        monsterListField.setAccessible(true);
        ArrayList<String> monsterList =
            (ArrayList<String>)monsterListField.get(__instance);

        // 기존 Strong 리스트 제거 후 재생성
        monsterList.removeIf(key ->
            key.equals("Chosen and Byrds") ||
            key.equals("Sentry and Sphere") ||
            key.equals("Snake Plant") ||
            key.equals("Snecko") ||
            key.equals("Centurion and Healer") ||
            key.equals("Cultist and Chosen") ||
            key.equals("3 Cultists") ||
            key.equals("Shelled Parasite and Fungi")
        );

        // populateMonsterList 메서드 호출
        Method populateMethod = TheCity.class
            .getSuperclass()
            .getDeclaredMethod("populateMonsterList",
                ArrayList.class, int.class, boolean.class);
        populateMethod.setAccessible(true);
        populateMethod.invoke(__instance, monsters, count, false);
    }
}
```

**결과**: Snake Plant가 10.0/36.0 = 27.78% 확률로 출현.

---

### 2. 새로운 몬스터 추가

**목표**: 2막 Strong 풀에 커스텀 몬스터 "City Guard" 추가

```java
@SpirePatch(
    clz = TheCity.class,
    method = "generateStrongEnemies"
)
public class AddCityGuardPatch {
    @SpirePostfixPatch
    public static void Postfix(TheCity __instance, int count) {
        try {
            Field monsterListField = TheCity.class
                .getSuperclass()
                .getDeclaredField("monsterList");
            monsterListField.setAccessible(true);
            ArrayList<String> monsterList =
                (ArrayList<String>)monsterListField.get(__instance);

            // Strong 풀에 City Guard 3개 추가 (가중치 3.0F 효과)
            for (int i = 0; i < 3; i++) {
                // 랜덤 위치에 삽입 (Weak 2개 이후부터)
                int insertIndex = 2 + AbstractDungeon.monsterRng
                    .random(monsterList.size() - 2);
                monsterList.add(insertIndex, "City Guard");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

// MonsterHelper에 City Guard 조우 정의 추가
@SpirePatch(
    clz = MonsterHelper.class,
    method = "getEncounter"
)
public class CityGuardEncounterPatch {
    @SpireInsertPatch(
        locator = Locator.class
    )
    public static SpireReturn<MonsterGroup> Insert(String key) {
        if (key.equals("City Guard")) {
            return SpireReturn.Return(
                new MonsterGroup(
                    new AbstractMonster[] {
                        new CityGuardMonster(-150.0F, 0.0F),
                        new CityGuardMonster(150.0F, 0.0F)
                    }
                )
            );
        }
        return SpireReturn.Continue();
    }

    private static class Locator extends SpireInsertLocator {
        @Override
        public int[] Locate(CtBehavior ctMethodToPatch) throws Exception {
            Matcher matcher = new Matcher.MethodCallMatcher(
                MonsterGroup.class, "<init>"
            );
            return LineFinder.findInOrder(
                ctMethodToPatch, matcher
            );
        }
    }
}
```

**결과**: City Guard가 Strong 풀에 추가되어 다른 Strong 적들과 같이 출현.

---

### 3. 엘리트 풀 확률 불균등 변경

**목표**: Book of Stabbing 50%, Gremlin Leader 30%, Slavers 20%

```java
@SpirePatch(
    clz = TheCity.class,
    method = "generateElites"
)
public class UnequalEliteChancePatch {
    @SpirePrefixPatch
    public static SpireReturn<Void> Prefix(
        TheCity __instance,
        int count
    ) {
        ArrayList<MonsterInfo> monsters = new ArrayList<>();
        monsters.add(new MonsterInfo("Gremlin Leader", 3.0F));  // 30%
        monsters.add(new MonsterInfo("Slavers", 2.0F));         // 20%
        monsters.add(new MonsterInfo("Book of Stabbing", 5.0F)); // 50%

        MonsterInfo.normalizeWeights(monsters);

        try {
            Method populateMethod = TheCity.class
                .getSuperclass()
                .getDeclaredMethod("populateMonsterList",
                    ArrayList.class, int.class, boolean.class);
            populateMethod.setAccessible(true);
            populateMethod.invoke(__instance, monsters, count, true);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return SpireReturn.Return(null);
    }
}
```

**결과**:
- Book of Stabbing: 5.0/10.0 = 50%
- Gremlin Leader: 3.0/10.0 = 30%
- Slavers: 2.0/10.0 = 20%

---

### 4. 보스 선택 로직 변경

**목표**: Daily Run이 아니어도 항상 3개 보스 중 랜덤 선택

```java
@SpirePatch(
    clz = TheCity.class,
    method = "initializeBoss"
)
public class AlwaysRandomBossPatch {
    @SpirePrefixPatch
    public static SpireReturn<Void> Prefix(TheCity __instance) {
        try {
            Field bossListField = TheCity.class
                .getSuperclass()
                .getDeclaredField("bossList");
            bossListField.setAccessible(true);
            ArrayList<String> bossList =
                (ArrayList<String>)bossListField.get(__instance);

            bossList.clear();
            bossList.add("Automaton");
            bossList.add("Collector");
            bossList.add("Champ");

            // monsterRng를 이용한 시드 기반 셔플
            Field monsterRngField = TheCity.class
                .getSuperclass()
                .getDeclaredField("monsterRng");
            monsterRngField.setAccessible(true);
            com.megacrit.cardcrawl.random.Random monsterRng =
                (com.megacrit.cardcrawl.random.Random)monsterRngField.get(__instance);

            Collections.shuffle(bossList,
                new java.util.Random(monsterRng.randomLong()));
        } catch (Exception e) {
            e.printStackTrace();
        }

        return SpireReturn.Return(null);
    }
}
```

**결과**: 모든 플레이에서 보스가 33.33% 균등 확률로 선택됨.

---

### 5. 제외 규칙 수정

**목표**: Weak에서 Chosen 출현 시 "3 Cultists"도 제외

```java
@SpirePatch(
    clz = TheCity.class,
    method = "generateExclusions"
)
public class ExtendedExclusionPatch {
    @SpirePostfixPatch
    public static ArrayList<String> Postfix(
        ArrayList<String> retVal,
        TheCity __instance
    ) {
        try {
            Field monsterListField = TheCity.class
                .getSuperclass()
                .getDeclaredField("monsterList");
            monsterListField.setAccessible(true);
            ArrayList<String> monsterList =
                (ArrayList<String>)monsterListField.get(__instance);

            String lastWeakEnemy = monsterList.get(monsterList.size() - 1);

            if (lastWeakEnemy.equals("Chosen")) {
                if (!retVal.contains("3 Cultists")) {
                    retVal.add("3 Cultists");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return retVal;
    }
}
```

**결과**: Weak에서 Chosen 출현 시 "Chosen and Byrds", "Cultist and Chosen", **"3 Cultists"** 모두 제외됨.

---

### 6. Chosen 패턴 변경

**목표**: Ascension 17+에서도 첫 턴에 Poke 사용

```java
@SpirePatch(
    clz = Chosen.class,
    method = "getMove"
)
public class ChosenFirstTurnPokePatch {
    @SpirePrefixPatch
    public static SpireReturn<Void> Prefix(Chosen __instance, int num) {
        try {
            Field firstTurnField = Chosen.class
                .getDeclaredField("firstTurn");
            firstTurnField.setAccessible(true);
            boolean firstTurn = firstTurnField.getBoolean(__instance);

            if (firstTurn) {
                firstTurnField.setBoolean(__instance, false);

                // POKE (5) 설정
                Field damageField = AbstractMonster.class
                    .getDeclaredField("damage");
                damageField.setAccessible(true);
                ArrayList<DamageInfo> damage =
                    (ArrayList<DamageInfo>)damageField.get(__instance);

                Method setMoveMethod = AbstractMonster.class
                    .getDeclaredMethod("setMove", byte.class,
                        AbstractMonster.Intent.class, int.class,
                        int.class, boolean.class);
                setMoveMethod.setAccessible(true);
                setMoveMethod.invoke(__instance, (byte)5,
                    AbstractMonster.Intent.ATTACK,
                    damage.get(2).base, 2, true);

                return SpireReturn.Return(null);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return SpireReturn.Continue();
    }
}
```

**결과**: Ascension 17+에서도 첫 턴에 Poke(6×2) 사용 후 Hex 패턴으로 진행.

---

### 7. SphericGuardian 시작 블록 제거

**목표**: 전투 시작 시 Block 40 제거

```java
@SpirePatch(
    clz = SphericGuardian.class,
    method = "usePreBattleAction"
)
public class RemoveSphereStartingBlockPatch {
    @SpirePrefixPatch
    public static SpireReturn<Void> Prefix(SphericGuardian __instance) {
        // Barricade와 Artifact만 부여
        AbstractDungeon.actionManager.addToBottom(
            new ApplyPowerAction(__instance, __instance,
                new BarricadePower(__instance))
        );
        AbstractDungeon.actionManager.addToBottom(
            new ApplyPowerAction(__instance, __instance,
                new ArtifactPower(__instance, 3))
        );

        // Block 40 부여 생략

        return SpireReturn.Return(null);
    }
}
```

**결과**: SphericGuardian이 시작 블록 40 없이 전투 시작 (Barricade와 Artifact 3만 유지).

---

### 8. Byrd Flight 수치 변경

**목표**: Flight를 항상 5로 고정

```java
@SpirePatch(
    clz = Byrd.class,
    method = SpirePatch.CONSTRUCTOR
)
public class ByrdFlightIncreasePatch {
    @SpirePostfixPatch
    public static void Postfix(Byrd __instance, float x, float y) {
        try {
            Field flightAmtField = Byrd.class
                .getDeclaredField("flightAmt");
            flightAmtField.setAccessible(true);
            flightAmtField.setInt(__instance, 5);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
```

**결과**: Byrd의 Flight가 Ascension 수준 관계없이 항상 5.

---

### 9. 몬스터 HP 범위 조정

**목표**: Chosen의 HP를 100~110으로 증가

```java
@SpirePatch(
    clz = Chosen.class,
    method = SpirePatch.CONSTRUCTOR,
    paramtypez = { float.class, float.class }
)
public class ChosenHPIncreasePatch {
    @SpireInsertPatch(
        locator = Locator.class
    )
    public static void Insert(Chosen __instance, float x, float y) {
        if (AbstractDungeon.ascensionLevel >= 7) {
            __instance.setHp(105, 110);
        } else {
            __instance.setHp(100, 105);
        }
    }

    private static class Locator extends SpireInsertLocator {
        @Override
        public int[] Locate(CtBehavior ctMethodToPatch) throws Exception {
            Matcher matcher = new Matcher.MethodCallMatcher(
                AbstractMonster.class, "setHp"
            );
            return LineFinder.findInOrder(
                ctMethodToPatch, matcher
            );
        }
    }
}
```

**결과**:
- Normal: HP 100~105
- Ascension 7+: HP 105~110

---

### 10. 커스텀 조합 추가

**목표**: "Chosen and Snecko" 조합 추가

```java
@SpirePatch(
    clz = TheCity.class,
    method = "generateStrongEnemies"
)
public class AddChosenSneckoComboPatch {
    @SpirePostfixPatch
    public static void Postfix(TheCity __instance, int count) {
        try {
            Field monsterListField = TheCity.class
                .getSuperclass()
                .getDeclaredField("monsterList");
            monsterListField.setAccessible(true);
            ArrayList<String> monsterList =
                (ArrayList<String>)monsterListField.get(__instance);

            // Strong 풀에 "Chosen and Snecko" 2개 추가
            for (int i = 0; i < 2; i++) {
                int insertIndex = 2 + AbstractDungeon.monsterRng
                    .random(monsterList.size() - 2);
                monsterList.add(insertIndex, "Chosen and Snecko");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

// MonsterHelper에 조우 정의 추가
@SpirePatch(
    clz = MonsterHelper.class,
    method = "getEncounter"
)
public class ChosenSneckoEncounterPatch {
    @SpireInsertPatch(
        locator = Locator.class
    )
    public static SpireReturn<MonsterGroup> Insert(String key) {
        if (key.equals("Chosen and Snecko")) {
            return SpireReturn.Return(
                new MonsterGroup(
                    new AbstractMonster[] {
                        new Chosen(-200.0F, 0.0F),
                        new Snecko(150.0F, 0.0F)
                    }
                )
            );
        }
        return SpireReturn.Continue();
    }

    private static class Locator extends SpireInsertLocator {
        @Override
        public int[] Locate(CtBehavior ctMethodToPatch) throws Exception {
            Matcher matcher = new Matcher.MethodCallMatcher(
                MonsterGroup.class, "<init>"
            );
            return LineFinder.findInOrder(
                ctMethodToPatch, matcher
            );
        }
    }
}
```

**결과**: "Chosen and Snecko" 조합이 Strong 풀에 추가되어 출현.

---

## 관련 클래스

### 던전 및 구조 클래스

| 클래스 | 경로 | 역할 |
|--------|------|------|
| TheCity | `com.megacrit.cardcrawl.dungeons.TheCity` | Act 2 던전 메인 클래스 |
| AbstractDungeon | `com.megacrit.cardcrawl.dungeons.AbstractDungeon` | 던전 기본 클래스 |
| TheCityScene | `com.megacrit.cardcrawl.scenes.TheCityScene` | 도시 배경 씬 |
| MonsterInfo | `com.megacrit.cardcrawl.monsters.MonsterInfo` | 몬스터 정보 및 가중치 |
| MonsterHelper | `com.megacrit.cardcrawl.helpers.MonsterHelper` | 몬스터 조우 생성 헬퍼 |

### Weak 몬스터 클래스

| 클래스 | 경로 | 역할 |
|--------|------|------|
| SphericGuardian | `com.megacrit.cardcrawl.monsters.city.SphericGuardian` | 구체 수호자 |
| Chosen | `com.megacrit.cardcrawl.monsters.city.Chosen` | 선택받은 자 |
| ShelledParasite | `com.megacrit.cardcrawl.monsters.city.ShelledParasite` | 껍질 기생충 |
| Byrd | `com.megacrit.cardcrawl.monsters.city.Byrd` | 새 |
| Looter | `com.megacrit.cardcrawl.monsters.city.Looter` | 약탈자 |
| Mugger | `com.megacrit.cardcrawl.monsters.city.Mugger` | 강도 |

### Strong 몬스터 클래스

| 클래스 | 경로 | 역할 |
|--------|------|------|
| SnakePlant | `com.megacrit.cardcrawl.monsters.city.SnakePlant` | 뱀 식물 |
| Snecko | `com.megacrit.cardcrawl.monsters.city.Snecko` | 스네코 |
| Centurion | `com.megacrit.cardcrawl.monsters.city.Centurion` | 백부장 |
| Healer | `com.megacrit.cardcrawl.monsters.city.Healer` | 치료사 |
| Cultist | `com.megacrit.cardcrawl.monsters.exordium.Cultist` | 광신도 (1막에서 재사용) |
| Sentry | `com.megacrit.cardcrawl.monsters.exordium.Sentry` | 파수꾼 (1막 Elite, 2막 조합) |
| FungiBeast | `com.megacrit.cardcrawl.monsters.exordium.FungiBeast` | 곰팡이 짐승 (1막에서 재사용) |

### Elite 몬스터 클래스

| 클래스 | 경로 | 역할 |
|--------|------|------|
| GremlinLeader | `com.megacrit.cardcrawl.monsters.city.GremlinLeader` | 그렘린 리더 |
| Taskmaster | `com.megacrit.cardcrawl.monsters.city.Taskmaster` | 작업반장 (Slavers 보스) |
| SlaverBlue | `com.megacrit.cardcrawl.monsters.city.SlaverBlue` | 파란 노예상 |
| SlaverRed | `com.megacrit.cardcrawl.monsters.city.SlaverRed` | 빨간 노예상 |
| BookOfStabbing | `com.megacrit.cardcrawl.monsters.city.BookOfStabbing` | 찌르기의 책 |
| GremlinWarrior | `com.megacrit.cardcrawl.monsters.exordium.GremlinWarrior` | 그렘린 전사 |
| GremlinThief | `com.megacrit.cardcrawl.monsters.exordium.GremlinThief` | 그렘린 도둑 |
| GremlinFat | `com.megacrit.cardcrawl.monsters.exordium.GremlinFat` | 뚱뚱한 그렘린 |
| GremlinTsundere | `com.megacrit.cardcrawl.monsters.exordium.GremlinTsundere` | 츤데레 그렘린 |
| GremlinWizard | `com.megacrit.cardcrawl.monsters.exordium.GremlinWizard` | 그렘린 마법사 |

### Boss 몬스터 클래스

| 클래스 | 경로 | 역할 |
|--------|------|------|
| Automaton | `com.megacrit.cardcrawl.monsters.city.Automaton` | 자동기계 |
| Collector | `com.megacrit.cardcrawl.monsters.city.Collector` | 수집가 |
| Champ | `com.megacrit.cardcrawl.monsters.city.Champ` | 챔피언 |

### 파워 및 액션 클래스

| 클래스 | 경로 | 역할 |
|--------|------|------|
| BarricadePower | `com.megacrit.cardcrawl.powers.BarricadePower` | 방어도 턴 종료 시 유지 |
| ArtifactPower | `com.megacrit.cardcrawl.powers.ArtifactPower` | 디버프 무시 |
| FlightPower | `com.megacrit.cardcrawl.powers.FlightPower` | 공격 반감 |
| HexPower | `com.megacrit.cardcrawl.powers.HexPower` | 드로우 감소 |
| FrailPower | `com.megacrit.cardcrawl.powers.FrailPower` | 방어도 25% 감소 |
| VulnerablePower | `com.megacrit.cardcrawl.powers.VulnerablePower` | 받는 데미지 50% 증가 |
| WeakPower | `com.megacrit.cardcrawl.powers.WeakPower` | 공격력 25% 감소 |
| StrengthPower | `com.megacrit.cardcrawl.powers.StrengthPower` | 공격력 증가 |

### 유틸리티 클래스

| 클래스 | 경로 | 역할 |
|--------|------|------|
| Random | `com.megacrit.cardcrawl.random.Random` | 시드 기반 RNG |
| UnlockTracker | `com.megacrit.cardcrawl.unlock.UnlockTracker` | 해금 및 발견 추적 |
| Settings | `com.megacrit.cardcrawl.core.Settings` | 게임 설정 |

---

## 참고 사항

### Weak vs Strong 차이점

| 항목 | Act 1 | Act 2 | 변화 |
|------|-------|-------|------|
| Weak 수 | 3개 | 2개 | -1 |
| Strong 수 | 9개 | 12개 | +3 |
| 제외 규칙 | 있음 | 있음 (확장) | - |

**이유**: 2막은 1막보다 난이도가 높으므로 Weak 비율을 줄이고 Strong 비율을 증가시킴.

### 1막 몬스터 재사용

2막 Strong 풀에서 1막 몬스터를 재사용:
- **Cultist**: 1막 Weak → 2막 Strong 조합
- **Sentry**: 1막 Elite → 2막 Strong 조합 ("Sentry and Sphere")
- **FungiBeast**: 1막 Strong → 2막 Strong 조합 ("Shelled Parasite and Fungi")

**주의**: 동일 몬스터도 Act에 따라 역할이 다름!

### Ascension 주요 변화

| Ascension | 변화 내용 |
|-----------|----------|
| A2+ | 대부분 몬스터의 데미지 증가 |
| A7+ | 대부분 몬스터의 HP 증가 |
| A12+ | 카드 업그레이드 확률 25% → 12.5% |
| A17+ | Chosen 첫 턴 Hex 사용, SphericGuardian Block 35, Byrd Flight 4 |

### 몬스터 배치 좌표

몬스터의 X, Y 좌표는 화면 배치를 결정:
- **X**: 좌우 위치 (-500 ~ +500, 중앙 0)
- **Y**: 상하 위치 (기본 0, Byrd는 25~70 랜덤)

**예시**:
```java
new Byrd(-360.0F, MathUtils.random(25.0F, 70.0F)) // 왼쪽, 높이 랜덤
new Byrd(-80.0F, MathUtils.random(25.0F, 70.0F))  // 중앙 왼쪽, 높이 랜덤
new Byrd(200.0F, MathUtils.random(25.0F, 70.0F))  // 오른쪽, 높이 랜덤
```

### RNG 시드 사용

```java
// TheCity.java:62-63
mapRng = new Random(Long.valueOf(Settings.seed.longValue() + (AbstractDungeon.actNum * 100)));

// TheCity.java:189
Collections.shuffle(bossList, new Random(monsterRng.randomLong()));
```

**중요**: 시드 기반 RNG를 사용하므로 동일 시드에서는 항상 같은 몬스터 순서 생성. 모드에서 수정 시 시드 일관성 유지 필요.

---

이 문서는 Slay the Spire Act 2 몬스터 풀 시스템의 완전한 참고 자료로, 모드 제작 시 필요한 모든 정보를 포함하고 있습니다.
