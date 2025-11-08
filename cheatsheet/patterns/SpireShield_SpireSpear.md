# 첨탑 방패와 첨탑 창 (SpireShield & SpireSpear)

## 기본 정보

**등장**: 4막 보스전 CorruptHeart가 소환하는 미니언
**타입**: 엘리트 (ELITE)
**특징**: 플레이어를 앞뒤로 협공하는 듀오 미니언

---

## SpireShield (첨탑 방패)

### 기본 정보

**클래스명**: `SpireShield`
**전체 경로**: `com.megacrit.cardcrawl.monsters.ending.SpireShield`
**ID**: `"SpireShield"`
**역할**: 방어형 미니언 - 디버프와 블록 부여

### HP 정보

| 난이도 | HP |
|--------|-----|
| A0-A7 | 110 |
| A8+ | 125 |

**코드 위치**: 51-55줄

```java
if (AbstractDungeon.ascensionLevel >= 8) {
    setHp(125);
} else {
    setHp(110);
}
```

### 생성자 정보

```java
public SpireShield()
```

**특징**:
- X: -1000.0F, Y: 15.0F (소환 시 위치 조정됨)
- 히트박스: 0.0F, -20.0F, 380.0F, 290.0F
- 애니메이션: `images/monsters/theEnding/shield/skeleton.*`

---

## SpireSpear (첨탑 창)

### 기본 정보

**클래스명**: `SpireSpear`
**전체 경로**: `com.megacrit.cardcrawl.monsters.ending.SpireSpear`
**ID**: `"SpireSpear"`
**역할**: 공격형 미니언 - 다단 히트와 버프

### HP 정보

| 난이도 | HP |
|--------|-----|
| A0-A7 | 160 |
| A8+ | 180 |

**코드 위치**: 51-55줄

```java
if (AbstractDungeon.ascensionLevel >= 8) {
    setHp(180);
} else {
    setHp(160);
}
```

### 생성자 정보

```java
public SpireSpear()
```

**특징**:
- X: 70.0F, Y: 10.0F (소환 시 위치 조정됨)
- 히트박스: 0.0F, -15.0F, 380.0F, 290.0F
- 애니메이션: `images/monsters/theEnding/spear/skeleton.*`
- 애니메이션 속도: 0.7x (느린 움직임)

---

## 특수 메커니즘: 협공 시스템

### usePreBattleAction - 양면 공격 설정

**SpireShield 코드**: 67-76줄
**SpireSpear 코드**: 69-75줄

```java
// SpireShield
public void usePreBattleAction() {
    // 플레이어에게 Surrounded(협공) 파워 부여 (SpireShield만)
    AbstractDungeon.actionManager.addToBottom(
        new ApplyPowerAction(AbstractDungeon.player, this,
            new SurroundedPower(AbstractDungeon.player))
    );

    // Artifact 파워 부여
    if (AbstractDungeon.ascensionLevel >= 18) {
        AbstractDungeon.actionManager.addToBottom(
            new ApplyPowerAction(this, this,
                new ArtifactPower(this, 2))
        );
    } else {
        AbstractDungeon.actionManager.addToBottom(
            new ApplyPowerAction(this, this,
                new ArtifactPower(this, 1))
        );
    }
}

// SpireSpear (Artifact만 부여, Surrounded 부여 안함)
public void usePreBattleAction() {
    if (AbstractDungeon.ascensionLevel >= 18) {
        AbstractDungeon.actionManager.addToBottom(
            new ApplyPowerAction(this, this,
                new ArtifactPower(this, 2))
        );
    } else {
        AbstractDungeon.actionManager.addToBottom(
            new ApplyPowerAction(this, this,
                new ArtifactPower(this, 1))
        );
    }
}
```

**중요**: **SpireShield만** Surrounded 파워를 부여함

**Surrounded 파워 효과**:
- 플레이어가 앞뒤로 협공당하는 상태
- 플레이어의 공격 방향이 제한됨
- 한 미니언이 죽으면 자동으로 해제됨

**Artifact 파워**:
| 난이도 | Artifact 스택 |
|--------|--------------|
| A0-A17 | 1 |
| A18+ | 2 |

### die() - 협공 해제 로직

**SpireShield 코드**: 175-191줄
**SpireSpear 코드**: 170-185줄

```java
public void die() {
    super.die();
    for (AbstractMonster m : (AbstractDungeon.getCurrRoom()).monsters.monsters) {
        if (!m.isDead && !m.isDying) {
            // Surrounded 파워 제거
            if (AbstractDungeon.player.hasPower("Surrounded")) {
                // 플레이어를 살아남은 미니언 방향으로 회전
                AbstractDungeon.player.flipHorizontal =
                    (m.drawX < AbstractDungeon.player.drawX);
                AbstractDungeon.actionManager.addToBottom(
                    new RemoveSpecificPowerAction(
                        AbstractDungeon.player,
                        AbstractDungeon.player,
                        "Surrounded"
                    )
                );
            }

            // BackAttack 파워 제거 (존재하면)
            if (m.hasPower("BackAttack")) {
                AbstractDungeon.actionManager.addToBottom(
                    new RemoveSpecificPowerAction(m, m, "BackAttack")
                );
            }
        }
    }
}
```

**협공 해제 타이밍**:
1. SpireShield 또는 SpireSpear 중 하나가 죽으면
2. 플레이어의 Surrounded 파워 즉시 제거
3. 플레이어가 살아남은 적 방향으로 자동 회전
4. BackAttack 파워도 함께 제거

---

## SpireShield 패턴 정보

### 패턴 순환

**moveCount % 3** 기반 패턴 순환
- 턴 0: BASH 또는 FORTIFY (50% 확률)
- 턴 1: BASH 또는 FORTIFY (이전 패턴 반복 방지)
- 턴 2: SMASH (고정)
- 이후 반복...

### 패턴 1: 강타 (Bash)

**바이트 값**: `1` (BASH)
**의도**: `ATTACK_DEBUFF`
**데미지**: 12 또는 14

**데미지 설정**:
| 난이도 | 데미지 |
|--------|--------|
| A0-A2 | 12 |
| A3+ | 14 |

**효과**:
- **단일 공격**
- 플레이어에게 **디버프 부여** (50% 확률로 분기):
  - 플레이어가 구슬을 가지고 있으면: **집중력(Focus) -1**
  - 구슬이 없으면: **힘(Strength) -1**

**코드 위치**: 57-62줄, 82-98줄

```java
// 데미지 설정 (생성자)
if (AbstractDungeon.ascensionLevel >= 3) {
    this.damage.add(new DamageInfo(this, 14));  // index 0
    this.damage.add(new DamageInfo(this, 38));  // index 1
} else {
    this.damage.add(new DamageInfo(this, 12));  // index 0
    this.damage.add(new DamageInfo(this, 34));  // index 1
}

// takeTurn
case 1:  // BASH
    AbstractDungeon.actionManager.addToBottom(
        new ChangeStateAction(this, "ATTACK")
    );
    AbstractDungeon.actionManager.addToBottom(new WaitAction(0.35F));
    AbstractDungeon.actionManager.addToBottom(
        new DamageAction(AbstractDungeon.player,
            this.damage.get(0),
            AbstractGameAction.AttackEffect.BLUNT_HEAVY)
    );

    // 디버프 분기
    if (!AbstractDungeon.player.orbs.isEmpty() &&
        AbstractDungeon.aiRng.randomBoolean()) {
        // 구슬 보유 시: Focus -1
        AbstractDungeon.actionManager.addToBottom(
            new ApplyPowerAction(AbstractDungeon.player, this,
                new FocusPower(AbstractDungeon.player, -1), -1)
        );
    } else {
        // 구슬 없음: Strength -1
        AbstractDungeon.actionManager.addToBottom(
            new ApplyPowerAction(AbstractDungeon.player, this,
                new StrengthPower(AbstractDungeon.player, -1), -1)
        );
    }
    break;
```

**AI 로직**: 127-146줄

```java
protected void getMove(int num) {
    switch (this.moveCount % 3) {
        case 0:
            if (AbstractDungeon.aiRng.randomBoolean()) {
                setMove((byte)2, Intent.DEFEND);
            } else {
                setMove((byte)1, Intent.ATTACK_DEBUFF,
                    this.damage.get(0).base);
            }
            break;

        case 1:
            if (!lastMove((byte)1)) {
                setMove((byte)1, Intent.ATTACK_DEBUFF,
                    this.damage.get(0).base);
            } else {
                setMove((byte)2, Intent.DEFEND);
            }
            break;

        default:  // case 2
            setMove((byte)3, Intent.ATTACK_DEFEND,
                this.damage.get(1).base);
            break;
    }
    this.moveCount++;
}
```

### 패턴 2: 강화 (Fortify)

**바이트 값**: `2` (FORTIFY)
**의도**: `DEFEND`
**효과**: **모든 적에게 블록 30 부여**

**코드 위치**: 65줄, 105-107줄

```java
private static final int FORTIFY_BLOCK = 30;

// takeTurn
case 2:  // FORTIFY
    for (AbstractMonster m :
         (AbstractDungeon.getMonsters()).monsters) {
        AbstractDungeon.actionManager.addToBottom(
            new GainBlockAction(m, this, 30)
        );
    }
    break;
```

**효과 범위**:
- CorruptHeart 포함
- SpireShield 자신
- SpireSpear (살아있으면)
- 총 3개체에 블록 30씩 부여

### 패턴 3: 강타 (Smash)

**바이트 값**: `3` (SMASH)
**의도**: `ATTACK_DEFEND`
**데미지**: 34 또는 38

**데미지 설정**:
| 난이도 | 데미지 |
|--------|--------|
| A0-A2 | 34 |
| A3+ | 38 |

**효과**:
- **강력한 단일 공격**
- **자신에게 블록 부여** (난이도별):
  - A0-A17: **피해량만큼 블록 획득** (34 또는 38)
  - A18+: **고정 99 블록 획득**

**코드 위치**: 65줄, 110-118줄

```java
case 3:  // SMASH
    AbstractDungeon.actionManager.addToBottom(
        new ChangeStateAction(this, "OLD_ATTACK")
    );
    AbstractDungeon.actionManager.addToBottom(new WaitAction(0.5F));
    AbstractDungeon.actionManager.addToBottom(
        new DamageAction(AbstractDungeon.player,
            this.damage.get(1),
            AbstractGameAction.AttackEffect.BLUNT_HEAVY)
    );

    if (AbstractDungeon.ascensionLevel >= 18) {
        AbstractDungeon.actionManager.addToBottom(
            new GainBlockAction(this, this, 99)
        );
    } else {
        AbstractDungeon.actionManager.addToBottom(
            new GainBlockAction(this, this,
                this.damage.get(1).output)
        );
    }
    break;
```

**블록량**:
| 난이도 | 블록량 |
|--------|--------|
| A0-A17 | 34 또는 38 (데미지와 동일) |
| A18+ | 99 (고정) |

---

## SpireSpear 패턴 정보

### 패턴 순환

**moveCount % 3** 기반 패턴 순환
- 턴 0: BURN_STRIKE 또는 PIERCER (이전 턴에 BURN_STRIKE 안 했으면)
- 턴 1: SKEWER (고정)
- 턴 2: PIERCER 또는 BURN_STRIKE (50% 확률)
- 이후 반복...

### 패턴 1: 화염 타격 (Burn Strike)

**바이트 값**: `1` (BURN_STRIKE)
**의도**: `ATTACK_DEBUFF`
**데미지**: 5 x 2 또는 6 x 2 (총 10 또는 12)

**데미지 설정**:
| 난이도 | 1회 데미지 | 총 데미지 |
|--------|-----------|----------|
| A0-A2 | 5 | 10 |
| A3+ | 6 | 12 |

**효과**:
- **2회 연속 공격**
- **화상(Burn) 카드 2장** 추가:
  - A0-A17: **버리기 더미**에 추가
  - A18+: **뽑기 더미**에 추가 (더 위험)

**코드 위치**: 57-65줄, 81-92줄

```java
// 데미지 설정 (생성자)
if (AbstractDungeon.ascensionLevel >= 3) {
    this.skewerCount = 4;
    this.damage.add(new DamageInfo(this, 6));   // index 0
    this.damage.add(new DamageInfo(this, 10));  // index 1
} else {
    this.skewerCount = 3;
    this.damage.add(new DamageInfo(this, 5));   // index 0
    this.damage.add(new DamageInfo(this, 10));  // index 1
}

// takeTurn
case 1:  // BURN_STRIKE
    for (i = 0; i < 2; i++) {
        AbstractDungeon.actionManager.addToBottom(
            new ChangeStateAction(this, "ATTACK")
        );
        AbstractDungeon.actionManager.addToBottom(new WaitAction(0.15F));
        AbstractDungeon.actionManager.addToBottom(
            new DamageAction(AbstractDungeon.player,
                this.damage.get(0),
                AbstractGameAction.AttackEffect.FIRE)
        );
    }

    if (AbstractDungeon.ascensionLevel >= 18) {
        // 뽑기 더미에 추가 (더 위험)
        AbstractDungeon.actionManager.addToBottom(
            new MakeTempCardInDrawPileAction(
                new Burn(), 2, false, true)
        );
    } else {
        // 버리기 더미에 추가
        AbstractDungeon.actionManager.addToBottom(
            new MakeTempCardInDiscardAction(new Burn(), 2)
        );
    }
    break;
```

**화상 카드 추가 위치**:
| 난이도 | 추가 위치 |
|--------|----------|
| A0-A17 | 버리기 더미 |
| A18+ | 뽑기 더미 (셔플됨) |

### 패턴 2: 관통 (Piercer)

**바이트 값**: `2` (PIERCER)
**의도**: `BUFF`
**효과**: **모든 적에게 힘(Strength) +2 부여**

**코드 위치**: 97-100줄

```java
case 2:  // PIERCER
    for (AbstractMonster m :
         (AbstractDungeon.getMonsters()).monsters) {
        AbstractDungeon.actionManager.addToBottom(
            new ApplyPowerAction(m, this,
                new StrengthPower(m, 2), 2)
        );
    }
    break;
```

**효과 범위**:
- CorruptHeart 포함
- SpireShield (살아있으면)
- SpireSpear 자신
- 총 3개체에 힘 +2씩 부여

### 패턴 3: 꼬챙이 (Skewer)

**바이트 값**: `3` (SKEWER)
**의도**: `ATTACK`
**데미지**: 10 x 3 또는 10 x 4 (총 30 또는 40)

**데미지 설정**:
| 난이도 | 횟수 | 1회 데미지 | 총 데미지 |
|--------|------|-----------|----------|
| A0-A2 | 3 | 10 | 30 |
| A3+ | 4 | 10 | 40 |

**효과**:
- **연속 다단 히트** (3회 또는 4회)
- 각 공격마다 0.05초 대기 (빠른 연속 공격)

**코드 위치**: 57-65줄, 103-109줄

```java
private int skewerCount;

// 생성자
if (AbstractDungeon.ascensionLevel >= 3) {
    this.skewerCount = 4;
    this.damage.add(new DamageInfo(this, 6));   // index 0
    this.damage.add(new DamageInfo(this, 10));  // index 1
} else {
    this.skewerCount = 3;
    this.damage.add(new DamageInfo(this, 5));   // index 0
    this.damage.add(new DamageInfo(this, 10));  // index 1
}

// takeTurn
case 3:  // SKEWER
    for (i = 0; i < this.skewerCount; i++) {
        AbstractDungeon.actionManager.addToBottom(
            new ChangeStateAction(this, "ATTACK")
        );
        AbstractDungeon.actionManager.addToBottom(new WaitAction(0.05F));
        AbstractDungeon.actionManager.addToBottom(
            new DamageAction(AbstractDungeon.player,
                this.damage.get(1),
                AbstractGameAction.AttackEffect.SLASH_DIAGONAL,
                true)  // fast = true
        );
    }
    break;
```

**AI 로직**: 117-137줄

```java
protected void getMove(int num) {
    switch (this.moveCount % 3) {
        case 0:
            if (!lastMove((byte)1)) {
                setMove((byte)1, Intent.ATTACK_DEBUFF,
                    this.damage.get(0).base, 2, true);
            } else {
                setMove((byte)2, Intent.BUFF);
            }
            break;

        case 1:
            setMove((byte)3, Intent.ATTACK,
                this.damage.get(1).base,
                this.skewerCount, true);
            break;

        default:  // case 2
            if (AbstractDungeon.aiRng.randomBoolean()) {
                setMove((byte)2, Intent.BUFF);
            } else {
                setMove((byte)1, Intent.ATTACK_DEBUFF,
                    this.damage.get(0).base, 2, true);
            }
            break;
    }
    this.moveCount++;
}
```

---

## CorruptHeart와의 관계

### CorruptHeart 소환 로직

**CorruptHeart는 이 미니언들을 직접 소환하지 않습니다.**
- SpireShield와 SpireSpear는 전투 시작 시 **이미 배치되어 있음**
- CorruptHeart.java에는 미니언 소환 코드가 없음
- 게임 엔진에서 전투 초기화 시 함께 스폰됨

### 전투 초기 상태

```
[SpireShield] --- [CorruptHeart] --- [SpireSpear]
     (왼쪽)          (중앙)              (오른쪽)
      방어형          보스               공격형
```

**플레이어**:
- Surrounded 파워로 협공 상태
- 양쪽에서 동시에 위협을 받음

### 전략적 역할 분담

**SpireShield (방어형)**:
- 디버프로 플레이어 약화 (Focus/Strength -1)
- 팀 전체 블록 부여 (30 블록)
- 자신 생존력 강화 (99 블록)
- Artifact로 디버프 저항

**SpireSpear (공격형)**:
- 다단 히트로 블록 관통
- 화상 카드로 덱 오염
- 팀 전체 공격력 증가 (Strength +2)
- Artifact로 디버프 저항

**CorruptHeart (보스)**:
- 강력한 단일 타격
- 대량 다단 히트 (Blood Shots)
- 지속 디버프 (Debilitate)
- 자가 강화 (Strength, Artifact, Beat of Death 등)

---

## 특수 동작

### changeState - 애니메이션 상태 변경

**SpireShield**: 151-162줄

```java
public void changeState(String key) {
    switch (key) {
        case "OLD_ATTACK":
            this.state.setAnimation(0, "old_attack", false);
            this.state.addAnimation(0, "Idle", true, 0.0F);
            break;
        case "ATTACK":
            this.state.setAnimation(0, "Attack", false);
            this.state.addAnimation(0, "Idle", true, 0.0F);
            break;
    }
}
```

**SpireSpear**: 141-153줄

```java
public void changeState(String key) {
    AnimationState.TrackEntry e = null;
    switch (key) {
        case "SLOW_ATTACK":
            this.state.setAnimation(0, "Attack_1", false);
            e = this.state.addAnimation(0, "Idle", true, 0.0F);
            e.setTimeScale(0.5F);
            break;
        case "ATTACK":
            this.state.setAnimation(0, "Attack_2", false);
            e = this.state.addAnimation(0, "Idle", true, 0.0F);
            e.setTimeScale(0.7F);
            break;
    }
}
```

**SpireShield 애니메이션**:
- `ATTACK`: Bash 패턴에서 사용
- `OLD_ATTACK`: Smash 패턴에서 사용

**SpireSpear 애니메이션**:
- `ATTACK`: 일반 공격 (Burn Strike, Skewer)
- `SLOW_ATTACK`: 느린 공격 (현재 미사용)
- Idle 애니메이션 속도: 0.7x 또는 0.5x

### damage - 피격 처리

**SpireShield**: 167-172줄
**SpireSpear**: 160-166줄

```java
public void damage(DamageInfo info) {
    super.damage(info);
    if (info.owner != null &&
        info.type != DamageInfo.DamageType.THORNS &&
        info.output > 0) {
        this.state.setAnimation(0, "Hit", false);

        // SpireSpear만 Idle 속도 조절
        AnimationState.TrackEntry e =
            this.state.addAnimation(0, "Idle", true, 0.0F);
        e.setTimeScale(0.7F);  // SpireSpear only
    }
}
```

**피격 애니메이션**:
- 가시 데미지가 아닌 실제 피해만 애니메이션 재생
- Hit 애니메이션 → Idle로 복귀

---

## 수정 예시

### SpireShield HP 증가

```java
// SpireShield.java 생성자 (51-55줄)

// 기존
if (AbstractDungeon.ascensionLevel >= 8) {
    setHp(125);
} else {
    setHp(110);
}

// 수정: 모든 난이도 HP +50
if (AbstractDungeon.ascensionLevel >= 8) {
    setHp(175);  // 125 → 175
} else {
    setHp(160);  // 110 → 160
}
```

### SpireSpear 공격 횟수 증가

```java
// SpireSpear.java 생성자 (57-65줄)

// 기존
if (AbstractDungeon.ascensionLevel >= 3) {
    this.skewerCount = 4;
    this.damage.add(new DamageInfo(this, 6));
    this.damage.add(new DamageInfo(this, 10));
} else {
    this.skewerCount = 3;
    this.damage.add(new DamageInfo(this, 5));
    this.damage.add(new DamageInfo(this, 10));
}

// 수정: Skewer 횟수 +1
if (AbstractDungeon.ascensionLevel >= 3) {
    this.skewerCount = 5;  // 4 → 5
    this.damage.add(new DamageInfo(this, 6));
    this.damage.add(new DamageInfo(this, 10));
} else {
    this.skewerCount = 4;  // 3 → 4
    this.damage.add(new DamageInfo(this, 5));
    this.damage.add(new DamageInfo(this, 10));
}
```

### SpireShield 블록량 증가

```java
// SpireShield.java (65줄, 105-107줄)

// 기존 상수
private static final int FORTIFY_BLOCK = 30;

// 수정: 블록량 증가
private static final int FORTIFY_BLOCK = 50;  // 30 → 50

// takeTurn의 FORTIFY 케이스는 자동으로 변경된 상수 사용
```

### SpireSpear 화상 카드 증가

```java
// SpireSpear.java takeTurn (81-92줄)

// 기존
if (AbstractDungeon.ascensionLevel >= 18) {
    AbstractDungeon.actionManager.addToBottom(
        new MakeTempCardInDrawPileAction(
            new Burn(), 2, false, true)
    );
} else {
    AbstractDungeon.actionManager.addToBottom(
        new MakeTempCardInDiscardAction(new Burn(), 2)
    );
}

// 수정: 화상 카드 2장 → 3장
if (AbstractDungeon.ascensionLevel >= 18) {
    AbstractDungeon.actionManager.addToBottom(
        new MakeTempCardInDrawPileAction(
            new Burn(), 3, false, true)  // 2 → 3
    );
} else {
    AbstractDungeon.actionManager.addToBottom(
        new MakeTempCardInDiscardAction(new Burn(), 3)  // 2 → 3
    );
}
```

### Artifact 스택 증가

```java
// SpireShield.java usePreBattleAction (68-76줄)
// SpireSpear.java usePreBattleAction (70-75줄)

// 기존
if (AbstractDungeon.ascensionLevel >= 18) {
    AbstractDungeon.actionManager.addToBottom(
        new ApplyPowerAction(this, this,
            new ArtifactPower(this, 2))
    );
} else {
    AbstractDungeon.actionManager.addToBottom(
        new ApplyPowerAction(this, this,
            new ArtifactPower(this, 1))
    );
}

// 수정: 모든 난이도 Artifact +1
if (AbstractDungeon.ascensionLevel >= 18) {
    AbstractDungeon.actionManager.addToBottom(
        new ApplyPowerAction(this, this,
            new ArtifactPower(this, 3))  // 2 → 3
    );
} else {
    AbstractDungeon.actionManager.addToBottom(
        new ApplyPowerAction(this, this,
            new ArtifactPower(this, 2))  // 1 → 2
    );
}
```

---

## 중요 필드

### SpireShield

| 필드 | 타입 | 설명 |
|------|------|------|
| `moveCount` | int | 패턴 순환 카운터 (% 3) |
| `damage` | ArrayList&lt;DamageInfo&gt; | [0]=Bash, [1]=Smash 데미지 |

### SpireSpear

| 필드 | 타입 | 설명 |
|------|------|------|
| `moveCount` | int | 패턴 순환 카운터 (% 3) |
| `skewerCount` | int | Skewer 공격 횟수 (3 또는 4) |
| `damage` | ArrayList&lt;DamageInfo&gt; | [0]=BurnStrike, [1]=Skewer 데미지 |

---

## 관련 파일

### SpireShield
- **소스**: `com/megacrit/cardcrawl/monsters/ending/SpireShield.java`
- **애니메이션**: `images/monsters/theEnding/shield/skeleton.atlas`
- **애니메이션**: `images/monsters/theEnding/shield/skeleton.json`
- **로컬라이징**: `localization/[lang]/monsters.json` → `"SpireShield"`

### SpireSpear
- **소스**: `com/megacrit/cardcrawl/monsters/ending/SpireSpear.java`
- **애니메이션**: `images/monsters/theEnding/spear/skeleton.atlas`
- **애니메이션**: `images/monsters/theEnding/spear/skeleton.json`
- **로컬라이징**: `localization/[lang]/monsters.json` → `"SpireSpear"`

### 관련 클래스
- **CorruptHeart.java**: 4막 최종 보스
- **SurroundedPower.java**: 협공 상태 파워
- **ArtifactPower.java**: 디버프 저항 파워

---

## 전투 팁

### 우선 처치 순서

**일반적 권장**: SpireSpear → SpireShield → CorruptHeart

**이유**:
1. **SpireSpear 우선**:
   - 다단 히트로 블록 관통 (10 x 4 = 40)
   - 화상 카드로 덱 오염
   - 팀 전체 Strength 버프

2. **SpireShield 다음**:
   - 팀 전체 블록 30 부여
   - 고정 99 블록 획득 (A18+)
   - Focus/Strength 디버프

3. **CorruptHeart 마지막**:
   - Invincible 300/200으로 보호됨
   - 미니언 처치 후 집중 공격

### 상황별 전략

**블록 덱 (방어 중심)**:
- SpireSpear 우선 (Strength 버프 차단)
- Fortify 블록 30은 감당 가능
- Smash 99 블록도 관리 가능

**공격 덱 (빠른 처치)**:
- SpireSpear 우선 (빠르게 제거)
- 협공 해제 후 CorruptHeart 집중
- SpireShield는 방치 가능

**구슬 덱 (Defect)**:
- SpireShield 우선 (Focus -1 방지)
- Focus 유지가 생존/화력 핵심
- SpireSpear는 블록으로 버티기

**저체력 상황**:
- SpireSpear 우선 필수
- Skewer 40 데미지 즉사 위험
- 생존이 최우선

### 핵심 대응 요령

1. **협공 상태**: 한 미니언만 죽여도 즉시 해제
2. **Artifact**: 디버프 2회 무효 (A18+)
3. **턴 예측**: moveCount % 3으로 패턴 예측 가능
4. **AoE 공격**: 보스+미니언 동시 타격 효율적
5. **화상 관리**: Exhaust 카드로 제거 또는 Well-Laid Plans로 보관
