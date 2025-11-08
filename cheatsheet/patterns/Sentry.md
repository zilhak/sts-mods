# 센트리 (Sentry)

## 기본 정보

**클래스명**: `Sentry`
**전체 경로**: `com.megacrit.cardcrawl.monsters.exordium.Sentry`
**ID**: `"Sentry"`
**엔카운터 이름**: `"Sentries"` (복수형 - 항상 3마리 등장)
**타입**: 엘리트 (ELITE)
**등장 지역**: 1막 (Exordium)

---

## HP 정보

| 난이도 | HP 범위 |
|--------|---------|
| 기본 (A0-A7) | 38-42 |
| A8+ | 39-45 |

**코드 위치**: 32-34줄, 47-51줄

```java
private static final int HP_MIN = 38;
private static final int HP_MAX = 42;
private static final int A_2_HP_MIN = 39;
private static final int A_2_HP_MAX = 45;

if (AbstractDungeon.ascensionLevel >= 8) {
    setHp(39, 45);
} else {
    setHp(38, 42);
}
```

**중요**: 센트리는 항상 **3마리**가 동시에 등장하므로 총 HP는 약 120-135 정도

---

## 생성자 정보

### 주요 생성자
```java
public Sentry(float x, float y)
```

**파라미터**:
- `x`: X 좌표
- `y`: Y 좌표

**특징**:
- 단일 생성자만 존재
- 복잡한 파라미터 없음
- 항상 동일한 방식으로 생성

**코드 위치**: 43-77줄

```java
public Sentry(float x, float y) {
    super(NAME, "Sentry", 42, 0.0F, -5.0F, 180.0F, 310.0F, null, x, y);
    this.type = AbstractMonster.EnemyType.ELITE;

    // HP 설정
    if (AbstractDungeon.ascensionLevel >= 8) {
        setHp(39, 45);
    } else {
        setHp(38, 42);
    }

    // 데미지 설정
    if (AbstractDungeon.ascensionLevel >= 3) {
        this.beamDmg = 10;
    } else {
        this.beamDmg = 9;
    }

    // Dazed 카드 수 설정
    if (AbstractDungeon.ascensionLevel >= 18) {
        this.dazedAmt = 3;
    } else {
        this.dazedAmt = 2;
    }

    this.damage.add(new DamageInfo((AbstractCreature)this, this.beamDmg));

    // 애니메이션 로드
    loadAnimation("images/monsters/theBottom/sentry/skeleton.atlas",
                  "images/monsters/theBottom/sentry/skeleton.json", 1.0F);

    AnimationState.TrackEntry e = this.state.setAnimation(0, "idle", true);
    e.setTimeScale(2.0F);
    e.setTime(e.getEndTime() * MathUtils.random());

    // 애니메이션 전환 설정
    this.stateData.setMix("idle", "attack", 0.1F);
    this.stateData.setMix("idle", "spaz1", 0.1F);
    this.stateData.setMix("idle", "hit", 0.1F);
}
```

---

## 패턴 정보

### 패턴 1: 볼트 (Bolt)

**바이트 값**: `3`
**의도**: `DEBUFF`
**발동 조건**: 첫 턴에 짝수 인덱스 센트리가 사용 또는 AI 로직에 따름

**효과**: 플레이어 버리기 더미에 **Dazed 카드** 추가

**Dazed 카드 수**:
| 난이도 | Dazed 수 |
|--------|----------|
| 기본 (A0-A17) | 2장 |
| A18+ | 3장 |

**코드 위치**: 35줄, 39-40줄, 59-63줄, 87-105줄

```java
// 상수
private static final byte BOLT = 3;
private static final int DAZED_AMT = 2;
private static final int A_18_DAZED_AMT = 3;

// 생성자에서 설정
if (AbstractDungeon.ascensionLevel >= 18) {
    this.dazedAmt = 3;
} else {
    this.dazedAmt = 2;
}

// takeTurn 실행
case 3:
    AbstractDungeon.actionManager.addToBottom(
        new SFXAction("THUNDERCLAP")
    );

    // 시각 효과
    if (!Settings.FAST_MODE) {
        AbstractDungeon.actionManager.addToBottom(
            new VFXAction((AbstractCreature)this,
                new ShockWaveEffect(this.hb.cX, this.hb.cY,
                    Color.ROYAL, ShockWaveEffect.ShockWaveType.ADDITIVE),
                0.5F)
        );
        AbstractDungeon.actionManager.addToBottom(
            new FastShakeAction((AbstractCreature)AbstractDungeon.player,
                0.6F, 0.2F)
        );
    } else {
        AbstractDungeon.actionManager.addToBottom(
            new VFXAction((AbstractCreature)this,
                new ShockWaveEffect(this.hb.cX, this.hb.cY,
                    Color.ROYAL, ShockWaveEffect.ShockWaveType.ADDITIVE),
                0.1F)
        );
        AbstractDungeon.actionManager.addToBottom(
            new FastShakeAction((AbstractCreature)AbstractDungeon.player,
                0.6F, 0.15F)
        );
    }

    // Dazed 카드 추가
    AbstractDungeon.actionManager.addToBottom(
        new MakeTempCardInDiscardAction(
            (AbstractCard)new Dazed(),
            this.dazedAmt
        )
    );
    break;
```

**특징**:
- 매우 화려한 시각 효과 (충격파, 화면 흔들림)
- 천둥소리 효과 (THUNDERCLAP)
- **Dazed 카드**: 비용 있는 상태 카드, 아무 효과 없음
- 버리기 더미에 추가되므로 다음 덱 셔플 시 드로우됨

**수정 포인트**:
- Dazed 수 변경: `dazedAmt` 필드 또는 A18 조건문
- 효과 제거/변경: VFXAction, SFXAction 수정
- 다른 상태 카드 추가: `new Dazed()` → 다른 카드

---

### 패턴 2: 빔 (Beam)

**바이트 값**: `4`
**의도**: `ATTACK`
**발동 조건**: 첫 턴에 홀수 인덱스 센트리가 사용 또는 AI 로직에 따름

**데미지**:
| 난이도 | 데미지 |
|--------|--------|
| 기본 (A0-A2) | 9 |
| A3+ | 10 |

**코드 위치**: 36-37줄, 53-57줄, 65줄, 106-131줄

```java
// 상수
private static final byte BEAM = 4;

// 생성자에서 설정
if (AbstractDungeon.ascensionLevel >= 3) {
    this.beamDmg = 10;
} else {
    this.beamDmg = 9;
}

// 데미지 등록
this.damage.add(new DamageInfo((AbstractCreature)this, this.beamDmg));

// takeTurn 실행
case 4:
    // 공격 애니메이션
    AbstractDungeon.actionManager.addToBottom(
        new ChangeStateAction(this, "ATTACK")
    );

    // 빔 효과음
    AbstractDungeon.actionManager.addToBottom(
        new SFXAction("ATTACK_MAGIC_BEAM_SHORT", 0.5F)
    );

    // 화면 테두리 플래시
    AbstractDungeon.actionManager.addToBottom(
        new VFXAction(new BorderFlashEffect(Color.SKY))
    );

    // 레이저 효과
    if (Settings.FAST_MODE) {
        AbstractDungeon.actionManager.addToBottom(
            new VFXAction(
                new SmallLaserEffect(
                    AbstractDungeon.player.hb.cX,
                    AbstractDungeon.player.hb.cY,
                    this.hb.cX,
                    this.hb.cY
                ),
                0.1F
            )
        );
    } else {
        AbstractDungeon.actionManager.addToBottom(
            new VFXAction(
                new SmallLaserEffect(
                    AbstractDungeon.player.hb.cX,
                    AbstractDungeon.player.hb.cY,
                    this.hb.cX,
                    this.hb.cY
                ),
                0.3F
            )
        );
    }

    // 데미지 적용
    AbstractDungeon.actionManager.addToBottom(
        new DamageAction(
            (AbstractCreature)AbstractDungeon.player,
            this.damage.get(0),
            AbstractGameAction.AttackEffect.NONE,
            Settings.FAST_MODE
        )
    );
    break;
```

**특징**:
- 매우 화려한 레이저 효과
- 빔 효과음 (ATTACK_MAGIC_BEAM_SHORT)
- 화면 테두리 플래시 (하늘색)
- "ATTACK" 애니메이션 재생

**수정 포인트**:
- 데미지 변경: `beamDmg` 필드 또는 A3 조건문
- 효과 제거/변경: VFXAction, SFXAction 수정

---

## AI 로직 (getMove)

**매우 단순한 교대 패턴**

**코드 위치**: 158-174줄

```java
protected void getMove(int num) {
    if (this.firstMove) {
        // 첫 번째 턴: 인덱스에 따라 패턴 결정
        if ((AbstractDungeon.getMonsters()).monsters.lastIndexOf(this) % 2 == 0) {
            // 짝수 인덱스 (0, 2) → Bolt
            setMove((byte)3, AbstractMonster.Intent.DEBUFF);
        } else {
            // 홀수 인덱스 (1) → Beam
            setMove((byte)4, AbstractMonster.Intent.ATTACK,
                ((DamageInfo)this.damage.get(0)).base);
        }
        this.firstMove = false;
        return;
    }

    // 이후 턴: 이전 패턴과 반대 패턴
    if (lastMove((byte)4)) {
        // 이전에 Beam → Bolt
        setMove((byte)3, AbstractMonster.Intent.DEBUFF);
    } else {
        // 이전에 Bolt → Beam
        setMove((byte)4, AbstractMonster.Intent.ATTACK,
            ((DamageInfo)this.damage.get(0)).base);
    }
}
```

**로직 설명**:

### 첫 번째 턴
- **센트리 인덱스 확인**: `monsters.lastIndexOf(this) % 2`
- **짝수 인덱스 (0, 2)**: **Bolt** (디버프)
- **홀수 인덱스 (1)**: **Beam** (공격)

### 이후 턴
- **이전 패턴이 Beam**: **Bolt**
- **이전 패턴이 Bolt**: **Beam**

**특징**:
- 완벽하게 교대로 패턴 사용
- 첫 턴에 2개는 Bolt, 1개는 Beam
- 매우 예측 가능한 AI
- 랜덤 요소 전혀 없음

**전략적 의미**:
- 센트리 3마리의 패턴이 엇갈림
- 첫 턴: Bolt-Beam-Bolt 순서
- 2턴: Beam-Bolt-Beam 순서
- 3턴: Bolt-Beam-Bolt 순서 (반복)

---

## 특수 메커니즘

### Artifact 파워 시작

**코드 위치**: 80-82줄

```java
public void usePreBattleAction() {
    AbstractDungeon.actionManager.addToBottom(
        new ApplyPowerAction((AbstractCreature)this, (AbstractCreature)this,
            new ArtifactPower((AbstractCreature)this, 1), 1)
    );
}
```

**Artifact 파워**:
- 디버프 무효화 (1회)
- 각 센트리마다 1개씩 보유
- 총 3개의 Artifact (센트리 3마리)

**전략적 의미**:
- 디버프 카드가 효과가 없음 (첫 1회)
- 약화, 취약 등이 막힘
- Artifact를 제거해야 디버프 가능

### 다중 센트리 시스템

**엔카운터 구조**:
- 항상 **3마리** 동시 등장
- 각각 독립적인 HP 및 행동
- 인덱스에 따라 첫 턴 패턴 다름

**배치**:
```
센트리 0 (좌측)  - 첫 턴 Bolt
센트리 1 (중앙)  - 첫 턴 Beam
센트리 2 (우측)  - 첫 턴 Bolt
```

**전략**:
- 우선 순위 결정 중요 (어떤 센트리부터 처치할지)
- Dazed 카드를 추가하는 센트리 우선 처치
- 공격하는 센트리는 나중에 처치

### 애니메이션 상태 전환

**코드 위치**: 74-76줄, 148-155줄, 142-144줄

```java
// 생성자: 애니메이션 전환 속도 설정
this.stateData.setMix("idle", "attack", 0.1F);
this.stateData.setMix("idle", "spaz1", 0.1F);
this.stateData.setMix("idle", "hit", 0.1F);

// changeState: 공격 애니메이션
public void changeState(String stateName) {
    switch (stateName) {
        case "ATTACK":
            this.state.setAnimation(0, "attack", false);
            this.state.addAnimation(0, "idle", true, 0.0F);
            break;
    }
}

// damage: 피격 애니메이션
public void damage(DamageInfo info) {
    super.damage(info);
    if (info.owner != null && info.type != DamageInfo.DamageType.THORNS &&
        info.output > 0) {
        this.state.setAnimation(0, "hit", false);
        this.state.addAnimation(0, "idle", true, 0.0F);
    }
}
```

**애니메이션 종류**:
- **idle**: 기본 대기 (루프)
- **attack**: 공격 (1회)
- **hit**: 피격 (1회)
- **spaz1**: 특수 애니메이션 (미사용?)

**특징**:
- 매우 빠른 전환 (0.1초)
- 공격/피격 후 idle로 복귀

### Dazed 카드 시스템

**Dazed 카드**:
- **타입**: 상태 카드 (Status)
- **비용**: 1 에너지
- **효과**: 없음 (Unplayable이 아님)
- **특징**: 순수 방해 카드

**버리기 더미에 추가**:
```java
new MakeTempCardInDiscardAction((AbstractCard)new Dazed(), this.dazedAmt)
```

**전략적 영향**:
- 덱을 희석시킴
- 다음 셔플 시 드로우 가능
- 에너지 낭비 유발
- 덱 순환 방해

---

## 수정 예시

### 1. Dazed 수 대폭 증가

```java
@SpirePatch(
    cls = "com.megacrit.cardcrawl.monsters.exordium.Sentry",
    method = SpirePatch.CONSTRUCTOR
)
public static class SentryDazedPatch {
    @SpirePostfixPatch
    public static void Postfix(Sentry __instance, float x, float y) {
        if (AbstractDungeon.isAscensionMode &&
            AbstractDungeon.ascensionLevel >= 25) {
            // A25에서 Dazed 5장으로 증가
            ReflectionHacks.setPrivate(__instance, Sentry.class, "dazedAmt", 5);
        }
    }
}
```

### 2. HP 증가

```java
@SpirePatch(
    cls = "com.megacrit.cardcrawl.monsters.exordium.Sentry",
    method = SpirePatch.CONSTRUCTOR
)
public static class SentryHPPatch {
    @SpirePostfixPatch
    public static void Postfix(Sentry __instance, float x, float y) {
        if (AbstractDungeon.isAscensionMode &&
            AbstractDungeon.ascensionLevel >= 25) {
            // A25에서 HP 15% 증가
            int newMaxHP = (int)(__instance.maxHealth * 1.15f);
            __instance.maxHealth = newMaxHP;
            __instance.currentHealth = newMaxHP;
        }
    }
}
```

### 3. Artifact 수 증가

```java
@SpirePatch(
    cls = "com.megacrit.cardcrawl.monsters.exordium.Sentry",
    method = "usePreBattleAction"
)
public static class SentryArtifactPatch {
    @SpirePostfixPatch
    public static void Postfix(Sentry __instance) {
        if (AbstractDungeon.isAscensionMode &&
            AbstractDungeon.ascensionLevel >= 25) {
            // A25에서 Artifact 1개 추가 (총 2개)
            AbstractDungeon.actionManager.addToBottom(
                new ApplyPowerAction(__instance, __instance,
                    new ArtifactPower(__instance, 1), 1)
            );
        }
    }
}
```

### 4. 빔 데미지 증가

```java
@SpirePatch(
    cls = "com.megacrit.cardcrawl.monsters.exordium.Sentry",
    method = SpirePatch.CONSTRUCTOR
)
public static class SentryBeamPatch {
    @SpirePostfixPatch
    public static void Postfix(Sentry __instance, float x, float y) {
        if (AbstractDungeon.isAscensionMode &&
            AbstractDungeon.ascensionLevel >= 25) {
            // A25에서 빔 데미지 +3
            if (!__instance.damage.isEmpty()) {
                __instance.damage.get(0).base += 3;
            }
        }
    }
}
```

### 5. 패턴 변경: 연속 공격

```java
@SpirePatch(
    cls = "com.megacrit.cardcrawl.monsters.exordium.Sentry",
    method = "getMove"
)
public static class SentryPatternPatch {
    @SpirePrefixPatch
    public static SpireReturn<Void> Prefix(Sentry __instance, int num) {
        if (AbstractDungeon.isAscensionMode &&
            AbstractDungeon.ascensionLevel >= 25) {
            // A25에서 무조건 빔 사용 (공격만)
            boolean firstMove = ReflectionHacks.getPrivate(__instance,
                Sentry.class, "firstMove");

            if (firstMove) {
                ReflectionHacks.setPrivate(__instance, Sentry.class,
                    "firstMove", false);
            }

            // 항상 Beam 사용
            __instance.setMove((byte)4, AbstractMonster.Intent.ATTACK,
                __instance.damage.get(0).base);

            return SpireReturn.Return(null);
        }
        return SpireReturn.Continue();
    }
}
```

---

## 중요 필드

| 필드명 | 타입 | 설명 |
|--------|------|------|
| `firstMove` | boolean | 첫 턴 여부 |
| `beamDmg` | int | 빔 데미지 |
| `dazedAmt` | int | Dazed 카드 수 |

---

## 관련 파일

- **본 파일**: `com/megacrit/cardcrawl/monsters/exordium/Sentry.java`
- **카드**: `com.megacrit.cardcrawl.cards.status.Dazed` (Dazed 상태 카드)
- **파워**: `com.megacrit.cardcrawl.powers.ArtifactPower` (Artifact)
- **이펙트**:
  - `BorderFlashEffect` (화면 테두리 플래시)
  - `ShockWaveEffect` (충격파 효과)
  - `SmallLaserEffect` (레이저 빔 효과)
- **액션**:
  - `DamageAction` (데미지 적용)
  - `ApplyPowerAction` (파워 부여)
  - `MakeTempCardInDiscardAction` (카드 생성)
  - `ChangeStateAction` (상태 변경)
  - `VFXAction` (시각 효과)
  - `SFXAction` (음향 효과)
  - `FastShakeAction` (화면 흔들림)

---

## 참고사항

1. **다중 적 전투**: 항상 3마리 등장, 우선 순위 결정 중요
2. **교대 패턴**: 완벽하게 예측 가능한 AI (Bolt-Beam 교대)
3. **Artifact 보유**: 각 센트리마다 1개씩, 총 3개
4. **Dazed 누적**: 센트리를 늦게 처치할수록 Dazed 카드 증가
5. **화려한 효과**: 레이저, 충격파, 화면 흔들림 등
6. **빠른 모드**: Settings.FAST_MODE에 따라 효과 시간 조절
7. **인덱스 기반**: 첫 턴 패턴이 인덱스에 따라 결정됨
8. **엘리트 보상**: 체력 회복 및 레어 카드/유물 획득 기회
9. **애니메이션**: "images/monsters/theBottom/sentry/" 경로 사용
10. **데미지 인덱스**: damage.get(0) = Beam 데미지만 사용
11. **가시 무시**: 가시 데미지는 hit 애니메이션 트리거 안 함
12. **전략**: Bolt 센트리 2개를 먼저 처치하는 것이 일반적
