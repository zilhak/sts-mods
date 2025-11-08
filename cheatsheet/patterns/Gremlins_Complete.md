# Gremlins Complete Guide (그렘린 완전 가이드)

1막의 모든 그렘린 몬스터 패턴 정리 (5종류)

---

## 그렘린 공통 특징

### 기본 메커니즘
1. **deathReact()**: 동료가 죽으면 도주 (Intent.ESCAPE로 전환)
2. **escapeNext()**: 플레이어 HP 낮을 때 탈출 플래그 설정
3. **음성**: VO_GREMLIN* 시리즈 (공격/죽음 음성)
4. **대사**: 도주 시 대사 출력 (SpeechBubble)

### 도주(ESCAPE) 공통 코드
모든 그렘린은 바이트값 99로 도주 패턴 보유:
```java
case 99:
  AbstractDungeon.effectList.add(new SpeechBubble(..., DIALOG[X], ...));
  AbstractDungeon.actionManager.addToBottom(new EscapeAction(this));
  AbstractDungeon.actionManager.addToBottom(new SetMoveAction(this, (byte)99,
    AbstractMonster.Intent.ESCAPE));
```

---

## GremlinTsundere (츤데레 그렘린)

### 기본 정보
- **ID**: `GremlinTsundere`
- **HP**: 12-15 (A0-6) / 13-17 (A7+)
- **애니메이션**: `images/monsters/theBottom/femaleGremlin/skeleton.atlas`
- **특징**: **방어 버프** 전문가, 혼자 남으면 공격으로 전환

### 패턴 정보

#### 1. PROTECT (보호) - 바이트값: 1
**의도**: DEFEND
**효과**: 랜덤 아군 몬스터에게 블록 부여
| 난이도 | 블록량 | 코드 위치 |
|--------|--------|-----------|
| A0-6 | 7 | Line 51 |
| A7-16 | 8 | Line 48 |
| A17+ | 11 | Line 45 |

**중요**: `GainBlockRandomMonsterAction` 사용 - 자신 포함 랜덤

```java
// Line 74-92
case 1:
  AbstractDungeon.actionManager.addToBottom(new GainBlockRandomMonsterAction(this,
    this.blockAmt));

  int aliveCount = 0;
  for (AbstractMonster m : (AbstractDungeon.getMonsters()).monsters) {
    if (!m.isDying && !m.isEscaping) {
      aliveCount++;
    }
  }

  if (this.escapeNext) {
    AbstractDungeon.actionManager.addToBottom(new SetMoveAction(this, (byte)99,
      AbstractMonster.Intent.ESCAPE));
  } else if (aliveCount > 1) {
    setMove(MOVES[0], (byte)1, AbstractMonster.Intent.DEFEND);
  } else {
    setMove(MOVES[1], (byte)2, AbstractMonster.Intent.ATTACK,
      ((DamageInfo)this.damage.get(0)).base);
  }
```

#### 2. BASH (공격) - 바이트값: 2
**의도**: ATTACK (혼자 남았을 때만)
**데미지**:
| 난이도 | 데미지 | 코드 위치 |
|--------|--------|-----------|
| A0-1 | 6 | Line 57 |
| A2+ | 8 | Line 55 |

```java
// Line 96-104
case 2:
  AbstractDungeon.actionManager.addToBottom(new AnimateSlowAttackAction(this));
  AbstractDungeon.actionManager.addToBottom(new DamageAction(AbstractDungeon.player,
    this.damage.get(0), AbstractGameAction.AttackEffect.BLUNT_LIGHT));
  if (this.escapeNext) {
    AbstractDungeon.actionManager.addToBottom(new SetMoveAction(this, (byte)99,
      AbstractMonster.Intent.ESCAPE));
  } else {
    AbstractDungeon.actionManager.addToBottom(new SetMoveAction(this, MOVES[1], (byte)2,
      AbstractMonster.Intent.ATTACK, ((DamageInfo)this.damage.get(0)).base));
  }
```

### AI 로직
```java
// Line 135-137
protected void getMove(int num) {
  setMove(MOVES[0], (byte)1, AbstractMonster.Intent.DEFEND);
}
```

**특징**: getMove는 첫 턴만 호출, 이후는 takeTurn 내 SetMoveAction 사용
- 동료 있음: PROTECT 반복
- 혼자 남음: BASH 반복

---

## GremlinWizard (마법사 그렘린)

### 기본 정보
- **ID**: `GremlinWizard`
- **HP**: 21-25 (A0-6) / 22-26 (A7+)
- **애니메이션**: `images/monsters/theBottom/wizardGremlin/skeleton.atlas`
- **특징**: **차징(CHARGE)** 후 강력한 마법 공격

### 패턴 정보

#### 1. DOPE_MAGIC (강력 마법) - 바이트값: 1
**의도**: ATTACK
**데미지**:
| 난이도 | 데미지 | 코드 위치 |
|--------|--------|-----------|
| A0-1 | 25 | Line 52 |
| A2+ | 30 | Line 50 |

**조건**: currentCharge == 3일 때만 사용 가능

```java
// Line 84-93
case 1:
  this.currentCharge = 0;
  AbstractDungeon.actionManager.addToBottom(new DamageAction(AbstractDungeon.player,
    this.damage.get(0), AbstractGameAction.AttackEffect.FIRE));
  if (this.escapeNext) {
    AbstractDungeon.actionManager.addToBottom(new SetMoveAction(this, (byte)99,
      AbstractMonster.Intent.ESCAPE));
  } else if (AbstractDungeon.ascensionLevel >= 17) {
    setMove(MOVES[1], (byte)1, AbstractMonster.Intent.ATTACK,
      ((DamageInfo)this.damage.get(0)).base); // A17+는 연속 사용 가능
  } else {
    setMove(MOVES[0], (byte)2, AbstractMonster.Intent.UNKNOWN);
  }
```

#### 2. CHARGE (차징) - 바이트값: 2
**의도**: UNKNOWN
**효과**: currentCharge++ (3회 충전 후 DOPE_MAGIC)

**대사**: "충전 중..." 텍스트 표시 (Line 68)

```java
// Line 66-79
case 2:
  this.currentCharge++;
  AbstractDungeon.actionManager.addToBottom(new TextAboveCreatureAction(this, DIALOG[1]));

  if (this.escapeNext) {
    AbstractDungeon.actionManager.addToBottom(new SetMoveAction(this, (byte)99,
      AbstractMonster.Intent.ESCAPE));
  } else if (this.currentCharge == 3) {
    playSfx();
    AbstractDungeon.actionManager.addToBottom(new TalkAction(this, DIALOG[2], 1.5F, 3.0F));
    AbstractDungeon.actionManager.addToBottom(new SetMoveAction(this, MOVES[1], (byte)1,
      AbstractMonster.Intent.ATTACK, ((DamageInfo)this.damage.get(0)).base));
  } else {
    setMove(MOVES[0], (byte)2, AbstractMonster.Intent.UNKNOWN);
  }
```

### AI 로직
```java
// Line 146-148
protected void getMove(int num) {
  setMove(MOVES[0], (byte)2, AbstractMonster.Intent.UNKNOWN);
}
```

**패턴 순서**:
- A0-16: CHARGE → CHARGE → CHARGE → DOPE_MAGIC → CHARGE 반복
- A17+: CHARGE → CHARGE → CHARGE → DOPE_MAGIC → DOPE_MAGIC 반복

---

## GremlinWarrior (전사 그렘린)

### 기본 정보
- **ID**: `GremlinWarrior`
- **HP**: 20-24 (A0-6) / 21-25 (A7+)
- **애니메이션**: `images/monsters/theBottom/angryGremlin/skeleton.atlas`
- **특징**: **분노(Angry)** 파워 보유, 단순 공격형

### usePreBattleAction()
전투 시작 시 **AngryPower** 부여:
| 난이도 | 분노량 | 코드 위치 |
|--------|--------|-----------|
| A0-16 | 1 | Line 65 |
| A17+ | 2 | Line 63 |

```java
// Line 61-67
public void usePreBattleAction() {
  if (AbstractDungeon.ascensionLevel >= 17) {
    AbstractDungeon.actionManager.addToBottom(new ApplyPowerAction(this, this,
      new AngryPower(this, 2)));
  } else {
    AbstractDungeon.actionManager.addToBottom(new ApplyPowerAction(this, this,
      new AngryPower(this, 1)));
  }
}
```

**AngryPower**: 피해 입을 때마다 힘 증가

### 패턴 정보

#### SCRATCH (할퀴기) - 바이트값: 1
**의도**: ATTACK (유일한 패턴)
**데미지**:
| 난이도 | 데미지 | 코드 위치 |
|--------|--------|-----------|
| A0-1 | 4 | Line 49 |
| A2+ | 5 | Line 47 |

```java
// Line 71-82
case 1:
  AbstractDungeon.actionManager.addToBottom(new AnimateSlowAttackAction(this));
  AbstractDungeon.actionManager.addToBottom(new DamageAction(AbstractDungeon.player,
    this.damage.get(0), AbstractGameAction.AttackEffect.SLASH_DIAGONAL));

  if (this.escapeNext) {
    AbstractDungeon.actionManager.addToBottom(new SetMoveAction(this, (byte)99,
      AbstractMonster.Intent.ESCAPE));
  } else {
    AbstractDungeon.actionManager.addToBottom(new SetMoveAction(this, (byte)1,
      AbstractMonster.Intent.ATTACK, ((DamageInfo)this.damage.get(0)).base));
  }
```

### AI 로직
```java
// Line 133-135
protected void getMove(int num) {
  setMove((byte)1, AbstractMonster.Intent.ATTACK, ((DamageInfo)this.damage.get(0)).base);
}
```

**특징**: 가장 단순한 AI - 매 턴 SCRATCH만 사용

---

## GremlinFat (뚱뚱한 그렘린)

### 기본 정보
- **ID**: `GremlinFat`
- **HP**: 13-17 (A0-6) / 14-18 (A7+)
- **애니메이션**: `images/monsters/theBottom/fatGremlin/skeleton.atlas`
- **특징**: **약화 + 나약** 디버프 콤보

### 패턴 정보

#### BLUNT (둔기 공격) - 바이트값: 2
**의도**: ATTACK_DEBUFF (유일한 패턴)
**데미지**:
| 난이도 | 데미지 | 코드 위치 |
|--------|--------|-----------|
| A0-1 | 4 | Line 53 |
| A2+ | 5 | Line 51 |

**디버프**:
- 약화 1턴 (모든 난이도)
- 나약 1턴 (A17+만 추가)

```java
// Line 66-89
case 2:
  AbstractDungeon.actionManager.addToBottom(new AnimateSlowAttackAction(this));
  AbstractDungeon.actionManager.addToBottom(new DamageAction(AbstractDungeon.player,
    this.damage.get(0), AbstractGameAction.AttackEffect.BLUNT_LIGHT));
  AbstractDungeon.actionManager.addToBottom(new ApplyPowerAction(AbstractDungeon.player,
    this, new WeakPower(AbstractDungeon.player, 1, true), 1));

  if (AbstractDungeon.ascensionLevel >= 17) {
    AbstractDungeon.actionManager.addToBottom(new ApplyPowerAction(AbstractDungeon.player,
      this, new FrailPower(AbstractDungeon.player, 1, true), 1));
  }

  if (this.escapeNext) {
    AbstractDungeon.actionManager.addToBottom(new SetMoveAction(this, (byte)99,
      AbstractMonster.Intent.ESCAPE));
  } else {
    AbstractDungeon.actionManager.addToBottom(new RollMoveAction(this));
  }
```

### AI 로직
```java
// Line 143-145
protected void getMove(int num) {
  setMove(MOVES[0], (byte)2, AbstractMonster.Intent.ATTACK_DEBUFF,
    ((DamageInfo)this.damage.get(0)).base);
}
```

**특징**: 매 턴 BLUNT만 사용

---

## GremlinThief (도둑 그렘린)

### 기본 정보
- **ID**: `GremlinThief`
- **HP**: 10-14 (A0-6) / 11-15 (A7+)
- **애니메이션**: `images/monsters/theBottom/thiefGremlin/skeleton.atlas`
- **특징**: 가장 낮은 HP, 높은 공격력

### 패턴 정보

#### PUNCTURE (찌르기) - 바이트값: 1
**의도**: ATTACK (유일한 패턴)
**데미지**:
| 난이도 | 데미지 | 코드 위치 |
|--------|--------|-----------|
| A0-1 | 9 | Line 47 |
| A2+ | 10 | Line 45 |

**특징**: 그렘린 중 가장 높은 단일 데미지

```java
// Line 62-82
case 1:
  AbstractDungeon.actionManager.addToBottom(new AnimateSlowAttackAction(this));
  AbstractDungeon.actionManager.addToBottom(new DamageAction(AbstractDungeon.player,
    this.damage.get(0), AbstractGameAction.AttackEffect.SLASH_HORIZONTAL));

  if (!this.escapeNext) {
    AbstractDungeon.actionManager.addToBottom(new SetMoveAction(this, (byte)1,
      AbstractMonster.Intent.ATTACK, this.thiefDamage));
  } else {
    AbstractDungeon.actionManager.addToBottom(new SetMoveAction(this, (byte)99,
      AbstractMonster.Intent.ESCAPE));
  }
```

### AI 로직
```java
// Line 124-126
protected void getMove(int num) {
  setMove((byte)1, AbstractMonster.Intent.ATTACK, ((DamageInfo)this.damage.get(0)).base);
}
```

**특징**: 매 턴 PUNCTURE만 사용, 가장 위협적인 공격형

---

## 그렘린 비교표

| 이름 | HP | 데미지 | 특수능력 | 위협도 |
|------|----|----|---------|--------|
| **Tsundere** | 12-17 | 6/8 | 아군 블록 7-11 | 높음 (보조) |
| **Wizard** | 21-26 | 25/30 | 차징 후 마법 | 높음 (지연) |
| **Warrior** | 20-25 | 4/5 | 분노 +1/2 | 중간 (누적) |
| **Fat** | 13-18 | 4/5 | 약화+나약 | 중간 (디버프) |
| **Thief** | 10-15 | 9/10 | 없음 | 높음 (직접) |

### 위협도 분석
1. **Wizard**: 30 데미지 + 차징 시간 → 가장 위험
2. **Thief**: 10 데미지 매 턴 → 누적 위협
3. **Tsundere**: 블록 11 → 전투 지연
4. **Warrior**: 분노 누적 → 장기전 위협
5. **Fat**: 약화+나약 → 간접 위협

---

## 그렘린 조우 전략

### 일반 조우 (3-5마리)

**우선순위 처치 순서**:
1. **Wizard** (차징 3턴 전에 처치)
2. **Thief** (높은 데미지)
3. **Warrior** (분노 누적 전)
4. **Fat** (디버프)
5. **Tsundere** (마지막 남으면 공격형으로 전환)

**광역 공격 유효**: 그렘린은 HP가 낮아 광역으로 일괄 처리 가능

### 도주 메커니즘
- 동료가 죽으면 deathReact() 발동
- 모든 그렘린이 ESCAPE Intent로 전환
- 1턴 내에 도주하므로 즉시 처치 필요

---

## 그렘린 수정 예시

### 1. Wizard 차징 시간 단축 (2회로)
```java
// GremlinWizard Line 34, 73 수정
private static final int CHARGE_LIMIT = 2; // 3 → 2

if (this.currentCharge == 2) { // 3 → 2
  // ... DOPE_MAGIC 준비
}
```

### 2. Warrior 분노 증가량 상향
```java
// GremlinWarrior Line 62-66 수정
if (AbstractDungeon.ascensionLevel >= 17) {
  AbstractDungeon.actionManager.addToBottom(new ApplyPowerAction(this, this,
    new AngryPower(this, 4))); // 2 → 4
} else {
  AbstractDungeon.actionManager.addToBottom(new ApplyPowerAction(this, this,
    new AngryPower(this, 2))); // 1 → 2
}
```

### 3. Tsundere 블록 대상 자신으로 고정
```java
// GremlinTsundere Line 75 수정
AbstractDungeon.actionManager.addToBottom(new GainBlockAction(this, this,
  this.blockAmt)); // GainBlockRandomMonsterAction → GainBlockAction
```

### 4. Fat 디버프 턴수 증가
```java
// GremlinFat Line 71 수정
AbstractDungeon.actionManager.addToBottom(new ApplyPowerAction(AbstractDungeon.player,
  this, new WeakPower(AbstractDungeon.player, 2, true), 2)); // 1 → 2

// Line 77-79 수정 (A17+)
if (AbstractDungeon.ascensionLevel >= 17) {
  AbstractDungeon.actionManager.addToBottom(new ApplyPowerAction(AbstractDungeon.player,
    this, new FrailPower(AbstractDungeon.player, 2, true), 2)); // 1 → 2
}
```

### 5. Thief 데미지 대폭 상향
```java
// GremlinThief Line 44-48 수정
if (AbstractDungeon.ascensionLevel >= 2) {
  this.thiefDamage = 15; // 10 → 15
} else {
  this.thiefDamage = 12; // 9 → 12
}
```

### 6. 도주 불가능하게 만들기
```java
// 모든 그렘린의 deathReact(), escapeNext() 메서드 제거 또는 비활성화
@Override
public void deathReact() {
  // 아무것도 하지 않음
}

public void escapeNext() {
  // 아무것도 하지 않음
}
```

---

## 전투 팁

1. **Wizard 우선**: 차징 3턴 내에 반드시 처치
2. **도주 대비**: 광역 공격으로 동시 처치 권장
3. **Tsundere 고립**: 마지막까지 남기지 말 것 (공격형 전환)
4. **Warrior 장기전**: 분노 누적 전에 빠르게 처리
5. **Fat 디버프**: 공격/방어 덱 모두에 영향

---

## 관련 파일
- **Powers**:
  - `AngryPower`: 피해 입을 때마다 힘 증가 (GremlinWarrior)
  - `WeakPower`, `FrailPower`: GremlinFat 디버프
- **Actions**:
  - `GainBlockRandomMonsterAction`: 랜덤 아군 블록 (GremlinTsundere)
  - `EscapeAction`: 도주 처리 (모든 그렘린)
  - `TextAboveCreatureAction`: 차징 텍스트 (GremlinWizard)
- **음성**: VO_GREMLIN* 시리즈
- **특이사항**: deathReact()와 escapeNext()를 통한 그룹 도주 메커니즘
