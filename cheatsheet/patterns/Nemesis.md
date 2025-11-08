# 복수자 (Nemesis)

## 기본 정보

**클래스명**: `Nemesis`
**전체 경로**: `com.megacrit.cardcrawl.monsters.beyond.Nemesis`
**ID**: `"Nemesis"`
**타입**: 엘리트 (ELITE)
**등장 지역**: 3막 (Beyond)

---

## HP 정보

| 난이도 | HP |
|--------|-----|
| 기본 (A0-A7) | 185 |
| A8+ | 200 |

**코드 위치**: 67-71줄

```java
if (AbstractDungeon.ascensionLevel >= 8) {
    setHp(200);
} else {
    setHp(185);
}
```

---

## 생성자 정보

### 주요 생성자
```java
public Nemesis()
```

**특징**:
- X: 0.0F, Y: 0.0F
- 히트박스: 5.0F, -10.0F, 350.0F, 440.0F
- 애니메이션: `images/monsters/theForest/nemesis/skeleton.*`
- 3개의 눈(eye) 본(Bone) 참조 (시각 효과용)

---

## 특수 메커니즘: 무적(Intangible) 시스템

**핵심 동작**:
- 매 턴 종료 시 **무적(Intangible) 1턴** 자동 부여
- 무적 상태일 때 받는 데미지가 1로 감소

**코드 위치**: 117-119줄, 125-128줄

```java
// takeTurn 마지막에 자동 부여
if (!hasPower("Intangible")) {
    AbstractDungeon.actionManager.addToBottom(
        new ApplyPowerAction(this, this,
            new IntangiblePower(this, 1))
    );
}

// damage 메서드에서 데미지 감소 처리
public void damage(DamageInfo info) {
    if (info.output > 0 && hasPower("Intangible")) {
        info.output = 1;
    }
    // ...
}
```

**전략적 의미**:
- 플레이어는 2턴마다 1번만 실제 데미지를 줄 수 있음
- 무적을 제거하는 카드/파워가 유용함
- 약한 공격을 여러 번 하면 비효율적

---

## 특수 메커니즘: 대낫 쿨다운

**필드**: `private int scytheCooldown = 0`

**동작 원리**:
- 대낫(Scythe) 사용 시 쿨다운 2로 설정
- 매 턴마다 쿨다운 감소
- 쿨다운 > 0이면 대낫 사용 불가

**코드 위치**: 43줄, 153줄, 167-170줄

```java
// 필드 선언
private int scytheCooldown = 0;

// getMove 시작 시 감소
protected void getMove(int num) {
    this.scytheCooldown--;
    // ...
}

// 대낫 사용 시 쿨다운 설정
if (!lastMove((byte)3) && this.scytheCooldown <= 0) {
    setMove((byte)3, AbstractMonster.Intent.ATTACK, 45);
    this.scytheCooldown = 2;  // 쿨다운 2턴
}
```

---

## 패턴 정보

### 패턴 2: 삼연격 (Tri-Attack)

**바이트 값**: `2` (TRI_ATTACK)
**의도**: `ATTACK`
**데미지**: 6 x 3 또는 7 x 3 (총 18 또는 21)

**데미지 설정**:
| 난이도 | 1회 데미지 | 총 데미지 |
|--------|-----------|----------|
| A0-A2 | 6 | 18 |
| A3+ | 7 | 21 |

**효과**:
- 플레이어에게 **3회 연속 공격**
- 각 공격이 독립적으로 계산됨

**코드 위치**: 38-42줄, 73-77줄, 80줄, 94-98줄

```java
// 데미지 상수
private static final int FIRE_DMG = 6;
private static final int FIRE_TIMES = 3;
private static final int A_2_FIRE_DMG = 7;

// 생성자에서 설정
if (AbstractDungeon.ascensionLevel >= 3) {
    this.fireDmg = 7;
} else {
    this.fireDmg = 6;
}

// 데미지 등록 (index 1)
this.damage.add(new DamageInfo(this, this.fireDmg));

// takeTurn 실행
case 2:
    for (i = 0; i < 3; i++) {
        AbstractDungeon.actionManager.addToBottom(
            new DamageAction(AbstractDungeon.player,
                this.damage.get(1),
                AbstractGameAction.AttackEffect.FIRE)
        );
    }
    break;
```

**특징**:
- 공격 이펙트: FIRE (3번)
- 각 공격이 개별적으로 방어도 무시
- 취약(Vulnerable) 상태 시 3번 모두 적용

**수정 포인트**:
- 데미지 변경: `fireDmg` 필드 또는 생성자 로직
- 공격 횟수 변경: `for (i = 0; i < 3; i++)` 수정

---

### 패턴 3: 대낫 (Scythe)

**바이트 값**: `3` (SCYTHE)
**의도**: `ATTACK`
**데미지**: 45 (모든 난이도 동일)
**쿨다운**: 2턴

**효과**:
- 플레이어에게 **45 데미지**
- 사용 후 2턴 동안 재사용 불가

**코드 위치**: 37줄, 79줄, 87-93줄

```java
// 데미지 상수
private static final int SCYTHE_DMG = 45;

// 생성자에서 데미지 등록 (index 0)
this.damage.add(new DamageInfo(this, 45));

// takeTurn 실행
case 3:
    AbstractDungeon.actionManager.addToBottom(
        new ChangeStateAction(this, "ATTACK")
    );
    playSfx();
    AbstractDungeon.actionManager.addToBottom(
        new WaitAction(0.4F)
    );
    AbstractDungeon.actionManager.addToBottom(
        new DamageAction(AbstractDungeon.player,
            this.damage.get(0),
            AbstractGameAction.AttackEffect.SLASH_HEAVY)
    );
    break;
```

**특징**:
- 공격 이펙트: SLASH_HEAVY
- 공격 애니메이션 재생
- 음성 효과 랜덤 재생 (VO_NEMESIS_1A/1B)

**수정 포인트**:
- 데미지 변경: `SCYTHE_DMG` 상수 또는 damage.get(0) 수정
- 쿨다운 변경: `this.scytheCooldown = 2` 수정

---

### 패턴 4: 화상 (Tri-Burn)

**바이트 값**: `4` (TRI_BURN)
**의도**: `DEBUFF`
**효과**: 덱에 **화상(Burn) 카드** 추가

**화상 카드 수**:
| 난이도 | 화상 카드 수 |
|--------|-------------|
| A0-A17 | 3 |
| A18+ | 5 |

**코드 위치**: 41줄, 100-112줄

```java
// 화상 카드 수 상수
private static final int BURN_AMT = 3;

// takeTurn 실행
case 4:
    AbstractDungeon.actionManager.addToBottom(
        new SFXAction("VO_NEMESIS_1C")
    );
    AbstractDungeon.actionManager.addToBottom(
        new VFXAction(this,
            new ShockWaveEffect(this.hb.cX, this.hb.cY,
                Settings.GREEN_TEXT_COLOR,
                ShockWaveEffect.ShockWaveType.CHAOTIC),
            1.5F)
    );

    if (AbstractDungeon.ascensionLevel >= 18) {
        AbstractDungeon.actionManager.addToBottom(
            new MakeTempCardInDiscardAction(new Burn(), 5)
        );
    } else {
        AbstractDungeon.actionManager.addToBottom(
            new MakeTempCardInDiscardAction(new Burn(), 3)
        );
    }
    break;
```

**특징**:
- 충격파(ShockWave) 시각 효과
- 음성 효과 (VO_NEMESIS_1C)
- 화상 카드는 버리기 더미에 추가
- 화상 카드: 소모(Unplayable), 코스트 증가

**수정 포인트**:
- 화상 카드 수 변경: `new Burn(), 3` 또는 `5` 수정
- 다른 상태이상 카드로 변경: `new Burn()` → `new Wound()` 등

---

## 특수 동작

### 매 턴 후 무적 부여

**코드 위치**: 117-119줄

```java
// takeTurn 마지막
if (!hasPower("Intangible")) {
    AbstractDungeon.actionManager.addToBottom(
        new ApplyPowerAction(this, this,
            new IntangiblePower(this, 1))
    );
}
```

**특징**:
- 모든 패턴 후 자동 실행
- 이미 무적이면 추가 부여 안 함

### 피격 시 애니메이션

**코드 위치**: 125-136줄

```java
public void damage(DamageInfo info) {
    if (info.output > 0 && hasPower("Intangible")) {
        info.output = 1;
    }

    if (info.owner != null &&
        info.type != DamageInfo.DamageType.THORNS &&
        info.output > 0) {
        AnimationState.TrackEntry e =
            this.state.setAnimation(0, "Hit", false);
        this.state.addAnimation(0, "Idle", true, 0.0F);
        e.setTimeScale(0.8F);
    }
    super.damage(info);
}
```

**특징**:
- 가시 데미지는 애니메이션 없음
- Hit 애니메이션 후 Idle로 전환

### 사망 시

**코드 위치**: 233-236줄

```java
public void die() {
    playDeathSfx();
    super.die();
}
```

**특징**:
- 랜덤 사망 음성 (VO_NEMESIS_2A/2B)

### 시각 효과 (update)

**코드 위치**: 239-253줄

```java
public void update() {
    super.update();
    if (!this.isDying) {
        this.fireTimer -= Gdx.graphics.getDeltaTime();
        if (this.fireTimer < 0.0F) {
            this.fireTimer = 0.05F;
            // 3개의 눈에서 불 파티클 생성
            AbstractDungeon.effectList.add(
                new NemesisFireParticle(
                    this.skeleton.getX() + this.eye1.getWorldX(),
                    this.skeleton.getY() + this.eye1.getWorldY())
            );
            // eye2, eye3도 동일
        }
    }
}
```

**특징**:
- 0.05초마다 불 파티클 생성
- 3개의 눈에서 파티클 발생

---

## AI 로직 (getMove)

**코드 위치**: 152-210줄

### 첫 번째 턴

```java
if (this.firstMove) {
    this.firstMove = false;

    if (num < 50) {
        setMove((byte)2, AbstractMonster.Intent.ATTACK,
            this.fireDmg, 3, true);  // 삼연격
    } else {
        setMove((byte)4, AbstractMonster.Intent.DEBUFF);  // 화상
    }
    return;
}
```

**로직**:
- 50% 확률로 삼연격(2) 또는 화상(4)

### 이후 턴: num < 30 (30% 확률)

```java
if (num < 30) {
    if (!lastMove((byte)3) && this.scytheCooldown <= 0) {
        setMove((byte)3, AbstractMonster.Intent.ATTACK, 45);  // 대낫
        this.scytheCooldown = 2;
    }
    else if (AbstractDungeon.aiRng.randomBoolean()) {
        if (!lastTwoMoves((byte)2)) {
            setMove((byte)2, AbstractMonster.Intent.ATTACK,
                this.fireDmg, 3, true);  // 삼연격
        } else {
            setMove((byte)4, AbstractMonster.Intent.DEBUFF);  // 화상
        }
    }
    else if (!lastMove((byte)4)) {
        setMove((byte)4, AbstractMonster.Intent.DEBUFF);  // 화상
    } else {
        setMove((byte)2, AbstractMonster.Intent.ATTACK,
            this.fireDmg, 3, true);  // 삼연격
    }
}
```

**로직**:
1. 이전 턴이 대낫이 아니고 쿨다운 끝났으면 → 대낫(3)
2. 그렇지 않으면 50% 확률:
   - 이전 2턴이 삼연격이 아니면 → 삼연격(2)
   - 이전 2턴이 삼연격이면 → 화상(4)
3. 나머지 50%:
   - 이전 턴이 화상이 아니면 → 화상(4)
   - 이전 턴이 화상이면 → 삼연격(2)

### 이후 턴: num 30-64 (35% 확률)

```java
else if (num < 65) {
    if (!lastTwoMoves((byte)2)) {
        setMove((byte)2, AbstractMonster.Intent.ATTACK,
            this.fireDmg, 3, true);  // 삼연격
    }
    else if (AbstractDungeon.aiRng.randomBoolean()) {
        if (this.scytheCooldown > 0) {
            setMove((byte)4, AbstractMonster.Intent.DEBUFF);  // 화상
        } else {
            setMove((byte)3, AbstractMonster.Intent.ATTACK, 45);  // 대낫
            this.scytheCooldown = 2;
        }
    } else {
        setMove((byte)4, AbstractMonster.Intent.DEBUFF);  // 화상
    }
}
```

**로직**:
1. 이전 2턴이 삼연격이 아니면 → 삼연격(2)
2. 이전 2턴이 삼연격이면 50% 확률:
   - 쿨다운 중이면 → 화상(4)
   - 쿨다운 끝났으면 → 대낫(3)
3. 나머지 50% → 화상(4)

### 이후 턴: num 65-99 (35% 확률)

```java
else if (!lastMove((byte)4)) {
    setMove((byte)4, AbstractMonster.Intent.DEBUFF);  // 화상
}
else if (AbstractDungeon.aiRng.randomBoolean() &&
         this.scytheCooldown <= 0) {
    setMove((byte)3, AbstractMonster.Intent.ATTACK, 45);  // 대낫
    this.scytheCooldown = 2;
} else {
    setMove((byte)2, AbstractMonster.Intent.ATTACK,
        this.fireDmg, 3, true);  // 삼연격
}
```

**로직**:
1. 이전 턴이 화상이 아니면 → 화상(4)
2. 이전 턴이 화상이고 50% 확률 & 쿨다운 끝났으면 → 대낫(3)
3. 그 외 → 삼연격(2)

**수정 포인트**:
- 확률 변경: `if (num < 30)`, `if (num < 65)` 조건
- 패턴 우선순위 변경
- 쿨다운 로직 변경

---

## 수정 예시

### 1. 무적 제거 (A25)

```java
@SpirePatch(
    cls = "com.megacrit.cardcrawl.monsters.beyond.Nemesis",
    method = "takeTurn"
)
public static class NemesisNoIntangiblePatch {
    @SpirePostfixPatch
    public static void Postfix(Nemesis __instance) {
        if (AbstractDungeon.isAscensionMode &&
            AbstractDungeon.ascensionLevel >= 25) {
            // 마지막에 추가된 무적 제거
            // 또는 무적 대신 다른 효과 추가
        }
    }
}
```

### 2. 대낫 데미지 증가

```java
@SpirePatch(
    cls = "com.megacrit.cardcrawl.monsters.beyond.Nemesis",
    method = SpirePatch.CONSTRUCTOR
)
public static class NemesisScytheDamagePatch {
    @SpirePostfixPatch
    public static void Postfix(Nemesis __instance) {
        if (AbstractDungeon.isAscensionMode &&
            AbstractDungeon.ascensionLevel >= 25) {
            // damage.get(0)은 대낫
            if (!__instance.damage.isEmpty()) {
                __instance.damage.get(0).base += 10;  // 45 → 55
            }
        }
    }
}
```

### 3. 화상 카드 추가 증가

```java
@SpirePatch(
    cls = "com.megacrit.cardcrawl.monsters.beyond.Nemesis",
    method = "takeTurn"
)
public static class NemesisBurnPatch {
    @SpireInsertPatch(
        locator = BurnLocator.class
    )
    public static void Insert(Nemesis __instance) {
        if (AbstractDungeon.isAscensionMode &&
            AbstractDungeon.ascensionLevel >= 25) {
            // 추가 화상 카드
            AbstractDungeon.actionManager.addToBottom(
                new MakeTempCardInDiscardAction(new Burn(), 2)
            );
        }
    }

    private static class BurnLocator extends SpireInsertLocator {
        @Override
        public int[] Locate(CtBehavior ctMethodToPatch) throws Exception {
            Matcher matcher = new Matcher.MethodCallMatcher(
                MakeTempCardInDiscardAction.class, "<init>");
            return LineFinder.findInOrder(ctMethodToPatch, matcher);
        }
    }
}
```

---

## 중요 필드

| 필드명 | 타입 | 설명 |
|--------|------|------|
| `firstMove` | boolean | 첫 턴 여부 |
| `scytheCooldown` | int | 대낫 쿨다운 (0 이하일 때 사용 가능) |
| `fireDmg` | int | 삼연격 1회 데미지 |
| `fireTimer` | float | 불 파티클 생성 타이머 |
| `eye1, eye2, eye3` | Bone | 불 파티클 생성 위치 |

---

## 관련 파일

- **본 파일**: `com/megacrit/cardcrawl/monsters/beyond/Nemesis.java`
- **카드**:
  - `com.megacrit.cardcrawl.cards.status.Burn`
- **파워**:
  - `com.megacrit.cardcrawl.powers.IntangiblePower`
- **이펙트**:
  - `com.megacrit.cardcrawl.vfx.NemesisFireParticle`
  - `com.megacrit.cardcrawl.vfx.combat.ShockWaveEffect`
- **액션**:
  - `ChangeStateAction`
  - `DamageAction`
  - `ApplyPowerAction`
  - `MakeTempCardInDiscardAction`
  - `VFXAction`
  - `SFXAction`
  - `WaitAction`
  - `RollMoveAction`

---

## 참고사항

1. **무적 시스템**: 핵심 메커니즘, 매 턴마다 무적 부여
2. **쿨다운 관리**: 대낫은 2턴마다 한 번만 사용 가능
3. **복잡한 AI**: 이전 패턴과 쿨다운 상태를 모두 고려
4. **다양한 패턴**: 공격(2종) + 디버프(1종)
5. **시각 효과**: 3개의 눈에서 불 파티클 지속 생성
6. **첫 턴 랜덤**: 50% 확률로 삼연격 또는 화상
7. **A18 변경점**: 화상 카드 3 → 5개
8. **무적 카운터**: 무적 제거 카드/파워가 효과적
