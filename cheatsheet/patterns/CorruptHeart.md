# 타락한 심장 (Corrupt Heart)

## 기본 정보

**클래스명**: `CorruptHeart`
**전체 경로**: `com.megacrit.cardcrawl.monsters.ending.CorruptHeart`
**ID**: `"CorruptHeart"`
**타입**: 보스 (BOSS)
**등장 지역**: 4막 (The Ending) - 게임의 최종 보스

---

## HP 정보

| 난이도 | HP |
|--------|-----|
| 기본 (A0-A8) | 750 |
| A9+ | 800 |

**특징**:
- 게임에서 가장 높은 HP를 가진 보스
- 단일 페이즈 (부활 메커니즘 없음)
- Invincible 파워로 시작 (200-300 HP 방어)

---

## 생성자 정보

```java
public CorruptHeart() {
    super(NAME, "CorruptHeart", 750, 30.0F, -30.0F, 476.0F, 410.0F, null, -50.0F, 30.0F);

    // 애니메이션 초기화
    loadAnimation("images/npcs/heart/skeleton.atlas",
                  "images/npcs/heart/skeleton.json", 1.0F);
    AnimationState.TrackEntry e = state.setAnimation(0, "idle", true);
    e.setTimeScale(1.5F);
    state.addListener(animListener);

    // HP 설정
    if (AbstractDungeon.ascensionLevel >= 9) {
        setHp(800);
    } else {
        setHp(750);
    }

    // 데미지 정보 초기화 (2개)
    if (AbstractDungeon.ascensionLevel >= 4) {
        damage.add(new DamageInfo(this, 45));  // [0] Echo Attack
        damage.add(new DamageInfo(this, 2));   // [1] Blood Shots (각 타)
        bloodHitCount = 15;  // Blood Shots 타수
    } else {
        damage.add(new DamageInfo(this, 40));  // [0] Echo Attack
        damage.add(new DamageInfo(this, 2));   // [1] Blood Shots (각 타)
        bloodHitCount = 12;  // Blood Shots 타수
    }
}
```

---

## Beat of Death 메커니즘 (핵심 특수 메커니즘)

### 개념
**Beat of Death**: 플레이어가 **카드를 사용할 때마다** 현재 HP의 일정량을 데미지로 받는 메커니즘

### usePreBattleAction 코드

```java
public void usePreBattleAction() {
    // BGM 변경
    CardCrawlGame.music.unsilenceBGM();
    AbstractDungeon.scene.fadeOutAmbiance();
    AbstractDungeon.getCurrRoom().playBgmInstantly("BOSS_ENDING");

    // Invincible 파워 부여
    int invincibleAmt = 300;
    if (AbstractDungeon.ascensionLevel >= 19) {
        invincibleAmt -= 100;  // A19+: 200
    }
    ApplyPowerAction(this, this, new InvinciblePower(this, invincibleAmt));

    // Beat of Death 파워 부여
    int beatAmount = 1;
    if (AbstractDungeon.ascensionLevel >= 19) {
        beatAmount++;  // A19+: 2
    }
    ApplyPowerAction(this, this, new BeatOfDeathPower(this, beatAmount));
}
```

### Beat of Death 파워 동작

**BeatOfDeathPower 클래스**:
```java
public void onAfterUseCard(AbstractCard card, UseCardAction action) {
    flash();
    addToBot(new DamageAction(AbstractDungeon.player,
        new DamageInfo(this.owner, this.amount, DamageType.THORNS),
        AttackEffect.BLUNT_LIGHT));
    updateDescription();
}
```

**효과**:
- 플레이어가 카드를 사용할 때마다 발동
- 데미지 타입: `THORNS` (가시 데미지)
- **현재 HP의 일정량**이 아닌 **고정 데미지** (amount)
- A0-A18: 카드당 1 데미지
- A19+: 카드당 2 데미지

**중요**:
- Block으로 방어 가능
- 버퍼 효과 무시
- 카드 사용 최소화 필요
- 턴당 10장 = 10-20 데미지

---

## 시작 파워

### Invincible (무적)

**InvinciblePower 클래스**:
```java
public int onAttackedToChangeDamage(DamageInfo info, int damageAmount) {
    if (damageAmount > this.amount) {
        damageAmount = this.amount;
    }
    this.amount -= damageAmount;
    if (this.amount < 0) {
        this.amount = 0;
    }
    updateDescription();
    return damageAmount;
}

public void atStartOfTurn() {
    this.amount = this.maxAmt;  // 매 턴 시작 시 완전 회복
    updateDescription();
}
```

| 난이도 | Invincible 양 |
|--------|---------------|
| A0-A18 | 300 |
| A19+ | 200 |

**메커니즘**:
- 받는 데미지를 최대 Invincible 양으로 제한
- 데미지를 받으면 Invincible 양 감소
- **매 턴 시작 시 완전 회복** (300 또는 200으로 리셋)
- 0이 되어도 파워는 유지됨 (매 턴 재생)

**전략적 중요성**:
- 1타에 300+ 데미지 불가능 (A19+: 200+)
- 다중 공격이 유리 (각 타마다 Invincible 소모)
- Poison, Burn 같은 지속 데미지 매우 효과적
- 매 턴 초기화되므로 지속 데미지가 필수

### Beat of Death (죽음의 박동)

| 난이도 | Beat 데미지 |
|--------|-------------|
| A0-A18 | 1 |
| A19+ | 2 |

**메커니즘**:
- 카드 사용 시마다 발동
- THORNS 타입 데미지 (가시 데미지)
- Block으로 방어 가능
- 카드 사용 최소화 필요

---

## 패턴 정보

### 패턴 순환 시스템

```java
private int moveCount = 0;  // 패턴 카운터
private int buffCount = 0;  // 버프 카운터
private boolean isFirstMove = true;  // 첫 턴 플래그

protected void getMove(int num) {
    // 첫 턴: 무조건 Debilitate
    if (isFirstMove) {
        setMove((byte)3, Intent.STRONG_DEBUFF);
        isFirstMove = false;
        return;
    }

    // 이후: 3턴 순환 (moveCount % 3)
    switch (moveCount % 3) {
        case 0:  // 0, 3, 6, 9, ...
            if (AbstractDungeon.aiRng.randomBoolean()) {
                // 50%: Blood Shots
                setMove((byte)1, Intent.ATTACK, damage.get(1).base, bloodHitCount, true);
            } else {
                // 50%: Echo Attack
                setMove((byte)2, Intent.ATTACK, damage.get(0).base);
            }
            break;

        case 1:  // 1, 4, 7, 10, ...
            if (!lastMove((byte)2)) {
                // Echo Attack (직전에 사용 안했으면)
                setMove((byte)2, Intent.ATTACK, damage.get(0).base);
            } else {
                // Blood Shots (직전에 Echo Attack 사용했으면)
                setMove((byte)1, Intent.ATTACK, damage.get(1).base, bloodHitCount, true);
            }
            break;

        default:  // 2, 5, 8, 11, ...
            // Buff 패턴
            setMove((byte)4, Intent.BUFF);
            break;
    }

    moveCount++;
}
```

**패턴 순환 요약**:
1. **첫 턴 (Turn 1)**: Debilitate (강제)
2. **Turn 2**: Blood Shots (50%) 또는 Echo Attack (50%)
3. **Turn 3**: Echo Attack 또는 Blood Shots (직전 패턴 고려)
4. **Turn 4**: Buff
5. **Turn 5**: 랜덤 공격
6. **Turn 6**: 랜덤 공격
7. **Turn 7**: Buff
8. (반복)

---

## 전체 패턴 상세

### 패턴 1: Debilitate (쇠약화)

**의도**: `STRONG_DEBUFF`
**바이트 코드**: `3`
**첫 턴**: 필수 사용 (Turn 1)

**효과**:
1. **Vulnerable 2턴** (취약)
2. **Weak 2턴** (약화)
3. **Frail 2턴** (연약)
4. **Dazed ×1** (드로우 파일에 추가)
5. **Slimed ×1** (드로우 파일에 추가)
6. **Wound ×1** (드로우 파일에 추가)
7. **Burn ×1** (드로우 파일에 추가)
8. **Void ×1** (드로우 파일에 추가)

**takeTurn 코드**:
```java
case 3:  // DEBILITATE
    // 시각 효과
    AbstractDungeon.actionManager.addToBottom(
        new VFXAction(new HeartMegaDebuffEffect())
    );

    // 디버프 3종
    AbstractDungeon.actionManager.addToBottom(
        new ApplyPowerAction(AbstractDungeon.player, this,
            new VulnerablePower(AbstractDungeon.player, 2, true), 2)
    );
    AbstractDungeon.actionManager.addToBottom(
        new ApplyPowerAction(AbstractDungeon.player, this,
            new WeakPower(AbstractDungeon.player, 2, true), 2)
    );
    AbstractDungeon.actionManager.addToBottom(
        new ApplyPowerAction(AbstractDungeon.player, this,
            new FrailPower(AbstractDungeon.player, 2, true), 2)
    );

    // 상태이상 카드 5장 추가 (드로우 파일에)
    AbstractDungeon.actionManager.addToBottom(
        new MakeTempCardInDrawPileAction(new Dazed(), 1, true, false, false,
            Settings.WIDTH * 0.2F, Settings.HEIGHT / 2.0F)
    );
    AbstractDungeon.actionManager.addToBottom(
        new MakeTempCardInDrawPileAction(new Slimed(), 1, true, false, false,
            Settings.WIDTH * 0.35F, Settings.HEIGHT / 2.0F)
    );
    AbstractDungeon.actionManager.addToBottom(
        new MakeTempCardInDrawPileAction(new Wound(), 1, true, false, false,
            Settings.WIDTH * 0.5F, Settings.HEIGHT / 2.0F)
    );
    AbstractDungeon.actionManager.addToBottom(
        new MakeTempCardInDrawPileAction(new Burn(), 1, true, false, false,
            Settings.WIDTH * 0.65F, Settings.HEIGHT / 2.0F)
    );
    AbstractDungeon.actionManager.addToBottom(
        new MakeTempCardInDrawPileAction(new VoidCard(), 1, true, false, false,
            Settings.WIDTH * 0.8F, Settings.HEIGHT / 2.0F)
    );
    break;
```

**전략적 중요성**:
- 게임에서 가장 강력한 디버프 패턴
- 3종 디버프 + 5장 상태이상 카드
- Artifact 파워로 완전 무효화 가능
- Orange Pellets로 디버프 제거 가능
- 상태이상 카드는 Exhaust/Remove로 처리

---

### 패턴 2: Blood Shots (피의 일격)

**의도**: `ATTACK`
**바이트 코드**: `1`

**데미지**:
| 난이도 | 각 타 데미지 | 타수 | 총 데미지 |
|--------|-------------|------|-----------|
| A0-A3 | 2 | 12 | 24 |
| A4+ | 2 | 15 | 30 |

**효과**:
- 플레이어에게 **2 데미지 × 12타** (A4+: 15타)
- 다중 공격 (Intangible, Blur 효과적)
- 시각 효과: `BloodShotEffect`
- AttackEffect: `BLUNT_HEAVY` (각 타마다)

**takeTurn 코드**:
```java
case 1:  // BLOOD_SHOTS
    // 시각 효과 (속도 모드에 따라)
    if (Settings.FAST_MODE) {
        AbstractDungeon.actionManager.addToBottom(
            new VFXAction(new BloodShotEffect(hb.cX, hb.cY,
                AbstractDungeon.player.hb.cX, AbstractDungeon.player.hb.cY,
                bloodHitCount), 0.25F)
        );
    } else {
        AbstractDungeon.actionManager.addToBottom(
            new VFXAction(new BloodShotEffect(hb.cX, hb.cY,
                AbstractDungeon.player.hb.cX, AbstractDungeon.player.hb.cY,
                bloodHitCount), 0.6F)
        );
    }

    // 다중 공격
    for (int i = 0; i < bloodHitCount; i++) {
        AbstractDungeon.actionManager.addToBottom(
            new DamageAction(AbstractDungeon.player, damage.get(1),
                AttackEffect.BLUNT_HEAVY, true)
        );
    }
    break;
```

**전략**:
- Intangible 효과 (1 데미지만 받음)
- Blur, Wraith Form 효과적
- Thorns, Bronze Scales 반격 효과적
- Orichalcum (Block 6) 있으면 피해 없음

---

### 패턴 3: Echo Attack (메아리 공격)

**의도**: `ATTACK`
**바이트 코드**: `2`

**데미지**:
| 난이도 | 데미지 |
|--------|--------|
| A0-A3 | 40 |
| A4+ | 45 |

**효과**:
- 플레이어에게 **40 데미지** (A4+: 45) (단일 강력 공격)
- 시각 효과: `ViceCrushEffect` (압축 효과)
- AttackEffect: `BLUNT_HEAVY`
- Vulnerable 상태면 60+ 데미지

**takeTurn 코드**:
```java
case 2:  // ECHO_ATTACK
    AbstractDungeon.actionManager.addToBottom(
        new VFXAction(new ViceCrushEffect(
            AbstractDungeon.player.hb.cX,
            AbstractDungeon.player.hb.cY), 0.5F)
    );

    AbstractDungeon.actionManager.addToBottom(
        new DamageAction(AbstractDungeon.player, damage.get(0),
            AttackEffect.BLUNT_HEAVY)
    );
    break;
```

**전략**:
- 충분한 Block 확보 필수 (40-50)
- Vulnerable 상태 주의 (60+ 데미지)
- Intangible 효과적
- Buffer 효과 (1 데미지로 감소)

---

### 패턴 4: Buff (강화)

**의도**: `BUFF`
**바이트 코드**: `4`
**주기**: 매 3턴마다 (Turn 4, 7, 10, 13, ...)

**효과**: buffCount에 따라 다름

#### Buff 0: Artifact ×2 + Strength 회복

```java
case 4:  // BUFF
    // Strength 회복 계산
    int additionalAmount = 0;
    if (hasPower("Strength") && getPower("Strength").amount < 0) {
        additionalAmount = -getPower("Strength").amount;
    }

    // 시각 효과
    AbstractDungeon.actionManager.addToBottom(
        new VFXAction(new BorderFlashEffect(new Color(0.8F, 0.5F, 1.0F, 1.0F)))
    );
    AbstractDungeon.actionManager.addToBottom(
        new VFXAction(new HeartBuffEffect(hb.cX, hb.cY))
    );

    // Strength +2 (+ 음수 보정)
    AbstractDungeon.actionManager.addToBottom(
        new ApplyPowerAction(this, this,
            new StrengthPower(this, additionalAmount + 2),
            additionalAmount + 2)
    );

    // buffCount에 따른 추가 효과
    switch (buffCount) {
        case 0:
            // Artifact ×2
            AbstractDungeon.actionManager.addToBottom(
                new ApplyPowerAction(this, this,
                    new ArtifactPower(this, 2), 2)
            );
            break;
        // ...
    }

    buffCount++;
    break;
```

**효과**:
1. **Strength 음수 보정** (음수면 0으로 만듦)
2. **Strength +2**
3. **Artifact ×2** (디버프 2회 무효)

**중요**:
- Disarm 등 Strength 감소 무효화
- 디버프 2회 무효 (Vulnerable, Weak 등)
- Artifact는 다음 버프까지 유지

#### Buff 1: Beat of Death +1

```java
case 1:
    // Beat of Death +1
    AbstractDungeon.actionManager.addToBottom(
        new ApplyPowerAction(this, this,
            new BeatOfDeathPower(this, 1), 1)
    );
    break;
```

**효과**:
1. **Strength 음수 보정 + Strength +2** (공통)
2. **Beat of Death +1** (카드당 데미지 증가)

| 기존 Beat | 버프 후 |
|-----------|---------|
| 1 (A0-A18) | 2 |
| 2 (A19+) | 3 |

**전략적 중요성**:
- 카드 사용 시 데미지 2배
- A19+에서 3 데미지로 증가
- 카드 사용 극도로 제한 필요

#### Buff 2: Painful Stabs (고통스러운 찔림)

```java
case 2:
    // Painful Stabs
    AbstractDungeon.actionManager.addToBottom(
        new ApplyPowerAction(this, this,
            new PainfulStabsPower(this))
    );
    break;
```

**PainfulStabsPower 클래스**:
```java
public void onInflictDamage(DamageInfo info, int damageAmount, AbstractCreature target) {
    if (damageAmount > 0 && info.type != DamageType.THORNS) {
        addToBot(new MakeTempCardInDiscardAction(new Wound(), 1));
    }
}
```

**효과**:
1. **Strength 음수 보정 + Strength +2** (공통)
2. **Painful Stabs 파워** (공격 시마다 Wound 추가)

**메커니즘**:
- CorruptHeart가 데미지를 줄 때마다 발동
- 플레이어의 버리기 더미에 **Wound 카드 1장 추가**
- THORNS 타입 데미지는 제외
- Blood Shots (15타) = Wound 15장

**전략적 중요성**:
- 버리기 더미 오염 심각
- Medkit (Exhaust Status) 필수
- Evolve로 활용 가능

#### Buff 3: Strength +10

```java
case 3:
    // Strength +10
    AbstractDungeon.actionManager.addToBottom(
        new ApplyPowerAction(this, this,
            new StrengthPower(this, 10), 10)
    );
    break;
```

**효과**:
1. **Strength 음수 보정 + Strength +2** (공통)
2. **Strength +10**

**데미지 변화**:
| 패턴 | 기본 | Buff 3 후 |
|------|------|-----------|
| Echo Attack | 45 | 55 |
| Blood Shots (15타) | 30 | 60 |

**전략**:
- 빠른 전투 종료 필요
- Disarm 효과 무효 (다음 Buff에서 회복)

#### Buff 4+: Strength +50

```java
default:
    // Strength +50
    AbstractDungeon.actionManager.addToBottom(
        new ApplyPowerAction(this, this,
            new StrengthPower(this, 50), 50)
    );
    break;
```

**효과**:
1. **Strength 음수 보정 + Strength +2** (공통)
2. **Strength +50**

**데미지 변화**:
| 패턴 | 기본 | Buff 4 후 |
|------|------|-----------|
| Echo Attack | 45 | 95 |
| Blood Shots (15타) | 30 | 150 |

**전략**:
- 사실상 게임 종료 신호
- Turn 16까지 가면 매우 위험
- 빠른 승부 필수

---

## Buff 패턴 타임라인

| 턴 | 패턴 | Buff 종류 | 누적 효과 |
|----|------|-----------|-----------|
| 1 | Debilitate | - | - |
| 2 | 공격 | - | - |
| 3 | 공격 | - | - |
| 4 | **Buff 0** | Artifact ×2 + Str +2 | Artifact 2, Str +2 |
| 5 | 공격 | - | - |
| 6 | 공격 | - | - |
| 7 | **Buff 1** | Beat +1 + Str +2 | Artifact 2?, Beat 2(or 3), Str +4 |
| 8 | 공격 | - | - |
| 9 | 공격 | - | - |
| 10 | **Buff 2** | Painful Stabs + Str +2 | Painful Stabs, Str +6 |
| 11 | 공격 | - | - |
| 12 | 공격 | - | - |
| 13 | **Buff 3** | Str +10 + Str +2 | Painful Stabs, Str +18 |
| 14 | 공격 | - | Echo 45→63 |
| 15 | 공격 | - | Blood 30→48 |
| 16 | **Buff 4** | Str +50 + Str +2 | Painful Stabs, Str +70 |
| 17+ | 공격 | - | 거의 불가능 |

---

## 특수 동작

### die() 메서드

```java
@Override
public void die() {
    if (!(AbstractDungeon.getCurrRoom()).cannotLose) {
        super.die();
        state.removeListener(animListener);
        onBossVictoryLogic();
        onFinalBossVictoryLogic();  // 최종 보스 승리 처리
        CardCrawlGame.stopClock = true;  // 시계 정지
    }
}
```

**효과**:
- 최종 보스 승리 처리
- 게임 시계 정지 (타이머 중지)
- 애니메이션 리스너 제거
- 승리 화면 전환

---

## AI 로직 상세

### 첫 턴 강제 패턴

```java
if (isFirstMove) {
    setMove((byte)3, Intent.STRONG_DEBUFF);  // Debilitate
    isFirstMove = false;
    return;
}
```

- Turn 1은 항상 Debilitate
- 디버프 3종 + 상태이상 5장
- Artifact 준비 필수

### 3턴 순환 시스템

**moveCount % 3**:
- **0, 3, 6, 9, ...**: 50% Blood Shots, 50% Echo Attack
- **1, 4, 7, 10, ...**: Echo Attack 우선 (직전 패턴 고려)
- **2, 5, 8, 11, ...**: Buff 패턴

**패턴 예측**:
```
Turn 1: Debilitate (강제)
Turn 2: Blood Shots (50%) or Echo (50%)
Turn 3: Echo (우선) or Blood (직전 Echo면)
Turn 4: Buff 0 (Artifact)
Turn 5: Blood (50%) or Echo (50%)
Turn 6: Echo (우선) or Blood (직전 Echo면)
Turn 7: Buff 1 (Beat +1)
Turn 8: Blood (50%) or Echo (50%)
...
```

---

## 전투 전략

### Phase 1: 초반 (Turn 1-6)

**목표**: 생존 + 데미지 축적

**Turn 1 대응** (Debilitate):
- **Artifact 사용** (3회 추천)
- Orange Pellets 준비
- 상태이상 카드 제거 계획
- Block 40+ 확보 (다음 턴 대비)

**Turn 2-6**:
- Invincible 깎기 (다중 공격 유리)
- Beat of Death 최소화 (카드 사용 제한)
- Poison/Burn 지속 데미지 설치
- Block 관리 (40+ 유지)

### Phase 2: 중반 (Turn 7-12)

**위험 요소**:
- **Turn 7 Buff**: Beat of Death +1 (카드당 2 데미지)
- **Turn 10 Buff**: Painful Stabs (공격 시 Wound 추가)

**전략**:
- 카드 사용 극도로 제한 (Beat 2)
- 지속 데미지 의존 (Poison, Burn)
- Wound 카드 관리 (Medkit 필수)
- 높은 Block 유지

### Phase 3: 후반 (Turn 13+)

**위험 요소**:
- **Turn 13 Buff**: Strength +12 (Echo 57, Blood 42)
- **Turn 16 Buff**: Strength +52 (Echo 97, Blood 82)

**전략**:
- Turn 13 전에 처치 목표
- 불가피하면 Buffer/Intangible 사용
- 1-2턴 내 종료 필요

---

## 난이도별 차이

### A0-A3 (기본)

| 요소 | 값 |
|------|-----|
| HP | 750 |
| Invincible | 300 |
| Beat of Death | 1 |
| Echo Attack | 40 |
| Blood Shots | 2×12 = 24 |

### A4-A8

| 요소 | 변화 |
|------|------|
| Echo Attack | 40 → 45 |
| Blood Shots | 2×12 → 2×15 |

### A9-A18

| 요소 | 변화 |
|------|------|
| HP | 750 → 800 |

### A19+

| 요소 | 변화 |
|------|------|
| Invincible | 300 → 200 |
| Beat of Death | 1 → 2 |

**A19+ 전략적 차이**:
- **Invincible 감소**: 200 (좋음)
- **Beat of Death 증가**: 카드당 2 데미지 (매우 나쁨)
- Turn 7 Buff 후 카드당 3 데미지
- 카드 사용 극도로 제한 필요
- 지속 데미지 덱 강제

---

## 추천 덱 구성

### 필수 요소

1. **Artifact 파워/카드**:
   - Artifact Potion ×3
   - Orange Pellets
   - Panacea
   - Clockwork Souvenir

2. **지속 데미지**:
   - Noxious Fumes (Invincible 극복)
   - Catalyst (Poison 증폭)
   - Envenom (공격마다 Poison)
   - Rupture + Self-Damage (지속 Strength)

3. **상태이상 제거**:
   - Medkit (Exhaust Status)
   - Dark Shackles (Exhaust)
   - 자연 Exhaust 카드

4. **효율적 Block**:
   - Blur (유지형 Block)
   - Wraith Form (Intangible)
   - Footwork (Dexterity)
   - Barricade (Block 유지)

### 캐릭터별 전략

#### Ironclad
- **Rupture + Self-Damage**: 카드 사용 최소화
- **Bludgeon**: 단일 고데미지
- **Sentinel**: Block 확보
- **Limit Break**: Strength 증폭

#### Silent
- **Noxious Fumes + Catalyst**: 지속 데미지
- **Wraith Form**: Intangible
- **Footwork**: Block 증폭
- **Blur**: Block 유지

#### Defect
- **Creative AI**: 지속 카드 생성
- **Capacitor + Defragment**: Orb 증폭
- **Glacier + Blizzard**: 고효율 Block
- **Echo Form**: 카드 효율 2배

#### Watcher
- **Establishment + Talk to the Hand**: 카드 코스트 감소 + 회복
- **Mental Fortress + Stance Dance**: 자동 Block
- **Vault + Halt**: 고효율 Block
- **Ragnarok**: 단일 카드 고데미지

---

## 유물 추천

### 필수 유물

| 유물 | 효과 | 중요도 |
|------|------|--------|
| **Clockwork Souvenir** | 매 턴 Artifact 1 | ★★★★★ |
| **Incense Burner** | 6턴마다 Intangible 1 | ★★★★★ |
| **Tungsten Rod** | 피해 -1 (Beat 무효) | ★★★★★ |
| **Fairy in a Bottle** | 사망 시 부활 | ★★★★☆ |
| **Lizard Tail** | 사망 시 부활 | ★★★★☆ |

### 유용 유물

| 유물 | 효과 | 중요도 |
|------|------|--------|
| **Orichalcum** | 턴 종료 시 Block 6 | ★★★★☆ |
| **Bronze Scales** | 공격 받으면 가시 3 | ★★★★☆ |
| **Torii** | 5 이하 피해 1로 감소 | ★★★★☆ |
| **Thread and Needle** | Dexterity 4 (전투 시작 시) | ★★★☆☆ |
| **Runic Cube** | 카드 잃을 때 에너지 | ★★★☆☆ |

---

## 물약 추천

| 물약 | 효과 | 사용 시점 |
|------|------|----------|
| **Artifact Potion** | Artifact 1 | Turn 1 (Debilitate) |
| **Intangible Potion** | Intangible 1턴 | Turn 13+ (고데미지) |
| **Duplication Potion** | 다음 카드 2회 | Catalyst, Wraith Form |
| **Fear Potion** | Vulnerable 3 | 고데미지 턴 |
| **Fairy in a Bottle** | 사망 시 부활 | 예비 |

---

## 수정 예시

### 1. HP 증가 (A25+)

```java
@SpirePatch(
    clz = CorruptHeart.class,
    method = SpirePatch.CONSTRUCTOR
)
public static class CorruptHeartHPPatch {
    @SpirePostfixPatch
    public static void Postfix(CorruptHeart __instance) {
        if (AbstractDungeon.ascensionLevel >= 25) {
            // HP 800 → 1000
            __instance.setHp(1000);
        }
    }
}
```

### 2. Invincible 매 턴 증가 (A25+)

```java
@SpirePatch(
    clz = CorruptHeart.class,
    method = "usePreBattleAction"
)
public static class CorruptHeartInvinciblePatch {
    @SpirePostfixPatch
    public static void Postfix(CorruptHeart __instance) {
        if (AbstractDungeon.ascensionLevel >= 25) {
            // Invincible 200 → 250
            int invincibleAmt = 250;
            if (AbstractDungeon.ascensionLevel >= 19) {
                invincibleAmt = 250;  // A25: 250
            }
        }
    }
}
```

### 3. Beat of Death 증가 (A25+)

```java
@SpirePatch(
    clz = CorruptHeart.class,
    method = "usePreBattleAction"
)
public static class CorruptHeartBeatPatch {
    @SpirePostfixPatch
    public static void Postfix(CorruptHeart __instance) {
        if (AbstractDungeon.ascensionLevel >= 25) {
            // Beat of Death +1 추가 (A19: 2 → 3)
            AbstractDungeon.actionManager.addToBottom(
                new ApplyPowerAction(__instance, __instance,
                    new BeatOfDeathPower(__instance, 1), 1)
            );
        }
    }
}
```

### 4. Buff 주기 단축 (A25+)

```java
@SpirePatch(
    clz = CorruptHeart.class,
    method = "getMove"
)
public static class CorruptHeartBuffPatch {
    @SpirePrefixPatch
    public static void Prefix(CorruptHeart __instance, int num) {
        if (AbstractDungeon.ascensionLevel >= 25) {
            // Buff 주기 3턴 → 2턴
            // moveCount % 2 == 0 → Buff
        }
    }
}
```

---

## 중요 필드

| 필드명 | 타입 | 설명 |
|--------|------|------|
| `isFirstMove` | boolean | 첫 턴 여부 (true면 Debilitate 강제) |
| `moveCount` | int | 패턴 카운터 (3턴 순환, % 3) |
| `buffCount` | int | Buff 카운터 (어떤 Buff를 사용할지 결정) |
| `bloodHitCount` | int | Blood Shots 타수 (A0-A3: 12, A4+: 15) |
| `animListener` | HeartAnimListener | 애니메이션 리스너 (die 시 제거) |

---

## 관련 파일

- **본 파일**: `com/megacrit/cardcrawl/monsters/ending/CorruptHeart.java`
- **파워**:
  - `com.megacrit.cardcrawl.powers.InvinciblePower` (무적, 매 턴 회복)
  - `com.megacrit.cardcrawl.powers.BeatOfDeathPower` (카드 사용 시 데미지)
  - `com.megacrit.cardcrawl.powers.PainfulStabsPower` (공격 시 Wound 추가)
  - `com.megacrit.cardcrawl.powers.ArtifactPower` (디버프 무효)
  - `com.megacrit.cardcrawl.powers.StrengthPower` (힘)
  - `com.megacrit.cardcrawl.powers.VulnerablePower` (취약)
  - `com.megacrit.cardcrawl.powers.WeakPower` (약화)
  - `com.megacrit.cardcrawl.powers.FrailPower` (연약)
- **카드**:
  - `com.megacrit.cardcrawl.cards.status.Dazed` (멍함, 사용 불가)
  - `com.megacrit.cardcrawl.cards.status.Slimed` (점액)
  - `com.megacrit.cardcrawl.cards.status.Wound` (상처, 사용 불가)
  - `com.megacrit.cardcrawl.cards.status.Burn` (화상)
  - `com.megacrit.cardcrawl.cards.status.VoidCard` (공허, Ethereal)
- **미니언** (SpireShield, SpireSpear는 CorruptHeart와 별개로 등장):
  - `com.megacrit.cardcrawl.monsters.ending.SpireShield` (방패 미니언)
  - `com.megacrit.cardcrawl.monsters.ending.SpireSpear` (창 미니언)
- **액션**:
  - `DamageAction` (데미지)
  - `ApplyPowerAction` (파워 부여)
  - `MakeTempCardInDrawPileAction` (드로우 파일에 카드 추가)
- **VFX**:
  - `HeartMegaDebuffEffect` (디버프 효과)
  - `HeartBuffEffect` (버프 효과)
  - `BloodShotEffect` (피의 일격 효과)
  - `ViceCrushEffect` (압축 효과)
  - `BorderFlashEffect` (테두리 번쩍임)
- **애니메이션**:
  - `images/npcs/heart/skeleton.atlas`
  - `images/npcs/heart/skeleton.json`

---

## 참고사항

1. **게임의 최종 보스**: 4막 (The Ending) 유일 보스
2. **Invincible 메커니즘**: 매 턴 시작 시 완전 회복 (300 또는 200)
3. **Beat of Death**: 카드 사용마다 1-2 데미지 (카드 사용 최소화 필요)
4. **첫 턴 Debilitate**: 3종 디버프 + 5장 상태이상 (Artifact 필수)
5. **3턴 순환 패턴**: 공격 2턴 → Buff 1턴 반복
6. **Buff 누적**: Artifact → Beat +1 → Painful Stabs → Str +10 → Str +50
7. **Painful Stabs**: 공격마다 Wound 추가 (Blood Shots 15타 = Wound 15장)
8. **A19+ 차이**: Invincible 200 (좋음), Beat 2 (매우 나쁨)
9. **추천 턴 수**: Turn 10-13 안에 처치 (Buff 3 전)
10. **지속 데미지 필수**: Poison, Burn으로 Invincible 극복
11. **Artifact 필수**: Clockwork Souvenir 또는 Artifact Potion ×3
12. **상태이상 관리**: Medkit으로 Status 카드 Exhaust
13. **미니언 없음**: CorruptHeart 단독 전투 (SpireShield/SpireSpear는 다른 전투)
14. **die() 특수 처리**: onFinalBossVictoryLogic() 호출, 게임 시계 정지
15. **HeartAnimListener**: 애니메이션 이벤트 리스너 (die 시 제거 필요)
