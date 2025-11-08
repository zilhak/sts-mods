# 특수 조우 시스템 (Special Encounters)

Slay the Spire의 특수 조우 시스템에 대한 완전한 분석 문서입니다. 일반적인 몬스터 풀과 다른 고정된 조우들(Act 4, Neow Event)을 다룹니다.

## 목차
1. [시스템 개요](#시스템-개요)
2. [Act 4: The Ending](#act-4-the-ending)
3. [Shield and Spear 조우](#shield-and-spear-조우)
4. [The Heart 보스 조우](#the-heart-보스-조우)
5. [Neow Event 시스템](#neow-event-시스템)
6. [수정 방법](#수정-방법)
7. [관련 클래스](#관련-클래스)

---

## 시스템 개요

### 특수 조우란?

특수 조우는 일반적인 몬스터 풀 생성 시스템과 다른 메커니즘으로 작동하는 조우입니다:

1. **Act 4 (The Ending)**: 고정된 맵 구조와 특정 조우만 존재
2. **Shield and Spear**: Act 4의 유일한 엘리트 조우
3. **The Heart**: Act 4의 최종 보스
4. **Neow Event**: 게임 시작 시 축복을 주는 이벤트

### 일반 조우와의 차이점

| 특징 | 일반 조우 | 특수 조우 |
|------|----------|----------|
| 맵 생성 | 랜덤 생성 (generateMap) | 고정 생성 (generateSpecialMap) |
| 몬스터 선택 | 가중치 기반 풀에서 선택 | 고정된 조우만 존재 |
| 제외 규칙 | 동적 제외 시스템 | 제외 규칙 없음 |
| 보상 | 일반 보상 테이블 | 특수 보상 (100% Medium 등) |

---

## Act 4: The Ending

### 시스템 개요

Act 4는 다른 Act들과 완전히 다른 맵 생성 시스템을 사용합니다. 랜덤 생성 대신 고정된 5개의 방으로 구성된 직선 경로입니다.

```
Rest(Y=0) → Shop(Y=1) → Elite(Y=2) → Boss(Y=3) → Victory(Y=4)
```

### 맵 생성 메커니즘

```java
// TheEnding.java:73-168
private void generateSpecialMap() {
    // Y=0: Rest Room (휴식처)
    MapRoomNode restNode = new MapRoomNode(3, 0);
    restNode.room = (AbstractRoom)new RestRoom();

    // Y=1: Shop Room (상점)
    MapRoomNode shopNode = new MapRoomNode(3, 1);
    shopNode.room = (AbstractRoom)new ShopRoom();

    // Y=2: Elite Monster Room (엘리트 전투)
    MapRoomNode enemyNode = new MapRoomNode(3, 2);
    enemyNode.room = (AbstractRoom)new MonsterRoomElite();

    // Y=3: Boss Room (보스 전투)
    MapRoomNode bossNode = new MapRoomNode(3, 3);
    bossNode.room = (AbstractRoom)new MonsterRoomBoss();

    // Y=4: Victory Room (승리)
    MapRoomNode victoryNode = new MapRoomNode(3, 4);
    victoryNode.room = (AbstractRoom)new TrueVictoryRoom();

    // 직선 연결: Rest → Shop → Elite → Boss → Victory
    restNode.addEdge(new MapEdge(3, 0, 3, 0, 3, 1, 3, 1, false));
    shopNode.addEdge(new MapEdge(3, 1, 3, 1, 3, 2, 3, 2, false));
    enemyNode.addEdge(new MapEdge(3, 2, 3, 2, 3, 3, 3, 3, false));
    bossNode.addEdge(new MapEdge(3, 3, 3, 3, 3, 4, 3, 4, false));

    // 맵 리스트에 추가
    this.map.add(new ArrayList<>());
    ((ArrayList<MapRoomNode>)this.map.get(0)).add(restNode);

    this.map.add(new ArrayList<>());
    ((ArrayList<MapRoomNode>)this.map.get(1)).add(shopNode);

    this.map.add(new ArrayList<>());
    ((ArrayList<MapRoomNode>)this.map.get(2)).add(enemyNode);

    this.map.add(new ArrayList<>());
    ((ArrayList<MapRoomNode>)this.map.get(3)).add(bossNode);

    this.map.add(new ArrayList<>());
    ((ArrayList<MapRoomNode>)this.map.get(4)).add(victoryNode);

    // 플레이어 시작 위치 설정 (Y=0, Rest Room)
    this.currMapNode = restNode;
}
```

### 몬스터 풀 생성

Act 4는 "Shield and Spear"만 존재하며, 이는 엘리트 조우로만 등장합니다.

```java
// TheEnding.java:204-214
protected void generateMonsters() {
    // 일반 몬스터 풀 (사용되지 않음)
    monsterList = new ArrayList<>();
    monsterList.add("Shield and Spear");
    monsterList.add("Shield and Spear");
    monsterList.add("Shield and Spear");

    // 엘리트 몬스터 풀 (Y=2에서 사용)
    eliteMonsterList = new ArrayList<>();
    eliteMonsterList.add("Shield and Spear");
    eliteMonsterList.add("Shield and Spear");
    eliteMonsterList.add("Shield and Spear");
}
```

### 보스 풀 생성

```java
// TheEnding.java:235-239
protected void initializeBoss() {
    // 항상 "The Heart"만 존재
    bossList.add("The Heart");
    bossList.add("The Heart");
    bossList.add("The Heart");
}
```

### Act 4 특수 규칙

#### 1. mapRng 초기화

```java
// TheEnding.java:54-62
public TheEnding() {
    super("TheEnding", "The Ending", AbstractPlayer.PlayerClass.IRONCLAD, false);

    // Act 4 전용 mapRng 초기화 (actNum * 300)
    if (Settings.isFinalActAvailable && !Settings.isEndless) {
        this.mapRng = new Random(Long.valueOf(Settings.seed.longValue() + (this.actNum * 300)));
    } else {
        this.mapRng = new Random(Long.valueOf(Settings.seed.longValue() + (this.actNum * 100)));
    }
}
```

#### 2. 보상 시스템

Act 4의 보상은 특별한 확률을 사용합니다:

```java
// TheEnding.java:216-227
protected void generateWeakEnemies(int count) {
    // Weak enemies pool - Act 4에는 존재하지 않음
    // generateSpecialMap()이 모든 방을 고정 생성하므로 호출되지 않음
}

protected void generateStrongEnemies(int count) {
    // Strong enemies pool - Act 4에는 존재하지 않음
    // generateSpecialMap()이 모든 방을 고정 생성하므로 호출되지 않음
}

protected void generateElites(int count) {
    // Elite 생성 시 "Shield and Spear"만 사용
    // Y=2 위치에 고정 배치
}
```

#### 3. 보물 상자 확률

Act 4의 보물 상자는 특수한 확률을 사용합니다 (코드에는 명시되지 않았으나 게임 내 동작):

- **Small Chest**: 0% (등장하지 않음)
- **Medium Chest**: 100% (항상 Medium)
- **Large Chest**: 0% (등장하지 않음)

#### 4. 유물 확률

Act 4 Elite 보상 유물:

- **Common Relic**: 0% (등장하지 않음)
- **Uncommon Relic**: 100% (항상 Uncommon)
- **Rare Relic**: 0% (등장하지 않음)

### Act 4 진입 조건

```java
// TheEnding.java:64-71
// Act 4 진입 조건 체크
if (Settings.isFinalActAvailable && !Settings.isEndless) {
    // The Heart를 깨기 위한 조건:
    // 1. Act 1, 2, 3 각각의 보스를 깬 후 특정 아이템 획득
    // 2. 3개의 특별한 열쇠 (Ruby Key, Emerald Key, Sapphire Key) 보유
    // 3. Act 3 보스 처치 후 Act 4로 진입
}
```

---

## Shield and Spear 조우

### 개요

Shield and Spear는 Act 4의 유일한 엘리트 조우로, 두 개의 몬스터가 동시에 등장합니다:

1. **Spire Shield** (방패): 방어와 디버프에 특화
2. **Spire Spear** (창): 공격과 버프에 특화

### Spire Shield (방패)

#### 기본 스탯

```java
// SpireShield.java:39-64
public SpireShield() {
    super(NAME, "SpireShield", HP, NORMAL_X, NORMAL_Y,
          HURTBOX_WIDTH, HURTBOX_HEIGHT, null, OFFSET_X, OFFSET_Y);

    // HP
    if (AbstractDungeon.ascensionLevel >= 8) {
        setHp(125);  // A8+: 125
    } else {
        setHp(110);  // Normal: 110
    }

    // 공격 데미지
    if (AbstractDungeon.ascensionLevel >= 3) {
        this.bashDmg = 14;   // A3+: Bash 14
        this.smashDmg = 38;  // A3+: Smash 38
    } else {
        this.bashDmg = 12;   // Normal: Bash 12
        this.smashDmg = 34;  // Normal: Smash 34
    }

    // Block 양
    this.blockAmt = 30;  // 항상 30 Block

    this.type = EnemyType.ELITE;
}
```

#### 전투 전 행동

```java
// SpireShield.java:67-76
public void usePreBattleAction() {
    // Surrounded power 부여 (플레이어가 도망칠 수 없음)
    AbstractDungeon.actionManager.addToBottom(
        new ApplyPowerAction(
            AbstractDungeon.player, this,
            new SurroundedPower(AbstractDungeon.player)
        )
    );

    // Artifact power 부여 (디버프 무효화)
    if (AbstractDungeon.ascensionLevel >= 18) {
        // A18+: Artifact 2
        AbstractDungeon.actionManager.addToBottom(
            new ApplyPowerAction(this, this, new ArtifactPower(this, 2), 2)
        );
    } else {
        // Normal: Artifact 1
        AbstractDungeon.actionManager.addToBottom(
            new ApplyPowerAction(this, this, new ArtifactPower(this, 1), 1)
        );
    }
}
```

**Surrounded Power**: 플레이어가 전투에서 도망칠 수 없게 만듭니다 (Escape 불가).

#### 행동 패턴

```java
// SpireShield.java:78-145
protected void getMove(int num) {
    // 첫 턴: 항상 Fortify (Block 30)
    if (this.firstMove) {
        setMove((byte)1, Intent.DEFEND);
        this.firstMove = false;
        return;
    }

    // 패턴 카운터에 따른 행동
    switch (this.moveCount % 4) {
        case 0:
            // Bash: 공격 + Frail 2
            setMove((byte)2, Intent.ATTACK_DEBUFF, this.bashDmg);
            break;

        case 1:
            // Fortify: Block 30
            setMove((byte)1, Intent.DEFEND);
            break;

        case 2:
            // Smash: 강력한 공격
            setMove((byte)3, Intent.ATTACK, this.smashDmg);
            break;

        case 3:
            // Fortify: Block 30
            setMove((byte)1, Intent.DEFEND);
            break;
    }

    this.moveCount++;
}

public void takeTurn() {
    switch (this.nextMove) {
        case 1: // FORTIFY
            // 30 Block 획득
            AbstractDungeon.actionManager.addToBottom(
                new GainBlockAction(this, this, this.blockAmt)
            );
            break;

        case 2: // BASH
            // 공격 + Frail 2 부여
            AbstractDungeon.actionManager.addToBottom(
                new DamageAction(
                    AbstractDungeon.player,
                    this.damage.get(0),
                    AttackEffect.BLUNT_HEAVY
                )
            );
            AbstractDungeon.actionManager.addToBottom(
                new ApplyPowerAction(
                    AbstractDungeon.player, this,
                    new FrailPower(AbstractDungeon.player, 2, true), 2
                )
            );
            break;

        case 3: // SMASH
            // 강력한 공격
            AbstractDungeon.actionManager.addToBottom(
                new DamageAction(
                    AbstractDungeon.player,
                    this.damage.get(1),
                    AttackEffect.BLUNT_HEAVY
                )
            );
            break;
    }

    AbstractDungeon.actionManager.addToBottom(new RollMoveAction(this));
}
```

#### Shield 행동 패턴 요약

| 턴 | 행동 | 효과 |
|-----|------|------|
| 1 | Fortify | Block 30 |
| 2 | Bash | 공격 (12/14) + Frail 2 |
| 3 | Fortify | Block 30 |
| 4 | Smash | 강력한 공격 (34/38) |
| 5 | Fortify | Block 30 (반복) |

### Spire Spear (창)

#### 기본 스탯

```java
// SpireSpear.java:38-66
public SpireSpear() {
    super(NAME, "SpireSpear", HP, NORMAL_X, NORMAL_Y,
          HURTBOX_WIDTH, HURTBOX_HEIGHT, null, OFFSET_X, OFFSET_Y);

    // HP
    if (AbstractDungeon.ascensionLevel >= 8) {
        setHp(180);  // A8+: 180
    } else {
        setHp(160);  // Normal: 160
    }

    // 공격 데미지
    if (AbstractDungeon.ascensionLevel >= 3) {
        this.burnStrikeDmg = 6;   // A3+: Burn Strike 6
        this.piercerDmg = 10;     // Piercer 10 (고정)
        this.skewerCount = 4;     // A3+: Skewer 4회
    } else {
        this.burnStrikeDmg = 5;   // Normal: Burn Strike 5
        this.piercerDmg = 10;     // Piercer 10 (고정)
        this.skewerCount = 3;     // Normal: Skewer 3회
    }

    // Strength 버프
    this.strAmt = 3;  // 항상 Strength +3

    this.type = EnemyType.ELITE;
}
```

#### 전투 전 행동

```java
// SpireSpear.java:69-75
public void usePreBattleAction() {
    // Artifact power 부여 (디버프 무효화)
    if (AbstractDungeon.ascensionLevel >= 18) {
        // A18+: Artifact 2
        AbstractDungeon.actionManager.addToBottom(
            new ApplyPowerAction(this, this, new ArtifactPower(this, 2), 2)
        );
    } else {
        // Normal: Artifact 1
        AbstractDungeon.actionManager.addToBottom(
            new ApplyPowerAction(this, this, new ArtifactPower(this, 1), 1)
        );
    }
}
```

#### 행동 패턴

```java
// SpireSpear.java:77-155
protected void getMove(int num) {
    // 첫 턴: 항상 Burn Strike (공격 + Burn 2장)
    if (this.firstMove) {
        setMove((byte)1, Intent.ATTACK_DEBUFF, this.burnStrikeDmg);
        this.firstMove = false;
        return;
    }

    // 패턴 카운터에 따른 행동
    switch (this.moveCount % 4) {
        case 0:
            // Skewer: 다단히트 공격 (3~4회)
            setMove((byte)3, Intent.ATTACK, this.piercerDmg, this.skewerCount, true);
            break;

        case 1:
            // Burn Strike: 공격 + Burn 2장
            setMove((byte)1, Intent.ATTACK_DEBUFF, this.burnStrikeDmg);
            break;

        case 2:
            // Skewer: 다단히트 공격 (3~4회)
            setMove((byte)3, Intent.ATTACK, this.piercerDmg, this.skewerCount, true);
            break;

        case 3:
            // Strengthen: Strength +3
            setMove((byte)2, Intent.BUFF);
            break;
    }

    this.moveCount++;
}

public void takeTurn() {
    switch (this.nextMove) {
        case 1: // BURN_STRIKE
            // 공격 + Burn 카드 2장 추가
            AbstractDungeon.actionManager.addToBottom(
                new DamageAction(
                    AbstractDungeon.player,
                    this.damage.get(0),
                    AttackEffect.SLASH_DIAGONAL
                )
            );
            AbstractDungeon.actionManager.addToBottom(
                new MakeTempCardInDiscardAction(
                    new Burn(), 2  // Burn 2장
                )
            );
            break;

        case 2: // STRENGTHEN
            // Strength +3 버프
            AbstractDungeon.actionManager.addToBottom(
                new ApplyPowerAction(
                    this, this,
                    new StrengthPower(this, this.strAmt), this.strAmt
                )
            );
            break;

        case 3: // SKEWER
            // 다단히트 공격 (3~4회)
            for (int i = 0; i < this.skewerCount; i++) {
                AbstractDungeon.actionManager.addToBottom(
                    new DamageAction(
                        AbstractDungeon.player,
                        this.damage.get(1),
                        AttackEffect.SLASH_HORIZONTAL, true
                    )
                );
            }
            break;
    }

    AbstractDungeon.actionManager.addToBottom(new RollMoveAction(this));
}
```

#### Spear 행동 패턴 요약

| 턴 | 행동 | 효과 |
|-----|------|------|
| 1 | Burn Strike | 공격 (5/6) + Burn 2장 |
| 2 | Skewer | 10 x 3(or 4)회 공격 |
| 3 | Burn Strike | 공격 (5/6) + Burn 2장 |
| 4 | Skewer | 10 x 3(or 4)회 공격 |
| 5 | Strengthen | Strength +3 |
| 6 | Burn Strike | 공격 (5/6) + Burn 2장 (반복) |

### Shield and Spear 전투 전략

#### 위협 요소

1. **Surrounded Power**: 도망칠 수 없으므로 반드시 승리해야 함
2. **높은 Artifact**: A18+에서 각각 Artifact 2를 가짐 (총 4개 디버프 무효)
3. **지속적인 방해**: Frail 2, Burn 카드 추가
4. **강력한 공격**: Smash 38 단일 공격, Skewer 10x4 다단히트
5. **증가하는 데미지**: Spear의 Strength 버프로 점점 강해짐

#### 승천 난이도 변화

| 승천 | Shield HP | Spear HP | Bash | Smash | Burn Strike | Skewer 횟수 | Artifact |
|------|-----------|----------|------|-------|-------------|------------|----------|
| A0-2 | 110 | 160 | 12 | 34 | 5 | 3 | 1 |
| A3-7 | 110 | 160 | 14 | 38 | 6 | 4 | 1 |
| A8-17 | 125 | 180 | 14 | 38 | 6 | 4 | 1 |
| A18+ | 125 | 180 | 14 | 38 | 6 | 4 | 2 |

---

## The Heart 보스 조우

### 개요

The Heart (CorruptHeart)는 Act 4의 최종 보스이며, 게임의 진정한 엔딩을 볼 수 있는 조우입니다.

### 기본 스탯

```java
// CorruptHeart.java:53-77
public CorruptHeart() {
    super(NAME, "CorruptHeart", 750, 30.0F, -30.0F,
          476.0F, 410.0F, null, -50.0F, 30.0F);

    // HP 설정
    if (AbstractDungeon.ascensionLevel >= 9) {
        setHp(800);  // A9+: 800 HP
    } else {
        setHp(750);  // Normal: 750 HP
    }

    // 데미지 설정
    if (AbstractDungeon.ascensionLevel >= 4) {
        // A4+
        this.damage.add(new DamageInfo(this, 45));  // Echo: 45
        this.damage.add(new DamageInfo(this, 2));   // Blood Shots: 2x15
        this.bloodHitCount = 15;
    } else {
        // Normal
        this.damage.add(new DamageInfo(this, 40));  // Echo: 40
        this.damage.add(new DamageInfo(this, 2));   // Blood Shots: 2x12
        this.bloodHitCount = 12;
    }

    this.type = EnemyType.BOSS;
}
```

### 전투 전 행동

```java
// CorruptHeart.java:80-95
public void usePreBattleAction() {
    // 배경음악 변경
    CardCrawlGame.music.unsilenceBGM();
    AbstractDungeon.scene.fadeOutAmbiance();
    AbstractDungeon.getCurrRoom().playBgmInstantly("BOSS_ENDING");

    // Invincible power (데미지 무효화)
    int invincibleAmt = 300;
    if (AbstractDungeon.ascensionLevel >= 19) {
        invincibleAmt -= 100;  // A19+: 200
    }

    AbstractDungeon.actionManager.addToBottom(
        new ApplyPowerAction(this, this,
            new InvinciblePower(this, invincibleAmt), invincibleAmt
        )
    );

    // Beat of Death power (턴마다 1 데미지)
    int beatAmount = 1;
    if (AbstractDungeon.ascensionLevel >= 19) {
        beatAmount++;  // A19+: Beat 2
    }

    AbstractDungeon.actionManager.addToBottom(
        new ApplyPowerAction(this, this,
            new BeatOfDeathPower(this, beatAmount), beatAmount
        )
    );
}
```

**Invincible Power**: The Heart는 전투 시작 시 300 (Normal) 또는 200 (A19+) 데미지를 무효화합니다. 이 버퍼를 먼저 깎아야 실제 HP를 감소시킬 수 있습니다.

**Beat of Death Power**: 매 턴마다 플레이어에게 1 (Normal) 또는 2 (A19+) 데미지를 줍니다. 이는 Block으로 막을 수 없습니다.

### 행동 패턴

The Heart의 행동은 3턴 주기로 반복되며, 첫 턴은 항상 DEBILITATE입니다.

```java
// CorruptHeart.java:250-278
protected void getMove(int num) {
    // 첫 턴: 항상 DEBILITATE (대규모 디버프)
    if (this.isFirstMove) {
        setMove((byte)3, Intent.STRONG_DEBUFF);
        this.isFirstMove = false;
        return;
    }

    // 3턴 주기 패턴
    switch (this.moveCount % 3) {
        case 0:
            // 50% 확률로 Blood Shots 또는 Echo
            if (AbstractDungeon.aiRng.randomBoolean()) {
                setMove((byte)1, Intent.ATTACK,
                    this.damage.get(1).base, this.bloodHitCount, true);
            } else {
                setMove((byte)2, Intent.ATTACK,
                    this.damage.get(0).base);
            }
            break;

        case 1:
            // 이전에 Echo를 사용하지 않았다면 Echo, 아니면 Blood Shots
            if (!lastMove((byte)2)) {
                setMove((byte)2, Intent.ATTACK,
                    this.damage.get(0).base);
            } else {
                setMove((byte)1, Intent.ATTACK,
                    this.damage.get(1).base, this.bloodHitCount, true);
            }
            break;

        case 2:
            // 버프
            setMove((byte)4, Intent.BUFF);
            break;
    }

    this.moveCount++;
}
```

### 행동 상세 설명

#### 1. DEBILITATE (첫 턴 고정)

```java
// CorruptHeart.java:101-158
case 3: // DEBILITATE
    // 시각 효과
    AbstractDungeon.actionManager.addToBottom(
        new VFXAction(new HeartMegaDebuffEffect())
    );

    // Vulnerable 2 부여
    AbstractDungeon.actionManager.addToBottom(
        new ApplyPowerAction(
            AbstractDungeon.player, this,
            new VulnerablePower(AbstractDungeon.player, 2, true), 2
        )
    );

    // Weak 2 부여
    AbstractDungeon.actionManager.addToBottom(
        new ApplyPowerAction(
            AbstractDungeon.player, this,
            new WeakPower(AbstractDungeon.player, 2, true), 2
        )
    );

    // Frail 2 부여
    AbstractDungeon.actionManager.addToBottom(
        new ApplyPowerAction(
            AbstractDungeon.player, this,
            new FrailPower(AbstractDungeon.player, 2, true), 2
        )
    );

    // 5가지 상태이상 카드 추가 (각 1장씩)
    // 1. Dazed
    AbstractDungeon.actionManager.addToBottom(
        new MakeTempCardInDrawPileAction(
            new Dazed(), 1, true, false, false,
            Settings.WIDTH * 0.2F, Settings.HEIGHT / 2.0F
        )
    );

    // 2. Slimed
    AbstractDungeon.actionManager.addToBottom(
        new MakeTempCardInDrawPileAction(
            new Slimed(), 1, true, false, false,
            Settings.WIDTH * 0.35F, Settings.HEIGHT / 2.0F
        )
    );

    // 3. Wound
    AbstractDungeon.actionManager.addToBottom(
        new MakeTempCardInDrawPileAction(
            new Wound(), 1, true, false, false,
            Settings.WIDTH * 0.5F, Settings.HEIGHT / 2.0F
        )
    );

    // 4. Burn
    AbstractDungeon.actionManager.addToBottom(
        new MakeTempCardInDrawPileAction(
            new Burn(), 1, true, false, false,
            Settings.WIDTH * 0.65F, Settings.HEIGHT / 2.0F
        )
    );

    // 5. Void
    AbstractDungeon.actionManager.addToBottom(
        new MakeTempCardInDrawPileAction(
            new VoidCard(), 1, true, false, false,
            Settings.WIDTH * 0.8F, Settings.HEIGHT / 2.0F
        )
    );
    break;
```

**DEBILITATE 효과**:
- Vulnerable 2, Weak 2, Frail 2 부여
- Dazed, Slimed, Wound, Burn, Void 각 1장씩 뽑기 더미에 추가

#### 2. BUFF (버프)

```java
// CorruptHeart.java:167-208
case 4: // GAIN_ONE_STRENGTH
    // Strength 디버프 중화 (음수 Strength가 있으면 0으로)
    int additionalAmount = 0;
    if (hasPower("Strength") && getPower("Strength").amount < 0) {
        additionalAmount = -getPower("Strength").amount;
    }

    // 시각 효과
    AbstractDungeon.actionManager.addToBottom(
        new VFXAction(new BorderFlashEffect(
            new Color(0.8F, 0.5F, 1.0F, 1.0F)
        ))
    );
    AbstractDungeon.actionManager.addToBottom(
        new VFXAction(new HeartBuffEffect(this.hb.cX, this.hb.cY))
    );

    // 기본 Strength +2 + 디버프 중화
    AbstractDungeon.actionManager.addToBottom(
        new ApplyPowerAction(this, this,
            new StrengthPower(this, additionalAmount + 2),
            additionalAmount + 2
        )
    );

    // buffCount에 따른 추가 버프
    switch (this.buffCount) {
        case 0:
            // 첫 버프: Artifact 2
            AbstractDungeon.actionManager.addToBottom(
                new ApplyPowerAction(this, this,
                    new ArtifactPower(this, 2), 2
                )
            );
            break;

        case 1:
            // 두 번째 버프: Beat of Death +1
            AbstractDungeon.actionManager.addToBottom(
                new ApplyPowerAction(this, this,
                    new BeatOfDeathPower(this, 1), 1
                )
            );
            break;

        case 2:
            // 세 번째 버프: Painful Stabs (매 공격마다 1장 추가 Wound)
            AbstractDungeon.actionManager.addToBottom(
                new ApplyPowerAction(this, this,
                    new PainfulStabsPower(this)
                )
            );
            break;

        case 3:
            // 네 번째 버프: Strength +10
            AbstractDungeon.actionManager.addToBottom(
                new ApplyPowerAction(this, this,
                    new StrengthPower(this, 10), 10
                )
            );
            break;

        default:
            // 다섯 번째 이후 버프: Strength +50
            AbstractDungeon.actionManager.addToBottom(
                new ApplyPowerAction(this, this,
                    new StrengthPower(this, 50), 50
                )
            );
            break;
    }

    this.buffCount++;
    break;
```

**BUFF 효과**:
- 항상: Strength +2 + 음수 Strength 중화
- 1회차: Artifact 2
- 2회차: Beat of Death +1
- 3회차: Painful Stabs
- 4회차: Strength +10
- 5회차 이후: Strength +50

#### 3. BLOOD_SHOTS (다단히트 공격)

```java
// CorruptHeart.java:209-236
case 1: // BLOOD_SHOTS
    // 시각 효과 (FAST_MODE 여부에 따라 시간 조정)
    if (Settings.FAST_MODE) {
        AbstractDungeon.actionManager.addToBottom(
            new VFXAction(
                new BloodShotEffect(
                    this.hb.cX, this.hb.cY,
                    AbstractDungeon.player.hb.cX,
                    AbstractDungeon.player.hb.cY,
                    this.bloodHitCount
                ),
                0.25F
            )
        );
    } else {
        AbstractDungeon.actionManager.addToBottom(
            new VFXAction(
                new BloodShotEffect(
                    this.hb.cX, this.hb.cY,
                    AbstractDungeon.player.hb.cX,
                    AbstractDungeon.player.hb.cY,
                    this.bloodHitCount
                ),
                0.6F
            )
        );
    }

    // 다단히트 공격 (2x12 또는 2x15)
    for (int i = 0; i < this.bloodHitCount; i++) {
        AbstractDungeon.actionManager.addToBottom(
            new DamageAction(
                AbstractDungeon.player,
                this.damage.get(1),
                AttackEffect.BLUNT_HEAVY,
                true  // fast attack
            )
        );
    }
    break;
```

**BLOOD_SHOTS 효과**: 2 데미지를 12회 (Normal) 또는 15회 (A4+) 반복

#### 4. ECHO_ATTACK (단일 공격)

```java
// CorruptHeart.java:237-244
case 2: // ECHO_ATTACK
    // 시각 효과
    AbstractDungeon.actionManager.addToBottom(
        new VFXAction(
            new ViceCrushEffect(
                AbstractDungeon.player.hb.cX,
                AbstractDungeon.player.hb.cY
            ),
            0.5F
        )
    );

    // 단일 강력한 공격
    AbstractDungeon.actionManager.addToBottom(
        new DamageAction(
            AbstractDungeon.player,
            this.damage.get(0),
            AttackEffect.BLUNT_HEAVY
        )
    );
    break;
```

**ECHO_ATTACK 효과**: 40 (Normal) 또는 45 (A4+) 단일 공격

### The Heart 행동 순서 요약

| 턴 | 행동 | 효과 |
|-----|------|------|
| 1 | DEBILITATE | Vuln/Weak/Frail 2 + 상태이상 카드 5장 |
| 2 | Blood Shots or Echo | 2x12(15) or 40(45) 공격 |
| 3 | Echo or Blood Shots | 40(45) or 2x12(15) 공격 |
| 4 | BUFF | Str +2 + Artifact 2 |
| 5 | Blood Shots or Echo | 2x12(15) or 40(45) 공격 |
| 6 | Echo or Blood Shots | 40(45) or 2x12(15) 공격 |
| 7 | BUFF | Str +2 + Beat +1 |
| 8 | Blood Shots or Echo | 2x12(15) or 40(45) 공격 |
| 9 | Echo or Blood Shots | 40(45) or 2x12(15) 공격 |
| 10 | BUFF | Str +2 + Painful Stabs |

### 승천 난이도 변화

| 승천 | HP | Echo | Blood Shots | Invincible | Beat of Death |
|------|-----|------|-------------|------------|---------------|
| A0-3 | 750 | 40 | 2x12 | 300 | 1 |
| A4-8 | 750 | 45 | 2x15 | 300 | 1 |
| A9-18 | 800 | 45 | 2x15 | 300 | 1 |
| A19+ | 800 | 45 | 2x15 | 200 | 2 |

### The Heart 전투 전략

#### 위협 요소

1. **Invincible Buffer**: 300/200 데미지를 먼저 깎아야 실제 HP 감소
2. **Beat of Death**: 매 턴 1/2 필수 데미지 (Block 불가)
3. **첫 턴 디버프**: Vulnerable, Weak, Frail + 상태이상 카드 5장
4. **다단히트 공격**: Blood Shots (2x12 or 2x15) - Block 효율 감소
5. **지속적인 버프**: Strength 증가, Artifact, Painful Stabs
6. **장기전 불리**: 버프가 쌓일수록 강력해짐

---

## Neow Event 시스템

### 개요

Neow Event는 게임 시작 시 첫 번째 방에서 발생하는 특수 이벤트입니다. 플레이어는 Neow로부터 축복을 받을 수 있으며, 이전 승리 횟수에 따라 보상의 종류가 달라집니다.

### Boss Count 추적 시스템

```java
// NeowEvent.java:81-90
if (Settings.isStandardRun() ||
    (Settings.isEndless && AbstractDungeon.floorNum <= 1)) {

    // 플레이어 캐릭터별 보스 처치 횟수 불러오기
    this.bossCount = CardCrawlGame.playerPref.getInteger(
        AbstractDungeon.player.chosenClass.name() + "_SPIRITS",
        0
    );
    AbstractDungeon.bossCount = this.bossCount;

} else if (Settings.seedSet) {
    // 시드가 설정된 경우 bossCount = 1
    this.bossCount = 1;
} else {
    // Endless 모드 또는 기타 경우 bossCount = 0
    this.bossCount = 0;
}
```

**Boss Count (Spirits)**:
- 해당 캐릭터로 보스를 처치할 때마다 1씩 증가
- 캐릭터별로 독립적으로 관리 (e.g., "IRONCLAD_SPIRITS", "THE_SILENT_SPIRITS")
- 이 값에 따라 Neow의 보상 카테고리가 결정됨

### 보상 카테고리 시스템

Neow의 보상은 4개의 카테고리로 나뉩니다:

```java
// NeowEvent.java:121-175
private ArrayList<NeowRewardDef> getRewardOptions(int category) {
    ArrayList<NeowRewardDef> rewardOptions = new ArrayList<>();

    switch (category) {
        case 0: // 첫 보상 (카드 중심)
            rewardOptions.add(new NeowRewardDef(
                NeowRewardType.THREE_CARDS,
                TEXT[0]  // "카드 3장 중 1장 획득"
            ));
            rewardOptions.add(new NeowRewardDef(
                NeowRewardType.ONE_RANDOM_RARE_CARD,
                TEXT[1]  // "랜덤 레어 카드 1장 획득"
            ));
            rewardOptions.add(new NeowRewardDef(
                NeowRewardType.REMOVE_CARD,
                TEXT[2]  // "카드 1장 제거"
            ));
            rewardOptions.add(new NeowRewardDef(
                NeowRewardType.UPGRADE_CARD,
                TEXT[3]  // "카드 1장 업그레이드"
            ));
            rewardOptions.add(new NeowRewardDef(
                NeowRewardType.TRANSFORM_CARD,
                TEXT[4]  // "카드 1장 변환"
            ));
            rewardOptions.add(new NeowRewardDef(
                NeowRewardType.RANDOM_COLORLESS,
                TEXT[30]  // "무색 카드 1장 획득"
            ));
            break;

        case 1: // 두 번째 보상 (자원 중심)
            rewardOptions.add(new NeowRewardDef(
                NeowRewardType.THREE_SMALL_POTIONS,
                TEXT[5]  // "포션 3개 획득"
            ));
            rewardOptions.add(new NeowRewardDef(
                NeowRewardType.RANDOM_COMMON_RELIC,
                TEXT[6]  // "커먼 유물 1개 획득"
            ));
            rewardOptions.add(new NeowRewardDef(
                NeowRewardType.TEN_PERCENT_HP_BONUS,
                TEXT[7] + this.hp_bonus + " ]"  // "최대 체력 +10% 증가"
            ));
            rewardOptions.add(new NeowRewardDef(
                NeowRewardType.THREE_ENEMY_KILL,
                TEXT[28]  // "적 3회 처치 시 효과"
            ));
            rewardOptions.add(new NeowRewardDef(
                NeowRewardType.HUNDRED_GOLD,
                TEXT[8] + 'd' + TEXT[9]  // "골드 100 획득"
            ));
            break;

        case 2: // 세 번째 보상 (프리미엄, 대가 필요)
            // 먼저 대가(Drawback)를 선택
            ArrayList<NeowRewardDrawbackDef> drawbackOptions =
                getRewardDrawbackOptions();
            this.drawbackDef = drawbackOptions.get(
                NeowEvent.rng.random(0, drawbackOptions.size() - 1)
            );
            this.drawback = this.drawbackDef.type;

            // 프리미엄 보상
            rewardOptions.add(new NeowRewardDef(
                NeowRewardType.RANDOM_COLORLESS_2,
                TEXT[31]  // "레어 무색 카드 1장 획득"
            ));

            if (this.drawback != NeowRewardDrawback.CURSE) {
                rewardOptions.add(new NeowRewardDef(
                    NeowRewardType.REMOVE_TWO,
                    TEXT[10]  // "카드 2장 제거"
                ));
            }

            rewardOptions.add(new NeowRewardDef(
                NeowRewardType.ONE_RARE_RELIC,
                TEXT[11]  // "레어 유물 1개 획득"
            ));
            rewardOptions.add(new NeowRewardDef(
                NeowRewardType.THREE_RARE_CARDS,
                TEXT[12]  // "레어 카드 3장 중 1장 획득"
            ));

            if (this.drawback != NeowRewardDrawback.NO_GOLD) {
                rewardOptions.add(new NeowRewardDef(
                    NeowRewardType.TWO_FIFTY_GOLD,
                    TEXT[13] + 'ú' + TEXT[14]  // "골드 250 획득"
                ));
            }

            rewardOptions.add(new NeowRewardDef(
                NeowRewardType.TRANSFORM_TWO_CARDS,
                TEXT[15]  // "카드 2장 변환"
            ));

            if (this.drawback != NeowRewardDrawback.TEN_PERCENT_HP_LOSS) {
                rewardOptions.add(new NeowRewardDef(
                    NeowRewardType.TWENTY_PERCENT_HP_BONUS,
                    TEXT[16] + (this.hp_bonus * 2) + " ]"  // "최대 체력 +20% 증가"
                ));
            }
            break;

        case 3: // 네 번째 보상 (보스 유물 교환)
            rewardOptions.add(new NeowRewardDef(
                NeowRewardType.BOSS_RELIC,
                UNIQUE_REWARDS[0]  // "시작 유물을 보스 유물로 교환"
            ));
            break;
    }

    return rewardOptions;
}
```

### 대가(Drawback) 시스템

Category 2 보상을 선택할 때는 반드시 대가를 치러야 합니다:

```java
// NeowEvent.java:105-118
private ArrayList<NeowRewardDrawbackDef> getRewardDrawbackOptions() {
    ArrayList<NeowRewardDrawbackDef> drawbackOptions = new ArrayList<>();

    // 1. 최대 체력 10% 감소
    drawbackOptions.add(new NeowRewardDrawbackDef(
        NeowRewardDrawback.TEN_PERCENT_HP_LOSS,
        TEXT[17] + this.hp_bonus + TEXT[18]
    ));

    // 2. 모든 골드 상실
    drawbackOptions.add(new NeowRewardDrawbackDef(
        NeowRewardDrawback.NO_GOLD,
        TEXT[19]
    ));

    // 3. 저주 카드 1장 획득
    drawbackOptions.add(new NeowRewardDrawbackDef(
        NeowRewardDrawback.CURSE,
        TEXT[20]
    ));

    // 4. 현재 체력의 30% 손실
    drawbackOptions.add(new NeowRewardDrawbackDef(
        NeowRewardDrawback.PERCENT_DAMAGE,
        TEXT[21] + (AbstractDungeon.player.currentHealth / 10 * 3) + TEXT[29] + " "
    ));

    return drawbackOptions;
}
```

### 대가 적용 시스템

```java
// NeowReward.java:267-287
public void activate() {
    this.activated = true;

    // 대가 적용
    switch (this.drawback) {
        case CURSE:
            // 저주 카드 1장 추가
            this.cursed = true;
            break;

        case NO_GOLD:
            // 모든 골드 상실
            AbstractDungeon.player.loseGold(AbstractDungeon.player.gold);
            break;

        case TEN_PERCENT_HP_LOSS:
            // 최대 체력 10% 감소
            AbstractDungeon.player.decreaseMaxHealth(this.hp_bonus);
            break;

        case PERCENT_DAMAGE:
            // 현재 체력의 30% 손실
            AbstractDungeon.player.damage(
                new DamageInfo(
                    null,
                    AbstractDungeon.player.currentHealth / 10 * 3,
                    DamageInfo.DamageType.HP_LOSS
                )
            );
            break;
    }

    // 보상 적용 (코드 생략)
}
```

### Neow 보상 타입 전체 목록

```java
// NeowReward.java:517
public enum NeowRewardType {
    // Category 0 (카드 중심)
    THREE_CARDS,              // 카드 3장 중 1장 선택
    ONE_RANDOM_RARE_CARD,     // 랜덤 레어 카드 1장
    REMOVE_CARD,              // 카드 1장 제거
    UPGRADE_CARD,             // 카드 1장 업그레이드
    TRANSFORM_CARD,           // 카드 1장 변환
    RANDOM_COLORLESS,         // 무색 카드 1장

    // Category 1 (자원 중심)
    THREE_SMALL_POTIONS,      // 포션 3개
    RANDOM_COMMON_RELIC,      // 커먼 유물 1개
    TEN_PERCENT_HP_BONUS,     // 최대 체력 +10%
    HUNDRED_GOLD,             // 골드 100
    THREE_ENEMY_KILL,         // Neow's Lament (적 3회 처치)

    // Category 2 (프리미엄, 대가 필요)
    RANDOM_COLORLESS_2,       // 레어 무색 카드 1장
    REMOVE_TWO,               // 카드 2장 제거
    ONE_RARE_RELIC,           // 레어 유물 1개
    THREE_RARE_CARDS,         // 레어 카드 3장 중 1장
    TWO_FIFTY_GOLD,           // 골드 250
    TRANSFORM_TWO_CARDS,      // 카드 2장 변환
    TWENTY_PERCENT_HP_BONUS,  // 최대 체력 +20%

    // Category 3 (보스 유물 교환)
    BOSS_RELIC                // 시작 유물 → 보스 유물
}
```

### Neow's Lament 유물

```java
// NeowReward.java:335
case THREE_ENEMY_KILL:
    // Neow's Lament 유물 획득
    AbstractDungeon.getCurrRoom().spawnRelicAndObtain(
        Settings.WIDTH / 2,
        Settings.HEIGHT / 2,
        new NeowsLament()
    );
    break;
```

**Neow's Lament 효과**: 다음 3번의 전투에서 적들이 전투 시작 시 1 HP가 됩니다.

### Boss Relic 교환 시스템

```java
// NeowReward.java:327-333
case BOSS_RELIC:
    // 시작 유물 제거 (첫 번째 유물)
    AbstractDungeon.player.loseRelic(
        ((AbstractRelic)AbstractDungeon.player.relics.get(0)).relicId
    );

    // 랜덤 보스 유물 획득
    AbstractDungeon.getCurrRoom().spawnRelicAndObtain(
        Settings.WIDTH / 2,
        Settings.HEIGHT / 2,
        AbstractDungeon.returnRandomRelic(AbstractRelic.RelicTier.BOSS)
    );
    break;
```

### 카드 보상 확률

```java
// NeowReward.java:487-492
public AbstractCard.CardRarity rollRarity() {
    // 33% 확률로 Uncommon, 67% 확률로 Common
    if (NeowEvent.rng.randomBoolean(0.33F)) {
        return AbstractCard.CardRarity.UNCOMMON;
    }
    return AbstractCard.CardRarity.COMMON;
}
```

**무색 카드 보상 (RANDOM_COLORLESS)**:
- 33% Uncommon, 67% Common
- Common 레어도가 나온 경우 자동으로 Uncommon으로 상향
- 최종: 33% Uncommon, 67% Uncommon → 실질적으로 100% Uncommon

**무색 카드 보상 (RANDOM_COLORLESS_2, 레어 전용)**:
- rareOnly = true
- 100% Rare 무색 카드

### Neow Event 보상 선택 흐름

```
게임 시작
    ↓
Boss Count 확인 (0 or 1+)
    ↓
Boss Count = 0 → Category 0 or 1 중 1개 선택
Boss Count ≥ 1 → Category 0, 1, 2, 3 중 4개 선택지 제공
    ↓
Category 2 선택 시 → 대가(Drawback) 적용
    ↓
보상 활성화 (카드 선택, 유물 획득 등)
    ↓
게임 진행
```

---

## 수정 방법

### 1. Act 4 맵 구조 변경

**목적**: Act 4의 고정된 맵 구조를 변경하여 추가 방을 삽입하거나 순서를 변경합니다.

```java
@SpirePatch(
    clz = TheEnding.class,
    method = "generateSpecialMap"
)
public class CustomAct4MapPatch {
    @SpirePostfixPatch
    public static void Postfix(TheEnding __instance) {
        try {
            // 기존 맵 가져오기
            Field mapField = AbstractDungeon.class.getDeclaredField("map");
            mapField.setAccessible(true);
            ArrayList<ArrayList<MapRoomNode>> map =
                (ArrayList<ArrayList<MapRoomNode>>) mapField.get(__instance);

            // 추가 방 삽입 (예: Elite 전 추가 전투)
            MapRoomNode extraCombatNode = new MapRoomNode(3, 2);
            extraCombatNode.room = new MonsterRoom();

            // Y=2 위치에 추가 방 삽입
            ArrayList<MapRoomNode> newRow = new ArrayList<>();
            newRow.add(extraCombatNode);
            map.add(2, newRow);  // Elite 전에 삽입

            // 연결 재설정
            MapRoomNode shopNode = map.get(1).get(0);
            MapRoomNode eliteNode = map.get(3).get(0);

            // Shop → ExtraCombat → Elite 연결
            shopNode.edges.clear();
            shopNode.addEdge(new MapEdge(3, 1, 3, 1, 3, 2, 3, 2, false));

            extraCombatNode.addEdge(new MapEdge(3, 2, 3, 2, 3, 3, 3, 3, false));

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
```

### 2. Shield and Spear HP 및 데미지 수정

**목적**: Shield and Spear의 HP와 공격력을 변경합니다.

```java
@SpirePatch(
    clz = SpireShield.class,
    method = SpirePatch.CONSTRUCTOR
)
public class BuffShieldPatch {
    @SpirePostfixPatch
    public static void Postfix(SpireShield __instance) {
        try {
            // HP 1.5배 증가
            Field maxHealthField = AbstractMonster.class.getDeclaredField("maxHealth");
            maxHealthField.setAccessible(true);
            int currentMaxHp = maxHealthField.getInt(__instance);
            maxHealthField.set(__instance, (int)(currentMaxHp * 1.5));

            // 현재 HP도 함께 증가
            Field currentHealthField = AbstractMonster.class.getDeclaredField("currentHealth");
            currentHealthField.setAccessible(true);
            currentHealthField.set(__instance, (int)(currentMaxHp * 1.5));

            // Bash 데미지 증가 (12/14 → 18/21)
            Field bashDmgField = SpireShield.class.getDeclaredField("bashDmg");
            bashDmgField.setAccessible(true);
            int bashDmg = bashDmgField.getInt(__instance);
            bashDmgField.set(__instance, bashDmg + 6);

            // Smash 데미지 증가 (34/38 → 50/56)
            Field smashDmgField = SpireShield.class.getDeclaredField("smashDmg");
            smashDmgField.setAccessible(true);
            int smashDmg = smashDmgField.getInt(__instance);
            smashDmgField.set(__instance, smashDmg + 16);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
```

### 3. The Heart Invincible 버퍼 제거

**목적**: The Heart의 Invincible power를 제거하거나 감소시킵니다.

```java
@SpirePatch(
    clz = CorruptHeart.class,
    method = "usePreBattleAction"
)
public class RemoveInvinciblePatch {
    @SpirePrefixPatch
    public static SpireReturn<Void> Prefix(CorruptHeart __instance) {
        // Invincible power를 부여하지 않도록 원본 메서드 실행 차단

        // 배경음악 변경 (원본 코드 일부 유지)
        CardCrawlGame.music.unsilenceBGM();
        AbstractDungeon.scene.fadeOutAmbiance();
        AbstractDungeon.getCurrRoom().playBgmInstantly("BOSS_ENDING");

        // Beat of Death만 부여 (Invincible은 제거)
        int beatAmount = 1;
        if (AbstractDungeon.ascensionLevel >= 19) {
            beatAmount++;
        }

        AbstractDungeon.actionManager.addToBottom(
            new ApplyPowerAction(
                __instance, __instance,
                new BeatOfDeathPower(__instance, beatAmount), beatAmount
            )
        );

        // 원본 메서드 실행 차단
        return SpireReturn.Return(null);
    }
}
```

### 4. The Heart 버프 순서 변경

**목적**: The Heart의 버프 순서를 변경하여 전략을 바꿉니다 (예: 먼저 Painful Stabs 부여).

```java
@SpirePatch(
    clz = CorruptHeart.class,
    method = "takeTurn"
)
public class CustomHeartBuffOrderPatch {
    @SpireInsertPatch(
        locator = BuffLocator.class
    )
    public static SpireReturn<Void> Insert(CorruptHeart __instance) {
        try {
            Field buffCountField = CorruptHeart.class.getDeclaredField("buffCount");
            buffCountField.setAccessible(true);
            int buffCount = buffCountField.getInt(__instance);

            // Strength 중화 로직 (원본 유지)
            int additionalAmount = 0;
            if (__instance.hasPower("Strength") &&
                __instance.getPower("Strength").amount < 0) {
                additionalAmount = -__instance.getPower("Strength").amount;
            }

            // 시각 효과
            AbstractDungeon.actionManager.addToBottom(
                new VFXAction(new BorderFlashEffect(
                    new Color(0.8F, 0.5F, 1.0F, 1.0F)
                ))
            );
            AbstractDungeon.actionManager.addToBottom(
                new VFXAction(new HeartBuffEffect(
                    __instance.hb.cX, __instance.hb.cY
                ))
            );

            // Strength +2 기본 버프
            AbstractDungeon.actionManager.addToBottom(
                new ApplyPowerAction(__instance, __instance,
                    new StrengthPower(__instance, additionalAmount + 2),
                    additionalAmount + 2
                )
            );

            // 버프 순서 변경: Painful Stabs를 첫 번째로
            switch (buffCount) {
                case 0:
                    // 첫 버프: Painful Stabs (변경됨)
                    AbstractDungeon.actionManager.addToBottom(
                        new ApplyPowerAction(__instance, __instance,
                            new PainfulStabsPower(__instance)
                        )
                    );
                    break;

                case 1:
                    // 두 번째: Artifact 2 (원래 첫 번째)
                    AbstractDungeon.actionManager.addToBottom(
                        new ApplyPowerAction(__instance, __instance,
                            new ArtifactPower(__instance, 2), 2
                        )
                    );
                    break;

                case 2:
                    // 세 번째: Beat of Death +1 (원래 두 번째)
                    AbstractDungeon.actionManager.addToBottom(
                        new ApplyPowerAction(__instance, __instance,
                            new BeatOfDeathPower(__instance, 1), 1
                        )
                    );
                    break;

                case 3:
                    // 네 번째: Strength +10 (유지)
                    AbstractDungeon.actionManager.addToBottom(
                        new ApplyPowerAction(__instance, __instance,
                            new StrengthPower(__instance, 10), 10
                        )
                    );
                    break;

                default:
                    // 다섯 번째 이후: Strength +50 (유지)
                    AbstractDungeon.actionManager.addToBottom(
                        new ApplyPowerAction(__instance, __instance,
                            new StrengthPower(__instance, 50), 50
                        )
                    );
                    break;
            }

            buffCountField.set(__instance, buffCount + 1);

            // 원본 메서드 실행 차단
            return SpireReturn.Return(null);

        } catch (Exception e) {
            e.printStackTrace();
            return SpireReturn.Continue();
        }
    }

    private static class BuffLocator extends SpireInsertLocator {
        @Override
        public int[] Locate(CtBehavior ctMethodToPatch) throws Exception {
            Matcher finalMatcher = new Matcher.FieldAccessMatcher(
                CorruptHeart.class, "buffCount"
            );
            return LineFinder.findInOrder(ctMethodToPatch, finalMatcher);
        }
    }
}
```

### 5. Neow 보상 카테고리 강제 설정

**목적**: Boss Count에 관계없이 항상 특정 카테고리 보상을 제공합니다.

```java
@SpirePatch(
    clz = NeowEvent.class,
    method = SpirePatch.CONSTRUCTOR
)
public class AlwaysPremiumNeowRewardsPatch {
    @SpirePostfixPatch
    public static void Postfix(NeowEvent __instance) {
        try {
            // Boss Count를 강제로 1로 설정하여 모든 카테고리 보상 활성화
            Field bossCountField = NeowEvent.class.getDeclaredField("bossCount");
            bossCountField.setAccessible(true);
            bossCountField.set(__instance, 1);

            AbstractDungeon.bossCount = 1;

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
```

### 6. Neow 대가 제거

**목적**: Category 2 보상을 선택해도 대가를 치르지 않습니다.

```java
@SpirePatch(
    clz = NeowReward.class,
    method = "activate"
)
public class NoDrawbackPatch {
    @SpirePrefixPatch
    public static void Prefix(NeowReward __instance) {
        try {
            // Drawback을 NONE으로 강제 설정
            Field drawbackField = NeowReward.class.getDeclaredField("drawback");
            drawbackField.setAccessible(true);
            drawbackField.set(__instance, NeowReward.NeowRewardDrawback.NONE);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
```

### 7. Neow 보상에 추가 옵션 삽입

**목적**: Neow 보상에 커스텀 옵션을 추가합니다 (예: "모든 카드 업그레이드").

```java
@SpirePatch(
    clz = NeowReward.class,
    method = "getRewardOptions"
)
public class CustomNeowRewardPatch {
    @SpirePostfixPatch
    public static ArrayList<NeowReward.NeowRewardDef> Postfix(
        ArrayList<NeowReward.NeowRewardDef> __result,
        NeowReward __instance,
        int category
    ) {
        if (category == 2) {
            // Category 2에 "모든 카드 업그레이드" 옵션 추가
            __result.add(new NeowReward.NeowRewardDef(
                NeowReward.NeowRewardType.UPGRADE_CARD,  // 임시로 UPGRADE_CARD 재사용
                "[커스텀] 덱의 모든 카드를 업그레이드합니다."
            ));
        }

        return __result;
    }
}

// 활성화 시 처리
@SpirePatch(
    clz = NeowReward.class,
    method = "activate"
)
public class CustomNeowRewardActivatePatch {
    @SpirePostfixPatch
    public static void Postfix(NeowReward __instance) {
        try {
            Field typeField = NeowReward.class.getDeclaredField("type");
            typeField.setAccessible(true);
            NeowReward.NeowRewardType type =
                (NeowReward.NeowRewardType) typeField.get(__instance);

            // 커스텀 보상 타입 체크 (옵션 레이블로 식별)
            Field optionLabelField = NeowReward.class.getDeclaredField("optionLabel");
            optionLabelField.setAccessible(true);
            String optionLabel = (String) optionLabelField.get(__instance);

            if (optionLabel.contains("[커스텀]")) {
                // 모든 카드 업그레이드
                for (AbstractCard card : AbstractDungeon.player.masterDeck.group) {
                    if (card.canUpgrade()) {
                        card.upgrade();
                    }
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
```

### 8. Shield and Spear 분리 스폰

**목적**: Shield와 Spear를 별도의 전투로 분리합니다.

```java
@SpirePatch(
    clz = TheEnding.class,
    method = "generateElites"
)
public class SeparateShieldSpearPatch {
    @SpirePostfixPatch
    public static void Postfix(TheEnding __instance, int count) {
        try {
            Field eliteMonsterListField =
                AbstractDungeon.class.getDeclaredField("eliteMonsterList");
            eliteMonsterListField.setAccessible(true);
            ArrayList<String> eliteMonsterList =
                (ArrayList<String>) eliteMonsterListField.get(__instance);

            // 기존 "Shield and Spear" 제거
            eliteMonsterList.clear();

            // Shield 단독, Spear 단독 추가
            eliteMonsterList.add("Shield");   // 커스텀 ID
            eliteMonsterList.add("Spear");    // 커스텀 ID

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

// MonsterHelper에서 개별 스폰 처리
@SpirePatch(
    clz = MonsterHelper.class,
    method = "getEncounter"
)
public class CustomShieldSpearEncounterPatch {
    @SpirePostfixPatch
    public static MonsterGroup Postfix(MonsterGroup __result, String key) {
        if ("Shield".equals(key)) {
            // Shield만 스폰
            MonsterGroup group = new MonsterGroup(
                new AbstractMonster[] {
                    new SpireShield(-350.0F, 0.0F)
                }
            );
            return group;
        }

        if ("Spear".equals(key)) {
            // Spear만 스폰
            MonsterGroup group = new MonsterGroup(
                new AbstractMonster[] {
                    new SpireSpear(150.0F, 0.0F)
                }
            );
            return group;
        }

        return __result;
    }
}
```

### 9. The Heart 첫 턴 행동 변경

**목적**: The Heart의 첫 턴을 DEBILITATE 대신 다른 행동으로 변경합니다.

```java
@SpirePatch(
    clz = CorruptHeart.class,
    method = "getMove"
)
public class CustomHeartFirstMovePatch {
    @SpirePrefixPatch
    public static SpireReturn<Void> Prefix(CorruptHeart __instance, int num) {
        try {
            Field isFirstMoveField = CorruptHeart.class.getDeclaredField("isFirstMove");
            isFirstMoveField.setAccessible(true);
            boolean isFirstMove = isFirstMoveField.getBoolean(__instance);

            if (isFirstMove) {
                // 첫 턴을 BUFF로 변경 (대신 DEBILITATE)
                ReflectionHacks.privateMethod(
                    CorruptHeart.class, "setMove", byte.class, Intent.class
                ).invoke(__instance, (byte)4, Intent.BUFF);

                isFirstMoveField.set(__instance, false);

                // 원본 메서드 실행 차단
                return SpireReturn.Return(null);
            }

            // 첫 턴이 아니면 원본 로직 실행
            return SpireReturn.Continue();

        } catch (Exception e) {
            e.printStackTrace();
            return SpireReturn.Continue();
        }
    }
}
```

### 10. Act 4 보물 상자 확률 변경

**목적**: Act 4의 보물 상자가 항상 Large Chest가 나오도록 변경합니다.

```java
@SpirePatch(
    clz = AbstractDungeon.class,
    method = "getRandomChest"
)
public class Act4LargeChestPatch {
    @SpirePrefixPatch
    public static SpireReturn<AbstractChest> Prefix() {
        // Act 4인지 확인
        if (AbstractDungeon.id.equals("TheEnding")) {
            // 항상 Large Chest 반환
            return SpireReturn.Return(new LargeChest());
        }

        // 다른 Act는 원본 로직 실행
        return SpireReturn.Continue();
    }
}
```

---

## 관련 클래스

### Act 4 관련
- `com.megacrit.cardcrawl.dungeons.TheEnding` - Act 4 던전 클래스
- `com.megacrit.cardcrawl.rooms.TrueVictoryRoom` - Act 4 승리 방
- `com.megacrit.cardcrawl.monsters.ending.SpireShield` - Shield 몬스터
- `com.megacrit.cardcrawl.monsters.ending.SpireSpear` - Spear 몬스터
- `com.megacrit.cardcrawl.monsters.ending.CorruptHeart` - The Heart 보스

### The Heart 관련 Powers
- `com.megacrit.cardcrawl.powers.InvinciblePower` - 데미지 무효화 버퍼
- `com.megacrit.cardcrawl.powers.BeatOfDeathPower` - 턴마다 필수 데미지
- `com.megacrit.cardcrawl.powers.PainfulStabsPower` - 공격 시 Wound 추가
- `com.megacrit.cardcrawl.powers.SurroundedPower` - 도망 불가 (Shield)

### Neow Event 관련
- `com.megacrit.cardcrawl.neow.NeowEvent` - Neow 이벤트 메인 클래스
- `com.megacrit.cardcrawl.neow.NeowReward` - Neow 보상 시스템
- `com.megacrit.cardcrawl.relics.NeowsLament` - Neow's Lament 유물

### 상태이상 카드
- `com.megacrit.cardcrawl.cards.status.Burn` - Burn 카드
- `com.megacrit.cardcrawl.cards.status.Wound` - Wound 카드
- `com.megacrit.cardcrawl.cards.status.Dazed` - Dazed 카드
- `com.megacrit.cardcrawl.cards.status.Slimed` - Slimed 카드
- `com.megacrit.cardcrawl.cards.status.VoidCard` - Void 카드

### 맵 및 방 관련
- `com.megacrit.cardcrawl.map.MapRoomNode` - 맵 노드
- `com.megacrit.cardcrawl.map.MapEdge` - 맵 연결
- `com.megacrit.cardcrawl.rooms.MonsterRoomElite` - 엘리트 전투 방
- `com.megacrit.cardcrawl.rooms.MonsterRoomBoss` - 보스 전투 방

### 유틸리티
- `com.megacrit.cardcrawl.helpers.MonsterHelper` - 몬스터 생성 헬퍼
- `com.megacrit.cardcrawl.random.Random` - RNG 시스템
- `com.megacrit.cardcrawl.saveAndContinue.SaveFile` - 저장 시스템

---

## 요약

특수 조우 시스템은 일반적인 조우와 다른 고정된 메커니즘으로 작동합니다:

1. **Act 4 (The Ending)**: 고정된 5개의 방으로 구성된 직선 경로 (Rest → Shop → Elite → Boss → Victory)
2. **Shield and Spear**: Act 4의 유일한 엘리트 조우로, 방어에 특화된 Shield와 공격에 특화된 Spear가 동시 등장
3. **The Heart**: Act 4의 최종 보스로, Invincible 버퍼(300/200)와 Beat of Death로 장기전을 강요하며, 3턴 주기 패턴 반복
4. **Neow Event**: 게임 시작 시 Boss Count에 따라 4가지 카테고리 보상 제공, Category 2는 대가(Drawback) 필요

이 문서의 정보를 활용하여 Act 4의 난이도 조정, The Heart의 패턴 변경, Neow 보상 커스터마이징 등 다양한 모드를 제작할 수 있습니다.
