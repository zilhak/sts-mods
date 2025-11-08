# 시간 먹는 자 (Time Eater)

## 기본 정보

**클래스명**: `TimeEater`
**전체 경로**: `com.megacrit.cardcrawl.monsters.beyond.TimeEater`
**ID**: `"TimeEater"`
**타입**: 보스 (BOSS)
**등장 지역**: 3막 (The Beyond)

---

## HP 정보

| 난이도 | HP |
|--------|-----|
| 기본 (A0-A8) | 456 |
| A9+ | 480 |

**특징**: 3막 보스, 카드 카운터 메커니즘, TimeWarp 시스템

---

## 생성자 정보

```java
public TimeEater() {
    super(NAME, "TimeEater", 456, -10.0F, -30.0F, 476.0F, 410.0F, null, -50.0F, 30.0F);

    // HP 설정
    if (AbstractDungeon.ascensionLevel >= 9) {
        setHp(480);
    } else {
        setHp(456);
    }

    // 애니메이션 로드
    loadAnimation("images/monsters/theForest/timeEater/skeleton.atlas",
                  "images/monsters/theForest/timeEater/skeleton.json", 1.0F);

    // 데미지 설정
    if (AbstractDungeon.ascensionLevel >= 4) {
        reverbDmg = 8;      // REVERBERATE
        headSlamDmg = 32;   // HEAD_SLAM
    } else {
        reverbDmg = 7;
        headSlamDmg = 26;
    }
}
```

---

## 고유 메커니즘

### 카드 카운터 시스템 (TimeWarp)

**핵심 메커니즘**: TimeEater는 플레이어가 사용하는 **모든 카드를 카운트**합니다.

**TimeWarpPower 특성**:
- 전투 시작 시 자동으로 TimeWarpPower 부여 (`usePreBattleAction`)
- 플레이어가 카드를 사용할 때마다 카운터 +1
- **카운터가 12에 도달하면** TimeWarp 발동

**TimeWarp 발동 효과**:
```java
public void onAfterUseCard(AbstractCard card, UseCardAction action) {
    flashWithoutSound();
    this.amount++;  // 카드 사용 시마다 +1

    if (this.amount == 12) {
        this.amount = 0;  // 카운터 리셋
        playApplyPowerSfx();

        // 1. 플레이어 턴 즉시 종료
        AbstractDungeon.actionManager.callEndTurnEarlySequence();

        // 2. 시각 효과
        AbstractDungeon.effectsQueue.add(new BorderFlashEffect(Color.GOLD, true));
        AbstractDungeon.topLevelEffectsQueue.add(new TimeWarpTurnEndEffect());

        // 3. 모든 몬스터 Strength +2
        for (AbstractMonster m : AbstractDungeon.getMonsters().monsters) {
            addToBot(new ApplyPowerAction(m, m, new StrengthPower(m, 2), 2));
        }
    }

    updateDescription();
}
```

**중요 특징**:
- **모든 카드** 카운트 (공격, 스킬, 파워 무관)
- **카운터는 전투 내내 유지** (리셋되지 않음, 12 도달 시만 0으로)
- TimeWarp 발동 시 **플레이어 턴 즉시 종료** + **Strength +2 영구 증가**
- 카운터는 화면에 표시됨 (파워 UI)

**전략적 함의**:
- **덱 사이클이 빠를수록 불리** (많은 카드 사용)
- **비용 0 카드**도 카운트됨
- **Innate 카드**, **X코스트 카드** 모두 카운트
- 12장 이후 Strength +2로 모든 공격 강화
- **덱이 크면** 더 많은 카드를 사용하게 되어 불리

---

## 패턴 정보

### 패턴 1: REVERBERATE (반향)

**의도**: `ATTACK` (다단 공격)
**바이트 코드**: `2`

**데미지**:
| 난이도 | 데미지 | 횟수 |
|--------|--------|------|
| 기본 (A0-A3) | 7 x 3 | 3회 |
| A4+ | 8 x 3 | 3회 |

**효과**:
- **7-8 데미지를 3회** 공격
- 총 **21-24 데미지**
- ShockWaveEffect 시각 효과

**코드**:
```java
case 2:  // REVERBERATE
    for (int i = 0; i < 3; i++) {
        AbstractDungeon.actionManager.addToBottom(
            new VFXAction(this, new ShockWaveEffect(
                this.hb.cX, this.hb.cY,
                Settings.BLUE_TEXT_COLOR,
                ShockWaveEffect.ShockWaveType.CHAOTIC), 0.75F)
        );

        AbstractDungeon.actionManager.addToBottom(
            new DamageAction(AbstractDungeon.player,
                this.damage.get(0),
                AbstractGameAction.AttackEffect.FIRE)
        );
    }
    break;
```

**발동 확률**:
- **< 45%**: 최근 2회 REVERBERATE 사용하지 않았다면 45% 확률
- 연속 2회 사용 방지 (`lastTwoMoves(2)`)

---

### 패턴 2: RIPPLE (파문)

**의도**: `DEFEND_DEBUFF`
**바이트 코드**: `3`

**효과**:
| 난이도 | Block | Vulnerable | Weak | Frail (A19+) |
|--------|-------|------------|------|--------------|
| 기본 (A0-A18) | 20 | 1턴 | 1턴 | - |
| A19+ | 20 | 1턴 | 1턴 | 1턴 |

**효과**:
- **자신에게 Block 20** 획득
- **플레이어에게 Vulnerable 1턴** (받는 데미지 +50%)
- **플레이어에게 Weak 1턴** (공격력 -25%)
- **A19+: Frail 1턴 추가** (Block -25%)

**코드**:
```java
case 3:  // RIPPLE
    AbstractDungeon.actionManager.addToBottom(
        new GainBlockAction(this, this, 20)
    );

    AbstractDungeon.actionManager.addToBottom(
        new ApplyPowerAction(AbstractDungeon.player, this,
            new VulnerablePower(AbstractDungeon.player, 1, true), 1)
    );

    AbstractDungeon.actionManager.addToBottom(
        new ApplyPowerAction(AbstractDungeon.player, this,
            new WeakPower(AbstractDungeon.player, 1, true), 1)
    );

    // A19+: Frail 추가
    if (AbstractDungeon.ascensionLevel >= 19) {
        AbstractDungeon.actionManager.addToBottom(
            new ApplyPowerAction(AbstractDungeon.player, this,
                new FrailPower(AbstractDungeon.player, 1, true), 1)
        );
    }
    break;
```

**발동 확률**:
- **80-99%**: 직전 턴이 HEAD_SLAM이 아니면 발동
- **34% 확률**로 REVERBERATE 대신 사용

---

### 패턴 3: HEAD_SLAM (머리 박치기)

**의도**: `ATTACK_DEBUFF`
**바이트 코드**: `4`

**데미지**:
| 난이도 | 데미지 | DrawReductionPower | Slimed (A19+) |
|--------|--------|-------------------|---------------|
| 기본 (A0-A3) | 26 | 1턴 | - |
| A4-A18 | 32 | 1턴 | - |
| A19+ | 32 | 1턴 | 2장 |

**효과**:
- **26-32 데미지** 공격
- **DrawReductionPower 1턴** (다음 턴 카드 드로우 1장 감소)
- **A19+: Slimed 2장** 버려진 카드 더미에 추가

**코드**:
```java
case 4:  // HEAD_SLAM
    AbstractDungeon.actionManager.addToBottom(
        new ChangeStateAction(this, "ATTACK")
    );
    AbstractDungeon.actionManager.addToBottom(new WaitAction(0.4F));

    AbstractDungeon.actionManager.addToBottom(
        new DamageAction(AbstractDungeon.player,
            this.damage.get(1),
            AbstractGameAction.AttackEffect.POISON)
    );

    // DrawReductionPower (드로우 -1)
    AbstractDungeon.actionManager.addToBottom(
        new ApplyPowerAction(AbstractDungeon.player, this,
            new DrawReductionPower(AbstractDungeon.player, 1))
    );

    // A19+: Slimed 2장
    if (AbstractDungeon.ascensionLevel >= 19) {
        AbstractDungeon.actionManager.addToBottom(
            new MakeTempCardInDiscardAction(new Slimed(), 2)
        );
    }
    break;
```

**발동 확률**:
- **50-79%**: 직전 턴이 HEAD_SLAM이 아니면 35% 확률
- 연속 사용 방지 (`lastMove(4)`)

---

### 패턴 4: HASTE (가속)

**의도**: `BUFF`
**바이트 코드**: `5`
**특징**: **전투 중 1회만 사용** 가능

**조건**:
- **HP < 50%** (maxHealth / 2)
- **아직 사용하지 않음** (`!usedHaste`)

**효과**:
- **모든 디버프 제거** (`RemoveDebuffsAction`)
- **Shackled 파워 제거** (Strength 감소 효과)
- **HP 회복**: `(maxHealth / 2) - currentHealth`
  - HP를 정확히 **50%로 회복**
- **A19+: Block 32 추가 획득** (headSlamDmg만큼)

**코드**:
```java
case 5:  // HASTE
    // 대사
    AbstractDungeon.actionManager.addToBottom(
        new ShoutAction(this, DIALOG[1], 0.5F, 2.0F)
    );

    // 디버프 제거
    AbstractDungeon.actionManager.addToBottom(
        new RemoveDebuffsAction(this)
    );

    // Shackled 제거
    AbstractDungeon.actionManager.addToBottom(
        new RemoveSpecificPowerAction(this, this, "Shackled")
    );

    // HP를 50%로 회복
    int healAmount = (this.maxHealth / 2) - this.currentHealth;
    AbstractDungeon.actionManager.addToBottom(
        new HealAction(this, this, healAmount)
    );

    // A19+: Block 32
    if (AbstractDungeon.ascensionLevel >= 19) {
        AbstractDungeon.actionManager.addToBottom(
            new GainBlockAction(this, this, this.headSlamDmg)
        );
    }
    break;
```

**중요**:
- **HP가 50% 이하로 떨어지면** 즉시 HASTE 사용
- **전투 중 1회만** 사용 가능 (`usedHaste = true`)
- **HP를 50%로 회복**하므로 **2번 체력바**처럼 작동
- **Strength 감소** 같은 디버프 모두 제거
- A19+에서는 **Block 32 추가**로 방어력도 확보

---

## 특수 동작

### usePreBattleAction

전투 시작 전 자동 실행:

```java
public void usePreBattleAction() {
    // 음악 설정
    CardCrawlGame.music.unsilenceBGM();
    AbstractDungeon.scene.fadeOutAmbiance();
    AbstractDungeon.getCurrRoom().playBgmInstantly("BOSS_BEYOND");

    // 업적 추적
    UnlockTracker.markBossAsSeen("WIZARD");

    // TimeWarpPower 부여
    AbstractDungeon.actionManager.addToBottom(
        new ApplyPowerAction(this, this, new TimeWarpPower(this))
    );
}
```

**효과**:
- 3막 보스 음악 재생
- **TimeWarpPower 자동 부여** (카드 카운터 시작)

---

### die

사망 시 처리:

```java
public void die() {
    if (!AbstractDungeon.getCurrRoom().cannotLose) {
        useFastShakeAnimation(5.0F);
        CardCrawlGame.screenShake.rumble(4.0F);
        super.die();

        onBossVictoryLogic();
        UnlockTracker.hardUnlockOverride("WIZARD");
        UnlockTracker.unlockAchievement("TIME_EATER");
        onFinalBossVictoryLogic();
    }
}
```

**효과**:
- 화면 흔들림 효과
- 보스 승리 처리
- 업적 해제

---

### damage

피격 애니메이션:

```java
public void damage(DamageInfo info) {
    super.damage(info);

    if (info.owner != null &&
        info.type != DamageInfo.DamageType.THORNS &&
        info.output > 0) {
        this.state.setAnimation(0, "Hit", false);
        this.state.addAnimation(0, "Idle", true, 0.0F);
    }
}
```

---

### changeState

상태 변경 (공격 애니메이션):

```java
public void changeState(String stateName) {
    switch (stateName) {
        case "ATTACK":
            this.state.setAnimation(0, "Attack", false);
            this.state.addAnimation(0, "Idle", true, 0.0F);
            break;
    }
}
```

---

## AI 로직 (getMove)

### 우선순위 1: HASTE (HP < 50% & 미사용)

```java
if (this.currentHealth < this.maxHealth / 2 && !this.usedHaste) {
    this.usedHaste = true;
    setMove((byte)5, AbstractMonster.Intent.BUFF);
    return;
}
```

**조건**:
- HP < 50%
- 아직 HASTE 미사용

**우선순위**: **최우선** (다른 패턴보다 먼저 체크)

---

### 패턴 선택 확률 (num < 45)

```java
if (num < 45) {  // 45% 확률
    if (!lastTwoMoves((byte)2)) {
        // REVERBERATE (7-8 x 3)
        setMove((byte)2, AbstractMonster.Intent.ATTACK,
                ((DamageInfo)this.damage.get(0)).base, 3, true);
        return;
    }
    // 최근 2회 사용했다면 재추첨
    getMove(AbstractDungeon.aiRng.random(50, 99));
    return;
}
```

**REVERBERATE**:
- **45% 확률**
- **최근 2회 사용하지 않음** 조건
- 조건 불충족 시 **50-99 범위에서 재추첨**

---

### 패턴 선택 확률 (45 ≤ num < 80)

```java
if (num < 80) {  // 35% 확률 (45~79)
    if (!lastMove((byte)4)) {
        // HEAD_SLAM (26-32)
        setMove((byte)4, AbstractMonster.Intent.ATTACK_DEBUFF,
                ((DamageInfo)this.damage.get(1)).base);
        return;
    }

    // 직전이 HEAD_SLAM이면
    if (AbstractDungeon.aiRng.randomBoolean(0.66F)) {
        // 66%: REVERBERATE
        setMove((byte)2, AbstractMonster.Intent.ATTACK,
                ((DamageInfo)this.damage.get(0)).base, 3, true);
        return;
    }
    // 34%: RIPPLE
    setMove((byte)3, AbstractMonster.Intent.DEFEND_DEBUFF);
    return;
}
```

**HEAD_SLAM**:
- **35% 확률** (45-79 범위)
- **직전 턴이 HEAD_SLAM이 아님** 조건
- 조건 불충족 시:
  - **66%**: REVERBERATE
  - **34%**: RIPPLE

---

### 패턴 선택 확률 (80 ≤ num)

```java
if (!lastMove((byte)3)) {
    // RIPPLE (Block 20 + Vulnerable + Weak + Frail)
    setMove((byte)3, AbstractMonster.Intent.DEFEND_DEBUFF);
    return;
}

// 직전이 RIPPLE이면 재추첨
getMove(AbstractDungeon.aiRng.random(74));
```

**RIPPLE**:
- **20% 확률** (80-99 범위)
- **직전 턴이 RIPPLE이 아님** 조건
- 조건 불충족 시 **0-74 범위에서 재추첨**

---

### 최종 확률 정리

| 패턴 | 확률 | 조건 |
|------|------|------|
| **HASTE** | 최우선 | HP < 50% & 미사용 |
| **REVERBERATE** | ~45% | 최근 2회 미사용 |
| **HEAD_SLAM** | ~35% | 직전 턴 미사용 |
| **RIPPLE** | ~20% | 직전 턴 미사용 |

**재추첨 로직**:
- REVERBERATE 연속 2회 → 50-99 재추첨
- HEAD_SLAM 연속 → 66% REVERBERATE, 34% RIPPLE
- RIPPLE 연속 → 0-74 재추첨

---

## 전투 전략

### 카드 카운터 관리

**핵심 전략**: **카드 사용 최소화**

**카운터 관리**:
- **12장 도달 시** TimeWarp 발동
- **Strength +2 영구 증가** (누적)
- **플레이어 턴 즉시 종료**

**권장 사항**:
- **고효율 카드** 사용 (1장으로 큰 효과)
- **비용 0 카드** 사용 자제 (카운터만 증가)
- **덱 사이클 속도 조절** (카드 무리하게 사용하지 않기)
- **파워 카드** 적절히 사용 (1장으로 지속 효과)

**위험 덱 타입**:
- **Shiv 덱**: 많은 비용 0 카드
- **Infinite 덱**: 무한 콤보로 많은 카드 사용
- **Exhaust 덱**: 빠른 덱 사이클

**유리한 덱 타입**:
- **Heavy Blade 덱**: 적은 카드로 큰 데미지
- **Limit Break 덱**: 파워 1-2장으로 승부
- **Static Discharge 덱**: 자동 데미지

---

### 패턴별 대응

**REVERBERATE 대응**:
- **21-24 데미지** (7-8 x 3)
- **다단 공격**이므로 Block 효율적
- Plated Armor 효과적

**RIPPLE 대응**:
- **Vulnerable + Weak + Frail (A19+)**
- **Orange Pellets**로 디버프 제거
- Artifact Potion 사전 사용
- 다음 턴 **방어 집중**

**HEAD_SLAM 대응**:
- **26-32 데미지** + **드로우 -1**
- **드로우 감소**로 다음 턴 핸드 부족
- **Slimed 2장 (A19+)** 추가
- 충분한 Block 확보

**HASTE 대응**:
- **HP 50% 이하 시 발동**
- **HP 50%로 회복** + **디버프 제거**
- **A19+: Block 32 추가**
- HP 50% 근처에서 **큰 데미지 집중**
- HASTE 이후 **체력 관리 재시작**

---

### 추천 카드

**고효율 공격** (카드 1장으로 큰 데미지):
- Heavy Blade
- Bludgeon
- Immolate
- Whirlwind (X코스트지만 1장)

**파워 카드** (지속 효과):
- Demon Form
- Barricade
- Static Discharge
- Creative AI

**디버프 제거**:
- Orange Pellets
- Artifact Potion

**Block 확보**:
- Impervious
- Entrench
- Body Slam

**다단 공격 대응**:
- Plated Armor
- Bronze Scales

---

### 위험 요소

**카드 카운터 (TimeWarp)**:
- **12장마다** 플레이어 턴 즉시 종료
- **Strength +2 영구 증가** (누적)
- 많은 카드 사용할수록 불리

**REVERBERATE**:
- 21-24 데미지 (7-8 x 3)
- 45% 확률 (높은 빈도)

**RIPPLE (A19+)**:
- Vulnerable + Weak + Frail 3종 디버프
- Block 20으로 장기전 유도

**HEAD_SLAM (A19+)**:
- 32 데미지 + 드로우 -1 + Slimed 2장
- 핸드 부족 유발

**HASTE**:
- HP 50%로 회복 (2번 체력바)
- 디버프 모두 제거
- A19+: Block 32 추가

---

## 수정 예시

### 1. 카드 카운터 임계값 감소 (A25+)

```java
@SpirePatch(
    clz = TimeWarpPower.class,
    method = "onAfterUseCard"
)
public static class TimeWarpThresholdPatch {
    @SpireInsertPatch(
        locator = ThresholdLocator.class
    )
    public static SpireReturn<Void> Insert(TimeWarpPower __instance) {
        if (AbstractDungeon.ascensionLevel >= 25) {
            __instance.amount++;

            // 12장 → 9장으로 감소
            if (__instance.amount == 9) {
                __instance.amount = 0;
                // TimeWarp 발동 로직
                __instance.playApplyPowerSfx();
                AbstractDungeon.actionManager.callEndTurnEarlySequence();
                // Strength +2 (또는 +3으로 증가)
                for (AbstractMonster m : AbstractDungeon.getMonsters().monsters) {
                    addToBot(new ApplyPowerAction(m, m,
                        new StrengthPower(m, 3), 3));
                }
                return SpireReturn.Return(null);
            }
            return SpireReturn.Return(null);
        }
        return SpireReturn.Continue();
    }

    private static class ThresholdLocator extends SpireInsertLocator {
        public int[] Locate(CtBehavior ctMethodToPatch) throws Exception {
            Matcher finalMatcher = new Matcher.FieldAccessMatcher(
                TimeWarpPower.class, "amount");
            return LineFinder.findInOrder(ctMethodToPatch, finalMatcher);
        }
    }
}
```

---

### 2. HASTE 회복량 증가

```java
@SpirePatch(
    clz = TimeEater.class,
    method = "takeTurn"
)
public static class HasteHealPatch {
    @SpireInsertPatch(
        locator = HasteLocator.class
    )
    public static void Insert(TimeEater __instance) {
        if (AbstractDungeon.ascensionLevel >= 25) {
            // HP 50% → 75%로 회복
            int extraHeal = __instance.maxHealth / 4;
            AbstractDungeon.actionManager.addToBottom(
                new HealAction(__instance, __instance, extraHeal)
            );
        }
    }
}
```

---

### 3. RIPPLE Block 증가

```java
@SpirePatch(
    clz = TimeEater.class,
    method = "takeTurn"
)
public static class RippleBlockPatch {
    @SpirePostfixPatch
    public static void Postfix(TimeEater __instance) {
        if (AbstractDungeon.ascensionLevel >= 25 &&
            __instance.nextMove == 3) {  // RIPPLE
            // Block 20 → 30
            AbstractDungeon.actionManager.addToBottom(
                new GainBlockAction(__instance, __instance, 10)
            );
        }
    }
}
```

---

### 4. TimeWarp Strength 증가량 조절

```java
@SpirePatch(
    clz = TimeWarpPower.class,
    method = "onAfterUseCard"
)
public static class TimeWarpStrengthPatch {
    @SpireInsertPatch(
        locator = StrengthLocator.class
    )
    public static void Insert(TimeWarpPower __instance) {
        if (AbstractDungeon.ascensionLevel >= 25) {
            // Strength +2 → +3
            for (AbstractMonster m : AbstractDungeon.getMonsters().monsters) {
                addToBot(new ApplyPowerAction(m, m,
                    new StrengthPower(m, 1), 1));  // 추가 +1
            }
        }
    }
}
```

---

## 중요 필드

| 필드명 | 타입 | 설명 |
|--------|------|------|
| `reverbDmg` | int | REVERBERATE 데미지 (7 또는 8) |
| `headSlamDmg` | int | HEAD_SLAM 데미지 (26 또는 32) |
| `usedHaste` | boolean | HASTE 사용 여부 (전투 중 1회) |
| `firstTurn` | boolean | 첫 턴 여부 (대사 출력용) |

---

## 관련 파일

- **본 파일**: `com/megacrit/cardcrawl/monsters/beyond/TimeEater.java`
- **파워**:
  - `com.megacrit.cardcrawl.powers.TimeWarpPower` (카드 카운터)
  - `com.megacrit.cardcrawl.powers.StrengthPower`
  - `com.megacrit.cardcrawl.powers.DrawReductionPower`
  - `com.megacrit.cardcrawl.powers.VulnerablePower`
  - `com.megacrit.cardcrawl.powers.WeakPower`
  - `com.megacrit.cardcrawl.powers.FrailPower`
- **액션**:
  - `DamageAction`
  - `GainBlockAction`
  - `ApplyPowerAction`
  - `HealAction`
  - `RemoveDebuffsAction`
  - `RemoveSpecificPowerAction`
  - `MakeTempCardInDiscardAction`
- **카드**:
  - `com.megacrit.cardcrawl.cards.status.Slimed`
- **VFX**:
  - `ShockWaveEffect`
  - `TimeWarpTurnEndEffect`
  - `BorderFlashEffect`

---

## 참고사항

1. **카드 카운터 시스템**: TimeWarpPower로 모든 카드 사용 추적
2. **TimeWarp 발동**: 12장마다 턴 종료 + Strength +2
3. **HASTE 메커니즘**: HP 50% 이하 시 1회 발동, HP 50%로 회복
4. **RIPPLE (A19+)**: Vulnerable + Weak + Frail 3종 디버프
5. **HEAD_SLAM (A19+)**: 드로우 -1 + Slimed 2장
6. **연속 패턴 방지**: REVERBERATE, HEAD_SLAM, RIPPLE 모두 연속 방지 로직
7. **카드 효율 중요**: 적은 카드로 큰 효과내는 덱이 유리
8. **Infinite 덱 불리**: 많은 카드 사용으로 TimeWarp 빈번 발동
9. **파워 카드 추천**: 지속 효과로 카드 사용 최소화
10. **A19+ 강화**: RIPPLE Frail 추가, HEAD_SLAM Slimed 2장, HASTE Block 32
