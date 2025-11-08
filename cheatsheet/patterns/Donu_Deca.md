# 도누와 데카 (Donu and Deca)

## 기본 정보

### Donu (공격형)

**클래스명**: `Donu`
**전체 경로**: `com.megacrit.cardcrawl.monsters.beyond.Donu`
**ID**: `"Donu"`
**타입**: 보스 (BOSS)
**위치**: 오른쪽 (100.0F, 20.0F)

### Deca (방어형)

**클래스명**: `Deca`
**전체 경로**: `com.megacrit.cardcrawl.monsters.beyond.Deca`
**ID**: `"Deca"`
**타입**: 보스 (BOSS)
**위치**: 왼쪽 (-350.0F, 30.0F)

**인카운터명**: `"Donu and Deca"`
**등장 지역**: 3막 (The Beyond)

---

## HP 정보

### Donu (도누)

| 난이도 | HP |
|--------|-----|
| 기본 (A0-A8) | 250 |
| A9+ | 265 |

### Deca (데카)

| 난이도 | HP |
|--------|-----|
| 기본 (A0-A8) | 250 |
| A9+ | 265 |

**총 HP**: 500 (A0-A8) / 530 (A9+)

---

## 듀얼 보스 메커니즘

### 동시 등장 시스템

**스폰 순서**:
```java
new MonsterGroup(new AbstractMonster[] {
    (AbstractMonster)new Deca(),  // 왼쪽
    (AbstractMonster)new Donu()   // 오른쪽
});
```

**특징**:
- 항상 함께 등장
- 독립적으로 행동
- 서로 버프 공유 가능
- 둘 다 죽어야 전투 승리

---

### 상호작용 시스템

**패턴 교차**:
- **Donu (공격형)**: 공격 → 버프 → 공격 → 버프...
- **Deca (방어형)**: 공격+디버프 → 방어 → 공격+디버프 → 방어...
- 둘이 번갈아가며 위협 생성

**버프 공유**:
- **Donu의 Circle of Protection**: 모든 몬스터에게 Strength +3
- **Deca의 Square of Protection**: 모든 몬스터에게 Block 16 (+ A19: Plated Armor 3)

---

## Donu 패턴 (공격형)

### 초기 상태
```java
private boolean isAttacking = false;  // 첫 턴은 버프부터 시작
```

---

### 패턴 1: Circle of Protection (원형 보호)

**의도**: `BUFF`
**이름**: `MOVES[0]` (Circle of Protection)

**효과**:
- **모든 몬스터**에게 Strength +3 부여
- Donu + Deca 둘 다 강화
- 공격력 영구 증가

**AI 조건**:
```java
if (!isAttacking) {
    setMove(CIRCLE_NAME, (byte)2, Intent.BUFF);
}
```

**코드 구현**:
```java
case 2:  // CIRCLE_OF_PROTECTION
    for (AbstractMonster m : AbstractDungeon.getMonsters().monsters) {
        SFXAction("MONSTER_DONU_DEFENSE");
        ApplyPowerAction(m, this, new StrengthPower(m, 3), 3);
    }
    isAttacking = true;  // 다음 턴은 공격
    break;
```

---

### 패턴 2: Beam (광선)

**의도**: `ATTACK` (x2 멀티어택)
**공격 횟수**: 2회

**데미지**:
| 난이도 | 데미지 | 총합 |
|--------|--------|------|
| 기본 (A0-A3) | 10 x 2 | 20 |
| A4+ | 12 x 2 | 24 |

**AI 조건**:
```java
if (isAttacking) {
    setMove((byte)0, Intent.ATTACK, damage.get(0).base, 2, true);
}
```

**코드 구현**:
```java
case 0:  // BEAM
    ChangeStateAction(this, "ATTACK");
    WaitAction(0.5F);
    for (int i = 0; i < 2; i++) {
        DamageAction(player, damage.get(0), AttackEffect.FIRE);
    }
    isAttacking = false;  // 다음 턴은 버프
    break;
```

---

## Deca 패턴 (방어/디버프형)

### 초기 상태
```java
private boolean isAttacking = true;  // 첫 턴은 공격부터 시작
```

---

### 패턴 1: Beam (광선 + 혼란)

**의도**: `ATTACK_DEBUFF` (x2 멀티어택)
**공격 횟수**: 2회

**데미지**:
| 난이도 | 데미지 | 총합 |
|--------|--------|------|
| 기본 (A0-A3) | 10 x 2 | 20 |
| A4+ | 12 x 2 | 24 |

**디버프**: Dazed 2장 추가 (버려진 카드 더미)

**AI 조건**:
```java
if (isAttacking) {
    setMove((byte)0, Intent.ATTACK_DEBUFF, damage.get(0).base, 2, true);
}
```

**코드 구현**:
```java
case 0:  // BEAM
    ChangeStateAction(this, "ATTACK");
    WaitAction(0.5F);
    for (int i = 0; i < 2; i++) {
        DamageAction(player, damage.get(0), AttackEffect.FIRE);
    }
    MakeTempCardInDiscardAction(new Dazed(), 2);  // Dazed 2장
    isAttacking = false;  // 다음 턴은 방어
    break;
```

---

### 패턴 2: Square of Protection (사각형 보호)

**의도**: `DEFEND` (기본) / `DEFEND_BUFF` (A19+)

**효과**:
- **모든 몬스터**에게 Block 16 부여
- **A19+**: 추가로 Plated Armor 3 부여

**AI 조건**:
```java
if (!isAttacking) {
    if (AbstractDungeon.ascensionLevel >= 19) {
        setMove((byte)2, Intent.DEFEND_BUFF);
    } else {
        setMove((byte)2, Intent.DEFEND);
    }
}
```

**코드 구현**:
```java
case 2:  // SQUARE_OF_PROTECTION
    for (AbstractMonster m : AbstractDungeon.getMonsters().monsters) {
        GainBlockAction(m, this, 16);
        if (AbstractDungeon.ascensionLevel >= 19) {
            ApplyPowerAction(m, this, new PlatedArmorPower(m, 3), 3);
        }
    }
    isAttacking = true;  // 다음 턴은 공격
    break;
```

---

## AI 로직 분석

### Donu AI (교대 패턴)

**패턴 순서**:
1. **첫 턴**: Circle of Protection (버프) → `isAttacking = true`
2. **둘째 턴**: Beam (공격 x2) → `isAttacking = false`
3. **셋째 턴**: Circle of Protection (버프) → `isAttacking = true`
4. 반복...

**결과**: **버프 → 공격 → 버프 → 공격** 반복

---

### Deca AI (교대 패턴)

**패턴 순서**:
1. **첫 턴**: Beam + Dazed (공격+디버프) → `isAttacking = false`
2. **둘째 턴**: Square of Protection (방어+버프) → `isAttacking = true`
3. **셋째 턴**: Beam + Dazed (공격+디버프) → `isAttacking = false`
4. 반복...

**결과**: **공격+디버프 → 방어 → 공격+디버프 → 방어** 반복

---

## 타이밍 조합 분석

### 턴별 동작 (시작 상태 기준)

| 턴 | Donu | Deca |
|----|------|------|
| 1 | Circle (버프) | Beam+Dazed (공격) |
| 2 | Beam (공격) | Square (방어) |
| 3 | Circle (버프) | Beam+Dazed (공격) |
| 4 | Beam (공격) | Square (방어) |

**위험 턴**:
- **1턴**: Deca 공격 (20-24) + Dazed 2장
- **2턴**: Donu 공격 (20-24) + 모두 방어
- **3턴**: Deca 공격 (20-24) + Dazed 2장 + Donu Strength +3
- **4턴**: Donu 강화 공격 (23-27) + 모두 방어

---

## 전투 전 액션

### Donu

**Artifact 부여**:
```java
public void usePreBattleAction() {
    if (AbstractDungeon.ascensionLevel >= 19) {
        ApplyPowerAction(this, this, new ArtifactPower(this, 3));
    } else {
        ApplyPowerAction(this, this, new ArtifactPower(this, 2));
    }
}
```

| 난이도 | Artifact |
|--------|----------|
| 기본 (A0-A18) | 2 |
| A19+ | 3 |

---

### Deca

**음악 + Artifact 부여**:
```java
public void usePreBattleAction() {
    CardCrawlGame.music.unsilenceBGM();
    AbstractDungeon.scene.fadeOutAmbiance();
    AbstractDungeon.getCurrRoom().playBgmInstantly("BOSS_BEYOND");

    if (AbstractDungeon.ascensionLevel >= 19) {
        ApplyPowerAction(this, this, new ArtifactPower(this, 3));
    } else {
        ApplyPowerAction(this, this, new ArtifactPower(this, 2));
    }

    UnlockTracker.markBossAsSeen("DONUT");
}
```

| 난이도 | Artifact |
|--------|----------|
| 기본 (A0-A18) | 2 |
| A19+ | 3 |

---

## 사망 처리

### 둘 중 하나 사망

**공통 die() 로직**:
```java
public void die() {
    super.die();
    if (AbstractDungeon.getMonsters().areMonstersBasicallyDead()) {
        // 둘 다 죽었을 때만 승리 처리
        useFastShakeAnimation(5.0F);
        CardCrawlGame.screenShake.rumble(4.0F);
        onBossVictoryLogic();
        UnlockTracker.hardUnlockOverride("DONUT");
        UnlockTracker.unlockAchievement("SHAPES");
        onFinalBossVictoryLogic();
    }
}
```

**특징**:
- **하나만 죽으면**: 전투 계속
- **둘 다 죽으면**: 승리 처리
- **기본 게임**: 생존자 강화 없음 (Artifact로 디버프 차단만)

---

## 전투 전략

### 우선순위 선택

**1. Deca 먼저 처치**:
- **장점**: Dazed 카드 중단, Plated Armor 중단 (A19+)
- **단점**: Donu의 지속적인 버프

**2. Donu 먼저 처치**:
- **장점**: Strength 버프 중단, 공격력 증가 차단
- **단점**: 계속되는 Dazed 누적

**추천**: **Deca 먼저** (Dazed 누적이 더 위험)

---

### 난이도별 전략

**A0-A3** (기본):
- 공격력: 10x2 = 20
- Artifact: 2
- 방어 위주 플레이 가능

**A4-A8** (중간):
- 공격력: 12x2 = 24
- 매 턴 24 데미지 대비 필요

**A9-A18** (높음):
- HP: 250 → 265 (총 530)
- 장기전 준비 필요

**A19+** (극한):
- Artifact: 2 → 3
- Plated Armor 3 추가 (매 방어턴)
- 디버프 전략 약화
- 강력한 단타 또는 AOE 필요

---

### 카드 추천

**AOE 공격** (양쪽 동시):
- Whirlwind
- Immolate
- Cleave
- Dagger Spray (Silent)
- Meteor Strike (Defect)

**강력한 단타** (집중 처치):
- Heavy Blade
- Bludgeon
- Feed (처치 시 HP 증가)
- Carnage
- Glass Knife (Silent)

**디버프 차단** (Artifact 제거):
- Dark Shackles (Strength 감소 무시됨)
- Disarm (Artifact 소모 후 적용)
- Corpse Explosion (Silent)

**Dazed 대응**:
- Second Wind (Dazed 소모)
- Evolve (Dazed 1장당 카드 드로우)
- Fire Breathing (Dazed 추가 시 데미지)

---

### 위험 요소

**Strength 누적**:
- Donu가 3턴마다 Circle 사용
- Strength +3 영구 누적
- 5턴 후: 24 → 30 데미지
- 8턴 후: 24 → 36 데미지

**Dazed 누적**:
- Deca가 2턴마다 Dazed 2장
- 8턴 후: Dazed 8장
- 덱 회전 방해
- 핵심 카드 묻힘

**Plated Armor (A19+)**:
- Deca가 2턴마다 +3
- 8턴 후: 12 고정 방어
- 약한 다단 공격 무효화

---

## 수정 예시

### 1. 파트너 사망 시 강화

```java
@SpirePatch(
    cls = "com.megacrit.cardcrawl.monsters.beyond.Donu",
    method = "die"
)
public static class DonuDeathEnragePatch {
    @SpirePrefixPatch
    public static void Prefix(Donu __instance) {
        // Donu 사망 시 Deca 강화
        for (AbstractMonster m : AbstractDungeon.getMonsters().monsters) {
            if (m instanceof Deca && !m.isDying) {
                AbstractDungeon.actionManager.addToTop(
                    new ApplyPowerAction(m, m, new StrengthPower(m, 3), 3)
                );
            }
        }
    }
}

@SpirePatch(
    cls = "com.megacrit.cardcrawl.monsters.beyond.Deca",
    method = "die"
)
public static class DecaDeathEnragePatch {
    @SpirePrefixPatch
    public static void Prefix(Deca __instance) {
        // Deca 사망 시 Donu 강화
        for (AbstractMonster m : AbstractDungeon.getMonsters().monsters) {
            if (m instanceof Donu && !m.isDying) {
                AbstractDungeon.actionManager.addToTop(
                    new ApplyPowerAction(m, m, new StrengthPower(m, 3), 3)
                );
            }
        }
    }
}
```

---

### 2. 버프 강화 (A25+)

```java
@SpirePatch(
    cls = "com.megacrit.cardcrawl.monsters.beyond.Donu",
    method = "takeTurn"
)
public static class DonuBuffPatch {
    @SpireInsertPatch(
        locator = CircleLocator.class
    )
    public static void Insert(Donu __instance) {
        if (AbstractDungeon.ascensionLevel >= 25) {
            // Circle: Strength +3 → +5
            // 또는 추가로 Metallicize 4 부여
        }
    }
}

@SpirePatch(
    cls = "com.megacrit.cardcrawl.monsters.beyond.Deca",
    method = "takeTurn"
)
public static class DecaBuffPatch {
    @SpireInsertPatch(
        locator = SquareLocator.class
    )
    public static void Insert(Deca __instance) {
        if (AbstractDungeon.ascensionLevel >= 25) {
            // Square: Block 16 → 20
            // Plated Armor 3 → 5
            // 또는 추가로 Thorns 2 부여
        }
    }
}
```

---

### 3. 공격 횟수 증가

```java
@SpirePatch(
    cls = "com.megacrit.cardcrawl.monsters.beyond.Donu",
    method = "takeTurn"
)
public static class DonuAttackPatch {
    @SpireInsertPatch(
        locator = BeamLocator.class
    )
    public static void Insert(Donu __instance) {
        if (AbstractDungeon.ascensionLevel >= 25) {
            // Beam: 2회 → 3회
            // 12 x 2 = 24 → 12 x 3 = 36
        }
    }
}
```

---

### 4. 시너지 패턴 추가

```java
@SpirePatch(
    cls = "com.megacrit.cardcrawl.monsters.beyond.Donu",
    method = "takeTurn"
)
public static class DonuSynergyPatch {
    @SpirePostfixPatch
    public static void Postfix(Donu __instance) {
        // Deca가 방어 턴일 때 Donu 공격력 증가
        for (AbstractMonster m : AbstractDungeon.getMonsters().monsters) {
            if (m instanceof Deca) {
                Deca deca = (Deca) m;
                if (!ReflectionHacks.getPrivate(deca, Deca.class, "isAttacking")) {
                    // Deca 방어 중: Donu 데미지 +50%
                }
            }
        }
    }
}
```

---

## 중요 필드

### Donu

| 필드명 | 타입 | 기본값 | 설명 |
|--------|------|--------|------|
| `beamDmg` | int | 10/12 | Beam 데미지 (A4+: 12) |
| `isAttacking` | boolean | false | 공격 턴 여부 (교대 제어) |

### Deca

| 필드명 | 타입 | 기본값 | 설명 |
|--------|------|--------|------|
| `beamDmg` | int | 10/12 | Beam 데미지 (A4+: 12) |
| `isAttacking` | boolean | true | 공격 턴 여부 (교대 제어) |

---

## 애니메이션 상태

### Donu

**애니메이션 믹싱**:
```java
stateData.setMix("Hit", "Idle", 0.1F);
stateData.setMix("Attack_2", "Idle", 0.1F);
```

**상태 전환**:
- `"Idle"`: 기본 대기
- `"Attack_2"`: 공격 애니메이션
- `"Hit"`: 피격 애니메이션

---

### Deca

**애니메이션 믹싱** (Donu와 동일):
```java
stateData.setMix("Hit", "Idle", 0.1F);
stateData.setMix("Attack_2", "Idle", 0.1F);
```

**상태 전환** (Donu와 동일):
- `"Idle"`: 기본 대기
- `"Attack_2"`: 공격 애니메이션
- `"Hit"`: 피격 애니메이션

---

## 관련 파일

### 몬스터

- **Donu**: `com/megacrit/cardcrawl/monsters/beyond/Donu.java`
- **Deca**: `com/megacrit/cardcrawl/monsters/beyond/Deca.java`

### 파워

- `com.megacrit.cardcrawl.powers.StrengthPower`
- `com.megacrit.cardcrawl.powers.ArtifactPower`
- `com.megacrit.cardcrawl.powers.PlatedArmorPower` (A19+ Deca)

### 액션

- `DamageAction`
- `ApplyPowerAction`
- `GainBlockAction`
- `MakeTempCardInDiscardAction`
- `ChangeStateAction`
- `SFXAction`

### 카드

- `com.megacrit.cardcrawl.cards.status.Dazed` (Deca 디버프)

### 스폰

- `com.megacrit.cardcrawl.helpers.MonsterHelper` (line 560)
- `com.megacrit.cardcrawl.monsters.MonsterGroup`

---

## 참고사항

1. **듀얼 보스**: 항상 함께 등장, 독립 행동
2. **교대 패턴**: Donu는 버프→공격, Deca는 공격→방어
3. **버프 공유**: 둘 다 모든 몬스터 강화
4. **Artifact**: 각각 2-3개로 디버프 차단
5. **Dazed 누적**: Deca가 매 공격마다 2장씩
6. **Strength 누적**: Donu가 매 버프마다 +3씩 영구
7. **A19+ 강화**: Artifact 3, Plated Armor 3
8. **기본 게임**: 파트너 사망 시 강화 없음 (사용자 정의 필요)
9. **승리 조건**: 둘 다 처치해야 승리
10. **음악**: Deca가 "BOSS_BEYOND" BGM 재생

---

## 업적

**DONUT**: Donu and Deca 처치
**SHAPES**: 관련 업적 (도형 보스들)
