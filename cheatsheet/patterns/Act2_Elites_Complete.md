# 2막 엘리트 - 완전 가이드

## 개요

**등장 지역**: 2막 (The City)
**엘리트 몬스터**:
1. Book of Stabbing (찌르기의 서) - 다중 공격 특화
2. Gremlin Leader (그렘린 우두머리) - 하수인 소환 및 버프
3. Taskmaster (감독관) - 단일 패턴 엘리트

---

# 1. 찌르기의 서 (Book of Stabbing)

## 기본 정보

**클래스명**: `BookOfStabbing`
**전체 경로**: `com.megacrit.cardcrawl.monsters.city.BookOfStabbing`
**ID**: `"BookOfStabbing"`
**타입**: 엘리트 (ELITE)

---

## HP 정보

| 난이도 | HP 범위 |
|--------|---------|
| 기본 (A0-A7) | 160-164 |
| A8+ | 168-172 |

**코드 위치**: 52-56줄

```java
if (AbstractDungeon.ascensionLevel >= 8) {
    setHp(168, 172);
} else {
    setHp(160, 164);
}
```

---

## 특수 파워

### Painful Stabs

**부여 시점**: 전투 시작 시 (`usePreBattleAction`)
**효과**: 플레이어가 공격 카드를 사용할 때마다 1 데미지 받음

**코드 위치**: 71-73줄

```java
public void usePreBattleAction() {
    AbstractDungeon.actionManager.addToBottom(
        new ApplyPowerAction(this, this, new PainfulStabsPower(this))
    );
}
```

---

## 패턴 정보

### 패턴 1: 다중 찌르기 (Multi Stab)

**바이트 값**: `1`
**의도**: `ATTACK` (다중 공격)
**데미지**: 6 x N회 (A3+: 7 x N회)

**데미지 수치**:
| 난이도 | 1타당 데미지 |
|--------|-------------|
| 기본 (A0-A2) | 6 |
| A3+ | 7 |

**타수 시스템**:
- 초기: 1타
- 매 사용 후: +1타 증가
- 최대 제한: 없음 (무한 증가)

**코드 위치**: 33-34줄, 58-64줄, 82-89줄

```java
// 데미지 상수
private static final int STAB_DAMAGE = 6;
private static final int A_2_STAB_DAMAGE = 7;

// 생성자에서 설정
if (AbstractDungeon.ascensionLevel >= 3) {
    this.stabDmg = 7;
    this.bigStabDmg = 24;
} else {
    this.stabDmg = 6;
    this.bigStabDmg = 21;
}

// takeTurn 실행
case 1:
    AbstractDungeon.actionManager.addToBottom(
        new ChangeStateAction(this, "ATTACK")
    );
    AbstractDungeon.actionManager.addToBottom(new WaitAction(0.5F));

    for (int i = 0; i < this.stabCount; i++) {
        AbstractDungeon.actionManager.addToBottom(
            new SFXAction("MONSTER_BOOK_STAB_" + MathUtils.random(0, 3))
        );
        AbstractDungeon.actionManager.addToBottom(
            new DamageAction(
                AbstractDungeon.player,
                this.damage.get(0),
                AbstractGameAction.AttackEffect.SLASH_VERTICAL,
                false, true
            )
        );
    }
    break;
```

**특징**:
- 사용할 때마다 타수 증가
- 음성 효과 랜덤 재생 (4가지 중 1개)

---

### 패턴 2: 대형 찌르기 (Big Stab)

**바이트 값**: `2`
**의도**: `ATTACK` (단일 대공격)
**데미지**: 21 (A3+: 24)

**데미지 수치**:
| 난이도 | 데미지 |
|--------|--------|
| 기본 (A0-A2) | 21 |
| A3+ | 24 |

**코드 위치**: 34-35줄, 58-64줄, 95-99줄

```java
// 데미지 상수
private static final int BIG_STAB_DAMAGE = 21;
private static final int A_2_BIG_STAB_DAMAGE = 24;

// takeTurn 실행
case 2:
    AbstractDungeon.actionManager.addToBottom(
        new ChangeStateAction(this, "ATTACK_2")
    );
    AbstractDungeon.actionManager.addToBottom(new WaitAction(0.5F));
    AbstractDungeon.actionManager.addToBottom(
        new DamageAction(
            AbstractDungeon.player,
            this.damage.get(1),
            AbstractGameAction.AttackEffect.SLASH_VERTICAL
        )
    );
    break;
```

**특징**:
- 단일 대공격
- A18+에서는 사용 후 stabCount 증가

---

## AI 로직 (getMove)

**코드 위치**: 130-151줄

```java
protected void getMove(int num) {
    if (num < 15) {  // 15% 확률
        if (lastMove((byte)2)) {  // 이전이 BIG_STAB이면
            this.stabCount++;
            setMove((byte)1, Intent.ATTACK,
                this.damage.get(0).base, this.stabCount, true);
        } else {  // 이전이 BIG_STAB이 아니면
            setMove((byte)2, Intent.ATTACK, this.damage.get(1).base);
            if (AbstractDungeon.ascensionLevel >= 18) {
                this.stabCount++;  // A18+: BIG_STAB 사용 시에도 stabCount 증가
            }
        }
    } else if (lastTwoMoves((byte)1)) {  // 이전 2턴이 모두 STAB이면
        setMove((byte)2, Intent.ATTACK, this.damage.get(1).base);
        if (AbstractDungeon.ascensionLevel >= 18) {
            this.stabCount++;
        }
    } else {  // 기본: STAB
        this.stabCount++;
        setMove((byte)1, Intent.ATTACK,
            this.damage.get(0).base, this.stabCount, true);
    }
}
```

**로직 설명**:

1. **15% 확률 구간 (num < 15)**:
   - 이전 턴이 BIG_STAB(2): stabCount++, STAB 사용
   - 이전 턴이 STAB(1): BIG_STAB 사용 (A18+: stabCount++)

2. **85% 확률 구간 (num >= 15)**:
   - 이전 2턴이 모두 STAB(1): BIG_STAB 사용 (A18+: stabCount++)
   - 그 외: stabCount++, STAB 사용

**핵심 메커니즘**:
- stabCount는 STAB 패턴 선택 시 증가
- A18+에서는 BIG_STAB 사용 시에도 증가
- STAB 2연속 사용 시 강제로 BIG_STAB 사용

---

## 전투 전략

### 추천 전략

**Painful Stabs 대응**:
- 공격 카드 사용 시 1 데미지 누적
- 스킬 중심 덱이 유리
- 파워 카드로 전투 준비

**다중 공격 대응**:
- 장기전 불리 (타수 무한 증가)
- Plated Armor: 타수당 감소로 효과적
- Blur/Buffer: 다중 공격 완전 방어

**위험 요소**:
- 5턴 이후: 6x5 = 30+ 데미지
- 10턴 이후: 6x10 = 60+ 데미지
- Painful Stabs 누적 데미지

**카운터 전략**:
- Plated Armor: 타수당 감소
- Thorns: 타수당 반격
- Blur: Block 누적으로 생존

---

## 수정 포인트

**데미지 변경**:
- `stabDmg` 필드 수정 (생성자 58-64줄)
- `bigStabDmg` 필드 수정 (생성자 58-64줄)

**타수 시스템 변경**:
- `stabCount` 증가 로직 수정 (getMove 130-151줄)
- 최대 타수 제한 추가 가능

**Painful Stabs 제거**:
- usePreBattleAction 메서드 수정 (71-73줄)

---

# 2. 그렘린 우두머리 (Gremlin Leader)

## 기본 정보

**클래스명**: `GremlinLeader`
**전체 경로**: `com.megacrit.cardcrawl.monsters.city.GremlinLeader`
**ID**: `"GremlinLeader"`
**타입**: 엘리트 (ELITE)
**인카운터 이름**: `"Gremlin Leader Combat"`

---

## HP 정보

| 난이도 | HP 범위 |
|--------|---------|
| 기본 (A0-A7) | 140-148 |
| A8+ | 145-155 |

**코드 위치**: 72-76줄

```java
if (AbstractDungeon.ascensionLevel >= 8) {
    setHp(145, 155);
} else {
    setHp(140, 148);
}
```

---

## 하수인 정보

### 초기 하수인

**등장 구성**: 2마리 (위치: 0, 1번 슬롯)
**빈 슬롯**: 1개 (2번 슬롯 - 소환용)

**코드 위치**: 95-103줄

```java
public void usePreBattleAction() {
    this.gremlins[0] = AbstractDungeon.getMonsters().monsters.get(0);
    this.gremlins[1] = AbstractDungeon.getMonsters().monsters.get(1);
    this.gremlins[2] = null;  // 소환용 슬롯

    // 모든 하수인에게 Minion 파워 부여
    for (AbstractMonster m : this.gremlins) {
        AbstractDungeon.actionManager.addToBottom(
            new ApplyPowerAction(m, m, new MinionPower(this))
        );
    }
}
```

**Minion 파워 효과**: Leader 사망 시 모든 하수인 도망

---

## 패턴 정보

### 패턴 1: 소환 (Rally)

**바이트 값**: `2`
**의도**: `UNKNOWN`
**효과**: 그렘린 2마리 소환

**코드 위치**: 43줄, 108-112줄

```java
private static final byte RALLY = 2;
private static final String RALLY_NAME = MOVES[0];

// takeTurn 실행
case 2:
    AbstractDungeon.actionManager.addToBottom(
        new ChangeStateAction(this, "CALL")
    );
    AbstractDungeon.actionManager.addToBottom(
        new SummonGremlinAction(this.gremlins)
    );
    AbstractDungeon.actionManager.addToBottom(
        new SummonGremlinAction(this.gremlins)
    );
    break;
```

**특징**:
- 2마리 연속 소환
- 빈 슬롯에 랜덤 그렘린 생성
- 최대 3마리까지 존재 가능

---

### 패턴 2: 격려 (Encourage)

**바이트 값**: `3`
**의도**: `DEFEND_BUFF`
**효과**:
- **모든 아군 (본인 포함)**: Strength 부여
- **미니언만**: Block 부여

**Strength 수치**:
| 난이도 | Strength |
|--------|----------|
| 기본 (A0-A2) | 3 |
| A3-A17 | 4 |
| A18+ | 5 |

**Block 수치**:
| 난이도 | Block |
|--------|-------|
| 기본 (A0-A17) | 6 |
| A18+ | 10 |

**코드 위치**: 48-55줄, 78-87줄, 114-126줄

```java
// 상수
private static final byte ENCOURAGE = 3;
private static final int STR_AMT = 3;
private static final int BLOCK_AMT = 6;
private static final int A_2_STR_AMT = 4;
private static final int A_18_STR_AMT = 5;
private static final int A_18_BLOCK_AMT = 10;

// 생성자에서 설정
if (AbstractDungeon.ascensionLevel >= 18) {
    this.strAmt = 5;
    this.blockAmt = 10;
} else if (AbstractDungeon.ascensionLevel >= 3) {
    this.strAmt = 4;
    this.blockAmt = 6;
} else {
    this.strAmt = 3;
    this.blockAmt = 6;
}

// takeTurn 실행
case 3:
    AbstractDungeon.actionManager.addToBottom(
        new ShoutAction(this, getEncourageQuote())
    );
    for (AbstractMonster m : AbstractDungeon.getMonsters().monsters) {
        if (m == this) {
            // 본인: Strength만
            AbstractDungeon.actionManager.addToBottom(
                new ApplyPowerAction(m, this,
                    new StrengthPower(m, this.strAmt), this.strAmt)
            );
        } else if (!m.isDying) {
            // 미니언: Strength + Block
            AbstractDungeon.actionManager.addToBottom(
                new ApplyPowerAction(m, this,
                    new StrengthPower(m, this.strAmt), this.strAmt)
            );
            AbstractDungeon.actionManager.addToBottom(
                new GainBlockAction(m, this, this.blockAmt)
            );
        }
    }
    break;
```

**특징**:
- 대사 랜덤 출력 (3가지 중 1개)
- Leader 본인도 Strength 획득
- 미니언만 Block 추가 획득

---

### 패턴 3: 찌르기 (Stab)

**바이트 값**: `4`
**의도**: `ATTACK` (다중 공격)
**데미지**: 6 x 3회 = 18

**코드 위치**: 56-57줄, 91줄, 129-137줄

```java
// 상수
private static final byte STAB = 4;
private int STAB_DMG = 6, STAB_AMT = 3;

// 데미지 등록
this.damage.add(new DamageInfo(this, this.STAB_DMG));

// takeTurn 실행
case 4:
    AbstractDungeon.actionManager.addToBottom(
        new ChangeStateAction(this, "ATTACK")
    );
    AbstractDungeon.actionManager.addToBottom(new WaitAction(0.5F));
    AbstractDungeon.actionManager.addToBottom(
        new DamageAction(AbstractDungeon.player,
            this.damage.get(0),
            AbstractGameAction.AttackEffect.SLASH_HORIZONTAL, true)
    );
    AbstractDungeon.actionManager.addToBottom(
        new DamageAction(AbstractDungeon.player,
            this.damage.get(0),
            AbstractGameAction.AttackEffect.SLASH_VERTICAL, true)
    );
    AbstractDungeon.actionManager.addToBottom(
        new DamageAction(AbstractDungeon.player,
            this.damage.get(0),
            AbstractGameAction.AttackEffect.SLASH_HEAVY)
    );
    break;
```

**특징**:
- 3회 연속 공격 (총 18 데미지)
- 공격 이펙트 각기 다름

---

## AI 로직 (getMove)

**코드 위치**: 154-207줄

```java
protected void getMove(int num) {
    if (numAliveGremlins() == 0) {  // 하수인 0마리
        if (num < 75) {  // 75% 확률
            if (!lastMove((byte)2)) {
                setMove(RALLY_NAME, (byte)2, Intent.UNKNOWN);  // RALLY
            } else {
                setMove((byte)4, Intent.ATTACK,
                    this.STAB_DMG, this.STAB_AMT, true);  // STAB
            }
        } else {  // 25% 확률
            if (!lastMove((byte)4)) {
                setMove((byte)4, Intent.ATTACK,
                    this.STAB_DMG, this.STAB_AMT, true);  // STAB
            } else {
                setMove(RALLY_NAME, (byte)2, Intent.UNKNOWN);  // RALLY
            }
        }
    } else if (numAliveGremlins() < 2) {  // 하수인 1마리
        if (num < 50) {  // 50% 확률
            if (!lastMove((byte)2)) {
                setMove(RALLY_NAME, (byte)2, Intent.UNKNOWN);  // RALLY
            } else {
                getMove(AbstractDungeon.aiRng.random(50, 99));  // 재귀
            }
        } else if (num < 80) {  // 30% 확률
            if (!lastMove((byte)3)) {
                setMove((byte)3, Intent.DEFEND_BUFF);  // ENCOURAGE
            } else {
                setMove((byte)4, Intent.ATTACK,
                    this.STAB_DMG, this.STAB_AMT, true);  // STAB
            }
        } else {  // 20% 확률
            if (!lastMove((byte)4)) {
                setMove((byte)4, Intent.ATTACK,
                    this.STAB_DMG, this.STAB_AMT, true);  // STAB
            } else {
                getMove(AbstractDungeon.aiRng.random(0, 80));  // 재귀
            }
        }
    } else {  // 하수인 2마리 이상
        if (num < 66) {  // 66% 확률
            if (!lastMove((byte)3)) {
                setMove((byte)3, Intent.DEFEND_BUFF);  // ENCOURAGE
            } else {
                setMove((byte)4, Intent.ATTACK,
                    this.STAB_DMG, this.STAB_AMT, true);  // STAB
            }
        } else {  // 34% 확률
            if (!lastMove((byte)4)) {
                setMove((byte)4, Intent.ATTACK,
                    this.STAB_DMG, this.STAB_AMT, true);  // STAB
            } else {
                setMove((byte)3, Intent.DEFEND_BUFF);  // ENCOURAGE
            }
        }
    }
}
```

**로직 요약**:

### 하수인 0마리
- 75%: RALLY (이전 턴 RALLY 아니면)
- 25%: STAB (이전 턴 STAB 아니면)

### 하수인 1마리
- 50%: RALLY (이전 턴 RALLY 아니면)
- 30%: ENCOURAGE (이전 턴 ENCOURAGE 아니면)
- 20%: STAB (이전 턴 STAB 아니면)

### 하수인 2마리 이상
- 66%: ENCOURAGE (이전 턴 ENCOURAGE 아니면)
- 34%: STAB (이전 턴 STAB 아니면)

**핵심 메커니즘**:
- 하수인 수에 따라 패턴 확률 변화
- 같은 패턴 연속 방지 (재귀로 재선택)
- 하수인 적을수록 소환 우선

---

## 특수 동작

### 사망 시 (die)

**코드 위치**: 244-261줄

```java
public void die() {
    super.die();
    boolean first = true;

    // 살아있는 미니언에게 대사 출력
    for (AbstractMonster m : AbstractDungeon.getCurrRoom().monsters.monsters) {
        if (!m.isDying) {
            if (first) {
                AbstractDungeon.actionManager.addToBottom(
                    new ShoutAction(m, DIALOG[3], 0.5F, 1.2F)
                );
                first = false;
            } else {
                AbstractDungeon.actionManager.addToBottom(
                    new ShoutAction(m, DIALOG[4], 0.5F, 1.2F)
                );
            }
        }
    }

    // 모든 미니언 도망
    for (AbstractMonster m : AbstractDungeon.getCurrRoom().monsters.monsters) {
        if (!m.isDying) {
            AbstractDungeon.actionManager.addToBottom(new EscapeAction(m));
        }
    }
}
```

**특징**:
- Leader 사망 시 모든 하수인 도망
- 대사 출력 (2가지 중 순서대로)

---

## 전투 전략

### 추천 전략

**Leader 우선 처치**:
- 소환 차단
- 하수인 자동 제거
- 빠른 전투 종료

**하수인 관리**:
- AOE 카드 활용
- 하수인 수 조절
- ENCOURAGE 빈도 감소

**위험 요소**:
- ENCOURAGE 누적 (Strength 급증)
- 다수 하수인 동시 공격
- 소환 반복 시 장기전

**카운터 전략**:
- AOE: Whirlwind, Immolate
- 디버프: Weak, Vulnerable
- 속공: Leader 집중 타격

---

## 수정 포인트

**Strength/Block 수치 변경**:
- `strAmt`, `blockAmt` 필드 수정 (78-87줄)

**소환 횟수 변경**:
- takeTurn의 RALLY 케이스 수정 (108-112줄)

**AI 확률 조정**:
- getMove의 확률 조건 수정 (154-207줄)

---

# 3. 감독관 (Taskmaster)

## 기본 정보

**클래스명**: `Taskmaster`
**전체 경로**: `com.megacrit.cardcrawl.monsters.city.Taskmaster`
**ID**: `"SlaverBoss"` (내부 ID)
**타입**: 엘리트 (ELITE)

---

## HP 정보

| 난이도 | HP 범위 |
|--------|---------|
| 기본 (A0-A7) | 54-60 |
| A8+ | 57-64 |

**코드 위치**: 39-43줄

```java
if (AbstractDungeon.ascensionLevel >= 8) {
    setHp(57, 64);
} else {
    setHp(54, 60);
}
```

**특징**: 낮은 HP (엘리트 중 최저)

---

## 패턴 정보

### 패턴: 채찍질 (Scouring Whip)

**바이트 값**: `2`
**의도**: `ATTACK_DEBUFF`
**데미지**: 7 (고정)

**효과**:
- 플레이어에게 **7 데미지**
- 덱에 **Wound 추가**
- A18+: 자신에게 **Strength +1**

**Wound 추가 개수**:
| 난이도 | Wound 개수 |
|--------|-----------|
| 기본 (A0-A2) | 1장 |
| A3-A17 | 2장 |
| A18+ | 3장 |

**코드 위치**: 28-32줄, 45-51줄, 66-77줄, 85-87줄

```java
// 상수
private static final int SCOURING_WHIP_DMG = 7;
private static final int WOUNDS = 1;
private static final int A_2_WOUNDS = 2;

// 생성자에서 설정
if (AbstractDungeon.ascensionLevel >= 18) {
    this.woundCount = 3;
} else if (AbstractDungeon.ascensionLevel >= 3) {
    this.woundCount = 2;
} else {
    this.woundCount = 1;
}

this.damage.add(new DamageInfo(this, 4));   // 사용 안 함
this.damage.add(new DamageInfo(this, 7));   // Scouring Whip

// takeTurn 실행
public void takeTurn() {
    switch (this.nextMove) {
        case 2:
            playSfx();
            AbstractDungeon.actionManager.addToBottom(
                new AnimateSlowAttackAction(this)
            );
            AbstractDungeon.actionManager.addToBottom(
                new DamageAction(AbstractDungeon.player,
                    this.damage.get(1),
                    AbstractGameAction.AttackEffect.SLASH_HEAVY)
            );
            AbstractDungeon.actionManager.addToBottom(
                new MakeTempCardInDiscardAction(
                    new Wound(), this.woundCount)
            );
            if (AbstractDungeon.ascensionLevel >= 18) {
                AbstractDungeon.actionManager.addToBottom(
                    new ApplyPowerAction(this, this,
                        new StrengthPower(this, 1), 1)
                );
            }
            break;
    }

    AbstractDungeon.actionManager.addToBottom(new RollMoveAction(this));
}

// getMove: 무조건 Scouring Whip
protected void getMove(int num) {
    setMove((byte)2, Intent.ATTACK_DEBUFF, 7);
}
```

**특징**:
- **단일 패턴** (다른 패턴 존재하지 않음)
- 매 턴 동일한 행동 반복
- A18+: 턴마다 Strength +1 (누적)

---

## AI 로직 (getMove)

**코드 위치**: 85-87줄

```java
protected void getMove(int num) {
    setMove((byte)2, AbstractMonster.Intent.ATTACK_DEBUFF, 7);
}
```

**로직 설명**:
- **무조건 Scouring Whip 사용**
- 확률 없음, 조건 없음
- num 값 사용 안 함

**핵심 메커니즘**:
- 예측 가능한 단순 패턴
- 대응 용이하지만 지속 데미지
- Wound 누적이 주요 위협

---

## 전투 전략

### 추천 전략

**속공 전략** (추천):
- 낮은 HP (54-64) 이용
- 빠른 처치로 Wound 축적 방지
- 고데미지 카드 집중 사용

**Wound 관리**:
- 매 턴 Wound 추가 (A18+: 3장)
- Exhaust 카드로 제거
- 덱 순환 속도 저하 주의

**A18+ 위험 요소**:
- Wound 3장/턴 (9턴 = 27장)
- 매 턴 Strength +1 누적
- 장기전 시 데미지 급증

**카운터 전략**:
- 속공: 5턴 내 처치 목표
- Second Wind: Wound 활용
- Evolve: Wound를 드로우로 전환

---

## 특수 동작

### 음성 효과 (playSfx)

**코드 위치**: 89-96줄

```java
private void playSfx() {
    int roll = MathUtils.random(1);
    if (roll == 0) {
        AbstractDungeon.actionManager.addToBottom(
            new SFXAction("VO_SLAVERLEADER_1A")
        );
    } else {
        AbstractDungeon.actionManager.addToBottom(
            new SFXAction("VO_SLAVERLEADER_1B")
        );
    }
}
```

**특징**: 공격 시 랜덤 음성 (2가지 중 1개)

### 사망 시 (die)

**코드 위치**: 98-111줄

```java
private void playDeathSfx() {
    int roll = MathUtils.random(1);
    if (roll == 0) {
        CardCrawlGame.sound.play("VO_SLAVERLEADER_2A");
    } else {
        CardCrawlGame.sound.play("VO_SLAVERLEADER_2B");
    }
}

public void die() {
    super.die();
    playDeathSfx();
}
```

**특징**: 사망 시 랜덤 음성 (2가지 중 1개)

---

## 수정 포인트

**데미지 변경**:
- 고정 7 데미지 (getMove 87줄)
- damage.get(1) 초기화 부분 수정 (54줄)

**Wound 개수 변경**:
- `woundCount` 필드 수정 (45-51줄)

**Strength 메커니즘 추가/제거**:
- takeTurn의 A18 조건 수정 (73-76줄)

**패턴 다양화**:
- getMove 전면 수정 (85-87줄)
- takeTurn에 새 케이스 추가 (66-77줄)

---

## 중요 필드

### Book of Stabbing

| 필드명 | 타입 | 설명 |
|--------|------|------|
| `stabCount` | int | 현재 타수 (1부터 시작, 무한 증가) |
| `stabDmg` | int | Multi Stab 1타 데미지 (6 또는 7) |
| `bigStabDmg` | int | Big Stab 데미지 (21 또는 24) |

### Gremlin Leader

| 필드명 | 타입 | 설명 |
|--------|------|------|
| `gremlins` | AbstractMonster[] | 하수인 배열 (크기 3) |
| `strAmt` | int | Encourage Strength (3, 4, 또는 5) |
| `blockAmt` | int | Encourage Block (6 또는 10) |
| `STAB_DMG` | int | Stab 데미지 (6 고정) |
| `STAB_AMT` | int | Stab 타수 (3 고정) |

### Taskmaster

| 필드명 | 타입 | 설명 |
|--------|------|------|
| `woundCount` | int | 턴당 Wound 추가 개수 (1, 2, 또는 3) |

---

## 관련 파일

### Book of Stabbing
- **본 파일**: `com/megacrit/cardcrawl/monsters/city/BookOfStabbing.java`
- **파워**: `com.megacrit.cardcrawl.powers.PainfulStabsPower`
- **액션**:
  - `ChangeStateAction`
  - `DamageAction`
  - `ApplyPowerAction`
  - `SFXAction`

### Gremlin Leader
- **본 파일**: `com/megacrit/cardcrawl/monsters/city/GremlinLeader.java`
- **파워**:
  - `com.megacrit.cardcrawl.powers.MinionPower`
  - `com.megacrit.cardcrawl.powers.StrengthPower`
- **액션**:
  - `SummonGremlinAction`
  - `ShoutAction`
  - `ApplyPowerAction`
  - `GainBlockAction`
  - `DamageAction`
  - `EscapeAction`

### Taskmaster
- **본 파일**: `com/megacrit/cardcrawl/monsters/city/Taskmaster.java`
- **카드**: `com.megacrit.cardcrawl.cards.status.Wound`
- **파워**: `com.megacrit.cardcrawl.powers.StrengthPower` (A18+)
- **액션**:
  - `AnimateSlowAttackAction`
  - `DamageAction`
  - `MakeTempCardInDiscardAction`
  - `ApplyPowerAction`
  - `SFXAction`

---

## 참고사항

1. **Book of Stabbing**:
   - Painful Stabs 파워 (공격 카드마다 1 데미지)
   - 타수 무한 증가 (최대 제한 없음)
   - 두 가지 공격 패턴 (Multi Stab, Big Stab)
   - A18+: Big Stab 사용 시에도 stabCount 증가

2. **Gremlin Leader**:
   - 하수인 수에 따라 AI 변화
   - ENCOURAGE: 모든 아군 Strength, 미니언만 Block
   - Leader 사망 시 모든 하수인 도망
   - RALLY: 2마리 연속 소환

3. **Taskmaster**:
   - **단일 패턴만 존재** (Scouring Whip)
   - 매 턴 Wound 누적 (A18+: 3장)
   - A18+: 매 턴 Strength +1 자가 버프
   - 낮은 HP로 속공 가능

4. **엘리트 특징**:
   - 높은 HP (Book, Leader)
   - 복잡한 메커니즘 (Book, Leader)
   - 단순하지만 치명적 (Taskmaster)
   - A18+ 대폭 강화

5. **문서 정확성**:
   - 실제 디컴파일 소스 기반
   - 모든 수치와 로직 검증됨
   - 코드 라인 번호 명시
