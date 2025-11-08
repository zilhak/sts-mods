# 라가불린 (Lagavulin)

## 기본 정보

**클래스명**: `Lagavulin`
**전체 경로**: `com.megacrit.cardcrawl.monsters.exordium.Lagavulin`
**ID**: `"Lagavulin"`
**타입**: 엘리트 (ELITE)
**등장 지역**: 1막 (Exordium)

---

## HP 정보

| 난이도 | HP 범위 |
|--------|---------|
| 기본 (A0-A7) | 109-111 |
| A8+ | 112-115 |

**코드 위치**: 35-36줄, 56-60줄

```java
private static final int HP_MIN = 109;
private static final int HP_MAX = 111;
private static final int A_2_HP_MIN = 112;
private static final int A_2_HP_MAX = 115;

if (AbstractDungeon.ascensionLevel >= 8) {
    setHp(112, 115);
} else {
    setHp(109, 111);
}
```

---

## 생성자 정보

### 주요 생성자
```java
public Lagavulin(boolean setAsleep)
```

**파라미터**:
- `setAsleep`: 수면 상태로 시작할지 여부

**중요**:
- `setAsleep = true`: 수면 상태로 시작 (기본값)
- `setAsleep = false`: 깨어있는 상태로 시작

**코드 위치**: 51-95줄

```java
this.asleep = setAsleep;

// 애니메이션 설정
AnimationState.TrackEntry e = null;
if (!this.asleep) {
    this.isOut = true;
    this.isOutTriggered = true;
    e = this.state.setAnimation(0, "Idle_2", true);  // 깨어있는 애니메이션
    updateHitbox(0.0F, -25.0F, 320.0F, 370.0F);
} else {
    e = this.state.setAnimation(0, "Idle_1", true);  // 수면 애니메이션
}
```

---

## 패턴 정보

### 패턴 1: 디버프 (Debuff)

**바이트 값**: `1`
**의도**: `STRONG_DEBUFF`
**발동 조건**: 깨어난 후 첫 턴 또는 AI 로직에 따름

**효과**:
- 플레이어의 **민첩(Dexterity)** 감소
- 플레이어의 **힘(Strength)** 감소

**디버프 수치**:
| 난이도 | 민첩/힘 감소 |
|--------|--------------|
| 기본 (A0-A17) | -1 |
| A18+ | -2 |

**코드 위치**: 35-36줄, 42줄, 68-72줄, 117-134줄

```java
// 상수
private static final byte DEBUFF = 1;
private static final int DEBUFF_AMT = -1;
private static final int A_18_DEBUFF_AMT = -2;

// 생성자에서 설정
if (AbstractDungeon.ascensionLevel >= 18) {
    this.debuff = -2;
} else {
    this.debuff = -1;
}

// takeTurn 실행
case 1:
    this.debuffTurnCount = 0;
    AbstractDungeon.actionManager.addToBottom(
        new ChangeStateAction(this, "DEBUFF")
    );
    AbstractDungeon.actionManager.addToBottom(new WaitAction(0.3F));

    // 민첩 감소
    AbstractDungeon.actionManager.addToBottom(
        new ApplyPowerAction(
            (AbstractCreature)AbstractDungeon.player,
            (AbstractCreature)this,
            new DexterityPower((AbstractCreature)AbstractDungeon.player, this.debuff),
            this.debuff
        )
    );

    // 힘 감소
    AbstractDungeon.actionManager.addToBottom(
        new ApplyPowerAction(
            (AbstractCreature)AbstractDungeon.player,
            (AbstractCreature)this,
            new StrengthPower((AbstractCreature)AbstractDungeon.player, this.debuff),
            this.debuff
        )
    );

    AbstractDungeon.actionManager.addToBottom(new RollMoveAction(this));
    break;
```

**특징**:
- 매우 강력한 영구 디버프
- 민첩과 힘을 동시에 감소
- 전투가 길어질수록 누적됨
- "Debuff" 애니메이션 재생

**수정 포인트**:
- 디버프 수치 변경: `debuff` 필드 또는 A18 조건문
- 디버프 종류 추가/변경: takeTurn의 ApplyPowerAction 수정

---

### 패턴 2: 강력한 공격 (Strong Attack)

**바이트 값**: `3`
**의도**: `ATTACK`
**발동 조건**: 깨어난 후 AI 로직에 따름

**데미지**:
| 난이도 | 데미지 |
|--------|--------|
| 기본 (A0-A2) | 18 |
| A3+ | 20 |

**코드 위치**: 38줄, 42줄, 62-66줄, 74줄, 135-142줄

```java
// 상수
private static final byte STRONG_ATK = 3;
private static final int STRONG_ATK_DMG = 18;
private static final int A_2_STRONG_ATK_DMG = 20;

// 생성자에서 설정
if (AbstractDungeon.ascensionLevel >= 3) {
    this.attackDmg = 20;
} else {
    this.attackDmg = 18;
}

// 데미지 등록
this.damage.add(new DamageInfo((AbstractCreature)this, this.attackDmg));

// takeTurn 실행
case 3:
    this.debuffTurnCount++;
    AbstractDungeon.actionManager.addToBottom(
        new ChangeStateAction(this, "ATTACK")
    );
    AbstractDungeon.actionManager.addToBottom(new WaitAction(0.3F));
    AbstractDungeon.actionManager.addToBottom(
        new DamageAction(
            (AbstractCreature)AbstractDungeon.player,
            this.damage.get(0),
            AbstractGameAction.AttackEffect.BLUNT_HEAVY
        )
    );
    AbstractDungeon.actionManager.addToBottom(new RollMoveAction(this));
    break;
```

**특징**:
- 높은 데미지
- "Attack" 애니메이션 재생
- `debuffTurnCount` 증가 (디버프 사용 빈도 관리)

**수정 포인트**:
- 데미지 변경: `attackDmg` 필드 또는 A3 조건문

---

### 패턴 3: 수면 (Idle/Sleep)

**바이트 값**: `5`
**의도**: `SLEEP`
**발동 조건**: 수면 상태일 때

**효과**: 아무 행동도 하지 않음 (턴 넘김)

**코드 위치**: 40줄, 49줄, 143-165줄

```java
// 상수
private static final byte IDLE = 5;

// takeTurn 실행
case 5:
    this.idleCount++;
    if (this.idleCount >= 3) {
        // 3턴 수면 후 깨어남
        logger.info("idle happened");
        this.isOutTriggered = true;
        AbstractDungeon.actionManager.addToBottom(
            new ChangeStateAction(this, "OPEN")
        );
        AbstractDungeon.actionManager.addToBottom(
            new SetMoveAction(this, (byte)3, AbstractMonster.Intent.ATTACK,
                ((DamageInfo)this.damage.get(0)).base)
        );
    } else {
        setMove((byte)5, AbstractMonster.Intent.SLEEP);
    }

    // 대사 출력
    switch (this.idleCount) {
        case 1:
            AbstractDungeon.actionManager.addToBottom(
                new TalkAction((AbstractCreature)this, DIALOG[1], 0.5F, 2.0F)
            );
            AbstractDungeon.actionManager.addToBottom(new RollMoveAction(this));
            break;
        case 2:
            AbstractDungeon.actionManager.addToBottom(
                new TalkAction((AbstractCreature)this, DIALOG[2], 0.5F, 2.0F)
            );
            AbstractDungeon.actionManager.addToBottom(new RollMoveAction(this));
            break;
    }
    break;
```

**특징**:
- 3턴 동안 수면
- 1, 2턴째 대사 출력
- 3턴째 자동으로 깨어남
- 데미지를 받으면 즉시 깨어남 (damage 메서드 참조)

---

### 패턴 4: 깨어남 (Open)

**바이트 값**: `4` (플레이어 공격으로 깨어남)
**바이트 값**: `6` (자연스럽게 깨어남)
**의도**: `STUN` (플레이어 공격으로 깨어남 시)
**발동 조건**: 수면 중 데미지를 받거나 3턴 경과

**효과**:
- 수면 상태 해제
- Metallicize 파워 제거
- 방어도 제거
- 다음 턴에 공격 준비

**코드 위치**: 39-41줄, 167-177줄, 187-206줄, 209-224줄

```java
// 상수
private static final byte OPEN = 4;
private static final byte OPEN_NATURAL = 6;

// takeTurn 실행 - 플레이어 공격으로 깨어남
case 4:
    AbstractDungeon.actionManager.addToBottom(
        new TextAboveCreatureAction((AbstractCreature)this,
            TextAboveCreatureAction.TextType.STUNNED)
    );
    AbstractDungeon.actionManager.addToBottom(new RollMoveAction(this));
    break;

// takeTurn 실행 - 자연스럽게 깨어남
case 6:
    AbstractDungeon.actionManager.addToBottom(
        new ChangeStateAction(this, "OPEN")
    );
    setMove((byte)3, AbstractMonster.Intent.ATTACK,
        ((DamageInfo)this.damage.get(0)).base);
    createIntent();
    this.isOutTriggered = true;
    AbstractDungeon.actionManager.addToBottom(new RollMoveAction(this));
    break;

// changeState 실행
public void changeState(String stateName) {
    if (stateName.equals("OPEN") && !this.isDying) {
        this.isOut = true;
        updateHitbox(0.0F, -25.0F, 320.0F, 360.0F);

        // 대사 출력
        AbstractDungeon.actionManager.addToBottom(
            new TalkAction((AbstractCreature)this, DIALOG[3], 0.5F, 2.0F)
        );

        // Metallicize 제거
        AbstractDungeon.actionManager.addToBottom(
            new ReducePowerAction((AbstractCreature)this, (AbstractCreature)this,
                "Metallicize", 8)
        );

        // 음악 전환
        CardCrawlGame.music.unsilenceBGM();
        AbstractDungeon.scene.fadeOutAmbiance();
        CardCrawlGame.music.playPrecachedTempBgm();

        // 애니메이션 전환
        this.state.setAnimation(0, "Coming_out", false);
        this.state.addAnimation(0, "Idle_2", true, 0.0F);
    }
}
```

**특징**:
- 플레이어 공격으로 깨어남: 1턴 스턴 (아무 행동 안 함)
- 자연스럽게 깨어남: 즉시 공격 준비
- Metallicize 8 제거
- 엘리트 전투 음악 시작
- "Coming_out" 애니메이션 재생

---

## AI 로직 (getMove)

**코드 위치**: 227-241줄

```java
protected void getMove(int num) {
    if (this.isOut) {
        // 깨어있는 상태
        if (this.debuffTurnCount < 2) {
            // 디버프를 2번 미만 사용했으면
            if (lastTwoMoves((byte)3)) {
                // 공격 연속 2번 → 디버프
                setMove(DEBUFF_NAME, (byte)1, AbstractMonster.Intent.STRONG_DEBUFF);
            } else {
                // 그 외 → 공격
                setMove((byte)3, AbstractMonster.Intent.ATTACK,
                    ((DamageInfo)this.damage.get(0)).base);
            }
        } else {
            // 디버프를 2번 사용했으면 → 디버프
            setMove(DEBUFF_NAME, (byte)1, AbstractMonster.Intent.STRONG_DEBUFF);
        }
    } else {
        // 수면 상태 → 계속 수면
        setMove((byte)5, AbstractMonster.Intent.SLEEP);
    }
}
```

**로직 설명**:

### 수면 상태 (isOut = false)
- 무조건 **수면(IDLE)** 패턴
- takeTurn에서 `idleCount` 관리

### 깨어있는 상태 (isOut = true)
1. **디버프 횟수 < 2**:
   - 공격 연속 2번 사용 → **디버프**
   - 그 외 → **공격**
2. **디버프 횟수 ≥ 2**:
   - 무조건 **디버프**

**특징**:
- 디버프 사용 횟수 추적 (`debuffTurnCount`)
- 최소 2번은 디버프 사용 보장
- 공격 연속 사용 방지

---

## 특수 메커니즘

### 수면 시스템

**핵심 변수**:
- `asleep`: 전투 시작 시 수면 여부 (생성자 파라미터)
- `isOut`: 현재 깨어있는지 여부
- `isOutTriggered`: 깨어남 트리거 발동 여부
- `idleCount`: 수면 턴 카운터 (0-3)

**수면 상태 특징**:
1. **시작 시 보호**:
   - **방어도 8** 획득
   - **Metallicize 8** 파워 부여
2. **3턴 제한**:
   - 3턴 동안 아무 행동 안 함
   - 3턴째 자동으로 깨어남
3. **조기 깨어남**:
   - 데미지를 받으면 즉시 깨어남
   - 1턴 스턴 상태 (아무 행동 안 함)

**코드 위치**: 98-110줄 (usePreBattleAction)

```java
public void usePreBattleAction() {
    if (this.asleep) {
        // 수면 시작 시 엘리트 음악 미리 로드
        CardCrawlGame.music.precacheTempBgm("ELITE");

        // 방어도 8 획득
        AbstractDungeon.actionManager.addToBottom(
            new GainBlockAction((AbstractCreature)this, (AbstractCreature)this, 8)
        );

        // Metallicize 8 부여
        AbstractDungeon.actionManager.addToBottom(
            new ApplyPowerAction((AbstractCreature)this, (AbstractCreature)this,
                new MetallicizePower((AbstractCreature)this, 8), 8)
        );
    } else {
        // 깨어있는 상태로 시작 시 즉시 음악 재생
        CardCrawlGame.music.unsilenceBGM();
        AbstractDungeon.scene.fadeOutAmbiance();
        CardCrawlGame.music.playTempBgmInstantly("ELITE");
        setMove(DEBUFF_NAME, (byte)1, AbstractMonster.Intent.STRONG_DEBUFF);
    }
}
```

### Metallicize 파워

**파워 효과**: 매 턴 종료 시 방어도 획득
**초기 수치**: 8
**제거 시점**: 깨어날 때 완전히 제거됨

**의미**:
- 수면 중 데미지 경감
- 플레이어가 공격해도 효과적으로 방어
- 깨어나면 방어 능력 상실

### 히트박스 변경

**코드 위치**: 86줄, 196줄

```java
// 수면 상태: 작은 히트박스
// (기본 히트박스 사용)

// 깨어난 상태: 큰 히트박스
updateHitbox(0.0F, -25.0F, 320.0F, 360.0F);
```

**의미**:
- 수면 중: 작게 웅크린 형태
- 깨어남: 큰 형태로 변경

### 데미지 반응 시스템

**코드 위치**: 209-224줄

```java
public void damage(DamageInfo info) {
    int previousHealth = this.currentHealth;
    super.damage(info);

    if (this.currentHealth != previousHealth && !this.isOutTriggered) {
        // 수면 중 데미지를 받으면 깨어남
        setMove((byte)4, AbstractMonster.Intent.STUN);
        createIntent();
        this.isOutTriggered = true;
        AbstractDungeon.actionManager.addToBottom(
            new ChangeStateAction(this, "OPEN")
        );
    } else if (this.isOutTriggered && info.owner != null &&
               info.type != DamageInfo.DamageType.THORNS && info.output > 0) {
        // 깨어있을 때 데미지를 받으면 Hit 애니메이션
        this.state.setAnimation(0, "Hit", false);
        this.state.addAnimation(0, "Idle_2", true, 0.0F);
    }
}
```

**로직**:
1. **수면 중 데미지**: 즉시 깨어남, 다음 턴 스턴
2. **깨어있을 때 데미지**: Hit 애니메이션만 재생
3. **가시(Thorns) 데미지**: 무시 (깨어나지 않음)

### 디버프 카운터 시스템

**변수**: `debuffTurnCount`
**용도**: 디버프 사용 횟수 추적

**로직**:
- 디버프 사용 시: `debuffTurnCount = 0` (초기화)
- 공격 사용 시: `debuffTurnCount++`
- `debuffTurnCount >= 2`: 강제로 디버프 사용

**의미**: 최소 2번은 디버프를 사용하도록 보장

---

## 수정 예시

### 1. 수면 턴 수 증가

```java
@SpirePatch(
    cls = "com.megacrit.cardcrawl.monsters.exordium.Lagavulin",
    method = "takeTurn"
)
public static class LagavulinIdlePatch {
    @SpireInsertPatch(
        locator = IdleLocator.class
    )
    public static void Insert(Lagavulin __instance) {
        if (AbstractDungeon.isAscensionMode &&
            AbstractDungeon.ascensionLevel >= 25) {
            // A25에서 수면 턴 수를 5턴으로 증가
            // (idleCount >= 3 조건을 idleCount >= 5로 변경)
        }
    }

    private static class IdleLocator extends SpireInsertLocator {
        @Override
        public int[] Locate(CtBehavior ctMethodToPatch) throws Exception {
            // idleCount >= 3 조건 찾기
            Matcher finalMatcher = new Matcher.FieldAccessMatcher(
                Lagavulin.class, "idleCount"
            );
            return LineFinder.findInOrder(ctMethodToPatch, finalMatcher);
        }
    }
}
```

### 2. 디버프 수치 증가

```java
@SpirePatch(
    cls = "com.megacrit.cardcrawl.monsters.exordium.Lagavulin",
    method = SpirePatch.CONSTRUCTOR
)
public static class LagavulinDebuffPatch {
    @SpirePostfixPatch
    public static void Postfix(Lagavulin __instance, boolean setAsleep) {
        if (AbstractDungeon.isAscensionMode &&
            AbstractDungeon.ascensionLevel >= 25) {
            // A25에서 디버프 -3으로 증가 (A18 기준 -1 추가)
            ReflectionHacks.setPrivate(__instance, Lagavulin.class, "debuff", -3);
        }
    }
}
```

### 3. Metallicize 수치 증가

```java
@SpirePatch(
    cls = "com.megacrit.cardcrawl.monsters.exordium.Lagavulin",
    method = "usePreBattleAction"
)
public static class LagavulinMetallicizePatch {
    @SpirePostfixPatch
    public static void Postfix(Lagavulin __instance) {
        if (AbstractDungeon.isAscensionMode &&
            AbstractDungeon.ascensionLevel >= 25) {
            // A25에서 Metallicize 추가 (기본 8 + 추가 4 = 12)
            boolean asleep = ReflectionHacks.getPrivate(__instance, Lagavulin.class, "asleep");
            if (asleep) {
                AbstractDungeon.actionManager.addToBottom(
                    new ApplyPowerAction(__instance, __instance,
                        new MetallicizePower(__instance, 4), 4)
                );
            }
        }
    }
}
```

### 4. 깨어날 때 추가 효과

```java
@SpirePatch(
    cls = "com.megacrit.cardcrawl.monsters.exordium.Lagavulin",
    method = "changeState"
)
public static class LagavulinOpenPatch {
    @SpirePostfixPatch
    public static void Postfix(Lagavulin __instance, String stateName) {
        if (stateName.equals("OPEN") &&
            AbstractDungeon.isAscensionMode &&
            AbstractDungeon.ascensionLevel >= 25) {
            // A25에서 깨어날 때 힘 3 획득
            AbstractDungeon.actionManager.addToBottom(
                new ApplyPowerAction(__instance, __instance,
                    new StrengthPower(__instance, 3), 3)
            );
        }
    }
}
```

---

## 중요 필드

| 필드명 | 타입 | 설명 |
|--------|------|------|
| `asleep` | boolean | 전투 시작 시 수면 여부 |
| `isOut` | boolean | 현재 깨어있는지 여부 |
| `isOutTriggered` | boolean | 깨어남 트리거 발동 여부 |
| `idleCount` | int | 수면 턴 카운터 (0-3) |
| `debuffTurnCount` | int | 디버프 사용 후 경과 턴 |
| `attackDmg` | int | 공격 데미지 |
| `debuff` | int | 디버프 수치 (음수) |

---

## 관련 파일

- **본 파일**: `com/megacrit/cardcrawl/monsters/exordium/Lagavulin.java`
- **파워**:
  - `com.megacrit.cardcrawl.powers.MetallicizePower` (금속화)
  - `com.megacrit.cardcrawl.powers.DexterityPower` (민첩)
  - `com.megacrit.cardcrawl.powers.StrengthPower` (힘)
- **액션**:
  - `DamageAction` (데미지 적용)
  - `ApplyPowerAction` (파워 부여)
  - `ReducePowerAction` (파워 제거)
  - `GainBlockAction` (방어도 획득)
  - `ChangeStateAction` (상태 변경)
  - `TalkAction` (대사 출력)
  - `TextAboveCreatureAction` (텍스트 표시)
  - `SetMoveAction` (패턴 강제 설정)

---

## 참고사항

1. **수면 전략**: 라가불린을 공격하지 않고 3턴 기다리면 자연스럽게 깨어남 (스턴 없음)
2. **조기 깨우기**: 공격해서 깨우면 1턴 스턴, 하지만 총 4턴을 얻는 것과 동일
3. **디버프 위험**: 민첩/힘 -1 or -2는 영구 디버프로 매우 위험함
4. **Metallicize**: 수면 중 8 방어도, 총 24 방어도 제공 (3턴)
5. **전투 음악**: 깨어나면 엘리트 전투 음악 시작
6. **애니메이션 상태**: Idle_1 (수면), Idle_2 (깨어남), Coming_out (깨어나는 중)
7. **히트박스 변경**: 깨어나면 히트박스가 커짐 (시각적 효과)
8. **가시 무시**: 가시(Thorns) 데미지로는 깨어나지 않음
9. **대사 시스템**: DIALOG[1], DIALOG[2] (수면 중), DIALOG[3] (깨어남)
10. **로거 사용**: "idle happened" 로그 출력 (디버깅용)
