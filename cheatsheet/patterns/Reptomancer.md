# 파충술사 (Reptomancer)

## 기본 정보

**클래스명**: `Reptomancer`
**전체 경로**: `com.megacrit.cardcrawl.monsters.beyond.Reptomancer`
**ID**: `"Reptomancer"`
**타입**: 엘리트 (ELITE)
**등장 지역**: 3막 (Beyond)

**미니언**: `SnakeDagger` (뱀 단검)

---

## HP 정보

### Reptomancer (본체)

| 난이도 | HP 범위 |
|--------|---------|
| 기본 (A0-A7) | 180-190 |
| A8+ | 190-200 |

**코드 위치**: 59-63줄

```java
if (AbstractDungeon.ascensionLevel >= 8) {
    setHp(190, 200);
} else {
    setHp(180, 190);
}
```

### SnakeDagger (미니언)

| HP 범위 |
|---------|
| 20-25 |

**코드 위치**: SnakeDagger.java 24줄, 32줄

```java
private static final int HP_MIN = 20;
private static final int HP_MAX = 25;

// 생성자
AbstractDungeon.monsterHpRng.random(20, 25)
```

---

## 생성자 정보

### Reptomancer
```java
public Reptomancer()
```

**특징**:
- X: -20.0F, Y: 10.0F
- 히트박스: 0.0F, -30.0F, 220.0F, 320.0F
- 애니메이션: `images/monsters/theForest/mage/skeleton.*`

### SnakeDagger
```java
public SnakeDagger(float x, float y)
```

**특징**:
- 히트박스: 0.0F, -50.0F, 140.0F, 130.0F
- 애니메이션: `images/monsters/theForest/mage_dagger/skeleton.*`

---

## 특수 메커니즘: 미니언 소환 시스템

### 미니언 위치 배열

**코드 위치**: 38-39줄

```java
public static final float[] POSX =
    new float[] { 210.0F, -220.0F, 180.0F, -250.0F };
public static final float[] POSY =
    new float[] { 75.0F, 115.0F, 345.0F, 335.0F };
```

**위치**:
- 슬롯 0: (210, 75) - 오른쪽 아래
- 슬롯 1: (-220, 115) - 왼쪽 아래
- 슬롯 2: (180, 345) - 오른쪽 위
- 슬롯 3: (-250, 335) - 왼쪽 위

### 미니언 추적 시스템

**필드**: `private AbstractMonster[] daggers = new AbstractMonster[4]`

**코드 위치**: 41줄, 83-97줄, 136-143줄

```java
// usePreBattleAction: 초기 미니언 위치 저장
public void usePreBattleAction() {
    for (AbstractMonster m : (AbstractDungeon.getMonsters()).monsters) {
        if (!m.id.equals(this.id)) {
            // 모든 미니언에게 MinionPower 부여
            AbstractDungeon.actionManager.addToBottom(
                new ApplyPowerAction(m, m,
                    new MinionPower(this))
            );
        }
        if (m instanceof SnakeDagger) {
            // 초기 위치 결정 (Reptomancer 기준으로 앞/뒤)
            if ((AbstractDungeon.getMonsters()).monsters.indexOf(m) >
                (AbstractDungeon.getMonsters()).monsters.indexOf(this)) {
                this.daggers[0] = m;  // 앞쪽
            } else {
                this.daggers[1] = m;  // 뒤쪽
            }
        }
    }
}
```

### 소환 로직

**코드 위치**: 135-143줄

```java
case 2:  // SPAWN_DAGGER
    AbstractDungeon.actionManager.addToBottom(
        new ChangeStateAction(this, "SUMMON")
    );
    AbstractDungeon.actionManager.addToBottom(
        new WaitAction(0.5F)
    );

    daggersSpawned = 0;
    for (i = 0;
         daggersSpawned < this.daggersPerSpawn && i < this.daggers.length;
         i++) {
        if (this.daggers[i] == null ||
            this.daggers[i].isDeadOrEscaped()) {
            SnakeDagger daggerToSpawn =
                new SnakeDagger(POSX[i], POSY[i]);
            this.daggers[i] = daggerToSpawn;
            AbstractDungeon.actionManager.addToBottom(
                new SpawnMonsterAction(daggerToSpawn, true)
            );
            daggersSpawned++;
        }
    }
    break;
```

**소환 수**:
| 난이도 | 소환 수 |
|--------|---------|
| A0-A17 | 1 |
| A18+ | 2 |

**소환 제한**: `canSpawn()` 메서드로 체크
- 현장의 미니언이 3마리 초과면 소환 불가
- 빈 슬롯부터 순서대로 소환

---

## Reptomancer 패턴 정보

### 패턴 1: 뱀 공격 (Snake Strike)

**바이트 값**: `1` (SNAKE_STRIKE)
**의도**: `ATTACK_DEBUFF`
**데미지**: 13 x 2 또는 16 x 2 (총 26 또는 32)

**데미지 설정**:
| 난이도 | 1회 데미지 | 총 데미지 |
|--------|-----------|----------|
| A0-A2 | 13 | 26 |
| A3+ | 16 | 32 |

**효과**:
- 플레이어에게 **2회 연속 공격**
- **약화(Weak) 1턴** 부여

**코드 위치**: 38줄, 65-71줄, 103-124줄

```java
// 데미지 상수
private static final int SNAKE_STRIKE_DMG = 13;
private static final int A_2_SNAKE_STRIKE_DMG = 16;

// 생성자에서 설정
if (AbstractDungeon.ascensionLevel >= 3) {
    this.damage.add(new DamageInfo(this, 16));  // index 0
    this.damage.add(new DamageInfo(this, 34));  // index 1
} else {
    this.damage.add(new DamageInfo(this, 13));  // index 0
    this.damage.add(new DamageInfo(this, 30));  // index 1
}

// takeTurn 실행
case 1:
    AbstractDungeon.actionManager.addToBottom(
        new ChangeStateAction(this, "ATTACK")
    );
    AbstractDungeon.actionManager.addToBottom(
        new WaitAction(0.3F)
    );
    // 첫 번째 공격
    AbstractDungeon.actionManager.addToBottom(
        new VFXAction(
            new BiteEffect(
                AbstractDungeon.player.hb.cX +
                    MathUtils.random(-50.0F, 50.0F) * Settings.scale,
                AbstractDungeon.player.hb.cY +
                    MathUtils.random(-50.0F, 50.0F) * Settings.scale,
                Color.ORANGE.cpy()
            ), 0.1F
        )
    );
    AbstractDungeon.actionManager.addToBottom(
        new DamageAction(AbstractDungeon.player,
            this.damage.get(0),
            AbstractGameAction.AttackEffect.NONE)
    );
    // 두 번째 공격 (동일)
    AbstractDungeon.actionManager.addToBottom(...);
    // 약화 부여
    AbstractDungeon.actionManager.addToBottom(
        new ApplyPowerAction(AbstractDungeon.player, this,
            new WeakPower(AbstractDungeon.player, 1, true), 1)
    );
    break;
```

**특징**:
- 물어뜯기 시각 효과 (주황색)
- 랜덤 위치에 BiteEffect 2회
- 약화는 2번 공격 후 1회만 부여

**수정 포인트**:
- 데미지 변경: 생성자의 `damage.get(0)` 수정
- 공격 횟수 변경: VFXAction + DamageAction 반복 수정
- 약화 턴 수 변경: `new WeakPower(..., 1, ...)` 수정

---

### 패턴 2: 미니언 소환 (Spawn Dagger)

**바이트 값**: `2` (SPAWN_DAGGER)
**의도**: `UNKNOWN`
**효과**: 뱀 단검 소환

**소환 수**:
| 난이도 | 소환 수 |
|--------|---------|
| A0-A17 | 1 |
| A18+ | 2 |

**코드 위치**: 38줄, 53-57줄, 131-144줄

```java
// 소환 수 상수
private static final int DAGGERS_PER_SPAWN = 1;
private static final int ASC_2_DAGGERS_PER_SPAWN = 2;

// 생성자에서 설정
if (AbstractDungeon.ascensionLevel >= 18) {
    this.daggersPerSpawn = 2;
} else {
    this.daggersPerSpawn = 1;
}

// takeTurn 실행
case 2:
    AbstractDungeon.actionManager.addToBottom(
        new ChangeStateAction(this, "SUMMON")
    );
    AbstractDungeon.actionManager.addToBottom(
        new WaitAction(0.5F)
    );

    daggersSpawned = 0;
    for (i = 0;
         daggersSpawned < this.daggersPerSpawn &&
         i < this.daggers.length;
         i++) {
        if (this.daggers[i] == null ||
            this.daggers[i].isDeadOrEscaped()) {
            SnakeDagger daggerToSpawn =
                new SnakeDagger(POSX[i], POSY[i]);
            this.daggers[i] = daggerToSpawn;
            AbstractDungeon.actionManager.addToBottom(
                new SpawnMonsterAction(daggerToSpawn, true)
            );
            daggersSpawned++;
        }
    }
    break;
```

**특징**:
- 소환 애니메이션 재생
- 빈 슬롯부터 순서대로 소환
- `canSpawn()` 체크로 최대 3마리 제한
- 소환된 미니언에게 자동으로 MinionPower 부여

**수정 포인트**:
- 소환 수 변경: `daggersPerSpawn` 필드
- 소환 위치 변경: `POSX`, `POSY` 배열
- 최대 미니언 수 변경: `canSpawn()` 메서드

---

### 패턴 3: 큰 물기 (Big Bite)

**바이트 값**: `3` (BIG_BITE)
**의도**: `ATTACK`
**데미지**: 30 또는 34

**데미지 설정**:
| 난이도 | 데미지 |
|--------|--------|
| A0-A2 | 30 |
| A3+ | 34 |

**코드 위치**: 36-37줄, 65-71줄, 146-157줄

```java
// 데미지 상수
private static final int BITE_DMG = 30;
private static final int A_2_BITE_DMG = 34;

// 생성자에서 설정 (damage.get(1))
if (AbstractDungeon.ascensionLevel >= 3) {
    this.damage.add(new DamageInfo(this, 34));
} else {
    this.damage.add(new DamageInfo(this, 30));
}

// takeTurn 실행
case 3:
    AbstractDungeon.actionManager.addToBottom(
        new AnimateFastAttackAction(this)
    );
    AbstractDungeon.actionManager.addToBottom(
        new VFXAction(
            new BiteEffect(
                AbstractDungeon.player.hb.cX +
                    MathUtils.random(-50.0F, 50.0F) * Settings.scale,
                AbstractDungeon.player.hb.cY +
                    MathUtils.random(-50.0F, 50.0F) * Settings.scale,
                Color.CHARTREUSE.cpy()
            ), 0.1F
        )
    );
    AbstractDungeon.actionManager.addToBottom(
        new DamageAction(AbstractDungeon.player,
            this.damage.get(1),
            AbstractGameAction.AttackEffect.NONE)
    );
    break;
```

**특징**:
- 물어뜯기 시각 효과 (연두색)
- 빠른 공격 애니메이션
- 단일 타격

**수정 포인트**:
- 데미지 변경: 생성자의 `damage.get(1)` 수정

---

## SnakeDagger (미니언) 패턴 정보

### 패턴 1: 상처 (Wound)

**바이트 값**: `1` (WOUND)
**의도**: `ATTACK_DEBUFF`
**데미지**: 9 (고정)

**효과**:
- 플레이어에게 **9 데미지**
- 덱에 **상처(Wound) 카드 1장** 추가

**코드 위치**: SnakeDagger.java 25줄, 36줄, 56-62줄

```java
// 데미지 상수
private static final int STAB_DMG = 9;

// 생성자에서 설정
this.damage.add(new DamageInfo(this, 9));  // index 0

// takeTurn 실행
case 1:
    AbstractDungeon.actionManager.addToBottom(
        new ChangeStateAction(this, "ATTACK")
    );
    AbstractDungeon.actionManager.addToBottom(
        new WaitAction(0.3F)
    );
    AbstractDungeon.actionManager.addToBottom(
        new DamageAction(AbstractDungeon.player,
            this.damage.get(0),
            AbstractGameAction.AttackEffect.SLASH_HORIZONTAL)
    );
    AbstractDungeon.actionManager.addToBottom(
        new MakeTempCardInDiscardAction(new Wound(), 1)
    );
    break;
```

**특징**:
- 공격 이펙트: SLASH_HORIZONTAL
- 상처 카드는 버리기 더미에 추가
- 상처 카드: 소모(Unplayable)

**수정 포인트**:
- 데미지 변경: `STAB_DMG` 상수
- 상처 카드 수 변경: `new Wound(), 1` 수정

---

### 패턴 2: 자폭 (Explode)

**바이트 값**: `2` (EXPLODE)
**의도**: `ATTACK`
**데미지**: 25 (고정)

**효과**:
- 플레이어에게 **25 데미지**
- 자신의 HP를 0으로 만듦 (자살)

**코드 위치**: SnakeDagger.java 26줄, 39-40줄, 63-69줄

```java
// 데미지 상수
private static final int SACRIFICE_DMG = 25;

// 생성자에서 설정
this.damage.add(new DamageInfo(this, 25));  // index 1
this.damage.add(new DamageInfo(this, 25,
    DamageInfo.DamageType.HP_LOSS));  // index 2 (자해용)

// takeTurn 실행
case 2:
    AbstractDungeon.actionManager.addToBottom(
        new ChangeStateAction(this, "SUICIDE")
    );
    AbstractDungeon.actionManager.addToBottom(
        new WaitAction(0.4F)
    );
    AbstractDungeon.actionManager.addToBottom(
        new DamageAction(AbstractDungeon.player,
            this.damage.get(1),
            AbstractGameAction.AttackEffect.SLASH_HEAVY)
    );
    AbstractDungeon.actionManager.addToBottom(
        new LoseHPAction(this, this, this.currentHealth)
    );
    break;
```

**특징**:
- 공격 이펙트: SLASH_HEAVY
- 자폭 애니메이션 (Attack2)
- 두 번째 턴에 무조건 사용

**수정 포인트**:
- 데미지 변경: `SACRIFICE_DMG` 상수

---

## Reptomancer AI 로직

**코드 위치**: 196-230줄

### 첫 번째 턴

```java
if (this.firstMove) {
    this.firstMove = false;
    setMove((byte)2, AbstractMonster.Intent.UNKNOWN);  // 소환
    return;
}
```

**로직**:
- 무조건 미니언 소환

### 이후 턴: num < 33 (33% 확률)

```java
if (num < 33) {
    if (!lastMove((byte)1)) {
        setMove((byte)1, AbstractMonster.Intent.ATTACK_DEBUFF,
            this.damage.get(0).base, 2, true);  // 뱀 공격
    } else {
        getMove(AbstractDungeon.aiRng.random(33, 99));  // 재추첨
    }
}
```

**로직**:
- 이전 턴이 뱀 공격이 아니면 → 뱀 공격(1)
- 이전 턴이 뱀 공격이면 → 33-99 범위 재추첨

### 이후 턴: num 33-65 (33% 확률)

```java
else if (num < 66) {
    if (!lastTwoMoves((byte)2)) {
        if (canSpawn()) {
            setMove((byte)2, AbstractMonster.Intent.UNKNOWN);  // 소환
        } else {
            setMove((byte)1, AbstractMonster.Intent.ATTACK_DEBUFF,
                this.damage.get(0).base, 2, true);  // 뱀 공격
        }
    } else {
        setMove((byte)1, AbstractMonster.Intent.ATTACK_DEBUFF,
            this.damage.get(0).base, 2, true);  // 뱀 공격
    }
}
```

**로직**:
1. 이전 2턴이 소환이 아니면:
   - 소환 가능하면 → 소환(2)
   - 소환 불가능하면 → 뱀 공격(1)
2. 이전 2턴이 소환이면 → 뱀 공격(1)

### 이후 턴: num 66-99 (34% 확률)

```java
else if (!lastMove((byte)3)) {
    setMove((byte)3, AbstractMonster.Intent.ATTACK,
        this.damage.get(1).base);  // 큰 물기
} else {
    getMove(AbstractDungeon.aiRng.random(65));  // 0-65 재추첨
}
```

**로직**:
- 이전 턴이 큰 물기가 아니면 → 큰 물기(3)
- 이전 턴이 큰 물기면 → 0-65 범위 재추첨

**수정 포인트**:
- 확률 변경: `if (num < 33)`, `if (num < 66)` 조건
- 첫 턴 패턴 변경
- 소환 조건 변경

---

## SnakeDagger AI 로직

**코드 위치**: SnakeDagger.java 86-94줄

```java
protected void getMove(int num) {
    if (this.firstMove) {
        this.firstMove = false;
        setMove((byte)1, AbstractMonster.Intent.ATTACK_DEBUFF, 9);  // 상처
        return;
    }
    setMove((byte)2, AbstractMonster.Intent.ATTACK, 25);  // 자폭
}
```

**로직**:
- 첫 번째 턴: 무조건 상처 (패턴 1)
- 두 번째 턴: 무조건 자폭 (패턴 2)

**특징**:
- 매우 단순한 AI
- 2턴 수명

---

## 특수 동작

### Reptomancer: 사망 시 미니언 제거

**코드 위치**: 185-193줄

```java
public void die() {
    super.die();
    for (AbstractMonster m :
         (AbstractDungeon.getCurrRoom()).monsters.monsters) {
        if (!m.isDead && !m.isDying) {
            AbstractDungeon.actionManager.addToTop(
                new HideHealthBarAction(m)
            );
            AbstractDungeon.actionManager.addToTop(
                new SuicideAction(m)
            );
        }
    }
}
```

**특징**:
- Reptomancer가 죽으면 모든 미니언 즉시 제거
- 체력바 숨김 → 자살 순서

### canSpawn() 메서드

**코드 위치**: 162-173줄

```java
private boolean canSpawn() {
    int aliveCount = 0;
    for (AbstractMonster m : (AbstractDungeon.getMonsters()).monsters) {
        if (m != this && !m.isDying) {
            aliveCount++;
        }
    }
    if (aliveCount > 3) {
        return false;
    }
    return true;
}
```

**로직**:
- 현재 살아있는 미니언 수 확인
- 3마리 초과 시 소환 불가
- 자신(Reptomancer)은 제외

---

## 수정 예시

### 1. 소환 수 증가 (A25)

```java
@SpirePatch(
    cls = "com.megacrit.cardcrawl.monsters.beyond.Reptomancer",
    method = SpirePatch.CONSTRUCTOR
)
public static class ReptomancerSpawnPatch {
    @SpirePostfixPatch
    public static void Postfix(Reptomancer __instance) {
        if (AbstractDungeon.isAscensionMode &&
            AbstractDungeon.ascensionLevel >= 25) {
            // daggersPerSpawn 필드 변경
            try {
                Field spawnField = Reptomancer.class
                    .getDeclaredField("daggersPerSpawn");
                spawnField.setAccessible(true);
                spawnField.set(__instance, 3);  // 3마리 소환
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
```

### 2. 뱀 단검 HP 증가

```java
@SpirePatch(
    cls = "com.megacrit.cardcrawl.monsters.beyond.SnakeDagger",
    method = SpirePatch.CONSTRUCTOR,
    paramtypez = { float.class, float.class }
)
public static class SnakeDaggerHPPatch {
    @SpirePostfixPatch
    public static void Postfix(SnakeDagger __instance, float x, float y) {
        if (AbstractDungeon.isAscensionMode &&
            AbstractDungeon.ascensionLevel >= 25) {
            // HP 증가
            __instance.currentHealth += 10;
            __instance.maxHealth += 10;
        }
    }
}
```

### 3. Reptomancer 뱀 공격 데미지 증가

```java
@SpirePatch(
    cls = "com.megacrit.cardcrawl.monsters.beyond.Reptomancer",
    method = SpirePatch.CONSTRUCTOR
)
public static class ReptomancerSnakeDamagePatch {
    @SpirePostfixPatch
    public static void Postfix(Reptomancer __instance) {
        if (AbstractDungeon.isAscensionMode &&
            AbstractDungeon.ascensionLevel >= 25) {
            // damage.get(0)은 뱀 공격
            if (!__instance.damage.isEmpty()) {
                __instance.damage.get(0).base += 4;  // 16 → 20
            }
        }
    }
}
```

---

## 중요 필드

### Reptomancer

| 필드명 | 타입 | 설명 |
|--------|------|------|
| `firstMove` | boolean | 첫 턴 여부 |
| `daggersPerSpawn` | int | 소환 시 생성할 미니언 수 |
| `daggers` | AbstractMonster[] | 미니언 추적 배열 (4칸) |
| `POSX` | float[] | 미니언 소환 X 좌표 |
| `POSY` | float[] | 미니언 소환 Y 좌표 |

### SnakeDagger

| 필드명 | 타입 | 설명 |
|--------|------|------|
| `firstMove` | boolean | 첫 턴 여부 (public) |

---

## 관련 파일

- **본 파일**:
  - `com/megacrit/cardcrawl/monsters/beyond/Reptomancer.java`
  - `com/megacrit/cardcrawl/monsters/beyond/SnakeDagger.java`
- **카드**:
  - `com.megacrit.cardcrawl.cards.status.Wound`
- **파워**:
  - `com.megacrit.cardcrawl.powers.MinionPower`
  - `com.megacrit.cardcrawl.powers.WeakPower`
- **이펙트**:
  - `com.megacrit.cardcrawl.vfx.combat.BiteEffect`
- **액션**:
  - `AnimateFastAttackAction`
  - `ChangeStateAction`
  - `DamageAction`
  - `ApplyPowerAction`
  - `MakeTempCardInDiscardAction`
  - `SpawnMonsterAction`
  - `SuicideAction`
  - `LoseHPAction`
  - `HideHealthBarAction`
  - `VFXAction`
  - `WaitAction`
  - `RollMoveAction`

---

## 참고사항

1. **미니언 시스템**: 핵심 메커니즘, 최대 4마리까지 배치
2. **첫 턴 소환**: 항상 미니언부터 소환
3. **미니언 수명**: 2턴 (1턴 공격 + 2턴 자폭)
4. **A18 변경점**: 소환 시 1마리 → 2마리
5. **소환 제한**: 최대 3마리까지만 (본체 제외)
6. **본체 사망**: 모든 미니언 즉시 제거
7. **복잡한 AI**: 이전 2턴 패턴 체크, 소환 가능 여부 체크
8. **미니언 AI**: 매우 단순 (상처 → 자폭)
9. **상처 카드**: 덱을 오염시킴
10. **전략**: 미니언을 먼저 처리할지, 본체를 먼저 처리할지 선택
