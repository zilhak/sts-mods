# Act 3 (TheBeyond) - 3막 몬스터 풀 상세 문서

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

### 3막 던전 특징

**던전 ID**: `TheBeyond`
**던전 이름**: "피안" (The Beyond)
**층수 범위**: Floor 34~50 (Act 3)
**배경**: 피안 차원 (TheBeyondScene)

### 방 생성 확률 (TheBeyond.java:83-107)

```java
// 방 타입별 생성 확률 (2막과 동일)
shopRoomChance = 0.05F;      // 상점: 5%
restRoomChance = 0.12F;      // 휴식처: 12%
treasureRoomChance = 0.0F;   // 보물방: 0%
eventRoomChance = 0.22F;     // 이벤트: 22%
eliteRoomChance = 0.08F;     // 엘리트: 8%

// 상자/유물 확률 (1, 2막과 동일)
smallChestChance = 50;       // 작은 상자: 50%
mediumChestChance = 33;      // 중간 상자: 33%
largeChestChance = 17;       // 큰 상자: 17%

commonRelicChance = 50;      // 일반 유물: 50%
uncommonRelicChance = 33;    // 고급 유물: 33%
rareRelicChance = 17;        // 희귀 유물: 17%

// 카드 업그레이드 확률 (3막만 특별!)
if (AbstractDungeon.ascensionLevel >= 12) {
    cardUpgradedChance = 0.25F;   // A12+: 25%
} else {
    cardUpgradedChance = 0.5F;    // Normal: 50%
}
```

**중요**: 3막은 **카드 업그레이드 확률이 50%**로 1, 2막(25%)보다 2배 높음!

### 몬스터 풀 구조 (TheBeyond.java:110-114)

```java
protected void generateMonsters() {
    generateWeakEnemies(2);    // 약한 적 2개
    generateStrongEnemies(12); // 강한 적 12개
    generateElites(10);        // 엘리트 10개
}
```

**구조**: 2막과 동일 (Weak 2, Strong 12, Elite 10)

### mapRng 시드 (TheBeyond.java:54)

```java
mapRng = new Random(Long.valueOf(
    Settings.seed.longValue() + (AbstractDungeon.actNum * 200)
));
```

**중요**: Act 3는 시드에 `actNum * 200`을 더함 (1막: 100, 2막: 100, 3막: 200)

---

## 호출 흐름

### 1. 던전 초기화

```
TheBeyond 생성자
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
MonsterRoom/MonsterRoomElite
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

**TheBeyond.java:150-166**:
```java
protected ArrayList<String> generateExclusions() {
    ArrayList<String> retVal = new ArrayList<>();
    switch ((String)monsterList.get(monsterList.size() - 1)) {
        case "3 Darklings":
            retVal.add("3 Darklings");
            break;
        case "Orb Walker":
            retVal.add("Orb Walker");
            break;
        case "3 Shapes":
            retVal.add("4 Shapes");
            break;
    }
    return retVal;
}
```

**제외 로직**:
- Weak에서 **3 Darklings** 출현 → Strong에서 **3 Darklings** 제외
- Weak에서 **Orb Walker** 출현 → Strong에서는 제외 없음 (Orb Walker가 Strong 풀에 없음)
- Weak에서 **3 Shapes** 출현 → Strong에서 **4 Shapes** 제외

**주의**: Orb Walker 제외 규칙은 의미 없음 (Strong에 Orb Walker가 없음)

---

## 약한 적 (Weak Enemies) 풀

### 몬스터 풀 구성 (TheBeyond.java:116-123)

| 몬스터 ID | 이름 | 가중치 | 정규화 확률 |
|----------|------|--------|------------|
| 3 Darklings | 어둠 3마리 | 2.0F | 33.33% |
| Orb Walker | 오브 워커 | 2.0F | 33.33% |
| 3 Shapes | 형상 3개 | 2.0F | 33.33% |

**총 가중치**: 6.0F
**정규화**: 완전 균등 분포

```java
protected void generateWeakEnemies(int count) {
    ArrayList<MonsterInfo> monsters = new ArrayList<>();
    monsters.add(new MonsterInfo("3 Darklings", 2.0F));
    monsters.add(new MonsterInfo("Orb Walker", 2.0F));
    monsters.add(new MonsterInfo("3 Shapes", 2.0F));
    MonsterInfo.normalizeWeights(monsters);
    populateMonsterList(monsters, count, false);
}
```

### 1. 3 Darklings (어둠 3마리)

**MonsterHelper.java:512-513**:
```java
case "3 Darklings":
    return new MonsterGroup(new AbstractMonster[] {
        (AbstractMonster)new Darkling(-440.0F, 10.0F),
        (AbstractMonster)new Darkling(-140.0F, 30.0F),
        (AbstractMonster)new Darkling(180.0F, -5.0F)
    });
```

**조합**: Darkling 3마리

**개별 Darkling 정보 (Darkling.java:39-79)**:

**HP**: 48~56
**Ascension 7+**: 50~59

**Ascension 2+ 데미지**:
- Chomp: 8 → 9
- Nip: 7~11 → 9~13 (랜덤)

**전투 전 준비 (usePreBattleAction - Line 82-85)**:
```java
// cannotLose 활성화 (Darkling이 모두 죽어야 승리)
(AbstractDungeon.getCurrRoom()).cannotLose = true;

// Regrow 파워 부여 (죽으면 부활)
AbstractDungeon.actionManager.addToBottom(
    new ApplyPowerAction(this, this, new RegrowPower(this))
);
```

**패턴 (Darkling.java:134-170)**:

**halfDead 상태** (HP 0 이하):
- **REINCARNATE (5)**: maxHP의 50% 회복 + Regrow 파워 재부여

**일반 패턴**:
- **First Turn**:
  - 50%: HARDEN (Block 12, A17+: Block 12 + Str 2)
  - 50%: NIP (9~13 damage)
- **이후**:
  - Chomp 미사용 또는 연속 2회 미사용 시: CHOMP (9×2)
  - 그 외: NIP

**takeTurn 로직 (Darkling.java:88-131)**:
```java
public void takeTurn() {
    switch (this.nextMove) {
        case 1: // CHOMP
            AbstractDungeon.actionManager.addToBottom(
                new ChangeStateAction(this, "ATTACK")
            );
            AbstractDungeon.actionManager.addToBottom(new WaitAction(0.5F));
            // 2회 공격
            AbstractDungeon.actionManager.addToBottom(
                new DamageAction(AbstractDungeon.player,
                    this.damage.get(0), AttackEffect.BLUNT_HEAVY)
            );
            AbstractDungeon.actionManager.addToBottom(
                new DamageAction(AbstractDungeon.player,
                    this.damage.get(0), AttackEffect.BLUNT_HEAVY)
            );
            break;

        case 2: // HARDEN
            AbstractDungeon.actionManager.addToBottom(
                new GainBlockAction(this, this, 12)
            );
            if (AbstractDungeon.ascensionLevel >= 17) {
                AbstractDungeon.actionManager.addToBottom(
                    new ApplyPowerAction(this, this,
                        new StrengthPower(this, 2), 2)
                );
            }
            break;

        case 3: // NIP
            AbstractDungeon.actionManager.addToBottom(
                new AnimateFastAttackAction(this)
            );
            AbstractDungeon.actionManager.addToBottom(
                new DamageAction(AbstractDungeon.player,
                    this.damage.get(1), AttackEffect.BLUNT_LIGHT)
            );
            break;

        case 4: // COUNT (대사만)
            AbstractDungeon.actionManager.addToBottom(
                new TextAboveCreatureAction(this, DIALOG[0])
            );
            break;

        case 5: // REINCARNATE
            // 효과음
            if (MathUtils.randomBoolean()) {
                AbstractDungeon.actionManager.addToBottom(
                    new SFXAction("DARKLING_REGROW_2",
                        MathUtils.random(-0.1F, 0.1F))
                );
            } else {
                AbstractDungeon.actionManager.addToBottom(
                    new SFXAction("DARKLING_REGROW_1",
                        MathUtils.random(-0.1F, 0.1F))
                );
            }
            // maxHP의 50% 회복
            AbstractDungeon.actionManager.addToBottom(
                new HealAction(this, this, this.maxHealth / 2)
            );
            // REVIVE 상태로 전환
            AbstractDungeon.actionManager.addToBottom(
                new ChangeStateAction(this, "REVIVE")
            );
            // Regrow 파워 재부여
            AbstractDungeon.actionManager.addToBottom(
                new ApplyPowerAction(this, this, new RegrowPower(this), 1)
            );
            // 유물 onSpawnMonster 트리거
            for (AbstractRelic r : AbstractDungeon.player.relics) {
                r.onSpawnMonster(this);
            }
            break;
    }

    AbstractDungeon.actionManager.addToBottom(new RollMoveAction(this));
}
```

**중요 메커니즘**:
- **cannotLose**: 3마리 중 하나라도 살아있으면 전투 종료 불가
- **Regrow 파워**: 첫 번째 죽음 시 부활 (각 Darkling당 1회)
- **부활**: maxHP의 50% 회복 + Regrow 재부여 (최대 2회 부활 가능)
- **Ascension 17+**: HARDEN 시 Strength +2 추가

**전략적 고려사항**:
- 3마리 모두 1회씩 부활 가능 (총 6마리 처치 필요)
- AoE 공격으로 동시 처치 유도
- Strength 버프 누적 방지 (A17+)

### 2. Orb Walker (오브 워커)

**MonsterHelper.java:528-529**:
```java
case "Orb Walker":
    return new MonsterGroup((AbstractMonster)new OrbWalker(-30.0F, 20.0F));
```

단일 몬스터 조우.

**기본 스탯 (OrbWalker.java:27-64)**:
```java
// HP
HP_MIN = 90;
HP_MAX = 96;
A_2_HP_MIN = 92;
A_2_HP_MAX = 102;

// 데미지
LASER_DMG = 10;
CLAW_DMG = 15;
A_2_LASER_DMG = 11;
A_2_CLAW_DMG = 16;
```

**전투 전 준비 (usePreBattleAction - OrbWalker.java:67-74)**:
```java
if (AbstractDungeon.ascensionLevel >= 17) {
    AbstractDungeon.actionManager.addToBottom(
        new ApplyPowerAction(this, this,
            new GenericStrengthUpPower(this, MOVES[0], 5))
    );
} else {
    AbstractDungeon.actionManager.addToBottom(
        new ApplyPowerAction(this, this,
            new GenericStrengthUpPower(this, MOVES[0], 3))
    );
}
```

**GenericStrengthUpPower**: 매 턴 종료 시 Strength +3 or +5 (A17+)

**패턴 (OrbWalker.java:97-110)**:
```java
protected void getMove(int num) {
    if (num < 40) {
        if (!lastTwoMoves((byte)2)) {
            setMove((byte)2, Intent.ATTACK, this.damage.get(1).base);
        } else {
            setMove((byte)1, Intent.ATTACK_DEBUFF, this.damage.get(0).base);
        }
    } else {
        if (!lastMove((byte)1)) {
            setMove((byte)1, Intent.ATTACK_DEBUFF, this.damage.get(0).base);
        } else {
            setMove((byte)2, Intent.ATTACK, this.damage.get(1).base);
        }
    }
}
```

**패턴 흐름**:
```
num 0~39 (40%):
  - Claw 연속 2회 미사용 시: CLAW (15 or 16 damage)
  - 그 외: LASER (10 or 11 damage + Burn 1장)

num 40~99 (60%):
  - Laser 직후가 아니면: LASER (10 or 11 damage + Burn 1장)
  - 그 외: CLAW (15 or 16 damage)
```

**takeTurn 로직 (OrbWalker.java:78-94)**:
```java
public void takeTurn() {
    switch (this.nextMove) {
        case 2: // CLAW
            AbstractDungeon.actionManager.addToBottom(
                new AnimateSlowAttackAction(this)
            );
            AbstractDungeon.actionManager.addToBottom(
                new DamageAction(AbstractDungeon.player,
                    this.damage.get(1), AttackEffect.SLASH_HEAVY)
            );
            break;

        case 1: // LASER
            AbstractDungeon.actionManager.addToBottom(
                new ChangeStateAction(this, "ATTACK")
            );
            AbstractDungeon.actionManager.addToBottom(new WaitAction(0.4F));
            AbstractDungeon.actionManager.addToBottom(
                new DamageAction(AbstractDungeon.player,
                    this.damage.get(0), AttackEffect.FIRE)
            );
            // Burn 카드 추가
            AbstractDungeon.actionManager.addToBottom(
                new MakeTempCardInDiscardAndDeckAction(new Burn())
            );
            break;
    }
    AbstractDungeon.actionManager.addToBottom(new RollMoveAction(this));
}
```

**중요 포인트**:
- **GenericStrengthUpPower**: 매 턴 Str +3 or +5 누적 → 빠른 처치 필요
- **Burn 카드**: LASER 공격 시 버리는 더미와 덱에 Burn 1장 추가
- **높은 HP**: 90~102로 Weak 치고는 높은 편

### 3. 3 Shapes (형상 3개)

**MonsterHelper.java:517-518**:
```java
case "3 Shapes":
    return spawnShapes(true);
```

**spawnShapes 함수 (MonsterHelper.java:643-690)**:
```java
private static MonsterGroup spawnShapes(boolean weak) {
    AbstractMonster[] retVal;
    ArrayList<String> shapePool = new ArrayList<>();
    shapePool.add("Repulsor");  // 2개
    shapePool.add("Repulsor");
    shapePool.add("Exploder");  // 2개
    shapePool.add("Exploder");
    shapePool.add("Spiker");    // 2개
    shapePool.add("Spiker");

    if (weak) {
        retVal = new AbstractMonster[3];  // 3 Shapes
    } else {
        retVal = new AbstractMonster[4];  // 4 Shapes
    }

    // 첫 번째 Shape
    int index = AbstractDungeon.miscRng.random(shapePool.size() - 1);
    String key = shapePool.get(index);
    shapePool.remove(index);
    retVal[0] = getShape(key, -480.0F, 6.0F);

    // 두 번째 Shape
    index = AbstractDungeon.miscRng.random(shapePool.size() - 1);
    key = shapePool.get(index);
    shapePool.remove(index);
    retVal[1] = getShape(key, -240.0F, -6.0F);

    // 세 번째 Shape
    index = AbstractDungeon.miscRng.random(shapePool.size() - 1);
    key = shapePool.get(index);
    shapePool.remove(index);
    retVal[2] = getShape(key, 0.0F, 16.0F);

    // 네 번째 Shape (4 Shapes만)
    if (!weak) {
        index = AbstractDungeon.miscRng.random(shapePool.size() - 1);
        key = shapePool.get(index);
        retVal[3] = getShape(key, 240.0F, -16.0F);
    }

    return new MonsterGroup(retVal);
}
```

**Shape 종류별 확률**:
- **Repulsor**: 2/6 = 33.33%
- **Exploder**: 2/6 = 33.33%
- **Spiker**: 2/6 = 33.33%

**조합 예시**:
- Repulsor + Exploder + Spiker
- Repulsor + Repulsor + Exploder
- Exploder + Exploder + Spiker
- 등등 (3개 중복 가능)

**배치 좌표**:
- Position 1: (-480.0F, 6.0F)
- Position 2: (-240.0F, -6.0F)
- Position 3: (0.0F, 16.0F)

**Shape 타입별 특징**:
- **Repulsor**: 밀어내기 공격
- **Exploder**: 자폭 공격
- **Spiker**: 가시 공격

---

## 강한 적 (Strong Enemies) 풀

### 몬스터 풀 구성 (TheBeyond.java:125-138)

| 몬스터 ID | 이름 | 가중치 | 정규화 확률 |
|----------|------|--------|------------|
| Spire Growth | 첨탑 성장체 | 1.0F | 12.5% |
| Transient | 일시적 존재 | 1.0F | 12.5% |
| 4 Shapes | 형상 4개 | 1.0F | 12.5% |
| Maw | 아귀 | 1.0F | 12.5% |
| Sphere and 2 Shapes | 구체 + 형상 2개 | 1.0F | 12.5% |
| Jaw Worm Horde | 턱벌레 무리 | 1.0F | 12.5% |
| 3 Darklings | 어둠 3마리 | 1.0F | 12.5% |
| Writhing Mass | 꿈틀대는 덩어리 | 1.0F | 12.5% |

**총 가중치**: 8.0F
**완전 균등 분포**: 각 12.5%

```java
protected void generateStrongEnemies(int count) {
    ArrayList<MonsterInfo> monsters = new ArrayList<>();
    monsters.add(new MonsterInfo("Spire Growth", 1.0F));
    monsters.add(new MonsterInfo("Transient", 1.0F));
    monsters.add(new MonsterInfo("4 Shapes", 1.0F));
    monsters.add(new MonsterInfo("Maw", 1.0F));
    monsters.add(new MonsterInfo("Sphere and 2 Shapes", 1.0F));
    monsters.add(new MonsterInfo("Jaw Worm Horde", 1.0F));
    monsters.add(new MonsterInfo("3 Darklings", 1.0F));
    monsters.add(new MonsterInfo("Writhing Mass", 1.0F));
    MonsterInfo.normalizeWeights(monsters);
    populateFirstStrongEnemy(monsters, generateExclusions());
    populateMonsterList(monsters, count, false);
}
```

**중요**: 모든 Strong 적이 **완전 균등 확률**!

### 1. Spire Growth (첨탑 성장체)

**MonsterHelper.java:530-531**:
```java
case "Spire Growth":
    return new MonsterGroup((AbstractMonster)new SpireGrowth());
```

단일 몬스터.

### 2. Transient (일시적 존재)

**MonsterHelper.java:510-511**:
```java
case "Transient":
    return new MonsterGroup((AbstractMonster)new Transient());
```

단일 몬스터.

**특수 메커니즘**: 특정 턴 내에 처치하지 못하면 사라짐 (도주).

### 3. 4 Shapes (형상 4개)

**MonsterHelper.java:534-535**:
```java
case "4 Shapes":
    return spawnShapes(false);
```

**spawnShapes(false) 호출**: weak=false → 4마리 생성

**조합**: Repulsor/Exploder/Spiker 중 랜덤 4개

**배치 좌표**:
- Position 1: (-480.0F, 6.0F)
- Position 2: (-240.0F, -6.0F)
- Position 3: (0.0F, 16.0F)
- Position 4: (240.0F, -16.0F)

**제외 규칙**: Weak에서 "3 Shapes" 출현 시 제외됨.

### 4. Maw (아귀)

**MonsterHelper.java:532-533**:
```java
case "Maw":
    return new MonsterGroup((AbstractMonster)new Maw(-70.0F, 20.0F));
```

단일 몬스터.

### 5. Sphere and 2 Shapes (구체 + 형상 2개)

**MonsterHelper.java:536-539**:
```java
case "Sphere and 2 Shapes":
    return new MonsterGroup(new AbstractMonster[] {
        getAncientShape(-435.0F, 10.0F),
        getAncientShape(-210.0F, 0.0F),
        (AbstractMonster)new SphericGuardian(110.0F, 10.0F)
    });
```

**조합**: AncientShape 2개 + SphericGuardian 1기

**getAncientShape 함수**: Repulsor/Exploder/Spiker 중 랜덤 반환

**주의**: SphericGuardian은 2막 Weak 몬스터이지만 3막 Strong 조합에 재등장!

### 6. Jaw Worm Horde (턱벌레 무리)

**MonsterHelper.java:519-520**:
```java
case "Jaw Worm Horde":
    return new MonsterGroup(new AbstractMonster[] {
        (AbstractMonster)new JawWorm(-490.0F, -5.0F, true),
        (AbstractMonster)new JawWorm(-150.0F, 20.0F, true),
        (AbstractMonster)new JawWorm(175.0F, 5.0F, true)
    });
```

**조합**: JawWorm 3마리

**매개변수**: `new JawWorm(x, y, setMaxPower=true)` - 세 번째 매개변수 `true`는 강화된 버전

**주의**: JawWorm은 1막 Weak 몬스터이지만 3막에서는 3마리 강화 버전으로 등장!

### 7. 3 Darklings (어둠 3마리)

Weak 풀과 동일하지만 Strong 풀에도 포함됨.

**제외 규칙**: Weak에서 "3 Darklings" 출현 시 Strong에서도 제외됨.

### 8. Writhing Mass (꿈틀대는 덩어리)

**MonsterHelper.java:544-545**:
```java
case "Writhing Mass":
    return new MonsterGroup((AbstractMonster)new WrithingMass());
```

단일 몬스터.

---

## 엘리트 적 (Elite Enemies) 풀

### 엘리트 풀 구성 (TheBeyond.java:140-147)

| 몬스터 ID | 이름 | 가중치 | 정규화 확률 |
|----------|------|--------|------------|
| Giant Head | 거대한 머리 | 2.0F | 33.33% |
| Nemesis | 복수자 | 2.0F | 33.33% |
| Reptomancer | 파충술사 | 2.0F | 33.33% |

**총 가중치**: 6.0F
**완전 균등 분포**: 각 33.33%

```java
protected void generateElites(int count) {
    ArrayList<MonsterInfo> monsters = new ArrayList<>();
    monsters.add(new MonsterInfo("Giant Head", 2.0F));
    monsters.add(new MonsterInfo("Nemesis", 2.0F));
    monsters.add(new MonsterInfo("Reptomancer", 2.0F));
    MonsterInfo.normalizeWeights(monsters);
    populateMonsterList(monsters, count, true);
}
```

### 1. Giant Head (거대한 머리)

**MonsterHelper.java:546-547**:
```java
case "Giant Head":
    return new MonsterGroup((AbstractMonster)new GiantHead());
```

단일 엘리트 몬스터.

### 2. Nemesis (복수자)

**MonsterHelper.java:542-543**:
```java
case "Nemesis":
    return new MonsterGroup((AbstractMonster)new Nemesis());
```

단일 엘리트 몬스터.

### 3. Reptomancer (파충술사)

**MonsterHelper.java:506-507**:
```java
case "Reptomancer":
    return new MonsterGroup(new AbstractMonster[] {
        (AbstractMonster)new SnakeDagger(Reptomancer.POSX[1], Reptomancer.POSY[1]),
        (AbstractMonster)new Reptomancer(),
        (AbstractMonster)new SnakeDagger(Reptomancer.POSX[0], Reptomancer.POSY[0])
    });
```

**조합**: SnakeDagger 2마리 + Reptomancer 1마리

**배치**: Reptomancer를 중앙에, SnakeDagger를 양옆에 배치

**Reptomancer.POSX/POSY**: Reptomancer 클래스의 정적 좌표 배열

---

## 보스 (Boss) 풀

### 보스 선택 로직 (TheBeyond.java:169-203)

```java
protected void initializeBoss() {
    bossList.clear();

    // Daily Run인 경우
    if (Settings.isDailyRun) {
        bossList.add("Awakened One");
        bossList.add("Time Eater");
        bossList.add("Donu and Deca");
        Collections.shuffle(bossList, new Random(monsterRng.randomLong()));
    }
    // 처음 보는 보스 우선 배정
    else if (!UnlockTracker.isBossSeen("CROW")) {
        bossList.add("Awakened One");
    } else if (!UnlockTracker.isBossSeen("DONUT")) {
        bossList.add("Donu and Deca");
    } else if (!UnlockTracker.isBossSeen("WIZARD")) {
        bossList.add("Time Eater");
    } else {
        // 모두 본 경우 랜덤
        bossList.add("Awakened One");
        bossList.add("Time Eater");
        bossList.add("Donu and Deca");
        Collections.shuffle(bossList, new Random(monsterRng.randomLong()));
    }

    // 안전 장치
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

### 보스 목록

| 보스 ID | 이름 | UnlockTracker ID | 우선순위 |
|---------|------|------------------|---------|
| Awakened One | 각성한 자 | CROW | 1순위 |
| Donu and Deca | 도누와 데카 | DONUT | 2순위 |
| Time Eater | 시간 포식자 | WIZARD | 3순위 |

**선택 메커니즘**:
1. **Daily Run**: 3개 모두 추가 후 랜덤 셔플
2. **일반 플레이**: 미발견 보스 우선 (CROW → DONUT → WIZARD 순)
3. **모두 발견**: 3개 모두 추가 후 랜덤 셔플
4. **안전 장치**: bossList 크기 검증 및 복구

**UnlockTracker ID 주의**:
- Awakened One: **"CROW"** (까마귀)
- Donu and Deca: **"DONUT"** (도넛)
- Time Eater: **"WIZARD"** (마법사)

---

## 몬스터 상세 정보

### Darkling (어둠) - 상세 분석

**클래스**: `com.megacrit.cardcrawl.monsters.beyond.Darkling`

**기본 스탯 (Darkling.java:39-79)**:
```java
// HP
HP_MIN = 48;
HP_MAX = 56;
A_2_HP_MIN = 50;
A_2_HP_MAX = 59;

// 데미지
BITE_DMG = 8;
A_2_BITE_DMG = 9;

// Nip 데미지 (랜덤)
if (AbstractDungeon.ascensionLevel >= 2) {
    this.chompDmg = 9;
    this.nipDmg = AbstractDungeon.monsterHpRng.random(9, 13);
} else {
    this.chompDmg = 8;
    this.nipDmg = AbstractDungeon.monsterHpRng.random(7, 11);
}

// 블록
BLOCK_AMT = 12;
```

**전투 전 준비 (usePreBattleAction - Darkling.java:82-85)**:
```java
// 방 설정: cannotLose (모든 Darkling 처치 전까지 승리 불가)
(AbstractDungeon.getCurrRoom()).cannotLose = true;

// Regrow 파워 부여
AbstractDungeon.actionManager.addToBottom(
    new ApplyPowerAction(this, this, new RegrowPower(this))
);
```

**Regrow 파워**:
- HP가 0 이하가 되면 halfDead 상태로 전환
- halfDead 상태에서 REINCARNATE 행동 수행
- maxHP의 50% 회복 후 부활
- Regrow 파워 재부여 (1회 더 부활 가능)

**패턴 로직 (Darkling.java:134-170)**:
```java
protected void getMove(int num) {
    // halfDead 상태: 부활
    if (this.halfDead) {
        setMove((byte)5, Intent.BUFF); // REINCARNATE
        return;
    }

    // First Turn
    if (this.firstMove) {
        if (num < 50) {
            if (AbstractDungeon.ascensionLevel >= 17) {
                setMove((byte)2, Intent.DEFEND_BUFF); // HARDEN + STR
            } else {
                setMove((byte)2, Intent.DEFEND); // HARDEN
            }
        } else {
            setMove((byte)3, Intent.ATTACK, this.damage.get(1).base); // NIP
        }
        this.firstMove = false;
        return;
    }

    // 이후 패턴
    if (num < 33) {
        // 33% 확률
        if (!lastMove((byte)1) || !lastMoveBefore((byte)1)) {
            // Chomp 미사용 또는 연속 2회 미사용
            setMove((byte)1, Intent.ATTACK, this.damage.get(0).base, 2, true);
        } else {
            setMove((byte)3, Intent.ATTACK, this.damage.get(1).base);
        }
    } else {
        // 67% 확률
        if (!lastTwoMoves((byte)3)) {
            // Nip 연속 2회 미사용
            setMove((byte)3, Intent.ATTACK, this.damage.get(1).base);
        } else {
            setMove((byte)1, Intent.ATTACK, this.damage.get(0).base, 2, true);
        }
    }
}
```

**패턴 요약**:
```
halfDead 상태:
  → REINCARNATE (부활)

First Turn:
  - 50%: HARDEN (Block 12, A17+: +Str 2)
  - 50%: NIP (7~11 or 9~13 랜덤 damage)

이후 (num 0~99):
  num 0~32 (33%):
    - Chomp 미사용 or 연속 2회 미사용: CHOMP (8 or 9 × 2)
    - 그 외: NIP

  num 33~99 (67%):
    - Nip 연속 2회 미사용: NIP
    - 그 외: CHOMP
```

**부활 메커니즘 (takeTurn case 5 - Darkling.java:113-127)**:
```java
case 5: // REINCARNATE
    // 효과음 (2종 중 랜덤)
    if (MathUtils.randomBoolean()) {
        AbstractDungeon.actionManager.addToBottom(
            new SFXAction("DARKLING_REGROW_2",
                MathUtils.random(-0.1F, 0.1F))
        );
    } else {
        AbstractDungeon.actionManager.addToBottom(
            new SFXAction("DARKLING_REGROW_1",
                MathUtils.random(-0.1F, 0.1F))
        );
    }

    // maxHP의 50% 회복
    AbstractDungeon.actionManager.addToBottom(
        new HealAction(this, this, this.maxHealth / 2)
    );

    // REVIVE 애니메이션
    AbstractDungeon.actionManager.addToBottom(
        new ChangeStateAction(this, "REVIVE")
    );

    // Regrow 파워 재부여 (1회 더 부활 가능)
    AbstractDungeon.actionManager.addToBottom(
        new ApplyPowerAction(this, this, new RegrowPower(this), 1)
    );

    // 유물 onSpawnMonster 트리거
    for (AbstractRelic r : AbstractDungeon.player.relics) {
        r.onSpawnMonster(this);
    }
    break;
```

**전략적 고려사항**:
- **cannotLose 메커니즘**: 3마리 모두 처치해야 승리 (1~2마리만 죽이면 전투 종료 안됨)
- **Regrow 파워**: 각 Darkling이 1회씩 부활 가능 → 최대 6번 처치 필요
- **부활 시 50% HP**: 빠르게 재처치 필요
- **Ascension 17+**: HARDEN 시 Strength +2 누적 → 장기전 불리
- **AoE 전략**: 여러 Darkling을 동시에 처치하여 부활 횟수 감소

**Nip 데미지 랜덤성**:
- `AbstractDungeon.monsterHpRng.random(7, 11)` or `(9, 13)` 사용
- 각 Darkling마다 생성 시점에 고정된 랜덤 값

---

### OrbWalker (오브 워커) - 상세 분석

**클래스**: `com.megacrit.cardcrawl.monsters.beyond.OrbWalker`

**기본 스탯 (OrbWalker.java:27-64)**:
```java
// HP
HP_MIN = 90;
HP_MAX = 96;
A_2_HP_MIN = 92;
A_2_HP_MAX = 102;

// 데미지
LASER_DMG = 10;
CLAW_DMG = 15;
A_2_LASER_DMG = 11;
A_2_CLAW_DMG = 16;
```

**전투 전 준비 (usePreBattleAction - OrbWalker.java:67-74)**:
```java
if (AbstractDungeon.ascensionLevel >= 17) {
    AbstractDungeon.actionManager.addToBottom(
        new ApplyPowerAction(this, this,
            new GenericStrengthUpPower(this, MOVES[0], 5))
    );
} else {
    AbstractDungeon.actionManager.addToBottom(
        new ApplyPowerAction(this, this,
            new GenericStrengthUpPower(this, MOVES[0], 3))
    );
}
```

**GenericStrengthUpPower**:
- **Normal**: 매 턴 종료 시 Strength +3
- **Ascension 17+**: 매 턴 종료 시 Strength +5

**패턴 로직 (OrbWalker.java:97-110)**:
```java
protected void getMove(int num) {
    if (num < 40) {
        // 40% 확률
        if (!lastTwoMoves((byte)2)) {
            // Claw 연속 2회 미사용
            setMove((byte)2, Intent.ATTACK, this.damage.get(1).base);
        } else {
            setMove((byte)1, Intent.ATTACK_DEBUFF, this.damage.get(0).base);
        }
    } else {
        // 60% 확률
        if (!lastMove((byte)1)) {
            // Laser 직후가 아님
            setMove((byte)1, Intent.ATTACK_DEBUFF, this.damage.get(0).base);
        } else {
            setMove((byte)2, Intent.ATTACK, this.damage.get(1).base);
        }
    }
}
```

**패턴 흐름**:
```
num 0~39 (40%):
  - Claw 연속 2회 미사용 시: CLAW (15 or 16)
  - 그 외: LASER (10 or 11 + Burn)

num 40~99 (60%):
  - Laser 직후가 아니면: LASER (10 or 11 + Burn)
  - 그 외: CLAW (15 or 16)
```

**실제 패턴 예시**:
```
Turn 1 (num 60): LASER
Turn 2 (num 30): CLAW (Claw 연속 2회 미사용)
Turn 3 (num 70): LASER (Laser 직후 아님)
Turn 4 (num 80): CLAW (Laser 직후)
Turn 5 (num 40): LASER (Claw 1회만 사용, 연속 2회 아님)
...
```

**행동 실행 (takeTurn - OrbWalker.java:78-94)**:
```java
public void takeTurn() {
    switch (this.nextMove) {
        case 2: // CLAW
            AbstractDungeon.actionManager.addToBottom(
                new AnimateSlowAttackAction(this)
            );
            AbstractDungeon.actionManager.addToBottom(
                new DamageAction(AbstractDungeon.player,
                    this.damage.get(1), AttackEffect.SLASH_HEAVY)
            );
            break;

        case 1: // LASER
            AbstractDungeon.actionManager.addToBottom(
                new ChangeStateAction(this, "ATTACK")
            );
            AbstractDungeon.actionManager.addToBottom(new WaitAction(0.4F));
            AbstractDungeon.actionManager.addToBottom(
                new DamageAction(AbstractDungeon.player,
                    this.damage.get(0), AttackEffect.FIRE)
            );
            // Burn 카드를 버리는 더미와 덱에 추가
            AbstractDungeon.actionManager.addToBottom(
                new MakeTempCardInDiscardAndDeckAction(new Burn())
            );
            break;
    }
    AbstractDungeon.actionManager.addToBottom(new RollMoveAction(this));
}
```

**Burn 카드**:
- **타입**: 상태 카드 (Status)
- **효과**: Unplayable (플레이 불가)
- **추가 위치**: 버리는 더미 + 덱 (각 1장씩)
- **누적**: LASER 사용할 때마다 2장씩 증가

**전략적 고려사항**:
- **GenericStrengthUpPower**: 매 턴 Str +3 or +5 누적 → 장기전 매우 불리
- **Burn 카드**: 덱을 오염시켜 핸드 질 저하 → 빠른 처치 필수
- **높은 HP**: 90~102로 3막 Weak 치고는 높음
- **Ascension 17+**: Str 증가량 +5로 극도로 위험

**Burn 카드 누적 예시**:
```
Turn 1 (LASER): Burn 2장 (버리는 더미 1 + 덱 1)
Turn 2 (CLAW): 누적 없음
Turn 3 (LASER): Burn 2장 추가 (총 4장)
Turn 4 (CLAW): 누적 없음
Turn 5 (LASER): Burn 2장 추가 (총 6장)
```

---

## 수정 방법

### 1. 카드 업그레이드 확률 변경

**목표**: 3막 카드 업그레이드 확률을 25%로 하향 (다른 막과 동일하게)

```java
@SpirePatch(
    clz = TheBeyond.class,
    method = "initializeLevelSpecificChances"
)
public class UnifyCardUpgradeChancePatch {
    @SpirePostfixPatch
    public static void Postfix(TheBeyond __instance) {
        try {
            Field cardUpgradedChanceField = TheBeyond.class
                .getSuperclass()
                .getDeclaredField("cardUpgradedChance");
            cardUpgradedChanceField.setAccessible(true);

            if (AbstractDungeon.ascensionLevel >= 12) {
                cardUpgradedChanceField.setFloat(__instance, 0.125F);
            } else {
                cardUpgradedChanceField.setFloat(__instance, 0.25F);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
```

**결과**: 3막 카드 업그레이드 확률이 1, 2막과 동일해짐 (Normal 25%, A12+ 12.5%).

---

### 2. Strong 풀 확률 불균등 변경

**목표**: Transient를 30%, 나머지를 10%로 변경

```java
@SpirePatch(
    clz = TheBeyond.class,
    method = "generateStrongEnemies"
)
public class IncreaseTransientChancePatch {
    @SpirePrefixPatch
    public static SpireReturn<Void> Prefix(TheBeyond __instance, int count) {
        ArrayList<MonsterInfo> monsters = new ArrayList<>();
        monsters.add(new MonsterInfo("Spire Growth", 1.0F));       // 10%
        monsters.add(new MonsterInfo("Transient", 3.0F));          // 30%
        monsters.add(new MonsterInfo("4 Shapes", 1.0F));           // 10%
        monsters.add(new MonsterInfo("Maw", 1.0F));                // 10%
        monsters.add(new MonsterInfo("Sphere and 2 Shapes", 1.0F)); // 10%
        monsters.add(new MonsterInfo("Jaw Worm Horde", 1.0F));     // 10%
        monsters.add(new MonsterInfo("3 Darklings", 1.0F));        // 10%
        monsters.add(new MonsterInfo("Writhing Mass", 1.0F));      // 10%

        MonsterInfo.normalizeWeights(monsters);

        try {
            ArrayList<String> exclusions = (ArrayList<String>)
                TheBeyond.class.getDeclaredMethod("generateExclusions")
                    .invoke(__instance);

            Method populateFirstMethod = TheBeyond.class
                .getSuperclass()
                .getDeclaredMethod("populateFirstStrongEnemy",
                    ArrayList.class, ArrayList.class);
            populateFirstMethod.setAccessible(true);
            populateFirstMethod.invoke(__instance, monsters, exclusions);

            Method populateMethod = TheBeyond.class
                .getSuperclass()
                .getDeclaredMethod("populateMonsterList",
                    ArrayList.class, int.class, boolean.class);
            populateMethod.setAccessible(true);
            populateMethod.invoke(__instance, monsters, count, false);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return SpireReturn.Return(null);
    }
}
```

**결과**: Transient 3.0/10.0 = 30%, 나머지 각 10%.

---

### 3. Darkling 부활 횟수 제한

**목표**: Darkling이 1회만 부활하도록 변경 (Regrow 재부여 제거)

```java
@SpirePatch(
    clz = Darkling.class,
    method = "takeTurn"
)
public class LimitDarklingRevivePatch {
    @SpireInsertPatch(
        locator = Locator.class
    )
    public static void Insert(Darkling __instance) {
        // Regrow 재부여 액션 제거
        // (takeTurn의 case 5에서 ApplyPowerAction 제거)
    }

    private static class Locator extends SpireInsertLocator {
        @Override
        public int[] Locate(CtBehavior ctMethodToPatch) throws Exception {
            Matcher matcher = new Matcher.MethodCallMatcher(
                ApplyPowerAction.class, "<init>"
            );
            int[] lines = LineFinder.findInOrder(
                ctMethodToPatch, matcher
            );
            // REINCARNATE case의 ApplyPowerAction만 찾기
            return new int[] { lines[lines.length - 1] };
        }
    }
}

// 또는 더 간단한 방법: Regrow 파워 자체를 수정
@SpirePatch(
    clz = RegrowPower.class,
    method = "onDeath"
)
public class SingleReviveRegrowPatch {
    @SpirePrefixPatch
    public static SpireReturn<Void> Prefix(RegrowPower __instance) {
        // 부활 1회만 허용 (amount 체크)
        if (__instance.amount > 1) {
            return SpireReturn.Return(null);
        }
        return SpireReturn.Continue();
    }
}
```

**결과**: Darkling이 1회만 부활 (총 2번 처치).

---

### 4. OrbWalker Strength 증가량 감소

**목표**: GenericStrengthUpPower를 Normal 2, A17+ 3으로 감소

```java
@SpirePatch(
    clz = OrbWalker.class,
    method = "usePreBattleAction"
)
public class ReduceOrbWalkerStrengthPatch {
    @SpirePrefixPatch
    public static SpireReturn<Void> Prefix(OrbWalker __instance) {
        if (AbstractDungeon.ascensionLevel >= 17) {
            AbstractDungeon.actionManager.addToBottom(
                new ApplyPowerAction(__instance, __instance,
                    new GenericStrengthUpPower(__instance,
                        OrbWalker.MOVES[0], 3))
            );
        } else {
            AbstractDungeon.actionManager.addToBottom(
                new ApplyPowerAction(__instance, __instance,
                    new GenericStrengthUpPower(__instance,
                        OrbWalker.MOVES[0], 2))
            );
        }

        return SpireReturn.Return(null);
    }
}
```

**결과**: Normal Str +2/턴, A17+ Str +3/턴.

---

### 5. Weak 풀에 새로운 몬스터 추가

**목표**: "2 Orb Walkers" 조우 추가

```java
@SpirePatch(
    clz = TheBeyond.class,
    method = "generateWeakEnemies"
)
public class AddDoubleOrbWalkerPatch {
    @SpirePostfixPatch
    public static void Postfix(TheBeyond __instance, int count) {
        try {
            Field monsterListField = TheBeyond.class
                .getSuperclass()
                .getDeclaredField("monsterList");
            monsterListField.setAccessible(true);
            ArrayList<String> monsterList =
                (ArrayList<String>)monsterListField.get(__instance);

            // "2 Orb Walkers" 1개 추가
            int insertIndex = AbstractDungeon.monsterRng.random(0, monsterList.size());
            monsterList.add(insertIndex, "2 Orb Walkers");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

// MonsterHelper에 조우 정의 (이미 존재하는 경우 생략 가능)
// MonsterHelper.java:540-541 참조
```

**결과**: "2 Orb Walkers" 조우가 Weak 풀에 추가됨.

---

### 6. Shape 조합 고정

**목표**: 3 Shapes를 항상 Repulsor 3개로 고정

```java
@SpirePatch(
    clz = MonsterHelper.class,
    method = "spawnShapes"
)
public class FixedRepulsorShapesPatch {
    @SpirePrefixPatch
    public static SpireReturn<MonsterGroup> Prefix(boolean weak) {
        if (weak) {
            // 3 Shapes: Repulsor 3개
            AbstractMonster[] retVal = new AbstractMonster[3];
            retVal[0] = new Repulsor(-480.0F, 6.0F);
            retVal[1] = new Repulsor(-240.0F, -6.0F);
            retVal[2] = new Repulsor(0.0F, 16.0F);

            return SpireReturn.Return(new MonsterGroup(retVal));
        }
        return SpireReturn.Continue();
    }
}
```

**결과**: "3 Shapes"는 항상 Repulsor 3개로 구성됨.

---

### 7. 제외 규칙 추가

**목표**: Weak에서 Orb Walker 출현 시 Strong에서 "Sphere and 2 Shapes" 제외

```java
@SpirePatch(
    clz = TheBeyond.class,
    method = "generateExclusions"
)
public class ExtendedExclusionPatch {
    @SpirePostfixPatch
    public static ArrayList<String> Postfix(
        ArrayList<String> retVal,
        TheBeyond __instance
    ) {
        try {
            Field monsterListField = TheBeyond.class
                .getSuperclass()
                .getDeclaredField("monsterList");
            monsterListField.setAccessible(true);
            ArrayList<String> monsterList =
                (ArrayList<String>)monsterListField.get(__instance);

            String lastWeakEnemy = monsterList.get(monsterList.size() - 1);

            if (lastWeakEnemy.equals("Orb Walker")) {
                retVal.add("Sphere and 2 Shapes");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return retVal;
    }
}
```

**결과**: Weak "Orb Walker" → Strong "Sphere and 2 Shapes" 제외.

---

### 8. 보스 선택 로직 변경

**목표**: Time Eater를 항상 첫 번째 보스로 고정

```java
@SpirePatch(
    clz = TheBeyond.class,
    method = "initializeBoss"
)
public class ForceTimeEaterBossPatch {
    @SpirePrefixPatch
    public static SpireReturn<Void> Prefix(TheBeyond __instance) {
        try {
            Field bossListField = TheBeyond.class
                .getSuperclass()
                .getDeclaredField("bossList");
            bossListField.setAccessible(true);
            ArrayList<String> bossList =
                (ArrayList<String>)bossListField.get(__instance);

            bossList.clear();
            bossList.add("Time Eater");
            bossList.add("Time Eater"); // 안전 장치용
        } catch (Exception e) {
            e.printStackTrace();
        }

        return SpireReturn.Return(null);
    }
}
```

**결과**: Time Eater가 항상 3막 보스로 등장.

---

### 9. Elite 풀 확률 변경

**목표**: Reptomancer 50%, Giant Head 30%, Nemesis 20%

```java
@SpirePatch(
    clz = TheBeyond.class,
    method = "generateElites"
)
public class UnequalEliteChancePatch {
    @SpirePrefixPatch
    public static SpireReturn<Void> Prefix(TheBeyond __instance, int count) {
        ArrayList<MonsterInfo> monsters = new ArrayList<>();
        monsters.add(new MonsterInfo("Giant Head", 3.0F));    // 30%
        monsters.add(new MonsterInfo("Nemesis", 2.0F));       // 20%
        monsters.add(new MonsterInfo("Reptomancer", 5.0F));   // 50%

        MonsterInfo.normalizeWeights(monsters);

        try {
            Method populateMethod = TheBeyond.class
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

**결과**: Reptomancer 50%, Giant Head 30%, Nemesis 20%.

---

### 10. 커스텀 Strong 조우 추가

**목표**: "Maw and Writhing Mass" 조합 추가

```java
@SpirePatch(
    clz = TheBeyond.class,
    method = "generateStrongEnemies"
)
public class AddMawWrithingComboPatch {
    @SpirePostfixPatch
    public static void Postfix(TheBeyond __instance, int count) {
        try {
            Field monsterListField = TheBeyond.class
                .getSuperclass()
                .getDeclaredField("monsterList");
            monsterListField.setAccessible(true);
            ArrayList<String> monsterList =
                (ArrayList<String>)monsterListField.get(__instance);

            // "Maw and Writhing Mass" 2개 추가
            for (int i = 0; i < 2; i++) {
                int insertIndex = 2 + AbstractDungeon.monsterRng
                    .random(monsterList.size() - 2);
                monsterList.add(insertIndex, "Maw and Writhing Mass");
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
public class MawWrithingEncounterPatch {
    @SpireInsertPatch(
        locator = Locator.class
    )
    public static SpireReturn<MonsterGroup> Insert(String key) {
        if (key.equals("Maw and Writhing Mass")) {
            return SpireReturn.Return(
                new MonsterGroup(
                    new AbstractMonster[] {
                        new Maw(-200.0F, 20.0F),
                        new WrithingMass(150.0F, 0.0F)
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

**결과**: "Maw and Writhing Mass" 조합이 Strong 풀에 추가됨.

---

## 관련 클래스

### 던전 및 구조 클래스

| 클래스 | 경로 | 역할 |
|--------|------|------|
| TheBeyond | `com.megacrit.cardcrawl.dungeons.TheBeyond` | Act 3 던전 메인 클래스 |
| AbstractDungeon | `com.megacrit.cardcrawl.dungeons.AbstractDungeon` | 던전 기본 클래스 |
| TheBeyondScene | `com.megacrit.cardcrawl.scenes.TheBeyondScene` | 피안 배경 씬 |
| MonsterInfo | `com.megacrit.cardcrawl.monsters.MonsterInfo` | 몬스터 정보 및 가중치 |
| MonsterHelper | `com.megacrit.cardcrawl.helpers.MonsterHelper` | 몬스터 조우 생성 헬퍼 |

### Weak 몬스터 클래스

| 클래스 | 경로 | 역할 |
|--------|------|------|
| Darkling | `com.megacrit.cardcrawl.monsters.beyond.Darkling` | 어둠 (부활 메커니즘) |
| OrbWalker | `com.megacrit.cardcrawl.monsters.beyond.OrbWalker` | 오브 워커 (Burn 카드) |
| Repulsor | `com.megacrit.cardcrawl.monsters.beyond.Repulsor` | 형상 - 밀어내기 |
| Exploder | `com.megacrit.cardcrawl.monsters.beyond.Exploder` | 형상 - 자폭 |
| Spiker | `com.megacrit.cardcrawl.monsters.beyond.Spiker` | 형상 - 가시 |

### Strong 몬스터 클래스

| 클래스 | 경로 | 역할 |
|--------|------|------|
| SpireGrowth | `com.megacrit.cardcrawl.monsters.beyond.SpireGrowth` | 첨탑 성장체 |
| Transient | `com.megacrit.cardcrawl.monsters.beyond.Transient` | 일시적 존재 (턴 제한) |
| Maw | `com.megacrit.cardcrawl.monsters.beyond.Maw` | 아귀 |
| WrithingMass | `com.megacrit.cardcrawl.monsters.beyond.WrithingMass` | 꿈틀대는 덩어리 |
| SphericGuardian | `com.megacrit.cardcrawl.monsters.city.SphericGuardian` | 구체 수호자 (2막 재사용) |
| JawWorm | `com.megacrit.cardcrawl.monsters.exordium.JawWorm` | 턱벌레 (1막 재사용, 강화 버전) |

### Elite 몬스터 클래스

| 클래스 | 경로 | 역할 |
|--------|------|------|
| GiantHead | `com.megacrit.cardcrawl.monsters.beyond.GiantHead` | 거대한 머리 |
| Nemesis | `com.megacrit.cardcrawl.monsters.beyond.Nemesis` | 복수자 |
| Reptomancer | `com.megacrit.cardcrawl.monsters.beyond.Reptomancer` | 파충술사 |
| SnakeDagger | `com.megacrit.cardcrawl.monsters.beyond.SnakeDagger` | 뱀 단검 (Reptomancer 하수인) |

### Boss 몬스터 클래스

| 클래스 | 경로 | 역할 |
|--------|------|------|
| AwakenedOne | `com.megacrit.cardcrawl.monsters.beyond.AwakenedOne` | 각성한 자 |
| TimeEater | `com.megacrit.cardcrawl.monsters.beyond.TimeEater` | 시간 포식자 |
| Donu | `com.megacrit.cardcrawl.monsters.beyond.Donu` | 도누 (듀얼 보스) |
| Deca | `com.megacrit.cardcrawl.monsters.beyond.Deca` | 데카 (듀얼 보스) |

### 파워 및 카드 클래스

| 클래스 | 경로 | 역할 |
|--------|------|------|
| RegrowPower | `com.megacrit.cardcrawl.powers.RegrowPower` | 부활 메커니즘 (Darkling) |
| GenericStrengthUpPower | `com.megacrit.cardcrawl.powers.GenericStrengthUpPower` | 매 턴 Strength 증가 |
| Burn | `com.megacrit.cardcrawl.cards.status.Burn` | 플레이 불가 상태 카드 |

### 유틸리티 클래스

| 클래스 | 경로 | 역할 |
|--------|------|------|
| Random | `com.megacrit.cardcrawl.random.Random` | 시드 기반 RNG |
| UnlockTracker | `com.megacrit.cardcrawl.unlock.UnlockTracker` | 해금 및 발견 추적 |
| Settings | `com.megacrit.cardcrawl.core.Settings` | 게임 설정 |

---

## 참고 사항

### Act별 비교

| 항목 | Act 1 | Act 2 | Act 3 | 변화 |
|------|-------|-------|-------|------|
| Weak 수 | 3개 | 2개 | 2개 | 1→2: -1 |
| Strong 수 | 9개 | 12개 | 12개 | 1→2: +3 |
| Elite 수 | 10개 | 10개 | 10개 | 동일 |
| 카드 업그레이드 확률 | 25% | 25% | 50% | 3막만 2배 |

### 3막 특수사항

**1. 카드 업그레이드 확률**:
- Normal: **50%** (1, 2막의 2배!)
- Ascension 12+: **25%** (1, 2막과 동일)

**이유**: 3막은 최종 막이므로 더 강한 카드가 필요함.

**2. 몬스터 재사용**:
- **Darkling**: 3막 Weak + Strong 양쪽에 등장
- **SphericGuardian**: 2막 Weak → 3막 Strong 조합
- **JawWorm**: 1막 Weak → 3막 Strong (강화 버전)

**3. Shape 시스템**:
- **spawnShapes 함수**: weak 매개변수로 3개/4개 구분
- **랜덤 조합**: Repulsor/Exploder/Spiker 중 중복 가능
- **3 Shapes → 4 Shapes 제외**: 유사 조우 연속 방지

### Darkling 메커니즘 상세

**cannotLose 시스템**:
```java
// Darkling.usePreBattleAction (Line 83)
(AbstractDungeon.getCurrRoom()).cannotLose = true;
```

- 전투 승리 조건: **모든 Darkling 처치**
- 1~2마리만 처치 시: 전투 계속 진행
- 마지막 Darkling 처치 시: `cannotLose = false` → 승리

**Regrow 파워**:
- **트리거**: HP ≤ 0
- **효과**: halfDead 상태 전환 → REINCARNATE 행동
- **부활**: maxHP의 50% 회복 + Regrow 재부여
- **최대 부활 횟수**: 각 Darkling당 1회 (Regrow 재부여로 인해)

**부활 사이클**:
```
Darkling HP 100 (Regrow 1)
  ↓ 피해 100
Darkling HP 0 (halfDead)
  ↓ REINCARNATE
Darkling HP 50 (Regrow 1)
  ↓ 피해 50
Darkling HP 0 (halfDead)
  ↓ REINCARNATE
Darkling HP 25 (Regrow 1)
  ↓ 피해 25
Darkling HP 0 (완전 죽음)
```

실제로는 **3회 처치 필요** (초기 1회 + 부활 2회)!

### OrbWalker Burn 메커니즘

**Burn 카드 추가**:
```java
// OrbWalker.takeTurn case 1 (Line 90)
AbstractDungeon.actionManager.addToBottom(
    new MakeTempCardInDiscardAndDeckAction(new Burn())
);
```

**MakeTempCardInDiscardAndDeckAction**:
- **버리는 더미**: Burn 1장 추가
- **덱**: Burn 1장 추가 (랜덤 위치)

**누적 효과**:
- LASER 1회: Burn 2장
- LASER 5회: Burn 10장
- LASER 10회: Burn 20장

**덱 오염**: Burn 카드가 핸드에 자주 등장 → 플레이 가능한 카드 감소

### Shape 조합 확률

**spawnShapes 로직**:
1. shapePool: [Repulsor, Repulsor, Exploder, Exploder, Spiker, Spiker]
2. 첫 번째: 6개 중 랜덤 선택 후 제거
3. 두 번째: 5개 중 랜덤 선택 후 제거
4. 세 번째: 4개 중 랜덤 선택 후 제거
5. (4 Shapes) 네 번째: 3개 중 랜덤 선택

**가능한 조합 수**:
- 3 Shapes: 6 × 5 × 4 = 120 조합
- 각 타입별 확률은 복잡하지만 대략 균등

**예시 조합**:
- Repulsor × 3: 낮은 확률
- Repulsor × 2 + Exploder × 1: 중간 확률
- Repulsor + Exploder + Spiker: 높은 확률

### mapRng 시드 차이

| Act | 시드 계산 |
|-----|----------|
| Act 1 | `seed + actNum * 100` |
| Act 2 | `seed + actNum * 100` |
| Act 3 | `seed + actNum * 200` |

**Act 3만 다른 이유**: 알 수 없음 (버그 또는 의도적 설계)

---

이 문서는 Slay the Spire Act 3 몬스터 풀 시스템의 완전한 참고 자료로, 모드 제작 시 필요한 모든 정보를 포함하고 있습니다.
